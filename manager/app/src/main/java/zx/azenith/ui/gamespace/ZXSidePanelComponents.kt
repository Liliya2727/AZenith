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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
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
        targetValue = if (checked) 20.dp else 0.dp,
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
            .requiredSize(width = 52.dp, height = 32.dp)
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
    state: ZXSidePanelStateHolder,
    modifier: Modifier = Modifier,
    onDragDelta: ((Float, Float) -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val cfg = state.liveMonitor
    val alphaVal = (cfg.opacity / 100f).coerceIn(0.2f, 1f)
    val scaleFactor = (cfg.scalePercent / 100f).coerceIn(0.75f, 1.4f)

    val currentDensity = LocalDensity.current
    val scaledDensity = remember(currentDensity, scaleFactor) {
        Density(
            density = currentDensity.density * scaleFactor,
            fontScale = currentDensity.fontScale * scaleFactor
        )
    }

    val animatedCpu by animateFloatAsState(
        targetValue = (state.cpuLoad / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120),
        label = "HudCpuBar"
    )
    val animatedGpu by animateFloatAsState(
        targetValue = (state.gpuLoad / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120),
        label = "HudGpuBar"
    )

    val dragMod = if (onDragDelta != null) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        }
    } else {
        Modifier
    }

    val baseModifier = modifier
        .then(dragMod)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        when (cfg.layoutStyle) {
            "stacked" -> {
                val metrics = buildList<Triple<String, String, Color>> {
                    if (cfg.showFps) add(Triple("FPS", "${state.currentFps}", scheme.primary))
                    if (cfg.showCpu) add(Triple("CPU", "${state.cpuLoad}%", scheme.primary))
                    if (cfg.showGpu) add(Triple("GPU", "${state.gpuLoad}%", scheme.tertiary))
                    val tempLabel = when (cfg.tempSource) {
                        "battery" -> "BAT"
                        "cpu" -> "CPU"
                        "gpu" -> "GPU"
                        "skin" -> "SKIN"
                        else -> "TEMP"
                    }
                    if (cfg.showTemp) add(Triple(tempLabel, "${state.tempCelsius}°C", scheme.secondary))
                    if (cfg.showPing) add(Triple("PING", "${state.pingMs}ms", scheme.primary))
                    if (cfg.showBattery) add(Triple("BAT", "${state.batteryLevel}%", scheme.onSurface))
                    if (cfg.showProfile) add(Triple("MODE", state.currentProfileDisplayName, scheme.tertiary))
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.surfaceContainerHigh.copy(alpha = alphaVal),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = baseModifier
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        metrics.chunked(2).forEach { rowMetrics ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowMetrics.forEach { (label, value, accentColor) ->
                                    Row(
                                        modifier = Modifier
                                            .widthIn(min = 68.dp)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = accentColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "minimal" -> {
                val segments = buildList<Pair<String, Color>> {
                    if (cfg.showFps) add("${state.currentFps} FPS" to scheme.primary)
                    if (cfg.showCpu) add("${state.cpuLoad}%" to scheme.onSurface)
                    if (cfg.showGpu) add("${state.gpuLoad}%" to scheme.tertiary)
                    if (cfg.showTemp) add("${state.tempCelsius}°" to scheme.secondary)
                    if (cfg.showPing) add("${state.pingMs}ms" to scheme.onSurface)
                    if (cfg.showBattery) add("${state.batteryLevel}%" to scheme.onSurfaceVariant)
                    if (cfg.showProfile) {
                        val modeShort = when (state.currentProfileCode) {
                            "1" -> if (state.isLiteMode) "Lite" else "Perf"
                            "2" -> "Bal"
                            "3" -> "Eco"
                            else -> state.currentProfileDisplayName
                        }
                        add(modeShort to scheme.primary)
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh.copy(alpha = alphaVal),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = baseModifier
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        segments.forEachIndexed { idx, (text, color) ->
                            if (idx > 0) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = color,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            else -> {
                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh.copy(alpha = alphaVal),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = baseModifier
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (cfg.showFps) {
                            Text(
                                text = "${state.currentFps}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (cfg.showCpu) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "C",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(4.dp)
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
                                    text = "${state.cpuLoad}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (cfg.showGpu) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(4.dp)
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
                                    text = "${state.gpuLoad}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (cfg.showTemp) {
                            val tempColor = when {
                                state.tempCelsius >= 52 -> scheme.error
                                state.tempCelsius >= 44 -> scheme.secondary
                                else -> scheme.primary
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("thermostat"),
                                    contentDescription = null,
                                    tint = tempColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${state.tempCelsius}°",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tempColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (cfg.showPing) {
                            val pingColor = if (state.pingMs > 120) scheme.error else scheme.onSurface
                            val netIcon = if (state.isCellularNetwork) "signal_cellular" else "wifi"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(netIcon),
                                    contentDescription = null,
                                    tint = if (state.pingMs > 120) scheme.error else scheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${state.pingMs}ms",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = pingColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (cfg.showBattery) {
                            val batColor = if (state.batteryLevel <= 20) scheme.error else scheme.onSurfaceVariant
                            val batIcon = if (state.isBatteryCharging) "battery_charging" else "battery_std"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(batIcon),
                                    contentDescription = null,
                                    tint = if (state.isBatteryCharging) scheme.primary else batColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${state.batteryLevel}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = batColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (cfg.showProfile) {
                            val profIcon = when (state.currentProfileCode) {
                                "1" -> "rocket_launch"
                                "2" -> "balance"
                                "3" -> "eco"
                                else -> "rocket_launch"
                            }
                            val profShort = when (state.currentProfileCode) {
                                "1" -> if (state.isLiteMode) "Lite" else "Perf"
                                "2" -> "Bal"
                                "3" -> "Eco"
                                else -> state.currentProfileDisplayName
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(profIcon),
                                    contentDescription = null,
                                    tint = scheme.tertiary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = profShort,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.tertiary,
                                    maxLines = 1,
                                    softWrap = false
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
