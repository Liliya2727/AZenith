# rynx-thermalcore

An Android thermal management daemon written in Rust. I built this because the stock thermal stack on my MTK device (Infinix GT 10 Pro / MT6893) was either too aggressive — throttling at 36 °C — or too passive, letting the SoC hit 50 °C before reacting. I wanted something that sits *alongside* the vendor HAL, not fighting it, and that learns my usage patterns over time.

---

## What this is (and isn't)

**It is:** a userspace daemon that reads battery + SoC temperatures, CPU load, and charger/screen state, runs a PID controller with a small predictive feed-forward term, and writes cooling intensity to either the MTK DVFSRC OPP table (coordinated CPU+GPU+DDR scaling) or a curated set of "soft-safe" cooling devices (`thermal-devfreq`, `cpu_adaptive`, `cpufreq`, BCL). It persists its PID gains and event history to disk so it gets slightly better the longer it runs.

**It isn't:** a replacement for the kernel thermal framework, a governor, or a magic "cooling" app. It doesn't undervolt, it doesn't touch GPU clocks directly, and it won't make a hot phone cold — it just makes the throttle decisions *earlier* and *smoother* than the stock curve on my device.

---

## Why I wrote it

On MT6893 (Dimensity 8050), the stock `thermalloadalgod` + `mtktscpu` zone runs ~11 °C hotter than the battery thermistor. The vendor HAL reacts to that internal zone, but by the time it decides to throttle, the battery has already soaked heat. My daemon fuses both readings (50/50 blend), so the control temperature *leads* the battery temp without chasing every spike. The result: sustained loads (gaming, compiles) hold a higher stable frequency for longer, and idle drops to zero throttle within seconds — no more "stuck throttling at 30 °C" bug.

---

## How it works

### The control loop (~1 Hz)

```
battery/temp (deci-°C)  ──► EWMA (α=0.3)
         │
         ▼
max CPU-ish zone temp (milli-°C) ──► /100 ──► blend 50/50 ──► control_temp
         │                                                         │
         ▼                                                         ▼
    ThermalFusion                                            PID + predictor
         │                                                         │
         ▼                                                         ▼
   gradient alert                                              intensity (0..1)
                                                                  │
                    ┌─────────────────────────────────────────────┘
                    ▼
         ┌───────────────────────┐
         │ ActuatorMode::Dvfsrc  │──► write dvfsrc_req_vcore_opp (only on change)
         │ ActuatorMode::Soft    │──► write cur_state on soft-safe cooling devices
         └───────────────────────┘
```

### The three layers

| Layer | Files | What it does |
|-------|-------|--------------|
| **Sense** | `monitor.rs`, `thermal_zones.rs`, `cpu.rs`, `context.rs` | Reads temps, filters, fuses, computes CPU load variance, detects screen/charger |
| **Decide** | `learning.rs`, `policy_manager.rs`, `prediction.rs` | Picks target temp (Idle/Sustained/Boost/Critical), runs PID, adds predictive bias |
| **Act** | `dvfsrc.rs`, `cooling.rs` | Maps intensity → OPP index or `cur_state`, smooths writes, skips no-ops |

### Target temperature selection ("AI mode")

`ThermalAI::determine_target()` is a tiny state machine, not a neural net:

| Mode | Trigger | Target |
|------|---------|--------|
| `Critical` | `control_temp ≥ 48.0 °C` | 36.0 °C (force cooldown) |
| `Idle` | screen off | 36.0 °C |
| `Boost` | screen on, `load_variance > 0.15` | 44.0 °C |
| `Sustained` | screen on, otherwise | 40.0 °C |

Lower target = more aggressive cooling. The variance threshold distinguishes bursty UI interaction (Boost) from steady gaming/video (Sustained).

### PID controller

```
error = control_temp - target_temp
if current ≤ target:          return 0.0  (full release)
if |error| < 3.0 °C:          return 0.0  (deadband)
P = kp * error
I = ki * clamp(∫error dt, ±100)
D = kd * d(error)/dt
output = clamp(P + I + D, 0, 1)
```

Defaults: `kp=0.008, ki=0.0005, kd=0.02`, clamped on load to sane bounds so a corrupted `learning.dat` can't lock the daemon into permanent over/under-throttle.

### Predictive feed-forward

A 6-feature online linear regression (SGD, lr=0.01) predicts *temperature change per second* from:
`[last_temp, cpu_load, load_variance, is_charging, battery_current, internal_battery_gradient]`

It trains on the last 60 samples, extrapolates 45 s ahead with damping, and produces a bias 0→1 as the forecast approaches 48 °C. The bias is mixed in at 50% weight:

```
final_intensity = min(1.0, pid_output + 0.5 * predict_bias)
```

This is **proactive** — it throttles *before* the temperature arrives — but bounded so it can never *release* throttle on its own.

---

## The "machine learning" model (and why it's not overfitting)

I use the term "AI" loosely in the logs (`AI Mode Change`). What's actually learned:

