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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
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
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
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

data class NavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val gradientColors: List<Color> = listOf(Color.Transparent, Color.Transparent)
)

/**
 * Extension function for smooth scrolling pager
 */
suspend fun PagerState.smoothScrollToPage(
    targetPage: Int,
    perPageDurationMs: Int = 220,
    maxDurationMs: Int = 650
) {
    val distance = targetPage - currentPage
    if (distance == 0 && currentPageOffsetFraction == 0f) return

    val pageSizePx = (layoutInfo.pageSize + layoutInfo.pageSpacing).toFloat()
    if (pageSizePx <= 0f) {
        animateScrollToPage(targetPage)
        return
    }

    val totalOffsetPx = (distance - currentPageOffsetFraction) * pageSizePx
    val duration = (perPageDurationMs * abs(distance)).coerceIn(perPageDurationMs, maxDurationMs)

    var previous = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        Animatable(0f).animateTo(
            targetValue = totalOffsetPx,
            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
        ) {
            scrollBy(value - previous)
            previous = value
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(fromTileType: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // STATE: Apakah scroll animation nyala atau nggak? (Default false)
    var useScrollAnimation by remember { mutableStateOf(settingsPrefs.getBoolean("use_scroll_animation", false)) }

    val pagerRoutes = remember { listOf("home", "applist", "tweaks", "settings") }
    val pagerState = rememberPagerState(initialPage = 0) { pagerRoutes.size }
    
    // Bottom bar routes dinamis tergantung setting
    val bottomBarRoutes = remember(useScrollAnimation) {
        if (useScrollAnimation) setOf("main") 
        else setOf("home", "applist", "tweaks", "settings")
    }

    LaunchedEffect(fromTileType, useScrollAnimation) {
        val rootNav = if (useScrollAnimation) "main" else "home"
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

    var pendingReboot by remember { mutableStateOf(false) }
        
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rawRoute = navBackStackEntry?.destination?.route
    val isOnMainPager = rawRoute == "main"
    
    // Evaluasi current route untuk highlight di BottomNavBar
    val currentRoute = if (useScrollAnimation && isOnMainPager) {
        pagerRoutes[pagerState.currentPage]
    } else {
        rawRoute
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

    val refreshStatus = {
        rootStatus = RootUtils.requestRootAccess()
        moduleInstalled = RootUtils.isModuleInstalled()
        isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
        useScrollAnimation = settingsPrefs.getBoolean("use_scroll_animation", false) // Update status animasi
        pendingReboot = Shell.cmd("test -f /data/adb/modules/AZenith/reboot").exec().isSuccess
    }

    LaunchedEffect(rawRoute, pagerState.currentPage) {
        refreshStatus()
    }

    val navItems = remember {
        listOf(
            NavItem("home", R.string.nav_home, Icons.Rounded.Home),
            NavItem("applist", R.string.nav_applist, Icons.Rounded.Widgets),
            NavItem("tweaks", R.string.nav_tweaks, Icons.Rounded.SettingsInputComponent),
            NavItem("settings", R.string.nav_settings, Icons.Rounded.Settings)
        )
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
                if (available.y < -10f) isFabVisible.value = false
                else if (available.y > 10f) isFabVisible.value = true
                return Offset.Zero
            }
        }
    }
   
    CompositionLocalProvider(LocalAppHazeState provides hazeState) {
        RootDialogsProvider {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    // Penentuan start destination dinamis
                    startDestination = if (hasCompletedGetStarted) {
                        if (useScrollAnimation) "main" else "home"
                    } else "get_started",
                    
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .nestedScroll(nestedScrollConnection)
                        .then(
                            if (isBlurEnabled) Modifier.hazeSource(state = hazeState) else Modifier
                        ),
                    enterTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeIn(animationSpec = tween(700)) 
                        } else if (targetState.destination.route !in bottomBarRoutes) {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    exitTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route in bottomBarRoutes) {
                            fadeOut(animationSpec = tween(700))
                        } else if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -(fullWidth / 4) },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        } else {
                            fadeOut(animationSpec = tween(150))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.destination.route !in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -(fullWidth / 4) },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    popExitTransition = {
                        if (initialState.destination.route !in bottomBarRoutes) {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        } else {
                            fadeOut(animationSpec = tween(150))
                        }
                    },
                    predictivePopEnterTransition = {
                        if (initialState.destination.route !in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -(fullWidth / 4) },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    predictivePopExitTransition = {
                        if (initialState.destination.route !in bottomBarRoutes) {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        } else {
                            fadeOut(animationSpec = tween(150))
                        }
                    }
                ) {
                    composable("get_started") { GetStartedScreen(navController) }
                    
                    // Route Pager (Kode 2)
                    composable("main") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (pagerRoutes[page]) {
                                "home" -> HomeScreen()
                                "applist" -> ApplistScreen(navController)
                                "tweaks" -> TweakScreen(navController)
                                "settings" -> SettingsScreen(navController)
                            }
                        }
                    }
                    
                    // Route Normal (Kode 1)
                    composable("home") { HomeScreen() }
                    composable("applist") { ApplistScreen(navController) }
                    composable("tweaks") { TweakScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }

                    // Subscreens
                    composable("color_palette") { ColorPaletteScreen(navController) }
                    composable("colorscheme") { ColorSchemeSettings(navController) }
                    composable("FasScreen") { FasScreen(navController) }
                    composable("bypasschg") { BypassChargeScreen(navController) }
                    composable("bypasschg_check") { BypassChargeCheckScreen(navController) }
                    composable("preferenced") { PreferenceTweakScreen(navController) }
                    composable("aboutscreen") { AboutScreen(navController) }
                    composable("fpsgoscreen") { FpsGoSettings(navController) }
                    composable(
                        route = "app_settings/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pkg = backStackEntry.arguments?.getString("pkg")
                        AppSettingsScreen(navController, pkg)
                    }
                }
                
                AnimatedVisibility(
                    visible = rootStatus && moduleInstalled && rawRoute in bottomBarRoutes,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavBar(
                        items = navItems,
                        selectedRoute = currentRoute ?: "home",
                        isBlurEnabled = isBlurEnabled,
                        hazeState = hazeState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onItemSelected = { route ->
                            if (useScrollAnimation) {
                                // Logic klik untuk Pager (Kode 2)
                                val targetIndex = pagerRoutes.indexOf(route)
                                if (isOnMainPager) {
                                    if (pagerState.currentPage != targetIndex) {
                                        coroutineScope.launch {
                                            pagerState.smoothScrollToPage(targetIndex)
                                        }
                                    }
                                } else {
                                    navController.navigate("main") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetIndex)
                                    }
                                }
                            } else {
                                // Logic klik untuk Normal NavHost (Kode 1)
                                if (rawRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            }
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
    hazeState: HazeState? = null
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    NavPill(
                        item = item,
                        isSelected = isSelected,
                        isBlurEnabled = isBlurEnabled, 
                        onClick = { onItemSelected(item.route) },
                        modifier = if (isSelected) Modifier.weight(1f) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    isSelected: Boolean,
    isBlurEnabled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    val animationSpec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)
    
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected && isBlurEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            isSelected && !isBlurEnabled -> MaterialTheme.colorScheme.primary
            !isSelected && isBlurEnabled -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        },
        animationSpec = animationSpec,
        label = "bgColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected && isBlurEnabled -> MaterialTheme.colorScheme.primary
            isSelected && !isBlurEnabled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = animationSpec,
        label = "contentColor"
    )

    val shape = if (isSelected) RoundedCornerShape(24.dp) else CircleShape
    
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
        
        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start
            ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)),
            exit = shrinkHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start
            ) + fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false, 
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 5.dp) 
            )
        }
    }
}
