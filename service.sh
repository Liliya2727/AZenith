#!/system/bin/sh
# Wait until the system has fully booted
while [ -z "$(getprop sys.boot_completed)" ]; do
    sleep 15
done

# Log file path
LOG_FILE="/data/local/tmp/antares.log"

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" >> "$LOG_FILE"
}

# Remove old log file
rm -f "$LOG_FILE"

# Create a new log file
touch "$LOG_FILE"
log_message "Boot completed - Antares service starting..."

# Run Antares Service
Antares >/dev/null 2>&1 &

# Log Antares service execution
log_message "Antares service started."