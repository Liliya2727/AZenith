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

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.GamepadFocusNav
import zx.azenith.ui.component.GamepadInput
import kotlin.math.roundToInt

private val BENTO_GAP = 12.dp
private const val BENTO_STAGGER_MS = 55L
private const val SWAP_TURN_DEG = 120f

/** Normalised once so every Morph built from them is matched on stable instances. */
internal object BentoShapes {
    val SoftBurst: RoundedPolygon by lazy { MaterialShapes.SoftBurst.normalized() }
    val Cookie4: RoundedPolygon by lazy { MaterialShapes.Cookie4Sided.normalized() }
    val Cookie6: RoundedPolygon by lazy { MaterialShapes.Cookie6Sided.normalized() }
    val Cookie9: RoundedPolygon by lazy { MaterialShapes.Cookie9Sided.normalized() }
    val Cookie12: RoundedPolygon by lazy { MaterialShapes.Cookie12Sided.normalized() }
    val Clover4: RoundedPolygon by lazy { MaterialShapes.Clover4Leaf.normalized() }
    val Clover8: RoundedPolygon by lazy { MaterialShapes.Clover8Leaf.normalized() }
    val Sunny: RoundedPolygon by lazy { MaterialShapes.Sunny.normalized() }
    val VerySunny: RoundedPolygon by lazy { MaterialShapes.VerySunny.normalized() }
    val Pentagon: RoundedPolygon by lazy { MaterialShapes.Pentagon.normalized() }
    val Gem: RoundedPolygon by lazy { MaterialShapes.Gem.normalized() }
    val Puffy: RoundedPolygon by lazy { MaterialShapes.Puffy.normalized() }
    val Flower: RoundedPolygon by lazy { MaterialShapes.Flower.normalized() }
    val Heart: RoundedPolygon by lazy { MaterialShapes.Heart.normalized() }
    val Burst: RoundedPolygon by lazy { MaterialShapes.Burst.normalized() }
    val Arch: RoundedPolygon by lazy { MaterialShapes.Arch.normalized() }
    val Slanted: RoundedPolygon by lazy { MaterialShapes.Slanted.normalized() }
    val Circle: RoundedPolygon by lazy { MaterialShapes.Circle.normalized() }
}

/**
 * Bento container: staggered spring entrance, squish + corner morph on press, lift on d-pad focus.
 * [content] receives the live engagement fraction as a lambda so children animate in the draw
 * phase instead of recomposing every frame.
 */
@Composable
internal fun BentoTile(
    order: Int,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    restCorner: Dp = 28.dp,
    engagedCorner: Dp = 14.dp,
    content: @Composable BoxScope.(engaged: () -> Float) -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val interaction = remember { MutableInteractionSource() }
    val clickPressed by interaction.collectIsPressedAsState()
    var touchPressed by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }

    val press = animateFloatAsState(
        targetValue = if (clickPressed || touchPressed) 1f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "BentoPress"
    )
    val lift = animateFloatAsState(
        targetValue = if (focused && GamepadInput.active) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "BentoLift"
    )
    val fill = animateColorAsState(container, motion.defaultEffectsSpec(), label = "BentoFill")

    val enter = remember { Animatable(0f) }
    val enterSpec = motion.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        delay(order * BENTO_STAGGER_MS)
        enter.animateTo(1f, enterSpec)
    }

    val engaged = { maxOf(press.value, lift.value) }

    val interactive = if (onClick != null) {
        Modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) GamepadFocusNav.publishLastFocused(requester)
            }
            .focusRequester(requester)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                onClick = onClick
            )
    } else {
        // Read-only tiles still answer a touch with the squish, without becoming focus targets.
        Modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                touchPressed = true
                tryAwaitRelease()
                touchPressed = false
            })
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val e = enter.value
                val k = engaged().coerceIn(0f, 1.2f)
                val s = (0.86f + 0.14f * e) * (1f - 0.045f * press.value) * (1f + 0.025f * lift.value)
                scaleX = s
                scaleY = s
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * 28.dp.toPx()
                shape = RoundedCornerShape(lerp(restCorner, engagedCorner, k.coerceIn(0f, 1f)))
                clip = true
            }
            .drawBehind { drawRect(fill.value) }
            .then(interactive)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content(engaged)
        }
    }
}

