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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha


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
                    text = if (hasItems && safeIndex >= 0) items[safeIndex] else "",
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEachIndexed { index, text ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                if (index in items.indices) {
                                    onItemSelected(index)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
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
    val progressFraction = ((sliderPosition - valueRange.start) /
        (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "LabeledSliderProgress"
    )

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

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceContainerHighest)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderPosition,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}
