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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import zx.azenith.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    hazeState: HazeState? = null
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val isBlurEnabled = remember { prefs.getBoolean("is_blur_enabled", false) }
    
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val containerColor = if (isBlurEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = if (isBlurEnabled && hazeState != null) Color.Transparent else containerColor,
        modifier = Modifier.then(
            if (isBlurEnabled && hazeState != null) {
                Modifier.hazeChild(
                    state = hazeState,
                    shape = sheetShape,
                    style = HazeStyle(
                        backgroundColor = containerColor,
                        blurRadius = 24.dp,
                        tint = Color.Black.copy(alpha = 0.1f)
                    )
                )
            } else Modifier
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
                        ExpressiveListItemHighlight(
                            containerColor = Color.Transparent,
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
                        ExpressiveListItemHighlight(
                            containerColor = Color.Transparent,
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
