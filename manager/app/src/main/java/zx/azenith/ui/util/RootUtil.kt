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

package zx.azenith.ui.util


import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import zx.azenith.R


object RootUtils {
    private const val MODULE_DIR = "/data/adb/modules/AZenith"
    private const val API_DIR_PATH = "/data/data/zx.azenith/API"
    private const val PROFILE_FILE_NAME = "current_profile"
    private const val PROFILE_PATH = "$API_DIR_PATH/$PROFILE_FILE_NAME"
    private const val DAEMON_API_DIR_PATH = "/data/adb/.config/AZenith/API"
    private const val DAEMON_PROFILE_PATH = "$DAEMON_API_DIR_PATH/$PROFILE_FILE_NAME"
    private const val MODES_FILE_NAME = "current_modes"
    private const val DAEMON_MODES_PATH = "$DAEMON_API_DIR_PATH/$MODES_FILE_NAME"

    /**
     * Read a file through the root shell. Returns null when the file is missing
     * or unreadable, which is the same branch every `Shell.cmd("cat …").exec()`
     * call site used to take on failure.
     */
    internal fun readRootFile(path: String): String? {
        return try {
            val file = SuFile(path)
            if (!file.exists()) return null
            file.newInputStream().bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    /** App-private mirror files can be read directly; only fall back to root. */
    private fun readDirectOrRoot(path: String): String? {
        val direct = try {
            File(path).takeIf { it.exists() }?.readText()?.trim()
        } catch (_: Exception) {
            null
        }
        return direct ?: readRootFile(path)
    }

    /**
     * One poller for both daemon API files, shared by every observer. The old
     * design started a separate 600 ms `SuFile` (root `dd` fork) watcher per
     * collector — roughly 12-14 forks/second with Home and Game Space open.
     */
    private val watcherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val daemonFileEvents: SharedFlow<String> = flow {
        var lastProfile: String? = null
        var lastModes: String? = null
        while (true) {
            val profile = readRootFile(DAEMON_PROFILE_PATH)?.trim()
            if (profile != null && profile != lastProfile) {
                lastProfile = profile
                emit(PROFILE_FILE_NAME)
            }
            val modes = readRootFile(DAEMON_MODES_PATH)?.trim()
            if (modes != null && modes != lastModes) {
                lastModes = modes
                emit(MODES_FILE_NAME)
            }
            delay(600)
        }
    }.shareIn(watcherScope, SharingStarted.WhileSubscribed(5_000), replay = 0)

    /** One `pidof` poll shared by all liveness collectors. */
    private val serviceStatusEvents: SharedFlow<Pair<Int, String>> = flow {
        var last: Pair<Int, String>? = null
        while (true) {
            val current = getServiceStatusRes()
            if (current != last) {
                emit(current)
                last = current
            }
            delay(2_000)
        }
    }.shareIn(watcherScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /**
     * Write a file through the root shell. Content is written verbatim -- a
     * caller replacing `Shell.cmd("echo $v > f")` must include the trailing
     * newline itself, because `echo` appended one.
     */
    internal fun writeRootFile(path: String, content: String) {
        try {
            val file = SuFile(path)
            val dir = SuFile(path.substringBeforeLast('/', ""))
            if (!dir.exists()) dir.mkdirs()
            val tmp = SuFile(dir, file.name + ".tmp_" + System.nanoTime())
            tmp.newOutputStream().use { it.write(content.toByteArray()) }
            tmp.renameTo(file)
        } catch (e: Exception) {
            // no-op
        }
    }

    /**
     * Create an empty file through the root shell, replacing `touch <path>`.
     */
    internal fun touchRootFile(path: String) {
        try {
            val file = SuFile(path)
            if (!file.exists()) {
                SuFile(path.substringBeforeLast('/', "")).mkdirs()
                file.createNewFile()
            }
        } catch (e: Exception) {
            // no-op
        }
    }

    /**
     * Whether a path exists through the root shell, replacing `test -e <path>`.
     */
    internal fun rootFileExists(path: String): Boolean {
        return try {
            SuFile(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a path through the root shell, replacing `rm -f <path>`. Missing is
     * not an error, matching `rm -f`.
     */
    internal fun deleteRootFile(path: String) {
        try {
            SuFile(path).delete()
        } catch (e: Exception) {
            // no-op
        }
    }

    private fun syncProfileState() {
        val apiDir = SuFile(API_DIR_PATH)
        if (!apiDir.exists()) {
            apiDir.mkdirs()
        }

        val daemonFile = SuFile(DAEMON_PROFILE_PATH)
        if (daemonFile.exists()) {
            val content = daemonFile.newInputStream().bufferedReader().use { it.readText() }
            // The mirror is written into the very directory FileObserver watches, so an
            // unconditional write re-fires the observer that called us -- ~20 root shells a
            // second, forever. Only write when the value actually moved.
            if (content == readMirror()) return
            writeRootFile(PROFILE_PATH, content)
        }
    }

    private fun readMirror(): String? = try {
        File(PROFILE_PATH).takeIf { it.exists() }?.readText()
    } catch (e: Exception) {
        null
    }

    fun getModuleVersionCode(): Int {
        // Was `grep '^versionCode=' … | cut -d= -f2` through a shell. Read the
        // file and take the line directly rather than spawning grep and cut.
        val content = readRootFile("$MODULE_DIR/module.prop")
            ?: return -1
        val line = content.lineSequence()
            .firstOrNull { it.startsWith("versionCode=") }
            ?: return -1
        return line.substringAfter("versionCode=").trim().toIntOrNull() ?: -1
    }

    data class GameInfo(val pkg: String?, val startTime: String?)

    fun observeGameInfo(): Flow<GameInfo> = flow {
        var lastInfo: GameInfo? = null
        while (true) {
            val raw = readDirectOrRoot("/data/data/zx.azenith/API/gameinfo")
            var currentInfo = GameInfo(null, null)

            if (!raw.isNullOrBlank()) {
                val lines = raw.lines()
                val firstLine = lines[0].split(" ")

                val pkg = firstLine.getOrNull(0)?.takeIf { it != "NULL" && it.isNotBlank() }
                val time = lines.find { it.startsWith("Time:") }?.substringAfter("Time:")?.trim()

                currentInfo = GameInfo(pkg, time)
            }

            if (currentInfo != lastInfo) {
                emit(currentInfo)
                lastInfo = currentInfo
            }
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    // Raw "0".."3" alongside the resolved label: "1" maps to two different strings depending on
    // litemode, so the dialog cannot match on the label to highlight the active row.
    fun observeProfileValue(): Flow<String> = callbackFlow {
        syncProfileState()

        trySend(getCurrentProfileValue())

        val job = launch {
            daemonFileEvents.collect { file ->
                if (file == PROFILE_FILE_NAME) {
                    syncProfileState()
                    trySend(getCurrentProfileValue())
                }
            }
        }

        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO)

    /**
     * Auto mode lives in AIenabled but is mirrored to current_modes by whoever
     * flips the toggle, so watch the mirror rather than polling a property the
     * app itself wrote -- that way a change made from the Settings tab or the
     * tile both reach the home screen immediately.
     */
    fun observeAutoMode(): Flow<String> = callbackFlow {
        trySend(getAutoModeValue())

        val job = launch {
            daemonFileEvents.collect { file ->
                if (file == MODES_FILE_NAME) trySend(getAutoModeValue())
            }
        }

        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun getAutoModeValue(): String =
        readRootFile(DAEMON_MODES_PATH)?.trim()
            ?: PropertyUtils.get("persist.sys.azenithconf.AIenabled")

    fun getCurrentProfileValue(): String {
        // The mirror is the app's own file; a plain read costs nothing, a SuFile read
        // spawns a root shell command every time a profile event lands.
        val appMirror = readMirror()?.trim()
        if (!appMirror.isNullOrEmpty()) return appMirror
        return readRootFile(DAEMON_PROFILE_PATH)?.trim().orEmpty()
    }

    fun observeProfileRes(): Flow<Int> = callbackFlow {
        syncProfileState()

        trySend(getCurrentProfileRes())

        val job = launch {
            daemonFileEvents.collect { file ->
                if (file == PROFILE_FILE_NAME) {
                    syncProfileState()
                    trySend(getCurrentProfileRes())
                }
            }
        }

        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO)

    fun getCurrentProfileRes(): Int {
        val content = readDirectOrRoot(PROFILE_PATH)?.trim()

        if (content.isNullOrBlank()) {
            return R.string.status_unknown
        }

        return when (content) {
            "0" -> R.string.status_initializing
            "1" -> {
                val isLite = PropertyUtils.get("persist.sys.azenithconf.litemode", "0") == "1"
                if (isLite) R.string.profile_perflite else R.string.Profile_Performance
            }
            "2" -> R.string.Profile_Balanced
            "3" -> R.string.Profile_ECO_mode
            else -> R.string.status_unknown
        }
    }

    fun requestRootAccess(): Boolean {
        val currentShell = Shell.getCachedShell()
        if (currentShell != null && !currentShell.isRoot) {
            currentShell.close()
        }
        return Shell.getShell().isRoot
    }

    fun observeServiceStatusRes(): Flow<Pair<Int, String>> = serviceStatusEvents

    fun isRootGranted(): Boolean {
        val currentShell = Shell.getCachedShell()
        if (currentShell != null && !currentShell.isRoot) {
            currentShell.close()
        }
        return Shell.getShell().isRoot
    }

    fun getRootManager(): String {
        return try {
            val suVer = Shell.cmd("su -v").exec().out.firstOrNull() ?: ""
            when {
                suVer.contains("KernelSU", ignoreCase = true) -> "KernelSU"
                suVer.contains("MAGISK", ignoreCase = true) -> "Magisk"
                suVer.contains("APatch", ignoreCase = true) -> "APatch"
                SuFile("/data/adb/ksu").exists() -> "KernelSU"
                SuFile("/data/adb/magisk").exists() -> "Magisk"
                SuFile("/data/adb/ap").exists() -> "APatch"
                suVer.isNotBlank() -> suVer.substringAfter(':').ifBlank { suVer }
                else -> "Superuser"
            }
        } catch (_: Exception) {
            "Superuser"
        }
    }

    fun isModuleInstalled(): Boolean {
        return SuFile(MODULE_DIR).exists()
    }

    fun getServiceStatusRes(): Pair<Int, String> {
        // Stays a shell call. The obvious alternative -- scanning /proc for the
        // daemon -- is much worse: libsu implements a SuFile read by running
        // `dd` (ShellBlockIO), so a scan would fork one process per /proc entry
        // on a device that runs a few hundred, every two seconds. One `pidof`
        // fork per poll is the cheaper shape.
        val result = Shell.cmd("pidof sys.azenith-service").exec()
        return if (result.isSuccess) {
            val pid = result.out.firstOrNull() ?: ""
            R.string.status_alive to pid
        } else {
            R.string.status_suspended to ""
        }
    }

    fun isUpdateApkAvailable(): Boolean {
        return SuFile("/data/adb/modules/AZenith/AZenith.apk").exists()
    }

    fun isModuleUpdatePendingReboot(): Boolean {
        return SuFile("/data/adb/modules/AZenith/update").exists()
    }

    /**
     * Re-asserts the app-ops the manager cannot take for granted after a boot.
     *
     * MIUI/HyperOS and other Chinese forks gate package visibility behind a vendor
     * app-op, and `pm grant` alone leaves the list showing only this app. The
     * app-ops are a parallel route, not a follow-up to the grant, so every
     * spelling is issued regardless of what the grant returned.
     *
     * The same forks can reset SYSTEM_ALERT_WINDOW, and without that app-op Game
     * Space can neither attach its overlay nor start its foreground service from
     * the daemon's background broadcast, so it is re-asserted here as well.
     */
    fun ensurePackageVisibility() {
        val cmds = mutableListOf(
            "appops set zx.azenith SYSTEM_ALERT_WINDOW allow",
            "pm grant zx.azenith android.permission.QUERY_ALL_PACKAGES",
            "pm grant zx.azenith com.android.permission.GET_INSTALLED_APPS",
            "appops set zx.azenith android:query_all_packages allow",
            "appops set zx.azenith query_all_packages allow",
            "appops set zx.azenith android:get_installed_apps allow",
            "appops set zx.azenith GET_INSTALLED_APPS allow",
            "appops set zx.azenith 10022 allow"
        )
        try {
            Shell.cmd(cmds.joinToString("\n") { "$it 2>/dev/null" }).exec()
        } catch (e: Exception) {
            // no-op
        }
    }
}
