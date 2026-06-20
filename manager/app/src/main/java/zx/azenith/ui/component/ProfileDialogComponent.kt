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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import zx.azenith.R


private data class ProfileOption(
    val titleRes: Int,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getProfileOptions(): List<ProfileOption> {
    return listOf(
        ProfileOption(R.string.Profile_Balanced, "2", Icons.Outlined.Water),
        ProfileOption(R.string.Profile_Performance, "1", Icons.Outlined.OfflineBolt),
        ProfileOption(R.string.Profile_ECO_mode, "3", Icons.Outlined.EnergySavingsLeaf),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current
    val options = getProfileOptions()


    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(250, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {

        BackHandler(onBack = onDismiss)
        

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) 
                .background(Color.Black.copy(alpha = 0.42f)) 
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss 
                ),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = if (show) 1f else 0.9f,
                animationSpec = tween(250, easing = LinearOutSlowInEasing),
                label = "dialog_scale"
            )


            Box(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp) 
                    .padding(24.dp) 
                    .scale(scale)
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
                        text = stringResource(R.string.Profile_Select),
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
                                        text = stringResource(option.titleRes),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = { 
                                    SmallLeadingIcon(icon = option.icon) 
                                },
                                onClick = {
                                    onDismiss()
                                    onProfile(option.reason)
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
