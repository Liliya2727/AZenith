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

package zx.azenith.ui.subscreens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.viewmodel.ApplistViewmodel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import zx.azenith.ui.viewmodel.AppSettingsViewModel
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.component.ExpressiveDropdownItem
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.tween
import zx.azenith.ui.util.getSupportedRefreshRates
import java.io.File
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.ui.platform.LocalDensity

@Composable
fun AppSettingsScreen(
    navController: NavController, 
    packageName: String?,
    viewModel: AppSettingsViewModel = viewModel(),
    appListViewModel: ApplistViewmodel = viewModel() 
) {
    val context = LocalContext.current
    val appDetails = remember(packageName) { getAppDetails(context, packageName) }
    val colorScheme = MaterialTheme.colorScheme
    LaunchedEffect(packageName) { 
        viewModel.loadConfig() 
    }

    val config = viewModel.fullConfig[packageName]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var localMasterOn by remember(config != null) { mutableStateOf(config != null) }
    

    var userToggled by remember { mutableStateOf(false) }

    val booleanModes = listOf(
        stringResource(R.string.default_label),
        stringResource(R.string.on_label),
        stringResource(R.string.off_label)
    )

    val rendererModes = listOf(
        stringResource(R.string.Renderer_Default),
        "SkiaVK",
        "SkiaGL"
    )

    val rendererValues = listOf(
        "default", 
        "skiavk",
        "skiagl"
    )

    val dynamicRefreshModes = remember { getSupportedRefreshRates(context) }

    fun getBoolIndex(v: String?): Int = when(v) {
        "true" -> 1
        "false" -> 2
        else -> 0
    }
    
    DisposableEffect(Unit) {
        onDispose {
            appListViewModel.loadApps(context, forceRefresh = true)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { 
            AppSettingsTopAppBar(
                scrollBehavior = scrollBehavior,
                onLaunchApp = {
                    packageName?.let { pkg ->
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Aplikasi tidak bisa dibuka", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenAppInfo = {
                    packageName?.let { pkg ->
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$pkg")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                onBack = { 
                    appListViewModel.loadApps(context, forceRefresh = true) 
                    navController.popBackStack() 
                }
            ) 
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ExpressiveList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf( 
                       {
                           ExpressiveInfoCard(
                               supportingContent = { Text(text = "App-specific settings will override global settings. Leave them as default to inherit global settings.") },
                               leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                               containerColor = colorScheme.surfaceContainerLow,
                               onClick = {}
                           )
                       }
                    )
                )
            }
        
        
            item { AppHeader(appDetails, packageName) }
            

            item {
                ExpressiveList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf {
                        ExpressiveSwitchItem(
                            icon = Icons.Rounded.PowerSettingsNew,
                            title = stringResource(R.string.master_switch),
                            summary = stringResource(R.string.master_switch_desc),
                            checked = localMasterOn,
                            onCheckedChange = { isChecked ->
                                userToggled = true // BARU: Tandai bahwa user yang mengubah state
                                localMasterOn = isChecked 
                                packageName?.let { pkg -> viewModel.toggleMasterSwitch(pkg, isChecked) } 
                            }
                        )
                    }
                )
            }
            
            item {
                AnimatedVisibility(
                    visible = localMasterOn,
                    // BARU: Jika bukan user yang nge-klik (misal baru buka app/loading), maka munculkan instan tanpa animasi
                    enter = if (userToggled) {
                        expandVertically(animationSpec = tween(400)) + fadeIn()
                    } else {
                        EnterTransition.None
                    },
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    val displayConfig = config ?: zx.azenith.ui.util.AppConfig() 
                    
                    Column {
                        

                
                        SectionHeader(stringResource(R.string.preferred_settings))
                        ExpressiveList(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            content = listOf(
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.Speed,
                                        title = stringResource(R.string.perf_lite_mode),
                                        summary = stringResource(R.string.perf_lite_mode_desc_short),
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.perf_lite_mode),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "perf_lite_mode", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.RocketLaunch,
                                        title = stringResource(R.string.game_preload),
                                        summary = stringResource(R.string.game_preload_desc),
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.game_preload),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "game_preload", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.SwapVerticalCircle,
                                        title = stringResource(R.string.app_priority),
                                        summary = stringResource(R.string.app_priority_desc),
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.app_priority),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "app_priority", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.DoNotDisturbOn,
                                        title = stringResource(R.string.dnd_mode),
                                        summary = stringResource(R.string.dnd_mode_desc),
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.dnd_on_gaming),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "dnd_on_gaming", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.WebStories,
                                        title = stringResource(R.string.refreshrates),
                                        summary = stringResource(R.string.refreshrates_desc),
                                        items = dynamicRefreshModes,
                                        selectedIndex = dynamicRefreshModes.indexOf(displayConfig.refresh_rate).coerceAtLeast(0),
                                        onItemSelected = { index ->
                                            val value = dynamicRefreshModes[index]
                                            packageName?.let { viewModel.updateSetting(it, "refresh_rate", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.Layers,
                                        title = stringResource(R.string.renderengine),
                                        summary = stringResource(R.string.renderengine_desc),
                                        items = rendererModes,
                                        selectedIndex = rendererValues.indexOfFirst { it.equals(displayConfig.renderer, ignoreCase = true) }.coerceAtLeast(0),
                                        onItemSelected = { index ->
                                            val value = rendererValues[index]
                                            packageName?.let { viewModel.updateSetting(it, "renderer", value) }
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = localMasterOn,
                    // BARU: Jika bukan user yang nge-klik (misal baru buka app/loading), maka munculkan instan tanpa animasi
                    enter = if (userToggled) {
                        expandVertically(animationSpec = tween(400)) + fadeIn()
                    } else {
                        EnterTransition.None
                    },
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    val displayConfig = config ?: zx.azenith.ui.util.AppConfig() 
                    
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpressiveList(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            content = listOf( 
                               {
                                   ExpressiveInfoCard(
                                       supportingContent = { Text(text = "By using per-app refresh rate, your screen refresh rate is fully handled by AZenith.") },
                                       leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                       containerColor = colorScheme.surfaceContainerLow,
                                       onClick = {}
                                   )
                               }
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 28.dp,
            end = 28.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun AppHeader(appDetails: Triple<String, android.content.pm.ApplicationInfo?, String>, packageName: String?) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current
    
    // Target ukuran ikon (100.dp kotak luar dikurangi padding 16.dp di setiap sisi = 68.dp)
    val iconSize = 68.dp
    val targetSizePx = remember(iconSize, density) {
        with(density) { iconSize.roundToPx() }
    }

    // Ambil dari cache jika sudah ada
    var appBitmap by remember(packageName) {
        mutableStateOf(packageName?.let { AppIconCache.get(it) })
    }

    // Load icon di background thread persis seperti ApplistScreen
    LaunchedEffect(packageName, targetSizePx) {
        if (appBitmap == null && packageName != null) {
            appDetails.second?.let { appInfo ->
                try {
                    appBitmap = AppIconCache.loadIcon(pm, appInfo, targetSizePx)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Crossfade(
                    targetState = appBitmap,
                    animationSpec = tween(durationMillis = 200),
                    label = "HeaderIconFade"
                ) { icon ->
                    if (icon == null) {
                        // Kotak loading transparan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        )
                    } else {
                        // Gambar asli
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = appDetails.first,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = packageName ?: stringResource(R.string.unknown_package),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = "v${appDetails.third}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


fun getAppDetails(context: android.content.Context, packageName: String?): Triple<String, android.content.pm.ApplicationInfo?, String> {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName ?: "", 0)
        val packageInfo = pm.getPackageInfo(packageName ?: "", 0)
        val label = pm.getApplicationLabel(info).toString()
        // Kita tidak lagi me-load icon (Drawable) di sini, cukup teruskan 'info' (ApplicationInfo)
        val version = packageInfo.versionName ?: context.getString(R.string.status_unknown)
        Triple(label, info, version)
    } catch (e: Exception) {
        Triple(context.getString(R.string.unknown_app), null, "0.0.0")
    }
}



@Composable
fun AppSettingsTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior, 
    onLaunchApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onBack: () -> Unit
) {
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
        LargeFlexibleTopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.app_settings_title),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            actions = {
                // Tombol Buka Aplikasi
                IconButton(onClick = onLaunchApp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Launch, 
                        contentDescription = "Launch App"
                    )
                }
                // Tombol Buka App Info (System Settings)
                IconButton(onClick = onOpenAppInfo) {
                    Icon(
                        imageVector = Icons.Rounded.Info, 
                        contentDescription = "App Info"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
