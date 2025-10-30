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

import BannerDarkZenith from "/webui.bannerdarkmode.avif";
import BannerLightZenith from "/webui.bannerlightmode.avif";
import AvatarZenith from "/webui.avatar.avif";
import SchemeBanner from "/webui.schemebanner.avif";
import ResoBanner from "/webui.reso.avif";
import { exec, toast } from "kernelsu";
const moduleInterface = window.$azenith;
const fileInterface = window.$FILE;
const RESO_PROP = "persist.sys.azenithconf.resosettings";

const executeCommand = async (cmd, cwd = null) => {
  try {
    const { errno, stdout, stderr } = await exec(cmd, cwd ? { cwd } : {});
    return { errno, stdout, stderr };
  } catch (e) {
    return { errno: -1, stdout: "", stderr: e.message || String(e) };
  }
};

window.executeCommand = executeCommand;

let lastMessageTime = 0;

const showRandomMessage = () => {
  const now = Date.now();
  if (now - lastMessageTime < 10000) return; // 10s cooldown
  lastMessageTime = now;

  const c = document.getElementById("msg");
  if (!c) return;

  // Random number from 0 to 29
  const randomIndex = Math.floor(Math.random() * 30);

  // Get translation for that specific key
  const message = getTranslation(`randomMessages.${randomIndex}`);
  c.textContent = message;
};

let lastModuleVersion = { time: 0, value: "" };
const checkModuleVersion = async () => {
  const now = Date.now();
  if (now - lastModuleVersion.time < 30000) return;

  try {
    const { errno: c, stdout: s } = await executeCommand(
      "echo 'Version :' && grep \"version=\" /data/adb/modules/AZenith/module.prop | awk -F'=' '{print $2}'"
    );
    if (c === 0) {
      lastModuleVersion = { time: now, value: s.trim() };
      const elem = document.getElementById("moduleVer");
      if (elem) elem.textContent = lastModuleVersion.value;
    }
  } catch {}
};

