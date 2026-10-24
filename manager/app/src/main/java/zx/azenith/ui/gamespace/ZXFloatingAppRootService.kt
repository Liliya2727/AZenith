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

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import android.system.Os
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ZXFloatingAppRootService : RootService() {

    companion object {
        private const val DESCRIPTOR = "zx.azenith.ui.gamespace.IZXFloatingDisplayRoot"
        const val TRANSACTION_CREATE_DISPLAY = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_SET_SURFACE = IBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_RELEASE_DISPLAY = IBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_INJECT_MOTION = IBinder.FIRST_CALL_TRANSACTION + 4
        const val TRANSACTION_INJECT_BACK = IBinder.FIRST_CALL_TRANSACTION + 5
        const val TRANSACTION_LAUNCH_APP = IBinder.FIRST_CALL_TRANSACTION + 6
        const val TRANSACTION_ENFORCE_PORTRAIT = IBinder.FIRST_CALL_TRANSACTION + 7

        private const val SYSTEM_UID = 1000
        private const val FLAG_PUBLIC = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        private const val FLAG_OWN_CONTENT_ONLY = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
        private const val FLAG_SUPPORTS_TOUCH = 1 shl 6
        private const val FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
        private const val FLAG_TRUSTED = 1 shl 10
        private const val FLAG_OWN_DISPLAY_GROUP = 1 shl 11
        private const val FLAG_ALWAYS_UNLOCKED = 1 shl 12
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newCachedThreadPool()
    private val virtualDisplays = ConcurrentHashMap<Int, VirtualDisplay>()

    private var sysContext: Context? = null
    private var inputManagerInstance: Any? = null
    private var injectInputMethod: java.lang.reflect.Method? = null
    private var setDisplayIdMethod: java.lang.reflect.Method? = null

    override fun onCreate() {
        super.onCreate()
        runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        runCatching {
            sysContext = createPackageContext("android", Context.CONTEXT_IGNORE_SECURITY)
        }
        runCatching {
            val imClass = InputManager::class.java
            inputManagerInstance = imClass.getDeclaredMethod("getInstance").invoke(null)
            injectInputMethod = imClass.getDeclaredMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            ).apply { isAccessible = true }
            setDisplayIdMethod = InputEvent::class.java.getDeclaredMethod(
                "setDisplayId",
                Int::class.javaPrimitiveType
            ).apply { isAccessible = true }
        }
        // Keep main thread euid at SYSTEM_UID so kernel binder transactions carry UID 1000
        runCatching { Os.seteuid(SYSTEM_UID) }
    }

    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                when (code) {
                    TRANSACTION_CREATE_DISPLAY -> {
                        data.enforceInterface(DESCRIPTOR)
                        val name = data.readString() ?: "AZenithFloating"
                        val width = data.readInt()
                        val height = data.readInt()
                        val dpi = data.readInt()
                        val surface = if (data.readInt() != 0) Surface.CREATOR.createFromParcel(data) else null
                        val displayId = createTrustedVirtualDisplay(name, width, height, dpi, surface)
                        reply?.writeNoException()
                        reply?.writeInt(displayId)
                        return true
                    }
                    TRANSACTION_SET_SURFACE -> {
                        data.enforceInterface(DESCRIPTOR)
                        val displayId = data.readInt()
                        val surface = if (data.readInt() != 0) Surface.CREATOR.createFromParcel(data) else null
                        setVirtualDisplaySurface(displayId, surface)
                        reply?.writeNoException()
                        return true
                    }
                    TRANSACTION_RELEASE_DISPLAY -> {
                        data.enforceInterface(DESCRIPTOR)
                        val displayId = data.readInt()
                        releaseVirtualDisplay(displayId)
                        reply?.writeNoException()
                        return true
                    }
                    TRANSACTION_INJECT_MOTION -> {
                        data.enforceInterface(DESCRIPTOR)
                        val displayId = data.readInt()
                        val event = MotionEvent.CREATOR.createFromParcel(data)
                        injectMotionOnDisplay(displayId, event)
                        event.recycle()
                        return true
                    }
                    TRANSACTION_INJECT_BACK -> {
                        data.enforceInterface(DESCRIPTOR)
                        val displayId = data.readInt()
                        injectBackKeyOnDisplay(displayId)
                        reply?.writeNoException()
                        return true
                    }
                    TRANSACTION_LAUNCH_APP -> {
                        data.enforceInterface(DESCRIPTOR)
                        val pkg = data.readString().orEmpty()
                        val displayId = data.readInt()
                        launchPackageOnDisplay(pkg, displayId)
                        reply?.writeNoException()
                        return true
                    }
                    TRANSACTION_ENFORCE_PORTRAIT -> {
                        data.enforceInterface(DESCRIPTOR)
                        val displayId = data.readInt()
                        enforcePortraitOnDisplay(displayId)
                        reply?.writeNoException()
                        return true
                    }
                }
                return super.onTransact(code, data, reply, flags)
            }
        }
    }

    override fun onDestroy() {
        virtualDisplays.values.forEach { vd ->
            runCatching { vd.release() }
        }
        virtualDisplays.clear()
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun <T> runOnMainAsSystem(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching { Os.seteuid(SYSTEM_UID) }
            return block()
        }
        var result: T? = null
        val latch = CountDownLatch(1)
        mainHandler.post {
            try {
                runCatching { Os.seteuid(SYSTEM_UID) }
                result = block()
            } finally {
                latch.countDown()
            }
        }
        latch.await(2500, TimeUnit.MILLISECONDS)
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun createTrustedVirtualDisplay(
        name: String,
        width: Int,
        height: Int,
        dpi: Int,
        surface: Surface?
    ): Int {
        return runOnMainAsSystem {
            val sysDm = sysContext?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val fallbackDm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

            val flagCandidates = intArrayOf(
                FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_SUPPORTS_TOUCH or
                    FLAG_DESTROY_CONTENT_ON_REMOVAL or FLAG_TRUSTED or
                    FLAG_OWN_DISPLAY_GROUP or FLAG_ALWAYS_UNLOCKED,
                FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_SUPPORTS_TOUCH or
                    FLAG_DESTROY_CONTENT_ON_REMOVAL or FLAG_TRUSTED,
                FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_SUPPORTS_TOUCH or
                    FLAG_DESTROY_CONTENT_ON_REMOVAL,
                FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY
            )

            if (sysDm != null) {
                for (candidateFlags in flagCandidates) {
                    val vd = runCatching {
                        sysDm.createVirtualDisplay(name, width, height, dpi, surface, candidateFlags)
                    }.getOrNull()
                    if (vd != null) {
                        val id = vd.display.displayId
                        virtualDisplays[id] = vd
                        enforcePortraitOnDisplay(id)
                        return@runOnMainAsSystem id
                    }
                }
            }

            if (fallbackDm != null) {
                val appUid = applicationInfo.uid
                runCatching { Os.seteuid(appUid) }
                try {
                    val vd = runCatching {
                        fallbackDm.createVirtualDisplay(
                            name,
                            width,
                            height,
                            dpi,
                            surface,
                            FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY
                        )
                    }.getOrNull()
                    if (vd != null) {
                        val id = vd.display.displayId
                        virtualDisplays[id] = vd
                        enforcePortraitOnDisplay(id)
                        return@runOnMainAsSystem id
                    }
                } finally {
                    runCatching { Os.seteuid(SYSTEM_UID) }
                }
            }
            -1
        }
    }

    private fun setVirtualDisplaySurface(displayId: Int, surface: Surface?) {
        runOnMainAsSystem {
            virtualDisplays[displayId]?.surface = surface
        }
    }

    private fun releaseVirtualDisplay(displayId: Int) {
        runOnMainAsSystem {
            virtualDisplays.remove(displayId)?.let { vd ->
                runCatching { vd.surface = null }
                runCatching { vd.release() }
            }
        }
    }

    private fun injectMotionOnDisplay(displayId: Int, event: MotionEvent) {
        try {
            runCatching { Os.seteuid(SYSTEM_UID) }
            setDisplayIdMethod?.invoke(event, displayId)
            val im = inputManagerInstance ?: return
            injectInputMethod?.invoke(im, event, 0)
        } catch (_: Exception) {
        }
    }

    private fun injectBackKeyOnDisplay(displayId: Int) {
        ioExecutor.execute {
            try {
                runCatching { Os.seteuid(SYSTEM_UID) }
                val now = SystemClock.uptimeMillis()
                val down = KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_BACK,
                    0,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                val up = KeyEvent(
                    now,
                    now + 10L,
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_BACK,
                    0,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD
                )
                setDisplayIdMethod?.invoke(down, displayId)
                setDisplayIdMethod?.invoke(up, displayId)
                val im = inputManagerInstance ?: return@execute
                injectInputMethod?.invoke(im, down, 0)
                injectInputMethod?.invoke(im, up, 0)
            } catch (_: Exception) {
            }
        }
    }

    private fun enforcePortraitOnDisplay(displayId: Int) {
        if (displayId <= 0) return
        ioExecutor.execute {
            runCatching {
                val cmd = "cmd window set-ignore-orientation-request -d $displayId true >/dev/null 2>&1; " +
                    "cmd window set-fix-to-user-rotation -d $displayId enabled >/dev/null 2>&1; " +
                    "cmd window user-rotation -d $displayId lock 0 >/dev/null 2>&1"
                Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd)).waitFor(1200, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun launchPackageOnDisplay(pkg: String, displayId: Int) {
        if (pkg.isBlank() || displayId <= 0) return
        ioExecutor.execute {
            try {
                val pm = packageManager
                val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                }
                val component = launchIntent?.component?.flattenToShortString()

                // Lock virtual display rotation to portrait before starting the activity
                val lockCmd = "cmd window set-ignore-orientation-request -d $displayId true >/dev/null 2>&1; " +
                    "cmd window set-fix-to-user-rotation -d $displayId enabled >/dev/null 2>&1; " +
                    "cmd window user-rotation -d $displayId lock 0 >/dev/null 2>&1"
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", lockCmd))
                        .waitFor(1000, TimeUnit.MILLISECONDS)
                }

                var startedViaContext = false
                if (launchIntent != null) {
                    startedViaContext = runOnMainAsSystem {
                        try {
                            val opts = ActivityOptions.makeBasic().apply {
                                launchDisplayId = displayId
                            }
                            (sysContext ?: this@ZXFloatingAppRootService).startActivity(
                                launchIntent,
                                opts.toBundle()
                            )
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                }

                if (!startedViaContext && component != null) {
                    val startCmd = "cmd activity start-activity --user 0 --display $displayId " +
                        "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                        "-f 0x18008000 -n $component >/dev/null 2>&1"
                    runCatching {
                        Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", startCmd))
                            .waitFor(1500, TimeUnit.MILLISECONDS)
                    }
                }

                // Re-apply portrait lock after Activity startup in case onResume calls setRequestedOrientation
                Thread.sleep(650L)
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", lockCmd))
                        .waitFor(1000, TimeUnit.MILLISECONDS)
                }
            } catch (_: Exception) {
            }
        }
    }
}

class ZXFloatingDisplayRootClient(private val remote: IBinder) {

    companion object {
        private const val DESCRIPTOR = "zx.azenith.ui.gamespace.IZXFloatingDisplayRoot"
    }

    val isBinderAlive: Boolean
        get() = remote.isBinderAlive

    fun createVirtualDisplay(
        name: String,
        width: Int,
        height: Int,
        dpi: Int,
        surface: Surface?
    ): Int {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeString(name)
            data.writeInt(width)
            data.writeInt(height)
            data.writeInt(dpi)
            if (surface != null) {
                data.writeInt(1)
                surface.writeToParcel(data, 0)
            } else {
                data.writeInt(0)
            }
            remote.transact(ZXFloatingAppRootService.TRANSACTION_CREATE_DISPLAY, data, reply, 0)
            reply.readException()
            reply.readInt()
        } catch (_: Exception) {
            -1
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun setSurface(displayId: Int, surface: Surface?) {
        if (displayId <= 0) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(displayId)
            if (surface != null) {
                data.writeInt(1)
                surface.writeToParcel(data, 0)
            } else {
                data.writeInt(0)
            }
            remote.transact(ZXFloatingAppRootService.TRANSACTION_SET_SURFACE, data, reply, 0)
            reply.readException()
        } catch (_: Exception) {
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun releaseDisplay(displayId: Int) {
        if (displayId <= 0) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(displayId)
            remote.transact(ZXFloatingAppRootService.TRANSACTION_RELEASE_DISPLAY, data, reply, 0)
            reply.readException()
        } catch (_: Exception) {
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun injectMotionEvent(displayId: Int, event: MotionEvent) {
        if (displayId <= 0) return
        val data = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(displayId)
            event.writeToParcel(data, 0)
            remote.transact(
                ZXFloatingAppRootService.TRANSACTION_INJECT_MOTION,
                data,
                null,
                IBinder.FLAG_ONEWAY
            )
        } catch (_: Exception) {
        } finally {
            data.recycle()
        }
    }

    fun injectBack(displayId: Int) {
        if (displayId <= 0) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(displayId)
            remote.transact(ZXFloatingAppRootService.TRANSACTION_INJECT_BACK, data, reply, 0)
            reply.readException()
        } catch (_: Exception) {
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun launchAppOnDisplay(pkg: String, displayId: Int) {
        if (displayId <= 0 || pkg.isBlank()) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeString(pkg)
            data.writeInt(displayId)
            remote.transact(ZXFloatingAppRootService.TRANSACTION_LAUNCH_APP, data, reply, 0)
            reply.readException()
        } catch (_: Exception) {
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun enforcePortrait(displayId: Int) {
        if (displayId <= 0) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeInt(displayId)
            remote.transact(ZXFloatingAppRootService.TRANSACTION_ENFORCE_PORTRAIT, data, reply, 0)
            reply.readException()
        } catch (_: Exception) {
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
