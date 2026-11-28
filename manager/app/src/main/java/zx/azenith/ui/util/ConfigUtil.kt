/*
 * Copyright (C) 2026-2027 Zexshia
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

package zx.azenith.ui.util

import com.topjohnwu.superuser.Shell
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AZenithConfig(
    // Schema version
    val version: Int = 1,

    // System
    @SerialName("soc_type") val socType: Int? = null,
    @SerialName("debug_mode") val debugMode: Boolean = false,
    @SerialName("disable_tweak") val disableTweak: Boolean = false,

    // Daemon
    @SerialName("auto_mode") val autoMode: Boolean = true,
    @SerialName("drop_foreground") val dropForeground: Boolean = false,
    @SerialName("profile_notifications") val profileNotifications: Boolean = true,
    @SerialName("show_toast") val showToast: Boolean = false,
    @SerialName("force_sidepanel") val forceSidepanel: Boolean = false,

    // CPU
    @SerialName("cpu_limit") val cpuLimit: Boolean = false,
    @SerialName("freq_offset") val freqOffset: String = "Disabled",

    // CPU Governors
    @SerialName("default_cpu_gov") val defaultCpuGov: String = "",
    @SerialName("custom_default_cpu_gov") val customDefaultCpuGov: String = "",
    @SerialName("custom_powersave_cpu_gov") val customPowersaveCpuGov: String = "",
    @SerialName("custom_performance_cpu_gov") val customPerformanceCpuGov: String = "",

    // I/O Schedulers
    @SerialName("default_io") val defaultIo: String = "",
    @SerialName("custom_default_io") val customDefaultIo: String = "",
    @SerialName("custom_powersave_io") val customPowersaveIo: String = "",
    @SerialName("custom_performance_io") val customPerformanceIo: String = "",

    // Mali GPU Governors
    @SerialName("default_mali_gov") val defaultMaliGov: String = "",
    @SerialName("custom_default_mali_gov") val customDefaultMaliGov: String = "",
    @SerialName("custom_powersave_mali_gov") val customPowersaveMaliGov: String = "",
    @SerialName("custom_performance_mali_gov") val customPerformanceMaliGov: String = "",

    // Display & GPU
    val renderer: String = "default",
    @SerialName("scheme_config") val schemeConfig: String = "1000 1000 1000 1000",
    val sfl: Boolean = false,
    @SerialName("mali_sched") val maliSched: Boolean = false,

    // Features
    @SerialName("app_preload") val appPreload: Boolean = false,
    @SerialName("preload_budget") val preloadBudget: String = "500M",
    @SerialName("clear_bg") val clearBg: Boolean = false,
    @SerialName("app_priority") val appPriority: Boolean = true,
    val dnd: Boolean = false,
    val fstrim: Boolean = false,
    val thermalcore: Boolean = false,

    // Preferences
    val schedtunes: Boolean = false,
    val walttunes: Boolean = false,
    val justintime: Boolean = false,
    @SerialName("disable_trace") val disableTrace: Boolean = false,
    @SerialName("kill_logd") val killLogd: Boolean = false,
    @SerialName("disable_thermal") val disableThermal: Boolean = false,
    @SerialName("use_fpsgo") val useFpsgo: Boolean = false,
    val fpsged: Boolean = false,

    // Standby Charge
    @SerialName("standby_chg") val standbyChg: Boolean = false,
    @SerialName("standby_path") val standbyPath: String = "NEED_SETUP"
)

object ConfigUtils {
    const val DAEMON_CONFIG_PATH = "/data/adb/.config/AZenith/config.json"
    const val APP_CONFIG_MIRROR_PATH = "/data/data/zx.azenith/API/config.json"
    private const val DAEMON_BIN = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Volatile
    private var cachedConfig: AZenithConfig? = null

    @Volatile
    private var cachedMirrorMtime: Long = -1L

    /**
     * Load [AZenithConfig] from `/data/data/zx.azenith/API/config.json` via direct File I/O
     * (zero root shell fork), falling back to root read on `/data/adb/.config/AZenith/config.json`
     * only if the app mirror is absent.
     */
    fun load(forceRootReload: Boolean = false): AZenithConfig {
        val mirrorFile = File(APP_CONFIG_MIRROR_PATH)
        if (!forceRootReload && mirrorFile.exists()) {
            val mtime = mirrorFile.lastModified()
            val cur = cachedConfig
            if (cur != null && mtime > 0L && mtime == cachedMirrorMtime) {
                return cur
            }
            try {
                val raw = mirrorFile.readText()
                if (raw.isNotBlank()) {
                    val parsed = json.decodeFromString<AZenithConfig>(raw)
                    cachedConfig = parsed
                    cachedMirrorMtime = mtime
                    return parsed
                }
            } catch (_: Exception) {
            }
        }

        if (!forceRootReload) {
            cachedConfig?.let { return it }
        }

        val rootRaw = RootUtils.readRootFile(DAEMON_CONFIG_PATH)
        if (!rootRaw.isNullOrBlank()) {
            try {
                val parsed = json.decodeFromString<AZenithConfig>(rootRaw)
                cachedConfig = parsed
                writeMirrorLocal(parsed)
                return parsed
            } catch (_: Exception) {
            }
        }

        val fallback = cachedConfig ?: AZenithConfig()
        cachedConfig = fallback
        return fallback
    }

    private fun writeMirrorLocal(cfg: AZenithConfig) {
        try {
            val mirrorFile = File(APP_CONFIG_MIRROR_PATH)
            mirrorFile.parentFile?.mkdirs()
            val tmpFile = File(mirrorFile.parentFile, ".config.json.tmp_${System.nanoTime()}")
            val encoded = json.encodeToString(cfg) + "\n"
            tmpFile.writeText(encoded)
            if (tmpFile.renameTo(mirrorFile)) {
                cachedMirrorMtime = mirrorFile.lastModified()
            } else {
                tmpFile.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun escapeShellArg(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    /**
     * Update a single configuration key via `sys.azenith-service --config-set <key> <value>`
     * (which holds `.config.lock` via `flock` and mirrors atomically), while updating the
     * in-memory cache immediately so UI reads see the new value without waiting.
     */
    fun set(key: String, value: String) {
        val current = load()
        val updated = applyKeyToConfig(current, key, value)
        cachedConfig = updated
        writeMirrorLocal(updated)

        val safeKey = escapeShellArg(key)
        val safeVal = escapeShellArg(value)
        Shell.cmd("$DAEMON_BIN --config-set $safeKey $safeVal").submit()
    }

    fun setBool(key: String, value: Boolean) {
        set(key, if (value) "true" else "false")
    }

    fun setInt(key: String, value: Int) {
        set(key, value.toString())
    }

    /**
     * Batch-update multiple configuration keys in a single root command invocation.
     */
    fun setMany(updates: Map<String, String>) {
        if (updates.isEmpty()) return
        var cfg = load()
        val cmds = mutableListOf<String>()
        for ((k, v) in updates) {
            val canonicalKey = mapLegacyOrNewKey(k) ?: continue
            cfg = applyKeyToConfig(cfg, canonicalKey, v)
            cmds.add("$DAEMON_BIN --config-set ${escapeShellArg(canonicalKey)} ${escapeShellArg(v)}")
        }
        cachedConfig = cfg
        writeMirrorLocal(cfg)
        if (cmds.isNotEmpty()) {
            Shell.cmd(*cmds.toTypedArray()).exec()
        }
    }

    fun parseFlexibleBool(raw: String): Boolean {
        return when (raw.trim().lowercase()) {
            "1", "true", "yes", "on" -> true
            else -> false
        }
    }

    /**
     * Maps either a legacy `persist.sys.azenith*` property name or a new `snake_case` key
     * to the canonical `config.json` key name. Returns `null` if unknown.
     */
    fun mapLegacyOrNewKey(key: String): String? {
        return when (key) {
            "soc_type", "persist.sys.azenith.soctype", "persist.sys.azenithdebug.soctype" -> "soc_type"
            "debug_mode", "persist.sys.azenith.debugmode" -> "debug_mode"
            "disable_tweak", "persist.sys.azenith.disabletweak" -> "disable_tweak"
            "auto_mode", "persist.sys.azenithconf.AIenabled" -> "auto_mode"
            "drop_foreground", "persist.sys.azenith.dropforeground" -> "drop_foreground"
            "profile_notifications", "persist.sys.azenith.profilenotifications" -> "profile_notifications"
            "show_toast", "persist.sys.azenithconf.showtoast" -> "show_toast"
            "force_sidepanel", "persist.sys.azenith.force_sidepanel" -> "force_sidepanel"
            "cpu_limit", "persist.sys.azenithconf.cpulimit" -> "cpu_limit"
            "freq_offset", "persist.sys.azenithconf.freqoffset" -> "freq_offset"
            "default_cpu_gov", "persist.sys.azenith.default_cpu_gov" -> "default_cpu_gov"
            "custom_default_cpu_gov", "persist.sys.azenith.custom_default_cpu_gov" -> "custom_default_cpu_gov"
            "custom_powersave_cpu_gov", "persist.sys.azenith.custom_powersave_cpu_gov" -> "custom_powersave_cpu_gov"
            "custom_performance_cpu_gov", "persist.sys.azenith.custom_performance_cpu_gov" -> "custom_performance_cpu_gov"
            "default_io", "persist.sys.azenith.default_balanced_IO" -> "default_io"
            "custom_default_io", "persist.sys.azenith.custom_default_balanced_IO" -> "custom_default_io"
            "custom_powersave_io", "persist.sys.azenith.custom_powersave_IO" -> "custom_powersave_io"
            "custom_performance_io", "persist.sys.azenith.custom_performance_IO" -> "custom_performance_io"
            "default_mali_gov", "persist.sys.azenith.default_maligpu_gov" -> "default_mali_gov"
            "custom_default_mali_gov", "persist.sys.azenith.custom_default_maligpu_gov" -> "custom_default_mali_gov"
            "custom_powersave_mali_gov", "persist.sys.azenith.custom_powersave_maligpu_gov" -> "custom_powersave_mali_gov"
            "custom_performance_mali_gov", "persist.sys.azenith.custom_performance_maligpu_gov" -> "custom_performance_mali_gov"
            "renderer", "persist.sys.azenithconf.renderer" -> "renderer"
            "scheme_config", "persist.sys.azenithconf.schemeconfig" -> "scheme_config"
            "sfl", "persist.sys.azenithconf.SFL" -> "sfl"
            "mali_sched", "persist.sys.azenithconf.malisched" -> "mali_sched"
            "app_preload", "persist.sys.azenithconf.APreload" -> "app_preload"
            "preload_budget", "persist.sys.azenithconf.preloadbudget" -> "preload_budget"
            "clear_bg", "persist.sys.azenithconf.clearbg" -> "clear_bg"
            "app_priority", "persist.sys.azenithconf.iosched" -> "app_priority"
            "dnd", "persist.sys.azenithconf.dnd" -> "dnd"
            "fstrim", "persist.sys.azenithconf.fstrim" -> "fstrim"
            "thermalcore", "persist.sys.azenithconf.thermalcore" -> "thermalcore"
            "schedtunes", "persist.sys.azenithconf.schedtunes" -> "schedtunes"
            "walttunes", "persist.sys.azenithconf.walttunes" -> "walttunes"
            "justintime", "persist.sys.azenithconf.justintime" -> "justintime"
            "disable_trace", "persist.sys.azenithconf.disabletrace" -> "disable_trace"
            "kill_logd", "persist.sys.azenithconf.logd" -> "kill_logd"
            "disable_thermal", "persist.sys.azenithconf.DThermal" -> "disable_thermal"
            "use_fpsgo", "persist.sys.azenithconf.usefpsgo" -> "use_fpsgo"
            "fpsged", "persist.sys.azenithconf.fpsged" -> "fpsged"
            "standby_chg", "bypass_chg", "persist.sys.azenithconf.bypasschg" -> "standby_chg"
            "standby_path", "bypass_path", "persist.sys.azenithconf.bypasspath" -> "standby_path"
            else -> null
        }
    }

    fun getKeyString(cfg: AZenithConfig, key: String): String {
        return when (mapLegacyOrNewKey(key) ?: key) {
            "soc_type" -> cfg.socType?.toString().orEmpty()
            "debug_mode" -> cfg.debugMode.toString()
            "disable_tweak" -> cfg.disableTweak.toString()
            "auto_mode" -> cfg.autoMode.toString()
            "drop_foreground" -> cfg.dropForeground.toString()
            "profile_notifications" -> cfg.profileNotifications.toString()
            "show_toast" -> cfg.showToast.toString()
            "force_sidepanel" -> cfg.forceSidepanel.toString()
            "cpu_limit" -> cfg.cpuLimit.toString()
            "freq_offset" -> cfg.freqOffset
            "default_cpu_gov" -> cfg.defaultCpuGov
            "custom_default_cpu_gov" -> cfg.customDefaultCpuGov
            "custom_powersave_cpu_gov" -> cfg.customPowersaveCpuGov
            "custom_performance_cpu_gov" -> cfg.customPerformanceCpuGov
            "default_io" -> cfg.defaultIo
            "custom_default_io" -> cfg.customDefaultIo
            "custom_powersave_io" -> cfg.customPowersaveIo
            "custom_performance_io" -> cfg.customPerformanceIo
            "default_mali_gov" -> cfg.defaultMaliGov
            "custom_default_mali_gov" -> cfg.customDefaultMaliGov
            "custom_powersave_mali_gov" -> cfg.customPowersaveMaliGov
            "custom_performance_mali_gov" -> cfg.customPerformanceMaliGov
            "renderer" -> cfg.renderer
            "scheme_config" -> cfg.schemeConfig
            "sfl" -> cfg.sfl.toString()
            "mali_sched" -> cfg.maliSched.toString()
            "app_preload" -> cfg.appPreload.toString()
            "preload_budget" -> cfg.preloadBudget
            "clear_bg" -> cfg.clearBg.toString()
            "app_priority" -> cfg.appPriority.toString()
            "dnd" -> cfg.dnd.toString()
            "fstrim" -> cfg.fstrim.toString()
            "thermalcore" -> cfg.thermalcore.toString()
            "schedtunes" -> cfg.schedtunes.toString()
            "walttunes" -> cfg.walttunes.toString()
            "justintime" -> cfg.justintime.toString()
            "disable_trace" -> cfg.disableTrace.toString()
            "kill_logd" -> cfg.killLogd.toString()
            "disable_thermal" -> cfg.disableThermal.toString()
            "use_fpsgo" -> cfg.useFpsgo.toString()
            "fpsged" -> cfg.fpsged.toString()
            "standby_chg" -> cfg.standbyChg.toString()
            "standby_path" -> cfg.standbyPath
            else -> ""
        }
    }

    private fun applyKeyToConfig(cfg: AZenithConfig, key: String, valStr: String): AZenithConfig {
        val k = mapLegacyOrNewKey(key) ?: key
        return when (k) {
            "soc_type" -> cfg.copy(socType = valStr.trim().toIntOrNull())
            "debug_mode" -> cfg.copy(debugMode = parseFlexibleBool(valStr))
            "disable_tweak" -> cfg.copy(disableTweak = parseFlexibleBool(valStr))
            "auto_mode" -> cfg.copy(autoMode = parseFlexibleBool(valStr))
            "drop_foreground" -> cfg.copy(dropForeground = parseFlexibleBool(valStr))
            "profile_notifications" -> cfg.copy(profileNotifications = parseFlexibleBool(valStr))
            "show_toast" -> cfg.copy(showToast = parseFlexibleBool(valStr))
            "force_sidepanel" -> cfg.copy(forceSidepanel = parseFlexibleBool(valStr))
            "cpu_limit" -> cfg.copy(cpuLimit = parseFlexibleBool(valStr))
            "freq_offset" -> cfg.copy(freqOffset = valStr)
            "default_cpu_gov" -> cfg.copy(defaultCpuGov = valStr)
            "custom_default_cpu_gov" -> cfg.copy(customDefaultCpuGov = valStr)
            "custom_powersave_cpu_gov" -> cfg.copy(customPowersaveCpuGov = valStr)
            "custom_performance_cpu_gov" -> cfg.copy(customPerformanceCpuGov = valStr)
            "default_io" -> cfg.copy(defaultIo = valStr)
            "custom_default_io" -> cfg.copy(customDefaultIo = valStr)
            "custom_powersave_io" -> cfg.copy(customPowersaveIo = valStr)
            "custom_performance_io" -> cfg.copy(customPerformanceIo = valStr)
            "default_mali_gov" -> cfg.copy(defaultMaliGov = valStr)
            "custom_default_mali_gov" -> cfg.copy(customDefaultMaliGov = valStr)
            "custom_powersave_mali_gov" -> cfg.copy(customPowersaveMaliGov = valStr)
            "custom_performance_mali_gov" -> cfg.copy(customPerformanceMaliGov = valStr)
            "renderer" -> cfg.copy(renderer = valStr)
            "scheme_config" -> cfg.copy(schemeConfig = valStr)
            "sfl" -> cfg.copy(sfl = parseFlexibleBool(valStr))
            "mali_sched" -> cfg.copy(maliSched = parseFlexibleBool(valStr))
            "app_preload" -> cfg.copy(appPreload = parseFlexibleBool(valStr))
            "preload_budget" -> cfg.copy(preloadBudget = valStr)
            "clear_bg" -> cfg.copy(clearBg = parseFlexibleBool(valStr))
            "app_priority" -> cfg.copy(appPriority = parseFlexibleBool(valStr))
            "dnd" -> cfg.copy(dnd = parseFlexibleBool(valStr))
            "fstrim" -> cfg.copy(fstrim = parseFlexibleBool(valStr))
            "thermalcore" -> cfg.copy(thermalcore = parseFlexibleBool(valStr))
            "schedtunes" -> cfg.copy(schedtunes = parseFlexibleBool(valStr))
            "walttunes" -> cfg.copy(walttunes = parseFlexibleBool(valStr))
            "justintime" -> cfg.copy(justintime = parseFlexibleBool(valStr))
            "disable_trace" -> cfg.copy(disableTrace = parseFlexibleBool(valStr))
            "kill_logd" -> cfg.copy(killLogd = parseFlexibleBool(valStr))
            "disable_thermal" -> cfg.copy(disableThermal = parseFlexibleBool(valStr))
            "use_fpsgo" -> cfg.copy(useFpsgo = parseFlexibleBool(valStr))
            "fpsged" -> cfg.copy(fpsged = parseFlexibleBool(valStr))
            "standby_chg" -> cfg.copy(standbyChg = parseFlexibleBool(valStr))
            "standby_path" -> cfg.copy(standbyPath = valStr)
            else -> cfg
        }
    }
}
