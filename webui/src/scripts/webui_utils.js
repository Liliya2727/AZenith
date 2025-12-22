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
import { wrapInputStream, PackageManagerInterface } from "webuix";
const WEBUI_VERSION = ".placeholder";
const moduleInterface = window.$AZenith;
const fileInterface = window.$AZFile;
const GAMELIST_PATH = "/data/adb/.config/AZenith/gamelist/azenithApplist.json";
const RESO_PROP = "persist.sys.azenithconf.resosettings";
const DEVICE_PROP = "sys.azenith.device";
const DEVICE_PROPS = [
  "ro.product.vendor_dlkm.device",
  "ro.product.vendor.device",
  "ro.product.system.device",
  "ro.product.odm.device",
  "ro.product.product.device",
  "ro.product.board",
  "ro.product.device",
  "ro.product.model",
  "ro.product.vendor.model",
  "ro.product.system.model"
];
const PERAPP_SETTINGS = {
  perf_lite_mode: "default",
  dnd_on_gaming: "default",
  app_priority: "default",
  game_preload: "default",
  refresh_rate: "default",
  renderer: "default",
};

const PERAPP_SCHEMA = [
  { key: "perf_lite_mode", label: "Performance Lite Mode", type: "tri" },
  { key: "dnd_on_gaming", label: "Do Not Disturb", type: "tri" },
  { key: "app_priority", label: "App Priority", type: "tri" },
  { key: "game_preload", label: "Game Preload", type: "tri" },
  {
    key: "renderer",
    label: "Renderer",
    type: "enum",
    options: ["default", "skiagl", "vulkan"]
  },
  {
    key: "refresh_rate",
    label: "Refresh Rate",
    type: "enum-dynamic"
  }
];
let lastGameCheck = { time: 0, status: "" };
let lastProfile = { time: 0, value: "" };
let lastServiceCheck = { time: 0, status: "", pid: "" };
let pressTimer = null;
let appListLoaded = false;
let cachedSOCData = null;
let cachedDeviceData = null;
let loopsActive = false;
let loopTimeout = null;
let heavyInitDone = false;
let cleaningInterval = null;
let heavyInitTimeouts = [];
let profilerPathCache = null;
let currentScreen = "main"; 

// execute Command functions
const executeCommand = async (cmd, cwd = null) => {
  try {
    const { errno, stdout, stderr } = await exec(cmd, cwd ? { cwd } : {});
    return { errno, stdout, stderr };
  } catch (e) {
    return { errno: -1, stdout: "", stderr: e.message || String(e) };
  }
};
window.executeCommand = executeCommand;

// Main Script
const setGPUMaliDefault = async (c) => {
  const s = "/data/adb/.config/AZenith",
        r = `${s}/API/current_profile`;

  await executeCommand(
    `setprop persist.sys.azenith.custom_default_gpumali_gov ${c}`
  );

  const { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  if (d === 0 && l.trim() === "2") {
    await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsGPUMali ${c}`
    );
  }
};

const loadGPUMaliDefault = async () => {
  const { errno: c, stdout: s } = await executeCommand(
    "sh -c 'ls /sys/devices/platform/soc/*.mali/devfreq/*.mali/available_governors 2>/dev/null | head -n 1'"
  );
  if (c !== 0 || !s.trim()) return;

  const path = s.trim();

  const { errno: r, stdout: g } = await executeCommand(
    `chmod 644 ${path} && cat ${path}`
  );
  if (r !== 0) return;

  const select = document.getElementById("gpuMaliDefault");
  if (!select) return;

  select.innerHTML = "";
  g.trim().split(/\s+/).forEach((gov) => {
    const opt = document.createElement("option");
    opt.value = gov;
    opt.textContent = gov;
    select.appendChild(opt);
  });

  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_default_gpumali_gov)" ] && getprop persist.sys.azenith.custom_default_gpumali_gov || getprop persist.sys.azenith.default_gpumali_gov'`
  );
  if (l === 0 && m.trim()) select.value = m.trim();
};

const setGPUMaliPowersave = async (c) => {
  const s = "/data/adb/.config/AZenith",
        r = `${s}/API/current_profile`;

  await executeCommand(
    `setprop persist.sys.azenith.custom_powersave_gpumali_gov ${c}`
  );

  const { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  if (d === 0 && l.trim() === "3") {
    await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsGPUMali ${c}`
    );
  }
};

const loadGPUMaliPowersave = async () => {
  const { errno: c, stdout: s } = await executeCommand(
    "sh -c 'ls /sys/devices/platform/soc/*.mali/devfreq/*.mali/available_governors 2>/dev/null | head -n 1'"
  );
  if (c !== 0 || !s.trim()) return;

  const path = s.trim();

  const { errno: r, stdout: g } = await executeCommand(
    `chmod 644 ${path} && cat ${path}`
  );
  if (r !== 0) return;

  const select = document.getElementById("gpuMaliPowersave");
  if (!select) return;

  select.innerHTML = "";
  g.trim().split(/\s+/).forEach((gov) => {
    const opt = document.createElement("option");
    opt.value = gov;
    opt.textContent = gov;
    select.appendChild(opt);
  });

  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_powersave_gpumali_gov)" ] && getprop persist.sys.azenith.custom_powersave_gpumali_gov'`
  );
  if (l === 0 && m.trim()) select.value = m.trim();
};

const setGPUMaliPerformance = async (c) => {
  const s = "/data/adb/.config/AZenith",
        r = `${s}/API/current_profile`;

  await executeCommand(
    `setprop persist.sys.azenith.custom_performance_gpumali_gov ${c}`
  );

  const { errno: d, stdout: l } = await executeCommand(`cat ${r}`);
  if (d === 0 && l.trim() === "1") {
    await executeCommand(
      `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsGPUMali ${c}`
    );
  }
};

const loadGPUMaliPerformance = async () => {
  const { errno: c, stdout: s } = await executeCommand(
    "sh -c 'ls /sys/devices/platform/soc/*.mali/devfreq/*.mali/available_governors 2>/dev/null | head -n 1'"
  );
  if (c !== 0 || !s.trim()) return;

  const path = s.trim();

  const { errno: r, stdout: g } = await executeCommand(
    `chmod 644 ${path} && cat ${path}`
  );
  if (r !== 0) return;

  const select = document.getElementById("gpuMaliPerformance");
  if (!select) return;

  select.innerHTML = "";
  g.trim().split(/\s+/).forEach((gov) => {
    const opt = document.createElement("option");
    opt.value = gov;
    opt.textContent = gov;
    select.appendChild(opt);
  });

  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_performance_gpumali_gov)" ] && getprop persist.sys.azenith.custom_performance_gpumali_gov'`
  );
  if (l === 0 && m.trim()) select.value = m.trim();
};

const checkGPUMaliCompatibility = async () => {
  const { stdout } = await executeCommand(
    "getprop sys.azenith.maligovsupport"
  );

  const value = stdout.trim();
  const maliCard = document.getElementById("maliCard");
  if (!maliCard) return;

  if (value === "1") {
    maliCard.classList.remove("hidden");
    await loadGPUMaliDefault();
    await loadGPUMaliPowersave();
    await loadGPUMaliPerformance();
  } else {
    maliCard.classList.add("hidden");
  }
};

