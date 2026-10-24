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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class M3SliderVariant {
    Primary,
    Secondary,
    Red,
    Green,
    Blue
}

@Composable
fun M3ExpressiveLinearSlider(
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 1f,
    variant: M3SliderVariant = M3SliderVariant.Primary
) {
    val scheme = MaterialTheme.colorScheme
    val activeColor = when (variant) {
        M3SliderVariant.Primary -> scheme.primary
        M3SliderVariant.Secondary -> scheme.secondary
        M3SliderVariant.Red -> Color(0xFFFF6B6B)
        M3SliderVariant.Green -> Color(0xFF51CF66)
        M3SliderVariant.Blue -> Color(0xFF4DABF7)
    }

    var isDragging by remember { mutableStateOf(false) }
    val handleWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "SliderHandleWidth"
    )
    val handleScaleY by animateFloatAsState(
        targetValue = if (isDragging) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "SliderHandleScaleY"
    )

    val range = (max - min).coerceAtLeast(0.001f)
    val fraction = ((value - min) / range).coerceIn(0f, 1f)

    fun snapAndEmit(rawFraction: Float) {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val rawVal = min + clamped * range
        val snapped = if (step > 0f) {
            (Math.round((rawVal - min) / step) * step + min).coerceIn(min, max)
        } else {
            rawVal.coerceIn(min, max)
        }
        onValueChange(snapped)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(min, max, step) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        snapAndEmit(offset.x / size.width.toFloat().coerceAtLeast(1f))
                        tryAwaitRelease()
                        isDragging = false
                    }
                )
            }
            .pointerInput(min, max, step) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        snapAndEmit(offset.x / size.width.toFloat().coerceAtLeast(1f))
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        snapAndEmit(change.position.x / size.width.toFloat().coerceAtLeast(1f))
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val reservedForHandle = handleWidth + 8.dp
        val trackAvailable = (totalWidth - reservedForHandle).coerceAtLeast(0.dp)
        val activeWidth = trackAvailable * fraction
        val inactiveWidth = trackAvailable * (1f - fraction)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active Track
            Box(
                modifier = Modifier
                    .width(activeWidth)
                    .height(20.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            bottomStart = 12.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(activeColor)
            )

            // Handle
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(handleWidth)
                    .height(32.dp)
                    .graphicsLayer {
                        scaleY = handleScaleY
                    }
                    .clip(CircleShape)
                    .background(activeColor)
            )

            // Inactive Track with Stop Dot
            Box(
                modifier = Modifier
                    .width(inactiveWidth)
                    .height(20.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .background(scheme.surfaceContainerHighest),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (inactiveWidth >= 10.dp) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.65f))
                    )
                }
            }
        }
    }
}