/**
 * A Material shape that morphs to [shape] with a spin whenever it changes, and toward
 * [pressedShape] while [engaged] rises. The icon stays upright; only the fill rotates.
 */
@Composable
internal fun MorphBadge(
    shape: RoundedPolygon,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    pressedShape: RoundedPolygon? = null,
    engaged: () -> Float = { 0f },
    spin: Float = 0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val motion = MaterialTheme.motionScheme
    val swapSpec = motion.slowSpatialSpec<Float>()
    val tint = animateColorAsState(color, motion.defaultEffectsSpec(), label = "BadgeTint")

    var current by remember { mutableStateOf(shape) }
    // Held as state and read in draw so the morph and its progress swap in the same frame.
    var swapMorph by remember { mutableStateOf(Morph(shape, shape)) }
    var pressMorph by remember(pressedShape) {
        mutableStateOf(pressedShape?.let { Morph(current, it) })
    }
    val swap = remember { Animatable(1f) }
    var turns by remember { mutableIntStateOf(0) }
    val turn = animateFloatAsState(
        targetValue = turns * SWAP_TURN_DEG,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 160f),
        label = "BadgeTurn"
    )

    LaunchedEffect(shape) {
        if (shape === current) return@LaunchedEffect
        swapMorph = Morph(current, shape)
        pressMorph = pressedShape?.let { Morph(shape, it) }
        current = shape
        turns++
        swap.snapTo(0f)
        swap.animateTo(1f, swapSpec)
    }

    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val s = swap.value
                val held = pressMorph
                val morph: Morph
                val progress: Float
                if (s < 1f || held == null) {
                    morph = swapMorph
                    progress = s
                } else {
                    morph = held
                    progress = engaged()
                }
                path.rewind()
                val outline = morph.toPath(progress.coerceIn(0f, 1f), path)
                matrix.reset()
                matrix.scale(this.size.width, this.size.height)
                outline.transform(matrix)
                rotate(turn.value + engaged() * spin) {
                    drawPath(outline, tint.value)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val k = 1f + 0.12f * engaged()
                scaleX = k
                scaleY = k
            },
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun NudgeChevron(engaged: () -> Float, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(24.dp)
            .graphicsLayer { translationX = engaged() * 6.dp.toPx() }
    )
}

