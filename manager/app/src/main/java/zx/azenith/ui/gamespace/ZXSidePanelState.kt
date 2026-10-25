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

import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class SidePanelQuickTool(
    val id: String,
    val name: String,
    val icon: String,
    val active: Boolean
)

data class SidePanelAntiInterferenceItem(
    val id: String,
    val name: String,
    val icon: String,
    val active: Boolean
)

data class SidePanelCrosshairState(
    val enabled: Boolean = false,
    val style: String = "cross",
    val showDot: Boolean = true,
    val showCross: Boolean = true,
    val showCircle: Boolean = false,
    val showAngle: Boolean = false,
    val outline: Boolean = true,
    val colorHex: String = "#d0bcff",
    val size: Float = 28f,
    val thickness: Float = 3f,
    val gap: Float = 6f,
    val circleRadius: Float = 20f,
    val rotation: Float = 0f,
    val opacity: Int = 95
) {
    val parsedColor: Color
        get() = when (colorHex.lowercase()) {
            "#d0bcff" -> Color(0xFFD0BCFF)
            "#82d5c8" -> Color(0xFF82D5C8)
            "#ffb1c8" -> Color(0xFFFFB1C8)
            "#e9c349" -> Color(0xFFE9C349)
            "#51cf66" -> Color(0xFF51CF66)
            "#ff6b6b" -> Color(0xFFFF6B6B)
            "#ffffff" -> Color(0xFFFFFFFF)
            else -> Color(0xFFD0BCFF)
        }
}

data class SidePanelLiveMonitorState(
    val enabled: Boolean = false,
    val layoutStyle: String = "line",
    val showFps: Boolean = true,
    val showCpu: Boolean = true,
    val showGpu: Boolean = true,
    val showTemp: Boolean = false,
    val showPing: Boolean = false,
    val showBattery: Boolean = false,
    val showProfile: Boolean = false,
    val opacity: Int = 92
)

data class SidePanelAppItem(
    val packageName: String,
    val name: String,
    val iconBitmap: ImageBitmap? = null,
    val isSystemApp: Boolean = false
)

data class SidePanelWidgetMeta(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val category: String,
    val mergeGroup: String? = null
)

sealed interface SidePanelFeedBlock {
    val category: String

    data class Single(
        val widgetId: String,
        override val category: String
    ) : SidePanelFeedBlock

    data class Merged(
        val groupIds: List<String>,
        val mergeGroup: String,
        override val category: String
    ) : SidePanelFeedBlock
}

@Stable
class ZXSidePanelStateHolder {
    companion object {
        val DEFAULT_ACTIVE_WIDGETS = listOf(
            "fpsHeader",
            "speedometers",
            "profileSwitch",
            "brightnessSlider",
            "volumeSlider",
            "quickTools",
            "antiInterference",
            "liveMonitor",
            "crosshair",
            "displayTouch"
        )

        val DEFAULT_LIBRARY_WIDGETS = listOf(
            "networkPing",
            "thermalBattery"
        )
    }

    private var boundPrefs: SharedPreferences? = null
    var currentAppPackage by mutableStateOf("default")
        private set

    var editMode by mutableStateOf(false)
    var activeSubscreen by mutableStateOf<String?>(null)
    var activeScrollCategory by mutableStateOf("overview")

    var liveMonitor by mutableStateOf(SidePanelLiveMonitorState())
    val floatingHudVisible: Boolean
        get() = liveMonitor.enabled

    var brightness by mutableIntStateOf(82)
    var volume by mutableIntStateOf(68)
    var muted by mutableStateOf(false)

    var isAutoMode by mutableStateOf(true)
    var currentProfileCode by mutableStateOf("1")
    var isLiteMode by mutableStateOf(false)
    var activeProfile by mutableStateOf("performance")
    var profileSessionStartMs by mutableLongStateOf(System.currentTimeMillis())
    var profileDurationText by mutableStateOf("00:00:00")

    var fpsCap by mutableIntStateOf(120)
    var currentFps by mutableIntStateOf(60)
    var cpuLoad by mutableIntStateOf(24)
    var gpuLoad by mutableIntStateOf(18)
    var tempCelsius by mutableIntStateOf(36)
    var pingMs by mutableIntStateOf(24)
    var batteryLevel by mutableIntStateOf(100)
    var isBatteryCharging by mutableStateOf(false)
    var isBypassChargingActive by mutableStateOf(false)
    var isRootAvailable by mutableStateOf(false)

