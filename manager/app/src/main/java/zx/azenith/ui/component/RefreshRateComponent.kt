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
import androidx.compose.material.icons.rounded.Check
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WebStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import zx.azenith.R
import zx.azenith.ui.util.getSupportedRefreshRatesPicker


private data class RefreshRatePickerOption(
    val titleString: String,
    val reason: String,
    val icon: ImageVector
)

private fun getRefreshRatePickerOptions(context: Context): List<RefreshRatePickerOption> {
    val supported = getSupportedRefreshRatesPicker(context)
    return supported.map { rate ->
        RefreshRatePickerOption(
            titleString = context.getString(R.string.refresh_rate_format, rate),
            reason = rate,
            icon = Icons.Outlined.WebStories
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshRatePickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRefreshRatePicker: (String) -> Unit,
    origin: Offset = Offset(0.5f, 0.28f),
    currentRefreshRate: String? = null
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current
    val options = remember(context) { getRefreshRatePickerOptions(context) }
    val activeDialogCount = LocalActiveDialogCount.current
    androidx.compose.runtime.DisposableEffect(show) {
        if (show) activeDialogCount.value++
        onDispose {
            if (show) activeDialogCount.value--
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackHandler(enabled = show, onBack = onDismiss)
        // Scrim is a sibling of the card, not its parent: AnimatedVisibility transitions the whole
        // subtree, so a fullscreen scrim sharing the card's container transform makes the card's
        // scale pivot resolve against screen bounds and it grows from the middle of the screen.
        AnimatedVisibility(
            visible = show,
            enter = Motion.scrimEnter(),
            exit = Motion.scrimExit(),
            modifier = Modifier.fillMaxSize()
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

        // The card's AnimatedVisibility is sized to the card itself and centred in the root Box.
        // Giving it fillMaxSize would make its bounds fullscreen, and the scale pivot would then
        // resolve against screen coordinates — which is exactly the centre-of-screen pop this
        // replaces. Bounds of this node must equal bounds of the card for the origin to be right.
        AnimatedVisibility(
            visible = show,
            enter = Motion.cardEnterFrom(origin),
            exit = Motion.cardExitTo(origin),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp) 
                    .padding(24.dp) 
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                        } else Modifier
                    )
                    .background(
                        if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f) 
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.RefreshRatePicker_Select),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val content = options.map { option ->
                        @Composable {
                            val isSelected = option.reason.equals(currentRefreshRate, ignoreCase = true)
                            ExpressiveListItemHighlight(
                                modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)),
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                headlineContent = { 
                                    Text(
                                        text = option.titleString,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                leadingContent = { 
                                    SmallLeadingIcon(icon = option.icon) 
                                },
                                trailingContent = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onDismiss()
                                    onRefreshRatePicker(option.reason)
                                }
                            )
                        }
                    }

                    ExpressiveColumn(
                            modifier = Modifier.fillMaxWidth(),
                            content = content
                        )
                    }
                }
            }
        }
    }
}
