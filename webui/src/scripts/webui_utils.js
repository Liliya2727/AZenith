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
import ResoBanner from "/webui.reso.avif";
import { exec } from "kernelsu";

const executeCommand = async (cmd, cwd = null) => {
  try {
    const { errno, stdout, stderr } = await exec(cmd, cwd ? { cwd } : {});
    return { errno, stdout, stderr };
  } catch (e) {
    return { errno: -1, stdout: "", stderr: e.message || String(e) };
  }
};

const showToast = async (c) => {
  ksu.toast(c);
};

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

const showRandomMessage = () => {
  const c = document.getElementById("msg");
  const s = randomMessages[Math.floor(Math.random() * randomMessages.length)];
  c.textContent = s;
};

const checkModuleVersion = async () => {
  try {
    const { errno: c, stdout: s } = await executeCommand(
      "echo 'Version :' && grep \"version=\" /data/adb/modules/AZenith/module.prop | awk -F'=' '{print $2}'"
    );
    if (c === 0) {
      document.getElementById("moduleVer").textContent = s.trim();
    }
  } catch {
    // optional: handle errors here if needed
  }
};

const checkProfile = async () => {
  try {
    const { errno: c, stdout: s } = await executeCommand(
      "cat /data/adb/.config/AZenith/API/current_profile"
    );

    if (c === 0) {
      const r = s.trim();
      const d = document.getElementById("CurProfile");

      let l =
        {
          0: "Initializing...",
          1: "Performance",
          2: "Balanced",
          3: "ECO Mode",
        }[r] || "Unknown";

      // Check cpulimit property
      const { errno: c2, stdout: s2 } = await executeCommand(
        "getprop persist.sys.azenithconf.cpulimit"
      );

      if (c2 === 0 && s2.trim() === "1") {
        l += " (Lite)";
      }

      d.textContent = l;

      switch (l.replace(" (Lite)", "")) {
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
    showToast("Error checking profile:", m);
  }
};

const checkAvailableRAM = async () => {
  try {
    const { stdout: c } = await executeCommand(
      "cat /proc/meminfo | grep -E 'MemTotal|MemAvailable'"
    );

    const s = c.trim().split("\n");
    let r = 0, d = 0;

    for (const l of s) {
      if (l.includes("MemTotal")) r = parseInt(l.match(/\d+/)[0]);
      else if (l.includes("MemAvailable")) d = parseInt(l.match(/\d+/)[0]);
    }

    if (r && d) {
      const m = r - d;
      const h = (m / 1024 / 1024).toFixed(2);
      const g = (r / 1024 / 1024).toFixed(2);
      const f = (d / 1024 / 1024).toFixed(2);
      const y = ((m / r) * 100).toFixed(0);

      document.getElementById("ramInfo").textContent = 
        `${h} GB / ${g} GB (${y}%) — Available: ${f} GB`;
    } else {
      document.getElementById("ramInfo").textContent = "Error reading memory";
    }
  } catch {
    document.getElementById("ramInfo").textContent = "Error";
  }
};

const checkCPUFrequencies = async () => {
  const c = document.getElementById("cpuFreqInfo");
  let s = "";

  try {
    const { stdout: r } = await executeCommand(
      "ls /sys/devices/system/cpu/cpufreq/ | grep policy"
    );
    const d = r.trim().split("\n").filter(Boolean);

    for (const l of d) {
      const m = `/sys/devices/system/cpu/cpufreq/${l}`;
      const [{ stdout: h }, { stdout: g }] = await Promise.all([
        executeCommand(`cat ${m}/scaling_cur_freq`),
        executeCommand(`cat ${m}/related_cpus`),
      ]);

      const f = (parseInt(h.trim()) / 1e3).toFixed(0);
      const y = g.trim().split(" ").join(", ");
      s += `Cluster ${y}: ${f} MHz<br>`;
    }

    c.innerHTML = s.trim();
  } catch {
    c.innerHTML = "Failed to read CPU frequencies.";
  }
};
let cachedSOCData = null;
const fetchSOCDatabase = async () => {
  if (!cachedSOCData) {
    try {
      cachedSOCData = await (await fetch("webui.soclist.json")).json();
    } catch {
      cachedSOCData = {};
    }
  }
  return cachedSOCData;
};

const checkCPUInfo = async () => {
  const c = localStorage.getItem("soc_info");
  try {
    const { errno: s, stdout: r } = await executeCommand("getprop ro.soc.model");
    if (s === 0) {
      let d = r.trim().replace(/\s+/g, "").toUpperCase();
      const l = await fetchSOCDatabase();
      let m = l[d];

      if (!m) {
        for (let h = d.length; h >= 6; h--) {
          const g = d.substring(0, h);
          if (l[g]) {
            m = l[g];
            break;
          }
        }
      }

      if (!m) m = d;

      document.getElementById("cpuInfo").textContent = m;

      if (c !== m) {
        localStorage.setItem("soc_info", m);
      }
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
};

const checkKernelVersion = async () => {
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
};

const getAndroidVersion = async () => {
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
};

const checkServiceStatus = async () => {
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
};
const checkfpsged = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.fpsged"
  );
  0 === c && (document.getElementById("fpsged").checked = "1" === s.trim());
};

const setfpsged = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.fpsged 1"
      : "setprop persist.sys.azenithconf.fpsged 0"
  );
};

