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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import kotlin.math.cos
import kotlin.math.sin

/** Total length of the Game Space expressive intro animation (at max 2800ms <= 3s). */
const val GAME_SPACE_INTRO_MS = 2800

private val BOOT_STAGES = listOf(
    R.string.gamespace_boot_link,
    R.string.gamespace_boot_scan,
    R.string.gamespace_boot_tune,
    R.string.gamespace_boot_ready,
)

/**
 * Expressive Material 3 Intro screen:
 * - Spring-physics driven shape-morphing emblem.
 * - Floating playful geometric gaming satellites (Square, Triangle, Cross, Circle).
 * - Connected morphing pill stage indicators.
 * - Tap to skip immediately. Max duration: 2.8s.
 */
@Composable
fun ZXGameSpaceIntro(onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var stageIndex by remember { mutableIntStateOf(0) }
    var handedOff by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    val finishOnce = {
        if (!handedOff) {
            handedOff = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GAME_SPACE_INTRO_MS, easing = FastOutSlowInEasing)
            )
            finishOnce()
        }

        // Stepping through the 4 boot stages cleanly across 2.6s before final handoff.
        delay(350)
        stageIndex = 1
        delay(650)
        stageIndex = 2
        delay(750)
        stageIndex = 3
        delay(850)
        finishOnce()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch { runCatching { progress.snapTo(1f) } }
                    finishOnce()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        ExpressiveAmbientWash()
        ExpressiveFloatingGlyphs(stageIndex)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ExpressiveHeroEmblem(stageIndex)
                Spacer(Modifier.height(18.dp))
                ExpressiveWordmark(stageIndex)
                Spacer(Modifier.height(8.dp))
                ExpressiveModeBadge(stageIndex)
            }

            ExpressiveStageTracker(
                stageIndex = stageIndex,
                progress = progress.value
            )
        }
    }
}

/** Soft breathing radial glow in tonal container hues. */
@Composable
private fun ExpressiveAmbientWash() {
    val scheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlowTransition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientPulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.42f * pulse),
                            scheme.tertiaryContainer.copy(alpha = 0.24f * pulse),
                            Color.Transparent
                        ),
                        radius = size.minDimension * 0.75f
                    )
                )
            }
    )
}

/**
 * 4 Playful gaming glyph satellites floating around the center with spring physics
 * and harmonic floating oscillation.
 */
