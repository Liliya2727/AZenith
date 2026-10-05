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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.theme.AccentIntensity
import zx.azenith.ui.theme.NavLabelMode
import zx.azenith.ui.theme.NavStyle
import zx.azenith.ui.theme.NAV_CENTER
import zx.azenith.ui.theme.Personalization
import zx.azenith.ui.theme.CENTER

/**
 * Owns the Personalization screen's information architecture.
 *
 * One group per question the user is asking. Colour answers "how is the theme
 * painted". Shape answers "how big are the corners and the type". Navigation
 * answers "how does the bar behave".
 */
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
        SettingsGroup(
            titleRes = R.string.pers_colour_group,
            icon = Icons.Filled.Palette,
            initiallyExpanded = true
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

        SettingsGroup(titleRes = R.string.pers_shape_group, icon = Icons.Filled.RoundedCorner) {
            ExpressiveColumn(
                content = buildList {
                    add {
                        CardItem {
                            LabeledSlider(
                                icon = Icons.Filled.RoundedCorner,
                                label = stringResource(R.string.pers_roundness),
                                value = pers.roundness,
                                valueText = cornerLabel(pers.roundness, CENTER),
                                valueRange = 0f..1f,
                                steps = 0,
                                snapTo = CENTER,
                                onValueChange = { onPersonalizationChange(pers.copy(roundness = it)) }
                            )
                        }
                    }
                    add {
                        CardItem {
                            LabeledSlider(
                                icon = Icons.Filled.FormatSize,
                                label = stringResource(R.string.pers_text_scale),
                                value = pers.textScale,
                                valueText = "${(pers.textScale * 100).toInt()}%",
                                valueRange = 0.85f..1.3f,
                                steps = 8,
                                commitOnRelease = true,
                                valueTextOf = { "${(it * 100).toInt()}%" },
                                onValueChange = { onPersonalizationChange(pers.copy(textScale = it)) }
                            )
                        }
                    }
                }
            )
        }

        SettingsGroup(titleRes = R.string.pers_navbar, icon = Icons.Filled.ViewAgenda) {
            ExpressiveColumn(
                content = buildList {
                    add {
                        CardItem {
                            LabeledControl(Icons.Filled.ViewAgenda, stringResource(R.string.pers_nav_floating))
                            ConnectedToggleRow(
                                options = NavStyle.entries,
                                selected = pers.navStyle,
                                label = { stringResource(it.labelRes) },
                                onSelect = { onPersonalizationChange(pers.copy(navStyle = it)) }
                            )
                        }
                    }
                    if (pers.navStyle == NavStyle.Floating) {
                        add {
                            CardItem {
                                LabeledSlider(
                                    icon = Icons.Filled.HorizontalRule,
                                    label = stringResource(R.string.pers_nav_shape),
                                    value = pers.navRadius,
                                    valueText = cornerLabel(pers.navRadius, NAV_CENTER),
                                    valueRange = 0f..1f,
                                    steps = 0,
                                    snapTo = NAV_CENTER,
                                    onValueChange = { onPersonalizationChange(pers.copy(navRadius = it)) }
                                )
                            }
                        }
                    }
                    add {
                        CardItem {
                            LabeledControl(Icons.AutoMirrored.Filled.Label, stringResource(R.string.pers_nav_labels))
                            ConnectedToggleRow(
                                options = NavLabelMode.entries,
                                selected = pers.navLabels,
                                label = { stringResource(it.labelRes) },
                                onSelect = { onPersonalizationChange(pers.copy(navLabels = it)) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CardItem(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}
