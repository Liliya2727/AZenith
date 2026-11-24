#!/system/bin/sh

# Copyright 2024 Dcx400
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Shared helpers. SOURCE this file (. common.sh), do not execute it.
# Root gate: root-only tweaks check [ "$IS_ROOT" = 1 ]; non-root = shell uid 2000 via ADB.

D=/data/local/tmp/vixun
vixun="$D"
mkdir -p "$D" 2>/dev/null
IS_ROOT=0; [ "$(id -u)" = 0 ] && IS_ROOT=1

# setprop that logs refusals (denied.log is reset at every boot by service.sh)
sp() { setprop "$1" "$2" 2>/dev/null || echo "$1" >> "$D/denied.log"; }
# notify <tag> <text> [title] : post a notification. As root, post as shell uid 2000 first (root-posted ones are often dropped)
notify() {
    _nt=""; [ -n "$3" ] && _nt="-t '$3'"
    if [ "$(id -u)" = 0 ] && command -v su >/dev/null 2>&1; then
        su 2000 -c "cmd notification post $_nt '$1' '$2'" >/dev/null 2>&1 && return 0
        su -lp 2000 -c "cmd notification post $_nt '$1' '$2'" >/dev/null 2>&1 && return 0
    fi
    if [ -n "$3" ]; then cmd notification post -t "$3" "$1" "$2" >/dev/null 2>&1; else cmd notification post "$1" "$2" >/dev/null 2>&1; fi
}
# write to a sysfs/tracefs node only if it is writable
wr() { [ -w "$2" ] && echo "$1" > "$2"; }

# ---- backup / restore of Settings values (first original value is kept, once per key) ----
# bk_put <ns> <key> <value> : back up the original ("null" = key did not exist), then put
bk_put() {
    f="$D/orig/$1.$2"; mkdir -p "$D/orig" 2>/dev/null
    if [ ! -f "$f" ]; then
        v=$(settings get "$1" "$2" 2>/dev/null)
        echo "${v:-null}" > "$f"
    fi
    settings put "$1" "$2" "$3"
}
# bk_restore : put every backed-up key back (delete the key if it did not exist), then drop the backup
bk_restore() {
    for f in "$D"/orig/*; do
        [ -f "$f" ] || continue
        b=${f##*/}; ns=${b%%.*}; k=${b#*.}; v=$(cat "$f")
        case "$ns" in system|secure|global) ;; *) continue ;; esac
        if [ "$v" = null ] || [ -z "$v" ]; then settings delete "$ns" "$k"; else settings put "$ns" "$k" "$v"; fi
    done
    rm -rf "$D/orig"
}

# ---- display refresh rate ----
# Highest refresh rate among the physical display's supported modes.
# Do NOT use the current SurfaceFlinger rate: it can be 60/90/120 depending on the active mode.
# Prefer mode-list entries from dumpsys display, then SurfaceFlinger mode data. Prints nothing + returns 1 on failure.
get_max_display_hz() {
    _hz="$(dumpsys display 2>/dev/null | awk '
        /supportedModes|mSupportedModes|DisplayModeRecord|DisplayMode\{/ {
            line=$0
            while (match(line, /fps=[0-9]+([.][0-9]+)?/)) {
                value=substr(line, RSTART+4, RLENGTH-4)+0
                if (value > max) max=value
                line=substr(line, RSTART+RLENGTH)
            }
            while (match(line, /refreshRate=[0-9]+([.][0-9]+)?/)) {
                value=substr(line, RSTART+12, RLENGTH-12)+0
                if (value > max) max=value
                line=substr(line, RSTART+RLENGTH)
            }
        }
        END { if (max > 0) printf "%.0f", max }
    ' 2>/dev/null)"

    # Some vendor builds expose supported mode rates more clearly in SurfaceFlinger.
    if [ -z "$_hz" ] || [ "$_hz" -le 0 ] 2>/dev/null; then
        _hz="$(dumpsys SurfaceFlinger 2>/dev/null | awk '
            /mSupportedModes|supportedModes|DisplayModeRecord|DisplayMode\{/ {
                line=$0
                while (match(line, /fps=[0-9]+([.][0-9]+)?/)) {
                    value=substr(line, RSTART+4, RLENGTH-4)+0
                    if (value > max) max=value
                    line=substr(line, RSTART+RLENGTH)
                }
                while (match(line, /refreshRate=[0-9]+([.][0-9]+)?/)) {
                    value=substr(line, RSTART+12, RLENGTH-12)+0
                    if (value > max) max=value
                    line=substr(line, RSTART+RLENGTH)
                }
            }
            END { if (max > 0) printf "%.0f", max }
        ' 2>/dev/null)"
    fi

    case "$_hz" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ "$_hz" -ge 30 ] && [ "$_hz" -le 1000 ] || return 1
    printf '%s' "$_hz"
}

# Max refresh rate without re-probing dumpsys: $DEV_MAXHZ (same shell) -> hz_max in $D/device.info -> live get_max_display_hz.
cached_max_hz() {
    _ch="$DEV_MAXHZ"
    [ -n "$_ch" ] || _ch=$(sed -n 's/^hz_max=//p' "$D/device.info" 2>/dev/null)
    case "$_ch" in ''|*[!0-9]*) get_max_display_hz; return ;; esac
    printf '%s' "$_ch"
}

