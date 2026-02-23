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
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import android.os.FileObserver
import zx.azenith.R

object RootUtils {
    private const val MODULE_DIR = "/data/adb/modules/AZenith"
    private const val API_DIR_PATH = "/data/data/zx.azenith/API"
    private const val PROFILE_FILE_NAME = "current_profile"
    private const val PROFILE_PATH = "$API_DIR_PATH/$PROFILE_FILE_NAME"

    fun observeProfileRes(): Flow<Int> = callbackFlow {

        trySend(getCurrentProfileRes())

        val apiDir = File(API_DIR_PATH)
        
        if (!apiDir.exists()) {
            Shell.cmd("mkdir -p $API_DIR_PATH && chmod 755 $API_DIR_PATH").exec()
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
        val result = Shell.cmd("cat $PROFILE_PATH").exec()
        val content = if (result.isSuccess) result.out.firstOrNull()?.trim() else null
        
        return when (content) {
            "0" -> R.string.status_initializing
            "1" -> {
                val liteProp = Shell.cmd("getprop persist.sys.azenithconf.litemode").exec()
                val isLite = liteProp.out.firstOrNull()?.trim() == "1"                
                if (isLite) R.string.profile_perflite else R.string.Profile_Performance
            }
            "2" -> R.string.Profile_Balanced
            "3" -> R.string.Profile_ECO_mode
            else -> R.string.status_unknown
        }
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
        return Shell.getShell().isRoot
    }
    
    fun isModuleInstalled(): Boolean {
        val result = Shell.cmd("[ -d $MODULE_DIR ] && echo yes || echo no").exec()
        return result.isSuccess && result.out.firstOrNull() == "yes"
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
}
