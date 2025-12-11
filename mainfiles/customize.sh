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
abort_corrupted() {
  ui_print ""
  ui_print "! Installation Aborted"
  ui_print "! The AZenith package appears to be corrupted or incomplete."
  ui_print "! Required installation files were not found."
  ui_print ""
  ui_print "! Please re-download the module and try again."
  abort "# # # # # # # # # # # # # # # # # # # # #"
}
abort_arch() {
  ui_print "! Installation Aborted"
  ui_print "! Unsupported CPU Architecture Detected"
  ui_print "! Your device architecture is not compatible with this build of AZenith."
  ui_print "! Supported architectures:"
  ui_print "  • arm64-v8a"
  ui_print "  • armeabi-v7a"
  ui_print "  • x86 / x86_64"
  ui_print "  • riscv64"
  abort "# # # # # # # # # # # # # # # # # # # # #"
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
mkdir -p "$MODULE_CONFIG/gamelist"
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
extract "$ZIPFILE" 'system/bin/sys.azenith-rianixiathermalcorev4' "$MODPATH"
ui_print "- Extracting service.sh..."
extract "$ZIPFILE" service.sh "$MODPATH"
ui_print "- Extracting module.prop..."
extract "$ZIPFILE" module.prop "$MODPATH"
ui_print "- Extracting uninstall.sh..."
extract "$ZIPFILE" uninstall.sh "$MODPATH"
if ! ls "$MODULE_CONFIG/gamelist/"gamelist.* >/dev/null 2>&1; then
    ui_print "- Extracting gamelist.txt..."
    extract "$ZIPFILE" gamelist.txt "$MODULE_CONFIG/gamelist"
fi
ui_print "- Extracting module icon..."
extract "$ZIPFILE" module.avatar.webp "/data/local/tmp"
ui_print "- Extracting module banner..."
extract "$ZIPFILE" module.banner.avif "$MODPATH"

# Target architecture
case $ARCH in
"arm64") ARCH_TMP="arm64-v8a" ;;
"arm") ARCH_TMP="armeabi-v7a" ;;
"x64") ARCH_TMP="x86_64" ;;
"x86") ARCH_TMP="x86" ;;
"riscv64") ARCH_TMP="riscv64" ;;
*) abort_arch ;;
esac

# Extract daemon
ui_print "- Extracting sys.azenith-service for $ARCH_TMP"
extract "$ZIPFILE" "libs/$ARCH_TMP/sys.azenith-service" "$TMPDIR"
cp "$TMPDIR"/libs/"$ARCH_TMP"/* "$MODPATH/system/bin"
BIN_PATH="/data/adb/modules/AZenith/system/bin"
ln -sf "$MODPATH/system/bin/sys.azenith-service" "$MODPATH/system/bin/sys.azenith-service_log"
ln -sf "$MODPATH/system/bin/sys.azenith-service" "$MODPATH/system/bin/sys.azenith-profiler"
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
			ln -sf "$BIN_PATH/sys.azenith-service" "$dir/sys.azenith-service"
			ln -sf "$BIN_PATH/sys.azenith-service" "$dir/sys.azenith-service_log"
			ln -sf "$BIN_PATH/sys.azenith-service" "$dir/sys.azenith-profiler"
			ln -sf "$BIN_PATH/sys.azenith-profilesettings" "$dir/sys.azenith-profilesettings"
			ln -sf "$BIN_PATH/sys.azenith-utilityconf" "$dir/sys.azenith-utilityconf"
			ln -sf "$BIN_PATH/sys.azenith-preloadbin" "$dir/sys.azenith-preloadbin"
            ln -sf "$BIN_PATH/sys.azenith-rianixiathermalcorev4" "$dir/sys.azenith-rianixiathermalcorev4"
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
	setprop persist.sys.azenithdebug.soctype 1
	;;
*sm* | *qcom* | *SM* | *QCOM* | *Qualcomm* | *sdm* | *snapdragon*)
	soc="Snapdragon"
	ui_print "- Applying Tweaks for $soc"
	setprop persist.sys.azenithdebug.soctype 2
	;;
*exynos* | *Exynos* | *EXYNOS* | *universal* | *samsung* | *erd* | *s5e*)
	soc="Exynos"
	ui_print "- Applying Tweaks for $soc"
	setprop persist.sys.azenithdebug.soctype 3
	;;
*Unisoc* | *unisoc* | *ums*)
	soc="Unisoc"
	ui_print "- Applying Tweaks for $soc"
	setprop persist.sys.azenithdebug.soctype 4
	;;
*gs* | *Tensor* | *tensor*)
	soc="Tensor"
	ui_print "- Applying Tweaks for $soc"
	setprop persist.sys.azenithdebug.soctype 5
	;;
*)
	soc="Unknown"
	ui_print "- Applying Tweaks for $chipset"
	setprop persist.sys.azenithdebug.soctype 0
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

# Make Properties
ui_print "- Setting UP AZenith Properties"
setprop persist.sys.azenithdebug.freqlist "Disabled 90% 80% 70% 60% 50% 40%"
setprop persist.sys.azenithdebug.vsynclist "Disabled 60hz 90hz 120hz"

# Set default freqoffset if not set
if [ -z "$(getprop persist.sys.azenithconf.freqoffset)" ]; then
	setprop persist.sys.azenithconf.freqoffset "Disabled"
fi

# Set default vsync config if not set
if [ -z "$(getprop persist.sys.azenithconf.vsync)" ]; then
	setprop persist.sys.azenithconf.vsync "Disabled"
fi

# Set default color scheme if not set
if [ -z "$(getprop persist.sys.azenithconf.schemeconfig)" ]; then
	setprop persist.sys.azenithconf.schemeconfig "1000 1000 1000 1000"
fi

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
persist.sys.azenithconf.thermalcore
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

if [ -z "$(getprop persist.sys.azenithconf.preloadbudget)" ]; then
    setprop persist.sys.azenithconf.preloadbudget 500M
fi

if [ -z "$(getprop persist.sys.azenithconf.AIenabled)" ]; then
    ui_print "- Enabling Auto Mode"
    setprop persist.sys.azenithconf.AIenabled 1
fi

ui_print "- Disable Debugmode"
setprop persist.sys.azenith.debugmode "false"

ui_print "- Extracting AZenith Toast..."
extract "$ZIPFILE" azenithtoast.apk "$MODPATH"
ui_print "- Installing AZenith Toast..."
pm install "$MODPATH/azenithtoast.apk" > /dev/null 2>&1
rm "$MODPATH/azenithtoast.apk"

# Set Permissions
ui_print "- Setting Permissions..."
set_perm_recursive "$MODPATH/system/bin" 0 2000 0777 0777
chmod +x "$MODPATH/system/bin/sys.azenith-service"
chmod +x "$MODPATH/system/bin/sys.azenith-profilesettings"
chmod +x "$MODPATH/system/bin/sys.azenith-utilityconf"
chmod +x "$MODPATH/system/bin/sys.azenith-preloadbin"
chmod +x "$MODPATH/system/bin/sys.azenith-rianixiathermalcorev4"

ui_print "- Installation complete!"