let lastProfile = { time: 0, value: "" };
const checkProfile = async () => {
  const now = Date.now();
  if (now - lastProfile.time < 5000) return;

  try {
    const { errno: c, stdout: s } = await executeCommand(
      "cat /data/adb/.config/AZenith/API/current_profile"
    );

    if (c !== 0) return;
    const r = s.trim();
    const d = document.getElementById("CurProfile");
    if (!d) return;

    let l =
      { 0: "Initializing...", 1: "Performance", 2: "Balanced", 3: "ECO Mode" }[
        r
      ] || "Unknown";

    // Check for Lite mode
    const { errno: c2, stdout: s2 } = await executeCommand(
      "getprop persist.sys.azenithconf.cpulimit"
    );
    if (c2 === 0 && s2.trim() === "1") l += " (Lite)";

    if (lastProfile.value === l) return;
    lastProfile = { time: now, value: l };

    d.textContent = l;

    // Detect theme mode
    const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;

    // Dark mode colors (original) vs light mode (darker/saturated)
    const colors = isDark
      ? {
          Performance: "#ef4444",
          Balanced: "#7dd3fc",
          "ECO Mode": "#5eead4",
          "Initializing...": "#60a5fa",
          Default: "#ffffff",
        }
      : {
          Performance: "#b91c1c",
          Balanced: "#0284c7",
          "ECO Mode": "#0d9488",
          "Initializing...": "#2563eb",
          Default: "#1f2937",
        };

    const key = l.replace(" (Lite)", "");
    d.style.color = colors[key] || colors.Default;
  } catch (m) {
    console.error("checkProfile error:", m);
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
    const { errno: s, stdout: r } = await executeCommand(
      "getprop ro.soc.model"
    );
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

let lastServiceCheck = { time: 0, status: "", pid: "" };

const checkServiceStatus = async () => {
  const now = Date.now();
  if (now - lastServiceCheck.time < 5000) return; // 5s cooldown
  lastServiceCheck.time = now;

  const r = document.getElementById("serviceStatus");
  const d = document.getElementById("servicePID");
  if (!r || !d) return;

  try {
    // Get PID
    const { errno: pidErr, stdout: pidOut } = await executeCommand(
      "/system/bin/toybox pidof sys.azenith-service"
    );

    let status = "";
    let pidText = getTranslation("serviceStatus.servicePID", "null");

    if (pidErr === 0 && pidOut.trim() !== "0") {
      const pid = pidOut.trim();
      pidText = getTranslation("serviceStatus.servicePID", pid);
      d.textContent = pidText; // show PID immediately

      // Run profile & AI queries in parallel
      const [profileRawResult, aiRawResult] = await Promise.all([
        executeCommand("cat /data/adb/.config/AZenith/API/current_profile"),
        executeCommand("getprop persist.sys.azenithconf.AIenabled")
      ]);

      const profile = profileRawResult.stdout?.trim() || "";
      const ai = aiRawResult.stdout?.trim() || "";

      // Determine status
      if (profile === "0") {
        status = getTranslation("serviceStatus.initializing");
      } else if (["1", "2", "3"].includes(profile)) {
        status =
          ai === "1"
            ? getTranslation("serviceStatus.runningAuto")
            : ai === "0"
            ? getTranslation("serviceStatus.runningIdle")
            : getTranslation("serviceStatus.unknownProfile");
      } else {
        status = getTranslation("serviceStatus.unknownProfile");
      }
    } else {
      status = getTranslation("serviceStatus.suspended");
      d.textContent = pidText; // show null PID
    }

    // Update status only if changed
    if (lastServiceCheck.status !== status) r.textContent = status;

    lastServiceCheck.status = status;
    lastServiceCheck.pid = pidText;

  } catch (e) {
    console.warn("checkServiceStatus error:", e);
  }
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
  const fstrimToast = getTranslation("toast.fstrim");
  toast(fstrimToast);
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
    const showCantAccessToast = getTranslation("toast.showcantaccess");
    toast(showCantAccessToast);
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
    const savedPackagesToast = getTranslation("toast.savedPackages", editedLines.length);
    toast(savedPackagesToast);
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
  const savedPackagesToast = getTranslation("toast.savedPackages", mergedLines.length);
  toast(savedPackagesToast);
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
      const cantRestartToast = getTranslation("toast.cantRestart");
      toast(cantRestartToast);
      return;
    }

    let { stdout: pid } = await executeCommand(
      "/system/bin/toybox pidof sys.azenith-service"
    );
    if (!pid || pid.trim() === "") {
      const serviceDeadToast = getTranslation("toast.serviceDead");
      toast(serviceDeadToast);
      return;
    }

    const restartingDaemonToast = getTranslation("toast.restartingDaemon");
    toast(restartingDaemonToast);

    await executeCommand(
      "setprop persist.sys.azenith.state stopped && pkill -9 -f sys.azenith-service; su -c '/data/adb/modules/AZenith/system/bin/sys.azenith-service > /dev/null 2>&1 & disown'"
    );

    await checkServiceStatus();
  } catch (r) {
    const restartFailedToast = getTranslation("toast.restartFailed");
    toast(restartFailedToast);
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
  const c = document.getElementById("schemeModal");
  if (!c) return; // Modal element not found

  const s = c.querySelector(".scheme-container");
  if (!s) return; // Modal content not found

  document.body.classList.add("modal-open");
  c.classList.add("show");

  const originalHeight = window.innerHeight;

  const resizeHandler = () => {
    if (!s) return;
    window.innerHeight < originalHeight - 150
      ? (s.style.transform = "translateY(-10%) scale(1)")
      : (s.style.transform = "translateY(0) scale(1)");
  };

  window.addEventListener("resize", resizeHandler, { passive: true });
  c._resizeHandler = resizeHandler;

  resizeHandler();
};

const hidecolorscheme = () => {
  const c = document.getElementById("schemeModal");
  if (!c) return; // exit if modal not found

  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  const colorSchemeSavedToast = getTranslation("toast.colorSchemeSaved");
  toast(colorSchemeSavedToast);

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
      const invalidColorSchemeToast = getTranslation("toast.invalidColorScheme");
      toast(invalidColorSchemeToast);
      return { red: 1000, green: 1000, blue: 1000, saturation: 1000 };
    }

    return { red: s, green: r, blue: d, saturation: l };
  } catch (m) {
    console.log("Error reading display settings:", m);
    const colorSchemeNotFoundToast = getTranslation("toast.colorSchemeNotFound");
    toast(colorSchemeNotFoundToast);
    return { red: 1000, green: 1000, blue: 1000, saturation: 1000 };
  }
};

const setRGB = async (c, s, r) => {
  await executeCommand(
    `service call SurfaceFlinger 1015 i32 1 f ${c / 1000} f 0 f 0 f 0 f 0 f ${
      s / 1000
    } f 0 f 0 f 0 f 0 f ${r / 1000} f 0 f 0 f 0 f 0 f 1`
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
  const displayResetToast = getTranslation("toast.displayReset");
  toast(displayResetToast);
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
    const alreadyPerformanceToast = getTranslation("toast.alreadyPerformance");
    toast(alreadyPerformanceToast);
    return;
  }
  executeCommand("su -c sys.azenith-profiler 1 >/dev/null 2>&1 &");
};

