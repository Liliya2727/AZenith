#!/system/bin/sh
# AppOps applier (OPT-IN) for Transsion Optimizer.
# Engine credit: Astrax (script de AppOps: mode_of parser, readback, retry variants, per-call timeout).
# Adapted: no permission revoke, state in /data/local/tmp/vixun, protected packages skipped,
#          original mode of every op is saved once to $D/orig_appops (restored by uninstall.sh).
#
# usage: appops_apply.sh [cfg] [logfile] [src]      defaults: $D/appops.cfg  $D/appops.log  boot
# cfg format (one package per line):  pkg|OP:mode,OP:mode      mode = allow|ignore|deny|foreground
#   e.g.  com.example.app|WAKE_LOCK:ignore,COARSE_LOCATION:deny
# Empty / missing cfg -> exit 0, module defaults untouched.
# restore mode:  appops_apply.sh restore <backupfile>   (puts the saved modes back, then deletes the backup)
# env (used by ram_daemon Game RAM Guard): AO_BK=backup file, AO_SKIP=package to skip (the game in front), AO_MAX=max packages
# DO NOT put RUN_IN_BACKGROUND / RUN_ANY_IN_BACKGROUND for packages in $D/app_priority or $D/app_restrict:
# ram_daemon re-applies those every ~10 min and they would overwrite each other (such ops are skipped + logged "conflict").
case "$0" in */*) _bd=${0%/*} ;; *) _bd=. ;; esac
. "$_bd/common.sh"
if [ "$1" = restore ]; then
    _bk="${2:-$D/orig_appops}"
    [ -f "$_bk" ] || exit 0
    while IFS='|' read -r p o m; do [ -n "$p" ] && cmd appops set "$p" "$o" "$m" 2>/dev/null; done < "$_bk"
    rm -f "$_bk"
    exit 0
fi
AO_BK="${AO_BK:-$D/orig_appops}"
CFG="${1:-$D/appops.cfg}"
LOGF="${2:-$D/appops.log}"
SRC="${3:-boot}"
[ -s "$CFG" ] || exit 0
TMPD="$D"
mkdir -p "$TMPD" "$(dirname "$LOGF")" 2>/dev/null
W="$TMPD/.am_w.$$"
rm -rf "$W" 2>/dev/null
mkdir -p "$W" 2>/dev/null
NOW() { date '+%Y-%m-%d %H:%M:%S'; }
JOBS=6
TS=$(NOW)

# same static part as protected() in ram_daemon.sh: system, launcher, gms, root/ADB managers
prot() {
    case "$1" in
        android|com.android.systemui|com.android.phone|com.android.settings|com.android.providers*|\
        com.google.android.gms*|com.google.android.gsf*|*inputmethod*|*keyboard*|*launcher*|*Launcher*|\
        *dialer*|*telecom*|*axeron*|*shizuku*) return 0 ;;
    esac
    case "$1" in
        com.topjohnwu.magisk*|*magisk*|*kernelsu*|*ksunext*|*sukisu*|*apatch*|*lsposed*|*shizuku*|*dhizuku*|\
        com.rosan.*|com.termux*|*brevent*|com.android.shell) return 0 ;;
    esac
    return 1
}

run_to() {
    rf="$1"; rt="$2"; shift; shift
    "$@" >"$rf" 2>&1 </dev/null &
    rp=$!
    ( sleep "$rt"; kill "$rp" 2>/dev/null ) >/dev/null 2>&1 &
    rw=$!
    wait "$rp"
    kill "$rw" 2>/dev/null
}

mode_of() {
    awk -v op="$1" '
        { l=$0; sub(/^[ \t]+/, "", l) }
        l ~ ("^Uid mode: " op ":") { sub(/^Uid mode: [A-Z_0-9]+: */, "", l); sub(/[; ].*$/, "", l); um=l; next }
        l ~ ("^" op ":") { sub(/^[A-Z_0-9]+: */, "", l); sub(/[; ].*$/, "", l); pm=l }
        END { if (um != "" && um != "default") print um; else if (pm != "") print pm; else print "default" }
    ' "$2"
}

