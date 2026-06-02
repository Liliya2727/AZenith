@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeTint
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.ui.platform.LocalContext

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
                    style = HazeStyle(
                        backgroundColor = containerColor,
                        blurRadius = 24.dp,
                        tint = HazeTint(Color.Black.copy(alpha = 0.1f)) // <--- BUNGKUS DENGAN HazeTint
                    )
                )
            } else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)      // Your extra padding
                .navigationBarsPadding()     // Automatically adds the bottom inset for the nav bar
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