const applybalancedprofile = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("2" === c.trim()) {
    const alreadyBalancedToast = getTranslation("toast.alreadyBalanced");
    toast(alreadyBalancedToast);
    return;
  }
  executeCommand("su -c sys.azenith-profiler 2 >/dev/null 2>&1 &");
};

const applyecomode = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if ("3" === c.trim()) {
    const alreadyECOToast = getTranslation("toast.alreadyECO");
    toast(alreadyECOToast);
    return;
  }
  executeCommand("su -c sys.azenith-profiler 3 >/dev/null 2>&1 &");
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

const setIObalance = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(
    `setprop persist.sys.azenith.custom_default_balanced_IO ${c}`
  );
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "2" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO ${c}`
    ));
};

const loadIObalance = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler"
  );
  if (c === 0) {
    let schedulers = s.trim().split(/\s+/).map(sch => sch.replace(/[\[\]]/g, ""));
    let select = document.getElementById("ioSchedulerBalanced");

    select.innerHTML = "";

    schedulers.forEach(sch => {
      let opt = document.createElement("option");
      opt.value = sch;
      opt.textContent = sch;
      select.appendChild(opt);
    });

    let { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ] && getprop persist.sys.azenith.custom_default_balanced_IO || getprop persist.sys.azenith.default_balanced_IO'`
    );

    if (l === 0) {
      let current = m.trim().replace(/[\[\]]/g, "");
      select.value = current;
    }
  }
};

const setIOperformance = async (c) => {
  let s = "/data/adb/.config/AZenith",
    r = `${s}/API/current_profile`;
  await executeCommand(
    `setprop persist.sys.azenith.custom_performance_IO ${c}`
  );
  let { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  0 === d &&
    "1" === l.trim() &&
    (await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO ${c}`
    ));
};

const loadIOperformance = async () => {
  const { errno: c, stdout: s } = await executeCommand(
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler"
  );
  if (c === 0) {
    const schedulers = s.trim().split(/\s+/).map(x => x.replace(/[\[\]]/g, ""));
    const select = document.getElementById("ioSchedulerPerformance");
    select.innerHTML = "";

    schedulers.forEach(sch => {
      const opt = document.createElement("option");
      opt.value = sch;
      opt.textContent = sch;
      select.appendChild(opt);
    });

    const { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_performance_IO)" ] && getprop persist.sys.azenith.custom_performance_IO'`
    );

    if (l === 0) select.value = m.trim().replace(/[\[\]]/g, "");
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
    "chmod 644 /sys/block/sda/queue/scheduler && cat /sys/block/sda/queue/scheduler"
  );
  if (c === 0) {
    const schedulers = s.trim().split(/\s+/).map(x => x.replace(/[\[\]]/g, ""));
    const select = document.getElementById("ioSchedulerPowersave");
    select.innerHTML = "";

    schedulers.forEach(sch => {
      const opt = document.createElement("option");
      opt.value = sch;
      opt.textContent = sch;
      select.appendChild(opt);
    });

    const { errno: l, stdout: m } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenith.custom_powersave_IO)" ] && getprop persist.sys.azenith.custom_powersave_IO'`
    );

    if (l === 0) select.value = m.trim().replace(/[\[\]]/g, "");
  }
};

const showAdditionalSettings = async () => {
  const c = document.getElementById("additional-modal"),
    s = c.querySelector(".additional-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
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
    s.style.transform =
      window.innerHeight < r - 150
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
  const c = document.getElementById("gamelistModal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const hideSchemeSettings = () => {
  const c = document.getElementById("schemeModal");
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
    const logSavedMsg = getTranslation("toast.logSaved");
    toast(logSavedMsg);
  } catch (e) {
    const logSaveFailedMsg = getTranslation("toast.logSaveFailed");
    toast(logSaveFailedMsg);
    console.error("saveLog error:", e);
  }
};

const currentColor = {
  red: 1000,
  green: 1000,
  blue: 1000,
  saturation: 1000,
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

  // Update slider and number inputs
  [c, s, r, d].forEach((el, i) => {
    switch (i) {
      case 0:
        el.value = m.red;
        cv.value = m.red;
        break;
      case 1:
        el.value = m.green;
        sv.value = m.green;
        break;
      case 2:
        el.value = m.blue;
        rv.value = m.blue;
        break;
      case 3:
        el.value = m.saturation;
        dv.value = m.saturation;
        break;
    }
  });

  currentColor.red = m.red;
  currentColor.green = m.green;
  currentColor.blue = m.blue;
  currentColor.saturation = m.saturation;

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
      if (numberInput.value === "") numberInput.value = slider.value;
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

    currentColor.red = 1000;
    currentColor.green = 1000;
    currentColor.blue = 1000;
    currentColor.saturation = 1000;

    saveDisplaySettings(1000, 1000, 1000, 1000);
    const displayResetMsg = getTranslation("toast.displayReset");
    toast(displayResetMsg);
  });
};

