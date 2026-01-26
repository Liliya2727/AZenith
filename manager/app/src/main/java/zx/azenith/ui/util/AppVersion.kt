package zx.azenith.ui.util

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "Unknown"

        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        
        "$versionName ($versionCode)"
    } catch (e: Exception) {
        "Unknown"
    }
}
