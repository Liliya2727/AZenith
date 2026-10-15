use std::io;
use std::fs;
use std::path::Path;
use super::android_ffi::Logger;
use super::constants::*;

// ============================================================================
// MTK DVFSRC ACTUATOR
// ============================================================================
//
// Talks to /sys/kernel/helio-dvfsrc, the coordinated SoC DVFS controller that
// MTK's own thermal HAL / dvfsrc-helper drive. Requesting a VCORE OPP scales
// CPU + GPU + cache + DDR level together, working WITH the vendor stack rather
// than fighting individual cooling actors.
//
// Semantics: writing `dvfsrc_req_vcore_opp` with OPP index N asks the
// controller to not exceed that performance level. Higher index == lower
// power. Index 0 == full performance (release). The table read from the kernel
// lists 24 OPPs (0 = fastest ... 23 = slowest).

pub struct DvfsrcActuator {
    pub available: bool,
    opp_count: u32,
    last_opp: Option<u32>,
}

impl DvfsrcActuator {
    pub fn new(logger: &Logger) -> Self {
        let base = Path::new(DVFSRC_SYSFS_PATH);
        let req = base.join("dvfsrc_req_vcore_opp");
        if !base.exists() || !req.exists() {
            logger.info("DVFSRC actuator unavailable: helio-dvfsrc node not found");
            return DvfsrcActuator { available: false, opp_count: 0, last_opp: None };
        }

        let opp_count = fs::read_to_string(base.join("dvfsrc_num_opps"))
            .ok()
            .and_then(|s| s.trim().parse::<u32>().ok())
            .unwrap_or(0);

        if opp_count == 0 {
            logger.info("DVFSRC actuator unavailable: no OPP table");
            return DvfsrcActuator { available: false, opp_count: 0, last_opp: None };
        }

        logger.info(&format!("DVFSRC actuator ready: {} OPP levels", opp_count));
        DvfsrcActuator { available: true, opp_count, last_opp: None }
    }

    /// Map intensity (0..1) to a DVFSRC OPP index.
    /// intensity 0 -> release (OPP 0, full performance).
    /// intensity 1 -> lowest (OPP max).
    fn opp_for_intensity(&self, intensity: f32) -> u32 {
        if self.opp_count == 0 {
            return 0;
        }
        let i = intensity.clamp(0.0, 1.0);
        // Round to nearest OPP; a small deadband so faint requests stay at 0.
        if i < DVFSRC_FLOOR {
            return 0;
        }
        let idx = (i * (self.opp_count - 1) as f32).round() as u32;
        idx.min(self.opp_count - 1)
    }

    pub fn apply(&mut self, intensity: f32, logger: &Logger) -> io::Result<()> {
        if !self.available {
            return Ok(());
        }
        let opp = self.opp_for_intensity(intensity);

        // Only write when the requested OPP changes, to avoid hammering the
        // sysfs node every control tick.
        if self.last_opp == Some(opp) {
            return Ok(());
        }

        let req = Path::new(DVFSRC_SYSFS_PATH).join("dvfsrc_req_vcore_opp");
        fs::write(&req, opp.to_string())?;
        self.last_opp = Some(opp);
        logger.debug(&format!("DVFSRC request OPP {} (intensity {:.2})", opp, intensity));
        Ok(())
    }
}


