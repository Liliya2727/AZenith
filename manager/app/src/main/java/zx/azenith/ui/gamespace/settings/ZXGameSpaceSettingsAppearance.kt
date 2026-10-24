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

package zx.azenith.ui.gamespace.settings

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.rememberDynamicColorScheme
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.component.ScaleConfirmationDialog
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.gamespace.GAMESPACE_PRESET_KEY_COLORS
import zx.azenith.ui.gamespace.GameSpaceSwitch
import zx.azenith.ui.theme.ColorEngine
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.util.batchResetGameWallpapers
import zx.azenith.ui.util.batchSetGameWallpaper
import zx.azenith.ui.util.batchSetGameWallpaperMode
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.getHomeWallpaperBlur
import zx.azenith.ui.util.isHomeWallpaperEnabled
import zx.azenith.ui.util.resolveGlobalGameWallpaperMode
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.saveMediaDirectly
import zx.azenith.ui.util.setGlobalGameWallpaperMode
import zx.azenith.ui.util.setHomeWallpaperBlur
import zx.azenith.ui.util.setHomeWallpaperEnabled
import zx.azenith.ui.util.tapHaptic
import zx.azenith.ui.viewmodel.ApplistViewmodel

@Composable
internal fun ZXGameSpaceSettingsAppearance(
    modifier: Modifier = Modifier
) {
    SettingsAppearancePane(
        modifier = modifier
    )
}