const showFPSGEDIfMediatek = () => {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const fpsgedDiv = document.getElementById("fpsged-container");
  if (fpsgedDiv) {
    fpsgedDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
};

const checkDND = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.dnd"
  );
  0 === c && (document.getElementById("DoNoDis").checked = "1" === s.trim());
};

const setDND = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.dnd 1"
      : "setprop persist.sys.azenithconf.dnd 0"
  );
};

const checkBypassChargeStatus = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.bypasschg"
  );
  0 === c && (document.getElementById("Zepass").checked = "1" === s.trim());
};

const setBypassChargeStatus = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.bypasschg 1"
      : "setprop persist.sys.azenithconf.bypasschg 0"
  );
};
const showBypassIfMTK = () => {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const ZepassDiv = document.getElementById("Zepass-container");
  if (ZepassDiv) {
    ZepassDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
};

const checkLiteModeStatus = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.cpulimit"
  );
  0 === c && (document.getElementById("LiteMode").checked = "1" === s.trim());
};

const setLiteModeStatus = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.cpulimit 1"
      : "setprop persist.sys.azenithconf.cpulimit 0"
  );
};

const checkDThermal = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.DThermal"
  );
  0 === c && (document.getElementById("DThermal").checked = "1" === s.trim());
};

const setDThermal = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.DThermal 1"
      : "setprop persist.sys.azenithconf.DThermal 0"
  );
};

const showThermalIfMTK = () => {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const thermalDiv = document.getElementById("DThermal-container");
  if (thermalDiv) {
    thermalDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
};

const checkSFL = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.SFL"
  );
  0 === c && (document.getElementById("SFL").checked = "1" === s.trim());
};

const setSFL = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.SFL 1"
      : "setprop persist.sys.azenithconf.SFL 0"
  );
};
const checkschedtunes = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.schedtunes"
  );
  0 === c && (document.getElementById("schedtunes").checked = "1" === s.trim());
};

const setschedtunes = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.schedtunes 1"
      : "setprop persist.sys.azenithconf.schedtunes 0"
  );
};

const checkiosched = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.iosched"
  );
  0 === c && (document.getElementById("iosched").checked = "1" === s.trim());
};

const setiosched = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.iosched 1"
      : "setprop persist.sys.azenithconf.iosched 0"
  );
};

const applyFSTRIM = async () => {
  await executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf FSTrim"
  );
  showToast("Trimmed Unused Blocks");
};

const setDefaultCpuGovernor = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(
    `setprop persist.sys.azenith.custom_default_cpu_gov ${c}`
  );
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "2" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov ${c}`
    ));
};

const loadCpuGovernors = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("cpuGovernor");
    d.innerHTML = "";
    r.forEach((c) => {
      let s = document.createElement("option");
      s.value = c;
      s.textContent = c;
      d.appendChild(s);
    });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_default_cpu_gov)" ] && getprop persist.sys.azenith.custom_default_cpu_gov || getprop persist.sys.azenith.default_cpu_gov'`
    );
    0 === l && (d.value = m.trim());
  }
};

const setGovernorPowersave = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(
    `setprop persist.sys.azenith.custom_powersave_cpu_gov ${c}`
  );
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "3" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov ${c}`
    ));
};

const GovernorPowersave = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("GovernorPowersave");
    d.innerHTML = "";
    r.forEach((c) => {
      let s = document.createElement("option");
      s.value = c;
      s.textContent = c;
      d.appendChild(s);
    });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_powersave_cpu_gov)" ] && getprop persist.sys.azenith.custom_powersave_cpu_gov'`
    );
    0 === l && (d.value = m.trim());
  }
};

