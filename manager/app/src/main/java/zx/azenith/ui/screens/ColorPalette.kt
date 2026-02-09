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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
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
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getHeaderImage
import android.net.Uri
import com.yalantis.ucrop.UCrop
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale 
import java.io.File
// TAMBAHKAN IMPORT INI JIKA BELUM ADA
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
    
private val keyColorOptions = listOf(
    Color(0xFF1A73E8).toArgb(),
    Color(0xFFEA4335).toArgb(),
    Color(0xFF34A853).toArgb(),
    Color(0xFF9333EA).toArgb(),
    Color(0xFFFB8C00).toArgb(),
    Color(0xFF009688).toArgb(),
    Color(0xFFE91E63).toArgb(),
    Color(0xFF795548).toArgb(),
)

@Composable
fun ColorPaletteScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var hasCustomHeader by rememberSaveable {
        mutableStateOf(context.getHeaderImage() != null)
    }
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                context.saveHeaderImage(it.toString())
                hasCustomHeader = true
            }
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_banner_${System.currentTimeMillis()}.jpg"))
            
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(20f, 9f)
                .withOptions(UCrop.Options().apply {
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                })
    
            cropLauncher.launch(uCrop.getIntent(context))
        }
    }

    var currentColorMode by remember { 
        mutableStateOf(ThemeController.getAppSettings(context).colorMode) 
    }
    var currentKeyColor by remember { 
        mutableIntStateOf(ThemeController.getAppSettings(context).keyColor) 
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PaletteTopAppBar(
                scrollBehavior,
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface // Pastikan ini ada agar gradien terlihat menyatu
    ) { innerPadding -> // Perhatikan tanda kurung ini
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gunakan 'item' untuk membungkus Composable di dalam LazyColumn
            item {
                val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
                ThemePreviewCard(keyColor = currentKeyColor, isDark = isDark)
            }

            item {
                val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Accent Color",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        ColorButton(
                            color = Color.Unspecified,
                            isSelected = currentKeyColor == 0,
                            isDark = isDark,
                            onClick = {
                                currentKeyColor = 0
                                prefs.edit { putInt("key_color", 0) }
                            }
                        )

                        keyColorOptions.forEach { colorInt ->
                            ColorButton(
                                color = Color(colorInt),
                                isSelected = currentKeyColor == colorInt,
                                isDark = isDark,
                                onClick = {
                                    currentKeyColor = colorInt
                                    prefs.edit { putInt("key_color", colorInt) }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Appearance",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val options = listOf(
                        ColorMode.SYSTEM, ColorMode.LIGHT, ColorMode.DARK, ColorMode.DARKAMOLED
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        options.forEachIndexed { index, mode ->
                            ToggleButton(
                                checked = currentColorMode == mode,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        currentColorMode = mode
                                        prefs.edit { putInt("color_mode", mode.value) }
                                    }
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARKAMOLED -> Icons.Filled.Brightness1
                                    },
                                    contentDescription = mode.name
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Banner",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        val headerOptions = listOf(false, true)

                        headerOptions.forEachIndexed { index, isCustom ->
                            ToggleButton(
                                checked = hasCustomHeader == isCustom,
                                onCheckedChange = { checked ->
                                    if (!checked) return@ToggleButton

                                    if (isCustom) {
                                        hasCustomHeader = true
                                        imagePicker.launch(arrayOf("image/*"))
                                    } else {
                                        context.clearHeaderImage()
                                        hasCustomHeader = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    headerOptions.lastIndex ->
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCustom)
                                            Icons.Filled.Image
                                        else
                                            Icons.Filled.Restore,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = if (isCustom)
                                            "Custom"
                                        else
                                            "Default"
                                    )
                                }
                            }
                        }                
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PaletteTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                    text = "Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

@Composable
private fun ThemePreviewCard(keyColor: Int, isDark: Boolean) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    
    val colorScheme = when {
        keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        keyColor == 0 -> if (isDark) darkColorScheme() else expressiveLightColorScheme()
        else -> rememberDynamicColorScheme(seedColor = Color(keyColor), isDark = isDark)
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(screenRatio),
            color = colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column {
                Box(modifier = Modifier.height(40.dp).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    Text("AZenith", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().height(32.dp), shape = RoundedCornerShape(8.dp)) {}
                    Surface(color = colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().height(20.dp), shape = RoundedCornerShape(6.dp)) {}
                    Surface(color = colorScheme.surfaceColorAtElevation(1.dp), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp)) {}
                }
                Surface(color = colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(36.dp)) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Home, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                        Icon(Icons.Filled.Settings, null, tint = colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButton(color: Color, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else MaterialTheme.colorScheme
    } else rememberDynamicColorScheme(seedColor = color, isDark = isDark)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawArc(color = colorScheme.primaryContainer, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                drawArc(color = colorScheme.tertiaryContainer, startAngle = 0f, sweepAngle = 180f, useCenter = true)
            }
            if (isSelected) {
                Box(modifier = Modifier.size(48.dp).border(2.dp, colorScheme.primary, CircleShape))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
