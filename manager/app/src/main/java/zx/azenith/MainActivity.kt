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

package zx.azenith

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.tracing.Trace
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.mainscreens.*
import zx.azenith.ui.subscreens.*
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.util.*
import zx.azenith.ui.theme.NavEdge
import zx.azenith.ui.theme.NavLabelMode
import zx.azenith.ui.theme.NavStyle
import zx.azenith.ui.theme.currentPersonalization
import androidx.compose.ui.graphics.RectangleShape


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        val fromTileType = if (intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            val component = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, android.content.ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
            }
            
            when (component?.className) {
                "zx.azenith.TileService.BypassChgTileService" -> "bypass"
                "zx.azenith.TileService.ProfileTileService" -> "profile"
                else -> null
            }
        } else null

        setContent {
            AZenithTheme {
                MainScreen(fromTileType)
            }
        }
    }
}

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * MD3 emphasized motion curves.
 *
 * Compose only ships FastOutSlowIn, LinearOutSlowIn and FastOutLinearIn, so
 * the emphasized pair the MD3 motion spec defines for screen transitions is
 * written out here from its cubic-beziers. Screen transitions still use
 * easing and duration; spring physics is for component state changes.
 */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

data class NavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val gradientColors: List<Color> = listOf(Color.Transparent, Color.Transparent)
)

