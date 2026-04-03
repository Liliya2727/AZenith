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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import zx.azenith.ui.theme.animateColorSchemeAsState
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent

private val keyColorOptions = listOf(
    Color(0xFFF44336).toArgb(),
    Color(0xFFE91E63).toArgb(),
    Color(0xFF9C27B0).toArgb(),
    Color(0xFF673AB7).toArgb(),
    Color(0xFF3F51B5).toArgb(),
    Color(0xFF2196F3).toArgb(),
    Color(0xFF00BCD4).toArgb(),
    Color(0xFF009688).toArgb(),
    Color(0xFF4FAF50).toArgb(),
    Color(0xFFFFEB3B).toArgb(),
    Color(0xFFFFC107).toArgb(),
    Color(0xFFFF9800).toArgb(),
    Color(0xFF795548).toArgb(),
    Color(0xFF607D8F).toArgb(),
    Color(0xFFFF9CA8).toArgb(),
)

@Composable
fun ColorPaletteScreen(navController: NavController) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current 
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
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
    
    val colorScheme = MaterialTheme.colorScheme 
    
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_banner_${System.currentTimeMillis()}.jpg"))
            
            val options = UCrop.Options().apply {
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)

                setToolbarColor(colorScheme.surface.toArgb()) 
                setToolbarWidgetColor(colorScheme.onSurface.toArgb()) 
                setRootViewBackgroundColor(colorScheme.surfaceContainerLowest.toArgb()) 
                setActiveControlsWidgetColor(colorScheme.primary.toArgb()) 
                setCropFrameColor(colorScheme.primary.toArgb()) 
                setCropGridColor(colorScheme.primary.copy(alpha = 0.5f).toArgb()) 
                setDimmedLayerColor(colorScheme.scrim.copy(alpha = 0.6f).toArgb())
            }
            
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(20f, 9f)
                .withOptions(options)

            cropLauncher.launch(uCrop.getIntent(context))
        }
    }


    var currentColorMode by remember { 
        mutableStateOf(ThemeController.getAppSettings(context).colorMode) 
    }
    var currentKeyColor by remember { 
        mutableIntStateOf(ThemeController.getAppSettings(context).keyColor) 
    }

    val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
    val amoledMode = currentColorMode == ColorMode.DARKAMOLED

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PaletteTopAppBar(
                scrollBehavior,
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding -> 
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(top = innerPadding.calculateTopPadding()), 
                    contentAlignment = Alignment.Center
                ) {
                    ThemePreviewCard(
                        keyColor = currentKeyColor, 
                        isDark = isDark, 
                        isAmoled = amoledMode,
                        isLandscape = true
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.6f) 
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp, 
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    settingsItems(
                        currentColorMode = currentColorMode,
                        currentKeyColor = currentKeyColor,
                        isDark = isDark,
                        hasCustomHeader = hasCustomHeader,
                        prefs = prefs,
                        onColorModeChange = { currentColorMode = it },
                        onKeyColorChange = { currentKeyColor = it },
                        onHasCustomHeaderChange = { hasCustomHeader = it },
                        imagePicker = imagePicker,
                        context = context
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ThemePreviewCard(
                        keyColor = currentKeyColor, 
                        isDark = isDark, 
                        isAmoled = amoledMode,
                        isLandscape = false
                    )
                }

                settingsItems(
                    currentColorMode = currentColorMode,
                    currentKeyColor = currentKeyColor,
                    isDark = isDark,
                    hasCustomHeader = hasCustomHeader,
                    prefs = prefs,
                    onColorModeChange = { currentColorMode = it },
                    onKeyColorChange = { currentKeyColor = it },
                    onHasCustomHeaderChange = { hasCustomHeader = it },
                    imagePicker = imagePicker,
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.settingsItems(
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    isDark: Boolean,
    hasCustomHeader: Boolean,
    prefs: android.content.SharedPreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onHasCustomHeaderChange: (Boolean) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    context: Context
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.accent_color),
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.99f }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.92f to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(modifier = Modifier.width(12.dp))
                
                ColorButton(
                    color = Color.Unspecified,
                    isSelected = currentKeyColor == 0,
                    isDark = isDark,
                    onClick = {
                        onKeyColorChange(0)
                        prefs.edit { putInt("key_color", 0) }
                    }
                )

                keyColorOptions.forEach { colorInt ->
                    ColorButton(
                        color = Color(colorInt),
                        isSelected = currentKeyColor == colorInt,
                        isDark = isDark,
                        onClick = {
                            onKeyColorChange(colorInt)
                            prefs.edit { putInt("key_color", colorInt) }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }

    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.appearance),
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
                                onColorModeChange(mode)
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
                stringResource(R.string.banner),
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
                                onHasCustomHeaderChange(true)
                                imagePicker.launch(arrayOf("image/*"))
                            } else {
                                context.clearHeaderImage()
                                onHasCustomHeaderChange(false)
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
                                    stringResource(R.string.custom)
                                else
                                    stringResource(R.string.default_label)
                            )
                        }
                    }
                }                
            }
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
                    text = stringResource(R.string.theme),
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

@Composable
private fun ThemePreviewCard(keyColor: Int, isDark: Boolean, isAmoled: Boolean, isLandscape: Boolean) { 
    val context = LocalContext.current
    
    val targetColorScheme = when {
        keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            rememberDynamicColorScheme(
                seedColor = Color.Unspecified,
                isDark = isDark,
                isAmoled = isAmoled,
                primary = base.primary,
                secondary = base.secondary,
                tertiary = base.tertiary,
                neutral = base.surface,
                neutralVariant = base.surfaceVariant,
                error = base.error
            )
        }
        keyColor == 0 -> {
            val base = if (isDark) darkColorScheme() else expressiveLightColorScheme()
            if (isAmoled) base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerHigh = Color(0xFF111111),
                surfaceContainerHighest = Color(0xFF1A1A1A)
            ) else base
        }
        else -> rememberDynamicColorScheme(
            seedColor = Color(keyColor), 
            isDark = isDark, 
            isAmoled = isAmoled
        )
    }

    val colorScheme = animateColorSchemeAsState(targetColorScheme)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.55f) 
                .aspectRatio(0.48f), 
            color = colorScheme.surface,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AZenith", 
                        style = MaterialTheme.typography.labelMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {}

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(55.dp),
                        color = colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {}
                    
                    Surface(
                        modifier = Modifier.weight(1f).height(55.dp),
                        color = colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {}
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    color = colorScheme.surfaceColorAtElevation(1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun ColorButton(color: Color, isSelected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    
    val targetColorScheme = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else MaterialTheme.colorScheme
    } else rememberDynamicColorScheme(seedColor = color, isDark = isDark)

    val colorScheme = animateColorSchemeAsState(targetColorScheme)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer, 
        modifier = Modifier.size(72.dp) 
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f, label = "scale")
            
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
