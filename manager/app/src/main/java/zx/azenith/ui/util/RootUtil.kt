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


import android.os.FileObserver
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import zx.azenith.R


object RootUtils {
    private const val MODULE_DIR = "/data/adb/modules/AZenith"
    private const val API_DIR_PATH = "/data/data/zx.azenith/API"
    private const val PROFILE_FILE_NAME = "current_profile"
    private const val PROFILE_PATH = "$API_DIR_PATH/$PROFILE_FILE_NAME"
    private const val DAEMON_PROFILE_PATH = "/data/adb/.config/AZenith/API/current_profile"

    private fun readRootFile(path: String): String? {
        return try {
            val file = SuFile(path)
            if (!file.exists()) return null
            file.newInputStream().bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    private fun writeRootFile(path: String, content: String) {
        try {
            SuFile(path).newOutputStream().use { it.write(content.toByteArray()) }
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
            writeRootFile(PROFILE_PATH, content)
        }
    }

    fun getModuleVersionCode(): Int {
        val result = Shell.cmd("grep '^versionCode=' /data/adb/modules/AZenith/module.prop | cut -d= -f2").exec().out
        return result.firstOrNull()?.trim()?.toIntOrNull() ?: -1
    }

    data class GameInfo(val pkg: String?, val startTime: String?)

    fun observeGameInfo(): Flow<GameInfo> = flow {
        var lastInfo: GameInfo? = null
        while (true) {
            val raw = readRootFile("/data/data/zx.azenith/API/gameinfo")
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

    fun observeProfileRes(): Flow<Int> = callbackFlow {
        syncProfileState()

        trySend(getCurrentProfileRes())

        val apiDir = File(API_DIR_PATH)
        if (!apiDir.exists()) {
            SuFile(API_DIR_PATH).mkdirs()
        }

        val observer = object : FileObserver(apiDir, MODIFY or CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                if (path == PROFILE_FILE_NAME) {
                    trySend(getCurrentProfileRes())
                }
            }
        }

        observer.startWatching()
        awaitClose { observer.stopWatching() }
    }.flowOn(Dispatchers.IO)

    fun getCurrentProfileRes(): Int {
        val content = readRootFile(PROFILE_PATH)

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

    fun observeServiceStatusRes(): Flow<Pair<Int, String>> = flow {
        var lastStatus: Pair<Int, String>? = null
        while (true) {
            val currentStatus = getServiceStatusRes()
            if (currentStatus != lastStatus) {
                emit(currentStatus)
                lastStatus = currentStatus
            }
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    fun isRootGranted(): Boolean {
        val currentShell = Shell.getCachedShell()
        if (currentShell != null && !currentShell.isRoot) {
            currentShell.close()
        }
        return Shell.getShell().isRoot
    }

    fun isModuleInstalled(): Boolean {
        return SuFile(MODULE_DIR).exists()
    }

    fun getServiceStatusRes(): Pair<Int, String> {
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
}
