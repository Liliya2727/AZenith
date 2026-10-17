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
import android.content.Intent
import android.hardware.input.InputManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline

/**
 * Quick settings sheet anchored to the right edge on solid Material 3 surfaces.
 *
 * Network, Bluetooth audio and input devices come from framework callbacks rather than a single
 * read, so the panel stays accurate for as long as it is open.
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

    // Volume control state
    val maxVol = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVol by remember(audioManager) {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2))
    }

    // Screen brightness control state
    val activity = context as? Activity
    var brightness by remember(activity) {
        val windowVal = activity?.window?.attributes?.screenBrightness ?: -1f
        val initial = if (windowVal >= 0f) {
            windowVal
        } else {
            val sys = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            }.getOrDefault(128)
            (sys / 255f).coerceIn(0.05f, 1f)
        }
        mutableFloatStateOf(initial)
    }

    // Hardware status monitoring
    val network = rememberQuickNetworkState(connectivityManager)
    val bluetoothEndpoints = rememberQuickBluetoothEndpoints(audioManager, bluetoothManager)
    val inputDevices = rememberQuickInputDevices(inputManager)
    val ram = rememberRamState()
    val battery = rememberBatteryState()

    val controllers = remember(inputDevices) {
        inputDevices.filter { it.kind == QuickInputKind.Controller }
    }
    val hasGamepad = controllers.isNotEmpty()

    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val endSafePad = maxOf(24.dp, cutoutInsets.calculateEndPadding(layoutDirection))

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = 0.50f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = motion.defaultSpatialSpec()
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = motion.defaultSpatialSpec()
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .widthIn(min = 380.dp, max = 460.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 20f) onClose()
                        }
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 380.dp, max = 460.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
                    color = scheme.surfaceContainerHigh,
                    shadowElevation = 16.dp,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 24.dp,
                                end = endSafePad,
                                top = 20.dp,
                                bottom = 20.dp
                            )
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title bar with close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.quick_settings_title),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "System Telemetry & Controls",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(scheme.surfaceContainerHighest, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.dialog_cancel),
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 2x2 Bento Grid: Telemetry Cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BentoStatCard(
                                    icon = if (network.isWifi) Icons.Rounded.Wifi else if (network.isCellular) Icons.Rounded.SignalCellularAlt else Icons.Rounded.WifiOff,
                                    iconContainerColor = if (network.isConnected) scheme.primaryContainer else scheme.surfaceContainerLow,
                                    iconTint = if (network.isConnected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                    category = stringResource(R.string.quick_settings_network),
                                    headline = network.label,
                                    modifier = Modifier.weight(1f)
                                )

                                BentoStatCard(
                                    icon = if (bluetoothEndpoints.isNotEmpty()) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                                    iconContainerColor = if (bluetoothEndpoints.isNotEmpty()) scheme.secondaryContainer else scheme.surfaceContainerLow,
                                    iconTint = if (bluetoothEndpoints.isNotEmpty()) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                                    category = stringResource(R.string.quick_settings_bluetooth),
                                    headline = bluetoothEndpoints.firstOrNull()?.name ?: stringResource(R.string.quick_settings_value_none),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BentoStatCard(
                                    icon = if (battery.second) Icons.Rounded.BatteryChargingFull else if (battery.first <= 15) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryStd,
                                    iconContainerColor = if (battery.second) scheme.primaryContainer else if (battery.first <= 15) scheme.errorContainer else scheme.surfaceContainerLow,
                                    iconTint = if (battery.second) scheme.onPrimaryContainer else if (battery.first <= 15) scheme.onErrorContainer else scheme.onSurfaceVariant,
                                    category = "Battery",
                                    headline = "${battery.first}%${if (battery.second) " Charging" else ""}",
                                    modifier = Modifier.weight(1f)
                                )

                                BentoStatCard(
                                    icon = Icons.Rounded.Memory,
                                    iconContainerColor = scheme.tertiaryContainer,
                                    iconTint = scheme.onTertiaryContainer,
                                    category = "RAM",
                                    headline = "${ram.second} Used",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Hero Bento Card: Input Mode & Active Controller
                        BentoInputHeroCard(
                            isGamepad = hasGamepad,
                            controllerName = controllers.firstOrNull()?.name,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Slider Bento Cards: Media Volume & Screen Brightness
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val volumeFraction = if (maxVol > 0) (currentVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f) else 0f
                            val volumePercent = (volumeFraction * 100f).roundToInt()
                            val volIcon = when {
                                currentVol == 0 -> Icons.AutoMirrored.Rounded.VolumeMute
                                volumeFraction < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            }

                            BentoSliderCard(
                                icon = volIcon,
                                title = stringResource(R.string.quick_settings_volume),
                                valuePercent = volumePercent,
                                accent = scheme.primary,
                                value = currentVol.toFloat(),
                                onValueChange = { raw ->
                                    val step = raw.roundToInt().coerceIn(0, maxVol)
                                    if (step != currentVol) {
                                        currentVol = step
                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val brightPercent = (brightness.coerceIn(0f, 1f) * 100f).roundToInt()
                            val brightIcon = when {
                                brightness < 0.35f -> Icons.Rounded.BrightnessLow
                                brightness < 0.70f -> Icons.Rounded.BrightnessMedium
                                else -> Icons.Rounded.BrightnessHigh
                            }

                            BentoSliderCard(
                                icon = brightIcon,
                                title = stringResource(R.string.quick_settings_brightness),
                                valuePercent = brightPercent,
                                accent = scheme.tertiary,
                                value = brightness * 100f,
                                onValueChange = { raw ->
                                    val next = (raw / 100f).coerceIn(0.02f, 1f)
                                    brightness = next
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = next
                                        win.attributes = lp
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // System Settings Navigation link
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Open System Settings",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                        color = scheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
