/*
 * Copyright 2025 AZenith
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

package zx.azenith.ui.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.compose.rememberConfettiKitState
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import io.github.vinceglb.confettikit.core.models.Shape
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.res.stringResource
import zx.azenith.R
import zx.azenith.ui.theme.currentPersonalization

/**
 * Wraps [BannerCard] with a hold-to-reveal easter egg: long-pressing the banner buzzes, crossfades
 * it into a gradient Monet card carrying the slogan, and bursts confetti in from both edges.
 * Long-pressing again reverts it. Deliberately not persisted — the egg is a one-session surprise,
 * so it always comes back hidden on the next launch.
 */
@Composable
fun BannerWithEasterEgg(
    status: String,
    pid: String,
    isBannerEnabled: Boolean,
    isBlurEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Neutral,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var activated by remember { mutableStateOf(false) }

    // Bumping this rebuilds the parties list, which is the LaunchedEffect key ConfettiKit's frame
    // loop uses — that is what actually restarts the burst. Keying it on activation alone would
    // only ever fire once per process.
    var burst by remember { mutableIntStateOf(0) }
    val parties = remember(burst) { twoSidedParade() }
    val confettiState = rememberConfettiKitState()

    // Hold-only: scale down while the finger is down and spring back on release.
    // No tap pulse, because a tap is over before the spring has travelled.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(percent = (currentPersonalization().bannerRadius * 100).toInt()))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = { onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    buzz(context)
                    activated = !activated
                    if (activated) {
                        confettiState.reset()
                        burst++
                    }
                }
            )
    ) {
        AnimatedContent(
            targetState = activated,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(tween(420)) + scaleIn(tween(420), initialScale = 1.03f))
                        .togetherWith(fadeOut(tween(240)))
                } else {
                    (fadeIn(tween(360)) + scaleIn(tween(360), initialScale = 0.98f))
                        .togetherWith(fadeOut(tween(240)) + scaleOut(tween(240), targetScale = 1.03f))
                }
            },
            label = "BannerEasterEggSwap"
        ) { isActive ->
            if (isActive) {
                EasterEggCard(compact = !isBannerEnabled)
            } else {
                BannerCard(
                    status = status,
                    pid = pid,
                    isBannerEnabled = isBannerEnabled,
                    isBlurEnabled = isBlurEnabled,
                    modifier = Modifier.fillMaxSize(),
                    clickable = false,
                    tone = tone,
                    onClick = {}
                )
            }
        }

        // Drawn last so particles land on top of the card rather than behind it.
        if (burst > 0) {
            ConfettiKit(
                modifier = Modifier.fillMaxSize(),
                parties = parties,
                state = confettiState
            )
        }
    }
}

/** One party mirrored: a burst from the left edge and a matching one from the right. */
private fun twoSidedParade(): List<Party> {
    // Velocity is (cos(angle), sin(angle)) with +y pointing down, so -45° flies right-and-up.
    // Each party must spawn on the edge it flies away FROM, or every particle exits the canvas
    // within a frame and nothing is ever visible.
    val fromLeft = Party(
        angle = Angle.RIGHT - 45,
        spread = Spread.SMALL,
        speed = 10f,
        maxSpeed = 30f,
        damping = 0.9f,
        colors = listOf(0xFFFCE18A, 0xFFFF726D, 0xFFF4306D, 0xFFB48DEF).map { it.toInt() },
        shapes = listOf(Shape.CustomShape(RoundedCornerShape(5.dp)), Shape.Circle),
        // A 600 ms window finishes inside the first couple of dropped frames on this device, so the
        // burst never becomes visible. Widen it and give particles a matching lifetime.
        emitter = Emitter(duration = 1600.milliseconds).max(90),
        timeToLive = 2600L,
        position = Position.Relative(0.0, 0.5)
    )
    return listOf(
        fromLeft,
        fromLeft.copy(
            angle = fromLeft.angle - 90,
            position = Position.Relative(1.0, 0.5)
        )
    )
}

private fun buzz(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
}

