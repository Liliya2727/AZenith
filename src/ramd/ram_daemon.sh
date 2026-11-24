#!/system/bin/sh
# Transsion Optimizer - Adaptive RAM daemon (shell uid, no root needed)
# usage: ram_daemon.sh [once|once game|stop|info|sessions [N]|fstrim|prio add|rm|list [pkg]|restrict add|rm|list [pkg]]  (no arg = run daemon)
# Game RAM Guard (formerly "game mode"; RAM/priority only, no CPU/GPU): gamelist.txt + $D/gamelist -> one soft sweep, no routine kill while playing; off: $D/no_gamemode
# Sessions/learning/heat (game_stats.sh): $D/game_sessions.log, $D/game_learn (per-game sweep aggressiveness), battery-temp aware | off: $D/no_learn, $D/no_heat_aware, $D/no_heat_notify | thresholds: $D/heat.cfg
# Kill-regret (regret.sh): an app reopened <90 s after the daemon killed it ranks lower; 3 regrets/24 h = skipped by the normal kill pass | off: $D/no_regret
# ZRAM-aware (zram_aware.sh): swap usage steers trim/kill levels and the game sweep target; off: $D/no_zram_aware | thresholds: $D/zram_thr.cfg (ZR_SAT/ZR_LOW/ZR_COMPACT)
# whitelist: $D/ram_whitelist | off flags: $D/no_usap, $D/no_fstrim | fstrim hours: $D/fstrim_hours (1-48, default 3)

