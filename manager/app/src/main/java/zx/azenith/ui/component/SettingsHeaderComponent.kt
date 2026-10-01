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


import android.os.SystemClock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.util.*


@Composable
fun AppInfoHeaderContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // The clock only needs minute granularity, so the tick is aligned to the next
    // minute boundary instead of firing every second. The 1 Hz tick that used to
    // live here forced a full frame every second, on this screen and on every
    // other screen the pager keeps composed alongside it.
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            time = now
            delay(60_000L - (now.timeInMillis % 60_000L))
        }
    }
    
    val hourFormat = remember { SimpleDateFormat("HH", Locale.getDefault()) }
    val minuteFormat = remember { SimpleDateFormat("mm", Locale.getDefault()) }
    
    val buildDateString = remember {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        formatter.format(Date(BuildConfig.BUILD_TIME))
    }

    val wallpaperBitmap by WallpaperCache.bitmapState
    

    // Uptime is second-granular, so it does need a 1 Hz tick, but it must not
    // invalidate this whole header. LiveUptime owns its own state and is the
    // only thing that recomposes once a second; the wall clock, the wallpaper
    // and the six build-info rows above stay untouched. Seeding the state from
    // elapsedRealtime (rather than starting empty) keeps the value correct on
    // the very first composition, so there is no second relayout to fill it in.
    val uptimeMillis = SystemClock.elapsedRealtime()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), 
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .heightIn(max = 280.dp) 
                .aspectRatio(0.48f) 
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) 
                .padding(2.dp) 
                .clip(RoundedCornerShape(15.dp)) 
                .background(MaterialTheme.colorScheme.surfaceVariant) 
        ) {

            if (wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap!!, 
                    contentDescription = stringResource(R.string.cd_wallpaper),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hourFormat.format(time.time),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 44.sp, 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = minuteFormat.format(time.time),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.offset(y = (-12).dp)
                )
            }
        }


        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppInfoTextItem(title = stringResource(R.string.app_name_settings), value = context.getString(R.string.app_name))
            AppInfoTextItem(title = stringResource(R.string.str_author), value = stringResource(R.string.str_archhaven_devs))
            AppInfoTextItem(title = stringResource(R.string.str_build_date), value = buildDateString)
            AppInfoTextItem(title = stringResource(R.string.str_version_code), value = BuildConfig.VERSION_CODE.toString())
            AppInfoTextItem(title = stringResource(R.string.str_package_name), value = context.packageName)

            LiveUptime(uptimeMillis)
        }
    }
}

/**
 * Device uptime as a live "1h 23m 45s" value.
 *
 * Split out of [AppInfoHeaderContent] so the unavoidable once-a-second update
 * invalidates only the row that displays it. The state is seeded from
 * elapsedRealtime during the first composition rather than left empty, so the
 * row renders its real value immediately and does not need a second pass.
 */
@Composable
private fun LiveUptime(initialElapsedRealtime: Long, modifier: Modifier = Modifier) {
    var elapsed by remember(initialElapsedRealtime) {
        mutableStateOf(formatUptime(initialElapsedRealtime))
    }
    LaunchedEffect(Unit) {
        while (true) {
            // Wake on the second boundary so the value cannot drift up to a
            // second behind the status-bar clock.
            val now = SystemClock.elapsedRealtime()
            elapsed = formatUptime(now)
            delay(1_000L - (now % 1_000L))
        }
    }
    AppInfoTextItem(
        title = stringResource(R.string.str_device_uptime),
        value = elapsed
    )
}

private fun formatUptime(elapsedRealtime: Long): String {
    val totalSeconds = elapsedRealtime / 1000
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (days > 0) {
        "${days}d ${hours}h ${minutes}m ${seconds}s"
    } else {
        "${hours}h ${minutes}m ${seconds}s"
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
