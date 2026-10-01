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


import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import zx.azenith.ExpressiveShapes


private val largeCorner = 26.dp
private val smallCorner = 4.dp

private val topShape = RoundedCornerShape(
    topStart = largeCorner,
    topEnd = largeCorner,
    bottomStart = smallCorner,
    bottomEnd = smallCorner
)
private val middleShape = RoundedCornerShape(smallCorner)
private val bottomShape = RoundedCornerShape(
    topStart = smallCorner,
    topEnd = smallCorner,
    bottomStart = largeCorner,
    bottomEnd = largeCorner
)
private val singleShape = RoundedCornerShape(largeCorner)

@Composable
fun ExpressiveList(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Column(
            modifier = Modifier.clip(
                if (content.size == 1) singleShape else RoundedCornerShape(largeCorner)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }
                Column(
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), shape)
                ) {
                    itemContent()
                }
            }
        }
    }
}

@Composable
fun <T> ExpressiveLazyList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(all = 16.dp),
    title: String = "",
    key: ((T) -> Any)? = null,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = contentPadding
        ) {
            itemsIndexed(
                items = items,
                key = if (key != null) { _, item -> key(item) } else null
            ) { index, item ->
                val shape = when {
                    items.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == items.lastIndex -> bottomShape
                    else -> middleShape
                }
                Column(
                    modifier = Modifier

                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    itemContent(item)
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveListItem(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (onClick != null || onLongClick != null) {
                    it.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                } else {
                    it
                }
            }
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingContent()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            headlineContent()
            if (supportingContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.outline
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        supportingContent()
                    }
                }
            }
        }
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    trailingContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveListItemHighlight(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent, 
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor) 
            .let {
                if (onClick != null || onLongClick != null) {
                    it.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                } else {
                    it
                }
            }
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingContent()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            headlineContent()
            if (supportingContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.outline
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        supportingContent()
                    }
                }
            }
        }
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    trailingContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveInfoCard(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent, 
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor) 
            .let {
                if (onClick != null || onLongClick != null) {
                    it.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                } else {
                    it
                }
            }
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingContent()
            }
        }
        

        if (supportingContent != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.outline
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                        supportingContent()
                    }
                }
            }
        } else {

            Spacer(modifier = Modifier.weight(1f))
        }
        
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    trailingContent()
                }
            }
        }
    }
}


@Composable
fun ExpressiveSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ExpressiveListItem(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            role = Role.Switch,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        ),
        headlineContent = { Text(title) },
        leadingContent = icon?.let { { LeadingIcon(icon = it, contentDescription = title) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                thumbContent = {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                }, 
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

/**
 * How [ExpressiveDropdownItem] presents its options.
 *
 * [Sheet] suits the longer option lists (governors, schedulers, refresh rates)
 * where an anchored popup either scrolls off-screen or gets clipped by the row.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    enabled: Boolean = true,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val hasItems = items.isNotEmpty()
    val safeIndex = if (hasItems) {
        selectedIndex.coerceIn(0, items.lastIndex)
    } else {
        -1
    }
    val selectedLabel = if (hasItems && safeIndex >= 0) items[safeIndex] else ""

    ExpressiveListItem(
        modifier = Modifier
            .animateContentSize()
            .then(
                if (enabled) {
                    Modifier.clickable { expanded = true }
                } else {
                    Modifier
                }
            ),
        leadingContent = icon?.let { { LeadingIcon(icon = it, contentDescription = title) } },
        headlineContent = { Text(text = title) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                Text(
                    text = selectedLabel,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

            }
        }
    )

    if (hasItems) {
        OptionPickerSheet(
            show = expanded,
            title = title,
            subtitle = summary,
            items = items,
            selectedIndex = safeIndex,
            accent = MaterialTheme.colorScheme.primary,
            onSelect = { index ->
                if (index in items.indices) {
                    onItemSelected(index)
                }
            },
            onDismiss = { expanded = false },
        )
    }
}

/**
 * One option row in [OptionPickerSheet].
 *
 * The press state is local to the row and the animated scale is read inside
 * [Modifier.graphicsLayer], so a press animates the draw phase only and the
 * sheet's other rows are not invalidated with it. Scaling the layer rather
 * than the modifier keeps the touch target at full size, which is what
 * stops a fast tap from cancelling the gesture mid-animation.
 */
@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "OptionRowScale",
    )
    val color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "OptionRowColor",
    )

    ExpressiveListItemHighlight(
        onClick = onClick,
        containerColor = color,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(ExpressiveShapes.large),
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = accent,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    )
}

