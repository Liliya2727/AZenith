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

package zx.azenith.ui.gamespace

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.ExpressiveDropdownItem
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveSliderItem
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.subscreens.getAppDetails
import zx.azenith.ui.subscreens.isGameCategory
import zx.azenith.ui.util.AppConfig
import zx.azenith.ui.util.DebugUtils
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.getSupportedDownscaleFactors
import zx.azenith.ui.util.getSupportedFpsTargets
import zx.azenith.ui.util.getSupportedRefreshRates
import zx.azenith.ui.viewmodel.AppSettingsViewModel
import zx.azenith.ui.viewmodel.ApplistViewmodel

/**
 * Game Space App Tuning Sheet: slides up from below with Material 3 bouncy expressive physics over
 * a Haze-blurred backdrop, styled natively in the two-pane Game Space layout.
 */
@Composable
internal fun ZXGameSpaceAppSettingsOverlay(
    visible: Boolean,
    packageName: String?,
    allApps: List<ApplistViewmodel.AppInfo>,
    hazeState: HazeState,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onClose: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    var activePkg by remember { mutableStateOf(packageName) }
    if (packageName != null) {
        activePkg = packageName
    }

    LaunchedEffect(activePkg) {
        if (activePkg != null) {
            viewModel.loadConfig()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = motion.defaultEffectsSpec()),
        modifier = Modifier.fillMaxSize()
    ) {
        val pkg = activePkg ?: return@AnimatedVisibility
        val resolvedApp = remember(pkg, allApps) {
            allApps.find { it.packageName == pkg }
        }
        val appDetails = remember(pkg) { getAppDetails(context, pkg) }
        val isGameApp = remember(pkg) { isGameCategory(context, pkg) }
        val config = viewModel.fullConfig[pkg]
        var localMasterOn by remember(pkg, config != null) { mutableStateOf(config != null) }

        var isFullModeEnabled by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isFullModeEnabled = DebugUtils.isFullModeEnabled()
        }
        var bypassSupported by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            bypassSupported = withContext(Dispatchers.IO) {
                val path = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "")
                path != "UNSUPPORTED" && path != "NEED_SETUP"
            }
        }

        val booleanModes = listOf(
            stringResource(R.string.default_label),
            stringResource(R.string.on_label),
            stringResource(R.string.off_label)
        )
        val rendererModes = listOf(
            stringResource(R.string.Renderer_Default),
            stringResource(R.string.renderer_skia_vk),
            stringResource(R.string.renderer_skia_vk_threaded),
            stringResource(R.string.renderer_skiagl),
            stringResource(R.string.renderer_skiagl_threaded),
            stringResource(R.string.renderer_opengl_es),
            stringResource(R.string.renderer_opengl_es_threaded),
            stringResource(R.string.Renderer_Vulkan)
        )
        val rendererValues = listOf(
            "default",
            "skiavk",
            "skiavkthreaded",
            "skiagl",
            "skiaglthreaded",
            "opengl",
            "openglthreaded",
            "vulkan"
        )
        val defaultLabel = stringResource(R.string.default_label)
        val rawRefreshModes = remember { getSupportedRefreshRates(context) }
        val rawDownscaleSteps = remember { getSupportedDownscaleFactors() }
        val rawFpsSteps = remember { getSupportedFpsTargets(context) }
        val downscaleDisplaySteps = remember(rawDownscaleSteps, defaultLabel) {
            rawDownscaleSteps.map { if (it == "default") defaultLabel else it }
        }
        val fpsDisplaySteps = remember(rawFpsSteps, defaultLabel) {
            rawFpsSteps.map { if (it == "default") defaultLabel else it }
        }
        val dynamicRefreshDisplayModes = remember(rawRefreshModes, defaultLabel) {
            rawRefreshModes.map { mode ->
                if (mode.equals("default", ignoreCase = true)) defaultLabel else mode
            }
        }
        fun getBoolIndex(v: String?): Int = when (v) {
            "true" -> 1
            "false" -> 2
            else -> 0
        }

        val insets = WindowInsets.displayCutout.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val startPad = maxOf(24.dp, insets.calculateStartPadding(layoutDirection))
        val endPad = maxOf(24.dp, insets.calculateEndPadding(layoutDirection))
        val topPad = maxOf(18.dp, insets.calculateTopPadding())
        val bottomPad = maxOf(18.dp, insets.calculateBottomPadding())

        CompositionLocalProvider(
            LocalContentColor provides scheme.onSurface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeBlurStyle.Material3(
                            containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.62f)
                        ) {
                            blurRadius(28.dp)
                        }
                    )
                    .padding(start = startPad, end = endPad, top = topPad, bottom = bottomPad)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left GameSpace Identity Sidebar
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = scheme.surfaceContainerLow.copy(alpha = 0.90f),
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .width(264.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TopBarIconButton(
                                            icon = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.cd_back),
                                            onClick = onClose
                                        )
                                        Text(
                                            text = stringResource(R.string.app_settings_title),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = scheme.onSurface
                                        )
                                    }
                                }

                                // App identity card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = scheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (resolvedApp != null) {
                                                AppIconImage(app = resolvedApp, size = 48.dp)
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = scheme.primaryContainer,
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Android,
                                                            contentDescription = null,
                                                            tint = scheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = resolvedApp?.label ?: appDetails.first,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = scheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = pkg,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = scheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = scheme.secondaryContainer,
                                                contentColor = scheme.onSecondaryContainer
                                            ) {
                                                Text(
                                                    text = "v${appDetails.third}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = if (localMasterOn) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                                contentColor = if (localMasterOn) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                                            ) {
                                                Text(
                                                    text = if (localMasterOn) {
                                                        stringResource(R.string.gamespace_tuned)
                                                    } else {
                                                        stringResource(R.string.gamespace_untuned)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Master Switch Card
                                val masterShape = RoundedCornerShape(18.dp)
                                Surface(
                                    shape = masterShape,
                                    color = if (localMasterOn) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                    contentColor = if (localMasterOn) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .gamepadFocusOutline(shape = masterShape, register = true)
                                        .clip(masterShape)
                                        .clickable {
                                            val next = !localMasterOn
                                            localMasterOn = next
                                            viewModel.toggleMasterSwitch(pkg, next)
                                            ApplistViewmodel.apps = ApplistViewmodel.apps.map {
                                                if (it.packageName == pkg) it.copy(isEnabledInConfig = next) else it
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PowerSettingsNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.master_switch),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (localMasterOn) "Active profile hooks" else "Tap to enable tuning",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (localMasterOn) {
                                                    scheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    scheme.onSurfaceVariant
                                                },
                                                maxLines = 1
                                            )
                                        }
                                        Switch(
                                            checked = localMasterOn,
                                            onCheckedChange = { isChecked ->
                                                localMasterOn = isChecked
                                                viewModel.toggleMasterSwitch(pkg, isChecked)
                                                ApplistViewmodel.apps = ApplistViewmodel.apps.map {
                                                    if (it.packageName == pkg) it.copy(isEnabledInConfig = isChecked) else it
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Bottom Quick Actions: Launch & System Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val launchShape = RoundedCornerShape(16.dp)
                                Surface(
                                    shape = launchShape,
                                    color = scheme.primary,
                                    contentColor = scheme.onPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .gamepadFocusOutline(shape = launchShape, register = true)
                                        .clip(launchShape)
                                        .clickable {
                                            if (resolvedApp != null) {
                                                onLaunch(resolvedApp)
                                            } else {
                                                launchGame(context, pkg)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.gamespace_play_game),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                val infoShape = RoundedCornerShape(16.dp)
                                Surface(
                                    shape = infoShape,
                                    color = scheme.secondaryContainer,
                                    contentColor = scheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .gamepadFocusOutline(shape = infoShape, register = true)
                                        .clip(infoShape)
                                        .clickable {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:$pkg")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            runCatching { context.startActivity(intent) }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = stringResource(R.string.str_app_info),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right GameSpace Tuning Stage
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = scheme.surfaceContainerLow.copy(alpha = 0.85f),
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AnimatedContent(
                            targetState = localMasterOn,
                            transitionSpec = {
                                (fadeIn(motion.defaultEffectsSpec()) + scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialScale = 0.96f
                                )).togetherWith(
                                    fadeOut(motion.defaultEffectsSpec()) + scaleOut(targetScale = 0.98f)
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "AppTuningMasterContent"
                        ) { enabled ->
                            if (!enabled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.secondaryContainer,
                                            contentColor = scheme.onSecondaryContainer,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Tune,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.master_switch),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.master_switch_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.widthIn(max = 360.dp)
                                        )
                                    }
                                }
                            } else {
                                val displayConfig = config ?: AppConfig()
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.section_performance).uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = scheme.primary
                                        )
                                    }
                                    item {
                                        ExpressiveList(
                                            content = listOf {
                                                ExpressiveDropdownItem(
                                                    icon = Icons.Rounded.Speed,
                                                    title = stringResource(R.string.perf_lite_mode),
                                                    summary = stringResource(R.string.perf_lite_mode_desc_short),
                                                    items = booleanModes,
                                                    selectedIndex = getBoolIndex(displayConfig.perf_lite_mode),
                                                    onItemSelected = { index ->
                                                        val value = listOf("default", "true", "false")[index]
                                                        viewModel.updateSetting(pkg, "perf_lite_mode", value)
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    item {
                                        Text(
                                            text = stringResource(R.string.section_additionalsettings).uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = scheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    item {
                                        ExpressiveList(
                                            content = buildList {
                                                if (bypassSupported) {
                                                    add {
                                                        ExpressiveDropdownItem(
                                                            icon = Icons.Rounded.Cable,
                                                            title = stringResource(R.string.bcharging),
                                                            summary = stringResource(R.string.enable_bypass_charge_desc),
                                                            items = booleanModes,
                                                            selectedIndex = getBoolIndex(displayConfig.bypass_charging),
                                                            onItemSelected = { index ->
                                                                val value = listOf("default", "true", "false")[index]
                                                                viewModel.updateSetting(pkg, "bypass_charging", value)
                                                            }
                                                        )
                                                    }
                                                }
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.RocketLaunch,
                                                        title = stringResource(R.string.game_preload),
                                                        summary = stringResource(R.string.game_preload_desc),
                                                        items = booleanModes,
                                                        selectedIndex = getBoolIndex(displayConfig.game_preload),
                                                        onItemSelected = { index ->
                                                            val value = listOf("default", "true", "false")[index]
                                                            viewModel.updateSetting(pkg, "game_preload", value)
                                                        }
                                                    )
                                                }
                                                if (isFullModeEnabled) {
                                                    add {
                                                        ExpressiveDropdownItem(
                                                            icon = Icons.Rounded.SwapVerticalCircle,
                                                            title = stringResource(R.string.app_priority),
                                                            summary = stringResource(R.string.app_priority_desc),
                                                            items = booleanModes,
                                                            selectedIndex = getBoolIndex(displayConfig.app_priority),
                                                            onItemSelected = { index ->
                                                                val value = listOf("default", "true", "false")[index]
                                                                viewModel.updateSetting(pkg, "app_priority", value)
                                                            }
                                                        )
                                                    }
                                                }
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.DoNotDisturbOn,
                                                        title = stringResource(R.string.dnd_mode),
                                                        summary = stringResource(R.string.dnd_mode_desc),
                                                        items = booleanModes,
                                                        selectedIndex = getBoolIndex(displayConfig.dnd_on_gaming),
                                                        onItemSelected = { index ->
                                                            val value = listOf("default", "true", "false")[index]
                                                            viewModel.updateSetting(pkg, "dnd_on_gaming", value)
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    item {
                                        Text(
                                            text = stringResource(R.string.section_display_render_settings).uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            ),
                                            color = scheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    item {
                                        ExpressiveList(
                                            content = buildList {
                                                if (isFullModeEnabled) {
                                                    add {
                                                        ExpressiveDropdownItem(
                                                            icon = Icons.Rounded.WebStories,
                                                            title = stringResource(R.string.refreshrates),
                                                            summary = stringResource(R.string.refreshrates_desc),
                                                            items = dynamicRefreshDisplayModes,
                                                            selectedIndex = rawRefreshModes.indexOfFirst {
                                                                it.equals(displayConfig.refresh_rate, ignoreCase = true)
                                                            }.coerceAtLeast(0),
                                                            onItemSelected = { index ->
                                                                val value = rawRefreshModes[index]
                                                                viewModel.updateSetting(pkg, "refresh_rate", value)
                                                            }
                                                        )
                                                    }
                                                }
                                                add {
                                                    ExpressiveDropdownItem(
                                                        icon = Icons.Rounded.Layers,
                                                        title = stringResource(R.string.renderengine),
                                                        summary = stringResource(R.string.renderengine_desc),
                                                        items = rendererModes,
                                                        selectedIndex = rendererValues.indexOfFirst {
                                                            it.equals(displayConfig.renderer, ignoreCase = true)
                                                        }.coerceAtLeast(0),
                                                        onItemSelected = { index ->
                                                            val value = rendererValues[index]
                                                            viewModel.updateSetting(pkg, "renderer", value)
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    if (isGameApp) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.section_resolution_settings).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp
                                                ),
                                                color = scheme.primary,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        item {
                                            ExpressiveList(
                                                content = buildList {
                                                    add {
                                                        val currentDownscaleIndex = rawDownscaleSteps
                                                            .indexOf(displayConfig.resolution_downscale)
                                                            .coerceAtLeast(0)
                                                        var downscaleSliderPos by remember(displayConfig.resolution_downscale) {
                                                            mutableFloatStateOf(currentDownscaleIndex.toFloat())
                                                        }
                                                        ExpressiveSliderItem(
                                                            icon = Icons.Rounded.AspectRatio,
                                                            title = stringResource(R.string.resolution_downscale_title),
                                                            subtitle = stringResource(R.string.resolution_downscale_desc),
                                                            badgeText = downscaleDisplaySteps.getOrElse(downscaleSliderPos.toInt()) { defaultLabel },
                                                            sliderPosition = downscaleSliderPos,
                                                            valueRange = 0f..(rawDownscaleSteps.size - 1).toFloat(),
                                                            steps = rawDownscaleSteps.size - 2,
                                                            onValueChange = { downscaleSliderPos = it },
                                                            onValueChangeFinished = {
                                                                val value = rawDownscaleSteps[downscaleSliderPos.toInt()]
                                                                viewModel.updateSetting(pkg, "resolution_downscale", value)
                                                            }
                                                        )
                                                    }
                                                    add {
                                                        val currentFpsIndex = rawFpsSteps
                                                            .indexOf(displayConfig.resolution_fps)
                                                            .coerceAtLeast(0)
                                                        var fpsSliderPos by remember(displayConfig.resolution_fps) {
                                                            mutableFloatStateOf(currentFpsIndex.toFloat())
                                                        }
                                                        val downscaleIsOff = displayConfig.resolution_downscale == "default"
                                                        ExpressiveSliderItem(
                                                            icon = Icons.Rounded.Speed,
                                                            title = stringResource(R.string.resolution_fps_target_title),
                                                            subtitle = stringResource(R.string.resolution_fps_target_desc),
                                                            badgeText = "${fpsDisplaySteps.getOrElse(fpsSliderPos.toInt()) { "60" }}fps",
                                                            sliderPosition = fpsSliderPos,
                                                            valueRange = 0f..(rawFpsSteps.size - 1).toFloat(),
                                                            steps = rawFpsSteps.size - 2,
                                                            enabled = !downscaleIsOff,
                                                            onValueChange = { fpsSliderPos = it },
                                                            onValueChangeFinished = {
                                                                val value = rawFpsSteps[fpsSliderPos.toInt()]
                                                                viewModel.updateSetting(pkg, "resolution_fps", value)
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