// Shared so the pill and its call site agree on one spec. Hoisted to file level
// because a composable default argument cannot see a local of the other scope.
private val NAV_PILL_SPEC: FiniteAnimationSpec<Color> =
    androidx.compose.material3.MotionScheme.Companion.expressive().defaultEffectsSpec()



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(fromTileType: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var useScrollAnimation by remember { mutableStateOf(settingsPrefs.getBoolean("use_scroll_animation", true)) }
    
    val pagerRoutes = remember { listOf("home", "applist", "tweaks", "settings") }
    val pagerState = rememberPagerState(initialPage = 0) { pagerRoutes.size }
    
    val bottomBarRoutes = remember { setOf("main") }
    var showExitConfirm by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(fromTileType) {
        val rootNav = "main"
        when (fromTileType) {
            "bypass" -> {
                navController.navigate("bypasschg") {
                    popUpTo(rootNav) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            "profile" -> {
                navController.navigate(rootNav) {
                    popUpTo(rootNav) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
     
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rawRoute = navBackStackEntry?.destination?.route
    val isOnMainPager = rawRoute == "main"
    
    // Which route the navbar highlights. This is tracked from the tap rather
    // than derived from the pager, because both pager states are wrong for
    // this purpose: settledPage only changes once the 500 ms scroll finishes
    // (so the highlight visibly lags a third of a second behind the page),
    // and currentPage only flips at the scroll midpoint. Tapping a tab has to
    // move the pill immediately, so the intent is recorded here and the
    // animation is just the UI catching up to it.
    val highlightRoute = rememberSaveable { mutableStateOf("home") }

    val currentRoute = if (isOnMainPager) {
        // On swipes there is no tap to record, so take the destination from the
        // pager itself. targetPage is the page the in-flight gesture is heading
        // for, which is already committed from the first pixel of the drag --
        // settledPage would not move until the scroll finished, which is the
        // lag this replaces. The offset fraction is deliberately not read here:
        // it is @FrequentlyChangingValue and would recompose the whole screen
        // on every frame. The pill animates from it in a graphicsLayer below.
        if (!pagerState.isScrollInProgress) {
            highlightRoute.value = pagerRoutes[pagerState.settledPage]
        } else {
            highlightRoute.value = pagerRoutes[pagerState.targetPage]
        }
        highlightRoute.value
    } else {
        highlightRoute.value
    }

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val hasCompletedGetStarted = remember {
        appPrefs.getBoolean("has_completed_get_started", false)
    }
    
    LaunchedEffect(Unit) {
        WallpaperCache.init(context)
    }
    
    var isBlurEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("expressive_blur_ui", false)) }
    val hazeState = remember { HazeState() }
    var rootStatus by remember { mutableStateOf(false) }
    var moduleInstalled by remember { mutableStateOf(false) }

    val navItems = remember { AZENITH_NAV_ITEMS }
    
    val pendingReboot by RebootManager.pendingReboot.collectAsState()

    // Requesting root spawns a shell, and isModuleInstalled() stats a path
    // through su, so probing either one blocks for as long as a su prompt
    // takes. Running that on every route change put the cost in front of each
    // tab switch, so the root and module probes are polled on an IO dispatcher
    // instead of fired per click, and only the preference reads — which are
    // plain SharedPreferences — stay synchronous.
    val refreshPrefs = {
        val newBlur = settingsPrefs.getBoolean("expressive_blur_ui", false)
        if (isBlurEnabled != newBlur) isBlurEnabled = newBlur
        val newScroll = settingsPrefs.getBoolean("use_scroll_animation", true)
        if (useScrollAnimation != newScroll) useScrollAnimation = newScroll
    }

    // Probing root is a fork+exec through su, so the very first call blocks for
    // as long as the su prompt takes. It is launched rather than awaited so it
    // cannot sit in front of the first composition, and the loop is seeded with
    // a cheap read of the already-known state instead of re-probing twice.
    LaunchedEffect(Unit) {
        refreshPrefs()
        while (true) {
            withContext(Dispatchers.IO) {
                rootStatus = RootUtils.requestRootAccess()
                moduleInstalled = RootUtils.isModuleInstalled()
            }
            refreshPrefs()
            delay(2000)
        }
    }
    
    val installingDialog = rememberInstallingDialog()
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            coroutineScope.launch {
                installingDialog.withInstalling {
                    val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        Shell.cmd(
                            "cp /data/adb/modules/AZenith/AZenith.apk /data/local/tmp/AZenith_tmp.apk",
                            "sleep 5 && pm install -r /data/local/tmp/AZenith_tmp.apk",
                            "rm -f /data/local/tmp/AZenith_tmp.apk"
                        ).exec()
                    }
                    if (result.isSuccess) {
                        Toast.makeText(context, context.getString(R.string.toast_update_success), Toast.LENGTH_SHORT).show()
                    } else {
                        val errorLog = result.out.joinToString("\n").ifEmpty { context.getString(R.string.status_unknown) }
                        Toast.makeText(context, context.getString(R.string.toast_install_fail, errorLog), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )

    val rebootDialog = rememberConfirmDialog(
        onConfirm = {
            Shell.cmd("svc power reboot || reboot").submit()
        }
    )

    
    LaunchedEffect(rootStatus) {
        if (rootStatus) {
            val moduleVC = RootUtils.getModuleVersionCode()
            val appVC = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }

            if (appVC < moduleVC && RootUtils.isUpdateApkAvailable()) {
                updateDialog.showConfirm(
                    title = context.getString(R.string.dialog_update_available_title),
                    content = context.getString(R.string.dialog_update_available_content, appVC, moduleVC),
                    confirm = context.getString(R.string.dialog_update_available_confirm),
                    dismiss = context.getString(R.string.dialog_update_available_dismiss)
                )
            }

            if (RootUtils.isModuleUpdatePendingReboot()) {
                rebootDialog.showConfirm(
                    title = context.getString(R.string.dialog_module_update_title),
                    content = context.getString(R.string.dialog_module_update_content),
                    confirm = context.getString(R.string.dialog_module_update_confirm),
                    dismiss = context.getString(R.string.dialog_module_update_dismiss)
                )
            }
        }
    }
    
    val isFabVisible = remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (available.y < -10f) isFabVisible.value = false
                    else if (available.y > 10f) isFabVisible.value = true
                }
                return Offset.Zero
            }
        }
    }

   
    val activeDialogCount = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    CompositionLocalProvider(
        LocalAppHazeState provides hazeState,
        zx.azenith.ui.component.LocalAppBlurEnabled provides isBlurEnabled,
        zx.azenith.ui.component.LocalActiveDialogCount provides activeDialogCount
    ) {
        RootDialogsProvider {
            val isAnyDialogOpen = zx.azenith.ui.component.LocalActiveDialogCount.current.value > 0 || installingDialog.isShown || updateDialog.isShown || rebootDialog.isShown
            
            androidx.activity.compose.BackHandler(enabled = isOnMainPager && !isAnyDialogOpen) {
                if (showExitConfirm) {
                    showExitConfirm = false
                } else if (pagerState.currentPage != 0) {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                } else {
                    showExitConfirm = true
                }
            }
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    // Penentuan start destination dinamis
                    startDestination = if (hasCompletedGetStarted) "main" else "get_started",
                    
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .nestedScroll(nestedScrollConnection),
                    enterTransition = { zx.azenith.ui.navigation.enterTransition() },
                    exitTransition = { zx.azenith.ui.navigation.exitTransition() },
                    popEnterTransition = { zx.azenith.ui.navigation.popEnterTransition() },
                    popExitTransition = { zx.azenith.ui.navigation.popExitTransition() }
                ) {
                    composable("get_started") {
                        // get_started writes has_completed_get_started itself and
                        // then calls back. It used to relaunch the activity through
                        // "am start -S", which force-stops the process first, so
                        // finishing setup restarted the app from cold instead of
                        // moving on to "main".
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            GetStartedScreen(navController) {
                            navController.navigate("main") {
                                popUpTo("get_started") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        }
                    }
                    
                    // Route Pager (Kode 2)
                    composable("main") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this@composable) {

                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                // Prefetch all adjacent pages for long jumps.
                                beyondViewportPageCount = 3
                            ) { page ->
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val absOffset = kotlin.math.abs(pageOffset)
                            
                            val pageModifier = if (!useScrollAnimation) {
                                Modifier.graphicsLayer {
                                    if (absOffset < 1f) {
                                        translationX = pageOffset * size.width
                                        alpha = 1f - absOffset
                                        val scale = 1f - (absOffset * 0.05f)
                                        scaleX = scale
                                        scaleY = scale
                                    } else {
                                        alpha = 0f
                                    }
                                }.zIndex(if (absOffset < 0.5f) 1f else 0f)
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = pageModifier.fillMaxSize()
                            ) {
                                when (pagerRoutes[page]) {
                                    "home" -> HomeScreen()
                                    "applist" -> ApplistScreen(navController)
                                    "tweaks" -> TweakScreen(navController)
                                    "settings" -> SettingsScreen(navController)
                                }
                            }
                            
                        } // ends HorizontalPager
                        

                                } // ends Box
                            } // ends ScreenWrapper
                        } // ends outer Box
                    } // ends composable

                    // Subscreens
                    composable("color_palette") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            ColorPaletteScreen(navController)
                        }
                    }
                    composable("colorscheme") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            ColorSchemeSettings(navController)
                        }
                    }
                    composable("FasScreen") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            FasScreen(navController)
                        }
                    }
                    composable("bypasschg") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            BypassChargeScreen(navController)
                        }
                    }
                    composable("bypasschg_check") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            BypassChargeCheckScreen(navController)
                        }
                    }
                    composable("preferenced") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            PreferenceTweakScreen(navController)
                        }
                    }
                    composable("aboutscreen") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            AboutScreen(navController)
                        }
                    }
                    composable("fpsgoscreen") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            FpsGoSettings(navController)
                        }
                    }
                    composable("governorsettings") {
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            GovSettings(navController)
                        }
                    }
                    composable(
                        route = "app_settings/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pkg = backStackEntry.arguments?.getString("pkg")
                        zx.azenith.ui.component.ScreenWrapper(navController = navController, animatedVisibilityScope = this) {
                            AppSettingsScreen(navController, pkg)
                        }
                    }
                }
                
                // Haze captures its sources on the frame after the blurred layer's bounds are
                // known, so a navbar that slides in on frame zero reaches the user before there
                // is anything to blur -- it reads as a floating transparent pill. Hold it off
                // screen until two frames have passed and the source capture has settled.
                var navBarRevealGateOpen by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    withFrameNanos { }
                    delay(160)
                    navBarRevealGateOpen = true
                }

                val shouldShowNavBar = rawRoute in bottomBarRoutes
                val navBarVisibilityProgressState = androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (shouldShowNavBar && navBarRevealGateOpen) 1f else 0f,
                    // M3's own spatial spec, so the navbar arrives with the same spring the
                    // rest of the expressive motion uses rather than a hand-picked one.
                    animationSpec = androidx.compose.material3.MotionScheme.Companion.expressive()
                        .defaultSpatialSpec(),
                    label = "NavBarVisibilityProgress"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        // Slide only, never alpha: a translucent layer is skipped by the haze
                        // source capture, so animating opacity silently drops the blur.
                        .graphicsLayer {
                            translationY = size.height * (1f - navBarVisibilityProgressState.value)
                        }
                ) {
                    BottomNavBar(
                        items = navItems,
                        selectedRoute = currentRoute ?: "home",
                        pagerState = pagerState,
                        isBlurEnabled = isBlurEnabled,
                        hazeState = hazeState,
                        navStyle = currentPersonalization().navStyle,
                        navShape = currentPersonalization().navRadius,
                                        navLabels = currentPersonalization().navLabels,
                        vibrantNav = currentPersonalization().vibrantNav,
                        navScale = currentPersonalization().navScale,
                        navSpacing = currentPersonalization().navSpacing,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onItemSelected = { route ->
                            val targetIndex = pagerRoutes.indexOf(route)
                            if (isOnMainPager) {
                                if (pagerState.currentPage != targetIndex) {
                                    coroutineScope.launch {
                                        // Instrumentation only -- these traces are
                                        // read with `adb shell atrace` / Perfetto on a
                                        // device, so no timing is asserted here.
                                        // The section brackets the whole scroll so its
                                        // duration is the tab-switch cost.
                                        Trace.beginSection("AZenith:tabScrollTo")
                                        try {
                                            // One call for every distance. A jump of two
                                            // or three scrolls straight through the
                                            // pages in between at the same rate, which
                                            // reads as one continuous slide instead of
                                            // the hard cut scrollToPage produced.
                                            pagerState.animateScrollToPage(
                                                targetIndex,
                                                animationSpec = androidx.compose.animation.core.tween(
                                                    durationMillis = if (Math.abs(targetIndex - pagerState.currentPage) > 1) 320 else 500,
                                                    easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
                                                )
                                            )
                                        } finally {
                                            Trace.endSection()
                                        }
                                    }
                                }
                            } else {
                                navController.navigate("main") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetIndex)
                                }
                            }
                            highlightRoute.value = route
                        }
                    )
                }
                
                val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                if (navBarHeight > 32.dp) {
                    val colorScheme = MaterialTheme.colorScheme
                    val bottomScrimGradient = remember(colorScheme) {
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.1f to colorScheme.surface.copy(alpha = 0.3f),
                            0.2f to colorScheme.surface.copy(alpha = 0.4f),
                            0.3f to colorScheme.surface.copy(alpha = 0.5f),
                            0.4f to colorScheme.surface.copy(alpha = 0.7f),
                            0.5f to colorScheme.surface.copy(alpha = 0.8f),
                            0.6f to colorScheme.surface.copy(alpha = 0.9f),
                            1.0f to colorScheme.surface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(navBarHeight + 12.dp)
                            .align(Alignment.BottomCenter)
                            .background(bottomScrimGradient)
                    )
                }

                AnimatedVisibility(
                    visible = rootStatus && moduleInstalled && pendingReboot && rawRoute in bottomBarRoutes && isFabVisible.value,
                    enter = scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = scaleOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 116.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            rebootDialog.showConfirm(
                                title = context.getString(R.string.dialog_reboot_required_title),
                                content = context.getString(R.string.dialog_reboot_required_content),
                                confirm = context.getString(R.string.reboot),
                                dismiss = context.getString(R.string.dialog_update_available_dismiss)
                            )
                        },
                        icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.reboot)) },
                        text = { Text(stringResource(R.string.reboot), fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    )
                }
            }
            ConfirmDialogHost(handle = updateDialog)
            ConfirmDialogHost(handle = rebootDialog)
            InstallingDialogHost(handle = installingDialog)
            
            zx.azenith.ui.component.ExitPopup(
                visible = showExitConfirm,
                onDismiss = { showExitConfirm = false },
                onConfirm = { (context as? android.app.Activity)?.finishAffinity() }
            )
        }
    }
}

