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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.toPath
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Total length of the Game Space expressive intro animation (5000ms). */
const val GAME_SPACE_INTRO_MS = 5000

private val BOOT_STAGES = listOf(
    R.string.gamespace_boot_link,
    R.string.gamespace_boot_scan,
    R.string.gamespace_boot_tune,
    R.string.gamespace_boot_ready,
)

/**
 * Material 3 Expressive Intro Screen:
 * - 5-second choreographed sequence displaying each boot stage clearly.
 * - Mascot silhouette drops and settles in at the start.
 * - Dynamic RNG explosion triggers exclusively upon reaching the final 'Ready' stage.
 * - Particles burst with high visibility, random morphing, continuous spin, and outline shapes.
 * - Lingers for 1s at 'Ready' before cleanly fading and transitioning off screen.
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
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                )
                onFinished()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Logo reveals immediately with an energetic punch
        launch {
            logoAlpha.animateTo(1f, tween(180, easing = LinearEasing))
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

        // Linear overall progress progression (0 to 1 over 5s)
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = GAME_SPACE_INTRO_MS, easing = LinearEasing)
            )
        }

        // 4 Boot Stages across 5s timeline:
        // Stage 0: 0ms -> 1000ms (Link)
        // Stage 1: 1000ms -> 2100ms (Scan)
        // Stage 2: 2100ms -> 3200ms (Tune)
        // Stage 3: 3200ms -> 5000ms (Ready) -> explodes immediately on Ready, lingers for ~1s, then exits cleanly
        delay(1000)
        stageIndex = 1
        delay(1100)
        stageIndex = 2
        delay(1100)
        stageIndex = 3

        // Explode only upon reaching the 'Ready' stage
        launch {
            explosionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
        }

        // Linger on 'Ready' for 1 second before starting clean exit transition
        delay(1000)
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

private data class ExplosionParticle(
    val baseAngleDeg: Float,
    val radialMultiplier: Float,
    val shapeA: RoundedPolygon,
    val shapeB: RoundedPolygon,
    val isOutlined: Boolean,
    val strokeWidthDp: Float,
    val baseScaleDp: Float,
    val initialSpinDeg: Float,
    val spinSpeedDegPerSec: Float,
    val colorIndex: Int
)

/**
 * Exploding Material 3 shapes bursting outward from the impact center:
 * - Dynamic RNG generation per intro invocation for unique burst patterns.
 * - Solid filled and outline-only Material 3 shapes (circles, bursts, clovers, suns, cookies).
 * - Continuous spin rotation accelerating from impact and drifting gently.
 * - Smooth morphological transformation between shapes while lingering.
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

    val availableShapes = remember {
        listOf(
            BentoShapes.Circle,
            BentoShapes.Cookie4,
            BentoShapes.Cookie6,
            BentoShapes.Cookie9,
            BentoShapes.Clover4,
            BentoShapes.Clover8,
            BentoShapes.Sunny,
            BentoShapes.VerySunny,
            BentoShapes.Puffy,
            BentoShapes.SoftBurst,
            BentoShapes.Flower
        )
    }

    val particles = remember {
        val count = 26
        val rng = kotlin.random.Random(System.currentTimeMillis())
        List(count) { i ->
            val angleDeg = (i * (360f / count) + rng.nextFloat() * 20f - 10f) % 360f
            val shape1 = availableShapes[rng.nextInt(availableShapes.size)]
            var shape2 = availableShapes[rng.nextInt(availableShapes.size)]
            if (shape1 === shape2) {
                shape2 = availableShapes[(availableShapes.indexOf(shape1) + 1) % availableShapes.size]
            }
            ExplosionParticle(
                baseAngleDeg = angleDeg,
                radialMultiplier = 0.70f + rng.nextFloat() * 0.60f,
                shapeA = shape1,
                shapeB = shape2,
                isOutlined = rng.nextFloat() < 0.35f,
                strokeWidthDp = 2.4f + rng.nextFloat() * 1.6f,
                baseScaleDp = 22f + rng.nextFloat() * 18f,
                initialSpinDeg = rng.nextFloat() * 360f,
                spinSpeedDegPerSec = (if (rng.nextBoolean()) 1f else -1f) * (80f + rng.nextFloat() * 200f),
                colorIndex = rng.nextInt(6)
            )
        }
    }

    val morphs = remember(particles) {
        particles.map { Morph(it.shapeA, it.shapeB) }
    }

    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f - 40.dp.toPx()
        val baseBurstRadius = 175.dp.toPx()
        val driftDistance = 110.dp.toPx()

        // Fast initial explosive curve (0..0.22) followed by gentle lingering drift
        val burstFraction = (progress / 0.22f).coerceIn(0f, 1f)
        val burstDistance = 1f - (1f - burstFraction) * (1f - burstFraction) * (1f - burstFraction)
        val driftFraction = ((progress - 0.22f) / 0.78f).coerceIn(0f, 1f)

        // Particle scale punch
        val particleScale = when {
            progress < 0.08f -> (progress / 0.08f) * 1.15f
            progress < 0.18f -> 1.15f - ((progress - 0.08f) / 0.10f) * 0.15f
            progress > 0.85f -> ((1f - progress) / 0.15f).coerceIn(0f, 1f)
            else -> 1f
        }

        // Particle alpha stays crisp and lingers smoothly
        val particleAlpha = when {
            progress > 0.85f -> (((1f - progress) / 0.15f) * 0.98f).coerceIn(0f, 0.98f)
            else -> 0.98f
        }

        val totalTimeSec = progress * 1.8f

        particles.forEachIndexed { index, particle ->
            val angleRad = particle.baseAngleDeg * (PI / 180f).toFloat()
            val totalDistance = (baseBurstRadius * burstDistance + driftDistance * driftFraction) * particle.radialMultiplier

            val px = cx + cos(angleRad) * totalDistance
            val py = cy + sin(angleRad) * totalDistance

            val color = palette[particle.colorIndex % palette.size].copy(alpha = particleAlpha)
            val currentRotation = particle.initialSpinDeg + (particle.spinSpeedDegPerSec * totalTimeSec)

            // Morph between shapeA and shapeB across the lingering lifecycle
            val morphFraction = (sin((progress * PI * 1.5f).coerceIn(0.0, PI * 1.5f)).toFloat()).coerceIn(0f, 1f)

            val sizePx = particle.baseScaleDp.dp.toPx() * particleScale

            path.rewind()
            val outline = morphs[index].toPath(morphFraction, path)
            matrix.reset()
            matrix.scale(sizePx, sizePx)
            outline.transform(matrix)

            rotate(currentRotation, pivot = Offset(px, py)) {
                translate(left = px - sizePx / 2f, top = py - sizePx / 2f) {
                    if (particle.isOutlined) {
                        drawPath(
                            path = outline,
                            color = color,
                            style = Stroke(
                                width = particle.strokeWidthDp.dp.toPx() * particleScale,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else {
                        drawPath(
                            path = outline,
                            color = color
                        )
                    }
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
