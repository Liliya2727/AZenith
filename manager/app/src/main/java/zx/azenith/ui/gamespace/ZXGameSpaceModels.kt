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

import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.view.InputDevice
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import zx.azenith.R
import zx.azenith.ui.viewmodel.ApplistViewmodel

internal enum class TopCategory(val labelRes: Int) {
    Games(R.string.gamespace_tab_games),
    Settings(R.string.settings)
}

internal sealed interface RibbonEntry {
    val key: String

    data object Welcome : RibbonEntry {
        override val key: String = "__welcome__"
    }

    data class Game(val app: ApplistViewmodel.AppInfo) : RibbonEntry {
        override val key: String = app.packageName
    }

    data object Library : RibbonEntry {
        override val key: String = "__library__"
    }
}

internal enum class SettingsSidebarTab(
    val labelRes: Int,
    val icon: ImageVector
) {
    General(R.string.gamespace_settings_general, Icons.Rounded.Tune),
    Tweaks(R.string.nav_tweaks, Icons.Rounded.Bolt),
    Appearance(R.string.appearance, Icons.Rounded.Palette),
    About(R.string.section_about, Icons.Rounded.Info)
}

internal data class SettingsSearchItem(
    val titleRes: Int,
    val summaryRes: Int? = null,
    val summaryText: String? = null,
    val tab: SettingsSidebarTab,
    val icon: ImageVector
)

internal val SETTINGS_SEARCH_ITEMS = listOf(
    // General
    SettingsSearchItem(
        titleRes = R.string.show_notifications,
        summaryText = "Post status notifications when active profile switches",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.NotificationsActive
    ),
    SettingsSearchItem(
        titleRes = R.string.tap_haptic_feedback,
        summaryText = "Tactile impulse feedback on interactive taps and toggles",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Vibration
    ),
    SettingsSearchItem(
        titleRes = R.string.show_toast,
        summaryText = "Display brief toast notifications on profile and state switches",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Info
    ),
    SettingsSearchItem(
        titleRes = R.string.allow_verbose_log,
        summaryText = "Write extended debug diagnostics to /data/adb/.config/AZenith",
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.BugReport
    ),
    SettingsSearchItem(
        titleRes = R.string.disable_auto_mode,
        summaryRes = R.string.disable_auto_mode_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Memory
    ),
    SettingsSearchItem(
        titleRes = R.string.disable_tweak,
        summaryRes = R.string.disable_tweak_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Security
    ),
    SettingsSearchItem(
        titleRes = R.string.profile_timeout,
        summaryRes = R.string.profile_timeout_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.HourglassEmpty
    ),
    SettingsSearchItem(
        titleRes = R.string.restart_service,
        summaryRes = R.string.restart_service_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.RestartAlt
    ),
    SettingsSearchItem(
        titleRes = R.string.save_log,
        summaryRes = R.string.save_log_desc,
        tab = SettingsSidebarTab.General,
        icon = Icons.Rounded.Save
    ),

    // Tweaks
    SettingsSearchItem(
        titleRes = R.string.perf_lite_mode,
        summaryRes = R.string.perf_lite_mode_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.Speed
    ),
    SettingsSearchItem(
        titleRes = R.string.game_preload,
        summaryRes = R.string.game_preload_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.RocketLaunch
    ),
    SettingsSearchItem(
        titleRes = R.string.memory_killer,
        summaryRes = R.string.memory_killer_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.CleaningServices
    ),
    SettingsSearchItem(
        titleRes = R.string.app_priority_control,
        summaryRes = R.string.app_priority_control_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.SwapVerticalCircle
    ),
    SettingsSearchItem(
        titleRes = R.string.dnd_mode_gaming,
        summaryRes = R.string.dnd_mode_gaming_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.DoNotDisturbOn
    ),
    SettingsSearchItem(
        titleRes = R.string.trim_filesystem,
        summaryRes = R.string.trim_filesystem_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.ContentCut
    ),
    SettingsSearchItem(
        titleRes = R.string.thermalcore_service,
        summaryRes = R.string.thermalcore_service_desc,
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.ThermostatAuto
    ),
    SettingsSearchItem(
        titleRes = R.string.refreshrates,
        summaryText = "Display refresh rate locked during gaming",
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.WebStories
    ),
    SettingsSearchItem(
        titleRes = R.string.renderengine,
        summaryText = "HWUI graphics compositing backend",
        tab = SettingsSidebarTab.Tweaks,
        icon = Icons.Rounded.SettingsSuggest
    ),

    // Appearance
    SettingsSearchItem(
        titleRes = R.string.appearance,
        summaryText = "Auto, Light, Dark, and AMOLED pure black modes",
        tab = SettingsSidebarTab.Appearance,
        icon = Icons.Rounded.BrightnessAuto
    ),
    SettingsSearchItem(
        titleRes = R.string.pers_text_scale,
        summaryText = "App scale percent from 60% downscale to 150% upscale",
        tab = SettingsSidebarTab.Appearance,
        icon = Icons.Rounded.ZoomIn
    ),
    SettingsSearchItem(
        titleRes = R.string.appearance,
        summaryText = "Follow Monet dynamic colors or select a preset palette",
        tab = SettingsSidebarTab.Appearance,
        icon = Icons.Rounded.Palette
    ),

    // About
    SettingsSearchItem(
        titleRes = R.string.section_about,
        summaryRes = R.string.str_creator_maintainer,
        tab = SettingsSidebarTab.About,
        icon = Icons.Rounded.Info
    ),
    SettingsSearchItem(
        titleRes = R.string.str_licenses,
        summaryRes = R.string.str_open_source,
        tab = SettingsSidebarTab.About,
        icon = Icons.Rounded.WorkspacePremium
    )
)

