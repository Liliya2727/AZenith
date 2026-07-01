/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <AZenith.h>
#include <dirent.h>
#include <string.h>

BypassNode bypass_list[] = {
    {"COMMON_INPUT_SUSPEND", "/sys/class/power_supply/battery/input_suspend", "1", "0"},
    {"COMMON_BATT_INPUT_SUSPEND", "/sys/class/power_supply/battery/battery_input_suspend", "1", "0"},
    {"COMMON_CHG_CONTROL", "/sys/class/power_supply/battery/charger_control", "0", "1"},
    {"COMMON_CHG_DISABLE", "/sys/class/power_supply/battery/charge_disable", "1", "0"},
    {"COMMON_CHG_ENABLED_V1", "/sys/class/power_supply/battery/charging_enabled", "0", "1"},
    {"COMMON_CHG_ENABLED_V2", "/sys/class/power_supply/battery/charge_enabled", "0", "1"},
    {"COMMON_BATT_CHG_ENABLED", "/sys/class/power_supply/battery/battery_charging_enabled", "0", "1"},
    {"COMMON_DEVICE_CHG_EN", "/sys/class/power_supply/battery/device/Charging_Enable", "0", "1"},

    {"MTK_BYPASS_CHG", "/sys/devices/platform/charger/bypass_charger", "1", "0"},
    {"MTK_CURRENT_CMD", "/proc/mtk_battery_cmd/current_cmd", "0 1", "0 0"},
    {"TRAN_AICHG_DISABLE", "/sys/devices/platform/charger/tran_aichg_disable_charger", "1", "0"},
    {"MTK_DISABLE_BATTERY_CHG", "/sys/devices/platform/mt-battery/disable_charger", "1", "0"},
    {"MTK_ADV_PATH", "/proc/mtk_battery_cmd/en_power_path", "0", "1"},

    {"OPLUS_MMI_1", "/sys/class/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_2", "/sys/class/power_supply/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_3", "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_MMI_SOC", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/mmi_charging_enable", "0", "1"},
    {"OPLUS_EXP_CHG_ENABLE", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/chg_enable", "0", "1"},
    {"OPLUS_COOLDOWN_STATE", "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/cool_down", "1", "0"},

    {"AC_CHG_ENABLED", "/sys/class/power_supply/ac/charging_enabled", "0", "1"},
    {"CHG_DATA_ENABLE", "/sys/class/power_supply/charge_data/enable_charger", "0", "1"},
    {"DC_CHG_ENABLED", "/sys/class/power_supply/dc/charging_enabled", "0", "1"},
    {"OP_DISABLE_CHG", "/sys/class/power_supply/battery/op_disable_charge", "1", "0"},
    {"CHGALG_DISABLE_CHG", "/sys/class/power_supply/chargalg/disable_charging", "1", "0"},
    {"BATT_CONNECT_DISABLE", "/sys/class/power_supply/battery/connect_disable", "1", "0"},
    {"I2C_CHG_ENABLE", "/sys/devices/platform/omap/omap_i2c.3/i2c-3/3-005f/charge_enable", "0", "1"},
    {"QPNP_SMB_BATT_EN", "/sys/devices/soc/qpnp-smbcharger-18/power_supply/battery/battery_charging_enabled", "0", "1"},

    {"QCOM_SUSPEND", "/sys/class/qcom-battery/input_suspend", "0", "1"},
    {"QCOM_EN_CHG", "/sys/class/qcom-battery/charging_enabled", "0", "1"},
    {"QCOM_COOL_MODE", "/sys/class/qcom-battery/cool_mode", "1", "0"},
    {"QCOM_PROTECT_EN", "/sys/class/qcom-battery/batt_protect_en", "1", "0"},
    {"QCOM_PMIC_GLINK_SUSPEND",
     "/sys/devices/platform/soc/soc:qcom,pmic_glink/soc:qcom,pmic_glink:qcom,battery_charger/"
     "force_charger_suspend",
     "1", "0"},

    {"PM8058_DISABLE", "/sys/module/pmic8058_charger/parameters/disabled", "1", "0"},
    {"PM8921_DISABLE", "/sys/module/pm8921_charger/parameters/disabled", "1", "0"},
    {"SMB137B_DISABLE", "/sys/module/smb137b/parameters/disabled", "1", "0"},
    {"SMB1357_DISABLE_PROC", "/proc/smb1357_disable_chrg", "1", "0"},
    {"BQ2589X_EN_CHG", "/sys/class/power_supply/bq2589x_charger/enable_charging", "0", "1"},

    {"PIXEL_CHG_DISABLE", "/sys/devices/platform/soc/soc:google,charger/charge_disable", "1", "0"},
    {"PIXEL_DEBUG_SUSPEND", "/sys/kernel/debug/google_charger/chg_suspend", "1", "0"},
    {"PIXEL_INPUT_SUSPEND", "/sys/kernel/debug/google_charger/input_suspend", "1", "0"},
    {"PIXEL_STOP_LEVEL", "/sys/devices/platform/google,charger/charge_stop_level", "5", "100"},
    {"PIXEL_CHG_MODE", "/sys/kernel/debug/google_charger/chg_mode", "0", "1"},

    {"SAM_STORE_MODE", "/sys/class/power_supply/battery/store_mode", "1", "0"},
    {"LGE_CHG_ENABLE", "/sys/devices/platform/lge-unified-nodes/charging_enable", "0", "1"},
    {"LGE_CHG_COMPLETED", "/sys/devices/platform/lge-unified-nodes/charging_completed", "1", "0"},
    {"LGE_STOP_LEVEL", "/sys/module/lge_battery/parameters/charge_stop_level", "5", "100"},

    {"ASUS_LIMIT_EN", "/sys/class/asuslib/charger_limit_en", "1", "0"},
    {"ASUS_SUSPEND_EN", "/sys/class/asuslib/charging_suspend_en", "1", "0"},
    {"HUAWEI_CHG_EN_1", "/sys/devices/platform/huawei_charger/enable_charger", "0", "1"},
    {"HUAWEI_CHG_EN_2", "/sys/class/hw_power/charger/charge_data/enable_charger", "0", "1"},

    {"NUBIA_BYPASS_MODE", "/sys/kernel/nubia_charge/charger_bypass", "on", "off"},
    {"MANTA_CHG_EN", "/sys/devices/virtual/power_supply/manta-battery/charge_enabled", "0", "1"},
    {"CAT_CHG_SWITCH", "/sys/devices/platform/battery/CCIChargerSwitch", "0", "1"},
    {"SPREADTRUM_STOP_CHG", "/sys/class/power_supply/battery/stop_charge", "1", "0"},
    {"TEGRA_I2C_STATE", "/sys/devices/platform/tegra12-i2c.0/i2c-0/0-006b/charging_state", "disabled", "enabled"},

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
    {"RESTRICTED_CHG_WIRELESS", "/sys/class/power_supply/wireless/restricted_charging", "1", "0"}};
const int bypass_list_size = sizeof(bypass_list) / sizeof(BypassNode);