do_pkg() {
    idx="$1"; pkg="$2"; ops="$3"
    PW="$W/p$idx"
    mkdir -p "$PW"
    L="$PW/log"; : > "$L"
    t=0; ok=0; al=0; fl=0; un=0
    run_to "$PW/get" 12 cmd appops get "$pkg"
    todo=""
    for kv in $(echo "$ops" | tr ',' ' '); do
        op="${kv%%:*}"; want="${kv#*:}"
        case "$op" in *[!A-Z_0-9]*|"") continue ;; esac
        case "$want" in allow|ignore|deny|foreground) ;; *) continue ;; esac
        case "$op" in
            RUN_IN_BACKGROUND|RUN_ANY_IN_BACKGROUND)
                if grep -qxF "$pkg" "$D/app_priority" "$D/app_restrict" 2>/dev/null; then
                    echo "$TS|$SRC|$pkg|$op|$want|conflict" >> "$L"; continue
                fi ;;
        esac
        t=$((t + 1))
        if [ "$(mode_of "$op" "$PW/get")" = "$want" ]; then
            echo "$TS|$SRC|$pkg|$op|$want|already" >> "$L"; al=$((al + 1))
        else
            todo="$todo $op:$want"
        fi
    done
    if [ -n "$todo" ]; then
        # save the original mode once per op, before the first change
        for kv in $todo; do op="${kv%%:*}"
            grep -q "^$pkg|$op|" "$AO_BK" 2>/dev/null || echo "$pkg|$op|$(mode_of "$op" "$PW/get")" >> "$AO_BK"
        done
        for kv in $todo; do
            op="${kv%%:*}"; want="${kv#*:}"
            run_to "$PW/s" 8 cmd appops set "$pkg" "$op" "$want"
            if grep -qiE 'unknown operation|invalid operation|unknown op' "$PW/s" 2>/dev/null; then
                echo "$op" >> "$PW/unsup"
            fi
        done
        run_to "$PW/get" 12 cmd appops get "$pkg"
        for kv in $todo; do
            op="${kv%%:*}"; want="${kv#*:}"
            if grep -qx "$op" "$PW/unsup" 2>/dev/null; then
                echo "$TS|$SRC|$pkg|$op|$want|unsup" >> "$L"; un=$((un + 1)); continue
            fi
            res=fail
            if [ "$(mode_of "$op" "$PW/get")" = "$want" ]; then
                res=ok
            else
                for v in 2 3 4; do
                    case "$v" in
                        2) run_to "$PW/s" 8 cmd appops set --user 0 "$pkg" "$op" "$want" ;;
                        3) run_to "$PW/s" 8 cmd appops set --uid "$pkg" "$op" "$want" ;;
                        4) run_to "$PW/s" 10 appops set "$pkg" "$op" "$want" ;;
                    esac
                    run_to "$PW/c" 8 cmd appops get "$pkg" "$op"
                    if grep -qE "(^|[^a-z])$op: $want|Uid mode: $op: $want" "$PW/c" 2>/dev/null; then res=ok; break; fi
                done
            fi
            echo "$TS|$SRC|$pkg|$op|$want|$res" >> "$L"
            if [ "$res" = ok ]; then ok=$((ok + 1)); else fl=$((fl + 1)); fi
        done
    fi
    echo "$t $ok $al $fl $un 0" > "$PW/cnt"
}

run_to "$W/pk" 20 pm list packages
sed -i 's/^package://' "$W/pk" 2>/dev/null

idx=0; running=0; MS=0
: > "$W/order"
while IFS='|' read -r pkg ops _; do
    case "$pkg" in ""|\#*) continue ;; esac
    case "$pkg" in *.*) ;; *) continue ;; esac
        [ "$idx" -ge "${AO_MAX:-9999}" ] && break
    idx=$((idx + 1))
    echo "$idx" >> "$W/order"
    if [ -n "$AO_SKIP" ] && [ "$pkg" = "$AO_SKIP" ]; then
        mkdir -p "$W/p$idx"
        echo "$TS|$SRC|$pkg|-|-|ingame" > "$W/p$idx/log"
        echo "0 0 0 0 0 0" > "$W/p$idx/cnt"
        continue
    fi
    if prot "$pkg"; then
        mkdir -p "$W/p$idx"
        echo "$TS|$SRC|$pkg|-|-|protected" > "$W/p$idx/log"
        echo "0 0 0 0 0 0" > "$W/p$idx/cnt"
        continue
    fi
    if ! grep -qx "$pkg" "$W/pk" 2>/dev/null; then
        mkdir -p "$W/p$idx"
        echo "$TS|$SRC|$pkg|-|-|missing" > "$W/p$idx/log"
        echo "0 0 0 0 0 1" > "$W/p$idx/cnt"
        continue
    fi
    do_pkg "$idx" "$pkg" "$ops" &
    running=$((running + 1))
    if [ "$running" -ge "$JOBS" ]; then wait; running=0; fi
done < "$CFG"
wait

run_to "$W/ws" 8 cmd appops write-settings

T=0; OK=0; AL=0; FL=0; UN=0; MS=0
: > "$W/all"
for i in $(cat "$W/order"); do
    [ -f "$W/p$i/log" ] && cat "$W/p$i/log" >> "$W/all"
    if [ -f "$W/p$i/cnt" ]; then
        read -r a b c d e f < "$W/p$i/cnt"
        T=$((T + a)); OK=$((OK + b)); AL=$((AL + c)); FL=$((FL + d)); UN=$((UN + e)); MS=$((MS + f))
    fi
done

{
    echo "#RUN|$TS|$SRC|$T|$OK|$AL|$FL|$UN|$MS"
    cat "$W/all"
} >> "$LOGF" 2>/dev/null
n=$(wc -l < "$LOGF" 2>/dev/null)
if [ "${n:-0}" -gt 1200 ]; then
    tail -n 800 "$LOGF" > "$W/trim" 2>/dev/null && cat "$W/trim" > "$LOGF"
fi
rm -rf "$W" 2>/dev/null
echo "AMSUM|$T|$OK|$AL|$FL|$UN|$MS"
exit 0
