package zx.azenith.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.* // PENTING: Untuk remember, mutableStateOf, DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.rememberDynamicColorScheme

// Mode warna untuk pengaturan aplikasi
enum class ColorMode(val value: Int) {
    SYSTEM(3), LIGHT(4), DARK(5), DARKAMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    fun getDarkThemeValue(systemDarkTheme: Boolean) = when (this) {
        SYSTEM -> systemDarkTheme
        LIGHT -> false
        DARK -> true
        DARKAMOLED -> true
    }
}

data class AppSettings(val colorMode: ColorMode, val keyColor: Int)

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val colorMode = ColorMode.fromValue(
            prefs.getInt("color_mode", ColorMode.SYSTEM.value)
        )
        val keyColor = prefs.getInt("key_color", 0) 
        return AppSettings(colorMode, keyColor)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AZenithTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // State reaktif
    var themeState by remember { mutableStateOf(ThemeController.getAppSettings(context)) }

    // Listener SharedPreferences agar warna berubah instan tanpa recreate/flicker
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            themeState = ThemeController.getAppSettings(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = themeState.colorMode.getDarkThemeValue(systemDarkTheme)
    val amoledMode = themeState.colorMode == ColorMode.DARKAMOLED
    val isDynamic = themeState.keyColor == 0

    val colorScheme = when {
        isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            rememberDynamicColorScheme(
                seedColor = Color.Unspecified,
                isDark = darkTheme,
                isAmoled = amoledMode,
                primary = base.primary,
                secondary = base.secondary,
                tertiary = base.tertiary,
                neutral = base.surface,
                neutralVariant = base.surfaceVariant,
                error = base.error
            )
        }
        !isDynamic -> rememberDynamicColorScheme(
            seedColor = Color(themeState.keyColor), 
            isDark = darkTheme, 
            isAmoled = amoledMode
        )
        else -> if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
    }

    // SideEffect untuk mengatur warna icon Status Bar & Nav Bar
    // Pastikan ini di dalam AZenithTheme, setelah variabel darkTheme didefinisikan
    val view = androidx.compose.ui.platform.LocalView.current
    
    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        
        // 1. Beritahu sistem bahwa kita menangani warna ikon sendiri
        val controller = WindowInsetsControllerCompat(window, view)
        
        // 2. Logika kontras:
        // Jika darkTheme = TRUE (Mode Gelap), isAppearanceLight harus FALSE agar ikon jadi PUTIH
        // Jika darkTheme = FALSE (Mode Terang), isAppearanceLight harus TRUE agar ikon jadi HITAM
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }


    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        4 -> false  // Light
        5, 6 -> true   // Dark / Amoled
        else -> isSystemInDarkTheme()
    }
}
