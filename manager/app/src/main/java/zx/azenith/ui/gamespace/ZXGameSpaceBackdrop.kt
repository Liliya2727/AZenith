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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package zx.azenith.ui.gamespace

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.AppIconCache
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.util.getGameWallpaperMode
import zx.azenith.ui.util.getGameWallpaperUri
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.util.getHeaderImage

/**
 * Full-screen atmospheric background that crossfades between the ZXGameSpace contour/symbol wave
 * and a rich game artwork stage with Material 3 tonal scrims.
 */
@Composable
internal fun ZXGameSpaceStageBackdrop(entry: RibbonEntry) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isAmoled = remember { prefs.getInt("color_mode", ColorMode.SYSTEM.value) == ColorMode.DARKAMOLED.value }

    var customWallpaperUri by remember { mutableStateOf(context.getHeaderImage()) }
    var gameWallpaperTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var dimmingIntensity by remember {
        androidx.compose.runtime.mutableIntStateOf(prefs.getInt("gamespace_wallpaper_dimming_intensity", 75))
    }
    var floatingShapesEnabled by remember {
        mutableStateOf(prefs.getBoolean("gamespace_floating_shapes_enabled", true))
    }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "header_image_uri") {
                customWallpaperUri = context.getHeaderImage()
            } else if (key == "gamespace_wallpaper_dimming_intensity") {
                dimmingIntensity = prefs.getInt("gamespace_wallpaper_dimming_intensity", 75)
            } else if (key == "gamespace_floating_shapes_enabled") {
                floatingShapesEnabled = prefs.getBoolean("gamespace_floating_shapes_enabled", true)
            } else if (key == "gamespace_wallpaper_blur" || key == "gamespace_wallpaper_enabled" || key == "enable_banner_image" || key == "gamespace_global_game_wallpaper_mode" || key?.startsWith("game_wallpaper_") == true) {
                gameWallpaperTrigger++
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Crossfade(
        targetState = entry,
        animationSpec = tween(300),
        label = "ZXGameSpaceBackdropCrossfade"
    ) { current ->
        Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceContainerLowest)) {
            val _trigger = gameWallpaperTrigger
            val isHomescreen = current == RibbonEntry.Welcome

            val (isWallpaperEnabled, effectiveDim, effectiveBlur) = when (current) {
                is RibbonEntry.Game -> {
                    val pkg = current.app.packageName
                    val enabled = prefs.getBoolean("game_wallpaper_enabled_$pkg", true)
                    val gameDim = prefs.getInt("game_wallpaper_dim_$pkg", -1)
                    val dim = if (gameDim >= 0) gameDim else dimmingIntensity
                    val blur = prefs.getInt("game_wallpaper_blur_$pkg", 0)
                    Triple(enabled, dim, blur)
                }
                else -> {
                    val enabled = prefs.getBoolean("gamespace_wallpaper_enabled", prefs.getBoolean("enable_banner_image", true))
                    val dim = dimmingIntensity
                    val blur = prefs.getInt("gamespace_wallpaper_blur", 0)
                    Triple(enabled, dim, blur)
                }
            }

            if (isWallpaperEnabled) {
                when (current) {
                    is RibbonEntry.Game -> {
                        val pkg = current.app.packageName
                        val mode = context.getGameWallpaperMode(pkg)
                        val customGameUri = context.getGameWallpaperUri(pkg)
                        val effectiveUri = when (mode) {
                            "follow_home" -> customWallpaperUri
                            "custom" -> customGameUri ?: customWallpaperUri
                            else -> null
                        }

                        if (effectiveUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                    .alpha(if (isAmoled) 0.60f else 0.75f)
                            ) {
                                MediaBannerRenderer(
                                    uriString = effectiveUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (mode == "follow_home") {
                            Image(
                                painter = painterResource(R.drawable.banner_bg),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                    .alpha(if (isAmoled) 0.45f else 0.60f)
                            )
                        } else {
                            val bitmap = AppIconCache.get(current.app.packageName)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.75f)
                                        .align(Alignment.CenterEnd)
                                        .blur(36.dp, BlurredEdgeTreatment.Unbounded)
                                        .alpha(if (isAmoled) 0.50f else 0.65f)
                                )
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(340.dp)
                                        .align(Alignment.CenterEnd)
                                        .offset(x = (-64).dp, y = (-12).dp)
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                        .alpha(if (isAmoled) 0.60f else 0.72f)
                                )
                            } else if (customWallpaperUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                        .alpha(if (isAmoled) 0.60f else 0.75f)
                                ) {
                                    MediaBannerRenderer(
                                        uriString = customWallpaperUri,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.banner_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                        .alpha(if (isAmoled) 0.45f else 0.60f)
                                )
                            }
                        }
                    }

                    else -> {
                        if (customWallpaperUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                    .alpha(if (isAmoled) 0.65f else 0.80f)
                            ) {
                                MediaBannerRenderer(
                                    uriString = customWallpaperUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(R.drawable.banner_bg),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (effectiveBlur > 0) Modifier.blur(effectiveBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                    .alpha(if (isAmoled) 0.48f else 0.62f)
                            )
                            ZXGameSpaceContourCanvas(
                                outlineColor = scheme.outlineVariant
                            )
                        }
                    }
                }

                if (isHomescreen && floatingShapesEnabled) {
                    ZXGameSpaceFloatingShapes(modifier = Modifier.fillMaxSize())
                }

                val baseDimColor = if (isAmoled) Color.Black else scheme.surfaceContainerLowest
                val dimScale = (effectiveDim / 75f).coerceIn(0.15f, 1.45f)

                // Left-to-right dimming gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isHomescreen) {
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.88f else 0.85f) * dimScale).coerceIn(0f, 0.98f)),
                                        0.35f to baseDimColor.copy(alpha = ((if (isAmoled) 0.84f else 0.80f) * dimScale).coerceIn(0f, 0.98f)),
                                        0.68f to baseDimColor.copy(alpha = ((if (isAmoled) 0.44f else 0.38f) * dimScale).coerceIn(0f, 0.98f)),
                                        1.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.22f else 0.18f) * dimScale).coerceIn(0f, 0.98f))
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.78f else 0.75f) * dimScale).coerceIn(0f, 0.98f)),
                                        0.45f to baseDimColor.copy(alpha = ((if (isAmoled) 0.35f else 0.30f) * dimScale).coerceIn(0f, 0.98f)),
                                        1.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.16f else 0.12f) * dimScale).coerceIn(0f, 0.98f))
                                    )
                                )
                            }
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.32f else 0.28f) * dimScale).coerceIn(0f, 0.98f)),
                                    0.45f to Color.Transparent,
                                    1.00f to baseDimColor.copy(alpha = ((if (isAmoled) 0.48f else 0.42f) * dimScale).coerceIn(0f, 0.98f))
                                )
                            )
                        )
                )
            } else {
                if (isHomescreen && floatingShapesEnabled) {
                    ZXGameSpaceFloatingShapes(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Sweeping topographic contour curves across the stage.
 */
@Composable
internal fun ZXGameSpaceContourCanvas(outlineColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (i in 0..4) {
            val shift = i * 26f
            val wave = Path().apply {
                moveTo(w * 0.12f, h * 0.52f + shift)
                cubicTo(
                    w * 0.38f, h * 0.24f + shift,
                    w * 0.64f, h * 0.62f - shift,
                    w * 0.18f + shift, h * 0.18f + shift
                )
            }
            drawPath(
                path = wave,
                color = outlineColor.copy(alpha = 0.16f - i * 0.025f),
                style = Stroke(width = 2.2f)
            )
        }
    }
}

private data class FloatingShapeConfig(
    val initialShape: RoundedPolygon,
    val relX: Float,
    val relY: Float,
    val size: Dp,
    val colorProvider: (ColorScheme) -> Color,
    val floatDurationMs: Int,
    val floatDistDp: Dp,
    val rotDurationMs: Int,
    val staggerDelayMs: Long
)

private val FLOATING_SHAPE_CONFIGS = listOf(
    FloatingShapeConfig(
        initialShape = BentoShapes.Circle,
        relX = 0.70f,
        relY = 0.22f,
        size = 36.dp,
        colorProvider = { it.primary },
        floatDurationMs = 5200,
        floatDistDp = 14.dp,
        rotDurationMs = 7200,
        staggerDelayMs = 600L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.Cookie4,
        relX = 0.85f,
        relY = 0.36f,
        size = 28.dp,
        colorProvider = { it.tertiary },
        floatDurationMs = 6100,
        floatDistDp = 12.dp,
        rotDurationMs = 8400,
        staggerDelayMs = 1800L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.Clover4,
        relX = 0.56f,
        relY = 0.26f,
        size = 32.dp,
        colorProvider = { it.secondary },
        floatDurationMs = 4800,
        floatDistDp = 16.dp,
        rotDurationMs = 6500,
        staggerDelayMs = 3000L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.Sunny,
        relX = 0.75f,
        relY = 0.65f,
        size = 40.dp,
        colorProvider = { it.primary },
        floatDurationMs = 6800,
        floatDistDp = 18.dp,
        rotDurationMs = 9100,
        staggerDelayMs = 4200L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.SoftBurst,
        relX = 0.90f,
        relY = 0.18f,
        size = 24.dp,
        colorProvider = { it.outlineVariant },
        floatDurationMs = 4300,
        floatDistDp = 10.dp,
        rotDurationMs = 5800,
        staggerDelayMs = 5400L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.Puffy,
        relX = 0.44f,
        relY = 0.16f,
        size = 30.dp,
        colorProvider = { it.tertiary },
        floatDurationMs = 5600,
        floatDistDp = 13.dp,
        rotDurationMs = 7600,
        staggerDelayMs = 2400L
    ),
    FloatingShapeConfig(
        initialShape = BentoShapes.Gem,
        relX = 0.62f,
        relY = 0.78f,
        size = 28.dp,
        colorProvider = { it.secondary },
        floatDurationMs = 6400,
        floatDistDp = 15.dp,
        rotDurationMs = 8800,
        staggerDelayMs = 3600L
    )
)

private val FLOATING_MORPH_SHAPES: List<RoundedPolygon> by lazy {
    listOf(
        BentoShapes.Circle,
        BentoShapes.Cookie4,
        BentoShapes.Clover4,
        BentoShapes.Sunny,
        BentoShapes.SoftBurst,
        BentoShapes.Gem,
        BentoShapes.Puffy,
        BentoShapes.Flower,
        BentoShapes.Arch,
        BentoShapes.Pentagon,
        BentoShapes.Cookie9
    )
}

/**
 * Ambient Material 3 shapes floating and morphing across the homescreen stage.
 */
@Composable
internal fun ZXGameSpaceFloatingShapes(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        FLOATING_SHAPE_CONFIGS.forEach { config ->
            val posX = w * config.relX
            val posY = h * config.relY
            FloatingShapeItem(
                config = config,
                color = config.colorProvider(scheme),
                modifier = Modifier.offset(x = posX, y = posY)
            )
        }
    }
}

@Composable
private fun FloatingShapeItem(
    config: FloatingShapeConfig,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FloatingShape_${config.relX}")
    val density = LocalDensity.current
    val floatDistPx = with(density) { config.floatDistDp.toPx() }

    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -floatDistPx,
        targetValue = floatDistPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = config.floatDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    val floatOffsetX by infiniteTransition.animateFloat(
        initialValue = -floatDistPx * 0.65f,
        targetValue = floatDistPx * 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (config.floatDurationMs * 1.35f).roundToInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatX"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = config.rotDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatRot"
    )

    var currentShape by remember { mutableStateOf(config.initialShape) }
    var activeMorph by remember { mutableStateOf(Morph(config.initialShape, config.initialShape)) }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(config.staggerDelayMs)
        while (isActive) {
            val next = FLOATING_MORPH_SHAPES.filter { it !== currentShape }.random()
            activeMorph = Morph(currentShape, next)
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )
            currentShape = next
            val pause = kotlin.random.Random.nextLong(3800L, 8000L)
            delay(pause)
        }
    }

    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = floatOffsetX
                translationY = floatOffsetY
                rotationZ = rotationAngle
            }
            .size(config.size)
            .drawBehind {
                path.rewind()
                val outline = activeMorph.toPath(morphProgress.value.coerceIn(0f, 1f), path)
                matrix.reset()
                matrix.scale(this.size.width, this.size.height)
                outline.transform(matrix)
                drawPath(outline, color = color.copy(alpha = 0.12f))
                drawPath(
                    outline,
                    color = color.copy(alpha = 0.28f),
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
    )
}

/**
 * Dual-tone split badge mirroring the compact platform tag pill.
 */
@Composable
internal fun ZXGameSpaceDualBadge(left: String, right: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.surface,
            modifier = Modifier
                .background(scheme.onSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(
            text = right,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.onSurface,
            modifier = Modifier
                .background(scheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun ZXGameSpacePlatformTag(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = scheme.onSurface,
        contentColor = scheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
