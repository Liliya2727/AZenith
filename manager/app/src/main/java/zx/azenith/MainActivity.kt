package zx.azenith

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import zx.azenith.ui.screens.HomeScreen
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.screens.ApplistScreen
import zx.azenith.ui.screens.TweakScreen
import zx.azenith.ui.screens.AppSettingsScreen
import zx.azenith.ui.screens.SettingsScreen
import zx.azenith.ui.screens.ColorPaletteScreen
import zx.azenith.ui.util.RootUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AZenithTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 1. Ambil status root menggunakan produceState
    val rootStatus by produceState(initialValue = false) {
        value = RootUtils.isRootGranted()
    }
    
    val moduleInstalled by produceState(initialValue = false) {
        value = RootUtils.isModuleInstalled()
    }

    val navItems = remember {
        listOf(
            Triple("Home", Icons.Rounded.Home, "home"),
            Triple("Applist", Icons.Rounded.Widgets, "applist"),
            Triple("Tweaks", Icons.Rounded.SettingsInputComponent, "tweaks"),
            Triple("Settings", Icons.Rounded.Settings, "settings")
        )
    }

    val bottomBarRoutes = remember {
        setOf("home", "applist", "tweaks", "settings")
    }

    Scaffold(
        bottomBar = {
            if (rootStatus && moduleInstalled && !isLandscape && currentRoute in bottomBarRoutes) {
                BottomBar(navController, navItems, currentRoute)
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (rootStatus && moduleInstalled && isLandscape && currentRoute in bottomBarRoutes) {
                SideBar(navController, navItems, currentRoute)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    // GUNAKAN INI: Hanya terapkan padding bawah (untuk Navbar)
                    // Padding atas dibiarkan 0 agar TopAppBar tetap mepet ke Status Bar
                    .padding(bottom = innerPadding.calculateBottomPadding()) 
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        // Jika dari rute utama masuk ke sub-page (seperti color_palette) -> Slide Left
                        if (targetState.destination.route !in bottomBarRoutes) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400))
                        } else {
                            // Antar tab utama (Home <-> Applist, dll) -> Fade
                            fadeIn(animationSpec = tween(340))
                        }
                    },
                    exitTransition = {
                        // Jika meninggalkan rute utama menuju sub-page -> Slide Out Left (Sedikit)
                        if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, targetOffset = { it / 4 }, animationSpec = tween(400)) + fadeOut()
                        } else {
                            // Antar tab utama -> Fade
                            fadeOut(animationSpec = tween(340))
                        }
                    },
                    popEnterTransition = {
                        // Jika kembali dari sub-page ke rute utama -> Slide Right
                        if (initialState.destination.route !in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, initialOffset = { it / 4 }, animationSpec = tween(400)) + fadeIn()
                        } else {
                            // Antar tab utama -> Fade
                            fadeIn(animationSpec = tween(340))
                        }
                    },
                    popExitTransition = {
                        // Jika sub-page ditutup -> Scale Down + Fade Out
                        if (initialState.destination.route !in bottomBarRoutes) {
                            scaleOut(targetScale = 0.9f, animationSpec = tween(300)) + fadeOut()
                        } else {
                            // Antar tab utama -> Fade
                            fadeOut(animationSpec = tween(340))
                        }
                    }
                ) {
                    composable("home") { HomeScreen() }
                    composable("applist") { ApplistScreen(navController) }
                    composable("tweaks") { TweakScreen() }
                    composable("settings") { SettingsScreen(navController) }
                    composable("color_palette") { ColorPaletteScreen(navController) }
                    composable(
                        route = "app_settings/{pkg}",
                        arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val pkg = backStackEntry.arguments?.getString("pkg")
                        AppSettingsScreen(navController, pkg)
                    }
                }
            }
        }
    }
}


@Composable
private fun BottomBar(
    navController: NavHostController,
    items: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, String>>,
    activeTabRoute: String?
) {
    NavigationBar {
        items.forEach { (label, icon, route) ->
            val isTabSelected = activeTabRoute == route
            
            NavigationBarItem(
                selected = isTabSelected,
                onClick = {
                    if (activeTabRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Jika sudah di tab yang sama tapi di sub-page, maka kembali ke root tab
                        if (navController.currentDestination?.route != route) {
                            navController.popBackStack(route, inclusive = false)
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
private fun SideBar(
    navController: NavHostController,
    items: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, String>>,
    currentRoute: String?
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            items.forEach { (label, icon, route) ->
                val isSelected = currentRoute == route
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) },
                    alwaysShowLabel = false
                )
            }
        }
    }
}
