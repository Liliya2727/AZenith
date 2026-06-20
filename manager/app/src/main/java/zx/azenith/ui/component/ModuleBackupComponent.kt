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
                text = stringResource(R.string.str_backup_restore),
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
                            headlineContent = { Text(stringResource(R.string.str_backup_configuration), color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(stringResource(R.string.str_save_your_current_tweak_settin), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = { SmallLeadingIcon(Icons.Outlined.Save) },
                            onClick = {
                                onDismiss()
                                onBackup()
                            }
                        )
                    },
                    {
                        ExpressiveListItem(
                            headlineContent = { Text(stringResource(R.string.str_restore_configuration), color = MaterialTheme.colorScheme.onSurface) },
                            supportingContent = { Text(stringResource(R.string.str_load_a_previously_saved_backup), color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
