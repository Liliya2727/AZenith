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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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


private data class RendererOption(
    val titleRes: String,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRendererOptions(context: Context): List<RendererOption> {
    return listOf(
        RendererOption(context.getString(R.string.Renderer_Default), "default", Icons.Rounded.Layers),
        RendererOption("SkiaVK", "skiavk", Icons.Rounded.Layers),
        RendererOption("SkiaVK (Threaded)", "skiavkthreaded", Icons.Rounded.Layers),
        RendererOption("SkiaGL", "skiagl", Icons.Rounded.Layers),
        RendererOption("SkiaGL (Threaded)", "skiaglthreaded", Icons.Rounded.Layers),
        RendererOption("OpenGL ES", "opengl", Icons.Rounded.Layers),
        RendererOption("OpenGL ES (Threaded)", "openglthreaded", Icons.Rounded.Layers),
        RendererOption("Vulkan", "vulkan", Icons.Rounded.Layers),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RendererDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRenderer: (String) -> Unit,
    origin: Offset = Offset(0.5f, 0.28f)
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current
    val options = getRendererOptions(context)
    val activeDialogCount = LocalActiveDialogCount.current
    androidx.compose.runtime.DisposableEffect(show) {
        if (show) activeDialogCount.value++
        onDispose {
            if (show) activeDialogCount.value--
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackHandler(enabled = show, onBack = onDismiss)

        // The scrim is a sibling of the card rather than its parent. AnimatedVisibility applies
        // its transition to the whole subtree, so nesting the card inside a fullscreen scrim that
        // also animates makes the card's scale pivot resolve against screen-sized bounds, and the
        // dialog then appears to grow from the middle of the screen instead of from the tap point.
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
                            text = stringResource(R.string.Renderer_Select),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val content = options.map { option ->
                            @Composable {
                                ExpressiveListItem(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    headlineContent = {
                                        Text(
                                            text = option.titleRes,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingContent = {
                                        SmallLeadingIcon(icon = option.icon)
                                    },
                                    onClick = {
                                        onDismiss()
                                        onRenderer(option.reason)
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
