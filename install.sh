#!/bin/sh

# Magisk Module Installation Script for Antares

# Configuration flags
SKIPMOUNT=false
PROPFILE=false
POSTFSDATA=false
LATESTARTSERVICE=true

# List of system paths to replace (if any)
REPLACE=""

# Log file path
LOG_FILE="/data/local/tmp/AntaresInstall.log"

# Function for structured logging
log_message() {
    local level="$1"
    local message="$2"
    local timestamp
    timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# Function to print messages with a delay
delayed_print() {
    ui_print "$1"
    sleep 1
}

# Remove previous log file
rm -f "$LOG_FILE"

# Gather system information
DEVICE_NAME=$(getprop ro.product.board)
CPU_ARCH=$(getprop ro.product.cpu.abi)

# Start logging
log_message "INFO" "Starting Antares installation..."
log_message "INFO" "Device: $DEVICE_NAME, Architecture: $CPU_ARCH"

# Display banner
ui_print
ui_print "          𝐀𝐍𝐓𝐀𝐑𝐄𝐒!            "
ui_print
delayed_print "- Release Date : 02/03/2025"
ui_print "- Author       : @Zexshia"
delayed_print "- Version      : 2.0"
delayed_print "- Device       : $DEVICE_NAME $CPU_ARCH"
delayed_print "- Installing Antares!"

# Creating necessary directories
if mkdir -p /data/Antares && touch /data/Antares/perfflagtmp; then
    log_message "INFO" "Created /data/Antares directory and perfflagtmp file."
else
    log_message "ERROR" "Failed to create /data/Antares directory or perfflagtmp file."
fi

# Extracting module files
delayed_print "- Extracting module files..."
if unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2; then
    log_message "INFO" "Extracted system files."
else
    log_message "ERROR" "Failed to extract system files."
fi

if unzip -o "$ZIPFILE" 'action.sh' -d "$MODPATH" >&2; then
    log_message "INFO" "Extracted action.sh."
else
    log_message "ERROR" "Failed to extract action.sh."
fi

if unzip -o "$ZIPFILE" 'service.sh' -d "$MODPATH" >&2; then
    log_message "INFO" "Extracted service.sh."
else
    log_message "ERROR" "Failed to extract service.sh."
fi

if unzip -o "$ZIPFILE" 'gamelist.txt' -d "/data/Antares" >&2; then
    log_message "INFO" "Extracted gamelist.txt."
else
    log_message "ERROR" "Failed to extract gamelist.txt."
fi

if unzip -o "$ZIPFILE" 'antares.png' -d "/data/local/tmp" >&2; then
    log_message "INFO" "Extracted antares.png."
else
    log_message "ERROR" "Failed to extract antares.png."
fi

# Installing toast notification
delayed_print "- Installing Bellavita Toast..."
if unzip -o "$ZIPFILE" 'toast.apk' -d "$MODPATH" >&2; then
    log_message "INFO" "Extracted toast.apk."
else
    log_message "ERROR" "Failed to extract toast.apk."
fi

if pm install "$MODPATH/toast.apk"; then
    log_message "INFO" "Installed toast.apk."
else
    log_message "ERROR" "Failed to install toast.apk."
fi

if rm "$MODPATH/toast.apk"; then
    log_message "INFO" "Removed toast.apk from MODPATH."
else
    log_message "WARNING" "Failed to remove toast.apk from MODPATH."
fi

# Setting permissions
if set_perm_recursive "$MODPATH" 0 0 0777 0777; then
    log_message "INFO" "Set permissions for $MODPATH."
else
    log_message "ERROR" "Failed to set permissions for $MODPATH."
fi

ui_print "- Installation complete!"
log_message "INFO" "Antares installation completed successfully."