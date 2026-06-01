@file:OptIn(ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import zx.azenith.ui.component.*
import zx.azenith.ui.util.expressiveBlur // <-- Import Modifier Blur

@Composable
fun BackupRestoreBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // PENERAPAN BLUR
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.expressiveBlur(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            fallbackColor = MaterialTheme.colorScheme.surfaceContainer,
            alpha = 0.75f,
            blurRadius = 30.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            ExpressiveList(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = listOf(
                    {
                        ExpressiveListItem(
                            headlineContent = { Text("Backup Configuration") },
                            supportingContent = { Text("Save your current tweak settings") },
                            leadingContent = { SmallLeadingIcon(Icons.Outlined.Save) },
                            onClick = {
                                onDismiss()
                                onBackup()
                            }
                        )
                    },
                    {
                        ExpressiveListItem(
                            headlineContent = { Text("Restore Configuration") },
                            supportingContent = { Text("Load a previously saved backup") },
                            leadingContent = { SmallLeadingIcon(Icons.Outlined.SettingsBackupRestore) },
                            onClick = {
                                onDismiss()
                                onRestore()
                            }
                        )
                    }
                )
            )
        }
    }
}
