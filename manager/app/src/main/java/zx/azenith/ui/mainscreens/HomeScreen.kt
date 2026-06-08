@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.mainscreens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.viewmodel.HomeViewModel


                    val isPerformanceMode = uiState.currentProfileRes == R.string.Profile_Performance || uiState.currentProfileRes == R.string.profile_perflite
                    val showGameCard = isPerformanceMode && !uiState.runningGamePkg.isNullOrEmpty()
                    
                    // 👇 1. TAMBAHKAN INI: Simpan data terakhir agar animasi exit tidak patah
                    
                    
                    // Update nilai memori HANYA jika datanya valid (tidak kosong)
                    
                    
                    if (isLandscape) {
                        // MODE LANDSCAPE
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // ... (Kode Row Banner dan InfoTile tetap sama) ...
                    
                            AnimatedVisibility(
                                visible = showGameCard,
                                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                            ) {
                                // 👇 2. GUNAKAN VARIABEL RETAINED DI SINI
                                
                            }
                        }
                    } else {
                        // MODE PORTRAIT
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // ... (Kode BannerCard tetap sama) ...
                    
                            AnimatedVisibility(
                                visible = showGameCard,
                                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                            ) {
                                // 👇 3. GUNAKAN VARIABEL RETAINED DI SINI JUGA
                                
                            }
                    
                            // ... (Kode Row InfoTile tetap sama) ...
                        }
                    }
                

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.uiState.collectAsState()

    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var isBlurEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("expressive_blur_ui", false)) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showRebootSheet by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    
    LaunchedEffect(Unit) {
        viewModel.refreshAiMode()
        isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                HomeTopAppBar(
                    scrollBehavior = scrollBehavior,
                    onRebootClick = { showRebootSheet = true }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp, end = 16.dp,
                    bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val bannerStatus = if (!uiState.moduleInstalled) stringResource(R.string.module_not_installed) else stringResource(uiState.serviceStatusRes)
                    
               
                    val isPerformanceMode = uiState.currentProfileRes == R.string.Profile_Performance || uiState.currentProfileRes == R.string.profile_perflite
                    val showGameCard = isPerformanceMode && !uiState.runningGamePkg.isNullOrEmpty()
                    
                    var retainedPkg by remember { mutableStateOf("") }
                    var retainedStartTime by remember { mutableStateOf("00:00:00") }
                    
                    LaunchedEffect(uiState.runningGamePkg, uiState.runningGameStartTime) {
                        if (!uiState.runningGamePkg.isNullOrEmpty()) {
                            retainedPkg = uiState.runningGamePkg!!
                            retainedStartTime = uiState.runningGameStartTime ?: "00:00:00"
                        }
                    }
                
                    if (isLandscape) {
                        // MODE LANDSCAPE
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Barisan Atas (Banner + InfoTile berdampingan)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    BannerCard(
                                        status = bannerStatus, pid = uiState.servicePid,
                                        isBannerEnabled = uiState.isBannerEnabled, 
                                        isBlurEnabled = isBlurEnabled,
                                        modifier = Modifier.fillMaxSize()
                                    ) { }
                                }
                                
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    InfoTile(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        icon = Icons.Rounded.Token, 
                                        label = stringResource(R.string.current_profile), 
                                        value = stringResource(uiState.currentProfileRes), 
                                        highlight = (uiState.currentProfileRes != R.string.status_initializing), 
                                        showArrow = uiState.autoMode == "0"
                                    ) { if (uiState.autoMode == "0") showProfileDialog = true }
                                    
                                    InfoTile(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        icon = Icons.Rounded.Security, 
                                        label = stringResource(R.string.root_access), 
                                        value = if (uiState.rootStatus) stringResource(R.string.root_granted) else stringResource(R.string.root_not_granted), 
                                        highlight = false
                                    ) {}
                                }
                            }
                
                            // 👇 Taruh dibawah (Sesuai request) menggunakan AnimatedVisibility
                            AnimatedVisibility(
                                visible = showGameCard,
                                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                            ) {
                                if (retainedPkg.isNotEmpty()) { // Cegah render jika masih benar-benar kosong
                                    RunningGameCard(
                                        pkgName = retainedPkg,
                                        startTimeStr = retainedStartTime
                                    )
                                }
                            }
                        }
                    } else {
                        // MODE PORTRAIT
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            BannerCard(
                                status = bannerStatus, pid = uiState.servicePid,
                                isBannerEnabled = uiState.isBannerEnabled,
                                isBlurEnabled = isBlurEnabled,
                                modifier = if (uiState.isBannerEnabled) Modifier.fillMaxWidth().aspectRatio(20 / 9f) else Modifier.fillMaxWidth().height(100.dp)
                            ) { }
                
                            // 👇 Taruh ditengah (Antara Banner dan dua Tile)
                            AnimatedVisibility(
                                visible = showGameCard,
                                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring()) + fadeOut()
                            ) {
                                if (retainedPkg.isNotEmpty()) { 
                                    RunningGameCard(
                                        pkgName = retainedPkg,
                                        startTimeStr = retainedStartTime
                                    )
                                }
                            }
                
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InfoTile(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    icon = Icons.Rounded.Token, 
                                    label = stringResource(R.string.current_profile), 
                                    value = stringResource(uiState.currentProfileRes), 
                                    highlight = (uiState.currentProfileRes != R.string.status_initializing), 
                                    showArrow = uiState.autoMode == "0"
                                ) { if (uiState.autoMode == "0") showProfileDialog = true }
                                
                                InfoTile(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    icon = Icons.Rounded.Security, 
                                    label = stringResource(R.string.root_access), 
                                    value = if (uiState.rootStatus) stringResource(R.string.root_granted) else stringResource(R.string.root_not_granted), 
                                    highlight = false
                                ) {}
                            }
                        }
                    }
                }
                
                item { DeviceInfoCard() }
                item { LinkCard(Icons.Rounded.Favorite, R.string.support_us, R.string.support_us_desc) { uriHandler.openUri("https://t.me/ZeshArch") } }
                item { LinkCard(Icons.Rounded.Info, R.string.learn_more, R.string.learn_more_desc) { uriHandler.openUri("https://github.com/Liliya2727/AZenith") } }
            }
        }

        RootAppDialog {
            RebootBottomSheet(
                show = showRebootSheet,
                onDismiss = { showRebootSheet = false },
                onReboot = { reason -> viewModel.rebootDevice(reason) }
            )
        }

        // 👇 KEMBALIKAN KODE INI SEPERTI SEMULA
        RootAppDialog {
            ProfileDialog(
                show = showProfileDialog,
                onDismiss = { showProfileDialog = false },
                onProfile = { profileReason ->
                    viewModel.applyProfile(profileReason) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_applying_profile))
                        }
                    }
                }
            )
        }
    }
}