1. **PID gains** (`kp`, `ki`, `kd`) — persisted, clamped on load, *never* adapted online. They're just the last known-good values.
2. **Event history** — ring buffer of 200 `ThermalEvent` records (timestamp, temp, variance, target, pid_output). Used for post-hoc analysis, not live training.
3. **Predictor weights** — 6 floats updated via SGD every sample. This *is* online learning, but:

**Why it doesn't overfit:**

- **Tiny model** — 6 weights + bias. Not enough capacity to memorize noise.
- **Predicts deltas, not absolutes** — learning "temperature change per second" is far more stable than learning absolute temperature (no drift, no offset memorization).
- **Fixed learning rate** (0.01) + **weight decay via damping** — no adaptive optimizers that can explode.
- **Recency-weighted extrapolation** — the 45 s forecast averages the last 8 predictions with linearly increasing weight toward the newest, so one bad sample doesn't swing the forecast.
- **Bias only activates above 40 °C** — below Sustained target, `predict_bias = 0`. The predictor literally cannot cause idle throttling.
- **Clamped output** — final intensity hard-clamped to `[0, 1]`.

The predictor is a *guard rail*, not the steering wheel. The PID does the heavy lifting; the predictor just taps the brakes a fraction earlier when it sees a consistent climb.

---

## System properties (all under `persist.sys.rynx.*`)

| Property | Default | Purpose |
|----------|---------|---------|
| `thermalcore-bigdata.path` | `/data/vendor/rynx_thermal_data` | Where `learning.dat` lives (fallback: `/data/local/tmp/rynx_thermal_data`) |
| `thermal_path` | `/sys/class/power_supply/battery/temp` | Battery temperature node (inotify source) |
| `actuator` | *(auto)* | `dvfsrc` \| `soft` \| empty = auto-detect |
| `thermalcore.gradient_alert` | `250` (25.0 °C) | SoC-vs-battery gradient runaway alert threshold |
| `thermal-auditlogs` | `false` | Enable audit log lines |
| `thermalcore-debug` | `false` | Enable `debug`-level logcat output |

**Clean break** — the old `persist.sys.rianixia.*` props are no longer read. On first run, if the new data dir is empty but a legacy `learning.dat` exists in the old paths, it's copied over once (postcard can't read bincode, so gains reset but the file is preserved).

---

## Building

```bash
# Android (arm64-v8a, API 26) — requires Android NDK r27c at $ANDROID_NDK_ROOT or /home/ryanistr/android-ndk-r27c
compile-rust -a            # builds, strips, UPX-compresses → ./rynx-thermalcore
compile-rust -a -npx       # skip UPX

# Host (for linting only; linking -llog fails without stubs)
cargo build --release --target $(rustc -vV | grep host | awk '{print $2}')

# Collector binary (1 Hz JSONL dataset logger)
cargo build --release --features collector --bin thermal-collector
```

The `compile-rust` wrapper reads the package name from `Cargo.toml` so the output binary is always `rynx-thermalcore`.

---

## Deploying on device

```bash
adb push rynx-thermalcore /data/local/tmp/
adb shell chmod 755 /data/local/tmp/rynx-thermalcore

# Needs root for sysfs writes
adb shell su -c '/data/local/tmp/rynx-thermalcore'
```

Watch logs:
```bash
adb logcat -s RynxThermalCore
```

Typical log line (every ≥15 s):
```
Batt:31°C SoC:42°C Ctrl:38°C | Tgt:40°C | Sustained | Var:0.012 | PID:0.23 Bias:0.05 Out:0.26
```

---

## Contributing

I don't have a formal contribution process — this is a personal project that happens to be open. But if you want to hack on it:

1. **Test on real hardware** — the control loop is timing-sensitive and hardware-specific. Simulator mode (`--features simulator`) helps with logic, but nothing replaces a warm phone.
2. **Keep the dependency tree lean** — `inotify` with `default-features=false` drops tokio/futures/mio; `postcard` with `use-std` drops heapless. Every transitive dep costs cold-start time.
3. **Preserve the release profile** — LTO + single codegen unit + strip is non-negotiable for binary size and startup latency.
4. **Don't add online gain adaptation** — the current "learning" is just persistence. If you want adaptive gains, that's a separate design (meta-PID, Bayesian, etc.) and needs far more validation.
5. **Run `cargo check` with both feature sets** — `cargo check` and `cargo check --features simulator,collector` must both pass.

Open an issue or PR if you find a bug or have a focused improvement. I'll review.

---

## License

Licensed under the **Apache License, Version 2.0** — see [LICENSE](LICENSE) for the full text.

```
Copyright (c) 2024 rianixia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## Acknowledgments

- MTK's `helio-dvfsrc` documentation (reverse-engineered from kernel source) for the OPP semantics
- The `postcard` crate for a serde-compatible, no-std binary format that survives crate upgrades
- `skydoves/compose-stability-analyzer` — not used here, but the stability/inference tooling taught me to clamp persisted gains on load
- Every thermal engineer who's ever cursed at a stuck throttle state — this one's for you