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

import android.content.res.Configuration
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.navArgument
import zx.azenith.R
import zx.azenith.ui.mainscreens.*
import zx.azenith.ui.subscreens.*
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.util.*
import kotlinx.coroutines.launch
import com.topjohnwu.superuser.Shell
import zx.azenith.ui.component.* import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.RestartAlt
import java.io.File
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.blurEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(fromTileType: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(fromTileType) {
        when (fromTileType) {
            "bypass" -> {
                navController.navigate("bypasschg") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            "profile" -> {
                navController.navigate("home") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    
    
    var pendingReboot by remember { mutableStateOf(false) }
        
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

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
        pendingReboot = Shell.cmd("test -f /data/adb/modules/AZenith/reboot").exec().isSuccess
    }

    LaunchedEffect(currentRoute) {
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
    
    val bottomBarRoutes = remember { setOf("home", "applist", "tweaks", "settings") }
    
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            coroutineScope.launch {
                val cacheApk = File(context.cacheDir, "AZenith_update.apk")
                
                val copyCmd = """
                    cp /data/adb/modules/AZenith/AZenith.apk ${cacheApk.absolutePath}
                    chmod 644 ${cacheApk.absolutePath}
                """.trimIndent()
                
                val result = Shell.cmd(copyCmd).exec()
                
                if (result.isSuccess && cacheApk.exists()) {
                    try {
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            cacheApk
                        )
                        
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open installer: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to copy update file from Root.", Toast.LENGTH_SHORT).show()
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
                    title = "App Update Available",
                    content = "Your app manager version ($appVC) is older than the module version ($moduleVC).\n\nUpdate now to get the latest features?",
                    confirm = "Update Now",
                    dismiss = "Later"
                )
            }

            if (RootUtils.isModuleUpdatePendingReboot()) {
                rebootDialog.showConfirm(
                    title = "Module Update Available",
                    content = "Module update is pending to be applied. Reboot the device now?",
                    confirm = "Reboot Now",
                    dismiss = "Not Now"
                )
            }
        }
    }
    
    val isFabVisible = remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Jika scroll ke bawah (y negatif), sembunyikan FAB
                if (available.y < -10f) {
                    isFabVisible.value = false
                } 
                // Jika scroll ke atas (y positif), munculkan FAB
                else if (available.y > 10f) {
                    isFabVisible.value = true
                }
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
                    startDestination = if (hasCompletedGetStarted) "home" else "get_started",
                    // Pasang nested scroll di sini untuk melacak scroll secara global
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .nestedScroll(nestedScrollConnection)
                        .then(
                            if (isBlurEnabled) Modifier.hazeSource(state = hazeState) else Modifier
                        ),
                    enterTransition = {
                        if (initialState.destination.route == "get_started" && targetState.destination.route == "home") {
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
                        if (initialState.destination.route == "get_started" && targetState.destination.route == "home") {
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
                    }
                ) {
                    composable("get_started") { GetStartedScreen(navController) }
                    composable("home") { HomeScreen() }
                    composable("applist") { ApplistScreen(navController) }
                    composable("tweaks") { TweakScreen(navController) }
                    composable("settings") { SettingsScreen(navController) }
                    composable("color_palette") { ColorPaletteScreen(navController) }
                    composable("colorscheme") { ColorSchemeSettings(navController) }
                    composable("FasScreen") { FasScreen(navController) }
                    composable("bypasschg") { BypassChargeScreen(navController) }
                    composable("bypasschg_check") { BypassChargeCheckScreen(navController) }
                    composable("preferenced") { PreferenceTweakScreen(navController) }
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
                    visible = rootStatus && moduleInstalled && currentRoute in bottomBarRoutes,
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
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = rootStatus && moduleInstalled && pendingReboot && currentRoute in bottomBarRoutes && isFabVisible.value,
                    enter = scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = scaleOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 116.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            rebootDialog.showConfirm(
                                title = "Reboot Required",
                                content = "A configuration has been restored. Reboot the device now to apply changes?",
                                confirm = "Reboot",
                                dismiss = "Later"
                            )
                        },
                        icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reboot") },
                        text = { Text("Reboot", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    )
                }
            }
            ConfirmDialogHost(handle = updateDialog)
            ConfirmDialogHost(handle = rebootDialog)
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
