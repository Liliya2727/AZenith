package zx.azenith.ui.util

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// State global untuk Haze dan Toggle
val LocalHazeState = compositionLocalOf { HazeState() }
val LocalBlurEnabled = compositionLocalOf { false }

/**
 * Modifier kustom untuk komponen yang ingin diberi efek Blur.
 * Menggunakan Monet Color (warna dinamis MaterialTheme) yang di-blend dengan transparan.
 */
@Composable
fun Modifier.expressiveBlur(
    shape: Shape,
    alpha: Float = 0.65f,
    blurRadius: Dp = 24.dp,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer
): Modifier {
    val isBlurEnabled = LocalBlurEnabled.current
    val hazeState = LocalHazeState.current
    
    // Warna Monet/Dynamic Color transparan untuk Frosted Glass
    val monetTintColor = fallbackColor.copy(alpha = alpha)

    return if (isBlurEnabled) {
        this.clip(shape) // Wajib di-clip dulu sebelum hazeChild
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(monetTintColor), // Bungkus dengan HazeTint untuk API baru
                    blurRadius = blurRadius,
                    noiseFactor = 0.05f // Efek sedikit noise agar mirip iOS / Frosted glass asli
                )
            )
    } else {
        this.background(color = fallbackColor, shape = shape)
    }
}
