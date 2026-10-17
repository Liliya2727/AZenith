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

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.Token
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.hazeSource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.topjohnwu.superuser.Shell
import java.net.Inet4Address
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.BuildConfig
import zx.azenith.R
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import com.materialkolor.rememberDynamicColorScheme
import zx.azenith.ui.component.AppIconCache
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.ExpressiveDropdownItem
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveSliderItem
import zx.azenith.ui.component.GamepadFocusRoot
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.subscreens.getAppDetails
import zx.azenith.ui.subscreens.isGameCategory
import zx.azenith.ui.util.AppConfig
import zx.azenith.ui.util.DebugUtils
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.dumpDiagnosticLogs
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getSupportedDownscaleFactors
import zx.azenith.ui.util.getSupportedFpsTargets
import zx.azenith.ui.util.getSupportedRefreshRates
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import zx.azenith.ui.theme.ColorEngine
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.viewmodel.AppSettingsViewModel
import zx.azenith.ui.viewmodel.ApplistViewmodel
import zx.azenith.ui.viewmodel.SettingsViewModel
import zx.azenith.ui.viewmodel.TweakViewModel

private val LocalGameSpaceAnimationsEnabled = compositionLocalOf { true }

private enum class TopCategory(val labelRes: Int) {
    Games(R.string.gamespace_tab_games),
    Settings(R.string.settings)
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

private enum class SettingsSidebarTab(
    val labelRes: Int,
    val icon: ImageVector
) {
    General(R.string.gamespace_settings_general, Icons.Rounded.Tune),
    Tweaks(R.string.nav_tweaks, Icons.Rounded.Bolt),
    Theme(R.string.theme, Icons.Rounded.Palette),
    About(R.string.section_about, Icons.Rounded.Info)
}

private data class SettingsSearchItem(
    val titleRes: Int,
    val summaryRes: Int? = null,
    val summaryText: String? = null,
    val tab: SettingsSidebarTab,
    val icon: ImageVector
)

private val SETTINGS_SEARCH_ITEMS = listOf(
    // General
    SettingsSearchItem(
        titleRes = R.string.show_notifications,
        summaryText = "Post status notifications when active profile switches",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.NotificationsActive
    ),
    SettingsSearchItem(
        titleRes = R.string.tap_haptic_feedback,
        summaryText = "Tactile impulse feedback on interactive taps and toggles",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Vibration
    ),
    SettingsSearchItem(
        titleRes = R.string.show_toast,
        summaryText = "Display brief toast notifications on profile and state switches",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Info
    ),
    SettingsSearchItem(
        titleRes = R.string.allow_verbose_log,
        summaryText = "Write extended debug diagnostics to /data/adb/.config/AZenith",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.BugReport
    ),
    SettingsSearchItem(
        titleRes = R.string.disable_auto_mode,
        summaryRes = R.string.disable_auto_mode_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Memory
    ),
    SettingsSearchItem(
        titleRes = R.string.disable_tweak,
        summaryRes = R.string.disable_tweak_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Security
    ),
    SettingsSearchItem(
        titleRes = R.string.profile_timeout,
        summaryRes = R.string.profile_timeout_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.HourglassEmpty
    ),
    SettingsSearchItem(
        titleRes = R.string.restart_service,
        summaryRes = R.string.restart_service_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.RestartAlt
    ),
    SettingsSearchItem(
        titleRes = R.string.save_log,
        summaryRes = R.string.save_log_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Save
    ),

    // Tweaks
    SettingsSearchItem(
        titleRes = R.string.perf_lite_mode,
        summaryRes = R.string.perf_lite_mode_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.Speed
    ),
    SettingsSearchItem(
        titleRes = R.string.game_preload,
        summaryRes = R.string.game_preload_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.RocketLaunch
    ),
    SettingsSearchItem(
        titleRes = R.string.memory_killer,
        summaryRes = R.string.memory_killer_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.CleaningServices
    ),
    SettingsSearchItem(
        titleRes = R.string.app_priority_control,
        summaryRes = R.string.app_priority_control_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.SwapVerticalCircle
    ),
    SettingsSearchItem(
        titleRes = R.string.dnd_mode_gaming,
        summaryRes = R.string.dnd_mode_gaming_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.DoNotDisturbOn
    ),
    SettingsSearchItem(
        titleRes = R.string.trim_filesystem,
        summaryRes = R.string.trim_filesystem_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.ContentCut
    ),
    SettingsSearchItem(
        titleRes = R.string.thermalcore_service,
        summaryRes = R.string.thermalcore_service_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.ThermostatAuto
    ),
    SettingsSearchItem(
        titleRes = R.string.refreshrates,
        summaryText = "Display refresh rate locked during gaming",
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.WebStories
    ),
    SettingsSearchItem(
        titleRes = R.string.renderengine,
        summaryText = "HWUI graphics compositing backend",
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.SettingsSuggest
    ),

    // Theme
    SettingsSearchItem(
        titleRes = R.string.theme,
        summaryText = "Auto, Light, Dark, and AMOLED pure black modes",
        tab = SettingsSidebarTab.Theme,
        icon = Icons.Rounded.BrightnessAuto
    ),
    SettingsSearchItem(
        titleRes = R.string.pers_text_scale,
        summaryText = "App scale percent from 70% downscale to 150% upscale",
        tab = SettingsSidebarTab.Theme,
        icon = Icons.Rounded.ZoomIn
    ),
    SettingsSearchItem(
        titleRes = R.string.str_use_scroll_animation,
        summaryText = "Toggle UI animations or instant pop-ups",
        tab = SettingsSidebarTab.Theme,
        icon = Icons.Rounded.PlayArrow
    ),
    SettingsSearchItem(
        titleRes = R.string.theme,
        summaryText = "Follow Monet dynamic colors or select a preset palette",
        tab = SettingsSidebarTab.Theme,
        icon = Icons.Rounded.Palette
    ),

    // About
    SettingsSearchItem(
        titleRes = R.string.section_about,
        summaryRes = R.string.str_creator_maintainer,
        tab = SettingsSidebarTab.About,
        icon = Icons.Rounded.Info
    ),
    SettingsSearchItem(
        titleRes = R.string.str_licenses,
        summaryRes = R.string.str_open_source,
        tab = SettingsSidebarTab.About,
        icon = Icons.Rounded.WorkspacePremium
    )
)

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

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 100).coerceIn(70, 150))
    }
    var animationsEnabled by remember {
        mutableStateOf(prefs.getBoolean("gamespace_animations", true))
    }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "gamespace_app_scale" -> {
                    appScalePercent = prefs.getInt("gamespace_app_scale", 100).coerceIn(70, 150)
                }
                "gamespace_animations" -> {
                    animationsEnabled = prefs.getBoolean("gamespace_animations", true)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val scope = rememberCoroutineScope()
    val ribbonFocusRequester = remember { FocusRequester() }
    var category by rememberSaveable { mutableStateOf(TopCategory.Games) }
    var selectedSettingsTab by rememberSaveable { mutableStateOf(SettingsSidebarTab.General) }
    var selectedRibbonIndex by remember { mutableIntStateOf(0) }
    var exitPrompt by remember { mutableStateOf(false) }
    var tuningPkg by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var quickSettingsOpen by remember { mutableStateOf(false) }
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
    val hazeState = remember { HazeState() }
    val baseDensity = LocalDensity.current
    val scaleFactor = appScalePercent.coerceIn(70, 150) / 100f

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * scaleFactor,
            fontScale = baseDensity.fontScale
        ),
        LocalGameSpaceAnimationsEnabled provides animationsEnabled
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
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (homeExpanded) {
                                homeExpanded = false
                                scope.launch {
                                    welcomeScroll.animateScrollTo(0)
                                    delay(50)
                                    runCatching { ribbonFocusRequester.requestFocus() }
                                }
                                true
                            } else if (libraryExpanded) {
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
                .hazeSource(hazeState)
        ) {
            ZXGameSpaceStageBackdrop(
                entry = if (category == TopCategory.Games) activeEntry else RibbonEntry.Welcome
            )
        }

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
                    category = if (category == TopCategory.Settings) TopCategory.Games else TopCategory.Settings
                },
                onAvatarClick = {
                    val nextProfile = when (profileValue) {
                        "1" -> "2"
                        "2" -> "3"
                        else -> "1"
                    }
                    onApplyProfile(nextProfile)
                },
                onStatusClick = { quickSettingsOpen = !quickSettingsOpen },
                onExit = { exitPrompt = true }
            )

            Spacer(Modifier.height(6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Games Stage
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                ) {
                    AnimatedVisibility(
                        visible = !searchOpen && !homeExpanded && !libraryExpanded,
                        enter = if (animationsEnabled) fadeIn(effectsSpec) + expandVertically(spatialSpec) else EnterTransition.None,
                        exit = if (animationsEnabled) fadeOut(effectsSpec) + shrinkVertically(spatialSpec) else ExitTransition.None
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
                                },
                                focusRequester = ribbonFocusRequester
                            )

                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState = activeEntry,
                            transitionSpec = {
                                if (!animationsEnabled) {
                                    EnterTransition.None.togetherWith(ExitTransition.None)
                                } else {
                                    val fromIndex = ribbonEntries.indexOf(initialState)
                                    val toIndex = ribbonEntries.indexOf(targetState)
                                    val movingForward = toIndex >= fromIndex

                                    val slideInOffset = { size: Int ->
                                        if (movingForward) (size * 0.18f).roundToInt()
                                        else (-size * 0.18f).roundToInt()
                                    }
                                    val slideOutOffset = { size: Int ->
                                        if (movingForward) (-size * 0.18f).roundToInt()
                                        else (size * 0.18f).roundToInt()
                                    }

                                    (slideInHorizontally(
                                        animationSpec = motion.defaultSpatialSpec(),
                                        initialOffsetX = slideInOffset
                                    ) + scaleIn(
                                        animationSpec = motion.defaultSpatialSpec(),
                                        initialScale = 0.94f
                                    ) + fadeIn(
                                        animationSpec = motion.defaultEffectsSpec()
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = motion.defaultSpatialSpec(),
                                            targetOffsetX = slideOutOffset
                                        ) + scaleOut(
                                            animationSpec = motion.defaultSpatialSpec(),
                                            targetScale = 1.03f
                                        ) + fadeOut(
                                            animationSpec = motion.defaultEffectsSpec()
                                        )
                                    )
                                }
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
                                    onOpenSettings = {
                                        selectedSettingsTab = SettingsSidebarTab.General
                                        category = TopCategory.Settings
                                    },
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
                                    onClose = { selectedRibbonIndex = 0 },
                                    onNavigateUp = { ribbonFocusRequester.requestFocus() }
                                )
                            }
                        }
                    }
                }

                // Search Overlay
                ZXGameSpaceSearchOverlay(
                    visible = searchOpen,
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ZXGameSpaceSearchPane(
                        query = searchQuery,
                        apps = allApps,
                        onLaunch = onLaunch,
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

                // Settings Stage - Expressive Bouncy slide in from right overlay
                ZXGameSpaceSettingsOverlay(
                    visible = category == TopCategory.Settings,
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxSize()
                ) {
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

            BackHandler(
                enabled = tuningPkg != null || quickSettingsOpen || category == TopCategory.Settings ||
                    searchOpen || homeExpanded || libraryExpanded
            ) {
                if (tuningPkg != null) {
                    tuningPkg = null
                } else if (quickSettingsOpen) {
                    quickSettingsOpen = false
                } else if (category == TopCategory.Settings) {
                    category = TopCategory.Games
                } else if (searchOpen) {
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
        }

        // Floating Quick Settings Panel (overlay on right side, does not distort background)
        ZXGameSpaceQuickSettingsOverlay(
            visible = quickSettingsOpen,
            onClose = { quickSettingsOpen = false }
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
            hazeState = hazeState,
            onLaunch = onLaunch,
            onClose = { tuningPkg = null }
        )
    }
    }
}

/**
 * Quick settings sheet anchored to the right edge on solid Material 3 surfaces.
 *
 * Network, Bluetooth audio and input devices come from framework callbacks rather than a single
 * read, so the panel stays accurate for as long as it is open.
 */
@Composable
private fun ZXGameSpaceQuickSettingsOverlay(
    visible: Boolean,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val connectivityManager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val inputManager = remember(context) { context.getSystemService(Context.INPUT_SERVICE) as? InputManager }
    val bluetoothManager = remember(context) { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }

    // Volume control state
    val maxVol = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var volFraction by remember(audioManager) {
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf((curVol.toFloat() / maxVol.coerceAtLeast(1)).coerceIn(0f, 1f))
    }

    // Display brightness control state
    val activity = context as? Activity
    var brightnessFraction by remember(activity) {
        val windowAttr = activity?.window?.attributes?.screenBrightness ?: -1f
        val currentBrightness = if (windowAttr >= 0f) {
            windowAttr
        } else {
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            }.getOrDefault(0.5f)
        }
        mutableFloatStateOf(currentBrightness.coerceIn(0.02f, 1f))
    }

    // Live framework observers
    val network = rememberQuickNetworkState(connectivityManager, visible)
    val bluetoothEndpoints = rememberQuickBluetoothEndpoints(audioManager, visible)
    val inputDevices = rememberQuickInputDevices(inputManager, visible)
    val isBtOn = remember(bluetoothEndpoints, visible) {
        bluetoothEndpoints.isNotEmpty() || runCatching { bluetoothManager?.adapter?.isEnabled == true }.getOrDefault(false)
    }

    // Scrim overlay - dismiss on click outside
    AnimatedVisibility(
        visible = visible,
        enter = if (animationsEnabled) fadeIn(animationSpec = motion.defaultEffectsSpec()) else EnterTransition.None,
        exit = if (animationsEnabled) fadeOut(animationSpec = motion.defaultEffectsSpec()) else ExitTransition.None
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Floating panel sliding in from right side with spring bouncy animation
    AnimatedVisibility(
        visible = visible,
        enter = if (animationsEnabled) {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec())
        } else {
            EnterTransition.None
        },
        exit = if (animationsEnabled) {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        } else {
            ExitTransition.None
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 24.dp, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerLow,
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through from dismissing panel
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Header (No separator line, pure spacing)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.quick_settings_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bento Widgets Scrollable Container
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bento Row 1: Network & Bluetooth (Side-by-side equal width)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Network Bento Card
                            val netCategory = when {
                                !network.online -> stringResource(R.string.quick_settings_network)
                                network.transport == NetworkCapabilities.TRANSPORT_WIFI -> stringResource(R.string.quick_settings_net_wifi)
                                network.transport == NetworkCapabilities.TRANSPORT_CELLULAR -> stringResource(R.string.quick_settings_net_cellular)
                                network.transport == NetworkCapabilities.TRANSPORT_ETHERNET -> stringResource(R.string.quick_settings_net_ethernet)
                                else -> stringResource(R.string.quick_settings_network)
                            }
                            val netHeadline = when {
                                !network.online -> stringResource(R.string.quick_settings_offline)
                                network.transport == NetworkCapabilities.TRANSPORT_WIFI -> network.wifiSsid ?: stringResource(R.string.quick_settings_value_verified)
                                network.transport == NetworkCapabilities.TRANSPORT_CELLULAR -> network.carrierName ?: stringResource(R.string.quick_settings_value_verified)
                                else -> stringResource(R.string.quick_settings_value_verified)
                            }
                            BentoStatCard(
                                icon = when {
                                    !network.online -> Icons.Rounded.WifiOff
                                    network.transport == NetworkCapabilities.TRANSPORT_WIFI -> Icons.Rounded.Wifi
                                    network.transport == NetworkCapabilities.TRANSPORT_CELLULAR -> Icons.Rounded.SignalCellularAlt
                                    network.transport == NetworkCapabilities.TRANSPORT_ETHERNET -> Icons.Rounded.SettingsEthernet
                                    else -> Icons.Rounded.Public
                                },
                                iconContainerColor = if (network.online) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                iconTint = if (network.online) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                category = netCategory,
                                headline = netHeadline,
                                modifier = Modifier.weight(1f)
                            )

                            // Bluetooth Bento Card
                            val btDevice = bluetoothEndpoints.firstOrNull()?.name
                            val btHeadline = when {
                                bluetoothEndpoints.isNotEmpty() -> btDevice ?: stringResource(R.string.quick_settings_value_active)
                                isBtOn -> stringResource(R.string.quick_settings_value_active)
                                else -> stringResource(R.string.quick_settings_value_none)
                            }
                            BentoStatCard(
                                icon = when {
                                    bluetoothEndpoints.isNotEmpty() -> Icons.Rounded.BluetoothConnected
                                    isBtOn -> Icons.Rounded.Bluetooth
                                    else -> Icons.Rounded.BluetoothDisabled
                                },
                                iconContainerColor = if (bluetoothEndpoints.isNotEmpty()) scheme.tertiaryContainer else if (isBtOn) scheme.surfaceContainerHigh else scheme.surfaceContainer,
                                iconTint = if (bluetoothEndpoints.isNotEmpty()) scheme.onTertiaryContainer else scheme.onSurfaceVariant,
                                category = stringResource(R.string.quick_settings_bluetooth),
                                headline = btHeadline,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bento Row 2: Input Mode Hero Bento Card (Touch vs Gamepad dynamic detection)
                        val connectedGamepad = inputDevices.firstOrNull {
                            it.kind == QuickInputKind.GAMEPAD || it.kind == QuickInputKind.JOYSTICK || it.kind == QuickInputKind.REMOTE
                        }
                        val isGamepadActive = GamepadInput.dpadMode || (connectedGamepad != null && GamepadInput.active)
                        BentoInputHeroCard(
                            isGamepad = isGamepadActive,
                            controllerName = connectedGamepad?.name?.takeIf { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bento Row 3: Media Volume Slider Bento Card
                        BentoSliderCard(
                            icon = when {
                                volFraction <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                                volFraction < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            title = stringResource(R.string.quick_settings_volume),
                            valuePercent = (volFraction * 100).roundToInt(),
                            accent = scheme.primary,
                            value = volFraction,
                            onValueChange = { newFraction ->
                                volFraction = newFraction
                                audioManager?.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (newFraction * maxVol).roundToInt(),
                                    0
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bento Row 4: Display Brightness Slider Bento Card
                        BentoSliderCard(
                            icon = when {
                                brightnessFraction < 0.33f -> Icons.Rounded.BrightnessLow
                                brightnessFraction < 0.66f -> Icons.Rounded.BrightnessMedium
                                else -> Icons.Rounded.BrightnessHigh
                            },
                            title = stringResource(R.string.quick_settings_brightness),
                            valuePercent = (brightnessFraction * 100).roundToInt(),
                            accent = scheme.tertiary,
                            value = brightnessFraction,
                            onValueChange = { newFraction ->
                                brightnessFraction = newFraction
                                activity?.window?.let { window ->
                                    val attributes = window.attributes
                                    attributes.screenBrightness = newFraction.coerceIn(0.01f, 1f)
                                    window.attributes = attributes
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure Material 3 Bento Stat Card.
 * Compact, borderless filled surface with prominent icon container, category label, and headline status.
 */
@Composable
private fun BentoStatCard(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    category: String,
    headline: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pure Material 3 Input Mode Hero Bento Card.
 * Expressively morphs between Touch Screen and Gamepad modes upon user input or controller detection.
 */
@Composable
private fun BentoInputHeroCard(
    isGamepad: Boolean,
    controllerName: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isGamepad) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isGamepad,
                        transitionSpec = {
                            (fadeIn(animationSpec = motion.defaultEffectsSpec()) +
                                scaleIn(animationSpec = motion.defaultSpatialSpec(), initialScale = 0.8f))
                                .togetherWith(
                                    fadeOut(animationSpec = motion.defaultEffectsSpec()) +
                                        scaleOut(animationSpec = motion.defaultSpatialSpec(), targetScale = 0.8f)
                                )
                        },
                        label = "InputHeroIcon"
                    ) { gamepadActive ->
                        Icon(
                            imageVector = if (gamepadActive) Icons.Rounded.SportsEsports else Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = if (gamepadActive) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_settings_input_devices),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                AnimatedContent(
                    targetState = isGamepad to controllerName,
                    transitionSpec = {
                        fadeIn(animationSpec = motion.defaultEffectsSpec())
                            .togetherWith(fadeOut(animationSpec = motion.defaultEffectsSpec()))
                    },
                    label = "InputHeroTitle"
                ) { (gamepadActive, name) ->
                    Text(
                        text = if (gamepadActive) (name ?: stringResource(R.string.quick_settings_gamepad)) else stringResource(R.string.quick_settings_input_touchscreen),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGamepad) scheme.secondaryContainer else scheme.surfaceContainerHigh
            ) {
                AnimatedContent(
                    targetState = isGamepad,
                    transitionSpec = {
                        fadeIn(animationSpec = motion.defaultEffectsSpec())
                            .togetherWith(fadeOut(animationSpec = motion.defaultEffectsSpec()))
                    },
                    label = "InputHeroBadge"
                ) { gamepadActive ->
                    Text(
                        text = if (gamepadActive) "GAMEPAD" else "TOUCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (gamepadActive) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pure Material 3 Slider Bento Card.
 * Borderless filled container with real-time value percentage and haptic ZenithSlider.
 */
@Composable
private fun BentoSliderCard(
    icon: ImageVector,
    title: String,
    valuePercent: Int,
    accent: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }
                Text(
                    text = "$valuePercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
            }

            Spacer(Modifier.height(4.dp))

            ZenithSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = {},
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Default-network snapshot; [TRANSPORT_NONE] and nulls mean "not reported". */
private data class QuickNetworkState(
    val online: Boolean = false,
    val transport: Int = TRANSPORT_NONE,
    val validated: Boolean = false,
    val wifiSsid: String? = null,
    val carrierName: String? = null,
)

/** `NetworkCapabilities.TRANSPORT_UNKNOWN` is hidden, so the absent case is spelled locally. */
private const val TRANSPORT_NONE = -1

/**
 * Tracks the default network through [ConnectivityManager.registerDefaultNetworkCallback],
 * resolving live connection state, Wi-Fi SSID, and carrier name.
 */
@Composable
private fun rememberQuickNetworkState(
    connectivityManager: ConnectivityManager?,
    enabled: Boolean
): QuickNetworkState {
    val context = LocalContext.current
    var state by remember(connectivityManager) { mutableStateOf(QuickNetworkState()) }
    DisposableEffect(connectivityManager, enabled) {
        val manager = connectivityManager
        if (manager == null || !enabled) {
            onDispose { }
        } else {
            var capabilities: NetworkCapabilities? = null
            var link: LinkProperties? = null
            fun publish() {
                state = runCatching { quickNetworkState(context, capabilities, link) }
                    .getOrDefault(QuickNetworkState())
            }
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    capabilities = networkCapabilities
                    publish()
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    link = linkProperties
                    publish()
                }

                override fun onLost(network: Network) {
                    capabilities = null
                    link = null
                    publish()
                }
            }
            runCatching { manager.registerDefaultNetworkCallback(callback) }
            runCatching {
                capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
                link = manager.getLinkProperties(manager.activeNetwork)
            }
            publish()
            onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
        }
    }
    return state
}

private fun quickNetworkState(
    context: Context,
    capabilities: NetworkCapabilities?,
    link: LinkProperties?
): QuickNetworkState {
    if (capabilities == null) return QuickNetworkState()
    val transport = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkCapabilities.TRANSPORT_ETHERNET
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkCapabilities.TRANSPORT_BLUETOOTH
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkCapabilities.TRANSPORT_VPN
        else -> TRANSPORT_NONE
    }
    val wifiSsid = if (transport == NetworkCapabilities.TRANSPORT_WIFI) {
        runCatching {
            var ssid: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                ssid = wifiInfo?.ssid
            }
            if (ssid == null || ssid == "<unknown ssid>") {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ssid = wm?.connectionInfo?.ssid
            }
            ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }?.removeSurrounding("\"")
        }.getOrNull()
    } else null

    val carrierName = if (transport == NetworkCapabilities.TRANSPORT_CELLULAR) {
        runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.networkOperatorName?.takeIf { it.isNotBlank() } ?: tm?.simOperatorName?.takeIf { it.isNotBlank() }
        }.getOrNull()
    } else null

    return QuickNetworkState(
        online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        transport = transport,
        validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        wifiSsid = wifiSsid,
        carrierName = carrierName
    )
}

/** Bluetooth audio endpoints, the only Bluetooth state readable without BLUETOOTH_CONNECT. */
private enum class QuickBluetoothKind {
    A2DP, SCO, LE_HEADSET, LE_SPEAKER, LE_HEARING_AID, LE_BROADCAST, OTHER
}

private data class QuickBluetoothEndpoint(val name: String, val kind: QuickBluetoothKind)

private fun QuickBluetoothKind.labelRes(): Int = when (this) {
    QuickBluetoothKind.A2DP -> R.string.quick_settings_bt_a2dp
    QuickBluetoothKind.SCO -> R.string.quick_settings_bt_sco
    QuickBluetoothKind.LE_HEADSET -> R.string.quick_settings_bt_le_headset
    QuickBluetoothKind.LE_SPEAKER -> R.string.quick_settings_bt_le_speaker
    QuickBluetoothKind.LE_HEARING_AID -> R.string.quick_settings_bt_le_hearing_aid
    QuickBluetoothKind.LE_BROADCAST -> R.string.quick_settings_bt_le_broadcast
    QuickBluetoothKind.OTHER -> R.string.quick_settings_bt_unknown
}

private fun quickBluetoothKind(type: Int): QuickBluetoothKind = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> QuickBluetoothKind.A2DP
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> QuickBluetoothKind.SCO
    AudioDeviceInfo.TYPE_BLE_HEADSET -> QuickBluetoothKind.LE_HEADSET
    AudioDeviceInfo.TYPE_BLE_SPEAKER -> QuickBluetoothKind.LE_SPEAKER
    AudioDeviceInfo.TYPE_BLE_HEARING_AID -> QuickBluetoothKind.LE_HEARING_AID
    AudioDeviceInfo.TYPE_BLE_BROADCAST -> QuickBluetoothKind.LE_BROADCAST
    else -> QuickBluetoothKind.OTHER
}

private val QUICK_BLUETOOTH_KINDS = setOf(
    QuickBluetoothKind.A2DP,
    QuickBluetoothKind.SCO,
    QuickBluetoothKind.LE_HEADSET,
    QuickBluetoothKind.LE_SPEAKER,
    QuickBluetoothKind.LE_HEARING_AID,
    QuickBluetoothKind.LE_BROADCAST,
)

/**
 * Tracks routed Bluetooth audio through [AudioManager.registerAudioDeviceCallback]. Routing is
 * what actually changes while a headset connects or disconnects, so this is the live source.
 */
@Composable
private fun rememberQuickBluetoothEndpoints(
    audioManager: AudioManager?,
    enabled: Boolean
): List<QuickBluetoothEndpoint> {
    var endpoints by remember(audioManager) { mutableStateOf(emptyList<QuickBluetoothEndpoint>()) }
    DisposableEffect(audioManager, enabled) {
        val manager = audioManager
        if (manager == null || !enabled) {
            onDispose { }
        } else {
            fun refresh() {
                endpoints = runCatching {
                    manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        .map { QuickBluetoothEndpoint(it.productName.toString(), quickBluetoothKind(it.type)) }
                        .filter { it.kind in QUICK_BLUETOOTH_KINDS }
                        .distinctBy { it.name to it.kind }
                }.getOrDefault(emptyList())
            }
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
            }
            runCatching { manager.registerAudioDeviceCallback(callback, null) }
            refresh()
            onDispose { runCatching { manager.unregisterAudioDeviceCallback(callback) } }
        }
    }
    return endpoints
}

private enum class QuickInputKind {
    GAMEPAD, JOYSTICK, TOUCHSCREEN, TOUCHPAD, MOUSE, STYLUS, ROTARY, REMOTE, KEYBOARD, TRACKBALL, OTHER
}

private data class QuickInputDevice(
    val id: Int,
    val name: String,
    val kind: QuickInputKind,
)

private fun QuickInputKind.labelRes(): Int = when (this) {
    QuickInputKind.GAMEPAD -> R.string.quick_settings_gamepad
    QuickInputKind.JOYSTICK -> R.string.quick_settings_input_joystick
    QuickInputKind.TOUCHSCREEN -> R.string.quick_settings_input_touchscreen
    QuickInputKind.TOUCHPAD -> R.string.quick_settings_input_touchpad
    QuickInputKind.MOUSE -> R.string.quick_settings_input_mouse
    QuickInputKind.STYLUS -> R.string.quick_settings_input_stylus
    QuickInputKind.ROTARY -> R.string.quick_settings_input_rotary
    QuickInputKind.REMOTE -> R.string.quick_settings_tv_remote
    QuickInputKind.KEYBOARD -> R.string.quick_settings_input_keyboard
    QuickInputKind.TRACKBALL -> R.string.quick_settings_input_trackball
    QuickInputKind.OTHER -> R.string.quick_settings_input_other
}

/** Named from the sources a device reports; gamepad is tested first because it also claims joystick. */
private fun quickInputKind(sources: Int): QuickInputKind = when {
    sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD -> QuickInputKind.GAMEPAD
    sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK -> QuickInputKind.JOYSTICK
    sources and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN -> QuickInputKind.TOUCHSCREEN
    sources and InputDevice.SOURCE_TOUCHPAD == InputDevice.SOURCE_TOUCHPAD -> QuickInputKind.TOUCHPAD
    sources and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS -> QuickInputKind.STYLUS
    sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE -> QuickInputKind.MOUSE
    sources and InputDevice.SOURCE_ROTARY_ENCODER == InputDevice.SOURCE_ROTARY_ENCODER -> QuickInputKind.ROTARY
    sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD -> QuickInputKind.REMOTE
    sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD -> QuickInputKind.KEYBOARD
    sources and InputDevice.SOURCE_TRACKBALL == InputDevice.SOURCE_TRACKBALL -> QuickInputKind.TRACKBALL
    else -> QuickInputKind.OTHER
}

/**
 * Tracks every attached input device through [InputManager.registerInputDeviceListener], so a
 * controller or remote plugged in mid-session shows up without reopening the sheet.
 */
@Composable
private fun rememberQuickInputDevices(
    inputManager: InputManager?,
    enabled: Boolean
): List<QuickInputDevice> {
    var devices by remember(inputManager) { mutableStateOf(emptyList<QuickInputDevice>()) }
    DisposableEffect(inputManager, enabled) {
        val manager = inputManager
        if (manager == null || !enabled) {
            onDispose { }
        } else {
            fun refresh() {
                devices = runCatching {
                    // int[] has no mapNotNull overload, only on Iterable.
                    manager.inputDeviceIds.toList()
                        .mapNotNull { id -> InputDevice.getDevice(id)?.takeIf { !it.isVirtual } }
                        .map { device ->
                            QuickInputDevice(
                                id = device.id,
                                name = device.name.orEmpty(),
                                kind = quickInputKind(device.sources),
                            )
                        }
                        .sortedWith(compareBy({ it.kind.ordinal }, { it.name }))
                }.getOrDefault(emptyList())
            }
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) = refresh()
                override fun onInputDeviceChanged(deviceId: Int) = refresh()
                override fun onInputDeviceRemoved(deviceId: Int) = refresh()
            }
            runCatching { manager.registerInputDeviceListener(listener, null) }
            refresh()
            onDispose { runCatching { manager.unregisterInputDeviceListener(listener) } }
        }
    }
    return devices
}

@Composable
private fun ZXGameSpaceSettingsOverlay(
    visible: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val scheme = MaterialTheme.colorScheme
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current
    AnimatedVisibility(
        visible = visible,
        enter = if (!animationsEnabled) {
            EnterTransition.None
        } else {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec())
        },
        exit = if (!animationsEnabled) {
            ExitTransition.None
        } else {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = HazeBlurStyle.Material3(
                        containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.55f)
                    ) {
                        blurRadius(28.dp)
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
private fun ZXGameSpaceSearchOverlay(
    visible: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val scheme = MaterialTheme.colorScheme
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (!animationsEnabled) {
            EnterTransition.None
        } else {
            slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight / 6 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec())
        },
        exit = if (!animationsEnabled) {
            ExitTransition.None
        } else {
            slideOutVertically(
                targetOffsetY = { fullHeight -> -fullHeight / 8 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = HazeBlurStyle.Material3(
                        containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.62f)
                    ) {
                        blurRadius(28.dp)
                    }
                )
        ) {
            content()
        }
    }
}

/** Sheet offset (px) at which a drag stops being scroll and becomes the full-screen step. */
private const val HOME_EXPAND_AT = 90

private const val SEARCH_RESULT_CAP = 6

private enum class SearchScopeFilter(val label: String) {
    All("All"),
    Games("Games"),
    Tuned("Tuned"),
    Settings("Settings")
}

private data class ResolvedSettingItem(
    val title: String,
    val summary: String,
    val tab: SettingsSidebarTab,
    val icon: ImageVector
)

@Composable
private fun ZXGameSpaceSearchPane(
    query: String,
    apps: List<ApplistViewmodel.AppInfo>,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onOpenSettings: (ApplistViewmodel.AppInfo) -> Unit,
    onNavigateToSettingsTab: (SettingsSidebarTab) -> Unit,
    onClose: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var showAllApps by remember(query) { mutableStateOf(false) }
    var scopeFilter by remember { mutableStateOf(SearchScopeFilter.All) }

    val resolvedSettings = remember(context) {
        SETTINGS_SEARCH_ITEMS.map { item ->
            val title = context.getString(item.titleRes)
            val summary = item.summaryRes?.let { context.getString(it) } ?: item.summaryText ?: ""
            ResolvedSettingItem(
                title = title,
                summary = summary,
                tab = item.tab,
                icon = item.icon
            )
        }
    }

    val matchedApps = remember(query, apps, scopeFilter) {
        val base = when (scopeFilter) {
            SearchScopeFilter.Games -> apps.filter { it.isRecommended || it.isEnabledInConfig }
            SearchScopeFilter.Tuned -> apps.filter { it.isEnabledInConfig }
            SearchScopeFilter.Settings -> emptyList()
            SearchScopeFilter.All -> apps
        }
        if (query.isBlank()) {
            base.sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> { it.isEnabledInConfig }
                    .thenByDescending { it.isRecommended }
                    .thenBy { it.label.lowercase() }
            )
        } else {
            base.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }.sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> {
                    it.label.equals(query, ignoreCase = true)
                }.thenByDescending { it.label.startsWith(query, ignoreCase = true) }
                    .thenByDescending { it.isEnabledInConfig }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    val matchedSettings = remember(query, resolvedSettings, scopeFilter) {
        if (scopeFilter == SearchScopeFilter.Games || scopeFilter == SearchScopeFilter.Tuned) {
            emptyList()
        } else if (query.isBlank()) {
            if (scopeFilter == SearchScopeFilter.Settings) resolvedSettings else resolvedSettings.take(4)
        } else {
            resolvedSettings.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true) ||
                    it.tab.name.contains(query, ignoreCase = true)
            }.sortedWith(
                compareByDescending<ResolvedSettingItem> {
                    it.title.equals(query, ignoreCase = true)
                }.thenByDescending { it.title.startsWith(query, ignoreCase = true) }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    val shownApps = if (showAllApps || scopeFilter != SearchScopeFilter.All) {
        matchedApps
    } else {
        matchedApps.take(SEARCH_RESULT_CAP)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow.copy(alpha = 0.88f),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Filter Bar & Result Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchScopeFilter.entries.forEach { filter ->
                    ExpressiveFilterChip(
                        selected = scopeFilter == filter,
                        label = filter.label,
                        onClick = { scopeFilter = filter }
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = if (query.isBlank()) {
                        stringResource(R.string.gamespace_search_hint)
                    } else {
                        "${matchedApps.size + matchedSettings.size} results"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                TopBarIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.gamespace_search_close),
                    onClick = onClose
                )
            }

            if (matchedApps.isEmpty() && matchedSettings.isEmpty()) {
                EmptyPane(
                    icon = Icons.Rounded.Search,
                    titleRes = R.string.gamespace_search_empty,
                    bodyRes = R.string.gamespace_search_hint,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (shownApps.isNotEmpty()) {
                        item(key = "header_apps") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stringResource(R.string.gamespace_tab_games)} & ${stringResource(R.string.gamespace_all_apps)} (${matchedApps.size})".uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = scheme.primary
                                )
                                if (matchedApps.size > SEARCH_RESULT_CAP && !showAllApps && scopeFilter == SearchScopeFilter.All) {
                                    Surface(
                                        shape = CircleShape,
                                        color = scheme.secondaryContainer,
                                        contentColor = scheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { showAllApps = true }
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.gamespace_show_all)} (${matchedApps.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val chunkedApps = shownApps.chunked(2)
                        itemsIndexed(chunkedApps, key = { idx, row -> "app_row_${idx}_${row.first().packageName}" }) { _, rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowApps.forEach { app ->
                                    val shape = RoundedCornerShape(18.dp)
                                    val versionName = app.packageInfo.versionName?.takeIf { it.isNotBlank() }
                                    Surface(
                                        shape = shape,
                                        color = scheme.surfaceContainerHigh.copy(alpha = 0.90f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(66.dp)
                                            .gamepadFocusOutline(shape = shape, register = true)
                                            .clip(shape)
                                            .clickable { onLaunch(app) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AppIconImage(app = app, size = 42.dp)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = app.label,
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = scheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (app.isEnabledInConfig) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(scheme.primary)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (versionName != null) {
                                                        "v$versionName · ${app.packageName}"
                                                    } else {
                                                        app.packageName
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = scheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
                                if (rowApps.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (matchedSettings.isNotEmpty()) {
                        item(key = "header_settings") {
                            Text(
                                text = "${stringResource(R.string.settings)} (${matchedSettings.size})".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = scheme.primary,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }

                        val chunkedSettings = matchedSettings.chunked(2)
                        itemsIndexed(chunkedSettings, key = { idx, row -> "setting_row_${idx}_${row.first().title}" }) { _, rowSettings ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowSettings.forEach { setting ->
                                    val shape = RoundedCornerShape(18.dp)
                                    Surface(
                                        shape = shape,
                                        color = scheme.surfaceContainerHigh.copy(alpha = 0.90f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(62.dp)
                                            .gamepadFocusOutline(shape = shape, register = true)
                                            .clip(shape)
                                            .clickable { onNavigateToSettingsTab(setting.tab) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = scheme.secondaryContainer,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = setting.icon,
                                                        contentDescription = null,
                                                        tint = scheme.onSecondaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = setting.title,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = scheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (setting.summary.isNotBlank()) {
                                                    Text(
                                                        text = setting.summary,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = scheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(setting.tab.labelRes),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = scheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowSettings.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Game Space App Tuning Sheet: slides up from below with Material 3 bouncy expressive physics over
 * a Haze-blurred backdrop, styled natively in the two-pane Game Space layout.
 */
@Composable
private fun ZXGameSpaceAppSettingsOverlay(
    visible: Boolean,
    packageName: String?,
    allApps: List<ApplistViewmodel.AppInfo>,
    hazeState: HazeState,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onClose: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current

    var activePkg by remember { mutableStateOf(packageName) }
    if (packageName != null) {
        activePkg = packageName
    }

    LaunchedEffect(activePkg) {
        if (activePkg != null) {
            viewModel.loadConfig()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (!animationsEnabled) {
            EnterTransition.None
        } else {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec())
        },
        exit = if (!animationsEnabled) {
            ExitTransition.None
        } else {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        },
        modifier = Modifier.fillMaxSize()
    ) {
        val pkg = activePkg ?: return@AnimatedVisibility
        val resolvedApp = remember(pkg, allApps) {
            allApps.find { it.packageName == pkg }
        }
        val appDetails = remember(pkg) { getAppDetails(context, pkg) }
        val isGameApp = remember(pkg) { isGameCategory(context, pkg) }
        val config = viewModel.fullConfig[pkg]
        var localMasterOn by remember(pkg, config != null) { mutableStateOf(config != null) }

        var isFullModeEnabled by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isFullModeEnabled = DebugUtils.isFullModeEnabled()
        }
        var bypassSupported by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            bypassSupported = withContext(Dispatchers.IO) {
                val path = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "")
                path != "UNSUPPORTED" && path != "NEED_SETUP"
            }
        }

        val booleanModes = listOf(
            stringResource(R.string.default_label),
            stringResource(R.string.on_label),
            stringResource(R.string.off_label)
        )
        val rendererModes = listOf(
            stringResource(R.string.Renderer_Default),
            stringResource(R.string.renderer_skia_vk),
            stringResource(R.string.renderer_skia_vk_threaded),
            stringResource(R.string.renderer_skiagl),
            stringResource(R.string.renderer_skiagl_threaded),
            stringResource(R.string.renderer_opengl_es),
            stringResource(R.string.renderer_opengl_es_threaded),
            stringResource(R.string.Renderer_Vulkan)
        )
        val rendererValues = listOf(
            "default",
            "skiavk",
            "skiavkthreaded",
            "skiagl",
            "skiaglthreaded",
            "opengl",
            "openglthreaded",
            "vulkan"
        )
        val defaultLabel = stringResource(R.string.default_label)
        val rawRefreshModes = remember { getSupportedRefreshRates(context) }
        val rawDownscaleSteps = remember { getSupportedDownscaleFactors() }
        val rawFpsSteps = remember { getSupportedFpsTargets(context) }
        val downscaleDisplaySteps = remember(rawDownscaleSteps, defaultLabel) {
            rawDownscaleSteps.map { if (it == "default") defaultLabel else it }
        }
        val fpsDisplaySteps = remember(rawFpsSteps, defaultLabel) {
            rawFpsSteps.map { if (it == "default") defaultLabel else it }
        }
        val dynamicRefreshDisplayModes = remember(rawRefreshModes, defaultLabel) {
            rawRefreshModes.map { mode ->
                if (mode.equals("default", ignoreCase = true)) defaultLabel else mode
            }
        }
        fun getBoolIndex(v: String?): Int = when (v) {
            "true" -> 1
            "false" -> 2
            else -> 0
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = HazeBlurStyle.Material3(
                        containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.62f)
                    ) {
                        blurRadius(28.dp)
                    }
                )
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left GameSpace Identity Sidebar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surfaceContainerLow.copy(alpha = 0.90f),
                    modifier = Modifier
                        .width(264.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TopBarIconButton(
                                        icon = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.cd_back),
                                        onClick = onClose
                                    )
                                    Text(
                                        text = stringResource(R.string.app_settings_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = scheme.onSurface
                                    )
                                }
                            }

                            // App identity card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = scheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (resolvedApp != null) {
                                            AppIconImage(app = resolvedApp, size = 48.dp)
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = scheme.primaryContainer,
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Android,
                                                        contentDescription = null,
                                                        tint = scheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resolvedApp?.label ?: appDetails.first,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = scheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = pkg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = scheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.secondaryContainer,
                                            contentColor = scheme.onSecondaryContainer
                                        ) {
                                            Text(
                                                text = "v${appDetails.third}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (localMasterOn) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                            contentColor = if (localMasterOn) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                text = if (localMasterOn) {
                                                    stringResource(R.string.gamespace_tuned)
                                                } else {
                                                    stringResource(R.string.gamespace_untuned)
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Master Switch Card
                            val masterShape = RoundedCornerShape(18.dp)
                            Surface(
                                shape = masterShape,
                                color = if (localMasterOn) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                contentColor = if (localMasterOn) scheme.onPrimaryContainer else scheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .gamepadFocusOutline(shape = masterShape, register = true)
                                    .clip(masterShape)
                                    .clickable {
                                        val next = !localMasterOn
                                        localMasterOn = next
                                        viewModel.toggleMasterSwitch(pkg, next)
                                        ApplistViewmodel.apps = ApplistViewmodel.apps.map {
                                            if (it.packageName == pkg) it.copy(isEnabledInConfig = next) else it
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PowerSettingsNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.master_switch),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (localMasterOn) "Active profile hooks" else "Tap to enable tuning",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (localMasterOn) {
                                                scheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                scheme.onSurfaceVariant
                                            },
                                            maxLines = 1
                                        )
                                    }
                                    Switch(
                                        checked = localMasterOn,
                                        onCheckedChange = { isChecked ->
                                            localMasterOn = isChecked
                                            viewModel.toggleMasterSwitch(pkg, isChecked)
                                            ApplistViewmodel.apps = ApplistViewmodel.apps.map {
                                                if (it.packageName == pkg) it.copy(isEnabledInConfig = isChecked) else it
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom Quick Actions: Launch & System Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val launchShape = RoundedCornerShape(16.dp)
                            Surface(
                                shape = launchShape,
                                color = scheme.primary,
                                contentColor = scheme.onPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .gamepadFocusOutline(shape = launchShape, register = true)
                                    .clip(launchShape)
                                    .clickable {
                                        if (resolvedApp != null) {
                                            onLaunch(resolvedApp)
                                        } else {
                                            launchGame(context, pkg)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.gamespace_play_game),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            val infoShape = RoundedCornerShape(16.dp)
                            Surface(
                                shape = infoShape,
                                color = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer,
                                modifier = Modifier
                                    .size(44.dp)
                                    .gamepadFocusOutline(shape = infoShape, register = true)
                                    .clip(infoShape)
                                    .clickable {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:$pkg")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = stringResource(R.string.str_app_info),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right GameSpace Tuning Stage
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surfaceContainerLow.copy(alpha = 0.85f),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = localMasterOn,
                        transitionSpec = {
                            (fadeIn(motion.defaultEffectsSpec()) + scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialScale = 0.96f
                            )).togetherWith(
                                fadeOut(motion.defaultEffectsSpec()) + scaleOut(targetScale = 0.98f)
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "AppTuningMasterContent"
                    ) { enabled ->
                        if (!enabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = scheme.secondaryContainer,
                                        contentColor = scheme.onSecondaryContainer,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = null,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.master_switch),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.master_switch_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.widthIn(max = 360.dp)
                                    )
                                }
                            }
                        } else {
                            val displayConfig = config ?: AppConfig()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        text = stringResource(R.string.section_performance).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = scheme.primary
                                    )
                                }
                                item {
                                    ExpressiveList(
                                        content = listOf {
                                            ExpressiveDropdownItem(
                                                icon = Icons.Rounded.Speed,
                                                title = stringResource(R.string.perf_lite_mode),
                                                summary = stringResource(R.string.perf_lite_mode_desc_short),
                                                items = booleanModes,
                                                selectedIndex = getBoolIndex(displayConfig.perf_lite_mode),
                                                onItemSelected = { index ->
                                                    val value = listOf("default", "true", "false")[index]
                                                    viewModel.updateSetting(pkg, "perf_lite_mode", value)
                                                }
                                            )
                                        }
                                    )
                                }

                                item {
                                    Text(
                                        text = stringResource(R.string.section_additionalsettings).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = scheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                item {
                                    ExpressiveList(
                                        content = buildList {
                                            if (bypassSupported) {
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.Cable,
                                                        title = stringResource(R.string.bcharging),
                                                        summary = stringResource(R.string.enable_bypass_charge_desc),
                                                        items = booleanModes,
                                                        selectedIndex = getBoolIndex(displayConfig.bypass_charging),
                                                        onItemSelected = { index ->
                                                            val value = listOf("default", "true", "false")[index]
                                                            viewModel.updateSetting(pkg, "bypass_charging", value)
                                                        }
                                                    )
                                                }
                                            }
                                            add {
                                                ExpressiveDropdownItem(
                                                    icon = Icons.Rounded.RocketLaunch,
                                                    title = stringResource(R.string.game_preload),
                                                    summary = stringResource(R.string.game_preload_desc),
                                                    items = booleanModes,
                                                    selectedIndex = getBoolIndex(displayConfig.game_preload),
                                                    onItemSelected = { index ->
                                                        val value = listOf("default", "true", "false")[index]
                                                        viewModel.updateSetting(pkg, "game_preload", value)
                                                    }
                                                )
                                            }
                                            if (isFullModeEnabled) {
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.SwapVerticalCircle,
                                                        title = stringResource(R.string.app_priority),
                                                        summary = stringResource(R.string.app_priority_desc),
                                                        items = booleanModes,
                                                        selectedIndex = getBoolIndex(displayConfig.app_priority),
                                                        onItemSelected = { index ->
                                                            val value = listOf("default", "true", "false")[index]
                                                            viewModel.updateSetting(pkg, "app_priority", value)
                                                        }
                                                    )
                                                }
                                            }
                                            add {
                                                ExpressiveDropdownItem(
                                                    icon = Icons.Rounded.DoNotDisturbOn,
                                                    title = stringResource(R.string.dnd_mode),
                                                    summary = stringResource(R.string.dnd_mode_desc),
                                                    items = booleanModes,
                                                    selectedIndex = getBoolIndex(displayConfig.dnd_on_gaming),
                                                    onItemSelected = { index ->
                                                        val value = listOf("default", "true", "false")[index]
                                                        viewModel.updateSetting(pkg, "dnd_on_gaming", value)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }

                                item {
                                    Text(
                                        text = stringResource(R.string.section_display_render_settings).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = scheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                item {
                                    ExpressiveList(
                                        content = buildList {
                                            if (isFullModeEnabled) {
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.WebStories,
                                                        title = stringResource(R.string.refreshrates),
                                                        summary = stringResource(R.string.refreshrates_desc),
                                                        items = dynamicRefreshDisplayModes,
                                                        selectedIndex = rawRefreshModes.indexOfFirst {
                                                            it.equals(displayConfig.refresh_rate, ignoreCase = true)
                                                        }.coerceAtLeast(0),
                                                        onItemSelected = { index ->
                                                            val value = rawRefreshModes[index]
                                                            viewModel.updateSetting(pkg, "refresh_rate", value)
                                                        }
                                                    )
                                                }
                                            }
                                            add {
                                                ExpressiveDropdownItem(
                                                    icon = Icons.Rounded.Layers,
                                                    title = stringResource(R.string.renderengine),
                                                    summary = stringResource(R.string.renderengine_desc),
                                                    items = rendererModes,
                                                    selectedIndex = rendererValues.indexOfFirst {
                                                        it.equals(displayConfig.renderer, ignoreCase = true)
                                                    }.coerceAtLeast(0),
                                                    onItemSelected = { index ->
                                                        val value = rendererValues[index]
                                                        viewModel.updateSetting(pkg, "renderer", value)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }

                                if (isGameApp) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.section_resolution_settings).uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = scheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    item {
                                        ExpressiveList(
                                            content = buildList {
                                                add {
                                                    val currentDownscaleIndex = rawDownscaleSteps
                                                        .indexOf(displayConfig.resolution_downscale)
                                                        .coerceAtLeast(0)
                                                    var downscaleSliderPos by remember(displayConfig.resolution_downscale) {
                                                        mutableFloatStateOf(currentDownscaleIndex.toFloat())
                                                    }
                                                    ExpressiveSliderItem(
                                                        icon = Icons.Rounded.AspectRatio,
                                                        title = stringResource(R.string.resolution_downscale_title),
                                                        subtitle = stringResource(R.string.resolution_downscale_desc),
                                                        badgeText = downscaleDisplaySteps.getOrElse(downscaleSliderPos.toInt()) { defaultLabel },
                                                        sliderPosition = downscaleSliderPos,
                                                        valueRange = 0f..(rawDownscaleSteps.size - 1).toFloat(),
                                                        steps = rawDownscaleSteps.size - 2,
                                                        onValueChange = { downscaleSliderPos = it },
                                                        onValueChangeFinished = {
                                                            val value = rawDownscaleSteps[downscaleSliderPos.toInt()]
                                                            viewModel.updateSetting(pkg, "resolution_downscale", value)
                                                        }
                                                    )
                                                }
                                                add {
                                                    val currentFpsIndex = rawFpsSteps
                                                        .indexOf(displayConfig.resolution_fps)
                                                        .coerceAtLeast(0)
                                                    var fpsSliderPos by remember(displayConfig.resolution_fps) {
                                                        mutableFloatStateOf(currentFpsIndex.toFloat())
                                                    }
                                                    val downscaleIsOff = displayConfig.resolution_downscale == "default"
                                                    ExpressiveSliderItem(
                                                        icon = Icons.Rounded.Speed,
                                                        title = stringResource(R.string.resolution_fps_target_title),
                                                        subtitle = stringResource(R.string.resolution_fps_target_desc),
                                                        badgeText = "${fpsDisplaySteps.getOrElse(fpsSliderPos.toInt()) { "60" }}fps",
                                                        sliderPosition = fpsSliderPos,
                                                        valueRange = 0f..(rawFpsSteps.size - 1).toFloat(),
                                                        steps = rawFpsSteps.size - 2,
                                                        enabled = !downscaleIsOff,
                                                        onValueChange = { fpsSliderPos = it },
                                                        onValueChangeFinished = {
                                                            val value = rawFpsSteps[fpsSliderPos.toInt()]
                                                            viewModel.updateSetting(pkg, "resolution_fps", value)
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
@Composable
private fun ZXGameSpaceExitDialog(
    onDismiss: () -> Unit,
    onExitShell: () -> Unit,
    onExitApp: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val defaultRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { defaultRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(min = 460.dp, max = 520.dp)
                .padding(20.dp)
                .gamepadKeyWatch()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Disc & Title
                Surface(
                    shape = CircleShape,
                    color = scheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            tint = scheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.gamespace_exit_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Action Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Option 1: Return to AZenith
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .focusRequester(defaultRequester)
                            .gamepadFocusOutline(shape = RoundedCornerShape(20.dp), register = true)
                            .clickable(onClick = onExitShell)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.gamespace_exit_shell),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.gamespace_exit_shell_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Quit AZenith Completely
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .gamepadFocusOutline(shape = RoundedCornerShape(20.dp), register = true)
                            .clickable(onClick = onExitApp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = scheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.gamespace_exit_app),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.error
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.gamespace_exit_app_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Cancel Pill Button
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .gamepadFocusOutline(shape = RoundedCornerShape(percent = 50), register = true)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Full-screen atmospheric background that crossfades between the ZXGameSpace contour/symbol wave
 * and a rich game artwork stage with Material 3 tonal scrims.
 */
@Composable
private fun ZXGameSpaceStageBackdrop(entry: RibbonEntry) {
    val scheme = MaterialTheme.colorScheme
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current
    val isAmoled = scheme.surfaceContainerLowest == Color.Black

    Crossfade(
        targetState = entry,
        animationSpec = if (animationsEnabled) tween(300) else snap(),
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
                                .alpha(if (isAmoled) 0.22f else 0.38f)
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
                                .alpha(if (isAmoled) 0.20f else 0.28f)
                        )
                    } else if (!isAmoled) {
                        Image(
                            painter = painterResource(R.drawable.banner_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.28f)
                        )
                    }
                }

                else -> {
                    if (!isAmoled) {
                        Image(
                            painter = painterResource(R.drawable.banner_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.22f)
                        )
                    }
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
                            colors = if (isAmoled) {
                                listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            } else {
                                listOf(
                                    scheme.surfaceContainerLowest.copy(alpha = 0.92f),
                                    scheme.surfaceContainerLowest.copy(alpha = 0.58f),
                                    scheme.primaryContainer.copy(alpha = 0.14f)
                                )
                            }
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isAmoled) {
                                listOf(
                                    Color.Black.copy(alpha = 0.78f),
                                    Color.Transparent,
                                    Color.Black
                                )
                            } else {
                                listOf(
                                    scheme.surfaceContainerLowest.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    scheme.surfaceContainerLowest.copy(alpha = 0.94f)
                                )
                            }
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
    onStatusClick: () -> Unit = {},
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Segment: Category Navigation Tabs & Scrolled Wordmark
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val density = LocalDensity.current
            var tabBounds by remember { mutableStateOf(mapOf<TopCategory, Pair<Float, Float>>()) }
            val currentBounds = tabBounds[selectedCategory] ?: (0f to 0f)

            val animatedPillX by animateFloatAsState(
                targetValue = currentBounds.first,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "TopBarPillX"
            )
            val animatedPillWidth by animateFloatAsState(
                targetValue = currentBounds.second,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "TopBarPillWidth"
            )

            Box(contentAlignment = Alignment.CenterStart) {
                if (animatedPillWidth > 0f) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier
                            .offset { IntOffset(animatedPillX.roundToInt(), 0) }
                            .width(with(density) { animatedPillWidth.toDp() })
                            .height(36.dp)
                    ) {}
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopCategory.entries.forEach { tab ->
                        val selected = tab == selectedCategory
                        val shape = RoundedCornerShape(14.dp)
                        Surface(
                            shape = shape,
                            color = Color.Transparent,
                            contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(shape)
                                .onGloballyPositioned { coordinates ->
                                    val parentX = coordinates.positionInParent().x
                                    val width = coordinates.size.width.toFloat()
                                    if (tabBounds[tab]?.first != parentX || tabBounds[tab]?.second != width) {
                                        tabBounds = tabBounds + (tab to (parentX to width))
                                    }
                                }
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clickable { onSelectCategory(tab) }
                        ) {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
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
                            .background(scheme.outlineVariant.copy(alpha = 0.6f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right Segment: Actions & System Status Capsule
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val animationsEnabled = LocalGameSpaceAnimationsEnabled.current
            val searchRequester = remember { FocusRequester() }
            val searchWidth by animateDpAsState(
                targetValue = if (searchOpen) 268.dp else 38.dp,
                animationSpec = if (animationsEnabled) {
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                } else {
                    snap()
                },
                label = "SearchBarWidth"
            )
            val searchBg by animateColorAsState(
                targetValue = if (searchOpen) {
                    scheme.surfaceContainerHighest
                } else {
                    scheme.surfaceContainerHigh.copy(alpha = 0.85f)
                },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "SearchBarBg"
            )

            LaunchedEffect(searchOpen) {
                if (searchOpen) {
                    searchRequester.requestFocus()
                }
            }

            Surface(
                shape = CircleShape,
                color = searchBg,
                contentColor = scheme.onSurfaceVariant,
                modifier = Modifier
                    .width(searchWidth)
                    .height(38.dp)
                    .clip(CircleShape)
                    .gamepadFocusOutline(shape = CircleShape, register = !searchOpen)
                    .then(
                        if (!searchOpen) {
                            Modifier.clickable(onClick = onSearchClick)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (searchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = scheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(scheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchRequester),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_apps),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceContainerHigh,
                            contentColor = scheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSearchClose)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.gamespace_search_close),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.gamespace_library_title),
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Profile avatar with online status indicator dot.
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .gamepadFocusOutline(shape = CircleShape, register = true)
                    .clickable(onClick = onAvatarClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = stringResource(R.string.gamespace_profile),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 1.dp)
                            .clip(CircleShape)
                            .background(scheme.primary)
                            .border(1.5.dp, scheme.surfaceContainerLowest, CircleShape)
                    )
                }
            }

            TopBarIconButton(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = stringResource(R.string.gamespace_rail_exit),
                tint = scheme.onSurfaceVariant,
                onClick = onExit
            )

            // Material 3 Expressive Unified System Status Capsule (Clock & Battery)
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .gamepadFocusOutline(shape = CircleShape, register = true)
                    .clickable(onClick = onStatusClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = clock,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = scheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(scheme.outlineVariant.copy(alpha = 0.7f))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isCharging = battery.second
                        val isLow = battery.first <= 20
                        val batteryColor = when {
                            isCharging -> scheme.primary
                            isLow -> scheme.error
                            else -> scheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Rounded.BatteryChargingFull
                                isLow -> Icons.Rounded.BatteryAlert
                                else -> Icons.Rounded.BatteryStd
                            },
                            contentDescription = stringResource(R.string.gamespace_info_battery),
                            tint = batteryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${battery.first}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = batteryColor
                        )
                    }
                }
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
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = tint,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .gamepadFocusOutline(shape = CircleShape, register = true)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
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
    onActivate: (RibbonEntry) -> Unit,
    focusRequester: FocusRequester? = null
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val itemPitchPx = with(density) { 62.dp.toPx() }
    val expandedRightBasePx = with(density) { (88.dp + 12.dp).toPx() }

    val carouselPos = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(selectedIndex, isDragging) {
        if (!isDragging) {
            carouselPos.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val activeEntry = entries.getOrElse(selectedIndex) { RibbonEntry.Welcome }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .pointerInput(entries.size) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        val maxIdx = entries.lastIndex.coerceAtLeast(0)
                        val target = carouselPos.value.roundToInt().coerceIn(0, maxIdx)
                        if (target != currentSelectedIndex) {
                            currentOnSelect(target)
                        }
                        scope.launch {
                            carouselPos.animateTo(
                                targetValue = target.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val maxIdx = entries.lastIndex.coerceAtLeast(0)
                        val nextPos = (carouselPos.value - (dragAmount / itemPitchPx))
                            .coerceIn(0f, maxIdx.toFloat())
                        scope.launch {
                            carouselPos.snapTo(nextPos)
                        }
                        val nearest = nextPos.roundToInt().coerceIn(0, maxIdx)
                        if (nearest != currentSelectedIndex) {
                            currentOnSelect(nearest)
                        }
                    }
                )
            }
    ) {
        val scrollItems = (carouselPos.value - 2f).coerceAtLeast(0f)
        val totalOffsetPx = -scrollItems * itemPitchPx

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .offset { IntOffset(x = totalOffsetPx.roundToInt(), y = 0) }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .gamepadScreenEntry(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            entries.forEachIndexed { index, item ->
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
                    }
                )
            }
        }

        // Adjacent label sits just right of the expanded tile's lower edge and stays anchored at slot 3.
        val visualSlot = carouselPos.value.coerceIn(0f, 2f)
        val labelOffsetPx = ((visualSlot * itemPitchPx) + expandedRightBasePx).roundToInt()

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
    val colorSpec = motion.defaultEffectsSpec<Color>()

    val tileSize by animateDpAsState(
        targetValue = if (selected) 88.dp else 54.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
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
        // ring when it does not, positioned at the top-right corner of the highlight.
        if (entry is RibbonEntry.Game) {
            val on = entry.app.isEnabledInConfig
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (selected) 6.dp else 4.dp)
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIconImage(app = app, size = if (selected) 82.dp else 54.dp)
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
    onOpenSettings: () -> Unit,
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
                    onOpenSettings = onOpenSettings
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
    onOpenSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onOpenSettings),
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
            // Left Hero Column: Emblem/Title + Package/Version metadata
            val versionName = app.packageInfo.versionName?.takeIf { it.isNotBlank() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 14.dp, end = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconImage(app = app, size = 56.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (versionName != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.secondaryContainer.copy(alpha = 0.85f),
                                    contentColor = scheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = "v$versionName",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
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
    onClose: () -> Unit,
    onNavigateUp: () -> Unit = {}
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
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onNavigateUp()
                        true
                    } else {
                        false
                    }
                },
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

@Composable
private fun ZXGameSpaceSettingsStage(
    selectedTab: SettingsSidebarTab = SettingsSidebarTab.General,
    onSelectTab: (SettingsSidebarTab) -> Unit = {},
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    deviceName: String = "",
    chipsetName: String = "",
    onApplyProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current

    Row(
        modifier = modifier.padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.82f),
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = scheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                )

                var tabPositions by remember { mutableStateOf(mapOf<SettingsSidebarTab, Pair<Float, Float>>()) }
                val currentPos = tabPositions[selectedTab] ?: (0f to 0f)

                val animatedY by animateFloatAsState(
                    targetValue = currentPos.first,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SidebarChipY"
                )
                val animatedHeight by animateFloatAsState(
                    targetValue = currentPos.second,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SidebarChipHeight"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (animatedHeight > 0f) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, animatedY.roundToInt()) }
                                .height(with(density) { animatedHeight.toDp() })
                        ) {}
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSidebarTab.entries.forEach { tab ->
                            val isSelected = tab == selectedTab
                            val itemShape = RoundedCornerShape(16.dp)
                            val contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant

                            Surface(
                                shape = itemShape,
                                color = Color.Transparent,
                                contentColor = contentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        val parentY = coordinates.positionInParent().y
                                        val height = coordinates.size.height.toFloat()
                                        if (tabPositions[tab]?.first != parentY || tabPositions[tab]?.second != height) {
                                            tabPositions = tabPositions + (tab to (parentY to height))
                                        }
                                    }
                                    .gamepadFocusOutline(shape = itemShape, register = true)
                                    .clip(itemShape)
                                    .clickable { onSelectTab(tab) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerLow.copy(alpha = 0.72f),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.98f))
                        .togetherWith(fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 1.01f))
                },
                modifier = Modifier.fillMaxSize(),
                label = "SettingsPaneTransition"
            ) { tab ->
                when (tab) {
                    SettingsSidebarTab.General -> SettingsGeneralPane()
                    SettingsSidebarTab.Tweaks -> SettingsTweaksPane(
                        profile = profile,
                        profileValue = profileValue,
                        refreshHz = refreshHz,
                        onApplyProfile = onApplyProfile
                    )
                    SettingsSidebarTab.Theme -> SettingsThemePane()
                    SettingsSidebarTab.About -> SettingsAboutPane(
                        deviceName = deviceName,
                        chipsetName = chipsetName
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGeneralPane(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.gamespace_settings_general),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Text(
                    text = "Core daemon behavior, system services, and diagnostic utilities",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            statusFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.show_notifications),
                    summary = "Post status notifications when active profile switches",
                    checked = uiState.profileNotifications,
                    onCheckedChange = { settingsViewModel.setProfileNotifications(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.tap_haptic_feedback),
                    summary = "Tactile impulse feedback on interactive taps and toggles",
                    checked = uiState.tapHaptic,
                    onCheckedChange = { settingsViewModel.onTapHapticToggle(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.show_toast),
                    summary = "Display brief toast notifications on profile and state switches",
                    checked = uiState.stateToast,
                    onCheckedChange = { settingsViewModel.setShowToast(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.allow_verbose_log),
                    summary = "Write extended debug diagnostics to /data/adb/.config/AZenith",
                    checked = uiState.debugMode,
                    onCheckedChange = { settingsViewModel.setDebugMode(it) }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.disable_auto_mode),
                    summary = stringResource(R.string.disable_auto_mode_desc),
                    checked = uiState.autoMode,
                    onCheckedChange = { settingsViewModel.setAutoMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.disable_tweak),
                    summary = stringResource(R.string.disable_tweak_desc),
                    checked = uiState.disableTweak,
                    onCheckedChange = { settingsViewModel.setDisableTweak(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.HourglassEmpty,
                    title = stringResource(R.string.profile_timeout),
                    summary = stringResource(R.string.profile_timeout_desc),
                    checked = uiState.profileTimeout,
                    onCheckedChange = { settingsViewModel.setProfileTimeout(it) }
                )
            }
        }

        Text(
            text = stringResource(R.string.section_diagnostics).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = scheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.restart_service),
                summary = stringResource(R.string.restart_service_desc),
                onClick = {
                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service --rerun").submit { res ->
                        statusFeedback = if (res.isSuccess) {
                            context.getString(R.string.toast_restarting_service)
                        } else {
                            "Restart failed"
                        }
                    }
                }
            )
            SettingsActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Save,
                title = stringResource(R.string.save_log),
                summary = stringResource(R.string.save_log_desc),
                onClick = {
                    coroutineScope.launch {
                        val file = dumpDiagnosticLogs(context, saveToDownloads = true)
                        statusFeedback = if (file != null && file.exists()) {
                            context.getString(R.string.toast_log_saved)
                        } else {
                            context.getString(R.string.toast_log_save_fail)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsTweaksPane(
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    onApplyProfile: (String) -> Unit = {},
    tweakViewModel: TweakViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (!tweakViewModel.isUiLoaded) {
            tweakViewModel.loadAllConfiguration(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_tweaks),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Text(
                    text = "Kernel governor, scheduling optimizations, and hardware accelerators",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val profiles = listOf(
                Triple("1", R.string.Profile_Performance, R.drawable.ic_performance),
                Triple("2", R.string.Profile_Balanced, R.drawable.ic_balanced),
                Triple("3", R.string.Profile_ECO_mode, R.drawable.ic_eco)
            )
            profiles.forEach { (value, labelRes, iconRes) ->
                val active = profileValue == value || (value == "2" && profileValue.isBlank())
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    shape = shape,
                    color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .gamepadFocusOutline(shape = shape, register = true)
                        .clip(shape)
                        .clickable { onApplyProfile(value) }
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
                            modifier = Modifier.size(24.dp)
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.perf_lite_mode),
                    summary = stringResource(R.string.perf_lite_mode_desc),
                    checked = tweakViewModel.liteState ?: false,
                    onCheckedChange = { tweakViewModel.updateLiteMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.RocketLaunch,
                    title = stringResource(R.string.game_preload),
                    summary = stringResource(R.string.game_preload_desc),
                    checked = tweakViewModel.preloadState ?: false,
                    onCheckedChange = { tweakViewModel.updatePreloadMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.CleaningServices,
                    title = stringResource(R.string.memory_killer),
                    summary = stringResource(R.string.memory_killer_desc),
                    checked = tweakViewModel.memKillerState ?: false,
                    onCheckedChange = { tweakViewModel.updateMemoryKiller(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.SwapVerticalCircle,
                    title = stringResource(R.string.app_priority_control),
                    summary = stringResource(R.string.app_priority_control_desc),
                    checked = tweakViewModel.appPriorState ?: false,
                    onCheckedChange = { tweakViewModel.updateAppPriority(it) }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = stringResource(R.string.dnd_mode_gaming),
                    summary = stringResource(R.string.dnd_mode_gaming_desc),
                    checked = tweakViewModel.dndState ?: false,
                    onCheckedChange = { tweakViewModel.updateDndMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.ContentCut,
                    title = stringResource(R.string.trim_filesystem),
                    summary = stringResource(R.string.trim_filesystem_desc),
                    checked = tweakViewModel.fstrimState ?: false,
                    onCheckedChange = { tweakViewModel.updateFstrim(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.ThermostatAuto,
                    title = stringResource(R.string.thermalcore_service),
                    summary = stringResource(R.string.thermalcore_service_desc),
                    checked = tweakViewModel.thermalState ?: false,
                    onCheckedChange = { tweakViewModel.updateThermalCore(it) }
                )

                SettingsActionCard(
                    icon = Icons.Rounded.WebStories,
                    title = stringResource(R.string.refreshrates),
                    summary = "Display refresh rate locked during gaming",
                    trailingBadge = if (refreshHz > 0) "${refreshHz}Hz" else "${tweakViewModel.currentRefreshRate ?: 60}Hz",
                    onClick = {
                        val supported = getSupportedRefreshRatesPicker(context)
                        if (supported.isNotEmpty()) {
                            val curr = (tweakViewModel.currentRefreshRate ?: 60).toString()
                            val currIdx = supported.indexOf(curr)
                            val next = if (currIdx >= 0 && currIdx < supported.lastIndex) supported[currIdx + 1] else supported.first()
                            tweakViewModel.executeSetRefreshRates(next, context)
                        }
                    }
                )
                SettingsActionCard(
                    icon = Icons.Rounded.SettingsSuggest,
                    title = stringResource(R.string.renderengine),
                    summary = "HWUI graphics compositing backend",
                    trailingBadge = tweakViewModel.currentRenderer?.take(10) ?: "Default",
                    onClick = {
                        val current = (tweakViewModel.currentRenderer ?: "default").lowercase()
                        val next = when {
                            current.contains("vk") -> "default"
                            current.contains("gl") -> "skiavk"
                            else -> "skiagl"
                        }
                        tweakViewModel.executeSetRenderer(next, context)
                    }
                )
            }
        }
    }
}

private val GAMESPACE_PRESET_KEY_COLORS = listOf(
    0xFFF44336.toInt() to "Crimson",
    0xFFE91E63.toInt() to "Sakura",
    0xFF9C27B0.toInt() to "Orchid",
    0xFF673AB7.toInt() to "Iris",
    0xFF3F51B5.toInt() to "Indigo",
    0xFF2196F3.toInt() to "Azure",
    0xFF00BCD4.toInt() to "Cyan",
    0xFF009688.toInt() to "Teal",
    0xFF4FAF50.toInt() to "Emerald",
    0xFFFFEB3B.toInt() to "Solar",
    0xFFFFC107.toInt() to "Amber",
    0xFFFF9800.toInt() to "Tangerine",
    0xFF795548.toInt() to "Mocha",
    0xFF607D8F.toInt() to "Slate",
    0xFFFF9CA8.toInt() to "Blossom",
)

@Composable
private fun SettingsThemePane() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val systemDark = isSystemInDarkTheme()

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var colorModeValue by remember { mutableIntStateOf(prefs.getInt("color_mode", ColorMode.SYSTEM.value)) }
    var keyColorValue by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 100).coerceIn(70, 150))
    }
    var animationsOn by remember {
        mutableStateOf(prefs.getBoolean("gamespace_animations", true))
    }
    val currentColorSpec = remember {
        ColorEngine.fromPersisted(prefs.getString("color_spec", null))
    }

    val isDarkPreview = when (ColorMode.fromValue(colorModeValue)) {
        ColorMode.LIGHT -> false
        ColorMode.DARK, ColorMode.DARKAMOLED -> true
        ColorMode.SYSTEM -> systemDark
    }

    val followMonet = keyColorValue == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Theme mode, interface scaling, motion transitions, and Monet or preset color palettes",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // 1. Theme Mode (Auto / Light / Dark / AMOLED)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (colorModeValue == ColorMode.DARKAMOLED.value) {
                                "AMOLED mode draws pure black backgrounds where possible"
                            } else {
                                "Auto, Light, and Dark follow Monet dynamic surface tones"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer
                    ) {
                        Text(
                            text = when (ColorMode.fromValue(colorModeValue)) {
                                ColorMode.SYSTEM -> "Auto"
                                ColorMode.LIGHT -> "Light"
                                ColorMode.DARK -> "Dark"
                                ColorMode.DARKAMOLED -> "AMOLED"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val modes = listOf(
                        Triple("Auto", ColorMode.SYSTEM.value, Icons.Rounded.BrightnessAuto),
                        Triple("Light", ColorMode.LIGHT.value, Icons.Rounded.LightMode),
                        Triple("Dark", ColorMode.DARK.value, Icons.Rounded.DarkMode),
                        Triple("AMOLED", ColorMode.DARKAMOLED.value, Icons.Rounded.Android)
                    )
                    modes.forEach { (name, value, icon) ->
                        val selected = colorModeValue == value
                        val pillRadius by animateIntAsState(
                            targetValue = if (selected) 50 else 28,
                            animationSpec = if (animationsOn) {
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            } else {
                                snap()
                            },
                            label = "ThemeModePillRadius"
                        )
                        val shape = RoundedCornerShape(percent = pillRadius)
                        Surface(
                            shape = shape,
                            color = if (selected) scheme.primary else scheme.surfaceContainerHighest,
                            contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clip(shape)
                                .clickable {
                                    colorModeValue = value
                                    prefs.edit().putInt("color_mode", value).apply()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) scheme.onPrimary else scheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. App Scale (70% .. 150%, step 5%, default 100%) + Animations Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHigh,
                modifier = Modifier.weight(1.25f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = scheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = null,
                                        tint = scheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "App Scale",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "70% downscale to 150% upscale (5% steps)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val stepBtnShape = RoundedCornerShape(12.dp)
                            Surface(
                                shape = stepBtnShape,
                                color = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .size(34.dp)
                                    .gamepadFocusOutline(shape = stepBtnShape, register = true)
                                    .clip(stepBtnShape)
                                    .clickable(enabled = appScalePercent > 70) {
                                        val next = (appScalePercent - 5).coerceIn(70, 150)
                                        appScalePercent = next
                                        prefs.edit().putInt("gamespace_app_scale", next).apply()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = "Decrease scale",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (appScalePercent == 100) scheme.primaryContainer else scheme.primary,
                                contentColor = if (appScalePercent == 100) scheme.onPrimaryContainer else scheme.onPrimary,
                                modifier = Modifier
                                    .gamepadFocusOutline(shape = CircleShape, register = true)
                                    .clip(CircleShape)
                                    .clickable {
                                        appScalePercent = 100
                                        prefs.edit().putInt("gamespace_app_scale", 100).apply()
                                    }
                            ) {
                                Text(
                                    text = "${appScalePercent}%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = stepBtnShape,
                                color = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .size(34.dp)
                                    .gamepadFocusOutline(shape = stepBtnShape, register = true)
                                    .clip(stepBtnShape)
                                    .clickable(enabled = appScalePercent < 150) {
                                        val next = (appScalePercent + 5).coerceIn(70, 150)
                                        appScalePercent = next
                                        prefs.edit().putInt("gamespace_app_scale", next).apply()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Increase scale",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    ZenithSlider(
                        value = appScalePercent.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(70, 150)
                            if (snapped != appScalePercent) {
                                appScalePercent = snapped
                                prefs.edit().putInt("gamespace_app_scale", snapped).apply()
                            }
                        },
                        onValueChangeFinished = {},
                        valueRange = 70f..150f,
                        steps = 15,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }

            SettingsSwitchCard(
                modifier = Modifier.weight(0.95f),
                icon = Icons.Rounded.AutoAwesome,
                title = "Animations",
                summary = if (animationsOn) {
                    "Play Material 3 bouncy expressive animations"
                } else {
                    "Instant pop-ups with transition animations disabled"
                },
                checked = animationsOn,
                onCheckedChange = { enabled ->
                    animationsOn = enabled
                    prefs.edit().putBoolean("gamespace_animations", enabled).apply()
                }
            )
        }

        // 3. App Theme Palette (Follow Monet vs Existing Preset Seed Colors)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Theme Palette",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (followMonet) {
                                "Following system Monet dynamic wallpaper palette"
                            } else {
                                val presetLabel = GAMESPACE_PRESET_KEY_COLORS.firstOrNull { it.first == keyColorValue }?.second ?: "Custom"
                                "Using $presetLabel preset color seed from Personalization"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val monetPillShape = RoundedCornerShape(percent = if (followMonet) 50 else 28)
                        Surface(
                            shape = monetPillShape,
                            color = if (followMonet) scheme.primary else scheme.surfaceContainerHighest,
                            contentColor = if (followMonet) scheme.onPrimary else scheme.onSurface,
                            modifier = Modifier
                                .gamepadFocusOutline(shape = monetPillShape, register = true)
                                .clip(monetPillShape)
                                .clickable {
                                    keyColorValue = 0
                                    prefs.edit().putInt("key_color", 0).apply()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Follow Monet",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        val presetPillShape = RoundedCornerShape(percent = if (!followMonet) 50 else 28)
                        Surface(
                            shape = presetPillShape,
                            color = if (!followMonet) scheme.primary else scheme.surfaceContainerHighest,
                            contentColor = if (!followMonet) scheme.onPrimary else scheme.onSurface,
                            modifier = Modifier
                                .gamepadFocusOutline(shape = presetPillShape, register = true)
                                .clip(presetPillShape)
                                .clickable {
                                    if (followMonet) {
                                        val firstPreset = GAMESPACE_PRESET_KEY_COLORS.first().first
                                        keyColorValue = firstPreset
                                        prefs.edit().putInt("key_color", firstPreset).apply()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Preset Palette",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item(key = "monet_dynamic") {
                        GameSpacePaletteSwatch(
                            seedColor = Color.Unspecified,
                            label = "Monet",
                            selected = followMonet,
                            isDark = isDarkPreview,
                            colorSpec = currentColorSpec,
                            onClick = {
                                keyColorValue = 0
                                prefs.edit().putInt("key_color", 0).apply()
                            }
                        )
                    }
                    items(
                        items = GAMESPACE_PRESET_KEY_COLORS,
                        key = { it.first }
                    ) { (colorInt, name) ->
                        GameSpacePaletteSwatch(
                            seedColor = Color(colorInt),
                            label = name,
                            selected = keyColorValue == colorInt,
                            isDark = isDarkPreview,
                            colorSpec = currentColorSpec,
                            onClick = {
                                keyColorValue = colorInt
                                prefs.edit().putInt("key_color", colorInt).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameSpacePaletteSwatch(
    seedColor: Color,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    colorSpec: ColorEngine,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val animationsEnabled = LocalGameSpaceAnimationsEnabled.current

    val swatchScheme = if (seedColor == Color.Unspecified) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = isDark,
            specVersion = colorSpec.librarySpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            specVersion = colorSpec.librarySpec
        )
    }

    val cornerPercent by animateIntAsState(
        targetValue = if (selected) 36 else 24,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            snap()
        },
        label = "SwatchCornerPercent"
    )
    val shape = RoundedCornerShape(percent = cornerPercent)

    Surface(
        shape = shape,
        color = if (selected) swatchScheme.primaryContainer else swatchScheme.surfaceContainerHighest,
        modifier = Modifier
            .width(84.dp)
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    drawArc(
                        color = swatchScheme.primary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.tertiaryContainer,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.secondaryContainer,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = swatchScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = swatchScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(swatchScheme.primary)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (selected) swatchScheme.onPrimaryContainer else swatchScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsAboutPane(
    deviceName: String = "",
    chipsetName: String = ""
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_about),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Project information, architecture specifications, and community links",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = rememberDrawablePainter(
                                context.applicationInfo.loadIcon(context.packageManager)
                            ),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.primaryContainer)
                        )

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = scheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = scheme.primary
                            )
                            Text(
                                text = "$deviceName · $chipsetName",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "COMMUNITY & CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Code,
                            title = "GitHub",
                            summary = "Source repository",
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                        )
                        SettingsActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Share,
                            title = "Telegram",
                            summary = "Discussion group",
                            onClick = { uriHandler.openUri("https://t.me/ArchHavenDisc") }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.str_maintainer).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar_liliya),
                            contentDescription = stringResource(R.string.str_liliya),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.str_liliya),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.str_creator_maintainer),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = stringResource(R.string.str_open_source).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.str_licenses) + " · Apache 2.0",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Built with Material 3 Expressive, Android NDK, Kotlin Coroutines & Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) scheme.primaryContainer else scheme.surfaceContainerHighest,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                    uncheckedThumbColor = scheme.outline,
                    uncheckedTrackColor = scheme.surfaceContainerHighest
                )
            )
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailingBadge: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.secondaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
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
