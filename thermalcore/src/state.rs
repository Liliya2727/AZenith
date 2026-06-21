use serde::{ Deserialize, Serialize };

// ============================================================================
// THERMAL STATE DEFINITIONS
// ============================================================================

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub enum ThermalState {
    Normal,
    Rising,
    Controlled,
    Critical,
    Recovery,
}

#[derive(Debug, Clone, Copy, PartialEq, PartialOrd)]
pub enum MitigationTier {
    None,
    Gentle,
    Moderate,
    Emergency,
}