const checkWebUISupport = async () => {
  const loadingText = document.getElementById("loading-text");
  const loadingScreen = document.getElementById("loading-screen");

  try {
    if (typeof ksu !== "undefined" && typeof ksu.getPackagesInfo === "function") {
      const res = ksu.getPackagesInfo(JSON.stringify(["android"]));
      const list = JSON.parse(res || "[]");

      if (Array.isArray(list) && list.length > 0) {
        return;
      }
    }
  } catch (_) {}

  try {
    if (typeof window.$packageManager !== "undefined") {
      const app = await window.$packageManager.getApplicationInfo("android",0,0);
      if (app) {
        return;
      }
    }
  } catch (_) {}

  if (loadingText) {
    loadingText.textContent =
      "⚠ WebUI is not supported in this app\n\n" +
      "Try using WebuiX apk or MMRL apk";
  }

  if (loadingScreen) {
    loadingScreen.classList.remove("hidden");
  }

  throw new Error("WebUI not supported");
};

const checkServiceRunning = async () => {
  const loadingText = document.getElementById("loading-text");
  const loadingScreen = document.getElementById("loading-screen");

  try {
    const { errno, stdout } = await executeCommand(
      "/system/bin/pidof sys.azenith-service"
    );

    const pid = (stdout || "").trim();

    if (errno !== 0 || !pid) {
      if (loadingText) {
        loadingText.textContent =
          "AZenith daemon is not running.\n\n" +
          "Please reboot your device or Run:\n" +
          "sys.azenith-service --run";
      }

      if (loadingScreen) {
        loadingScreen.classList.remove("hidden");
      }

      throw new Error("AZenith daemon not running");
    }

    return;

  } catch (err) {
    console.error("Service check failed:", err);
    throw err;
  }
};

const checkModuleVersion = async () => {
  try {
    const { errno: c, stdout: s } = await executeCommand(
      "/data/adb/modules/AZenith/system/bin/sys.azenith-service --version"
    );

    if (c !== 0) return;

    const moduleVersion = s.trim();

    if (moduleVersion !== WEBUI_VERSION) {
      const loadingText = document.getElementById("loading-text");
      const loadingScreen = document.getElementById("loading-screen");

      if (loadingText) {
        loadingText.textContent =
          `⚠ Version mismatch detected.\n` +
          `WebUI version doesn't match with\n` +
          `current Daemon version\n` +
          `Please update or reinstall to continue.`;
      }

      if (loadingScreen) {
        loadingScreen.classList.remove("hidden");
      }

      throw new Error("WebUI version mismatch — stopping initialization.");
    }

    const el = document.getElementById("moduleVer");
    if (el) el.textContent = moduleVersion;

  } catch (err) {
    console.error("Failed to check module version:", err);
    throw err;
  }
};

const normalize = s => s.toLowerCase().replace(/[^a-z0-9]+/g, "_");

const fetchDeviceDatabase = async () => {
  if (!cachedDeviceData) {
    try {
      cachedDeviceData = await (await fetch("webui.device.json")).json();
    } catch {
      cachedDeviceData = {};
    }
  }
  return cachedDeviceData;
};

const execProp = async prop => {
  const { stdout } = await executeCommand(`getprop ${prop}`);
  return stdout.trim();
};

const collectProps = async () => {
  const set = new Set();
  for (const prop of DEVICE_PROPS) {
    const v = await execProp(prop);
    if (v) set.add(v);
  }
  const fp = await execProp("ro.build.fingerprint");
  if (fp.includes("/")) set.add(fp.split("/")[2].split(":")[0]);
  return [...set];
};

const findMatch = (props, db) => {
  const normProps = props.map(normalize);
  for (const [friendly, identifier] of Object.entries(db)) {
    const n = normalize(identifier);
    if (normProps.includes(n)) return friendly;
  }
  for (const [friendly, identifier] of Object.entries(db)) {
    const n = normalize(identifier);
    if (normProps.some(p => p.startsWith(n) || n.startsWith(p))) return friendly;
  }
  return props.sort((a, b) => b.length - a.length)[0] || "Unknown Device";
};

const storeProp = async v => {
  await executeCommand(`setprop ${DEVICE_PROP} "${v}"`);
};

const checkDeviceInfo = async () => {
  const saved = await execProp(DEVICE_PROP);
  if (saved && saved !== "") {
    document.getElementById("deviceInfo").textContent = saved;
    return;
  }

  const db = await fetchDeviceDatabase();
  const props = await collectProps();
  const result = findMatch(props, db);

  await storeProp(result);
  document.getElementById("deviceInfo").textContent = result;
};

const updateGameStatus = async () => {
  const now = Date.now();
  if (now - lastGameCheck.time < 1000) return;
  lastGameCheck.time = now;

  const banner = document.getElementById("gameinfo");
  if (!banner) return;

  try {
    const gameRaw = await executeCommand(
      "cat /data/adb/.config/AZenith/API/gameinfo"
    );
    let gameLine = (gameRaw.stdout || "").trim();

    if (
      !gameLine ||
      ["null", "(null)", "NULL"].includes(gameLine.toUpperCase()) ||
      gameLine.toUpperCase().startsWith("NULL 0 0")
    ) {
      gameLine = null;
    }

    const aiResult = await executeCommand(
      "getprop persist.sys.azenithconf.AIenabled"
    );
    const aiEnabled = aiResult.stdout?.trim() === "0";

    let statusText = "";

    if (!gameLine) {
      statusText = aiEnabled
        ? getTranslation("serviceStatus.idleMode")
        : getTranslation("serviceStatus.noApps");
    } else {
      const pkg = gameLine.split(" ")[0]?.trim();

      if (!pkg || ["NULL", "null", "(null)"].includes(pkg.toUpperCase())) {
        statusText = aiEnabled
          ? getTranslation("serviceStatus.idleMode")
          : getTranslation("serviceStatus.noApps");
      } else {
        let label = pkg;

        try {
          const infoList = JSON.parse(ksu.getPackagesInfo(JSON.stringify([pkg])));
          if (Array.isArray(infoList) && infoList.length > 0) {
            label = infoList[0].appLabel || infoList[0].label || infoList[0].appName || pkg;
          }
        } catch (e) {          
          if (typeof window.$packageManager !== "undefined") {
            try {
              const appInfo = await window.$packageManager.getApplicationInfo(pkg, 0, 0);
              if (appInfo) {
                label = (typeof appInfo.getLabel === "function" ? appInfo.getLabel() : appInfo.label)
                        || appInfo.appName
                        || pkg;
              }
            } catch (err) {
              console.warn("Failed to get app label", pkg, err);
              label = pkg;
            }
          }
        }

        statusText = aiEnabled
          ? getTranslation("serviceStatus.activeIdle", label)
          : getTranslation("serviceStatus.active", label);
      }
    }

    if (lastGameCheck.status !== statusText) {
      banner.textContent = statusText;
      lastGameCheck.status = statusText;
    }
  } catch (e) {
    console.warn("updateGameStatus error:", e);
    banner.textContent = getTranslation("serviceStatus.error");
  }
};

const setActiveToolbar = (activeId) => {
  document.querySelectorAll("#bottomToolbar button").forEach(btn => {
    btn.classList.toggle("active", btn.id === activeId);
  });
};

const hidePanel = (panel) => {
  panel.classList.add("showAnim");
  clearTimeout(panel.hideTimer);
  panel.hideTimer = setTimeout(() => {
    panel.classList.add("hidden");
  }, 230);
};

const showPanel = (panel) => {
  clearTimeout(panel.hideTimer);
  panel.classList.remove("hidden");
  void panel.offsetWidth;
  panel.classList.remove("showAnim");
};

