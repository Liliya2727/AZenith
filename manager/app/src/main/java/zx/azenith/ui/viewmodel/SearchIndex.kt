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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import zx.azenith.R

/**
 * Where a search result sends the user.
 *
 * [Pager] is a page of the main pager rather than a pushed route, so selecting
 * one has to scroll the pager instead of calling navigate -- sending "home" to
 * navigate() would push a second copy of the main graph.
 */
sealed interface SearchTarget {
    data class Pager(val route: String) : SearchTarget
    data class Route(val route: String) : SearchTarget
    /**
     * A destination that is only reachable from inside another screen. Tapping
     * it pushes [parent] first and then [route], so Back pops to the screen the
     * entry belongs to instead of the main pager.
     */
    data class Nested(val route: String, val parent: String, val parentTitleRes: Int) : SearchTarget
}

/**
 * A capability a row needs before it is worth offering.
 *
 * The screens hide or disable rows the device cannot use -- the FPSGO screen is
 * MediaTek-only, the Mali governor list exists only when a `*.mali` devfreq node
 * is present, and several tweaks sit behind full mode. Search reads the index
 * directly, so without this it offers rows the app itself refuses to show.
 */
enum class SearchRequirement {
    MediaTek,
    FullMode,
    MaliGpu,
    BypassSupported
}

/** What this device can actually do, probed when the search screen opens. */
data class SearchCapabilities(
    val vendor: String = "unknown",
    val fullMode: Boolean = false,
    val hasMaliGpu: Boolean = false,
    val bypassSupported: Boolean = false
) {
    /**
     * An unknown vendor satisfies MediaTek: a probe that could not tell must not
     * hide rows the app itself would show. The rest are booleans the app already
     * resolves before rendering, so they gate exactly.
     */
    fun satisfies(requirement: SearchRequirement): Boolean = when (requirement) {
        SearchRequirement.MediaTek -> vendor == "unknown" || vendor == "mediatek"
        SearchRequirement.FullMode -> fullMode
        SearchRequirement.MaliGpu -> hasMaliGpu
        SearchRequirement.BypassSupported -> bypassSupported
    }
}

/**
 * One searchable thing: a toggle, a slider, a menu row or a whole screen.
 *
 * All text is held as resource ids rather than resolved strings so the index
 * is a compile-time constant, locale-independent, and costs nothing to build.
 * [SearchIndex.match] resolves them through the caller's own Context.
 */
data class SearchEntry(
    val id: String,
    val titleRes: Int,
    val summaryRes: Int? = null,
    val icon: ImageVector,
    val group: SearchGroup,
    val target: SearchTarget,
    val keywordsRes: List<Int> = emptyList(),
    /** Absent unless the row is conditional on the device; see [SearchCapabilities]. */
    val requires: List<SearchRequirement> = emptyList()
)

/**
 * The screen an entry lives on, which doubles as its result-list header. One
 * group per destination so a result list reads as "these live on Governors"
 * rather than as a flat wall of unrelated rows.
 *
 * [tab] is the main tab the screen hangs off, so a group is exactly as wide as
 * one entry point: Governors/Bypass/FpsGo/Prefs/Colors are only reachable from
 * Tweaks, Personalization/About only from Settings.
 */
enum class SearchGroup(val labelRes: Int, val tab: SearchTab) {
    Tweaks(R.string.nav_tweaks, SearchTab.Tweaks),
    Settings(R.string.nav_settings, SearchTab.Settings),
    Governors(R.string.gov_settings, SearchTab.Tweaks),
    Bypass(R.string.bcharging, SearchTab.Tweaks),
    FpsGo(R.string.str_fpsgo_settings, SearchTab.Tweaks),
    Prefs(R.string.prefs, SearchTab.Tweaks),
    Colors(R.string.color_scheme, SearchTab.Tweaks),
    Personalization(R.string.personalization_title, SearchTab.Settings),
    About(R.string.section_about, SearchTab.Settings)
}

/**
 * A tab search can be opened from.
 *
 * Only Tweaks and Settings carry a search button, so those are the only scopes
 * that exist -- Home and Apps have no entry point and listing them would offer
 * rows no one can reach.
 */
enum class SearchTab(val labelRes: Int) {
    Tweaks(R.string.nav_tweaks),
    Settings(R.string.nav_settings);

