#!/bin/sh

# Magisk Module Installation Script for Antares

# Configuration flags
SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=false
LATESTARTSERVICE=true

# List of system paths to replace (if any)
REPLACE=""

# Function to print messages with a delay
delayed_print() {
    ui_print "$1"
    sleep 1
}

# Gather system information
DEVICE_NAME=$(getprop ro.product.board)
CPU_ARCH=$(getprop ro.product.cpu.abi)

# Display banner
ui_print
ui_print "          𝐀𝐍𝐓𝐀𝐑𝐄𝐒!            "
ui_print
delayed_print "- Release Date : 02/03/2025"
delayed_print "- Author       : @Zexshia"
delayed_print "- Version      : 2.0"
delayed_print "- Device       : $DEVICE_NAME"
delayed_print "- CPU Arch     : $CPU_ARCH"
delayed_print "- Installing Antares!"

# Creating necessary directories
mkdir -p /data/Antares
touch /data/Antares/perfflagtmp

# Extracting module files
delayed_print "- Extracting module files..."
unzip -o "$ZIPFILE" 'common/system.prop' -d "$MODPATH" >&2
unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2
unzip -o "$ZIPFILE" 'action.sh' -d "$MODPATH" >&2
unzip -o "$ZIPFILE" 'service.sh' -d "$MODPATH" >&2
unzip -o "$ZIPFILE" 'gamelist.txt' -d "/data/Antares" >&2
unzip -o "$ZIPFILE" 'antares.png' -d "/data/local/tmp" >&2

# Installing toast notification
delayed_print "- Installing Bellavita Toast..."
unzip -o "$ZIPFILE" 'toast.apk' -d "$MODPATH" >&2
pm install "$MODPATH/toast.apk"
rm "$MODPATH/toast.apk"

# Setting permissions
set_perm_recursive "$MODPATH" 0 0 0777 0777

ui_print "- Installation complete!"