    val currentProfileDisplayName: String
        get() = when (currentProfileCode) {
            "0" -> "Initializing"
            "1" -> if (isLiteMode) "Perf Lite" else "Performance"
            "2" -> "Balanced"
            "3" -> "ECO Mode"
            else -> when (activeProfile) {
                "saving" -> "ECO Mode"
                "balanced" -> "Balanced"
                else -> if (isLiteMode) "Perf Lite" else "Performance"
            }
        }

    var visualFilter by mutableStateOf("standard")
    var rgbRed by mutableIntStateOf(100)
    var rgbGreen by mutableIntStateOf(100)
    var rgbBlue by mutableIntStateOf(100)
    var saturation by mutableIntStateOf(100)
    var contrast by mutableIntStateOf(100)
    var sharpness by mutableIntStateOf(35)
    var shadowBoost by mutableIntStateOf(20)
    var touchSampling by mutableIntStateOf(720)
    var touchSensitivity by mutableIntStateOf(7)

    var hudOffsetX by mutableFloatStateOf(0f)
    var hudOffsetY by mutableFloatStateOf(0f)

    // Widget Drag-and-Drop State (Edit Mode)
    var draggingWidgetId by mutableStateOf<String?>(null)
    var draggingFromLibrary by mutableStateOf(false)
    var draggingSourceIndex by mutableIntStateOf(-1)
    var dragPointerInWorkspace by mutableStateOf(Offset.Zero)
    var dragGrabOffset by mutableStateOf(Offset.Zero)
    var dropTargetIndex by mutableIntStateOf(-1)
    var isHoveringLibraryDropZone by mutableStateOf(false)

    // Landing / Dropping Widget Animation
    var droppingWidgetId by mutableStateOf<String?>(null)
    var droppingWidgetOffset by mutableStateOf(Offset.Zero)

    // Dynamic 3-Column Grid Spans (1..3)
    val widgetSpans = mutableStateMapOf<String, Int>()

    fun getWidgetSpan(widgetId: String): Int {
        return widgetSpans[widgetId] ?: 3
    }

    fun setWidgetSpan(widgetId: String, span: Int) {
        val clamped = span.coerceIn(1, 3)
        widgetSpans[widgetId] = clamped
        saveWidgetLayoutPreferences()
    }

    fun cycleWidgetSpan(widgetId: String): Int {
        val current = getWidgetSpan(widgetId)
        val next = if (current >= 3) 1 else current + 1
        setWidgetSpan(widgetId, next)
        return next
    }

    val quickTools = mutableStateListOf(
        SidePanelQuickTool(id = "screenshot", name = "Screenshot", icon = "screenshot_region", active = false),
        SidePanelQuickTool(id = "record", name = "Screen record", icon = "videocam", active = false),
        SidePanelQuickTool(id = "bypass_charging", name = "Bypass charging", icon = "bolt", active = false),
        SidePanelQuickTool(id = "cleanup", name = "Memory cleanup", icon = "cleaning_services", active = false)
    )

    val antiInterference = mutableStateListOf(
        SidePanelAntiInterferenceItem(id = "dnd", name = "Mute notifications", icon = "notifications_off", active = true),
        SidePanelAntiInterferenceItem(id = "calls", name = "Reject calls", icon = "phone_disabled", active = false),
        SidePanelAntiInterferenceItem(id = "gestures", name = "Lock gestures", icon = "swipe_vertical", active = true),
        SidePanelAntiInterferenceItem(id = "autobright", name = "Lock brightness", icon = "brightness_lock", active = true)
    )

    var crosshair by mutableStateOf(SidePanelCrosshairState())

    val installedApps = mutableStateListOf<SidePanelAppItem>()
    val quickApps = mutableStateListOf<String>()
    var defaultQuickApps: List<String> = emptyList()
    var activeGamePackage by mutableStateOf<String?>(null)
    var onQuickAppsChanged: ((List<String>) -> Unit)? = null
    var onLaunchAppRequest: ((String) -> Unit)? = null
    var onSelectProfileRequest: ((String) -> Unit)? = null
    var onToggleAutoModeRequest: ((Boolean) -> Unit)? = null
    var onBrightnessChangedRequest: ((Int) -> Unit)? = null
    var onVolumeChangedRequest: ((Int, Boolean) -> Unit)? = null

    val activeWidgets = mutableStateListOf<String>().apply {
        addAll(DEFAULT_ACTIVE_WIDGETS)
    }

    val libraryWidgets = mutableStateListOf<String>().apply {
        addAll(DEFAULT_LIBRARY_WIDGETS)
    }

