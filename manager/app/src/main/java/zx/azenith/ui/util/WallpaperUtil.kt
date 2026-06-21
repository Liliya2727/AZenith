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

            } finally {
                isLoaded = true
            }
        }
    }
}
