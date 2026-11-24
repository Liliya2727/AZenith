#!/system/bin/sh
# Game session recorder, per-game learning and heat awareness. SOURCE this file (. game_stats.sh).
# - Sessions: one line per game session in $D/game_sessions.log (last ~100 kept).
# - Learning: per-game running averages in $D/game_learn decide how hard the ENTRY sweep is
#   (heavy -> full sweep, light -> lighter, new/normal -> default). Global thresholds are never auto-changed.
# - Heat: only READS the battery temperature. No thermal/CPU/GPU control. It postpones optional work
#   (fstrim, compaction, L1 trim) and posts a notification while gaming when it gets hot.
# Flags/config in $D:  no_learn | no_heat_aware | no_heat_notify | heat.cfg (HEAT_WARM=40 HEAT_HOT=43, whole degrees C, 25-60)

SESSF=$D/game_sessions.log
LEARNF=$D/game_learn
BATT_T=${BATT_T:-/sys/class/power_supply/battery/temp}
HEAT_AWARE=1; HEAT_WARM=40; HEAT_HOT=43; HEAT=0; BT=0; heat_note=0
S_PKG=""; S_START=0; S_MINA=100; S_MAXZ=0; S_MAXPSI=0; S_EMERG=0; S_MAXT=0; S_LEARN=new; LEARN=new

# ---------------- heat ----------------
heat_load_cfg() {
    HEAT_WARM=40; HEAT_HOT=43
    HEAT_AWARE=1; [ -f "$D/no_heat_aware" ] && HEAT_AWARE=0
    [ -r "$BATT_T" ] || HEAT_AWARE=0
    [ -f "$D/heat.cfg" ] || return 0
    for _k in HEAT_WARM HEAT_HOT; do
        _v=$(grep -m1 "^$_k=" "$D/heat.cfg" 2>/dev/null | cut -d= -f2 | tr -d ' \r')
        case "$_v" in ''|*[!0-9]*) ;; *) [ "$_v" -ge 25 ] && [ "$_v" -le 60 ] && eval "$_k=$_v" ;; esac
    done
    [ "$HEAT_WARM" -lt "$HEAT_HOT" ] || { HEAT_WARM=40; HEAT_HOT=43; }
}
# BT = battery temperature in tenths of a degree; HEAT = 0 ok / 1 warm / 2 hot (2 degree hysteresis on the way down)
heat_tick() {
    [ "$HEAT_AWARE" = 1 ] || { HEAT=0; return 0; }
    read -r _bt < "$BATT_T" 2>/dev/null || return 0
    case "$_bt" in ''|*[!0-9]*) return 0 ;; esac
    [ "$_bt" -lt 100 ] && _bt=$((_bt * 10))      # some kernels report whole degrees
    BT=$_bt
    _w=$((HEAT_WARM * 10)); _h=$((HEAT_HOT * 10))
    _t=0; [ "$BT" -ge "$_w" ] && _t=1; [ "$BT" -ge "$_h" ] && _t=2
    if [ "$_t" -lt "$HEAT" ]; then
        case "$HEAT" in
            2) [ "$BT" -ge $((_h - 20)) ] && _t=2 ;;
            1) [ "$BT" -ge $((_w - 20)) ] && _t=1 ;;
        esac
    fi
    if [ "$_t" != "$HEAT" ]; then log "heat $HEAT->$_t temp=$((BT / 10)).$((BT % 10))C"; HEAT=$_t; fi
}
heat_msg() {
    case "$(getprop persist.sys.locale)" in
        id*|in*) echo "Suhu baterai $1°C saat main game. Pertimbangkan istirahat sebentar atau pindah ke tempat teduh." ;;
        *) echo "Battery is at $1°C while gaming. Consider a short break or some shade." ;;
    esac
}

