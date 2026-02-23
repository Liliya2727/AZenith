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

package zx.azenith.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zx.azenith.R
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.rounded.*
import zx.azenith.ui.component.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.superuser.Shell
import androidx.navigation.NavController
import android.content.Context
import android.view.WindowManager
import zx.azenith.ui.util.PropertyUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.SolidColor

// 3. Untuk Border pada Card
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.Arrangement

@Composable
fun ColorSchemeSettings(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // States untuk Slider (Range 0 - 2000)
    var redVal by remember { mutableFloatStateOf(1000f) }
    var greenVal by remember { mutableFloatStateOf(1000f) }
    var blueVal by remember { mutableFloatStateOf(1000f) }
    var satVal by remember { mutableFloatStateOf(1000f) }
    var isLoading by remember { mutableStateOf(true) }

    // Helper: Apply RGB ke SurfaceFlinger
    val applyRGB = { r: Float, g: Float, b: Float ->
        Shell.cmd("service call SurfaceFlinger 1015 i32 1 f ${r / 1000f} f 0 f 0 f 0 f 0 f ${g / 1000f} f 0 f 0 f 0 f 0 f ${b / 1000f} f 0 f 0 f 0 f 0 f 1").submit()
    }

    // Helper: Apply Saturation ke SurfaceFlinger
    val applySat = { s: Float ->
        Shell.cmd("service call SurfaceFlinger 1022 f ${s / 1000f}").submit()
    }

    // Helper: Save to Prop (Dijalankan saat slide selesai)
    val saveToProp = {
        val config = "${redVal.toInt()} ${greenVal.toInt()} ${blueVal.toInt()} ${satVal.toInt()}"
        PropertyUtils.set("persist.sys.azenithconf.schemeconfig", config)
    }

    // Load Initial Data
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
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ColorSchemeTopAppBar(scrollBehavior, onBack = { navController.popBackStack() })
            },
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
                        AsyncImage(
                            model = R.drawable.schemeillust,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text(stringResource(R.string.color_scheme)) },
                                        leadingContent = { Icon(Icons.Rounded.Palette, null) },
                                        trailingContent = {
                                            // TOMBOL RESET ke 1000
                                            IconButton(onClick = {
                                                redVal = 1000f
                                                greenVal = 1000f
                                                blueVal = 1000f
                                                satVal = 1000f
                                                applyRGB(1000f, 1000f, 1000f)
                                                applySat(1000f)
                                                saveToProp()
                                                Toast.makeText(context, context.getString(R.string.toast_settings_reset), Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.reset))
                                            }
                                        },
                                        containerColor = colorScheme.secondaryContainer,
                                        onClick = {}
                                    )
                                },
                                // SLIDER RED
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_red),
                                        summary = stringResource(R.string.color_red_desc),
                                        value = redVal,
                                        accentColor = Color(0xFFEF5350),
                                        onValueChange = { 
                                            redVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                },
                                // SLIDER GREEN
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_green),
                                        summary = stringResource(R.string.color_green_desc),
                                        value = greenVal,
                                        accentColor = Color(0xFF66BB6A),
                                        onValueChange = { 
                                            greenVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                },
                                // SLIDER BLUE
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_blue),
                                        summary = stringResource(R.string.color_blue_desc),
                                        value = blueVal,
                                        accentColor = Color(0xFF42A5F5),
                                        onValueChange = { 
                                            blueVal = it
                                            applyRGB(redVal, greenVal, blueVal)
                                        },
                                        onFinish = { saveToProp() }
                                    )
                                },
                                // SLIDER SATURATION
                                { 
                                    ColorSliderItem(
                                        label = stringResource(R.string.color_saturation),
                                        summary = stringResource(R.string.color_saturation_desc),
                                        value = satVal,
                                        accentColor = colorScheme.primary,
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
fun ColorSliderItem(
    label: String,
    summary: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = value / 2000f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
            
            // Badge nilai aktif (Primary jika bukan default 1000)
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

        // Custom Progress Bar Stack
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceContainerHighest)
            )
            
            // Foreground Track (Animated)
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.6f), accentColor)
                        )
                    )
            )

            // Slider Transparan untuk interaksi
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onFinish,
                valueRange = 0f..2000f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

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
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.schemecolor),
                    style = MaterialTheme.typography.titleLarge,
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