use std::fs;
use super::android_ffi::Logger;

// ============================================================================
// EXTERNAL CONTEXT AWARENESS
// ============================================================================

const CURRENT_NOW_PATH: &str = "/sys/class/power_supply/battery/current_now";
const CHARGER_TYPE_PATH: &str = "/sys/class/power_supply/charger/type";
const CHARGER_ONLINE_PATH: &str = "/sys/class/power_supply/charger/online";

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ChargerState {
    None,
    USB,
    AC,
    Wireless,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ScreenState {
    Off,
    On,
}

#[derive(Debug, Default, Clone, Copy)]
pub struct PowerFeatures {
    pub is_charging: f32,
    pub current_now_abs: f32,
}

#[derive(Clone)]
pub struct ExternalContext {
    pub charger_state: ChargerState,
    pub screen_state: ScreenState,
    pub power_features: PowerFeatures,
}

impl ExternalContext {
    pub fn new() -> Self {
        ExternalContext {
            charger_state: ChargerState::None,
            screen_state: ScreenState::Off,
            power_features: PowerFeatures::default(),
        }
    }

    pub fn update(&mut self, logger: &Logger) {
        self.charger_state = Self::detect_charger();
        self.screen_state = Self::detect_screen();
        self.power_features = self.get_power_features(logger);
    }

    pub fn get_power_features(&self, logger: &Logger) -> PowerFeatures {
        match fs::read_to_string(CURRENT_NOW_PATH) {
            Ok(current_str) => {
                match current_str.trim().parse::<i64>() {
                    Ok(current_ua) => {
                        let is_charging = current_ua > 0;
                        let current_abs_ua = current_ua.abs() as f32;

                        PowerFeatures {
                            is_charging: if is_charging {
                                1.0
                            } else {
                                0.0
                            },

                            current_now_abs: current_abs_ua / 1_000_000.0,
                        }
                    }
                    Err(e) => {
                        logger.debug(&format!("Failed to parse {}: {}", CURRENT_NOW_PATH, e));
                        PowerFeatures::default()
                    }
                }
            }
            Err(_) => {
                logger.debug(
                    &format!("{} not found, falling back to charger/online", CURRENT_NOW_PATH)
                );
                self.fallback_charging_check()
            }
        }
    }

    fn fallback_charging_check(&self) -> PowerFeatures {
        if let Ok(status) = fs::read_to_string(CHARGER_ONLINE_PATH) {
            if status.trim() == "1" {
                return PowerFeatures { is_charging: 1.0, current_now_abs: 0.0 };
            }
        }
        PowerFeatures::default()
    }

    pub fn detect_charger() -> ChargerState {
        if let Ok(status) = fs::read_to_string(CHARGER_TYPE_PATH) {
            let status = status.trim().to_lowercase();
            if status.contains("usb_dcp") || status.contains("usb_pd") {
                return ChargerState::AC;
            }
            if status.contains("usb") {
                return ChargerState::USB;
            }
        }

        if let Ok(status) = fs::read_to_string("/sys/class/power_supply/usb/type") {
            let status = status.trim().to_lowercase();
            if status.contains("usb_dcp") || status.contains("usb_pd") {
                return ChargerState::AC;
            }
            if status.contains("usb") {
                return ChargerState::USB;
            }
        }

        if let Ok(status) = fs::read_to_string("/sys/class/power_supply/wireless/online") {
            if status.trim() == "1" {
                return ChargerState::Wireless;
            }
        }

        if let Ok(status) = fs::read_to_string("/sys/class/power_supply/ac/online") {
            if status.trim() == "1" {
                return ChargerState::AC;
            }
        }

        ChargerState::None
    }

    pub fn detect_screen() -> ScreenState {
        if let Ok(state) = fs::read_to_string("/sys/class/leds/lcd-backlight/brightness") {
            if let Ok(brightness) = state.trim().parse::<i32>() {
                return if brightness > 0 { ScreenState::On } else { ScreenState::Off };
            }
        }

        if
            let Ok(state) = fs::read_to_string(
                "/sys/class/backlight/panel0-backlight/actual_brightness"
            )
        {
            if let Ok(brightness) = state.trim().parse::<i32>() {
                return if brightness > 0 { ScreenState::On } else { ScreenState::Off };
            }
        }

        if let Ok(state) = fs::read_to_string("/sys/class/graphics/fb0/show_blank_event") {
            return if state.trim() == "1" { ScreenState::Off } else { ScreenState::On };
        }

        ScreenState::On
    }

    pub fn is_charging(&self) -> bool {
        self.power_features.is_charging > 0.5
    }

    pub fn get_context_bias(&self) -> f32 {
        let mut bias = 1.0;

        if self.is_charging() {
            bias *= 0.95;
        }

        if self.screen_state == ScreenState::On {
            bias *= 0.97;
        }

        if self.charger_state == ChargerState::Wireless {
            bias *= 0.92;
        }

        bias
    }
}
