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

package zx.azenith.ui.gamespace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.settings.SettingsAboutPane
import zx.azenith.ui.gamespace.settings.SettingsAppearancePane
import zx.azenith.ui.gamespace.settings.SettingsAudioPane
import zx.azenith.ui.gamespace.settings.SettingsGeneralPane
import zx.azenith.ui.gamespace.settings.SettingsTweaksPane
import zx.azenith.ui.util.AudioUtil

/**
 * Fun spring overshoot bounce modifier for buttons and cards.
 * Scales down slightly on press and releases with a lively overshoot rebound.
 */
@Composable
internal fun Modifier.springPressBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f
): Modifier {
    if (!enabled) return this
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch {
                    scaleAnim.animateTo(
                        targetValue = pressedScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                val upOrCancel = waitForUpOrCancellation()
                scope.launch {
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }
}

/**
 * Adaptive multi-column layout for setting items.
 * Switches between 1, 2, or 3 columns based on measured available stage width.
 * Uses row-aligned chunking so cards stay strictly symmetrical without vertical drift.
 */
@Composable
internal fun AdaptiveSettingsGrid(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    minColumnWidth: Dp = 340.dp,
    spacing: Dp = 10.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val columns = when {
            availableWidth < minColumnWidth * 2 + spacing -> 1
            availableWidth < minColumnWidth * 3 + spacing * 2 -> 2
            else -> 3
        }

        val rows = remember(items, columns) { items.chunked(columns) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            rows.forEach { rowItems ->
                if (columns == 1) {
                    rowItems.first().invoke()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                item()
                            }
                        }
                        if (rowItems.size < columns) {
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ZXGameSpaceSettingsStage(
    selectedTab: SettingsSidebarTab = SettingsSidebarTab.General,
    onSelectTab: (SettingsSidebarTab) -> Unit = {},
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    deviceName: String = "",
    chipsetName: String = "",
    onApplyProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier) {
        val isCompactLandscape = maxWidth < 920.dp
        val sidebarWidth = if (isCompactLandscape) 78.dp else 220.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompactLandscape) 8.dp else 12.dp),
                    horizontalAlignment = if (isCompactLandscape) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isCompactLandscape) {
                        Text(
                            text = stringResource(R.string.settings).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = scheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }

                    val sidebarScrollState = rememberScrollState()
                    var sidebarHeightPx by remember { mutableIntStateOf(0) }
                    var tabPositions by remember { mutableStateOf(mapOf<SettingsSidebarTab, Pair<Float, Float>>()) }
                    val currentPos = tabPositions[selectedTab] ?: (0f to 0f)

                    LaunchedEffect(selectedTab, currentPos.first, currentPos.second, sidebarHeightPx) {
                        if (currentPos.second > 0f && sidebarHeightPx > 0) {
                            val targetTop = currentPos.first.roundToInt()
                            val targetBottom = (currentPos.first + currentPos.second).roundToInt()
                            val currentScroll = sidebarScrollState.value
                            if (targetTop < currentScroll) {
                                sidebarScrollState.animateScrollTo(targetTop)
                            } else if (targetBottom > currentScroll + sidebarHeightPx) {
                                sidebarScrollState.animateScrollTo((targetBottom - sidebarHeightPx).coerceAtLeast(0))
                            }
                        }
                    }

                    val animatedY by animateFloatAsState(
                        targetValue = currentPos.first,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "SidebarChipY"
                    )
                    val animatedHeight by animateFloatAsState(
                        targetValue = currentPos.second,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "SidebarChipHeight"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onGloballyPositioned { sidebarHeightPx = it.size.height }
                            .verticalScroll(sidebarScrollState)
                    ) {
                        if (animatedHeight > 0f) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = scheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(0, animatedY.roundToInt()) }
                                    .height(with(density) { animatedHeight.toDp() })
                            ) {}
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalAlignment = if (isCompactLandscape) Alignment.CenterHorizontally else Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsSidebarTab.entries.forEach { tab ->
                                val isSelected = tab == selectedTab
                                val itemShape = RoundedCornerShape(16.dp)
                                val contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant

                                Surface(
                                    shape = itemShape,
                                    color = Color.Transparent,
                                    contentColor = contentColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coordinates ->
                                            val parentY = coordinates.positionInParent().y
                                            val height = coordinates.size.height.toFloat()
                                            if (tabPositions[tab]?.first != parentY || tabPositions[tab]?.second != height) {
                                                tabPositions = tabPositions + (tab to (parentY to height))
                                            }
                                        }
                                        .gamepadFocusOutline(shape = itemShape, register = true)
                                        .clip(itemShape)
                                        .springPressBounce(pressedScale = 0.94f)
                                        .clickable {
                                            AudioUtil.playOk(context)
                                            onSelectTab(tab)
                                        }
                                ) {
                                    if (isCompactLandscape) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes),
                                                tint = contentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerLow.copy(alpha = 0.72f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.94f))
                            .togetherWith(fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 1.02f))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "SettingsPaneTransition"
                ) { tab ->
                    when (tab) {
                        SettingsSidebarTab.General -> SettingsGeneralPane()
                        SettingsSidebarTab.Tweaks -> SettingsTweaksPane(
                            profile = profile,
                            profileValue = profileValue,
                            refreshHz = refreshHz,
                            onApplyProfile = onApplyProfile
                        )
                        SettingsSidebarTab.Appearance -> SettingsAppearancePane()
                        SettingsSidebarTab.Audio -> SettingsAudioPane()
                        SettingsSidebarTab.About -> SettingsAboutPane(
                            deviceName = deviceName,
                            chipsetName = chipsetName
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    val iconBoxShape = if (checked) {
        RoundedCornerShape(12.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 8.dp)
    }

    val iconBgColor by animateColorAsState(
        targetValue = if (checked) scheme.primaryContainer else scheme.surfaceContainerHighest,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "SwitchCardIconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (checked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "SwitchCardIconTint"
    )

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .springPressBounce(pressedScale = 0.97f)
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable {
                val newVal = !checked
                if (newVal) {
                    AudioUtil.playOk(context)
                } else {
                    AudioUtil.playCancel(context)
                }
                onCheckedChange(newVal)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = iconBoxShape,
                color = iconBgColor,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            GameSpaceSwitch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

/** Material 3 Switch with checkmark and X icon spring morphing in the thumb for GameSpace. */
@Composable
internal fun GameSpaceSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val handleCheckedChange: ((Boolean) -> Unit)? = if (onCheckedChange != null) {
        { newVal ->
            if (newVal) {
                AudioUtil.playOk(context)
            } else {
                AudioUtil.playCancel(context)
            }
            onCheckedChange(newVal)
        }
    } else null

    Switch(
        checked = checked,
        onCheckedChange = handleCheckedChange,
        enabled = enabled,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    (fadeIn(spring(dampingRatio = 0.6f, stiffness = 600f)) +
                        scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialScale = 0.4f))
                        .togetherWith(
                            fadeOut(spring(dampingRatio = 0.6f, stiffness = 600f)) +
                                scaleOut(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), targetScale = 0.4f)
                        )
                },
                label = "GameSpaceSwitchThumb"
            ) { isChecked ->
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (enabled) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = scheme.surfaceContainerHighest,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailingBadge: String? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val iconShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .springPressBounce(pressedScale = 0.97f)
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable {
                AudioUtil.playOk(context)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = iconShape,
                color = scheme.secondaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Tonal badge over a headline: resting empty state. */
@Composable
internal fun EmptyPane(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp)
            )
        }
    }
}
