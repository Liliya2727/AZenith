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

package zx.azenith.ui.gamespace

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.viewmodel.ApplistViewmodel

/**
 * Interactive Side Panel Setup Dialog shown on first game launch to configure whether to pin the
 * floating Game Panel pull-out bar to the Left Side or Right Side and how sensitive it
 * responds to swipe-in gestures.
 */
@Composable
internal fun ZXGameSpaceSidePanelOnboardingOverlay(
    visible: Boolean,
    pendingApp: ApplistViewmodel.AppInfo?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var selectedSide by remember(visible) {
        mutableStateOf(prefs.getString("gamespace_sidepanel_side", "left") ?: "left")
    }
    var committedSensitivity by remember(visible) {
        mutableIntStateOf(prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100))
    }
    var sliderSensitivity by remember(committedSensitivity) {
        mutableIntStateOf(committedSensitivity)
    }
    var isConfirmed by remember(visible) { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = if (isConfirmed) {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (selectedSide == "left") -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        } else {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 4 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        },
        modifier = Modifier.fillMaxSize()
    ) {
        val insets = WindowInsets.displayCutout.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val startPad = maxOf(32.dp, insets.calculateStartPadding(layoutDirection))
        val endPad = maxOf(32.dp, insets.calculateEndPadding(layoutDirection))
        val topPad = maxOf(20.dp, insets.calculateTopPadding())
        val bottomPad = maxOf(20.dp, insets.calculateBottomPadding())

        CompositionLocalProvider(
            LocalContentColor provides scheme.onSurface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeBlurStyle.Material3(
                            containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.72f)
                        ) {
                            blurRadius(28.dp)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(start = startPad, end = endPad, top = topPad, bottom = bottomPad),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerLow,
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (pendingApp != null) {
                                    AppIconImage(app = pendingApp, size = 48.dp)
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = scheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.GridView,
                                                contentDescription = null,
                                                tint = scheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Configure In-Game Side Panel",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = if (pendingApp != null) {
                                            "Launching ${pendingApp.label} · Choose where to pin the floating Game Panel bar and its swipe-in sensitivity"
                                        } else {
                                            "Choose where to pin the floating Game Panel pull-out bar and how sensitive it responds to edge swipes"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TopBarIconButton(
                                icon = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_back),
                                onClick = onDismiss
                            )
                        }

                        // 1. Left Side vs Right Side Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val sideChoices = listOf(
                                Triple("left", "Left Side", "Pin floating pull-out bar to the left screen edge"),
                                Triple("right", "Right Side", "Pin floating pull-out bar to the right screen edge")
                            )
                            sideChoices.forEach { (key, title, desc) ->
                                val selected = selectedSide == key
                                val cornerPercent by animateIntAsState(
                                    targetValue = if (selected) 26 else 18,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "OnboardingSideCorner"
                                )
                                val cardShape = RoundedCornerShape(percent = cornerPercent)
                                Surface(
                                    shape = cardShape,
                                    color = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                    contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .gamepadFocusOutline(shape = cardShape, register = true)
                                        .clip(cardShape)
                                        .clickable { selectedSide = key }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Visual landscape frame showing pinned floating bar & pull-out gesture
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = scheme.surfaceContainerLowest,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(
                                                            if (key == "left") Alignment.CenterStart
                                                            else Alignment.CenterEnd
                                                        )
                                                        .width(6.dp)
                                                        .height(34.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (selected) scheme.primary else scheme.onSurfaceVariant
                                                        )
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .align(
                                                            if (key == "left") Alignment.CenterStart
                                                            else Alignment.CenterEnd
                                                        )
                                                        .padding(
                                                            start = if (key == "left") 14.dp else 0.dp,
                                                            end = if (key == "right") 14.dp else 0.dp
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (key == "right") {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                            contentDescription = null,
                                                            tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (selected) {
                                                            scheme.secondaryContainer
                                                        } else {
                                                            scheme.surfaceContainerHigh
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Swipe In",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = if (selected) {
                                                                scheme.onSecondaryContainer
                                                            } else {
                                                                scheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    if (key == "left") {
                                                        Icon(
                                                            imageVector = Icons.Rounded.ChevronRight,
                                                            contentDescription = null,
                                                            tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (selected) {
                                                        scheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    } else {
                                                        scheme.onSurfaceVariant
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (selected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = scheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = scheme.onPrimary,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Swipe-In Sensitivity Section
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Swipe-In Response Sensitivity",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = scheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = when {
                                                sliderSensitivity <= 45 -> "Less Sensitive · ${sliderSensitivity}%"
                                                sliderSensitivity >= 80 -> "More Sensitive · ${sliderSensitivity}%"
                                                else -> "Balanced · ${sliderSensitivity}%"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = scheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf(
                                        "Less Sensitive" to 35,
                                        "Balanced" to 65,
                                        "More Sensitive" to 90
                                    )
                                    presets.forEach { (label, targetVal) ->
                                        val selected = when (targetVal) {
                                            35 -> sliderSensitivity <= 45
                                            90 -> sliderSensitivity >= 80
                                            else -> sliderSensitivity in 46..79
                                        }
                                        val pillRadius by animateIntAsState(
                                            targetValue = if (selected) 50 else 28,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "OnboardingSensPillRadius"
                                        )
                                        val pillShape = RoundedCornerShape(percent = pillRadius)
                                        Surface(
                                            shape = pillShape,
                                            color = if (selected) scheme.primary else scheme.surfaceContainerHighest,
                                            contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .gamepadFocusOutline(shape = pillShape, register = true)
                                                .clip(pillShape)
                                            .clickable {
                                                sliderSensitivity = targetVal
                                                committedSensitivity = targetVal
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                ZenithSlider(
                                    value = sliderSensitivity.toFloat(),
                                    onValueChange = { raw ->
                                        val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(20, 100)
                                        if (snapped != sliderSensitivity) {
                                            sliderSensitivity = snapped
                                        }
                                    },
                                    onValueChangeFinished = {
                                        committedSensitivity = sliderSensitivity
                                    },
                                    valueRange = 20f..100f,
                                    steps = 15,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                )
                            }
                        }

                        // 3. Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cancelShape = RoundedCornerShape(18.dp)
                            Surface(
                                shape = cancelShape,
                                color = scheme.surfaceContainerHigh,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .gamepadFocusOutline(shape = cancelShape, register = true)
                                    .clip(cancelShape)
                                    .clickable(onClick = onDismiss)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            val confirmShape = CircleShape
                            Surface(
                                shape = confirmShape,
                                color = scheme.primary,
                                contentColor = scheme.onPrimary,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .gamepadFocusOutline(shape = confirmShape, register = true)
                                    .clip(confirmShape)
                                    .clickable {
                                        isConfirmed = true
                                        prefs.edit()
                                            .putString("gamespace_sidepanel_side", selectedSide)
                                            .putInt("gamespace_sidepanel_sensitivity", sliderSensitivity)
                                            .putBoolean("gamespace_sidepanel_setup_done", true)
                                            .apply()
                                        onConfirm()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 13.dp, horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (pendingApp != null) {
                                            Icons.Rounded.PlayArrow
                                        } else {
                                            Icons.Rounded.Check
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (pendingApp != null) {
                                            "Save & Launch ${pendingApp.label}"
                                        } else {
                                            "Save Side Panel Settings"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
