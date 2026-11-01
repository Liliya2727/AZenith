#
# Copyright (C) 2026-2027 Zexshia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SKIPUNZIP=1

# Paths
MODULE_CONFIG="/data/adb/.config/AZenith"
device_codename=$(getprop ro.product.board)
chip=$(getprop ro.hardware)
HM_DIR="/data/adb/hybrid-mount"
HM_CONFIG="$HM_DIR/config.toml"
API_LEVEL=$(getprop ro.build.version.sdk)
readonly APK_COMP="$MODPATH/AZenith.apk"
readonly TMP_DIR="/data/local/tmp"
readonly TMP_APK="$TMP_DIR/AZenith_install.apk"

mkdir -p "$TMP_DIR"
unzip -o "$ZIPFILE" "installer-lang.sh" "translations/*/strings.sh" -d "$TMP_DIR" >&2
[ -f "$TMP_DIR/installer-lang.sh" ] || abort "! Installer language data is missing."
AZENITH_TRANSLATIONS_DIR="$TMP_DIR/translations"
. "$TMP_DIR/installer-lang.sh"

installer_msg() {
    echo "$(azenith_text "$@")"
}

# Create File
make_node() {
	[ ! -f "$2" ] && echo "$1" >"$2"
}

# Prepare app files
_prepare_apk() {
    if [ ! -f "$APK_COMP" ]; then
        installer_msg apk_missing "$APK_COMP" >&2
        return 1
    fi    
    installer_msg preparing_apk
    cp "$APK_COMP" "$TMP_APK" || return 1
    chmod 644 "$TMP_APK" || return 1  
    return 0
}

# Cleanup temporary APK
_cleanup_apk() {
    [ -f "$TMP_APK" ] && rm -f "$TMP_APK"
}

# Install APK
install_manager() {
    local apk_path="$1"
    local app_name="$2"
    local tmp_log="$TMP_DIR/install_log.txt"

    local MAX_TIMEOUT=100

    (
        local install_output
        install_output=$(pm install -r -d --user 0 "$apk_path" 2>&1)    
        if ! echo "$install_output" | grep -iq "Success"; then
            install_output=$(cmd package install -r -d --user 0 "$apk_path" 2>&1)
        fi
        echo "$install_output" > "$tmp_log"
    ) &
    local pid=$!

    local i=0
    while kill -0 $pid 2>/dev/null; do
        clear
        case $((i % 4)) in
            0) installer_msg installing_spinner - "$app_name" ;;
            1) installer_msg installing_spinner / "$app_name" ;;
            2) installer_msg installing_spinner '|' "$app_name" ;;
            3) installer_msg installing_spinner '\' "$app_name" ;;
        esac
        sleep 0.1
        i=$((i + 1))

        if [ $i -ge $MAX_TIMEOUT ]; then
            installer_msg install_timeout_stopping
            kill -9 $pid 2>/dev/null
            echo "Timeout: Installation exceeded 10 seconds" > "$tmp_log"
            break
        fi
    done

    wait $pid 2>/dev/null
    clear

    local result
    [ -f "$tmp_log" ] && result=$(cat "$tmp_log")
    rm -f "$tmp_log"

    if echo "$result" | grep -iq "Success"; then
        installer_msg install_success "$app_name"
		echo ""
        return 0
    else
        installer_msg install_failed "$app_name"
        if echo "$result" | grep -iq "Timeout"; then
            installer_msg install_timeout
            installer_msg install_timeout_reason
        else
            installer_msg install_error_log "$(echo "$result" | head -n 2)"
        fi
        installer_msg run_module_action
        installer_msg unzip_module_manual
        installer_msg install_module_manual
        installer_msg install_continuing
        sleep 3
        return 1
    fi
}


abort_api() {
  installer_msg installation_aborted
  installer_msg unsupported_android
  installer_msg android_required
  abort "$(azenith_text android_current "$API_LEVEL")"
}

abort_corrupted() {
  clear
  installer_msg installation_aborted
  installer_msg corrupted_package
  installer_msg required_files_missing
  abort "$(azenith_text redownload_module)"
}

abort_arch() {
  clear
  installer_msg installation_aborted
  installer_msg unsupported_arch
  installer_msg incompatible_arch
  installer_msg supported_arch
  echo "  • arm64-v8a"
  abort "  • armeabi-v7a"
}

installation_complete() {
  installer_msg install_complete
  installer_msg install_thanks
  installer_msg reboot_device
  installer_msg open_manager_action
  installer_msg grant_root
}