@Composable
private fun ExpressiveFloatingGlyphs(stageIndex: Int) {
    val scheme = MaterialTheme.colorScheme
    val enterAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 1) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "GlyphsEnter"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "GlyphOrbit")
    val orbitTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitTime"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enterAnim }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radiusX = size.width * 0.28f * enterAnim
        val radiusY = size.height * 0.28f * enterAnim
        val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round)

        // Glyph 1: Rounded Square (Top-Left)
        val x1 = cx - radiusX + cos(orbitTime.toDouble()).toFloat() * 18f
        val y1 = cy - radiusY + sin(orbitTime.toDouble()).toFloat() * 14f
        drawRoundRect(
            color = scheme.primary.copy(alpha = 0.38f),
            topLeft = Offset(x1 - 18f, y1 - 18f),
            size = Size(36f, 36f),
            cornerRadius = CornerRadius(10f, 10f),
            style = stroke
        )

        // Glyph 2: Triangle (Top-Right)
        val x2 = cx + radiusX + sin(orbitTime.toDouble() + 1.5).toFloat() * 16f
        val y2 = cy - radiusY + cos(orbitTime.toDouble() + 1.5).toFloat() * 18f
        val tri = Path().apply {
            moveTo(x2, y2 - 20f)
            lineTo(x2 + 20f, y2 + 16f)
            lineTo(x2 - 20f, y2 + 16f)
            close()
        }
        drawPath(tri, color = scheme.tertiary.copy(alpha = 0.38f), style = stroke)

        // Glyph 3: Cross / X (Bottom-Left)
        val x3 = cx - radiusX + sin(orbitTime.toDouble() + 3.0).toFloat() * 18f
        val y3 = cy + radiusY + cos(orbitTime.toDouble() + 3.0).toFloat() * 14f
        drawLine(
            color = scheme.secondary.copy(alpha = 0.38f),
            start = Offset(x3 - 14f, y3 - 14f),
            end = Offset(x3 + 14f, y3 + 14f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = scheme.secondary.copy(alpha = 0.38f),
            start = Offset(x3 + 14f, y3 - 14f),
            end = Offset(x3 - 14f, y3 + 14f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Glyph 4: Circle (Bottom-Right)
        val x4 = cx + radiusX + cos(orbitTime.toDouble() + 4.5).toFloat() * 14f
        val y4 = cy + radiusY + sin(orbitTime.toDouble() + 4.5).toFloat() * 18f
        drawCircle(
            color = scheme.primary.copy(alpha = 0.38f),
            radius = 18f,
            center = Offset(x4, y4),
            style = stroke
        )
    }
}

/** Expressive Morphing Emblem that springs in with bouncy physics. */
@Composable
private fun ExpressiveHeroEmblem(stageIndex: Int) {
    val scheme = MaterialTheme.colorScheme
    val scaleAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 0) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "EmblemScale"
    )
    val morphCorner by animateDpAsState(
        targetValue = if (stageIndex >= 2) 34.dp else 48.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "EmblemCorner"
    )

    Surface(
        shape = RoundedCornerShape(morphCorner),
        color = scheme.primaryContainer,
        modifier = Modifier
            .size(108.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .border(2.dp, scheme.primary.copy(alpha = 0.45f), RoundedCornerShape(morphCorner))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                colorFilter = ColorFilter.tint(scheme.onPrimaryContainer),
                modifier = Modifier.size(54.dp)
            )
        }
    }
}

/** Typography reveal with snappy spring physics. */
@Composable
private fun ExpressiveWordmark(stageIndex: Int) {
    val scheme = MaterialTheme.colorScheme
    val enterAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 1) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "WordmarkEnter"
    )

    Text(
        text = stringResource(R.string.app_name).uppercase(),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (8f - 4f * enterAnim).sp
        ),
        color = scheme.onSurface,
        modifier = Modifier.graphicsLayer {
            alpha = enterAnim
            translationY = 18f * (1f - enterAnim)
        }
    )
}

/** Playful Game Space pill badge with controller icon. */
@Composable
private fun ExpressiveModeBadge(stageIndex: Int) {
    val scheme = MaterialTheme.colorScheme
    val enterAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 1) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "BadgeEnter"
    )

    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
        modifier = Modifier.graphicsLayer {
            alpha = enterAnim
            scaleX = enterAnim
            scaleY = enterAnim
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.gamespace_mode_name).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = scheme.onSurface
            )
        }
    }
}

/**
 * Bottom Expressive Stage Tracker:
 * - Dynamic stage text with smooth animated swap.
 * - Connected morphing pill indicators with animated width and colors.
 */
@Composable
private fun ExpressiveStageTracker(
    stageIndex: Int,
    progress: Float
) {
    val scheme = MaterialTheme.colorScheme
    val safeStage = stageIndex.coerceIn(0, BOOT_STAGES.lastIndex)
    val isReady = safeStage == BOOT_STAGES.lastIndex

    Column(
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedContent(
            targetState = safeStage,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f))
                    .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.05f))
            },
            label = "StageText"
        ) { index ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isReady) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(BOOT_STAGES[index]),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isReady) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isReady) scheme.primary else scheme.onSurfaceVariant
                )
            }
        }

        // 4 Connected Morphing Stage Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BOOT_STAGES.indices.forEach { index ->
                val active = index == safeStage
                val completed = index < safeStage
                val pillWidth by animateDpAsState(
                    targetValue = if (active) 28.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "PillWidth"
                )
                val pillColor = when {
                    active -> scheme.primary
                    completed -> scheme.primary.copy(alpha = 0.5f)
                    else -> scheme.surfaceContainerHighest
                }

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(pillWidth)
                        .clip(CircleShape)
                        .background(pillColor)
                )
            }
        }
    }
}
