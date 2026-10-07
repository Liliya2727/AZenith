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

@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.viewmodel.ApplistViewmodel

// Expand/collapse is an emphasis moment: width overshoots ~20% then settles.
private val BounceSizeSpring: FiniteAnimationSpec<IntSize> = spring(
    dampingRatio = 0.45f,
    stiffness = 500f
)

// The trailing check pops past 1x on the way in and squashes on the way out.
private val BouncePopSpring: FiniteAnimationSpec<Float> = spring(
    dampingRatio = 0.4f,
    stiffness = 700f
)

/**
 * The app list's filter sheet: one toggle per filter, combinable across groups.
 * The system row is exclusive (show all / only system / hide); every other
 * toggle is independent, so e.g. Games + Disabled yields the disabled games.
 *
 * Each option carries the count it would show if selected, computed from the
 * same predicate as the list itself, so the sheet previews the combination
 * before the tap.
 */
@Composable
fun AppFilterSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: ApplistViewmodel,
    onSystemFilterChange: (ApplistViewmodel.SystemFilter) -> Unit,
    onReset: () -> Unit,
) {
    if (!show) return

    val gamesCount by remember(viewModel) { derivedStateOf { viewModel.countIf(games = true) } }
    val enabledCount by remember(viewModel) { derivedStateOf { viewModel.countIf(enabled = true) } }
    val disabledCount by remember(viewModel) { derivedStateOf { viewModel.countIf(disabled = true) } }
    val shownCount = viewModel.filteredApps.size

    AZenithSheetContent(
        show = show,
        onDismiss = onDismiss,
        title = stringResource(R.string.filter_sheet_title),
        subtitle = stringResource(R.string.filter_sheet_subtitle),
    ) {
        val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val scheme = MaterialTheme.colorScheme
        val motion = MaterialTheme.motionScheme

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = navBar + 16.dp)
        ) {
            // Live result chip: the one number that says whether the current
            // combination is what the user wants.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = scheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        AnimatedContent(
                            targetState = shownCount,
                            transitionSpec = { odometerSpecs(motion) },
                            label = "shownCount"
                        ) { n ->
                            Text(
                                text = stringResource(R.string.filter_sheet_count, n),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterGroup(title = stringResource(R.string.filter_group_type)) {
                FilterToggleButton(
                    label = stringResource(R.string.filter_games),
                    selected = viewModel.filterGames,
                    count = gamesCount,
                    onClick = { viewModel.filterGames = !viewModel.filterGames }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterGroup(title = stringResource(R.string.filter_group_service)) {
                FilterToggleButton(
                    label = stringResource(R.string.filter_enabled),
                    selected = viewModel.filterEnabled,
                    count = enabledCount,
                    onClick = { viewModel.filterEnabled = !viewModel.filterEnabled }
                )
                FilterToggleButton(
                    label = stringResource(R.string.filter_disabled),
                    selected = viewModel.filterDisabled,
                    count = disabledCount,
                    onClick = { viewModel.filterDisabled = !viewModel.filterDisabled }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FilterGroup(title = stringResource(R.string.filter_group_system)) {
                SystemSegmentRow(
                    current = viewModel.systemFilter,
                    onSelect = onSystemFilterChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reset))
                }
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

/**
 * Odometer swap for a number: digits roll upward on the expressive spatial
 * spring while the fade rides the effects spring, per the M3 motion scheme.
 */
private fun odometerSpecs(motion: MotionScheme): ContentTransform =
    (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 2 }) togetherWith
        (fadeOut(motion.defaultEffectsSpec()) + slideOutVertically(motion.defaultSpatialSpec()) { -it / 2 })

/**
 * A titled group card: the label sits on the sheet, the options share one
 * rounded surface a step above it -- the same grouping the app's list cards use.
 */
@Composable
private fun FilterGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/**
 * The exclusive system choice as a segmented control: three segments read as
 * one radio group, which pills do not, and M3's own shape animation carries
 * the selection move for free.
 */
@Composable
private fun SystemSegmentRow(
    current: ApplistViewmodel.SystemFilter,
    onSelect: (ApplistViewmodel.SystemFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        R.string.filter_system_show to ApplistViewmodel.SystemFilter.SHOW_ALL,
        R.string.filter_system_only to ApplistViewmodel.SystemFilter.ONLY_SYSTEM,
        R.string.filter_system_hide to ApplistViewmodel.SystemFilter.HIDE_SYSTEM,
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (labelRes, value) ->
            SegmentedButton(
                selected = current == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/**
 * One filter option as a stock M3 filter chip: transparent + outline-variant
 * at rest, secondaryContainer/onSecondaryContainer selected (the tokens'
 * own pairing), 32dp tall with the theme's ripple. The check rides trailing
 * so selecting only grows the right edge -- a leading check shoves the label
 * sideways, which reads as the chip jumping. Container/label/outline colours
 * and the width change all animate on the theme's expressive springs instead
 * of fixed durations, the way material3 animates its own components.
 */
@Composable
fun FilterToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val container by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "chipContent"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) Color.Transparent else scheme.outlineVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "chipOutline"
    )

    // Squash-and-pop per toggle; first composition skipped so the sheet opens still.
    val toggleScale = remember { Animatable(1f) }
    var bounceArmed by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (bounceArmed) {
            toggleScale.snapTo(0.94f)
            toggleScale.animateTo(1f, BouncePopSpring)
        } else {
            bounceArmed = true
        }
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, color = content)
                if (count != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = count,
                        transitionSpec = { odometerSpecs(motion) },
                        label = "toggleCount"
                    ) { n ->
                        Text(
                            text = n.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = content.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(
                    animationSpec = BouncePopSpring,
                    initialScale = 0.4f
                ) + fadeIn(motion.defaultEffectsSpec()),
                exit = scaleOut(
                    animationSpec = BouncePopSpring,
                    targetScale = 0.4f
                ) + fadeOut(motion.defaultEffectsSpec())
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = toggleScale.value
                scaleY = toggleScale.value
            }
            .animateContentSize(BounceSizeSpring),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            labelColor = content,
            iconColor = content,
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedTrailingIconColor = content
        ),
        border = BorderStroke(1.dp, outline)
    )
}