# Displaybanner
echo ""
echo "              AZenith              "
echo ""
installer_msg installing_azenith

# API Level Check (Require API 29+)
[ "$API_LEVEL" -lt 29 ] && abort_api

# Extract Module Directiories
mkdir -p "$MODULE_CONFIG"
mkdir -p "$MODULE_CONFIG/debug"
mkdir -p "$MODULE_CONFIG/API"
mkdir -p "$MODULE_CONFIG/preload"
mkdir -p "$MODULE_CONFIG/bypasschgconfig"
mkdir -p "$MODULE_CONFIG/gamelist"
mkdir -p "$MODPATH/system/bin"
installer_msg create_config

# Flashable integrity checkup
installer_msg extracting_verify
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
[ ! -f "$TMPDIR/verify.sh" ] && abort_corrupted
source "$TMPDIR/verify.sh"

# Install and validate language data before using translated installer messages.
installer_msg extracting_language
extract "$ZIPFILE" installer-lang.sh "$MODPATH"
for language_file in "$TMP_DIR"/translations/*/strings.sh; do
    [ -f "$language_file" ] || continue
    relative_file="${language_file#"$TMP_DIR"/}"
    extract "$ZIPFILE" "$relative_file" "$MODPATH"
done
AZENITH_TRANSLATIONS_DIR="$MODPATH/translations"
. "$MODPATH/installer-lang.sh"

# Target architecture detection
case $ARCH in
"arm64") ARCH_TMP="arm64-v8a" ;;
"arm") ARCH_TMP="armeabi-v7a" ;;
*) abort_arch ;;
esac

installer_msg extracting_binaries "$ARCH_TMP"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-service" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-profilesettings" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-rianixiathermalcore" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-utilityconf" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-preferencedtweaks" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-preloadbin" "$TMPDIR"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-appmonitoring" "$TMPDIR"
cp "$TMPDIR/libs/$ARCH_TMP/"* "$MODPATH/system/bin/"
rm -rf "$TMPDIR/libs"
installer_msg binaries_installed

# Extract Module standard files
installer_msg extracting_service
extract "$ZIPFILE" service.sh "$MODPATH"
installer_msg extracting_post_fs_data
extract "$ZIPFILE" post-fs-data.sh "$MODPATH"
installer_msg extracting_action
extract "$ZIPFILE" action.sh "$MODPATH"
installer_msg extracting_cleanup
extract "$ZIPFILE" cleanup.sh "$MODPATH"
installer_msg extracting_module_prop
extract "$ZIPFILE" module.prop "$MODPATH"
azenith_update_module_description "$MODPATH/module.prop"
cp "$MODPATH/module.prop" "$MODPATH/module.prop.orig"
installer_msg extracting_uninstall
extract "$ZIPFILE" uninstall.sh "$MODPATH"
if [ ! -f "$MODULE_CONFIG/gamelist/azenithApplist.json" ]; then
    installer_msg extracting_applist
    extract "$ZIPFILE" azenithApplist.json "$MODULE_CONFIG/gamelist"
fi
installer_msg extracting_banner
extract "$ZIPFILE" module.banner.jpg "$MODPATH"

# Skip mountify
touch "$MODPATH/skip_mountify"

# Skip hybrid mount
if [ -f "$HM_CONFIG" ]; then
    installer_msg hybrid_detected

    HM_BIN=""
    if [ -x "/data/adb/modules/hybrid_mount/hybrid-mount" ]; then
        HM_BIN="/data/adb/modules/hybrid_mount/hybrid-mount"
    elif command -v hybrid-mount >/dev/null 2>&1; then
        HM_BIN="hybrid-mount"
    fi

    if [ -n "$HM_BIN" ]; then
        installer_msg hybrid_cli_found "$HM_BIN"
        $HM_BIN api config-patch --patch '{"rules":{"AZenith":{"default_mode":"ignore"}}}' --apply-runtime >/dev/null 2>&1
        installer_msg hybrid_runtime_updated
    else
        installer_msg hybrid_cli_missing
    fi

    if ! grep -q "\[rules\.AZenith\]" "$HM_CONFIG"; then
        echo "" >> "$HM_CONFIG"
        echo "[rules.AZenith]" >> "$HM_CONFIG"
        echo 'default_mode = "ignore"' >> "$HM_CONFIG"
        installer_msg hybrid_rule_added
    fi
else
    installer_msg hybrid_not_installed
fi

# Use Symlink for APatch / KernelSU
if [ "$KSU" = "true" ] || [ "$APATCH" = "true" ]; then
	# skip mount on APatch / KernelSU
	touch "$MODPATH/skip_mount"
	installer_msg ksu_skip_mount
	# symlink ourselves on $PATH
	manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
	BIN_PATH="/data/adb/modules/AZenith/system/bin"
	for dir in $manager_paths; do
		[ -d "$dir" ] && {
			installer_msg creating_symlink "$dir"
			ln -sf "$BIN_PATH/sys.azenith-service" "$dir/sys.azenith-service"
			ln -sf "$BIN_PATH/sys.azenith-service" "$dir/zx" # Binary calls for CLI
			ln -sf "$BIN_PATH/sys.azenith-profilesettings" "$dir/sys.azenith-profilesettings"
			ln -sf "$BIN_PATH/sys.azenith-utilityconf" "$dir/sys.azenith-utilityconf"
			ln -sf "$BIN_PATH/sys.azenith-preferencedtweaks" "$dir/sys.azenith-preferencedtweaks"
			ln -sf "$BIN_PATH/sys.azenith-preloadbin" "$dir/sys.azenith-preloadbin"
			ln -sf "$BIN_PATH/sys.azenith-preloadbin" "$dir/sys.azenith-appmonitoring"
            ln -sf "$BIN_PATH/sys.azenith-rianixiathermalcore" "$dir/sys.azenith-rianixiathermalcore"
		}
	done
fi

# Apply Tweaks Based on Chipset
installer_msg checking_soc
chipset=$(grep -i 'hardware' /proc/cpuinfo | uniq | cut -d ':' -f2 | sed 's/^[ \t]*//')
[ -z "$chipset" ] && chipset="$(getprop ro.board.platform) $(getprop ro.hardware)"
case "$(echo "$chipset" | tr '[:upper:]' '[:lower:]')" in
*mt* | *MT*)
	soc="MediaTek"
	installer_msg applying_tweaks "$soc"
	setprop persist.sys.azenith.soctype 1
	;;
