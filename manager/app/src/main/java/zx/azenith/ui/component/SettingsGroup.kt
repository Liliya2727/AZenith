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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zx.azenith.R

/**
 * One titled group of personalization controls.
 *
 * Every group on the Personalization screen goes through this so they read as
 * siblings: the same header, the same 16dp inset, the same vertical rhythm. A
 * group's header sits *outside* its card -- a label sitting on the container it
 * names makes the boundary ambiguous, and the ragged 12/28dp header padding
 * that used to vary per section is what made the page read as clutter.
 *
 * [content] is the group body. Pass [ExpressiveColumn]-style rows for switches,
 * or plain controls for pickers and sliders; both sit on the same surface.
 */
@Composable
internal fun SettingsGroup(
    titleRes: Int,
    modifier: Modifier = Modifier,
    captionRes: Int? = null,
    titlePadding: Dp = 28.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = titlePadding)
        )
        if (captionRes != null) {
            Text(
                text = stringResource(captionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = titlePadding)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}