# Rate used for frame-time math: what the user actually allows, not the panel maximum.
# Priority: manual override ($D/hz) > settings system peak_refresh_rate (if lower than $1) > $1 (= max).
get_effective_hz() {
    _ov="$(cat "$D/hz" 2>/dev/null | tr -dc '0-9')"
    if [ -n "$_ov" ] && [ "$_ov" -ge 30 ] && [ "$_ov" -le 1000 ]; then printf '%s' "$_ov"; return; fi
    _pk="$(settings get system peak_refresh_rate 2>/dev/null | cut -d. -f1)"
    case "$_pk" in ''|*[!0-9]*) _pk="" ;; esac
    if [ -n "$_pk" ] && [ "$_pk" -ge 30 ] && [ "$_pk" -lt "$1" ]; then printf '%s' "$_pk"; else printf '%s' "$1"; fi
}

# ---- device detection ----
# Sets DEV_NAME DEV_SOC DEV_RAM_MB DEV_CORES DEV_MAXHZ DEV_TIER DEV_TIER_SRC and writes $D/device.info
# Tier: ENTRY / MID / HIGH / FKILLER / FLAGSHIP  (SoC family first; RAM only caps it or is the fallback for unknown SoCs)
detect_device() {
    DEV_NAME=$(settings get global device_name 2>/dev/null)
    case "$DEV_NAME" in ''|null) DEV_NAME=$(getprop ro.product.marketname) ;; esac
    [ -n "$DEV_NAME" ] || DEV_NAME=$(getprop ro.product.model)
    DEV_NAME=$(printf '%s' "$DEV_NAME" | tr -cd 'A-Za-z0-9 ._+-' | tr 'a-z' 'A-Z' | cut -c1-40)
    _p="$(getprop ro.board.platform) $(getprop ro.hardware) $(getprop ro.soc.model) $(getprop ro.mediatek.platform)"
    _p=$(printf '%s' "$_p" | tr 'A-Z' 'a-z')
    DEV_SOC=$(getprop ro.soc.model); [ -n "$DEV_SOC" ] || DEV_SOC=$(getprop ro.board.platform)
    DEV_RAM_MB=$(awk '/^MemTotal:/ {printf "%d", $2/1024; exit}' /proc/meminfo 2>/dev/null)
    DEV_RAM_MB=${DEV_RAM_MB:-0}
    DEV_CORES=$(grep -c '^processor' /proc/cpuinfo 2>/dev/null)
    DEV_MAXHZ=$(get_max_display_hz) || DEV_MAXHZ=""
    DEV_TIER_SRC=soc
    case "$_p" in
        *mt6580*|*mt6739*|*mt6761*|*mt6762*|*mt6765*|*sp9863*|*sc9863*|*ums312*|*sp7731*|*sc7731*|*mt6767*|*mt6768*|*mt6769*|*mt6771*|*ums512*|*ums9230*|*ums9620*|*bengal*|*trinket*) DEV_TIER=ENTRY ;;
        *mt6785*|*mt6789*|*mt6833*|*mt6853*|*mt6855*|*mt6877*|*mt6879*|*holi*|*lito*|*atoll*) DEV_TIER=MID ;;
        *mt6893*|*mt8*) DEV_TIER=HIGH ;;
        *mt6895*|*mt6896*|*mt6897*|*mt6899*|*kona*) DEV_TIER=FKILLER ;;
        *mt6983*|*mt6985*|*mt6989*|*mt6991*|*lahaina*|*taro*|*kalama*|*pineapple*|*sun*) DEV_TIER=FLAGSHIP ;;
        *) DEV_TIER_SRC=ram
           if [ "$DEV_RAM_MB" -le 6200 ]; then DEV_TIER=ENTRY; else DEV_TIER=MID; fi ;;
    esac
    # low RAM caps the tier
    if [ "$DEV_RAM_MB" -gt 0 ] && [ "$DEV_RAM_MB" -le 3200 ]; then DEV_TIER=ENTRY; fi
    mkdir -p "$D" 2>/dev/null
    {
        echo "name=$DEV_NAME"; echo "soc=$DEV_SOC"; echo "ram_mb=$DEV_RAM_MB"; echo "cores=$DEV_CORES"
        echo "hz_max=${DEV_MAXHZ:-?}"; echo "tier=$DEV_TIER"; echo "tier_src=$DEV_TIER_SRC"
    } > "$D/device.info" 2>/dev/null
}

