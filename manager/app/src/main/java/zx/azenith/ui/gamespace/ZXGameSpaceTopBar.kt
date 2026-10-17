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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.viewmodel.ApplistViewmodel

private const val SEARCH_RESULT_CAP = 6

/**
 * ZXGameSpace top navigation bar: "Games | System" on the left; Search, Settings, Profile Avatar with live
 * status dot, Exit, and Clock on the right.
 */
@Composable
internal fun ZXGameSpaceTopBar(
    selectedCategory: TopCategory,
    clock: String,
    battery: Pair<Int, Boolean>,
    isScrolled: Boolean = false,
    onSelectCategory: (TopCategory) -> Unit,
    searchOpen: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onStatusClick: () -> Unit = {},
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Segment: Category Navigation Tabs & Scrolled Wordmark
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val density = LocalDensity.current
            var tabBounds by remember { mutableStateOf(mapOf<TopCategory, Pair<Float, Float>>()) }
            val currentBounds = tabBounds[selectedCategory] ?: (0f to 0f)

            val animatedPillX by animateFloatAsState(
                targetValue = currentBounds.first,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "TopBarPillX"
            )
            val animatedPillWidth by animateFloatAsState(
                targetValue = currentBounds.second,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "TopBarPillWidth"
            )

            Box(contentAlignment = Alignment.CenterStart) {
                if (animatedPillWidth > 0f) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier
                            .offset { IntOffset(animatedPillX.roundToInt(), 0) }
                            .width(with(density) { animatedPillWidth.toDp() })
                            .height(36.dp)
                    ) {}
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopCategory.entries.forEach { tab ->
                        val selected = tab == selectedCategory
                        val shape = RoundedCornerShape(14.dp)
                        Surface(
                            shape = shape,
                            color = Color.Transparent,
                            contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(shape)
                                .onGloballyPositioned { coordinates ->
                                    val parentX = coordinates.positionInParent().x
                                    val width = coordinates.size.width.toFloat()
                                    if (tabBounds[tab]?.first != parentX || tabBounds[tab]?.second != width) {
                                        tabBounds = tabBounds + (tab to (parentX to width))
                                    }
                                }
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clickable { onSelectCategory(tab) }
                        ) {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right Segment: Morphing Search Bar, Control Actions & Unified Status Capsule
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val searchRequester = remember { FocusRequester() }
            val searchWidth by animateDpAsState(
                targetValue = if (searchOpen) 268.dp else 38.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "SearchBarWidth"
            )
            val searchBg by animateColorAsState(
                targetValue = if (searchOpen) {
                    scheme.surfaceContainerHighest
                } else {
                    scheme.surfaceContainerHigh.copy(alpha = 0.85f)
                },
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "SearchBarBg"
            )

            LaunchedEffect(searchOpen) {
                if (searchOpen) {
                    runCatching { searchRequester.requestFocus() }
                }
            }

            Surface(
                shape = CircleShape,
                color = searchBg,
                contentColor = scheme.onSurfaceVariant,
                modifier = Modifier
                    .width(searchWidth)
                    .height(38.dp)
                    .clip(CircleShape)
                    .gamepadFocusOutline(shape = CircleShape, register = !searchOpen)
                    .then(
                        if (!searchOpen) {
                            Modifier.clickable(onClick = onSearchClick)
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (searchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = scheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(scheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchRequester),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_apps),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { onSearchQueryChange("") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceContainerHigh,
                            contentColor = scheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSearchClose)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.gamespace_search_close),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.search_apps),
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            TopBarIconButton(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = stringResource(R.string.gamespace_exit_title),
                tint = scheme.error,
                onClick = onExit
            )

            // Material 3 Expressive Unified System Status Capsule (Clock & Battery)
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .gamepadFocusOutline(shape = CircleShape, register = true)
                    .clickable(onClick = onStatusClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = clock,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = scheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(scheme.outlineVariant.copy(alpha = 0.7f))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isCharging = battery.second
                        val isLow = battery.first <= 20
                        val batteryColor = when {
                            isCharging -> scheme.primary
                            isLow -> scheme.error
                            else -> scheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = when {
                                isCharging -> Icons.Rounded.BatteryChargingFull
                                isLow -> Icons.Rounded.BatteryAlert
                                else -> Icons.Rounded.BatteryStd
                            },
                            contentDescription = stringResource(R.string.gamespace_info_battery),
                            tint = batteryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${battery.first}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = batteryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = tint,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .gamepadFocusOutline(shape = CircleShape, register = true)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Leave prompt for the power button: close the shell and stay on AZenith, or close both. Backing
 * out of the dialog leaves the two alone.
 *
 * A dialog is its own window, so it carries [gamepadKeyWatch] — the activity never sees its keys.
 */
@Composable
internal fun ZXGameSpaceExitDialog(
    onDismiss: () -> Unit,
    onExitShell: () -> Unit,
    onExitApp: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val defaultRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { defaultRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(min = 460.dp, max = 520.dp)
                .padding(20.dp)
                .gamepadKeyWatch()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Disc & Title
                Surface(
                    shape = CircleShape,
                    color = scheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            tint = scheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.gamespace_exit_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Action Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Option 1: Return to AZenith
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .focusRequester(defaultRequester)
                            .gamepadFocusOutline(shape = RoundedCornerShape(20.dp), register = true)
                            .clickable(onClick = onExitShell)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.gamespace_exit_shell),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.gamespace_exit_shell_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Quit AZenith Completely
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .gamepadFocusOutline(shape = RoundedCornerShape(20.dp), register = true)
                            .clickable(onClick = onExitApp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = scheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.gamespace_exit_app),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.error
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.gamespace_exit_app_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Cancel Pill Button
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .gamepadFocusOutline(shape = RoundedCornerShape(percent = 50), register = true)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun ZXGameSpaceSearchOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val motion = MaterialTheme.motionScheme

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight / 6 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight / 8 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
internal fun ZXGameSpaceSearchPane(
    query: String,
    apps: List<ApplistViewmodel.AppInfo>,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onOpenSettings: (ApplistViewmodel.AppInfo) -> Unit,
    onNavigateToSettingsTab: (SettingsSidebarTab) -> Unit,
    onClose: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var scopeFilter by remember { mutableStateOf(SearchScopeFilter.All) }

    val resolvedSettings = remember(context) {
        SETTINGS_SEARCH_ITEMS.map { item ->
            val title = context.getString(item.titleRes)
            val summary = item.summaryRes?.let { context.getString(it) } ?: item.summaryText ?: ""
            ResolvedSettingItem(
                title = title,
                summary = summary,
                tab = item.tab,
                icon = item.icon
            )
        }
    }

    val matchedApps = remember(query, apps, scopeFilter) {
        val base = when (scopeFilter) {
            SearchScopeFilter.Games -> apps.filter { it.isRecommended || it.isEnabledInConfig }
            SearchScopeFilter.Tuned -> apps.filter { it.isEnabledInConfig }
            SearchScopeFilter.Settings -> emptyList()
            SearchScopeFilter.All -> apps
        }
        if (query.isBlank()) {
            base.sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> { it.isEnabledInConfig }
                    .thenByDescending { it.isRecommended }
                    .thenBy { it.label.lowercase() }
            )
        } else {
            base.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }.sortedWith(
                compareByDescending<ApplistViewmodel.AppInfo> {
                    it.label.equals(query, ignoreCase = true)
                }.thenByDescending { it.label.startsWith(query, ignoreCase = true) }
                    .thenByDescending { it.isEnabledInConfig }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    val matchedSettings = remember(query, resolvedSettings, scopeFilter) {
        if (scopeFilter == SearchScopeFilter.Games || scopeFilter == SearchScopeFilter.Tuned) {
            emptyList()
        } else if (query.isBlank()) {
            resolvedSettings.take(SEARCH_RESULT_CAP)
        } else {
            resolvedSettings.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shadowElevation = 24.dp,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, bottom = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Scope Filter Pills and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(SearchScopeFilter.entries) { filter ->
                        val selected = scopeFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { scopeFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = scheme.surfaceContainerLow,
                                selectedContainerColor = scheme.primaryContainer,
                                labelColor = scheme.onSurfaceVariant,
                                selectedLabelColor = scheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = scheme.outlineVariant.copy(alpha = 0.35f),
                                selectedBorderColor = scheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.gamespace_search_close),
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Results Container
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Section 1: Matching Games / Apps
                if (matchedApps.isNotEmpty()) {
                    item(key = "header_apps") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Apps & Games (${matchedApps.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.primary
                            )
                        }
                    }

                    val appChunks = matchedApps.chunked(2)
                    items(appChunks, key = { chunk -> chunk.joinToString { it.packageName } }) { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowApps.forEach { app ->
                                val shape = RoundedCornerShape(18.dp)
                                Surface(
                                    shape = shape,
                                    color = scheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .gamepadFocusOutline(shape = shape, register = true)
                                        .clip(shape)
                                        .clickable { onLaunch(app) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AppIconImage(
                                            app = app,
                                            size = 46.dp
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = scheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (app.isEnabledInConfig) "Tuned by AZenith" else app.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (app.isEnabledInConfig) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .clickable { onOpenSettings(app) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Tune,
                                                    contentDescription = "Tune",
                                                    tint = scheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowApps.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Section 2: Matching Settings & Tweak Shortcuts
                if (matchedSettings.isNotEmpty()) {
                    item(key = "header_settings") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Settings & Tweaks (${matchedSettings.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.secondary
                            )
                        }
                    }

                    val settingChunks = matchedSettings.chunked(2)
                    items(settingChunks, key = { chunk -> chunk.joinToString { it.title } }) { rowSettings ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowSettings.forEach { setting ->
                                val shape = RoundedCornerShape(18.dp)
                                Surface(
                                    shape = shape,
                                    color = scheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(68.dp)
                                        .gamepadFocusOutline(shape = shape, register = true)
                                        .clip(shape)
                                        .clickable { onNavigateToSettingsTab(setting.tab) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = scheme.secondaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = setting.icon,
                                                    contentDescription = null,
                                                    tint = scheme.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = setting.title,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = scheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (setting.summary.isNotBlank()) {
                                                Text(
                                                    text = setting.summary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = scheme.primaryContainer
                                        ) {
                                            Text(
                                                text = stringResource(setting.tab.labelRes),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = scheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowSettings.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