const hideGameListModal = () => {
  let c = document.getElementById("gamelistModal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  c._resizeHandler &&
    (window.removeEventListener("resize", c._resizeHandler),
    delete c._resizeHandler);
};

let originalGamelist = "";

const showGameListModal = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.AIenabled"
  );
  if (0 === c && "0" === s.trim()) {
    showToast("Can't access in current mode");
    return;
  }

  const r = document.getElementById("gamelistModal");
  const d = document.getElementById("gamelistInput");
  const searchInput = document.getElementById("gamelistSearch");
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
    searchInput.value = "";
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
};

const filterGameList = () => {
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
};

const saveGameList = async () => {
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

  if (!searchTerm) {
    const outputString = editedLines.join("|").replace(/"/g, '\\"');
    await executeCommand(
      `echo "${outputString}" > /data/adb/.config/AZenith/gamelist/gamelist.txt`
    );
    showToast(`Saved ${editedLines.length} packages`);
    hideGameListModal();
    return;
  }

  let editedIndex = 0;
  const mergedLines = originalLines.map((line) => {
    if (line.toLowerCase().includes(searchTerm)) {
      const replacement = editedLines[editedIndex++]?.trim();
      return replacement || line;
    }
    return line;
  });

  const outputString = mergedLines.join("|").replace(/"/g, '\\"');
  await executeCommand(
    `echo "${outputString}" > /data/adb/.config/AZenith/gamelist/gamelist.txt`
  );
  showToast(`Saved ${mergedLines.length} packages`);
  hideGameListModal();
};
const checklogger = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenith.debugmode"
  );
  0 === c && (document.getElementById("logger").checked = "true" === s.trim());
};

const setlogger = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenith.debugmode true"
      : "setprop persist.sys.azenith.debugmode false"
  );
};

const setVsyncValue = async (c) => {
  await executeCommand(`setprop persist.sys.azenithconf.vsync ${c}`);
  await executeCommand(
    `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf disablevsync ${c}`
  );
};

const loadVsyncValue = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithdebug.vsynclist"
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
      `sh -c '[ -n "$(getprop persist.sys.azenithconf.vsync)" ] && getprop persist.sys.azenithconf.vsync'`
    );
    0 === l && (d.value = m.trim());
  }
};

const setCpuFreqOffsets = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`setprop persist.sys.azenithconf.freqoffset ${c}`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  if (d === 0) {
    let profile = l.trim();
    if (profile === "2" || profile === "3") {
      await executeCommand(
        `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsfreqs`
      );
    }
  }
};

const loadCpuFreq = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithdebug.freqlist"
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
      `sh -c '[ -n "$(getprop persist.sys.azenithconf.freqoffset)" ] && getprop persist.sys.azenithconf.freqoffset'`
    );
    0 === l && (d.value = m.trim());
  }
};
const checkKillLog = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.logd"
  );
  0 === c && (document.getElementById("logd").checked = "1" === s.trim());
};

const setKillLog = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.logd 1"
      : "setprop persist.sys.azenithconf.logd 0"
  );
};

const startService = async () => {
  try {
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
};

const checkGPreload = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.APreload"
  );
  0 === c && (document.getElementById("GPreload").checked = "1" === s.trim());
};

const setGPreloadStatus = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.APreload 1"
      : "setprop persist.sys.azenithconf.APreload 0"
  );
};
const checkRamBoost = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.clearbg"
  );
  0 === c && (document.getElementById("clearbg").checked = "1" === s.trim());
};

const setRamBoostStatus = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.clearbg 1"
      : "setprop persist.sys.azenithconf.clearbg 0"
  );
};

const checkmalisched = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.malisched"
  );
  0 === c && (document.getElementById("malisched").checked = "1" === s.trim());
};

const setmalisched = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.malisched 1"
      : "setprop persist.sys.azenithconf.malisched 0"
  );
};

const showMaliSchedIfMediatek = () => {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const MaliSchedDiv = document.getElementById("malisched-container");
  if (MaliSchedDiv) {
    MaliSchedDiv.style.display = soc.includes("mediatek") ? "flex" : "none";
  }
};

