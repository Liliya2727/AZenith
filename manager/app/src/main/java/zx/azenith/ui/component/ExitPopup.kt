package zx.azenith.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zx.azenith.R

@Composable
fun ExitPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AZenithDialog(
        visible = visible,
        onDismiss = onDismiss,
        spec = DialogSpec.Exit
    ) {
        DialogIconDisc(
            icon = Icons.AutoMirrored.Rounded.ExitToApp,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))
        DialogTitle(text = stringResource(R.string.dialog_exit_confirm_title), centered = true)
        Spacer(modifier = Modifier.height(12.dp))
        DialogBody(text = stringResource(R.string.dialog_exit_confirm_content), centered = true)
        Spacer(modifier = Modifier.height(24.dp))
        DialogActions(
            onDismiss = onDismiss,
            confirmText = stringResource(R.string.dialog_exit_confirm_button),
            onConfirm = onConfirm,
            destructive = true
        )
    }
}