const showCards = (container) => {
  const cards = Array.from(container.querySelectorAll(".appCard"));

  cards.forEach(card => {
    clearTimeout(card.hideTimer);
    card.classList.add("hidden");
    card.classList.add("showAnim");
  });

  cards.forEach((card, i) => {
    setTimeout(() => {
      card.classList.remove("hidden");
      requestAnimationFrame(() => card.classList.remove("showAnim"));
    }, i * 40);
  });
};

const hideCards = (container) => {
  const cards = container.querySelectorAll(".appCard");
  cards.forEach(card => {
    card.classList.add("showAnim"); 
    clearTimeout(card.hideTimer);
    card.hideTimer = setTimeout(() => card.classList.add("hidden"), 230);
  });
};

const showGameListMenu = async () => {
  if (currentScreen === "gamelist") return;
  currentScreen = "gamelist";
  loadAppList(); 

  const main = document.getElementById("mainMenu");
  const gameList = document.getElementById("gameListMenu");
  const search = document.getElementById("searchInput");
  const avatar = document.getElementById("Avatar");
  const title = document.getElementById("textJudul");

  hidePanel(main);
  showPanel(gameList);
  showPanel(search);

  title.classList.add("showAnim");
  avatar.classList.add("showAnim");

  showCards(gameList);

  setActiveToolbar("openGameList");

  requestAnimationFrame(() => {    
    gameList.scrollTop = 0;
    document.documentElement.scrollTop = 0;
  });
};

const showMainMenu = async () => {
  if (currentScreen === "main") return;
  currentScreen = "main";

  const main = document.getElementById("mainMenu");
  const gameList = document.getElementById("gameListMenu");
  const search = document.getElementById("searchInput");
  const avatar = document.getElementById("Avatar");
  const title = document.getElementById("textJudul");

  hideCards(gameList);
  hidePanel(gameList);
  hidePanel(search);

  showPanel(main);
  title.classList.remove("showAnim");
  avatar.classList.remove("showAnim");

  setActiveToolbar("openMain");

  requestAnimationFrame(() => {
    main.scrollTop = 0;
    document.documentElement.scrollTop = 0;
  });
};

const readGameList = async () => {
  await executeCommand(`mkdir -p $(dirname ${GAMELIST_PATH})`);
  await executeCommand(`touch ${GAMELIST_PATH}`);
  const { stdout } = await executeCommand(`cat ${GAMELIST_PATH}`);
  if (!stdout.trim()) return {};
  try {
    const data = JSON.parse(stdout);
    Object.keys(data).forEach(pkg => {
      data[pkg] = { ...PERAPP_SETTINGS, ...data[pkg] };
    });
    return data;
  } catch {
    return {};
  }
};

