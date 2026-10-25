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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import zx.azenith.ui.component.ZenithSlider

@Composable
fun SidePanelWidgetContent(
    widgetId: String,
    state: ZXSidePanelStateHolder,
    isMerged: Boolean = false,
    span: Int = state.getWidgetSpan(widgetId)
) {
    when (widgetId) {
        "fpsHeader" -> FpsMeterWidget(state, isMerged, span)
        "speedometers" -> SpeedometersWidget(state, isMerged, span)
        "profileSwitch" -> ProfileSegmentedWidget(state, isMerged, span)
        "brightnessSlider" -> BrightnessSliderWidget(state, isMerged, span)
        "volumeSlider" -> VolumeSliderWidget(state, isMerged, span)
        "quickTools" -> QuickToolsWidget(state, span)
        "antiInterference" -> AntiInterferenceWidget(state, span)
        "liveMonitor" -> LiveMonitorWidget(state, span)
        "crosshair" -> CrosshairWidget(state, span)
        "displayTouch" -> DisplayTouchWidget(state, span)
        "networkPing" -> NetworkPingWidget(state, span)
        "thermalBattery" -> ThermalBatteryWidget(state, span)
    }
}

@Composable
fun EditModeWidgetCard(
    widgetId: String,
    index: Int,
    state: ZXSidePanelStateHolder,
    span: Int = state.getWidgetSpan(widgetId),
    onReportBounds: (cardBoundsInRoot: Rect, removeBtnBoundsInRoot: Rect, resizeGripBoundsInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = state.widgetCatalog[widgetId] ?: return
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var removeBtnBounds by remember { mutableStateOf(Rect.Zero) }
    var resizeGripBounds by remember { mutableStateOf(Rect.Zero) }

    val isBeingDragged = state.draggingWidgetId == widgetId && !state.draggingFromLibrary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                cardBounds = coords.boundsInRoot()
                onReportBounds(cardBounds, removeBtnBounds, resizeGripBounds)
            }
            .graphicsLayer {
                alpha = if (isBeingDragged) 0.22f else 1f
                scaleX = if (isBeingDragged) 0.96f else 1f
                scaleY = if (isBeingDragged) 0.96f else 1f
            }
    ) {
        // Disabled Inner Widget Preview (entire surface is draggable directly)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.94f }
        ) {
            SidePanelWidgetContent(
                widgetId = widgetId,
                state = state,
                isMerged = false,
                span = span
            )
        }

        // Circular '-' Remove Button at Top-Right
        Surface(
            shape = CircleShape,
            color = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .onGloballyPositioned { coords ->
                    removeBtnBounds = coords.boundsInRoot()
                    onReportBounds(cardBounds, removeBtnBounds, resizeGripBounds)
                }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.removeWidgetToLibrary(widgetId)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("remove"),
                    contentDescription = "Remove widget",
                    tint = scheme.onErrorContainer,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Corner Mirrored 'L' Resize Grip at Bottom-Right
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp, end = 4.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(bottomEnd = 16.dp))
                .onGloballyPositioned { coords ->
                    resizeGripBounds = coords.boundsInRoot()
                    onReportBounds(cardBounds, removeBtnBounds, resizeGripBounds)
                }
                .pointerInput(widgetId) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var dragged = false
                        val initialSpan = state.getWidgetSpan(widgetId)
                        var currentSpan = initialSpan
                        val stepThreshold = 44.dp.toPx()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!dragged) {
                                    state.cycleWidgetSpan(widgetId)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                break
                            }
                            val dx = change.position.x - down.position.x
                            totalDx = dx
                            if (abs(totalDx) > viewConfiguration.touchSlop) {
                                dragged = true
                                change.consume()
                                val stepChange = (totalDx / stepThreshold).toInt()
                                val targetSpan = (initialSpan + stepChange).coerceIn(1, 3)
                                if (targetSpan != currentSpan) {
                                    currentSpan = targetSpan
                                    state.setWidgetSpan(widgetId, targetSpan)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.size(22.dp).padding(3.dp)) {
                val strokeW = 2.4.dp.toPx()
                val w = size.width
                val h = size.height
                val cornerRadius = 3.dp.toPx()
                val gripColor = scheme.primary

                // Outer mirrored 'L' corner bracket: _|
                val path = Path().apply {
                    moveTo(w * 0.35f, h - strokeW / 2f)
                    lineTo(w - cornerRadius, h - strokeW / 2f)
                    quadraticTo(w, h - strokeW / 2f, w, h - cornerRadius)
                    lineTo(w, h * 0.35f)
                }
                drawPath(
                    path = path,
                    color = gripColor,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Inner mirrored 'L' corner bracket
                val innerPath = Path().apply {
                    moveTo(w * 0.65f, h - strokeW * 2.5f)
                    lineTo(w - cornerRadius - strokeW * 1.4f, h - strokeW * 2.5f)
                    quadraticTo(
                        w - strokeW * 1.4f,
                        h - strokeW * 2.5f,
                        w - strokeW * 1.4f,
                        h - cornerRadius - strokeW * 1.4f
                    )
                    lineTo(w - strokeW * 1.4f, h * 0.65f)
                }
                drawPath(
                    path = innerPath,
                    color = gripColor.copy(alpha = 0.65f),
                    style = Stroke(
                        width = strokeW * 0.85f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun WidgetDropLandingPlaceholder(
    widgetId: String,
    state: ZXSidePanelStateHolder,
    span: Int = 3,
    modifier: Modifier = Modifier
) {
    val meta = state.widgetCatalog[widgetId] ?: return
    val scheme = MaterialTheme.colorScheme
    val targetHeight = if (span == 1) 96.dp else if (span == 2) 90.dp else 84.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.18f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 2.dp.toPx()
            val dashPx = 10.dp.toPx()
            val gapPx = 6.dp.toPx()
            drawRoundRect(
                color = scheme.primary.copy(alpha = 0.85f),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(
                    width = strokePx,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                )
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = ZXSidePanelIcons.get(meta.icon),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(if (span == 1) 22.dp else 20.dp)
            )
            if (span >= 2) {
                Text(
                    text = "Drop here",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
            }
        }
    }
}

@Composable
fun DroppingWidgetAnimationOverlay(
    widgetId: String,
    state: ZXSidePanelStateHolder,
    initialOffset: Offset,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = state.widgetCatalog[widgetId] ?: run {
        onFinished()
        return
    }
    val scheme = MaterialTheme.colorScheme
    val animProgress = remember { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(widgetId) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        onFinished()
    }

    val progress = animProgress.value
    val currentScale = 1.04f - (0.04f * progress)
    val currentAlpha = (1f - ((progress - 0.72f) / 0.28f)).coerceIn(0f, 1f)
    val dropYShift = progress * 16.dp.value

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHighest.copy(alpha = 0.96f),
        border = BorderStroke(
            width = (2.dp.value * (1f - progress)).coerceAtLeast(0.5f).dp,
            color = scheme.primary.copy(alpha = (1f - progress).coerceIn(0f, 1f))
        ),
        shadowElevation = (28.dp.value * (1f - progress)).coerceAtLeast(0f).dp,
        modifier = modifier
            .offset {
                IntOffset(
                    initialOffset.x.roundToInt(),
                    (initialOffset.y + dropYShift).roundToInt()
                )
            }
            .widthIn(min = 240.dp, max = 304.dp)
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
                alpha = currentAlpha
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get(meta.icon),
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meta.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = meta.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DraggedWidgetMockupOverlay(
    widgetId: String,
    state: ZXSidePanelStateHolder,
    modifier: Modifier = Modifier
) {
    val meta = state.widgetCatalog[widgetId] ?: return
    val scheme = MaterialTheme.colorScheme
    val isHoverLibrary = state.isHoveringLibraryDropZone

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainerHighest.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 2.dp,
            color = if (isHoverLibrary) scheme.error else scheme.primary
        ),
        shadowElevation = 28.dp,
        modifier = modifier
            .widthIn(min = 240.dp, max = 304.dp)
            .graphicsLayer {
                scaleX = 1.03f
                scaleY = 1.03f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isHoverLibrary) scheme.errorContainer else scheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get(meta.icon),
                            contentDescription = null,
                            tint = if (isHoverLibrary) scheme.onErrorContainer else scheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meta.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isHoverLibrary) "Release to move to library" else meta.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHoverLibrary) scheme.error else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Miniature visual mockup skeleton of the widget being carried
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = scheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(scheme.primary.copy(alpha = 0.45f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(scheme.secondaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.2f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHighest)
                    )
                }
            }
        }
    }
}

