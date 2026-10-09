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


import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext


object WallpaperCache {
    val bitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)

    /**
     * Guards the expensive decode, and is only set on success. It used to be set
     * in a `finally`, which meant a load that threw was remembered as done: the
     * wallpaper is fetched on first composition, before get-started has granted
     * the storage permissions it needs, so the read failed, the cache locked
     * itself empty, and the header stayed blank until the process restarted.
     */
    private var isLoaded = false

    /** Set while a load is in flight so concurrent callers await the same one. */
    private var inFlight: Deferred<ImageBitmap?>? = null

    suspend fun init(context: Context) {
        if (isLoaded) return
        inFlight?.let { runCatching { it.await() }; if (isLoaded) return }

        val job = scope.async {
            withContext(Dispatchers.IO) {
                decodeWallpaper(context)
            }
        }
        inFlight = job

        // A failed decode must not be cached, or the missing value sticks for
        // the life of the process and only a restart clears it.
        bitmapState.value = runCatching { job.await() }.getOrNull()
        if (bitmapState.value != null) isLoaded = true
        inFlight = null
    }

    private fun decodeWallpaper(context: Context): ImageBitmap? {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val drawable = wallpaperManager.drawable
        if (drawable != null) {
            val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
            val targetHeight = 800
            val targetWidth = (targetHeight * ratio).toInt()
            return drawable.toBitmap(width = targetWidth, height = targetHeight).asImageBitmap()
        }

        // On a live wallpaper the platform returns null here, although a static
        // image still exists behind the scene (the live wallpaper engine keeps
        // a bitmap for launcher/lock). AOSP's fallback is the built-in
        // `default_wallpaper`, but some vendors strip it, which is exactly the
        // blank header on this device. Read the engine's own current bitmap
        // through the root shell before giving up.
        return decodeRootWallpaper() ?: readProviderFiles()
    }

    private fun decodeRootWallpaper(): ImageBitmap? {
        val rootBitmap = readRootBitmap("/data/system/users/0/wallpaper")
        return rootBitmap?.let { decodeToTarget(it) }
    }

    private fun readProviderFiles(): ImageBitmap? {
        val dir = "/data/user_de/0/com.transsion.livewallpaper.custompictorial/files"
        val providerFiles = listOf(
            "$dir/wallpaper_launcher.jpg",
            "$dir/wallpaper_keyguard.jpg",
            "$dir/normal_fg_20261008074830.png",
        )
        for (path in providerFiles) {
            val rootBitmap = readRootBitmap(path) ?: continue
            val decoded = decodeToTarget(rootBitmap) ?: continue
            return decoded
        }
        return null
    }

    /** Reads one file through the root shell and decodes it, bounded in size. */
    private fun readRootBitmap(path: String): Bitmap? {
        return try {
            val file = SuFile(path)
            if (!file.exists()) return null
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            file.newInputStream().use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null
            var scale = 1
            while (options.outHeight / (scale * 2) >= 800) scale *= 2
            val opts = options
            opts.inSampleSize = scale
            opts.inJustDecodeBounds = false
            file.newInputStream().use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeToTarget(bitmap: Bitmap): ImageBitmap? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetHeight = 800
        val targetWidth = (targetHeight * ratio).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        return scaled.asImageBitmap()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