case "$0" in */*) _bd0=${0%/*} ;; *) _bd0=. ;; esac
. "$_bd0/common.sh"
. "$_bd0/zram_aware.sh"
. "$_bd0/game_stats.sh"
. "$_bd0/regret.sh"
PIDF=$D/ramd.pid
LOG=$D/ramd.log
WL=$D/ram_whitelist
PRIOF=$D/app_priority
RESF=$D/app_restrict
mkdir -p "$D"

BASE_INT=5; FAST_INT=5; GAME_INT=6; OFF_INT=60; LOG_MAX=65536
PROCD=/proc
case "$0" in */*) _bd=${0%/*} ;; *) _bd=. ;; esac
GL_BASE="$_bd/gamelist.txt"; GAME_SWEEP_AVAIL=55
TRIM_GAP=300
TOPCS=${TOPCS:-/dev/cpuset/top-app/cgroup.procs}
_ps=$(getconf PAGESIZE 2>/dev/null); case "$_ps" in ''|*[!0-9]*) _ps=4096 ;; esac; PAGE_KB=$((_ps / 1024))
_fh=$(cat "$D/fstrim_hours" 2>/dev/null); case "$_fh" in ''|*[!0-9]*) _fh=3 ;; esac
[ "$_fh" -ge 1 ] && [ "$_fh" -le 48 ] || _fh=3
FSTRIM_GAP=$((_fh * 3600)); FSTRIM_F="$D/fstrim_last"
MEM_MB=$(awk '/^MemTotal/{printf "%d", $2/1024}' /proc/meminfo)
SWAP_KB=$(awk '/^SwapTotal/{print $2}' /proc/meminfo)
if [ -f "$D/ram_tier" ]; then TIER=$(cat "$D/ram_tier")
elif [ "$MEM_MB" -lt 3000 ]; then TIER=3
elif [ "$MEM_MB" -lt 4300 ]; then TIER=4
elif [ "$MEM_MB" -lt 6200 ]; then TIER=6
elif [ "$MEM_MB" -lt 8300 ]; then TIER=8
else TIER=12; fi

case "$TIER" in
    3)  LV1_AVAIL=35; LV2_AVAIL=22; OFF_AVAIL=45; LV1_PSI=2; LV2_PSI=8;  COOLDOWN=30; CACHED_ADJ=800  ;;
    4)  LV1_AVAIL=32; LV2_AVAIL=20; OFF_AVAIL=42; LV1_PSI=3; LV2_PSI=10; COOLDOWN=40; CACHED_ADJ=800  ;;
    6)  LV1_AVAIL=28; LV2_AVAIL=16; OFF_AVAIL=38; LV1_PSI=3; LV2_PSI=10; COOLDOWN=45; CACHED_ADJ=900  ;;
    8)  LV1_AVAIL=24; LV2_AVAIL=13; OFF_AVAIL=34; LV1_PSI=4; LV2_PSI=12; COOLDOWN=60; CACHED_ADJ=900  ;;
    *)  LV1_AVAIL=18; LV2_AVAIL=9;  OFF_AVAIL=28; LV1_PSI=5; LV2_PSI=15; COOLDOWN=90; CACHED_ADJ=900  ;;
esac
case "$TIER" in
    3) MEMFACTOR=LOW;      MAXCACHED=16; REORANK=1 ;;
    4) MEMFACTOR=LOW;      MAXCACHED=20; REORANK=1 ;;
    6) MEMFACTOR=LOW;      MAXCACHED=24; REORANK=1 ;;
    8) MEMFACTOR="";       MAXCACHED=32; REORANK=0 ;;
    *) MEMFACTOR="";       MAXCACHED=48; REORANK=0 ;;
esac
case "$TIER" in
    3) USAP_MAX=2; USAP_MIN=1; USAP_THR=1; USAP_DLY=4000 ;;
    4) USAP_MAX=3; USAP_MIN=1; USAP_THR=1; USAP_DLY=3000 ;;
    6) USAP_MAX=4; USAP_MIN=2; USAP_THR=2; USAP_DLY=2500 ;;
    8) USAP_MAX=6; USAP_MIN=2; USAP_THR=3; USAP_DLY=2000 ;;
    *) USAP_MAX=8; USAP_MIN=3; USAP_THR=4; USAP_DLY=1500 ;;
esac
SDK=$(getprop ro.build.version.sdk)

if [ -z "$SWAP_KB" ] || [ "$SWAP_KB" -eq 0 ]; then
    NOSWAP=1
    LV1_AVAIL=$((LV1_AVAIL + 4)); LV2_AVAIL=$((LV2_AVAIL + 3)); OFF_AVAIL=$((OFF_AVAIL + 3))
else
    NOSWAP=0
fi
DEEP_AVAIL=$((LV2_AVAIL / 2))
load_zr_cfg

revert_usap() {
    for k in usap_pool_enabled usap_pool_size_max usap_pool_size_min usap_refill_threshold usap_pool_refill_delay_ms; do
        device_config delete runtime_native "$k" >/dev/null 2>&1
    done
}
apply_usap() {
    if [ "${SDK:-0}" -ge 29 ] && [ ! -f "$D/no_usap" ]; then
        device_config put runtime_native usap_pool_size_max "$USAP_MAX" >/dev/null 2>&1
        device_config put runtime_native usap_pool_size_min "$USAP_MIN" >/dev/null 2>&1
        device_config put runtime_native usap_refill_threshold "$USAP_THR" >/dev/null 2>&1
        device_config put runtime_native usap_pool_refill_delay_ms "$USAP_DLY" >/dev/null 2>&1
        device_config put runtime_native usap_pool_enabled true >/dev/null 2>&1
    else revert_usap; fi
}
apply_am() {
    apply_usap
    if [ -n "$MEMFACTOR" ]; then am memory-factor set "$MEMFACTOR" >/dev/null 2>&1
    else am memory-factor reset >/dev/null 2>&1; fi
    device_config put activity_manager max_cached_processes "$MAXCACHED" >/dev/null 2>&1
    device_config put activity_manager max_empty_app_processes "$((MAXCACHED / 2))" >/dev/null 2>&1
    if [ "$REORANK" = 1 ]; then device_config put activity_manager use_oom_re_ranking true >/dev/null 2>&1
    else device_config delete activity_manager use_oom_re_ranking >/dev/null 2>&1; fi
    if [ -f "$D/lmkd_tune" ] && ! zram_mod_on; then
        device_config put lmkd_native use_minfree_levels true >/dev/null 2>&1
        device_config put lmkd_native thrashing_limit_critical 60 >/dev/null 2>&1
    else
        device_config delete lmkd_native use_minfree_levels >/dev/null 2>&1
        device_config delete lmkd_native thrashing_limit_critical >/dev/null 2>&1
    fi
    if [ "$NOSWAP" = 0 ]; then device_config put activity_manager use_compaction true >/dev/null 2>&1
    else device_config delete activity_manager use_compaction >/dev/null 2>&1; fi
}
revert_am() {
    revert_usap
    for k in max_cached_processes max_empty_app_processes use_oom_re_ranking use_compaction; do
        device_config delete activity_manager "$k" >/dev/null 2>&1
    done
    device_config delete lmkd_native use_minfree_levels >/dev/null 2>&1
    device_config delete lmkd_native thrashing_limit_critical >/dev/null 2>&1
    am memory-factor reset >/dev/null 2>&1
}

load_games() {
    GAMES="
$({ cat "$GL_BASE"; echo; cat "$D/gamelist"; } 2>/dev/null | tr -d '\r')
"
    PRIOS="
$(grep -hv '^#' "$PRIOF" 2>/dev/null | tr -d '\r')
"
    WLS="
$(grep -hv '^#' "$WL" 2>/dev/null | tr -d '\r')
"
}

nice_of() { _st=$(cat "/proc/$1/stat" 2>/dev/null) || return 1; _st=${_st##*) }; set -- $_st; echo "${17}"; }
set_nice() {
    [ "$IS_ROOT" = 1 ] || return 0
    for _rp in $(pidof "$1" 2>/dev/null); do
        _cur=$(nice_of "$_rp"); case "$_cur" in ''|*[!0-9-]*) continue ;; esac
        [ "$_cur" = "$2" ] && continue
        renice -n $(( $2 - _cur )) -p "$_rp" >/dev/null 2>&1 </dev/null
        [ "$(nice_of "$_rp")" = "$2" ] || echo "renice:$1" >> "$D/denied.log"
    done
}
prio_on() {
    set_nice "$1" -4
    cmd activity set-standby-bucket "$1" active >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_IN_BACKGROUND allow >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1 </dev/null
    cmd activity set-bg-restriction-level --user 0 "$1" unrestricted >/dev/null 2>&1 </dev/null
    cmd deviceidle whitelist +"$1" >/dev/null 2>&1 </dev/null
}
prio_off() {
    set_nice "$1" 0

    cmd activity set-standby-bucket "$1" working_set >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_IN_BACKGROUND default >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_ANY_IN_BACKGROUND default >/dev/null 2>&1 </dev/null
    cmd activity set-bg-restriction-level --user 0 "$1" adaptive >/dev/null 2>&1 </dev/null
    cmd deviceidle whitelist -"$1" >/dev/null 2>&1 </dev/null
}
prio_each() {
    for _l in $(grep -hv '^#' "$PRIOF" 2>/dev/null | tr -d '\r'); do "$1" "$_l"; done
}

restrict_on() {
    cmd appops set "$1" RUN_ANY_IN_BACKGROUND ignore >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_IN_BACKGROUND ignore >/dev/null 2>&1 </dev/null
    cmd activity set-standby-bucket "$1" restricted >/dev/null 2>&1 </dev/null
    cmd activity set-bg-restriction-level --user 0 "$1" restricted >/dev/null 2>&1 </dev/null
}
restrict_off() {
    cmd appops set "$1" RUN_ANY_IN_BACKGROUND default >/dev/null 2>&1 </dev/null
    cmd appops set "$1" RUN_IN_BACKGROUND default >/dev/null 2>&1 </dev/null
    cmd activity set-standby-bucket "$1" working_set >/dev/null 2>&1 </dev/null
    cmd activity set-bg-restriction-level --user 0 "$1" adaptive >/dev/null 2>&1 </dev/null
}
restrict_each() {
    for _l in $(grep -hv '^#' "$RESF" 2>/dev/null | tr -d '\r'); do
        if [ "$1" = restrict_on ]; then
            case "$PRIOS" in *"
$_l
"*) continue ;; esac
            [ "$_l" = "$GAMING" ] && continue
        fi
        "$1" "$_l"
    done
}

gm_on() {
    [ -f "$D/opt_game_mode" ] || return 0
    [ -f "$D/game_mode_orig" ] && gm_off
    _o=$(cmd game mode "$1" 2>/dev/null </dev/null | tr 'A-Z' 'a-z' | grep -oE 'standard|performance|battery|custom' | head -n1)
    [ "$_o" = performance ] && return 0
    case "$_o" in standard|battery) ;; *) _o=standard ;; esac
    echo "$1|$_o" > "$D/game_mode_orig"
    cmd game set --mode performance "$1" >/dev/null 2>&1 </dev/null || { rm -f "$D/game_mode_orig"; log "game_mode: set failed $1"; }
}
gm_off() {
    [ -f "$D/game_mode_orig" ] || return 0
    IFS='|' read -r _gmp _gmm < "$D/game_mode_orig"
    [ -n "$_gmp" ] && cmd game set --mode "${_gmm:-standard}" "$_gmp" >/dev/null 2>&1 </dev/null
    rm -f "$D/game_mode_orig"
}

# Root: game process gets a negative oom_score_adj = lmkd never kills it. -750 is distinct from AMS values
# (persistent -800/-900), so detection can recognise it. AMS overwrites the value on every state change
# (floating window, home, ...), so it is re-applied from the loop. Off: touch $D/no_oom_protect
OOM_PROT=-750
oom_protect() {
    [ "$IS_ROOT" = 1 ] && [ ! -f "$D/no_oom_protect" ] || return 0
    for _op in $(pidof "$1" 2>/dev/null); do
        read -r _oa 2>/dev/null < "$PROCD/$_op/oom_score_adj" || continue
        [ "$_oa" -gt "$OOM_PROT" ] 2>/dev/null && echo "$OOM_PROT" > "$PROCD/$_op/oom_score_adj" 2>/dev/null
    done
    return 0
}
oom_unprotect() { # value left behind by us -> neutral previous-app level; AMS corrects it on its next update
    [ -n "$1" ] || return 0
    for _op in $(pidof "$1" 2>/dev/null); do
        read -r _oa 2>/dev/null < "$PROCD/$_op/oom_score_adj" || continue
        [ "$_oa" = "$OOM_PROT" ] && echo 700 > "$PROCD/$_op/oom_score_adj" 2>/dev/null
    done
    return 0
}
# game still on screen without being the focused app (floating window, split-screen, PiP): adj <= 100 (visible)
game_alive() {
    for _gp in $(pidof "$1" 2>/dev/null); do
        read -r _ga 2>/dev/null < "$PROCD/$_gp/oom_score_adj" || continue
        [ "$_ga" -le 100 ] 2>/dev/null && return 0
    done
    return 1
}
boost_game() {
    prio_on "$1"
    gm_on "$1"
    echo "$1" > "$D/game_boosted"
}
unboost_game() {
    gm_off
    _bp=$(cat "$D/game_boosted" 2>/dev/null)
    oom_unprotect "$_bp"
    [ -n "$_bp" ] || return 0
    case "$PRIOS" in *"
$_bp
"*) rm -f "$D/game_boosted"; return 0 ;; esac
    prio_off "$_bp"
    grep -qxF "$_bp" "$RESF" 2>/dev/null && restrict_on "$_bp"
    rm -f "$D/game_boosted"
}

GAO_BK="$D/orig_appops_game"; GAO_PID=""
game_appops_on() {
    [ -s "$D/appops_game.cfg" ] && [ ! -f "$D/no_game_appops" ] || return 0
    ( AO_BK="$GAO_BK" AO_SKIP="$1" AO_MAX=10 sh "$_bd/appops_apply.sh" "$D/appops_game.cfg" "$D/appops_game.log" game ) >/dev/null 2>&1 &
    GAO_PID=$!
}
game_appops_off() {
    [ -f "$GAO_BK" ] || [ -n "$GAO_PID" ] || return 0
    _gp=$GAO_PID; GAO_PID=""
    _gr() {
        _n=0; while [ -n "$_gp" ] && kill -0 "$_gp" 2>/dev/null && [ "$_n" -lt 40 ]; do sleep 1; _n=$((_n + 1)); done
        sh "$_bd/appops_apply.sh" restore "$GAO_BK"
    }
    if [ "$1" = sync ]; then _gr >/dev/null 2>&1; else ( _gr ) >/dev/null 2>&1 & fi
}

log() {
    if [ -f "$LOG" ] && [ "$(wc -c < "$LOG")" -gt $LOG_MAX ]; then
        tail -n 200 "$LOG" > "$LOG.tmp" && mv "$LOG.tmp" "$LOG"
    fi
    echo "$(date +%H:%M:%S) $*" >> "$LOG"
}

run_fstrim() {
    _t0=$(date +%s)
    sm fstrim > "$D/fstrim.out" 2>&1 </dev/null; _rc=$?
    if [ "$_rc" != 0 ] && [ "$IS_ROOT" = 1 ]; then
        fstrim -v /data > "$D/fstrim.out" 2>&1 </dev/null; _rc=$?
    fi
    _fo=$(head -c 200 "$D/fstrim.out" 2>/dev/null | tr '\n' ' ')
    log "fstrim rc=$_rc $(( $(date +%s) - _t0 ))s ${_fo}"
}
FSTRIM_PID=""
maybe_fstrim() {
    [ -f "$D/no_fstrim" ] && return 0
    [ $((now - FSTRIM_LAST)) -ge "$FSTRIM_GAP" ] || return 0
    _fl=$(cat "$FSTRIM_F" 2>/dev/null); case "$_fl" in ''|*[!0-9]*) _fl=0 ;; esac
    if [ "$_fl" -gt "$FSTRIM_LAST" ] && [ "$_fl" -le "$now" ]; then FSTRIM_LAST=$_fl; return 0; fi
    [ -n "$FSTRIM_PID" ] && kill -0 "$FSTRIM_PID" 2>/dev/null && return 0
    FSTRIM_LAST=$now; echo "$now" > "$FSTRIM_F"
    run_fstrim &
    FSTRIM_PID=$!
}

daemon_pid() {
    _dp=$(cat "$PIDF" 2>/dev/null)
    [ -n "$_dp" ] && tr '\0' ' ' 2>/dev/null < "/proc/$_dp/cmdline" | grep -q ram_daemon && echo "$_dp"
}

[ "$1" = stop ] && { _sp=$(daemon_pid); [ -n "$_sp" ] && kill "$_sp" 2>/dev/null; rm -f "$PIDF"; load_games; unboost_game; game_appops_off sync; prio_each prio_off; restrict_each restrict_off; revert_am; exit 0; }

read_avail() {
    _t=0; _a=0
    while read -r _k _v _; do
        case "$_k" in MemTotal:) _t=$_v ;; MemAvailable:) _a=$_v; break ;; esac
    done < /proc/meminfo
    if [ "$_t" -gt 0 ] 2>/dev/null; then AVAIL=$((_a * 100 / _t)); else AVAIL=100; fi
}
read_psi() {
    PSI=0
    [ -r /proc/pressure/memory ] || return 0
    _a=""; read -r _k _a _ 2>/dev/null < /proc/pressure/memory
    _v=${_a#avg10=}
    case "$_v" in ''|*[!0-9.]*) return 0 ;; esac
    _i=${_v%%.*}; _f=${_v#*.}; [ "$_f" = "$_v" ] && _f=0
    PSI=$((_i * 10 + ${_f%"${_f#?}"}))
}
avail_pct() { read_avail; echo "$AVAIL"; }

pick_game() {
    GAME_PKG=""
    for _n in $FG_TOP; do
        case "$GAMES" in *"
$_n
"*) GAME_PKG=$_n; break ;; esac
    done
    return 0
}
fg_top_fast() {
    [ -r "$TOPCS" ] || return 1
    FG_TOP=" "
    while read -r _tp; do
        read -r _adj 2>/dev/null < "$PROCD/$_tp/oom_score_adj" || continue
        case "$_adj" in 0|"$OOM_PROT") ;; *) continue ;; esac
        _n=""; read -r _n 2>/dev/null < "$PROCD/$_tp/cmdline"
        _n=${_n%%[!a-zA-Z0-9_.]*}
        case "$_n" in *.*) FG_TOP="$FG_TOP$_n " ;; esac
    done < "$TOPCS"
    [ "$FG_TOP" = " " ] && return 1
    pick_game
}
FULL_TICK=""; CAND_KEY=""
ensure_full() { [ "$FULL_TICK" = "$now" ] || fg_scan; }
fg_scan() {
    FG_PKGS=" "; FG_TOP=" "; GAME_PKG=""; _cs_ok=0
    for _p in "$PROCD"/[0-9]*; do
        read -r _adj 2>/dev/null < "$_p/oom_score_adj" || continue
        case "$_adj" in 0|"$OOM_PROT") ;; *) continue ;; esac
        _n=""; read -r _n 2>/dev/null < "$_p/cmdline"
        _n=${_n%%[!a-zA-Z0-9_.]*}
        case "$_n" in ''|*.*) ;; *) continue ;; esac
        [ -n "$_n" ] || continue
        FG_PKGS="$FG_PKGS$_n "
        _cs=""; read -r _cs 2>/dev/null < "$_p/cpuset"
        if [ -n "$_cs" ]; then
            _cs_ok=1
            case "$_cs" in */top-app*) FG_TOP="$FG_TOP$_n " ;; esac
        fi
    done
    [ "$_cs_ok" = 0 ] && FG_TOP=$FG_PKGS
    pick_game
    FULL_TICK=$now
}

