package zx.azenith.ui.util

import android.annotation.SuppressLint
import com.topjohnwu.superuser.Shell

object PropertyUtils {
    @SuppressLint("PrivateApi")
    fun get(key: String, def: String = ""): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java, String::class.java)
            get.invoke(null, key, def) as String
        } catch (e: Exception) {
            def
        }
    }

    @SuppressLint("PrivateApi")
    fun set(key: String, value: String) {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val set = systemProperties.getMethod("set", String::class.java, String::class.java)
            set.invoke(null, key, value)
        } catch (e: Exception) {
            // Gunakan Shell jika reflection gagal (karena permission)
            Shell.cmd("setprop $key $value").submit()
        }
    }
}
