//! Bypass charging logic.

use crate::config::resolution::restore_resolution_target;
use crate::core::{BypassNode, ProfileMode, DaemonContext};
use crate::proc::{get_pids_of_at, set_priority};
use crate::profile::{is_manual_mode, restore_per_app_overrides, update_game_info};
use crate::fileops;
use crate::{IS_FALSE, IS_TRUE, log_error, log_info, log_warn};
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

/// Write a value to a sysfs/proc node the way `echo` does, including the
/// trailing newline: a few vendor drivers only accept the terminated form.
fn write_node(path: &str, value: &str) -> std::io::Result<()> {
    let mut payload = String::with_capacity(value.len() + 1);
    payload.push_str(value);
    payload.push('\n');
    fileops::write_string(path, &payload)
}

pub fn echo_to_file(path: &str, value: &str, lock: i32) -> c_int {
    if access(path) != 0 {
        return -1;
    }
    chmod(path, 0o644);
    let res = write_node(path, value);
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
// Battery / charger state
// ---------------------------------------------------------------------------
const CHARGER_ONLINE_PATHS: [&str; 5] = [
    "/sys/class/power_supply/charger/online",
    "/sys/class/power_supply/usb/online",
    "/sys/class/power_supply/usb_switch/online",
    "/sys/class/power_supply/ac/online",
    "/sys/class/power_supply/dc/online",
];

const BATTERY_CURRENT_PATHS: [&str; 3] = [
    "/sys/class/power_supply/battery/current_now",
    "/sys/class/power_supply/battery/current_avg",
    "/sys/class/power_supply/battery/BatteryAverageCurrent",
];

const INPUT_CURRENT_PATHS: [&str; 3] = [
    "/sys/class/power_supply/usb/current_now",
    "/sys/class/power_supply/ac/current_now",
    "/sys/class/power_supply/dc/current_now",
];

/// True when a raw reading can only be microamps.
///
/// 10_000 mA (10 A) never flows through a phone battery, while 10_000 uA
/// (10 mA) is an everyday reading, so the magnitude alone is conclusive.
fn unit_from_magnitude(raw_abs: i64) -> bool {
    raw_abs >= 10_000
}

/// Normalise a raw sysfs current reading to mA with a known unit.
///
/// Returns `None` when the content is missing or not numeric, which lets
/// callers tell "unreadable" apart from a genuine 0 mA reading.
fn parse_current_ma(raw: &str, micro: bool) -> Option<i32> {
    let raw = raw.trim();
    if !raw
        .trim_start_matches(['+', '-'])
        .chars()
        .next()
        .is_some_and(|c| c.is_ascii_digit())
    {
        return None;
    }
    let val = parse_atol(raw).abs();
    Some(if micro { (val / 1000) as i32 } else { val as i32 })
}

/// Unit this device reports battery current in, learned once per process.
///
/// Judging every sample on its own magnitude breaks exactly the case that
/// matters: a working bypass reads a few thousand uA, which the ">= 10_000 is
/// uA" rule would report as thousands of mA. Learning the unit from a known
/// charging sample keeps small readings small for the rest of the run.
static CURRENT_IN_MICROAMPS: AtomicBool = AtomicBool::new(false);

/// Read a current value from sysfs and normalise it to mA.
///
/// Android exposes `current_now` either in uA or in mA depending on the
/// driver. The first reading large enough to be unambiguous fixes the unit for
/// the rest of the process (see `CURRENT_IN_MICROAMPS`).
pub fn read_ma(path: &str) -> Option<i32> {
    let raw = fileops::read_first_line(path)?;
    if unit_from_magnitude(parse_atol(raw.trim()).abs()) {
        CURRENT_IN_MICROAMPS.store(true, Ordering::Relaxed);
    }
    parse_current_ma(&raw, CURRENT_IN_MICROAMPS.load(Ordering::Relaxed))
}

/// Unit the driver was last seen reporting, for diagnostics.
pub fn current_unit_label() -> &'static str {
    if CURRENT_IN_MICROAMPS.load(Ordering::Relaxed) {
        "uA"
    } else {
        "mA"
    }
}

/// Battery current in mA, or `None` when no path is readable.
pub fn read_current_ma_checked() -> Option<i32> {
    BATTERY_CURRENT_PATHS.iter().find_map(|p| read_ma(p))
}

/// Charger input current in mA, or `None` when no path is readable.
pub fn read_input_current_ma() -> Option<i32> {
    INPUT_CURRENT_PATHS.iter().find_map(|p| read_ma(p))
}

/// Battery current in mA, with the historical `9999` sentinel on failure.
pub fn read_current_ma() -> i32 {
    read_current_ma_checked().unwrap_or(9999)
}

pub fn read_battery_status() -> Option<String> {
    fileops::read_first_line("/sys/class/power_supply/battery/status")
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
}

/// True when the battery is powering the system.
pub fn is_discharging() -> bool {
    read_battery_status()
        .map(|s| s.eq_ignore_ascii_case("Discharging"))
        .unwrap_or(false)
}

/// True when a charger is attached.
///
/// Kept separate from `is_charging()` on purpose: a phone in bypass reports
/// "Not charging" (and sometimes "Discharging") while still being plugged in,
/// and treating that as "no charger" makes callers disable bypass again on the
/// next tick.
pub fn is_plugged() -> bool {
    for path in CHARGER_ONLINE_PATHS {
        if fileops::read_int(path) == Some(1) {
            return true;
        }
    }
    match read_battery_status() {
        Some(s) => !s.eq_ignore_ascii_case("Discharging"),
        None => false,
    }
}

// ---------------------------------------------------------------------------
// Crash-safe restore (write-ahead pending restore record)
// ---------------------------------------------------------------------------
use std::sync::Once;
use std::sync::atomic::{AtomicBool, Ordering};

pub const PENDING_RESTORE_FILE: &str =
    "/data/adb/.config/AZenith/bypasschgconfig/pending_restore";

static RESTORE_REQUESTED: AtomicBool = AtomicBool::new(false);
static TEST_SIGNAL_HANDLERS: Once = Once::new();

/// Write-ahead record of a node that is about to be switched ON.
///
/// A `Drop` guard only covers normal returns and panics; it cannot cover
/// SIGKILL, a revoked root shell or a sudden reboot. The record on disk is the
/// only thing that survives those, so it is written before the node is touched
/// and replayed on the next daemon start / diagnosis run.
#[derive(Debug, Clone, PartialEq, Eq)]
struct PendingRestore {
    name: String,
    path: String,
    off_val: String,
    orig: Option<String>,
    pid: i32,
}

impl PendingRestore {
    fn render(&self) -> String {
        format!(
            "name={}\npath={}\noff={}\norig={}\npid={}\n",
            self.name,
            self.path,
            self.off_val,
            self.orig.as_deref().unwrap_or(""),
            self.pid
        )
    }