const writeGameList = async (data) => {
  const json = JSON.stringify(data, null, 2).replace(/(["\\])/g, '\\$1');
  await executeCommand(`echo "${json}" > ${GAMELIST_PATH}`);
};

let cachedRefreshRates = null;

const getSupportedRefreshRates = async () => {
  if (cachedRefreshRates) return cachedRefreshRates;

  const rates = new Set();

  try {
    const { stdout } = await executeCommand(`dumpsys display`);
    const regex = /fps=([\d.]+)/g;
    let match;
    while ((match = regex.exec(stdout)) !== null) {
      rates.add(Math.round(parseFloat(match[1])));
    }
  } catch {}

  if (!rates.size) rates.add(60);

  const baseRates = [60, 90, 120, 144];
  const supportedRates = baseRates.filter(v => rates.has(v));

  cachedRefreshRates = ["default", ...supportedRates];
  return cachedRefreshRates;
};

const openPerAppSettings = async (pkg, gamelist) => {
  const modal = document.getElementById("appSettingsModal");
  const list = document.getElementById("appModalSettings");

  document.body.classList.add("modal-open");
  const cfg = gamelist[pkg];
  if (!cfg) return;

  document.getElementById("appModalIcon").src = cachedIconMap[pkg];
  document.getElementById("appModalLabel").textContent = cachedLabelMap[pkg];
  document.getElementById("appModalPkg").textContent = pkg;

  const rows = [];

  for (const s of PERAPP_SCHEMA) {
    const value = cfg[s.key] ?? "default";

    if (s.type === "tri") {
      rows.push(`
        <div class="settingRow">
          <span>${s.label}</span>
          <div class="optionGroup" data-key="${s.key}">
            ${["false","default","true"].map(v => `
              <button class="optionBtn ${value === v ? "active" : ""}" data-value="${v}">
                ${v.charAt(0).toUpperCase() + v.slice(1)}
              </button>
            `).join("")}
          </div>
        </div>
      `);
    }

    if (s.type === "enum") {
      rows.push(`
        <div class="settingRow">
          <span>${s.label}</span>
          <div class="optionGroup" data-key="${s.key}">
            ${s.options.map(v => `
              <button class="optionBtn ${value === v ? "active" : ""}" data-value="${v}">
                ${v === "default" ? "Default" : v}
              </button>
            `).join("")}
          </div>
        </div>
      `);
    }

    if (s.type === "enum-dynamic") {
      const options = await getSupportedRefreshRates();
      rows.push(`
        <div class="settingRow">
          <span>${s.label}</span>
          <div class="optionGroup" data-key="${s.key}">
            ${options.map(v => `
              <button class="optionBtn ${String(value) === String(v) ? "active" : ""}" data-value="${v}">
                ${v === "default" ? "Default" : `${v} Hz`}
              </button>
            `).join("")}
          </div>
        </div>
      `);
    }
  }

  list.innerHTML = rows.join("");

  list.querySelectorAll(".optionGroup").forEach(group => {
    const key = group.dataset.key;
    group.querySelectorAll(".optionBtn").forEach(btn => {
      btn.onclick = async () => {
        group.querySelectorAll(".optionBtn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        cfg[key] = btn.dataset.value;
        await writeGameList(gamelist);
      };
    });
  });

  modal.classList.remove("hidden");
};

let cachedPkgList = [];
let cachedLabelMap = {};
let cachedIconMap = {};

const loadAppList = async () => {
  const container = document.getElementById("appList");
  const searchInput = document.getElementById("searchInput");
  if (!container || !searchInput) return;

  const loader2 = document.getElementById("loading-screen2");
  loader2?.classList.remove("hidden");
  document.body.classList.add("no-scroll");

  try {
    let gamelist = await readGameList();

    if (!appListLoaded) {
      let pkgList = [];
      try {
        pkgList = JSON.parse(ksu.listUserPackages());
      } catch {
        const r = await exec("pm list packages -3");
        pkgList = (r.stdout || "")
          .split("\n")
          .map(x => x.replace("package:", "").trim())
          .filter(Boolean);
      }

      if (!pkgList.length) {
        container.textContent = "No apps found.";
        return;
      }

      const labelMap = {};
      try {
        const infoList = JSON.parse(ksu.getPackagesInfo(JSON.stringify(pkgList)));
        infoList.forEach(i => {
          labelMap[i.packageName] = i.appLabel || i.label || i.packageName;
        });
      } catch {}

      if (window.$packageManager) {
        for (const pkg of pkgList) {
          if (!labelMap[pkg]) {
            try {
              const appInfo = await window.$packageManager.getApplicationInfo(pkg, 0, 0);
              labelMap[pkg] = appInfo?.getLabel?.() || appInfo?.label || appInfo?.appName || pkg;
            } catch {}
          }
        }
      }

      pkgList.forEach(p => labelMap[p] ||= p);

      const iconMap = {};
      pkgList.forEach(p => iconMap[p] = `ksu://icon/${p}`);

      try {
        const icons = JSON.parse(ksu.getPackagesIcons(JSON.stringify(pkgList), 256));
        icons.forEach(i => i.icon && (iconMap[i.packageName] = i.icon));
      } catch {}

      if (window.$packageManager) {
        for (const pkg of pkgList) {
          if (!iconMap[pkg] || iconMap[pkg].startsWith("ksu://icon")) {
            try {
              const iconStream = await window.$packageManager.getApplicationIcon(pkg, 0, 0);
              const buffer = await wrapInputStream(iconStream).then(r => r.arrayBuffer());
              const uint8 = new Uint8Array(buffer);
              let b64 = "";
              for (let i = 0; i < uint8.length; i++) b64 += String.fromCharCode(uint8[i]);
              iconMap[pkg] = "data:image/png;base64," + btoa(b64);
            } catch {}
          }
        }
      }

      cachedPkgList = pkgList;
      cachedLabelMap = labelMap;
      cachedIconMap = iconMap;
      appListLoaded = true;
    }

    const checkedCards = cachedPkgList.filter(pkg => pkg in gamelist);
    const uncheckedCards = cachedPkgList.filter(pkg => !(pkg in gamelist));

    const sortByLabel = list =>
      list.sort((a, b) =>
        cachedLabelMap[a].toLowerCase().localeCompare(cachedLabelMap[b].toLowerCase())
      );

    const finalList = [
      ...sortByLabel(checkedCards),
      ...sortByLabel(uncheckedCards),
    ];

    container.innerHTML = finalList.map(pkg => `
      <div class="common-card appCard bg-tonalSurface showAnim hidden" data-pkg="${pkg}">
        <img class="appIcon lazy-icon" data-src="${cachedIconMap[pkg]}" src="">
        <div class="meta">
          <div class="row">
            <div class="text-area">
              <div class="app-label">${cachedLabelMap[pkg]}</div>
              <div class="pkg-label">${pkg}</div>
            </div>
            <div class="toggle2 ${gamelist[pkg] ? "active" : ""}"
                 data-pkg="${pkg}"
                 data-state="${gamelist[pkg] ? "on" : "off"}"></div>
          </div>
        </div>
      </div>
    `).join("");

    const cardCache = {};
    container.querySelectorAll(".appCard").forEach(card => {
      const pkg = card.dataset.pkg;
      cardCache[pkg] = { card, label: cachedLabelMap[pkg], pkg };
      card.classList.add("hidden");
      card.onclick = (e) => {
        if (e.target.closest(".toggle2")) return;
        if (!gamelist[pkg]) return;
        openPerAppSettings(pkg, gamelist);
      };
    });
    
    

    const cards = finalList.map(pkg => cardCache[pkg].card);
    requestAnimationFrame(() => {
      cards.forEach((card, i) => {
        setTimeout(() => {
          card.classList.remove("hidden");
          card.classList.add("showAnim");
          requestAnimationFrame(() => card.classList.remove("showAnim"));
        }, i * 40);
      });
    });

    container.querySelectorAll(".toggle2").forEach(toggle => {
      const pkg = toggle.dataset.pkg;
      toggle.onclick = async () => {
        toggle.classList.toggle("active");
        const on = toggle.classList.contains("active");
        toggle.dataset.state = on ? "on" : "off";

        if (on) {
          gamelist[pkg] ||= { ...PERAPP_SETTINGS };
        } else {
          delete gamelist[pkg];
        }

        const card = cardCache[pkg].card;
        card.classList.add("showAnim");
        setTimeout(() => card.classList.remove("showAnim"), 120);

        await writeGameList(gamelist);
        sortCards();

        searchInput.value = "";
        searchInput.dispatchEvent(new Event("input"));
      };
    });

    const sortCards = () => {
      const activeSet = new Set(Object.keys(gamelist));
      const cardsArr = Object.values(cardCache);
      const positions = new Map();
      cardsArr.forEach(c => positions.set(c.card, c.card.getBoundingClientRect()));
      cardsArr.forEach(c => {
        c.isOn = activeSet.has(c.pkg);
        c.sortKey = c.label.toLowerCase();
      });
      cardsArr.sort((a, b) =>
        a.isOn !== b.isOn ? (a.isOn ? -1 : 1) : a.sortKey.localeCompare(b.sortKey)
      );
      cardsArr.forEach(c => container.appendChild(c.card));
      cardsArr.forEach(c => {
        const first = positions.get(c.card);
        const last = c.card.getBoundingClientRect();
        if (!first) return;
        const dx = first.left - last.left;
        const dy = first.top - last.top;
        c.card.style.transform = `translate(${dx}px, ${dy}px)`;
        c.card.style.transition = "none";
        requestAnimationFrame(() => {
          c.card.style.transition = "";
          c.card.style.transform = "";
        });
      });
    };

    searchInput.oninput = () => {
      const q = searchInput.value.toLowerCase();
      Object.values(cardCache).forEach(({ card, label, pkg }) => {
        const match = label.toLowerCase().includes(q) || pkg.toLowerCase().includes(q);
        if (match) {
          clearTimeout(card.hideTimer);
          card.classList.remove("hidden");
          card.classList.add("showAnim");
          requestAnimationFrame(() => card.classList.remove("showAnim"));
        } else {
          card.classList.add("showAnim");
          clearTimeout(card.hideTimer);
          card.hideTimer = setTimeout(() => card.classList.add("hidden"), 230);
        }
      });
    };

    const observer = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (e.isIntersecting) {
          const img = e.target;
          if (img.dataset.src && !img.loaded) {
            img.src = img.dataset.src;
            img.loaded = true;
            img.classList.add("loaded");
          }
          observer.unobserve(img);
        }
      });
    }, { rootMargin: "100px" });

    container.querySelectorAll(".lazy-icon").forEach(img => observer.observe(img));

  } catch (e) {
    console.error(e);
    container.textContent = "Error loading apps";
  } finally {
    loader2?.classList.add("hidden");
    document.body.classList.remove("no-scroll");
  }
};

const closeAppModal = () => {
  const modal = document.getElementById("appSettingsModal");
  if (modal.classList.contains("closing")) return;
  document.body.classList.remove("modal-open");
  modal.classList.add("closing");

  setTimeout(() => {
    modal.classList.remove("closing");
    modal.classList.add("hidden");
  }, 180);
};

bannerBox.addEventListener("touchstart", () => {
  pressTimer = setTimeout(() => {
    bannerInput.value = "";
    bannerInput.click();
  }, 600); 
});

bannerBox.addEventListener("touchend", () => {
  if (pressTimer) clearTimeout(pressTimer);
});

