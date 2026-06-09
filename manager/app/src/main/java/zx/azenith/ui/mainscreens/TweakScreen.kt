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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.mainscreens

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.LargeFlexibleTopAppBar
import zx.azenith.ui.viewmodel.TweakViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


@Composable
fun TweakScreen(
    navController: NavController,
    viewModel: TweakViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colorScheme = MaterialTheme.colorScheme
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }
    var showRefreshRateDialog by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<Map<String, String>?>(null) }
    
    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingRestoreData?.let { data ->
                scope.launch {
                    loadingDialog.withLoading {
                        viewModel.applyRestoreData(context, data) {
                            scope.launch { snackbarHostState.showSnackbar("Configuration restored successfully!") }
                        }
                    }
                }
            }
        },
        onDismiss = { pendingRestoreData = null }
    )

    LoadingDialogHost(handle = loadingDialog)
    ConfirmDialogHost(handle = confirmDialog)

    // Launcher untuk Buat File (Backup)
    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            showBackupRestoreSheet = false
            scope.launch {
                loadingDialog.withLoading {
                    viewModel.createConfigFileBackup(context, it) { success ->
                        scope.launch {
                            if (success) snackbarHostState.showSnackbar("Backup saved successfully!")
                            else snackbarHostState.showSnackbar("Failed to create backup.")
                        }
                    }
                }
            }
        }
    }
    
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showBackupRestoreSheet = false
            scope.launch {
                loadingDialog.withLoading {
                    viewModel.validateAndRestoreFile(context, it) { isValid, message, data ->
                        if (isValid && data != null) {
                            // Tampilkan dialog konfirmasi kalau sukses divalidasi
                            pendingRestoreData = data
                            val socName = zx.azenith.ui.util.BackupManager.getSocName(data["persist.sys.azenithdebug.soctype"])
                            confirmDialog.showConfirm(
                                title = "Restore Configuration?",
                                content = "A valid AZenith backup for $socName was found. Are you sure you want to overwrite your current settings?",
                                confirm = "Restore",
                                dismiss = "Cancel"
                            )
                        } else {
                            // Tampilkan dialog error (Chipset beda / file korup)
                            confirmDialog.showConfirm(
                                title = "Restore Failed",
                                content = message,
                                confirm = "OK",
                                dismiss = null // Hilangkan tombol cancel
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllConfiguration(context)
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TweakScreenTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onMoreClick = { showBackupRestoreSheet = true }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                )
            },
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
            
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpressiveList(
                        content = listOf( 
                            {
                                ExpressiveInfoCard(
                                    supportingContent = { Text(text = "These settings apply to all enabled apps by default. You can override them for specific apps in the Applist.") },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                    containerColor = colorScheme.surfaceContainerLow,
                                    onClick = {}
                                )
                            }
                        )
                    )
                }

                item { TweaksSectionTitle(text = "Performance") }
                item {
                    var socType by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(Unit) {
                        socType = PropertyUtils.get("persist.sys.azenithdebug.soctype")
                    }
                    if (socType != null && viewModel.liteState != null) {
                        val isMediaTek = socType == "1"
                        val available = false
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Speed,
                                        title = stringResource(R.string.perf_lite_mode),
                                        summary = stringResource(R.string.perf_lite_mode_desc),
                                        checked = viewModel.liteState!!,
                                        onCheckedChange = { viewModel.updateLiteMode(it) }
                                    )
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (available) 1f else 0.4f)) {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Filled.ArtTrack) },
                                            onClick = { 
                                                if (available) {
                                                    navController.navigate("FasScreen") 
                                                }
                                            },
                                            headlineContent = { Text(text = "Frame Aware Scheduling (FAS)" ) },
                                            supportingContent = { 
                                                Text(
                                                    text = if (available)
                                                        "Frame aware scheduling for Android" 
                                                    else
                                                        "Unavailable"
                                                ) 
                                            },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    }
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (isMediaTek) 1f else 0.4f)) {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Filled.Speed) },
                                            onClick = { 
                                                if (isMediaTek) {
                                                    navController.navigate("fpsgoscreen") 
                                                }
                                            },
                                            headlineContent = { Text(text = "FPSGO Settings") },
                                            supportingContent = { 
                                                Text(
                                                    text = if (isMediaTek) 
                                                        "Frame Per Second GO Settings for MediaTek" 
                                                    else 
                                                        "This option is only available for MediaTek Devices"
                                                ) 
                                            },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                        )
                                    }
                                }
                            )
                        )
                    } else {
                        SectionLoadingIndicator()
                    }
                }
                
                item { TweaksSectionTitle(stringResource(R.string.section_additionalsettings)) }
                item {
                    if (viewModel.preloadState != null && 
                        viewModel.memKillerState != null && 
                        viewModel.appPriorState != null && 
                        viewModel.dndState != null && 
                        viewModel.fstrimState != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.RocketLaunch,
                                        title = stringResource(R.string.game_preload),
                                        summary = stringResource(R.string.game_preload_desc),
                                        checked = viewModel.preloadState!!,
                                        onCheckedChange = { viewModel.updatePreloadMode(it) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.CleaningServices,
                                        title = stringResource(R.string.memory_killer),
                                        summary = stringResource(R.string.memory_killer_desc),
                                        checked = viewModel.memKillerState!!,
                                        onCheckedChange = { viewModel.updateMemoryKiller(it) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.SwapVerticalCircle,
                                        title = stringResource(R.string.app_priority_control),
                                        summary = stringResource(R.string.app_priority_control_desc),
                                        checked = viewModel.appPriorState!!,
                                        onCheckedChange = { viewModel.updateAppPriority(it) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.DoNotDisturbOn,
                                        title = stringResource(R.string.dnd_mode_gaming),
                                        summary = stringResource(R.string.dnd_mode_gaming_desc),
                                        checked = viewModel.dndState!!,
                                        onCheckedChange = { viewModel.updateDndMode(it) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Outlined.ContentCut,
                                        title = stringResource(R.string.trim_filesystem),
                                        summary = stringResource(R.string.trim_filesystem_desc),
                                        checked = viewModel.fstrimState!!,
                                        onCheckedChange = { viewModel.updateFstrim(it) }
                                    )
                                }
                            )
                        )
                    } else {
                        SectionLoadingIndicator()
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (viewModel.currentRefreshRate != null && viewModel.currentRenderer != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExpressiveTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.WebStories,
                                label = stringResource(R.string.refreshrates),
                                value = "${viewModel.currentRefreshRate}Hz",
                                showArrow = true,
                                highlight = false
                            ) {
                                showRefreshRateDialog = true
                            }

                            ExpressiveTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SettingsSuggest,
                                label = stringResource(R.string.renderengine),
                                value = viewModel.currentRenderer!!.uppercase(),
                                showArrow = true,
                                highlight = false
                            ) {
                                showRendererDialog = true
                            }
                        }
                    } else {
                        SectionLoadingIndicator()
                    }
                }

                item { TweaksSectionTitle(stringResource(R.string.section_CPUSettings)) }
                item {
                    if (viewModel.defaultGovIndex != null && 
                        viewModel.powersaveGovIndex != null && 
                        viewModel.freqOffsetIndex != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Outlined.Water,
                                        title = stringResource(R.string.default_cpu_gov),
                                        summary = stringResource(R.string.default_cpu_gov_desc),
                                        items = viewModel.availableGovernors ?: emptyList(),
                                        selectedIndex = viewModel.defaultGovIndex!!,
                                        onItemSelected = { viewModel.updateDefaultGovernor(it) }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Outlined.EnergySavingsLeaf,
                                        title = stringResource(R.string.powersave_cpu_gov),
                                        summary = stringResource(R.string.powersave_cpu_gov_desc),
                                        items = viewModel.availableGovernors ?: emptyList(),
                                        selectedIndex = viewModel.powersaveGovIndex!!,
                                        onItemSelected = { viewModel.updatePowersaveGovernor(it) }
                                    )
                                },
                                {
                                    FreqLimitSliderItem(
                                        icon = Icons.Outlined.Tune,
                                        initialValue = viewModel.freqOffsetIndex!!,
                                        labels = viewModel.offsetLabels,
                                        onSaved = { viewModel.saveFreqOffset(it) }
                                    )
                                }
                            )
                        )
                    } else {
                        SectionLoadingIndicator()
                    }
                }

                item { TweaksSectionTitle(stringResource(R.string.io_settings)) }
                item {
                    if (viewModel.availableIOSchedulers == null) {
                        SectionLoadingIndicator()
                    } else if (viewModel.availableIOSchedulers!!.isNotEmpty()) {
                        if (viewModel.balancedIOIndex != null && 
                            viewModel.performanceIOIndex != null && 
                            viewModel.powersaveIOIndex != null) {
                            ExpressiveList(
                                content = listOf(
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.Water,
                                            title = stringResource(R.string.balanced_io_scheduler),
                                            summary = stringResource(R.string.balanced_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.balancedIOIndex!!,
                                            onItemSelected = { viewModel.updateBalancedIO(it) }
                                        )
                                    },
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.OfflineBolt,
                                            title = stringResource(R.string.performance_io_scheduler),
                                            summary = stringResource(R.string.performance_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.performanceIOIndex!!,
                                            onItemSelected = { viewModel.updatePerformanceIO(it) }
                                        )
                                    },
                                    {
                                        ExpressiveDropdownItem(
                                            icon = Icons.Outlined.EnergySavingsLeaf,
                                            title = stringResource(R.string.powersave_io_scheduler),
                                            summary = stringResource(R.string.powersave_io_scheduler_desc),
                                            items = viewModel.availableIOSchedulers ?: emptyList(),
                                            selectedIndex = viewModel.powersaveIOIndex!!,
                                            onItemSelected = { viewModel.updatePowersaveIO(it) }
                                        )
                                    }
                                )
                            )
                        } else {
                            SectionLoadingIndicator()
                        }
                    } else {
                        Text(
                            text = "I/O Scheduler tidak didukung pada perangkat ini.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                
                item { TweaksSectionTitle(text = "Power & Thermal") }
                item {
                    if (viewModel.thermalState != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Cable) },
                                        onClick = { navController.navigate("bypasschg") },
                                        headlineContent = { Text(stringResource(R.string.bcharging)) },
                                        supportingContent = { Text(stringResource(R.string.bcharging_desc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(                                        
                                        icon = Icons.Filled.ThermostatAuto,
                                        title = stringResource(R.string.thermalcore_service),
                                        summary = stringResource(R.string.thermalcore_service_desc),
                                        checked = viewModel.thermalState!!,
                                        onCheckedChange = { viewModel.updateThermalCore(it) }
                                    )
                                }
                            )
                        )
                    } else {
                        SectionLoadingIndicator()
                    }
                }
                
                item { TweaksSectionTitle(stringResource(R.string.section_addons)) }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.FilterBAndW) },
                                    onClick = { navController.navigate("colorscheme") },
                                    headlineContent = { Text(stringResource(R.string.color_scheme)) },
                                    supportingContent = { Text(stringResource(R.string.schemecolordesc)) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.AddToPhotos) },
                                    onClick = { navController.navigate("preferenced") },
                                    headlineContent = { Text(stringResource(R.string.prefs)) },
                                    supportingContent = { Text(stringResource(R.string.prefsdesc)) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    )
                }
            }
        }
        
        RootAppDialog {
            BackupRestoreBottomSheet(
                show = showBackupRestoreSheet,
                onDismiss = { showBackupRestoreSheet = false },
                onBackup = { 
                    // Panggil file picker untuk buat file dengan nama default
                    createDocLauncher.launch("AZenith_Config.zx") 
                },
                onRestore = { 
                    // Panggil file picker dengan filter
                    openDocLauncher.launch(arrayOf("application/octet-stream", "*/*")) 
                }
            )
        }

        RootAppDialog {
            BackupRestoreBottomSheet(
                show = showBackupRestoreSheet,
                onDismiss = { showBackupRestoreSheet = false },
                onBackup = { scope.launch { snackbarHostState.showSnackbar("Dummy: Backup Started") } },
                onRestore = { scope.launch { snackbarHostState.showSnackbar("Dummy: Restore Started") } }
            )
        }


        RootAppDialog {
            RendererDialog(
                show = showRendererDialog,
                onDismiss = { showRendererDialog = false },
                onRenderer = { reason -> viewModel.executeSetRenderer(reason, context) }
            )
        }

        RootAppDialog {
            RefreshRatePickerDialog(
                show = showRefreshRateDialog,
                onDismiss = { showRefreshRateDialog = false },
                onRefreshRatePicker = { reason -> viewModel.executeSetRefreshRates(reason, context) }
            )
        }
    }
}

