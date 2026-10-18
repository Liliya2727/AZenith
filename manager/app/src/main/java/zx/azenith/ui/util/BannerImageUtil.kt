/*
 * Copyright (C) 2026-2027 Rapli
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


import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream


private const val PREFS_NAME = "settings"
private const val KEY_HEADER_IMAGE = "header_image_uri"
private const val KEY_ENABLE_BANNER = "enable_banner_image"
private const val KEY_BANNER_GRADIENT_ALPHA = "banner_gradient_alpha"

fun Context.saveMediaDirectly(sourceUri: Uri, extension: String): String? {
    return try {
        val bannerDir = File(filesDir, "banners")
        if (!bannerDir.exists()) bannerDir.mkdirs()

        val destFile = File(bannerDir, "banner_${System.currentTimeMillis()}.$extension")
        
        contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Context.deleteOldBannerFile(uriString: String?, excludingKey: String? = null) {
    if (uriString.isNullOrEmpty()) return
    try {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isStillReferenced = prefs.all.entries.any { (k, v) ->
            if (excludingKey != null && k == excludingKey) false
            else v == uriString
        }
        if (isStillReferenced) return

        val uri = Uri.parse(uriString)
        val path = uri.path ?: uriString 
        val file = File(path)
        
        if (file.exists() && file.absolutePath.startsWith(filesDir.absolutePath)) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun Context.getHeaderImage(): String? {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_HEADER_IMAGE, null)
}

fun Context.saveHeaderImage(uri: String) {
    val oldUri = getHeaderImage()
    
    if (oldUri != null && oldUri != uri) {
        deleteOldBannerFile(oldUri, excludingKey = KEY_HEADER_IMAGE)
    }

    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HEADER_IMAGE, uri)
        .apply()
}

fun Context.clearHeaderImage() {
    val oldUri = getHeaderImage()
    
    deleteOldBannerFile(oldUri, excludingKey = KEY_HEADER_IMAGE)

    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_HEADER_IMAGE)
        .apply()
}

fun Context.isBannerImageEnabled(): Boolean {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLE_BANNER, true)
}

fun Context.setBannerImageEnabled(enabled: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ENABLE_BANNER, enabled)
        .apply()
}

fun Context.getBannerGradientAlpha(): Float {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_BANNER_GRADIENT_ALPHA, 0.5f)
}

fun Context.setBannerGradientAlpha(alpha: Float) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_BANNER_GRADIENT_ALPHA, alpha)
        .apply()
}

fun Context.getGlobalGameWallpaperMode(): String {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("gamespace_global_game_wallpaper_mode", "custom") ?: "custom"
}

fun Context.setGlobalGameWallpaperMode(mode: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("gamespace_global_game_wallpaper_mode", mode)
        .apply()
}

fun Context.resolveGlobalGameWallpaperMode(packages: List<String>): String {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (packages.isEmpty()) {
        return prefs.getString("gamespace_global_game_wallpaper_mode", "custom") ?: "custom"
    }
    val hasCustomUri = packages.any { pkg ->
        !prefs.getString("game_wallpaper_uri_$pkg", null).isNullOrEmpty()
    }
    val explicitModes = packages.mapNotNull { pkg ->
        prefs.getString("game_wallpaper_mode_$pkg", null)
    }
    if (hasCustomUri || explicitModes.any { it == "custom" }) {
        return "custom"
    }
    if (explicitModes.size == packages.size) {
        val first = explicitModes.first()
        if (explicitModes.all { it == first }) {
            return first
        }
        return "custom"
    }
    val stored = prefs.getString("gamespace_global_game_wallpaper_mode", null)
    return stored ?: "custom"
}

fun Context.getGameWallpaperMode(packageName: String): String {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val hasUri = !prefs.getString("game_wallpaper_uri_$packageName", null).isNullOrEmpty()
    val fallback = if (hasUri) "custom" else (prefs.getString("gamespace_global_game_wallpaper_mode", "default") ?: "default")
    return prefs.getString("game_wallpaper_mode_$packageName", fallback) ?: fallback
}

fun Context.setGameWallpaperMode(packageName: String, mode: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString("game_wallpaper_mode_$packageName", mode)
        .putString("gamespace_global_game_wallpaper_mode", "custom")
        .apply()
}

fun Context.batchSetGameWallpaperMode(packages: List<String>, mode: String) {
    val editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    packages.forEach { pkg ->
        editor.putString("game_wallpaper_mode_$pkg", mode)
        if (mode == "default") {
            val oldUri = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("game_wallpaper_uri_$pkg", null)
            editor.remove("game_wallpaper_uri_$pkg")
            if (oldUri != null) {
                deleteOldBannerFile(oldUri, excludingKey = "game_wallpaper_uri_$pkg")
            }
        }
    }
    editor.putString("gamespace_global_game_wallpaper_mode", mode)
    editor.apply()
}

fun Context.batchSetGameWallpaper(packages: List<String>, uri: String) {
    val editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    packages.forEach { pkg ->
        editor.putString("game_wallpaper_mode_$pkg", "custom")
        editor.putString("game_wallpaper_uri_$pkg", uri)
        editor.putBoolean("game_wallpaper_enabled_$pkg", true)
    }
    editor.putString("gamespace_global_game_wallpaper_mode", "custom")
    editor.apply()
}

fun Context.batchResetGameWallpapers(packages: List<String>) {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    packages.forEach { pkg ->
        val oldUri = prefs.getString("game_wallpaper_uri_$pkg", null)
        editor.remove("game_wallpaper_mode_$pkg")
        editor.remove("game_wallpaper_uri_$pkg")
        editor.remove("game_wallpaper_enabled_$pkg")
        editor.remove("game_wallpaper_dim_$pkg")
        editor.remove("game_wallpaper_blur_$pkg")
        if (oldUri != null) {
            deleteOldBannerFile(oldUri, excludingKey = "game_wallpaper_uri_$pkg")
        }
    }
    editor.putString("gamespace_global_game_wallpaper_mode", "default")
    editor.apply()
}

fun Context.getGameWallpaperUri(packageName: String): String? {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("game_wallpaper_uri_$packageName", null)
}

fun Context.setGameWallpaperUri(packageName: String, uri: String?) {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val oldUri = prefs.getString("game_wallpaper_uri_$packageName", null)
    if (oldUri != null && oldUri != uri) {
        deleteOldBannerFile(oldUri, excludingKey = "game_wallpaper_uri_$packageName")
    }
    val editor = prefs.edit()
    if (uri != null) {
        editor.putString("game_wallpaper_uri_$packageName", uri)
        editor.putString("game_wallpaper_mode_$packageName", "custom")
        editor.putString("gamespace_global_game_wallpaper_mode", "custom")
    } else {
        editor.remove("game_wallpaper_uri_$packageName")
    }
    editor.apply()
}

fun Context.getGameWallpaperDim(packageName: String): Int {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("game_wallpaper_dim_$packageName", -1)
}

fun Context.setGameWallpaperDim(packageName: String, dim: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt("game_wallpaper_dim_$packageName", dim)
        .apply()
}

fun Context.getGameWallpaperBlur(packageName: String): Int {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("game_wallpaper_blur_$packageName", 0)
}

fun Context.setGameWallpaperBlur(packageName: String, blur: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt("game_wallpaper_blur_$packageName", blur)
        .apply()
}

fun Context.isGameWallpaperEnabled(packageName: String): Boolean {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean("game_wallpaper_enabled_$packageName", true)
}

fun Context.setGameWallpaperEnabled(packageName: String, enabled: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("game_wallpaper_enabled_$packageName", enabled)
        .apply()
}

fun Context.getHomeWallpaperBlur(): Int {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("gamespace_wallpaper_blur", 0)
}

fun Context.setHomeWallpaperBlur(blur: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt("gamespace_wallpaper_blur", blur)
        .apply()
}

fun Context.isHomeWallpaperEnabled(): Boolean {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("gamespace_wallpaper_enabled", prefs.getBoolean(KEY_ENABLE_BANNER, true))
}

fun Context.setHomeWallpaperEnabled(enabled: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("gamespace_wallpaper_enabled", enabled)
        .putBoolean(KEY_ENABLE_BANNER, enabled)
        .apply()
}
