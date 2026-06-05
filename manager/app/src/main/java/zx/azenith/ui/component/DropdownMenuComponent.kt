package zx.azenith.ui.component

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // 👇 Import scroll state
import androidx.compose.foundation.verticalScroll // 👇 Import vertical scroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect

@Composable
fun CustomDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current

    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f, animationSpec = tween(150), transformOrigin = TransformOrigin(1f, 0f)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150), transformOrigin = TransformOrigin(1f, 0f))
            ) {
                Surface(
                    modifier = modifier
                        .widthIn(min = 150.dp, max = 260.dp) 
                        // 👇 KUNCI ANTI-CRASH: Batasi tinggi maksimal agar tidak menembus layar!
                        .heightIn(max = 350.dp) 
                        .padding(top = 4.dp, end = 8.dp) 
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isBlurEnabled && hazeState != null) {
                                Modifier.hazeEffect(state = hazeState) { blurEffect { blurRadius = 24.dp } }
                            } else Modifier
                        ),
                    color = if (isBlurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = if (isBlurEnabled) 0.dp else 3.dp,
                    shadowElevation = if (isBlurEnabled) 0.dp else 3.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            // 👇 KUNCI ANTI-CRASH 2: Jadikan bisa di-scroll jika itemnya banyak!
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun CustomDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2 
        )
    }
}
