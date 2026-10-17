//! Profiler: one init phase plus three active profiles.
//!
//! | Function | Binary argument | File |
//! |---|---|---|
//! | [`initialize`] | `0` / `initialize` | [`initialize`] |
//! | [`performance_profile`] | `1` / `performance_profile` | [`performance`] |
//! | [`balanced_profile`] | `2` / `balanced_profile` | [`balanced`] |
//! | [`eco_mode`] | `3` / `eco_mode` | [`eco`] |
//!
//! **Taxonomy**: all three active profiles use the exact same step order
//! (see the [`common`] module docs); only `initialize` is a different phase.

mod balanced;
mod common;
mod eco;
mod initialize;
mod performance;

pub use balanced::balanced_profile;
pub use eco::eco_mode;
pub use initialize::initialize;
pub use performance::performance_profile;