@Composable
fun M3ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHighest,
        animationSpec = tween(durationMillis = 250),
        label = "SwitchTrackColor"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (checked) 24.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "SwitchThumbSize"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "SwitchThumbOffset"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "SwitchThumbColor"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCheckedChange
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = ZXSidePanelIcons.get("check"),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun M3ToggleTile(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (active) 28.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "ToggleTileCorner"
    )
    val containerColor by animateColorAsState(
        targetValue = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = tween(durationMillis = 250),
        label = "ToggleTileContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        animationSpec = tween(durationMillis = 250),
        label = "ToggleTileContent"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "ToggleTilePressScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun M3WidgetCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

@Composable
fun M3ExpressiveRadialGauge(
    percent: Int,
    label: String,
    activeColor: Color,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "GaugePressScale"
    )

    val animatedPercent by animateFloatAsState(
        targetValue = percent.toFloat(),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f),
        label = "GaugeSweepPercent"
    )

    val shape = RoundedCornerShape(24.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val inactiveStrokeColor = scheme.surfaceContainerHighest
            val stopDotColor = scheme.onSurfaceVariant.copy(alpha = 0.7f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(140f / 132f)
            ) {
                val scaleX = size.width / 140f
                val scaleY = size.height / 132f
                val s = min(scaleX, scaleY)

                val cx = size.width / 2f
                val cy = 70f * s
                val radius = 52f * s
                val strokeW = 12f * s

                val totalSweepLen = 217.82f
                val gapLength = 14f
                val clamped = animatedPercent.coerceIn(4f, 96f)
                val activeLen = max(4f, (clamped / 100f) * totalSweepLen - gapLength / 2f)
                val inactiveLen = max(4f, ((100f - clamped) / 100f) * totalSweepLen - gapLength / 2f)
                val inactiveStartLen = activeLen + gapLength

                val activeSweepDeg = (activeLen / totalSweepLen) * 240f
                val inactiveSweepDeg = (inactiveLen / totalSweepLen) * 240f
                val inactiveStartDeg = 150f + (inactiveStartLen / totalSweepLen) * 240f
                val pillAngle = -120f + (clamped / 100f) * 240f

                val arcTopLeft = Offset(cx - radius, cy - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                // Inactive Arc
                drawArc(
                    color = inactiveStrokeColor,
                    startAngle = inactiveStartDeg,
                    sweepAngle = inactiveSweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active Arc
                drawArc(
                    color = activeColor,
                    startAngle = 150f,
                    sweepAngle = activeSweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Stop Dot at +120deg from top (-90deg + 120deg = +30deg)
                rotate(degrees = 120f, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = stopDotColor,
                        radius = 2.2f * s,
                        center = Offset(cx, cy - radius)
                    )
                }

                // Pill Indicator Needle
                rotate(degrees = pillAngle, pivot = Offset(cx, cy)) {
                    val pillW = 5f * s
                    val pillH = 14f * s
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(cx - pillW / 2f, cy - radius - pillH / 2f),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(2.5f * s, 2.5f * s)
                    )
                }
            }

            // Center Readout Overlay
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$percent",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelMedium,
                        color = activeColor,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = CircleShape,
                    color = badgeContainerColor
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeContentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CrosshairReticleCanvas(
    crosshair: SidePanelCrosshairState,
    modifier: Modifier = Modifier
) {
    val color = crosshair.parsedColor
    val alpha = (crosshair.opacity / 100f).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .size(120.dp)
            .graphicsLayer {
                this.alpha = alpha
            }
    ) {
        val scale = size.width / 120f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val sz = crosshair.size * scale
        val gap = crosshair.gap * scale
        val thick = crosshair.thickness * scale
        val rad = crosshair.circleRadius * scale
        val rot = crosshair.rotation

        fun drawReticlePass(strokeColor: Color, extraStroke: Float) {
            val currentThick = thick + extraStroke
            if (crosshair.showCircle) {
                drawCircle(
                    color = strokeColor,
                    radius = rad,
                    center = Offset(cx, cy),
                    style = Stroke(width = currentThick)
                )
            }
            if (crosshair.showCross) {
                rotate(degrees = rot, pivot = Offset(cx, cy)) {
                    // Top line
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, cy - gap - sz / 2f),
                        end = Offset(cx, cy - gap),
                        strokeWidth = currentThick,
                        cap = StrokeCap.Round
                    )
                    // Bottom line
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx, cy + gap),
                        end = Offset(cx, cy + gap + sz / 2f),
                        strokeWidth = currentThick,
                        cap = StrokeCap.Round
                    )
                    // Left line
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx - gap - sz / 2f, cy),
                        end = Offset(cx - gap, cy),
                        strokeWidth = currentThick,
                        cap = StrokeCap.Round
                    )
                    // Right line
                    drawLine(
                        color = strokeColor,
                        start = Offset(cx + gap, cy),
                        end = Offset(cx + gap + sz / 2f, cy),
                        strokeWidth = currentThick,
                        cap = StrokeCap.Round
                    )
                }
            }
            if (crosshair.showAngle) {
                val path = Path().apply {
                    moveTo(cx - sz / 2f, cy + sz / 2.6f + gap / 2f)
                    lineTo(cx, cy + gap / 2f)
                    lineTo(cx + sz / 2f, cy + sz / 2.6f + gap / 2f)
                }
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = currentThick,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            if (crosshair.showDot) {
                val dotR = max(2f * scale, thick * 0.95f) + extraStroke / 2f
                drawCircle(
                    color = strokeColor,
                    radius = dotR,
                    center = Offset(cx, cy)
                )
            }
        }

        // Drop shadow outline pass matching `filter: drop-shadow(0px 0px 2px rgba(0,0,0,0.9))`
        if (crosshair.outline) {
            drawReticlePass(Color.Black.copy(alpha = 0.75f), 2.5f * scale)
        }
        drawReticlePass(color, 0f)
    }
}