/**
 * The one list of tabs. Public so the Personalization preview can hand the real
 * [BottomNavBar] the same items instead of keeping a parallel copy that drifts.
 */
val AZENITH_NAV_ITEMS = listOf(
    NavItem("home", R.string.nav_home, Icons.Rounded.Home),
    NavItem("applist", R.string.nav_applist, Icons.Rounded.Widgets),
    NavItem("tweaks", R.string.nav_tweaks, Icons.Rounded.SettingsInputComponent),
    NavItem("settings", R.string.nav_settings, Icons.Rounded.Settings)
)

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    pagerState: PagerState? = null,
    navStyle: NavStyle = NavStyle.Floating,
    vibrantNav: Boolean = false,
    navScale: Float = 1f,
    navSpacing: Float = 1f,
    navShape: Float = 0.5f,
    navLabels: NavLabelMode = NavLabelMode.SelectedOnly,
    // Carried as data so the landscape rail is an added enum value rather than a
    // second host composable; only Bottom is laid out today.
    navEdge: NavEdge = NavEdge.Bottom,
    // The Personalization preview draws this bar inside a card the user taps to
    // expand, so a live tab eats the tap meant for the card and the mock cannot
    // be zoomed. Off there: the pill keeps its look and drops its click handling.
    interactive: Boolean = true
) {
    // One Animatable for the whole style switch, so pinned and floating
    // interpolate together instead of each value snapping on its own clock and
    // the bar tearing through the intermediate widths. An Animatable rather than
    // rememberTransition: the wildcard animation.core import shadows
    // Transition.animateFloat with InfiniteTransition's overload, which takes no
    // target lambda.
    val pinnedAnim = remember { Animatable(if (navStyle == NavStyle.Pinned) 1f else 0f) }
    LaunchedEffect(navStyle) {
        pinnedAnim.animateTo(
            targetValue = if (navStyle == NavStyle.Pinned) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    // Clamped once, here: the spring overshoots past 1f by design, and every
    // dimension below multiplies this, so an out-of-range value becomes a
    // negative padding and throws rather than a slightly-too-large bar.
    val pinnedProgress = pinnedAnim.value.coerceIn(0f, 1f)
    // The boolean stays for the decisions that are genuinely binary -- which
    // surface colour, whether a shadow is right -- while every dimension above
    // interpolates on pinnedProgress.
    val pinned = navStyle == NavStyle.Pinned
    // Whether a real blur is drawn. A transparent surface is only correct when
    // there is a haze source to sample behind it; without one the bar would be
    // an invisible hole, which is why the mock has to opt in differently.
    val blurring = isBlurEnabled && hazeState != null

    // Read from the configuration rather than a constant: the bar has to keep
    // its margin on a narrow screen too, where a hard-coded cap would overflow.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val floatingMaxWidth =
        (screenWidth - NAV_FLOATING_MIN_MARGIN * navScale * navSpacing * 2)

    // The vibrant treatment tints the bar, the unselected pill and the blur from
    // the accent. Off, the bar is the stock neutral surface, so it is the one
    // place the accent never reaches.
    val accentBar = vibrantNav

    // Measured label widths, so the selected pill can interpolate its
    // width open and closed with the swipe instead of switching between
    // "icon only" and "icon + label" layouts. Measuring once per
    // composition keeps the per-frame path free of text layout.
    val labelMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val labelWidths = items.map { item ->
        with(density) {
            labelMeasurer.measure(
                text = AnnotatedString(stringResource(item.labelRes)),
                style = labelStyle
            ).size.width.toDp()
        }
    }

    // Percent-based so Full stays a capsule at any bar height; a fixed dp radius
    // cannot, because the height differs between Pinned and the other two.
    // A pinned bar is flush with the window edges, so the corner choice cannot
    // apply to it; only the floating bar reads navShape.
    // Pinned used to force RectangleShape, which is why it had no corner dial.
    // It reads navShape like the floating bar does; the default (0) stays sharp.
    val barRadius = RoundedCornerShape(percent = (navShape * 100).toInt())

    // Pinned draws its own surface down past the gesture-bar inset so the
    // background reaches the bottom edge; a floating bar instead *keeps* the
    // inset as padding, which is what lifts it clear of the screen edge.
    // Animated so the bar sinks into the inset as it pins rather than jumping.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // The inset as padding, straight through: full when floating so the bar
    // clears the gesture area, none when pinned so its surface reaches the edge.
    // Padded directly rather than via windowInsetsPadding, which would apply the
    // inset a second time and leave the bar too high in both states.
    val insetPadding = Modifier.padding(bottom = navInset * (1f - pinnedProgress))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(insetPadding)
            // Pinned is edge to edge; the other two keep the floating margin,
            // which lives on the row inside the surface rather than here.
            // No horizontal padding on this Box: the surface hugs its own content, so
            // padding on this Box is dead space that just narrows the bar. The
            // margin lives on the row inside the surface instead, which is what
            // keeps it equal on both sides.
            .padding(vertical = NAV_FLOATING_OUTER_PAD * navScale * (1f - pinnedProgress)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                // A stacked bar needs the full width or its labels crowd. An
                // inline one hugs its tabs and keeps [NAV_FLOATING_MIN_MARGIN]
                // as row padding inside, so the margin is the same on both sides
                // whatever the selected label measures. widthIn is what keeps the
                // hug honest: on a long translation the content would otherwise
                // run to the window edges.
                // Width stays a boolean. A fraction cannot blend "hug the tabs"
                // into "fill the window": fillMaxWidth(0f) is zero wide, not
                // wrap-content, so the bar disappears rather than shrinking. The
                // style change is carried by the padding, gap, weight and pill
                // values around it, which all interpolate.
                .then(if (pinned) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                .widthIn(max = if (pinned) Dp.Unspecified else floatingMaxWidth)
                .clip(barRadius)
                .then(if (blurring) Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = HazeBlurStyle.Material3(
                                    containerColor = if (accentBar) {
                                        MaterialTheme.colorScheme.primaryContainer
                                            .darken(NAV_BAR_DARKEN).copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
                                    }
                                ) { blurRadius(24.dp) }
                            ) else Modifier),
            shape = barRadius,
            // Tinted off the accent rather than the surface: an OEM theme can
            // resolve surfaceContainer to plain grey or black, which leaves the
            // bar reading as a hole in the page. primaryContainer is already the
            // accent at container strength, so darkening it a little gives a
            // colour that belongs to the theme without competing with the pill.
            color = when {
                blurring -> Color.Transparent
                accentBar -> MaterialTheme.colorScheme.primaryContainer.darken(NAV_BAR_DARKEN)
                pinned -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            // A pinned bar sits flush with the window rather than floating over the
            // page, so it needs no drop shadow; the others keep theirs to lift off.
            shadowElevation = if (isBlurEnabled || pinned) 0.dp else 8.dp
        ) {
            // One list of items, built once and placed by [navEdge]. Bottom lays
            // them out in a row under the finger; Start/End stack the identical
            // pills down a vertical rail for landscape.
            val navItemsContent: @Composable (Modifier) -> Unit = { pillModifier ->
                items.forEachIndexed { index, item ->
                    // A continuous 0..1 "how selected is this tab" value rather
                    // than a boolean, so a drag interpolates instead of
                    // snapping when the selection flips. The pager offset is a
                    // frequently-changing value, so it is confined to this
                    // derivedStateOf: only the four pills below re-read it.
                    val progress by remember(index) {
                        pagerState?.let { state ->
                            derivedStateOf {
                                val distance = (state.currentPage - index) + state.currentPageOffsetFraction
                                1f - kotlin.math.abs(distance).coerceIn(0f, 1f)
                            }
                        } ?: derivedStateOf { if (selectedRoute == item.route) 1f else 0f }
                    }
                    NavPill(
                        item = item,
                        selectionProgress = progress,
                        labelWidth = labelWidths[index] + 5.dp,
                        isBlurEnabled = isBlurEnabled,
                        labelMode = navLabels,
                        pinnedProgress = pinnedProgress,
                        accentBar = accentBar,
                        sizeScale = navScale,
                        interactive = interactive,
                        modifier = pillModifier,
                        // While the pager is being dragged the target changes
                        // every frame, so the tween would restart on each one
                        // and never reach its end value. Snap to the drag instead
                        // and let the pager's own curve provide the motion; taps
                        // and other discrete changes still get the tween.
                        animationSpec = if (pagerState?.isScrollInProgress == true) snap() else NAV_PILL_SPEC,
                        onClick = { onItemSelected(item.route) }
                    )
                }
            }

            when (navEdge) {
                NavEdge.Bottom -> Row(
                    modifier = Modifier
                        // Matches the surface: the inline bar shrinks with its
                        // content, the others divide the full width. The floating
                        // stacked bar takes a width proportional to the slider, so
                        // the gap between tabs shrinks with the icons instead of
                        // each one keeping an equal share of the screen.
                        .then(if (pinned) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                        .padding(
                            // Scaled so a smaller bar is tighter on screen too;
                            // a fixed margin kept the same gap at every size,
                            // which left the bar looking barely resized. Both axes
                            // interpolate with the style so the row does not jump
                            // as the bar pins.
                            horizontal = NAV_FLOATING_MIN_MARGIN * navScale * navSpacing *
                                (1f - pinnedProgress),
                            vertical = NAV_BAR_VERTICAL_PAD * navScale * (1f - pinnedProgress)
                        )
                        // Extend the pinned row past the gesture bar so the surface
                        // fills to the bottom edge instead of stopping above it.
                        .padding(bottom = navInset * pinnedProgress),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (navLabels == NavLabelMode.Never) 0.dp
                        else NAV_PILL_GAP * navScale * navSpacing * (1f - pinnedProgress),
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pinned divides the screen evenly. Floating sizes every tab to
                    // its own label, so the row hugs and the slider shrinks the
                    // icons, the gaps and the margins together. Weighting them
                    // would divide the screen instead and leave the gaps fixed.
                    // Pinned divides the screen evenly and floating lets each tab
                    // take only its own width. The weight animates between them so
                    // the tabs redistribute as the bar changes shape. weight(0f) is
                    // illegal, so the floating end takes no weight at all.
                    navItemsContent(
                        if (pinnedProgress > 0.01f) Modifier.weight(pinnedProgress) else Modifier
                    )
                }

                // Landscape rail. Not reachable from the UI yet; laid out here so
                // adding the preference is a one-line change at the call site.
                NavEdge.Start, NavEdge.End -> Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(
                            horizontal = if (pinned) 0.dp else 12.dp,
                            vertical = if (pinned) 0.dp else 10.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (pinned) 0.dp else 12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { navItemsContent(Modifier) }
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    selectionProgress: Float,
    labelWidth: Dp,
    isBlurEnabled: Boolean = false,
    labelMode: NavLabelMode = NavLabelMode.SelectedOnly,
    // A fraction, not a flag: the pill's height, label behaviour and colours all
    // differ between pinned and floating and have to interpolate together.
    pinnedProgress: Float = 0f,
    accentBar: Boolean = false,
    sizeScale: Float = 1f,
    interactive: Boolean = true,
    animationSpec: FiniteAnimationSpec<Color> = NAV_PILL_SPEC,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress = selectionProgress.coerceIn(0f, 1f)
    val isPinned = pinnedProgress > 0.5f
    // Boolean only for the colour targets; the width below is continuous.
    val isSelected = progress > 0.5f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    // Colours and the label are driven by the continuous progress value rather
    // than by the selected boolean, so during a drag they interpolate. The
    // boolean form only had two states, which is what made the bar appear to
    // freeze mid-swipe and then jump when the selection finally flipped.
    // Interpolated, so a style switch slides the label in or out instead of it
    // appearing. Always and SelectedOnly keep their existing meaning at each end.
    val stackedAmount = if (labelMode == NavLabelMode.Always) 1f else pinnedProgress.coerceIn(0f, 1f)
    val stacked = stackedAmount > 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    // Only the vibrant preset recolours the unselected tabs. The stock preset
    // keeps onSurfaceVariant, which is what the bar has always used.
    val onTile = if (accentBar) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    // The accent's own value, not a darker version of it. The bar and the tile
    // are the same hue, so all their separation has to come from value; darkening
    // both by different amounts left them about one sRGB level apart, which is
    // why the tile disappeared into the bar. Light mode keeps the neutral tile so
    // the bar and pill agree with each other.
    val unselectedBg = if (accentBar) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    // A second spring on the selection itself: as the highlight arrives the pill
    // settles past its target and eases back, which is what gives the tab switch its
    // M3 bounce. The press spring above is separate so the two do not fight.
    val selectionScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPinned) 1f else 1f + 0.10f * progress,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "selectionScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            // The classic bar has no pill at all: selection is carried by the
            // icon tint alone, which is what made the old bar read as a navbar.
            // Stacked shows every label at once, so a pill per tab highlighted
            // the whole bar. The selected tab is marked on its icon instead and
            // its label only changes colour. Pinned keeps no pill at all --
            // selection there is carried by the icon tint alone.
            isPinned || stacked -> Color.Transparent
            // Under blur the bar is only 40% opaque, so an opaque tile would sit
            // on it like a sticker; the alpha keeps both translucent and the
            // accent hue still separates the tile from the surface behind it.
            isBlurEnabled -> if (isSelected) primary.copy(alpha = 0.25f) else unselectedBg.copy(alpha = 0.55f)
            else -> if (isSelected) primary else unselectedBg
        },
        animationSpec = animationSpec,
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isPinned && isSelected -> primary
            isPinned -> onTile
            isSelected -> if (isBlurEnabled) primary else onPrimary
            else -> onTile
        },
        animationSpec = animationSpec,
        label = "contentColor"
    )

    // The label is never on the disc, so it must not take the icon's onPrimary.
    // The selected label takes the accent itself: the icon's disc is already
    // that colour, so a label in the same value would sit on the bar at the same
    // strength as the icon it is labelling.
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) primary else onTile,
        animationSpec = animationSpec,
        label = "labelColor"
    )

    // The stacked mode's only highlight: a disc behind the selected icon.
    val stackedIconBg by animateColorAsState(
        targetValue = when {
            isPinned -> Color.Transparent
            isSelected -> if (isBlurEnabled) primary.copy(alpha = 0.25f) else primary
            else -> Color.Transparent
        },
        animationSpec = animationSpec,
        label = "stackedIconBg"
    )

    // A 48 dp tall pill with a 24 dp radius is already a circle, so the old
    // CircleShape/RoundedCornerShape(24.dp) switch had no visual effect to
    // interpolate. Kept as a plain rounded shape.
    val shape = RoundedCornerShape(24.dp)

    // Never and SelectedOnly share the inline pill: the label opens beside the
    // icon on whichever tab is selected. Always stacks instead -- an expanded
    // label on all four tabs at once is what overflowed the bar.
    val labelFraction = when (labelMode) {
        NavLabelMode.Always -> 1f
        NavLabelMode.SelectedOnly -> progress
        NavLabelMode.Never -> 0f
    }

    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size((if (stacked) 22.dp else 24.dp) * sizeScale)
        )
    }

    // The label is always composed and its extent is interpolated with the
    // swipe rather than switched in once the selection flips, so a drag opens
    // and closes it instead of popping.
    // Two shapes, and the choice between them is the label mode, not the style:
    // a stacked bar always stacks, and which style it is only changes its height.
    if (stacked) {
        Column(
            modifier = modifier
                .scale(scale * selectionScale)
                .height((56.dp + 8.dp * stackedAmount) * sizeScale)
                .defaultMinSize(minWidth = 48.dp * sizeScale)
                .clip(shape)
                .background(bgColor)
                .then(
                    if (interactive) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ) else Modifier
                )
                .padding(horizontal = NAV_PILL_PAD_X * sizeScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(stackedIconBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { icon() }
            // fillMaxWidth rather than a fixed measured width: a Text sized to
            // labelWidth started at the left edge of that box, so it read as
            // offset from the icon instead of centred under it.
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // wrapContentWidth, not fillMaxWidth: the column sizes to its
                    // own content now, so filling the parent width would either
                    // collapse to zero (inside a wrap-content row) or push the
                    // label off its own icon.
                    .wrapContentWidth()
                    .padding(top = 3.dp)
                    .alpha(labelFraction * stackedAmount)
            )
        }
    } else {
        Row(
            modifier = modifier
                .scale(scale * selectionScale)
                .height((if (isPinned) 56.dp else 48.dp) * sizeScale)
                .defaultMinSize(minWidth = 48.dp * sizeScale)
                .clip(shape)
                .background(bgColor)
                .then(
                    if (interactive) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ) else Modifier
                )
                .padding(horizontal = NAV_PILL_PAD_INLINE_X * sizeScale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Box(
                modifier = Modifier
                    .width(labelWidth * labelFraction)
                    .clipToBounds()
            ) {
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .alpha(labelFraction)
                )
            }
        }
    }
}

