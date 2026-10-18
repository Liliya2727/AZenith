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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.gamespace

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import zx.azenith.ui.util.AudioUtil
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline

/**
 * Material 3 Expressive Quick Settings panel anchored to the right edge.
 *
 * Displays real-time Network and Bluetooth status, dynamic Input Mode hero,
 * and draggable Media Volume and Screen Brightness sliders.
 */
@Composable
internal fun ZXGameSpaceQuickSettingsOverlay(
    visible: Boolean,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val connectivityManager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val inputManager = remember(context) { context.getSystemService(Context.INPUT_SERVICE) as? InputManager }
    val bluetoothManager = remember(context) { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }

    // Volume control state (0f..1f fraction matching ZenithSlider)
    val maxVol = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var volFraction by remember(audioManager) {
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf((curVol.toFloat() / maxVol.coerceAtLeast(1)).coerceIn(0f, 1f))
    }

    // Display brightness control state (0.01f..1f fraction matching ZenithSlider)
    val activity = context as? Activity
    var brightnessFraction by remember(activity) {
        val windowAttr = activity?.window?.attributes?.screenBrightness ?: -1f
        val currentBrightness = if (windowAttr >= 0f) {
            windowAttr
        } else {
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            }.getOrDefault(0.5f)
        }
        mutableFloatStateOf(currentBrightness.coerceIn(0.01f, 1f))
    }

    // Hardware status monitoring
    val network = rememberQuickNetworkState(connectivityManager)
    val bluetoothEndpoints = rememberQuickBluetoothEndpoints(audioManager, bluetoothManager)
    val inputDevices = rememberQuickInputDevices(inputManager)
    val isBtOn = remember(bluetoothEndpoints, visible) {
        bluetoothEndpoints.isNotEmpty() || runCatching { bluetoothManager?.adapter?.isEnabled == true }.getOrDefault(false)
    }

    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val endSafePad = maxOf(24.dp, cutoutInsets.calculateEndPadding(layoutDirection))

    // Scrim overlay - dismiss on tap outside
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Floating panel sliding in from right side with spring bouncy animation
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = motion.defaultEffectsSpec()),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = endSafePad, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerLow,
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through from dismissing panel
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.quick_settings_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                AudioUtil.playCancel(context)
                                onClose()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bento Widgets Scrollable Container
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bento Row 1: Network & Bluetooth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Network Bento Card
                            val netCategory = when {
                                !network.isConnected -> stringResource(R.string.quick_settings_network)
                                network.isWifi -> stringResource(R.string.quick_settings_net_wifi)
                                network.isCellular -> stringResource(R.string.quick_settings_net_cellular)
                                else -> stringResource(R.string.quick_settings_network)
                            }
                            val netHeadline = when {
                                !network.isConnected -> stringResource(R.string.quick_settings_offline)
                                else -> network.label
                            }
                            BentoStatCard(
                                icon = when {
                                    !network.isConnected -> Icons.Rounded.WifiOff
                                    network.isWifi -> Icons.Rounded.Wifi
                                    network.isCellular -> Icons.Rounded.SignalCellularAlt
                                    else -> Icons.Rounded.Wifi
                                },
                                iconContainerColor = if (network.isConnected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                                iconTint = if (network.isConnected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                category = netCategory,
                                headline = netHeadline,
                                modifier = Modifier.weight(1f)
                            )

                            // Bluetooth Bento Card (shows Active when enabled, connected device name if paired, None only when disabled)
                            val btDevice = bluetoothEndpoints.firstOrNull()?.name
                            val btHeadline = when {
                                bluetoothEndpoints.isNotEmpty() -> btDevice ?: stringResource(R.string.quick_settings_value_active)
                                isBtOn -> stringResource(R.string.quick_settings_value_active)
                                else -> stringResource(R.string.quick_settings_value_none)
                            }
                            BentoStatCard(
                                icon = when {
                                    bluetoothEndpoints.isNotEmpty() -> Icons.Rounded.BluetoothConnected
                                    isBtOn -> Icons.Rounded.Bluetooth
                                    else -> Icons.Rounded.BluetoothDisabled
                                },
                                iconContainerColor = if (bluetoothEndpoints.isNotEmpty()) scheme.secondaryContainer else if (isBtOn) scheme.surfaceContainerHigh else scheme.surfaceContainer,
                                iconTint = if (bluetoothEndpoints.isNotEmpty()) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                                category = stringResource(R.string.quick_settings_bluetooth),
                                headline = btHeadline,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bento Row 2: Input Mode Hero Bento Card
                        val connectedGamepad = inputDevices.firstOrNull {
                            it.kind == QuickInputKind.Controller
                        }
                        val isGamepadActive = GamepadInput.dpadMode || (connectedGamepad != null && GamepadInput.active)
                        BentoInputHeroCard(
                            isGamepad = isGamepadActive,
                            controllerName = connectedGamepad?.name?.takeIf { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bento Row 3: Media Volume Slider Bento Card
                        BentoSliderCard(
                            icon = when {
                                volFraction <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                                volFraction < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            title = stringResource(R.string.quick_settings_volume),
                            valuePercent = (volFraction * 100).roundToInt(),
                            accent = scheme.primary,
                            value = volFraction,
                            onValueChange = { newFraction ->
                                volFraction = newFraction
                                audioManager?.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (newFraction * maxVol).roundToInt(),
                                    0
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bento Row 4: Display Brightness Slider Bento Card
                        BentoSliderCard(
                            icon = when {
                                brightnessFraction < 0.33f -> Icons.Rounded.BrightnessLow
                                brightnessFraction < 0.66f -> Icons.Rounded.BrightnessMedium
                                else -> Icons.Rounded.BrightnessHigh
                            },
                            title = stringResource(R.string.quick_settings_brightness),
                            valuePercent = (brightnessFraction * 100).roundToInt(),
                            accent = scheme.tertiary,
                            value = brightnessFraction,
                            onValueChange = { newFraction ->
                                brightnessFraction = newFraction
                                activity?.window?.let { window ->
                                    val attributes = window.attributes
                                    attributes.screenBrightness = newFraction.coerceIn(0.01f, 1f)
                                    window.attributes = attributes
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure Material 3 Bento Stat Card.
 * Compact, borderless filled surface with prominent icon container, category label, and headline status.
 */
@Composable
internal fun BentoStatCard(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    category: String,
    headline: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pure Material 3 Input Mode Hero Bento Card.
 * Expressively morphs between Touch Screen and Gamepad modes upon user input or controller detection.
 */
@Composable
internal fun BentoInputHeroCard(
    isGamepad: Boolean,
    controllerName: String?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isGamepad) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isGamepad,
                        transitionSpec = {
                            (fadeIn(animationSpec = motion.defaultEffectsSpec()) +
                                scaleIn(animationSpec = motion.defaultSpatialSpec(), initialScale = 0.8f))
                                .togetherWith(
                                    fadeOut(animationSpec = motion.defaultEffectsSpec()) +
                                        scaleOut(animationSpec = motion.defaultSpatialSpec(), targetScale = 0.8f)
                                )
                        },
                        label = "InputHeroIcon"
                    ) { gamepadActive ->
                        Icon(
                            imageVector = if (gamepadActive) Icons.Rounded.SportsEsports else Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = if (gamepadActive) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quick_settings_input_devices),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                AnimatedContent(
                    targetState = isGamepad to controllerName,
                    transitionSpec = {
                        fadeIn(animationSpec = motion.defaultEffectsSpec())
                            .togetherWith(fadeOut(animationSpec = motion.defaultEffectsSpec()))
                    },
                    label = "InputHeroTitle"
                ) { (gamepadActive, name) ->
                    Text(
                        text = if (gamepadActive) (name ?: stringResource(R.string.quick_settings_gamepad)) else stringResource(R.string.quick_settings_input_touchscreen),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGamepad) scheme.secondaryContainer else scheme.surfaceContainerHigh
            ) {
                AnimatedContent(
                    targetState = isGamepad,
                    transitionSpec = {
                        fadeIn(animationSpec = motion.defaultEffectsSpec())
                            .togetherWith(fadeOut(animationSpec = motion.defaultEffectsSpec()))
                    },
                    label = "InputHeroBadge"
                ) { gamepadActive ->
                    Text(
                        text = if (gamepadActive) "GAMEPAD" else "TOUCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (gamepadActive) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pure Material 3 Slider Bento Card.
 * Borderless filled container with real-time value percentage and haptic ZenithSlider.
 */
@Composable
internal fun BentoSliderCard(
    icon: ImageVector,
    title: String,
    valuePercent: Int,
    accent: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }
                Text(
                    text = "$valuePercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
            }

            Spacer(Modifier.height(4.dp))

            ZenithSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = {},
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
