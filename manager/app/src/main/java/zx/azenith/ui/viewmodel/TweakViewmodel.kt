package zx.azenith.ui.viewmodel

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.ui.util.PropertyUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import android.net.Uri
import zx.azenith.ui.util.BackupManager

    
class TweakViewModel : ViewModel() {

    var isUiLoaded by mutableStateOf(false)
        private set

    // Section 1 & 2 Properti CPU
    var liteState by mutableStateOf<Boolean?>(null)
    var availableGovernors by mutableStateOf<List<String>?>(null)
    var defaultGovIndex by mutableStateOf<Int?>(null)
    var powersaveGovIndex by mutableStateOf<Int?>(null)
    var freqOffsetIndex by mutableStateOf<Float?>(null)
    val offsetLabels = listOf("Disabled", "90%", "80%", "70%", "60%", "50%", "40%")

    // Section 3 Properti I/O Scheduler
    var availableIOSchedulers by mutableStateOf<List<String>?>(null)
    var balancedIOIndex by mutableStateOf<Int?>(null)
    var performanceIOIndex by mutableStateOf<Int?>(null)
    var powersaveIOIndex by mutableStateOf<Int?>(null)

    // Section 4 Properti Tambahan
    var preloadState by mutableStateOf<Boolean?>(null)
    var memKillerState by mutableStateOf<Boolean?>(null)
    var appPriorState by mutableStateOf<Boolean?>(null)
    var dndState by mutableStateOf<Boolean?>(null)
    var fstrimState by mutableStateOf<Boolean?>(null)

    // Section 5 Properti Power & Rendering
    var currentRenderer by mutableStateOf<String?>(null)
    var currentRefreshRate by mutableStateOf<Int?>(null)
    var thermalState by mutableStateOf<Boolean?>(null)
    
    private val configKeysToBackup = listOf(
        "persist.sys.azenithdebug.soctype", // WAJIB ADA UNTUK VALIDASI
        "persist.sys.azenithconf.cpulimit",
        "persist.sys.azenithconf.freqoffset",
        "persist.sys.azenithconf.APreload",
        "persist.sys.azenithconf.clearbg",
        "persist.sys.azenithconf.iosched",
        "persist.sys.azenithconf.dnd",
        "persist.sys.azenithconf.fstrim",
        "persist.sys.azenithconf.thermalcore",
        "persist.sys.azenithconf.schedtunes",
        "persist.sys.azenithconf.SFL",
        "persist.sys.azenithconf.justintime",
        "persist.sys.azenithconf.fpsged",
        "persist.sys.azenithconf.malisched",
        "persist.sys.azenithconf.walttunes",
        "persist.sys.azenithconf.disabletrace",
        "persist.sys.azenithconf.logd",
        "persist.sys.azenithconf.schemeconfig",
        "persist.sys.azenithconf.DThermal",
        "persist.sys.azenithconf.usefpsgo",
        "persist.sys.azenithconf.bypasschg",
        "persist.sys.azenithconf.bypasschgthreshold",
        "persist.sys.azenithconf.bypasspath",
        "persist.sys.azenithconf.preloadbudget",
        "persist.sys.azenith.custom_default_cpu_gov",
        "persist.sys.azenith.custom_powersave_cpu_gov",
        "persist.sys.azenith.custom_default_balanced_IO",
        "persist.sys.azenith.custom_performance_IO",
        "persist.sys.azenith.custom_powersave_IO"
    )
    