/**
 * The option list for an option-picker selection.
 *
 * Plain radio rows read as a settings dump, so the selected option is
 * surfaced as a filled container instead — the same emphasis Material gives
 * a chosen list item — and the check mark sits on the trailing edge, which
 * keeps the text column aligned with the rest of the settings list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionPickerSheet(
    show: Boolean,
    title: String,
    subtitle: String?,
    items: List<String>,
    selectedIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // ModalBottomSheet is a real Dialog window, so the window manager lays it out
    // against the display instead of the caller's slot. A sheet hand-assembled
    // from AnimatedVisibility + fillMaxSize() -- or from a Popup -- inherits the
    // caller's constraints, and for a list row that is the row itself: the scrim
    // and the options get measured into the row and the sheet reads as inline with
    // the toggle rather than overlaying the app. The Popup variant was tried and
    // does not fix it either -- a Popup is still positioned by its anchor.
    if (!show) return

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
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

            // Title and subtitle stay outside the scrolling list so they do not
            // scroll away with the options.
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                )
            }

            // A long governor or scheduler list is taller than the sheet, so the
            // rows live in their own lazy column rather than a Column that measures
            // every child up front. heightIn caps the list at the point where the
            // whole set is still scannable, and -- unlike the weight(1f, fill=false)
            // this replaced -- needs no unbounded parent constraint to resolve.
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(items) { index, text ->
                        OptionRow(
                            text = text,
                            selected = index == selectedIndex,
                            accent = accent,
                            onClick = {
                                onSelect(index)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ExpressiveRadioItem(
    title: String,
    summary: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ExpressiveListItem(
        onClick = onClick,
        modifier = Modifier.toggleable(
            value = selected,
            onValueChange = { onClick() },
            enabled = enabled,
            role = Role.RadioButton
        ),
        headlineContent = { Text(title) },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun ExpressiveCheckboxItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ExpressiveListItem(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            role = Role.Checkbox,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        ),
        headlineContent = { Text(title) },
        leadingContent = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource,
                modifier = Modifier.size(24.dp)
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun ExpressiveColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Column(
            modifier = Modifier.clip(
                if (content.size == 1) singleShape else RoundedCornerShape(largeCorner)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }
                Column(
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), shape)
                ) {
                    itemContent()
                }
            }
        }
    }
}

@Composable
fun LeadingIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = contentColor
        )
    }
}

@Composable
fun SmallLeadingIcon(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cs.primary.copy(alpha = 0.12f),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier
                .padding(7.dp)
                .size(22.dp)
        )
    }
}

/**
 * Renders the gradient progress track behind a [Slider].
 *
 * The animated progress is read inside [drawBehind] rather than in the
 * composable body: the track is the only thing that changes while dragging, so
 * reading the animation in the draw phase keeps the row's title, subtitle and
 * badge out of the per-frame recomposition.
 */
@Composable
fun SliderTrack(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    brush: Brush,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .drawBehind {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(color = trackColor, cornerRadius = corner)
                val fraction = progress().coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRoundRect(
                        brush = brush,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = corner
                    )
                }
            }
    )
}

private val trackHeight = 8.dp

@Composable
fun ExpressiveSliderItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    badgeText: String,
    sliderPosition: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(durationMillis = 300),
        label = "SliderEnabledAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(animatedAlpha)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (icon != null) {
                    LeadingIcon(icon = icon, contentDescription = title)
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            }

            Surface(
                color = colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        ZenithSlider(
            value = sliderPosition,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )
    }
}
