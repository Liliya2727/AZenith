/*
 * Copyright (C) 2025 Zexshia
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
package zx.azenith.ui.theme

import com.materialkolor.dynamiccolor.ColorSpec

/**
 * The colour engine the user picks from, decoupled from what the library ships.
 *
 * materialkolor 5.0.0-alpha07 exposes SPEC_2021 and SPEC_2025. The UI shows
 * Material Expressive for the SPEC_2025 mapping.
 */
enum class ColorEngine(
    val persistedName: String,
    val librarySpec: ColorSpec.SpecVersion
) {
    MaterialYou("SPEC_2021", ColorSpec.SpecVersion.SPEC_2021),

    MaterialExpressive("SPEC_2025", ColorSpec.SpecVersion.SPEC_2025);

    companion object {
        val Default = MaterialExpressive

        /**
         * Reads a persisted name. Unrecognised values -- including the bare
         * "DEFAULT" older builds wrote -- fall back to [Default] rather than
         * throwing, so a downgrade or a hand-edited pref cannot brick the theme.
         */
        fun fromPersisted(value: String?): ColorEngine =
            entries.firstOrNull { it.persistedName == value } ?: Default
    }
}