    fun createConfigFileBackup(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val propsMap = mutableMapOf<String, String>()
            configKeysToBackup.forEach { key ->
                propsMap[key] = PropertyUtils.get(key)
            }
            val success = BackupManager.createBackup(context, uri, propsMap)
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    // Hasil pengecekan: isSuccess, errorMessage, validDataToRestore
    fun validateAndRestoreFile(context: Context, uri: Uri, onResult: (Boolean, String, Map<String, String>?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val backupData = BackupManager.readBackup(context, uri)
            
            if (backupData == null) {
                withContext(Dispatchers.Main) { onResult(false, "Invalid or corrupted .zx backup file.", null) }
                return@launch
            }

            val currentSocType = PropertyUtils.get("persist.sys.azenithdebug.soctype")
            val backupSocType = backupData["persist.sys.azenithdebug.soctype"]

            if (currentSocType != backupSocType) {
                // Ambil nama dari chipset file backup tersebut
                val backupName = zx.azenith.ui.util.BackupManager.getSocName(backupSocType)
                
                // 👇 Pesan error custom sesuai dengan SOC dari file backup
                val errorMsg = "This backup file is for $backupName devices, restore aborted."
                
                withContext(Dispatchers.Main) { onResult(false, errorMsg, null) }
                return@launch
            }

            // Jika lolos validasi
            withContext(Dispatchers.Main) { onResult(true, "Valid Backup", backupData) }
        }
    }

    // Di dalam TweakViewModel.kt
    fun applyRestoreData(context: Context, backupData: Map<String, String>, onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupData.forEach { (key, value) ->
                if (key != "persist.sys.azenithdebug.soctype" && value.isNotEmpty()) {
                    PropertyUtils.set(key, value)
                }
            }
            
            Shell.cmd("touch /data/adb/modules/AZenith/reboot").exec()
            loadAllConfiguration(context)
            delay(1200) 
            
            withContext(Dispatchers.Main) { onFinished() }
        }
    }

    
    fun loadAllConfiguration(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            
            // 1. TUGAS INSTAN: Load semua property langsung di background.
            // Membaca system property sangat cepat, UI akan langsung memiliki data tanpa loading lama.
            launch {
                // Section 1 & FreqOffset (Prioritas)
                liteState = PropertyUtils.get("persist.sys.azenithconf.cpulimit") == "1"
                
                val savedOffset = PropertyUtils.get("persist.sys.azenithconf.freqoffset", "Disabled")
                freqOffsetIndex = when (savedOffset) {
                    "90" -> 1f; "80" -> 2f; "70" -> 3f; "60" -> 4f; "50" -> 5f; "40" -> 6f; else -> 0f
                }            
                
                // Section 4 & 5 (Bisa dibaca sekaligus karena sama-sama instan)
                preloadState = PropertyUtils.get("persist.sys.azenithconf.APreload") == "1"
                memKillerState = PropertyUtils.get("persist.sys.azenithconf.clearbg") == "1"
                appPriorState = PropertyUtils.get("persist.sys.azenithconf.iosched") == "1"
                dndState = PropertyUtils.get("persist.sys.azenithconf.dnd") == "1"
                fstrimState = PropertyUtils.get("persist.sys.azenithconf.fstrim") == "1"
                thermalState = PropertyUtils.get("persist.sys.azenithconf.thermalcore") == "1"

                currentRenderer = PropertyUtils.get("debug.hwui.renderer", "skiagl")
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                currentRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display.refreshRate.toInt() // Menggunakan cara direct seperti pembahasan sebelumnya
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.refreshRate.toInt()
                }
            }

            // 2. TUGAS BERAT (Shell/Root): Jalankan secara PARALEL
            // Menggunakan async agar proses baca Governor & I/O berjalan bersamaan (memotong waktu tunggu 50%)
            val govJob = async { loadGovernorsInternal() }
            val ioJob = async { loadIOSchedulersInternal() }
            
            // Tunggu kedua tugas root selesai
            govJob.await()
            ioJob.await()

