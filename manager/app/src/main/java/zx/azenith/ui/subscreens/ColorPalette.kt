@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.subscreens

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import zx.azenith.R
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.isBannerImageEnabled
import zx.azenith.ui.util.setBannerImageEnabled
import zx.azenith.ui.util.getBannerGradientAlpha
import zx.azenith.ui.util.setBannerGradientAlpha
import android.net.Uri
import com.yalantis.ucrop.UCrop
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import zx.azenith.ui.theme.animateColorSchemeAsState
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import zx.azenith.ui.component.*


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

    // STATE UNTUK BLUR UI
    var isBlurEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("expressive_blur_ui", false))
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    
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
                    snackbarHostState.showSnackbar("Image updated")
                }
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
    var currentColorSpec by remember { 
        mutableStateOf(ThemeController.getAppSettings(context).colorSpec) 
    }

    val isDark = currentColorMode.getDarkThemeValue(isSystemInDarkTheme())
    val amoledMode = currentColorMode == ColorMode.DARKAMOLED

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        colorSpec = currentColorSpec,
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
                        currentColorSpec = currentColorSpec,
                        isDark = isDark,
                        isBannerEnabled = isBannerEnabled,
                        bannerGradientAlpha = bannerGradientAlpha,
                        customBannerUri = customBannerUri,
                        isBlurEnabled = isBlurEnabled, // Teruskan parameter Blur
                        prefs = prefs,
                        onColorModeChange = { currentColorMode = it },
                        onKeyColorChange = { currentKeyColor = it },
                        onColorSpecChange = { currentColorSpec = it },
                        onBannerEnabledChange = { 
                            isBannerEnabled = it 
                            context.setBannerImageEnabled(it)
                        },
                        onBannerGradientAlphaChange = {
                            bannerGradientAlpha = it
                            context.setBannerGradientAlpha(it)
                        },
                        onBannerUpdated = { customBannerUri = it },
                        onBlurEnabledChange = {
                            isBlurEnabled = it
                            prefs.edit { putBoolean("expressive_blur_ui", it) }
                        },
                        imagePicker = imagePicker,
                        context = context,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
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
                        colorSpec = currentColorSpec,
                        isDark = isDark, 
                        isAmoled = amoledMode,
                        isLandscape = false
                    )
                }

                settingsItems(
                    currentColorMode = currentColorMode,
                    currentKeyColor = currentKeyColor,
                    currentColorSpec = currentColorSpec,
                    isDark = isDark,
                    isBannerEnabled = isBannerEnabled,
                    isBlurEnabled = isBlurEnabled,
                    bannerGradientAlpha = bannerGradientAlpha,
                    customBannerUri = customBannerUri,
                    prefs = prefs,
                    onColorModeChange = { currentColorMode = it },
                    onKeyColorChange = { currentKeyColor = it },
                    onColorSpecChange = { currentColorSpec = it },
                    onBannerEnabledChange = { 
                        isBannerEnabled = it 
                        context.setBannerImageEnabled(it)
                    },
                    onBannerGradientAlphaChange = {
                        bannerGradientAlpha = it
                        context.setBannerGradientAlpha(it)
                    },
                    onBlurEnabledChange = {
                        isBlurEnabled = it
                        prefs.edit { putBoolean("expressive_blur_ui", it) }
                    },
                    onBannerUpdated = { customBannerUri = it },
                    imagePicker = imagePicker,
                    context = context,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.settingsItems(
    currentColorMode: ColorMode,
    currentKeyColor: Int,
    currentColorSpec: ColorSpec.SpecVersion,
    isDark: Boolean,
    isBannerEnabled: Boolean,
    bannerGradientAlpha: Float,
    customBannerUri: String?,
    isBlurEnabled: Boolean,
    prefs: android.content.SharedPreferences,
    onColorModeChange: (ColorMode) -> Unit,
    onKeyColorChange: (Int) -> Unit,
    onColorSpecChange: (ColorSpec.SpecVersion) -> Unit,
    onBannerEnabledChange: (Boolean) -> Unit,
    onBannerGradientAlphaChange: (Float) -> Unit,
    onBannerUpdated: (String?) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
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
                        onClick = {
                            onKeyColorChange(0)
                            prefs.edit { putInt("key_color", 0) }
                        }
                    )
                }

                items(keyColorOptions) { colorInt ->
                    ColorButton(
                        color = Color(colorInt),
                        isSelected = currentKeyColor == colorInt,
                        isDark = isDark,
                        colorSpec = currentColorSpec,
                        onClick = {
                            onKeyColorChange(colorInt)
                            prefs.edit { putInt("key_color", colorInt) }
                        }
                    )
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
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            content = buildList {
                
                // --- KARTU 1: Switch & Tombol (Dibuat berdekatan) ---
                add {
                    Column {
                        ExpressiveSwitchItem(
                            icon = Icons.Outlined.Image,
                            title = "Enable Banner Image",
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
                                    // Padding atas dihilangkan agar lebih dekat dengan switch
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) 
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                ) {
                                    OutlinedButton(
                                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pick Image", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            context.clearHeaderImage()
                                            onBannerUpdated(null)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Set to default banner")
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
                                        Text("Default", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                // --- KARTU 2: Adjust Gradient (Dipisah) ---
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
                                    text = "Adjust Gradient",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            BannerGradientPreview(
                                gradientAlpha = bannerGradientAlpha,
                                customBannerUri = customBannerUri
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gradient Opacity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${(bannerGradientAlpha * 100).toInt()}%",
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
                                                contentDescription = "Reset",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Slider(
                                    value = bannerGradientAlpha,
                                    onValueChange = { newValue ->
                                        val snappedValue = if (newValue in 0.47f..0.53f) 0.5f else newValue
                                        onBannerGradientAlphaChange(snappedValue)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
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
                "Interface",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ExpressiveColumn(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            content = buildList {
                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.BlurOn,
                        title = "Expressive Blur",
                        summary = "Enable Frosted glass effect UI",
                        checked = isBlurEnabled,
                        onCheckedChange = onBlurEnabledChange
                    )
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
                "Color Specification",
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
                            if (checked) {
                                onColorSpecChange(spec)
                                prefs.edit { putString("color_spec", spec.name) }
                            }
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
                        Text(spec.name.replace("_", " "), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        LargeFlexibleTopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.theme),
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
private fun BannerGradientPreview(gradientAlpha: Float, customBannerUri: String?) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(20 / 9f)
            .clip(RoundedCornerShape(16.dp)),
        color = colorScheme.surfaceContainerHighest
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = customBannerUri,
                animationSpec = tween(500),
                label = "banner_crossfade"
            ) { uri ->
                if (uri != null) {
                    AsyncImage(
                        model = uri, 
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(), 
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.banner_bg), 
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(), 
                        contentScale = ContentScale.Crop
                    )
                }
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

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp),
                color = colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "Preview", 
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(keyColor: Int, colorSpec: ColorSpec.SpecVersion, isDark: Boolean, isAmoled: Boolean, isLandscape: Boolean) { 
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
private fun ColorButton(color: Color, isSelected: Boolean, isDark: Boolean, colorSpec: ColorSpec.SpecVersion, onClick: () -> Unit) {
    val context = LocalContext.current
    
    val targetColorScheme = if (color == Color.Unspecified) {
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
    } else {
        rememberDynamicColorScheme(
            seedColor = color, 
            isDark = isDark, 
            specVersion = colorSpec
        )
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
