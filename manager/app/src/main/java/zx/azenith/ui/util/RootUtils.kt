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

object RootUtils {
    private const val PROFILE_PATH = "/data/adb/.config/AZenith/API/current_profile"
    private const val MODULE_DIR = "/data/adb/modules/AZenith"

    fun observeProfile(): Flow<String> = flow {
        var lastContent = ""
        while (true) {
            val currentContent = getCurrentProfile()
            if (currentContent != lastContent) {
                emit(currentContent)
                lastContent = currentContent
            }
            delay(2000) 
        }
    }.flowOn(Dispatchers.IO)
    
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

    fun getCurrentProfile(): String {
        val result = Shell.cmd("cat $PROFILE_PATH").exec()
        val content = if (result.isSuccess) result.out.firstOrNull()?.trim() else null
        
        return when (content) {
            "0" -> "Initializing"
            "1" -> "Performance"
            "2" -> "Balanced"
            "3" -> "ECO Mode"
            else -> "Unknown"
        }
    }
    
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
