//! Bypass charging logic.

use crate::android_ffi::PROP_VALUE_MAX;
use crate::config::resolution::restore_resolution_target;
use crate::core::{BypassNode, ProfileMode, DaemonContext};
use crate::proc::{get_pids_of_at, set_priority};
use crate::profile::{is_manual_mode, restore_per_app_overrides, update_game_info};
use crate::sysprop::{get_or_default, get_into, set};
use crate::fileops;
use crate::{IS_FALSE, IS_TRUE, log_info, log_warn};
use std::os::raw::c_int;

// ---------------------------------------------------------------------------
// bypass_list
// ---------------------------------------------------------------------------
pub const BYPASS_LIST: [BypassNode; 78] = [
    BypassNode { name: "COMMON_INPUT_SUSPEND", path: "/sys/class/power_supply/battery/input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "COMMON_BATT_INPUT_SUSPEND", path: "/sys/class/power_supply/battery/battery_input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "COMMON_CHG_CONTROL", path: "/sys/class/power_supply/battery/charger_control", on_val: "0", off_val: "1" },
    BypassNode { name: "COMMON_CHG_DISABLE", path: "/sys/class/power_supply/battery/charge_disable", on_val: "1", off_val: "0" },
    BypassNode { name: "COMMON_CHG_ENABLED_V1", path: "/sys/class/power_supply/battery/charging_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "COMMON_CHG_ENABLED_V2", path: "/sys/class/power_supply/battery/charge_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "COMMON_BATT_CHG_ENABLED", path: "/sys/class/power_supply/battery/battery_charging_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "COMMON_DEVICE_CHG_EN", path: "/sys/class/power_supply/battery/device/Charging_Enable", on_val: "0", off_val: "1" },

    BypassNode { name: "MTK_BYPASS_CHG", path: "/sys/devices/platform/charger/bypass_charger", on_val: "1", off_val: "0" },
    BypassNode { name: "MTK_CURRENT_CMD", path: "/proc/mtk_battery_cmd/current_cmd", on_val: "0 1", off_val: "0 0" },
    BypassNode { name: "TRAN_AICHG_DISABLE", path: "/sys/devices/platform/charger/tran_aichg_disable_charger", on_val: "1", off_val: "0" },
    BypassNode { name: "MTK_DISABLE_BATTERY_CHG", path: "/sys/devices/platform/mt-battery/disable_charger", on_val: "1", off_val: "0" },
    BypassNode { name: "MTK_ADV_PATH", path: "/proc/mtk_battery_cmd/en_power_path", on_val: "0", off_val: "1" },

    BypassNode { name: "OPLUS_MMI_1", path: "/sys/class/oplus_chg/battery/mmi_charging_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "OPLUS_MMI_2", path: "/sys/class/power_supply/battery/mmi_charging_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "OPLUS_MMI_3", path: "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "OPLUS_MMI_SOC", path: "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/mmi_charging_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "OPLUS_EXP_CHG_ENABLE", path: "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/chg_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "OPLUS_COOLDOWN_STATE", path: "/sys/devices/platform/soc/soc:oplus,chg_intf/oplus_chg/battery/cool_down", on_val: "1", off_val: "0" },

    BypassNode { name: "AC_CHG_ENABLED", path: "/sys/class/power_supply/ac/charging_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "CHG_DATA_ENABLE", path: "/sys/class/power_supply/charge_data/enable_charger", on_val: "0", off_val: "1" },
    BypassNode { name: "DC_CHG_ENABLED", path: "/sys/class/power_supply/dc/charging_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "OP_DISABLE_CHG", path: "/sys/class/power_supply/battery/op_disable_charge", on_val: "1", off_val: "0" },
    BypassNode { name: "CHGALG_DISABLE_CHG", path: "/sys/class/power_supply/chargalg/disable_charging", on_val: "1", off_val: "0" },
    BypassNode { name: "BATT_CONNECT_DISABLE", path: "/sys/class/power_supply/battery/connect_disable", on_val: "1", off_val: "0" },
    BypassNode { name: "I2C_CHG_ENABLE", path: "/sys/devices/platform/omap/omap_i2c.3/i2c-3/3-005f/charge_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "QPNP_SMB_BATT_EN", path: "/sys/devices/soc/qpnp-smbcharger-18/power_supply/battery/battery_charging_enabled", on_val: "0", off_val: "1" },

    BypassNode { name: "QCOM_SUSPEND", path: "/sys/class/qcom-battery/input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "QCOM_EN_CHG", path: "/sys/class/qcom-battery/charging_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "QCOM_COOL_MODE", path: "/sys/class/qcom-battery/cool_mode", on_val: "1", off_val: "0" },
    BypassNode { name: "QCOM_PROTECT_EN", path: "/sys/class/qcom-battery/batt_protect_en", on_val: "1", off_val: "0" },
    BypassNode { name: "QCOM_BATT_PROTECTED", path: "/sys/class/qcom-battery/battery_protected", on_val: "1", off_val: "0" },
    BypassNode { name: "QCOM_PMIC_GLINK_SUSPEND", path: "/sys/devices/platform/soc/soc:qcom,pmic_glink/soc:qcom,pmic_glink:qcom,battery_charger/force_charger_suspend", on_val: "1", off_val: "0" },

    BypassNode { name: "PM8058_DISABLE", path: "/sys/module/pmic8058_charger/parameters/disabled", on_val: "1", off_val: "0" },
    BypassNode { name: "PM8921_DISABLE", path: "/sys/module/pm8921_charger/parameters/disabled", on_val: "1", off_val: "0" },
    BypassNode { name: "SMB137B_DISABLE", path: "/sys/module/smb137b/parameters/disabled", on_val: "1", off_val: "0" },
    BypassNode { name: "SMB1357_DISABLE_PROC", path: "/proc/smb1357_disable_chrg", on_val: "1", off_val: "0" },
    BypassNode { name: "BQ2589X_EN_CHG", path: "/sys/class/power_supply/bq2589x_charger/enable_charging", on_val: "0", off_val: "1" },

    BypassNode { name: "PIXEL_CHG_DISABLE", path: "/sys/devices/platform/soc/soc:google,charger/charge_disable", on_val: "1", off_val: "0" },
    BypassNode { name: "PIXEL_DEBUG_SUSPEND", path: "/sys/kernel/debug/google_charger/chg_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "PIXEL_INPUT_SUSPEND", path: "/sys/kernel/debug/google_charger/input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "PIXEL_STOP_LEVEL", path: "/sys/devices/platform/google,charger/charge_stop_level", on_val: "5", off_val: "100" },
    BypassNode { name: "PIXEL_CHG_MODE", path: "/sys/kernel/debug/google_charger/chg_mode", on_val: "0", off_val: "1" },

    BypassNode { name: "SAM_STORE_MODE", path: "/sys/class/power_supply/battery/store_mode", on_val: "1", off_val: "0" },
    BypassNode { name: "LGE_CHG_ENABLE", path: "/sys/devices/platform/lge-unified-nodes/charging_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "LGE_CHG_COMPLETED", path: "/sys/devices/platform/lge-unified-nodes/charging_completed", on_val: "1", off_val: "0" },
    BypassNode { name: "LGE_STOP_LEVEL", path: "/sys/module/lge_battery/parameters/charge_stop_level", on_val: "5", off_val: "100" },

    BypassNode { name: "ASUS_LIMIT_EN", path: "/sys/class/asuslib/charger_limit_en", on_val: "1", off_val: "0" },
    BypassNode { name: "ASUS_SUSPEND_EN", path: "/sys/class/asuslib/charging_suspend_en", on_val: "1", off_val: "0" },
    BypassNode { name: "HUAWEI_CHG_EN_1", path: "/sys/devices/platform/huawei_charger/enable_charger", on_val: "0", off_val: "1" },
    BypassNode { name: "HUAWEI_CHG_EN_2", path: "/sys/class/hw_power/charger/charge_data/enable_charger", on_val: "0", off_val: "1" },

    BypassNode { name: "NUBIA_BYPASS_MODE", path: "/sys/kernel/nubia_charge/charger_bypass", on_val: "on", off_val: "off" },
    BypassNode { name: "MANTA_CHG_EN", path: "/sys/devices/virtual/power_supply/manta-battery/charge_enabled", on_val: "0", off_val: "1" },
    BypassNode { name: "CAT_CHG_SWITCH", path: "/sys/devices/platform/battery/CCIChargerSwitch", on_val: "0", off_val: "1" },
    BypassNode { name: "SPREADTRUM_STOP_CHG", path: "/sys/class/power_supply/battery/stop_charge", on_val: "1", off_val: "0" },
    BypassNode { name: "TEGRA_I2C_STATE", path: "/sys/devices/platform/tegra12-i2c.0/i2c-0/0-006b/charging_state", on_val: "disabled", off_val: "enabled" },

    BypassNode { name: "SIOP_LEVEL_CTRL", path: "/sys/class/power_supply/battery/siop_level", on_val: "0", off_val: "100" },
    BypassNode { name: "SMART_INTERRUPT_CHG", path: "/sys/class/power_supply/battery_ext/smart_charging_interruption", on_val: "1", off_val: "0" },
    BypassNode { name: "CHG_LIMIT_ENABLE", path: "/proc/driver/charger_limit_enable", on_val: "1", off_val: "0" },
    BypassNode { name: "CHG_LIMIT_VAL", path: "/proc/driver/charger_limit", on_val: "5", off_val: "100" },
    BypassNode { name: "QPNP_ADAPTIVE_BLOCK", path: "/sys/module/qpnp_adaptive_charge/parameters/blocking", on_val: "1", off_val: "0" },
    BypassNode { name: "BATT_TEST_MODE", path: "/sys/class/power_supply/battery/test_mode", on_val: "1", off_val: "2" },
    BypassNode { name: "BATT_SLATE_MODE", path: "/sys/class/power_supply/battery/batt_slate_mode", on_val: "1", off_val: "0" },
    BypassNode { name: "BATT_DEFENDER_CNT", path: "/sys/class/power_supply/battery/bd_trickle_cnt", on_val: "1", off_val: "0" },
    BypassNode { name: "IDT_PIN_EN", path: "/sys/class/power_supply/idt/pin_enabled", on_val: "1", off_val: "0" },
    BypassNode { name: "CHG_STATE_CTRL", path: "/sys/class/power_supply/battery/charge_charger_state", on_val: "1", off_val: "0" },
    BypassNode { name: "ADAPTER_CC_MODE", path: "/sys/class/power_supply/main/adapter_cc_mode", on_val: "1", off_val: "0" },
    BypassNode { name: "HMT_TA_CHG", path: "/sys/class/power_supply/battery/hmt_ta_charge", on_val: "0", off_val: "1" },
    BypassNode { name: "MAXFG_OFF_CHG", path: "/sys/class/power_supply/maxfg/offmode_charger", on_val: "1", off_val: "0" },
    BypassNode { name: "COOL_MODE_MAIN", path: "/sys/class/power_supply/main/cool_mode", on_val: "1", off_val: "0" },
    BypassNode { name: "RESTRICTED_CHG_BATT", path: "/sys/class/power_supply/battery/restricted_charging", on_val: "1", off_val: "0" },
    BypassNode { name: "RESTRICTED_CHG_WIRELESS", path: "/sys/class/power_supply/wireless/restricted_charging", on_val: "1", off_val: "0" },
    BypassNode { name: "XIAOMI_MCA_INPUT_SUSPEND_V1", path: "/sys/devices/platform/soc/soc:mca_charge_interface/input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "XIAOMI_MCA_CHARGE_ENABLE_V1", path: "/sys/devices/platform/soc/soc:mca_charge_interface/charge_enable", on_val: "0", off_val: "1" },
    BypassNode { name: "XIAOMI_MCA_STOP_HANDLE_V1", path: "/sys/devices/platform/soc/soc:mca_business_charger/stop_handle_charge", on_val: "1", off_val: "0" },
    BypassNode { name: "XIAOMI_XM_INPUT_SUSPEND", path: "/sys/class/xm_power/charger/charge_interface/input_suspend", on_val: "1", off_val: "0" },
    BypassNode { name: "XIAOMI_XM_STOP_HANDLE", path: "/sys/class/xm_power/charger/charger_common/stop_handle_charge", on_val: "1", off_val: "0" },
    BypassNode { name: "XIAOMI_XM_CHARGE_ENABLE", path: "/sys/class/xm_power/charger/charge_interface/charge_enable", on_val: "0", off_val: "1" },
];

#[allow(dead_code)]
pub const BYPASS_LIST_SIZE: usize = BYPASS_LIST.len();

// ---------------------------------------------------------------------------
// echo_to_file
// ---------------------------------------------------------------------------
pub fn echo_to_file(path: &str, value: &str, lock: i32) -> c_int {
    if access(path) != 0 {
        return -1;
    }
    chmod(path, 0o644);
    let res = fileops::write_string(path, value);
    if lock != 0 {
        chmod(path, 0o444);
    }
    if res.is_err() { -1 } else { 0 }
}

fn access(path: &str) -> i32 {
    use std::ffi::CString;
    let c = match CString::new(path) {
        Ok(c) => c,
        Err(_) => return -1,
    };
    unsafe { libc::access(c.as_ptr(), 0) } // F_OK
}

fn chmod(path: &str, mode: u32) {
    use std::ffi::CString;
    let c = match CString::new(path) {
        Ok(c) => c,
        Err(_) => return,
    };
    unsafe { libc::chmod(c.as_ptr(), mode as libc::mode_t) };
}

// ---------------------------------------------------------------------------
// is_charging
// ---------------------------------------------------------------------------
pub fn is_charging() -> i32 {
    let mut status = [0u8; 32];
    let fd = unsafe { libc::open("/sys/class/power_supply/battery/status\0".as_ptr() , libc::O_RDONLY) };
    if fd < 0 {
        return 0;
    }
    let _ = unsafe { libc::read(fd, status.as_mut_ptr() as *mut _, status.len() - 1) };
    unsafe { libc::close(fd) };
    let s = unsafe { std::ffi::CStr::from_ptr(status.as_ptr() ) }.to_string_lossy();
    if s.contains("Charging") || s.contains("Full") {
        1
    } else {
        0
    }
}

// ---------------------------------------------------------------------------
// parse_atol — mimic C atol(): parse leading numeric prefix, stop at non-digit
// ---------------------------------------------------------------------------
fn parse_atol(s: &str) -> i64 {
    let trimmed = s.trim();
    let mut result: i64 = 0;
    let mut sign = 1i64;
    let mut chars = trimmed.chars().peekable();

    // Optional sign
    if let Some(&c) = chars.peek() {
        if c == '-' {
            sign = -1;
            chars.next();
        } else if c == '+' {
            chars.next();
        }
    }

    // Parse digits
    for c in chars {
        if let Some(d) = c.to_digit(10) {
            result = result * 10 + d as i64;
        } else {
            break;
        }
    }

    sign * result
}

// ---------------------------------------------------------------------------
// read_current_ma
// ---------------------------------------------------------------------------
pub fn read_current_ma() -> i32 {
    let current_paths = [
        "/sys/class/power_supply/battery/current_now\0",
        "/sys/class/power_supply/battery/BatteryAverageCurrent\0",
    ];
    for path in &current_paths {
        let c_path = std::ffi::CString::new(path.trim_end_matches('\0')).unwrap();
        let fd = unsafe { libc::open(c_path.as_ptr(), libc::O_RDONLY) };
        if fd >= 0 {
            let mut buf = [0u8; 32];
            let _ = unsafe { libc::read(fd, buf.as_mut_ptr() as *mut _, buf.len() - 1) };
            unsafe { libc::close(fd) };
            let s = unsafe { std::ffi::CStr::from_ptr(buf.as_ptr() ) }.to_string_lossy();
            let val = parse_atol(s.trim());
            let val = if val < 0 { -val } else { val };
            return if val > 1000 { (val / 1000) as i32 } else { val as i32 };
        }
    }
    9999
}

// ---------------------------------------------------------------------------
// disable_bypass
// ---------------------------------------------------------------------------
pub fn disable_bypass(_ctx: &mut DaemonContext) {
    // bypass path is stored in prop "persist.sys.azenithconf.bypasspath"
    let path_key = get_or_default("persist.sys.azenithconf.bypasspath", "");

    if path_key.is_empty() || path_key == "UNSUPPORTED" {
        return;
    }

    for node in &BYPASS_LIST {
        if node.name == path_key {
            let res = echo_to_file(node.path, node.off_val, 0);
            log_info!("bypass", "bypass disabled node={} path={} result={}", path_key, node.path, res);
            return;
        }
    }
    log_warn!("bypass", "disable requested but node={} not in table", path_key);
}

// ---------------------------------------------------------------------------
// enable_bypass
// ---------------------------------------------------------------------------
pub fn enable_bypass() -> i32 {
    let path_key = get_or_default("persist.sys.azenithconf.bypasspath", "");

    if path_key.is_empty() || path_key == "UNSUPPORTED" {
        return -1;
    }

    for node in &BYPASS_LIST {
        if node.name == path_key {
            echo_to_file(node.path, node.on_val, 1);
            return 0;
        }
    }
    -2
}

// ---------------------------------------------------------------------------
// check_bypass_compatibility
// ---------------------------------------------------------------------------
pub fn check_bypass_compatibility() -> i32 {
    println!("\u{1b}[36m[Bypass Charge Compatibility Check]\u{1b}[0m Initializing...");

    std::thread::sleep(std::time::Duration::from_secs(5));

    if is_charging() == 0 {
        println!("\u{1b}[33m[!] WARNING:\u{1b}[0m Charger not detected. Plug in first!");
        log_warn!("bypass", "compatibility check aborted reason=charger_disconnected");
        return -1;
    }

    let total_nodes = BYPASS_LIST.len();

    let mut skipped_count = 0;
    let mut tested_count = 0;

    for node in &BYPASS_LIST {
        if access(node.path) != 0 {
            println!("\u{1b}[90m[-] Node {:<25}: Not Found, skipping...\u{1b}[0m", node.name);
            skipped_count += 1;
            continue;
        }

        tested_count += 1;
        println!("\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node ({}/{}): \u{1b}[1;37m{}\u{1b}[0m", tested_count, total_nodes, node.name);
        echo_to_file(node.path, node.on_val, 0);

        let mut last_ma = 9999;
        for sec in 1..=10 {
            std::thread::sleep(std::time::Duration::from_secs(1));
            last_ma = read_current_ma();
            println!("    Checking current ({sec}s/10s): \u{1b}[33m{} mA\u{1b}[0m", last_ma);
        }

        echo_to_file(node.path, node.off_val, 0);

        if last_ma < 50 {
            println!("\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Found working node: \u{1b}[1m{}\u{1b}[0m", node.name);
            println!("\u{1b}[32m[INFO]\u{1b}[0m Process finished. {} nodes skipped.", skipped_count);

            log_info!("bypass", "compatible node found name={} skipped={}", node.name, skipped_count);

            return 0;
        } else {
            println!("\u{1b}[31m[FAILED]\u{1b}[0m Current drop test failed for {} ({} mA)", node.name, last_ma);
            println!("------------------------------------------");
            std::thread::sleep(std::time::Duration::from_millis(300));
        }
    }

    println!("\n\u{1b}[1;31m[-]\u{1b}[0m Final Result: No compatible bypass node found.");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Summary: {} Scanned, {} Tested, {} Skipped.\u{1b}[0m", total_nodes, tested_count, skipped_count);

    set("persist.sys.azenithconf.bypasspath", "UNSUPPORTED");
    set("persist.sys.azenithconf.bypasschg", "0");
    set("persist.sys.azenithconf.bypasschgthreshold", "20");
    let _ = std::fs::write(format!("{}/bypasspath", crate::core::BYPASSCHG_CONFIG), "UNSUPPORTED");
    let _ = std::fs::write(format!("{}/bypasschg", crate::core::BYPASSCHG_CONFIG), "0");
    let _ = std::fs::write(format!("{}/bypasschgthreshold", crate::core::BYPASSCHG_CONFIG), "20");
    1
}

// ---------------------------------------------------------------------------
// test_bypass_node_by_name
// ---------------------------------------------------------------------------
pub fn test_bypass_node_by_name(name: &str) -> i32 {
    if name.is_empty() {
        eprintln!("\u{1b}[31m[ERROR]\u{1b}[0m No bypass node name provided.");
        return 1;
    }

    for node in &BYPASS_LIST {
        if node.name == name {
            return test_single_node(node);
        }
    }

    eprintln!("\u{1b}[31m[ERROR]\u{1b}[0m Bypass node not found: {name}");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Use --bypasspathlist to see available nodes.");
    2
}

fn test_single_node(node: &BypassNode) -> i32 {
    println!("\n\u{1b}[36m[Bypass Charge Test - Single Node]\u{1b}[0m Initializing...");

    std::thread::sleep(std::time::Duration::from_secs(2));

    if is_charging() == 0 {
        println!("\u{1b}[33m[!] WARNING:\u{1b}[0m Charger not detected. Plug in first!");
        log_warn!("bypass", "single node test aborted reason=charger_disconnected");
        return -1;
    }

    if access(node.path) != 0 {
        println!("\u{1b}[31m[ERROR]\u{1b}[0m Node path not found: {}", node.path);
        return 2;
    }

    println!("\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node: \u{1b}[1;37m{}\u{1b}[0m", node.name);
    println!("    Path: {}", node.path);

    echo_to_file(node.path, node.on_val, 0);

    let mut last_ma = 9999;
    for sec in 1..=10 {
        std::thread::sleep(std::time::Duration::from_secs(1));
        last_ma = read_current_ma();
        println!("    Checking current ({sec}s/10s): \u{1b}[33m{} mA\u{1b}[0m", last_ma);
    }

    echo_to_file(node.path, node.off_val, 0);

    if last_ma < 50 {
        println!("\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Node is compatible: \u{1b}[1m{}\u{1b}[0m (Last: {} mA)", node.name, last_ma);
        log_info!("bypass", "single node test success name={} last_ma={}", node.name, last_ma);
        return 0;
    } else {
        println!("\n\u{1b}[31m[FAILED]\u{1b}[0m Node not compatible: {} (Last: {} mA)", node.name, last_ma);
        log_warn!("bypass", "single node test failed name={} last_ma={}", node.name, last_ma);
        return 1;
    }
}

// ---------------------------------------------------------------------------
// print_bypass_path_list
// ---------------------------------------------------------------------------
pub fn print_bypass_path_list() {
    println!("\n\u{1b}[36m[AZenith Available Bypass Path List]\u{1b}[0m");
    println!("---------------------------------------------------------------------------------------");
    println!(" {:<30} | {:<10} | {}", "NODE NAME", "STATUS", "SYSFS/PROC PATH");
    println!("---------------------------------------------------------------------------------------");

    let mut found_count = 0;

    for node in &BYPASS_LIST {
        if access(node.path) == 0 {
            println!(" \u{1b}[1;32m{:<30}\u{1b}[0m | \u{1b}[32m[FOUND]\u{1b}[0m    | {}", node.name, node.path);
            found_count += 1;
        } else {
            println!(" \u{1b}[90m{:<30} | [NOT FOUND] | {}\u{1b}[0m", node.name, node.path);
        }
    }

    println!("---------------------------------------------------------------------------------------");
    println!("\u{1b}[36m[SUMMARY]\u{1b}[0m Total Nodes: {} | Available on this device: \u{1b}[1;32m{}\u{1b}[0m\n", BYPASS_LIST.len(), found_count);
}

// ---------------------------------------------------------------------------
// handle_background_apps_event
// ---------------------------------------------------------------------------
pub fn handle_background_apps_event(ctx: &mut DaemonContext) {
    handle_background_apps_event_at(ctx, "/data/adb/.config/AZenith/background_apps")
}

pub fn handle_background_apps_event_at(ctx: &mut DaemonContext, path: &str) {
    let gamestart = match ctx.game.gamestart.clone() {
        Some(g) => g,
        None => return,
    };

    let mut new_pids = [0i32; 2];
    let max_track_pids = 2;
    let new_count = get_pids_of_at(&gamestart, path, &mut new_pids, max_track_pids);

    let new_count = if new_count == 0 && ctx.game.game_pid_count > 0 {
        if let Ok(st) = std::fs::metadata(path) {
            if st.len() == 0 {
                // C copies the old PIDs so the diff below sees "unchanged".
                for i in 0..ctx.game.game_pid_count as usize {
                    new_pids[i] = ctx.game.game_pids[i];
                }
                ctx.game.game_pid_count
            } else {
                0
            }
        } else {
            0
        }
    } else {
        new_count
    };

    let mut pids_changed = false;
    if new_count != ctx.game.game_pid_count {
        pids_changed = true;
    } else {
        for i in 0..new_count {
            let mut found = false;
            for j in 0..ctx.game.game_pid_count {
                if new_pids[i as usize] == ctx.game.game_pids[j as usize] {
                    found = true;
                    break;
                }
            }
            if !found {
                pids_changed = true;
                break;
            }
        }
    }

    if pids_changed {
        if new_count > 0 {
            log_info!("inotify", "tracking pids count={} game={}", new_count, ctx.game.active_app_name.as_deref().unwrap_or(gamestart.as_str()));
        } else {
            log_info!("inotify", "game pids cleared game={}", ctx.game.active_app_name.as_deref().unwrap_or(gamestart.as_str()));
        }

        ctx.game.game_pid_count = new_count;
        for i in 0..new_count as usize {
            ctx.game.game_pids[i] = new_pids[i];
            if IS_TRUE!(ctx.opts_app_priority()) {
                set_priority(ctx.game.game_pids[i]);
            } else if !IS_FALSE!(ctx.opts_app_priority()) {
                let mut val = [0u8; PROP_VALUE_MAX];
                let _ = get_into("persist.sys.azenithconf.iosched", &mut val);
                let val = unsafe { std::ffi::CStr::from_ptr(val.as_ptr() ) }.to_string_lossy();
                if val == "1" {
                    set_priority(ctx.game.game_pids[i]);
                }
            }
        }

        if new_count == 0 {
            let focused = ctx.current_system_cache.focused_app_str();
            if focused == gamestart.as_str() || ctx.render.is_restarting_renderer {
                log_info!("inotify", "game pids dropped reason={} game={}",
                    if ctx.render.is_restarting_renderer { "renderer_restart" } else { "still_focused" }, gamestart);
            } else {
                let manual = is_manual_mode(ctx);
                log_info!("inotify", "game closed action={} game={}",
                    if manual { "manual_close" } else { "exit_performance" }, gamestart);
                restore_resolution_target(ctx, &gamestart);
                if manual {
                    // Manual mode has no auto profile switch to clean up, so
                    // restore the per-app overrides (refresh rate, DND,
                    // renderer) directly here.
                    restore_per_app_overrides(ctx);
                }
                ctx.game.gamestart = None;
                ctx.game.active_app_name = None;
                ctx.power.fg_away_active = false;
                ctx.render.manual_sidepanel_shown = false;
                ctx.power.fg_away_timer = 0;
            }
        }

        update_game_info(ctx);
    }
}

// ---------------------------------------------------------------------------
// handle_dynamic_bypass
// ---------------------------------------------------------------------------
pub fn handle_dynamic_bypass(ctx: &mut DaemonContext) {
    let bypass_before = ctx.bypass.bypass_applied;

    if ctx.config_bypasspath_str() != "UNSUPPORTED" && !ctx.config_bypasspath_str().is_empty() {
        if ctx.profile.cur_mode == ProfileMode::PerformanceProfile {
            let threshold = ctx.bypass.config_bypasschgthreshold;
            let current_battery = ctx.current_system_cache.battery_level;
            let is_device_charging = ctx.current_system_cache.is_charging != 0;

            let bypass_charging_allowed = if IS_FALSE!(ctx.opts_bypass_charging()) {
                false
            } else if IS_TRUE!(ctx.opts_bypass_charging()) {
                true
            } else {
                ctx.bypass.config_bypasschg == 1
            };

            if current_battery >= 0 && bypass_charging_allowed && is_device_charging {
                if current_battery >= threshold {
                    if read_current_ma() > 50 {
                        enable_bypass();
                        if !ctx.bypass.bypass_applied {
                            log_info!("bypass", "bypass enabled battery={} threshold={}", current_battery, threshold);
                            ctx.bypass.bypass_applied = true;
                        }
                    }
                } else if ctx.bypass.bypass_applied {
                    log_info!("bypass", "bypass disabled battery={} threshold={}", current_battery, threshold);
                    disable_bypass(ctx);
                    ctx.bypass.bypass_applied = false;
                }
            } else if ctx.bypass.bypass_applied {
                disable_bypass(ctx);
                ctx.bypass.bypass_applied = false;
            }
        } else if ctx.bypass.bypass_applied {
            disable_bypass(ctx);
            ctx.bypass.bypass_applied = false;
        }
    }

    if bypass_before != ctx.bypass.bypass_applied && ctx.profile.cur_mode == ProfileMode::PerformanceProfile {
        crate::config::state::save_daemon_state(ctx);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicU32, Ordering};

    static SEQ: AtomicU32 = AtomicU32::new(0);

    fn tmp_file(tag: &str, content: &str) -> std::path::PathBuf {
        let n = SEQ.fetch_add(1, Ordering::Relaxed);
        let p = std::env::temp_dir().join(format!(
            "azenith_test_{}_{}_{}",
            std::process::id(),
            tag,
            n
        ));
        std::fs::write(&p, content).unwrap();
        p
    }

    fn ctx_with_game() -> DaemonContext {
        let mut ctx = DaemonContext::new();
        crate::core::init_globals();
        ctx.game.gamestart = Some("com.test.game".to_string());
        ctx.game.active_app_name = Some("Test Game".to_string());
        ctx.game.game_pid_count = 1;
        ctx.game.game_pids[0] = 4242;
        ctx
    }

    // C copies the old PIDs when the file is empty, so the diff sees
    // "unchanged" and keeps them. The old Rust skipped the copy and
    // overwrote the tracked PID with 0.
    #[test]
    fn empty_background_preserves_pids() {
        let mut ctx = ctx_with_game();
        let empty = tmp_file("bgapps", "");
        handle_background_apps_event_at(&mut ctx, empty.to_str().unwrap());
        assert_eq!(ctx.game.game_pid_count, 1);
        assert_eq!(ctx.game.game_pids[0], 4242);
        assert_eq!(ctx.game.gamestart.as_deref(), Some("com.test.game"));
    }

    // Normal case: file lists a different live PID -> tracked PIDs update,
    // the game is NOT dropped.
    #[test]
    fn background_updates_pids_without_drop() {
        let mut ctx = ctx_with_game();
        let f = tmp_file("bgapps", "com.test.game 7777 10001\n");
        handle_background_apps_event_at(&mut ctx, f.to_str().unwrap());
        assert_eq!(ctx.game.game_pid_count, 1);
        assert_eq!(ctx.game.game_pids[0], 7777);
        assert_eq!(ctx.game.gamestart.as_deref(), Some("com.test.game"));
    }
}
