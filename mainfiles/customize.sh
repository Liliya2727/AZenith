#
# Copyright (C) 2024-2025 Zexshia
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

# Create File
make_node() {
    [ ! -f "$2" ] && echo "$1" >"$2"
}

# Displaybanner
ui_print ""
ui_print "              AZenith              "
ui_print ""
ui_print "- Installing AZenith..."

# Extract Module Directiories
mkdir -p "$MODULE_CONFIG"
mkdir -p "$MODULE_CONFIG/debug"
mkdir -p "$MODULE_CONFIG/API"
mkdir -p "$MODULE_CONFIG/preload"
ui_print "- Create module config"

# Flashable integrity checkup
ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
[ ! -f "$TMPDIR/verify.sh" ] && abort_corrupted
source "$TMPDIR/verify.sh"

# Extract Module files
ui_print "- Extracting system directory..."
extract "$ZIPFILE" 'system/bin/sys.azenith-profilesettings' "$MODPATH"
extract "$ZIPFILE" 'system/bin/sys.azenith-utilityconf' "$MODPATH"
extract "$ZIPFILE" 'system/bin/sys.azenith-preloadbin' "$MODPATH"
extract "$ZIPFILE" 'system/bin/sys.azenith-preloadbin2' "$MODPATH"
ui_print "- Extracting service.sh..."
extract "$ZIPFILE" service.sh "$MODPATH"
ui_print "- Extracting module.prop..."
extract "$ZIPFILE" module.prop "$MODPATH"
ui_print "- Extracting uninstall.sh..."
extract "$ZIPFILE" uninstall.sh "$MODPATH"
ui_print "- Extracting gamelist.txt..."
extract "$ZIPFILE" gamelist.txt "$MODULE_CONFIG/gamelist"
ui_print "- Extracting module_icon.png..."
extract "$ZIPFILE" module_icon.png /data/local/tmp

# Target architecture
case $ARCH in
"arm64") ARCH_TMP="arm64-v8a" ;;
"arm") ARCH_TMP="armeabi-v7a" ;;
*) abort ;;
esac

# Extract daemon
ui_print "- Extracting sys_azenithrs-service for $ARCH_TMP"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys_azenithrs-service" "$TMPDIR"
cp "$TMPDIR"/libs/"$ARCH_TMP"/* "$MODPATH/system/bin"
rm -rf "$TMPDIR/libs"
ui_print "- Installing for Arch : $ARCH_TMP"

# Use Symlink
if [ "$KSU" = "true" ] || [ "$APATCH" = "true" ]; then
    # skip mount on APatch / KernelSU
    touch "$MODPATH/skip_mount"
    ui_print "- KSU/AP Detected, skipping module mount (skip_mount)"
    # symlink ourselves on $PATH
    manager_paths="/data/adb/ap/bin /data/adb/ksu/bin"
    BIN_PATH="/data/adb/modules/AZenith/system/bin"
    for dir in $manager_paths; do
        [ -d "$dir" ] && {
            ui_print "- Creating symlink in $dir"
            ln -sf "$BIN_PATH/sys_azenith-service" "$dir/sys_azenith-service"
            ln -sf "$BIN_PATH/sys.azenith-profilesettings" "$dir/sys.azenith-profilesettings"
            ln -sf "$BIN_PATH/sys.azenith-utilityconf" "$dir/sys.azenith-utilityconf"
            ln -sf "$BIN_PATH/sys.azenith-preloadbin" "$dir/sys.azenith-preloadbin"
            ln -sf "$BIN_PATH/sys.azenith-preloadbin2" "$dir/sys.azenith-preloadbin2"
        }
    done
fi

# Apply Tweaks Based on Chipset
ui_print "- Checking device soc"
chipset=$(grep -i 'hardware' /proc/cpuinfo | uniq | cut -d ':' -f2 | sed 's/^[ \t]*//')
[ -z "$chipset" ] && chipset="$(getprop ro.board.platform) $(getprop ro.hardware)"

case "$(echo "$chipset" | tr '[:upper:]' '[:lower:]')" in
*mt* | *MT*)
    soc="MediaTek"
    ui_print "- Applying Tweaks for $soc"
    make_node 1 "$MODULE_CONFIG/soctype"
    ;;