@Composable
private fun FpsMeterWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("speed"),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.currentFps}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

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
    isMerged: Boolean,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CPU", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                    Text("${state.cpuLoad}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = scheme.primary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPU", style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
                    Text("${state.gpuLoad}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = scheme.tertiary)
                }
            }
        }
        return
    }

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
    isMerged: Boolean,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        val currentTitle = when (state.activeProfile) {
            "saving" -> "Saving"
            "balanced" -> "Balanced"
            else -> "Perf"
        }
        val currentIcon = when (state.activeProfile) {
            "saving" -> "eco"
            "balanced" -> "balance"
            else -> "bolt"
        }
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = when (state.activeProfile) {
                            "saving" -> "balanced"
                            "balanced" -> "performance"
                            else -> "saving"
                        }
                        state.selectPerformanceProfile(next)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get(currentIcon),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val perfLabel = if (state.isLiteMode) "Perf Lite" else "Performance"
    val profiles = listOf(
        "saving" to "Power saving",
        "balanced" to "Balanced",
        "performance" to perfLabel
    )

    val inner: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto Mode + Current Running Profile + Profile Session Duration Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val autoChipContainer by animateColorAsState(
                    targetValue = if (state.isAutoMode) scheme.tertiaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 220),
                    label = "AutoModeChipContainer"
                )
                val autoChipContent by animateColorAsState(
                    targetValue = if (state.isAutoMode) scheme.onTertiaryContainer else scheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "AutoModeChipContent"
                )

                Surface(
                    shape = CircleShape,
                    color = autoChipContainer,
                    contentColor = autoChipContent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { state.toggleAutoMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get(if (state.isAutoMode) "auto_fix_high" else "tune"),
                            contentDescription = null,
                            tint = autoChipContent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (state.isAutoMode) "Auto Mode" else "Manual Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = autoChipContent,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("schedule"),
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${state.currentProfileDisplayName} • ${state.profileDurationText}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

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
                            .graphicsLayer {
                                alpha = if (!isSelected && state.isAutoMode && state.isRootAvailable) 0.72f else 1f
                            }
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
    }

    if (isMerged) {
        inner()
    } else {
        M3WidgetCard(
            contentPadding = PaddingValues(10.dp)
        ) {
            inner()
        }
    }
}

