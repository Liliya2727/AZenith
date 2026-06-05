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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import zx.azenith.ui.component.*

@Composable
fun BackupRestoreBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    CustomBottomSheet(
        visible = show,
        onDismiss = onDismiss
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
                            headlineContent = { Text("Backup Configuration", color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text("Save your current tweak settings", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = { SmallLeadingIcon(Icons.Outlined.Save) },
                            onClick = {
                                onDismiss()
                                onBackup()
                            }
                        )
                    },
                    {
                        ExpressiveListItem(
                            headlineContent = { Text("Restore Configuration", color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text("Load a previously saved backup", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
