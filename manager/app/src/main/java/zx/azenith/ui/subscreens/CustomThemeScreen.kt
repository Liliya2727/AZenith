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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import zx.azenith.ui.theme.BannerShape
import zx.azenith.ui.theme.withAccentIntensity
import zx.azenith.ui.theme.withContentContrast
import zx.azenith.ui.component.LookAndFeelSection


/**
 * Cache key for the wallpaper-derived swatch, which has no seed colour of its
 * own. Real seeds are ARGB ints, so this cannot collide with one.
 */
private const val DYNAMIC_KEY = 0

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
            .putString(Personalization.PREF_SHAPE, next.shapeScale.ordinal.toString())
            .putString(Personalization.PREF_MOTION, next.motionScale.ordinal.toString())
            .putFloat(Personalization.PREF_TEXT, next.textScale)
            .putFloat(Personalization.PREF_CORNERS, next.cornerBoost)
            .putBoolean(Personalization.PREF_CONTRAST, next.contentContrast)
            .putString(Personalization.PREF_ACCENT, next.accentIntensity.ordinal.toString())
            .putString(Personalization.PREF_BANNER_SHAPE, next.bannerShape.ordinal.toString())
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
    val previewDetail by previewTransition.animateFloat(label = "previewDetail") { target ->
        if (target == PreviewSize.Expanded) 1f else 0f
    }

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
    fun persistTheme(mode: ColorMode, key: Int, spec: ColorSpec.SpecVersion) {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            prefs.edit()
                .putInt("key_color", key)
                .putInt("color_mode", mode.value)
                .putString("color_spec", spec.name)
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
    val onColorSpecChange = { spec: ColorSpec.SpecVersion ->
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
                    ThemePreviewCard(
                        keyColor = currentKeyColor,
                        colorSpec = currentColorSpec,
                        isDark = isDark, 
                        isAmoled = amoledMode,
                        isLandscape = true,
                        isBannerEnabled = isBannerEnabled,
                        gradientAlpha = bannerGradientAlpha,
                        customBannerUri = customBannerUri,
                        personalization = personalization
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
                    detail = previewDetail,
                    expanded = previewExpandedByTap || !listScrolled,
                    // Only the tapped-open state zooms to the banner. At rest the full home
                    // screen is what you want to see, so it stays unzoomed.
                    focusTop = previewExpandedByTap,
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

                LazyColumn(
                    // weight, not fillMaxSize: the header already claims its animated height,
                    // and fillMaxSize would make the list overflow the Column instead of taking
                    // the space that is left -- which left it with no scrollable area at all.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .hazePageSource(),
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.settingsItems(
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    currentColorSpec: ColorSpec.SpecVersion,
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
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
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
    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.str_color_specification),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            val specOptions = ColorSpec.SpecVersion.entries
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                specOptions.forEachIndexed { index, spec ->
                    ToggleButton(
                        checked = currentColorSpec == spec,
                        onCheckedChange = { checked ->
                            if (checked) onColorSpecChange(spec)
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            specOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Text(
                            text = when (spec) {
                                ColorSpec.SpecVersion.SPEC_2021 -> stringResource(R.string.spec_material_you)
                                else -> stringResource(R.string.spec_material_expressive)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.accent_color),
                modifier = Modifier.padding(horizontal = 28.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
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
        }
    }

    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            LookAndFeelSection(
                pers = personalization,
                onPersonalizationChange = onPersonalizationChange
            )
        }
    }

    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.appearance),
                modifier = Modifier.padding(horizontal = 12.dp),
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
    }
    
    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.banner),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
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

                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) 
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
                            }
                        }
                    }
                }
            }
        )
        
        AnimatedVisibility(
            visible = isBannerEnabled,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        ) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = buildList {

                add {
                    AnimatedVisibility(
                        visible = isBannerEnabled,
                        enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LeadingIcon(icon = Icons.Outlined.Gradient)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.str_adjust_gradient),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                                        val snappedValue = if (newValue in 0.47f..0.53f) 0.5f else newValue
                                        onBannerGradientAlphaChange(snappedValue)
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
    
    item {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.str_interface),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
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
            }
        )
    }
}

/** Two sizes the mock preview can be pinned at; [PreviewSize.Expanded] is the readable one. */
private enum class PreviewSize { Expanded, Collapsed }

// Natural size the mock is authored at; everything else scales it to fit.
private val MOCK_W = 190.dp
private val MOCK_H = 396.dp

// How far the mock scales up when the card is tapped to frame the banner. 2.2x fills the
// card width from the mock's top edge, which puts the banner dead centre in the frame.
private const val FOCUS_ZOOM = 2.2f

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
 * The pinned mock preview.
 *
 * Height, corner radius and the card's internal detail all ride the same [previewTransition], so
 * shrinking on scroll and growing on tap are one continuous motion instead of a swap between two
 * layouts. [previewDetail] fades the small internal blocks out as the card shortens, which reads as
 * the mock zooming away rather than being cropped.
 */