@Composable
private fun EasterEggCard(compact: Boolean, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    // With the banner image off the card collapses to a 100 dp strip, where the full-size type
    // overflows. `isBannerEnabled` decides which of the two layouts we are in far more reliably
    // than measuring — inside AnimatedContent the measured height is the host's, not the card's.
    //
    // primary -> tertiary stays inside one tonal band in every M3 scheme, so a single on-* color
    // reads across the whole ramp. The candidates below are checked against it anyway because
    // dynamic color can hand us a scheme whose tertiary drifts lighter than its own on-color.
    val start = colorScheme.primary
    val end = colorScheme.tertiary
    val sheen = colorScheme.onPrimary.copy(alpha = SHEEN_ALPHA)
    val mid = lerp(start, end, 0.72f)

    val textColor = remember(colorScheme) {
        readableOn(
            backgrounds = listOf(
                start,
                mid,
                end,
                lerp(end, colorScheme.onPrimary, SHEEN_ALPHA)
            ),
            candidates = listOf(
                colorScheme.onPrimary,
                colorScheme.onTertiary,
                colorScheme.surface,
                colorScheme.onSurface
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Same percent-based shape as BannerCard, so swapping the banner for
            // the easter egg does not change the silhouette. A fixed dp radius
            // here read as a different corner from the one it replaces.
            .clip(
                RoundedCornerShape(percent = (currentPersonalization().bannerRadius * 100).toInt())
            )
            .background(
                Brush.linearGradient(
                    0f to start,
                    0.5f to mid,
                    1f to end,
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .background(
                Brush.radialGradient(
                    0f to sheen,
                    1f to Color.Transparent,
                    center = Offset(0f, 0f),
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = if (compact) 20.dp else 32.dp)
        ) {
            AnimatedSlogan(
                color = textColor,
                titleSize = if (compact) 16.sp else 27.sp,
                subtitleSize = if (compact) 12.sp else 19.sp,
                lineGap = if (compact) 1.dp else 5.dp
            )
        }
    }
}

private const val SHEEN_ALPHA = 0.12f

/** WCAG relative-luminance contrast between two opaque colors. */
private fun contrastRatio(a: Color, b: Color): Float {
    fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else
        ((c + 0.055f) / 1.055f).pow(2.4f)
    val la = 0.2126f * channel(a.red) + 0.7152f * channel(a.green) + 0.0722f * channel(a.blue)
    val lb = 0.2126f * channel(b.red) + 0.7152f * channel(b.green) + 0.0722f * channel(b.blue)
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

/**
 * First [candidates] entry clearing 4.5:1 against every background, else white on the darkest one.
 * Lets the gradient run without putting a scrim or plate behind the slogan.
 */
private fun readableOn(
    backgrounds: List<Color>,
    candidates: List<Color>
): Color = candidates.firstOrNull { c -> backgrounds.all { contrastRatio(c, it) >= 4.5f } }
    ?: candidates.maxByOrNull { c -> backgrounds.minOf { contrastRatio(c, it) } }
    ?: Color.White

/**
 * The two slogan clauses rise and fade in on a stagger off a single clock. Driving both lines from
 * two separate [rememberInfiniteTransition]-style clocks would make them drift against each other,
 * so one [Animatable] pair plus a fixed offset is what keeps the timing locked.
 */
@Composable
private fun AnimatedSlogan(
    color: Color,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    lineGap: Dp
) {
    val lead = remember { Animatable(0f) }
    val follow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        launch { lead.animateTo(1f, spec) }
        launch {
            delay(140)
            follow.animateTo(1f, spec)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SloganLine(
            text = stringResource(R.string.banner_slogan_title),
            fontSize = titleSize,
            weight = FontWeight.ExtraBold,
            progress = lead,
            color = color
        )
        SloganLine(
            text = stringResource(R.string.banner_slogan_subtitle),
            fontSize = subtitleSize,
            weight = FontWeight.SemiBold,
            progress = follow,
            color = color,
            modifier = Modifier.padding(top = lineGap)
        )
    }
}

@Composable
private fun SloganLine(
    text: String,
    fontSize: TextUnit,
    weight: FontWeight,
    progress: Animatable<Float, *>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = weight,
        letterSpacing = 0.4.sp,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            // Rises into place rather than fading in flat.
            translationY = 26.dp.toPx() * (1f - progress.value)
            scaleX = 0.94f + 0.06f * progress.value
            scaleY = 0.94f + 0.06f * progress.value
        }
    )
}