protected() {
    case "$1" in
        android|com.android.systemui|com.android.phone|com.android.settings|com.android.providers*|\
        com.google.android.gms*|com.google.android.gsf*|*inputmethod*|*keyboard*|*launcher*|*Launcher*|\
        *dialer*|*telecom*|*axeron*|*shizuku*) return 0 ;;
    esac
    case "$1" in
        com.topjohnwu.magisk*|*magisk*|*kernelsu*|*ksunext*|*sukisu*|*apatch*|*lsposed*|*shizuku*|*dhizuku*|\
        com.rosan.*|com.termux*|*brevent*|com.android.shell) return 0 ;;
    esac
    if [ "$2" = 1 ]; then
        case "$1" in com.android.*|com.mediatek*|com.qualcomm*|com.transsion.*|com.google.android.ext*|\
        com.google.android.permissioncontroller|com.google.android.packageinstaller) return 0 ;; esac
    fi
    case "$FG_PKGS" in *" $1 "*) return 0 ;; esac
    case "$PRIOS" in *"
$1
"*) return 0 ;; esac
    case "$WLS" in *"
$1
"*) return 0 ;; esac
    case "$REGS" in *"
$1
"*) [ "$2" = 1 ] || return 0 ;; esac
    return 1
}

build_cands() {
    [ "$CAND_KEY" = "$1/$2/$now" ] && return 0
    CAND_KEY="$1/$2/$now"
    : > "$D/cand.tmp"
    for _p in "$PROCD"/[0-9]*; do
        read -r _adj 2>/dev/null < "$_p/oom_score_adj" || continue
        [ "$_adj" -ge "$1" ] 2>/dev/null || continue
        if [ "$_adj" -ge 600 ] && [ "$_adj" -lt 700 ]; then continue; fi
        _n=""; read -r _n 2>/dev/null < "$_p/cmdline"
        _n=${_n%%[!a-zA-Z0-9_.:]*}
        case "$_n" in ''|*[!a-zA-Z0-9_.:]*) continue ;; esac
        _pk=${_n%%:*}
        case "$_pk" in *.*) ;; *) continue ;; esac
        protected "$_pk" "$2" && continue
        _rss=0; _rp=0
        read -r _ _rp _ 2>/dev/null < "$_p/statm" && _rss=$((_rp * PAGE_KB))
        echo "${_p##*/} $_n $_pk $_rss" >> "$D/cand.tmp"
    done
}