internal enum class SearchScopeFilter(val label: String) {
    All("All"),
    Games("Games"),
    Tuned("Tuned"),
    Settings("Settings")
}

internal data class ResolvedSettingItem(
    val title: String,
    val summary: String,
    val tab: SettingsSidebarTab,
    val icon: ImageVector
)

internal data class QuickNetworkState(
    val isWifi: Boolean,
    val isCellular: Boolean,
    val isConnected: Boolean,
    val label: String,
    val ip: String?
)

@Composable
internal fun rememberQuickNetworkState(connectivityManager: ConnectivityManager?): QuickNetworkState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(quickNetworkState(context, connectivityManager)) }

    DisposableEffect(connectivityManager) {
        if (connectivityManager == null) return@DisposableEffect onDispose {}

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                state = quickNetworkState(context, connectivityManager)
            }

            override fun onLost(network: Network) {
                state = quickNetworkState(context, connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state = quickNetworkState(context, connectivityManager)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                state = quickNetworkState(context, connectivityManager)
            }
        }

        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { connectivityManager.unregisterNetworkCallback(callback) } }
    }

    return state
}

internal fun quickNetworkState(context: Context, manager: ConnectivityManager?): QuickNetworkState {
    if (manager == null) {
        return QuickNetworkState(isWifi = false, isCellular = false, isConnected = false, label = "Offline", ip = null)
    }

    val activeNetwork = manager.activeNetwork ?: return QuickNetworkState(
        isWifi = false, isCellular = false, isConnected = false, label = "Disconnected", ip = null
    )
    val caps = manager.getNetworkCapabilities(activeNetwork) ?: return QuickNetworkState(
        isWifi = false, isCellular = false, isConnected = false, label = "No Network", ip = null
    )

    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    val lp = manager.getLinkProperties(activeNetwork)
    val ip = lp?.linkAddresses
        ?.map { it.address }
        ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        ?.hostAddress

    val label = when {
        isWifi -> {
            val raw = runCatching {
                var ssid: String? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val wifiInfo = caps.transportInfo as? WifiInfo
                    ssid = wifiInfo?.ssid
                }
                if (ssid == null || ssid == "<unknown ssid>") {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    ssid = wm?.connectionInfo?.ssid
                }
                ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }?.removeSurrounding("\"")
            }.getOrNull()
            if (!raw.isNullOrBlank()) raw else "Wi-Fi"
        }
        isCellular -> "Cellular Data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Connected"
    }

    return QuickNetworkState(
        isWifi = isWifi,
        isCellular = isCellular,
        isConnected = true,
        label = label,
        ip = ip
    )
}

internal enum class QuickBluetoothKind {
    Headphones, Speaker, Carkit, HearingAid, GenericAudio, Ble
}

internal data class QuickBluetoothEndpoint(val name: String, val kind: QuickBluetoothKind)

internal fun QuickBluetoothKind.labelRes(): Int = when (this) {
    QuickBluetoothKind.Headphones -> R.string.quick_settings_bt_le_headset
    QuickBluetoothKind.Speaker -> R.string.quick_settings_bt_le_speaker
    QuickBluetoothKind.Carkit -> R.string.quick_settings_bt_sco
    QuickBluetoothKind.HearingAid -> R.string.quick_settings_bt_le_hearing_aid
    QuickBluetoothKind.GenericAudio -> R.string.quick_settings_bt_unknown
    QuickBluetoothKind.Ble -> R.string.quick_settings_bt_le_broadcast
}

internal fun quickBluetoothKind(type: Int): QuickBluetoothKind = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> QuickBluetoothKind.Headphones
    AudioDeviceInfo.TYPE_HEARING_AID -> QuickBluetoothKind.HearingAid
    AudioDeviceInfo.TYPE_BLE_HEADSET -> QuickBluetoothKind.Headphones
    AudioDeviceInfo.TYPE_BLE_SPEAKER -> QuickBluetoothKind.Speaker
    AudioDeviceInfo.TYPE_BLE_BROADCAST -> QuickBluetoothKind.GenericAudio
    else -> QuickBluetoothKind.GenericAudio
}

