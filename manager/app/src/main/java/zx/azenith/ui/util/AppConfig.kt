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
