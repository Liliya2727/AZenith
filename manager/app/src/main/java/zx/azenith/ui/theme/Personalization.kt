package zx.azenith.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import zx.azenith.R

/**
 * App-wide personalization state that is not colour: shape scale, text scale and
 * motion intensity. Everything here is pure client-side theming -- no props, no
 * daemon keys -- so it can be changed live from the Personalization screen.
 *
 * Shape scale interpolates the M3 shape ramp instead of swapping between two
 * hand-written shape sets, so every step stays on-spec. Motion is a scale
 * factor rather than an on/off because the interesting middle is "halve every
 * duration", which an on/off switch cannot express.
 */
enum class ShapeScale(val labelRes: Int) {
    ExtraSmall(R.string.pers_shape_extra_small),
    Small(R.string.pers_shape_small),
    Medium(R.string.pers_shape_medium),
    Large(R.string.pers_shape_large),
    ExtraLarge(R.string.pers_shape_extra_large);

    companion object {
        fun fromValue(value: Int): ShapeScale = entries.getOrElse(value) { Medium }
    }
}

enum class MotionScale(val labelRes: Int) {
    Full(R.string.pers_motion_full),
    Reduced(R.string.pers_motion_reduced),
    Minimal(R.string.pers_motion_minimal);

    companion object {
        fun fromValue(value: Int): MotionScale = entries.getOrElse(value) { Full }
    }
}

/**
 * How far the palette is pushed from what Material You generated. Tone goes the
 * other way from the usual "vivid" slider: a dynamic scheme derived from a
 * wallpaper is often already near-neutral, so the useful knob is how much
 * saturation is added on top, not removed.
 */
enum class AccentIntensity(val labelRes: Int) {
    Neutral(R.string.pers_accent_neutral),
    Balanced(R.string.pers_accent_balanced),
    Vivid(R.string.pers_accent_vivid),
    Neon(R.string.pers_accent_neon);

    /** Multiplier applied to the HSL saturation of every accent role. */
    val saturationFactor: Float
        get() = when (this) {
            Neutral -> 0.72f
            Balanced -> 1f
            Vivid -> 1.28f
            Neon -> 1.55f
        }

    companion object {
        fun fromValue(value: Int): AccentIntensity = entries.getOrElse(value) { Balanced }
    }
}

/** Corner treatment for the banner and other full-bleed surfaces. */
enum class BannerShape(val labelRes: Int) {
    Rectangular(R.string.pers_banner_square),
    Rounded(R.string.pers_banner_rounded),
    Pill(R.string.pers_banner_pill);

    /** Corner radius as a fraction of the surface's own height. */
    val radiusFraction: Float
        get() = when (this) {
            Rectangular -> 0f
            Rounded -> 0.12f
            Pill -> 0.5f
        }

    companion object {
        fun fromValue(value: Int): BannerShape = entries.getOrElse(value) { Rounded }
    }
}