rank_cands() {
    [ -s "$RGF" ] || echo "0 #v1" > "$RGF"
    awk -v now="$now" '
        FNR==1 { fi++ }
        fi==1 { if ($2 !~ /^[-#]/) { if (!($2 in last) || $1 > last[$2]) last[$2]=$1; cnt[$2]++ } next }
        fi==2 { if ($2 !~ /^#/ && now - $1 < 86400) rg[$2]++; next }
        { rss[$3] += $4 }
        END { for (p in rss) { idle = (p in last) ? (now - last[p]) / 60 : 720; if (idle < 0) idle = 0
              printf "%d %s %d\n", rss[p] * (1 + idle / 30) / (1 + cnt[p] / 5 + 2 * rg[p]), p, rss[p] } }
    ' "$D/usage.log" "$RGF" "$D/cand.tmp" | sort -rn > "$D/rank.tmp"
}

kill_until() {
    _kn=0; : > "$D/last_kill"
    while read -r _s _pk _rss; do
        [ "$_kn" -ge "$2" ] && break
        am kill --user 0 "$_pk" >/dev/null 2>&1 </dev/null
        regret_note_kill "$_pk"
        _kn=$((_kn + 1))
        echo "$_pk($((_rss / 1024))M)" >> "$D/last_kill"
        sleep 0.4
        read_avail
        [ "$AVAIL" -ge "$1" ] && break
    done < "$D/rank.tmp"
    KILLED=$_kn
    [ "$_kn" -gt 0 ] && CAND_KEY=""
}
reclaim() {
    build_cands "$2" "$3"
    rank_cands
    kill_until "$1" "$4"
}

trim_top() {
    TRIMN=0
    sort -k4,4 -rn "$D/cand.tmp" > "$D/trim.tmp"
    while read -r _pid _name _pk _rss; do
        [ "$TRIMN" -ge "$1" ] && break
        case "$TRIMMED" in *" $_name "*) continue ;; esac
        am send-trim-memory --user 0 "$_pid" COMPLETE >/dev/null 2>&1 </dev/null
        TRIMMED="$TRIMMED$_name "
        TRIMN=$((TRIMN + 1))
    done < "$D/trim.tmp"
}
TRIMMED=" "

deep_clean() {
    sync
    [ "$NOSWAP" = 0 ] && [ "${HEAT:-0}" = 0 ] && cmd activity compact system >/dev/null 2>&1 </dev/null
}

grep -q '^0 #v2' "$D/usage.log" 2>/dev/null || printf '0 -\n0 #v2\n' > "$D/usage.log"

if [ "$1" = sessions ]; then
    sessions_report "$2"
    exit 0
fi

if [ "$1" = info ]; then
    read_avail; read_psi; load_games; fg_scan
    _fl=$(cat "$FSTRIM_F" 2>/dev/null); case "$_fl" in ''|*[!0-9]*) _fla=never ;; *) _fla="$(( ($(date +%s) - _fl) / 60 ))m ago" ;; esac
    echo "mem=${MEM_MB}MB tier=${TIER}GB noswap=${NOSWAP} memfactor=${MEMFACTOR:-default} maxcached=${MAXCACHED} rerank=${REORANK} usap=$([ -f "$D/no_usap" ] && echo off || echo "${USAP_MIN}-${USAP_MAX}/${USAP_THR}/${USAP_DLY}ms")"
    echo "lmkd_tune=$([ -f "$D/lmkd_tune" ] && { zram_mod_on && echo skipped-zram || echo on; } || echo off) prio=$(grep -hv "^#" "$PRIOF" 2>/dev/null | grep -c .) restrict=$(grep -hv "^#" "$RESF" 2>/dev/null | grep -c .) oom_prot=$([ "$IS_ROOT" = 1 ] && [ ! -f "$D/no_oom_protect" ] && echo on || echo off) game_guard=$([ -f "$D/no_gamemode" ] && echo off || echo on)/$(grep -c . "$GL_BASE" 2>/dev/null)games"
    echo "lv1=${LV1_AVAIL}% lv2=${LV2_AVAIL}% deep<${DEEP_AVAIL}% off=${OFF_AVAIL}% psi=${LV1_PSI}/${LV2_PSI} cd=${COOLDOWN}s adj>=${CACHED_ADJ} fstrim=$([ -f "$D/no_fstrim" ] && echo off || echo "${_fh}h last=${_fla}")"
    echo "live: avail=${AVAIL}% psi_avg10=$((PSI / 10)).$((PSI % 10)) top=[${FG_TOP# }] all_adj0=[${FG_PKGS# }]"
    zram_info
    heat_info
    regret_info
    exit 0
fi

if [ "$1" = prio ]; then
    case "$2" in
        add)
            case "$3" in ''|*[!a-zA-Z0-9_.]*) echo "invalid package"; exit 1 ;; esac
            pm path "$3" >/dev/null 2>&1 || { echo "not installed: $3"; exit 1; }
            if grep -qxF "$3" "$RESF" 2>/dev/null; then
                restrict_off "$3"; grep -vxF "$3" "$RESF" > "$RESF.tmp"; mv "$RESF.tmp" "$RESF"
                echo "(removed from restrict list)"
            fi
            grep -qxF "$3" "$PRIOF" 2>/dev/null || echo "$3" >> "$PRIOF"
            prio_on "$3"; echo "priority ON: $3" ;;
        rm)
            case "$3" in ''|*[!a-zA-Z0-9_.]*) echo "invalid package"; exit 1 ;; esac
            prio_off "$3"
            [ -f "$PRIOF" ] && { grep -vxF "$3" "$PRIOF" > "$PRIOF.tmp"; mv "$PRIOF.tmp" "$PRIOF"; }
            echo "priority OFF: $3" ;;
        *) cat "$PRIOF" 2>/dev/null ;;
    esac
    exit 0
