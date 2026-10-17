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

package zx.azenith.ui.gamespace

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.view.KeyEvent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Token
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.AppIconCache
import zx.azenith.ui.component.ScreenWrapper
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.GamepadFocusRoot
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.subscreens.AppSettingsScreen
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.viewmodel.ApplistViewmodel

private enum class TopCategory(val labelRes: Int) {
    Games(R.string.gamespace_tab_games),
    System(R.string.gamespace_tab_media)
}

private sealed interface RibbonEntry {
    val key: String

    data object Welcome : RibbonEntry {
        override val key: String = "__welcome__"
    }

    data class Game(val app: ApplistViewmodel.AppInfo) : RibbonEntry {
        override val key: String = app.packageName
    }

    data object Library : RibbonEntry {
        override val key: String = "__library__"
    }
}

/**
 * ZXGameSpace Material 3 Expressive home screen with full touch and controller support.
 */
@Composable
fun ZXGameSpaceHub(
    profile: String,
    profileValue: String = "",
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo> = games,
    onApplyProfile: (String) -> Unit = {},
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onExit: () -> Unit,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val effectsSpec = motion.defaultEffectsSpec<Float>()
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()

    var category by remember { mutableStateOf(TopCategory.Games) }
    var selectedRibbonIndex by remember { mutableIntStateOf(0) }
    var exitPrompt by remember { mutableStateOf(false) }
    var tuningPkg by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // The sheet's own offset decides the expansion, so the scroll lives above the stage.
    val welcomeScroll = rememberScrollState()
    var homeExpanded by remember { mutableStateOf(false) }
    var libraryExpanded by remember { mutableStateOf(false) }

    val ribbonEntries = remember(games) {
        buildList {
            add(RibbonEntry.Welcome)
            games.forEach { add(RibbonEntry.Game(it)) }
            add(RibbonEntry.Library)
        }
    }

    val safeIndex = selectedRibbonIndex.coerceIn(0, ribbonEntries.lastIndex)
    val activeEntry = ribbonEntries[safeIndex]

    // The stage's own gate drives the expansion; leaving one is all that is left to reset here.
    LaunchedEffect(activeEntry) {
        if (activeEntry != RibbonEntry.Welcome) {
            homeExpanded = false
            welcomeScroll.scrollTo(0)
        }
        if (activeEntry != RibbonEntry.Library) libraryExpanded = false
    }

    val clock = rememberClock()
    val battery = rememberBatteryState()
    val ramState = rememberRamState()
    val refreshHz = remember(context) {
        getSupportedRefreshRatesPicker(context).firstOrNull()?.toIntOrNull() ?: 60
    }
    val deviceName = remember(context) { getRealDeviceName(context) }
    val chipsetName = remember(context) { getChipsetName(context) }

    GamepadFocusRoot(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerLowest)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            category = TopCategory.Games
                            true
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            category = TopCategory.System
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Dynamic full-bleed atmospheric backdrop driven by the selected ribbon entry.
        ZXGameSpaceStageBackdrop(
            entry = if (category == TopCategory.Games) activeEntry else RibbonEntry.Welcome
        )

        val isScrolled = homeExpanded || (category == TopCategory.Games && activeEntry == RibbonEntry.Welcome && welcomeScroll.value > 15)
        val topBarScrimAlpha by animateFloatAsState(
            targetValue = if (isScrolled) 1f else 0f,
            animationSpec = motion.defaultEffectsSpec<Float>(),
            label = "TopBarScrimAlpha"
        )

        // Continuous edge-to-edge top bar gradient scrim merging with the status bar
        if (topBarScrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .graphicsLayer { alpha = topBarScrimAlpha }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                scheme.surface.copy(alpha = 0.96f),
                                scheme.surface.copy(alpha = 0.85f),
                                scheme.surface.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            ZXGameSpaceTopBar(
                selectedCategory = category,
                clock = clock,
                battery = battery,
                isScrolled = isScrolled,
                onSelectCategory = { category = it },
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchClose = {
                    searchOpen = false
                    searchQuery = ""
                },
                onSearchClick = {
                    category = TopCategory.Games
                    searchOpen = true
                },
                onSettingsClick = {
                    category = if (category == TopCategory.System) TopCategory.Games else TopCategory.System
                },
                onAvatarClick = {
                    val nextProfile = when (profileValue) {
                        "1" -> "2"
                        "2" -> "3"
                        else -> "1"
                    }
                    onApplyProfile(nextProfile)
                },
                onExit = { exitPrompt = true }
            )

            Spacer(Modifier.height(6.dp))

            if (category == TopCategory.Games) {
                // The library sits under the ribbon like the sheet does, so the ribbon only
                // leaves once one of them has been scrolled to full screen.
                AnimatedVisibility(
                    visible = !searchOpen && !homeExpanded && !libraryExpanded,
                    enter = fadeIn(effectsSpec) + expandVertically(spatialSpec),
                    exit = fadeOut(effectsSpec) + shrinkVertically(spatialSpec)
                ) {
                    Column {
                        ZXGameSpaceTileRibbon(
                            entries = ribbonEntries,
                            selectedIndex = safeIndex,
                            onSelect = { selectedRibbonIndex = it },
                            onActivate = { entry ->
                                when (entry) {
                                    is RibbonEntry.Game -> onLaunch(entry.app)
                                    RibbonEntry.Welcome -> {
                                        games.firstOrNull()?.let { selectedRibbonIndex = 1 }
                                    }
                                    RibbonEntry.Library -> {
                                        selectedRibbonIndex = ribbonEntries.lastIndex
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }

                BackHandler(enabled = searchOpen || homeExpanded || libraryExpanded) {
                    if (searchOpen) {
                        searchOpen = false
                        searchQuery = ""
                    } else if (homeExpanded) {
                        homeExpanded = false
                    } else if (libraryExpanded) {
                        libraryExpanded = false
                    } else {
                        selectedRibbonIndex = 0
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = activeEntry,
                        transitionSpec = {
                            fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "ZXGameSpaceMainStage"
                    ) { entry ->
                    when (entry) {
                        RibbonEntry.Welcome -> WelcomeHubStage(
                            scroll = welcomeScroll,
                            expanded = homeExpanded,
                            onExpandedChange = { homeExpanded = it },
                            profile = profile,
                            profileValue = profileValue,
                            games = games,
                            allAppsCount = allApps.size.coerceAtLeast(games.size),
                            refreshHz = refreshHz,
                            deviceName = deviceName,
                            chipsetName = chipsetName,
                            onApplyProfile = onApplyProfile,
                            onOpenSystem = { category = TopCategory.System },
                            onSelectGame = { game ->
                                val idx = ribbonEntries.indexOfFirst {
                                    it is RibbonEntry.Game && it.app.packageName == game.packageName
                                }
                                if (idx >= 0) selectedRibbonIndex = idx else onLaunch(game)
                            },
                            onLaunchGame = onLaunch,
                            onOpenLibrary = { selectedRibbonIndex = ribbonEntries.lastIndex }
                        )

                        is RibbonEntry.Game -> GameHeroStage(
                            app = entry.app,
                            onLaunch = { onLaunch(entry.app) },
                            onOpenAppSettings = { tuningPkg = entry.app.packageName }
                        )

                        RibbonEntry.Library -> LibraryGridStage(
                            games = games,
                            allApps = allApps,
                            expanded = libraryExpanded,
                            onOpenSettings = { tuningPkg = it.packageName },
                            onExpandedChange = { libraryExpanded = it },
                            onClose = { selectedRibbonIndex = 0 }
                        )
                    }
                }

                    ZXGameSpaceSearchOverlay(
                        visible = searchOpen,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ZXGameSpaceSearchPane(
                            query = searchQuery,
                            apps = allApps,
                            onLaunch = onLaunch,
                            onOpenSettings = { tuningPkg = it.packageName }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                SystemControlStage(
                    profile = profile,
                    profileValue = profileValue,
                    battery = battery,
                    ramState = ramState,
                    refreshHz = refreshHz,
                    deviceName = deviceName,
                    chipsetName = chipsetName,
                    onApplyProfile = onApplyProfile,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }

        if (exitPrompt) {
            ZXGameSpaceExitDialog(
                onDismiss = { exitPrompt = false },
                onExitShell = {
                    exitPrompt = false
                    onExit()
                },
                onExitApp = {
                    exitPrompt = false
                    onExitApp()
                }
            )
        }

        tuningPkg?.let { pkg ->
            ZXGameSpaceAppSettingsOverlay(packageName = pkg, onClose = { tuningPkg = null })
        }
    }
}

@Composable
private fun ZXGameSpaceSearchOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    // The scoped AnimatedVisibility overloads do not resolve from inside the stage Box.
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(effectsSpec),
        exit = fadeOut(effectsSpec)
    ) {
        content()
    }
}

/** Sheet offset (px) at which a drag stops being scroll and becomes the full-screen step. */
private const val HOME_EXPAND_AT = 90

private const val SEARCH_RESULT_CAP = 3

/**
 * Results for the top bar search, drawn over the stage rather than beside it. Apps only for now —
 * settings entries land here later. Word-for-word hits lead, then prefix hits, then the rest, and
 * anything past [SEARCH_RESULT_CAP] hides behind Show all.
 */
@Composable
private fun ZXGameSpaceSearchPane(
    query: String,
    apps: List<ApplistViewmodel.AppInfo>,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onOpenSettings: (ApplistViewmodel.AppInfo) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var showAll by remember(query) { mutableStateOf(false) }

    val matches = remember(query, apps) {
        if (query.isBlank()) {
            emptyList()
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
                .sortedWith(
                    compareByDescending<ApplistViewmodel.AppInfo> {
                        it.label.equals(query, ignoreCase = true)
                    }.thenByDescending { it.label.startsWith(query, ignoreCase = true) }
                        .thenBy { it.label.lowercase() }
                )
        }
    }
    val shown = if (showAll) matches else matches.take(SEARCH_RESULT_CAP)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface.copy(alpha = 0.96f))
    ) {
        when {
            query.isBlank() -> Text(
                text = stringResource(R.string.gamespace_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )

            matches.isEmpty() -> Text(
                text = stringResource(R.string.gamespace_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown, key = { it.packageName }) { app ->
                        val shape = MaterialTheme.shapes.medium
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .gamepadFocusOutline(shape = shape)
                                .clip(shape)
                                .clickable { onLaunch(app) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconImage(app = app, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(
                                            if (app.isEnabledInConfig) R.string.gamespace_tuned
                                            else R.string.gamespace_untuned
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (app.isEnabledInConfig) {
                                            scheme.primary
                                        } else {
                                            scheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                                TopBarIconButton(
                                    icon = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.gamespace_app_settings),
                                    onClick = { onOpenSettings(app) }
                                )
                            }
                        }
                    }
                }

                if (matches.size > SEARCH_RESULT_CAP && !showAll) {
                    TextButton(
                        onClick = { showAll = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(R.string.gamespace_show_all) + " (${matches.size})"
                        )
                    }
                }
            }
        }
    }
}

/**
 * The manager's App Settings, mounted inside the shell. Handing the job to the manager activity is
 * not an option: both activities are `singleTask`, so starting the manager from here would clear
 * Game Space off the task rather than stack on top of it.
 */
@Composable
private fun ZXGameSpaceAppSettingsOverlay(
    packageName: String,
    onClose: () -> Unit
) {
    val settingsNav = rememberNavController()
    val entry by settingsNav.currentBackStackEntryAsState()
    var landed by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    // The screen pops its own entry when it hands back, and an empty host is the overlay's way out.
    LaunchedEffect(entry) {
        if (entry != null) landed = true else if (landed) onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        NavHost(
            navController = settingsNav,
            startDestination = "app_settings"
        ) {
            composable("app_settings") {
                ScreenWrapper(navController = settingsNav, animatedVisibilityScope = this) {
                    AppSettingsScreen(navController = settingsNav, packageName = packageName)
                }
            }
        }
    }
}

/**
 * Leave prompt for the power button: close the shell and stay on AZenith, or close both. Backing
 * out of the dialog leaves the two alone.
 *
 * A dialog is its own window, so it carries [gamepadKeyWatch] — the activity never sees its keys.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZXGameSpaceExitDialog(
    onDismiss: () -> Unit,
    onExitShell: () -> Unit,
    onExitApp: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.gamepadKeyWatch(),
        icon = { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null) },
        title = { Text(stringResource(R.string.gamespace_exit_title)) },
        text = { Text(stringResource(R.string.gamespace_exit_desc)) },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onExitShell,
                    modifier = Modifier.gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
                ) {
                    Text(stringResource(R.string.gamespace_exit_shell))
                }
                TextButton(
                    onClick = onExitApp,
                    modifier = Modifier.gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
                ) {
                    Text(
                        text = stringResource(R.string.gamespace_exit_app),
                        color = scheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

/**
 * Full-screen atmospheric background that crossfades between the ZXGameSpace contour/symbol wave
 * and a rich game artwork stage with Material 3 tonal scrims.
 */
@Composable
private fun ZXGameSpaceStageBackdrop(entry: RibbonEntry) {
    val scheme = MaterialTheme.colorScheme

    Crossfade(
        targetState = entry,
        label = "ZXGameSpaceBackdropCrossfade"
    ) { current ->
        Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceContainerLowest)) {
            when (current) {
                is RibbonEntry.Game -> {
                    val bitmap = AppIconCache.get(current.app.packageName)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.72f)
                                .align(Alignment.CenterEnd)
                                .blur(36.dp, BlurredEdgeTreatment.Unbounded)
                                .alpha(0.38f)
                        )
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(320.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = (-72).dp, y = (-18).dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .alpha(0.28f)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.banner_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.28f)
                        )
                    }
                }

                else -> {
                    Image(
                        painter = painterResource(R.drawable.banner_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.22f)
                    )
                    ZXGameSpaceContourGlyphCanvas(
                        primaryColor = scheme.primary,
                        tertiaryColor = scheme.tertiary,
                        outlineColor = scheme.outlineVariant
                    )
                }
            }

            // Left-to-right and top-to-bottom Material 3 tonal scrims for guaranteed legibility.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f),
                                scheme.surfaceContainerLowest.copy(alpha = 0.58f),
                                scheme.primaryContainer.copy(alpha = 0.14f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.55f),
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Draws the abstract sweeping contour ribbons and translucent controller symbol geometry seen in the
 * upper-right quadrant of the ZXGameSpace Welcome Hub.
 */
@Composable
private fun ZXGameSpaceContourGlyphCanvas(
    primaryColor: Color,
    tertiaryColor: Color,
    outlineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sweeping topographic contour curves across the stage.
        for (i in 0..4) {
            val shift = i * 26f
            val wave = Path().apply {
                moveTo(w * 0.12f, h * 0.52f + shift)
                cubicTo(
                    w * 0.38f, h * 0.24f + shift,
                    w * 0.64f, h * 0.62f - shift,
                    w * 0.98f, h * 0.18f + shift
                )
            }
            drawPath(
                path = wave,
                color = outlineColor.copy(alpha = 0.16f - i * 0.025f),
                style = Stroke(width = 2.2f)
            )
        }

        // Translucent geometric controller symbols in the upper-right quadrant.
        val cx = w * 0.72f
        val cy = h * 0.34f
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)

        // Circle glyph
        drawCircle(
            color = tertiaryColor.copy(alpha = 0.18f),
            radius = 54f,
            center = Offset(cx + 120f, cy - 28f),
            style = stroke
        )
        // Square/pill glyph
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.18f),
            topLeft = Offset(cx - 88f, cy - 42f),
            size = Size(86f, 86f),
            cornerRadius = CornerRadius(20f, 20f),
            style = stroke
        )
        // Cross glyph
        val crossX = cx + 18f
        val crossY = cy + 24f
        drawLine(
            color = primaryColor.copy(alpha = 0.22f),
            start = Offset(crossX - 36f, crossY - 36f),
            end = Offset(crossX + 36f, crossY + 36f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.22f),
            start = Offset(crossX + 36f, crossY - 36f),
            end = Offset(crossX - 36f, crossY + 36f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        // Triangle glyph
        val tri = Path().apply {
            moveTo(cx + 195f, cy - 95f)
            lineTo(cx + 245f, cy - 10f)
            lineTo(cx + 145f, cy - 10f)
            close()
        }
        drawPath(
            path = tri,
            color = tertiaryColor.copy(alpha = 0.16f),
            style = stroke
        )
    }
}

/**
 * ZXGameSpace top navigation bar: "Games | System" on the left; Search, Settings, Profile Avatar with live
 * status dot, Exit, and Clock on the right.
 */
@Composable
private fun ZXGameSpaceTopBar(
    selectedCategory: TopCategory,
    clock: String,
    battery: Pair<Int, Boolean>,
    isScrolled: Boolean = false,
    onSelectCategory: (TopCategory) -> Unit,
    searchOpen: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopCategory.entries.forEach { tab ->
                val selected = tab == selectedCategory
                val shape = MaterialTheme.shapes.small
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(shape)
                        .gamepadFocusOutline(shape = shape)
                        .clickable { onSelectCategory(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = isScrolled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(18.dp)
                            .background(scheme.outlineVariant.copy(alpha = 0.7f))
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (searchOpen) {
            val searchRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { searchRequester.requestFocus() }

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier
                    .width(300.dp)
                    .focusRequester(searchRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            TopBarIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.gamespace_search_close),
                onClick = onSearchClose
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarIconButton(
                    icon = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.gamespace_library_title),
                onClick = onSearchClick
            )
            TopBarIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.gamespace_rail_settings),
                onClick = onSettingsClick
            )

            // Profile avatar with green/primary online status indicator dot (matches Image 2).
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .gamepadFocusOutline(shape = CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = stringResource(R.string.gamespace_profile),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(30.dp).clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .border(1.5.dp, scheme.surfaceContainerLowest, CircleShape)
                )
            }

            TopBarIconButton(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = stringResource(R.string.gamespace_rail_exit),
                tint = scheme.onSurfaceVariant,
                onClick = onExit
            )

            // Material 3 battery chip beside the clock
            Surface(
                shape = CircleShape,
                color = if (battery.second) scheme.primaryContainer
                        else if (battery.first <= 20) scheme.errorContainer
                        else scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (battery.second) Icons.Rounded.BatteryChargingFull
                                      else if (battery.first <= 20) Icons.Rounded.BatteryAlert
                                      else Icons.Rounded.BatteryStd,
                        contentDescription = stringResource(R.string.gamespace_info_battery),
                        tint = if (battery.second) scheme.onPrimaryContainer
                               else if (battery.first <= 20) scheme.onErrorContainer
                               else scheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${battery.first}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (battery.second) scheme.onPrimaryContainer
                                else if (battery.first <= 20) scheme.onErrorContainer
                                else scheme.onSurface
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            Text(
                text = clock,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface
            )
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .gamepadFocusOutline(shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * ZXGameSpace cross-media tile ribbon:
 * - Unselected tiles are compact 54dp rounded squares aligned to the top of the ribbon.
 * - Selected/focused tile expands smoothly to 88dp with a crisp outer focus ring.
 * - The selected item's badge + title sits immediately to the right of the expanded tile,
 *   directly underneath the adjacent compact tiles, matching the ZXGameSpace home layout.
 */
@Composable
private fun ZXGameSpaceTileRibbon(
    entries: List<RibbonEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onActivate: (RibbonEntry) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var selectedRightEdgePx by remember { mutableStateOf(0f) }
    var ribbonWidthPx by remember { mutableStateOf(0f) }

    LaunchedEffect(selectedIndex, entries.size) {
        // Past the third tile the pointer holds its slot and the row slides beneath it.
        runCatching { listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0)) }
    }

    val activeEntry = entries.getOrElse(selectedIndex) { RibbonEntry.Welcome }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .onGloballyPositioned { ribbonWidthPx = it.size.width.toFloat() }
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .gamepadScreenEntry(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            contentPadding = PaddingValues(end = 180.dp)
        ) {
            itemsIndexed(entries, key = { _, item -> item.key }) { index, item ->
                val isSelected = index == selectedIndex
                ZXGameSpaceRibbonTile(
                    entry = item,
                    selected = isSelected,
                    onFocus = { onSelect(index) },
                    onClick = {
                        if (isSelected) {
                            onActivate(item)
                        } else {
                            onSelect(index)
                        }
                    },
                    modifier = if (isSelected) {
                        Modifier.onGloballyPositioned { coords ->
                            selectedRightEdgePx = coords.positionInParent().x + coords.size.width
                        }
                    } else {
                        Modifier
                    }
                )
            }
        }

        // Adjacent label sits just right of the expanded tile's lower edge.
        val labelOffsetPx = remember(selectedRightEdgePx, ribbonWidthPx, density) {
            val gapPx = with(density) { 12.dp.toPx() }
            val maxPx = (ribbonWidthPx - with(density) { 220.dp.toPx() }).coerceAtLeast(0f)
            (selectedRightEdgePx + gapPx).coerceIn(0f, maxPx).roundToInt()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(x = labelOffsetPx, y = 0) }
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (activeEntry) {
                RibbonEntry.Welcome -> {
                    Text(
                        text = stringResource(R.string.gamespace_welcome),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }

                is RibbonEntry.Game -> {
                    ZXGameSpacePlatformTag(
                        text = if (activeEntry.app.isEnabledInConfig) {
                            stringResource(R.string.gamespace_tuned).uppercase()
                        } else {
                            stringResource(R.string.gamespace_untuned).uppercase()
                        }
                    )
                    Text(
                        text = activeEntry.app.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                RibbonEntry.Library -> {
                    Text(
                        text = stringResource(R.string.gamespace_library_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ZXGameSpaceRibbonTile(
    entry: RibbonEntry,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val colorSpec = motion.defaultEffectsSpec<Color>()

    val tileSize by animateDpAsState(
        targetValue = if (selected) 88.dp else 54.dp,
        animationSpec = spatialSpec,
        label = "RibbonTileSize"
    )
    val outerShape = if (selected) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.onSurface.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = colorSpec,
        label = "RibbonTileBorder"
    )

    Box(
        modifier = modifier
            .size(tileSize)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .gamepadFocusOutline(shape = outerShape, width = 2.dp)
            .clip(outerShape)
            .border(width = if (selected) 1.5.dp else 0.dp, color = borderColor, shape = outerShape)
            .clickable(onClick = onClick)
            .padding(if (selected) 3.dp else 0.dp)
    ) {
        val innerShape = if (selected) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
        Surface(
            shape = innerShape,
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxSize()
        ) {
            when (entry) {
                RibbonEntry.Welcome -> WelcomeGlyphTileContent(selected = selected)
                is RibbonEntry.Game -> GameTileContent(app = entry.app, selected = selected)
                RibbonEntry.Library -> LibraryTileContent()
            }
        }

        // Enabled/disabled at a glance: a filled dot when the app carries the tweaks, a hollow
        // ring when it does not.
        if (entry is RibbonEntry.Game) {
            val on = entry.app.isEnabledInConfig
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (selected) 5.dp else 3.dp)
                    .size(if (selected) 12.dp else 9.dp)
                    .clip(CircleShape)
                    .background(if (on) scheme.primary else scheme.surfaceContainerHighest)
                    .border(
                        width = 1.dp,
                        color = if (on) scheme.primary else scheme.outline.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * The signature 2x2 controller symbol quad (Triangle, Circle, Cross, Square) on the Welcome tile.
 */
@Composable
private fun WelcomeGlyphTileContent(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerHighest,
                        scheme.surfaceContainerLow
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val glyphSpan = if (selected) 34.dp else 22.dp
        Canvas(modifier = Modifier.size(glyphSpan)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = if (selected) 4.2f else 3f, cap = StrokeCap.Round)
            val color = scheme.onSurface

            // Top-Left: Square
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.08f, h * 0.08f),
                size = Size(w * 0.34f, h * 0.34f),
                cornerRadius = CornerRadius(3f, 3f),
                style = stroke
            )
            // Top-Right: Triangle
            val tri = Path().apply {
                moveTo(w * 0.75f, h * 0.08f)
                lineTo(w * 0.93f, h * 0.42f)
                lineTo(w * 0.57f, h * 0.42f)
                close()
            }
            drawPath(path = tri, color = color, style = stroke)

            // Bottom-Left: Cross (X)
            drawLine(
                color = color,
                start = Offset(w * 0.08f, h * 0.58f),
                end = Offset(w * 0.42f, h * 0.92f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(w * 0.42f, h * 0.58f),
                end = Offset(w * 0.08f, h * 0.92f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )

            // Bottom-Right: Circle
            drawCircle(
                color = color,
                radius = w * 0.17f,
                center = Offset(w * 0.75f, h * 0.75f),
                style = stroke
            )
        }
    }
}

@Composable
private fun GameTileContent(app: ApplistViewmodel.AppInfo, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIconImage(app = app, size = if (selected) 82.dp else 54.dp)
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LibraryTileContent() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.GridView,
            contentDescription = stringResource(R.string.gamespace_library_title),
            tint = scheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Stage 1: the home sheet. The bento wall used to sit in the stage's leftover height and clipped
 * at its edge, so what the normal-mode home reports is carried here instead -- one widget per
 * reading, and the sheet scrolls rather than cutting anything off. Scrolling it past the ribbon's
 * room slides the sheet up to full height and adds the readings the collapsed view has no space
 * for; scrolling back to the top hands that room back to the app list.
 */
@Composable
private fun WelcomeHubStage(
    scroll: ScrollState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    profile: String,
    profileValue: String,
    games: List<ApplistViewmodel.AppInfo>,
    allAppsCount: Int,
    refreshHz: Int,
    deviceName: String,
    chipsetName: String,
    onApplyProfile: (String) -> Unit,
    onOpenSystem: () -> Unit,
    onSelectGame: (ApplistViewmodel.AppInfo) -> Unit,
    onLaunchGame: (ApplistViewmodel.AppInfo) -> Unit,
    onOpenLibrary: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val wordmark = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    // Read through these in the gate: it outlives the composition that handed it the callbacks.
    val expandedNow = rememberUpdatedState(expanded)
    val notifyExpanded = rememberUpdatedState(onExpandedChange)
    // The drag that expands the sheet is spent on the expansion; the next one does the scrolling.
    var stageHold by remember { mutableStateOf(false) }

    val stageGate = remember(scroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val fromDrag = source == NestedScrollSource.UserInput
                if (stageHold && fromDrag && available.y < 0f) return available
                if (!expandedNow.value && available.y < 0f) {
                    val projected = scroll.value + available.y
                    if (projected >= HOME_EXPAND_AT) {
                        if (fromDrag) stageHold = true
                        notifyExpanded.value(true)
                        // Full screen puts the sheet at its own top, readings below the bar.
                        scope.launch { runCatching { scroll.animateScrollTo(0) } }
                        return Offset(0f, projected - HOME_EXPAND_AT)
                    }
                }
                if (expandedNow.value && fromDrag && available.y > 0f && scroll.value <= 0) {
                    stageHold = false
                    notifyExpanded.value(false)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    // Root and module are one-shot probes; the service is watched instead because the daemon can
    // restart underneath an open sheet and a stale pid would be worse than none.
    val rootGranted by produceState<Boolean?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { RootUtils.isRootGranted() }
    }
    val moduleInstalled by produceState<Boolean?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { RootUtils.isModuleInstalled() }
    }
    val service by RootUtils.observeServiceStatusRes()
        .collectAsState(initial = R.string.status_initializing to "")

    val rootValue = when (rootGranted) {
        true -> stringResource(R.string.root_granted)
        false -> stringResource(R.string.root_not_granted)
        else -> stringResource(R.string.status_checking)
    }
    val moduleValue = when (moduleInstalled) {
        true -> stringResource(service.first)
        false -> stringResource(R.string.module_not_installed)
        else -> stringResource(R.string.status_checking)
    }
    val suspended = stringResource(R.string.status_suspended)
    val pidValue = service.second.ifBlank { suspended }

    val topRoom by animateDpAsState(
        targetValue = if (expanded) 64.dp else 142.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>(),
        label = "WelcomeTopRoom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(stageGate)
            // A new touch is a new scroll: the drag that paid for the expansion is over.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                            if (change.changedToDown()) stageHold = false
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topRoom)
                    .clipToBounds(),
                contentAlignment = Alignment.BottomStart
            ) {
                HomeWordmark(
                    scroll = scroll,
                    held = expanded,
                    text = wordmark,
                    deviceName = deviceName,
                    chipsetName = chipsetName
                )
            }

            // Row 1: Core System & Performance Status (4 balanced cards across the width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = if (rootGranted == true) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                    label = stringResource(R.string.root_access),
                    value = rootValue,
                    container = if (rootGranted == false) scheme.errorContainer else scheme.primaryContainer,
                    onContainer = if (rootGranted == false) scheme.onErrorContainer else scheme.onPrimaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Memory,
                    label = stringResource(R.string.gamespace_info_module),
                    value = moduleValue,
                    container = if (moduleInstalled == false) scheme.errorContainer else scheme.secondaryContainer,
                    onContainer = if (moduleInstalled == false) scheme.onErrorContainer else scheme.onSecondaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Tune,
                    label = stringResource(R.string.gamespace_info_pid),
                    value = pidValue,
                    container = scheme.tertiaryContainer,
                    onContainer = scheme.onTertiaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Token,
                    label = stringResource(R.string.current_profile),
                    value = profile,
                    container = scheme.surfaceContainerHigh,
                    onContainer = scheme.onSurface,
                    badgeText = "CYCLE PROFILE",
                    onClick = {
                        val next = when (profileValue) {
                            "1" -> "2"
                            "2" -> "3"
                            else -> "1"
                        }
                        onApplyProfile(next)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: Landscape Bento Deck (2-column balanced layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Hardware & System Architecture Deck (55% width)
                HardwareSpecsBentoCard(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    deviceName = deviceName,
                    chipsetName = chipsetName,
                    refreshHz = refreshHz,
                    onOpenSystem = onOpenSystem
                )

                // Column 2: Gaming Library & Quick Action Deck (45% width)
                GamingLibraryBentoCard(
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                    gamesCount = games.size,
                    allAppsCount = allAppsCount,
                    onOpenLibrary = onOpenLibrary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 3: Community & Knowledge Links (Side-by-side 50/50 split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HubLinkTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.support_us),
                    description = stringResource(R.string.support_us_desc),
                    container = scheme.secondaryContainer.copy(alpha = 0.85f),
                    onContainer = scheme.onSecondaryContainer,
                    onClick = { uriHandler.openUri("https://t.me/ZeshArch") }
                )
                HubLinkTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.learn_more),
                    description = stringResource(R.string.learn_more_desc),
                    container = scheme.tertiaryContainer.copy(alpha = 0.85f),
                    onContainer = scheme.onTertiaryContainer,
                    onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** The wordmark at rest: hero size while the sheet is whole, shrinking off its own baseline as
 * the bar below it takes over. */
@Composable
private fun HomeWordmark(
    scroll: ScrollState,
    held: Boolean,
    text: String,
    deviceName: String,
    chipsetName: String
) {
    val scheme = MaterialTheme.colorScheme
    // Holds it down once the sheet is full screen, where the offset has gone back to zero.
    val hold by animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>(),
        label = "HomeWordmarkHold"
    )

    Column(
        modifier = Modifier
            .padding(start = 24.dp, bottom = 12.dp)
            .graphicsLayer {
                val ramp = (scroll.value / HOME_EXPAND_AT.toFloat()).coerceIn(0f, 1f)
                val progress = maxOf(ramp, hold)
                alpha = (1f - progress * 1.25f).coerceIn(0f, 1f)
                val scale = 1f - 0.35f * progress
                scaleX = scale
                scaleY = scale
                translationY = -progress * 28f
                transformOrigin = TransformOrigin(0f, 1f)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    lineHeight = 74.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                ),
                color = scheme.onSurface,
                maxLines = 1
            )
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    text = "GAME SPACE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = "$deviceName \u00b7 $chipsetName",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )
    }
}

@Composable
private fun HardwareSpecsBentoCard(
    modifier: Modifier = Modifier,
    deviceName: String,
    chipsetName: String,
    refreshHz: Int,
    onOpenSystem: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onOpenSystem),
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.device_info).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$deviceName \u00b7 $chipsetName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Speed,
                    label = stringResource(R.string.refreshrates),
                    value = stringResource(R.string.refresh_rate_format, refreshHz.toString())
                )
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Android,
                    label = stringResource(R.string.android_version),
                    value = "Android ${Build.VERSION.RELEASE}"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Memory,
                    label = "ARCH",
                    value = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
                )
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Check,
                    label = "SECURITY",
                    value = Build.VERSION.SECURITY_PATCH.ifBlank { "Up to date" }
                )
            }
        }
    }
}

@Composable
private fun SpecPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerHighest.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GamingLibraryBentoCard(
    modifier: Modifier = Modifier,
    gamesCount: Int,
    allAppsCount: Int,
    onOpenLibrary: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onOpenLibrary),
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.tertiaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gamespace_library_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.tertiary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$gamesCount ${stringResource(R.string.gamespace_info_games).lowercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.7f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.gamespace_info_games).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$gamesCount Tuned",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.7f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.gamespace_info_apps).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$allAppsCount Apps",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.gamespace_library_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** One reading on the sheet, at the stage's scale, filled in its own container role. */
@Composable
private fun HubInfoTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = onClick != null)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = shape,
        color = container
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = onContainer.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

/** Support and Learn More, carried as links the way the normal-mode home carries them. */
@Composable
private fun HubLinkTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onClick),
        shape = shape,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = onContainer.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = onContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun ActiveProfilesBentoCard(
    profile: String,
    profileValue: String,
    tunedCount: Int,
    chipsetName: String,
    deviceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.gamespace_active_profiles),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = tunedCount.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
            }

            // 4 circular profile/avatar medallions mirroring the 4 friend avatars in Image 1.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_performance,
                    active = profileValue == "1",
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                )
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_balanced,
                    active = profileValue == "2" || profileValue.isBlank(),
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_eco,
                    active = profileValue == "3",
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer
                )
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                )
            }

            Text(
                text = "$profile · $chipsetName\n$deviceName",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileCircleMedallion(
    iconRes: Int,
    active: Boolean,
    containerColor: Color,
    contentColor: Color
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(40.dp)
            .then(
                if (active) Modifier.border(2.dp, scheme.primary, CircleShape)
                else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EngineBannerBentoCard(
    featuredGame: ApplistViewmodel.AppInfo?,
    deviceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.banner_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.55f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f),
                                scheme.surfaceContainerLowest.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.gamespace_engine_store),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }

                Text(
                    text = featuredGame?.label ?: deviceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ZXGameSpaceDualBadge(left = "AZENITH", right = "M3")
            }
        }
    }
}

