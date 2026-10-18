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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Total length of the Game Space expressive intro animation (at max 2800ms <= 3s). */
const val GAME_SPACE_INTRO_MS = 2600

private val BOOT_STAGES = listOf(
    R.string.gamespace_boot_link,
    R.string.gamespace_boot_scan,
    R.string.gamespace_boot_tune,
    R.string.gamespace_boot_ready,
)

/**
 * Material 3 Expressive Intro Screen:
 * - The AZenith mascot silhouette drops from max size to small with energetic spring bounce physics.
 * - Solid Material 3 shapes (circles, capsules, diamonds, rounded squares) explode outward from the impact center.
 * - Clean silhouette without background container cards or dark vignetted glows.
 * - Morphing connected stage pills with stage status indicators.
 * - Seamless exit transition dissolving into Game Space Shell.
 */
@Composable
fun ZXGameSpaceIntro(
    onTransitionStart: () -> Unit = {},
    onFinished: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var stageIndex by remember { mutableIntStateOf(0) }
    var handedOff by remember { mutableStateOf(false) }

    val progress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }

    // Silhouette falling & bouncing physics from max size to small
    val logoScale = remember { Animatable(2.8f) }
    val logoAlpha = remember { Animatable(0f) }

    // Exploding Material 3 shapes burst
    val explosionProgress = remember { Animatable(0f) }

    val startExit = {
        if (!handedOff) {
            handedOff = true
            onTransitionStart()
            scope.launch {
                exitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
                onFinished()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Logo reveals immediately with an energetic punch
        launch {
            logoAlpha.animateTo(1f, tween(100, easing = LinearEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Exploding Material 3 shapes burst immediately on first open, then linger & drift slowly
        launch {
            explosionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GAME_SPACE_INTRO_MS, easing = LinearEasing)
            )
        }

        // Total progress progression
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GAME_SPACE_INTRO_MS, easing = FastOutSlowInEasing)
            )
            startExit()
        }

        // 4 Boot Stages
        delay(350)
        stageIndex = 1
        delay(600)
        stageIndex = 2
        delay(650)
        stageIndex = 3
        delay(750)
        startExit()
    }

    val exitVal = exitProgress.value
    val exitAlpha = (1f - exitVal * 1.3f).coerceIn(0f, 1f)
    val exitTranslationDown = 32f * exitVal
    val exitScale = 1f + 0.26f * exitVal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface.copy(alpha = (1f - exitVal).coerceIn(0f, 1f)))
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch { runCatching { progress.snapTo(1f) } }
                    startExit()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Exploding Material 3 shapes burst from the center impact point
        if (explosionProgress.value > 0f) {
            Material3ImpactExplosion(
                progress = explosionProgress.value,
                exitAlpha = exitAlpha
            )
        }

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
                // AZenith mascot silhouette directly on screen (no background card/circle)
                Image(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(scheme.primary),
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            val totalScale = logoScale.value * exitScale
                            scaleX = totalScale
                            scaleY = totalScale
                            alpha = logoAlpha.value * exitAlpha
                        }
                )

                Spacer(Modifier.height(18.dp))

                ExpressiveWordmark(
                    stageIndex = stageIndex,
                    exitTranslationY = exitTranslationDown,
                    exitAlpha = exitAlpha
                )

                Spacer(Modifier.height(8.dp))

                ExpressiveModeBadge(
                    stageIndex = stageIndex,
                    exitTranslationY = exitTranslationDown,
                    exitAlpha = exitAlpha
                )
            }

            ExpressiveStageTracker(
                stageIndex = stageIndex,
                progress = progress.value,
                exitTranslationY = exitTranslationDown * 1.2f,
                exitAlpha = exitAlpha
            )
        }
    }
}

/**
 * Exploding Material 3 shapes bursting outward from the impact center:
 * - Solid filled MD3 shapes (circles, capsules, diamonds, rounded squares).
 * - Fast initial burst outward followed by smooth lingering drift across the screen.
 * - Gentle fade-out near the end of the intro rather than vanishing quickly.
 */
