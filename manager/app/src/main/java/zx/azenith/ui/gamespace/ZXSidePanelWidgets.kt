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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package zx.azenith.ui.gamespace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SidePanelWidgetContent(
    widgetId: String,
    state: ZXSidePanelStateHolder,
    isMerged: Boolean = false
) {
    when (widgetId) {
        "fpsHeader" -> FpsMeterWidget(state, isMerged)
        "speedometers" -> SpeedometersWidget(state, isMerged)
        "profileSwitch" -> ProfileSegmentedWidget(state, isMerged)
        "brightnessSlider" -> BrightnessSliderWidget(state, isMerged)
        "volumeSlider" -> VolumeSliderWidget(state, isMerged)
        "quickTools" -> QuickToolsWidget(state)
        "antiInterference" -> AntiInterferenceWidget(state)
        "crosshair" -> CrosshairWidget(state)
        "displayTouch" -> DisplayTouchWidget(state)
        "networkPing" -> NetworkPingWidget(state)
        "thermalBattery" -> ThermalBatteryWidget()
        "voiceMacro" -> VoiceMacroWidget(state)
    }
}

@Composable
fun EditModeWidgetCard(
    widgetId: String,
    index: Int,
    totalCount: Int,
    state: ZXSidePanelStateHolder,
    modifier: Modifier = Modifier
) {
    val meta = state.widgetCatalog[widgetId] ?: return
    val scheme = MaterialTheme.colorScheme
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "EditWidgetScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.65f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "EditWidgetAlpha"
    )

    M3WidgetCard(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.graphicsLayer {
            scaleX = cardScale
            scaleY = cardScale
            alpha = cardAlpha
        }
    ) {
        // Edit Bar Header
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(widgetId, index, totalCount) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragAccumulatedY = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            dragAccumulatedY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragAccumulatedY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulatedY += dragAmount
                            val stepThreshold = 76.dp.toPx()
                            if (dragAccumulatedY > stepThreshold && index < totalCount - 1) {
                                state.moveActiveWidget(index, index + 1)
                                dragAccumulatedY = 0f
                            } else if (dragAccumulatedY < -stepThreshold && index > 0) {
                                state.moveActiveWidget(index, index - 1)
                                dragAccumulatedY = 0f
                            }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("drag_indicator"),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = meta.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { state.removeWidgetToLibrary(widgetId) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("remove"),
                            contentDescription = "Remove widget",
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Disabled Inner Widget Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.9f }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            SidePanelWidgetContent(
                widgetId = widgetId,
                state = state,
                isMerged = false
            )
        }
    }
}

