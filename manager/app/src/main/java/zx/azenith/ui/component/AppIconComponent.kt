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

package zx.azenith.ui.component


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zx.azenith.ui.viewmodel.ApplistViewmodel


object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(150)

    fun get(packageName: String): ImageBitmap? = synchronized(cache) { cache.get(packageName) }

    fun clear() = synchronized(cache) { cache.evictAll() }


    suspend fun loadIcon(pm: PackageManager, appInfo: ApplicationInfo, targetSizePx: Int): ImageBitmap =
        withContext(Dispatchers.IO) {

            get(appInfo.packageName)?.let { return@withContext it }

            val drawable = try {
                appInfo.loadIcon(pm) ?: pm.defaultActivityIcon
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }



            val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)


            drawable.setBounds(0, 0, targetSizePx, targetSizePx)
            drawable.draw(canvas)

            val imageBitmap = bitmap.asImageBitmap()

            synchronized(cache) {
                cache.put(appInfo.packageName, imageBitmap)
            }

            return@withContext imageBitmap
        }
}


@Composable
fun AppIconImage(
    app: ApplistViewmodel.AppInfo,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val pm = context.packageManager
    

    val density = LocalDensity.current
    val targetSizePx = remember(size, density) {
        with(density) { size.roundToPx() }
    }
    

    var appBitmap by remember(app.packageName) { 
        mutableStateOf(AppIconCache.get(app.packageName)) 
    }


    LaunchedEffect(app.packageName, targetSizePx) {
        if (appBitmap == null) {
            app.packageInfo.applicationInfo?.let { appInfo ->
                try {
                    appBitmap = AppIconCache.loadIcon(pm, appInfo, targetSizePx)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.size(size)) {
        Crossfade(
            targetState = appBitmap,
            animationSpec = tween(durationMillis = 200),
            label = "IconFade"
        ) { icon ->
            if (icon == null) {

                PlaceHolderBox(Modifier.fillMaxSize())
            } else {

                Image(
                    bitmap = icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun PlaceHolderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))

            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) 
    )
}