@Composable
private fun BrightnessSliderWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    var isDragging by remember { mutableStateOf(false) }
    var localFraction by remember {
        mutableFloatStateOf((state.brightness / 100f).coerceIn(0.05f, 1f))
    }

    LaunchedEffect(state.brightness) {
        if (!isDragging) {
            localFraction = (state.brightness / 100f).coerceIn(0.05f, 1f)
        }
    }

    val displayPercent = (localFraction * 100).roundToInt().coerceIn(5, 100)
    val brightnessIcon = when {
        localFraction < 0.33f -> Icons.Rounded.BrightnessLow
        localFraction < 0.66f -> Icons.Rounded.BrightnessMedium
        else -> Icons.Rounded.BrightnessHigh
    }

    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = when {
                            state.brightness < 40 -> 60
                            state.brightness < 80 -> 100
                            else -> 25
                        }
                        localFraction = next / 100f
                        state.updateHorizontalBrightness(next)
                        state.commitHorizontalBrightness(next)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = brightnessIcon,
                    contentDescription = null,
                    tint = scheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$displayPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.tertiary
                )
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (isMerged) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = brightnessIcon,
                        contentDescription = null,
                        tint = scheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }
                Text(
                    text = "$displayPercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.tertiary
                )
            }

            Spacer(Modifier.height(4.dp))

            ZenithSlider(
                value = localFraction,
                onValueChange = { newFraction ->
                    isDragging = true
                    localFraction = newFraction.coerceIn(0.05f, 1f)
                    state.updateHorizontalBrightness((newFraction * 100).roundToInt().coerceIn(5, 100))
                },
                onValueChangeFinished = {
                    isDragging = false
                    state.commitHorizontalBrightness((localFraction * 100).roundToInt().coerceIn(5, 100))
                },
                accent = scheme.tertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        BentoSliderCard(
            icon = brightnessIcon,
            title = "Brightness",
            valuePercent = displayPercent,
            accent = scheme.tertiary,
            value = localFraction,
            onValueChange = { newFraction ->
                isDragging = true
                localFraction = newFraction.coerceIn(0.05f, 1f)
                state.updateHorizontalBrightness((newFraction * 100).roundToInt().coerceIn(5, 100))
            },
            onValueChangeFinished = {
                isDragging = false
                state.commitHorizontalBrightness((localFraction * 100).roundToInt().coerceIn(5, 100))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VolumeSliderWidget(
    state: ZXSidePanelStateHolder,
    isMerged: Boolean,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    var isDragging by remember { mutableStateOf(false) }
    var localFraction by remember {
        mutableFloatStateOf(if (state.muted) 0f else (state.volume / 100f).coerceIn(0f, 1f))
    }

    LaunchedEffect(state.volume, state.muted) {
        if (!isDragging) {
            localFraction = if (state.muted) 0f else (state.volume / 100f).coerceIn(0f, 1f)
        }
    }

    val displayPercent = if (state.muted) 0 else (localFraction * 100).roundToInt().coerceIn(0, 100)
    val volumeIcon = when {
        state.muted || localFraction <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
        localFraction < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }

    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.toggleMuteAudio() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.muted) "Muted" else "$displayPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (isMerged) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        imageVector = volumeIcon,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }
                Text(
                    text = if (state.muted) "0%" else "$displayPercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
            }

            Spacer(Modifier.height(4.dp))

            ZenithSlider(
                value = localFraction,
                onValueChange = { newFraction ->
                    isDragging = true
                    localFraction = newFraction.coerceIn(0f, 1f)
                    if (state.muted && newFraction > 0.01f) {
                        state.muted = false
                    }
                    state.updateHorizontalVolume((newFraction * 100).roundToInt().coerceIn(0, 100))
                },
                onValueChangeFinished = {
                    isDragging = false
                    state.commitHorizontalVolume((localFraction * 100).roundToInt().coerceIn(0, 100))
                },
                accent = scheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        BentoSliderCard(
            icon = volumeIcon,
            title = "Volume",
            valuePercent = displayPercent,
            accent = scheme.primary,
            value = localFraction,
            onValueChange = { newFraction ->
                isDragging = true
                localFraction = newFraction.coerceIn(0f, 1f)
                if (state.muted && newFraction > 0.01f) {
                    state.muted = false
                }
                state.updateHorizontalVolume((newFraction * 100).roundToInt().coerceIn(0, 100))
            },
            onValueChangeFinished = {
                isDragging = false
                state.commitHorizontalVolume((localFraction * 100).roundToInt().coerceIn(0, 100))
            },
            onHeaderClick = { state.toggleMuteAudio() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickToolsWidget(
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.quickTools.take(2).forEach { tool ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (tool.active) scheme.primaryContainer else scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { state.triggerQuickTool(tool.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(tool.icon),
                                    contentDescription = tool.name,
                                    tint = if (tool.active) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.quickTools.drop(2).take(2).forEach { tool ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (tool.active) scheme.primaryContainer else scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { state.triggerQuickTool(tool.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(tool.icon),
                                    contentDescription = tool.name,
                                    tint = if (tool.active) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

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
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.antiInterference.take(2).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.active) scheme.errorContainer else scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { state.toggleAntiInterference(item.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(item.icon),
                                    contentDescription = item.name,
                                    tint = if (item.active) scheme.onErrorContainer else scheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.antiInterference.drop(2).take(2).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.active) scheme.errorContainer else scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { state.toggleAntiInterference(item.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(item.icon),
                                    contentDescription = item.name,
                                    tint = if (item.active) scheme.onErrorContainer else scheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

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
private fun LiveMonitorWidget(
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.toggleFloatingHud() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("monitoring"),
                    contentDescription = null,
                    tint = if (state.liveMonitor.enabled) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.liveMonitor.enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (state.liveMonitor.enabled) scheme.primary else scheme.onSurfaceVariant
                )
                Text(
                    text = "Monitor",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val styles = remember {
        listOf(
            "line" to "One line",
            "stacked" to "Stacked",
            "minimal" to "Compact"
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
                        onClick = { state.openSubscreen("liveMonitor") }
                    ),
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
                            imageVector = ZXSidePanelIcons.get("monitoring"),
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Live Monitor",
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
                    checked = state.liveMonitor.enabled,
                    onCheckedChange = { state.toggleFloatingHud() }
                )

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { state.openSubscreen("liveMonitor") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("chevron_right"),
                            contentDescription = "Open live monitor settings",
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
                val isSelected = state.liveMonitor.layoutStyle == id
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                    label = "LiveMonitorStyleScale"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                    animationSpec = tween(durationMillis = 250),
                    label = "LiveMonitorStyleContainer"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "LiveMonitorStyleContent"
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
                        .clickable { state.setLiveMonitorLayoutStyle(id) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CrosshairWidget(
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.toggleCrosshairQuick() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("my_location"),
                    contentDescription = null,
                    tint = if (state.crosshair.enabled) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.crosshair.enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (state.crosshair.enabled) scheme.primary else scheme.onSurfaceVariant
                )
                Text(
                    text = "Crosshair",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

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
                    text = "Crosshair",
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
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.openSubscreen("display") },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("display_settings"),
                    contentDescription = null,
                    tint = scheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.visualFilter.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

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
                                .clickable { state.selectVisualFilter(id) }
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
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("wifi_tethering"),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.pingMs}ms",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
                Text(
                    text = "Ping",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

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
private fun ThermalBatteryWidget(
    state: ZXSidePanelStateHolder,
    span: Int = 3
) {
    val scheme = MaterialTheme.colorScheme
    val isActive = state.isBypassChargingActive

    if (span == 1) {
        M3WidgetCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("thermostat"),
                    contentDescription = null,
                    tint = if (isActive) scheme.tertiary else scheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.tempCelsius}°C",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isActive) scheme.tertiary else scheme.primary
                )
                Text(
                    text = "${state.batteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        return
    }

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
                Column {
                    Text(
                        text = "Thermal & bypass",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "${state.tempCelsius}°C • Battery ${state.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (isActive) "Active" else "${state.tempCelsius}°C",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) scheme.primary else scheme.onSurface
            )
        }
    }
}

@Composable
fun LiveMonitorSubscreen(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme
    val lm = state.liveMonitor
    val styles = remember {
        listOf(
            "line" to "One line bar",
            "stacked" to "Stacked box",
            "minimal" to "Compact pill"
        )
    }
    val metricItems = remember {
        listOf(
            "fps" to "Frame rate (FPS)",
            "cpu" to "CPU load",
            "gpu" to "GPU load",
            "temp" to "Temperature",
            "ping" to "Network ping",
            "battery" to "Battery level",
            "profile" to "Active profile"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Monitor Preview & Main Toggle
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
                        text = "Floating overlay monitor",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Drag anywhere on screen while in game",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                M3ExpressiveSwitch(
                    checked = lm.enabled,
                    onCheckedChange = { state.toggleFloatingHud() }
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingMonitorPill(state = state)
                }
            }
        }

        // 2. Layout Style Selection
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Monitor layout",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styles.forEach { (styleId, label) ->
                    val isActive = lm.layoutStyle == styleId
                    M3ToggleTile(
                        active = isActive,
                        onClick = { state.setLiveMonitorLayoutStyle(styleId) },
                        modifier = Modifier.fillMaxWidth()
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
                                    imageVector = ZXSidePanelIcons.get("check_circle"),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Telemetry Metrics to Display
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Telemetry to show",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metricItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (metricKey, label) ->
                            val isActive = when (metricKey) {
                                "fps" -> lm.showFps
                                "cpu" -> lm.showCpu
                                "gpu" -> lm.showGpu
                                "temp" -> lm.showTemp
                                "ping" -> lm.showPing
                                "battery" -> lm.showBattery
                                "profile" -> lm.showProfile
                                else -> false
                            }

                            M3ToggleTile(
                                active = isActive,
                                onClick = { state.toggleLiveMonitorMetric(metricKey) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = ZXSidePanelIcons.get(
                                            if (isActive) "check_circle" else "add_circle"
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. Opacity Slider
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Overlay opacity",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = "${lm.opacity}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
            M3ExpressiveLinearSlider(
                value = lm.opacity.toFloat(),
                min = 40f,
                max = 100f,
                step = 1f,
                onValueChange = { state.updateLiveMonitorOpacity(it.roundToInt()) }
            )
        }

        // 5. Size Scale Slider
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monitor size",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = "${lm.scalePercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
            M3ExpressiveLinearSlider(
                value = lm.scalePercent.toFloat(),
                min = 75f,
                max = 140f,
                step = 1f,
                onValueChange = { state.updateLiveMonitorScale(it.roundToInt()) }
            )
        }

        // 6. Temperature Sensor Selection
        val tempSources = listOf(
            "soc" to "SOC / System",
            "battery" to "Battery",
            "cpu" to "CPU",
            "gpu" to "GPU",
            "skin" to "Skin / Screen"
        )
        M3WidgetCard(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Temperature sensor",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tempSources.forEach { (sourceId, label) ->
                    val isActive = lm.tempSource == sourceId
                    M3ToggleTile(
                        active = isActive,
                        onClick = { state.updateLiveMonitorTempSource(sourceId) },
                        modifier = Modifier.fillMaxWidth()
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
                                    imageVector = ZXSidePanelIcons.get("check_circle"),
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
                                onClick = { state.selectVisualFilter(id) },
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
            Text(
                text = "RGB balance",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant
            )

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
                    onValueChange = { state.updateRgbRedChannel(it.roundToInt()) }
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
                    onValueChange = { state.updateRgbGreenChannel(it.roundToInt()) }
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
                    onValueChange = { state.updateRgbBlueChannel(it.roundToInt()) }
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
                    onValueChange = { state.updateDisplaySaturation(it.roundToInt()) }
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
                    onValueChange = { state.updateDisplayContrast(it.roundToInt()) }
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
    var showSystemApps by remember { mutableStateOf(false) }

    val userApps = remember(state.installedApps) { state.installedApps.filter { !it.isSystemApp } }
    val displayedApps = if (showSystemApps) state.installedApps else userApps

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            M3WidgetCard(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header row with title, description, and singular System Apps toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.editMode) "Edit Sidepanel Shortcuts" else "More Apps",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.editMode) {
                                "Tap an app to add or remove it from the sidebar"
                            } else {
                                "Tap an app to launch it in a floating window"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    // Singular System Apps toggle (Material 3 container pill)
                    val toggleContainer by animateColorAsState(
                        targetValue = if (showSystemApps) scheme.primaryContainer else scheme.surfaceContainerHighest,
                        animationSpec = tween(180),
                        label = "systemToggleContainer"
                    )
                    val toggleContent by animateColorAsState(
                        targetValue = if (showSystemApps) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        animationSpec = tween(180),
                        label = "systemToggleContent"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = toggleContainer,
                        contentColor = toggleContent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val next = !showSystemApps
                                showSystemApps = next
                                if (next) {
                                    state.showToast(
                                        "Showing all apps. System apps may not be optimized for floating windows.",
                                        "warning"
                                    )
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ZXSidePanelIcons.get(if (showSystemApps) "check" else "tune"),
                                contentDescription = null,
                                tint = toggleContent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "System Apps",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = toggleContent
                            )
                        }
                    }
                }

                if (state.installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading installed apps...",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else if (displayedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!showSystemApps) "No user installed apps found" else "No apps found",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        displayedApps.chunked(3).forEach { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowApps.forEach { app ->
                                    val isAdded = state.quickApps.contains(app.packageName)
                                    val isActiveGame = app.packageName == state.activeGamePackage
                                    val containerColor by animateColorAsState(
                                        targetValue = when {
                                            isActiveGame -> scheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                            state.editMode && isAdded -> scheme.primaryContainer
                                            else -> scheme.surfaceContainerHigh
                                        },
                                        animationSpec = tween(durationMillis = 200),
                                        label = "AppCellContainer"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = when {
                                            isActiveGame -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            state.editMode && isAdded -> scheme.onPrimaryContainer
                                            else -> scheme.onSurface
                                        },
                                        animationSpec = tween(durationMillis = 200),
                                        label = "AppCellContent"
                                    )
                                    val shape = RoundedCornerShape(18.dp)

                                    Surface(
                                        shape = shape,
                                        color = containerColor,
                                        contentColor = contentColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(shape)
                                            .clickable {
                                                if (isActiveGame) {
                                                    state.showToast("Cannot open current game in floating window", "block")
                                                } else if (state.editMode) {
                                                    state.toggleQuickApp(app.packageName)
                                                } else {
                                                    state.launchQuickApp(app.packageName)
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (app.iconBitmap != null) {
                                                    Image(
                                                        bitmap = app.iconBitmap,
                                                        contentDescription = app.name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .graphicsLayer { if (isActiveGame) alpha = 0.45f }
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = ZXSidePanelIcons.get("grid_view"),
                                                        contentDescription = app.name,
                                                        tint = contentColor,
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(28.dp),
                                                    contentAlignment = Alignment.TopCenter
                                                ) {
                                                    Text(
                                                        text = if (isActiveGame) "${app.name}\n(Active Game)" else app.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = contentColor,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // Top-Right Badge: Only shown in edit mode
                                            if (state.editMode && !isActiveGame) {
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
                                                        imageVector = ZXSidePanelIcons.get(if (isAdded) "check" else "add"),
                                                        contentDescription = null,
                                                        tint = if (isAdded) scheme.onPrimary else scheme.onSurface,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowApps.size) {
                                    Spacer(modifier = Modifier.weight(1f).height(96.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