const detectResolution = async () => {
  const { errno, stdout } = await executeCommand(
    `wm size | grep -oE "[0-9]+x[0-9]+" | head -n 1`
  );
  if (errno !== 0 || !stdout.trim()) {
    console.error("Failed to detect resolution");
    const unableDetectResMsg = getTranslation("toast.unableDetectResolution");
    toast(unableDetectResMsg);
    return;
  }

  const defaultRes = stdout.trim();
  const [width, height] = defaultRes.split("x").map(Number);
  if (!width || !height) return;

  const mediumRes = `${Math.round(width * 0.9)}x${Math.round(height * 0.9)}`;
  const lowRes = `${Math.round(width * 0.8)}x${Math.round(height * 0.8)}`;

  const resoSizes = document.querySelectorAll(".reso-size");
  if (resoSizes.length === 3) {
    resoSizes[0].textContent = lowRes;
    resoSizes[1].textContent = mediumRes;
    resoSizes[2].textContent = defaultRes;
  }

  window._reso = {
    default: defaultRes,
    medium: mediumRes,
    low: lowRes,
    selected: defaultRes,
  };

  const { stdout: saved } = await executeCommand(`getprop ${RESO_PROP}`);
  const savedRes = saved.trim();

  if (savedRes) {
    const buttons = document.querySelectorAll(".reso-option");
    buttons.forEach((btn) => {
      const text = btn.querySelector(".reso-size")?.textContent;
      if (text === savedRes) btn.classList.add("active");
      else btn.classList.remove("active");
    });
    if (window._reso) window._reso.selected = savedRes;
  } else {
    document.querySelectorAll(".reso-option")[2]?.classList.add("active");
  }
};

const selectResolution = async (btn) => {
  document
    .querySelectorAll(".reso-option")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");

  const selectedText = btn.querySelector(".reso-size")?.textContent;
  if (!selectedText) return;

  if (window._reso) window._reso.selected = selectedText;
};

const applyResolution = async () => {
  if (!window._reso || !window._reso.selected) {
    const noResolutionSelected = getTranslation("toast.noResolutionSelected");
    toast(noResolutionSelected);
    return;
  }

  const selected = window._reso.selected;
  const def = window._reso.default;

  if (selected === def) {
    await executeCommand(`setprop ${RESO_PROP} ${selected}`);
    await executeCommand("wm size reset");
  } else {
    await executeCommand(`setprop ${RESO_PROP} ${selected}`);
    await executeCommand(`wm size ${selected}`);
  }
};