bannerInput.addEventListener("change", async (event) => {
  const files = event.target.files;
  if (!files || files.length === 0) return;
  const file = files[0];

  bannerLoader.classList.add("show");
  toast(getTranslation("toast.chngeimg"));

  const img = new Image();
  img.src = URL.createObjectURL(file);

  img.onload = async function () {
    const targetRatio = 16 / 9;
    let srcW = img.width;
    let srcH = img.height;
    let cropW = srcW;
    let cropH = Math.floor(srcW / targetRatio);

    if (cropH > srcH) {
      cropH = srcH;
      cropW = Math.floor(srcH * targetRatio);
    }

    const startX = (srcW - cropW) / 2;
    const startY = (srcH - cropH) / 2;

    const outW = 1280;
    const outH = Math.floor(outW / targetRatio);

    const canvas = document.createElement("canvas");
    canvas.width = outW;
    canvas.height = outH;

    const ctx = canvas.getContext("2d");
    ctx.drawImage(img, startX, startY, cropW, cropH, 0, 0, outW, outH);

    canvas.toBlob(async function (blob) {
      if (!blob) {
        bannerLoader.classList.remove("show");
        return;
      }

      const reader = new FileReader();
      reader.onload = async function () {
        if (!reader.result) return;
        const base64 = reader.result.toString().split(",")[1];

        const tmpFile = "/data/local/tmp/azenith_banner_tmp.b64";
        const outPath = "/data/local/tmp/azenith_banner_tmp.avif";

        await executeCommand(`rm -f "${tmpFile}" "${outPath}"`);

        const chunkSize = 16 * 1024; // 16 KB
        for (let i = 0; i < base64.length; i += chunkSize) {
          const chunk = base64.substring(i, i + chunkSize);
          await executeCommand(`echo -n "${chunk}" >> "${tmpFile}"`);
        }

        await executeCommand(`base64 -d "${tmpFile}" > "${outPath}"`);

        const dark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        const targetFile = dark
          ? "/data/adb/modules/AZenith/webroot/webui.bannerdarkmode.avif"
          : "/data/adb/modules/AZenith/webroot/webui.bannerlightmode.avif";

        await executeCommand(`mv "${outPath}" "${targetFile}"`);
        
        await executeCommand(`rm -f "${tmpFile}"`);
        location.reload();
        toast(getTranslation("toast.imgsuccess"));
      };

      reader.readAsDataURL(blob);
    }, "image/avif");
  };
});

export const saveConfig = async () => {
  try {
    const config = await collectCurrentConfig();

    const timestamp = new Date()
      .toISOString()
      .replace(/[-:]/g, "")
      .replace("T", "-")
      .split(".")[0];

    const filename = `/sdcard/azenith-config-${timestamp}.json`;

    // Convert JSON to base64 so shell can write safely
    const jsonString = JSON.stringify(config, null, 2);
    const base64Data = btoa(jsonString);

    await executeCommand(
      `echo "${base64Data}" > "${filename}"`
    );

    console.log("Config saved to:", filename);
    const saveconfToast = getTranslation("toast.saveconf", filename);
    toast(saveconfToast);
    return true;
  } catch (err) {
    console.error("saveConfig failed:", err);
    return false;
  }
};

export const loadConfigFile = async (file) => {
  try {
    // Read base64 text
    const base64Text = (await file.text()).trim();

    // Decode base64 → JSON string
    let jsonText;
    try {
      jsonText = atob(base64Text);
    } catch (e) {
      throw new Error("Base64 decode failed");
    }

    // Parse JSON
    let config;
    try {
      config = JSON.parse(jsonText);
    } catch (e) {
      throw new Error("Invalid JSON structure");
    }

    const expectedKeys = {
      config: [
        "justintime","disabletrace","logd","DThermal","SFL","malisched","fpsged",
        "schedtunes","clearbg","bypasschg","APreload","iosched","cpulimit","dnd",
        "AIenabled","renderer","freqoffset","schemeconfig","scale","showtoast",
        "resosettings","preloadbudget","thermalcore"
      ],
      governors: ["custom_default_cpu_gov", "custom_powersave_cpu_gov"],
      io: ["custom_default_balanced_IO", "custom_powersave_IO", "custom_performance_IO"]
    };

    // Validate structure
    for (const section of Object.keys(expectedKeys)) {
      if (!config[section] || typeof config[section] !== "object") {
        throw new Error(`Missing or invalid section: ${section}`);
      }

      for (const key of expectedKeys[section]) {
        if (!(key in config[section])) {
          throw new Error(`Missing key '${key}' in section '${section}'`);
        }
      }
    }

    await applySavedConfig(config);

    const loadconfToast = getTranslation("toast.loadconf");
    toast(loadconfToast);
    return true;

  } catch (err) {
    console.error("loadConfigFile failed:", err);
    const loadconffailToast = getTranslation("toast.loadconffail");
    toast(loadconffailToast);
    return false;
  }
};

const collectCurrentConfig = async () => {
  const get = async (prop, fallback = "0") => {
    try {
      const out = await executeCommand(`getprop ${prop}`);
      return (out.stdout || "").trim() || fallback;
    } catch {
      return fallback;
    }
  };

  return {
       
    config: {
      justintime: await get("persist.sys.azenithconf.justintime"),
      disabletrace: await get("persist.sys.azenithconf.disabletrace"),
      logd: await get("persist.sys.azenithconf.logd"),
      DThermal: await get("persist.sys.azenithconf.DThermal"),
      SFL: await get("persist.sys.azenithconf.SFL"),
      malisched: await get("persist.sys.azenithconf.malisched"),
      fpsged: await get("persist.sys.azenithconf.fpsged"),
      schedtunes: await get("persist.sys.azenithconf.schedtunes"),
      clearbg: await get("persist.sys.azenithconf.clearbg"),
      bypasschg: await get("persist.sys.azenithconf.bypasschg"),
      APreload: await get("persist.sys.azenithconf.APreload"),
      iosched: await get("persist.sys.azenithconf.iosched"),
      cpulimit: await get("persist.sys.azenithconf.cpulimit"),
      dnd: await get("persist.sys.azenithconf.dnd"),
      AIenabled: await get("persist.sys.azenithconf.AIenabled"),
      renderer: await get("persist.sys.azenithconf.renderer"),
      freqoffset: await get("persist.sys.azenithconf.freqoffset"),
      schemeconfig: await get("persist.sys.azenithconf.schemeconfig"),
      scale: await get("persist.sys.azenithconf.scale"),
      showtoast: await get("persist.sys.azenithconf.showtoast"),
      resosettings: await get("persist.sys.azenithconf.resosettings"),
      preloadbudget: await get("persist.sys.azenithconf.preloadbudget"),
      thermalcore: await get("persist.sys.azenithconf.thermalcore"),
    },

    governors: {
      custom_default_cpu_gov: await get("persist.sys.azenith.custom_default_cpu_gov"),
      custom_powersave_cpu_gov: await get("persist.sys.azenith.custom_powersave_cpu_gov"),
    },

    io: {
      custom_default_balanced_IO: await get("persist.sys.azenith.custom_default_balanced_IO"),
      custom_powersave_IO: await get("persist.sys.azenith.custom_powersave_IO"),
      custom_performance_IO: await get("persist.sys.azenith.custom_performance_IO"),
    }
  };
};