const showColorScheme = async () => {
  const c = document.getElementById("schemeModal"),
        s = c.querySelector(".scheme-content");
  document.body.classList.add("modal-open");
  c.classList.add("show");
  const r = window.innerHeight;
  const d = () => {
    window.innerHeight < r - 150
      ? (s.style.transform = "translateY(-10%) scale(1)")
      : (s.style.transform = "translateY(0) scale(1)");
  };
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
  d();
};

const hidecolorscheme = () => {
  const c = document.getElementById("schemeModal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  showToast("Saved color scheme settings.");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const saveDisplaySettings = (c, s, r, d) => {
  const cmd = `sh -c 'setprop persist.sys.azenithconf.schemeconfig "${c} ${s} ${r} ${d}"'`;
  executeCommand(cmd);
};

const loadDisplaySettings = async () => {
  try {
    const c = await executeCommand(
      `sh -c "getprop persist.sys.azenithconf.schemeconfig"`
    );
    const [s, r, d, l] = (
      typeof c === "object" && c.stdout ? c.stdout.trim() : String(c).trim()
    )
      .split(/\s+/)
      .map(Number);

    if ([s, r, d, l].some(isNaN)) {
      showToast("Invalid color_scheme format. Using safe defaults.");
      return { red: 1000, green: 1000, blue: 1000, saturation: 1000 };
    }

    return { red: s, green: r, blue: d, saturation: l };
  } catch (m) {
    console.log("Error reading display settings:", m);
    showToast("color_scheme not found. Using defaults.");
    return { red: 1000, green: 1000, blue: 1000, saturation: 1000 };
  }
};

const setRGB = async (c, s, r) => {
  await executeCommand(
    `service call SurfaceFlinger 1015 i32 1 f ${c / 1000} f 0 f 0 f 0 f 0 f ${s / 1000} f 0 f 0 f 0 f 0 f ${r / 1000} f 0 f 0 f 0 f 0 f 1`
  );
};

const setSaturation = async (c) => {
  await executeCommand(`service call SurfaceFlinger 1022 f ${c / 1000}`);
};

const resetDisplaySettings = async () => {
  await executeCommand(
    "service call SurfaceFlinger 1015 i32 1 f 1 f 0 f 0 f 0 f 0 f 1 f 0 f 0 f 0 f 0 f 1 f 0 f 0 f 0 f 0 f 1"
  );
  await executeCommand("service call SurfaceFlinger 1022 f 1");
  saveDisplaySettings(1000, 1000, 1000, 1000);
  document.getElementById("red").value = 1000;
  document.getElementById("green").value = 1000;
  document.getElementById("blue").value = 1000;
  document.getElementById("saturation").value = 1000;
  showToast("Display settings reset!");
};
const checkAI = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.AIenabled"
  );
  0 === c && (document.getElementById("disableai").checked = "0" === s.trim());
};

const setAI = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.AIenabled 0"
      : "setprop persist.sys.azenithconf.AIenabled 1"
  );
  await executeCommand(
    c
      ? "mv /data/adb/.config/AZenith/gamelist/gamelist.txt /data/adb/.config/AZenith/gamelist/gamelist.bin"
      : "mv /data/adb/.config/AZenith/gamelist/gamelist.bin /data/adb/.config/AZenith/gamelist/gamelist.txt"
  );
};

const applyperformanceprofile = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("1" === c.trim()) {
    showToast("You are already in Performance Profile");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 1 >/dev/null 2>&1 &"
  );
  setTimeout(() => {
    executeCommand("echo 1 > /data/adb/.config/AZenith/API/current_profile");
  }, 300);
  showToast("Applying Performance Profile");
};

const applybalancedprofile = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("2" === c.trim()) {
    showToast("Already in Balanced Profile");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 2 >/dev/null 2>&1 &"
  );
  setTimeout(() => {
    executeCommand("echo 2 > /data/adb/.config/AZenith/API/current_profile");
  }, 300);
  showToast("Applying Balanced Profile");
};

const applyecomode = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("3" === c.trim()) {
    showToast("Already in ECO Mode");
    return;
  }
  executeCommand(
    "/data/adb/modules/AZenith/system/bin/sys.azenith-profilesettings 3 >/dev/null 2>&1 &"
  );
  setTimeout(() => {
    executeCommand("echo 3 > /data/adb/.config/AZenith/API/current_profile");
  }, 300);
  showToast("Applying ECO Mode");
};

