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

package zx.azenith.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.ui.gamespace.springPressBounce
import zx.azenith.ui.util.AudioUtil

/**
 * Back/ESC on a gamepad is handled by the game space root before any child sees it, so an
 * open menu has to announce itself here or the shell navigates away underneath it.
 */
internal object ZXDropDownNav {
    private val dismissers = mutableListOf<() -> Unit>()

    val isActive: Boolean get() = dismissers.isNotEmpty()

    fun push(dismiss: () -> Unit) {
        if (!dismissers.contains(dismiss)) dismissers.add(dismiss)
    }

    fun pop(dismiss: () -> Unit) {
        dismissers.remove(dismiss)
    }

    fun dismissTop() {
        dismissers.lastOrNull()?.invoke()
    }
}

/** Root-space rect of the row a menu hangs off. */
data class ZXDropDownAnchor(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class ZXDropDownState internal constructor() {
    var expanded: Boolean by mutableStateOf(false)
        internal set

    var anchor: ZXDropDownAnchor? by mutableStateOf(null)
        internal set

    fun show() {
        expanded = true
    }

    fun dismiss() {
        expanded = false
    }
}

@Composable
fun rememberZXDropDownState(): ZXDropDownState = remember { ZXDropDownState() }

/** Measures the trigger row so the menu can hang off it. Apply to the row's modifier. */
fun Modifier.zxDropDownAnchor(state: ZXDropDownState): Modifier =
    this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInRoot()
        val size = coordinates.size
        val next = ZXDropDownAnchor(
            left = position.x,
            top = position.y,
            right = position.x + size.width,
            bottom = position.y + size.height
        )
        if (state.anchor != next) state.anchor = next
    }

/**
 * A compact anchored choice menu for the game space.
 *
 * Renders in-window rather than as a Dialog: a dialog is centred and full-width, which on a
 * phone or a TV covers most of the screen and leaves nothing for the d-pad to navigate by.
 * This sits directly under its anchor row, so the surrounding list stays readable and the
 * focus trap is a plain focus graph instead of a second window.
 *
 * Place it inside a [Box] that fills the area the menu may cover, as a sibling after the
 * content it overlays.
 */