@Composable
internal fun SettingsAppearancePane(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val systemDark = isSystemInDarkTheme()

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var customWallpaperUri by remember { mutableStateOf(context.getHeaderImage()) }
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(sourceUri) ?: ""
                val isVideo = mimeType.startsWith("video/")
                val extension = when {
                    isVideo -> "mp4"
                    mimeType == "image/gif" -> "gif"
                    mimeType == "image/png" -> "png"
                    mimeType == "image/webp" -> "webp"
                    else -> "jpg"
                }
                val savedUri = context.saveMediaDirectly(sourceUri, extension)
                if (savedUri != null) {
                    context.saveHeaderImage(savedUri)
                    withContext(Dispatchers.Main) {
                        customWallpaperUri = savedUri
                    }
                }
            }
        }
    }

    var sidePanelSide by remember {
        mutableStateOf(prefs.getString("gamespace_sidepanel_side", "left") ?: "left")
    }
    var sidePanelSensitivity by remember {
        mutableIntStateOf(prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100))
    }
    var sidePanelLength by remember {
        mutableIntStateOf(prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100))
    }
    var colorModeValue by remember { mutableIntStateOf(prefs.getInt("color_mode", ColorMode.SYSTEM.value)) }
    var keyColorValue by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    var confirmedScale by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    var pendingScaleConfirm by remember { mutableStateOf<Int?>(null) }
    var wallpaperDimmingIntensity by remember {
        mutableIntStateOf(prefs.getInt("gamespace_wallpaper_dimming_intensity", 75).coerceIn(15, 100))
    }
    var isWallpaperEnabled by remember {
        mutableStateOf(context.isHomeWallpaperEnabled())
    }
    var wallpaperBlur by remember {
        mutableIntStateOf(context.getHomeWallpaperBlur())
    }
    var floatingShapesEnabled by remember {
        mutableStateOf(prefs.getBoolean("gamespace_floating_shapes_enabled", true))
    }

    var colorSpec by remember {
        mutableStateOf(ColorEngine.fromPersisted(prefs.getString("color_spec", null)))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "header_image_uri" -> {
                    customWallpaperUri = context.getHeaderImage()
                }
                "gamespace_sidepanel_side" -> {
                    sidePanelSide = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                }
                "gamespace_sidepanel_sensitivity" -> {
                    sidePanelSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
                }
                "gamespace_sidepanel_length" -> {
                    sidePanelLength = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)
                }
                "gamespace_wallpaper_dimming_intensity" -> {
                    wallpaperDimmingIntensity = prefs.getInt("gamespace_wallpaper_dimming_intensity", 75).coerceIn(15, 100)
                }
                "gamespace_wallpaper_enabled", "enable_banner_image" -> {
                    isWallpaperEnabled = context.isHomeWallpaperEnabled()
                }
                "gamespace_wallpaper_blur" -> {
                    wallpaperBlur = context.getHomeWallpaperBlur()
                }
                "gamespace_floating_shapes_enabled" -> {
                    floatingShapesEnabled = prefs.getBoolean("gamespace_floating_shapes_enabled", true)
                }
                "color_mode" -> {
                    colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)
                }
                "color_spec" -> {
                    colorSpec = ColorEngine.fromPersisted(prefs.getString("color_spec", null))
                }
                "key_color" -> {
                    keyColorValue = prefs.getInt("key_color", 0)
                }
                "gamespace_app_scale" -> {
                    val updated = prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150)
                    appScalePercent = updated
                    if (pendingScaleConfirm == null) {
                        confirmedScale = updated
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isDarkPreview = remember(colorModeValue, systemDark) {
        when (ColorMode.fromValue(colorModeValue)) {
            ColorMode.LIGHT -> false
            ColorMode.DARK, ColorMode.DARKAMOLED -> true
            ColorMode.SYSTEM -> systemDark
        }
    }

    val followMonet = keyColorValue == 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Customize Side Panel dock & sensitivity, theme mode, interface scaling, and color palettes",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        CustomizeSidePanelSection(
            sidePanelSide = sidePanelSide,
            initialSensitivity = sidePanelSensitivity,
            initialLength = sidePanelLength,
            onSideChanged = {
                sidePanelSide = it
                prefs.edit().putString("gamespace_sidepanel_side", it).apply()
            },
            onSensitivityCommitted = {
                sidePanelSensitivity = it
                prefs.edit().putInt("gamespace_sidepanel_sensitivity", it).apply()
            },
            onLengthCommitted = {
                sidePanelLength = it
                prefs.edit().putInt("gamespace_sidepanel_length", it).apply()
            }
        )

        HomeWallpaperSection(
            customWallpaperUri = customWallpaperUri,
            initialDimming = wallpaperDimmingIntensity,
            initialBlur = wallpaperBlur,
            isWallpaperEnabled = isWallpaperEnabled,
            floatingShapesEnabled = floatingShapesEnabled,
            colorModeValue = colorModeValue,
            onChooseWallpaper = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onResetWallpaper = {
                context.clearHeaderImage()
                customWallpaperUri = null
            },
            onDimmingCommitted = {
                wallpaperDimmingIntensity = it
                prefs.edit().putInt("gamespace_wallpaper_dimming_intensity", it).apply()
            },
            onBlurCommitted = {
                wallpaperBlur = it
                context.setHomeWallpaperBlur(it)
            },
            onWallpaperEnabledChanged = {
                isWallpaperEnabled = it
                context.setHomeWallpaperEnabled(it)
            },
            onFloatingShapesToggled = {
                floatingShapesEnabled = it
                prefs.edit().putBoolean("gamespace_floating_shapes_enabled", it).apply()
            }
        )

        GameWallpaperSection(
            colorModeValue = colorModeValue
        )

        ThemeModeSection(
            colorModeValue = colorModeValue,
            onModeSelected = {
                colorModeValue = it
                prefs.edit().putInt("color_mode", it).apply()
            }
        )

        AppScaleSection(
            initialScale = appScalePercent,
            onScaleCommitted = { next ->
                if (next != confirmedScale) {
                    pendingScaleConfirm = next
                    appScalePercent = next
                    prefs.edit().putInt("gamespace_app_scale", next).apply()
                }
            }
        )

        PaletteSection(
            followMonet = followMonet,
            keyColorValue = keyColorValue,
            isDarkPreview = isDarkPreview,
            colorSpec = colorSpec,
            onFollowMonet = {
                keyColorValue = 0
                prefs.edit().putInt("key_color", 0).apply()
            },
            onSelectPreset = { colorInt ->
                keyColorValue = colorInt
                prefs.edit().putInt("key_color", colorInt).apply()
            }
        )
    }

    pendingScaleConfirm?.let { pendingScale ->
        ScaleConfirmationDialog(
            newScalePercent = pendingScale,
            onConfirm = {
                confirmedScale = pendingScale
                pendingScaleConfirm = null
            },
            onRevert = {
                appScalePercent = confirmedScale
                prefs.edit().putInt("gamespace_app_scale", confirmedScale).apply()
                pendingScaleConfirm = null
            }
        )
    }
}