@Composable
private fun PerfThermalBentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.gamespace_perf_thermal),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }

                Text(
                    text = stringResource(R.string.gamespace_perf_thermal_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { rotationZ = -14f }
                    .border(2.dp, scheme.primary.copy(alpha = 0.6f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizationTrophiesBentoCard(
    profile: String,
    profileValue: String,
    tunedCount: Int,
    gamesCount: Int,
    allAppsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val ratio = remember(tunedCount, gamesCount) {
        if (gamesCount > 0) (tunedCount.toFloat() / gamesCount.toFloat()).coerceIn(0.08f, 1f)
        else 0.33f
    }
    val percent = (ratio * 100f).roundToInt()
    val profileLevel = when (profileValue) {
        "1" -> 3
        "2" -> 2
        "3" -> 1
        else -> 2
    }

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.gamespace_trophies_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.gamespace_total_format, allAppsCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface
                )
            }

            // 4 Trophy-style stat columns (Platinum, Gold, Silver, Bronze tones).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrophyStatColumn(
                    icon = Icons.Rounded.EmojiEvents,
                    tint = scheme.primary,
                    count = tunedCount
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.WorkspacePremium,
                    tint = scheme.tertiary,
                    count = gamesCount
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.Speed,
                    tint = scheme.secondary,
                    count = profileLevel
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.Apps,
                    tint = scheme.onSurfaceVariant,
                    count = allAppsCount
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = scheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = profile.ifBlank { "—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                // Bottom progress bar on Material 3 tokens.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .clip(CircleShape)
                            .background(scheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophyStatColumn(
    icon: ImageVector,
    tint: Color,
    count: Int
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.16f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface
        )
    }
}

/**
 * 3 Circular Hardware Rings Card (mirrors the 3 Controller Battery Rings in Image 1).
 */
@Composable
private fun HardwareRingsBentoCard(
    battery: Pair<Int, Boolean>,
    ramState: Pair<Float, String>,
    refreshHz: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularTelemetryGauge(
                progress = (battery.first / 100f).coerceIn(0.05f, 1f),
                arcColor = scheme.primary,
                icon = Icons.Rounded.SportsEsports,
                subLabel = "${battery.first}%"
            )
            CircularTelemetryGauge(
                progress = ramState.first.coerceIn(0.05f, 1f),
                arcColor = scheme.tertiary,
                icon = Icons.Rounded.Memory,
                subLabel = ramState.second
            )
            CircularTelemetryGauge(
                progress = (refreshHz / 144f).coerceIn(0.25f, 1f),
                arcColor = scheme.secondary,
                icon = Icons.Rounded.Speed,
                subLabel = "${refreshHz}Hz"
            )
        }
    }
}

@Composable
private fun CircularTelemetryGauge(
    progress: Float,
    arcColor: Color,
    icon: ImageVector,
    subLabel: String
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.surfaceContainerHighest

    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpotlightBentoCard(
    game: ApplistViewmodel.AppInfo?,
    chipsetName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.schemeillust),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.65f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.72f),
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.gamespace_spotlight),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = game?.label ?: chipsetName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ZXGameSpaceDualBadge(
                        left = "AZENITH",
                        right = stringResource(
                            if (game?.isEnabledInConfig == true) R.string.gamespace_tuned
                            else R.string.gamespace_untuned
                        ).uppercase()
                    )
                }
            }
        }
    }
}