    fn parse(text: &str) -> Option<Self> {
        let mut name = None;
        let mut path = None;
        let mut off_val = None;
        let mut orig = None;
        let mut pid = 0;

        for line in text.lines() {
            let Some((key, value)) = line.split_once('=') else {
                continue;
            };
            match key.trim() {
                "name" => name = Some(value.trim().to_string()),
                "path" => path = Some(value.trim().to_string()),
                "off" => off_val = Some(value.trim().to_string()),
                "orig" => orig = Some(value.trim().to_string()).filter(|s| !s.is_empty()),
                "pid" => pid = value.trim().parse().unwrap_or(0),
                _ => {}
            }
        }

        Some(Self {
            name: name?,
            path: path?,
            off_val: off_val?,
            orig,
            pid,
        })
    }
}

fn write_pending(record: &PendingRestore) -> Result<(), String> {
    fileops::create_dir_all_best_effort(crate::core::BYPASSCHG_CONFIG);
    fileops::write_string(PENDING_RESTORE_FILE, &record.render())
        .map_err(|e| format!("cannot write pending restore record: {e}"))
}

fn remove_pending_file() {
    fileops::remove_file_best_effort(PENDING_RESTORE_FILE);
}

fn replay_pending_at(file: &str) -> Result<bool, String> {
    let Some(text) = fileops::read_string(file) else {
        return Ok(false);
    };
    let Some(record) = PendingRestore::parse(&text) else {
        fileops::remove_file_best_effort(file);
        return Err("corrupt pending restore record".to_string());
    };

    chmod(&record.path, 0o644);
    match write_node(&record.path, &record.off_val) {
        Ok(()) => {
            fileops::remove_file_best_effort(file);
            log_info!(
                "bypass",
                "pending restore applied node={} path={} value={} orig={:?} stale_pid={}",
                record.name, record.path, record.off_val, record.orig, record.pid
            );
            Ok(true)
        }
        Err(e) => Err(format!(
            "cannot restore node={} path={}: {e}",
            record.name, record.path
        )),
    }
}

/// Restore a node that an interrupted run left ON.
///
/// Also used at daemon start and before `-cbc`/`-tb`, because a diagnosis run
/// must not build on top of a latched node.
pub fn replay_pending_restore() -> bool {
    match replay_pending_at(PENDING_RESTORE_FILE) {
        Ok(true) => {
            println!("\u{1b}[32m[INFO]\u{1b}[0m Restored a charge node left ON by a previous run.");
            true
        }
        Ok(false) => false,
        Err(reason) => {
            println!("\u{1b}[33m[!] WARNING:\u{1b}[0m {}", reason);
            log_warn!("bypass", "pending restore failed reason={}", reason);
            true
        }
    }
}

/// CLI entry point (`-rp`): clean up after an interrupted test.
///
/// Deliberately independent of the daemon and of the configured bypass path: it
/// exists for the case where a root shell was killed between switching a node
/// ON and restoring it, so the user does not have to reboot.
pub fn restore_pending() -> i32 {
    if !fileops::exists(PENDING_RESTORE_FILE) {
        println!("\u{1b}[32m[INFO]\u{1b}[0m No interrupted bypass test to clean up.");
        return 0;
    }

    replay_pending_restore();

    if fileops::exists(PENDING_RESTORE_FILE) {
        println!(
            "\u{1b}[33m[!] WARNING:\u{1b}[0m Could not restore the node left ON by an interrupted test."
        );
        1
    } else {
        0
    }
}

/// Owns a node for the duration of a test and always puts it back.
pub struct BypassGuard {
    path: &'static str,
    off_val: &'static str,
    active: bool,
}

impl BypassGuard {
    /// Record the restore intent on disk, then switch the node ON.
    ///
    /// If the record cannot be written the node is left untouched: a test that
    /// cannot be undone is worse than no test. An unresolved record from an
    /// earlier failure is never clobbered, because it is the only way back for
    /// the node that failed to restore.
    pub fn arm(node: &BypassNode) -> Result<Self, String> {
        if let Err(reason) = replay_pending_at(PENDING_RESTORE_FILE) {
            log_error!(
                "bypass",
                "refusing to test node={} unresolved restore reason={}",
                node.name,
                reason
            );
            return Err(format!("unresolved pending restore: {reason}"));
        }

        let orig = fileops::read_first_line(node.path)
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty());

        let record = PendingRestore {
            name: node.name.to_string(),
            path: node.path.to_string(),
            off_val: node.off_val.to_string(),
            orig,
            pid: std::process::id() as i32,
        };
        write_pending(&record)?;

        if echo_to_file(node.path, node.on_val, 0) != 0 {
            // Keep the record when the rollback also fails: it is replayed later.
            if echo_to_file(node.path, node.off_val, 0) == 0 {
                remove_pending_file();
            }
            return Err(format!("could not write to {}", node.path));
        }

        Ok(Self {
            path: node.path,
            off_val: node.off_val,
            active: true,
        })
    }

    /// Put the node back. Idempotent; the pending record is kept if it fails.
    pub fn restore(&mut self) -> bool {
        if !self.active {
            return true;
        }
        self.active = false;

        if echo_to_file(self.path, self.off_val, 0) == 0 {
            remove_pending_file();
            log_info!("bypass", "node restored path={} value={}", self.path, self.off_val);
            true
        } else {
            log_error!(
                "bypass",
                "node restore failed path={} value={} - pending record kept for replay",
                self.path,
                self.off_val
            );
            false
        }
    }
}

impl Drop for BypassGuard {
    fn drop(&mut self) {
        self.restore();
    }
}

extern "C" fn request_restore_on_signal(_sig: c_int) {
    RESTORE_REQUESTED.store(true, Ordering::SeqCst);
}

fn install_test_signal_handlers() {
    TEST_SIGNAL_HANDLERS.call_once(|| {
        RESTORE_REQUESTED.store(false, Ordering::SeqCst);
        unsafe {
            libc::signal(libc::SIGINT, request_restore_on_signal as *const () as usize);
            libc::signal(libc::SIGTERM, request_restore_on_signal as *const () as usize);
            libc::signal(libc::SIGHUP, request_restore_on_signal as *const () as usize);
        }
    });
}

fn restore_requested() -> bool {
    RESTORE_REQUESTED.load(Ordering::SeqCst)
}

/// Sleep that notices a termination signal. Returns true when aborted, so the
/// caller can restore the node before exiting instead of dying inside a sleep.
fn sleep_interruptible(ms: u64) -> bool {
    let mut left = ms;
    while left > 0 {
        let step = left.min(50);
        std::thread::sleep(std::time::Duration::from_millis(step));
        left -= step;
        if restore_requested() {
            return true;
        }
    }
    false
}

// ---------------------------------------------------------------------------
// Node testing (shared by -cbc and -tb)
// ---------------------------------------------------------------------------

