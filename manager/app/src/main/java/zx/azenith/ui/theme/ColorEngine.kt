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
 * materialkolor 5.0.0-alpha07 exposes exactly two [ColorSpec.SpecVersion]
 * constants, SPEC_2021 and SPEC_2025, and there is no SPEC_2026 constant or
 * ColorSpec2026 class in the artifact. Rather than pretend otherwise, this enum
 * is the app's own list and each entry names the library version it resolves to
 * today. When a real 2026 ships, only [SPEC_2026]'s mapping changes; nothing in
 * the settings UI, the preference key or the mock preview moves.
 */
enum class ColorEngine(
    val persistedName: String,
    val librarySpec: ColorSpec.SpecVersion
) {
    MaterialYou("SPEC_2021", ColorSpec.SpecVersion.SPEC_2021),

    /**
     * Currently backed by SPEC_2025 because that is the newest mapping the
     * library has. Labelled 2026 in the UI because that is the spec being
     * targeted; see [ColorEngine].
     */
    MaterialExpressive("SPEC_2025", ColorSpec.SpecVersion.SPEC_2025),

    MaterialExpressive2026("SPEC_2026", ColorSpec.SpecVersion.SPEC_2025);

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