@Composable
internal fun rememberQuickBluetoothEndpoints(
    audioManager: AudioManager?,
    bluetoothManager: BluetoothManager?
): List<QuickBluetoothEndpoint> {
    var endpoints by remember { mutableStateOf(emptyList<QuickBluetoothEndpoint>()) }

    DisposableEffect(audioManager) {
        if (audioManager == null) return@DisposableEffect onDispose {}

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()

            private fun refresh() {
                val list = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .filter { dev ->
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            dev.type == AudioDeviceInfo.TYPE_HEARING_AID ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    }
                    .map { dev ->
                        val label = dev.productName?.toString().takeUnless { it.isNullOrBlank() }
                            ?: "Bluetooth Audio"
                        QuickBluetoothEndpoint(label, quickBluetoothKind(dev.type))
                    }
                endpoints = list
            }
        }

        audioManager.registerAudioDeviceCallback(callback, null)
        callback.onAudioDevicesAdded(emptyArray())
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    return endpoints
}

internal enum class QuickInputKind {
    Controller, Keyboard, Mouse, Touchpad, Stylus, Generic
}

internal data class QuickInputDevice(
    val id: Int,
    val name: String,
    val kind: QuickInputKind
)

internal fun QuickInputKind.labelRes(): Int = when (this) {
    QuickInputKind.Controller -> R.string.quick_settings_gamepad
    QuickInputKind.Keyboard -> R.string.quick_settings_input_keyboard
    QuickInputKind.Mouse -> R.string.quick_settings_input_mouse
    QuickInputKind.Touchpad -> R.string.quick_settings_input_touchpad
    QuickInputKind.Stylus -> R.string.quick_settings_input_stylus
    QuickInputKind.Generic -> R.string.quick_settings_input_other
}

internal fun quickInputKind(sources: Int): QuickInputKind = when {
    (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
        (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK -> QuickInputKind.Controller
    (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD -> QuickInputKind.Keyboard
    (sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE -> QuickInputKind.Mouse
    (sources and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD -> QuickInputKind.Touchpad
    (sources and InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS -> QuickInputKind.Stylus
    else -> QuickInputKind.Generic
}

@Composable
internal fun rememberQuickInputDevices(manager: InputManager?): List<QuickInputDevice> {
    var devices by remember(manager) { mutableStateOf(emptyList<QuickInputDevice>()) }
    DisposableEffect(manager) {
        if (manager == null) {
            onDispose { }
        } else {
            fun refresh() {
                devices = runCatching {
                    manager.inputDeviceIds.toList()
                        .mapNotNull { id -> InputDevice.getDevice(id)?.takeIf { !it.isVirtual } }
                        .map { device ->
                            QuickInputDevice(
                                id = device.id,
                                name = device.name.orEmpty(),
                                kind = quickInputKind(device.sources),
                            )
                        }
                        .sortedWith(compareBy({ it.kind.ordinal }, { it.name }))
                }.getOrDefault(emptyList())
            }
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) = refresh()
                override fun onInputDeviceChanged(deviceId: Int) = refresh()
                override fun onInputDeviceRemoved(deviceId: Int) = refresh()
            }
            runCatching { manager.registerInputDeviceListener(listener, null) }
            refresh()
            onDispose { runCatching { manager.unregisterInputDeviceListener(listener) } }
        }
    }
    return devices
}

internal val GAMESPACE_PRESET_KEY_COLORS = listOf(
    0xFFF44336.toInt() to "Crimson",
    0xFFE91E63.toInt() to "Sakura",
    0xFF9C27B0.toInt() to "Orchid",
    0xFF673AB7.toInt() to "Iris",
    0xFF3F51B5.toInt() to "Indigo",
    0xFF2196F3.toInt() to "Azure",
    0xFF00BCD4.toInt() to "Cyan",
    0xFF009688.toInt() to "Teal",
    0xFF4FAF50.toInt() to "Emerald",
    0xFFFFEB3B.toInt() to "Solar",
    0xFFFFC107.toInt() to "Amber",
    0xFFFF9800.toInt() to "Tangerine",
    0xFF795548.toInt() to "Mocha",
    0xFF607D8F.toInt() to "Slate",
    0xFFFF9CA8.toInt() to "Blossom"
)

@Composable
internal fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(10_000)
        }
    }
    return remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
}

@Composable
internal fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readBattery(context)
            delay(30_000)
        }
    }
    return state
}

internal fun readBattery(context: Context): Pair<Int, Boolean> {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return 0 to false
    val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return level to charging
}

@Composable
internal fun rememberRamState(): Pair<Float, String> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readRam(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readRam(context)
            delay(15_000)
        }
    }
    return state
}

internal fun readRam(context: Context): Pair<Float, String> {
    val am = context.getSystemService(ActivityManager::class.java) ?: return 0.5f to "—"
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    if (info.totalMem <= 0L) return 0.5f to "—"
    val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
    val fraction = (used.toFloat() / info.totalMem.toFloat()).coerceIn(0f, 1f)
    val usedGb = used.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return fraction to String.format(Locale.US, "%.1fG", usedGb)
}
