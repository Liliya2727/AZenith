#!/system/bin/sh

# Path to performance mode flag
gameenabled="/data/Antares/perfflagtmp"

MODULE_PROP="/data/adb/modules/Antares/module.prop"

# Function to print messages with delay
delayed_echo() {
    echo "$1"
    sleep 1
}

# Display banner
clear
echo ""
echo "==============================="
echo "           𝐀𝐍𝐓𝐀𝐑𝐄𝐒!               "
echo "==============================="
echo ""
delayed_echo "Checking Performance Mode Status..."

# Toggle Performance Mode
if [ -f "$gameenabled" ]; then
    delayed_echo "- Disabling Performance Mode..."
    delayed_echo ""
    delayed_echo "- Antares Performance Mode is now OFF."
    delayed_echo "- Games will run without performance enhancements."
    rm -f "$gameenabled"

    if [ -f "$MODULE_PROP" ]; then
        sed -i "s|^description=.*|description=Performance module designed for D8050! Recommend to use kernel that support Schedhorizon and Disable Thermal!                PerfMode: Disabled!❌|" "$MODULE_PROP"
    else
        echo "Error: module.prop not found!" >&2
    fi
else
    delayed_echo "- Enabling Performance Mode..."
    delayed_echo ""
    delayed_echo "- Antares Performance Mode is now ON!"
    delayed_echo "- Performance optimization will be applied in games."
    touch "$gameenabled"

    if [ -f "$MODULE_PROP" ]; then
        sed -i "s|^description=.*|description=Performance module designed for D8050! Recommend to use kernel that support Schedhorizon and Disable Thermal!                PerfMode: Enabled!✅|" "$MODULE_PROP"
    else
        echo "Error: module.prop not found!" >&2
    fi
fi

echo ""
delayed_echo "Done!"
echo ""