/// Seconds the node is held in its ON state before it is restored.
pub const TEST_DURATION_SECS: u32 = 10;
const TEST_SAMPLE_MS: u64 = 500;
/// Seconds ignored after switching the node ON: the charger needs a moment to
/// renegotiate, and the first samples are the noisiest.
const TEST_SETTLE_SECS: u32 = 2;
const RECOVERY_MAX_SECS: u32 = 5;
const RECOVERY_SAMPLE_MS: u64 = 250;

/// Battery current below this counts as "not charging".
const LOW_CURRENT_MA: i32 = 50;
/// How many consecutive low samples prove a drop. One sample can be a charger
/// pulse or a rounding artefact; a real bypass holds for every sample.
const LOW_WINDOW_SAMPLES: usize = 3;
/// A test is meaningless when the battery already draws less than this.
const MIN_TESTABLE_CURRENT_MA: i32 = 100;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum NodeVerdict {
    Pass,
    Aborted,
    WriteFailed,
    UnreadableCurrent,
    CurrentNotDropped { ma: i32 },
    NodeReverted { ma: i32 },
    BatteryDischarging { ma: i32 },
    InputCurrentLost { battery_ma: i32, input_ma: i32 },
    RestoreFailed { ma: i32 },
    NoRecovery { ma: i32 },
    NoBaseline { ma: i32 },
}

impl NodeVerdict {
    pub fn describe(self) -> String {
        match self {
            NodeVerdict::Pass => "bypass verified".to_string(),
            NodeVerdict::Aborted => "aborted by signal, node restored".to_string(),
            NodeVerdict::WriteFailed => "could not write to node".to_string(),
            NodeVerdict::UnreadableCurrent => "battery current unreadable".to_string(),
            NodeVerdict::CurrentNotDropped { ma } => format!("current did not drop ({ma} mA)"),
            NodeVerdict::NodeReverted { ma } => format!(
                "node was reset to its OFF value while testing ({ma} mA): the charger HAL or kernel overrides it"
            ),
            NodeVerdict::BatteryDischarging { ma } => format!(
                "battery is discharging during the test ({ma} mA): charging was killed, not bypassed"
            ),
            NodeVerdict::InputCurrentLost { battery_ma, input_ma } => format!(
                "charger input collapsed too ({input_ma} mA in, {battery_ma} mA to battery): charging was killed, not bypassed"
            ),
            NodeVerdict::RestoreFailed { ma } => format!(
                "restore write failed while current was {ma} mA: node may still be latched, reboot if charging does not resume"
            ),
            NodeVerdict::NoRecovery { ma } => format!(
                "current did not recover after restore ({ma} mA): node may still be latched"
            ),
            NodeVerdict::NoBaseline { ma } => format!(
                "battery was already at {ma} mA before the test: nothing to measure, node left untouched"
            ),
        }
    }
}

#[derive(Debug, Clone)]
pub struct NodeTestOutcome {
    pub verdict: NodeVerdict,
    pub baseline_ma: i32,
    pub battery_ma: i32,
    pub input_ma: Option<i32>,
    pub samples: usize,
    /// Samples below `LOW_CURRENT_MA` after the settling window.
    pub steady_low: usize,
    /// Samples taken after the settling window.
    pub settled_samples: usize,
    /// Battery current measured after the restore (0 when never measured).
    pub recovered_ma: i32,
}

impl NodeTestOutcome {
    fn none(verdict: NodeVerdict, baseline_ma: i32, battery_ma: i32) -> Self {
        Self {
            verdict,
            baseline_ma,
            battery_ma,
            input_ma: None,
            samples: 0,
            steady_low: 0,
            settled_samples: 0,
            recovered_ma: 0,
        }
    }
}

/// Rank a passing node with what the test actually measured.
///
/// There is no static list of "real bypass" paths that holds across ROMs, so
/// the order is derived from the run itself: how much of the observation window
/// stayed at zero current (steadiness) and how much of the charge rate came back
/// afterwards (clean restore). A node that only pulses the current down scores
/// lower than one that holds it at zero, and a clean restore breaks ties.
fn node_quality(outcome: &NodeTestOutcome) -> (i32, i32) {
    let steady = if outcome.settled_samples == 0 {
        0
    } else {
        (outcome.steady_low * 100 / outcome.settled_samples) as i32
    };
    let restored = if outcome.baseline_ma > 0 {
        outcome.recovered_ma * 100 / outcome.baseline_ma
    } else {
        0
    };
    (steady, restored)
}

fn median_ma(values: &mut [i32]) -> i32 {
    values.sort_unstable();
    values[values.len() / 2]
}

/// Longest run of consecutive samples below `LOW_CURRENT_MA`, skipping the
/// settling window. A real bypass holds low for every sample; a charge pulse
/// only produces isolated zeros.
fn longest_low_run(samples: &[i32], skip: usize) -> usize {
    let mut best = 0;
    let mut run = 0;
    for &v in samples.iter().skip(skip) {
        if v < LOW_CURRENT_MA {
            run += 1;
            best = best.max(run);
        } else {
            run = 0;
        }
    }
    best
}

fn value_or_unknown(v: Option<i32>) -> String {
    v.map(|ma| ma.to_string()).unwrap_or_else(|| "?".to_string())
}

/// Current value of the node itself, `?` when the node is write-only.
fn node_value(node: &BypassNode) -> String {
    fileops::read_first_line(node.path)
        .map(|v| v.trim().to_string())
        .filter(|v| !v.is_empty())
        .unwrap_or_else(|| "?".to_string())
}

/// True when the node is readable and has fallen back to its OFF value while
/// the test was running, i.e. something else (charger HAL, kernel, another
/// app) reverted our write.
fn node_fell_back(node: &BypassNode) -> bool {
    match fileops::read_first_line(node.path) {
        Some(v) => {
            let v = v.trim();
            v == node.off_val.trim() && v != node.on_val.trim()
        }
        None => false,
    }
}

/// Pre-flight shared by every node test.
///
/// Refuses to test when a result cannot mean anything: no charger, unreadable
/// current, or a battery that already draws almost nothing (full, or a charge
/// switch left latched by an earlier run).
pub fn test_preflight() -> Result<i32, String> {
    if !is_plugged() {
        return Err("charger not detected, plug in first".to_string());
    }
    let baseline = read_current_ma_checked()
        .ok_or_else(|| "cannot read battery current".to_string())?;
    if baseline < MIN_TESTABLE_CURRENT_MA {
        return Err(format!(
            "battery current is only {baseline} mA while plugged in: battery is full, or a charge switch is already latched. Restore the node first (or reboot) before testing"
        ));
    }
    Ok(baseline)
}

/// Current that proves charging resumed.
///
/// A latched node leaves the battery at ~0 mA, so any reading near the normal
/// charge rate is enough. Halving the baseline was too strict: after a bypass
/// the phone renegotiates the charger contract and often resumes at a third of
/// the original rate.
fn recovery_target(baseline_ma: i32) -> i32 {
    std::cmp::max(MIN_TESTABLE_CURRENT_MA, baseline_ma / 4)
}

