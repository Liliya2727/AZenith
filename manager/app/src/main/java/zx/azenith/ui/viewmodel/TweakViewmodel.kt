/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.viewmodel


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import zx.azenith.ui.util.RootUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.RefreshRateReceiver
import zx.azenith.ui.util.BackupManager
import zx.azenith.ui.util.ConfigUtils
import zx.azenith.ui.util.PropertyUtils


class TweakViewModel : ViewModel() {
    data class ValidationResult(
        val isValid: Boolean, 
        val message: String, 
        val hasTweaks: Boolean, 
        val hasApplist: Boolean,
        val socType: String?,
        val data: Map<String, String>?
    )
    
    var isUiLoaded by mutableStateOf(false)
        private set


    var liteState by mutableStateOf<Boolean?>(null)
    var availableGovernors by mutableStateOf<List<String>?>(null)
    var defaultGovIndex by mutableStateOf<Int?>(null)
    var powersaveGovIndex by mutableStateOf<Int?>(null)
    var performanceGovIndex by mutableStateOf<Int?>(null)
    var freqOffsetIndex by mutableStateOf<Float?>(null)
    val offsetLabels = listOf("Disabled", "90%", "80%", "70%", "60%", "50%", "40%") // These are used as values for PropertyUtils, so we should keep them as is or map them


    var availableIOSchedulers by mutableStateOf<List<String>?>(null)
    var balancedIOIndex by mutableStateOf<Int?>(null)
    var performanceIOIndex by mutableStateOf<Int?>(null)
    var powersaveIOIndex by mutableStateOf<Int?>(null)
    

    var isMaliGpuAvailable by mutableStateOf<Boolean?>(null)
    var availableMaliGovernors by mutableStateOf<List<String>?>(null)

    /**
     * These three lists are interpolated into `Shell.cmd` argument strings for the
     * module's own CLI, so an entry that is not a bare token would be able to
     * break out of the argument. The kernel writes them, but they are still file
     * contents: filter at the point of parsing rather than trusting the source.
     * Only [A-Za-z0-9._+-] is allowed, which covers every governor, I/O
     * scheduler and Mali governor name in use.
     */
    private val SAFE_NODE_TOKEN = Regex("[A-Za-z0-9._+-]+")