const checkjit = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.justintime"
  );
  0 === c && (document.getElementById("jit").checked = "1" === s.trim());
};

const setjit = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.justintime 1"
      : "setprop persist.sys.azenithconf.justintime 0"
  );
};

const checkdtrace = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.disabletrace"
  );
  0 === c && (document.getElementById("trace").checked = "1" === s.trim());
};

const setdtrace = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.disabletrace 1"
      : "setprop persist.sys.azenithconf.disabletrace 0"
  );
};

const checktoast = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.showtoast"
  );
  0 === c && (document.getElementById("toast").checked = "1" === s.trim());
};

const settoast = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.showtoast 1"
      : "setprop persist.sys.azenithconf.showtoast 0"
  );
};

const showCustomResolution = async () => {
  let c = document.getElementById("resomodal"),
    s = c.querySelector(".reso-content");
  document.body.classList.add("modal-open"), c.classList.add("show");

  await checkResolution();

  let r = window.innerHeight,
    d = () => {
      window.innerHeight < r - 150
        ? (s.style.transform = "translateY(-10%) scale(1)")
        : (s.style.transform = "translateY(0) scale(1)");
    };
  window.addEventListener("resize", d, { passive: true }),
    (c._resizeHandler = d),
    d();
};

const hideCustomResolution = () => {
  let c = document.getElementById("resomodal");
  c.classList.remove("show"),
    document.body.classList.remove("modal-open"),
    c._resizeHandler &&
      (window.removeEventListener("resize", c._resizeHandler),
      delete c._resizeHandler);
};

const checkResolution = async () => {
  let { stdout: w } = await executeCommand(
    "getprop persist.sys.azenithconf.wdsize"
  );
  let { stdout: h } = await executeCommand(
    "getprop persist.sys.azenithconf.hgsize"
  );

  document.getElementById("reso-width").value = w.trim() || "";
  document.getElementById("reso-height").value = h.trim() || "";
};

const setResolution = async (width, height) => {
  if (!width || !height) {
    showToast("Invalid resolution!");
    return;
  }

  let { stdout: freqpropsStr } = await executeCommand(
    "getprop persist.sys.azenithconf.scale"
  );
  let freqprops = parseInt(freqpropsStr.trim(), 10) || 0;

  if (freqprops === 0) {
    await executeCommand(`wm size ${width}x${height}`);
  } else if (freqprops === 1) {
    await executeCommand("wm size reset");
  }
  await executeCommand(`setprop persist.sys.azenithconf.wdsize ${width}`);
  await executeCommand(`setprop persist.sys.azenithconf.hgsize ${height}`);

  showToast(
    freqprops === 0
      ? `Resolution applied: ${width}x${height}`
      : `Resolution saved: ${width}x${height}, Current resolution set to default`
  );
};

const resetResolution = async () => {
  let { stdout: res } = await executeCommand("wm size");
  let match = res.match(/Physical size:\s*(\d+)x(\d+)/);
  if (match) {
    document.getElementById("reso-width").value = match[1];
    document.getElementById("reso-height").value = match[2];

    // reset back to defaults
    await executeCommand(`setprop persist.sys.azenithconf.wdsize ${match[1]}`);
    await executeCommand(`setprop persist.sys.azenithconf.hgsize ${match[2]}`);

    showToast(`Default resolution: ${match[1]}x${match[2]}`);
  } else {
    showToast("Failed to detect default resolution!");
  }
};

const checkunderscale = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.scale"
  );
  0 === c && (document.getElementById("applyinperf").checked = "1" === s.trim());
};

const setunderscale = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.scale 1"
      : "setprop persist.sys.azenithconf.scale 0"
  );
};

const setIObalance = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`setprop persist.sys.azenith.custom_default_balanced_IO ${c}`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "2" === l.trim() &&
    (await executeCommand(`/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO ${c}`));
};

const loadIObalance = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler | tr -d '[]'"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("ioSchedulerBalanced");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ] && getprop persist.sys.azenith.custom_default_balanced_IO || getprop persist.sys.azenith.default_balanced_IO'`
    );
    0 === l && (d.value = m.trim());
  }
};

const setIOperformance = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(`setprop persist.sys.azenith.custom_performance_IO ${c}`);
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "1" === l.trim() &&
    (await executeCommand(`/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO ${c}`));
};