/// Let the charger settle after the previous node was toggled.
///
/// A node test cuts charging for about twelve seconds. When it ends the charger
/// renegotiates its contract and the current climbs back over several seconds,
/// so testing again immediately would judge the next node against a battery
/// that is still recovering (347 mA instead of 2258 mA). Wait a short pause and
/// then for the current to be readable and steady again.
fn wait_for_settled_baseline() -> i32 {
    const PAUSE_MS: u64 = 3_000;
    const WAIT_MS: u64 = 15_000;
    const STEP_MS: u64 = 500;
    if sleep_interruptible(PAUSE_MS) {
        return -1;
    }
    let mut previous: Option<i32> = None;
    let mut waited = 0;
    while waited <= WAIT_MS {
        let current = read_current_ma_checked();
        if let (Some(prev), Some(now)) = (previous, current) {
            if reading_is_settled(prev, now) {
                return now;
            }
        }
        previous = current;
        if sleep_interruptible(STEP_MS) {
            return -1;
        }
        waited += STEP_MS;
    }
    read_current_ma_checked().unwrap_or(-1)
}

/// Two consecutive readings are settled when both are measurable and within
/// 15% of each other, i.e. the charger is no longer ramping back up.
fn reading_is_settled(prev: i32, now: i32) -> bool {
    if prev < MIN_TESTABLE_CURRENT_MA || now < MIN_TESTABLE_CURRENT_MA {
        return false;
    }
    (now - prev).abs() * 100 <= prev.max(now) * 15
}

/// After the node is restored, charging must come back. Without this the test
/// cannot tell "the node controls this current" from "current happened to be
/// low", and it also catches a restore that silently failed.
fn verify_recovery(baseline_ma: i32) -> (NodeVerdict, i32) {
    let target = recovery_target(baseline_ma);
    let mut best = 0;
    for _ in 0..(RECOVERY_MAX_SECS as u64 * 1000 / RECOVERY_SAMPLE_MS) {
        if sleep_interruptible(RECOVERY_SAMPLE_MS) {
            return (NodeVerdict::Aborted, best);
        }
        if let Some(v) = read_current_ma_checked() {
            best = best.max(v);
            if v >= target {
                return (NodeVerdict::Pass, best);
            }
        }
    }
    (NodeVerdict::NoRecovery { ma: best }, best)
}

/// Hold a node in its ON state and decide whether it produced a usable bypass.
///
/// A node passes only when all of these hold:
///   1. battery current drops below 50 mA (median of the samples),
///   2. the charger still feeds the system: input current stays >= 50 mA, or,
///      when input current is unreadable, the battery is not discharging,
///   3. current recovers after the node is restored.
pub fn test_node(node: &BypassNode, verbose: bool) -> NodeTestOutcome {
    install_test_signal_handlers();
    let baseline = read_current_ma_checked();
    let baseline_ma = baseline.unwrap_or(-1);

    // With no charge current there is nothing to drop, so a low reading after
    // the write proves nothing. Leaving the node untouched keeps a scan from
    // reporting fake bypasses - and from cutting a charge session for nothing.
    if baseline.is_none() {
        return NodeTestOutcome::none(NodeVerdict::UnreadableCurrent, baseline_ma, baseline_ma);
    }
    if baseline_ma < MIN_TESTABLE_CURRENT_MA {
        return NodeTestOutcome::none(
            NodeVerdict::NoBaseline { ma: baseline_ma },
            baseline_ma,
            baseline_ma,
        );
    }

    let mut guard = match BypassGuard::arm(node) {
        Ok(guard) => guard,
        Err(_) => {
            return NodeTestOutcome::none(NodeVerdict::WriteFailed, baseline_ma, baseline_ma);
        }
    };

    let total_samples = (TEST_DURATION_SECS as u64 * 1000 / TEST_SAMPLE_MS) as usize;
    let settle_samples = (TEST_SETTLE_SECS as u64 * 1000 / TEST_SAMPLE_MS) as usize;
    let mut battery: Vec<i32> = Vec::with_capacity(total_samples);
    let mut input: Vec<i32> = Vec::with_capacity(total_samples);
    let mut saw_discharging = false;
    let mut fell_back = false;
    let mut aborted = false;

    for i in 1..=total_samples {
        if sleep_interruptible(TEST_SAMPLE_MS) {
            aborted = true;
            break;
        }
        let battery_now = read_current_ma_checked();
        if let Some(v) = battery_now {
            battery.push(v);
        }
        let input_now = read_input_current_ma();
        if let Some(v) = input_now {
            input.push(v);
        }
        let discharging = is_discharging();
        saw_discharging |= discharging;
        if node_fell_back(node) {
            fell_back = true;
        }

        if verbose && i % 2 == 0 {
            println!(
                "    Checking current ({}/{}s): {} mA | avg {} mA | input {} mA | node \"{}\" | {}",
                i / 2,
                TEST_DURATION_SECS,
                value_or_unknown(battery_now),
                value_or_unknown(
                    read_ma(BATTERY_CURRENT_PATHS[1]).or_else(|| read_ma(BATTERY_CURRENT_PATHS[2]))
                ),
                value_or_unknown(input_now),
                node_value(node),
                read_battery_status().unwrap_or_else(|| "?".to_string())
            );
        }
    }

    let restored = guard.restore();

    if aborted {
        return NodeTestOutcome {
            verdict: NodeVerdict::Aborted,
            baseline_ma,
            battery_ma: battery.last().copied().unwrap_or(-1),
            input_ma: input.last().copied(),
            samples: battery.len(),
            steady_low: 0,
            settled_samples: 0,
            recovered_ma: 0,
        };
    }

    if battery.is_empty() {
        return NodeTestOutcome::none(NodeVerdict::UnreadableCurrent, baseline_ma, -1);
    }

    let samples = battery.len();
    let lowest = battery.iter().copied().min().unwrap_or(-1);
    let highest = battery.iter().copied().max().unwrap_or(-1);
    // Clamp the settling window: if only a few samples arrived (unreadable
    // node), skipping all of them would make the test impossible to pass.
    let skip = settle_samples.min(samples.saturating_sub(1));
    let low_run = longest_low_run(&battery, skip);
    let needed_low_run = LOW_WINDOW_SAMPLES.min(samples - skip);
    let bypass_seen = low_run >= needed_low_run;
    let steady_low = battery.iter().skip(skip).filter(|v| **v < LOW_CURRENT_MA).count();
    let battery_ma = median_ma(&mut battery);
    let input_ma = if input.is_empty() {
        None
    } else {
        Some(median_ma(&mut input))
    };

    if verbose {
        println!(
            "    Summary: min {} mA | median {} mA | max {} mA | longest drop {} sample(s) (need {} after {}s) | steady {}% | unit {}",
            lowest,
            battery_ma,
            highest,
            low_run,
            needed_low_run,
            TEST_SETTLE_SECS,
            if samples > skip {
                steady_low * 100 / (samples - skip)
            } else {
                0
            },
            current_unit_label()
        );
    }

    let (verdict, recovered_ma) = if !bypass_seen {
        if fell_back {
            (NodeVerdict::NodeReverted { ma: battery_ma }, 0)
        } else {
            (NodeVerdict::CurrentNotDropped { ma: battery_ma }, 0)
        }
    } else if !restored {
        (NodeVerdict::RestoreFailed { ma: battery_ma }, 0)
    } else if let Some(input_ma) = input_ma {
        if input_ma < LOW_CURRENT_MA {
            (NodeVerdict::InputCurrentLost { battery_ma, input_ma }, 0)
        } else {
            verify_recovery(baseline_ma)
        }
    } else if saw_discharging {
        (NodeVerdict::BatteryDischarging { ma: battery_ma }, 0)
    } else {
        verify_recovery(baseline_ma)
    };

    NodeTestOutcome {
        verdict,
        baseline_ma,
        battery_ma,
        input_ma,
        samples,
        steady_low,
        settled_samples: samples - skip,
        recovered_ma,
    }
}

