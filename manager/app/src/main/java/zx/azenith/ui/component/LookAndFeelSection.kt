package zx.azenith.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Palette
import zx.azenith.ui.theme.AccentIntensity
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.round
import zx.azenith.R
import zx.azenith.ui.theme.Personalization
import zx.azenith.ui.theme.withAccentIntensity
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ViewAgenda
import zx.azenith.ui.theme.NavLabelMode
import zx.azenith.ui.theme.NavStyle
import zx.azenith.ui.theme.NAV_CENTER
import zx.azenith.ui.theme.BANNER_CENTER
import zx.azenith.ui.theme.CENTER
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

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
        SettingsGroup(titleRes = R.string.pers_colour_group) {
            colorSpecContent?.invoke()
            accentSwatchContent?.invoke()
            colorModeContent?.invoke()
            ExpressiveColumn(
                content = buildList {
                    add {
                        LabeledSlider(
                            icon = Icons.Filled.Palette,
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
            )
        }

        SettingsGroup(titleRes = R.string.pers_shape_group) {
            ExpressiveColumn(
                content = buildList {
                    add {
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
                    add {
                        LabeledSlider(
                            icon = Icons.Filled.TextFields,
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
            )
        }

        SettingsGroup(titleRes = R.string.pers_navbar) {
            ExpressiveColumn(
                content = buildList {
                    // Pinned bar gak punya corner, jadi slider cuma ada pas floating.
                    if (pers.navStyle == NavStyle.Floating) {
                        add {
                            LabeledSlider(
                                icon = Icons.Filled.RoundedCorner,
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
                    add {
                        Column {
                            LabeledControl(Icons.Filled.Label, stringResource(R.string.pers_nav_labels))
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