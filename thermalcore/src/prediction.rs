use std::collections::VecDeque;
use super::android_ffi::Logger;
use super::constants::*;

// ============================================================================
// PREDICTIVE THERMAL MODEL
// ============================================================================
//
// Learns a mapping from observed features to the *temperature change* (delta)
// over one sample. Using delta instead of absolute temperature keeps the model
// from memorizing an absolute offset and lets it model "where the temperature
// is heading". Predictions are used to bias the controller forward so the
// daemon reacts before the temperature actually climbs.

#[derive(Clone, Debug)]
pub struct PredictiveSample {
    pub last_temp: f32,      // temperature at previous sample (deg C * 10)
    pub load: f32,           // cpu load 0..1
    pub load_variance: f32,  // recent load variance
    pub is_charging: f32,    // 0 or 1
    pub current_now: f32,    // battery current in A
    pub avg_gradient: f32,   // internal-batt gradient (deg C * 10)
    pub dt: f32,             // seconds since last sample
    pub temp_delta: f32,     // measured change in temp over this sample
}

pub struct PredictiveModel {
    pub history: VecDeque<PredictiveSample>,

    // weights over [temp, load, load_variance, charging, current, gradient]
    weights: [f32; 6],
    bias: f32,
    learning_rate: f32,

    last_temp: Option<f32>,
    last_time: Option<u64>,
}

impl Default for PredictiveModel {
    fn default() -> Self {
        Self::new()
    }
}

impl PredictiveModel {
    pub fn new() -> Self {
        PredictiveModel {
            history: VecDeque::with_capacity(PREDICT_HISTORY),
            // Initial weights: temperature itself dominates short-term change
            // (feedback - warmer tends to relax), load drives it up, etc.
            weights: [
                0.001,   // temp (deg C*10 -> 1C)
                6.0,     // cpu load
                0.1,     // load variance
                0.8,     // is_charging
                0.1,     // battery current
                0.01,    // avg gradient
            ],
            bias: 0.0,
            learning_rate: PREDICT_LEARNING_RATE,
            last_temp: None,
            last_time: None,
        }
    }

    pub fn len(&self) -> usize {
        self.history.len()
    }

    pub fn is_empty(&self) -> bool {
        self.history.is_empty()
    }

    /// Called every loop. Feeds a new observation (current features) and the
    /// current temperature, computing the delta against the previous sample.
    pub fn observe(
        &mut self,
        now: u64,
        temp: f32,
        load: f32,
        load_variance: f32,
        is_charging: f32,
        current_now: f32,
        avg_gradient: f32,
    ) {
        let dt = match self.last_time {
            Some(t) if now > t => (now - t) as f32,
            _ => 1.0,
        };

        let temp_delta = match self.last_temp {
            Some(prev) => temp - prev,
            None => {
                // first sample; seed history and prime the delta
                self.last_temp = Some(temp);
                self.last_time = Some(now);
                return;
            }
        };

        self.history.push_back(PredictiveSample {
            last_temp: self.last_temp.unwrap_or(temp),
            load,
            load_variance,
            is_charging,
            current_now,
            avg_gradient,
            dt: dt.max(0.1),
            temp_delta,
        });

        if self.history.len() > PREDICT_HISTORY {
            self.history.pop_front();
        }

        self.last_temp = Some(temp);
        self.last_time = Some(now);

        // Train incrementally on the sample we just completed.
        self.train_on_last();
    }

    pub fn last_delta(&self) -> Option<f32> {
        self.history.back().map(|s| s.temp_delta)
    }

    fn features(&self, s: &PredictiveSample) -> [f32; 6] {
        [
            s.last_temp,
            s.load,
            s.load_variance,
            s.is_charging,
            s.current_now,
            s.avg_gradient,
        ]
    }

    fn predict_delta_for(&self, s: &PredictiveSample) -> f32 {
        let f = self.features(s);
        let mut pred = self.bias;
        for i in 0..f.len() {
            pred += f[i] * self.weights[i];
        }
        // Normalize by sample duration so prediction is per-second.
        pred / s.dt
    }

    fn train_on_last(&mut self) {
        if self.history.len() < 2 {
            return;
        }
        let prev_idx = self.history.len() - 2;
        let actual_idx = self.history.len() - 1;

        let prev = self.history[prev_idx].clone();
        let actual = self.history[actual_idx].clone();

        let pred_delta = self.predict_delta_for(&prev);
        // Actual observed per-second delta.
        let actual_delta = actual.temp_delta / actual.dt;

        let error = pred_delta - actual_delta;

        let f = self.features(&prev);
        for i in 0..self.weights.len() {
            // weight -= lr * error * feature / dt scale
            self.weights[i] -= self.learning_rate * error * f[i] * (1.0 / prev.dt);
        }
        self.bias -= self.learning_rate * error * (1.0 / prev.dt);
    }

    /// Predict temperature `horizon_secs` into the future using the latest
    /// learned trend. Returns None if not enough data yet.
    pub fn predict_future_temp(&self, horizon_secs: u64) -> Option<i32> {
        if self.history.len() < PREDICT_MIN_SAMPLES {
            return None;
        }
        let last = self.history.back()?;
        let current = last.last_temp + last.temp_delta;
        // A more stable rate estimate: average delta over the recent window,
        // weighted toward the newest. This avoids trusting a single noisy step.
        let mut weighted_delta = 0.0;
        let mut total_w = 0.0;
        for (i, s) in self.history.iter().rev().take(PREDICT_AVG_WINDOW).enumerate() {
            let w = (i as f32 + 1.0) / (PREDICT_AVG_WINDOW as f32);
            weighted_delta += self.predict_delta_for(s) * w;
            total_w += w;
        }
        let rate = if total_w > 0.0 { weighted_delta / total_w } else { 0.0 };

        // Dampen long-horizon extrapolation so we don't over-react.
        let damp = 1.0 / (1.0 + horizon_secs as f32 * PREDICT_DAMP);
        let future = current + rate * (horizon_secs as f32) * damp;

        Some(future as i32)
    }

    /// Produce a proactive throttle bias (0..1) from the predicted future
    /// temperature. Bias grows the closer the forecast is to the critical
    /// threshold, and is zero below the sustained comfort target so idle
    /// prediction never causes throttling.
    pub fn predictive_bias(&self, horizon_secs: u64) -> f32 {
        let Some(future) = self.predict_future_temp(horizon_secs).map(|t| t as f32) else {
            return 0.0;
        };

        // Only act if the forecast exceeds where we'd already start caring.
        let floor = TARGET_TEMP_SUSTAINED as f32; // 400
        if future <= floor {
            return 0.0;
        }

        // Ramp to 1.0 approaching critical.
        let span = (TARGET_TEMP_CRITICAL - TARGET_TEMP_SUSTAINED) as f32; // 80
        let raw = (future - floor) / span;
        raw.clamp(0.0, 1.0)
    }

    pub fn log_stats(&self, logger: &Logger) {
        logger.debug(&format!(
            "PredictiveModel n={} w=[{:.3},{:.2},{:.2},{:.2},{:.2},{:.2}] b={:.2}",
            self.history.len(),
            self.weights[0],
            self.weights[1],
            self.weights[2],
            self.weights[3],
            self.weights[4],
            self.weights[5],
            self.bias
        ));
    }
}
