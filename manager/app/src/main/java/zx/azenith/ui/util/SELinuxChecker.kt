package zx.azenith.ui.util

import androidx.compose.runtime.Composable
import com.topjohnwu.superuser.io.SuFile

fun getSELinuxStatus(): String = SuFile("/sys/fs/selinux/enforce").run {
    when {
        !exists() -> "Disabled"
        !isFile -> "Unknown"
        !canRead() -> "Enforcing"
        else -> {
            val content = runCatching { 
                newInputStream().bufferedReader().use { it.readLine()?.trim() } 
            }.getOrNull()

            when (content) {
                "1" -> "Enforcing"
                "0" -> "Permissive"
                else -> "Unknown"
            }
        }
    }
}
