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
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.materialkolor.rememberDynamicColorScheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import zx.azenith.R
import androidx.compose.foundation.lazy.rememberLazyListState
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.component.*
import android.net.Uri
import com.yalantis.ucrop.UCrop
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import zx.azenith.ui.component.*
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import com.topjohnwu.superuser.Shell
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.SolidColor

// 3. Untuk Border pada Card
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.Arrangement
import zx.azenith.ui.util.PropertyUtils

@Composable
fun BypassChargeScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // State management using PropertyUtils
    var bypassPath by remember { mutableStateOf("") }
    var bypassChgState by remember { mutableStateOf<Boolean?>(null) }
    var thresholdValue by remember { mutableStateOf<Float?>(null) }
    
    // Check if the feature is supported
    val isUnsupported = bypassPath == "UNSUPPORTED"

    LaunchedEffect(Unit) {
        // Fetch values using PropertyUtils (Native/Reflection speed)
        bypassPath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "")
        
        val thresholdProp = PropertyUtils.get("persist.sys.azenithconf.bypasschgthreshold", "20")
        thresholdValue = thresholdProp.toFloatOrNull()?.coerceIn(20f, 50f) ?: 20f
        
        bypassChgState = PropertyUtils.get("persist.sys.azenithconf.bypasschg", "0") == "1"
    }

    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { BypassChgTopAppBar(scrollBehavior, onBack = { navController.popBackStack() }) },
            containerColor = colorScheme.surface 
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 24.dp + innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            
                item {
                    AsyncImage(
                        model = R.drawable.bypasschgillust,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(28.dp))
                            // Visual feedback if unsupported
                            .graphicsLayer(alpha = if (isUnsupported) 0.5f else 1f),
                        contentScale = ContentScale.Fit
                    )
                }

                // 1. Toggle Switch Section
                item {
                    if (bypassChgState != null) { 
                        ExpressiveList(
                            content = listOf {
                                ExpressiveSwitchItem(
                                    title = stringResource(R.string.enable_bypass_charge),
                                    summary = if (isUnsupported) stringResource(R.string.bypass_not_supported)
                                              else stringResource(R.string.enable_bypass_charge_desc),
                                    checked = bypassChgState!!,
                                    // Disable interaction if unsupported
                                    enabled = !isUnsupported,
                                    onCheckedChange = { isChecked ->
                                        bypassChgState = isChecked
                                        PropertyUtils.set("persist.sys.azenithconf.bypasschg", if (isChecked) "1" else "0")
                                    }
                                )
                            }
                        )
                    }
                }

                // 2. Charging Threshold Card
                thresholdValue?.let { currentVal -> 
                    item {
                        val animatedSliderValue by animateFloatAsState(
                            targetValue = currentVal,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                            label = "SliderAnimation"
                        )
                
                        val progress = (currentVal - 20f) / 30f 
                        val animatedBarProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            label = "BarProgress"
                        )
                        
                        // Set alpha for the whole card if disabled
                        val cardAlpha = if (isUnsupported) 0.5f else 1f

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(alpha = cardAlpha),
                            shape = RoundedCornerShape(26.dp),
                            color = colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.charging_threshold),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currentVal.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnsupported) colorScheme.outline else colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.charging_threshold_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.outline
                                )
    
                                Spacer(modifier = Modifier.height(24.dp))
    
                                // Custom Progress Bar
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
                                            .fillMaxWidth(animatedBarProgress)
                                            .height(8.dp)
                                            .align(Alignment.CenterStart)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                // Gunakan SolidColor agar tipenya sama-sama Brush
                                                if (isUnsupported) {
                                                    SolidColor(colorScheme.outline) 
                                                } else {
                                                    Brush.horizontalGradient(
                                                        listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)
                                                    )
                                                }
                                            )
                                    )
                                }
    
                                Spacer(modifier = Modifier.height(4.dp))
    
                                Slider(
                                    value = animatedSliderValue,
                                    enabled = !isUnsupported, // Disable slider
                                    onValueChange = { newValue -> 
                                        val step = 5f
                                        val snapped = (newValue / step).roundToInt() * step
                                        val finalValue = snapped.coerceIn(20f, 50f)
                
                                        thresholdValue = finalValue
                                        PropertyUtils.set("persist.sys.azenithconf.bypasschgthreshold", finalValue.toInt().toString())
                                    },
                                    valueRange = 20f..50f,
                                    steps = 5, 
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isUnsupported) colorScheme.outline else colorScheme.primary,
                                        disabledThumbColor = colorScheme.outline.copy(alpha = 0.5f),
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                )
    
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("20%", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    Text("50%", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
                
                item {
                    ExpressiveList(
                        content = listOf {
                            ExpressiveListItem(
                                onClick = { navController.navigate("bypasschg_check") },
                                headlineContent = { Text(stringResource(R.string.CompatibilityCheck)) },
                                supportingContent = { Text(stringResource(R.string.CompatibilityCheck_desc)) },
                                leadingContent = { Icon(Icons.Filled.CheckCircle, null) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BypassChgTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                    text = stringResource(R.string.bcharging),
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