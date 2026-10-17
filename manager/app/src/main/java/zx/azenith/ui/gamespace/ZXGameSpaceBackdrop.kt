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

package zx.azenith.ui.gamespace

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zx.azenith.R
import zx.azenith.ui.component.AppIconCache
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.util.getHeaderImage

/**
 * Full-screen atmospheric background that crossfades between the ZXGameSpace contour/symbol wave
 * and a rich game artwork stage with Material 3 tonal scrims.
 */
@Composable
internal fun ZXGameSpaceStageBackdrop(entry: RibbonEntry) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isAmoled = scheme.surfaceContainerLowest == Color.Black

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var customWallpaperUri by remember { mutableStateOf(context.getHeaderImage()) }
    var dimmingIntensity by remember {
        androidx.compose.runtime.mutableIntStateOf(prefs.getInt("gamespace_wallpaper_dimming_intensity", 75))
    }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "header_image_uri") {
                customWallpaperUri = context.getHeaderImage()
            } else if (key == "gamespace_wallpaper_dimming_intensity") {
                dimmingIntensity = prefs.getInt("gamespace_wallpaper_dimming_intensity", 75)
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
            when (current) {
                is RibbonEntry.Game -> {
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
                                .alpha(if (isAmoled) 0.60f else 0.72f)
                        )
                    } else if (customWallpaperUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                .alpha(if (isAmoled) 0.45f else 0.60f)
                        )
                    }
                }

                else -> {
                    if (customWallpaperUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                .alpha(if (isAmoled) 0.48f else 0.62f)
                        )
                        ZXGameSpaceContourGlyphCanvas(
                            primaryColor = scheme.primary,
                            tertiaryColor = scheme.tertiary,
                            outlineColor = scheme.outlineVariant
                        )
                    }
                }
            }

            val baseDimColor = if (isAmoled) Color.Black else scheme.surfaceContainerLowest
            val isHomescreen = current == RibbonEntry.Welcome
            val dimScale = (dimmingIntensity / 75f).coerceIn(0.15f, 1.45f)

            // Left-to-right dimming gradient: on homescreen, the far left 35% is more opaque
            // for navigation contrast, smoothly transitioning towards the right to subtle transparency (not fully transparent).
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
        }
    }
}

/**
 * Draws the abstract sweeping contour ribbons and translucent controller symbol geometry seen in the
 * upper-right quadrant of the ZXGameSpace Welcome Hub.
 */
@Composable
internal fun ZXGameSpaceContourGlyphCanvas(
    primaryColor: Color,
    tertiaryColor: Color,
    outlineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sweeping topographic contour curves across the stage.
        for (i in 0..4) {
            val shift = i * 26f
            val wave = Path().apply {
                moveTo(w * 0.12f, h * 0.52f + shift)
                cubicTo(
                    w * 0.38f, h * 0.24f + shift,
                    w * 0.64f, h * 0.62f - shift,
                    w * 0.98f, h * 0.18f + shift
                )
            }
            drawPath(
                path = wave,
                color = outlineColor.copy(alpha = 0.16f - i * 0.025f),
                style = Stroke(width = 2.2f)
            )
        }

        // Translucent geometric controller symbols in the upper-right quadrant.
        val cx = w * 0.72f
        val cy = h * 0.34f
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)

        // Circle glyph
        drawCircle(
            color = primaryColor.copy(alpha = 0.14f),
            radius = 38f,
            center = androidx.compose.ui.geometry.Offset(cx - 56f, cy - 32f),
            style = stroke
        )

        // Triangle glyph
        val triPath = Path().apply {
            moveTo(cx + 62f, cy - 64f)
            lineTo(cx + 94f, cy - 10f)
            lineTo(cx + 30f, cy - 10f)
            close()
        }
        drawPath(
            path = triPath,
            color = tertiaryColor.copy(alpha = 0.14f),
            style = stroke
        )

        // Square glyph
        drawRoundRect(
            color = outlineColor.copy(alpha = 0.12f),
            topLeft = androidx.compose.ui.geometry.Offset(cx + 18f, cy + 30f),
            size = androidx.compose.ui.geometry.Size(56f, 56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
            style = stroke
        )

        // Cross glyph (X)
        val xSize = 22f
        val xCenter = androidx.compose.ui.geometry.Offset(cx - 74f, cy + 46f)
        drawLine(
            color = primaryColor.copy(alpha = 0.14f),
            start = androidx.compose.ui.geometry.Offset(xCenter.x - xSize, xCenter.y - xSize),
            end = androidx.compose.ui.geometry.Offset(xCenter.x + xSize, xCenter.y + xSize),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.14f),
            start = androidx.compose.ui.geometry.Offset(xCenter.x + xSize, xCenter.y - xSize),
            end = androidx.compose.ui.geometry.Offset(xCenter.x - xSize, xCenter.y + xSize),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
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