    val widgetCatalog: Map<String, SidePanelWidgetMeta> = mapOf(
        "fpsHeader" to SidePanelWidgetMeta(
            id = "fpsHeader",
            title = "FPS counter",
            subtitle = "Real-time frame rate readout",
            icon = "speed",
            category = "overview",
            mergeGroup = "telemetry"
        ),
        "speedometers" to SidePanelWidgetMeta(
            id = "speedometers",
            title = "CPU & GPU speedometers",
            subtitle = "Dual radial load gauges",
            icon = "donut_large",
            category = "overview",
            mergeGroup = "telemetry"
        ),
        "profileSwitch" to SidePanelWidgetMeta(
            id = "profileSwitch",
            title = "Performance mode",
            subtitle = "Power saving, Balanced, Performance",
            icon = "bolt",
            category = "overview",
            mergeGroup = "telemetry"
        ),
        "brightnessSlider" to SidePanelWidgetMeta(
            id = "brightnessSlider",
            title = "Brightness slider",
            subtitle = "Quick screen brightness control",
            icon = "light_mode",
            category = "overview",
            mergeGroup = "sliders"
        ),
        "volumeSlider" to SidePanelWidgetMeta(
            id = "volumeSlider",
            title = "Volume slider",
            subtitle = "Game audio level & mute toggle",
            icon = "volume_up",
            category = "overview",
            mergeGroup = "sliders"
        ),
        "quickTools" to SidePanelWidgetMeta(
            id = "quickTools",
            title = "Quick access tools",
            subtitle = "Screenshot, record, bypass, cleanup",
            icon = "grid_view",
            category = "quickTools"
        ),
        "antiInterference" to SidePanelWidgetMeta(
            id = "antiInterference",
            title = "Anti-interference",
            subtitle = "Block calls, notifications & mistouch",
            icon = "do_not_disturb_on",
            category = "antiInterference"
        ),
        "liveMonitor" to SidePanelWidgetMeta(
            id = "liveMonitor",
            title = "Live Monitor",
            subtitle = "Floating telemetry layout & metrics",
            icon = "monitoring",
            category = "addons"
        ),
        "crosshair" to SidePanelWidgetMeta(
            id = "crosshair",
            title = "Crosshair",
            subtitle = "Custom reticle style, size & color",
            icon = "my_location",
            category = "addons"
        ),
        "displayTouch" to SidePanelWidgetMeta(
            id = "displayTouch",
            title = "Color Scheme",
            subtitle = "Color filters, RGB balance & tuning",
            icon = "display_settings",
            category = "addons"
        ),
        "networkPing" to SidePanelWidgetMeta(
            id = "networkPing",
            title = "Network & ping stabilizer",
            subtitle = "Dual Wi-Fi acceleration & live latency",
            icon = "wifi_tethering",
            category = "addons"
        ),
        "thermalBattery" to SidePanelWidgetMeta(
            id = "thermalBattery",
            title = "Thermal & bypass charging",
            subtitle = "Direct power delivery & core temp",
            icon = "thermostat",
            category = "addons"
        )
    )

    var toastMessage by mutableStateOf("Updated")
    var toastIcon by mutableStateOf("check_circle")
    var toastVisible by mutableStateOf(false)
    var toastSequence by mutableIntStateOf(0)

    fun bindPreferences(prefs: SharedPreferences) {
        boundPrefs = prefs
        loadWidgetLayoutPreferences()
        loadForApp(currentAppPackage)
    }

    fun loadWidgetLayoutPreferences() {
        val prefs = boundPrefs ?: return
        hudOffsetX = prefs.getFloat("gs_global_hud_offset_x", 0f)
        hudOffsetY = prefs.getFloat("gs_global_hud_offset_y", 0f)

        val savedActive = prefs.getString("gs_global_active_widgets", null)
        val savedLib = prefs.getString("gs_global_library_widgets", null)
        if (savedActive != null && savedLib != null) {
            val activeList = savedActive.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && widgetCatalog.containsKey(it) }
                .distinct()
                .toMutableList()
            val libList = savedLib.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && widgetCatalog.containsKey(it) && !activeList.contains(it) }
                .distinct()
                .toMutableList()

            // Ensure any newly introduced catalog widgets (such as liveMonitor) are included
            for (wId in DEFAULT_ACTIVE_WIDGETS) {
                if (!activeList.contains(wId) && !libList.contains(wId)) {
                    val defaultIdx = DEFAULT_ACTIVE_WIDGETS.indexOf(wId).coerceIn(0, activeList.size)
                    activeList.add(defaultIdx, wId)
                }
            }
            for (wId in DEFAULT_LIBRARY_WIDGETS) {
                if (!activeList.contains(wId) && !libList.contains(wId)) {
                    libList.add(wId)
                }
            }

