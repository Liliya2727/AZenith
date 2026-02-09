/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/system_properties.h>
#include <AZenith.h>

BypassNode bypass_list[] = {

    // --- COMMON SUSPEND & CONTROL ---
    {"COMMON_INPUT_SUSPEND", "/sys/class/power_supply/battery/input_suspend", "1", "0"},
    {"COMMON_BATT_INPUT_SUSPEND", "/sys/class/power_supply/battery/battery_input_suspend", "1", "0"},
    {"COMMON_CHG_CONTROL", "/sys/class/power_supply/battery/charger_control", "0", "1"},
    {"COMMON_CHG_DISABLE", "/sys/class/power_supply/battery/charge_disable", "1", "0"},
    {"COMMON_CHG_ENABLED_V1", "/sys/class/power_supply/battery/charging_enabled", "0", "1"},
    {"COMMON_CHG_ENABLED_V2", "/sys/class/power_supply/battery/charge_enabled", "0", "1"},
    {"COMMON_BATT_CHG_ENABLED", "/sys/class/power_supply/battery/battery_charging_enabled", "0", "1"},
    {"COMMON_DEVICE_CHG_EN", "/sys/class/power_supply/battery/device/Charging_Enable", "0", "1"},
    
    // --- MTK NODES ---
    {"MTK_BYPASS_CHG", "/sys/devices/platform/charger/bypass_charger", "1", "0"},
    {"MTK_CURRENT_CMD", "/proc/mtk_battery_cmd/current_cmd", "0 1", "0 0"},
    {"TRAN_AICHG_DISABLE", "/sys/devices/platform/charger/tran_aichg_disable_charger", "1", "0"},
    {"MTK_DISABLE_BATTERY_CHG", "/sys/devices/platform/mt-battery/disable_charger", "1", "0"},
    {"MTK_ADV_PATH", "/proc/mtk_battery_cmd/en_power_path", "0", "1"},

    // --- OPLUS / OPPO / REALME NODES ---
    {"OPLUS_MMI_1", "/sys/class/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_2", "/sys/class/power_supply/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_3", "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_SOC", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_EXP_CHG_ENABLE", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/chg_enable", "0", "1"},
    {"OPLUS_COOLDOWN_STATE", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/cool_down", "1", "0"},    

    // --- DISABLE COMMON NODES ---
    {"AC_CHG_ENABLED", "/sys/class/power_supply/ac/charging_enabled", "0", "1"},
    {"CHG_DATA_ENABLE", "/sys/class/power_supply/charge_data/enable_charger", "0", "1"},
    {"DC_CHG_ENABLED", "/sys/class/power_supply/dc/charging_enabled", "0", "1"},
    {"OP_DISABLE_CHG", "/sys/class/power_supply/battery/op_disable_charge", "1", "0"},
    {"CHGALG_DISABLE_CHG", "/sys/class/power_supply/chargalg/disable_charging", "1", "0"},
    {"BATT_CONNECT_DISABLE", "/sys/class/power_supply/battery/connect_disable", "1", "0"},
    {"I2C_CHG_ENABLE", "/sys/devices/platform/omap/omap_i2c.3/i2c-3/3-005f/charge_enable", "0", "1"},
    {"QPNP_SMB_BATT_EN", "/sys/devices/soc/qpnp-smbcharger-18/power_supply/battery/battery_charging_enabled", "0", "1"},

    // --- QUALCOMM SPECIFIC ---
    {"QCOM_SUSPEND", "/sys/class/qcom-battery/input_suspend", "0", "1"},
    {"QCOM_EN_CHG", "/sys/class/qcom-battery/charging_enabled", "0", "1"},
    {"QCOM_COOL_MODE", "/sys/class/qcom-battery/cool_mode", "1", "0"},
    {"QCOM_PROTECT_EN", "/sys/class/qcom-battery/batt_protect_en", "1", "0"},
    {"QCOM_PMIC_GLINK_SUSPEND", "/sys/devices/platform/soc/soc:qcom,pmic_glink/soc:qcom,pmic_glink:qcom,battery_charger/force_charger_suspend", "1", "0"},

    // --- PMIC PARAMETERS ---
    {"PM8058_DISABLE", "/sys/module/pmic8058_charger/parameters/disabled", "1", "0"},
    {"PM8921_DISABLE", "/sys/module/pm8921_charger/parameters/disabled", "1", "0"},
    {"SMB137B_DISABLE", "/sys/module/smb137b/parameters/disabled", "1", "0"},
    {"SMB1357_DISABLE_PROC", "/proc/smb1357_disable_chrg", "1", "0"},
    {"BQ2589X_EN_CHG", "/sys/class/power_supply/bq2589x_charger/enable_charging", "0", "1"},

    // --- GOOGLE PIXEL ---
    {"PIXEL_CHG_DISABLE", "/sys/devices/platform/soc/soc:google,charger/charge_disable", "1", "0"},
    {"PIXEL_DEBUG_SUSPEND", "/sys/kernel/debug/google_charger/chg_suspend", "1", "0"},
    {"PIXEL_INPUT_SUSPEND", "/sys/kernel/debug/google_charger/input_suspend", "1", "0"},
    {"PIXEL_STOP_LEVEL", "/sys/devices/platform/google,charger/charge_stop_level", "5", "100"},
    {"PIXEL_CHG_MODE", "/sys/kernel/debug/google_charger/chg_mode", "0", "1"},

    // --- SAMSUNG & LGE ---
    {"SAM_STORE_MODE", "/sys/class/power_supply/battery/store_mode", "1", "0"},
    {"LGE_CHG_ENABLE", "/sys/devices/platform/lge-unified-nodes/charging_enable", "0", "1"},
    {"LGE_CHG_COMPLETED", "/sys/devices/platform/lge-unified-nodes/charging_completed", "1", "0"},
    {"LGE_STOP_LEVEL", "/sys/module/lge_battery/parameters/charge_stop_level", "5", "100"},

    // --- ASUS & HUAWEI ---
    {"ASUS_LIMIT_EN", "/sys/class/asuslib/charger_limit_en", "1", "0"},
    {"ASUS_SUSPEND_EN", "/sys/class/asuslib/charging_suspend_en", "1", "0"},
    {"HUAWEI_CHG_EN_1", "/sys/devices/platform/huawei_charger/enable_charger", "0", "1"},
    {"HUAWEI_CHG_EN_2", "/sys/class/hw_power/charger/charge_data/enable_charger", "0", "1"},

    // --- MISC BRANDS ---
    {"NUBIA_BYPASS_MODE", "/sys/kernel/nubia_charge/charger_bypass", "on", "off"},
    {"MANTA_CHG_EN", "/sys/devices/virtual/power_supply/manta-battery/charge_enabled", "0", "1"},
    {"CAT_CHG_SWITCH", "/sys/devices/platform/battery/CCIChargerSwitch", "0", "1"},
    {"SPREADTRUM_STOP_CHG", "/sys/class/power_supply/battery/stop_charge", "1", "0"},
    {"TEGRA_I2C_STATE", "/sys/devices/platform/tegra12-i2c.0/i2c-0/0-006b/charging_state", "disabled", "enabled"},

    // --- SPECIAL CONTROLS ---
    {"SIOP_LEVEL_CTRL", "/sys/class/power_supply/battery/siop_level", "0", "100"},
    {"SMART_INTERRUPT_CHG", "/sys/class/power_supply/battery_ext/smart_charging_interruption", "1", "0"},
    {"CHG_LIMIT_ENABLE", "/proc/driver/charger_limit_enable", "1", "0"},
    {"CHG_LIMIT_VAL", "/proc/driver/charger_limit", "5", "100"},
    {"QPNP_ADAPTIVE_BLOCK", "/sys/module/qpnp_adaptive_charge/parameters/blocking", "1", "0"},
    {"BATT_TEST_MODE", "/sys/class/power_supply/battery/test_mode", "1", "2"},
    {"BATT_SLATE_MODE", "/sys/class/power_supply/battery/batt_slate_mode", "1", "0"},
    {"BATT_DEFENDER_CNT", "/sys/class/power_supply/battery/bd_trickle_cnt", "1", "0"},
    {"IDT_PIN_EN", "/sys/class/power_supply/idt/pin_enabled", "1", "0"},
    {"CHG_STATE_CTRL", "/sys/class/power_supply/battery/charge_charger_state", "1", "0"},
    {"ADAPTER_CC_MODE", "/sys/class/power_supply/main/adapter_cc_mode", "1", "0"},
    {"HMT_TA_CHG", "/sys/class/power_supply/battery/hmt_ta_charge", "0", "1"},
    {"MAXFG_OFF_CHG", "/sys/class/power_supply/maxfg/offmode_charger", "1", "0"},
    {"COOL_MODE_MAIN", "/sys/class/power_supply/main/cool_mode", "1", "0"},
    {"RESTRICTED_CHG_BATT", "/sys/class/power_supply/battery/restricted_charging", "1", "0"},
    {"RESTRICTED_CHG_WIRELESS", "/sys/class/power_supply/wireless/restricted_charging", "1", "0"}
};

/***********************************************************************************
 * Function Name      : echo_to_file
 * Inputs             : path (const char*), value (const char*), lock (int)
 * Returns            : int - Status result of systemv execution
 * Description        : Writes a value to a sysfs/proc node and optionally sets 
 * it to read-only (chmod 0444) to lock the value.
 ***********************************************************************************/
int echo_to_file(const char* path, const char* value, int lock) {
    if (access(path, F_OK) != 0) return -1;
    chmod(path, 0644);
    int res = systemv("echo '%s' > %s", value, path);
    if (lock) chmod(path, 0444);
    return res;
}

/***********************************************************************************
 * Function Name      : is_charging
 * Inputs             : None
 * Returns            : int - 1 if charging/full, 0 otherwise
 * Description        : Checks the battery status via sysfs to determine if a 
 * power source is connected.
 ***********************************************************************************/
int is_charging() {
    char status[32] = {0};
    int fd = open("/sys/class/power_supply/battery/status", O_RDONLY);
    if (fd < 0) return 0;
    read(fd, status, sizeof(status) - 1);
    close(fd);
    return (strstr(status, "Charging") || strstr(status, "Full")) ? 1 : 0;
}

/***********************************************************************************
 * Function Name      : read_current_ma
 * Inputs             : None
 * Returns            : int - Battery current in mA (positive value)
 * Description        : Reads battery current from common sysfs paths and 
 * normalizes the value to milliAmperes.
 ***********************************************************************************/
int read_current_ma() {
    const char* current_paths[] = {
        "/sys/class/power_supply/battery/current_now",
        "/sys/class/power_supply/battery/BatteryAverageCurrent"
    };
    for (int i = 0; i < 2; i++) {
        int fd = open(current_paths[i], O_RDONLY);
        if (fd >= 0) {
            char buf[32] = {0};
            read(fd, buf, sizeof(buf) - 1);
            close(fd);
            long val = atol(buf);
            if (val < 0) val = -val;
            return (val > 1000) ? (int)(val / 1000) : (int)val;
        }
    }
    return 9999;
}

/***********************************************************************************
 * Function Name      : disable_bypass
 * Inputs             : None
 * Returns            : void
 * Description        : Retrieves the saved bypass path from system properties and 
 * restores the node to its normal charging state.
 ***********************************************************************************/
void disable_bypass() {
    char path_key[PROP_VALUE_MAX];
    __system_property_get("persist.sys.azenithconf.bypasspath", path_key);

    if (strlen(path_key) == 0 || strcmp(path_key, "UNSUPPORTED") == 0) return;

    int total_nodes = sizeof(bypass_list) / sizeof(BypassNode);
    for (int i = 0; i < total_nodes; i++) {
        if (strcmp(bypass_list[i].name, path_key) == 0) {
            echo_to_file(bypass_list[i].path, bypass_list[i].off_val, 0);
            log_zenith(LOG_INFO, "Bypass Charging Disabled");
            return;
        }
    }
}

/***********************************************************************************
 * Function Name      : enable_bypass_logic
 * Inputs             : None
 * Returns            : int - 0 on success, -1 if unsupported, -2 if node not found
 * Description        : Sets the active bypass node to its 'on' value and locks it 
 * to bypass the battery charging circuit.
 ***********************************************************************************/
int enable_bypass_logic() {
    char path_key[PROP_VALUE_MAX];
    __system_property_get("persist.sys.azenithconf.bypasspath", path_key);

    if (strlen(path_key) == 0 || strcmp(path_key, "UNSUPPORTED") == 0) return -1;

    int total_nodes = sizeof(bypass_list) / sizeof(BypassNode);
    for (int i = 0; i < total_nodes; i++) {
        if (strcmp(bypass_list[i].name, path_key) == 0) {
            echo_to_file(bypass_list[i].path, bypass_list[i].on_val, 1);
            return 0; 
        }
    }
    return -2;
}

/***********************************************************************************
 * Function Name      : check_bypass_compatibility
 * Inputs             : None
 * Returns            : int - 0 if found, 1 if not found, -1 if no charger
 * Description        : Iterates through the bypass_list to find a working node.
 * Prints specific node names that are skipped due to 
 * non-existent paths.
 ***********************************************************************************/
int check_bypass_compatibility() {
    printf("\n\033[36m[Bypass Charge Compatibility Check]\033[0m Initializing...\n");

    if (!is_charging()) {
        printf("\033[33m[!] WARNING:\033[0m Charger not detected. Plug in first!\n");
        log_zenith(LOG_WARN, "Connect charger to check compatibility");
        return -1;
    }

    int total_nodes = sizeof(bypass_list) / sizeof(BypassNode);
    int skipped_count = 0;
    int tested_count = 0;

    for (int i = 0; i < total_nodes; i++) {
        // Logika Skip dengan Pesan Nama Node
        if (access(bypass_list[i].path, F_OK) != 0) {
            // \033[90m itu warna abu-abu (gray)
            printf("\033[90m[-] Node %-25s: Not Found, skipping...\033[0m\n", bypass_list[i].name);
            skipped_count++;
            continue; 
        }

        // Jika path ditemukan, mulai testing
        tested_count++;
        printf("\n\033[1;32m[+]\033[0m Testing Node (%d/%d): \033[1;37m%s\033[0m\n", i + 1, total_nodes, bypass_list[i].name);
        echo_to_file(bypass_list[i].path, bypass_list[i].on_val, 0);

        int last_ma = 9999;
        for (int sec = 1; sec <= 10; sec++) {
            sleep(1);
            last_ma = read_current_ma();
            printf("    Checking current (%ds/10s): \033[33m%d mA\033[0m\n", sec, last_ma);
        }

        // Kembalikan ke normal setelah test
        echo_to_file(bypass_list[i].path, bypass_list[i].off_val, 0);

        if (last_ma < 50) {
            printf("\n\033[1;32m[SUCCESS]\033[0m Found working node: \033[1m%s\033[0m\n", bypass_list[i].name);
            printf("\033[32m[INFO]\033[0m Process finished. %d nodes skipped.\n", skipped_count);
            
            __system_property_set("persist.sys.azenithconf.bypasspath", bypass_list[i].name);
            log_zenith(LOG_INFO, "Compatible path found: %s. Tested: $d, Skipped: %d", bypass_list[i].name, tested_count, skipped_count);
            return 0;
        } else {
            printf("\033[31m[FAILED]\033[0m Current drop test failed for %s (%d mA)\n", bypass_list[i].name, last_ma);
            printf("------------------------------------------\n");
            usleep(300000); 
        }
    }

    printf("\n\033[1;31m[-]\033[0m Final Result: No compatible bypass node found.\n");
    printf("\033[33m[INFO]\033[0m Summary: %d Scanned, %d Tested, %d Skipped.\033[0m\n", total_nodes, tested_count, skipped_count);
    
    __system_property_set("persist.sys.azenithconf.bypasspath", "UNSUPPORTED");
    __system_property_set("persist.sys.azenithconf.bypasschg", "0");
    __system_property_set("persist.sys.azenithconf.bypasschgthreshold", "20");
    return 1;
}

int get_battery_level() {
    int capacity = 0;
    FILE *fp = fopen("/sys/class/power_supply/battery/capacity", "r");
    if (fp) {
        fscanf(fp, "%d", &capacity);
        fclose(fp);
    }
    return capacity;
}
