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

import zx.azenith.ui.theme.azShape


import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.*
import zx.azenith.ui.viewmodel.SearchTab
import zx.azenith.ui.viewmodel.TweakViewModel
import zx.azenith.ui.component.ZenithSlider


/**
 * Which LazyColumn item holds each searchable row, so a search result can scroll
 * to it. Positional: the list is built inline and the rows carry no keys of their
 * own. Sections are in here too -- a heading is a legitimate landing spot.
 */
private val TWEAK_ROW_ITEMS: Map<String, Int> = mapOf(
    "tweaks.app_priority" to 4,
    "tweaks.bypass_charge" to 7,
    "tweaks.colors" to 9,
    "tweaks.dnd" to 4,
    "tweaks.fpsgo" to 2,
    "tweaks.fstrim" to 4,
    "tweaks.game_preload" to 4,
    "tweaks.governors" to 4,
    "tweaks.memory_killer" to 4,
    "tweaks.perf_lite" to 2,
    "tweaks.prefs" to 9,
    "tweaks.refresh_rate" to 5,
    "tweaks.renderer" to 5,
    "tweaks.section_additional" to 3,
    "tweaks.section_addons" to 8,
    "tweaks.section_performance" to 1,
    "tweaks.section_power_thermal" to 6,
    "tweaks.thermalcore" to 7,
)

