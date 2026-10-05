/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.theme.AccentIntensity
import zx.azenith.ui.theme.Personalization

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LookAndFeelSection(
    pers: Personalization,
    onPersonalizationChange: (Personalization) -> Unit,
    modifier: Modifier = Modifier,
    colorSpecContent: (@Composable () -> Unit)? = null,
    accentSwatchContent: (@Composable () -> Unit)? = null,
    colorModeContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // One section: what the palette is built from, which accent is pulled
        // out of it, how bright the app is lit and how strongly it speaks are a
        // single colour decision, so they share one card rather than two headers
        // that name the same thing.
        SettingsGroup(
            titleRes = R.string.personalization_colors_header,
            icon = Icons.Filled.ColorLens
        ) {
            colorSpecContent?.invoke()
            accentSwatchContent?.invoke()
            colorModeContent?.invoke()
            ExpressiveColumn(
                content = buildList {
                    add {
                        CardItem {
                            LabeledSlider(
                                icon = Icons.Filled.Tune,
                                label = stringResource(R.string.pers_accent_intensity),
                                value = pers.accentIntensity.ordinal.toFloat(),
                                valueText = stringResource(pers.accentIntensity.labelRes),
                                valueRange = 0f..3f,
                                steps = 2,
                                snapTo = 1f,
                                resetDefault = 1f,
                                onValueChange = {
                                    onPersonalizationChange(
                                        pers.copy(
                                            accentIntensity = AccentIntensity.entries[
                                                it.toInt().coerceIn(0, AccentIntensity.entries.lastIndex)
                                            ]
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
internal fun CardItem(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}
