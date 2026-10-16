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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
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

/** Length of the Game Space boot sequence. The rail's clock runs exactly this long. */
const val GAME_SPACE_INTRO_MS = 3000

/** The four stages the shell walks through, in the order the rail reports them. */
private val BOOT_STAGES = listOf(
    R.string.gamespace_boot_link,
    R.string.gamespace_boot_scan,
    R.string.gamespace_boot_tune,
    R.string.gamespace_boot_ready,
)

// When each entrance step lands, as a fraction of the clock: mark, wordmark, hairline, stage
// label, then the rail's last step. Fractions instead of fixed ms so the sequence keeps its
// shape whenever the length changes.
private val STAGE_AT = listOf(0.016f, 0.088f, 0.212f, 0.392f, 0.832f)

/**
 * The boot intro: one clock owns the walk and the handoff, a separate stagger walks the layers in,
 * and every layer moves on the theme's [MaterialTheme.motionScheme] — spatial specs for anything
 * that travels, effects specs for alpha — rather than hand-rolled tweens.
 *
 * Tapping skips: the clock's own coroutine is cancelled by the snap, so [onFinished] is guarded to
 * fire once whichever path gets there first.
 */
@Composable
fun ZXGameSpaceIntro(onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val clock = remember { Animatable(0f) }
    var stage by remember { mutableIntStateOf(0) }
    var handedOff by remember { mutableStateOf(false) }

    val finishOnce = {
        if (!handedOff) {
            handedOff = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        var elapsed = 0f
        STAGE_AT.forEachIndexed { index, at ->
            delay(((at - elapsed) * GAME_SPACE_INTRO_MS).toLong())
            stage = index + 1
            elapsed = at
        }
    }

    LaunchedEffect(Unit) {
        clock.animateTo(1f, tween(GAME_SPACE_INTRO_MS, easing = LinearEasing))
        finishOnce()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch { runCatching { clock.snapTo(1f) } }
                    finishOnce()
                }
            }
    ) {
        StageGlow(stage)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            HeroMark(stage)
            Spacer(Modifier.height(30.dp))
            Wordmark(stage)
            Spacer(Modifier.height(18.dp))
            Hairline(stage)
            Spacer(Modifier.height(18.dp))
            ShellLabel(stage)
            Spacer(Modifier.weight(1f))
            BootRail(clock = clock, step = (stage - 1).coerceIn(0, BOOT_STAGES.lastIndex))
        }
    }
}

/**
 * Accent wash under the mark. Container roles rather than the accents themselves, so the wash stays
 * a tonal tint of the wallpaper palette instead of an accent field over the surface.
 *
 * One pulse drives both gradients — a second clock at a different period reads as the stage
 * shaking rather than breathing. Both reads sit in the draw phase, so the wash repaints without
 * recomposing the intro.
 */
@Composable
private fun StageGlow(stage: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val enter = animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "stageGlow"
    )
    val pulse = rememberInfiniteTransition(label = "stageGlowPulse").animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "stageGlowPulseValue"
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val p = pulse.value
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = 0.55f * p),
                            scheme.tertiaryContainer.copy(alpha = 0.30f * p),
                            Color.Transparent
                        )
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            scheme.surface.copy(alpha = 0.85f * enter.value)
                        )
                    )
                )
            }
    )
}

/** Launcher mark on its expressive container: arrives on a spring, overshoot and all. */
@Composable
private fun HeroMark(stage: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val enter = animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "heroMark"
    )
    val fade = animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "heroMarkFade"
    )

    Box(
        modifier = Modifier
            .size(136.dp)
            .graphicsLayer {
                val e = enter.value
                scaleX = 0.68f + 0.32f * e
                scaleY = 0.68f + 0.32f * e
                alpha = fade.value
            }
            .clip(CircleShape)
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            colorFilter = ColorFilter.tint(scheme.onPrimaryContainer),
            modifier = Modifier.size(66.dp)
        )
    }
}

/** The wordmark, tracking opening as it rises. */
@Composable
private fun Wordmark(stage: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val enter = animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "wordmark"
    )

    // Tracking is layout-affecting, so this read stays in composition.
    val rise = enter.value
    Text(
        text = stringResource(R.string.app_name).uppercase(),
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (18f - 12f * rise).sp
        ),
        color = scheme.onSurface,
        modifier = Modifier.graphicsLayer {
            alpha = rise
            translationY = 22f * (1f - rise)
        }
    )
}

/** Hairline that grows out of the centre under the wordmark. */
@Composable
private fun Hairline(stage: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val grow = animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "hairline"
    )
    val fade = animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "hairlineFade"
    )

    Box(
        Modifier
            .width(340.dp)
            .height(2.dp)
            .graphicsLayer {
                scaleX = grow.value
                alpha = 0.9f * fade.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, scheme.primary, scheme.tertiary, Color.Transparent)
                )
            )
    )
}

/** The shell's name, spreading out under the hairline. */
@Composable
private fun ShellLabel(stage: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val enter = animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "shellLabel"
    )

    val rise = enter.value
    Text(
        text = stringResource(R.string.gamespace_mode_name).uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = (4f + 12f * rise).sp
        ),
        color = scheme.primary,
        modifier = Modifier.graphicsLayer {
            alpha = rise
            translationY = 14f * (1f - rise)
        }
    )
}

/** Boot rail: the stage line steps through the four stages, the bar is the clock itself. */
@Composable
private fun BootRail(clock: Animatable<Float, AnimationVector1D>, step: Int) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec())
            },
            label = "bootStage"
        ) { index ->
            Text(
                text = stringResource(BOOT_STAGES[index]).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(20.dp))

        Box(
            Modifier
                .weight(1f)
                .height(3.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = clock.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary)))
            )
        }

        Spacer(Modifier.width(20.dp))

        Text(
            text = "${(clock.value * 100f).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurface
        )
    }
}
