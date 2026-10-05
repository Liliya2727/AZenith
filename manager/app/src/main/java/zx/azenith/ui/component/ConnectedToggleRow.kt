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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import zx.azenith.ui.theme.AccentIntensity
import zx.azenith.ui.theme.withAccentIntensity

/**
 * A row of mutually exclusive toggle buttons that read as one control.
 *
 * This is Material's radio group: the leading/middle/trailing shape ladder is
 * what makes the buttons look joined rather than like N separate buttons, so any
 * screen picking one option out of a fixed set uses it instead of re-deriving
 * the ladder. Labels are clipped with an ellipsis because the buttons are
 * weight-equal and a long translated label would otherwise be cut mid-glyph.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> ConnectedToggleRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selected == option,
                onCheckedChange = { checked -> if (checked) onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(containerColor = containerColor)
            ) {
                Text(text = label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * One tile in a settings specimen row: the surface fill, the corner treatment
 * under test, and a hairline border so a zero radius is still visible rather
 * than looking like no setting was applied.
 */
@Composable
internal fun SpecimenTile(
    color: Color,
    shape: Shape,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    )
}

/** [SpecimenTile] with a radius given as a fraction of the tile's own height. */
@Composable
internal fun PercentSpecimenTile(
    color: Color,
    radiusFraction: Float,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) = SpecimenTile(
    color = color,
    shape = RoundedCornerShape(percent = (radiusFraction * 100).toInt()),
    width = width,
    height = height,
    modifier = modifier
)



@Composable
internal fun SectionCaption(res: Int) {
    Text(
        stringResource(res),
        modifier = Modifier.padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Icon + label header sitting directly above a control: the name of the thing
 * being changed, so a picker inside a group card does not read as anonymous.
 */
@Composable
internal fun LabeledControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallLeadingIcon(icon = icon)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
