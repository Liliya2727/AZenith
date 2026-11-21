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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import zx.azenith.ui.component.GamepadInput

/** Active GameSpace stage / tab driving context-sensitive button hints. */
internal enum class GameSpaceScreenId {
    Welcome,
    WelcomeExpanded,
    Game,
    Library,
    LibraryExpanded,
    WrapToFirst,
    Settings,
}

/** Focused control kind inside GameSpace, when observable from focus callbacks. */
internal enum class GameSpaceFocusKind {
    None,
    RibbonTile,
    BentoTile,
    PlayButton,
    WallpaperButton,
    AppSettingsButton,
    LibraryFilter,
    LibraryTile,
    SettingsTab,
    SettingsRow,
    Slider,
}

/**
 * GameSpace-only observable state for the bottom [GameSpaceHintBar].
 *
 * Mutations must happen only from side effects / callbacks (never during
 * composition). Every slot tracks its last writer token so a late blur or
 * screen exit cannot clear a newer owner's state.
 */
internal object GameSpaceHintState {
    var screen: GameSpaceScreenId by mutableStateOf(GameSpaceScreenId.Welcome)
        private set

    var focusKind: GameSpaceFocusKind by mutableStateOf(GameSpaceFocusKind.None)
        private set

    var customConfirmLabelRes: Int? by mutableStateOf(null)
        private set

    var overlayOpen: Boolean by mutableStateOf(false)
        private set

    private var screenOwner: Any? = null
    private var focusOwner: Any? = null
    private var overlayOwner: Any? = null

    fun setScreen(owner: Any, next: GameSpaceScreenId) {
        screenOwner = owner
        if (screen != next) screen = next
    }

    fun clearScreen(owner: Any) {
        if (screenOwner === owner) {
            screenOwner = null
            screen = GameSpaceScreenId.Welcome
        }
    }

    fun setFocus(owner: Any, kind: GameSpaceFocusKind, confirmLabelRes: Int? = null) {
        focusOwner = owner
        if (focusKind != kind) focusKind = kind
        if (customConfirmLabelRes != confirmLabelRes) customConfirmLabelRes = confirmLabelRes
    }

    fun clearFocus(owner: Any) {
        if (focusOwner === owner) {
            focusOwner = null
            focusKind = GameSpaceFocusKind.None
            customConfirmLabelRes = null
        }
    }

    fun setOverlayOpen(owner: Any, open: Boolean) {
        overlayOwner = owner
        if (overlayOpen != open) overlayOpen = open
    }

    fun clearOverlay(owner: Any) {
        if (overlayOwner === owner) {
            overlayOwner = null
            overlayOpen = false
        }
    }

    val isVisible: Boolean
        get() = (GamepadInput.controllerConnected || GamepadInput.dpadMode) &&
            !overlayOpen
}