/**
 * Ceiling on one stacked label, so a long translation cannot push a tab past
 * its quarter of the bar. A quarter plus slack: four tabs share the width
 * evenly.
 */
private val NAV_STACKED_MAX_LABEL = 96.dp

/**
 * Bounds on the inline floating bar's width, which follows the selected label.
 *
 * The minimum is what keeps a short label clear of the window edge; the
 * maximum is derived from the screen at the call site so a long translation is
 * capped instead of running off both sides.
 */
// Sized so the longest shipped label still fits at the floor: 3 icon-only
// tabs at 48dp + gaps + this pill's own chrome leaves 60dp of label room,
// which clears "Settings" in English. A tighter floor clipped it to an ellipsis.
/**
 * Darkens [amount] toward black in linear light, so a mid-tone accent shifts
 * perceptually even instead of the muddy result a straight RGB multiply gives.
 * Blending in sRGB crushes the channels unevenly and desaturates as it goes.
 */
private fun Color.darken(amount: Float): Color {
    fun channel(c: Float): Float =
        (1f - amount) * c + amount * (c * c * (c * 0.3053f + 0.6822f) + c * 0.0123f)
    return Color(
        red = channel(red),
        green = channel(green),
        blue = channel(blue),
        alpha = alpha
    )
}

/**
 * How far the accent is darkened for the bar surface. The bar has to stay near
 * the page it floats over, so most of the separation from a tile comes from
 * leaving the tile at the accent's own value rather than from pushing the bar
 * further down.
 */
private const val NAV_BAR_DARKEN = 0.72f

/** Screen margin a floating bar keeps either side, whatever its content. */
private val NAV_FLOATING_MIN_MARGIN = 14.dp
/** Gap between two pills. */
private val NAV_PILL_GAP = 6.dp

/** Vertical padding the floating bar adds above and below its row. */
private val NAV_BAR_VERTICAL_PAD = 10.dp

/** Vertical padding the floating bar's container adds outside the surface. */
private val NAV_FLOATING_OUTER_PAD = 20.dp

/** Horizontal padding inside a stacked pill. */
private val NAV_PILL_PAD_X = 4.dp

/** Horizontal padding inside an inline pill, wide enough for the open label. */
private val NAV_PILL_PAD_INLINE_X = 12.dp
