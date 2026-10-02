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
import androidx.compose.material.icons.rounded.Check

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
    origin: Offset = Offset(0.5f, 0.28f),
    currentRenderer: String? = null
) {
    val context = LocalContext.current
    val options = getRendererOptions(context)

    AZenithDialog(
        visible = show,
        onDismiss = onDismiss,
        origin = origin,
        spec = DialogSpec.Choice
    ) {
        Text(
            text = stringResource(R.string.Renderer_Select),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val content = options.map { option ->
            @Composable {
                val isSelected = option.reason.equals(currentRenderer, ignoreCase = true)
                ExpressiveListItemHighlight(
                    modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)),
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    headlineContent = {
                        Text(
                            text = option.titleRes,
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
