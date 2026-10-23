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

package zx.azenith.ui.gamespace

import android.content.Context
import android.view.KeyEvent
import zx.azenith.ui.theme.ColorMode
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import zx.azenith.ui.component.gamepadFocusOutline
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.ui.component.GamepadFocusRoot
import zx.azenith.ui.component.ZXDropDownNav
import zx.azenith.ui.util.TapHapticState
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.viewmodel.ApplistViewmodel

/**
 * ZXGameSpace Material 3 Expressive home screen with full touch and controller support.
 * Coordinates the Ribbon, Hero/Hub/Library Stages, Overlays, and Settings panels.
 */
@Composable
fun ZXGameSpaceHub(
    introActive: Boolean = false,
    profile: String,
    profileValue: String = "",
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo> = games,
    onApplyProfile: (String) -> Unit = {},
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onExit: () -> Unit,
    onExitApp: () -> Unit = {},
    loading: Boolean = false
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val effectsSpec = motion.defaultEffectsSpec<Float>()
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "gamespace_app_scale") {
                appScalePercent = prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val scope = rememberCoroutineScope()
    val ribbonFocusRequester = remember { FocusRequester() }
    var category by rememberSaveable { mutableStateOf(TopCategory.Games) }
    var selectedSettingsTab by rememberSaveable { mutableStateOf(SettingsSidebarTab.General) }

    LaunchedEffect(category) {
        if (category != TopCategory.Settings) {
            selectedSettingsTab = SettingsSidebarTab.General
        }
    }
    var selectedRibbonIndex by remember { mutableIntStateOf(0) }
    var exitPrompt by remember { mutableStateOf(false) }

    var prevExitPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(exitPrompt) {
        if (exitPrompt && !prevExitPrompt) {
            zx.azenith.ui.util.AudioUtil.playNotice(context)
        } else if (!exitPrompt && prevExitPrompt) {
            zx.azenith.ui.util.AudioUtil.playNoticeBack(context)
        }
        prevExitPrompt = exitPrompt
    }

    var tuningPkg by remember { mutableStateOf<String?>(null) }
    var wallpaperPkg by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var quickSettingsOpen by remember { mutableStateOf(false) }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var nowPlayingAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var pendingLaunchApp by remember { mutableStateOf<ApplistViewmodel.AppInfo?>(null) }
    var sidePanelPromptOpen by remember { mutableStateOf(false) }
    val requestAppLaunch: (ApplistViewmodel.AppInfo) -> Unit = { app ->
        val setupDone = prefs.getBoolean("gamespace_sidepanel_setup_done", false)
        if (!setupDone) {
            pendingLaunchApp = app
            sidePanelPromptOpen = true
        } else {
            ZXSidePanelService.show(context, app.packageName, app.label)
            onLaunch(app)
        }
    }
    val welcomeScroll = rememberScrollState()
    var homeExpanded by remember { mutableStateOf(false) }
    var libraryExpanded by remember { mutableStateOf(false) }

    val enabledApps = remember(games, allApps) {
        (games + allApps)
            .distinctBy { it.packageName }
            .filter { it.isEnabledInConfig }
            .sortedBy { it.label.lowercase() }
    }

    val ribbonEntries = remember(enabledApps) {
        buildList {
            add(RibbonEntry.Welcome)
            enabledApps.forEach { add(RibbonEntry.Game(it)) }
            add(RibbonEntry.Library)
            add(RibbonEntry.WrapToFirst)
        }
    }

    var previousRibbonIndex by remember { mutableIntStateOf(0) }
    val safeIndex = selectedRibbonIndex.coerceIn(0, ribbonEntries.lastIndex)
    val isNavigatingForward = (safeIndex >= previousRibbonIndex && !(previousRibbonIndex == 0 && safeIndex == ribbonEntries.lastIndex)) ||
        (previousRibbonIndex == ribbonEntries.lastIndex && safeIndex == 0)
    SideEffect {
        previousRibbonIndex = safeIndex
    }
    val activeEntry = ribbonEntries[safeIndex]
    val activeStageType = activeEntry.stageType
    var lastGameApp by remember { mutableStateOf(enabledApps.firstOrNull()) }
    if (activeEntry is RibbonEntry.Game) {
        lastGameApp = activeEntry.app
    }

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
    val overlayHazeState = remember { HazeState() }
    val backdropHazeState = remember { HazeState() }
    val baseDensity = LocalDensity.current
    val scaleFactor = appScalePercent.coerceIn(60, 150) / 100f

    val handleBack: () -> Unit = {
        if (exitPrompt) {
            exitPrompt = false
        } else if (sidePanelPromptOpen) {
            sidePanelPromptOpen = false
            pendingLaunchApp = null
        } else if (tuningPkg != null) {
            tuningPkg = null
        } else if (wallpaperPkg != null) {
            wallpaperPkg = null
        } else if (nowPlayingOpen) {
            nowPlayingOpen = false
        } else if (quickSettingsOpen) {
            quickSettingsOpen = false
        } else if (category == TopCategory.Settings) {
            selectedSettingsTab = SettingsSidebarTab.General
            category = TopCategory.Games
        } else if (searchOpen) {
            searchOpen = false
            searchQuery = ""
        } else if (activeEntry == RibbonEntry.Welcome && homeExpanded) {
            homeExpanded = false
        } else if (activeEntry == RibbonEntry.Library && libraryExpanded) {
            libraryExpanded = false
        } else if (selectedRibbonIndex != 0) {
            selectedRibbonIndex = 0
        } else {
            exitPrompt = true
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * scaleFactor,
            fontScale = baseDensity.fontScale
        )
    ) {
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
                            category = TopCategory.Settings
                            true
                        }
                        KeyEvent.KEYCODE_BUTTON_B,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            // A menu overlays the shell, so the pad's back closes the menu
                            // instead of walking out of the screen underneath it.
                            if (ZXDropDownNav.isActive) {
                                ZXDropDownNav.dismissTop()
                            } else {
                                handleBack()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (activeEntry == RibbonEntry.Welcome && homeExpanded) {
                                homeExpanded = false
                                scope.launch {
                                    welcomeScroll.animateScrollTo(0)
                                    delay(50)
                                    runCatching { ribbonFocusRequester.requestFocus() }
                                }
                                true
                            } else if (activeEntry == RibbonEntry.Library && libraryExpanded) {
                                libraryExpanded = false
                                scope.launch {
                                    delay(50)
                                    runCatching { ribbonFocusRequester.requestFocus() }
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Dynamic full-bleed atmospheric backdrop driven by the selected ribbon entry.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(backdropHazeState)
                .hazeSource(overlayHazeState, zIndex = 0f)
        ) {
            ZXGameSpaceStageBackdrop(
                entry = activeEntry
            )
        }

        val isAmoled = remember { prefs.getInt("color_mode", ColorMode.SYSTEM.value) == ColorMode.DARKAMOLED.value }
        val isHomescreenScrolled = category == TopCategory.Games && activeEntry == RibbonEntry.Welcome && (homeExpanded || welcomeScroll.value > 10)
        val isLibrary = category == TopCategory.Games && activeEntry == RibbonEntry.Library
        val isSettings = category == TopCategory.Settings

        val targetBlurAlpha = when {
            isSettings -> 1f
            isLibrary -> 1f
            searchOpen -> 1f
            isHomescreenScrolled -> {
                if (homeExpanded) 1f else (welcomeScroll.value.toFloat() / 180f).coerceIn(0f, 1f)
            }
            else -> 0f
        }

        // Focused background atmospheric blur over the wallpaper when scrolling into widgets, on Library, or in Settings
        val backdropBlurAlpha by animateFloatAsState(
            targetValue = targetBlurAlpha,
            animationSpec = motion.defaultEffectsSpec(),
            label = "BackdropBlurAlpha"
        )
        if (backdropBlurAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropBlurAlpha }
                    .hazeBlur(
                        input = HazeInput.Sources(backdropHazeState),
                        style = HazeBlurStyle.Material3(
                            containerColor = scheme.surfaceContainerLowest.copy(alpha = if (isAmoled) 0.60f else 0.52f)
                        ) {
                            blurRadius(28.dp)
                        }
                    )
            )
        }

        val isScrolled = category == TopCategory.Games && activeEntry == RibbonEntry.Welcome && (homeExpanded || welcomeScroll.value > 15)
        val isHomeLogoGone = (category != TopCategory.Games) ||
            (activeEntry != RibbonEntry.Welcome) ||
            (activeEntry == RibbonEntry.Welcome && (homeExpanded || welcomeScroll.value > 15))
        val topBarScrimAlpha by animateFloatAsState(
            targetValue = if (isScrolled) 1f else 0f,
            animationSpec = motion.defaultEffectsSpec<Float>(),
            label = "TopBarScrimAlpha"
        )

        // Continuous edge-to-edge top bar gradient scrim merging with the status bar
        if (topBarScrimAlpha > 0.01f && category == TopCategory.Games) {
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

        val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val startSafePad = maxOf(32.dp, cutoutInsets.calculateStartPadding(layoutDirection))
        val endSafePad = maxOf(32.dp, cutoutInsets.calculateEndPadding(layoutDirection))
        val topSafePad = maxOf(14.dp, cutoutInsets.calculateTopPadding())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(overlayHazeState, zIndex = 1f)
                .padding(
                    start = startSafePad,
                    end = endSafePad,
                    top = topSafePad,
                    bottom = 0.dp
                )
        ) {
            ZXGameSpaceTopBar(
                selectedCategory = category,
                clock = clock,
                battery = battery,
                isHomeLogoGone = isHomeLogoGone,
                onSelectCategory = {
                    searchOpen = false
                    searchQuery = ""
                    if (it != TopCategory.Settings) {
                        selectedSettingsTab = SettingsSidebarTab.General
                    }
                    category = it
                },
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
                    searchOpen = false
                    searchQuery = ""
                    if (category == TopCategory.Settings) {
                        selectedSettingsTab = SettingsSidebarTab.General
                        category = TopCategory.Games
                    } else {
                        selectedSettingsTab = SettingsSidebarTab.General
                        category = TopCategory.Settings
                    }
                },
                onAvatarClick = {
                    searchOpen = false
                    searchQuery = ""
                    val nextProfile = when (profileValue) {
                        "1" -> "2"
                        "2" -> "3"
                        else -> "1"
                    }
                    onApplyProfile(nextProfile)
                },
                onStatusClick = {
                    searchOpen = false
                    searchQuery = ""
                    quickSettingsOpen = !quickSettingsOpen
                },
                isNowPlayingOpen = nowPlayingOpen,
                onNowPlayingClick = {
                    searchOpen = false
                    searchQuery = ""
                    nowPlayingOpen = !nowPlayingOpen
                },
                onNowPlayingAnchor = { nowPlayingAnchor = it },
                onLogoClick = {
                    searchOpen = false
                    searchQuery = ""
                    category = TopCategory.Games
                    selectedRibbonIndex = 0
                    homeExpanded = false
                    scope.launch {
                        welcomeScroll.animateScrollTo(0)
                    }
                },
                onExit = {
                    searchOpen = false
                    searchQuery = ""
                    exitPrompt = true
                }
            )

            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = category,
                    transitionSpec = {
                        (fadeIn(animationSpec = motion.defaultEffectsSpec()) +
                            scaleIn(animationSpec = motion.defaultSpatialSpec(), initialScale = 0.98f))
                            .togetherWith(
                                fadeOut(animationSpec = motion.defaultEffectsSpec()) +
                                    scaleOut(animationSpec = motion.defaultSpatialSpec(), targetScale = 1.01f)
                            )
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "CategoryStageTransition"
                ) { currentCategory ->
                    when (currentCategory) {
                        TopCategory.Games -> {
                            // Games Stage
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                val isRibbonVisible = !searchOpen &&
                                    !(activeEntry == RibbonEntry.Welcome && homeExpanded) &&
                                    !(activeEntry == RibbonEntry.Library && libraryExpanded)
                                AnimatedVisibility(
                                    visible = isRibbonVisible,
                                    enter = fadeIn(effectsSpec) + expandVertically(spatialSpec),
                                    exit = fadeOut(effectsSpec) + shrinkVertically(spatialSpec)
                                ) {
                                    Column {
                                        ZXGameSpaceTileRibbon(
                                            entries = ribbonEntries,
                                            selectedIndex = safeIndex,
                                            onSelect = {
                                                selectedRibbonIndex = it
                                                if (ribbonEntries.getOrNull(it) != RibbonEntry.Welcome) {
                                                    homeExpanded = false
                                                }
                                            },
                                            onActivate = { entry ->
                                                when (entry) {
                                                    is RibbonEntry.Game -> requestAppLaunch(entry.app)
                                                    RibbonEntry.Welcome -> {
                                                        enabledApps.firstOrNull()?.let { selectedRibbonIndex = 1 }
                                                    }
                                                    RibbonEntry.Library -> {
                                                        selectedRibbonIndex = ribbonEntries.indexOf(RibbonEntry.Library)
                                                    }
                                                    RibbonEntry.WrapToFirst -> {
                                                        selectedRibbonIndex = ribbonEntries.indexOf(RibbonEntry.WrapToFirst)
                                                    }
                                                }
                                            },
                                            focusRequester = ribbonFocusRequester
                                        )

                                        Spacer(Modifier.height(10.dp))
                                    }
                                }

                                val canSwipeStage = !searchOpen &&
                                    !(activeEntry == RibbonEntry.Welcome && homeExpanded) &&
                                    !(activeEntry == RibbonEntry.Library && libraryExpanded)
                                var accumulatedDragX by remember { mutableFloatStateOf(0f) }
                                var hasSwipedInCurrentGesture by remember { mutableStateOf(false) }
                                val haptic = LocalHapticFeedback.current
                                val density = LocalDensity.current

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .then(
                                            if (canSwipeStage) {
                                                Modifier.pointerInput(safeIndex, ribbonEntries.size) {
                                                    detectHorizontalDragGestures(
                                                        onDragStart = {
                                                            accumulatedDragX = 0f
                                                            hasSwipedInCurrentGesture = false
                                                        },
                                                        onDragEnd = {
                                                            accumulatedDragX = 0f
                                                            hasSwipedInCurrentGesture = false
                                                        },
                                                        onDragCancel = {
                                                            accumulatedDragX = 0f
                                                            hasSwipedInCurrentGesture = false
                                                        },
                                                        onHorizontalDrag = { change, dragAmount ->
                                                            if (!hasSwipedInCurrentGesture) {
                                                                 accumulatedDragX += dragAmount
                                                                val thresholdPx = with(density) { 32.dp.toPx() }
                                                                if (accumulatedDragX <= -thresholdPx) {
                                                                    change.consume()
                                                                    hasSwipedInCurrentGesture = true
                                                                    if (TapHapticState.enabled.value) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    }
                                                                    AudioUtil.playScroll(context)
                                                                    if (safeIndex < ribbonEntries.lastIndex) {
                                                                        val next = safeIndex + 1
                                                                        selectedRibbonIndex = next
                                                                        if (ribbonEntries.getOrNull(next) != RibbonEntry.Welcome) {
                                                                            homeExpanded = false
                                                                        }
                                                                    }
                                                                } else if (accumulatedDragX >= thresholdPx) {
                                                                    change.consume()
                                                                    hasSwipedInCurrentGesture = true
                                                                    if (TapHapticState.enabled.value) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    }
                                                                    AudioUtil.playScroll(context)
                                                                    if (safeIndex > 0) {
                                                                        val prev = safeIndex - 1
                                                                        selectedRibbonIndex = prev
                                                                        if (ribbonEntries.getOrNull(prev) != RibbonEntry.Welcome) {
                                                                            homeExpanded = false
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    AnimatedContent(
                                        targetState = activeStageType,
                                        transitionSpec = {
                                            val movingForward = isNavigatingForward
                                            val springSpatial = spring<IntOffset>(
                                                dampingRatio = 0.65f,
                                                stiffness = 320f
                                            )
                                            val springScale = spring<Float>(
                                                dampingRatio = 0.65f,
                                                stiffness = 320f
                                            )
                                            val effectsSpec = motion.defaultEffectsSpec<Float>()

                                            val slideInOffset = { size: Int ->
                                                if (movingForward) (size * 0.18f).roundToInt()
                                                else (-size * 0.18f).roundToInt()
                                            }
                                            val slideOutOffset = { size: Int ->
                                                if (movingForward) (-size * 0.18f).roundToInt()
                                                else (size * 0.18f).roundToInt()
                                            }

                                            (slideInHorizontally(
                                                animationSpec = springSpatial,
                                                initialOffsetX = slideInOffset
                                            ) + scaleIn(
                                                animationSpec = springScale,
                                                initialScale = 0.94f
                                            ) + fadeIn(
                                                animationSpec = effectsSpec
                                            )).togetherWith(
                                                slideOutHorizontally(
                                                    animationSpec = springSpatial,
                                                    targetOffsetX = slideOutOffset
                                                ) + scaleOut(
                                                    animationSpec = springScale,
                                                    targetScale = 1.03f
                                                ) + fadeOut(
                                                    animationSpec = effectsSpec
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        label = "ZXGameSpaceMainStage"
                                    ) { stageType ->
                                        when (stageType) {
                                            RibbonStageType.Welcome -> WelcomeHubStage(
                                                scroll = welcomeScroll,
                                                expanded = homeExpanded,
                                                onExpandedChange = { if (activeEntry == RibbonEntry.Welcome) homeExpanded = it },
                                                appScalePercent = appScalePercent,
                                                profile = profile,
                                                profileValue = profileValue,
                                                games = enabledApps,
                                                allAppsCount = allApps.size.coerceAtLeast(enabledApps.size),
                                                refreshHz = refreshHz,
                                                deviceName = deviceName,
                                                chipsetName = chipsetName,
                                                onApplyProfile = onApplyProfile,
                                                onOpenSettings = {
                                                    selectedSettingsTab = SettingsSidebarTab.General
                                                    category = TopCategory.Settings
                                                },
                                                onSelectGame = { game ->
                                                    val idx = ribbonEntries.indexOfFirst {
                                                        it is RibbonEntry.Game && it.app.packageName == game.packageName
                                                    }
                                                    homeExpanded = false
                                                    if (idx >= 0) selectedRibbonIndex = idx else requestAppLaunch(game)
                                                },
                                                onLaunchGame = requestAppLaunch,
                                                onOpenLibrary = {
                                                    homeExpanded = false
                                                    selectedRibbonIndex = ribbonEntries.indexOf(RibbonEntry.Library)
                                                },
                                                onOpenHomeWallpaper = { wallpaperPkg = "home" }
                                            )

                                            RibbonStageType.Game -> {
                                                val currentGame = (activeEntry as? RibbonEntry.Game)?.app ?: lastGameApp
                                                if (currentGame != null) {
                                                    GameHeroStage(
                                                        app = currentGame,
                                                        movingForward = isNavigatingForward,
                                                        onLaunch = { requestAppLaunch(currentGame) },
                                                        onOpenAppSettings = { tuningPkg = currentGame.packageName },
                                                        onOpenWallpaperSettings = { wallpaperPkg = currentGame.packageName }
                                                    )
                                                }
                                            }

                                            RibbonStageType.Library -> LibraryGridStage(
                                                games = games,
                                                allApps = allApps,
                                                expanded = libraryExpanded,
                                                onOpenSettings = { tuningPkg = it.packageName },
                                                onExpandedChange = { if (activeEntry == RibbonEntry.Library) libraryExpanded = it },
                                                onClose = { selectedRibbonIndex = 0 },
                                                onNavigateUp = { ribbonFocusRequester.requestFocus() },
                                                loading = loading
                                            )

                                            RibbonStageType.WrapToFirst -> WrapToFirstStage(
                                                onReturnToFirst = { selectedRibbonIndex = 0 }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        TopCategory.Settings -> {
                            ZXGameSpaceSettingsStage(
                                selectedTab = selectedSettingsTab,
                                onSelectTab = { selectedSettingsTab = it },
                                profile = profile,
                                profileValue = profileValue,
                                refreshHz = refreshHz,
                                deviceName = deviceName,
                                chipsetName = chipsetName,
                                onApplyProfile = onApplyProfile,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Search Overlay
                ZXGameSpaceSearchOverlay(
                    visible = searchOpen,
                    onClose = {
                        searchOpen = false
                        searchQuery = ""
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ZXGameSpaceSearchPane(
                        query = searchQuery,
                        apps = allApps,
                        onLaunch = requestAppLaunch,
                        onOpenSettings = { tuningPkg = it.packageName },
                        onNavigateToSettingsTab = { tab ->
                            selectedSettingsTab = tab
                            category = TopCategory.Settings
                            searchOpen = false
                            searchQuery = ""
                        },
                        onClose = {
                            searchOpen = false
                            searchQuery = ""
                        }
                    )
                }
            }

            BackHandler(enabled = !introActive) {
                handleBack()
            }
        }

        // Floating Quick Settings Panel (overlay on right side, does not distort background)
        ZXGameSpaceQuickSettingsOverlay(
            visible = quickSettingsOpen,
            onClose = { quickSettingsOpen = false }
        )

        // Floating Now Playing Media Panel (morphing popover anchored to dot)
        NowPlayingPanelOverlay(
            visible = nowPlayingOpen,
            anchor = nowPlayingAnchor,
            endSafePad = endSafePad,
            hazeState = overlayHazeState,
            onClose = { nowPlayingOpen = false },
            onNavigateToAudioSettings = {
                nowPlayingOpen = false
                selectedSettingsTab = SettingsSidebarTab.Audio
                category = TopCategory.Settings
            }
        )

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

        ZXGameSpaceAppSettingsOverlay(
            visible = tuningPkg != null,
            packageName = tuningPkg,
            allApps = allApps,
            hazeState = overlayHazeState,
            onLaunch = requestAppLaunch,
            onClose = { tuningPkg = null }
        )

        ZXGameSpaceWallpaperOverlay(
            visible = wallpaperPkg != null,
            packageName = wallpaperPkg,
            allApps = allApps,
            hazeState = overlayHazeState,
            onLaunch = requestAppLaunch,
            onClose = { wallpaperPkg = null }
        )

        ZXGameSpaceSidePanelOnboardingOverlay(
            visible = sidePanelPromptOpen,
            pendingApp = pendingLaunchApp,
            hazeState = overlayHazeState,
            onDismiss = {
                sidePanelPromptOpen = false
                pendingLaunchApp = null
            },
            onConfirm = {
                val appToLaunch = pendingLaunchApp
                sidePanelPromptOpen = false
                pendingLaunchApp = null
                if (appToLaunch != null) {
                    scope.launch {
                        delay(240)
                        ZXSidePanelService.show(context, appToLaunch.packageName, appToLaunch.label)
                        onLaunch(appToLaunch)
                    }
                }
            }
        )
    }
    }
}
