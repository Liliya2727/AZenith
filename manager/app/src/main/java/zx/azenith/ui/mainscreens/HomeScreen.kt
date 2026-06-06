@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class, 
    ExperimentalMaterial3Api::class, 
    ExperimentalSharedTransitionApi::class // 👇 Opt-in untuk animasi Shared Element
)

package zx.azenith.ui.mainscreens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.viewmodel.HomeViewModel

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
        // 👇 BUNGKUS DENGAN SHARED TRANSITION LAYOUT
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            
            // LAYER 1: Layar Utama (Selalu Terlihat)
            AnimatedVisibility(
                visible = true,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
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
                            
                            if (isLandscape) {
                                // MODE LANDSCAPE
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
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                // 👇 KUNCI ANIMASI MORPHING KE DIALOG
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(key = "profile_morph"),
                                                    animatedVisibilityScope = this@AnimatedVisibility,
                                                    
                                                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(26.dp))
                                                ),
                                            icon = Icons.Rounded.Token, 
                                            label = stringResource(R.string.current_profile), 
                                            value = stringResource(uiState.currentProfileRes), 
                                            highlight = (uiState.currentProfileRes != R.string.status_initializing), 
                                            showArrow = uiState.autoMode == "0"
                                        ) { 
                                            if (uiState.autoMode == "0") showProfileDialog = true 
                                        }
                                        
                                        InfoTile(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            icon = Icons.Rounded.Security, 
                                            label = stringResource(R.string.root_access), 
                                            value = if (uiState.rootStatus) stringResource(R.string.root_granted) else stringResource(R.string.root_not_granted), 
                                            highlight = false
                                        ) {}
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        InfoTile(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                // 👇 KUNCI ANIMASI MORPHING KE DIALOG
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(key = "profile_morph"),
                                                    animatedVisibilityScope = this@AnimatedVisibility,
                                                    
                                                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(26.dp))
                                                ),
                                            icon = Icons.Rounded.Token, 
                                            label = stringResource(R.string.current_profile), 
                                            value = stringResource(uiState.currentProfileRes), 
                                            highlight = (uiState.currentProfileRes != R.string.status_initializing), 
                                            showArrow = uiState.autoMode == "0"
                                        ) { 
                                            if (uiState.autoMode == "0") showProfileDialog = true 
                                        }
                                        
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
            }

            // LAYER 2: Overlay Dialog Profile (Akan Menerima Transformasi dari Tile)
            AnimatedVisibility(
                visible = showProfileDialog,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                // Background Gelap (Scrim)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showProfileDialog = false }
                        )
                )

                // Posisi Dialog di Tengah Layar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(101f),
                    contentAlignment = Alignment.Center
                ) {
                    // Kotak Pendaratan Animasi Morphing
                    Box(
                        modifier = Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "profile_morph"),
                                animatedVisibilityScope = this@AnimatedVisibility,
                                
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp))
                            )
                    ) {
                        // Tampilkan Dialog di sini, BUKAN di dalam RootAppDialog lagi
                        ProfileDialog(
                            show = true, // Set selalu true karena animasinya dihandle oleh pembungkus di atas
                            onDismiss = { showProfileDialog = false },
                            onProfile = { profileReason ->
                                showProfileDialog = false // Tutup dialognya
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
        }

        // Reboot masih menggunakan RootAppDialog karena dia berbentuk Bottom Sheet
        RootAppDialog {
            RebootBottomSheet(
                show = showRebootSheet,
                onDismiss = { showRebootSheet = false },
                onReboot = { reason -> viewModel.rebootDevice(reason) }
            )
        }
    }
}
