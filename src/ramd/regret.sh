#!/system/bin/sh
# Kill-regret learning. SOURCE this file (. regret.sh).
# If an app the daemon just killed is opened again within REGRET_WIN seconds, that kill was a mistake ("regret").
#   - every regret makes that app rank lower for the next kills (24 h window, see rank_cands)
#   - REGRET_PROT_N regrets within 24 h -> the app is skipped by the normal kill/trim pass (deep pass can still take it)
# Cheap on purpose: recent kills live in a shell variable, a check is one string match per app switch, the only
# extra fork is one awk per minute. Nothing runs on a timer of its own. USAP pool / zygote are not touched.
# Off: touch $D/no_regret

RGF=$D/regret.log             # "epoch pkg" per regret (first line is a "0 #v1" marker)
RKILL=""                      # "|pkg=epoch" for kills of the last few minutes (memory only)
REGS=""                       # newline list of regret-protected packages
REGRET_WIN=90; REGRET_PROT_N=3; REGRET_DAY=86400

regret_note_kill() { # $1 = package that was just killed
    [ -f "$D/no_regret" ] && return 0
    [ "${#RKILL}" -gt 2000 ] && RKILL=""
    RKILL="$RKILL|$1=$now"
}
regret_check() { # $1 = package that just came to the front
    [ -n "$RKILL" ] || return 0
    case "$RKILL" in *"|$1="*) ;; *) return 0 ;; esac
    _rt=${RKILL##*"|$1="}; _rt=${_rt%%|*}              # most recent kill of this package
    case "$_rt" in ''|*[!0-9]*) return 0 ;; esac
    _rd=$((now - _rt))
    if [ "$_rd" -ge 0 ] && [ "$_rd" -le "$REGRET_WIN" ]; then
        echo "$now $1" >> "$RGF"
        log "REGRET $1 reopened ${_rd}s after kill"
    fi
}
regret_load() { # every ~60 s
    REGS=""
    [ -f "$D/no_regret" ] && return 0
    [ -s "$RGF" ] || return 0
    REGS="
$(awk -v now="$now" -v win="$REGRET_DAY" -v n="$REGRET_PROT_N" '$1 + win > now { c[$2]++ } END { for (p in c) if (c[p] >= n) print p }' "$RGF" 2>/dev/null)
"
}
regret_trim() { # every ~10 min
    [ "$(wc -l < "$RGF" 2>/dev/null)" -gt 200 ] && tail -n 100 "$RGF" > "$RGF.tmp" && mv "$RGF.tmp" "$RGF"
    return 0
}
regret_info() {
    now=$(date +%s); regret_load
    _rn=$(awk -v now="$now" '$1 + 86400 > now && $2 !~ /^#/ { c++ } END { print c + 0 }' "$RGF" 2>/dev/null)
    echo "regret: $([ -f "$D/no_regret" ] && echo off || echo "24h=${_rn:-0} protected=[$(echo $REGS)]")"
}