fi

if [ "$1" = restrict ]; then
    load_games; FG_PKGS=" "
    case "$2" in
        add)
            case "$3" in ''|*[!a-zA-Z0-9_.]*) echo "invalid package"; exit 1 ;; esac
            pm path "$3" >/dev/null 2>&1 || { echo "not installed: $3"; exit 1; }
            case "$PRIOS" in *"
$3
"*) echo "refused: $3 is in the priority list (prio rm $3 first)"; exit 1 ;; esac
            if protected "$3" 1; then echo "refused: $3 is protected (system / manager / launcher / whitelist)"; exit 1; fi
            grep -qxF "$3" "$RESF" 2>/dev/null || echo "$3" >> "$RESF"
            restrict_on "$3"; echo "restricted: $3" ;;
        rm)
            case "$3" in ''|*[!a-zA-Z0-9_.]*) echo "invalid package"; exit 1 ;; esac
            restrict_off "$3"
            [ -f "$RESF" ] && { grep -vxF "$3" "$RESF" > "$RESF.tmp"; mv "$RESF.tmp" "$RESF"; }
            echo "unrestricted: $3" ;;
        *) cat "$RESF" 2>/dev/null ;;
    esac
    exit 0
fi

if [ "$1" = fstrim ]; then
    date +%s > "$FSTRIM_F"; run_fstrim
    echo "fstrim rc=$_rc ${_fo}"; exit 0
