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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Exit animation budget for the sheet. The caller sets [CustomBottomSheet]'s
 * `visible` to false and the window is torn down this long after, so the slide
 * out finishes before the Popup goes away. Must cover the longest exit spec —
 * slideOutVertically is tween(250) and the scrim fade is tween(200).
 */
private const val SHEET_EXIT_MILLIS = 260L


@Composable
fun CustomBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    
    val density = LocalDensity.current
    val extraBottomPadding = 100.dp

    // A vertically scrollable child is only measurable if the sheet bounds its
    // own height first. Without this the sheet body is measured with an infinite
    // maximum height and any scrolling content inside it -- a verticalScroll
    // Column or a LazyColumn -- throws on measure. Capping here fixes every
    // caller at once instead of making each one remember to bound itself.
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.9f).dp

    LaunchedEffect(visible) {
        if (visible) {
            dragOffset.snapTo(0f)
        }
    }

    // A Popup is a real platform window, so the scrim and the sheet cover the
    // whole screen regardless of where the caller's composable sits. Rendered
    // inline, the fillMaxSize() below resolves against the caller's own
    // constraints — a lazy list row when the caller is a list item — which
    // strands the sheet inside its own row instead of over the app.
    //
    // The window is held open past `visible = false` so the exit slide still
    // plays; closing the Popup on the same frame would tear it down mid-flight
    // and the sheet would just vanish.
    var windowOpen by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            windowOpen = true
        } else {
            delay(SHEET_EXIT_MILLIS)
            windowOpen = false
        }
    }

    if (windowOpen) {
        Popup(
            properties = PopupProperties(
                focusable = true,
                // Both dismiss routes are ours: BackHandler above, and the scrim's
                // own onClick. Letting the window eat them would drop a tap.
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                // The sheet draws to the screen edge; the default platform window
                // clips to a wrap-content box and also caps width at the display.
                clippingEnabled = false,
                usePlatformDefaultWidth = false,
            )
        ) {
            // BackHandler has to live in here, not at the call site: a Popup is a
            // separate window, so the caller's BackHandler never sees the back
            // press. The sheet window is focusable, so it receives the key event.
            BackHandler(onBack = onDismiss)
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }


        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it }, 
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, 
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.zIndex(101f)
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .offset { 
                            IntOffset(
                                x = 0, 
                                y = dragOffset.value.roundToInt() + with(density) { extraBottomPadding.roundToPx() }
                            ) 
                        }
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                            } else Modifier
                        )
                        .background(
                            if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset.value > 250f) {
                                            onDismiss()
                                        } else {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            dragOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        dragOffset.snapTo(maxOf(0f, dragOffset.value + dragAmount))
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                

                    content()


                    Spacer(modifier = Modifier.height(extraBottomPadding))
                }
            }
        }
        }
}
}
