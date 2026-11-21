/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package zx.azenith.ui.gamespace

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.component.gamepadNotFocusable

internal data class GameSpaceHintItem(
    @param:DrawableRes val glyphRes: Int,
    @param:StringRes val labelRes: Int,
)

/**
 * Maps the active GameSpace screen + focused control to the buttons that are
 * genuinely handled on that screen. L1/R1, Select, L2, and R2 are omitted
 * because `ZXGameSpaceTopBar` already renders badges for them.
 */
internal fun resolveGameSpaceHints(
    screen: GameSpaceScreenId,
    focusKind: GameSpaceFocusKind,
    @StringRes customConfirmLabelRes: Int?,
): List<GameSpaceHintItem> = when (screen) {
    GameSpaceScreenId.Game -> buildList {
        when (focusKind) {
            GameSpaceFocusKind.PlayButton ->
                add(GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_play))
            GameSpaceFocusKind.WallpaperButton ->
                add(GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_wallpaper))
            GameSpaceFocusKind.AppSettingsButton ->
                add(GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_tuning))
            else -> Unit
        }
        add(GameSpaceHintItem(R.drawable.ic_gp_triangle, R.string.gamespace_hint_tuning))
        add(GameSpaceHintItem(R.drawable.ic_gp_square, R.string.gamespace_hint_wallpaper))
        add(GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back))
    }

    GameSpaceScreenId.WrapToFirst -> listOf(
        GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_home),
        GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back),
    )

    GameSpaceScreenId.Welcome -> listOf(
        GameSpaceHintItem(R.drawable.ic_gp_triangle, R.string.gamespace_hint_open),
        GameSpaceHintItem(R.drawable.ic_gp_square, R.string.gamespace_hint_wallpaper),
        GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_exit),
    )

    GameSpaceScreenId.WelcomeExpanded -> listOf(
        GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_select),
        GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back),
    )

    GameSpaceScreenId.Library -> listOf(
        GameSpaceHintItem(R.drawable.ic_gp_triangle, R.string.gamespace_hint_open),
        GameSpaceHintItem(R.drawable.ic_gp_square, R.string.gamespace_hint_wallpaper),
        GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back),
    )

    GameSpaceScreenId.LibraryExpanded -> listOf(
        GameSpaceHintItem(
            R.drawable.ic_gp_cross,
            customConfirmLabelRes ?: if (focusKind == GameSpaceFocusKind.LibraryTile) {
                R.string.gamespace_hint_open
            } else {
                R.string.gamespace_hint_select
            }
        ),
        GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back),
    )

    GameSpaceScreenId.Settings -> buildList {
        if (focusKind != GameSpaceFocusKind.Slider) {
            add(GameSpaceHintItem(R.drawable.ic_gp_cross, customConfirmLabelRes ?: R.string.gamespace_hint_select))
        }
        add(GameSpaceHintItem(R.drawable.ic_gp_circle, R.string.gamespace_hint_back))
    }
}

/**
 * Compact console-style button legend placed in `ZXGameSpaceTopBar` to the
 * right of the `AZenith [GAME SPACE]` wordmark (or directly beside the category
 * tabs while the wordmark is hidden). Uses the same spring geometry as the
 * wordmark so its horizontal position glides smoothly as the wordmark appears
 * or hides. Purely decorative: never takes focus and never consumes touches.
 */
@Composable
internal fun GameSpaceHintBar(
    modifier: Modifier = Modifier,
    isLogoVisible: Boolean = false,
    canFitInTopBar: Boolean = true,
    onWidthMeasured: ((Int) -> Unit)? = null,
) {
    val screen = GameSpaceHintState.screen
    val focusKind = GameSpaceHintState.focusKind
    val customConfirm = GameSpaceHintState.customConfirmLabelRes
    val visible = GameSpaceHintState.isVisible && canFitInTopBar
    val hints = resolveGameSpaceHints(screen, focusKind, customConfirm)
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme

    LaunchedEffect(screen, focusKind, customConfirm, visible, isLogoVisible) {
        val labels = hints.joinToString(" | ") { context.getString(it.labelRes) }
        Log.d("PAD", "gshint screen=$screen focus=$focusKind visible=$visible logo=$isLogoVisible items=[$labels]")
    }

    val leadingGap by animateDpAsState(
        targetValue = if (isLogoVisible) 10.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "HintBarLeadingGap",
    )

    AnimatedVisibility(
        visible = visible && hints.isNotEmpty(),
        enter = fadeIn(animationSpec = motion.defaultEffectsSpec()) +
            slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialOffsetX = { -it / 3 },
            ) +
            expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                expandFrom = Alignment.Start,
            ),
        exit = fadeOut(animationSpec = motion.defaultEffectsSpec()) +
            slideOutHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                targetOffsetX = { -it / 3 },
            ) +
            shrinkHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                shrinkTowards = Alignment.Start,
            ),
        modifier = modifier.gamepadNotFocusable(),
    ) {
        val scheme = MaterialTheme.colorScheme
        val contentTint = scheme.onSurfaceVariant.copy(alpha = 0.85f)
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerHigh.copy(alpha = 0.78f),
            contentColor = contentTint,
            modifier = Modifier
                .padding(start = leadingGap)
                .height(36.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                )
                .then(
                    if (onWidthMeasured != null) {
                        Modifier.onGloballyPositioned { coords ->
                            if (coords.size.width > 0) onWidthMeasured(coords.size.width)
                        }
                    } else {
                        Modifier
                    }
                ),
        ) {
            AnimatedContent(
                targetState = hints,
                transitionSpec = {
                    fadeIn(animationSpec = motion.defaultEffectsSpec())
                        .togetherWith(fadeOut(animationSpec = motion.defaultEffectsSpec()))
                },
                label = "GameSpaceHintItems",
            ) { currentHints ->
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    currentHints.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                painter = painterResource(item.glyphRes),
                                contentDescription = null,
                                tint = contentTint,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = contentTint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
