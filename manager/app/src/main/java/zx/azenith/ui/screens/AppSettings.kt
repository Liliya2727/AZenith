package zx.azenith.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.viewmodel.ApplistViewmodel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import zx.azenith.ui.screens.ApplistScreen
import zx.azenith.ui.viewmodel.AppSettingsViewModel
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveListItem
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.component.ExpressiveDropdownItem
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    navController: NavController, 
    packageName: String?,
    viewModel: AppSettingsViewModel = viewModel(),
    // Tambahkan ini agar tidak Unresolved reference
    appListViewModel: ApplistViewmodel = viewModel() 
) {
    val context = LocalContext.current
    val appDetails = remember(packageName) { getAppDetails(context, packageName) }
    
    LaunchedEffect(packageName) { 
        viewModel.loadConfig() 
    }

    val config = viewModel.fullConfig[packageName]
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // PINDAHKAN KE SINI (Di dalam fungsi Composable)
    var localMasterOn by remember(config != null) { mutableStateOf(config != null) }

    val booleanModes = listOf("Default", "On", "Off")
    val rendererModes = listOf("Default", "Vulkan", "SkiaGL")
    val refreshModes = listOf("Default", "60", "90", "120", "144")

    fun getBoolIndex(v: String?): Int = when(v) {
        "true" -> 1
        "false" -> 2
        else -> 0
    }
    
    // Refresh otomatis saat keluar dari screen
    DisposableEffect(Unit) {
        onDispose {
            appListViewModel.loadApps(context, forceRefresh = true)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { 
            AppSettingsTopAppBar(scrollBehavior) { 
                // Perbaikan Syntax: Gunakan kurung kurawal atau pisahkan dengan baris baru
                appListViewModel.loadApps(context, forceRefresh = true) 
                navController.popBackStack() 
            } 
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { AppHeader(appDetails, packageName) }

            item {
                ExpressiveList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf {
                        ExpressiveSwitchItem(
                            icon = Icons.Rounded.PowerSettingsNew,
                            title = "Master Switch",
                            summary = "Enable app to trigger Performance profiles",
                            checked = localMasterOn,
                            onCheckedChange = { isChecked ->
                                localMasterOn = isChecked 
                                packageName?.let { pkg -> viewModel.toggleMasterSwitch(pkg, isChecked) } 
                            }
                        )
                    }
                )
            }
            
            item {
                AnimatedVisibility(
                    visible = localMasterOn,
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    // Gunakan path lengkap jika import bermasalah
                    val displayConfig = config ?: zx.azenith.ui.util.AppConfig() 
                    
                    Column {
                        SectionHeader("Preferred Settings")
                        ExpressiveList(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            content = listOf(
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.Speed,
                                        title = "Perf Lite Mode",
                                        summary = "Reduce heating by reducing CPU frequency",
                                        items = booleanModes, // Pastikan parameter ini ada
                                        selectedIndex = getBoolIndex(displayConfig.perf_lite_mode),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "perf_lite_mode", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.RocketLaunch,
                                        title = "Game Preload",
                                        summary = "Preload libraries at game start",
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.game_preload),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "game_preload", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.PriorityHigh,
                                        title = "App Priority",
                                        summary = "Increase I/O scheduling priority",
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.app_priority),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "app_priority", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.DoNotDisturbOn,
                                        title = "DND Mode",
                                        summary = "Block notifications while gaming",
                                        items = booleanModes,
                                        selectedIndex = getBoolIndex(displayConfig.dnd_on_gaming),
                                        onItemSelected = { index ->
                                            val value = listOf("default", "true", "false")[index]
                                            packageName?.let { viewModel.updateSetting(it, "dnd_on_gaming", value) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.Refresh,
                                        title = "Refresh Rate",
                                        summary = "Set preferred Display refresh rates",
                                        items = refreshModes,
                                        selectedIndex = refreshModes.indexOf(displayConfig.refresh_rate).coerceAtLeast(0),
                                        onItemSelected = { index ->
                                            packageName?.let { viewModel.updateSetting(it, "refresh_rate", refreshModes[index]) }
                                        }
                                    )
                                },
                                {
                                    ExpressiveDropdownItem(
                                        icon = Icons.Rounded.Layers,
                                        title = "Renderer",
                                        summary = "Set preferred rendering engine",
                                        items = rendererModes,
                                        selectedIndex = rendererModes.indexOf(displayConfig.renderer.replaceFirstChar { it.uppercase() }).coerceAtLeast(0),
                                        onItemSelected = { index ->
                                            val value = rendererModes[index].lowercase()
                                            packageName?.let { viewModel.updateSetting(it, "renderer", value) }
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(70.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 28.dp,
            end = 28.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun AppHeader(appDetails: Triple<String, android.graphics.drawable.Drawable?, String>, packageName: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp),
            shadowElevation = 2.dp
        ) {
            AsyncImage(
                model = appDetails.second,
                contentDescription = null,
                modifier = Modifier.padding(16.dp).fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Label
        Text(
            text = appDetails.first,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Package Name
        Text(
            text = packageName ?: "Unknown Package",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Version Tag
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = "v${appDetails.third}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

fun getAppDetails(context: android.content.Context, packageName: String?): Triple<String, android.graphics.drawable.Drawable?, String> {
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName ?: "", 0)
        val packageInfo = pm.getPackageInfo(packageName ?: "", 0)
        val label = pm.getApplicationLabel(info).toString()
        val icon = pm.getApplicationIcon(info)
        val version = packageInfo.versionName ?: "Unknown"
        Triple(label, icon, version)
    } catch (e: Exception) {
        Triple("Unknown App", null, "0.0.0")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSettingsTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
    TopAppBar(
        title = { 
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}