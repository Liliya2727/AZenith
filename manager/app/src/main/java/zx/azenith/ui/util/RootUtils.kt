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

    // Gunakan Flow Polling karena FileObserver tidak tembus folder Root
    fun observeProfile(): Flow<String> = flow {
        var lastContent = ""
        while (true) {
            val currentContent = getCurrentProfile()
            // Hanya kirim data ke UI jika isinya berubah
            if (currentContent != lastContent) {
                emit(currentContent)
                lastContent = currentContent
            }
            // Cek setiap 2 detik (tidak boros baterai untuk satu file kecil)
            delay(2000) 
        }
    }.flowOn(Dispatchers.IO) // Jalankan di background thread
    
        // Observe Status Service secara Real-time
    fun observeServiceStatus(): Flow<Pair<String, String>> = flow {
        var lastStatus: Pair<String, String>? = null
        
        while (true) {
            val currentStatus = getServiceStatus()
            // Hanya emit jika status atau PID-nya berubah
            if (currentStatus != lastStatus) {
                emit(currentStatus)
                lastStatus = currentStatus
            }
            // Delay 2 atau 3 detik sudah cukup responsif
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)
    

    fun getCurrentProfile(): String {
        // Gunakan Shell.cmd agar pembacaan file benar-benar dilakukan sebagai Root
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
    
    // ... fungsi lainnya tetap sama
    fun isRootGranted(): Boolean {
        return Shell.getShell().isRoot
    }


    // 3. Ambil Service PID (pidof)
    // Return Pair(Status, PID)
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