@Composable
fun FloatingMonitorPill(
    fps: Int,
    cpuLoad: Int,
    gpuLoad: Int,
    modifier: Modifier = Modifier,
    onDragDelta: ((Float, Float) -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val animatedCpu by animateFloatAsState(
        targetValue = (cpuLoad / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "HudCpuBar"
    )
    val animatedGpu by animateFloatAsState(
        targetValue = (gpuLoad / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "HudGpuBar"
    )

    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerLow,
        shadowElevation = 16.dp,
        modifier = modifier
            .then(
                if (onDragDelta != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // FPS Readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$fps",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.primary
                )
                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            // CPU Load Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CPU",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedCpu)
                            .clip(CircleShape)
                            .background(scheme.primary)
                    )
                }
                Text(
                    text = "$cpuLoad%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(32.dp)
                )
            }

            // GPU Load Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GPU",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedGpu)
                            .clip(CircleShape)
                            .background(scheme.tertiary)
                    )
                }
                Text(
                    text = "$gpuLoad%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingAppWindow(
    appName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDragDelta: ((Float, Float) -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B141A),
        shadowElevation = 24.dp,
        modifier = modifier
            .width(320.dp)
            .height(440.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Header (#202c33)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF202C33))
                    .then(
                        if (onDragDelta != null) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("arrow_back"),
                        contentDescription = null,
                        tint = Color(0xFF8696A0),
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D9488)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = appName,
                        color = Color(0xFFE9EDEF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("close"),
                        contentDescription = "Close",
                        tint = Color(0xFF8696A0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // App Body (Simulated Chat #0b141a)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B141A))
            ) {
                // Radial dot background pattern (#1f2c34, 24px grid, 0.2 alpha)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepPx = 24.dp.toPx()
                    val dotR = 1.5.dp.toPx()
                    val dotColor = Color(0xFF1F2C34).copy(alpha = 0.45f)
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChatBubble(
                        text = "Anyone on for a ranked match?",
                        isOutgoing = false
                    )
                    ChatBubble(
                        text = "I'm hard stuck in diamond 😭",
                        isOutgoing = false
                    )
                    ChatBubble(
                        text = "Give me 5 mins, finishing up a quest.",
                        isOutgoing = true
                    )
                    ChatBubble(
                        text = "Get in the lobby.",
                        isOutgoing = true
                    )
                }
            }

            // App Input (#202c33)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF202C33))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("add"),
                        contentDescription = null,
                        tint = Color(0xFF8696A0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Color(0xFF2A3942))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Message",
                        color = Color(0xFF8696A0),
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A884)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ZXSidePanelIcons.get("send"),
                        contentDescription = "Send",
                        tint = Color(0xFF111B21),
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    isOutgoing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        } else {
            RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        }
        val bubbleColor = if (isOutgoing) Color(0xFF005C4B) else Color(0xFF202C33)

        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            Text(
                text = text,
                color = Color(0xFFE9EDEF),
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun M3ToastSnackbar(
    visible: Boolean,
    message: String,
    iconName: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerHighest,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = ZXSidePanelIcons.get(iconName),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
            }
        }
    }
}
