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

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ColumnScope
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
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
enum class SheetRowStyle(val isSelectable: Boolean) {
    /** Tap-to-run list. */
    Action(false),

    /** Radio group, one selected row. */
    SingleSelect(true),

    /** Checkbox group, any number selected. */
    MultiSelect(true),
}

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
 * the hand-rolled sheet, so the corner radius, drag handle, blur and inset padding
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
    AZenithSheet(
        show = show,
        onDismiss = onDismiss,
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        style = style,
        onItemClick = onItemClick,
        items = items,
        content = null,
    )
}

/**
 * Variant for sheets whose body is free-form scrolling content (a log list, a
 * rendered changelog) rather than a fixed item list.
 *
 * The caller owns the scroll container and must pad for the navigation bars,
 * since the sheet no longer does that around its own body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AZenithSheetContent(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AZenithSheet(
        show = show,
        onDismiss = onDismiss,
        title = title,
        modifier = modifier,
        subtitle = null,
        style = SheetRowStyle.Action,
        onItemClick = {},
        items = emptyList(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AZenithSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier,
    subtitle: String?,
    style: SheetRowStyle,
    onItemClick: (Int) -> Unit,
    items: List<SheetItem>,
    content: (@Composable ColumnScope.() -> Unit)?,
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
    val sheetSurface = rememberSheetSurface(style)
    val scheme = MaterialTheme.colorScheme

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
                .sheetSurface(isBlurEnabled, hazeState, sheetSurface)
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

            val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            if (content != null) {
                content()
            } else {
                // A long governor or scheduler list is taller than the sheet, so
                // the rows live in a lazy column rather than a Column that
                // measures every child up front.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = navBar + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(items) { index, sheetItem ->
                        // One container for the whole group rather than a pill per
                        // row: the grouping is what makes the sheet read as a radio
                        // group, and per-row pills break it into unrelated items.
                        // Only the outer rows paint the fill, squaring their inner
                        // corners, so the rows read as one rounded surface -- and
                        // they stay lazy, which a single eager item would not.
                        SheetRow(
                            item = sheetItem,
                            style = style,
                            groupFill = sheetSurface.group,
                            paintsGroupFill = true,
                            first = index == 0,
                            last = index == items.lastIndex,
                            onClick = { onItemClick(index) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The sheet's own background and its grouping fill, both derived from the row
 * style rather than passed in.
 *
 * A modal bottom sheet sits at MD3 elevation level 1, and M3 draws elevation as a
 * tint of [androidx.compose.material3.ColorScheme.surfaceTint] over the surface
 * -- that overlay is why an M3 dark surface is a deep tint of the theme rather
 * than the grey/black an OEM wallpaper hands back. The theme here pins its
 * neutral roles to the device surface, so the sheet derives its own ladder from
 * the seed instead of reading `surfaceContainer*` straight, and stays in the
 * theme's hue family whatever the OEM ships.
 *
 * A selectable sheet groups its rows, because that grouping is what makes it read
 * as a radio group, so the rows share one fill a step above the sheet. An action
 * sheet is a plain menu of things you can do, so its rows stay flat on the sheet.
 *
 * Blur replaces the flat fill when enabled, keeping the tint the same either way
 * so toggling blur does not change the sheet's apparent colour.
 */
@Immutable
private data class SheetSurface(val base: Color, val group: Color)

@Composable
private fun rememberSheetSurface(style: SheetRowStyle): SheetSurface {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme, style) {
        val seed = scheme.primary.takeIf { it != scheme.surface } ?: scheme.surfaceTint
        val from = scheme.surfaceContainerLow
        SheetSurface(
            base = from.tonalElevation(seed, TINT_MD3_LEVEL_1),
            group = from.tonalElevation(seed, TINT_MD3_LEVEL_1 + TINT_MD3_LEVEL_1),
        )
    }
}

@Composable
private fun Modifier.sheetSurface(
    isBlurEnabled: Boolean,
    hazeState: HazeState?,
    surface: SheetSurface,
): Modifier {
    val base = surface.base

    return this
        .background(base)
        .then(
            if (isBlurEnabled && hazeState != null) {
                Modifier.hazeBlur(
                    input = HazeInput.Backdrop(hazeState),
                    style = HazeBlurStyle.Material3(
                        containerColor = base.copy(alpha = 0.35f)
                    ) { blurRadius(24.dp) }
                )
            } else Modifier
        )
}

/** MD3 elevation level 1 -- the level a modal bottom sheet occupies. */
private const val TINT_MD3_LEVEL_1 = 0.05f

/** MD3's elevation overlay, expressed as a lerp toward [tint]. */
private fun Color.tonalElevation(tint: Color, strength: Float): Color = Color(
    red = red + (tint.red - red) * strength,
    green = green + (tint.green - green) * strength,
    blue = blue + (tint.blue - blue) * strength,
    alpha = alpha,
)

/** One row, in whichever of the three styles the sheet was opened with. */
@Composable
private fun SheetRow(
    item: SheetItem,
    style: SheetRowStyle,
    groupFill: Color,
    paintsGroupFill: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selectable = style.isSelectable
    val selected = item.selected && selectable

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape(first && paintsGroupFill, last && paintsGroupFill))
            .background(
                when {
                    selected -> scheme.secondaryContainer
                    paintsGroupFill -> groupFill
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = item.enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.icon != null) {
            SmallLeadingIcon(icon = item.icon)
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    !item.enabled -> scheme.onSurfaceVariant
                    selected -> scheme.onSecondaryContainer
                    else -> scheme.onSurface
                },
            )
            if (item.summary != null) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        // The trailing affordance is drawn only where the style calls for one, so
        // an action list carries no column of dead controls.
        when (style) {
            SheetRowStyle.SingleSelect -> RowRadioIndicator(selected = selected)
            SheetRowStyle.MultiSelect -> Checkbox(
                checked = item.selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = scheme.primary,
                    uncheckedColor = scheme.onSurfaceVariant,
                ),
            )
            SheetRowStyle.Action -> if (item.selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * The M3 radio control: an outer ring around a filled centre dot, on a primary
 * colour when selected.
 *
 * A bare filled dot on a bare ring is MD2's radio and the reason the old sheet
 * read as a column of grey dots -- M3 draws the ring around a dot and leaves the
 * whole control clear of the row behind it, so the ring is what the eye reads.
 */
@Composable
private fun RowRadioIndicator(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val ring = if (selected) scheme.primary else scheme.onSurfaceVariant
    val dot = if (selected) scheme.primary else scheme.outline

    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.dp.toPx()
            val ringInset = stroke / 2f
            val center = size.width / 2f

            drawCircle(
                color = ring,
                radius = center - ringInset,
                style = Stroke(width = stroke),
            )
            if (selected) {
                // MD3's selected dot fills 60% of the ring's inner diameter.
                drawCircle(color = dot, radius = center * 0.3f)
            }
        }
    }
}

private val GroupShape = RoundedCornerShape(20.dp)

/**
 * The row's own clip. The outer rows inherit the group's rounded outline; the
 * rows between them stay square so the group reads as one surface.
 */
private fun rowShape(first: Boolean, last: Boolean): RoundedCornerShape = RoundedCornerShape(
    topStart = if (first) 20.dp else 0.dp,
    topEnd = if (first) 20.dp else 0.dp,
    bottomStart = if (last) 20.dp else 0.dp,
    bottomEnd = if (last) 20.dp else 0.dp,
)
