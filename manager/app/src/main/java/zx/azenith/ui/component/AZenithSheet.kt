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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * How a sheet presents its rows.
 *
 * [Action] is a tap-to-run list, [SingleSelect] a radio group, [MultiSelect] a
 * checkbox group. One sheet surface serves all three so callers do not hand-roll
 * a fourth container.
 */
enum class SheetRowStyle { Action, SingleSelect, MultiSelect }

/**
 * One row in [AZenithSheet].
 *
 * [selected] drives the radio/check affordance and is ignored for [SheetRowStyle.Action].
 */
data class SheetItem(
    val label: String,
    val summary: String? = null,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * The app's one bottom sheet.
 *
 * Replaces the hand-rolled sheet in `OptionPickerSheet` and the callers of
 * `CustomBottomSheet`, so the corner radius, drag handle, blur and inset padding
 * are defined once and every sheet matches.
 *
 * Blur follows the `expressive_blur_ui` pref like the rest of the app. The input
 * is [HazeInput.Backdrop], not [HazeInput.Sources]: a `ModalBottomSheet` is its
 * own Dialog window, so the page's source lives in the window behind it and
 * `Sources` resolves to nothing there. `Backdrop` samples whatever is behind the
 * window and falls back to the sources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AZenithSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: SheetRowStyle = SheetRowStyle.Action,
    onItemClick: (Int) -> Unit = {},
    items: List<SheetItem> = emptyList(),
) {
    if (!show) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsPrefs = remember(context) {
        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }
    val isBlurEnabled = settingsPrefs.getBoolean("expressive_blur_ui", false)
    val hazeState = LocalAppHazeState.current
    val sheetState = rememberModalBottomSheetState()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        shape = shape,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (isBlurEnabled && hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Backdrop(hazeState),
                            style = HazeBlurStyle.Material3(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                            ) { blurRadius(24.dp) }
                        )
                    } else Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                )
        ) {
            // Drawn rather than Material's own dragHandle: the stock handle is a
            // pill sized for the stock sheet shape and does not sit right against
            // this sheet's wider corner radius.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
                )
            }

            // A long governor or scheduler list is taller than the sheet, so the
            // rows live in a lazy column rather than a Column that measures every
            // child up front.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items) { item ->
                    SheetRow(
                        item = item,
                        style = style,
                        onClick = { onItemClick(items.indexOf(item)) },
                    )
                }
            }
        }
    }
}

/**
 * One row, in whichever of the three styles the sheet was opened with.
 *
 * The trailing affordance is omitted entirely for [SheetRowStyle.Action] rather
 * than drawn disabled, so an action list does not carry a column of grey radios.
 */
@Composable
private fun SheetRow(
    item: SheetItem,
    style: SheetRowStyle,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (style == SheetRowStyle.SingleSelect && item.selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else Color.Transparent
            )
            .clickable(enabled = item.enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.icon != null) {
            SmallLeadingIcon(icon = item.icon)
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.selected && style != SheetRowStyle.Action) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = when {
                    !item.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    item.selected && style != SheetRowStyle.Action -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (item.summary != null) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (style) {
            SheetRowStyle.SingleSelect -> RadioButton(
                selected = item.selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accent,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            SheetRowStyle.MultiSelect -> Checkbox(
                checked = item.selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = accent),
            )
            SheetRowStyle.Action -> if (item.selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}