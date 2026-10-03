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

package zx.azenith.ui.viewmodel


import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.isBannerImageEnabled
import zx.azenith.ui.util.PropertyUtils

enum class RootStatusState { Checking, Granted, NotGranted }


data class HomeUiState(
    val isBannerEnabled: Boolean = false,
    val moduleInstalled: Boolean = false,
    val autoMode: String? = null,
    val rootStatus: Boolean = false,
    val rootStatusState: RootStatusState = RootStatusState.Checking,
    val serviceStatusRes: Int = R.string.status_initializing,
    val servicePid: String = "",
    val currentProfileRes: Int = R.string.status_initializing,
    val currentProfileValue: String = "",
    val isProfileApplying: Boolean = false,
    val pendingProfileValue: String = "",
    val runningGamePkg: String? = null,
    val runningGameStartTime: String? = null
)


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enable_banner_image") {
            _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        
        observeRootUtils()
        fetchInitialSystemData()
    }

    private fun observeRootUtils() {
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeServiceStatusRes().collect { (statusRes, pid) ->
                _uiState.value = _uiState.value.copy(serviceStatusRes = statusRes, servicePid = pid)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeProfileRes().collect { profileRes ->
                _uiState.value = _uiState.value.copy(currentProfileRes = profileRes)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeProfileValue().collect { value ->
                _uiState.value = _uiState.value.copy(
                    currentProfileValue = value,
                    // The daemon has confirmed the switch, so the spinner has done its job.
                    // Resolving on value rather than on the call returning is what keeps it
                    // honest: applyProfile returns before the profile actually changes.
                    isProfileApplying = if (_uiState.value.isProfileApplying && value == _uiState.value.pendingProfileValue) {
                        false
                    } else {
                        _uiState.value.isProfileApplying
                    }
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            AutoModeBus.changes.collect { mode ->
                _uiState.value = _uiState.value.copy(autoMode = mode)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeAutoMode().collect { mode ->
                _uiState.value = _uiState.value.copy(autoMode = mode)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeGameInfo().collect { info ->
                _uiState.value = _uiState.value.copy(
                    runningGamePkg = info.pkg,
                    runningGameStartTime = info.startTime
                )
            }
        }
    }


    private fun fetchInitialSystemData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRooted = RootUtils.requestRootAccess()
            val isModuleInstalled = RootUtils.isModuleInstalled()
            val mode = PropertyUtils.get("persist.sys.azenithconf.AIenabled")

            _uiState.value = _uiState.value.copy(
                rootStatus = isRooted,
                rootStatusState = if (isRooted) RootStatusState.Granted else RootStatusState.NotGranted,
                moduleInstalled = isModuleInstalled,
                autoMode = mode
            )
        }
    }

    fun refreshRootStatus(onResult: (changed: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val isRooted = RootUtils.requestRootAccess()
            val isModuleInstalled = RootUtils.isModuleInstalled()
            val before = _uiState.value
            val after = before.copy(
                rootStatus = isRooted,
                rootStatusState = if (isRooted) RootStatusState.Granted else RootStatusState.NotGranted,
                moduleInstalled = isModuleInstalled
            )
            _uiState.value = after
            viewModelScope.launch(Dispatchers.Main) {
                onResult(before.rootStatusState != after.rootStatusState ||
                        before.moduleInstalled != after.moduleInstalled)
            }
        }
    }

    fun applyProfile(profileReason: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isProfileApplying = true,
                pendingProfileValue = profileReason
            )
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p $profileReason").submit()
            viewModelScope.launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun rebootDevice(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = when (reason) {
                "" -> "svc power reboot"
                "soft_reboot" -> "killall system_server"
                "recovery" -> "/system/bin/input keyevent 26 && svc power reboot $reason || reboot $reason"
                else -> "svc power reboot $reason || reboot $reason"
            }
            Shell.cmd(cmd).submit()
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
    


    fun refreshAiMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = PropertyUtils.get("persist.sys.azenithconf.AIenabled")
            _uiState.value = _uiState.value.copy(autoMode = mode)
        }
    }

}

