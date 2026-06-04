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

// 1. Singleton Cache ala KernelSU yang menyimpan ImageBitmap langsung
object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(150) // Max 150 ikon di memori

    fun get(packageName: String): ImageBitmap? = synchronized(cache) { cache.get(packageName) }

    fun clear() = synchronized(cache) { cache.evictAll() }

    // Fungsi loading ikon yang kebal crash berkat draw limit Canvas
    suspend fun loadIcon(pm: PackageManager, appInfo: ApplicationInfo, targetSizePx: Int): ImageBitmap =
        withContext(Dispatchers.IO) {
            // Cek ulang cache barangkali thread lain sudah mendahului
            get(appInfo.packageName)?.let { return@withContext it }

            val drawable = try {
                appInfo.loadIcon(pm) ?: pm.defaultActivityIcon
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }

            // GAYA KERNELSU: Segede apa pun ukuran file asli icon di APK-nya (bahkan 158MB sekalipun),
            // Kita batasi alokasi memorinya di Canvas kecil ini (targetSizePx)
            val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Paksa drawable untuk menggambar dirinya sendiri agar pas ke dalam frame kecil targetSizePx
            drawable.setBounds(0, 0, targetSizePx, targetSizePx)
            drawable.draw(canvas)

            val imageBitmap = bitmap.asImageBitmap()

            synchronized(cache) {
                cache.put(appInfo.packageName, imageBitmap)
            }

            return@withContext imageBitmap
        }
}

// 2. Komponen UI persis KernelSU (pakai Crossfade & Placeholder)
@Composable
fun AppIconImage(
    app: ApplistViewmodel.AppInfo,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    // Hitung ukuran target px secara dinamis berdasarkan densitas layar HP
    val density = LocalDensity.current
    val targetSizePx = remember(size, density) {
        with(density) { size.roundToPx() }
    }
    
    // Cek cache dulu secara instan
    var appBitmap by remember(app.packageName) { 
        mutableStateOf(AppIconCache.get(app.packageName)) 
    }

    // Load di background coroutine menggunakan Dispatchers.IO yang sudah dibungkus di loadIcon
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
            animationSpec = tween(durationMillis = 200), // Fade effect halus 200ms
            label = "IconFade"
        ) { icon ->
            if (icon == null) {
                // Tampilkan kotak kerangka (placeholder) ala KernelSU saat masih loading
                PlaceHolderBox(Modifier.fillMaxSize())
            } else {
                // Tampilkan gambar asli jika sudah siap
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
            // Warna kotak loading transparan dikit biar nyatu sama tema gelap/terang
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) 
    )
}
