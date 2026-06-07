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
    val bitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)
    private var isLoaded = false

    suspend fun init(context: Context) {
        if (isLoaded) return

        withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val drawable = wallpaperManager.drawable
                
                if (drawable != null) {

                    val intrinsicWidth = drawable.intrinsicWidth
                    val intrinsicHeight = drawable.intrinsicHeight
                    
                    val ratio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                    val targetHeight = 800
                    val targetWidth = (targetHeight * ratio).toInt()
                    
                    val bitmap = drawable.toBitmap(width = targetWidth, height = targetHeight).asImageBitmap()
                    bitmapState.value = bitmap
                }
            } catch (e: Exception) {
                // 
            } finally {
                isLoaded = true
            }
        }
    }
}
