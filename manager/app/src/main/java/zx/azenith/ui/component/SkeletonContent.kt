/*
 * Copyright (C) 2026-2027 KowX
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Opacity every skeleton block in the current subtree shares.
 *
 * Defaults to 1 so a block placed outside a [SkeletonContent] still draws at
 * full strength rather than disappearing.
 */
private val LocalSkeletonAlpha = compositionLocalOf { 1f }

/**
 * Wraps a loading placeholder and runs one shimmer for the whole subtree.
 *
 * All the blocks inside read the same animated alpha, so they brighten and
 * dim together. Giving each block its own [rememberInfiniteTransition] would
 * give each one its own clock; the blocks would then be at different points in
 * the cycle and the group would read as flicker rather than as one surface.
 */
@Composable
fun SkeletonContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "SkeletonShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonShimmerAlpha"
    )
    CompositionLocalProvider(LocalSkeletonAlpha provides alpha) {
        Box(modifier = modifier) { content() }
    }
}

/**
 * Placeholder block shaped like a real list row, used while a screen's content
 * is still being read.
 *
 * This is the app's loading pattern: the screen keeps its own layout and the
 * content is stood in for by blocks in the positions the real rows will
 * occupy, so it reads as "this is arriving" rather than "there is nothing to
 * show".
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    val alpha = LocalSkeletonAlpha.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha))
    )
}

/** A circle standing in for an avatar or icon. */
@Composable
fun SkeletonCircle(modifier: Modifier = Modifier) {
    SkeletonBlock(modifier = modifier, shape = CircleShape)
}

/**
 * Skeleton for a list row: leading circle, then a title and a subtitle of
 * different widths, plus a trailing pill.
 */
@Composable
fun SkeletonListRow(
    modifier: Modifier = Modifier,
    circleSize: Int = 44
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonCircle(modifier = Modifier.size(circleSize.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(15.dp)
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(12.dp)
            )
        }
        SkeletonBlock(
            modifier = Modifier
                .width(38.dp)
                .height(20.dp),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Skeleton for a settings-style screen: full-width rows, each a labelled line
 * with a trailing control. This is the shape most subscreens take once their
 * values have loaded.
 */
@Composable
fun SkeletonSettingsList(
    modifier: Modifier = Modifier,
    rowCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SkeletonCircle(modifier = Modifier.size(40.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(11.dp)
                    )
                }
                SkeletonBlock(
                    modifier = Modifier
                        .width(46.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

/**
 * Skeleton for the large preview tile the theme and scheme screens show above
 * their controls.
 */
@Composable
fun SkeletonPreviewTile(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(210.dp),
            shape = RoundedCornerShape(26.dp)
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(15.dp)
        )
    }
}