// ---------------------------------------------------------------------------
// disable_bypass
// ---------------------------------------------------------------------------
pub fn disable_bypass(ctx: &mut DaemonContext) {
    // The write-ahead record is authoritative even when no path is configured:
    // it exists precisely because a node may still be ON.
    if fileops::exists(PENDING_RESTORE_FILE) {
        let _ = replay_pending_at(PENDING_RESTORE_FILE);
    }

    let path_key = ctx.config_bypasspath_str();

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
pub fn enable_bypass(ctx: &DaemonContext) -> i32 {
    if fileops::exists(PENDING_RESTORE_FILE) {
        log_warn!(
            "bypass",
            "enable refused: an unresolved pending restore still exists path={}",
            PENDING_RESTORE_FILE
        );
        return -3;
    }

    let path_key = ctx.config_bypasspath_str();

    if path_key.is_empty() || path_key == "UNSUPPORTED" {
        return -1;
    }

    for node in &BYPASS_LIST {
        if node.name == path_key {
            if echo_to_file(node.path, node.on_val, 1) == 0 {
                return 0;
            }
            log_warn!("bypass", "enable failed node={} path={}", path_key, node.path);
            return -4;
        }
    }
    -2
}

// ---------------------------------------------------------------------------
// check_bypass_compatibility
// ---------------------------------------------------------------------------

/// Save the last scan to disk so its result can still be inspected after the
/// console is gone or the app was closed.
fn write_check_report(lines: &[String], result: &str) -> String {
    let path = format!("{}/last_check", crate::core::BYPASSCHG_CONFIG);
    let stamp = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0);
    let mut text = format!("unix_time={stamp}\nresult={result}\n");
    for line in lines {
        text.push_str(line);
        text.push('\n');
    }
    fileops::write_string_best_effort(&path, &text);
    path
}

pub fn check_bypass_compatibility() -> i32 {
    println!("\u{1b}[36m[Standby Charge Compatibility Check]\u{1b}[0m Initializing...");

    replay_pending_restore();

    std::thread::sleep(std::time::Duration::from_secs(5));

    let baseline_ma = match test_preflight() {
        Ok(ma) => ma,
        Err(reason) => {
            println!("\u{1b}[33m[!] WARNING:\u{1b}[0m {reason}");
            log_warn!("bypass", "compatibility check aborted reason={}", reason);
            return -1;
        }
    };
    println!(
        "\u{1b}[90m[i] Baseline battery current: {} mA (driver unit {})\u{1b}[0m",
        baseline_ma,
        current_unit_label()
    );

    let total_nodes = BYPASS_LIST.len();

    let mut report: Vec<String> = Vec::new();
    let mut available: Vec<&BypassNode> = Vec::new();

    for node in &BYPASS_LIST {
        if access(node.path) != 0 {
            println!(
                "\u{1b}[90m[-] Node {:<25}: Not Found, skipping...\u{1b}[0m",
                node.name
            );
            report.push(format!(
                "{:<28} path={} verdict=path_not_found",
                node.name, node.path
            ));
        } else {
            available.push(node);
        }
    }

    let skipped_count = total_nodes - available.len();
    let available_count = available.len();

    println!(
        "\u{1b}[90m[i] {} of {} nodes exist on this device, testing every one of them\u{1b}[0m",
        available_count, total_nodes
    );

    // Every available node is tested, not just the first one that works: the
    // ranking below needs a measured comparison, and the per-node verdicts are
    // what the diagnosis report is for.
    let mut working: Vec<(&BypassNode, NodeTestOutcome)> = Vec::new();

    for (index, node) in available.iter().enumerate() {
        println!(
            "\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node ({}/{}): \u{1b}[1;37m{}\u{1b}[0m",
            index + 1,
            available_count,
            node.name
        );

        // A node that was just restored leaves the charger renegotiating, so
        // give the charge current time to come back and settle before judging.
        if index > 0 {
            let settled = wait_for_settled_baseline();
            println!(
                "\u{1b}[90m[i] Baseline before this node: {} mA\u{1b}[0m",
                if settled < 0 { "?".to_string() } else { settled.to_string() }
            );
        }

        let outcome = test_node(node, true);
        let (steady, restored_pct) = node_quality(&outcome);
        report.push(format!(
            "{:<28} path={} baseline={} median={} input={} samples={} steady={}% restore={}% verdict={}",
            node.name,
            node.path,
            outcome.baseline_ma,
            outcome.battery_ma,
            outcome.input_ma.map(|v| v.to_string()).unwrap_or_else(|| "-".to_string()),
            outcome.samples,
            steady,
            restored_pct,
            outcome.verdict.describe()
        ));

        if outcome.verdict == NodeVerdict::Aborted {
            println!("\n\u{1b}[33m[!] ABORTED\u{1b}[0m {}: {}", node.name, outcome.verdict.describe());
            log_warn!("bypass", "compatibility check aborted node={} reason=signal", node.name);
            let report_path = write_check_report(&report, "aborted");
            println!("\u{1b}[33m[INFO]\u{1b}[0m Result saved to {}", report_path);
            return -1;
        }

        if outcome.verdict == NodeVerdict::Pass {
            println!(
                "\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m {} stops charging: {} mA (baseline {} mA)",
                node.name, outcome.battery_ma, outcome.baseline_ma
            );
            log_info!(
                "bypass",
                "node accepted name={} baseline_ma={} battery_ma={} input_ma={:?} samples={}",
                node.name, outcome.baseline_ma, outcome.battery_ma, outcome.input_ma, outcome.samples
            );
            working.push((node, outcome));
        } else {
            println!("\n\u{1b}[31m[FAILED]\u{1b}[0m {}: {}", node.name, outcome.verdict.describe());
            log_warn!(
                "bypass",
                "node rejected name={} verdict={:?} baseline_ma={} battery_ma={} input_ma={:?} samples={}",
                node.name, outcome.verdict, outcome.baseline_ma, outcome.battery_ma, outcome.input_ma, outcome.samples
            );
        }

        println!("------------------------------------------");
        std::thread::sleep(std::time::Duration::from_millis(300));
    }

    if !working.is_empty() {
        // Best first: steadiest zero-current window, then the cleanest restore.
        // `sort_by` is stable, so identical scores keep the list order and the
        // choice stays deterministic across runs.
        working.sort_by(|a, b| node_quality(&b.1).cmp(&node_quality(&a.1)));

        println!(
            "\n\u{1b}[1;37m[RESULTS]\u{1b}[0m {} of {} node(s) on this device can stop charging, best first:",
            working.len(),
            available_count
        );
        for (rank, (node, outcome)) in working.iter().enumerate() {
            let (steady, restored_pct) = node_quality(outcome);
            println!(
                "  {}. \u{1b}[32m{:<24}\u{1b}[0m steady {:>3}% | restore {:>3}% | {:>5} mA",
                rank + 1,
                node.name,
                steady,
                restored_pct,
                outcome.battery_ma
            );
        }

        let (chosen, chosen_outcome) = &working[0];
        let (steady, restored_pct) = node_quality(chosen_outcome);
        println!(
            "\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Found working node: \u{1b}[1m{}\u{1b}[0m",
            chosen.name
        );
        println!(
            "\u{1b}[32m[INFO]\u{1b}[0m Best of {} working node(s): steady {}%, restore {}%.",
            working.len(),
            steady,
            restored_pct
        );
        println!(
            "\u{1b}[32m[INFO]\u{1b}[0m Process finished. {} tested, {} skipped.",
            available_count, skipped_count
        );
        println!(
            "\u{1b}[33m[INFO]\u{1b}[0m Configuration left unchanged; apply the node above to use it."
        );

        log_info!(
            "bypass",
            "compatible node found name={} working={} tested={} skipped={} steady={} restore={}",
            chosen.name, working.len(), available_count, skipped_count, steady, restored_pct
        );

        let report_path = write_check_report(&report, "compatible_node_found");
        println!("\u{1b}[32m[INFO]\u{1b}[0m Result saved to {}", report_path);

        return 0;
    }

    println!("\n\u{1b}[1;31m[-]\u{1b}[0m Final Result: No compatible standby charge node found.");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Summary: {} Scanned, {} Tested, {} Skipped.\u{1b}[0m", total_nodes, available_count, skipped_count);

    // A scan is a diagnostic, not a decision: leave the stored configuration
    // alone so the result stays visible and the path remains available for a
    // later restore.
    if let Err(reason) = replay_pending_at(PENDING_RESTORE_FILE) {
        println!("\u{1b}[1;31m[!]\u{1b}[0m {}", reason);
        log_warn!(
            "bypass",
            "compatibility check failed but restore is unresolved reason={} configuration_kept=1",
            reason
        );
    }

    let current_path = archconfig::load().standby_path;
    if current_path.is_empty() || current_path == "UNSUPPORTED" {
        println!("\u{1b}[33m[INFO]\u{1b}[0m Configuration left unchanged (no bypass path configured).");
    } else {
        println!(
            "\u{1b}[33m[INFO]\u{1b}[0m Configuration left unchanged (bypasspath={}).",
            current_path
        );
    }

    let report_path = write_check_report(&report, "no_compatible_node");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Result saved to {}", report_path);
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
            replay_pending_restore();
            return test_single_node(node);
        }
    }

    eprintln!("\u{1b}[31m[ERROR]\u{1b}[0m Bypass node not found: {name}");
    println!("\u{1b}[33m[INFO]\u{1b}[0m Use --bypasspathlist to see available nodes.");
    2
}

