package zx.azenith.ui.util

import android.app.WallpaperManager
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperCache {
    // Menyimpan bitmap dalam wujud State agar UI otomatis update kalau gambar sudah siap
    val bitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)
    private var isLoaded = false // Penanda agar tidak di-load berulang kali

    suspend fun init(context: Context) {
        if (isLoaded) return // Kalau sudah pernah di-load, langsung skip!

        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val drawable = wallpaperManager.drawable
                
                // Downscale tetap dilakukan agar hemat RAM
                val bitmap = drawable?.toBitmap(width = 400, height = 800)?.asImageBitmap()
                
                bitmapState.value = bitmap
            } catch (e: Exception) {
                // Abaikan jika error (misal permission belum ada)
            } finally {
                isLoaded = true
            }
        }
    }
}
