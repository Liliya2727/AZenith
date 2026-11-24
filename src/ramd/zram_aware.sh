#!/system/bin/sh
# ZRAM-aware helpers. SOURCE this file (. zram_aware.sh), do not execute it.
# READ-ONLY toward swap/vm: never writes zram or /proc/sys/vm nodes (owner = the "ZRAM" module).
# Works without that module too: swap signals come from /proc/meminfo.
# Flags/config in $D:  no_zram_aware (off)  |  zram_thr.cfg: ZR_SAT=85 ZR_LOW=40 ZR_COMPACT=70

ZRAM_MOD=/data/adb/modules/zram
ZRAM_LOG=/data/adb/zram/zram.log
MEMINFO=${MEMINFO:-/proc/meminfo}
ZR_AWARE=1; ZR_SAT=85; ZR_LOW=40; ZR_COMPACT=70; ZU=0

zram_mod_on() { [ -d "$ZRAM_MOD" ] && [ ! -f "$ZRAM_MOD/disable" ] && [ ! -f "$ZRAM_MOD/remove" ]; }

# thresholds from $D/zram_thr.cfg (numbers 0-100 only; invalid -> defaults)
load_zr_cfg() {
    ZR_SAT=85; ZR_LOW=40; ZR_COMPACT=70
    [ -f "$D/no_zram_aware" ] && ZR_AWARE=0 || ZR_AWARE=1
    [ -f "$D/zram_thr.cfg" ] || return 0
    for _k in ZR_SAT ZR_LOW ZR_COMPACT; do
        _v=$(grep -m1 "^$_k=" "$D/zram_thr.cfg" 2>/dev/null | cut -d= -f2 | tr -d ' \r')
        case "$_v" in ''|*[!0-9]*) ;; *) [ "$_v" -le 100 ] && eval "$_k=$_v" ;; esac
    done
    [ "$ZR_LOW" -lt "$ZR_SAT" ] || { ZR_SAT=85; ZR_LOW=40; }
}

# Wait (max 100 s) until the ZRAM module has written its log THIS boot, so SwapTotal is final.
zram_wait() {
    zram_mod_on || return 0
    _bt=$(( $(date +%s) - $(cut -d. -f1 /proc/uptime) )); _w=0
    while [ "$_w" -lt 100 ]; do
        _lm=$(stat -c %Y "$ZRAM_LOG" 2>/dev/null)
        [ "${_lm:-0}" -ge "$_bt" ] && break
        sleep 5; _w=$((_w + 5))
    done
    sleep 2
    log "zram_wait: ${_w}s"
}

# swap usage in % -> ZU (0 when there is no swap)
read_zused() {
    ZU=0; [ "$NOSWAP" = 0 ] || return 0
    _st=0; _sf=0
    while read -r _k _v _; do
        case "$_k" in SwapTotal:) _st=$_v ;; SwapFree:) _sf=$_v; break ;; esac
    done < "$MEMINFO"
    [ "$_st" -gt 0 ] 2>/dev/null && ZU=$(( (_st - _sf) * 100 / _st ))
}

# swap appeared/disappeared after daemon start (e.g. ZRAM module reset at boot+30s): re-base thresholds
swap_refresh() {
    _s=$(awk '/^SwapTotal/{print $2}' "$MEMINFO" 2>/dev/null)
    [ "${_s:-0}" -gt 0 ] 2>/dev/null && _n=0 || _n=1
    [ "$_n" = "$NOSWAP" ] && return 0
    if [ "$_n" = 0 ]; then _k=-1; else _k=1; fi
    LV1_AVAIL=$((LV1_AVAIL + _k * 4)); LV2_AVAIL=$((LV2_AVAIL + _k * 3)); OFF_AVAIL=$((OFF_AVAIL + _k * 3))
    DEEP_AVAIL=$((LV2_AVAIL / 2)); NOSWAP=$_n; SWAP_KB=${_s:-0}
    log "swap changed: NOSWAP=$NOSWAP lv1=$LV1_AVAIL lv2=$LV2_AVAIL off=$OFF_AVAIL"
}

# " ratio=x3.3 real=278MB" (needs read access to zram0/mm_stat; empty otherwise)
zram_ratio() {
    [ -r /sys/block/zram0/mm_stat ] || return 0
    read -r _zo _zc _zm _ < /sys/block/zram0/mm_stat
    [ "${_zc:-0}" -gt 0 ] 2>/dev/null || return 0
    awk -v o="$_zo" -v c="$_zc" -v m="$_zm" 'BEGIN{printf " ratio=x%.1f real=%dMB", o/c, m/1048576}'
}
zram_info() {
    read_zused
    echo "zram: module=$(zram_mod_on && echo on || echo off) aware=$ZR_AWARE swap_used=${ZU}% sat/low/compact=${ZR_SAT}/${ZR_LOW}/${ZR_COMPACT}$(zram_ratio)"
}