fi

if [ "$1" = once ] && [ "$2" = game ]; then
    now=$(date +%s)
    load_games; fg_scan; read_avail; b=$AVAIL
    build_cands 500 0
    : > "$D/last_kill"; _kn=0; _seen=" "
    while read -r _pid _name _pk _rss; do
        case "$GAMES" in *"
$_pk
"*) ;; *) continue ;; esac
        case "$_seen" in *" $_pk "*) continue ;; esac
        _seen="$_seen$_pk "
        am kill --user 0 "$_pk" >/dev/null 2>&1 </dev/null
        _kn=$((_kn + 1)); echo "$_pk" >> "$D/last_kill"
    done < "$D/cand.tmp"
    sleep 1; read_avail
    echo "Games closed: ${_kn} ($(tr '\n' ' ' < "$D/last_kill")) | RAM avail ${b}% -> ${AVAIL}%"
    [ "$_kn" = 0 ] && echo "No game left in background (a game in front, or in the priority list, is never closed)."
    log "once game: ${b}->${AVAIL}% closed=$_kn"
    exit 0
fi

if [ "$1" = once ]; then
    now=$(date +%s)
    load_games; fg_scan; read_avail; b=$AVAIL
    build_cands "$CACHED_ADJ" 0; trim_top 10
    reclaim 60 "$CACHED_ADJ" 0 30
    deep_clean; read_avail
    echo "RAM avail: ${b}% -> ${AVAIL}% (killed ${KILLED}: $(tr '\n' ' ' < "$D/last_kill"))"
    log "once: ${b}->${AVAIL}% killed=$KILLED"
    exit 0
fi

