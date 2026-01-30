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
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import zx.azenith.ui.screens.*
import zx.azenith.ui.theme.AZenithTheme
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


val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    // Tambahkan default value agar tidak error saat dipanggil tanpa gradient
    val gradientColors: List<Color> = listOf(Color.Transparent, Color.Transparent)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val rootStatus by produceState(initialValue = false) { value = RootUtils.isRootGranted() }
    val moduleInstalled by produceState(initialValue = false) { value = RootUtils.isModuleInstalled() }

    val navItems = remember {
        listOf(
            NavItem("home", "Home", Icons.Rounded.Home),
            NavItem("applist", "Applist", Icons.Rounded.Widgets),
            NavItem("tweaks", "Tweaks", Icons.Rounded.SettingsInputComponent),
            NavItem("settings", "Settings", Icons.Rounded.Settings)
        )
    }
    
    val bottomBarRoutes = remember { setOf("home", "applist", "tweaks", "settings") }

    // PAKAI BOX SAJA agar benar-benar melayang & transparan
    Box(modifier = Modifier.fillMaxSize()) {
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar muncul jika Landscape
            if (rootStatus && moduleInstalled && isLandscape && currentRoute in bottomBarRoutes) {
                SideBar(navController, navItems, currentRoute)
            }

            // NavHost sekarang memenuhi layar (Fullscreen)
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
                .widthIn(max = 500.dp)
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
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100), label = "scale"
    )
    
    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200), label = "labelAlpha"
    )

    val shape = if (isSelected) RoundedCornerShape(24.dp) else CircleShape
    
    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            // WAJIB: Agar transisi lebar Pill menjadi halus
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .then(if (!isSelected) Modifier.aspectRatio(1f) else Modifier)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp) // Tambahkan padding dalam
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            
            if (isSelected && labelAlpha > 0.01f) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.alpha(labelAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SideBar(
    navController: NavHostController,
    items: List<NavItem>,
    currentRoute: String?
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    alwaysShowLabel = false
                )
            }
        }
    }
}