@Composable
fun SectionLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun TweaksSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun FreqLimitSliderItem(
    icon: ImageVector? = null,
    initialValue: Float,
    labels: List<String>,
    onSaved: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initialValue) }
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = sliderValue / 6f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "OffsetProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    LeadingIcon(icon = icon, contentDescription = stringResource(R.string.freq_offset))
                    Spacer(modifier = Modifier.width(16.dp)) 
                }
                Text(
                    text = stringResource(R.string.freq_offset),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            }
            
            Surface(
                color = if (sliderValue.roundToInt() == 0) colorScheme.surfaceVariant else colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (sliderValue.roundToInt() == 0) stringResource(R.string.disabled) else labels[sliderValue.roundToInt()],
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (sliderValue.roundToInt() == 0) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceContainerHighest)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSaved(sliderValue) },
            valueRange = 0f..6f,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.disabled), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
            Text("40%", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
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
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val cardBgColor = colorScheme.surfaceColorAtElevation(1.dp)

    val iconBoxBgColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(400), 
        label = "iconBoxBgColorAnim"
    )

    val iconColor by animateColorAsState(
        targetValue = if (highlight) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "iconColorAnim"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        color = cardBgColor,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp) 
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)  
                    .clip(RoundedCornerShape(18.dp)) 
                    .background(iconBoxBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = iconColor,
                    modifier = Modifier.size(36.dp) 
                )

                if (showArrow) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight, 
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp), 
                        tint = iconColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = label, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        // Kombinasi fade in + sedikit scale up untuk masuk
                        // dan fade out + sedikit scale down untuk keluar (bikin efek "luntur/ngeblur")
                        (fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                         scaleIn(initialScale = 0.95f, animationSpec = tween(300, delayMillis = 100)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(200)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(200))
                            )
                    },
                    label = "ValueTextAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = colorScheme.onSurfaceVariant, 
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                }

            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun TweakScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onMoreClick: () -> Unit) {
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
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
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
            },
            title = {
                Text(
                    text = stringResource(R.string.nav_tweaks),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = "More Options"
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