const showCustomResolution = async () => {
  const c = document.getElementById("resomodal");
  if (!c) return; // exit if modal not found

  const s = c.querySelector(".reso-container");
  if (!s) return;

  document.body.classList.add("modal-open");
  c.classList.add("show");

  await detectResolution();

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

const hideResoSettings = () => {
  const c = document.getElementById("resomodal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const showSettings = async () => {
  const c = document.getElementById("settingsModal"),
    s = c.querySelector(".settings-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
  d();
};

const hideSettings = () => {
  const c = document.getElementById("settingsModal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

  const c = document.getElementById("disableai");
  const s = document.getElementById("profile-button");

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

const showProfilerSettings = async () => {
  const c = document.getElementById("profilermodal"),
    s = c.querySelector(".profiler-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
  d();
};

const hideProfilerSettings = () => {
  const c = document.getElementById("profilermodal");
  c.classList.remove("show");
  document.body.classList.remove("modal-open");
  if (c._resizeHandler) {
    window.removeEventListener("resize", c._resizeHandler);
    delete c._resizeHandler;
  }
};

const setupUIListeners = () => {
  const banner = document.getElementById("Banner");
  const avatar = document.getElementById("Avatar");
  const scheme = document.getElementById("Scheme");
  const reso = document.getElementById("Reso");

  if (avatar) avatar.src = AvatarZenith;
  if (scheme) scheme.src = SchemeBanner;
  if (reso) reso.src = ResoBanner;

  const updateBannerByTheme = () => {
    const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    if (banner) banner.src = isDark ? BannerDarkZenith : BannerLightZenith;
  };

  updateBannerByTheme();  

  // Listen for system theme changes
  window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", updateBannerByTheme);

  // Button Clicks
  document
    .getElementById("startButton")
    ?.addEventListener("click", startService);
    document
    .getElementById("applyperformance")
    ?.addEventListener("click", applyperformanceprofile);
  document
    .getElementById("applybalanced")
    ?.addEventListener("click", applybalancedprofile);
  document
    .getElementById("applypowersave")
    ?.addEventListener("click", applyecomode);
  document.getElementById("savelogButton")?.addEventListener("click", savelog);
  document.getElementById("FSTrim")?.addEventListener("click", applyFSTRIM);

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

  // Open settings
  document
    .getElementById("settingsButton")
    ?.addEventListener("click", showSettings);
  document
    .getElementById("close-settings")
    ?.addEventListener("click", hideSettings);
  document.getElementById("disableai").addEventListener("change", function () {
  setAI(this.checked),
    document
      .getElementById("profile-button")
      .classList.toggle("show", this.checked);
  });
    
  // Profile Settings
  document
    .getElementById("profile-button")
    ?.addEventListener("click", showProfilerSettings);
  document
    .getElementById("close-profiler")
    ?.addEventListener("click", hideProfilerSettings);
    
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
    await applyResolution();
    hideResoSettings();
  });
  document
    .getElementById("resetreso-btn")
    ?.addEventListener("click", hideResoSettings);
  document
    .getElementById("close-reso")
    ?.addEventListener("click", hideResoSettings);

  // Selectable resolutions
  document.querySelectorAll(".reso-option")?.forEach((btn) => {
    btn.addEventListener("click", () => selectResolution(btn));
  });

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

let loopsActive = false;
let loopTimeout = null;
let heavyInitDone = false;
let cleaningInterval = null;
let heavyInitTimeouts = [];

const cancelAllTimeouts = () => {
  heavyInitTimeouts.forEach(clearTimeout);
  heavyInitTimeouts = [];
};

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

const cleanMemory = () => {
  if (typeof globalThis.gc === "function") globalThis.gc();
};

const monitoredTasks = [
  { fn: checkServiceStatus, interval: 5000 },
  { fn: checkProfile, interval: 5000 },
  { fn: showRandomMessage, interval: 10000 },
];

const runMonitoredTasks = async () => {
  if (!loopsActive) return;
  const now = Date.now();
  if (!runMonitoredTasks.lastRun) runMonitoredTasks.lastRun = {};

  for (const task of monitoredTasks) {
    const last = runMonitoredTasks.lastRun[task.fn.name] || 0;
    if (now - last >= task.interval) {
      try {
        await task.fn();
      } catch (e) {
        console.warn(`Task ${task.fn.name} failed:`, e);
      }
      runMonitoredTasks.lastRun[task.fn.name] = Date.now();
    }
  }

  loopTimeout = setTimeout(runMonitoredTasks, 1000);
};

const startMonitoringLoops = () => {
  if (loopsActive) return;
  loopsActive = true;
  runMonitoredTasks();
};

const stopMonitoringLoops = () => {
  loopsActive = false;
  if (loopTimeout) clearTimeout(loopTimeout);
};

const observeVisibility = () => {
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
      stopMonitoringLoops();
      cancelAllTimeouts();
      if (cleaningInterval) clearInterval(cleaningInterval);
    } else {
      startMonitoringLoops();
    }
  });
};

const heavyInit = async () => {
  if (heavyInitDone) return;
  heavyInitDone = true;

  cancelAllTimeouts();
  if (cleaningInterval) clearInterval(cleaningInterval);

  const loader = document.getElementById("loading-screen");
  if (loader) loader.classList.remove("hidden");
  document.body.classList.add("no-scroll");

  const stage1 = [showRandomMessage, checkProfile, checkServiceStatus];
  await Promise.all(stage1.map((fn) => fn()));

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
  await Promise.all(quickChecks.map((fn) => fn()));

  const heavyAsync = [
    checkfpsged,
    checkLiteModeStatus,
    checkDThermal,
    checkiosched,
    checkGPreload,
    loadColorSchemeSettings,
  ];
  await Promise.all(heavyAsync.map((fn) => fn()));

  const heavySequential = [
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
    detectResolution,
  ];
  for (const fn of heavySequential) {
    await fn();
  }

  startMonitoringLoops();
  observeVisibility();

  if (loader) loader.classList.add("hidden");
  document.body.classList.remove("no-scroll");

  cleaningInterval = setInterval(cleanMemory, 15000);
};

// Event Listeners
setupUIListeners();
heavyInit();
