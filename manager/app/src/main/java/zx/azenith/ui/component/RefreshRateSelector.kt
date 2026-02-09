package zx.azenith.ui.component

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.util.getSupportedRefreshRatesPicker

private data class RefreshRatePickerOption(
    val titleString: String,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRefreshRatePickerOptions(context: Context): List<RefreshRatePickerOption> {
    // List sekarang berisi: ["144", "120", "90", "60"]
    val supported = getSupportedRefreshRatesPicker(context)
    
    return supported.mapIndexed { index, rate ->
        RefreshRatePickerOption(
            titleString = "$rate Hz",
            // index 0 akan jadi "0", index 1 jadi "1", dst.
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
    // Panggil options berdasarkan context saat ini
    val options = getRefreshRatePickerOptions(context)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.RefreshRatePicker_Select),
                    style = MaterialTheme.typography.titleLarge
                )

                val content = options.map { option ->
                    @Composable {
                        ExpressiveListItem(
                            modifier = Modifier.padding(vertical = 8.dp),
                            headlineContent = { Text(option.titleString) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp).background(
                                        MaterialTheme.colorScheme.secondaryContainer, CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(option.icon, null, modifier = Modifier.size(20.dp))
                                }
                            },
                            onClick = {
                                onDismiss()
                                // Mengirim "0", "1", "2" sesuai urutan max -> min
                                onRefreshRatePicker(option.reason)
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
private fun RefreshRatePickerDialogPreview() {
    MaterialTheme {
        RefreshRatePickerDialog(
            show = true,
            onDismiss = {},
            onRefreshRatePicker = {}
        )
    }
}