*sm* | *qcom* | *SM* | *QCOM* | *Qualcomm* | *sdm* | *snapdragon*)
	soc="Snapdragon"
	installer_msg applying_tweaks "$soc"
	setprop persist.sys.azenith.soctype 2
	;;
*exynos* | *Exynos* | *EXYNOS* | *universal* | *samsung* | *erd* | *s5e*)
	soc="Exynos"
	installer_msg applying_tweaks "$soc"
	setprop persist.sys.azenith.soctype 3
	;;
*Unisoc* | *unisoc* | *ums*)
	soc="Unisoc"
	installer_msg applying_tweaks "$soc"
	setprop persist.sys.azenith.soctype 4
	;;
*gs* | *Tensor* | *tensor*)
	soc="Tensor"
	installer_msg applying_tweaks "$soc"
	setprop persist.sys.azenith.soctype 5
	;;
*)
	soc="Unknown"
	installer_msg applying_tweaks "$chipset"
	setprop persist.sys.azenith.soctype 0
	;;
esac

# Soc Type
# 1) MediaTek
# 2) Snapdragon
# 3) Exynos
# 4) Unisoc
# 5) Tensor
# 0) Unknown

# Set default freqoffset
if [ -z "$(getprop persist.sys.azenithconf.freqoffset)" ]; then
	setprop persist.sys.azenithconf.freqoffset "Disabled"
	touch "$MODULE_CONFIG/freqoffset"
	echo "Disabled" > "$MODULE_CONFIG/freqoffset"
fi

# Set default color scheme if not set
if [ -z "$(getprop persist.sys.azenithconf.schemeconfig)" ]; then
	setprop persist.sys.azenithconf.schemeconfig "1000 1000 1000 1000"
fi

# Initiate bypasspath default value
if [ -z "$(getprop persist.sys.azenithconf.bypasspath)" ]; then
	setprop persist.sys.azenithconf.bypasspath "NEED_SETUP"
	touch "$MODULE_CONFIG/bypasschgconfig/bypasspath"
	echo "NEED_SETUP" > "$MODULE_CONFIG/bypasschgconfig/bypasspath"
fi

# Initiate bypasspath default value
if [ -z "$(getprop persist.sys.azenithconf.bypasschgthreshold)" ]; then
	setprop persist.sys.azenithconf.bypasschgthreshold "20"
	touch "$MODULE_CONFIG/bypasschgconfig/bypasschgthreshold"
	echo "20" > "$MODULE_CONFIG/bypasschgconfig/bypasschgthreshold"
fi