*sm* | *qcom* | *SM* | *QCOM* | *Qualcomm* | *sdm* | *snapdragon*)
    soc="Snapdragon"
    ui_print "- Applying Tweaks for $soc"
    make_node 2 "$MODULE_CONFIG/soctype"
    ;;
*exynos* | *Exynos* | *EXYNOS* | *universal* | *samsung* | *erd* | *s5e*)
    soc="Exynos"
    ui_print "- Applying Tweaks for $soc"
    make_node 3 "$MODULE_CONFIG/soctype"
    ;;
*Unisoc* | *unisoc* | *ums*)
    soc="Unisoc"
    ui_print "- Applying Tweaks for $soc"
    make_node 4 "$MODULE_CONFIG/soctype"
    ;;
*gs* | *Tensor* | *tensor*)
    soc="Tensor"
    ui_print "- Applying Tweaks for $soc"
    make_node 5 "$MODULE_CONFIG/soctype"
    ;;
*)
    soc="Unknown"
    ui_print "- Applying Tweaks for $chipset"
    make_node 0 "$MODULE_CONFIG/soctype"
    ;;
esac

# Soc Type
# 1) MediaTek
# 2) Snapdragon
# 3) Exynos
# 4) Unisoc
# 5) Tensor
# 0) Unknown

# Extract Webui
ui_print "- Extracting WebUI"
mkdir -p "$MODPATH/webroot"
unzip -o "$ZIPFILE" "webroot/*" -d "$TMPDIR" >&2
cp -r "$TMPDIR/webroot/"* "$MODPATH/webroot/"
rm -rf "$TMPDIR/webroot"

# Make Module Config
ui_print "- Creating Module Config"
make_node "1000 1000 1000 1000" "$MODULE_CONFIG/color_scheme"
make_node "Disabled 90% 80% 70% 60% 50% 40%" "$MODULE_CONFIG/availableFreq"
make_node "Disabled" "$MODULE_CONFIG/customFreqOffset"
make_node "Disabled 60hz 90hz 120hz" "$MODULE_CONFIG/availableValue"
make_node "Disabled" "$MODULE_CONFIG/customVsync"

# Set config properties to use
ui_print "- Setting config properties..."
props="
persist.sys.azenithconf.logd
persist.sys.azenithconf.DThermal
persist.sys.azenithconf.SFL
persist.sys.azenithconf.malisched
persist.sys.azenithconf.fpsged
persist.sys.azenithconf.schedtunes
persist.sys.azenithconf.clearbg
persist.sys.azenithconf.bypasschg
persist.sys.azenithconf.APreload
persist.sys.azenithconf.iosched
persist.sys.azenithconf.cpulimit
persist.sys.azenithconf.dnd
persist.sys.azenithconf.justintime
persist.sys.azenithconf.disabletrace
"
for prop in $props; do
    curval=$(getprop "$prop")
    if [ -z "$curval" ]; then
        setprop "$prop" 0
    fi
done
if [ -z "$(getprop persist.sys.azenithconf.showtoast)" ]; then
    setprop persist.sys.azenithconf.showtoast 1
fi

# Install toast if not installed
if pm list packages | grep -q bellavita.toast; then
    ui_print "- Bellavita Toast is already installed."
else
    ui_print "- Extracting Bellavita Toast..."
    unzip -qo "$ZIPFILE" toast.apk -d "$MODPATH" >&2
    ui_print "- Installing Bellavita Toast..."
    pm install "$MODPATH/toast.apk"
    rm "$MODPATH/toast.apk"
fi

# Make sure to enable Auto Every installment and Update
ui_print "- Enabling Auto Mode"
setprop persist.sys.azenithconf.AIenabled 1

# Clean Up useless files
ui_print "- Cleaning Up..."
rm -rf "$MODPATH/webroot/include"

# Set Permissions
ui_print "- Setting Permissions..."
set_perm_recursive "$MODPATH/system/bin" 0 2000 0777 0777
chmod +x "$MODPATH/system/bin/sys_azenithrs-service"
chmod +x "$MODPATH/system/bin/sys.azenith-profilesettings"
chmod +x "$MODPATH/system/bin/sys.azenith-utilityconf"
chmod +x "$MODPATH/system/bin/sys.azenith-preloadbin"
chmod +x "$MODPATH/system/bin/sys.azenith-preloadbin2"

ui_print "- Installation complete!"
