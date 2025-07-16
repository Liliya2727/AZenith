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
    [ ! -f "$2" ] && echo "$1" > "$2"
}


# Displaybanner
ui_print ""
ui_print "              AZenith              "
ui_print ""
ui_print "- Installing AZenith..."


# Extract Module Directiories
mkdir -p "$MODULE_CONFIG"
ui_print "- Create module config"

# Extract Module files
ui_print "- Extracting system directory..."
extract -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2
ui_print "- Extracting service.sh..."
extract -qo "$ZIPFILE" service.sh -d "$MODPATH" >&2
ui_print "- Extracting module.prop..."
extract -qo "$ZIPFILE" module.prop -d "$MODPATH" >&2
ui_print "- Extracting uninstall.sh..."
extract -qo "$ZIPFILE" uninstall.sh -d "$MODPATH" >&2
ui_print "- Extracting gamelist.txt..."
extract -qo "$ZIPFILE" gamelist.txt -d "$MODULE_CONFIG" >&2
ui_print "- Extracting AZenith_icon.png..."
extract -qo "$ZIPFILE" AZenith_icon.png -d /data/local/tmp >&2
ui_print "- Extracting webroot"
unzip -o "$ZIPFILE" "webroot/*" -d "$MODPATH" >&2

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

# Target architecture
case $ARCH in
"arm64") ARCH_TMP="arm64-v8a" ;;
"arm") ARCH_TMP="armeabi-v7a" ;;
"x64") ARCH_TMP="x86_64" ;;
"x86") ARCH_TMP="x86" ;;
"riscv64") ARCH_TMP="riscv64" ;;
*) abort ;;
esac

# Extract daemon
extract -o "$ZIPFILE" "libs/$ARCH_TMP/AZenith" "$TMPDIR"
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
			ln -sf "$BIN_PATH/AZenith" "$dir/AZenith"
			ln -sf "$BIN_PATH/AZenith" "$dir/AZenith_log"
			ln -sf "$BIN_PATH/AZenith_Profiler" "$dir/AZenith_Profiler"
			ln -sf "$BIN_PATH/bypassCharge" "$dir/bypassCharge"
			ln -sf "$BIN_PATH/vmt" "$dir/vmt"
		}
	done
fi

# Apply Tweaks Based on Chipset
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
    *)
        soc="Unknown"
        ui_print "- Unsupported Chipset: $chipset, Skipping some tweaks"
        make_node 0 "$MODULE_CONFIG/soctype"
        ;;
esac
# Soc Type
# 1) MediaTek
# 2) Snapdragon 
# 0) Unknown/No Unsupported
# /// More chipset will be added soon ////

# Inflate WebUI Based on SoC
case "$soc" in
    "MediaTek")
        ui_print "- Inflating WebUI for MediaTek"
        cp -f "$MODPATH/webroot/include/mtkhtml" "$MODPATH/webroot/index.html"
        cp -f "$MODPATH/webroot/include/mtkjs" "$MODPATH/webroot/assets/fn762ag.js"
        ;;
    "Snapdragon")
        ui_print "- Inflating WebUI for Snapdragon"
        cp -f "$MODPATH/webroot/include/sdmhtml" "$MODPATH/webroot/index.html"
        cp -f "$MODPATH/webroot/include/sdmjs" "$MODPATH/webroot/assets/fn762ag.js"
        ;;
    *)
        ui_print "- Inflating Universal WebUI"
        cp -f "$MODPATH/webroot/include/bschtml" "$MODPATH/webroot/index.html"
        cp -f "$MODPATH/webroot/include/bscjs" "$MODPATH/webroot/assets/fn762ag.js"
        ;;
esac
# I made 3 differs WebUI for this to match every Chipsets
# # # # # # 

# Make module config 
make_node 0 "$MODULE_CONFIG/clearbg"
make_node 0 "$MODULE_CONFIG/bypass"
make_node 0 "$MODULE_CONFIG/APreload"
make_node 0 "$MODULE_CONFIG/logger"
make_node 0 "$MODULE_CONFIG/logd"
make_node 0 "$MODULE_CONFIG/DThermal"
make_node 0 "$MODULE_CONFIG/dnd"
make_node 0 "$MODULE_CONFIG/schedtunes"
make_node 0 "$MODULE_CONFIG/fpsged"
make_node 0 "$MODULE_CONFIG/iosched"
make_node 0 "$MODULE_CONFIG/SFL"
make_node 0 "$MODULE_CONFIG/malisched"
make_node 0 "$MODULE_CONFIG/cpulimit"
make_node "1000 1000 1000 1000" "$MODULE_CONFIG/color_scheme"
make_node "Disabled 90% 80% 70% 60% 50% 40%" "$MODULE_CONFIG/availableFreq"
make_node "Disabled" "$MODULE_CONFIG/customFreqOffset"
make_node "Disabled 60hz 90hz 120hz" "$MODULE_CONFIG/availableValue"
make_node "Disabled" "$MODULE_CONFIG/customVsync"

# Make sure to enable Auto Every installment and Update
echo "1" > "$MODULE_CONFIG/AIenabled"

# Set Permissions
ui_print "- Setting Permissions..."
set_perm_recursive "$MODPATH/system/bin" 0 2000 0777 0777
chmod +x "$MODPATH/system/bin/AZenith"
chmod +x "$MODPATH/system/bin/AZenith_Profiler"
chmod +x "$MODPATH/system/bin/bypassCharge"
chmod +x "$MODPATH/system/bin/vmt"

ui_print "- Installation complete!"