@Composable
fun <T> ZXDropDownMenu(
    state: ZXDropDownState,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    optionSupporting: (T) -> String? = { null },
    optionIcon: ((T) -> ImageVector?)? = null,
    minWidth: Dp = 232.dp,
    maxWidth: Dp = 420.dp
) {
    if (options.isEmpty()) return

    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val requesters = remember(options) { List(options.size) { FocusRequester() } }

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var overlayWidth by remember { mutableFloatStateOf(0f) }
    var overlayHeight by remember { mutableFloatStateOf(0f) }
    var menuViewport by remember { mutableFloatStateOf(0f) }

    val dismissWithCancel: () -> Unit = {
        AudioUtil.playCancel(context)
        state.dismiss()
    }

    DisposableEffect(state.expanded) {
        if (!state.expanded) return@DisposableEffect onDispose { }
        ZXDropDownNav.push(dismissWithCancel)
        onDispose { ZXDropDownNav.pop(dismissWithCancel) }
    }

    BackHandler(enabled = state.expanded, onBack = dismissWithCancel)

    var openedBefore by remember { mutableStateOf(false) }
    LaunchedEffect(state.expanded) {
        if (state.expanded) {
            openedBefore = true
            val initial = options.indexOf(selectedOption).let { if (it >= 0) it else 0 }
            // The enter transition has to start before focus moves, or the first item is
            // still at its pre-scale size and the outline lands off the row.
            delay(70)
            runCatching { requesters[initial].requestFocus() }
        } else if (openedBefore) {
            // The focused item left the tree with the menu; hand the d-pad back to the row
            // that opened it rather than waiting for the next press to re-seed.
            GamepadFocusNav.restoreFocus()
        }
    }

    val anchor = state.anchor
    val gapPx = with(density) { 6.dp.toPx() }

    val menuWidth = with(density) {
        val anchorWidth = anchor?.let { it.right - it.left } ?: 0f
        (if (anchorWidth > 0f) anchorWidth else minWidth.toPx())
            .coerceIn(minWidth.toPx(), maxWidth.toPx())
            .toDp()
    }

    // Decided from the option count, never from the measured height: the enter spec would
    // otherwise be rebuilt mid-animation and restart every time the menu settles.
    val estimatedHeight = with(density) {
        (options.size * 52.dp.toPx() + 16.dp.toPx()).coerceAtMost(344.dp.toPx())
    }
    val flipsUp = anchor != null &&
        anchor.bottom - overlayOrigin.y + gapPx + estimatedHeight > overlayHeight &&
        anchor.top - overlayOrigin.y - gapPx - estimatedHeight > 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOrigin = coordinates.positionInRoot()
                overlayWidth = coordinates.size.width.toFloat()
                overlayHeight = coordinates.size.height.toFloat()
            }
    ) {
        // Both halves read one transition state so the scrim and the menu share a lifecycle:
        // while collapsed nothing is composed, and the rows underneath stay tappable.
        val transitionState = remember { MutableTransitionState(false) }
        transitionState.targetState = state.expanded

        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(durationMillis = 140, easing = Motion.EmphasizedDecelerate)),
            exit = fadeOut(animationSpec = tween(durationMillis = 110, easing = Motion.EmphasizedAccelerate))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissWithCancel
                    )
            )
        }

        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 110, easing = Motion.EmphasizedDecelerate)
            ) + expandVertically(
                // The clip grows from the anchor edge, so the panel reads as unfolding out of
                // the row rather than being scaled down into place.
                expandFrom = if (flipsUp) Alignment.Bottom else Alignment.Top,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                transformOrigin = TransformOrigin(0.5f, if (flipsUp) 1f else 0f)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 90, easing = Motion.EmphasizedAccelerate)
            ) + shrinkVertically(
                shrinkTowards = if (flipsUp) Alignment.Bottom else Alignment.Top,
                animationSpec = tween(durationMillis = 120, easing = Motion.EmphasizedAccelerate)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(durationMillis = 90, easing = Motion.EmphasizedAccelerate),
                transformOrigin = TransformOrigin(0.5f, if (flipsUp) 1f else 0f)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset {
                        val current = state.anchor ?: return@offset IntOffset.Zero
                        val menuWidthPx = with(density) { menuWidth.toPx() }
                        val left = current.left - overlayOrigin.x
                        val x = left.coerceIn(
                            0f,
                            (overlayWidth - menuWidthPx).coerceAtLeast(0f)
                        )
                        val below = current.bottom - overlayOrigin.y + gapPx
                        val y = if (flipsUp) {
                            current.top - overlayOrigin.y - gapPx - menuViewport
                        } else {
                            below
                        }
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                    .width(menuWidth)
                    .heightIn(max = 344.dp)
                    .onGloballyPositioned { menuViewport = it.size.height.toFloat() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        ZXDropDownItem(
                            label = optionLabel(option),
                            supporting = optionSupporting(option),
                            icon = optionIcon?.invoke(option),
                            selected = option == selectedOption,
                            requester = requesters[index],
                            upTarget = requesters.getOrNull(index - 1),
                            downTarget = requesters.getOrNull(index + 1),
                            scrollState = scrollState,
                            viewport = { menuViewport },
                            onClick = {
                                onOptionSelected(option)
                                state.dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZXDropDownItem(
    label: String,
    supporting: String?,
    icon: ImageVector?,
    selected: Boolean,
    requester: FocusRequester,
    upTarget: FocusRequester?,
    downTarget: FocusRequester?,
    scrollState: ScrollState,
    viewport: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(14.dp)

    val itemTop = remember { mutableFloatStateOf(0f) }
    val itemHeight = remember { mutableFloatStateOf(0f) }

    val fill by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ZXDropDownItemFill"
    )

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val checkScale by animateFloatAsState(
        targetValue = if (appeared && selected) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ZXDropDownCheckScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .springPressBounce(pressedScale = 0.97f)
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .background(fill, shape)
            .onGloballyPositioned { coordinates ->
                itemTop.floatValue = coordinates.positionInParent().y
                itemHeight.floatValue = coordinates.size.height.toFloat()
            }
            .onFocusChanged { focus ->
                if (!focus.isFocused) return@onFocusChanged
                val visible = viewport()
                if (visible <= 0f) return@onFocusChanged
                val top = itemTop.floatValue
                val height = itemHeight.floatValue
                val target = when {
                    top < scrollState.value -> top
                    top + height > scrollState.value + visible -> top + height - visible
                    else -> return@onFocusChanged
                }.coerceIn(0f, scrollState.maxValue.toFloat())
                scope.launch { scrollState.animateScrollTo(target.roundToInt()) }
            }
            .focusProperties {
                // The menu is the only thing that may hold focus while it is open, so the
                // horizontal search is cancelled outright and the vertical one loops at
                // the ends instead of walking out into the list behind the scrim.
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                up = upTarget ?: FocusRequester.Cancel
                down = downTarget ?: FocusRequester.Cancel
            }
            .focusRequester(requester)
            .clickable {
                AudioUtil.playOk(context)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        scheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        scheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
            )
        }
    }
}
