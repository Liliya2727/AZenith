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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens


import zx.azenith.ui.navigation.safePopBackStack
import android.app.Activity
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.theme.BANNER_CENTER
import zx.azenith.ui.component.*
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.theme.animateColorSchemeAsState
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getBannerGradientAlpha
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.isBannerImageEnabled
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.saveMediaDirectly
import zx.azenith.ui.util.setBannerGradientAlpha
import zx.azenith.ui.util.setBannerImageEnabled
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.theme.Personalization
import zx.azenith.ui.theme.withAccentIntensity
import zx.azenith.ui.component.LookAndFeelSection
import zx.azenith.ui.theme.ColorEngine
import zx.azenith.ui.theme.NavStyle
import zx.azenith.ui.theme.NavLabelMode
import androidx.compose.ui.graphics.Shape


/**
 * Cache key for the wallpaper-derived swatch, which has no seed colour of its
 * own. Real seeds are ARGB ints, so this cannot collide with one.
 */

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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var isBannerEnabled by rememberSaveable { 
        mutableStateOf(context.isBannerImageEnabled()) 
    }

    var bannerGradientAlpha by rememberSaveable { 
        mutableFloatStateOf(context.getBannerGradientAlpha()) 
    }

    var customBannerUri by remember { 
        mutableStateOf(context.getHeaderImage()) 
    }
    

    var pendingCropUriPath by rememberSaveable { 
        mutableStateOf<String?>(null) 
    }
    

    var isBlurEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("expressive_blur_ui", false))
    }
    
    var useScrollAnimation by rememberSaveable {
        mutableStateOf(prefs.getBoolean("use_scroll_animation", true))
    }

    var personalization by remember { mutableStateOf(Personalization.read(context)) }
    // Shape/text/motion changes are felt instantly across the whole app, so they are
    // written on every drag frame rather than on release: the slider is the preview.
    val persistPersonalization: (Personalization) -> Unit = { next ->
        personalization = next
        prefs.edit()
            .putFloat(Personalization.PREF_TEXT, next.textScale)
            .putFloat(Personalization.PREF_ROUNDNESS, next.roundness)
            .putString(Personalization.PREF_ACCENT, next.accentIntensity.ordinal.toString())
            .putFloat(Personalization.PREF_BANNER_SHAPE, next.bannerRadius)
            .putString(Personalization.PREF_NAV_STYLE, next.navStyle.ordinal.toString())
            .putFloat(Personalization.PREF_NAV_SHAPE, next.navRadius)
            .putString(Personalization.PREF_NAV_LABELS, next.navLabels.ordinal.toString())
            .apply()
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // The mock preview is pinned above the list instead of scrolling away in it: expanded at
    // rest, shrunk once the list has moved, and re-expanded on tap for a closer look.
    // Collapsing keys off isScrollInProgress rather than the scroll offset because the header's
    // own height change reflows the list and would otherwise re-trigger itself immediately.
    val lazyListState = rememberLazyListState()
    var previewExpandedByTap by rememberSaveable { mutableStateOf(false) }
    val listScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                lazyListState.firstVisibleItemScrollOffset > 8
        }
    }
    val listSettling by remember { derivedStateOf { lazyListState.isScrollInProgress } }
    LaunchedEffect(listSettling) {
        if (listSettling) previewExpandedByTap = false
    }

    val previewTransition = updateTransition(
        targetState = if (previewExpandedByTap || !listScrolled) PreviewSize.Expanded else PreviewSize.Collapsed,
        label = "previewSize"
    )
    // One transition drives both the header height and the card's internal detail, so the
    // collapse reads as the same object getting smaller rather than two separate animations.


    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                val uriString = it.toString()
                context.saveHeaderImage(uriString)
                customBannerUri = uriString
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.str_banner_updated))
                }
            }
        } else {
            pendingCropUriPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        pendingCropUriPath = null
    }
    
    val colorScheme = MaterialTheme.colorScheme 
    
    
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            val isGif = mimeType == "image/gif"
            val isVideoOrGif = isVideo || isGif
    

            var sizeBytes = 0L
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
            val maxSize = 50 * 1024 * 1024L
            if (sizeBytes > maxSize) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.str_max_video_size))
                }
                return@let
            }
    

            if (isVideo) {
                var durationMs = 0L
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, sourceUri)
                    val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = timeString?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
    
                if (durationMs > 30000L) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.str_video_too_long))
                    }
                    return@let
                }
            }
    

            if (isVideoOrGif) {

                coroutineScope.launch {
                    val extension = if (isVideo) "mp4" else "gif"
                    val savedUriString = context.saveMediaDirectly(sourceUri, extension)
                    
                    if (savedUriString != null) {
                        context.saveHeaderImage(savedUriString)
                        customBannerUri = savedUriString
                        snackbarHostState.showSnackbar(context.getString(R.string.str_pick_media_success))
                    } else {
                        snackbarHostState.showSnackbar(context.getString(R.string.str_pick_media_fail))
                    }
                }
            } else {

                val bannerDir = File(context.filesDir, "banners")
                if (!bannerDir.exists()) bannerDir.mkdirs()
                
                val destinationFile = File(bannerDir, "banner_${System.currentTimeMillis()}.jpg")
                pendingCropUriPath = destinationFile.absolutePath 
                val destinationUri = Uri.fromFile(destinationFile)
                
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
    }
    
    
    // Theme edits are staged here and only written to SharedPreferences when
    // the user saves, so a choice can be previewed and then backed out of.
    val savedSettings = remember { ThemeController.getAppSettings(context) }
    var currentColorMode by remember { mutableStateOf(savedSettings.colorMode) }
    var currentKeyColor by remember { mutableIntStateOf(savedSettings.keyColor) }
    var currentColorSpec by remember { mutableStateOf(savedSettings.colorSpec) }

    // One scheme cache for every swatch on the screen, so the accent row derives
    // each scheme once for the whole screen rather than once per swatch. It is
    // keyed on the spec, so changing the spec drops every entry.
    val swatchSchemeCache = remember(currentColorSpec) {
        mutableStateMapOf<Int, ColorScheme>()
    }

    // The save bar is gone, so the theme writes itself as each choice is made, the same
    // way the banner/blur/scroll toggles below already do.
    fun persistTheme(mode: ColorMode, key: Int, spec: ColorEngine) {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            prefs.edit()
                .putInt("key_color", key)
                .putInt("color_mode", mode.value)
                .putString("color_spec", spec.persistedName)
                .commit()
        }
    }

    val onColorModeChange = { mode: ColorMode ->
        currentColorMode = mode
        persistTheme(mode, currentKeyColor, currentColorSpec)
    }
    val onKeyColorChange = { key: Int ->
        currentKeyColor = key
        persistTheme(currentColorMode, key, currentColorSpec)
    }
    val onColorSpecChange = { spec: ColorEngine ->
        currentColorSpec = spec
        persistTheme(currentColorMode, currentKeyColor, spec)
    }

    val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
    val amoledMode = currentColorMode == ColorMode.DARKAMOLED


    Scaffold(
        modifier = Modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PaletteTopAppBar(
                onBack = { navController.safePopBackStack() },
            scrollBehavior = scrollBehavior
            )
        },
        contentColor = ScaffoldContentColor(),
        containerColor = ScaffoldContainerColor(MaterialTheme.colorScheme.surface)
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
MockCardFrame(
                        scheme = mockColorScheme(
                            keyColor = currentKeyColor,
                            colorSpec = currentColorSpec,
                            isDark = isDark,
                            isAmoled = amoledMode,
                            personalization = personalization
                        ),
                        shape = personalization.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        MockScreen(
                            scheme = MaterialTheme.colorScheme,
                            personalization = personalization,
                            isBannerEnabled = isBannerEnabled,
                            isBlurEnabled = isBlurEnabled,
                            gradientAlpha = bannerGradientAlpha,
                            customBannerUri = customBannerUri
                        )
                    }
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
                        currentColorSpec = currentColorSpec,
                        swatchSchemeCache = swatchSchemeCache,
                        isDark = isDark,
                        isBannerEnabled = isBannerEnabled,
                        bannerGradientAlpha = bannerGradientAlpha,
                        customBannerUri = customBannerUri,
                        isBlurEnabled = isBlurEnabled,
                        isLandscape = true,
                        prefs = prefs,
                        onColorModeChange = onColorModeChange,
                        onKeyColorChange = onKeyColorChange,
                        onColorSpecChange = onColorSpecChange,
                        onBannerEnabledChange = { 
                            isBannerEnabled = it 
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { context.setBannerImageEnabled(it) }
                        },
                        onBannerGradientAlphaChange = {
                            bannerGradientAlpha = it
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { context.setBannerGradientAlpha(it) }
                        },
                        onBannerUpdated = { customBannerUri = it },
                        onBlurEnabledChange = {
                            isBlurEnabled = it
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs.edit().putBoolean("expressive_blur_ui", it).commit() }
                        },
                        useScrollAnimation = useScrollAnimation,
                        onUseScrollAnimationChange = {
                            useScrollAnimation = it
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs.edit().putBoolean("use_scroll_animation", it).commit() }
                        },
                        imagePicker = imagePicker,
                        personalization = personalization,
                        onPersonalizationChange = { next ->
                            personalization = next
                            persistPersonalization(next)
                        },
                        context = context,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
                    )
                }
            }
        } else {
            // The preview lives outside the list, so it stays put while the settings scroll
            // underneath it; the list's own offset is what collapses it.
            Column(modifier = Modifier.fillMaxSize()) {
                PinnedPreviewHeader(
                    expanded = previewExpandedByTap || !listScrolled,
                    // Only the tapped-open state zooms to the banner. At rest the full home
                    // screen is what you want to see, so it stays unzoomed.
                    personalization = personalization,
                    onToggle = { previewExpandedByTap = !previewExpandedByTap },
                    keyColor = currentKeyColor,
                    colorSpec = currentColorSpec,
                    isDark = isDark,
                    isAmoled = amoledMode,
                    isBannerEnabled = isBannerEnabled,
                    gradientAlpha = bannerGradientAlpha,
                    customBannerUri = customBannerUri,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SheetHandle(onClick = { previewExpandedByTap = !previewExpandedByTap })
        
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .hazePageSource(),
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            settingsItems(
                            currentColorMode = currentColorMode,
                            currentKeyColor = currentKeyColor,
                            currentColorSpec = currentColorSpec,
                            swatchSchemeCache = swatchSchemeCache,
                            isDark = isDark,
                            isBannerEnabled = isBannerEnabled,
                            isBlurEnabled = isBlurEnabled,
                            bannerGradientAlpha = bannerGradientAlpha,
                            customBannerUri = customBannerUri,
                            isLandscape = false,
                            prefs = prefs,
                            onColorModeChange = onColorModeChange,
                            onKeyColorChange = onKeyColorChange,
                            onColorSpecChange = onColorSpecChange,
                            onBannerEnabledChange = { 
                                isBannerEnabled = it 
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { context.setBannerImageEnabled(it) }
                            },
                            onBannerGradientAlphaChange = {
                                bannerGradientAlpha = it
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { context.setBannerGradientAlpha(it) }
                            },
                            onBlurEnabledChange = {
                                isBlurEnabled = it
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs.edit().putBoolean("expressive_blur_ui", it).commit() }
                            },
                            useScrollAnimation = useScrollAnimation,
                            onUseScrollAnimationChange = {
                                useScrollAnimation = it
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { prefs.edit().putBoolean("use_scroll_animation", it).commit() }
                            },
                            onBannerUpdated = { customBannerUri = it },
                            imagePicker = imagePicker,
                            personalization = personalization,
                            onPersonalizationChange = { next ->
                                personalization = next
                                persistPersonalization(next)
                            },
                            context = context,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Which string names each [ColorEngine] option; the enum itself stays display-free. */
private fun specLabelRes(spec: ColorEngine): Int = when (spec) {
    ColorEngine.MaterialYou -> R.string.spec_material_you
    ColorEngine.MaterialExpressive -> R.string.spec_material_expressive

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.settingsItems(
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    currentColorSpec: ColorEngine,
    swatchSchemeCache: SnapshotStateMap<Int, ColorScheme>,
    isDark: Boolean,
    isBannerEnabled: Boolean,
    bannerGradientAlpha: Float,
    customBannerUri: String?,
    useScrollAnimation: Boolean,
    onUseScrollAnimationChange: (Boolean) -> Unit,
    isBlurEnabled: Boolean,
    isLandscape: Boolean,
    prefs: android.content.SharedPreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onColorSpecChange: (ColorEngine) -> Unit,
    onBannerEnabledChange: (Boolean) -> Unit,
    onBannerGradientAlphaChange: (Float) -> Unit,
    onBannerUpdated: (String?) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    personalization: Personalization,
    onPersonalizationChange: (Personalization) -> Unit,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Everything that decides *which colours* exist sits together: the spec the
    // palette is generated from, and the accent picked out of it. Intensity and
    // contrast live further down, in the Colour group inside LookAndFeelSection.
    item {
        Column {
            LookAndFeelSection(
                pers = personalization,
                onPersonalizationChange = onPersonalizationChange,
                // Colour controls live in the section's Colour group so the spec
                // picker, the accent swatches, accent intensity and contrast read as
                // one cluster instead of being split by the section boundary.
                colorSpecContent = {
                    LabeledControl(
                        Icons.Filled.Palette,
                        stringResource(R.string.str_color_specification)
                    )
                    ConnectedToggleRow(
                        options = ColorEngine.entries,
                        selected = currentColorSpec,
                        label = { stringResource(specLabelRes(it)) },
                        onSelect = onColorSpecChange,
                        // Kept on the older elevation-derived surface rather than the
                        // shared default: this row sits in its own group.
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                },
                accentSwatchContent = {
                    LabeledControl(Icons.Filled.Palette, stringResource(R.string.accent_color))
                    LazyRow(
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
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            ColorButton(
                                color = Color.Unspecified,
                                isSelected = currentKeyColor == 0,
                                isDark = isDark,
                                colorSpec = currentColorSpec,
                                schemeCache = swatchSchemeCache,
                                onClick = { onKeyColorChange(0) }
                            )
                        }

                        items(keyColorOptions) { colorInt ->
                            ColorButton(
                                color = Color(colorInt),
                                isSelected = currentKeyColor == colorInt,
                                isDark = isDark,
                                colorSpec = currentColorSpec,
                                schemeCache = swatchSchemeCache,
                                onClick = { onKeyColorChange(colorInt) }
                            )
                        }
                    }
                },
                colorModeContent = {
                    LabeledControl(Icons.Filled.Brightness4, stringResource(R.string.appearance))
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
                                    if (checked) onColorModeChange(mode)
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                )
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
            )
        }
    }

    item {
        SettingsGroup(titleRes = R.string.banner) {
            ExpressiveColumn(
                content = buildList {
                    add {
                        Column {
                            ExpressiveSwitchItem(
                                icon = Icons.Outlined.Image,
                                title = stringResource(R.string.str_enable_banner),
                                checked = isBannerEnabled,
                                onCheckedChange = onBannerEnabledChange
                            )
    
                            AnimatedVisibility(
                                visible = isBannerEnabled,
                                enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                                exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                    ) {
                                        OutlinedButton(
                                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.str_pick_media), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
    
                                        OutlinedButton(
                                            onClick = {
                                                context.clearHeaderImage()
                                                onBannerUpdated(null)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.str_default_banner_toast))
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 50.dp, bottomEnd = 50.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.default_label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LeadingIcon(icon = Icons.Outlined.Gradient)
                                        Spacer(Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(R.string.str_adjust_gradient),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
    
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.str_gradient_opacity),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.str_bannergradientalpha_100_toint, (bannerGradientAlpha * 100).toInt()),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = { onBannerGradientAlphaChange(0.5f) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Restore,
                                                        contentDescription = stringResource(R.string.reset),
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        ZenithSlider(
                                            value = bannerGradientAlpha,
                                            onValueChange = { newValue ->
                                                val snapped = if (newValue in 0.47f..0.53f) 0.5f else newValue
                                                onBannerGradientAlphaChange(snapped)
                                            },
                                            onValueChangeFinished = {},
                                            valueRange = 0f..1f,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LeadingIcon(icon = Icons.Filled.CropOriginal)
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.pers_banner_shape),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cornerLabel(personalization.bannerRadius, BANNER_CENTER),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { onPersonalizationChange(personalization.copy(bannerRadius = BANNER_CENTER)) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Restore,
                                                contentDescription = stringResource(R.string.reset),
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    ZenithSlider(
                                        value = personalization.bannerRadius,
                                        onValueChange = { newValue ->
                                            val snapped = if (kotlin.math.abs(newValue - BANNER_CENTER) < 0.03f) BANNER_CENTER else newValue
                                            onPersonalizationChange(personalization.copy(bannerRadius = snapped))
                                        },
                                        onValueChangeFinished = {},
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
    
    item {
        SettingsGroup(titleRes = R.string.str_interface) {
            ExpressiveColumn(
                content = buildList {
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.BlurOn,
                            title = stringResource(R.string.str_expressive_blur),
                            summary = stringResource(R.string.str_expressive_blur_summary),
                            checked = isBlurEnabled,
                            onCheckedChange = onBlurEnabledChange
                        )
                    }
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.SwipeRight,
                            title = stringResource(R.string.str_use_scroll_animation),
                            summary = stringResource(R.string.str_use_scroll_animation_summary),
                            checked = useScrollAnimation,
                            onCheckedChange = onUseScrollAnimationChange
                        )
                    }
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.ViewAgenda,
                            title = stringResource(R.string.pers_nav_floating),
                            summary = stringResource(
                                if (personalization.navStyle == NavStyle.Floating) R.string.pers_nav_floating_on
                                else R.string.pers_nav_floating_off
                            ),
                            checked = personalization.navStyle == NavStyle.Floating,
                            onCheckedChange = { floating ->
                                onPersonalizationChange(
                                    personalization.copy(
                                        navStyle = if (floating) NavStyle.Floating else NavStyle.Pinned
                                    )
                                )
                            }
                        )
                    }
                }
            )
        }
    }
}

/** Two sizes the mock preview can be pinned at; [PreviewSize.Expanded] is the readable one. */
private enum class PreviewSize { Expanded, Collapsed }

// Natural size the mock is authored at; everything else scales it to fit.
private val MOCK_NAV_LABELS = intArrayOf(
    R.string.nav_home, R.string.nav_applist, R.string.nav_tweaks, R.string.nav_settings
)
private val MOCK_W = 190.dp
private val MOCK_H = 396.dp

private const val DYNAMIC_KEY = 0

// Radius the mock dialog's scrim applies to the content behind it. Big enough to
// visibly smear the banner text, small enough that the mock stays readable.
private val MOCK_DIALOG_BLUR = 8.dp

// How far the mock scales up when the card is tapped to frame the banner. 2.2x fills the
// card width from the mock's top edge, which puts the banner dead centre in the frame.

/** Drag-handle pill di atas sheet settings, gaya Instagram comment sheet. */
@Composable
private fun SheetHandle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun PaletteTopAppBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appBarFade(colorScheme.surface, scrollBehavior.state.overlappedFraction)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.personalization_title),
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
            
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}

/**
 * The mock preview.
 *
 * One transition drives the layout: collapsed, the mock is cut into two halves
 * that sit side by side -- so the navbar and the banner are each shown at a
 * readable size instead of a squeezed portrait phone; expanded, the halves join
 * back into the whole portrait screen.
 */
@Composable
private fun PinnedPreviewHeader(
    expanded: Boolean,
    personalization: Personalization,
    onToggle: () -> Unit,
    keyColor: Int,
    colorSpec: ColorEngine,
    isDark: Boolean,
    isAmoled: Boolean,
    isBannerEnabled: Boolean = true,
    isBlurEnabled: Boolean = false,
    gradientAlpha: Float = 1f,
    customBannerUri: String? = null,
    modifier: Modifier = Modifier
) {
    val scheme = mockColorScheme(
        keyColor = keyColor,
        colorSpec = colorSpec,
        isDark = isDark,
        isAmoled = isAmoled,
        personalization = personalization
    )

    BoxWithConstraints(modifier = modifier) {
        // The expanded card is the tall portrait phone; the collapsed row is only
        // as tall as the two half-screens need, which is what leaves room for the
        // list underneath instead of an empty band.
        // Card aspect follows the mock's own 190:396 ratio once collapsed, so the
        // uniform scale fills both axes instead of leaving dead margins on one side.
        // Expanded: the tall portrait phone. Collapsed: the same phone shrunk, so the
        // card must scale DOWN with the frame -- deriving height from maxWidth alone
        // made the collapsed card taller than the expanded one.
        // Two measured heights, not the mock's aspect: MockScreen already scales to
        // fit, so deriving height from 190:396 either overshot the viewport or left a
        // dead band. Clamping to maxHeight made the card full-screen.
        val mockAspect = MOCK_H / MOCK_W
        // Collapsed: height is DERIVED from the width so the frame keeps the mock's
        // 190:396 ratio exactly. Setting them independently made MockScreen's minOf
        // go height-limited, which shrank the widgets and opened a dead margin.
        val targetHeight = if (expanded) maxWidth * 1.45f else maxWidth * 0.40f * mockAspect
        val height by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewHeight"
        )
        val shape = personalization.shapes.extraLarge
        val fullWidth = maxWidth
        val targetWidth = if (expanded) fullWidth else fullWidth * 0.40f
        val frameWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewWidth"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 16.dp)
        ) {
            // A fixed-dp radius reads rounder once the frame is smaller, because the
            // corner takes up more of the box. Scale it with the frame so the same
            // shape reads the same at any size.
            val cornerScale = frameWidth / fullWidth
            val base = 28.dp * cornerScale * personalization.cornerMultiplier
            val scaledShape = RoundedCornerShape(base)
            MockCardFrame(
                scheme = scheme,
                shape = scaledShape,
                // The tap target is the frame itself: swiping that starts on the card's
                // dead margins still reaches the list, while a tap on the mock expands it.
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(frameWidth)
                    .fillMaxHeight()
                    .clickable(onClick = onToggle)
            ) {
                MockScreen(
                    scheme = scheme,
                    personalization = personalization,
                    isBannerEnabled = isBannerEnabled,
                    isBlurEnabled = isBlurEnabled,
                    gradientAlpha = gradientAlpha,
                    customBannerUri = customBannerUri,
                    isCollapsed = !expanded
                )
            }
        }
    }
}

/** The mock's own device frame: surface, shape ramp and hairline border. */
@Composable
private fun MockCardFrame(
    scheme: ColorScheme,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = scheme.surface,
        // The frame follows the shape ramp, otherwise the corner controls change
        // nothing the user can see.
        shape = shape,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 8.dp
    ) { content() }
}


/**
 * The mock's screen contents, drawn at a fixed [MOCK_W] x [MOCK_H] and scaled by
 * the caller. Everything here comes from the same [Personalization] state as the
 * real UI, so the preview never lies about what a setting does.
 */
@Composable
private fun MockScreen(
    scheme: ColorScheme,
    personalization: Personalization,
    isBannerEnabled: Boolean,
    isBlurEnabled: Boolean,
    gradientAlpha: Float,
    customBannerUri: String?,
    isCollapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        // Fit both axes: scaling by width alone overflowed the slot vertically and
        // cropped the bottom rows -- the navbar -- away.
        val fit = minOf(maxWidth / MOCK_W, maxHeight / MOCK_H)
        // Clamp to half the widget's own height: past that a rectangle IS a circle.
        val k = personalization.cornerMultiplier
        val mediumShape = RoundedCornerShape((12.dp * k).coerceAtMost(27.5.dp))
        val largeShape = RoundedCornerShape((16.dp * k).coerceAtMost(52.dp))
        // Real blur on the content the mock dialog covers, so the toggle changes
        // something measurable instead of tinting a rectangle. Animated so the
        // reveal reads as the dialog arriving rather than a setting flipping.
        val blurRadius by animateDpAsState(
            targetValue = if (isBlurEnabled) MOCK_DIALOG_BLUR else 0.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "mockDialogBlur"
        )
        // The blur demo needs a shared parent for the content and the dialog that
        // covers it, so the two move together under the mock's scaling transform.
        // Nothing about the content itself changes; only the box it sits in.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(MOCK_W)
                .height(MOCK_H)
                .graphicsLayer(
                    scaleX = fit,
                    scaleY = fit,
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .padding(horizontal = 8.dp, vertical = if (isCollapsed) 2.dp else 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // The banner sits exactly where Home's does: below the app name,
            // filling the width, with the status/pid pills at the bottom start.
            BannerGradientPreview(
                gradientAlpha = gradientAlpha,
                customBannerUri = customBannerUri,
                isBannerEnabled = isBannerEnabled,
                bannerRadius = personalization.bannerRadius,
                modifier = Modifier.height(76.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(55.dp),
                    color = scheme.secondaryContainer,
                    shape = mediumShape
                ) {}

                Surface(
                    modifier = Modifier.weight(1f).height(55.dp),
                    color = scheme.surfaceColorAtElevation(1.dp),
                    shape = mediumShape
                ) {}
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = scheme.surfaceColorAtElevation(1.dp),
                shape = largeShape
            ) {}

            NavBarMock(
                navStyle = personalization.navStyle,
                navShape = personalization.navRadius,
                labelMode = personalization.navLabels,
                isBlurEnabled = isBlurEnabled,
                colorScheme = scheme
            )
        }

            MockBlurDialog(
                visible = isBlurEnabled,
                cornerScale = k,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun BannerGradientPreview(
    gradientAlpha: Float,
    customBannerUri: String?,
    isBannerEnabled: Boolean = true,
    bannerRadius: Float = 0.12f,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bannerShape = RoundedCornerShape(percent = (bannerRadius * 100).toInt())

    if (!isBannerEnabled) {
        // Mirrors BannerCard's image-off branch: a solid secondaryContainer row with a
        // leading glyph, so the mock shows what Home actually renders with the banner off.
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colorScheme.secondaryContainer,
            shape = bannerShape
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
                )
                Text(
                    text = stringResource(R.string.status_alive),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(bannerShape),
        color = colorScheme.surfaceContainerHighest
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = customBannerUri,
                animationSpec = tween(500),
                label = "banner_crossfade"
            ) { uri ->
                MediaBannerRenderer(
                    uriString = uri,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colorScheme.surfaceContainerLow.copy(alpha = gradientAlpha))
                        )
                    )
            )

            // The status/pid pills overlay the banner's bottom start, as they do on Home.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(R.string.status_alive),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSecondaryContainer
                    )
                }
                Surface(
                    color = colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(R.string.pid_format, "0"),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * The scheme the mock draws with, including the same personalization transforms the
 * real theme applies, so the preview cannot disagree with what ships.
 */
@Composable
private fun mockColorScheme(
    keyColor: Int,
    colorSpec: ColorEngine,
    isDark: Boolean,
    isAmoled: Boolean,
    personalization: Personalization
): ColorScheme {
    val context = LocalContext.current
    val target = if (keyColor == 0) {
        val base = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else -> if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = base.primary,
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec.librarySpec,
            primary = base.primary,
            secondary = base.secondary,
            tertiary = base.tertiary,
            neutral = base.surface,
            neutralVariant = base.surfaceVariant,
            error = base.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor),
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec.librarySpec
        )
    }
    return animateColorSchemeAsState(
        target
            .withAccentIntensity(personalization.accentIntensity)
    )
}

/**
 * The mock's navigation bar, drawn from the same [Personalization] state as the real
 * one so the preview never lies about the bar the user is configuring. A pinned bar
 * has no corners and no pill background; a floating one follows [navShape], and the
 * blur control shows through as a translucent surface exactly as it does on device.
 */
@Composable
private fun NavBarMock(
    navStyle: NavStyle,
    navShape: Float,
    labelMode: NavLabelMode,
    isBlurEnabled: Boolean,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    val pinned = navStyle == NavStyle.Pinned
    val accent = colorScheme.primary
    val muted = colorScheme.onSurfaceVariant
    val radius = if (pinned) 0.dp else (14.dp * (navShape / 0.5f).coerceIn(0f, 2f)).coerceAtMost(8.dp)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (pinned) Alignment.Center else Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .then(if (pinned) Modifier.fillMaxWidth() else Modifier.width(150.dp))
                .clip(RoundedCornerShape(radius)),
            // Blur is a translucency here: the mock has nothing behind it to blur,
            // so it shows the wash that Haze produces rather than faking a blur.
            color = when {
                pinned -> colorScheme.surfaceContainerHigh
                isBlurEnabled -> colorScheme.surfaceContainer.copy(alpha = 0.4f)
                else -> colorScheme.surfaceContainer
            },
            border = if (isBlurEnabled && !pinned)
                BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (pinned) 8.dp else 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(if (pinned) 0.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val selected = index == 1
                    // SelectedOnly is the shipped behaviour, so the mock shows the
                    // label on one tab only and the icons on the rest.
                    val showLabel = when (labelMode) {
                        NavLabelMode.Always -> true
                        NavLabelMode.SelectedOnly -> selected
                        NavLabelMode.Never -> false
                    }
                    Row(
                        modifier = Modifier
                            .then(
                                if (selected && !pinned) Modifier
                                    .background(accent, RoundedCornerShape(percent = 50))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                else Modifier
                            )
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    // A pinned bar carries selection with tint alone.
                                    if (selected) accent
                                    else muted.copy(alpha = if (pinned) 1f else 0.55f)
                                )
                        )
                        if (showLabel) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = stringResource(MOCK_NAV_LABELS[index]),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected && !pinned) colorScheme.onPrimary else muted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stands in for the profile picker dialog, the one place the app actually blurs.
 *
 * Haze needs real pixels behind it to sample, and the mock has none of its own,
 * so the blur is applied to the mock content behind this card instead of to the
 * card. Toggling the switch therefore scales, fades and blurs the same content
 * the real dialog blurs, which is what makes the setting legible from the
 * Personalization page alone.
 */




/**
 * Stands in for the profile picker dialog, the one place the app actually blurs.
 *
 * Haze needs real pixels behind it to sample and the mock has none of its own, so
 * the blur is applied to the mock content behind this card rather than to the
 * card. Toggling the switch therefore scales, fades and blurs the same content
 * the real dialog blurs, which is what makes the setting legible from the
 * Personalization page alone.
 */
@Composable
private fun MockBlurDialog(
    visible: Boolean,
    cornerScale: Float,
    modifier: Modifier = Modifier
) {
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.32f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mockScrim"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mockCard"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "mockCardScale"
    )
    if (scrimAlpha == 0f && cardAlpha == 0f) return

    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.78f)
                .alpha(cardAlpha)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                },
            // Translucent like the real dialog's blurred container: opaque would
            // hide the blur the toggle is meant to demonstrate.
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
            shape = RoundedCornerShape((20.dp * cornerScale).coerceAtMost(40.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(width = 74.dp, height = 8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                repeat(3) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (index == 1) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 1) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    colorSpec: ColorEngine,
    schemeCache: SnapshotStateMap<Int, ColorScheme>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Each swatch previews the scheme that choosing it would produce, so the
    // arc colours come from that preview scheme and not from the app theme,
    // which still holds the previously committed accent until Save.
    //
    // Deriving a scheme is a synchronous HCT quantisation. Doing it inline meant
    // every visible swatch recomputed one on each recomposition, which is what
    // made opening this screen and scrolling the swatch row stutter. The cache
    // is keyed on the seed and the spec, so a scheme is derived once and every
    // later composition of the same swatch is a map lookup.
    val targetColorScheme = if (color == Color.Unspecified) {
        schemeCache.getOrPut(DYNAMIC_KEY) {
            val baseScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                else ->
                    if (isDark) darkColorScheme() else expressiveLightColorScheme()
            }
            rememberDynamicColorScheme(
                seedColor = baseScheme.primary,
                isDark = isDark,
                specVersion = colorSpec.librarySpec,
                primary = baseScheme.primary,
                secondary = baseScheme.secondary,
                tertiary = baseScheme.tertiary,
                neutral = baseScheme.surface,
                neutralVariant = baseScheme.surfaceVariant,
                error = baseScheme.error
            )
        }
    } else {
        schemeCache.getOrPut(color.toArgb()) {
            rememberDynamicColorScheme(
                seedColor = color,
                isDark = isDark,
                specVersion = colorSpec.librarySpec
            )
        }
    }

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