const applySavedConfig = async (saved) => {
  if (!saved || typeof saved !== "object") return;

  const set = async (prop, value) => {
    if (value === undefined || value === null) return;
    try {
      await executeCommand(`setprop ${prop} "${value}"`);
    } catch (e) {
      console.error("Failed to set:", prop, value, e);
    }
  };

  if (saved.config) {
    await set("persist.sys.azenithconf.justintime", saved.config.justintime);
    await set("persist.sys.azenithconf.disabletrace", saved.config.disabletrace);
    await set("persist.sys.azenithconf.logd", saved.config.logd);
    await set("persist.sys.azenithconf.DThermal", saved.config.DThermal);
    await set("persist.sys.azenithconf.SFL", saved.config.SFL);
    await set("persist.sys.azenithconf.malisched", saved.config.malisched);
    await set("persist.sys.azenithconf.fpsged", saved.config.fpsged);
    await set("persist.sys.azenithconf.schedtunes", saved.config.schedtunes);
    await set("persist.sys.azenithconf.clearbg", saved.config.clearbg);
    await set("persist.sys.azenithconf.bypasschg", saved.config.bypasschg);
    await set("persist.sys.azenithconf.APreload", saved.config.APreload);
    await set("persist.sys.azenithconf.iosched", saved.config.iosched);
    await set("persist.sys.azenithconf.cpulimit", saved.config.cpulimit);
    await set("persist.sys.azenithconf.dnd", saved.config.dnd);
    await set("persist.sys.azenithconf.AIenabled", saved.config.AIenabled);
    await set("persist.sys.azenithconf.renderer", saved.config.renderer);
    await set("persist.sys.azenithconf.freqoffset", saved.config.freqoffset);
    await set("persist.sys.azenithconf.schemeconfig", saved.config.schemeconfig);
    await set("persist.sys.azenithconf.scale", saved.config.scale);
    await set("persist.sys.azenithconf.showtoast", saved.config.showtoast);
    await set("persist.sys.azenithconf.resosettings", saved.config.resosettings);
    await set("persist.sys.azenithconf.preloadbudget", saved.config.preloadbudget);
    await set("persist.sys.azenithconf.thermalcore", saved.config.thermalcore);
  }

  if (saved.governors) {
    await set("persist.sys.azenith.custom_default_cpu_gov", saved.governors.custom_default_cpu_gov);
    await set("persist.sys.azenith.custom_powersave_cpu_gov", saved.governors.custom_powersave_cpu_gov);
  }

  if (saved.io) {
    await set("persist.sys.azenith.custom_default_balanced_IO", saved.io.custom_default_balanced_IO);
    await set("persist.sys.azenith.custom_powersave_IO", saved.io.custom_powersave_IO);
    await set("persist.sys.azenith.custom_performance_IO", saved.io.custom_performance_IO);
  }
};

const showRandomMessage = () => {
  const c = document.getElementById("msg");
  if (!c) return;

  // Make sure currentTranslations exists
  if (!window.getTranslation) return;

  // Pick a random key from 0–29
  const randomIndex = Math.floor(Math.random() * 30);
  const message = window.getTranslation(`randomMessages.${randomIndex}`);

  if (message) {
    c.textContent = message;
  } else {
    c.textContent = "";
  }
};

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

    // Base profile name
    let l =
      {
        0: "Initializing...",
        1: "Performance",
        2: "Balanced",
        3: "ECO Mode",
      }[r] || "Unknown";

    if (r === "1") {
      const { errno: c2, stdout: s2 } = await executeCommand(
        "getprop persist.sys.azenithconf.litemode"
      );
      if (c2 === 0 && s2.trim() === "1") {
        l += " (Lite)";
      }
    }

    // Prevent unnecessary DOM update
    if (lastProfile.value === l) return;
    lastProfile = { time: now, value: l };

    d.textContent = l;

    const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;

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

  } catch (err) {
    console.error("checkProfile error:", err);
  }
};

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

const socProps = [
  "ro.soc.model",
  "ro.hardware.chipname",
  "ro.board.platform",
  "ro.chipname",
  "ro.hardware",
  "ro.mediatek.platform",
  "ro.vendor.soc.model",
  "ro.vendor.qti.soc_name",
  "ro.vendor.soc.model.part_name",
  "ro.vendor.soc.model.external_name"
];

const nrmlz = (s) => s.replace(/\s+/g, "").toLowerCase();

const getAllSoCProps = async () => {
  const out = [];
  for (const prop of socProps) {
    const { errno, stdout } = await executeCommand(`getprop ${prop}`);
    if (errno === 0) {
      const v = stdout.trim();
      if (v) out.push(v);
    }
  }
  return out.length ? out : ["UnknownSoC"];
};

const getMostFrequentValue = (arr) => {
  const count = {};
  arr.forEach(v => {
    const key = nrmlz(v);
    count[key] = (count[key] || 0) + 1;
  });

  let maxFreq = 0;
  for (const k in count) {
    if (count[k] > maxFreq) maxFreq = count[k];
  }

  const freqKeys = Object.keys(count).filter(k => count[k] === maxFreq);
  let longest = freqKeys[0];

  for (const k of freqKeys) {
    if (k.length > longest.length) longest = k;
  }

  return longest;
};

const checkCPUInfo = async () => {
  const cachedLocal = localStorage.getItem("soc_info");

  try {
    const { errno: azErr, stdout: azOut } = await executeCommand(`getprop sys.azenith.soc`);

    if (azErr === 0 && azOut.trim()) {
      const soc = azOut.trim();
      document.getElementById("cpuInfo").textContent = soc;
      if (cachedLocal !== soc) localStorage.setItem("soc_info", soc);
      showFPSGEDIfMediatek();
      showMaliSchedIfMediatek();
      showWaltIfSnapdragon();
      showThermalIfMTK();
      return;
    }

    const allValues = await getAllSoCProps();
    const chosen = getMostFrequentValue(allValues);
    const db = await fetchSOCDatabase();

    let match = null;
    for (const key in db) {
      if (nrmlz(key) === chosen) {
        match = db[key];
        break;
      }
    }

    if (!match) {
      for (let i = chosen.length; i >= 4 && !match; i--) {
        const part = chosen.substring(0, i);
        for (const key in db) {
          if (nrmlz(key).startsWith(part)) {
            match = db[key];
            break;
          }
        }
      }
    }

    const displayName = match ? `${match.VENDOR} ${match.NAME}` : chosen;

    document.getElementById("cpuInfo").textContent = displayName;
    if (cachedLocal !== displayName) localStorage.setItem("soc_info", displayName);

    await executeCommand(`setprop sys.azenith.soc "${displayName}"`);

  } catch {
    document.getElementById("cpuInfo").textContent = cachedLocal || "Error";
  }

  showFPSGEDIfMediatek();
  showMaliSchedIfMediatek();
  showWaltIfSnapdragon();
  showThermalIfMTK();
};

const checkKernelVersion = async () => {
  let cachedKernel = localStorage.getItem("kernel_version");

  try {
    const { errno, stdout } = await executeCommand("uname -r");
    const el = document.getElementById("kernelInfo");

    if (errno === 0 && stdout.trim()) {
      const version = stdout.trim();
      el.textContent = version;

      if (cachedKernel !== version) {
        localStorage.setItem("kernel_version", version);
      }
    } else {
      el.textContent = cachedKernel || "Unknown Kernel";
    }
  } catch {
    el.textContent = cachedKernel || "Error";
  }
};

const getAndroidVersion = async () => {
  let cachedVersion = localStorage.getItem("android_version");

  try {
    const { errno, stdout } = await executeCommand("getprop ro.build.version.release");
    const el = document.getElementById("android");

    if (errno === 0 && stdout.trim()) {
      const version = stdout.trim();
      el.textContent = version;

      if (cachedVersion !== version) {
        localStorage.setItem("android_version", version);
      }
    } else {
      el.textContent = cachedVersion || "Unknown Android";
    }
  } catch {
    el.textContent = cachedVersion || "Error";
  }
};

const checkServiceStatus = async () => {
  const now = Date.now();
  if (now - lastServiceCheck.time < 1000) return;
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

      // Fetch profile & AI in parallel without blocking PID display
      Promise.all([
        executeCommand("cat /data/adb/.config/AZenith/API/current_profile"),
        executeCommand("getprop persist.sys.azenithconf.AIenabled")
      ]).then(([profileRawResult, aiRawResult]) => {
        const profile = profileRawResult.stdout?.trim() || "";
        const ai = aiRawResult.stdout?.trim() || "";

        // Determine status
        if (profile === "0") status = getTranslation("serviceStatus.initializing");
        else if (["1", "2", "3"].includes(profile)) {
          status =
            ai === "1"
              ? getTranslation("serviceStatus.runningAuto")
              : ai === "0"
              ? getTranslation("serviceStatus.runningIdle")
              : getTranslation("serviceStatus.unknownProfile");
        } else status = getTranslation("serviceStatus.unknownProfile");

        if (lastServiceCheck.status !== status) r.textContent = status;
        lastServiceCheck.status = status;
      });
    } else {
      status = getTranslation("serviceStatus.suspended");
      d.textContent = pidText; // show null PID
      if (lastServiceCheck.status !== status) r.textContent = status;
      lastServiceCheck.status = status;
    }

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

