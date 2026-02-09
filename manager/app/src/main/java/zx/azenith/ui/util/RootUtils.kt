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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object RootUtils {
    private const val MODULE_DIR = "/data/adb/modules/AZenith"
    private const val API_DIR_PATH = "/data/data/zx.azenith/API"
    private const val PROFILE_FILE_NAME = "current_profile"
    private const val PROFILE_PATH = "$API_DIR_PATH/$PROFILE_FILE_NAME"

    fun observeProfile(): Flow<String> = callbackFlow {
        // Emit data awal
        trySend(getCurrentProfile())

        val apiDir = File(API_DIR_PATH)
        
        // Buat folder jika belum ada agar observer tidak error
        if (!apiDir.exists()) {
            Shell.cmd("mkdir -p $API_DIR_PATH && chmod 755 $API_DIR_PATH").exec()
        }

        // Pantau folder menggunakan constructor File (A10+)
        // Kita pantau MODIFY (isi berubah) dan CREATE (file diganti/dibuat ulang)
        val observer = object : FileObserver(apiDir, MODIFY or CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                if (path == PROFILE_FILE_NAME) {
                    trySend(getCurrentProfile())
                }
            }
        }

        observer.startWatching()
        awaitClose { observer.stopWatching() }
    }.flowOn(Dispatchers.IO)

    fun getCurrentProfile(): String {
        val result = Shell.cmd("cat $PROFILE_PATH").exec()
        val content = if (result.isSuccess) result.out.firstOrNull()?.trim() else null
        
        return when (content) {
            "0" -> "Initializing..."
            "1" -> {
                val liteProp = Shell.cmd("getprop persist.sys.azenithconf.litemode").exec()
                val isLite = liteProp.out.firstOrNull()?.trim() == "1"                
                if (isLite) "PerfLite" else "Performance"
            }
            "2" -> "Balanced"
            "3" -> "ECO Mode"
            else -> "Unknown"
        }
    }

    
    fun observeServiceStatus(): Flow<Pair<String, String>> = flow {
        var lastStatus: Pair<String, String>? = null
        while (true) {
            val currentStatus = getServiceStatus()
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

    fun getServiceStatus(): Pair<String, String> {
        val result = Shell.cmd("pidof sys.azenith-service").exec()
        return if (result.isSuccess) {
            val pid = result.out.firstOrNull() ?: ""
            "Alive" to pid
        } else {
            "Suspended" to ""
        }
    }
}