# ---------------- learning ----------------
# awk helper: class from running averages (n sessions, a = avg min avail %, z = avg max zram %, e = emergency %)
LC_FN='function cls(n,a,z,e){ if (n<3) return "new"; if (e>=25 || a<lv2) return "heavy"; if (a>=lv1+10 && z<=lo && e<=5) return "light"; return "normal" }'
learn_class() { # -> LEARN=new|light|normal|heavy
    LEARN=new
    [ -f "$D/no_learn" ] && return 0
    LEARN=$(awk -F'|' -v p="$1" -v lv2="$LV2_AVAIL" -v lv1="$LV1_AVAIL" -v lo="$ZR_LOW" "$LC_FN"'
        $1==p { print cls($2,$3,$4,$5); f=1; exit }
        END { if (!f) print "new" }' "$LEARNF" 2>/dev/null)
    case "$LEARN" in new|light|normal|heavy) ;; *) LEARN=new ;; esac
}
learn_update() { # pkg min_avail max_zram emergency(0|100): moving average, 3/4 old + 1/4 new
    [ -f "$LEARNF" ] || : > "$LEARNF"
    awk -F'|' -v p="$1" -v a="$2" -v z="$3" -v e="$4" '
        $1==p { n=$2+1; if (n>99) n=99; printf "%s|%d|%d|%d|%d\n", p, n, int(($3*3+a)/4), int(($4*3+z)/4), int(($5*3+e)/4); f=1; next }
        { print }
        END { if (!f) printf "%s|1|%d|%d|%d\n", p, a, z, e }' "$LEARNF" > "$LEARNF.tmp" 2>/dev/null && mv "$LEARNF.tmp" "$LEARNF"
}

# ---------------- sessions ----------------
session_start() { # $1 = package ; AVAIL is current
    S_PKG=$1; S_START=$(date +%s); S_MINA=${AVAIL:-100}; S_MAXZ=0; S_MAXPSI=0; S_EMERG=0; S_MAXT=${BT:-0}; S_LEARN=$LEARN
}
session_tick() { # once per loop tick while gaming (needs av, psi, lvl, ZU, BT)
    [ "$av" -lt "$S_MINA" ] && S_MINA=$av
    [ "$ZU" -gt "$S_MAXZ" ] && S_MAXZ=$ZU
    [ "$psi" -gt "$S_MAXPSI" ] && S_MAXPSI=$psi
    [ "$lvl" = 3 ] && S_EMERG=$((S_EMERG + 1))
    [ "$BT" -gt "$S_MAXT" ] && S_MAXT=$BT
    return 0
}
session_end() {
    [ -n "$S_PKG" ] || return 0
    _p=$S_PKG; S_PKG=""
    _d=$(( $(date +%s) - S_START ))
    [ "$_d" -ge 60 ] || return 0                       # ignore short blips (accidental detection)
    _em=0; [ "$S_EMERG" -ge 2 ] && _em=100
    printf '%s %s dur=%ds avail_min=%d%% zram_max=%d%% psi_max=%d.%d emerg=%d temp_max=%d.%dC learn=%s\n' \
        "$(date '+%Y-%m-%d %H:%M')" "$_p" "$_d" "$S_MINA" "$S_MAXZ" "$((S_MAXPSI / 10))" "$((S_MAXPSI % 10))" \
        "$S_EMERG" "$((S_MAXT / 10))" "$((S_MAXT % 10))" "$S_LEARN" >> "$SESSF"
    [ "$(wc -l < "$SESSF" 2>/dev/null)" -gt 150 ] && tail -n 100 "$SESSF" > "$SESSF.tmp" && mv "$SESSF.tmp" "$SESSF"
    [ -f "$D/no_learn" ] || learn_update "$_p" "$S_MINA" "$S_MAXZ" "$_em"
    log "SESSION $_p ${_d}s avail_min=${S_MINA}% zram_max=${S_MAXZ}% emerg=$S_EMERG temp_max=$((S_MAXT / 10)).$((S_MAXT % 10))C learn=$S_LEARN"
}

# ---------------- reports ----------------
heat_info() {
    heat_load_cfg; heat_tick
    echo "heat: $([ "$HEAT_AWARE" = 1 ] && echo "state=$HEAT temp=$((BT / 10)).$((BT % 10))C thr=${HEAT_WARM}/${HEAT_HOT}" || echo off) | learn: $([ -f "$D/no_learn" ] && echo off || echo "$(grep -c . "$LEARNF" 2>/dev/null || echo 0) games")"
}
sessions_report() { # $1 = number of sessions to show
    echo "== last sessions ($SESSF)"
    tail -n "${1:-10}" "$SESSF" 2>/dev/null || echo "(none)"
    echo "== learned: pkg | sessions | avg min avail% | avg max zram% | emergency% | class"
    if [ -f "$LEARNF" ]; then
        awk -F'|' -v lv2="$LV2_AVAIL" -v lv1="$LV1_AVAIL" -v lo="$ZR_LOW" "$LC_FN"'
            { printf "%s | %s | %s | %s | %s | %s\n", $1, $2, $3, $4, $5, cls($2,$3,$4,$5) }' "$LEARNF"
    else echo "(none)"; fi
}
