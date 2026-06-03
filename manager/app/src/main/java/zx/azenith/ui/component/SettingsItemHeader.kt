package zx.azenith.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import zx.azenith.BuildConfig
import zx.azenith.ui.util.*
import zx.azenith.R

@Composable
fun AppInfoHeaderContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            time = Calendar.getInstance()
        }
    }
    
    val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
    val minuteFormat = SimpleDateFormat("mm", Locale.getDefault())
    
    val buildDateString = remember {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        formatter.format(Date(BuildConfig.BUILD_TIME))
    }

    // 👇 CUKUP BACA DARI CACHE GLOBAL, NGGAK PERLU LOAD LAGI!
    val wallpaperBitmap by WallpaperCache.bitmapState
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), 
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- BAGIAN KIRI: Wallpaper & Jam ---
        Box(
            modifier = Modifier
                .weight(0.45f)
                .aspectRatio(0.48f) 
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) 
                .padding(1.dp) 
                .clip(RoundedCornerShape(15.dp)) 
                .background(MaterialTheme.colorScheme.surfaceVariant) 
        ) {
            // Render Wallpaper dari Cache
            if (wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap!!, 
                    contentDescription = "Device Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Jam Overlay
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hourFormat.format(time.time),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 38.sp, 
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = minuteFormat.format(time.time),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.offset(y = (-12).dp)
                )
            }
        }

        // --- BAGIAN KANAN: Detail Informasi ---
        Column(
            modifier = Modifier
                .weight(0.55f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppInfoTextItem(title = "App Name", value = context.getString(R.string.app_name))
            AppInfoTextItem(title = "Author", value = "Zexshia")
            AppInfoTextItem(title = "Build Date", value = buildDateString)
            AppInfoTextItem(title = "Version Code", value = BuildConfig.VERSION_CODE.toString())
            AppInfoTextItem(title = "Package Name", value = context.packageName)
        }
    }
}


@Composable
private fun AppInfoTextItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