data class Personalization(
    val shapeScale: ShapeScale = ShapeScale.Medium,
    val textScale: Float = 1f,
    val motionScale: MotionScale = MotionScale.Full,
    val cornerBoost: Float = 0f,
    val contentContrast: Boolean = false,
    val accentIntensity: AccentIntensity = AccentIntensity.Balanced,
    val bannerShape: BannerShape = BannerShape.Rounded,
    val navStyle: NavStyle = NavStyle.Floating,
    val navShape: NavShape = NavShape.Rounded,
    // SelectedOnly is the shipped behaviour: the pill interpolates its label open
    // with the swipe and every unselected tab shows the icon alone.
    val navLabels: NavLabelMode = NavLabelMode.SelectedOnly
) {
    /**
     * The M3 shape ramp. Two independent knobs compose: [ShapeScale] picks the
     * preset (what the toggle row sets) and [cornerBoost] is the fine trim on top
     * (what the slider sets). Every corner derives from one multiplier so the
     * ramp stays monotonic -- mixing per-role literals is what makes a custom
     * shape scheme look like a mistake.
     *
     * At the default preset with no trim this returns the app's stock ramp, so
     * the common case is byte-identical to having no personalization at all.
     */
    val shapes: Shapes
        get() {
            val m = shapeScaleMultiplier * (1f + cornerBoost)
            return Shapes(
                extraSmall = RoundedCornerShape(4.dp * m),
                small = RoundedCornerShape(8.dp * m),
                medium = RoundedCornerShape(12.dp * m),
                large = RoundedCornerShape(16.dp * m),
                extraLarge = RoundedCornerShape(28.dp * m)
            )
        }

    /** True when the ramp is the stock one, so the theme can skip overriding it. */
    val isStockShape: Boolean
        get() = shapeScale == ShapeScale.Medium && cornerBoost == 0f

    /** Corner scale of the extraLarge ramp step, exposed so a preview can keep its
     *  corners proportional while it shrinks. */
    val cornerMultiplier: Float
        get() = shapeScaleMultiplier * (1f + cornerBoost)

    private val shapeScaleMultiplier: Float
        get() = when (shapeScale) {
            ShapeScale.ExtraSmall -> 0.15f
            ShapeScale.Small -> 0.5f
            ShapeScale.Medium -> 1f
            ShapeScale.Large -> 1.6f
            ShapeScale.ExtraLarge -> 2.4f
        }

    /** Duration multiplier applied to the app's expressive motion scheme. */
    val motionFactor: Float
        get() = when (motionScale) {
            MotionScale.Full -> 1f
            MotionScale.Reduced -> 1.6f
            MotionScale.Minimal -> 2.6f
        }

    companion object {
        const val PREF_SHAPE = "pers_shape_scale"
        const val PREF_TEXT = "pers_text_scale"
        const val PREF_MOTION = "pers_motion_scale"
        const val PREF_CORNERS = "pers_corner_boost"
        const val PREF_CONTRAST = "pers_content_contrast"
        const val PREF_ACCENT = "pers_accent_intensity"
        const val PREF_BANNER_SHAPE = "pers_banner_shape"
        const val PREF_NAV_STYLE = "pers_nav_style"
        const val PREF_NAV_SHAPE = "pers_nav_shape"
        const val PREF_NAV_LABELS = "pers_nav_labels"

        fun read(context: Context): Personalization {
            val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return Personalization(
                shapeScale = ShapeScale.fromValue(
                    p.getString(PREF_SHAPE, null)?.toIntOrNull() ?: ShapeScale.Medium.ordinal
                ),
                textScale = p.getFloat(PREF_TEXT, 1f).coerceIn(0.85f, 1.3f),
                motionScale = MotionScale.fromValue(
                    p.getString(PREF_MOTION, null)?.toIntOrNull() ?: MotionScale.Full.ordinal
                ),
                cornerBoost = p.getFloat(PREF_CORNERS, 0f).coerceIn(0f, 0.75f),
                contentContrast = p.getBoolean(PREF_CONTRAST, false),
                accentIntensity = AccentIntensity.fromValue(
                    p.getString(PREF_ACCENT, null)?.toIntOrNull() ?: AccentIntensity.Balanced.ordinal
                ),
                bannerShape = BannerShape.fromValue(
                    p.getString(PREF_BANNER_SHAPE, null)?.toIntOrNull() ?: BannerShape.Rounded.ordinal
                ),
                navStyle = NavStyle.fromValue(
                    p.getString(PREF_NAV_STYLE, null)?.toIntOrNull() ?: NavStyle.Floating.ordinal
                ),
                navShape = NavShape.fromValue(
                    p.getString(PREF_NAV_SHAPE, null)?.toIntOrNull() ?: NavShape.Rounded.ordinal
                ),
                navLabels = NavLabelMode.fromValue(
                    p.getString(PREF_NAV_LABELS, null)?.toIntOrNull() ?: NavLabelMode.SelectedOnly.ordinal
                )
            )
        }
    }
}

val LocalPersonalization = staticCompositionLocalOf { Personalization() }

/** Live personalization, re-read whenever the screen writes a pref. */
@Composable
fun rememberPersonalization(): Personalization {
    val context = LocalContext.current
    val state = remember { mutableStateOf(Personalization.read(context)) }
    DisposableEffect(context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = Personalization.read(context)
        }
        p.registerOnSharedPreferenceChangeListener(listener)
        onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state.value
}

/**
 * Pushes surface-container values apart. Material You's default surface ramp
 * can read as flat next to a saturated accent; nudging the container steps and
 * the outline restores the separation without touching the accent itself, which
 * would change the palette the user explicitly picked.
 */
fun ColorScheme.withContentContrast(enabled: Boolean): ColorScheme {
    if (!enabled) return this
    return copy(
        surfaceContainer = surfaceContainer.push(0.012f),
        surfaceContainerHigh = surfaceContainerHigh.push(0.016f),
        surfaceContainerHighest = surfaceContainerHighest.push(0.02f),
        surfaceContainerLow = surfaceContainerLow.push(0.008f),
        surfaceBright = surfaceBright.push(0.02f),
        outline = outline.push(0.08f),
        outlineVariant = outlineVariant.push(0.06f)
    )
}

/**
 * Moves a colour toward the opposite pole of its own scheme. Pushing luminance
 * alone would clip in wide-gamut, so the mix is done in sRGB against the colour's
 * own black/white rather than toward a fixed constant.
 */
private fun Color.push(amount: Float): Color {
    val target = if (luminance() > 0.5f) Color.Black else Color.White
    return Color(
        red = red + (target.red - red) * amount,
        green = green + (target.green - green) * amount,
        blue = blue + (target.blue - blue) * amount,
        alpha = alpha
    )
}

@Composable
@ReadOnlyComposable
fun currentPersonalization(): Personalization = LocalPersonalization.current

