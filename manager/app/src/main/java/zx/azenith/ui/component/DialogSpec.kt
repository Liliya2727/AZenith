/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zx.azenith.R

/** The styling presets a dialog can ask for. Motion and layout live in [AZenithDialog]. */
enum class DialogStyle {
    /** Title, optional body, trailing action row. The M3 basic dialog. */
    Basic,

    /** A tinted icon disc above a centred title. */
    IconTop,

    /** Body with no action row, for progress and status dialogs. */
    BodyOnly,
}

/**
 * A dialog's appearance, so call sites pick a preset instead of restating colours and metrics.
 * Only the fields a given style actually reads are meaningful.
 */
data class DialogSpec(
    val style: DialogStyle = DialogStyle.Basic,
    val minWidth: Dp = 280.dp,
    val maxWidth: Dp = 352.dp,
    val cornerRadius: Dp = 28.dp,
    val contentPadding: Dp = 24.dp,
    val containerColor: Color? = null,
    val dismissible: Boolean = true,
    val registersActiveDialog: Boolean = true,
    val zIndex: Float = 0f
) {
    companion object {
        /** Option pickers: renderer, refresh rate, profile. */
        val Choice = DialogSpec(minWidth = 320.dp, maxWidth = 400.dp)

        /** Backup / restore options and plain confirmations. */
        val Confirm = DialogSpec(minWidth = 350.dp, maxWidth = 500.dp)

        /** The exit confirmation, with its leading icon disc. */
        val Exit = DialogSpec(style = DialogStyle.IconTop, minWidth = 280.dp, maxWidth = 340.dp)

        /** Blocking progress. Not dismissable. */
        val Loading = DialogSpec(
            style = DialogStyle.BodyOnly,
            minWidth = 100.dp,
            maxWidth = 100.dp,
            cornerRadius = 24.dp,
            dismissible = false
        )

        /** Blocking install progress. Not dismissable. */
        val Installing = DialogSpec(
            style = DialogStyle.BodyOnly,
            minWidth = 280.dp,
            maxWidth = 350.dp,
            dismissible = false
        )
    }
}

/** The M3 icon disc: a filled circle behind a tinted icon, which is how dialogs lead with an icon. */
@Composable
fun DialogIconDisc(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onErrorContainer,
    containerColor: Color = MaterialTheme.colorScheme.errorContainer,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

/** Cancel then confirm, M3 ordering. */
@Composable
fun DialogActions(
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.dialog_cancel),
    confirmEnabled: Boolean = true,
    destructive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onDismiss) { Text(dismissText) }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            } else ButtonDefaults.buttonColors()
        ) {
            Text(confirmText)
        }
    }
}

/** Standard M3 dialog title. */
@Composable
fun DialogTitle(text: String, centered: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier
    )
}

/** Body copy on [MaterialTheme.colorScheme.onSurfaceVariant]. */
@Composable
fun DialogBody(text: String, centered: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}