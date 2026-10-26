/*
 * Copyright (C) 2026 Ryanistr
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

package android.window

/**
 * Compile-time stand-in for the platform's hidden android.window.TaskFpsCallback. The boot
 * classloader always wins for android.*, so at runtime the real class is the one that loads and
 * FpsClient.Callback subclasses that; this only exists so the subclass compiles without the
 * platform SDK. Keep the shape identical to AOSP: one abstract method, implicit no-arg ctor.
 */
abstract class TaskFpsCallback {
    abstract fun onFpsReported(fps: Float)
}