@Composable
private fun Material3ImpactExplosion(
    progress: Float,
    exitAlpha: Float = 1f
) {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(
            scheme.primary,
            scheme.secondary,
            scheme.tertiary,
            scheme.primaryContainer,
            scheme.secondaryContainer,
            scheme.tertiaryContainer
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f - 40.dp.toPx() // Align with silhouette center
        val particleCount = 20
        val baseBurstRadius = 140.dp.toPx()
        val driftDistance = 80.dp.toPx()

        // Fast initial burst curve (0..0.25) that eases out into slow drift (0.25..1.0)
        val burstFraction = (progress / 0.22f).coerceIn(0f, 1f)
        val burstDistance = 1f - (1f - burstFraction) * (1f - burstFraction) * (1f - burstFraction)
        val driftFraction = ((progress - 0.22f) / 0.78f).coerceIn(0f, 1f)

        // Particle scale rises fast, sustains in full view, and fades out gently near exit
        val particleScale = when {
            progress < 0.08f -> progress / 0.08f
            progress > 0.82f -> ((1f - progress) / 0.18f).coerceIn(0f, 1f)
            else -> 1f
        }

        // Particle alpha stays prominent and lingers until transition
        val particleAlpha = when {
            progress > 0.80f -> (((1f - progress) / 0.20f) * 0.95f).coerceIn(0f, 0.95f)
            else -> 0.95f
        }

        for (i in 0 until particleCount) {
            val angleDeg = (i * (360f / particleCount) + (i * 11f)) % 360f
            val angleRad = angleDeg * (PI / 180f).toFloat()

            // Distance with individual radial multiplier and continuous slow outward drift
            val radialMultiplier = 0.75f + (i % 6) * 0.10f
            val totalDistance = (baseBurstRadius * burstDistance + driftDistance * driftFraction) * radialMultiplier

            val px = cx + cos(angleRad) * totalDistance
            val py = cy + sin(angleRad) * totalDistance

            val color = palette[i % palette.size].copy(alpha = particleAlpha)

            when (i % 4) {
                0 -> {
                    // Solid filled Circle
                    val radius = (7.5f.dp.toPx() + (i % 3) * 2.5f.dp.toPx()) * particleScale
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(px, py)
                    )
                }
                1 -> {
                    // Solid filled Capsule / Pill
                    val pw = (24.dp.toPx() + (i % 2) * 6.dp.toPx()) * particleScale
                    val ph = 10.dp.toPx() * particleScale
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(px - pw / 2f, py - ph / 2f),
                        size = Size(pw, ph),
                        cornerRadius = CornerRadius(ph / 2f, ph / 2f)
                    )
                }
                2 -> {
                    // Solid filled 4-Point Diamond Sparkle
                    val s = (15.dp.toPx() + (i % 2) * 4.dp.toPx()) * particleScale
                    val diamond = Path().apply {
                        moveTo(px, py - s)
                        lineTo(px + s * 0.55f, py)
                        lineTo(px, py + s)
                        lineTo(px - s * 0.55f, py)
                        close()
                    }
                    drawPath(path = diamond, color = color)
                }
                else -> {
                    // Solid filled Rounded Square
                    val s = (13.dp.toPx() + (i % 2) * 3.dp.toPx()) * particleScale
                    val cr = 4.5f.dp.toPx() * particleScale
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(px - s / 2f, py - s / 2f),
                        size = Size(s, s),
                        cornerRadius = CornerRadius(cr, cr)
                    )
                }
            }
        }
    }
}

/** Typography reveal with snappy spring physics and exit translation. */
@Composable
private fun ExpressiveWordmark(
    stageIndex: Int,
    exitTranslationY: Float = 0f,
    exitAlpha: Float = 1f
) {
    val scheme = MaterialTheme.colorScheme
    val enterAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 0) 1f else 0f,
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
            alpha = enterAnim * exitAlpha
            translationY = 18f * (1f - enterAnim) + exitTranslationY
        }
    )
}

/** Playful Game Space pill badge with controller icon and exit translation. */
@Composable
private fun ExpressiveModeBadge(
    stageIndex: Int,
    exitTranslationY: Float = 0f,
    exitAlpha: Float = 1f
) {
    val scheme = MaterialTheme.colorScheme
    val enterAnim by animateFloatAsState(
        targetValue = if (stageIndex >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "BadgeEnter"
    )

    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
        modifier = Modifier.graphicsLayer {
            alpha = enterAnim * exitAlpha
            scaleX = enterAnim
            scaleY = enterAnim
            translationY = exitTranslationY
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
 * - Graceful exit downward translation.
 */
@Composable
private fun ExpressiveStageTracker(
    stageIndex: Int,
    progress: Float,
    exitTranslationY: Float = 0f,
    exitAlpha: Float = 1f
) {
    val scheme = MaterialTheme.colorScheme
    val safeStage = stageIndex.coerceIn(0, BOOT_STAGES.lastIndex)
    val isReady = safeStage == BOOT_STAGES.lastIndex

    Column(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .graphicsLayer {
                alpha = exitAlpha
                translationY = exitTranslationY
            },
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