# Initiate bypasscharging state
if [ -z "$(getprop persist.sys.azenithconf.bypasschg)" ]; then
	setprop persist.sys.azenithconf.bypasschg "0"
	touch "$MODULE_CONFIG/bypasschgconfig/bypasschg"
	echo "0" > "$MODULE_CONFIG/bypasschgconfig/bypasschg"
fi

# Daemon Configurations
if [ -z "$(getprop persist.sys.azenithconf.showtoast)" ]; then
	setprop persist.sys.azenithconf.showtoast 0
fi

if [ -z "$(getprop persist.sys.azenith.profilenotifications)" ]; then
	setprop persist.sys.azenith.profilenotifications 1
fi

if [ -z "$(getprop persist.sys.azenith.dropforeground)" ]; then
	setprop persist.sys.azenith.dropforeground 0
fi

if [ -z "$(getprop persist.sys.azenith.disabletweak)" ]; then
	setprop persist.sys.azenith.disabletweak 0
fi

if [ -z "$(getprop persist.sys.azenithconf.iosched)" ]; then
	setprop persist.sys.azenithconf.iosched 1
fi

if [ -z "$(getprop persist.sys.azenithconf.renderer)" ]; then
	setprop persist.sys.azenithconf.renderer default
fi

if [ -z "$(getprop persist.sys.azenithconf.preloadbudget)" ]; then
    setprop persist.sys.azenithconf.preloadbudget 500M
fi

if [ -z "$(getprop persist.sys.azenithconf.AIenabled)" ]; then
    installer_msg enabling_auto
    setprop persist.sys.azenithconf.AIenabled 1
    echo 1 > "$MODULE_CONFIG/API/current_modes"
fi

installer_msg disabling_debug
setprop persist.sys.azenith.debugmode "false"

# Set config properties to use
installer_msg setting_properties
props="
persist.sys.azenithconf.logd
persist.sys.azenithconf.DThermal
persist.sys.azenithconf.SFL
persist.sys.azenithconf.malisched
persist.sys.azenithconf.fpsged
persist.sys.azenithconf.schedtunes
persist.sys.azenithconf.clearbg
persist.sys.azenithconf.APreload
persist.sys.azenithconf.cpulimit
persist.sys.azenithconf.dnd
persist.sys.azenithconf.justintime
persist.sys.azenithconf.disabletrace
persist.sys.azenithconf.thermalcore
persist.sys.azenithconf.walttunes
persist.sys.azenithconf.fstrim
persist.sys.azenithconf.usefpsgo
"
for prop in $props; do
	curval=$(getprop "$prop")
	if [ -z "$curval" ]; then
		setprop "$prop" 0
	fi
done

extract "$ZIPFILE" AZenith.apk "$MODPATH"

# Install Apps
APP_INSTALLED=false

if ! _prepare_apk; then
    installer_msg prepare_apk_failed >&2
else
    if install_manager "$TMP_APK" "AZenith Manager"; then
        APP_INSTALLED=true
    fi
    _cleanup_apk
fi

# Enable Launcher and grant permissions ONLY if app installed successfully
if [ "$APP_INSTALLED" = "true" ]; then
    installer_msg enabling_launcher
    pm enable --user 0 zx.azenith/.Launcher > /dev/null 2>&1

    # The module is not mounted yet, so the binary is staged to /data/local/tmp instead of
    # being run from $MODPATH; it is removed again as soon as the grants are done.
    # Without SYSTEM_ALERT_WINDOW Game Space cannot draw its overlay, and a fresh install
    # would show none until the device is rebooted.
    installer_msg setting_permissions
    BIN_STAGE="$TMP_DIR/sys.azenith-service"
    if [ -f "$MODPATH/system/bin/sys.azenith-service" ]; then
        cp "$MODPATH/system/bin/sys.azenith-service" "$BIN_STAGE" && {
            chmod 0755 "$BIN_STAGE"
            "$BIN_STAGE" --setup-access
            rm -f "$BIN_STAGE"
        }
    else
        installer_msg permission_setup_skipped
    fi

fi

# Remove old module files if available
installer_msg cleaning_old_files
[ -f "/data/local/tmp/module.avatar.webp" ] && rm -f "/data/local/tmp/module.avatar.webp"
if pm list packages | grep -q "azenith.toast"; then
    installer_msg uninstalling_old_components
    pm uninstall --user 0 azenith.toast > /dev/null 2>&1
fi

set_perm_recursive "$MODPATH/system/bin" 0 0 0755 0755

installation_complete
