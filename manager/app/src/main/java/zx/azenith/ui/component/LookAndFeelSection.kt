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

/**
 * Shape, text size, motion and content contrast.
 *
 * Split out of CustomThemeScreen because that file already carries the whole
 * settings list and this section is independently reusable by the quick-settings
 * tile. Every control reports through [onPersonalizationChange] instead of
 * touching SharedPreferences, so the screen stays the owner of persistence.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LookAndFeelSection(
    pers: Personalization,
    onPersonalizationChange: (Personalization) -> Unit,
    modifier: Modifier = Modifier,
    // The spec picker and accent swatches live in CustomThemeScreen because they own
    // their own state, but they are colour controls, so the screen passes them in as
    // leading slots. That keeps every colour control in one contiguous group instead
    // of splitting the section across two headers.
    colorSpecContent: (@Composable () -> Unit)? = null,
    accentSwatchContent: (@Composable () -> Unit)? = null,
    colorModeContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Grouped by what the control changes, not by which subsystem reads it:
        // everything that recolors is under Colours, everything that resizes under
        // Shape & size, the bar under Navigation, the banner under Banner, and the
        // rest under Motion & contrast. A control that shares a group card with its
        // neighbours is one the user can find again.
        SettingsGroup(titleRes = R.string.pers_colour_group) {
            colorSpecContent?.invoke()
            accentSwatchContent?.invoke()
            colorModeContent?.invoke()
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

        SettingsGroup(titleRes = R.string.pers_shape_group) {
            // One continuous knob instead of a preset row plus specimens: the
            // preview mockup already draws the real ramp, so a second picture of
            // it next to the slider is redundant, and the preset labels clipped.
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
            LabeledSlider(
                icon = Icons.Filled.TextFields,
                label = stringResource(R.string.pers_text_scale),
                value = pers.textScale,
                valueText = "${(pers.textScale * 100).toInt()}%",
                valueRange = 0.85f..1.3f,
                steps = 8,
                commitOnRelease = true,
                // Text scale drives the density of every screen, so committing per
                // frame re-measured the whole app mid-drag; the readout is passed in
                // so it still tracks the finger while the layout does not.
                valueTextOf = { "${(it * 100).toInt()}%" },
                onValueChange = { onPersonalizationChange(pers.copy(textScale = it)) }
            )

        }

        SettingsGroup(
            titleRes = R.string.pers_navbar,
            captionRes = R.string.pers_navbar_summary
        ) {
            
            // A pinned bar is flush with the window edges and has no corners to round,
            // so the corner choice is only offered while the bar floats.
            if (pers.navStyle == NavStyle.Floating) {
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
