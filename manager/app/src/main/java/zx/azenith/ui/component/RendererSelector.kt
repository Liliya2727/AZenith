package zx.azenith.ui.component

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import zx.azenith.ui.component.*
import zx.azenith.R
import zx.azenith.ui.util.expressiveBlur

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
    onRenderer: (String) -> Unit
) {
    if (!show) return

    val options = getRendererOptions()

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        // PENERAPAN BLUR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveBlur(
                    shape = RoundedCornerShape(28.dp),
                    fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    alpha = 0.75f,
                    blurRadius = 30.dp
                ),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent, 
            tonalElevation = 0.dp
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
                        ExpressiveListItem(
                            modifier = Modifier.padding(vertical = 8.dp),
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
