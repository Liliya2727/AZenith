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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zx.azenith.ui.util.PropertyUtils

data class SettingsUiState(
    val disableTweak: Boolean = false,
    val stateToast: Boolean = false,
    val autoMode: Boolean = false,
    val debugMode: Boolean = false,
    val profileTimeout: Boolean = false,
    val isLoaded: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProps()
    }

    private fun loadProps() {
        viewModelScope.launch(Dispatchers.IO) {
            // Semua ini reflection call (PropertyUtils), bukan shell fork
            val disableTweak = PropertyUtils.get("persist.sys.azenith.disabletweak") == "1"
            val stateToast = PropertyUtils.get("persist.sys.azenithconf.showtoast") == "1"
            val autoMode = PropertyUtils.get("persist.sys.azenithconf.AIenabled") == "0"
            val debugMode = PropertyUtils.get("persist.sys.azenith.debugmode") == "true"
            val profileTimeout = PropertyUtils.get("persist.sys.azenith.dropforeground") == "1"

            _uiState.value = SettingsUiState(
                disableTweak = disableTweak,
                stateToast = stateToast,
                autoMode = autoMode,
                debugMode = debugMode,
                profileTimeout = profileTimeout,
                isLoaded = true
            )
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
        viewModelScope.launch(Dispatchers.IO) {
            PropertyUtils.set("persist.sys.azenithconf.AIenabled", state)
            // ini tetap shell karena nulis ke file, bukan cuma prop
            Shell.cmd("echo $state > /data/adb/.config/AZenith/API/current_modes").submit()
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
