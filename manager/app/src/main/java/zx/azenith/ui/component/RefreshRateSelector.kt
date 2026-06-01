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
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.util.expressiveBlur

private data class RefreshRatePickerOption(
    val titleString: String,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRefreshRatePickerOptions(context: Context): List<RefreshRatePickerOption> {
    val supported = getSupportedRefreshRatesPicker(context)
    
    return supported.mapIndexed { index, rate ->
        RefreshRatePickerOption(
            titleString = context.getString(R.string.refresh_rate_format, rate),
            reason = index.toString(), 
            icon = Icons.Outlined.WebStories
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshRatePickerDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRefreshRatePicker: (String) -> Unit
) {
    if (!show) return
    val context = LocalContext.current
    val options = getRefreshRatePickerOptions(context)

    BasicAlertDialog(onDismissRequest = onDismiss) {
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
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.RefreshRatePicker_Select),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val content = options.map { option ->
                    @Composable {
                        ExpressiveListItem(
                            modifier = Modifier.padding(vertical = 4.dp),
                            headlineContent = { Text(option.titleString) },
                            leadingContent = { 
                                SmallLeadingIcon(icon = option.icon) 
                            },
                            onClick = {
                                onDismiss()
                                onRefreshRatePicker(option.reason)
                            }
                        )
                    }
                }

                ExpressiveColumn(
                    modifier = Modifier.padding(top = 12.dp),
                    content = content
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RefreshRatePickerDialogPreview() {
    MaterialTheme {
        RefreshRatePickerDialog(
            show = true,
            onDismiss = {},
            onRefreshRatePicker = {}
        )
    }
}
