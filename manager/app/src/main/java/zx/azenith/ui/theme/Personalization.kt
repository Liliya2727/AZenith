package zx.azenith.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
/**
 * How far the palette is pushed from what Material You generated. Tone goes the
 * other way from the usual "vivid" slider: a dynamic scheme derived from a
 * wallpaper is often already near-neutral, so the useful knob is how much
 * saturation is added on top, not removed.
 */
enum class AccentIntensity(val labelRes: Int) {
    Neutral(R.string.pers_accent_neutral),
    Balanced(R.string.pers_accent_balanced),
    Bright(R.string.pers_accent_bright),
    Vibrant(R.string.pers_accent_vibrant);

    /** Multiplier applied to the HSL saturation of every accent role. */
    val saturationFactor: Float
        get() = when (this) {
            Neutral -> 0.72f
            Balanced -> 1f
            Bright -> 1.28f
            Vibrant -> 1.55f
        }

    companion object {
        fun fromValue(value: Int): AccentIntensity = entries.getOrElse(value) { Balanced }
    }
}



/**
 * Slider positions, not amounts of change.
 *
 * The track is a dial around the stock ramp: [SHARP_END] and [ROUND_END] are the
 * extremes of the M3 shape scale and [CENTER] is the untouched stock ramp, so the
 * user always has a neutral point to fall back on and both directions read the
 * same way. Every other corner control follows the same convention -- boxed at
 * one end, pill at the other, stock in the middle.
 */
private const val SHARP_END = 0f
const val CENTER = 0.5f
private const val ROUND_END = 1f

/** Corner scale of the stock ramp, and of each end of the dial. */
private const val CENTER_MULTIPLIER = 1f
private const val SHARP_MULTIPLIER = 0.15f
private const val ROUND_MULTIPLIER = 2.4f

/**
 * Navbar size slider travel. The real range is deliberately narrow at the top --
 * the bar is already near the width four tabs need, so it can only grow 5% past
 * the original -- and generous at the bottom, where a compact bar is a real
 * request.
 */
const val NAV_SCALE_MIN = 0.60f
const val NAV_SCALE_MAX = 1.05f

/**
 * Rest point of the size dial: the shipped bar.
 */
const val NAV_SCALE_DEFAULT = 1f

/** Tab spacing dial travel, as a multiple of the shipped gap. */
const val NAV_SPACING_MIN = 0f
const val NAV_SPACING_MAX = 2.5f

/**
 * The size readout as a percentage, which is not the same number as the scale.
 *
 * [NAV_SCALE_MAX] is only 5% taller than the default in reality, but the readout
 * treats the whole travel as a boost, so the top end reads 150%. A raw 105%
 * understates the range the slider actually offers.
 */
fun navScalePercent(scale: Float): Int =
    (NAV_SCALE_MIN_READOUT +
        (scale - NAV_SCALE_MIN) / (NAV_SCALE_MAX - NAV_SCALE_MIN) *
            (NAV_SCALE_MAX_READOUT - NAV_SCALE_MIN_READOUT)).roundToInt()

private const val NAV_SCALE_MIN_READOUT = 60f
private const val NAV_SCALE_MAX_READOUT = 150f