# ENTRY/MID/HIGH/FKILLER/FLAGSHIP -> "entry level"/"mid range"/"high end"/"flagship killer"/"flagship" (shown in the module description)
tier_label() { case "$1" in ENTRY) echo "entry level" ;; MID) echo "mid range" ;; HIGH) echo "high end" ;; FKILLER) echo "flagship killer" ;; *) echo "flagship" ;; esac; }

# Append " | <Tuned for> <NAME> (<tier label>)" to the description line of a module.prop (idempotent)
update_description() {
    [ -w "$1" ] && [ -n "$DEV_NAME" ] || return 0
    _tf=$(cat "$D/tuned_for" 2>/dev/null); _tf=${_tf:-Tuned for}
    _tf=$(printf '%s' "$_tf" | tr -d '/&|\\')
    sed -i -e 's/^\(description=.*\) | .*$/\1/' -e "s/^description=.*/& | $_tf $DEV_NAME ($(tier_label "$DEV_TIER"))/" "$1" 2>/dev/null
}

# ---- volume-key prompt (getevent) ----
# ask_key <timeout_s> : 0 = Vol+ , 1 = Vol- , 2 = timeout , 3 = getevent unusable (no permission / missing)
# (cek) needs read access to /dev/input (shell uid normally has it)
ask_key() {
    command -v getevent >/dev/null 2>&1 || return 3
    _kf="$D/.key.$$"; _end=$(( $(date +%s) + ${1:-10} ))
    while [ "$(date +%s)" -lt "$_end" ]; do
        : > "$_kf"
        getevent -lqc 1 > "$_kf" 2>/dev/null &
        _kp=$!
        sleep 0.3 2>/dev/null || sleep 1
        if ! kill -0 "$_kp" 2>/dev/null && [ ! -s "$_kf" ]; then rm -f "$_kf"; return 3; fi
        while kill -0 "$_kp" 2>/dev/null && [ "$(date +%s)" -lt "$_end" ]; do sleep 0.2 2>/dev/null || sleep 1; done
        kill "$_kp" 2>/dev/null; wait "$_kp" 2>/dev/null
        if grep -q 'KEY_VOLUMEUP.*DOWN' "$_kf" 2>/dev/null; then rm -f "$_kf"; return 0; fi
        if grep -q 'KEY_VOLUMEDOWN.*DOWN' "$_kf" 2>/dev/null; then rm -f "$_kf"; return 1; fi
    done
    rm -f "$_kf"; return 2
}
