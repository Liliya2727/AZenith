pub const PROP_BIGDATA_PATH: &str = "persist.sys.rianixia.thermalcore-bigdata.path";
pub const DEFAULT_DATA_PATH: &str = "/data/vendor/rianixia_thermal_data";
pub const LEARNING_DATA_FILENAME: &str = "learning.dat";

// ============================================================================
// CONTEXT AWARENESS & AI CONSTANTS
// ============================================================================

pub const VAR_HIGH_THRESHOLD: f32 = 0.15;
pub const VAR_LOW_THRESHOLD: f32 = 0.05;

pub const TARGET_TEMP_IDLE: i32 = 360;
pub const TARGET_TEMP_SUSTAINED: i32 = 400;
pub const TARGET_TEMP_BOOST: i32 = 440;
pub const TARGET_TEMP_CRITICAL: i32 = 480;

pub const PID_KP: f32 = 0.008;
pub const PID_KI: f32 = 0.0005;
pub const PID_KD: f32 = 0.02;
pub const PID_INTEGRAL_LIMIT: f32 = 100.0;

// ============================================================================
// SYSTEM CONSTANTS
// ============================================================================

pub const HISTORY_WINDOW: usize = 200;
pub const CPU_LOAD_WINDOW: usize = 10;
pub const BATTERY_TEMP_PATH: &str = "/sys/class/power_supply/battery/temp";
pub const SAVE_INTERVAL_EVENTS: usize = 50;
pub const SAVE_INTERVAL_SECS: u64 = 600;
pub const SMOOTHING_ALPHA: f32 = 0.3;

pub const SYS_PROP_PATH: &str = "persist.sys.rianixia.thermal_path";
pub const PROP_LEARNING_ENABLED: &str = "persist.sys.rianixia.learning_enabled";
pub const PROP_THRESHOLD_FLOOR: &str = "persist.sys.rianixia.threshold_floor";
pub const PROP_AUDIT_LOGS_ENABLED: &str = "persist.sys.rianixia.thermal-auditlogs";
pub const PROP_GRADIENT_ALERT_THRESHOLD: &str = "persist.sys.rianixia.thermalcore.gradient_alert";

pub const COMFORT_MAX: i32 = 380;
pub const ADAPTIVE_MAX: i32 = 420;
pub const CRITICAL_THRESHOLD: i32 = 460;
pub const SAFE_TEMP: i32 = 350;
pub const ABSOLUTE_THRESHOLD_FLOOR: i32 = 360;
pub const DAILY_DOWNWARD_ADJUSTMENT_LIMIT: i32 = 30;
pub const SUSTAINED_TIME_SECS: u64 = 180;
pub const RECOVERY_STABLE_HOURS: u64 = 4;
pub const TEMPORAL_WINDOW_SECS: u64 = 600;
pub const DECAY_HALFLIFE_HOURS: f32 = 24.0;
pub const THRESHOLD_ADJ_GUARD_SECS: u64 = 900;
pub const STATE_DOWNGRADE_GUARD_SECS: u64 = 120;
pub const LOG_RATE_LIMIT_NORMAL_SECS: u64 = 15;
pub const PREDICTION_HORIZON_SECS: u64 = 45;
pub const PREDICTION_MIN_SAMPLES: usize = 5;
pub const ANOMALY_TEMP_JUMP_THRESHOLD: i32 = 100;
pub const GRADIENT_EMA_ALPHA: f32 = 0.2;
pub const GRADIENT_HIGH_SUSTAINED_SECS: u64 = 30;
pub const DEFAULT_GRADIENT_ALERT_THRESHOLD: i32 = 250;
pub const POLICY_MITIGATION_RATE_LIMIT_SECS: u64 = 2;
pub const POLICY_SENSITIVITY_CPU_THRESHOLD: f32 = 0.6;
pub const VARIANCE_LOW_THRESHOLD: f32 = 25.0;
pub const VARIANCE_HIGH_THRESHOLD: f32 = 100.0;
pub const LEARNING_RATE_BASE: f32 = 0.1;
pub const LEARNING_RATE_FAST: f32 = 0.15;
pub const LEARNING_RATE_SLOW: f32 = 0.05;