const hideBypassIfUnsupported = async () => {
  const { stdout } = await executeCommand(
    "getprop persist.sys.azenithconf.bypasspath"
  );

  const value = stdout.trim();
  const bypassDiv = document.getElementById("Zepass-container");

  if (!bypassDiv) return;

  if (value === "UNSUPPORTED" || value === "") {
    bypassDiv.style.display = "none";
  } else {
    bypassDiv.style.display = "flex";
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
      ? "setprop persist.sys.azenithconf.schedtunes 1 && setprop persist.sys.azenithconf.walttunes 0"
      : "setprop persist.sys.azenithconf.schedtunes 0"
  );
  checkwalt();
};

const checkwalt = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.walttunes"
  );
  0 === c && (document.getElementById("walttunes").checked = "1" === s.trim());
};

const setwalt = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.walttunes 1 && setprop persist.sys.azenithconf.schedtunes 0"
      : "setprop persist.sys.azenithconf.walttunes 0"
  );
  checkschedtunes();
};

const showWaltIfSnapdragon = () => {
  const soc = (localStorage.getItem("soc_info") || "").toLowerCase();
  const thermalDiv = document.getElementById("walt-container");
  if (thermalDiv) {
    thermalDiv.style.display = soc.includes("snapdragon") ? "flex" : "none";
  }
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

const setRRValue = async (c) => {
  await executeCommand(
    `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrefreshrates ${c}`
  );
};

const loadRRValue = async () => {
  const select = document.getElementById("setrr");
  if (!select) return;

  select.innerHTML = "";

  const defOpt = document.createElement("option");
  defOpt.value = "default";
  defOpt.textContent = "System Default";
  select.appendChild(defOpt);

  let maxRate = 60;

  try {
    const { stdout } = await executeCommand("dumpsys display");

    const regex = /fps=([\d.]+)/g;
    const rates = new Set();
    let match;

    while ((match = regex.exec(stdout)) !== null) {
      rates.add(Math.round(parseFloat(match[1])));
    }

    if (rates.size) {
      maxRate = Math.max(...rates);
    }
  } catch (_) {
    maxRate = 60;
  }
  
  const baseRates = [60, 90, 120, 144];

  const supportedRates = baseRates.filter(v => v <= maxRate);

  supportedRates.forEach(v => {
    const opt = document.createElement("option");
    opt.value = String(v);
    opt.textContent = `${v} Hz`;
    select.appendChild(opt);
  });

  try {
    const { errno, stdout } = await executeCommand(
      `cmd display get-displays | grep -oE "renderFrameRate [0-9.]+" | awk '{print int($2+0.5)}'`
    );

    if (errno === 0) {
      const val = stdout.trim();
      if ([...select.options].some(o => o.value === val)) {
        select.value = val;
      }
    }
  } catch (_) {}
};

const setCurRenderer = async (c) => {
  await executeCommand(`setprop persist.sys.azenithconf.renderer ${c}`)
  await executeCommand(
    `/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrender ${c}`
  );
};

const loadCurRenderer = async () => {
  const select = document.getElementById("renderer");
  if (!select) return;

  select.innerHTML = "";

  const defOpt = document.createElement("option");
  defOpt.value = "default";
  defOpt.textContent = "System Default";
  select.appendChild(defOpt);

  const renderers = ["vulkan", "skiagl"];

  renderers.forEach(r => {
    const opt = document.createElement("option");
    opt.value = r;
    opt.textContent = r.toUpperCase();
    select.appendChild(opt);
  });

  try {
    const { errno, stdout } = await executeCommand(
      `sh -c '[ -n "$(getprop persist.sys.azenithconf.renderer)" ] && getprop persist.sys.azenithconf.renderer'`
    );

    if (errno === 0) {
      const val = stdout.trim();

      if ([...select.options].some(o => o.value === val)) {
        select.value = val;
      }
    }
  } catch (_) {}
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
      "pkill -9 -f sys.azenith.rianixiathermalcorev4"
    );
    await executeCommand(
      "setprop persist.sys.azenith.state stopped && pkill -9 -f sys.azenith-service; su -c '/data/adb/modules/AZenith/system/bin/sys.azenith-service -r > /dev/null 2>&1 & disown'"
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

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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
};

const applyperformanceprofile = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if (c.trim() === "1") {
    toast(getTranslation("toast.alreadyPerformance"));
    return;
  }
  await executeCommand("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p 1 > /dev/null 2>&1 &");
};

const applybalancedprofile = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if (c.trim() === "2") {
    toast(getTranslation("toast.alreadyBalanced"));
    return;
  }
  await executeCommand("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p 2 > /dev/null 2>&1 &");
};

const applyecomode = async () => {
  let { stdout: c } = await executeCommand(
    "cat /data/adb/.config/AZenith/API/current_profile"
  );
  if (c.trim() === "3") {
    toast(getTranslation("toast.alreadyECO"));
    return;
  }
  await executeCommand("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p 3 > /dev/null 2>&1 &");
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

const checkfstrim = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.fstrim"
  );
  0 === c && (document.getElementById("FSTrim").checked = "1" === s.trim());
};

