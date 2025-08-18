/*
 * Copyright (C) 2024-2025 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import BannerZenith from "/webui.banner.avif";
import AvatarZenith from "/webui.avatar.avif";
import SchemeBanner from "/webui.schemebanner.avif";
import { exec } from "kernelsu";

const executeCommand = async (cmd, cwd = null) => {
  try {
    const { errno, stdout, stderr } = await exec(cmd, cwd ? { cwd } : {});
    return { errno, stdout, stderr };
  } catch (e) {
    return { errno: -1, stdout: "", stderr: e.message || String(e) };
  }
};

function EventEmitter() {
  this.listeners = {};
}
const randomMessages = [
  "The sky is really pretty today... did you notice?",
  "Sparkles make everything better ✨",
  "You’re doing your best, and that’s enough~",
  "It’s okay to rest. Even stars need time to shine.",
  "A warm drink, a deep breath… everything will be alright.",
  "Soft clouds, quiet hearts. Let’s take it slow today~",
  "You don’t need a reason to smile — just smile~",
  "Even little steps can lead to big dreams~",
  "The wind feels gentle today… like a hug from the world.",
  "You’re like a small flower growing through the cracks — beautiful and brave.",
  "I believe in you~ even if the world feels heavy sometimes.",
  "Let’s chase the light, even if the path is slow.",
  "The stars are always watching… and they’re proud of you.",
  "You sparkle more than you think ✨",
  "Sometimes doing nothing is the bravest thing of all.",
  "Let the sun kiss your cheeks and your worries fade away.",
  "Moonlight doesn’t rush, and neither should you~",
  "Hold my hand, even just in your thoughts~",
  "Gentle mornings start with a smile — even a sleepy one.",
  "Float like a cloud, soft and free~",
  "You’re a soft melody in a world that rushes — take your time.",
  "Cup of tea, cozy socks, and a heart that’s healing~",
  "Rainy days are for dreaming softly under blankets~",
  "Flowers bloom quietly — you will too.",
  "The sky doesn’t ask for permission to be beautiful, and neither should you.",
  "You are made of soft light and quiet courage~",
  "Stargazing isn’t procrastinating — it’s soul-healing.",
  "It's okay to have quiet days. The moon does too.",
  "Today’s vibe: calm skies, warm tea, soft heart.",
  "You don’t have to glow loud — some stars shine in silence ✨",
];

function showRandomMessage() {
  let c = document.getElementById("msg"),
    s = randomMessages[Math.floor(Math.random() * randomMessages.length)];
  c.textContent = s;
}

function Process() {
  (this.listeners = {}),
    (this.stdin = new EventEmitter()),
    (this.stdout = new EventEmitter()),
    (this.stderr = new EventEmitter());
}

async function showToast(c) {
  let { stdout: val } = await executeCommand(
    "getprop persist.sys.azenithconf.showtoast"
  );
  if (val.trim() === "1") {
    ksu.toast(c);
  }
}

(window.onload = () => {
  requestIdleCallback
    ? requestIdleCallback(heavyInit)
    : setTimeout(heavyInit, 100);
}),
  (EventEmitter.prototype.on = function (c, s) {
    this.listeners[c] || (this.listeners[c] = []), this.listeners[c].push(s);
  }),
  (EventEmitter.prototype.emit = function (c, ...s) {
    this.listeners[c] && this.listeners[c].forEach((c) => c(...s));
  }),
  (Process.prototype.on = function (c, s) {
    this.listeners[c] || (this.listeners[c] = []), this.listeners[c].push(s);
  }),
  (Process.prototype.emit = function (c, ...s) {
    this.listeners[c] && this.listeners[c].forEach((c) => c(...s));
  });
var u = {};
async function checkModuleVersion() {
  try {
    let { errno: c, stdout: s } = await executeCommand(
      "echo 'Version :' && grep \"version=\" /data/adb/modules/AZenith/module.prop | awk -F'=' '{print $2}'"
    );
    0 === c && (document.getElementById("moduleVer").textContent = s.trim());
  } catch {}
}
async function checkProfile() {
  try {
    let { errno: c, stdout: s } = await executeCommand(
      "cat /data/adb/.config/AZenith/API/current_profile"
    );
    if (0 === c) {
      let r = s.trim(),
        d = document.getElementById("CurProfile"),
        l =
          {
            0: "Initializing...",
            1: "Performance",
            2: "Balanced",
            3: "ECO Mode",
          }[r] || "Unknown";
      switch (((d.textContent = l), l)) {
        case "Performance":
          d.style.color = "#ef4444";
          break;
        case "ECO Mode":
          d.style.color = "#5eead4";
          break;
        case "Balanced":
          d.style.color = "#7dd3fc";
          break;
        case "Initializing...":
          d.style.color = "#60a5fa";
          break;
        default:
          d.style.color = "#ffffff";
      }
    }
  } catch (m) {
    console.error("Error checking profile:", m);
  }
}
async function checkAvailableRAM() {
  try {
    let { stdout: c } = await executeCommand(
        "cat /proc/meminfo | grep -E 'MemTotal|MemAvailable'"
      ),
      s = c.trim().split("\n"),
      r = 0,
      d = 0;
    for (let l of s)
      l.includes("MemTotal")
        ? (r = parseInt(l.match(/\d+/)[0]))
        : l.includes("MemAvailable") && (d = parseInt(l.match(/\d+/)[0]));
    if (r && d) {
      let m = r - d,
        h = (m / 1024 / 1024).toFixed(2),
        g = (r / 1024 / 1024).toFixed(2),
        f = (d / 1024 / 1024).toFixed(2),
        y = ((m / r) * 100).toFixed(0);
      document.getElementById(
        "ramInfo"
      ).textContent = `${h} GB / ${g} GB (${y}%) — Available: ${f} GB`;
    } else
      document.getElementById("ramInfo").textContent = "Error reading memory";
  } catch (p) {
    document.getElementById("ramInfo").textContent = "Error";
  }
}
async function checkCPUFrequencies() {
  let c = document.getElementById("cpuFreqInfo"),
    s = "";
  try {
    let { stdout: r } = await executeCommand(
        "ls /sys/devices/system/cpu/cpufreq/ | grep policy"
      ),
      d = r.trim().split("\n").filter(Boolean);
    for (let l of d) {
      let m = `/sys/devices/system/cpu/cpufreq/${l}`,
        [{ stdout: h }, { stdout: g }] = await Promise.all([
          executeCommand(`cat ${m}/scaling_cur_freq`),
          executeCommand(`cat ${m}/related_cpus`),
        ]),
        f = (parseInt(h.trim()) / 1e3).toFixed(0),
        y = g.trim().split(" ").join(", ");
      s += `Cluster ${y}: ${f} MHz<br>`;
    }
    c.innerHTML = s.trim();
  } catch (p) {
    c.innerHTML = "Failed to read CPU frequencies.";
  }
}
let cachedSOCData = null;
async function fetchSOCDatabase() {
  if (!cachedSOCData)
    try {
      cachedSOCData = await (await fetch("webui.soclist.json")).json();
    } catch {
      cachedSOCData = {};
    }
  return cachedSOCData;
}

async function checkCPUInfo() {
  let c = localStorage.getItem("soc_info");
  try {
    let { errno: s, stdout: r } = await executeCommand("getprop ro.soc.model");
    if (0 === s) {
      let d = r.trim().replace(/\s+/g, "").toUpperCase(),
        l = await fetchSOCDatabase(),
        m = l[d];
      if (!m)
        for (let h = d.length; h >= 6; h--) {
          let g = d.substring(0, h);
          if (l[g]) {
            m = l[g];
            break;
          }
        }
      m || (m = d),
        (document.getElementById("cpuInfo").textContent = m),
        c !== m && localStorage.setItem("soc_info", m);
    } else {
      document.getElementById("cpuInfo").textContent = c || "Unknown SoC";
    }
  } catch {
    document.getElementById("cpuInfo").textContent = c || "Error";
  }
  showFPSGEDIfMediatek();
  showMaliSchedIfMediatek();
  showBypassIfMTK();
  showThermalIfMTK();
}

async function checkKernelVersion() {
  let c = localStorage.getItem("kernel_version");
  try {
    let { errno: s, stdout: r } = await executeCommand("uname -r");
    if (0 === s && r.trim()) {
      let d = r.trim();
      (document.getElementById("kernelInfo").textContent = d),
        c !== d && localStorage.setItem("kernel_version", d);
    } else
      c
        ? (document.getElementById("kernelInfo").textContent = c)
        : (document.getElementById("kernelInfo").textContent =
            "Unknown Kernel");
  } catch {
    c
      ? (document.getElementById("kernelInfo").textContent = c)
      : (document.getElementById("kernelInfo").textContent = "Error");
  }
}
async function getAndroidVersion() {
  let c = localStorage.getItem("android_version");
  try {
    let { errno: s, stdout: r } = await executeCommand(
      "getprop ro.build.version.release"
    );
    if (0 === s && r.trim()) {
      let d = r.trim();
      (document.getElementById("android").textContent = d),
        c !== d && localStorage.setItem("android_version", d);
    } else
      c
        ? (document.getElementById("android").textContent = c)
        : (document.getElementById("android").textContent = "Unknown Android");
  } catch {
    c
      ? (document.getElementById("android").textContent = c)
      : (document.getElementById("android").textContent = "Error");
  }
}
async function checkServiceStatus() {
  let { errno: c, stdout: s } = await executeCommand(
      "/system/bin/toybox pidof sys.azenith-service"
    ),
    r = document.getElementById("serviceStatus"),
    d = document.getElementById("servicePID");
  if (0 === c && "0" !== s.trim()) {
    let l = s.trim(),
      { stdout: m } = await executeCommand(
        "cat /data/adb/.config/AZenith/API/current_profile"
      ),
      { stdout: h } = await executeCommand(
        "getprop persist.sys.azenithconf.AIenabled"
      ),
      g = m.trim(),
      f = h.trim();
    "0" === g
      ? (r.textContent = "Initializing...\uD83C\uDF31")
      : ["1", "2", "3"].includes(g)
      ? "1" === f
        ? (r.textContent = "Running\uD83C\uDF43")
        : "0" === f
        ? (r.textContent = "Idle\uD83D\uDCAB")
        : (r.textContent = "Unknown Profile")
      : (r.textContent = "Unknown Profile"),
      (d.textContent = "Service PID: " + l);
  } else
    (r.textContent = "Suspended\uD83D\uDCA4"),
      (d.textContent = "Service PID: null");
}
async function checkfpsged() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.fpsged"
  );
  0 === c && (document.getElementById("fpsged").checked = "1" === s.trim());
}
async function setfpsged(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.fpsged 1"
      : "setprop persist.sys.azenithconf.fpsged 0"
  );
}
function showFPSGEDIfMediatek() {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const fpsgedDiv = document.getElementById("fpsged-container");
  if (fpsgedDiv) {
    fpsgedDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
}
async function checkDND() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.dnd"
  );
  0 === c && (document.getElementById("DoNoDis").checked = "1" === s.trim());
}
async function setDND(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.dnd 1"
      : "setprop persist.sys.azenithconf.dnd 0"
  );
}
async function checkBypassChargeStatus() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.bypasschg"
  );
  0 === c && (document.getElementById("Zepass").checked = "1" === s.trim());
}
async function setBypassChargeStatus(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.bypasschg 1"
      : "setprop persist.sys.azenithconf.bypasschg 0"
  );
}
function showBypassIfMTK() {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const ZepassDiv = document.getElementById("Zepass-container");
  if (ZepassDiv) {
    ZepassDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
}
async function checkOPPIndexStatus() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.cpulimit"
  );
  0 === c && (document.getElementById("OPPIndex").checked = "1" === s.trim());
}
async function setOPPIndexStatus(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.cpulimit 1"
      : "setprop persist.sys.azenithconf.cpulimit 0"
  );
}
async function checkDThermal() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.DThermal"
  );
  0 === c && (document.getElementById("DThermal").checked = "1" === s.trim());
}
async function setDThermal(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.DThermal 1"
      : "setprop persist.sys.azenithconf.DThermal 0"
  );
}
function showThermalIfMTK() {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const thermalDiv = document.getElementById("DThermal-container");
  if (thermalDiv) {
    thermalDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
}
async function checkSFL() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.SFL"
  );
  0 === c && (document.getElementById("SFL").checked = "1" === s.trim());
}
async function setSFL(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.SFL 1"
      : "setprop persist.sys.azenithconf.SFL 0"
  );
}
async function checkschedtunes() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.schedtunes"
  );
  0 === c && (document.getElementById("schedtunes").checked = "1" === s.trim());
}
async function setschedtunes(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.schedtunes 1"
      : "setprop persist.sys.azenithconf.schedtunes 0"
  );
}
async function checkiosched() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.iosched"
  );
  0 === c && (document.getElementById("iosched").checked = "1" === s.trim());
}
async function setiosched(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.iosched 1"
      : "setprop persist.sys.azenithconf.iosched 0"
  );
}
async function applyFSTRIM() {
  await executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf FSTrim"
  ),
    showToast("Trimmed Unused Blocks");
}

async function setGameCpuGovernor(c) {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`echo ${c} > ${s}/custom_game_cpu_gov`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "1" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov ${c}`
    ));
}

async function loadGameGovernors() {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("cpuGovernorGame");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    // Use absolute paths and no cwd for shell logic
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -f /data/adb/.config/AZenith/custom_game_cpu_gov ] && cat /data/adb/.config/AZenith/custom_game_cpu_gov || cat /data/adb/.config/AZenith/game_cpu_gov'`
    );
    0 === l && (d.value = m.trim());
  }
}

async function setDefaultCpuGovernor(c) {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`echo ${c} > ${s}/custom_default_cpu_gov`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "2" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov ${c}`
    ));
}

async function loadCpuGovernors() {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("cpuGovernor");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -f /data/adb/.config/AZenith/custom_default_cpu_gov ] && cat /data/adb/.config/AZenith/custom_default_cpu_gov || cat /data/adb/.config/AZenith/default_cpu_gov'`
    );
    0 === l && (d.value = m.trim());
  }
}

async function setGovernorPowersave(c) {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`echo ${c} > ${s}/custom_powersave_cpu_gov`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "3" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov ${c}`
    ));
}

async function GovernorPowersave() {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("GovernorPowersave");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -f /data/adb/.config/AZenith/custom_powersave_cpu_gov ] && cat /data/adb/.config/AZenith/custom_powersave_cpu_gov || cat /data/adb/.config/AZenith/powersave_cpu_gov'`
    );
    0 === l && (d.value = m.trim());
  }
}
function hideGameListModal() {
  let c = document.getElementById("gamelistModal");
  c.classList.remove("show"),
    document.body.classList.remove("modal-open"),
    c._resizeHandler &&
      (window.removeEventListener("resize", c._resizeHandler),
      delete c._resizeHandler);
}
let originalGamelist = "";

async function showGameListModal() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.AIenabled"
  );
  if (0 === c && "0" === s.trim()) {
    showToast("Can't access in current mode");
    return;
  }

  const r = document.getElementById("gamelistModal");
  const d = document.getElementById("gamelistInput");
  const searchInput = document.getElementById("gamelistSearch"); // Must exist in HTML
  const l = r.querySelector(".gamelist-content");

  const { errno: m, stdout: h } = await executeCommand(
    "cat /data/adb/.config/AZenith/gamelist/gamelist.txt"
  );

  if (m === 0) {
    const formatted = h.trim().replace(/\|/g, "\n");
    originalGamelist = formatted;
    d.value = formatted;
  }

  if (searchInput) {
    searchInput.value = ""; // Reset search
    // Remove existing listener first to prevent duplicates on reopen
    searchInput.removeEventListener("input", filterGameList);
    searchInput.addEventListener("input", filterGameList);
  }

  r.classList.add("show");
  document.body.classList.add("modal-open");
  setTimeout(() => d.focus(), 100);

  const g = window.innerHeight;
  const f = () => {
    window.innerHeight < g - 150
      ? (l.style.transform = "translateY(-10%) scale(1)")
      : (l.style.transform = "translateY(0) scale(1)");
  };

  window.addEventListener("resize", f, { passive: true });
  r._resizeHandler = f;
  f();
}

function filterGameList() {
  const searchTerm = document
    .getElementById("gamelistSearch")
    .value.toLowerCase();
  const gamelistInput = document.getElementById("gamelistInput");

  if (!searchTerm) {
    gamelistInput.value = originalGamelist;
    return;
  }

  const filteredList = originalGamelist
    .split("\n")
    .filter((line) => line.toLowerCase().includes(searchTerm))
    .join("\n");

  gamelistInput.value = filteredList;
}
async function saveGameList() {
  const gamelistInput = document.getElementById("gamelistInput");
  const searchInput = document.getElementById("gamelistSearch");
  const searchTerm = (searchInput?.value || "").toLowerCase();

  const editedLines = gamelistInput.value
    .split("\n")
    .map((x) => x.trim())
    .filter(Boolean);

  const originalLines = originalGamelist
    .split("\n")
    .map((x) => x.trim())
    .filter(Boolean);

  // If no search, save entire input as-is
  if (!searchTerm) {
    const outputString = editedLines.join("|").replace(/"/g, '\\"');
    await executeCommand(
      `echo "${outputString}" > /data/adb/.config/AZenith/gamelist/gamelist.txt`
    );
    showToast(`Saved ${editedLines.length} packages`);
    hideGameListModal();
    return;
  }

  // Search is active → replace only matching lines with edited ones
  let editedIndex = 0;
  const mergedLines = originalLines.map((line) => {
    if (line.toLowerCase().includes(searchTerm)) {
      const replacement = editedLines[editedIndex++]?.trim();
      return replacement || line; // fallback to original if edited line is empty
    }
    return line;
  });

  const outputString = mergedLines.join("|").replace(/"/g, '\\"');
  await executeCommand(
    `echo "${outputString}" > /data/adb/.config/AZenith/gamelist/gamelist.txt`
  );
  showToast(`Saved ${mergedLines.length} packages`);
  hideGameListModal();
}
async function checklogger() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenith.debugmode"
  );
  0 === c && (document.getElementById("logger").checked = "true" === s.trim());
}
async function setlogger(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenith.debugmode true"
      : "setprop persist.sys.azenith.debugmode false"
  );
}
async function setVsyncValue(c) {
  await executeCommand(`echo ${c} > /data/adb/.config/AZenith/customVsync`),
    await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf disablevsync ${c}`
    );
}

async function loadVsyncValue() {
  let { errno: c, stdout: s } = await executeCommand(
    "cat /data/adb/.config/AZenith/availableValue"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("disablevsync");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -f /data/adb/.config/AZenith/customVsync ] && cat /data/adb/.config/AZenith/customVsync'`
    );
    0 === l && (d.value = m.trim());
  }
}
async function setCpuFreqOffsets(c) {
  await executeCommand(`echo ${c} >/data/adb/.config/AZenith/customFreqOffset`);
}

async function loadCpuFreq() {
  let { errno: c, stdout: s } = await executeCommand(
    "cat /data/adb/.config/AZenith/availableFreq"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("cpuFreq");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -f /data/adb/.config/AZenith/customFreqOffset ] && cat /data/adb/.config/AZenith/customFreqOffset'`
    );
    0 === l && (d.value = m.trim());
  }
}
async function checkKillLog() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.logd"
  );
  0 === c && (document.getElementById("logd").checked = "1" === s.trim());
}
async function setKillLog(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.logd 1"
      : "setprop persist.sys.azenithconf.logd 0"
  );
}
async function startService() {
  try {
    // Check current profile
    let { stdout: c } = await executeCommand(
      "cat /data/adb/.config/AZenith/API/current_profile"
    );
    let s = c.trim();

    if (s === "0") {
      showToast("Can't Restart, Initializing Daemon");
      return;
    }

    let { stdout: pid } = await executeCommand(
      "/system/bin/toybox pidof sys.azenith-service"
    );
    if (!pid || pid.trim() === "") {
      showToast("Service dead, Please reboot!");
      return;
    }

    showToast("Restarting Daemon...");

    await executeCommand(
      "setprop persist.sys.azenith.state stopped && pkill -9 -f sys.azenith-service; su -c '/data/adb/modules/AZenith/system/bin/sys.azenith-service > /dev/null 2>&1 & disown'"
    );

    await checkServiceStatus();
  } catch (r) {
    showToast("Failed to restart daemon");
    console.error("startService error:", r);
  }
}

async function checkGPreload() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.APreload"
  );
  0 === c && (document.getElementById("GPreload").checked = "1" === s.trim());
}
async function setGPreloadStatus(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.APreload 1"
      : "setprop persist.sys.azenithconf.APreload 0"
  );
}
async function checkRamBoost() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.clearbg"
  );
  0 === c && (document.getElementById("clearbg").checked = "1" === s.trim());
}
async function setRamBoostStatus(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.clearbg 1"
      : "setprop persist.sys.azenithconf.clearbg 0"
  );
}
async function checkmalisched() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.malisched"
  );
  0 === c && (document.getElementById("malisched").checked = "1" === s.trim());
}
async function setmalisched(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.malisched 1"
      : "setprop persist.sys.azenithconf.malisched 0"
  );
}
function showMaliSchedIfMediatek() {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const MaliSchedDiv = document.getElementById("malisched-container");
  if (MaliSchedDiv) {
    MaliSchedDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
}
async function showColorScheme() {
  let c = document.getElementById("schemeModal"),
    s = c.querySelector(".scheme-content");
  document.body.classList.add("modal-open"), c.classList.add("show");
  let r = window.innerHeight,
    d = () => {
      window.innerHeight < r - 150
        ? (s.style.transform = "translateY(-10%) scale(1)")
        : (s.style.transform = "translateY(0) scale(1)");
    };
  window.addEventListener("resize", d, {
    passive: !0,
  }),
    (c._resizeHandler = d),
    d();
}

function hidecolorscheme() {
  let c = document.getElementById("schemeModal");
  c.classList.remove("show"),
    document.body.classList.remove("modal-open"),
    showToast("Saved color scheme settings."),
    c._resizeHandler &&
      (window.removeEventListener("resize", c._resizeHandler),
      delete c._resizeHandler);
}
const CACHE_FILE_PATH = "/data/adb/.config/AZenith/color_scheme";

function saveDisplaySettings(c, s, r, d) {
  let l = `sh -c 'echo "${c} ${s} ${r} ${d}" > ${CACHE_FILE_PATH}'`;
  executeCommand(l);
}
async function loadDisplaySettings() {
  try {
    let c = await executeCommand(`sh -c "cat '${CACHE_FILE_PATH}'"`),
      [s, r, d, l] = (
        "object" == typeof c && c.stdout ? c.stdout.trim() : String(c).trim()
      )
        .split(/\s+/)
        .map(Number);
    if ([s, r, d, l].some(isNaN))
      return (
        showToast("Invalid color_scheme format. Using safe defaults."),
        {
          red: 1e3,
          green: 1e3,
          blue: 1e3,
          saturation: 1e3,
        }
      );
    return {
      red: s,
      green: r,
      blue: d,
      saturation: l,
    };
  } catch (m) {
    return (
      console.log("Error reading display settings:", m),
      showToast("color_scheme not found. Using defaults."),
      {
        red: 1e3,
        green: 1e3,
        blue: 1e3,
        saturation: 1e3,
      }
    );
  }
}
async function setRGB(c, s, r) {
  await executeCommand(
    `service call SurfaceFlinger 1015 i32 1 f ${c / 1e3} f 0 f 0 f 0 f 0 f ${
      s / 1e3
    } f 0 f 0 f 0 f 0 f ${r / 1e3} f 0 f 0 f 0 f 0 f 1`
  );
}
async function setSaturation(c) {
  await executeCommand(`service call SurfaceFlinger 1022 f ${c / 1e3}`);
}
async function resetDisplaySettings() {
  await executeCommand(
    "service call SurfaceFlinger 1015 i32 1 f 1 f 0 f 0 f 0 f 0 f 1 f 0 f 0 f 0 f 0 f 1 f 0 f 0 f 0 f 0 f 1"
  ),
    await executeCommand("service call SurfaceFlinger 1022 f 1"),
    saveDisplaySettings(1e3, 1e3, 1e3, 1e3),
    (document.getElementById("red").value = 1e3),
    (document.getElementById("green").value = 1e3),
    (document.getElementById("blue").value = 1e3),
    (document.getElementById("saturation").value = 1e3),
    showToast("Display settings reset!");
}
async function checkAI() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.AIenabled"
  );
  0 === c && (document.getElementById("disableai").checked = "0" === s.trim());
}
async function setAI(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.AIenabled 0"
      : "setprop persist.sys.azenithconf.AIenabled 1"
  ),
    await executeCommand(
      c
        ? "mv /data/adb/.config/AZenith/gamelist/gamelist.txt /data/adb/.config/AZenith/gamelist/gamelist.bin"
        : "mv /data/adb/.config/AZenith/gamelist/gamelist.bin /data/adb/.config/AZenith/gamelist/gamelist.txt"
    );
}
async function applyperformanceprofile() {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("1" === c.trim()) {
    showToast("You are already in Performance Profile");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 1 >/dev/null 2>&1 &"
  ),
    setTimeout(() => {
      executeCommand("echo 1 > /data/adb/.config/AZenith/API/current_profile");
    }, 300),
    showToast("Applying Performance Profile");
}
async function applybalancedprofile() {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("2" === c.trim()) {
    showToast("Already in Balanced Profile");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 2 >/dev/null 2>&1 &"
  ),
    setTimeout(() => {
      executeCommand("echo 2 > /data/adb/.config/AZenith/API/current_profile");
    }, 300),
    showToast("Applying Balanced Profile");
}
async function applyecomode() {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("3" === c.trim()) {
    showToast("Already in ECO Mode");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 3 >/dev/null 2>&1 &"
  ),
    setTimeout(() => {
      executeCommand("echo 3 > /data/adb/.config/AZenith/API/current_profile");
    }, 300),
    showToast("Applying ECO Mode");
}

async function checkjit() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.justintime"
  );
  0 === c && (document.getElementById("jit").checked = "1" === s.trim());
}
async function setjit(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.justintime 1"
      : "setprop persist.sys.azenithconf.justintime 0"
  );
}

async function checkdtrace() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.disabletrace"
  );
  0 === c && (document.getElementById("trace").checked = "1" === s.trim());
}
async function setdtrace(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.disabletrace 1"
      : "setprop persist.sys.azenithconf.disabletrace 0"
  );
}

async function checktoast() {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.showtoast"
  );
  0 === c && (document.getElementById("toast").checked = "1" === s.trim());
}
async function settoast(c) {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.showtoast 1"
      : "setprop persist.sys.azenithconf.showtoast 0"
  );
}

function setupUIListeners() {
  const c = document.getElementById("disableai");
  const s = document.getElementById("extraSettings");

  if (c && s) {
    c.addEventListener("change", function () {
      setAI(this.checked);
      s.style.display = this.checked ? "block" : "none";
      s.classList.toggle("show", this.checked);
    });

    executeCommand("getprop persist.sys.azenithconf.AIenabled").then(
      ({ stdout: r }) => {
        const d = r.trim() === "0";
        c.checked = d;
        s.style.display = d ? "block" : "none";
        s.classList.toggle("show", d);
      }
    );
  }

  async function savelog() {
    try {
      await executeCommand(
        "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf saveLog"
      );
      showToast("Saved Log");
    } catch (e) {
      showToast("Failed to save log");
      console.error("saveLog error:", e);
    }
  }

  // Button Clicks
  document
    .getElementById("startButton")
    ?.addEventListener("click", startService);
  document.getElementById("savelogButton")?.addEventListener("click", savelog);
  document
    .getElementById("applyperformance")
    ?.addEventListener("click", applyperformanceprofile);
  document
    .getElementById("applybalanced")
    ?.addEventListener("click", applybalancedprofile);
  document
    .getElementById("applypowersave")
    ?.addEventListener("click", applyecomode);
  document.getElementById("FSTrim")?.addEventListener("click", applyFSTRIM);

  // Toggle Switches
  document
    .getElementById("fpsged")
    ?.addEventListener("change", (e) => setfpsged(e.target.checked));
  document
    .getElementById("jit")
    ?.addEventListener("change", (e) => setjit(e.target.checked));
  document
    .getElementById("toast")
    ?.addEventListener("change", (e) => settoast(e.target.checked));
  document
    .getElementById("trace")
    ?.addEventListener("change", (e) => setdtrace(e.target.checked));
  document
    .getElementById("GPreload")
    ?.addEventListener("change", (e) => setGPreloadStatus(e.target.checked));
  document
    .getElementById("clearbg")
    ?.addEventListener("change", (e) => setRamBoostStatus(e.target.checked));
  document
    .getElementById("SFL")
    ?.addEventListener("change", (e) => setSFL(e.target.checked));
  document
    .getElementById("DThermal")
    ?.addEventListener("change", (e) => setDThermal(e.target.checked));
  document
    .getElementById("OPPIndex")
    ?.addEventListener("change", (e) => setOPPIndexStatus(e.target.checked));
  document
    .getElementById("schedtunes")
    ?.addEventListener("change", (e) => setschedtunes(e.target.checked));
  document
    .getElementById("logger")
    ?.addEventListener("change", (e) => setlogger(e.target.checked));
  document
    .getElementById("iosched")
    ?.addEventListener("change", (e) => setiosched(e.target.checked));
  document
    .getElementById("malisched")
    ?.addEventListener("change", (e) => setmalisched(e.target.checked));
  document
    .getElementById("DoNoDis")
    ?.addEventListener("change", (e) => setDND(e.target.checked));
  document
    .getElementById("logd")
    ?.addEventListener("change", (e) => setKillLog(e.target.checked));
  document
    .getElementById("Zepass")
    ?.addEventListener("change", (e) =>
      setBypassChargeStatus(e.target.checked)
    );

  // Select dropdowns
  document
    .getElementById("cpuGovernorGame")
    ?.addEventListener("change", (e) => setGameCpuGovernor(e.target.value));
  document
    .getElementById("cpuGovernor")
    ?.addEventListener("change", (e) => setDefaultCpuGovernor(e.target.value));
  document
    .getElementById("GovernorPowersave")
    ?.addEventListener("change", (e) => setGovernorPowersave(e.target.value));
  document
    .getElementById("cpuFreq")
    ?.addEventListener("change", (e) => setCpuFreqOffsets(e.target.value));
  document
    .getElementById("disablevsync")
    ?.addEventListener("change", (e) => setVsyncValue(e.target.value));

  // Color scheme buttons
  document
    .getElementById("colorschemebutton")
    ?.addEventListener("click", showColorScheme);
  document
    .getElementById("applybutton")
    ?.addEventListener("click", hidecolorscheme);

  // Gamelist modal buttons
  document
    .getElementById("editGamelistButton")
    ?.addEventListener("click", showGameListModal);
  document
    .getElementById("cancelButton")
    ?.addEventListener("click", hideGameListModal);
  document
    .getElementById("saveGamelistButton")
    ?.addEventListener("click", saveGameList);
}

function heavyInit() {
  checkAvailableRAM(),
    checkProfile(),
    checkServiceStatus(),
    checkGPreload(),
    loadColorSchemeSettings(),
    checkCPUFrequencies(),
    setInterval(checkCPUFrequencies, 2e3),
    setInterval(checkServiceStatus, 5e3),
    setInterval(checkProfile, 5e3),
    setInterval(showRandomMessage, 1e4),
    setInterval(checkAvailableRAM, 5e3),
    Promise.all([
      checkModuleVersion(),
      checkCPUInfo(),
      checkKernelVersion(),
      getAndroidVersion(),
      checkfpsged(),
      checkOPPIndexStatus(),
      checkDThermal(),
      checkiosched(),
      checkmalisched(),
      checkAI(),
      checkDND(),
      loadCpuFreq(),
      checkdtrace(),
      checkjit(),
      loadCpuGovernors(),
      loadGameGovernors(),
      GovernorPowersave(),
      checktoast(),
      loadVsyncValue(),
      checkBypassChargeStatus(),
      checkschedtunes(),
      checkSFL(),
      checkKillLog(),
      checklogger(),
      checkRamBoost(),
    ]).then(() => {
      document.getElementById("loading-screen").classList.add("hidden");
    });
}
document.getElementById("disableai").addEventListener("change", function () {
  setAI(this.checked),
    document
      .getElementById("extraSettings")
      .classList.toggle("show", this.checked);
});
let currentColor = {
  red: 1e3,
  green: 1e3,
  blue: 1e3,
  saturation: 1e3,
};

function debounce(c, s = 200) {
  let r;
  return (...d) => {
    clearTimeout(r), (r = setTimeout(() => c(...d), s));
  };
}

function updateColorState({ red: c, green: s, blue: r, saturation: d }) {
  (c !== currentColor.red ||
    s !== currentColor.green ||
    r !== currentColor.blue ||
    d !== currentColor.saturation) &&
    ((currentColor = {
      red: c,
      green: s,
      blue: r,
      saturation: d,
    }),
    saveDisplaySettings(c, s, r, d),
    setRGB(c, s, r),
    setSaturation(d));
}
async function loadColorSchemeSettings() {
  let c = document.getElementById("red"),
    s = document.getElementById("green"),
    r = document.getElementById("blue"),
    d = document.getElementById("saturation"),
    l = document.getElementById("reset-btn"),
    m = await loadDisplaySettings();
  (c.value = m.red),
    (s.value = m.green),
    (r.value = m.blue),
    (d.value = m.saturation),
    (currentColor = m),
    await setRGB(m.red, m.green, m.blue),
    await setSaturation(m.saturation);
  let h = debounce(() => {
    updateColorState({
      red: parseInt(c.value),
      green: parseInt(s.value),
      blue: parseInt(r.value),
      saturation: parseInt(d.value),
    });
  }, 100);
  c.addEventListener("input", h),
    s.addEventListener("input", h),
    r.addEventListener("input", h),
    d.addEventListener("input", h),
    l.addEventListener("click", async () => {
      (c.value = s.value = r.value = d.value = 1e3),
        await setRGB(1e3, 1e3, 1e3),
        await setSaturation(1e3),
        saveDisplaySettings(1e3, 1e3, 1e3, 1e3),
        showToast("Display settings reset!");
    });
}

document.addEventListener("DOMContentLoaded", () => {
  showRandomMessage();
  setupUIListeners();

  const banner = document.getElementById("Banner");
  const avatar = document.getElementById("Avatar");
  const scheme = document.getElementById("Scheme");

  if (banner) banner.src = BannerZenith;
  if (avatar) avatar.src = AvatarZenith;
  if (scheme) scheme.src = SchemeBanner;
});