@Composable
private fun PinnedPreviewHeader(
    detail: Float,
    expanded: Boolean,
    focusTop: Boolean,
    personalization: Personalization,
    onToggle: () -> Unit,
    keyColor: Int,
    colorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    isAmoled: Boolean,
    isBannerEnabled: Boolean = true,
    gradientAlpha: Float = 1f,
    customBannerUri: String? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        // Collapsed is a square tile; expanded is taller than that square, so the two never
        // read as inverted (maxWidth is wider than any fixed height would be).
        val targetHeight = if (expanded) maxWidth * 1.45f else maxWidth
        val height by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewHeight"
        )
        Box(modifier = Modifier.height(height)) {
            ThemePreviewCard(
                keyColor = keyColor,
                colorSpec = colorSpec,
                isDark = isDark,
                isAmoled = isAmoled,
                isLandscape = false,
                isBannerEnabled = isBannerEnabled,
                gradientAlpha = gradientAlpha,
                customBannerUri = customBannerUri,
                detail = detail,
                focusTop = focusTop,
                personalization = personalization,
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun BannerGradientPreview(
    gradientAlpha: Float,
    customBannerUri: String?,
    isBannerEnabled: Boolean = true,
    bannerShape: BannerShape = BannerShape.Rounded,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bannerRadius = RoundedCornerShape(percent = (bannerShape.radiusFraction * 100).toInt())

    if (!isBannerEnabled) {
        // Mirrors BannerCard's image-off branch: a solid secondaryContainer row with a
        // leading glyph, so the mock shows what Home actually renders with the banner off.
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = colorScheme.secondaryContainer,
            shape = bannerRadius
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
            .clip(bannerRadius),
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

@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    colorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    isAmoled: Boolean,
    isLandscape: Boolean,
    isBannerEnabled: Boolean = true,
    gradientAlpha: Float = 1f,
    customBannerUri: String? = null,
    detail: Float = 1f,
    focusTop: Boolean = false,
    personalization: Personalization = Personalization(),
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val targetColorScheme = if (keyColor == 0) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = isDark,
            isAmoled = isAmoled,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor), 
            isDark = isDark, 
            isAmoled = isAmoled,
            specVersion = colorSpec
        )
    }

    // The mock has to run the same personalization transform as the real theme,
    // otherwise the preview lies about exactly the settings the user is changing.
    val colorScheme = animateColorSchemeAsState(
        targetColorScheme = targetColorScheme
            .withAccentIntensity(personalization.accentIntensity)
            .withContentContrast(personalization.contentContrast)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = (modifier
                // The mock keeps its own aspect ratio and is scaled to fit, rather than
                // deriving height from width -- otherwise collapsing the header changes nothing
                // about the card and it overflows the pinned slot.
                .then(
                    if (isLandscape) Modifier.fillMaxWidth(0.85f)
                    else Modifier.fillMaxSize()
                )
                .let { m -> if (onClick != null) m.clickable { onClick() } else m }),
            color = colorScheme.surface,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            val detailAlpha = detail.coerceIn(0f, 1f)
            // The mock is drawn at its natural 0.48 aspect and scaled to fit the card, so a
            // collapsed (square) card shows the whole mock smaller instead of a stretched crop.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // focusTop frames the banner instead of the whole screen: the mock scales past
                // the card and slides down so the banner -- which lives in the upper half -- is
                // what fills the frame. Only the drawing moves; the card's own bounds do not.
                val zoom by animateFloatAsState(
                    targetValue = if (focusTop) FOCUS_ZOOM else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "previewZoom"
                )
                // Scale by WIDTH and anchor the origin at top-center: a centre-origin scale grows
                // past the card's top edge and silently crops the mock's own header off, which is
                // what made widgets vanish in the expanded state.
                val fit = maxWidth / MOCK_W * zoom
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(MOCK_W)
                        .height(MOCK_H)
                        .graphicsLayer(
                            scaleX = fit,
                            scaleY = fit,
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.app_name), 
                        style = MaterialTheme.typography.labelMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // The banner sits exactly where Home's does: below the app name, filling the width,
                // with the status/pid pills overlaid at the bottom start. When the banner image is
                // off, Home shows a compact secondaryContainer row instead, and so does this.
                BannerGradientPreview(
                    gradientAlpha = gradientAlpha,
                    customBannerUri = customBannerUri,
                    isBannerEnabled = isBannerEnabled,
                    bannerShape = personalization.bannerShape,
                    modifier = Modifier.height(86.dp)
                )

                // The lower mock blocks shrink and fade with `detail` instead of being cropped:
                // at collapsed height the card still reads as the same screen, just zoomed out.
                if (detailAlpha > 0.01f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = detailAlpha },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp * detailAlpha),
                            color = colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {}

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp * detailAlpha),
                            color = colorScheme.surfaceColorAtElevation(1.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {}
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp * detailAlpha)
                            .graphicsLayer { alpha = detailAlpha },
                        color = colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {}
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
    colorSpec: ColorSpec.SpecVersion,
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
                specVersion = colorSpec,
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
                specVersion = colorSpec
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