/** Rolls a number in from zero every time the stage is entered. */
@Composable
private fun RollingCount(count: Int, color: Color, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(count) {
        shown.animateTo(count.toFloat(), spring(dampingRatio = 1f, stiffness = 90f))
    }
    Text(
        text = shown.value.roundToInt().toString(),
        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

private fun lerpInt(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()

// Morphs the three primary widgets between compact 1-line top row and full bento deck.
@Composable
private fun BentoMorphLayout(
    progress: Float,
    settleBounce: Float = 0f,
    settleDirection: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val gapPx = with(density) { BENTO_GAP.roundToPx() }
    val compactHPx = with(density) { 72.dp.roundToPx() }
    val heroH1Px = with(density) { 184.dp.roundToPx() }
    val row2H1Px = with(density) { 136.dp.roundToPx() }
    val row3H1Px = with(density) { 88.dp.roundToPx() }

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val p = progress.coerceIn(0f, 1f)
        val overshoot = if (progress > 1f) (progress - 1f) else if (progress < 0f) progress else 0f

        val compactItemW = (totalWidth - 2 * gapPx) / 3

        val heroW1 = ((totalWidth - gapPx) * 1.22f / 2.22f).roundToInt()
        val rightColW1 = totalWidth - gapPx - heroW1
        val statusH1 = (heroH1Px - gapPx) / 2

        val row2Y1 = heroH1Px + gapPx
        val libW1 = ((totalWidth - gapPx) * 1f / 2.25f).roundToInt()
        val devW1 = totalWidth - gapPx - libW1

        val row3Y1 = row2Y1 + row2H1Px + gapPx
        val halfW1 = (totalWidth - gapPx) / 2
        val otherHalfW1 = totalWidth - gapPx - halfW1

        val totalDeckH1 = row3Y1 + row3H1Px

        val profileW = lerpInt(compactItemW, heroW1, p)
        val profileH = lerpInt(compactHPx, heroH1Px, p)

        val moduleW = lerpInt(compactItemW, rightColW1, p)
        val moduleH = lerpInt(compactHPx, statusH1, p)
        val moduleX = lerpInt(compactItemW + gapPx, heroW1 + gapPx, p)

        val rootW = lerpInt(compactItemW, rightColW1, p)
        val rootH = lerpInt(compactHPx, statusH1, p)
        val rootX = lerpInt(2 * (compactItemW + gapPx), heroW1 + gapPx, p)
        val rootY = lerpInt(0, statusH1 + gapPx, p)

        val totalHeight = lerpInt(compactHPx, totalDeckH1, p)

        val profilePlaceable = measurables.getOrNull(0)?.measure(
            Constraints.fixed(profileW.coerceAtLeast(0), profileH.coerceAtLeast(0))
        )
        val modulePlaceable = measurables.getOrNull(1)?.measure(
            Constraints.fixed(moduleW.coerceAtLeast(0), moduleH.coerceAtLeast(0))
        )
        val rootPlaceable = measurables.getOrNull(2)?.measure(
            Constraints.fixed(rootW.coerceAtLeast(0), rootH.coerceAtLeast(0))
        )

        val libPlaceable = if (p > 0.001f && measurables.size > 3) {
            measurables[3].measure(Constraints.fixed(libW1.coerceAtLeast(0), row2H1Px.coerceAtLeast(0)))
        } else null
        val devPlaceable = if (p > 0.001f && measurables.size > 4) {
            measurables[4].measure(Constraints.fixed(devW1.coerceAtLeast(0), row2H1Px.coerceAtLeast(0)))
        } else null
        val suppPlaceable = if (p > 0.001f && measurables.size > 5) {
            measurables[5].measure(Constraints.fixed(halfW1.coerceAtLeast(0), row3H1Px.coerceAtLeast(0)))
        } else null
        val docPlaceable = if (p > 0.001f && measurables.size > 6) {
            measurables[6].measure(Constraints.fixed(otherHalfW1.coerceAtLeast(0), row3H1Px.coerceAtLeast(0)))
        } else null

        val b = settleBounce
        val dir = settleDirection
        val dipPx = with(density) { 8.dp.toPx() }
        val reboundCompactPx = with(density) { 6.dp.toPx() }
        val overshootPx = with(density) { 20.dp.toPx() }

        val b0 = b
        val offsetY0 = overshoot * overshootPx + (if (dir > 0f) b0 * dipPx else -b0 * reboundCompactPx)
        val scaleY0 = 1f + overshoot * 0.45f + (if (dir > 0f) b0 * 0.035f else -b0 * 0.035f)
        val scaleX0 = 1f - overshoot * 0.20f + (if (dir > 0f) -b0 * 0.020f else b0 * 0.025f)

        val b1 = b * 0.94f
        val offsetY1 = overshoot * (overshootPx * 0.9f) + (if (dir > 0f) b1 * dipPx else -b1 * reboundCompactPx)
        val scaleY1 = 1f + overshoot * 0.40f + (if (dir > 0f) b1 * 0.032f else -b1 * 0.032f)
        val scaleX1 = 1f - overshoot * 0.18f + (if (dir > 0f) -b1 * 0.018f else b1 * 0.022f)

        val b2 = b * 0.88f
        val offsetY2 = overshoot * (overshootPx * 0.85f) + (if (dir > 0f) b2 * dipPx else -b2 * reboundCompactPx)
        val scaleY2 = 1f + overshoot * 0.40f + (if (dir > 0f) b2 * 0.032f else -b2 * 0.032f)
        val scaleX2 = 1f - overshoot * 0.18f + (if (dir > 0f) -b2 * 0.018f else b2 * 0.022f)

        layout(totalWidth, totalHeight) {
            profilePlaceable?.placeRelativeWithLayer(0, 0) {
                translationY = offsetY0
                scaleX = scaleX0
                scaleY = scaleY0
            }
            modulePlaceable?.placeRelativeWithLayer(moduleX, 0) {
                translationY = offsetY1
                scaleX = scaleX1
                scaleY = scaleY1
            }
            rootPlaceable?.placeRelativeWithLayer(rootX, rootY) {
                translationY = offsetY2
                scaleX = scaleX2
                scaleY = scaleY2
            }

            if (p > 0.001f) {
                val row2Alpha = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                val bRow2 = b * 0.82f
                val row2Scale = (0.90f + 0.10f * p) * (1f + bRow2 * 0.028f)
                val row2Y = lerpInt(compactHPx, row2Y1, p)
                val row2BounceY = overshoot * (overshootPx * 0.75f) + bRow2 * dipPx

                libPlaceable?.placeRelativeWithLayer(0, row2Y) {
                    alpha = row2Alpha
                    translationY = row2BounceY
                    scaleX = row2Scale
                    scaleY = row2Scale
                }
                devPlaceable?.placeRelativeWithLayer(libW1 + gapPx, row2Y) {
                    alpha = row2Alpha
                    translationY = row2BounceY
                    scaleX = row2Scale
                    scaleY = row2Scale
                }

                val row3Alpha = ((p - 0.32f) / 0.68f).coerceIn(0f, 1f)
                val bRow3 = b * 0.72f
                val row3Scale = (0.90f + 0.10f * p) * (1f + bRow3 * 0.024f)
                val row3Y = lerpInt(compactHPx, row3Y1, p)
                val row3BounceY = overshoot * (overshootPx * 0.65f) + bRow3 * dipPx

                suppPlaceable?.placeRelativeWithLayer(0, row3Y) {
                    alpha = row3Alpha
                    translationY = row3BounceY
                    scaleX = row3Scale
                    scaleY = row3Scale
                }
                docPlaceable?.placeRelativeWithLayer(halfW1 + gapPx, row3Y) {
                    alpha = row3Alpha
                    translationY = row3BounceY
                    scaleX = row3Scale
                    scaleY = row3Scale
                }
            }
        }
    }
}

// Bento wall morphing and transforming between a compact 72dp top row and the full deck.
@Composable
internal fun HubBentoGrid(
    expandProgress: Float,
    settleBounce: Float = 0f,
    settleDirection: Float = 1f,
    onExpandRequest: () -> Unit,
    moduleInstalled: Boolean?,
    moduleValue: String,
    servicePid: String?,
    rootGranted: Boolean?,
    rootValue: String,
    profile: String,
    profileValue: String,
    isManualMode: Boolean,
    isProfileApplying: Boolean,
    onProfileClick: (() -> Unit)?,
    deviceName: String,
    chipsetName: String,
    refreshHz: Int,
    gamesCount: Int,
    allAppsCount: Int,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSupport: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    BentoMorphLayout(
        progress = expandProgress,
        settleBounce = settleBounce,
        settleDirection = settleDirection,
        modifier = modifier.fillMaxWidth()
    ) {
        MorphedProfileTile(
            progress = expandProgress,
            profile = profile,
            profileValue = profileValue,
            isManualMode = isManualMode,
            isApplying = isProfileApplying,
            onClick = onProfileClick,
            onExpandClick = onExpandRequest
        )

        val moduleMissing = moduleInstalled == false
        MorphedStatusTile(
            progress = expandProgress,
            order = 1,
            icon = Icons.Rounded.Memory,
            label = stringResource(R.string.gamespace_info_module),
            value = moduleValue,
            badge = if (servicePid.isNullOrBlank()) null else stringResource(R.string.pid_format, servicePid),
            shape = if (moduleMissing) BentoShapes.Slanted else BentoShapes.Cookie12,
            pressedShape = BentoShapes.Cookie6,
            container = if (moduleMissing) scheme.errorContainer else scheme.secondaryContainer,
            onContainer = if (moduleMissing) scheme.onErrorContainer else scheme.onSecondaryContainer,
            badgeColor = if (moduleMissing) scheme.error else scheme.secondary,
            onBadge = if (moduleMissing) scheme.onError else scheme.onSecondary,
            spin = 60f,
            onExpandClick = onExpandRequest
        )

        val rootDenied = rootGranted == false
        MorphedStatusTile(
            progress = expandProgress,
            order = 2,
            icon = if (rootGranted == true) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
            label = stringResource(R.string.root_access),
            value = rootValue,
            badge = null,
            shape = if (rootDenied) BentoShapes.Slanted else BentoShapes.Clover4,
            pressedShape = BentoShapes.Clover8,
            container = if (rootDenied) scheme.errorContainer else scheme.surfaceContainerHigh,
            onContainer = if (rootDenied) scheme.onErrorContainer else scheme.onSurface,
            badgeColor = if (rootDenied) scheme.error else scheme.tertiary,
            onBadge = if (rootDenied) scheme.onError else scheme.onTertiary,
            spin = -45f,
            onExpandClick = onExpandRequest
        )

        LibraryTile(
            gamesCount = gamesCount,
            allAppsCount = allAppsCount,
            onClick = onOpenLibrary
        )

        DeviceTile(
            deviceName = deviceName,
            chipsetName = chipsetName,
            refreshHz = refreshHz,
            onClick = onOpenSettings
        )

        LinkTile(
            order = 5,
            icon = Icons.Rounded.Favorite,
            label = stringResource(R.string.support_us),
            description = stringResource(R.string.support_us_desc),
            shape = BentoShapes.Heart,
            pressedShape = BentoShapes.Burst,
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
            badgeColor = scheme.secondary,
            onBadge = scheme.onSecondary,
            onClick = onSupport
        )

        LinkTile(
            order = 6,
            icon = Icons.Rounded.Info,
            label = stringResource(R.string.learn_more),
            description = stringResource(R.string.learn_more_desc),
            shape = BentoShapes.Flower,
            pressedShape = BentoShapes.VerySunny,
            container = scheme.surfaceContainerHigh,
            onContainer = scheme.onSurface,
            badgeColor = scheme.primary,
            onBadge = scheme.onPrimary,
            onClick = onLearnMore
        )
    }
}

@Composable
private fun MorphedProfileTile(
    progress: Float,
    profile: String,
    profileValue: String,
    isManualMode: Boolean,
    isApplying: Boolean,
    onClick: (() -> Unit)?,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<Float>()
    val effects = motion.defaultEffectsSpec<Float>()
    val (shape, icon) = when (profileValue) {
        "1" -> BentoShapes.SoftBurst to Icons.Rounded.OfflineBolt
        "3" -> BentoShapes.Clover4 to Icons.Rounded.EnergySavingsLeaf
        else -> BentoShapes.Cookie9 to Icons.Rounded.Water
    }

    val p = progress.coerceIn(0f, 1f)
    val badgeSize = lerp(48.dp, 100.dp, p)
    val glyphSize = lerp(24.dp, 44.dp, p)
    val restCorner = lerp(24.dp, 36.dp, p)
    val engagedCorner = lerp(14.dp, 20.dp, p)
    val padH = lerp(14.dp, 20.dp, p)
    val padV = lerp(10.dp, 16.dp, p)
    val gap = lerp(12.dp, 18.dp, p)

    val effectiveClick = if (p < 0.35f) {
        if (isManualMode && onClick != null) onClick else onExpandClick
    } else {
        onClick
    }

    BentoTile(
        order = 0,
        container = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        modifier = modifier,
        onClick = effectiveClick,
        restCorner = restCorner,
        engagedCorner = engagedCorner
    ) { engaged ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            MorphBadge(
                shape = shape,
                color = scheme.primary,
                size = badgeSize,
                pressedShape = BentoShapes.Sunny,
                engaged = engaged,
                spin = 25f
            ) {
                AnimatedContent(
                    targetState = if (isApplying) null else icon,
                    transitionSpec = {
                        (fadeIn(effects) + scaleIn(spatial, initialScale = 0.4f))
                            .togetherWith(fadeOut(effects) + scaleOut(spatial, targetScale = 0.4f))
                    },
                    label = "MorphedHeroBadgeContent"
                ) { glyph ->
                    if (glyph == null) {
                        LoadingIndicator(modifier = Modifier.size(glyphSize), color = scheme.onPrimary)
                    } else {
                        Icon(
                            imageVector = glyph,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(glyphSize)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.current_profile).let { if (p < 0.4f) it.uppercase() else it },
                    style = if (p < 0.4f) {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    color = scheme.onPrimaryContainer.copy(alpha = if (p < 0.4f) 0.72f else 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedContent(
                    targetState = profile,
                    transitionSpec = {
                        (slideInVertically(motion.defaultSpatialSpec()) { it / 2 } + fadeIn(effects))
                            .togetherWith(slideOutVertically(motion.defaultSpatialSpec()) { -it / 2 } + fadeOut(effects))
                    },
                    label = "MorphedProfileName"
                ) { name ->
                    Text(
                        text = name,
                        style = if (p < 0.4f) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        },
                        color = scheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (p > 0.15f) {
                    Spacer(Modifier.height(lerp(0.dp, 10.dp, p)))
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = ((p - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            translationY = (1f - p) * 14.dp.toPx()
                        }
                    ) {
                        ModePill(isManualMode = isManualMode, engaged = engaged)
                    }
                }
            }

            if (p < 0.35f && isManualMode) {
                NudgeChevron(
                    engaged = engaged,
                    tint = scheme.onPrimaryContainer.copy(alpha = (1f - p * 2.8f).coerceIn(0f, 0.75f))
                )
            }
        }
    }
}

@Composable
private fun ModePill(isManualMode: Boolean, engaged: () -> Float) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val fill by animateColorAsState(
        targetValue = if (isManualMode) scheme.primary else scheme.onPrimaryContainer.copy(alpha = 0.10f),
        animationSpec = motion.defaultEffectsSpec(),
        label = "ModePillFill"
    )
    val ink by animateColorAsState(
        targetValue = if (isManualMode) scheme.onPrimary else scheme.onPrimaryContainer,
        animationSpec = motion.defaultEffectsSpec(),
        label = "ModePillInk"
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fill)
            .animateContentSize(motion.defaultSpatialSpec())
            .padding(start = 10.dp, end = if (isManualMode) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isManualMode) Icons.Rounded.Tune else Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isManualMode) stringResource(R.string.gamespace_mode_manual) else stringResource(R.string.str_auto_mode),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            maxLines = 1
        )
        AnimatedVisibility(visible = isManualMode) {
            NudgeChevron(engaged = engaged, tint = ink, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MorphedStatusTile(
    progress: Float,
    order: Int,
    icon: ImageVector,
    label: String,
    value: String,
    badge: String?,
    shape: RoundedPolygon,
    pressedShape: RoundedPolygon,
    container: Color,
    onContainer: Color,
    badgeColor: Color,
    onBadge: Color,
    spin: Float,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val effects = motion.defaultEffectsSpec<Float>()
    val p = progress.coerceIn(0f, 1f)

    val badgeSize = lerp(48.dp, 52.dp, p)
    val restCorner = lerp(24.dp, 28.dp, p)
    val engagedCorner = lerp(14.dp, 36.dp, p)
    val padH = lerp(14.dp, 16.dp, p)
    val padV = 10.dp

    BentoTile(
        order = order,
        container = container,
        contentColor = onContainer,
        modifier = modifier,
        onClick = if (p < 0.35f) onExpandClick else null,
        restCorner = restCorner,
        engagedCorner = engagedCorner
    ) { engaged ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(lerp(12.dp, 14.dp, p))
        ) {
            MorphBadge(
                shape = shape,
                color = badgeColor,
                size = badgeSize,
                pressedShape = pressedShape,
                engaged = engaged,
                spin = spin
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = onBadge, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.let { if (p < 0.4f) it.uppercase() else it },
                    style = if (p < 0.4f) {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    color = onContainer.copy(alpha = if (p < 0.4f) 0.72f else 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically(motion.defaultSpatialSpec()) { it / 2 } + fadeIn(effects))
                            .togetherWith(slideOutVertically(motion.defaultSpatialSpec()) { -it / 2 } + fadeOut(effects))
                    },
                    label = "MorphedStatusValue"
                ) { text ->
                    Text(
                        text = text,
                        style = if (p < 0.4f) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        },
                        color = onContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!badge.isNullOrBlank()) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = onBadge,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryTile(
    gamesCount: Int,
    allAppsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onTertiaryContainer
    BentoTile(
        order = 3,
        container = scheme.tertiaryContainer,
        contentColor = ink,
        modifier = modifier,
        onClick = onClick,
        restCorner = 32.dp,
        engagedCorner = 18.dp
    ) { engaged ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MorphBadge(
                    shape = BentoShapes.Puffy,
                    color = scheme.tertiary,
                    size = 56.dp,
                    pressedShape = BentoShapes.Flower,
                    engaged = engaged,
                    spin = 45f
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = scheme.onTertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.gamespace_library_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                NudgeChevron(engaged = engaged, tint = ink.copy(alpha = 0.7f))
            }
            Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    RollingCount(count = gamesCount, color = ink)
                    Text(
                        text = stringResource(R.string.gamespace_info_games),
                        style = MaterialTheme.typography.labelLarge,
                        color = ink.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    RollingCount(count = allAppsCount, color = ink)
                    Text(
                        text = stringResource(R.string.gamespace_info_apps),
                        style = MaterialTheme.typography.labelLarge,
                        color = ink.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTile(
    deviceName: String,
    chipsetName: String,
    refreshHz: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    BentoTile(
        order = 4,
        container = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        modifier = modifier,
        onClick = onClick,
        restCorner = 32.dp,
        engagedCorner = 18.dp
    ) { engaged ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MorphBadge(
                    shape = BentoShapes.Pentagon,
                    color = scheme.primary,
                    size = 56.dp,
                    pressedShape = BentoShapes.Gem,
                    engaged = engaged,
                    spin = -40f
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = chipsetName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NudgeChevron(engaged = engaged, tint = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Speed,
                    description = stringResource(R.string.refreshrates),
                    value = stringResource(R.string.refresh_rate_format, refreshHz.toString()),
                    shape = BentoShapes.Sunny,
                    engaged = engaged,
                    spin = 90f
                )
                SpecChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Android,
                    description = stringResource(R.string.android_version),
                    value = "Android ${Build.VERSION.RELEASE}",
                    shape = BentoShapes.Cookie4,
                    engaged = engaged,
                    spin = -60f
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Memory,
                    description = stringResource(R.string.gamespace_spec_arch),
                    value = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                    shape = BentoShapes.Arch,
                    engaged = engaged,
                    spin = 45f
                )
                SpecChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Security,
                    description = stringResource(R.string.gamespace_spec_patch),
                    value = Build.VERSION.SECURITY_PATCH,
                    shape = BentoShapes.Cookie6,
                    engaged = engaged,
                    spin = -90f
                )
            }
        }
    }
}

/** Icon-plus-value pill; the icon carries the meaning so no label word is needed. */
@Composable
private fun SpecChip(
    icon: ImageVector,
    description: String,
    value: String,
    shape: RoundedPolygon,
    engaged: () -> Float,
    spin: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MorphBadge(
            shape = shape,
            color = scheme.secondaryContainer,
            size = 32.dp,
            engaged = engaged,
            spin = spin
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LinkTile(
    order: Int,
    icon: ImageVector,
    label: String,
    description: String,
    shape: RoundedPolygon,
    pressedShape: RoundedPolygon,
    container: Color,
    onContainer: Color,
    badgeColor: Color,
    onBadge: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoTile(
        order = order,
        container = container,
        contentColor = onContainer,
        modifier = modifier,
        onClick = onClick,
        // Oversized radius clamps to a full pill at any height.
        restCorner = 100.dp,
        engagedCorner = 24.dp
    ) { engaged ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 76.dp)
                .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MorphBadge(
                shape = shape,
                color = badgeColor,
                size = 52.dp,
                pressedShape = pressedShape,
                engaged = engaged,
                spin = 30f
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = onBadge, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            NudgeChevron(engaged = engaged, tint = onContainer.copy(alpha = 0.7f))
        }
    }
}
