package zx.azenith.ui.component

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import zx.azenith.R

@Composable
fun ExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val confirmRequest = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        // The dialog window is laid out a frame or two after this composable runs.
        kotlinx.coroutines.delay(120)
        confirmRequest.requestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        // A dialog window is its own composition: the activity's key watch never
        // sees these keys, so the outline would stay off without this.
        modifier = Modifier.gamepadKeyWatch(),
        icon = {
            Icon(Icons.Rounded.ExitToApp, contentDescription = null)
        },
        title = {
            Text(text = stringResource(R.string.dialog_exit_confirm_title))
        },
        text = {
            Text(text = stringResource(R.string.dialog_exit_confirm_content))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                },
                modifier = Modifier
                    .focusRequester(confirmRequest)
                    .gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
            ) {
                Text(stringResource(R.string.dialog_exit_confirm_button), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
