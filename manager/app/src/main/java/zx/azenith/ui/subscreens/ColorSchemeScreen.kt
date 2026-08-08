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

package zx.azenith.ui.subscreens

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.topjohnwu.superuser.Shell
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils

enum class ColorPreset(
    val label: String,
    val r: Float,
    val g: Float,
    val b: Float,
    val s: Float
) {
    DEFAULT("Default", 1000f, 1000f, 1000f, 1000f),
    VIVID("Vivid", 1000f, 1000f, 1000f, 1250f),
    WARM("Warm", 1050f, 1000f, 950f, 1100f),
    COOL("Cool", 950f, 950f, 1050f, 1000f),
    AMOLED("AMOLED", 1020f, 1020f, 1020f, 1150f),
    CUSTOM("Custom", -1f, -1f, -1f, -1f)
}

@Composable
fun ColorSchemeSettings(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    
    var redVal by remember { mutableFloatStateOf(1000f) }
    var greenVal by remember { mutableFloatStateOf(1000f) }
    var blueVal by remember { mutableFloatStateOf(1000f) }
    var satVal by remember { mutableFloatStateOf(1000f) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf<ColorPreset?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val resetToastMsg = stringResource(R.string.toast_settings_reset)
    
    // Check if current values match a preset
    val matchingPreset = remember(redVal, greenVal, blueVal, satVal) {
        ColorPreset.values().firstOrNull { preset ->
            preset != ColorPreset.CUSTOM &&
            preset.r == redVal && preset.g == greenVal && preset.b == blueVal && preset.s == satVal
        }
    }

    val applyRGB = { r: Float, g: Float, b: Float ->
        Shell.cmd("service call SurfaceFlinger 1015 i32 1 f ${r / 1000f} f 0 f 0 f 0 f 0 f ${g / 1000f} f 0 f 0 f 0 f 0 f ${b / 1000f} f 0 f 0 f 0 f 0 f 1").submit()
    }

    val applySat = { s: Float ->
        Shell.cmd("service call SurfaceFlinger 1022 f ${s / 1000f}").submit()
    }

    val saveToProp = {
        val config = "${redVal.toInt()} ${greenVal.toInt()} ${blueVal.toInt()} ${satVal.toInt()}"
        PropertyUtils.set("persist.sys.azenithconf.schemeconfig", config)
    }

    LaunchedEffect(Unit) {
        val rawProp = PropertyUtils.get("persist.sys.azenithconf.schemeconfig")
        if (rawProp.isNotEmpty()) {
            val parts = rawProp.split(" ").mapNotNull { it.toFloatOrNull() }
            if (parts.size >= 4) {
                redVal = parts[0]
                greenVal = parts[1]
                blueVal = parts[2]
                satVal = parts[3]
            }
        }
        isLoading = false
        
        // Auto-detect preset on load
        selectedPreset = ColorPreset.values().firstOrNull { preset ->
            preset != ColorPreset.CUSTOM &&
            preset.r == redVal && preset.g == greenVal && preset.b == blueVal && preset.s == satVal
        }
    }
    
    // Auto-switch to CUSTOM if values don't match selected preset
    LaunchedEffect(matchingPreset) {
        if (matchingPreset == null && selectedPreset != ColorPreset.CUSTOM) {
            selectedPreset = ColorPreset.CUSTOM
        }
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ColorSchemeTopAppBar(scrollBehavior, onBack = { navController.popBackStack() })
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = colorScheme.surface
        ) { innerPadding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    item {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveInfoCard(
                                        supportingContent = { 
                                            Text(text = stringResource(R.string.str_adjust_and_calibrate_your_scre)) 
                                        },
                                        leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                        containerColor = colorScheme.surfaceContainerLow,
                                        onClick = {}
                                    )
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = colorScheme.surfaceContainerLow
                        ) {
                            AsyncImage(
                                model = R.drawable.schemeillust,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    item { PrefSectionTitle(stringResource(R.string.presets)) }
                    
                    item {
                        PresetSelectorItem(
                            currentPreset = selectedPreset ?: matchingPreset,
                            onPresetSelected = { preset ->
                                if (preset == ColorPreset.CUSTOM) {
                                    // CUSTOM mode: cukup enable sliders, retain current values
                                    selectedPreset = ColorPreset.CUSTOM
                                } else {
                                    // Apply preset values
                                    redVal = preset.r
                                    greenVal = preset.g
                                    blueVal = preset.b
                                    satVal = preset.s
                                    applyRGB(redVal, greenVal, blueVal)
                                    applySat(satVal)
                                    saveToProp()
                                    selectedPreset = preset
                                }
                            }
                        )
                    }
                    
                    item { PrefSectionTitle(stringResource(R.string.color_scheme)) }
                    
                    item {
                        val isCustomMode = selectedPreset == ColorPreset.CUSTOM || matchingPreset == null
                        ExpressiveList(
                            content = listOf(
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_red),
                                        summary = stringResource(R.string.color_red_desc),
                                        value = redVal,
                                        accentColor = Color(0xFFEF5350),
                                        enabled = isCustomMode,
                                        onValueChange = { 
                                            redVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                },
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_green),
                                        summary = stringResource(R.string.color_green_desc),
                                        value = greenVal,
                                        accentColor = Color(0xFF66BB6A),
                                        enabled = isCustomMode,
                                        onValueChange = { 
                                            greenVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                },
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_blue),
                                        summary = stringResource(R.string.color_blue_desc),
                                        value = blueVal,
                                        accentColor = Color(0xFF42A5F5),
                                        enabled = isCustomMode,
                                        onValueChange = { 
                                            blueVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                }
                            )
                        )
                    }
                    
                    item {
                        val isCustomMode = selectedPreset == ColorPreset.CUSTOM || matchingPreset == null
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpressiveList(
                            content = listOf(
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_saturation),
                                        summary = stringResource(R.string.color_saturation_desc),
                                        value = satVal,
                                        accentColor = colorScheme.primary,
                                        enabled = isCustomMode,
                                        onValueChange = { 
                                            satVal = it
                                            applySat(satVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetSelectorItem(
    currentPreset: ColorPreset?,
    onPresetSelected: (ColorPreset) -> Unit
) {
    ExpressiveList(
        content = ColorPreset.values().map { preset ->
            {
                ExpressiveRadioItem(
                    title = preset.label,
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    )
}

@Composable
fun ColorSliderItem(
    label: String,
    summary: String,
    value: Float,
    accentColor: Color,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = value / 2000f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )
    
    val alpha = if (enabled) 1f else 0.5f
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(durationMillis = 300),
        label = "sliderAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(animatedAlpha)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.outline
                )
            }
            
            Surface(
                color = if (value == 1000f) colorScheme.surfaceVariant else colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = value.toInt().toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (value == 1000f) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceContainerHighest)
            )
            

            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.6f), accentColor)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = { newValue ->
                if (enabled) {
                    val stickyValue = when {
                        newValue < 60f -> 0f
                        newValue in 960f..1040f -> 1000f
                        newValue > 1940f -> 2000f
                        else -> newValue
                    }
                    onValueChange(stickyValue)
                }
            },
            onValueChangeFinished = onFinish,
            valueRange = 0f..2000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledThumbColor = accentColor.copy(alpha = 0.5f),
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.min_val, 0), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
            Text(stringResource(R.string.default_val, 1000), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
            Text(stringResource(R.string.max_val, 2000), style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
        }
    }
}

@Composable
fun SchemeSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorSchemeTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        0.5f to colorScheme.surface.copy(alpha = 0.8f),
        0.6f to colorScheme.surface.copy(alpha = 0.7f),
        0.7f to colorScheme.surface.copy(alpha = 0.5f),
        0.8f to colorScheme.surface.copy(alpha = 0.4f),
        0.9f to colorScheme.surface.copy(alpha = 0.3f),
        1.0f to Color.Transparent 
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.color_scheme),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },        
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
