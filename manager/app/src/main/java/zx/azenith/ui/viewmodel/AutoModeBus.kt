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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Auto mode is a process-wide setting, but the Settings and Home screens each
 * resolve their own ViewModel instance, so a state change on one is invisible
 * to the other until the process restarts. This carries the change between
 * them. The persisted property stays the source of truth -- this only signals
 * that somebody changed it.
 */
object AutoModeBus {

    private val _changes = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)
    val changes: SharedFlow<String> = _changes

    fun notify(state: String) {
        _changes.tryEmit(state)
    }
}