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

package zx.binder

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/**
 * Mirrors SurfaceFlinger's own fps report for the focused task into a file, so the manager shows
 * the real presented frame rate instead of deriving one from `dumpsys SurfaceFlinger --latency`.
 *
 * This only works from a root process: WindowManagerService.registerTaskFpsCallback demands
 * ACCESS_FPS_COUNTER (signature|privileged) and AMS grants every permission to uid 0, while
 * SurfaceFlinger's own check sees system_server on the far side of the call. Everything past that
 * is the framework's job - the task id, the transaction code and the FrameTimeline maths.
 */
object FpsClient {
    private const val DEFAULT_OUT = "/data/adb/.config/AZenith/API/current_fps"
    private const val POLL_MS = 500L
    private const val STALE_MS = 5000L

    @JvmStatic
    fun main(args: Array<String>) {
        val out = File(args.firstOrNull { it.startsWith("--out=") }?.removePrefix("--out=")
            ?: DEFAULT_OUT)
        runCatching { org.lsposed.hiddenapibypass.HiddenApiBypass.setHiddenApiExemptions("L") }

        val wm = runCatching {
            Class.forName("android.view.WindowManagerGlobal").getMethod("getWindowManager").invoke(null)
        }.getOrNull() ?: runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, Context.WINDOW_SERVICE) ?: getService.invoke(null, "window")
            if (binder != null) {
                val stub = Class.forName("android.view.IWindowManager\$Stub")
                val asInterface = stub.getMethod("asInterface", Class.forName("android.os.IBinder"))
                asInterface.invoke(null, binder)
            } else null
        }.getOrNull() ?: run {
            System.err.println("fps: no WindowManager")
            return
        }

        fun findMethod(target: Class<*>, nameContains: String, paramCount: Int): java.lang.reflect.Method? {
            val methods = mutableListOf<java.lang.reflect.Method>()
            var c: Class<*>? = target
            while (c != null && c != Any::class.java) {
                methods.addAll(c.declaredMethods)
                c = c.superclass
            }
            target.interfaces.forEach { i ->
                methods.addAll(i.methods)
                methods.addAll(i.declaredMethods)
            }
            methods.addAll(target.methods)
            return methods.firstOrNull { m ->
                m.name.contains(nameContains) && m.parameterTypes.size == paramCount
            }
        }

        var register = findMethod(wm.javaClass, "TaskFpsCallback", 3)
        if (register == null) {
            runCatching {
                val iwm = Class.forName("android.view.IWindowManager")
                register = findMethod(iwm, "TaskFpsCallback", 3)
            }
        }
        if (register == null) {
            val candidates = mutableListOf<String>()
            var c: Class<*>? = wm.javaClass
            while (c != null && c != Any::class.java) {
                c.declaredMethods.filter { it.name.contains("Fps") }.forEach { candidates.add("${c.simpleName}#${it.name}(${it.parameterTypes.size})") }
                c = c.superclass
            }
            wm.javaClass.methods.filter { it.name.contains("Fps") }.forEach { candidates.add("public#${it.name}(${it.parameterTypes.size})") }
            System.err.println("fps: registerTaskFpsCallback not found. candidates=[${candidates.joinToString(",")}]")
            return
        }
        register.isAccessible = true

        var unregister = findMethod(wm.javaClass, "TaskFpsCallback", 1)
        if (unregister == null) {
            runCatching {
                val iwm = Class.forName("android.view.IWindowManager")
                unregister = findMethod(iwm, "TaskFpsCallback", 1)
            }
        }
        if (unregister == null) {
            System.err.println("fps: unregisterTaskFpsCallback not found")
            return
        }
        unregister.isAccessible = true

        val executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "zenith-fps").apply { isDaemon = true }
        }
        val state = State(out)

        while (true) {
            val taskId = focusedRootTaskId()
            if (taskId != state.taskId) {
                // A callback may only ever be registered for one task, and re-registering the
                // same object with a new id is silently ignored, so drop it before adding one.
                state.callback?.let { cb -> runCatching { unregister.invoke(wm, cb) } }
                state.callback = null
                state.taskId = taskId
                if (taskId != null) {
                    val cb = Callback(state)
                    val err = runCatching { register.invoke(wm, taskId, executor, cb) }.exceptionOrNull()
                    if (err == null) {
                        state.callback = cb
                    } else {
                        System.err.println("fps: register task=$taskId failed: $err")
                    }
                }
            }
            state.flushIfStale()
            Thread.sleep(POLL_MS)
        }
    }

    private fun focusedRootTaskId(): Int? = runCatching {
        val atm = Class.forName("android.app.ActivityTaskManager")
            .getMethod("getService").invoke(null) ?: return null
        val info = atm.javaClass.getMethod("getFocusedRootTaskInfo").invoke(atm) ?: return null
        fieldInt(info, "taskId") ?: callInt(info, "getTaskId")
    }.getOrNull()

    private fun fieldInt(target: Any, name: String): Int? = runCatching {
        target.javaClass.getField(name).getInt(target)
    }.getOrNull() ?: runCatching {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.getInt(target)
    }.getOrNull()

    private fun callInt(target: Any, name: String): Int? = runCatching {
        target.javaClass.getMethod(name).invoke(target) as? Int
    }.getOrNull()

    private class State(private val out: File) {
        @Volatile var taskId: Int? = null
        @Volatile var callback: Any? = null

        private val lock = Object()
        private var last = 0L

        fun publish(fps: Float) {
            synchronized(lock) {
                last = System.currentTimeMillis()
                write(fps)
            }
        }

        fun flushIfStale() {
            synchronized(lock) {
                if (last != 0L && System.currentTimeMillis() - last > STALE_MS) {
                    last = 0L
                    write(0f)
                }
            }
        }

        // "<fps> <epoch_ms>": the manager needs the stamp to tell a live reading from a frozen
        // one, and it cannot stat the file itself because /data/adb is root-only.
        private fun write(value: Float) {
            val text = "${value.toInt()} ${System.currentTimeMillis()}\n"
            val tmp = File("$out.${System.nanoTime()}.tmp")
            runCatching {
                out.parentFile?.mkdirs()
                tmp.writeText(text)
                tmp.setReadable(true, false)
                if (!tmp.renameTo(out)) {
                    out.writeText(text)
                    out.setReadable(true, false)
                    tmp.delete()
                }
            }.onFailure { System.err.println("fps: write $out failed: $it") }
        }
    }

    private class Callback(private val state: State) : android.window.TaskFpsCallback() {
        override fun onFpsReported(fps: Float) {
            state.publish(fps)
        }
    }
}
