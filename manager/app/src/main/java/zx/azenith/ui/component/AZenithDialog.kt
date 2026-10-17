/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.blur.hazeBlur
import zx.azenith.ui.theme.ACCENT_TINT_BLUR
import zx.azenith.ui.theme.accentTint

/**
 * The one dialog shell. Every dialog in the app goes through here, so motion, layout and the
 * global animation toggle are decided in one place instead of per call site.
 *
 * Two rules the layout depends on, both learned by breaking them first: the scrim and the card
 * are separate [AnimatedVisibility] nodes, and the card's node carries the card's own bounds — a
 * fullscreen parent would resolve [origin] against screen coordinates and the dialog would grow
 * from the middle of the screen instead of the control that opened it.
 *
 * [origin] is the trigger position normalised against the root (0..1), which is what
 * `DialogOriginState.origin` produces. It is rebased into card-local space before use.
 *
 * [animate] false drops straight to a plain M3 popup with no motion.
 */
@Composable
fun AZenithDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    spec: DialogSpec = DialogSpec(),
    origin: Offset = Offset(0.5f, 0.5f),
    animate: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val dialogsAnimated = animate
    val hazeState = LocalAppHazeState.current

    val activeDialogCount = LocalActiveDialogCount.current
    DisposableEffect(visible, spec.registersActiveDialog) {
        if (visible && spec.registersActiveDialog) activeDialogCount.value++
        onDispose { if (visible && spec.registersActiveDialog) activeDialogCount.value-- }
    }

    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val dialogRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val cardOrigin = if (rootSize == IntSize.Zero || cardSize == IntSize.Zero) {
        origin
    } else {
        Offset(
            x = ((origin.x * rootSize.width) - (rootSize.width - cardSize.width) / 2f) / cardSize.width,
            y = ((origin.y * rootSize.height) - (rootSize.height - cardSize.height) / 2f) / cardSize.height
        )
    }

    val containerColor = spec.containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh

    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            wasVisible = true
            dialogRequester.requestFocus()
        } else if (wasVisible) {
            // The dialog's focused node is disposed along with it and nothing re-seeds focus,
            // so without this the d-pad has nothing to move from once a dialog closes.
            wasVisible = false
            kotlinx.coroutines.delay(150)
            GamepadFocusNav.restoreFocus()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSize = it.size }
            .focusRequester(dialogRequester)
            .focusGroup()
            .gamepadKeyWatch()
            .focusProperties {
                // A dialog is modal: while it is up a d-pad may not walk out to the
                // screen behind. canFocus cannot express this — a deactivated node is
                // searched into, not pruned — so the exit is cancelled outright.
                if (visible) onExit = { cancelFocusChange() }
            }
    ) {
        BackHandler(enabled = visible, onBack = { if (spec.dismissible) onDismiss() })

        AnimatedVisibility(
            visible = visible,
            enter = if (dialogsAnimated) Motion.scrimEnter() else EnterTransition.None,
            exit = if (dialogsAnimated) Motion.scrimExit() else ExitTransition.None
        ) {
            val amoled = MaterialTheme.colorScheme.surfaceContainerLowest == Color.Black
            val scrimColor = if (amoled) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    // Tapping the scrim dismisses, but it must not be a d-pad stop:
                    // it covers the screen, so focus would land on the dim layer.
                    .focusProperties { canFocus = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (spec.dismissible) onDismiss() }
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(spec.zIndex)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = if (dialogsAnimated) Motion.cardEnterFrom(cardOrigin) else EnterTransition.None,
                exit = if (dialogsAnimated) Motion.cardExitTo(cardOrigin) else ExitTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = spec.minWidth, max = spec.maxWidth)
                        .onGloballyPositioned { cardSize = it.size }
                        .clip(RoundedCornerShape(spec.cornerRadius))
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = HazeBlurStyle.Material3(
                                    containerColor = containerColor.copy(alpha = 0.35f)
                                        .accentTint(ACCENT_TINT_BLUR)
                                ) { blurRadius(24.dp) }
                            )
                            } else Modifier
                        )
                        .background(
                            if (isBlurEnabled) Color.Transparent else containerColor
                        )
                        // The card swallows taps; it is not itself a d-pad stop.
                        .focusProperties { canFocus = false }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spec.contentPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}