    private fun parseNodeTokens(raw: String): List<String> =
        raw.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() && SAFE_NODE_TOKEN.matches(it) }
    var balancedMaliGovIndex by mutableStateOf<Int?>(null)
    var performanceMaliGovIndex by mutableStateOf<Int?>(null)
    var powersaveMaliGovIndex by mutableStateOf<Int?>(null)
    


    var preloadState by mutableStateOf<Boolean?>(null)
    var memKillerState by mutableStateOf<Boolean?>(null)
    var appPriorState by mutableStateOf<Boolean?>(null)
    var dndState by mutableStateOf<Boolean?>(null)
    var fstrimState by mutableStateOf<Boolean?>(null)


    var currentRenderer by mutableStateOf<String?>(null)
    var currentRefreshRate by mutableStateOf<Int?>(null)
    var thermalState by mutableStateOf<Boolean?>(null)
    
    var isRendererLoading by mutableStateOf(false)
        private set
    
    var isRefreshRateLoading by mutableStateOf(false)
        private set
    
    private val configKeysToBackup = listOf(
        "soc_type",
        "cpu_limit",
        "freq_offset",
        "app_preload",
        "clear_bg",
        "app_priority",
        "dnd",
        "fstrim",
        "thermalcore",
        "schedtunes",
        "sfl",
        "justintime",
        "fpsged",
        "mali_sched",
        "walttunes",
        "disable_trace",
        "kill_logd",
        "scheme_config",
        "disable_thermal",
        "use_fpsgo",
        "bypass_chg_threshold",
        "preload_budget",
        "custom_default_cpu_gov",
        "custom_powersave_cpu_gov",
        "custom_performance_cpu_gov",
        "custom_default_io",
        "custom_performance_io",
        "custom_powersave_io",
        "custom_default_mali_gov",
        "custom_performance_mali_gov",
        "custom_powersave_mali_gov"
    )
    
    
    private val APPLIST_BACKUP_KEY = "__AZENITH_APPLIST_DATA__"
    private val APPLIST_PATH = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
    
    suspend fun createConfigFileBackup(
        context: Context, 
        uri: Uri, 
        backupTweaks: Boolean, 
        backupApplist: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val propsMap = mutableMapOf<String, String>()
            val cfg = ConfigUtils.load(forceRootReload = true)

            propsMap["soc_type"] = cfg.socType?.toString().orEmpty()

            if (backupTweaks) {
                configKeysToBackup.forEach { key ->
                    if (key != "soc_type") {
                        propsMap[key] = ConfigUtils.getKeyString(cfg, key)
                    }
                }
            }

            if (backupApplist) {

                val applistContent = RootUtils.readRootFile(APPLIST_PATH).orEmpty()
                if (applistContent.isNotBlank()) {
                    propsMap[APPLIST_BACKUP_KEY] = applistContent
                }
            }

            val isSuccess = BackupManager.createBackup(context, uri, propsMap)
            
            delay(1500) 
            
            isSuccess 
        }
    }
    
    suspend fun validateAndRestoreFile(context: Context, uri: Uri): ValidationResult {
        return withContext(Dispatchers.IO) {
            val backupData = BackupManager.readBackup(context, uri)
            
            if (backupData == null) {
                return@withContext ValidationResult(false, context.getString(R.string.err_invalid_backup), false, false, null, null)
            }
            val backupSocType = backupData["soc_type"]
                ?: backupData["persist.sys.azenith.soctype"] 
                ?: backupData["persist.sys.azenithdebug.soctype"]
                
            val hasApplist = backupData.containsKey(APPLIST_BACKUP_KEY)
            
            val hasTweaks = backupData.keys.any { key ->
                val canonical = ConfigUtils.mapLegacyOrNewKey(key)
                canonical != null && canonical != "soc_type"
            }
    
            ValidationResult(true, "", hasTweaks, hasApplist, backupSocType, backupData)
        }
    }

    suspend fun applyRestoreData(
        context: Context, 
        backupData: Map<String, String>, 
        restoreTweaks: Boolean, 
        restoreApplist: Boolean
    ) {
        withContext(Dispatchers.IO) {
            if (restoreTweaks) {
                val updates = mutableMapOf<String, String>()
                backupData.forEach { (key, value) ->
                    val canonical = ConfigUtils.mapLegacyOrNewKey(key)
                    if (canonical != null &&
                        canonical != "soc_type" &&
                        key != APPLIST_BACKUP_KEY &&
                        value.isNotEmpty()
                    ) {
                        updates[canonical] = value
                    }
                }
                ConfigUtils.setMany(updates)
            }

            if (restoreApplist && backupData.containsKey(APPLIST_BACKUP_KEY)) {
                val applistContent = backupData[APPLIST_BACKUP_KEY]!!
                RootUtils.writeRootFile(APPLIST_PATH, applistContent)
            }
            
            RootUtils.touchRootFile("/data/adb/modules/AZenith/reboot")
            if (restoreTweaks) {
                loadAllConfiguration(context)
            }
            delay(1200) 
        }
    }
    
    /**
     * Re-reads only the values the renderer and refresh-rate tiles show.
     *
     * [loadAllConfiguration] does a full sweep: it also reads governor files and touches
     * every tweak property, so running it just to notice a prop change made by the module
     * or another app is wasteful. This lighter read is safe to call repeatedly, and it only
     * writes when a value actually changed so a poll does not recompose the tiles for
     * nothing.
     */
    suspend fun refreshDisplayState(context: Context) = withContext(Dispatchers.IO) {
        val rawRenderer = PropertyUtils.get("debug.hwui.renderer")
        val azenithRenderer = ConfigUtils.load().renderer

        val renderer = when {
            rawRenderer.isEmpty() && (azenithRenderer.isEmpty() || azenithRenderer.equals("default", ignoreCase = true)) -> "Default"

            azenithRenderer.isEmpty() -> if (rawRenderer.equals("default", ignoreCase = true)) "Default" else rawRenderer

            rawRenderer.isEmpty() -> if (azenithRenderer.equals("default", ignoreCase = true)) "Default" else azenithRenderer

            else -> {
                val isSameValue = rawRenderer.equals(azenithRenderer, ignoreCase = true)
                val isAzenithDefault = azenithRenderer.equals("default", ignoreCase = true)

                when {
                    isSameValue -> if (rawRenderer.equals("default", ignoreCase = true)) "Default" else rawRenderer
                    isAzenithDefault -> "Default ($rawRenderer)"
                    else -> "$azenithRenderer ($rawRenderer)"
                }
            }
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.refreshRate.toInt()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate.toInt()
        }

        if (currentRenderer != renderer) currentRenderer = renderer
        if (currentRefreshRate != refreshRate) currentRefreshRate = refreshRate
    }

    fun loadAllConfiguration(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cfg = ConfigUtils.load()
            launch {
                liteState = cfg.cpuLimit
                
                val savedOffset = cfg.freqOffset.ifEmpty { "Disabled" }
                freqOffsetIndex = when (savedOffset) {
                    "90" -> 1f; "80" -> 2f; "70" -> 3f; "60" -> 4f; "50" -> 5f; "40" -> 6f; else -> 0f
                }            
                
                preloadState = cfg.appPreload
                memKillerState = cfg.clearBg
                appPriorState = cfg.appPriority
                dndState = cfg.dnd
                fstrimState = cfg.fstrim
                thermalState = cfg.thermalcore

                refreshDisplayState(context)
            }

            val govJob = async { loadGovernorsInternal() }
            val ioJob = async { loadIOSchedulersInternal() }
            val maliJob = async { loadMaliGovernorsInternal() }

            govJob.await()
            ioJob.await()
            maliJob.await()

            withContext(Dispatchers.Main) {
                isUiLoaded = true
            }
        }
    }

    private fun loadGovernorsInternal() {
        val govs = RootUtils.readRootFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors")
            ?.let { parseNodeTokens(it) } ?: emptyList()
        if (govs.isNotEmpty()) {
            val cfg = ConfigUtils.load()
            val currentDefault = cfg.customDefaultCpuGov.ifEmpty { cfg.defaultCpuGov }
            val currentPowersave = cfg.customPowersaveCpuGov
            val currentPerformance = cfg.customPerformanceCpuGov

            availableGovernors = govs
            defaultGovIndex = govs.indexOf(currentDefault).coerceAtLeast(0)
            powersaveGovIndex = govs.indexOf(currentPowersave).coerceAtLeast(0)
            performanceGovIndex = govs.indexOf(currentPerformance).coerceAtLeast(0)
        } else {
            availableGovernors = emptyList()
        }
    }

    private fun loadIOSchedulersInternal() {
        val candidates = listOf("mmcblk0", "mmcblk1", "sda", "sdb", "sdc")
        var validBlock = ""
        for (block in candidates) {
            if (RootUtils.rootFileExists("/sys/block/$block/queue/scheduler")) {
                validBlock = block
                break
            }
        }
        if (validBlock.isNotEmpty()) {
            val rawOut = RootUtils.readRootFile("/sys/block/$validBlock/queue/scheduler").orEmpty()
            if (rawOut.isNotEmpty()) {
                val schedulers = parseNodeTokens(rawOut.replace("[", "").replace("]", ""))
                val cfg = ConfigUtils.load()

                val currentBal = cfg.customDefaultIo.ifEmpty { cfg.defaultIo }
                val currentPerf = cfg.customPerformanceIo
                val currentEco = cfg.customPowersaveIo

                availableIOSchedulers = schedulers
                balancedIOIndex = schedulers.indexOf(currentBal).coerceAtLeast(0)
                performanceIOIndex = schedulers.indexOf(currentPerf).coerceAtLeast(0)
                powersaveIOIndex = schedulers.indexOf(currentEco).coerceAtLeast(0)
            } else {
                availableIOSchedulers = emptyList()
            }
        } else {
            availableIOSchedulers = emptyList()
        }
    }

    private fun loadMaliGovernorsInternal() {

        val checkResult = Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf checkmalipath").exec()
        val hasMali = checkResult.out.joinToString("").trim() == "true"

        if (hasMali) {
            isMaliGpuAvailable = true

            // The node is behind a glob (`*.mali`). Was `cat /sys/class/devfreq/
            // *.mali/available_governors` through a shell for the expansion.
            // `SuFile.listFiles()` goes through the root shell, so the matching
            // no longer needs a process; the read goes through readRootFile
            // rather than File.readText, which is inherited from java.io.File
            // and would open an unprivileged FileInputStream.
            val maliGovs = try {
                SuFile("/sys/class/devfreq").listFiles()
                    ?.firstOrNull { it.name.endsWith(".mali") }
                    ?.let { RootUtils.readRootFile("${it.absolutePath}/available_governors") }
            } catch (e: Exception) {
                null
            }

            if (!maliGovs.isNullOrEmpty()) {
                val govs = parseNodeTokens(maliGovs)
                    .filterNot { it.startsWith("apu", ignoreCase = true) }
                val cfg = ConfigUtils.load()
                
                val currentBal = cfg.customDefaultMaliGov.ifEmpty { cfg.defaultMaliGov }
                val currentPerf = cfg.customPerformanceMaliGov
                val currentEco = cfg.customPowersaveMaliGov

                availableMaliGovernors = govs
                balancedMaliGovIndex = govs.indexOf(currentBal).coerceAtLeast(0)
                performanceMaliGovIndex = govs.indexOf(currentPerf).coerceAtLeast(0)
                powersaveMaliGovIndex = govs.indexOf(currentEco).coerceAtLeast(0)
            } else {
                availableMaliGovernors = emptyList()
            }
        } else {
            isMaliGpuAvailable = false
        }
    }

    

    fun updateLiteMode(checked: Boolean) {
        liteState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("cpu_limit", checked)
        }
    }

    fun updateDefaultGovernor(index: Int) {
        defaultGovIndex = index
        val selectedGov = availableGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_default_cpu_gov", selectedGov)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "2") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").exec()
            }
        }
    }

    fun updatePowersaveGovernor(index: Int) {
        powersaveGovIndex = index
        val selectedGov = availableGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_powersave_cpu_gov", selectedGov)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "3") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").exec()
            }
        }
    }
    
    fun updatePerformanceGovernor(index: Int) {
        performanceGovIndex = index
        val selectedGov = availableGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_performance_cpu_gov", selectedGov)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "3") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").exec()
            }
        }
    }

    fun saveFreqOffset(value: Float) {
        freqOffsetIndex = value
        val index = value.roundToInt()
        val propValue = if (index == 0) "Disabled" else offsetLabels[index].replace("%", "")
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("freq_offset", propValue)
        }
    }

    fun updateBalancedIO(index: Int) {
        balancedIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_default_io", selectedIO)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "2") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }

    fun updatePerformanceIO(index: Int) {
        performanceIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_performance_io", selectedIO)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "1") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }

    fun updatePowersaveIO(index: Int) {
        powersaveIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_powersave_io", selectedIO)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "3") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }
    
    fun updateBalancedMaliGov(index: Int) {
        balancedMaliGovIndex = index
        val selectedGov = availableMaliGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_default_mali_gov", selectedGov)

            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "2") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsMaliGov $selectedGov").exec()
            }
        }
    }

    fun updatePerformanceMaliGov(index: Int) {
        performanceMaliGovIndex = index
        val selectedGov = availableMaliGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_performance_mali_gov", selectedGov)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "1") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsMaliGov $selectedGov").exec()
            }
        }
    }

    fun updatePowersaveMaliGov(index: Int) {
        powersaveMaliGovIndex = index
        val selectedGov = availableMaliGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.set("custom_powersave_mali_gov", selectedGov)
            val currentProfile = RootUtils.readRootFile("/data/adb/.config/AZenith/API/current_profile")
            if (currentProfile == "3") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsMaliGov $selectedGov").exec()
            }
        }
    }

    fun updatePreloadMode(checked: Boolean) {
        preloadState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("app_preload", checked)
        }
    }

    fun updateMemoryKiller(checked: Boolean) {
        memKillerState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("clear_bg", checked)
        }
    }

    fun updateAppPriority(checked: Boolean) {
        appPriorState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("app_priority", checked)
        }
    }

    fun updateDndMode(checked: Boolean) {
        dndState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("dnd", checked)
        }
    }

    fun updateFstrim(checked: Boolean) {
        fstrimState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("fstrim", checked)
        }
    }

    fun updateThermalCore(checked: Boolean) {
        thermalState = checked
        viewModelScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("thermalcore", checked)
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setthermalcore ${if (checked) "1" else "0"}").exec()
        }
    }

    fun executeSetRenderer(reason: String, context: Context) {
        isRendererLoading = true
        ConfigUtils.set("renderer", reason)
        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrender $reason").submit {
            viewModelScope.launch {
                delay(1000)
                loadAllConfiguration(context)
                isRendererLoading = false
            }
        }
    }
    
    fun executeSetRefreshRates(reason: String, context: Context) {
        isRefreshRateLoading = true
    
        val fps = reason.toIntOrNull() ?: 60
        val intent = Intent(context, RefreshRateReceiver::class.java).apply {
            action = "zx.azenith.SET_FPS"
            putExtra("fps", fps)
        }
        context.sendBroadcast(intent)
    
        viewModelScope.launch {
            delay(1000)
            loadAllConfiguration(context)
            isRefreshRateLoading = false
        }
    }

}
