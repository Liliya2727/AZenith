use std::collections::VecDeque;
use super::constants::PREDICTION_MIN_SAMPLES;
use super::learning::ThermalEvent; // Use the rich ThermalEvent for history

// ============================================================================
// PREDICTIVE MODEL
// ============================================================================

pub struct PredictiveModel {
    pub history: VecDeque<ThermalEvent>,
    weights: [f32; 8],
    bias: f32,
    learning_rate: f32,
}

impl PredictiveModel {
    pub fn new() -> Self {
        PredictiveModel {
            history: VecDeque::with_capacity(20),
            weights: [
                0.95, // temp
                0.5, // temp_delta
                1.5, // cpu_load
                1.0, // core_load_max
                0.8, // is_charging
                0.5, // current_now_abs
                0.01, // hour_of_day
                0.1, // avg_gradient
            ],
            bias: 1.0,
            learning_rate: 0.01,
        }
    }

    pub fn add_sample(&mut self, event: &ThermalEvent) {
        if self.history.len() >= 20 {
            self.history.pop_front();
        }
        self.history.push_back(event.clone());

        self.update_model(event);
    }

    fn get_features(&self, event: &ThermalEvent) -> [f32; 8] {
        [
            event.temperature as f32,
            event.temp_delta as f32,
            event.cpu_load,
            event.core_load_max,
            event.is_charging,
            event.current_now_abs,
            event.hour_of_day,
            event.avg_gradient,
        ]
    }

    fn predict(&self, features: &[f32; 8]) -> f32 {
        let mut prediction = self.bias;
        for i in 0..features.len() {
            prediction += features[i] * self.weights[i];
        }
        prediction
    }

    pub fn predict_future_temp(&self, horizon_secs: u64) -> Option<i32> {
        if self.history.len() < PREDICTION_MIN_SAMPLES {
            return None;
        }

        let last_event = self.history.back().unwrap();
        let features = self.get_features(last_event);

        let next_temp_pred = self.predict(&features);

        let predicted_delta = next_temp_pred - (last_event.temperature as f32);

        let future_temp = next_temp_pred + predicted_delta * (horizon_secs as f32);

        Some(future_temp as i32)
    }

    fn update_model(&mut self, event: &ThermalEvent) {
        if self.history.len() < 2 {
            return;
        }

        let prev_event = &self.history[self.history.len() - 2];
        let features = self.get_features(prev_event);

        let prediction = self.predict(&features);
        let actual = event.temperature as f32;
        let error = prediction - actual;

        for i in 0..self.weights.len() {
            let gradient = error * features[i];
            self.weights[i] -= self.learning_rate * gradient;
        }

        self.bias -= self.learning_rate * error;
    }
}