    /** Route of the scoped search screen for this tab, as registered in MainActivity. */
    val searchRoute: String get() = "searchscreen/${name.lowercase()}"

    companion object {
        /** Inverse of [searchRoute]; anything unrecognised falls back to Tweaks. */
        fun ofRoute(route: String?): SearchTab =
            SearchTab.entries.firstOrNull { it.name.equals(route, ignoreCase = true) } ?: Tweaks
    }
}

object SearchIndex {

    /**
     * The index, authored by hand rather than derived from the screens.
     *
     * The screens build their rows inline inside LazyColumn scopes, so there is
     * no data model to walk -- a composable cannot be enumerated. Entries are
     * kept in the order they appear on their screen, which is the tie-break
     * order within a group.
     */
    val entries: List<SearchEntry> = listOf(
        // --- Tweaks -------------------------------------------------------
        SearchEntry(
            id = "tweaks.section_performance",
            titleRes = R.string.section_performance,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.perf_lite",
            titleRes = R.string.perf_lite_mode,
            summaryRes = R.string.perf_lite_mode_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            keywordsRes = listOf(R.string.profile_perflite)
        ),
        SearchEntry(
            id = "tweaks.fpsgo",
            titleRes = R.string.str_fpsgo_settings,
            summaryRes = R.string.str_fpsgo_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Route("fpsgoscreen"),
            keywordsRes = listOf(R.string.str_fpsgo_unavailable),
            requires = listOf(SearchRequirement.MediaTek)
        ),
        SearchEntry(
            id = "tweaks.section_additional",
            titleRes = R.string.section_additionalsettings,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.game_preload",
            titleRes = R.string.game_preload,
            summaryRes = R.string.game_preload_desc,
            icon = Icons.Rounded.RocketLaunch,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.memory_killer",
            titleRes = R.string.memory_killer,
            summaryRes = R.string.memory_killer_desc,
            icon = Icons.Rounded.CleaningServices,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            keywordsRes = listOf(R.string.str_tweak_configuration_settings),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "tweaks.app_priority",
            titleRes = R.string.app_priority_control,
            summaryRes = R.string.app_priority_control_desc,
            icon = Icons.Rounded.SwapVerticalCircle,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "tweaks.dnd",
            titleRes = R.string.dnd_mode_gaming,
            summaryRes = R.string.dnd_mode_gaming_desc,
            icon = Icons.Rounded.DoNotDisturbOn,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.fstrim",
            titleRes = R.string.trim_filesystem,
            summaryRes = R.string.trim_filesystem_desc,
            icon = Icons.Outlined.ContentCut,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "tweaks.governors",
            titleRes = R.string.gov_settings,
            summaryRes = R.string.gov_settingsdesc,
            icon = Icons.Filled.Ballot,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Route("governorsettings"),
            keywordsRes = listOf(R.string.section_CPUSettings, R.string.section_mali_gpu, R.string.io_settings)
        ),
        SearchEntry(
            id = "tweaks.refresh_rate",
            titleRes = R.string.refreshrates,
            icon = Icons.Rounded.WebStories,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            keywordsRes = listOf(R.string.refresh_rate_format)
        ),
        SearchEntry(
            id = "tweaks.renderer",
            titleRes = R.string.renderengine,
            icon = Icons.Rounded.SettingsSuggest,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            keywordsRes = listOf(R.string.Renderer_Default, R.string.renderer_opengl_es, R.string.renderer_skiagl, R.string.renderer_skia_vk)
        ),
        SearchEntry(
            id = "tweaks.section_power_thermal",
            titleRes = R.string.section_power_thermal,
            icon = Icons.Rounded.ThermostatAuto,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.bypass_charge",
            titleRes = R.string.bcharging,
            summaryRes = R.string.bcharging_desc,
            icon = Icons.Filled.Cable,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Route("bypasschg"),
            keywordsRes = listOf(R.string.enable_bypass_charge)
        ),
        SearchEntry(
            id = "tweaks.thermalcore",
            titleRes = R.string.thermalcore_service,
            summaryRes = R.string.thermalcore_service_desc,
            icon = Icons.Filled.ThermostatAuto,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.section_addons",
            titleRes = R.string.section_addons,
            icon = Icons.Rounded.AddToPhotos,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks")
        ),
        SearchEntry(
            id = "tweaks.backup",
            titleRes = R.string.str_backup_restore,
            icon = Icons.Outlined.Cloud,
            group = SearchGroup.Tweaks,
            target = SearchTarget.Pager("tweaks"),
            keywordsRes = listOf(R.string.str_tweak_configuration_settings, R.string.str_restore_configuration)
        ),

        // --- Settings -----------------------------------------------------
        SearchEntry(
            id = "settings.section_features",
            titleRes = R.string.section_features,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.show_icon",
            titleRes = R.string.show_icon,
            icon = Icons.Rounded.AddHome,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.disable_auto_mode",
            titleRes = R.string.disable_auto_mode,
            summaryRes = R.string.disable_auto_mode_desc,
            icon = Icons.Rounded.Assistant,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.disable_tweak",
            titleRes = R.string.disable_tweak,
            summaryRes = R.string.disable_tweak_desc,
            icon = Icons.Rounded.DeveloperBoardOff,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings"),
            keywordsRes = listOf(R.string.master_switch)
        ),
        SearchEntry(
            id = "settings.tap_haptic",
            titleRes = R.string.tap_haptic_feedback,
            icon = Icons.Rounded.Vibration,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.show_toast",
            titleRes = R.string.show_toast,
            icon = Icons.Rounded.Notifications,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.profile_notifications",
            titleRes = R.string.show_notifications,
            icon = Icons.Rounded.NotificationsActive,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.profile_timeout",
            titleRes = R.string.profile_timeout,
            summaryRes = R.string.profile_timeout_desc,
            icon = Icons.Rounded.Timer,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.section_others",
            titleRes = R.string.section_others,
            icon = Icons.Rounded.MoreHoriz,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.restart_service",
            titleRes = R.string.restart_service,
            summaryRes = R.string.restart_service_desc,
            icon = Icons.Rounded.RestartAlt,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.save_log",
            titleRes = R.string.save_log,
            summaryRes = R.string.save_log_desc,
            icon = Icons.Rounded.Save,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings"),
            keywordsRes = listOf(R.string.str_logs_diagnostics, R.string.str_send_logs)
        ),
        SearchEntry(
            id = "settings.verbose_log",
            titleRes = R.string.allow_verbose_log,
            icon = Icons.Rounded.BugReport,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.uninstall",
            titleRes = R.string.uninstall,
            icon = Icons.Rounded.Delete,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.changelog",
            titleRes = R.string.str_changelog,
            icon = Icons.AutoMirrored.Rounded.TextSnippet,
            group = SearchGroup.Settings,
            target = SearchTarget.Pager("settings")
        ),
        SearchEntry(
            id = "settings.about",
            titleRes = R.string.about_azenith,
            icon = Icons.Filled.ContactPage,
            group = SearchGroup.Settings,
            target = SearchTarget.Route("aboutscreen"),
            keywordsRes = listOf(R.string.section_about, R.string.cd_github, R.string.cd_telegram)
        ),

        // --- Governors ----------------------------------------------------
        SearchEntry(
            id = "gov.cpu",
            titleRes = R.string.section_CPUSettings,
            icon = Icons.Rounded.Ballot,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.cpu_default",
            titleRes = R.string.default_cpu_gov,
            summaryRes = R.string.default_cpu_gov_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.cpu_performance",
            titleRes = R.string.performance_cpu_gov,
            summaryRes = R.string.performance_cpu_gov_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.cpu_powersave",
            titleRes = R.string.powersave_cpu_gov,
            summaryRes = R.string.powersave_cpu_gov_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.io",
            titleRes = R.string.io_settings,
            icon = Icons.Rounded.Storage,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.io_balanced",
            titleRes = R.string.balanced_io_scheduler,
            summaryRes = R.string.balanced_io_scheduler_desc,
            icon = Icons.Rounded.Storage,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.io_performance",
            titleRes = R.string.performance_io_scheduler,
            summaryRes = R.string.performance_io_scheduler_desc,
            icon = Icons.Rounded.Storage,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.io_powersave",
            titleRes = R.string.powersave_io_scheduler,
            summaryRes = R.string.powersave_io_scheduler_desc,
            icon = Icons.Rounded.Storage,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings")
        ),
        SearchEntry(
            id = "gov.mali",
            titleRes = R.string.section_mali_gpu,
            icon = Icons.Rounded.Memory,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings"),
            requires = listOf(SearchRequirement.MaliGpu)
        ),
        SearchEntry(
            id = "gov.mali_balanced",
            titleRes = R.string.balanced_mali_gov,
            summaryRes = R.string.balanced_mali_gov_desc,
            icon = Icons.Rounded.Memory,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings"),
            requires = listOf(SearchRequirement.MaliGpu)
        ),
        SearchEntry(
            id = "gov.mali_performance",
            titleRes = R.string.performance_mali_gov,
            summaryRes = R.string.performance_mali_gov_desc,
            icon = Icons.Rounded.Memory,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings"),
            requires = listOf(SearchRequirement.MaliGpu)
        ),
        SearchEntry(
            id = "gov.mali_powersave",
            titleRes = R.string.powersave_mali_gov,
            summaryRes = R.string.powersave_mali_gov_desc,
            icon = Icons.Rounded.Memory,
            group = SearchGroup.Governors,
            target = SearchTarget.Route("governorsettings"),
            requires = listOf(SearchRequirement.MaliGpu)
        ),

        // --- Bypass charging ----------------------------------------------
        SearchEntry(
            id = "bypass.enable",
            titleRes = R.string.enable_bypass_charge,
            summaryRes = R.string.enable_bypass_charge_desc,
            icon = Icons.Filled.Cable,
            group = SearchGroup.Bypass,
            target = SearchTarget.Route("bypasschg"),
            requires = listOf(SearchRequirement.BypassSupported)
        ),
        SearchEntry(
            id = "bypass.threshold",
            titleRes = R.string.charging_threshold,
            summaryRes = R.string.charging_threshold_desc,
            icon = Icons.Rounded.BatteryChargingFull,
            group = SearchGroup.Bypass,
            target = SearchTarget.Route("bypasschg"),
            requires = listOf(SearchRequirement.BypassSupported)
        ),
        SearchEntry(
            id = "bypass.compat",
            titleRes = R.string.CompatibilityCheck,
            summaryRes = R.string.CompatibilityCheck_desc,
            icon = Icons.AutoMirrored.Rounded.FactCheck,
            group = SearchGroup.Bypass,
            target = SearchTarget.Route("bypasschg_check")
        ),

        // --- FPS GO -------------------------------------------------------
        SearchEntry(
            id = "fpsgo.enable",
            titleRes = R.string.str_use_fpsgo_title,
            summaryRes = R.string.str_use_fpsgo_summary,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.FpsGo,
            target = SearchTarget.Route("fpsgoscreen")
        ),
        SearchEntry(
            id = "fpsgo.ged",
            titleRes = R.string.fpsgo_ged,
            summaryRes = R.string.fpsgo_ged_desc,
            icon = Icons.Rounded.Speed,
            group = SearchGroup.FpsGo,
            target = SearchTarget.Route("fpsgoscreen"),
            keywordsRes = listOf(R.string.str_fpsgo_frame_per_second_go_is_a)
        ),

        // --- Preference tweaks --------------------------------------------
        SearchEntry(
            id = "prefs.section",
            titleRes = R.string.section_prefstweaks,
            icon = Icons.Rounded.AddToPhotos,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced")
        ),
        SearchEntry(
            id = "prefs.sched_tunes",
            titleRes = R.string.sched_tunes,
            summaryRes = R.string.sched_tunes_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced")
        ),
        SearchEntry(
            id = "prefs.walt_tunes",
            titleRes = R.string.walt_tunes,
            summaryRes = R.string.walt_tunes_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced")
        ),
        SearchEntry(
            id = "prefs.sfl",
            titleRes = R.string.sfl_latency,
            summaryRes = R.string.sfl_latency_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced")
        ),
        SearchEntry(
            id = "prefs.jit",
            titleRes = R.string.jit_compilation,
            summaryRes = R.string.jit_compilation_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced"),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "prefs.trace",
            titleRes = R.string.disable_trace,
            summaryRes = R.string.disable_trace_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced"),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "prefs.logd",
            titleRes = R.string.disable_logging,
            summaryRes = R.string.disable_logging_desc,
            icon = Icons.Rounded.Tune,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced"),
            requires = listOf(SearchRequirement.FullMode)
        ),
        SearchEntry(
            id = "prefs.gpu_mali",
            titleRes = R.string.gpu_mali,
            summaryRes = R.string.gpu_mali_desc,
            icon = Icons.Rounded.DeveloperBoard,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced"),
            keywordsRes = listOf(R.string.str_unavailable_mediatek),
            requires = listOf(SearchRequirement.MediaTek)
        ),
        SearchEntry(
            id = "prefs.thermals",
            titleRes = R.string.disable_thermals,
            summaryRes = R.string.disable_thermals_desc,
            icon = Icons.Rounded.Thermostat,
            group = SearchGroup.Prefs,
            target = SearchTarget.Route("preferenced"),
            keywordsRes = listOf(R.string.str_unavailable_mediatek),
            requires = listOf(SearchRequirement.MediaTek, SearchRequirement.FullMode)
        ),

        // --- Color scheme -------------------------------------------------
        SearchEntry(
            id = "colors.presets",
            titleRes = R.string.presets,
            icon = Icons.Rounded.Palette,
            group = SearchGroup.Colors,
            target = SearchTarget.Route("colorscheme"),
            keywordsRes = listOf(R.string.color_preset_vivid, R.string.color_preset_warm, R.string.color_preset_cool, R.string.color_preset_amoled)
        ),
        SearchEntry(
            id = "colors.red",
            titleRes = R.string.color_red,
            summaryRes = R.string.color_red_desc,
            icon = Icons.Rounded.Palette,
            group = SearchGroup.Colors,
            target = SearchTarget.Route("colorscheme")
        ),
        SearchEntry(
            id = "colors.green",
            titleRes = R.string.color_green,
            summaryRes = R.string.color_green_desc,
            icon = Icons.Rounded.Palette,
            group = SearchGroup.Colors,
            target = SearchTarget.Route("colorscheme")
        ),
        SearchEntry(
            id = "colors.blue",
            titleRes = R.string.color_blue,
            summaryRes = R.string.color_blue_desc,
            icon = Icons.Rounded.Palette,
            group = SearchGroup.Colors,
            target = SearchTarget.Route("colorscheme")
        ),
        SearchEntry(
            id = "colors.saturation",
            titleRes = R.string.color_saturation,
            summaryRes = R.string.color_saturation_desc,
            icon = Icons.Rounded.Palette,
            group = SearchGroup.Colors,
            target = SearchTarget.Route("colorscheme"),
            keywordsRes = listOf(R.string.str_adjust_and_calibrate_your_scre)
        ),

        // --- Personalization ----------------------------------------------
        SearchEntry(
            id = "pers.section_colors",
            titleRes = R.string.personalization_colors_header,
            icon = Icons.Filled.ColorLens,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.accent_intensity",
            titleRes = R.string.pers_accent_intensity,
            icon = Icons.Filled.Tune,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.pers_accent_neutral, R.string.pers_accent_balanced, R.string.pers_accent_bright, R.string.pers_accent_vibrant)
        ),
        SearchEntry(
            id = "pers.roundness",
            titleRes = R.string.pers_roundness,
            icon = Icons.Filled.RoundedCorner,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.pers_shape_group)
        ),
        SearchEntry(
            id = "pers.text_scale",
            titleRes = R.string.pers_text_scale,
            icon = Icons.Filled.FormatSize,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.pers_shape_group)
        ),
        SearchEntry(
            id = "pers.section_interface",
            titleRes = R.string.str_interface,
            icon = Icons.Filled.AutoAwesome,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.nav_floating",
            titleRes = R.string.pers_nav_floating,
            summaryRes = R.string.pers_navbar_summary,
            icon = Icons.Rounded.Smartphone,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.nav_style_floating, R.string.nav_style_pinned)
        ),
        SearchEntry(
            id = "pers.nav_shape",
            titleRes = R.string.pers_nav_shape,
            icon = Icons.Filled.HorizontalRule,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.nav_spacing",
            titleRes = R.string.pers_nav_spacing,
            icon = Icons.Filled.HorizontalRule,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.nav_size",
            titleRes = R.string.pers_nav_size,
            icon = Icons.Outlined.Straighten,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.nav_vibrant",
            titleRes = R.string.pers_nav_vibrant,
            summaryRes = R.string.pers_nav_vibrant_summary,
            icon = Icons.Outlined.Palette,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.nav_labels",
            titleRes = R.string.pers_nav_labels,
            icon = Icons.AutoMirrored.Filled.Label,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.nav_label_always, R.string.nav_label_selected, R.string.nav_label_never)
        ),
        SearchEntry(
            id = "pers.banner",
            titleRes = R.string.str_enable_banner,
            icon = Icons.Outlined.Image,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette"),
            keywordsRes = listOf(R.string.str_pick_media, R.string.str_adjust_gradient)
        ),
        SearchEntry(
            id = "pers.banner_shape",
            titleRes = R.string.pers_banner_shape,
            icon = Icons.Filled.Rectangle,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.blur",
            titleRes = R.string.str_expressive_blur,
            summaryRes = R.string.str_expressive_blur_summary,
            icon = Icons.Filled.BlurOn,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),
        SearchEntry(
            id = "pers.scroll_animation",
            titleRes = R.string.str_use_scroll_animation,
            summaryRes = R.string.str_use_scroll_animation_summary,
            icon = Icons.Filled.SwipeRight,
            group = SearchGroup.Personalization,
            target = SearchTarget.Route("color_palette")
        ),

        // --- About --------------------------------------------------------
        SearchEntry(
            id = "about.source",
            titleRes = R.string.str_source_code,
            summaryRes = R.string.str_view_the_source_code_on_github,
            icon = Icons.Rounded.Code,
            group = SearchGroup.About,
            target = SearchTarget.Route("aboutscreen")
        ),
        SearchEntry(
            id = "about.collaborators",
            titleRes = R.string.str_collaborators,
            icon = Icons.Rounded.People,
            group = SearchGroup.About,
            target = SearchTarget.Route("aboutscreen"),
            keywordsRes = listOf(R.string.str_maintainer, R.string.str_co_maintainer)
        ),
        SearchEntry(
            id = "about.licenses",
            titleRes = R.string.str_licenses,
            icon = Icons.Rounded.Description,
            group = SearchGroup.About,
            target = SearchTarget.Nested("licensescreen", "aboutscreen", R.string.section_about)
        ),
        SearchEntry(
            id = "about.libraries",
            titleRes = R.string.str_libraries_used,
            summaryRes = R.string.str_libraries_hint,
            icon = Icons.AutoMirrored.Rounded.LibraryBooks,
            group = SearchGroup.About,
            target = SearchTarget.Nested("librariesscreen", "aboutscreen", R.string.section_about)
        )
    )

    /**
     * Ranked matches for [query] within [tab], best first.
     *
     * Empty for a blank query -- the search screen shows its hint rather than
     * the whole index, which would be a worse directory than the app itself.
     *
     * [capabilities] drops the rows this device cannot show. Without it search
     * offers options the app itself refuses to render, which is worse than a
     * missing result: the user taps through to a screen that has no such row.
     */
    fun search(
        query: String,
        tab: SearchTab,
        capabilities: SearchCapabilities,
        resolve: (Int) -> String
    ): List<SearchEntry> {
        val tokens = query.lowercase().trim().split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        return entries
            .filter { it.group.tab == tab }
            .filter { entry -> entry.requires.all(capabilities::satisfies) }
            .mapNotNull { entry ->
                val haystack = haystack(entry, resolve)
                val title = resolve(entry.titleRes).lowercase()
                // Every token has to land somewhere, so "nav size" narrows to the
                // navbar-size slider instead of matching anything with "nav".
                val best = tokens.maxOf { token ->
                    when {
                        title == token -> 100
                        title.startsWith(token) -> 80
                        title.contains(token) -> 60
                        haystack.contains(token) -> 30
                        else -> 0
                    }
                }
                if (best == 0) null else entry to best
            }
            .sortedWith(
                compareByDescending<Pair<SearchEntry, Int>> { it.second }
                    .thenBy { it.first.group.ordinal }
                    .thenBy { entries.indexOf(it.first) }
            )
            .map { it.first }
    }

    private fun haystack(entry: SearchEntry, resolve: (Int) -> String): String = buildString {
        entry.summaryRes?.let { append(resolve(it)).append(' ') }
        entry.keywordsRes.forEach { append(resolve(it)).append(' ') }
    }.lowercase()
}