            withContext(Dispatchers.Main) {
                isUiLoaded = true
            }
        }
    }

    private fun loadGovernorsInternal() {
        val result = Shell.cmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors").exec()
        if (result.isSuccess) {
            val govs = result.out.firstOrNull()?.trim()?.split("\\s+".toRegex()) ?: emptyList()
            val currentDefault = PropertyUtils.get("persist.sys.azenith.custom_default_cpu_gov").ifEmpty {
                PropertyUtils.get("persist.sys.azenith.default_cpu_gov")
            }
            val currentPowersave = PropertyUtils.get("persist.sys.azenith.custom_powersave_cpu_gov")

            availableGovernors = govs
            defaultGovIndex = govs.indexOf(currentDefault).coerceAtLeast(0)
            powersaveGovIndex = govs.indexOf(currentPowersave).coerceAtLeast(0)
        } else {
            availableGovernors = emptyList() // Fallback jika gagal baca sysfs
        }
    }

    private fun loadIOSchedulersInternal() {
        val candidates = listOf("mmcblk0", "mmcblk1", "sda", "sdb", "sdc")
        var validBlock = ""
        for (block in candidates) {
            if (Shell.cmd("test -e /sys/block/$block/queue/scheduler").exec().isSuccess) {
                validBlock = block
                break
            }
        }
        if (validBlock.isNotEmpty()) {
            val result = Shell.cmd("cat /sys/block/$validBlock/queue/scheduler").exec()
            if (result.isSuccess) {
                val rawOut = result.out.firstOrNull() ?: ""
                val schedulers = rawOut.replace("[", "").replace("]", "").trim().split("\\s+".toRegex())

                val currentBal = PropertyUtils.get("persist.sys.azenith.custom_default_balanced_IO").ifEmpty {
                    PropertyUtils.get("persist.sys.azenith.default_balanced_IO")
                }
                val currentPerf = PropertyUtils.get("persist.sys.azenith.custom_performance_IO")
                val currentEco = PropertyUtils.get("persist.sys.azenith.custom_powersave_IO")

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

    fun updateLiteMode(checked: Boolean) {
        liteState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.cpulimit", if (checked) "1" else "0")
        }
    }

    fun updateDefaultGovernor(index: Int) {
        defaultGovIndex = index
        val selectedGov = availableGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.custom_default_cpu_gov", selectedGov)
            val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
            if (currentProfile == "2") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").exec()
            }
        }
    }

    fun updatePowersaveGovernor(index: Int) {
        powersaveGovIndex = index
        val selectedGov = availableGovernors?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.custom_powersave_cpu_gov", selectedGov)
            val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
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
            PropertyUtils.set("persist.sys.azenithconf.freqoffset", propValue)
        }
    }

    fun updateBalancedIO(index: Int) {
        balancedIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.custom_default_balanced_IO", selectedIO)
            val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
            if (currentProfile == "2") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }

    fun updatePerformanceIO(index: Int) {
        performanceIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.custom_performance_IO", selectedIO)
            val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
            if (currentProfile == "1") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }

    fun updatePowersaveIO(index: Int) {
        powersaveIOIndex = index
        val selectedIO = availableIOSchedulers?.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.custom_powersave_IO", selectedIO)
            val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
            if (currentProfile == "3") {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").exec()
            }
        }
    }

    fun updatePreloadMode(checked: Boolean) {
        preloadState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.APreload", if (checked) "1" else "0")
        }
    }

    fun updateMemoryKiller(checked: Boolean) {
        memKillerState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.clearbg", if (checked) "1" else "0")
        }
    }

    fun updateAppPriority(checked: Boolean) {
        appPriorState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.iosched", if (checked) "1" else "0")
        }
    }

    fun updateDndMode(checked: Boolean) {
        dndState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.dnd", if (checked) "1" else "0")
        }
    }

    fun updateFstrim(checked: Boolean) {
        fstrimState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.fstrim", if (checked) "1" else "0")
        }
    }

    fun updateThermalCore(checked: Boolean) {
        thermalState = checked
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.thermalcore", if (checked) "1" else "0")
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setthermalcore ${if (checked) "1" else "0"}").exec()
        }
    }

    fun executeSetRenderer(reason: String, context: Context) {
        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrender $reason").submit {
            viewModelScope.launch {
                delay(1000)
                loadAllConfiguration(context)
            }
        }
    }

    fun executeSetRefreshRates(reason: String, context: Context) {
        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrefreshrates $reason").submit {
            viewModelScope.launch {
                delay(1000)
                loadAllConfiguration(context)
            }
        }
    }
}
