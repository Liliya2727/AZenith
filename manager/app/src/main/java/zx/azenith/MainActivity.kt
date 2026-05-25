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

package zx.azenith

import android.content.res.Configuration
import android.os.Bundle
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
import androidx.navigation.navArgument
import zx.azenith.R
import zx.azenith.ui.screens.*
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.util.RootUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isFromTile = intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES"
        setContent {
            AZenithTheme {
                MainScreen(isFromTile)
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
fun MainScreen(isFromTile: Boolean = false) {
    val navController = rememberNavController()
    LaunchedEffect(isFromTile) {
        if (isFromTile) {
            navController.navigate("bypasschg") {
                popUpTo("home") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val rootStatus by produceState(initialValue = false) { value = RootUtils.isRootGranted() }
    val moduleInstalled by produceState(initialValue = false) { value = RootUtils.isModuleInstalled() }

    val navItems = remember {
        listOf(
            NavItem("home", R.string.nav_home, Icons.Rounded.Home),
            NavItem("applist", R.string.nav_applist, Icons.Rounded.Widgets),
            NavItem("tweaks", R.string.nav_tweaks, Icons.Rounded.SettingsInputComponent),
            NavItem("settings", R.string.nav_settings, Icons.Rounded.Settings)
        )
    }
    
    val bottomBarRoutes = remember { setOf("home", "applist", "tweaks", "settings") }

    Box(modifier = Modifier.fillMaxSize()) {
        
        Row(modifier = Modifier.fillMaxSize()) {
            if (rootStatus && moduleInstalled && isLandscape && currentRoute in bottomBarRoutes) {
                SideBar(navController, navItems, currentRoute)
            }

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),                
                enterTransition = {
                    if (targetState.destination.route !in bottomBarRoutes) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400))
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                },
                exitTransition = {
                    if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, targetOffset = { it / 4 }, animationSpec = tween(400)) + fadeOut()
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route !in bottomBarRoutes && targetState.destination.route in bottomBarRoutes) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, initialOffset = { it / 4 }, animationSpec = tween(400)) + fadeIn()
                    } else {
                        fadeIn(animationSpec = tween(340))
                    }
                },
                popExitTransition = {
                    if (initialState.destination.route !in bottomBarRoutes) {
                        scaleOut(targetScale = 0.9f, animationSpec = tween(300)) + fadeOut()
                    } else {
                        fadeOut(animationSpec = tween(340))
                    }
                }
            ) {
                composable("home") { HomeScreen() }
                composable("applist") { ApplistScreen(navController) }
                composable("tweaks") { TweakScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("color_palette") { ColorPaletteScreen(navController) }
                composable("colorscheme") { ColorSchemeSettings(navController) }
                composable("bypasschg") { BypassChargeScreen(navController) }
                composable("bypasschg_check") { BypassChargeCheckScreen(navController) }
                composable("preferenced") { PreferenceTweakScreen(navController) }
                composable(
                    route = "app_settings/{pkg}",
                    arguments = listOf(navArgument("pkg") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pkg = backStackEntry.arguments?.getString("pkg")
                    AppSettingsScreen(navController, pkg)
                }
            }
        }

        AnimatedVisibility(
            visible = rootStatus && moduleInstalled && !isLandscape && currentRoute in bottomBarRoutes,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomNavBar(
                items = navItems,
                selectedRoute = currentRoute ?: "home",
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                onItemSelected = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
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
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Efek scale saat ditekan
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    // Animasi transisi warna
    val animationSpec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)
    
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        animationSpec = animationSpec,
        label = "bgColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = animationSpec,
        label = "contentColor"
    )

    val shape = if (isSelected) RoundedCornerShape(24.dp) else CircleShape
    
    // HAPUS Box dan animateContentSize, kita pakai Row langsung seperti KSU
    Row(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .defaultMinSize(minWidth = 48.dp) // Pastikan bentuknya minimal bulat sempurna (48x48) saat menutup
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp), // Padding seragam
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        // Murni mengandalkan expandHorizontally untuk melebar tanpa bentrok
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
                softWrap = false, // Kunci biar teks gak loncat ke baris baru saat menyusut
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 5.dp) // Jarak antara icon dan teks
            )
        }
    }
}