/**
 * Scales saturation of every accent role while leaving neutrals alone.
 *
 * Saturation is adjusted in HSL rather than by mixing toward a fixed hue: mixing
 * toward a constant drags the hue with it, which is exactly the wrong behaviour
 * when the user picked a specific accent and only asked for it to read louder.
 * Surface roles are excluded on purpose -- a tinted surface at 1.55x saturation
 * stops reading as a background.
 */
fun ColorScheme.withAccentIntensity(intensity: AccentIntensity): ColorScheme {
    if (intensity == AccentIntensity.Balanced) return this
    val f = intensity.saturationFactor
    return copy(
        primary = primary.scaledSaturation(f),
        onPrimary = onPrimary.scaledSaturation(f),
        primaryContainer = primaryContainer.scaledSaturation(f),
        onPrimaryContainer = onPrimaryContainer.scaledSaturation(f),
        secondary = secondary.scaledSaturation(f),
        onSecondary = onSecondary.scaledSaturation(f),
        secondaryContainer = secondaryContainer.scaledSaturation(f),
        tertiary = tertiary.scaledSaturation(f),
        tertiaryContainer = tertiaryContainer.scaledSaturation(f)
    )
}

private fun Color.scaledSaturation(factor: Float): Color {
    val hsl = FloatArray(3)
    colorToHSL(this, hsl)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return hslToColor(hsl, this.alpha)
}

private fun colorToHSL(color: Color, out: FloatArray) {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    out[2] = l
    if (max == min) {
        out[0] = 0f
        out[1] = 0f
        return
    }
    val d = max - min
    out[1] = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    out[0] = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
}

private fun hslToColor(hsl: FloatArray, alpha: Float): Color {
    val (h, s, l) = hsl
    if (s == 0f) return Color(l, l, l, alpha)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun channel(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Color(channel(h + 1f / 3f), channel(h), channel(h - 1f / 3f), alpha)
}

/**
 * Wraps a [MotionScheme] and stretches every spec it hands out.
 *
 * MotionScheme is the single funnel for MD3 component animation specs, so
 * decorating it scales the whole app's motion from one place -- including
 * components that build their specs internally and never read a duration of
 * their own. Only duration is touched: the easing curves stay as authored,
 * because a slower curve with the wrong shape reads as sluggish rather than
 * calm.
 */
class ScaledMotionScheme(
    private val delegate: MotionScheme,
    private val factor: Float
) : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        delegate.defaultSpatialSpec<T>().stretched(factor)

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        delegate.fastSpatialSpec<T>().stretched(factor)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        delegate.slowSpatialSpec<T>().stretched(factor)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        delegate.defaultEffectsSpec<T>().stretched(factor)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        delegate.fastEffectsSpec<T>().stretched(factor)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        delegate.slowEffectsSpec<T>().stretched(factor)
}

/**
 * A spring carries its character in stiffness/damping, not in a duration, so
 * stretching one would need the physics re-derived. Specs that do have a
 * duration get it multiplied; springs are returned untouched, which keeps every
 * interactive transition feeling like the same material while the timed ones
 * (expands, fades, crossfades) honour the user's preference.
 */
private fun <T> FiniteAnimationSpec<T>.stretched(factor: Float): FiniteAnimationSpec<T> =
    if (factor == 1f) this
    else when (this) {
        is TweenSpec -> tween(
            durationMillis = (durationMillis * factor).toInt(),
            delayMillis = delay,
            easing = easing
        )

        else -> this
    }


/**
 * How the navigation bar is presented.
 *
 * [Pinned] is the pre-M3 bar: full width, edge to edge, icons only.
 * [Floating] is the current pill that floats clear of the screen edges.
 * Surfaced in the UI as one "Floating Navigation Bar" toggle, so these two are
 * the only states worth carrying.
 */
enum class NavStyle(val labelRes: Int) {
    Floating(R.string.nav_style_floating),
    Pinned(R.string.nav_style_pinned);

    companion object {
        fun fromValue(value: Int): NavStyle = entries.getOrElse(value) { Floating }
    }
}

/**
 * Corner treatment for the navigation bar surface. Percent-based so a fully
 * rounded bar stays a capsule at any bar height, which a fixed dp radius cannot
 * guarantee once the user changes [NavStyle].
 */
enum class NavShape(val labelRes: Int, val radiusFraction: Float) {
    Square(R.string.nav_shape_square, 0f),
    Soft(R.string.nav_shape_soft, 0.30f),
    Rounded(R.string.nav_shape_rounded, 0.50f),
    Full(R.string.nav_shape_full, 1f);

    companion object {
        fun fromValue(value: Int): NavShape = entries.getOrElse(value) { Rounded }
    }
}

/** Whether the tab labels are drawn at all. */
enum class NavLabelMode(val labelRes: Int) {
    Always(R.string.nav_label_always),
    SelectedOnly(R.string.nav_label_selected),
    Never(R.string.nav_label_never);

    companion object {
        fun fromValue(value: Int): NavLabelMode = entries.getOrElse(value) { SelectedOnly }
    }
}