const loadIOperformance = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler | tr -d '[]'"
  );
  if (0 === c) {
    let r = s.trim().split(/\s+/),
      d = document.getElementById("ioSchedulerPerformance");
    (d.innerHTML = ""),
      r.forEach((c) => {
        let s = document.createElement("option");
        (s.value = c), (s.textContent = c), d.appendChild(s);
      });
    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_performance_IO)" ] && getprop persist.sys.azenith.custom_performance_IO'`
    );
    0 === l && (d.value = m.trim());
  }
};

const setIOpowersave = async (c) => {
  const s = "/data/adb/.config/AZenith",
        r = `${s}/API/current_profile`;
  await executeCommand(`setprop persist.sys.azenith.custom_powersave_IO ${c}`);
  const { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  if (d === 0 && l.trim() === "3") {
    await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO ${c}`
    );
  }
};

const loadIOpowersave = async () => {
  const { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler | tr -d '[]'"
  );
  if (c === 0) {
    const r = s.trim().split(/\s+/),
          d = document.getElementById("ioSchedulerPowersave");
    d.innerHTML = "";
    r.forEach((c) => {
      const s = document.createElement("option");
      s.value = c;
      s.textContent = c;
      d.appendChild(s);
    });
    const { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_powersave_IO)" ] && getprop persist.sys.azenith.custom_powersave_IO'`
    );
    if (l === 0) d.value = m.trim();
  }
};