fn test_single_node(node: &BypassNode) -> i32 {
    println!("\n\u{1b}[36m[Standby Charge Test - Single Node]\u{1b}[0m Initializing...");

    std::thread::sleep(std::time::Duration::from_secs(2));

    let baseline_ma = match test_preflight() {
        Ok(ma) => ma,
        Err(reason) => {
            println!("\u{1b}[33m[!] WARNING:\u{1b}[0m {reason}");
            log_warn!("bypass", "single node test aborted reason={}", reason);
            return -1;
        }
    };

    if access(node.path) != 0 {
        println!("\u{1b}[31m[ERROR]\u{1b}[0m Node path not found: {}", node.path);
        return 2;
    }

    println!("\n\u{1b}[1;32m[+]\u{1b}[0m Testing Node: \u{1b}[1;37m{}\u{1b}[0m", node.name);
    println!("    Path: {}", node.path);
    println!(
        "\u{1b}[90m[i] Baseline battery current: {} mA (driver unit {})\u{1b}[0m",
        baseline_ma,
        current_unit_label()
    );

    let outcome = test_node(node, true);

    if outcome.verdict == NodeVerdict::Pass {
        let (steady, restored_pct) = node_quality(&outcome);
        println!(
            "\n\u{1b}[1;32m[SUCCESS]\u{1b}[0m Node is compatible: \u{1b}[1m{}\u{1b}[0m ({} mA, baseline {} mA, steady {}%, restore {}%)",
            node.name, outcome.battery_ma, outcome.baseline_ma, steady, restored_pct
        );
        log_info!(
            "bypass",
            "single node test success name={} baseline_ma={} battery_ma={} input_ma={:?} samples={}",
            node.name, outcome.baseline_ma, outcome.battery_ma, outcome.input_ma, outcome.samples
        );
        return 0;
    }

    if outcome.verdict == NodeVerdict::Aborted {
        println!("\n\u{1b}[33m[!] ABORTED\u{1b}[0m {}", outcome.verdict.describe());
        log_warn!("bypass", "single node test aborted name={} reason=signal", node.name);
        return -1;
    }

    println!("\n\u{1b}[31m[FAILED]\u{1b}[0m {}: {}", node.name, outcome.verdict.describe());
    log_warn!(
        "bypass",
        "single node test failed name={} verdict={:?} baseline_ma={} battery_ma={} input_ma={:?} samples={}",
        node.name, outcome.verdict, outcome.baseline_ma, outcome.battery_ma, outcome.input_ma, outcome.samples
    );
    1
}

