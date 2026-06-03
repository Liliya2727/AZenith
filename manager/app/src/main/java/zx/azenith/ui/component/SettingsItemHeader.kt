package zx.azenith.ui.component

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import zx.azenith.BuildConfig
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
    
    // 👇 HANYA PAKAI PADDING, JANGAN PAKAI CLIP & BACKGROUND DI SINI
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), 
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- BAGIAN KIRI: Wallpaper & Jam ---
        Box(
            modifier = Modifier
                .weight(0.45f)
                .aspectRatio(0.55f) 
                .clip(RoundedCornerShape(16.dp)) // Ini biarkan untuk clip gambarnya saja
        ) {
            Image(
                painter = painterResource(R.drawable.avatar), 
                contentDescription = "App Header Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hourFormat.format(time.time),
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = minuteFormat.format(time.time),
                    color = Color.White,
                    fontSize = 42.sp,
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
            AppInfoTextItem(title = "Build Date", value = "2026-06-03 16:22")
            AppInfoTextItem(title = "Version Name", value = BuildConfig.VERSION_NAME)
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
