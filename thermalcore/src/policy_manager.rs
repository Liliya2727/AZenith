use super::android_ffi::Logger;
use super::constants::*;

// ============================================================================
// PID CONTROLLER
// ============================================================================

pub struct PidController {
    pub kp: f32,
    pub ki: f32,
    pub kd: f32,

    integral: f32,
    prev_error: f32,
}

impl PidController {
    pub fn new(kp: f32, ki: f32, kd: f32) -> Self {
        PidController {
            kp,
            ki,
            kd,
            integral: 0.0,
            prev_error: 0.0,
        }
    }

    pub fn reset(&mut self) {
        self.integral = 0.0;
        self.prev_error = 0.0;
    }

    pub fn compute(
        &mut self,
        current_temp: i32,
        target_temp: i32,
        dt_seconds: f32,
        _logger: &Logger
    ) -> f32 {
        if dt_seconds <= 0.0 {
            return 0.0;
        }

        let error = (current_temp - target_temp) as f32;

        let p_term = self.kp * error;

        self.integral += error * dt_seconds;
        self.integral = self.integral.clamp(-PID_INTEGRAL_LIMIT, PID_INTEGRAL_LIMIT);
        let i_term = self.ki * self.integral;

        let derivative = (error - self.prev_error) / dt_seconds;
        let d_term = self.kd * derivative;

        self.prev_error = error;

        let output = p_term + i_term + d_term;
        let clamped = output.clamp(0.0, 1.0);

        clamped
    }
}

pub struct PolicyManager {
    pub pid: PidController,
}

impl PolicyManager {
    pub fn new() -> Self {
        PolicyManager {
            pid: PidController::new(PID_KP, PID_KI, PID_KD),
        }
    }

    pub fn update_params(&mut self, kp: f32, ki: f32, kd: f32) {
        self.pid.kp = kp;
        self.pid.ki = ki;
        self.pid.kd = kd;
    }
}
