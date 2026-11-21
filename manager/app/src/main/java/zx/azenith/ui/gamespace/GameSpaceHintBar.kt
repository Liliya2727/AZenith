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

package zx.azenith.ui.gamespace

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R
import zx.azenith.ui.component.gamepadNotFocusable

internal data class GameSpaceHintItem(
    @DrawableRes val glyphRes: Int,
    @StringRes val labelRes: Int,
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
 * Compact console-style button legend pinned to the bottom-center of GameSpace.
 * Purely decorative: never takes focus and never consumes touch events.
 */
@Composable
internal fun GameSpaceHintBar(
    modifier: Modifier = Modifier,
) {
    val screen = GameSpaceHintState.screen
    val focusKind = GameSpaceHintState.focusKind
    val customConfirm = GameSpaceHintState.customConfirmLabelRes
    val visible = GameSpaceHintState.isVisible
    val hints = resolveGameSpaceHints(screen, focusKind, customConfirm)
    val context = LocalContext.current

    LaunchedEffect(screen, focusKind, customConfirm, visible) {
        val labels = hints.joinToString(" | ") { context.getString(it.labelRes) }
        Log.d("PAD", "gshint screen=$screen focus=$focusKind visible=$visible items=[$labels]")
    }

    AnimatedVisibility(
        visible = visible && hints.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        modifier = modifier
            .gamepadNotFocusable()
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        val scheme = MaterialTheme.colorScheme
        val contentTint = scheme.onSurfaceVariant.copy(alpha = 0.82f)
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerHigh.copy(alpha = 0.72f),
            contentColor = contentTint,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                hints.forEach { item ->
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