// ---------------------------------------------------------------------------
// print_bypass_path_list
// ---------------------------------------------------------------------------
pub fn print_bypass_path_list() {
    println!("\n\u{1b}[36m[AZenith Available Standby Charge Path List]\u{1b}[0m");
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
            } else if !IS_FALSE!(ctx.opts_app_priority()) && ctx.config.app_priority {
                set_priority(ctx.game.game_pids[i]);
            }
        }

        if new_count == 0 {
            let focused = ctx.current_system_cache.focused_app_str();
            let now_ms = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis() as u64)
                .unwrap_or(0);
            let in_prelaunch = now_ms < ctx.game.prelaunch_until_ms;
            if focused == gamestart.as_str() || ctx.render.is_restarting_renderer || in_prelaunch {
                log_info!("inotify", "game pids dropped reason={} game={}",
                    if ctx.render.is_restarting_renderer { "renderer_restart" } else if in_prelaunch { "prelaunch_grace" } else { "still_focused" }, gamestart);
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
                ctx.game.prelaunched_pkg = None;
                ctx.game.prelaunch_until_ms = 0;
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
/// Hardcoded minimum battery level below which Standby Charge never pauses charging.
pub const STANDBY_CHARGE_FLOOR_PCT: i32 = 20;
/// Hysteresis window (in battery percentage points) below the locked session target
/// before charging resumes.
pub const STANDBY_CHARGE_HYSTERESIS_PCT: i32 = 2;

pub fn handle_dynamic_bypass(ctx: &mut DaemonContext) {
    let bypass_before = ctx.bypass.bypass_applied;
    let target_before = ctx.bypass.target_level;

    if ctx.config_bypasspath_str() != "UNSUPPORTED" && !ctx.config_bypasspath_str().is_empty() {
        let bypass_active_session = ctx.profile.cur_mode == ProfileMode::PerformanceProfile
            || (ctx.game.gamestart.is_some() && IS_TRUE!(ctx.opts_bypass_charging()));
        if bypass_active_session {
            let current_battery = ctx.current_system_cache.battery_level;
            let is_device_charging = ctx.current_system_cache.is_charging != 0;

            let bypass_charging_allowed = if IS_FALSE!(ctx.opts_bypass_charging()) {
                false
            } else if IS_TRUE!(ctx.opts_bypass_charging()) {
                true
            } else {
                ctx.config.standby_chg
            };

            if current_battery >= 0 && bypass_charging_allowed && is_device_charging {
                if ctx.bypass.target_level < 0 {
                    ctx.bypass.target_level = current_battery.max(STANDBY_CHARGE_FLOOR_PCT);
                    log_info!(
                        "bypass",
                        "standby charge target locked battery={} target={} floor={}",
                        current_battery,
                        ctx.bypass.target_level,
                        STANDBY_CHARGE_FLOOR_PCT
                    );
                }
                let target = ctx.bypass.target_level.max(STANDBY_CHARGE_FLOOR_PCT);

                if current_battery < STANDBY_CHARGE_FLOOR_PCT {
                    // Never pause charging below the hardcoded 20% safety floor.
                    if ctx.bypass.bypass_applied {
                        log_info!(
                            "bypass",
                            "standby charge resumed (below {}% floor) battery={} target={}",
                            STANDBY_CHARGE_FLOOR_PCT,
                            current_battery,
                            target
                        );
                        disable_bypass(ctx);
                        ctx.bypass.bypass_applied = false;
                    }
                } else if !ctx.bypass.bypass_applied {
                    if current_battery >= target && read_current_ma() > 50 && enable_bypass(ctx) == 0 {
                        log_info!(
                            "bypass",
                            "standby charge paused charging battery={} target={}",
                            current_battery,
                            target
                        );
                        ctx.bypass.bypass_applied = true;
                    }
                } else if current_battery <= target - STANDBY_CHARGE_HYSTERESIS_PCT {
                    log_info!(
                        "bypass",
                        "standby charge resumed charging battery={} target={} resume_at={}",
                        current_battery,
                        target,
                        target - STANDBY_CHARGE_HYSTERESIS_PCT
                    );
                    disable_bypass(ctx);
                    ctx.bypass.bypass_applied = false;
                }
            } else {
                if ctx.bypass.bypass_applied {
                    disable_bypass(ctx);
                    ctx.bypass.bypass_applied = false;
                }
                ctx.bypass.target_level = -1;
            }
        } else {
            if ctx.bypass.bypass_applied {
                disable_bypass(ctx);
                ctx.bypass.bypass_applied = false;
            }
            ctx.bypass.target_level = -1;
        }
    }

    if (bypass_before != ctx.bypass.bypass_applied || target_before != ctx.bypass.target_level)
        && ctx.profile.cur_mode == ProfileMode::PerformanceProfile
    {
        crate::config::state::save_daemon_state(ctx);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::os::unix::fs::MetadataExt;
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

    // -----------------------------------------------------------------------
    // current unit normalisation (detection accuracy)
    // -----------------------------------------------------------------------

    // Drivers disagree on the unit: some report mA, some uA.
    #[test]
    fn read_ma_normalises_milliamp_drivers() {
        assert_eq!(parse_current_ma("2500\n", false), Some(2500));
        assert_eq!(parse_current_ma("-1500\n", false), Some(1500));
        assert_eq!(parse_current_ma("0\n", false), Some(0));
    }

    // A bypass on a uA driver reads a few thousand uA. That must stay a few
    // mA: reporting 3600 mA made a working node look like a failed one.
    #[test]
    fn read_ma_normalises_microamp_drivers() {
        assert_eq!(parse_current_ma("2500000\n", true), Some(2500));
        assert_eq!(parse_current_ma("5227000\n", true), Some(5227));
        assert_eq!(parse_current_ma("3600\n", true), Some(3));
        assert_eq!(parse_current_ma("-3600\n", true), Some(3));
        assert_eq!(parse_current_ma("0\n", true), Some(0));
    }

    // The unit is learned from unambiguous magnitudes only, so a bypass reading
    // cannot flip an already known uA device back to mA.
    #[test]
    fn current_unit_is_learned_from_large_readings() {
        assert!(unit_from_magnitude(10_000));
        assert!(unit_from_magnitude(5_227_000));
        assert!(!unit_from_magnitude(9_999));
        assert!(!unit_from_magnitude(3_600));
        assert!(!unit_from_magnitude(0));
    }

    // Unreadable must stay distinguishable from a real 0 mA reading, otherwise
    // "cannot read current" is indistinguishable from "bypass works".
    #[test]
    fn read_ma_reports_unreadable_as_none() {
        assert_eq!(parse_current_ma("nonsense\n", false), None);
        assert_eq!(parse_current_ma("", false), None);
        assert_eq!(
            read_ma("/sys/class/power_supply/battery/does_not_exist"),
            None
        );
    }

    #[test]
    fn median_ignores_order() {
        let mut a = [3000, 0, 40];
        assert_eq!(median_ma(&mut a), 40);
        let mut b = [0, 10, 20, 30];
        assert_eq!(median_ma(&mut b), 20);
    }

    // A bypass makes the charger renegotiate, so recovery often lands well
    // below the original rate - still far above the ~0 mA of a latched node.
    #[test]
    fn recovery_target_tolerates_renegotiated_rate() {
        assert_eq!(recovery_target(2500), 625);
        assert_eq!(recovery_target(400), MIN_TESTABLE_CURRENT_MA);
        assert_eq!(recovery_target(0), MIN_TESTABLE_CURRENT_MA);
    }

    // Between two node tests the charger ramps back up. Only two readings that
    // are both measurable and close to each other count as settled.
    #[test]
    fn settle_requires_measurable_steady_current() {
        assert!(reading_is_settled(2000, 2100));
        assert!(reading_is_settled(500, 480));
        assert!(!reading_is_settled(50, 60));
        assert!(!reading_is_settled(200, 900));
    }

    // The scan has no static table of "real bypass" paths, so the ranking comes
    // from the run: a node that holds zero current for the whole window beats
    // one that only pulses down, and a clean restore breaks ties.
    #[test]
    fn quality_prefers_steady_bypass_and_clean_restore() {
        let steady = |low: usize, settled: usize, recovered: i32| NodeTestOutcome {
            verdict: NodeVerdict::Pass,
            baseline_ma: 1000,
            battery_ma: 2,
            input_ma: None,
            samples: settled,
            steady_low: low,
            settled_samples: settled,
            recovered_ma: recovered,
        };

        assert_eq!(node_quality(&steady(18, 18, 1000)), (100, 100));
        assert_eq!(node_quality(&steady(9, 18, 250)), (50, 25));
        assert_eq!(node_quality(&steady(0, 0, 0)), (0, 0));

        let mut ranked = [("pulse", steady(9, 18, 1000)), ("hold", steady(18, 18, 100))];
        ranked.sort_by(|a, b| node_quality(&b.1).cmp(&node_quality(&a.1)));
        assert_eq!(ranked[0].0, "hold");

        let mut tie = [("first", steady(18, 18, 500)), ("second", steady(18, 18, 500))];
        tie.sort_by(|a, b| node_quality(&b.1).cmp(&node_quality(&a.1)));
        assert_eq!(tie[0].0, "first");
    }

    // -----------------------------------------------------------------------
    // write-ahead restore record
    // -----------------------------------------------------------------------

    fn sample_record(path: &str, off_val: &str) -> PendingRestore {
        PendingRestore {
            name: "MTK_CURRENT_CMD".to_string(),
            path: path.to_string(),
            off_val: off_val.to_string(),
            orig: Some("0 1".to_string()),
            pid: 4242,
        }
    }

    #[test]
    fn pending_record_roundtrip() {
        let record = sample_record("/proc/mtk_battery_cmd/current_cmd", "0 0");
        assert_eq!(PendingRestore::parse(&record.render()), Some(record));
    }

    #[test]
    fn pending_record_tolerates_missing_orig() {
        let text = "name=NODE\npath=/sys/x\noff=1\norig=\npid=7\n";
        let parsed = PendingRestore::parse(text).unwrap();
        assert_eq!(parsed.orig, None);
        assert_eq!(parsed.pid, 7);
    }

    #[test]
    fn pending_record_requires_path_and_off() {
        assert_eq!(PendingRestore::parse("name=NODE\npid=7\n"), None);
    }

    // Replay must actually write the OFF value, not just delete the record.
    #[test]
    fn replay_restores_node_and_clears_record() {
        let node = tmp_file("node", "0 1");
        let pending = tmp_file("pending", &sample_record(node.to_str().unwrap(), "0 0").render());

        assert_eq!(replay_pending_at(pending.to_str().unwrap()), Ok(true));
        assert_eq!(std::fs::read_to_string(&node).unwrap(), "0 0\n");
        assert!(!pending.exists());
    }

    // If the write fails the record has to survive, otherwise the only memory
    // of what needs restoring is gone.
    #[test]
    fn replay_keeps_record_when_write_fails() {
        use std::os::unix::fs::PermissionsExt;

        // Unwritable target: a directory. It lives in its own subdir because
        // replay chmods the target, and that must not hit the shared temp dir.
        let dir = std::env::temp_dir().join(format!("azenith_restore_{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let pending = tmp_file(
            "pending",
            &sample_record(dir.to_str().unwrap(), "0 0").render(),
        );

        assert!(replay_pending_at(pending.to_str().unwrap()).is_err());
        assert!(pending.exists());

        let _ = std::fs::set_permissions(&dir, std::fs::Permissions::from_mode(0o755));
        let _ = std::fs::remove_dir_all(&dir);
    }

    #[test]
    fn replay_drops_corrupt_record() {
        let pending = tmp_file("pending", "not a record");
        assert!(replay_pending_at(pending.to_str().unwrap()).is_err());
        assert!(!pending.exists());
    }

    #[test]
    fn replay_without_record_is_a_noop() {
        let missing = std::env::temp_dir().join("azenith_pending_missing");
        assert_eq!(replay_pending_at(missing.to_str().unwrap()), Ok(false));
    }

    // A single low sample is a charge pulse; only a window counts as a drop.
    #[test]
    fn longest_low_run_needs_consecutive_samples() {
        assert_eq!(longest_low_run(&[0, 2400, 0, 1200, 0], 0), 1);
        assert_eq!(longest_low_run(&[2400, 0, 0, 0, 1200], 0), 3);
        assert_eq!(longest_low_run(&[0, 0, 0, 0], 0), 4);
        assert_eq!(longest_low_run(&[2400; 10], 0), 0);
    }

    #[test]
    fn longest_low_run_skips_settling_samples() {
        // High current at the start is the charger renegotiating, not a result.
        assert_eq!(longest_low_run(&[3600, 2400, 0, 0, 0], 2), 3);
        assert_eq!(longest_low_run(&[0, 0, 1200, 1200], 2), 0);
    }

    // A verdict must never claim success without the charger still feeding the
    // system; these are the cases that used to be reported as "working node".
    #[test]
    fn verdicts_describe_the_failure() {
        assert!(NodeVerdict::BatteryDischarging { ma: 0 }.describe().contains("discharging"));
        assert!(NodeVerdict::InputCurrentLost { battery_ma: 0, input_ma: 0 }
            .describe()
            .contains("collapsed"));
        assert!(NodeVerdict::NoRecovery { ma: 0 }.describe().contains("latched"));
        assert!(NodeVerdict::RestoreFailed { ma: 0 }.describe().contains("restore write failed"));
        assert!(NodeVerdict::NodeReverted { ma: 0 }.describe().contains("reset to its OFF value"));
        assert!(NodeVerdict::NoBaseline { ma: 3 }.describe().contains("already at 3 mA"));
        assert_eq!(NodeVerdict::Pass.describe(), "bypass verified");
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
        // get_pids_of_at validates the pid against /proc, so use this live
        // process (with its real uid) instead of an arbitrary number.
        let pid = std::process::id() as i32;
        let uid = std::fs::metadata("/proc/self").map(|m| m.uid() as i32).unwrap_or(10001);
        let f = tmp_file("bgapps", &format!("com.test.game {pid} {uid}\n"));
        handle_background_apps_event_at(&mut ctx, f.to_str().unwrap());
        assert_eq!(ctx.game.game_pid_count, 1);
        assert_eq!(ctx.game.game_pids[0], pid);
        assert_eq!(ctx.game.gamestart.as_deref(), Some("com.test.game"));
    }
}
