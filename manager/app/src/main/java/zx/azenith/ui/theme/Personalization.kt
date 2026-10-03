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

data class Personalization(
    val shapeScale: ShapeScale = ShapeScale.Medium,
    val textScale: Float = 1f,
    val motionScale: MotionScale = MotionScale.Full,
    val cornerBoost: Float = 0f,
    val contentContrast: Boolean = false
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
                contentContrast = p.getBoolean(PREF_CONTRAST, false)
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
