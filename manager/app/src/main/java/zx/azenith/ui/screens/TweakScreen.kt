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

package zx.azenith.ui.screens

import android.app.Activity
import android.os.Build
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
import zx.azenith.R
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.rounded.*
import zx.azenith.ui.component.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.superuser.Shell
import androidx.navigation.NavController
import android.content.Context
import android.view.WindowManager
import zx.azenith.ui.util.PropertyUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    var currentRenderer by remember { mutableStateOf<String?>(null) }
    var currentRefreshRate by remember { mutableStateOf<Int?>(null) } 
    
    val scope = rememberCoroutineScope()
    
    // Fungsi pembantu untuk refresh data UI
    val updateUiData = {
        // Ambil Renderer
        currentRenderer = PropertyUtils.get("debug.hwui.renderer", "Default")
        
        // Ambil Refresh Rate
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        currentRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.refreshRate.toInt()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate.toInt()
        }
    }
    
    // Load data awal
    LaunchedEffect(Unit) {
        updateUiData()
    }

    var showRendererDialog by remember { mutableStateOf(false) }
    var showRefreshRateDialog by remember { mutableStateOf(false) }
    
    RendererDialog(
        show = showRendererDialog,
        onDismiss = { showRendererDialog = false },
        onRenderer = { rendererReason ->
            scope.launch {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrender $rendererReason").submit()
                // 2. Langsung update UI setelah perintah dikirim
                delay(1000)
                updateUiData() 
                Toast.makeText(context, "Applied: $rendererReason", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    RefreshRatePickerDialog(
        show = showRefreshRateDialog,
        onDismiss = { showRefreshRateDialog = false },
        onRefreshRatePicker = { refreshrateReason ->
            scope.launch {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrefreshrates $refreshrateReason").submit()
                // 3. Langsung update UI setelah perintah dikirim
                delay(1000)
                updateUiData()
                Toast.makeText(context, "Applied Selected Refresh Rate", Toast.LENGTH_SHORT).show()
            }
        }
    )
        
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TweakScreenTopAppBar(scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                start = 16.dp,
                end = 16.dp,
                bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) { 
            item { SettingsSectionTitle(stringResource(R.string.section_rateandrender)) }
            if (currentRenderer != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.WebStories,
                            label = stringResource(R.string.refreshrates),
                            value = "${currentRefreshRate}Hz",
                            showArrow = true,
                            highlight = false
                        ) {
                            showRefreshRateDialog = true
                        }
            
                        ExpressiveTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SettingsSuggest,
                            label = stringResource(R.string.renderengine),
                            value = currentRenderer!!.uppercase(),
                            showArrow = true,
                            highlight = false
                        ) {
                            showRendererDialog = true
                        }
                    }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.section_additionalsettings)) }
            item {
                // State nullable
                var preloadState by remember { mutableStateOf<Boolean?>(null) }
                var liteState by remember { mutableStateOf<Boolean?>(null) }
                var memKillerState by remember { mutableStateOf<Boolean?>(null) }
                var appPriorState by remember { mutableStateOf<Boolean?>(null) }
                var dndState by remember { mutableStateOf<Boolean?>(null) }
                var fstrimState by remember { mutableStateOf<Boolean?>(null) }

                // Load state system properties
                LaunchedEffect(Unit) {
                                    
                    // Ambil semua state tweak
                    preloadState = PropertyUtils.get("persist.sys.azenithconf.APreload") == "1"
                    liteState = PropertyUtils.get("persist.sys.azenithconf.cpulimit") == "1"
                    memKillerState = PropertyUtils.get("persist.sys.azenithconf.clearbg") == "1"
                    appPriorState = PropertyUtils.get("persist.sys.azenithconf.iosched") == "1"
                    dndState = PropertyUtils.get("persist.sys.azenithconf.dnd") == "1"
                    fstrimState = PropertyUtils.get("persist.sys.azenithconf.fstrim") == "1"
                }

                
                if (preloadState != null && liteState != null && memKillerState != null && appPriorState != null && dndState != null && fstrimState != null) { 
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveSwitchItem(
                                    title = "Game Preload",
                                    summary = "Preload libraries at game start",
                                    checked = preloadState!!,
                                    onCheckedChange = { isChecked ->
                                        preloadState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.APreload", if (isChecked) "1" else "0")
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    title = "Perf Lite Mode",
                                    summary = "Reduce heating in performance profile by limiting tweaks",
                                    checked = liteState!!,
                                    onCheckedChange = { isChecked ->
                                        liteState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.cpulimit", if (isChecked) "1" else "0")
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    title = "Memory Killer",
                                    summary = "Clear ram usage at gamestart",
                                    checked = memKillerState!!,
                                    onCheckedChange = { isChecked ->
                                        memKillerState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.clearbg", if (isChecked) "1" else "0")
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    title = "App Priority Control",
                                    summary = "Increase running game I/O scheduling priority in Performance profiles",
                                    checked = appPriorState!!,
                                    onCheckedChange = { isChecked ->
                                        appPriorState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.iosched", if (isChecked) "1" else "0")
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    title = "DND Mode on Gaming",
                                    summary = "Enable DND in Performance Profile",
                                    checked = dndState!!,
                                    onCheckedChange = { isChecked ->
                                        dndState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.dnd", if (isChecked) "1" else "0")
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    title = "Trim Filesystem Partition",
                                    summary = "Trim unused blocks in system partitions to Increase I/O Performance",
                                    checked = fstrimState!!,
                                    onCheckedChange = { isChecked ->
                                        fstrimState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.fstrim", if (isChecked) "1" else "0")
                                    }
                                )
                            }
                        )
                    )
                } else {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
            item { SettingsSectionTitle(stringResource(R.string.section_addons)) }
            item {
                ExpressiveList(
                    content = listOf(
                        {
                            ExpressiveListItem(
                                onClick = {
                                    navController.navigate("bypasschg")
                                },
                                headlineContent = { Text(stringResource(R.string.bcharging)) },
                                supportingContent = { Text(stringResource(R.string.bcharging_desc)) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        },
                    )
                )
            }                        
        }
    }
}

@Composable
fun ExpressiveTile(
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
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
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
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Tweaks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