enter_game() {
        [ -n "$GAMING" ] && { session_end; unboost_game; game_appops_off sync; }
    GAMING=$1; g_cand=""; g_miss=0
    boost_game "$1"
    oom_protect "$1"
    game_appops_on "$1"
    read_avail; _b=$AVAIL
    ZU=0; _tgt=$GAME_SWEEP_AVAIL; _zt=""
    if [ "$NOSWAP" = 0 ] && [ "$ZR_AWARE" = 1 ]; then
        read_zused
        # swap still roomy: cached apps can be compressed instead of killed -> lower kill target
        if [ "$ZU" -lt "$ZR_COMPACT" ]; then _tgt=$((GAME_SWEEP_AVAIL - 15)); _zt=" zram=${ZU}%(soft)"
        else _zt=" zram=${ZU}%(full)"; fi
    fi
    # per-game history (game_stats.sh): tight games get the full sweep, roomy ones a lighter one
    learn_class "$1"
    case "$LEARN" in
        heavy) _tgt=$GAME_SWEEP_AVAIL ;;
        light) [ "$ZU" -lt "$ZR_COMPACT" ] && _tgt=$((GAME_SWEEP_AVAIL - 25)) ;;
    esac
    _zt="$_zt learn=$LEARN"; [ "$HEAT" -gt 0 ] && _zt="$_zt heat=$HEAT"
    session_start "$1"
    if [ "$_b" -lt "$_tgt" ]; then
        ensure_full
        build_cands "$CACHED_ADJ" 0; trim_top 8
        reclaim "$_tgt" "$CACHED_ADJ" 0 20
        sync
        log "GAME $1: sweep avail ${_b}%->${AVAIL}% tgt=${_tgt}%${_zt} killed=$KILLED [$(tr '\n' ' ' < "$D/last_kill")]"
        last_kill=$now
    else
        log "GAME $1: enter avail=${_b}% tgt=${_tgt}%${_zt} (no sweep)"
    fi
}
exit_game() {
    session_end
    unboost_game
    game_appops_off
    log "GAME exit $GAMING"
    GAMING=""; g_miss=0
}

[ -n "$(daemon_pid)" ] && exit 0
echo $$ > "$PIDF"
trap 'if [ "$(cat "$PIDF" 2>/dev/null)" = "$$" ]; then session_end; unboost_game; game_appops_off sync; rm -f "$PIDF"; fi; exit 0' TERM INT

zram_wait; swap_refresh; load_zr_cfg; heat_load_cfg; regret_load
apply_am
load_games
unboost_game
game_appops_off sync

prio_each prio_on
restrict_each restrict_on

log "daemon start pid=$$ $(sh "$0" info 2>/dev/null | head -n2 | tr '\n' ' ')"
last_kill=0; cd_s=$COOLDOWN; int=$BASE_INT
now=$(date +%s)
GAMING=""; g_cand=""; g_miss=0; ul_tick=0; l2_hit=0; prot_epoch=0; PREV_FG=" "; trim_epoch=0; FG_PKGS=" "
gl_epoch=$now; pr_epoch=$now; l1_epoch=0; calm=0
FSTRIM_LAST=$(cat "$FSTRIM_F" 2>/dev/null); case "$FSTRIM_LAST" in ''|*[!0-9]*) FSTRIM_LAST=$now ;; esac
[ "$FSTRIM_LAST" -gt "$now" ] && FSTRIM_LAST=$now

