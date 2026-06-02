@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class, 
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class
)

package zx.azenith.ui.mainscreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    }

    MaterialExpressiveTheme {
        // Bungkus keseluruhan layar dengan SharedTransitionLayout
        SharedTransitionLayout {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        BannerCard(
                                            status = bannerStatus, pid = uiState.servicePid,
                                            isBannerEnabled = uiState.isBannerEnabled, modifier = Modifier.fillMaxSize()
                                        ) { }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Tile Profil Landscape - Modifier weight harus ditaruh di bungkus AnimatedVisibility
                                        AnimatedVisibility(
                                            visible = true,
                                            modifier = Modifier.fillMaxWidth().weight(1f)
                                        ) {
                                            InfoTile(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .sharedBounds(
                                                        sharedContentState = rememberSharedContentState(key = "profile_dialog_transition"),
                                                        animatedVisibilityScope = this@AnimatedVisibility
                                                    ),
                                                icon = Icons.Rounded.Token,
                                                label = stringResource(R.string.current_profile), value = stringResource(uiState.currentProfileRes),
                                                highlight = (uiState.currentProfileRes != R.string.status_initializing), showArrow = uiState.autoMode == "0"
                                            ) { if (uiState.autoMode == "0") showProfileDialog = true }
                                        }

                                        InfoTile(
                                            modifier = Modifier.fillMaxWidth().weight(1f), icon = Icons.Rounded.Security,
                                            label = stringResource(R.string.root_access),
                                            value = if (uiState.rootStatus) stringResource(R.string.root_granted) else stringResource(R.string.root_not_granted),
                                            highlight = false
                                        ) {}
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    BannerCard(
                                        status = bannerStatus, pid = uiState.servicePid,
                                        isBannerEnabled = uiState.isBannerEnabled,
                                        modifier = if (uiState.isBannerEnabled) Modifier.fillMaxWidth().aspectRatio(20 / 9f) else Modifier.fillMaxWidth().height(100.dp)
                                    ) { }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Tile Profil Portrait - Modifier weight ditaruh di AnimatedVisibility
                                        AnimatedVisibility(
                                            visible = true,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            InfoTile(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .sharedBounds(
                                                        sharedContentState = rememberSharedContentState(key = "profile_dialog_transition"),
                                                        animatedVisibilityScope = this@AnimatedVisibility,
                                                        // Cukup gunakan boundsTransform, hapus baris resizeMode
                                                        boundsTransform = { _, _ -> 
                                                            spring(dampingRatio = 0.8f, stiffness = 400f) 
                                                        }
                                                    ),
                                                icon = Icons.Rounded.Token,
                                                label = stringResource(R.string.current_profile), value = stringResource(uiState.currentProfileRes),
                                                highlight = (uiState.currentProfileRes != R.string.status_initializing), showArrow = uiState.autoMode == "0"
                                            ) { if (uiState.autoMode == "0") showProfileDialog = true }
                                        }

                                        InfoTile(
                                            modifier = Modifier.weight(1f), icon = Icons.Rounded.Security,
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

                // DIALOG OVERLAY (Di luar Scaffold agar menutupi seluruh layar)
                CustomSharedDialog(
                    visible = showProfileDialog,
                    onDismissRequest = { showProfileDialog = false },
                    sharedKey = "profile_dialog_transition"
                ) {
                    ProfileDialogContent(
                        onDismiss = { showProfileDialog = false },
                        onProfile = { profileReason ->
                            showProfileDialog = false // Tutup dialog secara visual dulu
                            viewModel.applyProfile(profileReason) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_applying_profile))
                                }
                            }
                        }
                    )
                }

                RebootBottomSheet(
                    show = showRebootSheet,
                    onDismiss = { showRebootSheet = false },
                    onReboot = { reason -> viewModel.rebootDevice(reason) }
                )
            }
        }
    }
}
