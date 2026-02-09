/*
 * Copyright (C) 2026-2027 Zexshia
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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.screens

import android.os.Build
import android.system.Os
import android.app.Activity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import zx.azenith.R
import zx.azenith.ui.component.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import zx.azenith.ui.util.*
import androidx.compose.material.icons.rounded.*
import coil.compose.AsyncImage
import com.topjohnwu.superuser.Shell
import android.widget.Toast
import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen() {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val serviceInfo by produceState(initialValue = "Suspended" to "") {
        value = RootUtils.getServiceStatus()
    }
    // Mengamati perubahan Flow secara otomatis
    val currentProfile by RootUtils.observeProfile()
        .collectAsState(initial = "Initializing...")
    
    val rootStatus by produceState(initialValue = false) {
        value = RootUtils.isRootGranted()
    }
    
    val moduleInstalled by produceState(initialValue = false) {
        value = RootUtils.isModuleInstalled()
    }

    val listState = rememberLazyListState()
    
    var showProfileDialog by remember { mutableStateOf(false) } // Tambahkan state ini
    var autoMode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Gunakan string agar lebih aman saat pengecekan prop
        autoMode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim()
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    
    ProfileDialog(
        show = showProfileDialog,
        onDismiss = { showProfileDialog = false },
        onProfile = { profileReason ->
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p $profileReason").submit()
            Toast.makeText(context, "Applying Selected Profile", Toast.LENGTH_SHORT).show()
        }
    )
    
    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { HomeTopAppBar(scrollBehavior = scrollBehavior) },
            containerColor = MaterialTheme.colorScheme.surface 
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BannerCard(
                        status = if (!moduleInstalled)
                            stringResource(R.string.module_not_installed)
                        else
                            serviceInfo.first,
                        pid = serviceInfo.second
                    ) { }
                }
                
                
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Token,
                            label = stringResource(R.string.current_profile),
                            value = currentProfile,
                            highlight = (currentProfile != stringResource(R.string.status_initializing)),
                            showArrow = autoMode == "0" // Panah muncul jika AI mati
                        ) {
                            if (autoMode == "0") {
                                showProfileDialog = true // Trigger dialog muncul
                            } else {
                                //
                            }
                        }
                        
                        InfoTile(
                            Modifier.weight(1f),
                            Icons.Rounded.Security,
                            stringResource(R.string.root_access),
                            if (rootStatus)
                                stringResource(R.string.root_granted)
                            else
                                stringResource(R.string.root_not_granted),
                            highlight = false
                        ) {}
                    }
                }
                item { DeviceInfoCard() }
                item { SupportCard { uriHandler.openUri("https://t.me/ZeshArch") } }
                item { LearnMoreCard { uriHandler.openUri("https://github.com/Liliya2727/AZenith") } }
            }
        }
    }
}

@Composable
fun BannerCard(status: String, pid: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val customBannerUri = remember { context.getHeaderImage() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f)
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            if (customBannerUri != null) {
                AsyncImage(
                    model = customBannerUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
                        
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.85f)
                            )
                        )
                    )
            )            
                        
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = when {
                        status == stringResource(R.string.module_not_installed) ->
                            colorScheme.errorContainer
                        status == stringResource(R.string.status_alive) ->
                            colorScheme.secondaryContainer
                        else ->
                            colorScheme.errorContainer
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status == stringResource(R.string.status_alive)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = stringResource(R.string.pid_format, pid),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    val colorScheme = MaterialTheme.colorScheme
    
    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        0.5f to colorScheme.surface.copy(alpha = 0.8f),
        0.6f to colorScheme.surface.copy(alpha = 0.7f),
        0.7f to colorScheme.surface.copy(alpha = 0.5f),
        0.8f to colorScheme.surface.copy(alpha = 0.4f),
        0.9f to colorScheme.surface.copy(alpha = 0.3f),
        1.0f to Color.Transparent 
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Gunakan statusBarsPadding agar gradien membungkus area jam/baterai
            .background(smoothGradient)
            .statusBarsPadding() 
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_name_styled),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            scrollBehavior = scrollBehavior,
            // Matikan insets bawaan karena kita sudah pakai statusBarsPadding() di Box
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}


@Composable
fun DeviceInfoCard() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    val uname = Os.uname()

    val kernelVer = remember { uname.release }
    val selinux = remember { getSELinuxStatus() }
    val appVer = remember { getAppVersion(context) }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = colorScheme.surfaceColorAtElevation(1.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .animateContentSize(animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallLeadingIcon(Icons.Outlined.Info)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.device_info), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }

            Spacer(Modifier.height(8.dp))

            DeviceInfoRow(
                stringResource(R.string.kernel_version),
                kernelVer
            )
            
            DeviceInfoRow(
                stringResource(R.string.device_name),
                "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            
            DeviceInfoRow(
                stringResource(R.string.azenith_version),
                appVer
            )

            if (isExpanded) {
                DeviceInfoRow(stringResource(R.string.fingerprint), Build.FINGERPRINT)
                DeviceInfoRow(stringResource(R.string.selinux_status), selinux)
                DeviceInfoRow(stringResource(R.string.instruction_sets), Build.SUPPORTED_ABIS.joinToString(", "))
                DeviceInfoRow(
                    stringResource(R.string.android_version),
                    "${Build.VERSION.RELEASE} API${Build.VERSION.SDK_INT}"
                )
            }
        }
    }
}



@Composable
fun DeviceInfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 

@Composable
fun InfoTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    showArrow: Boolean = false, // Parameter baru
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() },
        color = if (highlight) colorScheme.secondaryContainer else colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Column {
                Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            }
            
            if (showArrow) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).size(20.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun LearnMoreCard(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    Surface(
        shape = shape,
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape) 
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(Icons.Rounded.Info)

                Spacer(Modifier.width(12.dp))

                Text(
                    stringResource(R.string.learn_more),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.learn_more_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SupportCard(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    Surface(
        shape = shape,
        color = colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallLeadingIcon(Icons.Rounded.Favorite)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.support_us),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.support_us_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SmallLeadingIcon(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cs.primary.copy(alpha = 0.12f),
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier
                .padding(7.dp)
                .size(18.dp)
        )
    }
}