@Composable
fun TweakScreen(
    navController: NavController,
    viewModel: TweakViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colorScheme = MaterialTheme.colorScheme
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val blurOn = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val scaffoldColor = ScaffoldContainerColor(MaterialTheme.colorScheme.surface)

    // Search results land here: entry id -> the LazyColumn item holding that row.
    // Item indices, not titles, because the list is built inline and the rows
    // are only addressable by position.
    val jumpPulse = listState.searchJumpTarget(
        owns = { it.startsWith("tweaks.") },
        indexOf = { id -> TWEAK_ROW_ITEMS[id] }
    )
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var requestHideBackupRestoreSheet by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }
    val rendererOrigin = rememberDialogOrigin()
    var showRefreshRateDialog by remember { mutableStateOf(false) }
    val refreshRateOrigin = rememberDialogOrigin()
    val backupOptionsOrigin = rememberDialogOrigin()
    val restoreDialogOrigin = rememberDialogOrigin()
    var pendingRestoreData by remember { mutableStateOf<Map<String, String>?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var optBackupTweaks by remember { mutableStateOf(true) }
    var optBackupApplist by remember { mutableStateOf(true) }

    var pendingRestoreResult by remember { mutableStateOf<TweakViewModel.ValidationResult?>(null) }
    var optRestoreTweaks by remember { mutableStateOf(true) }
    var optRestoreApplist by remember { mutableStateOf(true) }
    
    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog(onConfirm = {}, onDismiss = {})
    
    LoadingDialogHost(handle = loadingDialog)
    ConfirmDialogHost(handle = confirmDialog)

    // Gamepad shortcuts: Square opens Tweaks search, Triangle toggles the cloud
    // (backup/restore) sheet beside it.
    val isCurrentPage = LocalIsCurrentPagerPage.current
    val isPagerActive = LocalMainPagerActive.current
    val pageIndex = LocalPagerPageIndex.current
    RegisterGamepadActions(
        enabled = isCurrentPage && isPagerActive,
        hasSearch = true,
        isSearchOpen = false,
        onSearchToggle = { navController.navigate(SearchTab.Tweaks.searchRoute) },
        hasTopAction = true,
        isTopActionOpen = showBackupRestoreSheet,
        onTopAction = { showBackupRestoreSheet = !showBackupRestoreSheet },
        onCloseOverlay = {
            var closed = false
            if (showBackupRestoreSheet && !requestHideBackupRestoreSheet) { requestHideBackupRestoreSheet = true; closed = true }
            if (showRendererDialog) { showRendererDialog = false; closed = true }
            if (showRefreshRateDialog) { showRefreshRateDialog = false; closed = true }
            if (showBackupOptionsDialog) { showBackupOptionsDialog = false; closed = true }
            if (showRestoreDialog) { showRestoreDialog = false; closed = true }
            closed
        },
        scrollState = listState,
        onScrollDelta = { delta ->
            scrollBehavior.state.heightOffset =
                (scrollBehavior.state.heightOffset - delta)
                    .coerceIn(scrollBehavior.state.heightOffsetLimit, 0f)
        }
    )

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            scope.launch {
                val success = loadingDialog.withLoading {
                    viewModel.createConfigFileBackup(context, it, optBackupTweaks, optBackupApplist)
                }
                if (success) {
                    snackbarHostState.showSnackbar(context.getString(R.string.dialog_backup_success))
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.dialog_backup_fail))
                }
            }
        }
    }
    
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            showBackupRestoreSheet = false
            scope.launch {
                loadingDialog.withLoading {
                    val result = viewModel.validateAndRestoreFile(context, it)
                    if (result.isValid && result.data != null) {
                        pendingRestoreResult = result
                        optRestoreTweaks = result.hasTweaks
                        optRestoreApplist = result.hasApplist
                        showRestoreDialog = true
                    } else {
                        confirmDialog.showConfirm(context.getString(R.string.dialog_restore_fail_title), result.message, context.getString(android.R.string.ok), null)
                    }
                }
            }
        }
    }

    // .exec() is a blocking su round-trip. Left on the main thread it stalls the
    // first frame of the Tweaks tab, which is exactly when this screen is
    // entered, so move it off and let the row render immediately.
    var isFullModeEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isFullModeEnabled = withContext(Dispatchers.IO) {
            DebugUtils.isFullModeEnabled()
        }
    }

    // Read once for the whole screen. The restore dialog compared the backup's
    // SoC against this property in three separate places, two of them inside
    // content lambdas, so every recomposition of that dialog paid a fresh
    // property read. The value is fixed for as long as the screen is composed.
    // produceState rather than a plain read because the read needs a dispatcher
    // hop, and a bare `remember` block is not a suspend context.
    val currentSocType by produceState("") {
        value = withContext(Dispatchers.IO) {
            PropertyUtils.get("persist.sys.azenith.soctype")
        }
    }

    // The renderer prop and the display mode can be changed from outside the app (the
    // module's CLI, another app, a shell). Poll the small read so the tiles follow those
    // changes while the screen is composed, instead of waiting for the app to be reopened.
    LaunchedEffect(Unit) {
        viewModel.loadAllConfiguration(context)
        while (true) {
            delay(2_000)
            viewModel.refreshDisplayState(context)
        }
    }

    // Coming back from another app should be instant, without waiting for the next poll.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch(Dispatchers.IO) { viewModel.refreshDisplayState(context) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = if (!isLandscapeNav()) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
        topBar = {
            // Landscape drops the bar: the dock names the page and carries both actions.
            if (!isLandscapeNav()) {
                TweakScreenTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onSearchClick = { navController.navigate(SearchTab.Tweaks.searchRoute) },
                    onMoreClick = { showBackupRestoreSheet = true },
                    modifier = backupOptionsOrigin.trackedModifier(),
                    onMoreModifier = backupOptionsOrigin.trackedModifier(),
                )
            }
            PublishNavDockActions(
                listOf(
                    NavDockAction(
                        icon = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        onClick = { navController.navigate(SearchTab.Tweaks.searchRoute) }
                    ),
                    NavDockAction(
                        icon = Icons.Outlined.Cloud,
                        contentDescription = stringResource(R.string.cd_menu),
                        onClick = { showBackupRestoreSheet = true }
                    )
                )
            )
        },
        snackbarHost = {
            ZenithSnackbarHost(
                hostState = snackbarHostState,
                portraitBottomPadding = 100.dp,
                landscapeDockClearance = true
            )
        },
        containerColor = scaffoldColor,
        contentColor = ScaffoldContentColor()
    ) { innerPadding ->

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().hazePageSource().gamepadScreenEntry(page = pageIndex),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                start = 16.dp + safeAreaStartInset(),
                end = 16.dp + navBarEndClearance(),
                bottom = navBarBottomClearance(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        ) {

            item {
                Spacer(modifier = Modifier.height(16.dp))
                ExpressiveList(
                    content = listOf(
                        {
                            ExpressiveInfoCard(
                                supportingContent = { Text(text = stringResource(R.string.str_these_settings_apply_to_all_en)) },
                                leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                containerColor = colorScheme.surfaceContainerLow,
                                onClick = {}
                            )
                        }
                    )
                )
            }

            item { JumpRow(activeId = jumpPulse, entryId = "tweaks.section_performance", highlightShape = azShape(14.dp)) { TweaksSectionTitle(text = stringResource(R.string.section_performance)) } }
            item {
                var socType by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    socType = withContext(Dispatchers.IO) { getChipsetVendor(context) }
                }
                if (socType != null && viewModel.liteState != null) {
                    val isMediaTek   = socType == "mediatek"

                    ExpressiveList(
                        content = listOf(
                            {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.perf_lite") {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Speed,
                                        title = stringResource(R.string.perf_lite_mode),
                                        summary = stringResource(R.string.perf_lite_mode_desc),
                                        checked = viewModel.liteState!!,
                                        onCheckedChange = { viewModel.updateLiteMode(it) },
                                        focusKey = "tweaks.perf_lite",
                                        focusIndex = 2
                                    )
                                }
                            },
                            {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.fpsgo") {
                                    Box(modifier = Modifier.alpha(if (isMediaTek) 1f else 0.4f)) {
                                        ExpressiveListItem(
                                            leadingContent = { LeadingIcon(icon = Icons.Filled.Speed) },
                                            onClick = {
                                                if (isMediaTek) {
                                                    navController.navigate("fpsgoscreen")
                                                }
                                            },
                                            headlineContent = { Text(text = stringResource(R.string.str_fpsgo_settings)) },
                                            supportingContent = {
                                                Text(
                                                    text = if (isMediaTek)
                                                        stringResource(R.string.str_fpsgo_desc)
                                                    else
                                                        stringResource(R.string.str_fpsgo_unavailable)
                                                )
                                            },
                                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                            focusKey = "tweaks.fpsgo",
                                            focusIndex = 2
                                        )
                                    }
                                }
                            }
                        )
                    )
                } else {
                    SectionLoadingIndicator()
                }
            }

            item { JumpRow(activeId = jumpPulse, entryId = "tweaks.section_additional", highlightShape = azShape(14.dp)) { TweaksSectionTitle(stringResource(R.string.section_additionalsettings)) } }
            item {
                if (viewModel.preloadState != null &&
                    viewModel.memKillerState != null &&
                    viewModel.appPriorState != null &&
                    viewModel.dndState != null &&
                    viewModel.fstrimState != null) {
                    ExpressiveList(
                        content = buildList {
                            add {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.game_preload") {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.RocketLaunch,
                                        title = stringResource(R.string.game_preload),
                                        summary = stringResource(R.string.game_preload_desc),
                                        checked = viewModel.preloadState!!,
                                        onCheckedChange = { viewModel.updatePreloadMode(it) },
                                        focusKey = "tweaks.game_preload",
                                        focusIndex = 4
                                    )
                                }
                            }
                            if (isFullModeEnabled) {
                                add {
                                    JumpRow(activeId = jumpPulse, entryId = "tweaks.memory_killer") {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.CleaningServices,
                                            title = stringResource(R.string.memory_killer),
                                            summary = stringResource(R.string.memory_killer_desc),
                                            checked = viewModel.memKillerState!!,
                                            onCheckedChange = { viewModel.updateMemoryKiller(it) },
                                            focusKey = "tweaks.memory_killer",
                                            focusIndex = 4
                                        )
                                    }
                                }
                            }
                            if (isFullModeEnabled) {
                                add {
                                    JumpRow(activeId = jumpPulse, entryId = "tweaks.app_priority") {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.SwapVerticalCircle,
                                            title = stringResource(R.string.app_priority_control),
                                            summary = stringResource(R.string.app_priority_control_desc),
                                            checked = viewModel.appPriorState!!,
                                            onCheckedChange = { viewModel.updateAppPriority(it) },
                                            focusKey = "tweaks.app_priority",
                                            focusIndex = 4
                                        )
                                    }
                                }
                            }
                            add {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.dnd") {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.DoNotDisturbOn,
                                        title = stringResource(R.string.dnd_mode_gaming),
                                        summary = stringResource(R.string.dnd_mode_gaming_desc),
                                        checked = viewModel.dndState!!,
                                        onCheckedChange = { viewModel.updateDndMode(it) },
                                        focusKey = "tweaks.dnd",
                                        focusIndex = 4
                                    )
                                }
                            }
                            if (isFullModeEnabled) {
                                add {
                                    JumpRow(activeId = jumpPulse, entryId = "tweaks.fstrim") {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Outlined.ContentCut,
                                            title = stringResource(R.string.trim_filesystem),
                                            summary = stringResource(R.string.trim_filesystem_desc),
                                            checked = viewModel.fstrimState!!,
                                            onCheckedChange = { viewModel.updateFstrim(it) },
                                            focusKey = "tweaks.fstrim",
                                            focusIndex = 4
                                        )
                                    }
                                }
                            }
                            add {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.governors") {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Ballot) },
                                        onClick = { navController.navigate("governorsettings") },
                                        headlineContent = { Text(stringResource(R.string.gov_settings)) },
                                        supportingContent = { Text(stringResource(R.string.gov_settingsdesc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        focusKey = "tweaks.governors",
                                        focusIndex = 4
                                    )
                                }
                            }
                        }
                    )
                } else {
                    SectionLoadingIndicator()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (viewModel.currentRefreshRate != null && viewModel.currentRenderer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        JumpRow(
                            activeId = jumpPulse,
                            entryId = "tweaks.refresh_rate",
                            modifier = Modifier.weight(1f).then(refreshRateOrigin.trackedModifier())
                        ) {
                            ExpressiveTile(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.WebStories,
                                label = stringResource(R.string.refreshrates),
                                value = stringResource(R.string.refresh_rate_format, viewModel.currentRefreshRate.toString()),
                                showArrow = true,
                                highlight = true,
                                isLoading = viewModel.isRefreshRateLoading,
                                focusKey = "tweaks.refresh_rate",
                                focusIndex = 5,
                                horizontalNav = HorizontalNav.BLOCK_LEFT
                            ) {
                                showRefreshRateDialog = true
                            }
                        }

                        JumpRow(
                            activeId = jumpPulse,
                            entryId = "tweaks.renderer",
                            modifier = Modifier.weight(1f).then(rendererOrigin.trackedModifier())
                        ) {
                            ExpressiveTile(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.SettingsSuggest,
                                label = stringResource(R.string.renderengine),
                                value = viewModel.currentRenderer!!.let { r ->
                                    if (r.startsWith("Default")) stringResource(R.string.Renderer_Default) + r.removePrefix("Default") else r
                                }.uppercase(),
                                showArrow = true,
                                highlight = true,
                                isLoading = viewModel.isRendererLoading,
                                focusKey = "tweaks.renderer",
                                focusIndex = 5,
                                horizontalNav = HorizontalNav.BLOCK_RIGHT
                            ) {
                                showRendererDialog = true
                            }
                        }
                    }
                } else {
                    SectionLoadingIndicator()
                }
            }

            item { JumpRow(activeId = jumpPulse, entryId = "tweaks.section_power_thermal", highlightShape = azShape(14.dp)) { TweaksSectionTitle(text = stringResource(R.string.section_power_thermal)) } }
            item {
                if (viewModel.thermalState != null) {
                    ExpressiveList(
                        content = listOf(
                            {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.bypass_charge") {
                                    ExpressiveListItem(
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Cable) },
                                        onClick = { navController.navigate("bypasschg") },
                                        headlineContent = { Text(stringResource(R.string.bcharging)) },
                                        supportingContent = { Text(stringResource(R.string.bcharging_desc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                        focusKey = "tweaks.bypass_charge",
                                        focusIndex = 7
                                    )
                                }
                            },
                            {
                                JumpRow(activeId = jumpPulse, entryId = "tweaks.thermalcore") {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Filled.ThermostatAuto,
                                        title = stringResource(R.string.thermalcore_service),
                                        summary = stringResource(R.string.thermalcore_service_desc),
                                        checked = viewModel.thermalState!!,
                                        onCheckedChange = { viewModel.updateThermalCore(it) },
                                        focusKey = "tweaks.thermalcore",
                                        focusIndex = 7
                                    )
                                }
                            }
                        )
                    )
                } else {
                    SectionLoadingIndicator()
                }
            }

            item { JumpRow(activeId = jumpPulse, entryId = "tweaks.section_addons", highlightShape = azShape(14.dp)) { TweaksSectionTitle(stringResource(R.string.section_addons)) } }
            item {
                ExpressiveList(
                    content = listOf(
                        {
                            JumpRow(activeId = jumpPulse, entryId = "tweaks.colors") {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.FilterBAndW) },
                                    onClick = { navController.navigate("colorscheme") },
                                    headlineContent = { Text(stringResource(R.string.color_scheme)) },
                                    supportingContent = { Text(stringResource(R.string.schemecolordesc)) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                    focusKey = "tweaks.colors",
                                    focusIndex = 9
                                )
                            }
                        },
                        {
                            JumpRow(activeId = jumpPulse, entryId = "tweaks.prefs") {
                                ExpressiveListItem(
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.AddToPhotos) },
                                    onClick = { navController.navigate("preferenced") },
                                    headlineContent = { Text(stringResource(R.string.prefs)) },
                                    supportingContent = { Text(stringResource(R.string.prefsdesc)) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                    focusKey = "tweaks.prefs",
                                    focusIndex = 9
                                )
                            }
                        }
                    )
                )
            }
        }
    }

    RootAppDialog {
        BackupRestoreBottomSheet(
            show = showBackupRestoreSheet,
            onDismiss = {
                showBackupRestoreSheet = false
                requestHideBackupRestoreSheet = false
            },
            onBackup = {
                showBackupRestoreSheet = false
                requestHideBackupRestoreSheet = false
                showBackupOptionsDialog = true
            },
            onRestore = {
                openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
            },
            externalHideRequest = requestHideBackupRestoreSheet,
            onExternalHideConsumed = { requestHideBackupRestoreSheet = false }
        )
    }

    RootAppDialog {
        CustomContentDialog(
            visible = showBackupOptionsDialog,
            title = context.getString(R.string.dialog_backup_options_title),
            origin = backupOptionsOrigin.origin,
            confirmText = context.getString(R.string.dialog_backup_options_confirm),
            confirmEnabled = optBackupTweaks || optBackupApplist,
            onDismiss = { showBackupOptionsDialog = false },
            onConfirm = {
                showBackupOptionsDialog = false
                val sdf = java.text.SimpleDateFormat("ddMMyyyy_HHmmss", java.util.Locale.getDefault())
                val timestamp = sdf.format(java.util.Date())
                val dynamicFileName = "AZenithConfig_Backup_$timestamp.zx"
                createDocLauncher.launch(dynamicFileName)
            }
        ) {
            Column {
                Text(
                    text = stringResource(R.string.str_select_the_configurations_you),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadFocusOutline(
                            shape = RoundedCornerShape(12.dp),
                            focusKey = "dialog.backup.tweaks",
                            onActivate = { optBackupTweaks = !optBackupTweaks }
                        )
                        .clickable { optBackupTweaks = !optBackupTweaks }
                ) {
                    Checkbox(checked = optBackupTweaks, onCheckedChange = { optBackupTweaks = it })
                    Text(stringResource(R.string.str_tweak_configuration_settings), color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadFocusOutline(
                            shape = RoundedCornerShape(12.dp),
                            focusKey = "dialog.backup.applist",
                            onActivate = { optBackupApplist = !optBackupApplist }
                        )
                        .clickable { optBackupApplist = !optBackupApplist }
                ) {
                    Checkbox(checked = optBackupApplist, onCheckedChange = { optBackupApplist = it })
                    Text(stringResource(R.string.str_per_app_applist_settings), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    RootAppDialog {
        CustomContentDialog(
            visible = showRestoreDialog,
            title = context.getString(R.string.str_restore_configuration),
            origin = restoreDialogOrigin.origin,
            confirmText = context.getString(R.string.dialog_restore_confirm),
            confirmEnabled = pendingRestoreResult?.let { result ->
                val isSocMismatch = result.socType != currentSocType
                (optRestoreTweaks && !isSocMismatch) || optRestoreApplist
            } ?: false,
            onDismiss = { showRestoreDialog = false },
            onConfirm = {
                showRestoreDialog = false


                pendingRestoreResult?.let { result ->
                    val dataToRestore = result.data
                    val isSocMismatch = result.socType != currentSocType

                    if (dataToRestore != null) {
                        scope.launch {
                            loadingDialog.withLoading {
                                viewModel.applyRestoreData(context, dataToRestore, optRestoreTweaks && !isSocMismatch, optRestoreApplist)
                                viewModel.loadAllConfiguration(context)
                            }
                        }
                    }
                }
            }
        ) {

            pendingRestoreResult?.let { result ->
                val socName = zx.azenith.ui.util.BackupManager.getSocName(context, result.socType)
                val isSocMismatch = result.socType != currentSocType

                Column {
                    Text(
                        text = stringResource(R.string.str_backup_content_detected_select),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    if (isSocMismatch && result.hasTweaks) {
                        Text(
                            stringResource(R.string.str_warning_backup_is_for_socname, socName),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (result.hasTweaks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusOutline(
                                    shape = RoundedCornerShape(12.dp),
                                    focusKey = "dialog.restore.tweaks",
                                    onActivate = { if (!isSocMismatch) optRestoreTweaks = !optRestoreTweaks }
                                )
                                .clickable { if (!isSocMismatch) optRestoreTweaks = !optRestoreTweaks }
                        ) {
                            Checkbox(
                                checked = optRestoreTweaks && !isSocMismatch,
                                onCheckedChange = { if (!isSocMismatch) optRestoreTweaks = it },
                                enabled = !isSocMismatch
                            )
                            Text(stringResource(R.string.str_tweak_configuration_settings), color = if (isSocMismatch) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (result.hasApplist) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusOutline(
                                    shape = RoundedCornerShape(12.dp),
                                    focusKey = "dialog.restore.applist",
                                    onActivate = { optRestoreApplist = !optRestoreApplist }
                                )
                                .clickable { optRestoreApplist = !optRestoreApplist }
                        ) {
                            Checkbox(checked = optRestoreApplist, onCheckedChange = { optRestoreApplist = it })
                            Text(stringResource(R.string.str_per_app_applist_settings), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }

    RootAppDialog {
        RendererDialog(
            show = showRendererDialog,
            onDismiss = { showRendererDialog = false },
            onRenderer = { reason -> viewModel.executeSetRenderer(reason, context) },
            origin = rendererOrigin.origin,
            currentRenderer = viewModel.currentRenderer
        )
    }

    RootAppDialog {
        RefreshRatePickerDialog(
            show = showRefreshRateDialog,
            onDismiss = { showRefreshRateDialog = false },
            onRefreshRatePicker = { reason -> viewModel.executeSetRefreshRates(reason, context) },
            origin = refreshRateOrigin.origin,
            currentRefreshRate = viewModel.currentRefreshRate?.toString()
        )
    }
}

@Composable
fun SectionLoadingIndicator() {
    SkeletonSettingsList(rowCount = 3)
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
                shape = azShape(12.dp)
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
        
        Spacer(modifier = Modifier.height(4.dp))
        ZenithSlider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onSaved(sliderValue) },
            valueRange = 0f..6f,
            steps = 5,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.disabled), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
            Text(stringResource(R.string.str_40), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
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
    isLoading: Boolean = false,
    focusKey: String? = null,
    focusIndex: Int? = null,
    horizontalNav: HorizontalNav = HorizontalNav.BLOCK_BOTH,
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
            .clip(azShape(26.dp))
            .gamepadFocusOutline(
                onActivate = onClick,
                focusKey = focusKey,
                focusIndex = focusIndex,
                horizontalNav = horizontalNav
            )
            .clickable { onClick() }
            .animateContentSize(animationSpec = tween(BlurCrossfadeDurationMillis)),
        color = cardBgColor,
        shape = azShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(azShape(18.dp))
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
                

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BlurCrossfadeText(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


@Composable
fun TweakScreenTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreModifier: Modifier = Modifier
) {

    Box(
    modifier = modifier
        .fillMaxWidth()
        .gamepadNotFocusable()
        .appBarFade(MaterialTheme.colorScheme.surface, scrollBehavior.state.overlappedFraction)
        .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                GamepadFaceHints()
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                }
                IconButton(
                    onClick = onMoreClick,
                    modifier = onMoreModifier.focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = stringResource(R.string.cd_menu)
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