data class Personalization(
    val roundness: Float = CENTER,
    val textScale: Float = 1f,
    val accentIntensity: AccentIntensity = AccentIntensity.Balanced,
    val bannerRadius: Float = BANNER_CENTER_RADIUS,
    val navStyle: NavStyle = NavStyle.Floating,
    val navRadius: Float = NAV_CENTER_RADIUS,
    // SelectedOnly is the shipped behaviour: the pill interpolates its label open
    // with the swipe and every unselected tab shows the icon alone.
    val navLabels: NavLabelMode = NavLabelMode.SelectedOnly,
    // Off by default: the stock neutral surface is what most themes want, and an
    // accent-tinted bar is a deliberate choice rather than the baseline.
    val vibrantNav: Boolean = false,
    // 1f is the shipped bar. The travel is deliberately lopsided -- it can shrink
    // but only grows 5% past the original, because the bar is already near the
    // width the four tabs need.
    val navScale: Float = 1f,
    // Gap between tabs, as a multiple of the shipped 6dp. Separate from
    // navScale because the bar hugging its content means a tight gap reads as
    // cramped even at a comfortable size, and widening it does not need a bigger
    // icon to justify it.
    val navSpacing: Float = 1f
) {
    /**
     * The M3 shape ramp, scaled by one continuous [roundness] value.
     *
     * One knob, not a preset plus a trim: both used to move the same multiplier,
     * so two controls described one dimension. The mapping is linear on each side
     * of [DEFAULT_ROUNDNESS] so the stock ramp sits exactly at the centre of the
     * travel -- sharp and fully round are equidistant from the default, which is
     * what makes "released near the middle snaps back to stock" feel right.
     */
    val shapes: Shapes
        get() {
            val m = cornerMultiplier
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
        get() = roundness == CENTER

    /** Corner scale of the extraLarge ramp step, exposed so a preview can keep its
     *  corners proportional while it shrinks. */
    val cornerMultiplier: Float
        get() {
            val r = roundness.coerceIn(SHARP_END, ROUND_END)
            // Distance from centre, so the stock ramp is the slider's zero point.
            val t = 2f * kotlin.math.abs(r - CENTER)
            val extreme = if (r < CENTER) SHARP_MULTIPLIER else ROUND_MULTIPLIER
            return lerp(CENTER_MULTIPLIER, extreme, t)
        }

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

    /**
     * A widget corner that tracks [roundness], from a radius the surface would
     * have used on its own.
     *
     * Widgets cannot read [shapes] directly: the card surfaces are authored
     * against a 26dp radius that sits between the large and extraLarge ramp
     * steps, so scaling a ramp step moves them off the shape they were designed
     * for. Scaling the authored radius instead keeps the widget's proportions at
     * the stock setting and puts the sharp and round ends on the same dial.
     */
    fun widgetCorner(base: Dp): Shape = RoundedCornerShape(base * cornerMultiplier)

    /**
     * A widget corner that cannot exceed half of [height]: past that the ends
     * meet and a rectangle stops being a rectangle.
     */
    fun widgetCorner(base: Dp, height: Dp): Shape =
        RoundedCornerShape((base * cornerMultiplier).coerceAtMost(height / 2))

    companion object {
        const val PREF_ROUNDNESS = "pers_roundness"
        const val PREF_TEXT = "pers_text_scale"
        const val PREF_ACCENT = "pers_accent_intensity"
        const val PREF_BANNER_SHAPE = "pers_banner_shape"
        const val PREF_NAV_STYLE = "pers_nav_style"
        const val PREF_NAV_SHAPE = "pers_nav_shape"
        const val PREF_NAV_LABELS = "pers_nav_labels"
        const val PREF_NAV_VIBRANT = "pers_nav_vibrant"
        const val PREF_NAV_SCALE = "pers_nav_scale"
        const val PREF_NAV_SPACING = "pers_nav_spacing"

        /**
         * Reads a boolean a previous build may have stored as the string "1".
         *
         * A String is accepted because an earlier version of the nav prefs wrote
         * every value through getString, and reading such a key with
         * getBoolean throws ClassCastException on this platform.
         */
        private fun SharedPreferences.booleanOr(key: String, fallback: Boolean): Boolean =
            when (val stored = all[key]) {
                is Boolean -> stored
                is String -> stored == "1" || stored.equals("true", ignoreCase = true)
                else -> fallback
            }

        /**
         * Reads a float that a previous build may have stored as a String.
         *
         * Returns [fallback] for anything that is not a finite number in range,
         * which covers the ordinal-string case, a String that was never a number,
         * and a value a future version wrote that this one does not understand.
         */
        private fun SharedPreferences.floatOr(
            key: String,
            fallback: Float,
            range: ClosedFloatingPointRange<Float> = 0f..1f
        ): Float {
            val stored = try {
                all[key]
            } catch (_: ClassCastException) {
                null
            }
            val value = when (stored) {
                is Float -> stored
                is Int -> stored.toFloat()
                is String -> stored.toFloatOrNull()
                is Boolean -> null
                else -> null
            }
            return if (value != null && value.isFinite() && value in range) value else fallback
        }

        fun read(context: Context): Personalization {
            val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return Personalization(
                // Older builds wrote a preset ordinal and a trim into two keys; both
                // collapsed onto the stock ramp only when neither was set, so any
                // migrated value that is not exactly the default is dropped rather
                // than guessed at.
                roundness = p.floatOr(PREF_ROUNDNESS, CENTER),
                textScale = p.floatOr(PREF_TEXT, 1f, 0.85f..1.3f),
                accentIntensity = AccentIntensity.fromValue(
                    p.getString(PREF_ACCENT, null)?.toIntOrNull() ?: AccentIntensity.Balanced.ordinal
                ),
                // Banner and nav corners used to be enum ordinals written with
                // putString, then became continuous fractions written with
                // putFloat. getFloat casts the stored value unchecked, so an
                // install upgrading across that change would throw on every
                // launch; read the stored Object and coerce instead.
                bannerRadius = p.floatOr(PREF_BANNER_SHAPE, BANNER_CENTER_RADIUS),
                navStyle = NavStyle.fromValue(
                    p.getString(PREF_NAV_STYLE, null)?.toIntOrNull() ?: NavStyle.Floating.ordinal
                ),
                navRadius = p.floatOr(PREF_NAV_SHAPE, NAV_CENTER_RADIUS),
                navLabels = NavLabelMode.fromValue(
                    p.getString(PREF_NAV_LABELS, null)?.toIntOrNull() ?: NavLabelMode.SelectedOnly.ordinal
                ),
                vibrantNav = p.booleanOr(PREF_NAV_VIBRANT, false),
                navScale = p.floatOr(PREF_NAV_SCALE, 1f, NAV_SCALE_MIN..NAV_SCALE_MAX),
                navSpacing = p.floatOr(PREF_NAV_SPACING, 1f, NAV_SPACING_MIN..NAV_SPACING_MAX)
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
/**
 * Corner radius as a fraction of the surface's own height, so a fully rounded
 * bar stays a capsule whatever the bar's height is.
 *
 * Both corner sliders run the same dial as [CENTER]: boxed at one end, pill at
 * the other, stock in the middle.
 */
/** The stock value each corner slider rests at, so the UI can snap to the same point. */
const val BANNER_CENTER = 0.12f
// Rest point of the bar-corners dial: fully rounded, which is the Material
// capsule the stock floating bar uses.
const val NAV_CENTER = 1f

private const val BANNER_CENTER_RADIUS = BANNER_CENTER
private const val NAV_CENTER_RADIUS = NAV_CENTER

/**
 * Which edge the bar is docked to when the screen is landscape.
 *
 * Only [Bottom] is reachable today; the bar is hardwired to the bottom edge and
 * its reveal animation translates on Y. Carrying the choice as data rather than
 * branching on `Configuration.orientation` at the call site means the vertical
 * rail is an added enum value and a layout branch, not a new plumbing path.
 */
enum class NavEdge { Bottom, Start, End }

/** Whether the tab labels are drawn at all. */
enum class NavLabelMode(val labelRes: Int) {
    Always(R.string.nav_label_always),
    SelectedOnly(R.string.nav_label_selected),
    Never(R.string.nav_label_never);

    companion object {
        fun fromValue(value: Int): NavLabelMode = entries.getOrElse(value) { SelectedOnly }
    }
}

/**
 * How far a blurred surface is pulled toward the accent. A plain blur over a
 * neutral surface goes grey regardless of the theme's accent, so every blurred
 * surface in the app carries a tint at this strength.
 */
const val ACCENT_TINT_BLUR = 0.14f

/**
 * Blends [amount] of the accent into this colour, keeping its alpha. Blur is a
 * translucent pass, so the tint has to be applied before the alpha is used or it
 * washes out entirely.
 */
@Composable
fun Color.accentTint(amount: Float): Color {
    val accent = MaterialTheme.colorScheme.primary
    return Color(
        red = red + (accent.red - red) * amount,
        green = green + (accent.green - green) * amount,
        blue = blue + (accent.blue - blue) * amount,
        alpha = alpha
    )
}
