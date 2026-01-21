package zx.azenith.ui.util

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val perf_lite_mode: String = "default",
    val dnd_on_gaming: String = "default",
    val app_priority: String = "default",
    val game_preload: String = "default",
    val refresh_rate: String = "default",
    val renderer: String = "default"
)