const setfstrim = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.fstrim 1"
      : "setprop persist.sys.azenithconf.fstrim 0"
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
  // Candidate block devices
  const blocks = ["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"];
  let validBlock = null;

  // Find the first block that exists and has a scheduler file
  for (const blk of blocks) {
    const { errno } = await executeCommand(`test -e /sys/block/${blk}/queue/scheduler`);
    if (errno === 0) {
      validBlock = blk;
      break;
    }
  }

  if (!validBlock) {
    console.warn("No valid block device with scheduler found");
    return;
  }

  // Read scheduler list from detected block
  const { errno: c, stdout: s } = await executeCommand(`cat /sys/block/${validBlock}/queue/scheduler`);
  if (c !== 0) {
    console.warn("Failed to read scheduler from", validBlock);
    return;
  }

  // Populate select element
  const select = document.getElementById("ioSchedulerBalanced");
  if (!select) return;
  select.innerHTML = "";

  const schedulers = s.trim().split(/\s+/).map(sch => sch.replace(/[\[\]]/g, ""));
  schedulers.forEach(sch => {
    const opt = document.createElement("option");
    opt.value = sch;
    opt.textContent = sch;
    select.appendChild(opt);
  });

  // Get currently active/custom scheduler property
  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_default_balanced_IO)" ] && getprop persist.sys.azenith.custom_default_balanced_IO || getprop persist.sys.azenith.default_balanced_IO'`
  );

  if (l === 0) {
    const current = m.trim().replace(/[\[\]]/g, "");
    select.value = current;
  }

  console.log(`Detected block device: ${validBlock}`);
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
  // Candidate block devices
  const blocks = ["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"];
  let validBlock = null;

  // Find the first block device that has a scheduler file
  for (const blk of blocks) {
    const { errno } = await executeCommand(`test -e /sys/block/${blk}/queue/scheduler`);
    if (errno === 0) {
      validBlock = blk;
      break;
    }
  }

  if (!validBlock) {
    console.warn("No valid block device with scheduler found");
    return;
  }

  // Read available schedulers
  const { errno: c, stdout: s } = await executeCommand(`cat /sys/block/${validBlock}/queue/scheduler`);
  if (c !== 0) {
    console.warn(`Failed to read scheduler from /sys/block/${validBlock}`);
    return;
  }

  // Populate <select> options
  const select = document.getElementById("ioSchedulerPerformance");
  if (!select) return;
  select.innerHTML = "";

  const schedulers = s.trim().split(/\s+/).map(x => x.replace(/[\[\]]/g, ""));
  schedulers.forEach(sch => {
    const opt = document.createElement("option");
    opt.value = sch;
    opt.textContent = sch;
    select.appendChild(opt);
  });

  // Get current scheduler (custom performance property)
  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_performance_IO)" ] && getprop persist.sys.azenith.custom_performance_IO || echo ""'`
  );

  if (l === 0) {
    const current = m.trim().replace(/[\[\]]/g, "");
    if (current) select.value = current;
  }

  console.log(`Detected block device: ${validBlock}`);
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
  // Candidate block devices
  const blocks = ["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"];
  let validBlock = null;

  // Find first block device that has a scheduler file
  for (const blk of blocks) {
    const { errno } = await executeCommand(`test -e /sys/block/${blk}/queue/scheduler`);
    if (errno === 0) {
      validBlock = blk;
      break;
    }
  }

  if (!validBlock) {
    console.warn("No valid block device with scheduler found");
    return;
  }

  // Read available schedulers
  const { errno: c, stdout: s } = await executeCommand(`cat /sys/block/${validBlock}/queue/scheduler`);
  if (c !== 0) {
    console.warn(`Failed to read scheduler from /sys/block/${validBlock}`);
    return;
  }

  // Populate dropdown
  const select = document.getElementById("ioSchedulerPowersave");
  if (!select) return;
  select.innerHTML = "";

  const schedulers = s.trim().split(/\s+/).map(x => x.replace(/[\[\]]/g, ""));
  schedulers.forEach(sch => {
    const opt = document.createElement("option");
    opt.value = sch;
    opt.textContent = sch;
    select.appendChild(opt);
  });

  // Get current scheduler (custom powersave property)
  const { errno: l, stdout: m } = await executeCommand(
    `sh -c '[ -n "$(getprop persist.sys.azenith.custom_powersave_IO)" ] && getprop persist.sys.azenith.custom_powersave_IO || echo ""'`
  );

  if (l === 0) {
    const current = m.trim().replace(/[\[\]]/g, "");
    if (current) select.value = current;
  }

  console.log(`Detected block device: ${validBlock}`);
};

const showAdditionalSettings = async () => {
  const c = document.getElementById("additional-modal");
  const s = c.querySelector(".additional-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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
  const c = document.getElementById("preference-modal");
  const s = c.querySelector(".preference-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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
  const c = document.getElementById("settingsModal");
  const s = c.querySelector(".settings-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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
  const c = document.getElementById("profilermodal");
  const s = c.querySelector(".profiler-container");
  document.body.classList.add("modal-open");
  c.classList.add("show");

  s.style.transition = "none";
  s.style.transform = "translateY(20px) scale(0.98)";
  void s.offsetWidth;
  s.style.transition = "transform 0.25s ease, opacity 0.25s ease";
  s.style.opacity = "1";
  const r = window.innerHeight;
  const d = () => {
    s.style.transform =
      window.innerHeight < r - 150
        ? "translateY(-10%) scale(1)"
        : "translateY(0) scale(1)";
  };
  requestAnimationFrame(() => d());
  window.addEventListener("resize", d, { passive: true });
  c._resizeHandler = d;
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

const checkthermalcore = async () => {
  let { errno: c, stdout: s } = await executeCommand(
    "getprop persist.sys.azenithconf.thermalcore"
  );
  0 === c && (document.getElementById("thermalcore").checked = "1" === s.trim());
};

const setthermalcore = async (c) => {
  await executeCommand(
    c
      ? "setprop persist.sys.azenithconf.thermalcore 1 && /data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setthermalcore 1 &"
      : "setprop persist.sys.azenithconf.thermalcore 0 && /data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setthermalcore 0 &"
  );
};

const setupUIListeners = () => {
  const bannerBox = document.getElementById("bannerBox");
  const bannerInput = document.getElementById("bannerInput");
  const bannerLoader = document.getElementById("bannerLoader");
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

  // Toggle Switches
  document
    .getElementById("FSTrim")
    ?.addEventListener("change", (e) => setfstrim(e.target.checked));
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
    .getElementById("walttunes")
    ?.addEventListener("change", (e) => setwalt(e.target.checked));
  document
    .getElementById("thermalcore")
    ?.addEventListener("change", (e) => setthermalcore(e.target.checked));
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
    .getElementById("setrr")
    ?.addEventListener("change", (e) => setRRValue(e.target.value));
  document
    .getElementById("renderer")
    ?.addEventListener("change", (e) => setCurRenderer(e.target.value));
  
  // Select GPU Gov  
  document
    .getElementById("gpuMaliDefault")
    ?.addEventListener("change", (e) => setGPUMaliDefault(e.target.value));
  document
    .getElementById("gpuMaliPerformance")
    ?.addEventListener("change", (e) => setGPUMaliPerformance(e.target.value));
  document
    .getElementById("gpuMaliPowersave")
    ?.addEventListener("change", (e) => setGPUMaliPowersave(e.target.value));  

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
    
   // Menu Settings
  document
    .getElementById("openGameList")
    ?.addEventListener("click", showGameListMenu);

  document
    .getElementById("openMain")
    ?.addEventListener("click", showMainMenu);
    
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
  
  // AZenith Save and Load Config
  document
    .getElementById("saveconfig")
    .onclick = async () => {
    await saveConfig();
  };

  document.getElementById("closeAppModal").onclick = closeAppModal;

  document
    .getElementById("loadconfig").addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const ok = await loadConfigFile(file);

    if (ok) {
      // Small delay so setprop finishes writing
      setTimeout(() => {
        location.reload();
      }, 500);
    }
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

};

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
  { fn: updateGameStatus, interval: 5000 },
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

  const loops = [
    checkProfile, 
    checkServiceStatus, 
    showRandomMessage, 
    updateGameStatus,    
  ];
  await Promise.all(loops.map((fn) => fn()));
  
  await checkWebUISupport();
  await checkServiceRunning();
  await checkModuleVersion();

  const quickChecks = [
    checkGPUMaliCompatibility,
    loadCpuGovernors,
    loadCpuFreq,    
    loadIObalance,
    loadIOperformance,
    loadIOpowersave,
    GovernorPowersave,
  ];
  await Promise.all(quickChecks.map((fn) => fn()));

  const heavyAsync = [
    checkCPUInfo,
    checkDeviceInfo,
    checkKernelVersion,
    getAndroidVersion,
    checkfpsged,
    checkLiteModeStatus,
    checkDThermal,
    checkiosched,
    checkGPreload,
    loadColorSchemeSettings,
  ];
  await Promise.all(heavyAsync.map((fn) => fn()));

  const heavySequential = [
    hideBypassIfUnsupported,
    checkmalisched,
    checkAI,
    checkthermalcore,
    checkDND,
    checkdtrace,
    checkjit,
    checktoast,
    checkfstrim,
    loadCurRenderer,
    loadRRValue,
    checkBypassChargeStatus,
    checkschedtunes,
    checkwalt,
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
checkCPUInfo();
checkDeviceInfo();
checkKernelVersion();
getAndroidVersion();