const showAdditionalSettings = async () => {
  const c = document.getElementById("additional-modal"),
        s = c.querySelector(".additional-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  const r = window.innerHeight;
  const d = () => {
    s.style.transform = window.innerHeight < r - 150
      ? "translateY(-10%) scale(1)"
      : "translateY(0) scale(1)";
  };
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
  d();
};

const hideAdditionalSettings = () => {
  const c = document.getElementById("additional-modal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const showPreferenceSettings = async () => {
  const c = document.getElementById("preference-modal"),
        s = c.querySelector(".preference-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  const r = window.innerHeight;
  const d = () => {
    s.style.transform = window.innerHeight < r - 150
      ? "translateY(-10%) scale(1)"
      : "translateY(0) scale(1)";
  };
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
  d();
};

const hidePreferenceSettings = () => {
  const c = document.getElementById("preference-modal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const hideGamelistSettings = () => {
  const c = document.getElementById("gamelist-modal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const hideSchemeSettings = () => {
  const c = document.getElementById("scheme-modal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const hideResoSettings = () => {
  const c = document.getElementById("reso-modal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const savelog = async () => {
  try {
    await executeCommand(
      "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf saveLog"
    );
    showToast("Saved Log");
  } catch (e) {
    showToast("Failed to save log");
    console.error("saveLog error:", e);
  }
};
  
const setupUIListeners = () => {
  const banner = document.getElementById("Banner");
  const avatar = document.getElementById("Avatar");
const scheme = document.getElementById("Scheme");
const reso = document.getElementById("Reso");

if (banner) banner.src = BannerZenith;
if (avatar) avatar.src = AvatarZenith;
if (scheme) scheme.src = SchemeBanner;
if (reso) reso.src = ResoBanner;
  // Button Clicks
  document
    .getElementById("startButton")
    ?.addEventListener("click", startService);
  document
    .getElementById("savelogButton")
    ?.addEventListener("click", savelog);
  document
    .getElementById("FSTrim")
    ?.addEventListener("click", applyFSTRIM);

  // Toggle Switches
  document
    .getElementById("fpsged")
    ?.addEventListener("change", (e) => setfpsged(e.target.checked));
  document
    .getElementById("jit")
    ?.addEventListener("change", (e) => setjit(e.target.checked));
  document
    .getElementById("disableai")
    ?.addEventListener("change", (e) => setAI(e.target.checked));
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
    .getElementById("applyinperf")
    ?.addEventListener("change", (e) => setunderscale(e.target.checked));
  document
    .getElementById("SFL")
    ?.addEventListener("change", (e) => setSFL(e.target.checked));
  document
    .getElementById("DThermal")
    ?.addEventListener("change", (e) => setDThermal(e.target.checked));
  document
    .getElementById("LiteMode")
    ?.addEventListener("change", (e) => setLiteModeStatus(e.target.checked));
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
    .getElementById("cpuGovernor")
    ?.addEventListener("change", (e) => setDefaultCpuGovernor(e.target.value));
  document
    .getElementById("GovernorPowersave")
    ?.addEventListener("change", (e) => setGovernorPowersave(e.target.value));
  document
    .getElementById("ioSchedulerBalanced")
    ?.addEventListener("change", (e) => setIObalance(e.target.value));
  document
    .getElementById("ioSchedulerPerformance")
    ?.addEventListener("change", (e) => setIOperformance(e.target.value));
  document
    .getElementById("ioSchedulerPowersave")
    ?.addEventListener("change", (e) => setIOpowersave(e.target.value));
  document
    .getElementById("cpuFreq")
    ?.addEventListener("change", (e) => setCpuFreqOffsets(e.target.value));
  document
    .getElementById("disablevsync")
    ?.addEventListener("change", (e) => setVsyncValue(e.target.value));

  // Additional Settings
  document
    .getElementById("show-additional-settings")
    ?.addEventListener("click", showAdditionalSettings);
  document
    .getElementById("close-additional")
    ?.addEventListener("click", hideAdditionalSettings);

  // Preference Settings
  document
    .getElementById("show-preference-settings")
    ?.addEventListener("click", showPreferenceSettings);
  document
    .getElementById("close-preference")
    ?.addEventListener("click", hidePreferenceSettings);

  // Custom Resolution Settings
  document
    .getElementById("customreso")
    ?.addEventListener("click", showCustomResolution);
  document.getElementById("applyreso")?.addEventListener("click", async () => {
    let w = document.getElementById("reso-width").value;
    let h = document.getElementById("reso-height").value;
    await setResolution(w, h);
    hideCustomResolution();
  });
  document
    .getElementById("resetreso-btn")
    ?.addEventListener("click", resetResolution);
  document
    .getElementById("close-reso")
    ?.addEventListener("click", hideResoSettings);

  // Color scheme buttons
  document
    .getElementById("colorschemebutton")
    ?.addEventListener("click", showColorScheme);
  document
    .getElementById("applybutton")
    ?.addEventListener("click", hidecolorscheme);
  document
    .getElementById("close-scheme")
    ?.addEventListener("click", hideSchemeSettings);

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
  document
    .getElementById("close-gamelist")
    ?.addEventListener("click", hideGamelistSettings);    
};

let loopIntervals = [];
let loopsActive = false;
let heavyInitDone = false;
let heavyInitTimeouts = [];
let cleaningInterval = null;

const schedule = (fn, delay = 0) => {
  const id = setTimeout(() => {
    try {
      fn();
    } finally {
      heavyInitTimeouts = heavyInitTimeouts.filter((t) => t !== id);
    }
  }, delay);
  heavyInitTimeouts.push(id);
};

const cancelAllTimeouts = () => {
  for (const t of heavyInitTimeouts) clearTimeout(t);
  heavyInitTimeouts = [];
};

const cleanMemory = () => {
  if (typeof globalThis.gc === "function") globalThis.gc();
};

const startMonitoringLoops = () => {
  if (loopsActive) return;
  loopsActive = true;

  loopIntervals.push(setInterval(() => checkCPUFrequencies(), 5000));
  loopIntervals.push(
    setInterval(() => {
      checkServiceStatus();
      checkProfile();
    }, 9000)
  );
  loopIntervals.push(setInterval(() => checkAvailableRAM(), 8000));
  loopIntervals.push(setInterval(() => showRandomMessage(), 20000));
};

const stopMonitoringLoops = () => {
  loopsActive = false;
  loopIntervals.forEach(clearInterval);
  loopIntervals = [];
};

const observeVisibility = () => {
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
      stopMonitoringLoops();
      cancelAllTimeouts();
      clearInterval(cleaningInterval);
    } else {
      startMonitoringLoops();
    }
  });
};

const heavyInit = () => {
  if (heavyInitDone) return;
  heavyInitDone = true;

  cancelAllTimeouts();
  if (cleaningInterval) clearInterval(cleaningInterval);

  const loader = document.getElementById("loading-screen");

  Promise.allSettled([
    showRandomMessage(),
    checkProfile(),
    checkServiceStatus(),
    checkCPUFrequencies(),
    checkAvailableRAM(),
    checkGPreload(),
    loadColorSchemeSettings(),
  ]).then(() => {    
    startMonitoringLoops();
    observeVisibility();
  });

  const quickChecks = [
    checkModuleVersion,
    checkCPUInfo,
    checkKernelVersion,
    getAndroidVersion,
    loadCpuGovernors,
    loadCpuFreq,
    loadIObalance,
    loadIOperformance,
    loadIOpowersave,
    GovernorPowersave,
  ];

  const batchSize = 3;
  for (let i = 0; i < quickChecks.length; i += batchSize) {
    const batch = quickChecks.slice(i, i + batchSize);
    schedule(() => batch.forEach((fn) => fn()), 600 + i * 300);
  }

  const heavyChecks = [
    checkunderscale,
    checkResolution,
    checkfpsged,
    checkLiteModeStatus,
    checkDThermal,
    checkiosched,
    checkmalisched,
    checkAI,
    checkDND,
    checkdtrace,
    checkjit,
    checktoast,
    loadVsyncValue,
    checkBypassChargeStatus,
    checkschedtunes,
    checkSFL,
    checkKillLog,
    checklogger,
    checkRamBoost,
  ];
  if (loader) loader.classList.add("hidden");
  const step = 300;
  heavyChecks.forEach((fn, i) => {
    schedule(fn, 1600 + i * step);
  });

  cleaningInterval = setInterval(cleanMemory, 15000);
};

const currentColor = {
  red: 1e3,
  green: 1e3,
  blue: 1e3,
  saturation: 1e3,
};

const debounce = (fn, delay = 200) => {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
};

const updateColorState = ({ red, green, blue, saturation }) => {
  if (
    red !== currentColor.red ||
    green !== currentColor.green ||
    blue !== currentColor.blue ||
    saturation !== currentColor.saturation
  ) {
    currentColor.red = red;
    currentColor.green = green;
    currentColor.blue = blue;
    currentColor.saturation = saturation;

    saveDisplaySettings(red, green, blue, saturation);
    setRGB(red, green, blue);
    setSaturation(saturation);
  }
};
const loadColorSchemeSettings = async () => {
  const c = document.getElementById("red"),
    s = document.getElementById("green"),
    r = document.getElementById("blue"),
    d = document.getElementById("saturation"),
    l = document.getElementById("reset-btn"),
    cv = document.getElementById("red-value"),
    sv = document.getElementById("green-value"),
    rv = document.getElementById("blue-value"),
    dv = document.getElementById("saturation-value"),
    m = await loadDisplaySettings();

  c.value = m.red;
  s.value = m.green;
  r.value = m.blue;
  d.value = m.saturation;
  cv.value = m.red;
  sv.value = m.green;
  rv.value = m.blue;
  dv.value = m.saturation;

  currentColor = m;
  await setRGB(m.red, m.green, m.blue);
  await setSaturation(m.saturation);

  const handleInputChange = debounce(() => {
    cv.value = c.value;
    sv.value = s.value;
    rv.value = r.value;
    dv.value = d.value;

    updateColorState({
      red: Number(c.value),
      green: Number(s.value),
      blue: Number(r.value),
      saturation: Number(d.value),
    });
  }, 100);

  [c, s, r, d].forEach((el) => el.addEventListener("input", handleInputChange));

  const bindInput = (numberInput, slider, min, max) => {
    numberInput.addEventListener("input", () => {
      if (numberInput.value === "") return;
      slider.value = numberInput.value;
      handleInputChange();
    });

    const finalize = () => {
      if (numberInput.value === "") {
        numberInput.value = slider.value;
      }
      let v = Number(numberInput.value);
      if (isNaN(v)) v = min;
      if (v < min) v = min;
      if (v > max) v = max;
      numberInput.value = v;
      slider.value = v;
      handleInputChange();
    };

    numberInput.addEventListener("blur", finalize);
    numberInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") numberInput.blur();
    });
  };

  bindInput(cv, c, 0, 1000);
  bindInput(sv, s, 0, 1000);
  bindInput(rv, r, 0, 1000);
  bindInput(dv, d, 0, 2000);

  l.addEventListener("click", async () => {
    c.value = s.value = r.value = d.value = 1000;
    cv.value = sv.value = rv.value = dv.value = 1000;

    await setRGB(1000, 1000, 1000);
    await setSaturation(1000);
    updateColorState({ red: 1000, green: 1000, blue: 1000, saturation: 1000 });
    saveDisplaySettings(1000, 1000, 1000, 1000);
    showToast("Display settings reset!");
  });
};

// event Listeners
setupUIListeners();
heavyInit();