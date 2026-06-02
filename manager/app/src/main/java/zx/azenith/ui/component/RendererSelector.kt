package zx.azenith.ui.component

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import zx.azenith.R

private data class RendererOption(
    val titleRes: Int,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRendererOptions(): List<RendererOption> {
    return listOf(
        RendererOption(R.string.Renderer_Default, "Default", Icons.Rounded.SettingsBackupRestore),
        RendererOption(R.string.Renderer_Vulkan, "vulkan", Icons.Rounded.Layers),
        RendererOption(R.string.Renderer_SkiaGL, "skiagl", Icons.Rounded.Layers),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendererDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRenderer: (String) -> Unit,
    hazeState: HazeState? = null
) {
    if (!show) return
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = remember { prefs.getBoolean("is_blur_enabled", false) }

    val options = getRendererOptions()
    val dialogShape = RoundedCornerShape(28.dp)

    val containerColor = if (isBlurEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = dialogShape,
            color = if (isBlurEnabled && hazeState != null) Color.Transparent else containerColor,
            modifier = Modifier.then(
                if (isBlurEnabled && hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = containerColor,
                            blurRadius = 24.dp,
                            tint = HazeTint(Color.Black.copy(alpha = 0.1f)) // <--- BUNGKUS DENGAN HazeTint
                        )
                    )
                } else {
                    Modifier
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.Renderer_Select),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val content = options.map { option ->
                    @Composable
                    {
                        // Pakai Highlight & Color.Transparent agar blur tembus
                        ExpressiveListItemHighlight(
                            modifier = Modifier.padding(vertical = 8.dp),
                            containerColor = Color.Transparent,
                            headlineContent = {
                                Text(stringResource(option.titleRes))
                            },
                            leadingContent = { 
                                SmallLeadingIcon(icon = option.icon) 
                            },
                            onClick = {
                                onDismiss()
                                onRenderer(option.reason)
                            }
                        )
                    }
                }

                ExpressiveColumn(
                    modifier = Modifier.padding(top = 20.dp),
                    content = content
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RendererDialogPreview() {
    MaterialTheme {
        RendererDialog(
            show = true,
            onDismiss = {},
            onRenderer = {}
        )
    }
}
