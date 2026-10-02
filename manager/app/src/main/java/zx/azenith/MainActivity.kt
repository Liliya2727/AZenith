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
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
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
private val NAV_PILL_SPEC: FiniteAnimationSpec<Color> = tween(300, easing = FastOutSlowInEasing)



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

    val navItems = remember {
        listOf(
            NavItem("home", R.string.nav_home, Icons.Rounded.Home),
            NavItem("applist", R.string.nav_applist, Icons.Rounded.Widgets),
            NavItem("tweaks", R.string.nav_tweaks, Icons.Rounded.SettingsInputComponent),
            NavItem("settings", R.string.nav_settings, Icons.Rounded.Settings)
        )
    }
    
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
        zx.azenith.ui.component.LocalActiveDialogCount provides activeDialogCount
    ) {
        RootDialogsProvider {
            val isAnyDialogOpen = zx.azenith.ui.component.LocalActiveDialogCount.current.value > 0 || installingDialog.isShown || updateDialog.isShown || rebootDialog.isShown
            
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val currentOnBack by androidx.compose.runtime.rememberUpdatedState {
                    if (showExitConfirm) {
                        showExitConfirm = false
                    } else if (pagerState.currentPage != 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    } else {
                        showExitConfirm = true
                    }
                }
                val backCallback = androidx.compose.runtime.remember {
                    android.window.OnBackInvokedCallback {
                        currentOnBack()
                    }
                }
                androidx.compose.runtime.DisposableEffect(isOnMainPager, isAnyDialogOpen, context) {
                    var currentContext = context
                    var activity: android.app.Activity? = null
                    while (currentContext is android.content.ContextWrapper) {
                        if (currentContext is android.app.Activity) {
                            activity = currentContext
                            break
                        }
                        currentContext = currentContext.baseContext
                    }
                    val dispatcher = activity?.onBackInvokedDispatcher
                    if (isOnMainPager && !isAnyDialogOpen && dispatcher != null) {
                        dispatcher.registerOnBackInvokedCallback(
                            android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                            backCallback
                        )
                    }
                    onDispose {
                        dispatcher?.unregisterOnBackInvokedCallback(backCallback)
                    }
                }
            } else {
                androidx.activity.compose.BackHandler(enabled = isOnMainPager && !isAnyDialogOpen) {
                    if (showExitConfirm) {
                        showExitConfirm = false
                    } else if (pagerState.currentPage != 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    } else {
                        showExitConfirm = true
                    }
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
                    enterTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeIn(animationSpec = tween(700))
                        } else if (targetState.destination.route !in bottomBarRoutes) {
                            // Forward axis. MD3 moves the incoming screen
                            // emphasis-decelerate (400ms) and the outgoing one
                            // emphasis-accelerate (200ms) so the pair reads as
                            // one movement rather than two independent slides.
                            // A spring on the incoming offset leaves the
                            // outgoing screen with nothing to hand off to.
                            // The offset runs on the plain emphasized curve at
                            // 500ms rather than emphasized-decelerate: that
                            // curve rises almost vertically, so most of the
                            // travel lands in the first ~100ms and the screen
                            // reads as an instant cut that then settles. The
                            // fade stays on decelerate, where the fast start is
                            // what makes the incoming surface feel lit.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(500, easing = Emphasized)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        } else {
                            // Subscreen <-> subscreen: the same shared axis X as the
                            // root -> subscreen case. It used to be a bare fade, which
                            // is what made opening a submenu feel instant -- there was
                            // no positional movement to read as travel.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(400, easing = EmphasizedDecelerate)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        }
                    },
                    exitTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeOut(animationSpec = tween(700))
                        } else if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                            // The outgoing page has to travel with the incoming one
                            // or the pair reads as a new screen sliding over a
                            // static one. MD3 fade-through keeps the distance
                            // small and the exit short; it uses -1/8 at 90ms
                            // precisely because the incoming screen does the
                            // travelling. The incoming screen here moves far
                            // enough to need a real handoff, so the outgoing one
                            // is pushed to -1/3 over the emphasized curve and is
                            // NOT faded -- a fade would blank it out well before
                            // the incoming one has covered it, leaving a bare
                            // background visible for the rest of the transition.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 3) },
                                animationSpec = tween(500, easing = Emphasized)
                            )
                        } else {
                            // Subscreen <-> subscreen or root <-> subscreen (backwards): use
                            // M3 shared axis X, but give the outgoing content a short
                            // fade so the transition reads smooth rather than abrupt.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 6) },
                                animationSpec = tween(200, easing = EmphasizedAccelerate)
                            ) + fadeOut(animationSpec = tween(200, easing = EmphasizedAccelerate))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeIn(animationSpec = tween(700))
                        } else if (targetState.destination.route !in bottomBarRoutes) {
                            // Forward axis. MD3 moves the incoming screen
                            // emphasis-decelerate (400ms) and the outgoing one
                            // emphasis-accelerate (200ms) so the pair reads as
                            // one movement rather than two independent slides.
                            // A spring on the incoming offset leaves the
                            // outgoing screen with nothing to hand off to.
                            // The offset runs on the plain emphasized curve at
                            // 500ms rather than emphasized-decelerate: that
                            // curve rises almost vertically, so most of the
                            // travel lands in the first ~100ms and the screen
                            // reads as an instant cut that then settles. The
                            // fade stays on decelerate, where the fast start is
                            // what makes the incoming surface feel lit.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(500, easing = Emphasized)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        } else {
                            // Subscreen <-> subscreen: the same shared axis X as the
                            // root -> subscreen case. It used to be a bare fade, which
                            // is what made opening a submenu feel instant -- there was
                            // no positional movement to read as travel.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(400, easing = EmphasizedDecelerate)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        }
                    },
                    popExitTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeOut(animationSpec = tween(700))
                        } else if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                            // The outgoing page has to travel with the incoming one
                            // or the pair reads as a new screen sliding over a
                            // static one. MD3 fade-through keeps the distance
                            // small and the exit short; it uses -1/8 at 90ms
                            // precisely because the incoming screen does the
                            // travelling. The incoming screen here moves far
                            // enough to need a real handoff, so the outgoing one
                            // is pushed to -1/3 over the emphasized curve and is
                            // NOT faded -- a fade would blank it out well before
                            // the incoming one has covered it, leaving a bare
                            // background visible for the rest of the transition.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 3) },
                                animationSpec = tween(500, easing = Emphasized)
                            )
                        } else {
                            // Subscreen <-> subscreen or root <-> subscreen (backwards): use
                            // M3 shared axis X, but give the outgoing content a short
                            // fade so the transition reads smooth rather than abrupt.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 6) },
                                animationSpec = tween(200, easing = EmphasizedAccelerate)
                            ) + fadeOut(animationSpec = tween(200, easing = EmphasizedAccelerate))
                        }
                    },
                    predictivePopEnterTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeIn(animationSpec = tween(700))
                        } else if (targetState.destination.route !in bottomBarRoutes) {
                            // Forward axis. MD3 moves the incoming screen
                            // emphasis-decelerate (400ms) and the outgoing one
                            // emphasis-accelerate (200ms) so the pair reads as
                            // one movement rather than two independent slides.
                            // A spring on the incoming offset leaves the
                            // outgoing screen with nothing to hand off to.
                            // The offset runs on the plain emphasized curve at
                            // 500ms rather than emphasized-decelerate: that
                            // curve rises almost vertically, so most of the
                            // travel lands in the first ~100ms and the screen
                            // reads as an instant cut that then settles. The
                            // fade stays on decelerate, where the fast start is
                            // what makes the incoming surface feel lit.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(500, easing = Emphasized)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        } else {
                            // Subscreen <-> subscreen: the same shared axis X as the
                            // root -> subscreen case. It used to be a bare fade, which
                            // is what made opening a submenu feel instant -- there was
                            // no positional movement to read as travel.
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 3 },
                                animationSpec = tween(400, easing = EmphasizedDecelerate)
                            ) + fadeIn(animationSpec = tween(400, easing = EmphasizedDecelerate))
                        }
                    },
                    predictivePopExitTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeOut(animationSpec = tween(700))
                        } else if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                            // The outgoing page has to travel with the incoming one
                            // or the pair reads as a new screen sliding over a
                            // static one. MD3 fade-through keeps the distance
                            // small and the exit short; it uses -1/8 at 90ms
                            // precisely because the incoming screen does the
                            // travelling. The incoming screen here moves far
                            // enough to need a real handoff, so the outgoing one
                            // is pushed to -1/3 over the emphasized curve and is
                            // NOT faded -- a fade would blank it out well before
                            // the incoming one has covered it, leaving a bare
                            // background visible for the rest of the transition.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 3) },
                                animationSpec = tween(500, easing = Emphasized)
                            )
                        } else {
                            // Subscreen <-> subscreen or root <-> subscreen (backwards): use
                            // M3 shared axis X, but give the outgoing content a short
                            // fade so the transition reads smooth rather than abrupt.
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth: Int -> -(fullWidth / 6) },
                                animationSpec = tween(200, easing = EmphasizedAccelerate)
                            ) + fadeOut(animationSpec = tween(200, easing = EmphasizedAccelerate))
                        }
                    }
                ) {
                    composable("get_started") {
                        // get_started writes has_completed_get_started itself and
                        // then calls back. It used to relaunch the activity through
                        // "am start -S", which force-stops the process first, so
                        // finishing setup restarted the app from cold instead of
                        // moving on to "main".
                        GetStartedScreen(navController) {
                            navController.navigate("main") {
                                popUpTo("get_started") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    
                    // Route Pager (Kode 2)
                    composable("main") {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isBlurEnabled) Modifier.hazeSource(state = hazeState) else Modifier
                                )
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
                } // ends composable

                    // Subscreens
                    composable("color_palette") { ColorPaletteScreen(navController) }
                    composable("colorscheme") { ColorSchemeSettings(navController) }
                    composable("FasScreen") { FasScreen(navController) }
                    composable("bypasschg") { BypassChargeScreen(navController) }
                    composable("bypasschg_check") { BypassChargeCheckScreen(navController) }
                    composable("preferenced") { PreferenceTweakScreen(navController) }
                    composable("aboutscreen") { AboutScreen(navController) }
                    composable("fpsgoscreen") { FpsGoSettings(navController) }
                    composable("governorsettings") { GovSettings(navController) }
                    composable(
                        route = "app_settings/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pkg = backStackEntry.arguments?.getString("pkg")
                        AppSettingsScreen(navController, pkg)
                    }
                }
                
                AnimatedVisibility(
                    // The bar is chrome, not a root-dependent surface, so it is
                    // shown from the first frame. It used to wait on
                    // rootStatus && moduleInstalled, which are only set after
                    // requestRootAccess() returns -- on a cold start that is
                    // however long the su prompt takes, so the app opened to a
                    // bare background with no bar and no top bar. Each item
                    // disables itself instead of the whole bar disappearing.
                    visible = rawRoute in bottomBarRoutes,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavBar(
                        items = navItems,
                        selectedRoute = currentRoute ?: "home",
                        pagerState = pagerState,
                        isBlurEnabled = isBlurEnabled,
                        hazeState = hazeState,
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

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: Boolean = false,
    hazeState: HazeState? = null,
    pagerState: PagerState? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 26.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 350.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)) 
                .then(
                    if (isBlurEnabled && hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurEffect {
                                blurRadius = 24.dp
                            }
                        }
                    } else Modifier
                ),
            shape = RoundedCornerShape(28.dp),
            color = if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = if (isBlurEnabled) 0.dp else 8.dp
        ) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    selectionProgress: Float,
    labelWidth: Dp,
    isBlurEnabled: Boolean = false,
    animationSpec: FiniteAnimationSpec<Color> = NAV_PILL_SPEC,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress = selectionProgress.coerceIn(0f, 1f)
    // Boolean only for the colour targets; the width below is continuous.
    val isSelected = progress > 0.5f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    // Colours and the label are driven by the continuous progress value rather
    // than by the selected boolean, so during a drag they interpolate. The
    // boolean form only had two states, which is what made the bar appear to
    // freeze mid-swipe and then jump when the selection finally flipped.
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val unselectedBg = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    val bgColor by animateColorAsState(
        targetValue = if (isBlurEnabled) {
            if (isSelected) primary.copy(alpha = 0.25f) else unselectedBg
        } else {
            if (isSelected) primary else unselectedBg
        },
        animationSpec = animationSpec,
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isBlurEnabled) primary else onPrimary
        } else {
            onSurfaceVariant
        },
        animationSpec = animationSpec,
        label = "contentColor"
    )

    // A 48 dp tall pill with a 24 dp radius is already a circle, so the old
    // CircleShape/RoundedCornerShape(24.dp) switch had no visual effect to
    // interpolate. Kept as a plain rounded shape.
    val shape = RoundedCornerShape(24.dp)
    
    Row(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        // The label is always composed and its width is interpolated with the
        // swipe, instead of AnimatedVisibility switching it on only once the
        // selection flipped. Clipping to the scaled width (and zero width when
        // unselected) prevents layout changes from stretching icons.
        Box(
            modifier = Modifier
                .width(labelWidth * progress)
                .clipToBounds()
        ) {
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .alpha(progress)
            )
        }
    }
}