while :; do
    now=$(date +%s)
    read_avail; read_psi; av=$AVAIL; psi=$PSI
    heat_tick
    fg_top_fast || fg_scan
    if [ "$FG_TOP" != " " ]; then on=1; else on=0; fi
    [ $((now - gl_epoch)) -ge 60 ] && { gl_epoch=$now; load_games; swap_refresh; load_zr_cfg; heat_load_cfg; regret_load; }
    [ $((now - pr_epoch)) -ge 600 ] && { pr_epoch=$now; apply_am; prio_each prio_on; restrict_each restrict_on; regret_trim; }
    [ $((now - trim_epoch)) -ge "$TRIM_GAP" ] && { TRIMMED=" "; trim_epoch=$now; }

    if [ "$FG_TOP" != "$PREV_FG" ]; then
        for _f in $FG_TOP; do
            case "$PREV_FG" in *" $_f "*) continue ;; esac
            case "$_f" in *launcher*|*Launcher*|com.android.systemui) continue ;; esac
            regret_check "$_f"
            echo "$now $_f" >> "$D/usage.log"
        done
        PREV_FG=$FG_TOP; calm=0
        ul_tick=$((ul_tick + 1))
        if [ "$ul_tick" -ge 200 ]; then
            ul_tick=0
            if [ "$(wc -l < "$D/usage.log")" -gt 3000 ]; then
                tail -n 1500 "$D/usage.log" > "$D/usage.tmp" && mv "$D/usage.tmp" "$D/usage.log"
            fi
        fi
    fi

    if [ "$on" = 1 ] && [ -n "$GAME_PKG" ] && [ ! -f "$D/no_gamemode" ]; then
        g_miss=0
        if [ "$GAMING" != "$GAME_PKG" ]; then
            if [ "$g_cand" = "$GAME_PKG" ]; then enter_game "$GAME_PKG"; av=$AVAIL; else g_cand=$GAME_PKG; fi
        elif [ $((now - prot_epoch)) -ge 15 ]; then prot_epoch=$now; oom_protect "$GAME_PKG"
        fi
    else
        g_cand=""
        if [ -n "$GAMING" ]; then
            if [ ! -f "$D/no_gamemode" ] && game_alive "$GAMING"; then
                g_miss=0   # still visible behind a floating window / split / PiP: game is not closed
                [ $((now - prot_epoch)) -ge 15 ] && { prot_epoch=$now; oom_protect "$GAMING"; }
            else
                g_miss=$((g_miss + 1))
                [ "$g_miss" -ge 3 ] && exit_game
            fi
        fi
    fi

    lvl=0
    if [ "$av" -lt $LV2_AVAIL ] || [ "$psi" -ge $((LV2_PSI * 10)) ]; then lvl=2
    elif [ "$on" = 0 ] && [ "$av" -lt $OFF_AVAIL ]; then lvl=2
    elif [ "$av" -lt $LV1_AVAIL ] || [ "$psi" -ge $((LV1_PSI * 10)) ]; then lvl=1; fi
    if [ -n "$GAMING" ] && [ "$on" = 1 ]; then
        if [ "$av" -lt $DEEP_AVAIL ] || [ "$psi" -ge 250 ]; then lvl=3; else lvl=0; fi
    fi
    if [ "$NOSWAP" = 0 ] && [ "$ZR_AWARE" = 1 ]; then
        read_zused
        if [ -n "$GAMING" ] && [ "$on" = 1 ]; then
            # in game: swap saturated AND RAM low = real emergency
            [ "$ZU" -ge "$ZR_SAT" ] && [ "$av" -lt "$LV2_AVAIL" ] && lvl=3
        elif [ "$ZU" -ge "$ZR_SAT" ]; then
            [ "$lvl" -lt 1 ] && lvl=1                       # swap saturated: act earlier (trim)
        elif [ "$ZU" -le "$ZR_LOW" ] && [ "$lvl" = 1 ] && [ "$psi" -lt $((LV2_PSI * 10)) ]; then
            lvl=0                                           # swap roomy: moderate PSI is just zram working, hold off
        fi
    fi
    # kill level needs 2 consecutive samples (guards against transient dips right after compaction/drop_caches)
    if [ "$lvl" = 2 ] && [ "$av" -ge "$DEEP_AVAIL" ]; then
        l2_hit=$((l2_hit + 1)); [ "$l2_hit" -lt 2 ] && lvl=1
    else
        l2_hit=0
    fi
    if [ -n "$GAMING" ]; then
        session_tick
        if [ "$HEAT" = 2 ] && [ $((now - heat_note)) -ge 900 ]; then
            heat_note=$now; log "HEAT hot while gaming: $((BT / 10)).$((BT % 10))C"
            [ -f "$D/no_heat_notify" ] || { notify transsion_optimizer "$(heat_msg "$((BT / 10))")" "Transsion Optimizer" >/dev/null 2>&1 & }
        fi
    fi

    if [ "$lvl" -ge 1 ]; then
        int=$FAST_INT; calm=0
        if [ "$lvl" -lt 3 ] && [ "$HEAT" -lt 2 ] && [ $((now - l1_epoch)) -ge 20 ]; then   # hot: no optional trim
            l1_epoch=$now; ensure_full
            build_cands "$CACHED_ADJ" 0; trim_top 6
            [ "$TRIMN" -gt 0 ] && log "L1 avail=${av}% psi=${psi} trimmed=$TRIMN"
        fi
        eff_cd=$cd_s; [ "$av" -lt "$DEEP_AVAIL" ] && [ "$eff_cd" -gt 45 ] && eff_cd=45
        if [ "$lvl" -ge 2 ] && [ $((now - last_kill)) -ge "$eff_cd" ]; then
            tgt=$LV1_AVAIL; [ "$on" = 0 ] && tgt=$OFF_AVAIL; [ "$lvl" = 3 ] && tgt=$LV2_AVAIL
            tag=L2; [ "$lvl" = 3 ] && tag="GAME-EMERG"
            ensure_full
            reclaim "$tgt" "$CACHED_ADJ" 0 12
            names="$(tr '\n' ' ' < "$D/last_kill")"; kn=$KILLED
            if [ "$AVAIL" -lt "$DEEP_AVAIL" ]; then
                reclaim "$LV2_AVAIL" 500 1 6
                names="$names | deep: $(tr '\n' ' ' < "$D/last_kill")"; kn=$((kn + KILLED)); tag="$tag+DEEP"
            fi
            [ "$on" = 0 ] && [ "$kn" -gt 0 ] && deep_clean
            read_avail; na=$AVAIL
            log "$tag avail=${av}%->${na}% psi=${psi} killed=$kn [$names] screen=$on cd=${cd_s}s"
            last_kill=$now
            if [ $((na - av)) -lt 2 ]; then
                cd_s=$((cd_s * 2)); [ "$cd_s" -gt 300 ] && cd_s=300
            else
                cd_s=$COOLDOWN
            fi
        fi
    else
        int=$BASE_INT; cd_s=$COOLDOWN
        calm=$((calm + 1))
        if [ "$av" -ge $((LV1_AVAIL + 15)) ] && [ "$psi" -lt 5 ]; then
            if [ "$calm" -ge 24 ]; then int=15; elif [ "$calm" -ge 6 ]; then int=10; fi
        fi
        if [ -n "$GAMING" ]; then int=$GAME_INT; [ "$HEAT" = 2 ] && int=$((GAME_INT + 4)); fi   # hot: poll less
    fi
    [ "$on" = 0 ] && [ "$lvl" = 0 ] && int=$OFF_INT
    [ "$on" = 0 ] && [ "$lvl" = 0 ] && [ -z "$GAMING" ] && [ "$HEAT" = 0 ] && maybe_fstrim
    sleep "$int"
done
