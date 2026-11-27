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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens

import zx.azenith.ui.theme.azShape


import zx.azenith.ui.navigation.safePopBackStack
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.materialkolor.rememberDynamicColorScheme
import com.topjohnwu.superuser.Shell
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.ConfigUtils
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.RootUtils


/**
 * Which LazyColumn item holds each searchable row, so a search result can scroll
 * to it. Positional: the list is built inline inside the screen and the rows
 * carry no keys of their own.
 *
 * Portrait is one list: info(0) illustration(1) enable(2) compatibility(3).
 */
private val BYPASS_ROW_ITEMS: Map<String, Int> = mapOf(
    "bypass.enable" to 2,
    "bypass.compat" to 3,
)

/**
 * Landscape splits the screen in two lists, so each has its own index space.
 * Left: illustration(0) -- no searchable rows.
 * Right: info(0) enable(1) compatibility(2).
 */
private val BYPASS_LEFT_ITEMS: Map<String, Int> = emptyMap()
private val BYPASS_RIGHT_ITEMS: Map<String, Int> = mapOf(
    "bypass.enable" to 1,
    "bypass.compat" to 2
)

@Composable
fun BypassChargeScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val listState = rememberLazyListState()
    val rightListState = rememberLazyListState()
    RegisterGamepadActions(
        enabled = true,
        onBack = {
            navController.safePopBackStack()
            true
        },
        scrollState = if (isLandscape) rightListState else listState,
    )

    // Search results land here: entry id -> the LazyColumn item holding that row.
    val jumpPulse = listState.searchJumpTarget(
        owns = { id -> if (isLandscape) id in BYPASS_LEFT_ITEMS else id.startsWith("bypass.") },
        indexOf = { id -> if (isLandscape) BYPASS_LEFT_ITEMS[id] else BYPASS_ROW_ITEMS[id] }
    )
    val rightJumpPulse = rightListState.searchJumpTarget(
        owns = { id -> isLandscape && id in BYPASS_RIGHT_ITEMS },
        indexOf = { id -> BYPASS_RIGHT_ITEMS[id] }
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    var bypassPath by remember { mutableStateOf("") }
    var bypassChgState by remember { mutableStateOf<Boolean?>(null) }

    val isUnsupported = bypassPath == "UNSUPPORTED"
    val isNeedSetup = bypassPath == "NEED_SETUP"

    // The switch callback is a plain lambda, so it runs on the main thread.
    // Route the file writes through a scope on IO rather than making the tap
    // handler block on a root round trip.
    val writeScope = rememberCoroutineScope()

    // Load configuration asynchronously - skeleton shows immediately while loading
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val cfg = ConfigUtils.load()
            bypassPath = cfg.bypassPath
            bypassChgState = cfg.bypassChg
        }
    }

    val onToggleBypass: (Boolean) -> Unit = { isChecked ->
        bypassChgState = isChecked
        writeScope.launch(Dispatchers.IO) {
            ConfigUtils.setBool("bypass_chg", isChecked)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { BypassChgTopAppBar(onBack = { navController.safePopBackStack() },
            scrollBehavior = scrollBehavior) },
        contentColor = ScaffoldContentColor(),
        containerColor = ScaffoldContainerColor(colorScheme.surface) 
    ) { innerPadding ->
        val topPad = innerPadding.calculateTopPadding()
        val bottomPad = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        if (isLandscape) {
            // Two panes: the illustration on the left, the controls on the right,
            // so neither side needs a long scroll in landscape.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Illustration pane: no focus targets, so it must not claim the
                // screen entry — the controls pane below owns the d-pad seed.
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .hazePageSource(),
                    contentPadding = PaddingValues(
                        top = topPad,
                        bottom = bottomPad
                    ),
                    verticalArrangement = Arrangement.Center, 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    bypassIllustrationItem(
                        colorScheme = colorScheme,
                        illustrationHeight = 240.dp,
                        isUnsupported = isUnsupported
                    )
                }

                LazyColumn(
                    state = rightListState,
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .hazePageSource()
                        .gamepadScreenEntry(scopeKey = "subscreen:bypass_charge"),
                    contentPadding = PaddingValues(
                        top = topPad,
                        bottom = bottomPad
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    bypassInfoItem(colorScheme = colorScheme)
                    bypassOptionItems(
                        jumpPulse = rightJumpPulse,
                        isUnsupported = isUnsupported,
                        isNeedSetup = isNeedSetup,
                        bypassChgState = bypassChgState,
                        onToggle = onToggleBypass,
                        onOpenCheck = { navController.navigate("bypasschg_check") }
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazePageSource()
                    .gamepadScreenEntry(scopeKey = "subscreen:bypass_charge"),
                contentPadding = PaddingValues(
                    top = topPad,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = bottomPad
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                bypassInfoItem(colorScheme = colorScheme)
                bypassIllustrationItem(
                    colorScheme = colorScheme,
                    illustrationHeight = 200.dp,
                    isUnsupported = isUnsupported
                )
                bypassOptionItems(
                    jumpPulse = jumpPulse,
                    isUnsupported = isUnsupported,
                    isNeedSetup = isNeedSetup,
                    bypassChgState = bypassChgState,
                    onToggle = onToggleBypass,
                    onOpenCheck = { navController.navigate("bypasschg_check") }
                )
            }
        }
    }
}

/** The "pause battery charging" info card. */
private fun LazyListScope.bypassInfoItem(colorScheme: ColorScheme) {
    item {
        ExpressiveList(
            content = listOf(
                {
                    ExpressiveInfoCard(
                        supportingContent = { Text(text = stringResource(R.string.str_pause_battery_charging_when_pl)) },
                        leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                        containerColor = colorScheme.surfaceContainerLow,
                        onClick = {}
                    )
                }
            )
        )
    }
}

/** The illustration. */
private fun LazyListScope.bypassIllustrationItem(
    colorScheme: ColorScheme,
    illustrationHeight: Dp,
    isUnsupported: Boolean
) {
    item {
        Image(
            painter = painterResource(id = R.drawable.bypasschgillust),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(illustrationHeight)
                .clip(azShape(28.dp))
                .graphicsLayer(alpha = if (isUnsupported) 0.5f else 1f),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorScheme.primary)
        )
    }
}

/** The switch, the compatibility-check entry, and the 20% safety floor info card. */
private fun LazyListScope.bypassOptionItems(
    jumpPulse: String?,
    isUnsupported: Boolean,
    isNeedSetup: Boolean,
    bypassChgState: Boolean?,
    onToggle: (Boolean) -> Unit,
    onOpenCheck: () -> Unit
) {
    item {
        if (isNeedSetup) {
            ExpressiveList(
                content = listOf {
                    ExpressiveInfoCard(
                        supportingContent = { Text(text = stringResource(R.string.str_bypass_need_setup)) },
                        leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        onClick = {}
                    )
                }
            )
        } else if (bypassChgState == null) {
            SkeletonSettingsList(rowCount = 1)
        } else {
            ExpressiveList(
                content = listOf {
                    JumpRow(activeId = jumpPulse, entryId = "bypass.enable") {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.BatteryChargingFull,
                            title = stringResource(R.string.enable_bypass_charge),
                            summary = if (isUnsupported) stringResource(R.string.bypass_not_supported)
                                      else stringResource(R.string.enable_bypass_charge_desc),
                            checked = bypassChgState,
                            enabled = !isUnsupported,
                            onCheckedChange = onToggle
                        )
                    }
                }
            )
        }
    }

    item {
        ExpressiveList(
            content = listOf {
                JumpRow(activeId = jumpPulse, entryId = "bypass.compat") {
                    ExpressiveListItem(
                        onClick = onOpenCheck,
                        headlineContent = { Text(stringResource(R.string.CompatibilityCheck)) },
                        supportingContent = { Text(stringResource(R.string.CompatibilityCheck_desc)) },
                        leadingContent = { LeadingIcon(icon = Icons.Filled.CheckCircle) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                    )
                }
            }
        )
    }

    item {
        ExpressiveList(
            content = listOf {
                ExpressiveInfoCard(
                    supportingContent = { Text(text = stringResource(R.string.standby_charge_floor_desc)) },
                    leadingContent = { LeadingIcon(icon = Icons.Rounded.Shield) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = {}
                )
            }
        )
    }
}

@Composable
fun BypassChgTopAppBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .gamepadNotFocusable()
            .appBarFade(colorScheme.surface, scrollBehavior.state.overlappedFraction)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.bcharging),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },       
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