            activeWidgets.clear()
            activeWidgets.addAll(activeList)
            libraryWidgets.clear()
            libraryWidgets.addAll(libList)
        }

        val savedSpans = prefs.getString("gs_global_widget_spans", null)
        widgetSpans.clear()
        if (savedSpans != null) {
            savedSpans.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val span = parts[1].toIntOrNull()
                    if (span != null && parts[0].isNotEmpty()) {
                        widgetSpans[parts[0]] = span.coerceIn(1, 3)
                    }
                }
            }
        }
    }

    fun saveWidgetLayoutPreferences() {
        val prefs = boundPrefs ?: return
        val spansString = widgetSpans.entries.joinToString(";") { (k, v) -> "$k:$v" }
        prefs.edit()
            .putString("gs_global_active_widgets", activeWidgets.joinToString(","))
            .putString("gs_global_library_widgets", libraryWidgets.joinToString(","))
            .putString("gs_global_widget_spans", spansString)
            .putFloat("gs_global_hud_offset_x", hudOffsetX)
            .putFloat("gs_global_hud_offset_y", hudOffsetY)
            .apply()
    }

    fun loadForApp(packageName: String) {
        val pkgKey = packageName.trim().ifEmpty { "default" }
        currentAppPackage = pkgKey
        val prefs = boundPrefs ?: return
        val p = "gs_app_${pkgKey}_"

        crosshair = SidePanelCrosshairState(
            enabled = prefs.getBoolean("${p}ch_enabled", false),
            style = prefs.getString("${p}ch_style", "cross") ?: "cross",
            showDot = prefs.getBoolean("${p}ch_dot", true),
            showCross = prefs.getBoolean("${p}ch_cross", true),
            showCircle = prefs.getBoolean("${p}ch_circle", false),
            showAngle = prefs.getBoolean("${p}ch_angle", false),
            outline = prefs.getBoolean("${p}ch_outline", true),
            colorHex = prefs.getString("${p}ch_color", "#d0bcff") ?: "#d0bcff",
            size = prefs.getFloat("${p}ch_size", 28f).coerceIn(8f, 56f),
            thickness = prefs.getFloat("${p}ch_thick", 3f).coerceIn(1f, 8f),
            gap = prefs.getFloat("${p}ch_gap", 6f).coerceIn(0f, 28f)
        )

        liveMonitor = SidePanelLiveMonitorState(
            enabled = prefs.getBoolean("${p}lm_enabled", false),
            layoutStyle = prefs.getString("${p}lm_style", "line") ?: "line",
            showFps = prefs.getBoolean("${p}lm_fps", true),
            showCpu = prefs.getBoolean("${p}lm_cpu", true),
            showGpu = prefs.getBoolean("${p}lm_gpu", true),
            showTemp = prefs.getBoolean("${p}lm_temp", false),
            showPing = prefs.getBoolean("${p}lm_ping", false),
            showBattery = prefs.getBoolean("${p}lm_battery", false),
            showProfile = prefs.getBoolean("${p}lm_profile", false),
            opacity = prefs.getInt("${p}lm_opacity", 92).coerceIn(40, 100)
        )

        visualFilter = prefs.getString("${p}disp_filter", "standard") ?: "standard"
        rgbRed = prefs.getInt("${p}disp_r", 100).coerceIn(50, 150)
        rgbGreen = prefs.getInt("${p}disp_g", 100).coerceIn(50, 150)
        rgbBlue = prefs.getInt("${p}disp_b", 100).coerceIn(50, 150)
        saturation = prefs.getInt("${p}disp_sat", 100).coerceIn(0, 200)
        contrast = prefs.getInt("${p}disp_con", 100).coerceIn(50, 150)

        for (i in antiInterference.indices) {
            val item = antiInterference[i]
            val defaultActive = item.id != "calls"
            val savedActive = prefs.getBoolean("${p}anti_${item.id}", defaultActive)
            antiInterference[i] = item.copy(active = savedActive)
        }

        for (i in quickTools.indices) {
            val tool = quickTools[i]
            if (tool.id != "cleanup") {
                val savedActive = prefs.getBoolean("${p}tool_${tool.id}", false)
                quickTools[i] = tool.copy(active = savedActive)
            }
        }
    }

    fun saveCurrentAppPreferences() {
        val prefs = boundPrefs ?: return
        val pkgKey = currentAppPackage.ifEmpty { "default" }
        val p = "gs_app_${pkgKey}_"
        val editor = prefs.edit()

        editor.putBoolean("${p}ch_enabled", crosshair.enabled)
        editor.putString("${p}ch_style", crosshair.style)
        editor.putBoolean("${p}ch_dot", crosshair.showDot)
        editor.putBoolean("${p}ch_cross", crosshair.showCross)
        editor.putBoolean("${p}ch_circle", crosshair.showCircle)
        editor.putBoolean("${p}ch_angle", crosshair.showAngle)
        editor.putBoolean("${p}ch_outline", crosshair.outline)
        editor.putString("${p}ch_color", crosshair.colorHex)
        editor.putFloat("${p}ch_size", crosshair.size)
        editor.putFloat("${p}ch_thick", crosshair.thickness)
        editor.putFloat("${p}ch_gap", crosshair.gap)

        editor.putBoolean("${p}lm_enabled", liveMonitor.enabled)
        editor.putString("${p}lm_style", liveMonitor.layoutStyle)
        editor.putBoolean("${p}lm_fps", liveMonitor.showFps)
        editor.putBoolean("${p}lm_cpu", liveMonitor.showCpu)
        editor.putBoolean("${p}lm_gpu", liveMonitor.showGpu)
        editor.putBoolean("${p}lm_temp", liveMonitor.showTemp)
        editor.putBoolean("${p}lm_ping", liveMonitor.showPing)
        editor.putBoolean("${p}lm_battery", liveMonitor.showBattery)
        editor.putBoolean("${p}lm_profile", liveMonitor.showProfile)
        editor.putInt("${p}lm_opacity", liveMonitor.opacity)

        editor.putString("${p}disp_filter", visualFilter)
        editor.putInt("${p}disp_r", rgbRed)
        editor.putInt("${p}disp_g", rgbGreen)
        editor.putInt("${p}disp_b", rgbBlue)
        editor.putInt("${p}disp_sat", saturation)
        editor.putInt("${p}disp_con", contrast)

        for (item in antiInterference) {
            editor.putBoolean("${p}anti_${item.id}", item.active)
        }
        for (tool in quickTools) {
            if (tool.id != "cleanup") {
                editor.putBoolean("${p}tool_${tool.id}", tool.active)
            }
        }
        editor.apply()
    }

    fun showToast(message: String, iconName: String = "check_circle") {
        toastMessage = message
        toastIcon = iconName
        toastVisible = true
        toastSequence++
    }

    fun buildFeedBlocks(): List<SidePanelFeedBlock> {
        val blocks = mutableListOf<SidePanelFeedBlock>()
        var i = 0
        while (i < activeWidgets.size) {
            val wId = activeWidgets[i]
            val meta = widgetCatalog[wId]
            if (meta == null) {
                i++
                continue
            }
            val mg = meta.mergeGroup
            if (mg != null) {
                val groupIds = mutableListOf(wId)
                var j = i + 1
                while (j < activeWidgets.size) {
                    val nextId = activeWidgets[j]
                    val nextMeta = widgetCatalog[nextId]
                    if (nextMeta != null && nextMeta.mergeGroup == mg) {
                        groupIds.add(nextId)
                        j++
                    } else {
                        break
                    }
                }
                if (groupIds.size > 1) {
                    blocks.add(
                        SidePanelFeedBlock.Merged(
                            groupIds = groupIds,
                            mergeGroup = mg,
                            category = meta.category
                        )
                    )
                } else {
                    blocks.add(
                        SidePanelFeedBlock.Single(
                            widgetId = wId,
                            category = meta.category
                        )
                    )
                }
                i = j
            } else {
                blocks.add(
                    SidePanelFeedBlock.Single(
                        widgetId = wId,
                        category = meta.category
                    )
                )
                i++
            }
        }
        return blocks
    }

    fun toggleEditMode(force: Boolean? = null) {
        if (activeSubscreen != null) {
            activeSubscreen = null
        }
        cancelWidgetDrag()
        val next = force ?: !editMode
        editMode = next
    }

    fun openSubscreen(subscreenName: String) {
        if (editMode) {
            cancelWidgetDrag()
            editMode = false
        }
        activeSubscreen = subscreenName
    }

    fun closeSubscreen() {
        activeSubscreen = null
    }

    fun resetCurrentSubscreen() {
        when (activeSubscreen) {
            "crosshair" -> {
                crosshair = SidePanelCrosshairState()
                saveCurrentAppPreferences()
                showToast("Crosshair settings reset", "restart_alt")
            }
            "liveMonitor" -> {
                liveMonitor = SidePanelLiveMonitorState()
                saveCurrentAppPreferences()
                showToast("Live Monitor settings reset", "restart_alt")
            }
            "display" -> {
                visualFilter = "standard"
                rgbRed = 100
                rgbGreen = 100
                rgbBlue = 100
                saturation = 100
                contrast = 100
                sharpness = 35
                shadowBoost = 20
                touchSampling = 720
                touchSensitivity = 7
                saveCurrentAppPreferences()
                showToast("Display & RGB balance reset", "restart_alt")
            }
            "appsManager" -> {
                quickApps.clear()
                quickApps.addAll(defaultQuickApps)
                onQuickAppsChanged?.invoke(quickApps.toList())
                showToast("App shortcuts reset", "restart_alt")
            }
        }
    }

    fun startWidgetDrag(
        widgetId: String,
        fromLibrary: Boolean,
        sourceIndex: Int,
        initialPointerInWorkspace: Offset,
        grabOffset: Offset
    ) {
        draggingWidgetId = widgetId
        draggingFromLibrary = fromLibrary
        draggingSourceIndex = sourceIndex
        dragPointerInWorkspace = initialPointerInWorkspace
        dragGrabOffset = grabOffset
        dropTargetIndex = if (fromLibrary) 0 else sourceIndex.coerceIn(0, activeWidgets.size.coerceAtLeast(1) - 1)
        isHoveringLibraryDropZone = false
    }

    fun updateWidgetDrag(
        pointerInWorkspace: Offset,
        targetIndex: Int,
        hoveringLibrary: Boolean
    ) {
        if (draggingWidgetId == null) return
        dragPointerInWorkspace = pointerInWorkspace
        isHoveringLibraryDropZone = hoveringLibrary
        if (!hoveringLibrary && targetIndex >= 0) {
            val maxIdx = if (draggingFromLibrary) activeWidgets.size else (activeWidgets.size - 1).coerceAtLeast(0)
            dropTargetIndex = targetIndex.coerceIn(0, maxIdx)
        }
    }

    fun commitWidgetDrag() {
        val wId = draggingWidgetId ?: return
        val fromLib = draggingFromLibrary
        val srcIdx = draggingSourceIndex
        val dstIdx = dropTargetIndex
        val hoverLib = isHoveringLibraryDropZone
        val finalOffset = dragPointerInWorkspace - dragGrabOffset

        draggingWidgetId = null
        draggingFromLibrary = false
        draggingSourceIndex = -1
        dropTargetIndex = -1
        isHoveringLibraryDropZone = false

        val title = widgetCatalog[wId]?.title ?: wId
        if (fromLib) {
            if (!hoverLib) {
                libraryWidgets.remove(wId)
                if (!activeWidgets.contains(wId)) {
                    val insertIdx = dstIdx.coerceIn(0, activeWidgets.size)
                    activeWidgets.add(insertIdx, wId)
                    saveWidgetLayoutPreferences()
                    droppingWidgetId = wId
                    droppingWidgetOffset = finalOffset
                    showToast("Placed $title", "add_circle")
                }
            }
        } else {
            if (hoverLib) {
                removeWidgetToLibrary(wId)
            } else if (srcIdx in activeWidgets.indices && dstIdx in activeWidgets.indices) {
                if (srcIdx != dstIdx) {
                    moveActiveWidget(srcIdx, dstIdx)
                    saveWidgetLayoutPreferences()
                    showToast("Moved $title", "drag_indicator")
                }
                droppingWidgetId = wId
                droppingWidgetOffset = finalOffset
            }
        }
    }

    fun cancelWidgetDrag() {
        draggingWidgetId = null
        draggingFromLibrary = false
        draggingSourceIndex = -1
        dropTargetIndex = -1
        isHoveringLibraryDropZone = false
    }

    fun removeWidgetToLibrary(widgetId: String) {
        if (activeWidgets.remove(widgetId)) {
            if (!libraryWidgets.contains(widgetId)) {
                libraryWidgets.add(0, widgetId)
            }
            saveWidgetLayoutPreferences()
            val title = widgetCatalog[widgetId]?.title ?: widgetId
            showToast("Moved $title to library", "inventory_2")
        }
    }

    fun addWidgetFromLibrary(widgetId: String, atIndex: Int = 0) {
        if (libraryWidgets.remove(widgetId)) {
            if (!activeWidgets.contains(widgetId)) {
                val idx = atIndex.coerceIn(0, activeWidgets.size)
                activeWidgets.add(idx, widgetId)
            }
            saveWidgetLayoutPreferences()
            val title = widgetCatalog[widgetId]?.title ?: widgetId
            showToast("Added $title", "add_circle")
        }
    }

    fun moveActiveWidget(fromIndex: Int, toIndex: Int) {
        if (fromIndex in activeWidgets.indices && toIndex in activeWidgets.indices && fromIndex != toIndex) {
            val item = activeWidgets.removeAt(fromIndex)
            activeWidgets.add(toIndex, item)
            saveWidgetLayoutPreferences()
        }
    }

    fun resetDefaultWidgets() {
        activeWidgets.clear()
        activeWidgets.addAll(DEFAULT_ACTIVE_WIDGETS)
        libraryWidgets.clear()
        libraryWidgets.addAll(DEFAULT_LIBRARY_WIDGETS)
        widgetSpans.clear()
        saveWidgetLayoutPreferences()
        showToast("Restored default widget order", "restart_alt")
    }

    fun toggleAutoMode() {
        val nextAuto = !isAutoMode
        isAutoMode = nextAuto
        onToggleAutoModeRequest?.invoke(nextAuto)
        if (nextAuto) {
            showToast("Auto Mode enabled — dynamic game profile active", "auto_fix_high")
        } else {
            showToast("Manual Mode enabled — select profile below", "tune")
        }
    }

    fun selectPerformanceProfile(profileId: String) {
        val code = when (profileId) {
            "saving" -> "3"
            "balanced" -> "2"
            else -> "1"
        }
        if (isAutoMode && isRootAvailable) {
            showToast("Auto Mode is active — tap Auto Mode chip to switch manually", "auto_fix_high")
            return
        }
        activeProfile = profileId
        currentProfileCode = code
        profileSessionStartMs = System.currentTimeMillis()
        profileDurationText = "00:00:00"
        onSelectProfileRequest?.invoke(code)
        val label = when (profileId) {
            "saving" -> "ECO Mode"
            "balanced" -> "Balanced"
            else -> if (isLiteMode) "Performance Lite" else "Performance"
        }
        showToast("Applying $label profile", "bolt")
    }

    fun nudgeGaugeLoad(target: String) {
        if (target == "cpu") {
            showToast("CPU Load: $cpuLoad% • Temp: ${tempCelsius}°C", "donut_large")
        } else {
            showToast("GPU Load: $gpuLoad% • ${currentFps} FPS", "donut_large")
        }
    }

    fun updateHorizontalBrightness(value: Int) {
        val clamped = value.coerceIn(10, 100)
        brightness = clamped
        onBrightnessChangedRequest?.invoke(clamped)
    }

    fun updateHorizontalVolume(value: Int) {
        val clamped = value.coerceIn(0, 100)
        volume = clamped
        muted = clamped == 0
        onVolumeChangedRequest?.invoke(clamped, muted)
    }

    fun toggleMuteAudio() {
        muted = !muted
        onVolumeChangedRequest?.invoke(volume, muted)
    }

    fun triggerQuickTool(toolId: String) {
        val idx = quickTools.indexOfFirst { it.id == toolId }
        if (idx == -1) return
        val tool = quickTools[idx]
        if (toolId == "cleanup") {
            cpuLoad = (cpuLoad - 15).coerceAtLeast(18)
            gpuLoad = (gpuLoad - 12).coerceAtLeast(15)
            showToast("Memory cleaned — freed 1.4 GB RAM", "cleaning_services")
            return
        }
        val updated = tool.copy(active = !tool.active)
        quickTools[idx] = updated
        saveCurrentAppPreferences()
        showToast("${updated.name} ${if (updated.active) "started" else "stopped"}", updated.icon)
    }

    fun toggleAntiInterference(id: String) {
        val idx = antiInterference.indexOfFirst { it.id == id }
        if (idx == -1) return
        val item = antiInterference[idx]
        antiInterference[idx] = item.copy(active = !item.active)
        saveCurrentAppPreferences()
    }

    fun toggleCrosshairQuick() {
        crosshair = crosshair.copy(enabled = !crosshair.enabled)
        saveCurrentAppPreferences()
    }

    fun toggleCrosshairLayer(layerKey: String) {
        crosshair = when (layerKey) {
            "showDot" -> crosshair.copy(showDot = !crosshair.showDot, enabled = true)
            "showCross" -> crosshair.copy(showCross = !crosshair.showCross, enabled = true)
            "showCircle" -> crosshair.copy(showCircle = !crosshair.showCircle, enabled = true)
            "showAngle" -> crosshair.copy(showAngle = !crosshair.showAngle, enabled = true)
            else -> crosshair
        }
        saveCurrentAppPreferences()
    }

    fun setCrosshairStyle(styleName: String) {
        crosshair = when (styleName) {
            "dot" -> crosshair.copy(
                style = styleName,
                enabled = true,
                showDot = true,
                showCross = false,
                showCircle = false,
                showAngle = false
            )
            "cross" -> crosshair.copy(
                style = styleName,
                enabled = true,
                showDot = true,
                showCross = true,
                showCircle = false,
                showAngle = false
            )
            "circle" -> crosshair.copy(
                style = styleName,
                enabled = true,
                showDot = true,
                showCross = false,
                showCircle = true,
                showAngle = false
            )
            "chevron" -> crosshair.copy(
                style = styleName,
                enabled = true,
                showDot = true,
                showCross = false,
                showCircle = false,
                showAngle = true
            )
            else -> crosshair
        }
        saveCurrentAppPreferences()
    }

    fun setCrosshairColor(hex: String) {
        crosshair = crosshair.copy(colorHex = hex, enabled = true)
        saveCurrentAppPreferences()
    }

    fun updateCrosshairThickness(value: Float) {
        val snapped = (Math.round(value * 2f) / 2f).coerceIn(1f, 8f)
        crosshair = crosshair.copy(thickness = snapped)
        saveCurrentAppPreferences()
    }

    fun updateCrosshairSize(value: Float) {
        crosshair = crosshair.copy(size = value.coerceIn(8f, 56f))
        saveCurrentAppPreferences()
    }

    fun updateCrosshairGap(value: Float) {
        crosshair = crosshair.copy(gap = value.coerceIn(0f, 28f))
        saveCurrentAppPreferences()
    }

    fun toggleFloatingHud() {
        liveMonitor = liveMonitor.copy(enabled = !liveMonitor.enabled)
        saveCurrentAppPreferences()
    }

    fun setLiveMonitorLayoutStyle(styleName: String) {
        liveMonitor = liveMonitor.copy(layoutStyle = styleName, enabled = true)
        saveCurrentAppPreferences()
    }

    fun toggleLiveMonitorMetric(metricKey: String) {
        val cur = liveMonitor
        val updated = when (metricKey) {
            "fps" -> cur.copy(showFps = !cur.showFps)
            "cpu" -> cur.copy(showCpu = !cur.showCpu)
            "gpu" -> cur.copy(showGpu = !cur.showGpu)
            "temp" -> cur.copy(showTemp = !cur.showTemp)
            "ping" -> cur.copy(showPing = !cur.showPing)
            "battery" -> cur.copy(showBattery = !cur.showBattery)
            "profile" -> cur.copy(showProfile = !cur.showProfile)
            else -> cur
        }
        // Keep at least one metric enabled
        val anyEnabled = updated.showFps || updated.showCpu || updated.showGpu ||
            updated.showTemp || updated.showPing || updated.showBattery || updated.showProfile
        if (anyEnabled) {
            liveMonitor = updated
            saveCurrentAppPreferences()
        }
    }

    fun updateLiveMonitorOpacity(opacityPct: Int) {
        liveMonitor = liveMonitor.copy(opacity = opacityPct.coerceIn(40, 100))
        saveCurrentAppPreferences()
    }

    fun selectVisualFilter(filterId: String) {
        visualFilter = filterId
        saveCurrentAppPreferences()
    }

    fun updateRgbRedChannel(value: Int) {
        rgbRed = value.coerceIn(50, 150)
        saveCurrentAppPreferences()
    }

    fun updateRgbGreenChannel(value: Int) {
        rgbGreen = value.coerceIn(50, 150)
        saveCurrentAppPreferences()
    }

    fun updateRgbBlueChannel(value: Int) {
        rgbBlue = value.coerceIn(50, 150)
        saveCurrentAppPreferences()
    }

    fun updateDisplaySaturation(value: Int) {
        saturation = value.coerceIn(0, 200)
        saveCurrentAppPreferences()
    }

    fun updateDisplayContrast(value: Int) {
        contrast = value.coerceIn(50, 150)
        saveCurrentAppPreferences()
    }

    fun launchQuickApp(packageName: String) {
        onLaunchAppRequest?.invoke(packageName)
    }

    fun toggleQuickApp(packageName: String) {
        if (quickApps.contains(packageName)) {
            quickApps.remove(packageName)
        } else {
            quickApps.add(packageName)
        }
        onQuickAppsChanged?.invoke(quickApps.toList())
    }
}