@Composable
private fun FpsMeterWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val inner: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isMerged) Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
                    else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("speed"),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Frame rate",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(
                    text = "${state.currentFps}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
            }
        }
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun SpeedometersWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val inner: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            M3ExpressiveRadialGauge(
                percent = state.cpuLoad,
                label = "CPU",
                activeColor = scheme.primary,
                badgeContainerColor = scheme.primaryContainer,
                badgeContentColor = scheme.onPrimaryContainer,
                onClick = { state.nudgeGaugeLoad("cpu") },
                modifier = Modifier.weight(1f)
            )

            M3ExpressiveRadialGauge(
                percent = state.gpuLoad,
                label = "GPU",
                activeColor = scheme.tertiary,
                badgeContainerColor = scheme.tertiaryContainer,
                badgeContentColor = scheme.onTertiaryContainer,
                onClick = { state.nudgeGaugeLoad("gpu") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(14.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun ProfileSegmentedWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val profiles = remember {
        listOf(
            "saving" to "Power saving",
            "balanced" to "Balanced",
            "performance" to "Performance"
        )
    }

    val inner: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            profiles.forEach { (id, label) ->
                val isSelected = state.activeProfile == id
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 250),
                    label = "ProfileContainerColor"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "ProfileContentColor"
                )
                val shape = RoundedCornerShape(16.dp)

                Surface(
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .clickable { state.selectPerformanceProfile(id) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(8.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun BrightnessSliderWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("light_mode"),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${state.brightness}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }

            M3ExpressiveLinearSlider(
                value = state.brightness.toFloat(),
                min = 10f,
                max = 100f,
                step = 1f,
                variant = M3SliderVariant.Primary,
                onValueChange = { state.updateHorizontalBrightness(it.roundToInt()) }
            )
        }
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun VolumeSliderWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val volumeIconName = when {
        state.muted -> "volume_off"
        state.volume < 50 -> "volume_down"
        else -> "volume_up"
    }

    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.toggleMuteAudio() }
                    )
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get(volumeIconName),
                        contentDescription = null,
                        tint = scheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${state.volume}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.secondary
                )
            }

            M3ExpressiveLinearSlider(
                value = state.volume.toFloat(),
                min = 0f,
                max = 100f,
                step = 1f,
                variant = M3SliderVariant.Secondary,
                onValueChange = { state.updateHorizontalVolume(it.roundToInt()) }
            )
        }
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun QuickToolsWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    M3WidgetCard(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Quick access tools",
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.quickTools.forEach { tool ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = when {
                        isPressed -> 0.96f
                        tool.active -> 1.03f
                        else -> 1f
                    },
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "QuickToolScale"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (tool.active) scheme.primaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 250),
                    label = "QuickToolContainer"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (tool.active) scheme.onPrimaryContainer else scheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "QuickToolContent"
                )
                val shape = RoundedCornerShape(20.dp)

                Surface(
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(shape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(),
                            onClick = { state.triggerQuickTool(tool.id) }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get(tool.icon),
                            contentDescription = tool.name,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tool.name.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AntiInterferenceWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    M3WidgetCard(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Anti-interference",
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.antiInterference.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        M3ToggleTile(
                            active = item.active,
                            onClick = { state.toggleAntiInterference(item.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = ZXSidePanelIcons.get(item.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (item.active) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get("check_circle"),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .graphicsLayer { alpha = 0.8f }
                                        )
                                    }
                                }
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun CrosshairWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val styles = remember {
        listOf(
            "dot" to "Dot",
            "cross" to "Cross",
            "circle" to "Circle",
            "chevron" to "Angle"
        )
    }

    M3WidgetCard(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.openSubscreen("crosshair") }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("my_location"),
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Crosshair overlay",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                M3ExpressiveSwitch(
                    checked = state.crosshair.enabled,
                    onCheckedChange = { state.toggleCrosshairQuick() }
                )

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { state.openSubscreen("crosshair") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("chevron_right"),
                            contentDescription = "Open advanced crosshair settings",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            styles.forEach { (id, label) ->
                val isSelected = state.crosshair.style == id
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "CrosshairStyleScale"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 250),
                    label = "CrosshairStyleContainer"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "CrosshairStyleContent"
                )
                val shape = RoundedCornerShape(16.dp)

                Surface(
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(shape)
                        .clickable { state.setCrosshairStyle(id) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayTouchWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val filters = remember {
        listOf(
            "standard" to "Standard",
            "hdr" to "Vivid",
            "night" to "Night vision",
            "warm" to "Eye comfort"
        )
    }

    M3WidgetCard(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { state.openSubscreen("display") }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("display_settings"),
                            contentDescription = null,
                            tint = scheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Color Scheme",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
            }

            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { state.openSubscreen("display") }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("chevron_right"),
                        contentDescription = "Open display settings",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (id, label) ->
                        val isSelected = state.visualFilter == id
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                            animationSpec = tween(durationMillis = 250),
                            label = "DisplayFilterContainer"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                            animationSpec = tween(durationMillis = 250),
                            label = "DisplayFilterContent"
                        )
                        val shape = RoundedCornerShape(20.dp)

                        Surface(
                            shape = shape,
                            color = containerColor,
                            contentColor = contentColor,
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .clickable { state.visualFilter = id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = ZXSidePanelIcons.get("check"),
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
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

@Composable
private fun NetworkPingWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    M3WidgetCard(
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("wifi_tethering"),
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Network acceleration",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
            }

            Text(
                text = "${state.pingMs} ms",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.primary
            )
        }
    }
}

@Composable
private fun ThermalBatteryWidget() {
    val scheme = MaterialTheme.colorScheme
    M3WidgetCard(
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.tertiaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("thermostat"),
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Bypass charging",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
            }

            Surface(
                shape = CircleShape,
                color = scheme.secondaryContainer
            ) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceMacroWidget(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val voices = remember { listOf("Off", "Deep", "Robot", "Studio") }

    M3WidgetCard(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Voice changer",
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            voices.forEach { voice ->
                val isSelected = state.voiceFilter == voice
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "VoiceMacroScale"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 250),
                    label = "VoiceMacroContainer"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "VoiceMacroContent"
                )
                val shape = RoundedCornerShape(16.dp)

                Surface(
                    shape = shape,
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(shape)
                        .clickable { state.voiceFilter = voice }
                ) {
                    Text(
                        text = voice,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CrosshairSubscreen(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val ch = state.crosshair
    val colors = remember {
        listOf(
            "#d0bcff" to Color(0xFFD0BCFF),
            "#82d5c8" to Color(0xFF82D5C8),
            "#ffb1c8" to Color(0xFFFFB1C8),
            "#e9c349" to Color(0xFFE9C349),
            "#51cf66" to Color(0xFF51CF66),
            "#ff6b6b" to Color(0xFFFF6B6B),
            "#ffffff" to Color(0xFFFFFFFF)
        )
    }

    val layers = remember {
        listOf(
            "showDot" to "Center dot",
            "showCross" to "Cross lines",
            "showCircle" to "Circle ring",
            "showAngle" to "Angle chevron"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Active Reticle Preview Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Active reticle preview",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Updates live in-game",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                M3ExpressiveSwitch(
                    checked = ch.enabled,
                    onCheckedChange = { state.toggleCrosshairQuick() }
                )
            }

            // Preview Box (h-32 = 128.dp, rounded-[24px], md-surface-high)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val dotColor = scheme.onSurface.copy(alpha = 0.2f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stepPx = 16.dp.toPx()
                        val dotR = 1.dp.toPx()
                        var y = stepPx / 2f
                        while (y < size.height) {
                            var x = stepPx / 2f
                            while (x < size.width) {
                                drawCircle(color = dotColor, radius = dotR, center = Offset(x, y))
                                x += stepPx
                            }
                            y += stepPx
                        }
                    }

                    CrosshairReticleCanvas(crosshair = ch)
                }
            }

            // Reticle Color Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reticle color",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { (hex, swatchColor) ->
                        val isSelected = ch.colorHex.equals(hex, ignoreCase = true)
                        val swatchScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.25f else 1f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                            label = "ColorSwatchScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = swatchScale
                                    scaleY = swatchScale
                                    alpha = if (isSelected) 1f else 0.65f
                                }
                                .clip(CircleShape)
                                .background(swatchColor)
                                .clickable { state.setCrosshairColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.6f),
                                        radius = size.minDimension / 2f - 1.dp.toPx(),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Reticle Layers Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Reticle layers",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                layers.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (layerKey, label) ->
                            val isActive = when (layerKey) {
                                "showDot" -> ch.showDot
                                "showCross" -> ch.showCross
                                "showCircle" -> ch.showCircle
                                "showAngle" -> ch.showAngle
                                else -> false
                            }

                            M3ToggleTile(
                                active = isActive,
                                onClick = { state.toggleCrosshairLayer(layerKey) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = ZXSidePanelIcons.get(
                                            if (isActive) "check_circle" else "add_circle"
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Geometry & Thickness Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Geometry & thickness",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            // Stroke thickness
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stroke thickness",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    val thickText = if (ch.thickness % 1f == 0f) {
                        "${ch.thickness.toInt()} px"
                    } else {
                        "${ch.thickness} px"
                    }
                    Text(
                        text = thickText,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                }
                M3ExpressiveLinearSlider(
                    value = ch.thickness,
                    min = 1f,
                    max = 8f,
                    step = 0.5f,
                    onValueChange = { state.updateCrosshairThickness(it) }
                )
            }

            // Line length
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Line length",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${ch.size.roundToInt()} dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                }
                M3ExpressiveLinearSlider(
                    value = ch.size,
                    min = 8f,
                    max = 56f,
                    step = 1f,
                    onValueChange = { state.updateCrosshairSize(it) }
                )
            }

            // Center gap
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Center gap",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${ch.gap.roundToInt()} dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                }
                M3ExpressiveLinearSlider(
                    value = ch.gap,
                    min = 0f,
                    max = 28f,
                    step = 1f,
                    onValueChange = { state.updateCrosshairGap(it) }
                )
            }
        }
    }
}

@Composable
fun DisplaySubscreen(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val filters = remember {
        listOf(
            "standard" to "Standard",
            "hdr" to "Vivid",
            "night" to "Night vision",
            "warm" to "Eye comfort"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Visual Presets Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Visual presets",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (id, label) ->
                            val isActive = state.visualFilter == id
                            M3ToggleTile(
                                active = isActive,
                                onClick = { state.visualFilter = id },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (isActive) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get("check"),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. RGB Channel Balance Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RGB balance",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "R ${state.rgbRed}% • G ${state.rgbGreen}% • B ${state.rgbBlue}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Red channel
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Red channel",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.rgbRed}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF6B6B)
                    )
                }
                M3ExpressiveLinearSlider(
                    value = state.rgbRed.toFloat(),
                    min = 50f,
                    max = 150f,
                    step = 1f,
                    variant = M3SliderVariant.Red,
                    onValueChange = { state.rgbRed = it.roundToInt() }
                )
            }

            // Green channel
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Green channel",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.rgbGreen}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF51CF66)
                    )
                }
                M3ExpressiveLinearSlider(
                    value = state.rgbGreen.toFloat(),
                    min = 50f,
                    max = 150f,
                    step = 1f,
                    variant = M3SliderVariant.Green,
                    onValueChange = { state.rgbGreen = it.roundToInt() }
                )
            }

            // Blue channel
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blue channel",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.rgbBlue}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4DABF7)
                    )
                }
                M3ExpressiveLinearSlider(
                    value = state.rgbBlue.toFloat(),
                    min = 50f,
                    max = 150f,
                    step = 1f,
                    variant = M3SliderVariant.Blue,
                    onValueChange = { state.rgbBlue = it.roundToInt() }
                )
            }
        }

        // 3. Saturation & Contrast Card
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Display tuning",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saturation",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.saturation}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                }
                M3ExpressiveLinearSlider(
                    value = state.saturation.toFloat(),
                    min = 0f,
                    max = 200f,
                    step = 1f,
                    variant = M3SliderVariant.Primary,
                    onValueChange = { state.saturation = it.roundToInt() }
                )
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Contrast",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.contrast}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.secondary
                    )
                }
                M3ExpressiveLinearSlider(
                    value = state.contrast.toFloat(),
                    min = 50f,
                    max = 150f,
                    step = 1f,
                    variant = M3SliderVariant.Secondary,
                    onValueChange = { state.contrast = it.roundToInt() }
                )
            }
        }
    }
}

@Composable
fun AppsManagerSubscreen(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floating Apps",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.allApps.chunked(3).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowApps.forEach { app ->
                            val isAdded = state.quickApps.contains(app.id)
                            val containerColor by animateColorAsState(
                                targetValue = if (isAdded) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                animationSpec = tween(durationMillis = 250),
                                label = "AppCellContainer"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isAdded) scheme.onPrimaryContainer else scheme.onSurface,
                                animationSpec = tween(durationMillis = 250),
                                label = "AppCellContent"
                            )
                            val shape = RoundedCornerShape(20.dp)

                            Surface(
                                shape = shape,
                                color = containerColor,
                                contentColor = contentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(shape)
                                    .clickable { state.toggleQuickApp(app.id) }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get(app.icon),
                                            contentDescription = app.name,
                                            tint = contentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = contentColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Top-Right Add/Remove Badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 6.dp, end = 6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAdded) scheme.primary
                                                else scheme.surfaceContainerHighest
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get(if (isAdded) "remove" else "add"),
                                            contentDescription = null,
                                            tint = if (isAdded) scheme.onPrimary else scheme.onSurface,
                                            modifier = Modifier.size(14.dp)
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
