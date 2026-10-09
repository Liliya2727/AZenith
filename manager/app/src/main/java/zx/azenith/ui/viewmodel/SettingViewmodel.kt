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

package zx.azenith.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.TapHapticState
import zx.azenith.ui.util.fireConfirmHaptic
import zx.azenith.ui.util.isTapHapticEnabled
import zx.azenith.ui.util.setTapHapticEnabled

data class SettingsUiState(
    val disableTweak: Boolean = false,
    val stateToast: Boolean = false,
    val tapHaptic: Boolean = true,
    val autoMode: Boolean = false,
    val debugMode: Boolean = false,
    val profileTimeout: Boolean = false,
    val profileNotifications: Boolean = false,
    val isLoaded: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProps()
    }

    private fun loadProps() {
        viewModelScope.launch(Dispatchers.IO) {
            val disableTweak = PropertyUtils.get("persist.sys.azenith.disabletweak") == "1"
            val stateToast = PropertyUtils.get("persist.sys.azenithconf.showtoast") == "1"
            val tapHaptic = isTapHapticEnabled(getApplication())
            val autoMode = PropertyUtils.get("persist.sys.azenithconf.AIenabled") == "0"
            val debugMode = PropertyUtils.get("persist.sys.azenith.debugmode") == "true"
            val profileTimeout = PropertyUtils.get("persist.sys.azenith.dropforeground") == "1"
            val profileNotifications = PropertyUtils.get("persist.sys.azenith.profilenotifications") == "1"
    
            TapHapticState.enabled.value = tapHaptic
            _uiState.value = SettingsUiState(
                disableTweak = disableTweak,
                stateToast = stateToast,
                tapHaptic = tapHaptic,
                autoMode = autoMode,
                debugMode = debugMode,
                profileTimeout = profileTimeout,
                profileNotifications = profileNotifications,
                isLoaded = true
            )
        }
    }

    /** UI-only preference, so it is stored in SharedPreferences, not a system property. */
    fun setTapHaptic(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(tapHaptic = enabled)
        setTapHapticEnabled(getApplication(), enabled)
    }

    /**
     * The toggle owns its own feedback instead of leaning on the window hook. Turning it on pulses
     * through the decor view, which the hook cannot do because the flag is still false while this
     * runs; turning it off stays silent, and [TapHapticState.suppressTap] stands the hook down so
     * switching feedback off cannot be the one action that still buzzes.
     */
    fun onTapHapticToggle(enabled: Boolean) {
        if (enabled) fireConfirmHaptic(getApplication())
        else TapHapticState.suppressTap = true
        setTapHaptic(enabled)
    }
    
    fun setProfileNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(profileNotifications = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            val flag = if (enabled) "-sn" else "-hn"
            PropertyUtils.set("persist.sys.azenith.profilenotifications", if (enabled) "1" else "0")
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service $flag").submit()            
        }
    }
    
    fun setShowToast(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(stateToast = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.showtoast", if (enabled) "1" else "0")
        }
    }

    fun setAutoMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoMode = enabled)
        val state = if (enabled) "0" else "1"
        // Home and Settings are separate ViewModel instances, so the home
        // screen cannot see this state change on its own. Broadcast it.
        AutoModeBus.notify(state)
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.AIenabled", state)
            RootUtils.writeRootFile("/data/adb/.config/AZenith/API/current_modes", "$state\n")
        }
    }

    fun setDebugMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(debugMode = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.debugmode", if (enabled) "true" else "false")
        }
    }

    fun setDisableTweak(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(disableTweak = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.disabletweak", if (enabled) "1" else "0")
        }
    }

    fun setProfileTimeout(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(profileTimeout = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenith.dropforeground", if (enabled) "1" else "0")
        }
    }
}