@Composable
private fun CustomizeSidePanelSection(
    sidePanelSide: String,
    initialSensitivity: Int,
    initialLength: Int,
    onSideChanged: (String) -> Unit,
    onSensitivityCommitted: (Int) -> Unit,
    onLengthCommitted: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var sliderSensitivity by remember(initialSensitivity) { mutableIntStateOf(initialSensitivity) }
    var sliderLength by remember(initialLength) { mutableIntStateOf(initialLength) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Customize Side Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Floating pull-out bar pinned to screen edge when launching an app",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Column: Pinned Edge Position
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pinned Edge Position",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val sideOptions = listOf(
                            "left" to "Left Side",
                            "right" to "Right Side"
                        )
                        sideOptions.forEach { (sideKey, title) ->
                            val selected = sidePanelSide == sideKey
                            val cornerPercent by animateIntAsState(
                                targetValue = if (selected) 28 else 18,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "SidePanelEdgeCorner"
                            )
                            val cardShape = RoundedCornerShape(percent = cornerPercent)
                            Surface(
                                shape = cardShape,
                                color = if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .gamepadFocusOutline(shape = cardShape, register = true)
                                    .clip(cardShape)
                                    .clickable { onSideChanged(sideKey) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = scheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(
                                                        if (sideKey == "left") Alignment.CenterStart
                                                        else Alignment.CenterEnd
                                                    )
                                                    .width(5.dp)
                                                    .height((11f + (sliderLength / 100f) * 16f).dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (selected) scheme.primary else scheme.onSurfaceVariant
                                                    )
                                            )
                                            Icon(
                                                imageVector = if (sideKey == "left") {
                                                    Icons.Rounded.ChevronRight
                                                } else {
                                                    Icons.AutoMirrored.Rounded.ArrowBack
                                                },
                                                contentDescription = null,
                                                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .align(
                                                        if (sideKey == "left") Alignment.CenterStart
                                                        else Alignment.CenterEnd
                                                    )
                                                    .padding(
                                                        start = if (sideKey == "left") 8.dp else 0.dp,
                                                        end = if (sideKey == "right") 8.dp else 0.dp
                                                    )
                                                    .size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Swipe-In Sensitivity
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Swipe-In Sensitivity",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "${sliderSensitivity}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = scheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                        targetValue = if (selected) 50 else 26,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "SensPresetRadius"
                                    )
                                    val pillShape = RoundedCornerShape(percent = pillRadius)
                                    Surface(
                                        shape = pillShape,
                                        color = if (selected) scheme.primary else scheme.surfaceContainerLow,
                                        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .gamepadFocusOutline(shape = pillShape, register = true)
                                            .clip(pillShape)
                                            .clickable {
                                                sliderSensitivity = targetVal
                                                onSensitivityCommitted(targetVal)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                    onSensitivityCommitted(sliderSensitivity)
                                },
                                valueRange = 20f..100f,
                                steps = 15,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Row: Edge Bar Length (Dynamically scaled to screen size)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edge Bar Length",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "${sliderLength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val lengthPresets = listOf(
                                "Compact" to 30,
                                "Balanced" to 50,
                                "Long" to 80
                            )
                            lengthPresets.forEach { (label, targetVal) ->
                                val selected = when (targetVal) {
                                    30 -> sliderLength <= 38
                                    80 -> sliderLength >= 68
                                    else -> sliderLength in 39..67
                                }
                                val pillRadius by animateIntAsState(
                                    targetValue = if (selected) 50 else 26,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "LengthPresetRadius"
                                )
                                val pillShape = RoundedCornerShape(percent = pillRadius)
                                Surface(
                                    shape = pillShape,
                                    color = if (selected) scheme.primary else scheme.surfaceContainerLow,
                                    contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .gamepadFocusOutline(shape = pillShape, register = true)
                                        .clip(pillShape)
                                        .clickable {
                                            sliderLength = targetVal
                                            onLengthCommitted(targetVal)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        ZenithSlider(
                            value = sliderLength.toFloat(),
                            onValueChange = { raw ->
                                val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(20, 100)
                                if (snapped != sliderLength) {
                                    sliderLength = snapped
                                }
                            },
                            onValueChangeFinished = {
                                onLengthCommitted(sliderLength)
                            },
                            valueRange = 20f..100f,
                            steps = 15,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWallpaperSection(
    customWallpaperUri: String?,
    initialDimming: Int,
    initialBlur: Int,
    isWallpaperEnabled: Boolean,
    floatingShapesEnabled: Boolean,
    colorModeValue: Int,
    onChooseWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onDimmingCommitted: (Int) -> Unit,
    onBlurCommitted: (Int) -> Unit,
    onWallpaperEnabledChanged: (Boolean) -> Unit,
    onFloatingShapesToggled: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var sliderDimming by remember(initialDimming) { mutableIntStateOf(initialDimming) }
    var sliderBlur by remember(initialBlur) { mutableIntStateOf(initialBlur) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Home Wallpaper",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Customize stage and hub background wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isWallpaperEnabled) "Enabled" else "Off",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isWallpaperEnabled) scheme.primary else scheme.onSurfaceVariant
                    )
                    GameSpaceSwitch(
                        checked = isWallpaperEnabled,
                        onCheckedChange = onWallpaperEnabledChanged
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visual Preview Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.surfaceContainerLowest,
                    modifier = Modifier
                        .weight(0.48f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isWallpaperEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(scheme.surfaceContainerLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Wallpaper",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            if (customWallpaperUri != null) {
                                MediaBannerRenderer(
                                    uriString = customWallpaperUri,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (sliderBlur > 0) Modifier.blur(sliderBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.banner_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (sliderBlur > 0) Modifier.blur(sliderBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                )
                            }
                            val previewDimScale = (sliderDimming / 75f).coerceIn(0.15f, 1.45f)
                            val isAmoled = colorModeValue == ColorMode.DARKAMOLED.value
                            val previewBaseDim = if (isAmoled) Color.Black else scheme.surfaceContainerLowest

                            // GameSpace horizontal gradient dimming
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colorStops = arrayOf(
                                                0.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.88f else 0.85f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.35f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.84f else 0.80f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.68f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.44f else 0.38f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                1.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.22f else 0.18f) * previewDimScale).coerceIn(0f, 0.98f))
                                            )
                                        )
                                    )
                            )
                            // GameSpace vertical vignette
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.32f else 0.28f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.45f to Color.Transparent,
                                                1.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.48f else 0.42f) * previewDimScale).coerceIn(0f, 0.98f))
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Action Buttons Column (Symmetrical 110.dp matching the preview height)
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .height(110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chooseShape = RoundedCornerShape(16.dp)
                    Surface(
                        shape = chooseShape,
                        color = scheme.primary,
                        contentColor = scheme.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .gamepadFocusOutline(shape = chooseShape, register = true)
                            .clip(chooseShape)
                            .clickable(onClick = onChooseWallpaper)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (customWallpaperUri != null) "Change Wallpaper" else "Choose Wallpaper",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = customWallpaperUri != null,
                        enter = fadeIn(motion.defaultEffectsSpec()) + expandVertically(motion.defaultSpatialSpec()),
                        exit = fadeOut(motion.defaultEffectsSpec()) + shrinkVertically(motion.defaultSpatialSpec())
                    ) {
                        val resetShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = resetShape,
                            color = scheme.surfaceContainerHighest,
                            contentColor = scheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .gamepadFocusOutline(shape = resetShape, register = true)
                                .clip(resetShape)
                                .clickable(onClick = onResetWallpaper)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = scheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Reset to Default",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.error,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Dimming Intensity Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dimming Intensity",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Adjust wallpaper gradient dimming opacity",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$sliderDimming%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }

                ZenithSlider(
                    value = sliderDimming.toFloat(),
                    onValueChange = { raw ->
                        val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(15, 100)
                        if (snapped != sliderDimming) {
                            sliderDimming = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onDimmingCommitted(sliderDimming)
                    },
                    valueRange = 15f..100f,
                    steps = 16,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }

            // Wallpaper Blur Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wallpaper Blur",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Adjust background depth-of-field blur",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (sliderBlur == 0) "Sharp (0 dp)" else "$sliderBlur dp",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }

                ZenithSlider(
                    value = sliderBlur.toFloat(),
                    onValueChange = { raw ->
                        val snapped = raw.roundToInt().coerceIn(0, 25)
                        if (snapped != sliderBlur) {
                            sliderBlur = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onBlurCommitted(sliderBlur)
                    },
                    valueRange = 0f..25f,
                    steps = 24,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }

            // Floating Shapes Toggle Card
            val shapesToggleShape = RoundedCornerShape(18.dp)
            Surface(
                shape = shapesToggleShape,
                color = if (floatingShapesEnabled) scheme.surfaceContainerHighest.copy(alpha = 0.65f) else scheme.surfaceContainerHighest.copy(alpha = 0.35f),
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .gamepadFocusOutline(shape = shapesToggleShape, register = true)
                    .clip(shapesToggleShape)
                    .clickable { onFloatingShapesToggled(!floatingShapesEnabled) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (floatingShapesEnabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (floatingShapesEnabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Floating Shapes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Material 3 shapes hovering & morphing on homescreen",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    GameSpaceSwitch(
                        checked = floatingShapesEnabled,
                        onCheckedChange = onFloatingShapesToggled
                    )
                }
            }
        }
    }
}

@Composable
private fun GameWallpaperSection(
    colorModeValue: Int
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (ApplistViewmodel.apps.isEmpty()) {
            ApplistViewmodel().loadApps(context)
        }
    }

    val installed = ApplistViewmodel.apps
    val targetGames = remember(installed) {
        val configured = installed.filter { it.isEnabledInConfig || it.isRecommended }
        if (configured.isNotEmpty()) configured else installed.filter { !it.isSystem }
    }

    var selectedMode by remember(targetGames) {
        mutableStateOf(context.resolveGlobalGameWallpaperMode(targetGames.map { it.packageName }))
    }

    DisposableEffect(prefs, targetGames) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "gamespace_global_game_wallpaper_mode" || key?.startsWith("game_wallpaper_") == true) {
                selectedMode = context.resolveGlobalGameWallpaperMode(targetGames.map { it.packageName })
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var showBatchDialog by remember { mutableStateOf(false) }
    var pendingBatchUri by remember { mutableStateOf<String?>(null) }

    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(sourceUri) ?: ""
                val isVideo = mimeType.startsWith("video/")
                val extension = when {
                    isVideo -> "mp4"
                    mimeType == "image/gif" -> "gif"
                    mimeType == "image/png" -> "png"
                    mimeType == "image/webp" -> "webp"
                    else -> "jpg"
                }
                val savedUri = context.saveMediaDirectly(sourceUri, extension)
                if (savedUri != null) {
                    withContext(Dispatchers.Main) {
                        pendingBatchUri = savedUri
                        showBatchDialog = true
                    }
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Game Wallpaper",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Default background wallpaper mode and batch artwork across games",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            GameWallpaperOptionRow(
                title = "Default",
                subtitle = "Dynamic artwork generated from each game's icon",
                icon = Icons.Rounded.AutoAwesome,
                isSelected = selectedMode == "default",
                onClick = {
                    selectedMode = "default"
                    context.setGlobalGameWallpaperMode("default")
                    val pkgs = targetGames.map { it.packageName }
                    if (pkgs.isNotEmpty()) {
                        context.batchSetGameWallpaperMode(pkgs, "default")
                    }
                }
            )

            GameWallpaperOptionRow(
                title = "Follow Home Wallpaper",
                subtitle = "All games mirror the current Home tab wallpaper",
                icon = Icons.Rounded.Sync,
                isSelected = selectedMode == "follow_home",
                onClick = {
                    selectedMode = "follow_home"
                    context.setGlobalGameWallpaperMode("follow_home")
                    val pkgs = targetGames.map { it.packageName }
                    if (pkgs.isNotEmpty()) {
                        context.batchSetGameWallpaperMode(pkgs, "follow_home")
                    }
                }
            )

            GameWallpaperOptionRow(
                title = "Custom",
                subtitle = "Configure wallpaper individually per games",
                icon = Icons.Rounded.Palette,
                isSelected = selectedMode == "custom",
                onClick = {
                    selectedMode = "custom"
                    context.setGlobalGameWallpaperMode("custom")
                }
            )

            AnimatedVisibility(
                visible = selectedMode == "custom",
                enter = fadeIn(motion.defaultEffectsSpec()) + expandVertically(motion.defaultSpatialSpec()),
                exit = fadeOut(motion.defaultEffectsSpec()) + shrinkVertically(motion.defaultSpatialSpec())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val batchShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = batchShape,
                            color = scheme.primary,
                            contentColor = scheme.onPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .gamepadFocusOutline(shape = batchShape, register = true)
                                .clip(batchShape)
                                .clickable {
                                    batchImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Choose Wallpaper",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }

                        val resetShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = resetShape,
                            color = scheme.surfaceContainerHighest,
                            contentColor = scheme.error,
                            modifier = Modifier
                                .gamepadFocusOutline(shape = resetShape, register = true)
                                .clip(resetShape)
                                .clickable {
                                    val pkgs = targetGames.map { it.packageName }
                                    if (pkgs.isNotEmpty()) {
                                        context.batchResetGameWallpapers(pkgs)
                                        Toast.makeText(context, "Reset all game wallpapers to default", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = scheme.error
                                    )
                                    Text(
                                        text = "Reset All",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.error,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBatchDialog && pendingBatchUri != null) {
        ApplyGameWallpaperDialog(
            uri = pendingBatchUri!!,
            games = targetGames,
            onDismiss = {
                showBatchDialog = false
                pendingBatchUri = null
            },
            onApply = { selectedPackages ->
                if (selectedPackages.isNotEmpty()) {
                    context.batchSetGameWallpaper(selectedPackages, pendingBatchUri!!)
                    Toast.makeText(
                        context,
                        "Applied wallpaper to ${selectedPackages.size} game${if (selectedPackages.size > 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showBatchDialog = false
                pendingBatchUri = null
            }
        )
    }
}

@Composable
private fun GameWallpaperOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        shape = cardShape,
        color = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.5f),
        contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = cardShape, register = true)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) scheme.primary else scheme.surfaceContainerHighest,
                contentColor = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ApplyGameWallpaperDialog(
    uri: String,
    games: List<ApplistViewmodel.AppInfo>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var applyToAll by remember { mutableStateOf(true) }
    var selectedSet by remember { mutableStateOf(games.map { it.packageName }.toSet()) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            modifier = (if (isLandscape) {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.96f)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            } else {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .padding(12.dp)
            })
                .tapHaptic()
                .gamepadKeyWatch()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                         event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE)
                    ) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = if (isLandscape) 10.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primaryContainer,
                            contentColor = scheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Apply Wallpaper",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Choose either for all games or selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }

                // Middle Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Left Pane: preview & option toggles
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    MediaBannerRenderer(
                                        uriString = uri,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                val allShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = allShape,
                                    color = if (applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    contentColor = if (applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(allShape)
                                        .clickable { applyToAll = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "All Games (${games.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Apply to all library games",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (applyToAll) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                val selectShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = selectShape,
                                    color = if (!applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    contentColor = if (!applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(selectShape)
                                        .clickable { applyToAll = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (!applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "Select Games (${selectedSet.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Choose specific games",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (!applyToAll) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Pane: Game list or All Games confirmation
                            if (applyToAll) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = scheme.surfaceContainerLow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.primaryContainer,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = scheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "All ${games.size} games will receive this artwork",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = "Every game stage will showcase this wallpaper",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            items(games) { game ->
                                                AppIconImage(app = game, size = 26.dp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedSet.size} of ${games.size} selected",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = scheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = { selectedSet = games.map { it.packageName }.toSet() },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Select All", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = { selectedSet = emptySet() },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(games) { game ->
                                            val isChecked = selectedSet.contains(game.packageName)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isChecked) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surfaceContainerLow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        selectedSet = if (isChecked) {
                                                            selectedSet - game.packageName
                                                        } else {
                                                            selectedSet + game.packageName
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    AppIconImage(app = game, size = 26.dp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = game.label,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = scheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = game.packageName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = scheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            selectedSet = if (checked) {
                                                                selectedSet + game.packageName
                                                            } else {
                                                                selectedSet - game.packageName
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait layout
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = scheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                MediaBannerRenderer(
                                    uriString = uri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val allShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = allShape,
                                    color = if (applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                    contentColor = if (applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(allShape)
                                        .clickable { applyToAll = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "All Games (${games.size})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                val selectShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = selectShape,
                                    color = if (!applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                    contentColor = if (!applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(selectShape)
                                        .clickable { applyToAll = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (!applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Select Games",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            if (!applyToAll) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedSet.size} of ${games.size} selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { selectedSet = games.map { it.packageName }.toSet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Select All", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = { selectedSet = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(games) { game ->
                                        val isChecked = selectedSet.contains(game.packageName)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isChecked) scheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedSet = if (isChecked) {
                                                        selectedSet - game.packageName
                                                    } else {
                                                        selectedSet + game.packageName
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                AppIconImage(app = game, size = 32.dp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = game.label,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = scheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = game.packageName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = scheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedSet = if (checked) {
                                                            selectedSet + game.packageName
                                                        } else {
                                                            selectedSet - game.packageName
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All ${games.size} games will receive this artwork",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    val targetCount = if (applyToAll) games.size else selectedSet.size
                    Button(
                        onClick = {
                            val targetList = if (applyToAll) games.map { it.packageName } else selectedSet.toList()
                            onApply(targetList)
                        },
                        enabled = targetCount > 0,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply ($targetCount)")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSection(
    colorModeValue: Int,
    onModeSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (colorModeValue == ColorMode.DARKAMOLED.value) {
                            "AMOLED mode draws pure black backgrounds where possible"
                        } else {
                            "Auto, Light, and Dark follow Monet dynamic surface tones"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = when (ColorMode.fromValue(colorModeValue)) {
                            ColorMode.SYSTEM -> "Auto"
                            ColorMode.LIGHT -> "Light"
                            ColorMode.DARK -> "Dark"
                            ColorMode.DARKAMOLED -> "AMOLED"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val modes = listOf(
                    Triple("Auto", ColorMode.SYSTEM.value, Icons.Rounded.BrightnessAuto),
                    Triple("Light", ColorMode.LIGHT.value, Icons.Rounded.LightMode),
                    Triple("Dark", ColorMode.DARK.value, Icons.Rounded.DarkMode),
                    Triple("AMOLED", ColorMode.DARKAMOLED.value, Icons.Rounded.Android)
                )
                modes.forEach { (name, value, icon) ->
                    val selected = colorModeValue == value
                    val pillRadius by animateIntAsState(
                        targetValue = if (selected) 50 else 28,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "ThemeModePillRadius"
                    )
                    val shape = RoundedCornerShape(percent = pillRadius)
                    Surface(
                        shape = shape,
                        color = if (selected) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .gamepadFocusOutline(shape = shape, register = true)
                            .clip(shape)
                            .clickable { onModeSelected(value) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) scheme.onPrimary else scheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScaleSection(
    initialScale: Int,
    onScaleCommitted: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var sliderScalePercent by remember(initialScale) { mutableIntStateOf(initialScale) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = null,
                                tint = scheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "App Scale",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "60% downscale to 150% upscale",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stepBtnShape = RoundedCornerShape(12.dp)
                    Surface(
                        shape = stepBtnShape,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .size(34.dp)
                            .gamepadFocusOutline(shape = stepBtnShape, register = true)
                            .clip(stepBtnShape)
                            .clickable(enabled = sliderScalePercent > 60) {
                                val next = (sliderScalePercent - 5).coerceIn(60, 150)
                                sliderScalePercent = next
                                onScaleCommitted(next)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease scale",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (sliderScalePercent == 85) scheme.primaryContainer else scheme.primary,
                        contentColor = if (sliderScalePercent == 85) scheme.onPrimaryContainer else scheme.onPrimary,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = CircleShape, register = true)
                            .clip(CircleShape)
                            .clickable {
                                sliderScalePercent = 85
                                onScaleCommitted(85)
                            }
                    ) {
                        Text(
                            text = "${sliderScalePercent}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = stepBtnShape,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .size(34.dp)
                            .gamepadFocusOutline(shape = stepBtnShape, register = true)
                            .clip(stepBtnShape)
                            .clickable(enabled = sliderScalePercent < 150) {
                                val next = (sliderScalePercent + 5).coerceIn(60, 150)
                                sliderScalePercent = next
                                onScaleCommitted(next)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase scale",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            ZenithSlider(
                value = sliderScalePercent.toFloat(),
                onValueChange = { raw ->
                    val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(60, 150)
                    sliderScalePercent = snapped
                },
                onValueChangeFinished = {
                    onScaleCommitted(sliderScalePercent)
                },
                valueRange = 60f..150f,
                steps = 17,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

@Composable
private fun PaletteSection(
    followMonet: Boolean,
    keyColorValue: Int,
    isDarkPreview: Boolean,
    colorSpec: ColorEngine = ColorEngine.Default,
    onFollowMonet: () -> Unit,
    onSelectPreset: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val initialVisibleCount = 6
    val isSelectionInOverflow = remember(keyColorValue) {
        GAMESPACE_PRESET_KEY_COLORS.drop(initialVisibleCount).any { it.first == keyColorValue }
    }
    var showAllPresets by remember { mutableStateOf(isSelectionInOverflow) }
    val effectiveShowAll = showAllPresets || isSelectionInOverflow
    val displayedPresets = if (effectiveShowAll) {
        GAMESPACE_PRESET_KEY_COLORS
    } else {
        GAMESPACE_PRESET_KEY_COLORS.take(initialVisibleCount)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Theme Palette",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (followMonet) {
                            "Following system dynamic wallpaper palette"
                        } else {
                            val presetLabel = GAMESPACE_PRESET_KEY_COLORS.firstOrNull { it.first == keyColorValue }?.second ?: "Custom"
                            "Using $presetLabel preset color seed from Personalization"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val monetPillShape = RoundedCornerShape(percent = if (followMonet) 50 else 28)
                    Surface(
                        shape = monetPillShape,
                        color = if (followMonet) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (followMonet) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = monetPillShape, register = true)
                            .clip(monetPillShape)
                            .clickable(onClick = onFollowMonet)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dynamic",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    val presetPillShape = RoundedCornerShape(percent = if (!followMonet) 50 else 28)
                    Surface(
                        shape = presetPillShape,
                        color = if (!followMonet) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (!followMonet) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = presetPillShape, register = true)
                            .clip(presetPillShape)
                            .clickable {
                                if (keyColorValue == 0) {
                                    val firstPreset = GAMESPACE_PRESET_KEY_COLORS.first().first
                                    onSelectPreset(firstPreset)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Preset Color",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = displayedPresets,
                    key = { it.first }
                ) { (colorInt, name) ->
                    GameSpacePaletteSwatch(
                        seedColor = Color(colorInt),
                        label = name,
                        selected = keyColorValue == colorInt,
                        isDark = isDarkPreview,
                        colorSpec = colorSpec,
                        onClick = { onSelectPreset(colorInt) }
                    )
                }

                if (!effectiveShowAll) {
                    item(key = "load_more_presets") {
                        val shape = RoundedCornerShape(percent = 24)
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier
                                .width(84.dp)
                                .height(112.dp)
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clip(shape)
                                .clickable { showAllPresets = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Load more presets",
                                            tint = scheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "+${GAMESPACE_PRESET_KEY_COLORS.size - initialVisibleCount} More",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else if (!isSelectionInOverflow) {
                    item(key = "show_less_presets") {
                        val shape = RoundedCornerShape(percent = 24)
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier
                                .width(84.dp)
                                .height(112.dp)
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clip(shape)
                                .clickable { showAllPresets = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ExpandLess,
                                            contentDescription = "Show less presets",
                                            tint = scheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Show Less",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GameSpacePaletteSwatch(
    seedColor: Color,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    colorSpec: ColorEngine = ColorEngine.Default,
    onClick: () -> Unit
) {
    val currentScheme = MaterialTheme.colorScheme
    val swatchScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        specVersion = colorSpec.librarySpec
    )

    val cornerPercent by animateIntAsState(
        targetValue = if (selected) 36 else 24,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SwatchCornerPercent"
    )
    val shape = RoundedCornerShape(percent = cornerPercent)

    Surface(
        shape = shape,
        color = if (selected) swatchScheme.primaryContainer else currentScheme.surfaceContainerHighest,
        modifier = Modifier
            .width(84.dp)
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    drawArc(
                        color = swatchScheme.primary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.tertiaryContainer,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.secondaryContainer,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = swatchScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = swatchScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(swatchScheme.primary)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (selected) swatchScheme.onPrimaryContainer else currentScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
