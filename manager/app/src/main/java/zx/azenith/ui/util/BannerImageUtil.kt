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

private const val PREFS_NAME = "settings"
private const val KEY_HEADER_IMAGE = "header_image_uri"
private const val KEY_ENABLE_BANNER = "enable_banner_image"
private const val KEY_BANNER_GRADIENT_ALPHA = "banner_gradient_alpha"

private fun Context.deleteOldBannerFile(uriString: String?) {
    if (uriString.isNullOrEmpty()) return
    try {
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
        deleteOldBannerFile(oldUri)
    }

    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HEADER_IMAGE, uri)
        .apply()
}

fun Context.clearHeaderImage() {
    val oldUri = getHeaderImage()
    
    deleteOldBannerFile(oldUri)

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
