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

@file:OptIn(ExperimentalMaterial3Api::class)

package zx.azenith.ui.component


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.component.*


@Composable
fun BackupRestoreBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    AZenithSheet(
        show = show,
        onDismiss = onDismiss,
        title = stringResource(R.string.str_backup_restore),
        style = SheetRowStyle.Action,
        items = listOf(
            SheetItem(
                label = stringResource(R.string.str_backup_configuration),
                summary = stringResource(R.string.str_save_your_current_tweak_settin),
                icon = Icons.Outlined.Save,
            ),
            SheetItem(
                label = stringResource(R.string.str_restore_configuration),
                summary = stringResource(R.string.str_load_a_previously_saved_backup),
                icon = Icons.Outlined.SettingsBackupRestore,
            ),
        ),
        onItemClick = { index ->
            onDismiss()
            if (index == 0) onBackup() else onRestore()
        },
    )
}