/**
 * Dual-tone split badge mirroring the compact platform tag pill.
 */
@Composable
private fun ZXGameSpaceDualBadge(left: String, right: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.surface,
            modifier = Modifier
                .background(scheme.onSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(
            text = right,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.onSurface,
            modifier = Modifier
                .background(scheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ZXGameSpacePlatformTag(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = scheme.onSurface,
        contentColor = scheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Stage 2: ZXGameSpace game hero stage.
 * - Left hero column: icon, title, tuned/untuned line with the package name
 * - Far right corner: Play and App Settings
 */
@Composable
private fun GameHeroStage(
    app: ApplistViewmodel.AppInfo,
    onLaunch: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Hero Column: Emblem/Title + Subtitle + [Play Game] [...]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconImage(app = app, size = 52.dp)
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = buildString {
                        append(
                            stringResource(
                                if (app.isEnabledInConfig) R.string.gamespace_tuned
                                else R.string.gamespace_untuned
                            )
                        )
                        append(" · ")
                        append(app.packageName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }

            // Play and the App Settings gear own the far right corner once the pointer has
            // walked past the third tile.
            var gearFocused by remember { mutableStateOf(false) }
            val gearBg by animateColorAsState(
                targetValue = if (gearFocused) {
                    scheme.secondaryContainer
                } else {
                    scheme.surfaceContainerHighest.copy(alpha = 0.82f)
                },
                animationSpec = colorSpec,
                label = "GearBtnBg"
            )

            Surface(
                shape = CircleShape,
                color = gearBg,
                contentColor = if (gearFocused) scheme.onSecondaryContainer else scheme.onSurface,
                modifier = Modifier
                    .size(44.dp)
                    .onFocusChanged { gearFocused = it.isFocused }
                    .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenAppSettings)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.gamespace_app_settings),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            var playFocused by remember { mutableStateOf(false) }
            val playBg by animateColorAsState(
                targetValue = if (playFocused) {
                    scheme.primaryContainer
                } else {
                    scheme.surfaceContainerHighest.copy(alpha = 0.88f)
                },
                animationSpec = colorSpec,
                label = "PlayBtnBg"
            )
            val playFg by animateColorAsState(
                targetValue = if (playFocused) scheme.onPrimaryContainer else scheme.onSurface,
                animationSpec = colorSpec,
                label = "PlayBtnFg"
            )

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = playBg,
                contentColor = playFg,
                modifier = Modifier
                    .height(44.dp)
                    .onFocusChanged { playFocused = it.isFocused }
                    .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLaunch)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.gamespace_play_game),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

    }
}

/**
 * Stage 3: Full Game Library Grid when the rightmost Library tile is selected.
 * Uses Material 3 Expressive FilterChips (trailing check icon + animateContentSize).
 */
@Composable
private fun LibraryGridStage(
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo>,
    expanded: Boolean,
    onOpenSettings: (ApplistViewmodel.AppInfo) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var showAllApps by remember { mutableStateOf(games.isEmpty()) }
    var onlyEnabled by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val expandedNow = rememberUpdatedState(expanded)
    val notifyExpanded = rememberUpdatedState(onExpandedChange)

    // The grid takes the ribbon's room once it has been scrolled, and only a pull at the very top
    // hands the room back — a list barely taller than the viewport must not flip the two.
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > 0 || offset > 60) notifyExpanded.value(true)
            }
    }
    val gridPull = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                if (!expandedNow.value) return Offset.Zero
                if (gridState.firstVisibleItemIndex > 0) return Offset.Zero
                if (gridState.firstVisibleItemScrollOffset > 0) return Offset.Zero
                notifyExpanded.value(false)
                return Offset.Zero
            }
        }
    }

    val baseList = if (showAllApps) allApps else games
    val displayed = remember(baseList, onlyEnabled) {
        if (onlyEnabled) baseList.filter { it.isEnabledInConfig } else baseList
    }

    Column(modifier = Modifier.fillMaxSize().nestedScroll(gridPull)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveFilterChip(
                selected = !showAllApps,
                label = "${stringResource(R.string.gamespace_tab_games)} (${games.size})",
                onClick = { showAllApps = false }
            )
            ExpressiveFilterChip(
                selected = showAllApps,
                label = "${stringResource(R.string.gamespace_all_apps)} (${allApps.size})",
                onClick = { showAllApps = true }
            )
            ExpressiveFilterChip(
                selected = onlyEnabled,
                label = stringResource(R.string.gamespace_tuned),
                onClick = { onlyEnabled = !onlyEnabled }
            )

            Spacer(Modifier.weight(1f))

            TopBarIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.gamespace_library_close),
                onClick = onClose
            )
        }

        Spacer(Modifier.height(8.dp))

        if (displayed.isEmpty()) {
            EmptyPane(
                icon = Icons.Rounded.GridView,
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(displayed, key = { it.packageName }) { app ->
                    val shape = MaterialTheme.shapes.medium
                    Surface(
                        shape = shape,
                        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                        modifier = Modifier
                            .height(64.dp)
                            .gamepadFocusOutline(shape = shape)
                            .clip(shape)
                            // Settings first: the tile is a way into the app's tuning, not a launch.
                            .clickable { onOpenSettings(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(app = app, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        if (app.isEnabledInConfig) R.string.gamespace_tuned
                                        else R.string.gamespace_untuned
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (app.isEnabledInConfig) scheme.primary else scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()

    val container by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = colorSpec,
        label = "ChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = colorSpec,
        label = "ChipContent"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            AnimatedVisibility(visible = selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            selectedContainerColor = container,
            labelColor = content,
            selectedLabelColor = content,
            iconColor = content,
            selectedTrailingIconColor = content
        ),
        modifier = Modifier
            .animateContentSize(spatialSpec)
            .gamepadFocusOutline(shape = MaterialTheme.shapes.small)
    )
}

/**
 * Stage 4: System / Performance Control Deck when the top "System" tab is selected.
 */
@Composable
private fun SystemControlStage(
    profile: String,
    profileValue: String,
    battery: Pair<Int, Boolean>,
    ramState: Pair<Float, String>,
    refreshHz: Int,
    deviceName: String,
    chipsetName: String,
    onApplyProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_performance).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = scheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SystemProfileCard(
                labelRes = R.string.Profile_Performance,
                iconRes = R.drawable.ic_performance,
                active = profileValue == "1",
                onClick = { onApplyProfile("1") },
                modifier = Modifier.weight(1f)
            )
            SystemProfileCard(
                labelRes = R.string.Profile_Balanced,
                iconRes = R.drawable.ic_balanced,
                active = profileValue == "2" || profileValue.isBlank(),
                onClick = { onApplyProfile("2") },
                modifier = Modifier.weight(1f)
            )
            SystemProfileCard(
                labelRes = R.string.Profile_ECO_mode,
                iconRes = R.drawable.ic_eco,
                active = profileValue == "3",
                onClick = { onApplyProfile("3") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.current_profile),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = profile.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "$deviceName · $chipsetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            HardwareRingsBentoCard(
                battery = battery,
                ramState = ramState,
                refreshHz = refreshHz,
                onClick = {},
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SystemProfileCard(
    labelRes: Int,
    iconRes: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (active) scheme.onPrimaryContainer else scheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Tonal badge over a headline: resting empty state. */
@Composable
internal fun EmptyPane(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

@Composable
private fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(10_000)
        }
    }
    return remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
}

@Composable
private fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readBattery(context)
            delay(30_000)
        }
    }
    return state
}

private fun readBattery(context: Context): Pair<Int, Boolean> {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return 0 to false
    val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return level to charging
}

@Composable
private fun rememberRamState(): Pair<Float, String> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readRam(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readRam(context)
            delay(15_000)
        }
    }
    return state
}

private fun readRam(context: Context): Pair<Float, String> {
    val am = context.getSystemService(ActivityManager::class.java) ?: return 0.5f to "—"
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    if (info.totalMem <= 0L) return 0.5f to "—"
    val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
    val fraction = (used.toFloat() / info.totalMem.toFloat()).coerceIn(0f, 1f)
    val usedGb = used.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return fraction to String.format(java.util.Locale.US, "%.1fG", usedGb)
}
