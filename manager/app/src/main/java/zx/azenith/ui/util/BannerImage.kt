/*
 * Copyright (C) 2026-2027 Rapli
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

import android.content.Context

private const val PREFS_NAME = "settings"
private const val KEY_HEADER_IMAGE = "header_image_uri"

/**
 * @return uri string of custom header image, or null if using default
 */
fun Context.getHeaderImage(): String? {
    return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_HEADER_IMAGE, null)
}

/**
 * Save custom header image uri.
 * Default header should NOT be saved.
 */
fun Context.saveHeaderImage(uri: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HEADER_IMAGE, uri)
        .apply()
}

/**
 * Clear custom header image and fallback to default.
 */
fun Context.clearHeaderImage() {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_HEADER_IMAGE)
        .apply()
}