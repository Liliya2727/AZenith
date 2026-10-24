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
import android.util.Log
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
    private val displayCallbacks = ConcurrentHashMap<Int, Any>()

    private var iDisplayManagerInstance: Any? = null
    private var sysContext: Context? = null
    private var inputManagerInstance: Any? = null
    private var injectInputMethod: java.lang.reflect.Method? = null
    private var setDisplayIdMethod: java.lang.reflect.Method? = null

    override fun onCreate() {
        super.onCreate()
        runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        runCatching {
            Os.setuid(SYSTEM_UID)
            Os.setgid(SYSTEM_UID)
        }.onFailure {
            runCatching { Os.seteuid(SYSTEM_UID) }
        }
        runCatching {
            val ctx = createPackageContext("android", Context.CONTEXT_IGNORE_SECURITY)
            // Patch ContextImpl fields so DisplayManager transactions pass "android" matching UID 1000
            runCatching {
                val fOp = ctx.javaClass.getDeclaredField("mOpPackageName").apply { isAccessible = true }
                fOp.set(ctx, "android")
            }
            runCatching {
                val fBase = ctx.javaClass.getDeclaredField("mBasePackageName").apply { isAccessible = true }
                fBase.set(ctx, "android")
            }
            runCatching {
                val fAttr = ctx.javaClass.getDeclaredField("mAttributionSource").apply { isAccessible = true }
                val attrBuilder = android.content.AttributionSource.Builder(SYSTEM_UID)
                    .setPackageName("android")
                    .build()
                fAttr.set(ctx, attrBuilder)
            }
            sysContext = ctx
        }.onFailure {
            Log.e("ZXFloatRoot", "Failed to patch sysContext", it)
        }
        runCatching {
            val imClass = InputManager::class.java
            inputManagerInstance = runCatching {
                getSystemService(Context.INPUT_SERVICE)
            }.getOrNull() ?: runCatching {
                imClass.getDeclaredMethod("getInstance").invoke(null)
            }.getOrNull()
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
                        runCatching { event.recycle() }
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

    private fun getIDisplayManager(): Any? {
        if (iDisplayManagerInstance != null) return iDisplayManagerInstance
        return runCatching {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, Context.DISPLAY_SERVICE) as? IBinder ?: return null
            val stubClass = Class.forName("android.hardware.display.IDisplayManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val instance = asInterface.invoke(null, binder)
            iDisplayManagerInstance = instance
            instance
        }.onFailure { Log.e("ZXFloatRoot", "Failed to get IDisplayManager", it) }.getOrNull()
    }

    private fun createDisplayViaBinder(
        name: String,
        width: Int,
        height: Int,
        dpi: Int,
        surface: Surface?,
        flags: Int
    ): Int {
        val iDm = getIDisplayManager() ?: return -1
        return runCatching {
            runCatching { Os.seteuid(SYSTEM_UID) }

            val cbInterface = Class.forName("android.hardware.display.IVirtualDisplayCallback")
            val stubBinder = object : Binder() {
                override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                    return true
                }
            }
            val callbackLoader = cbInterface.classLoader ?: javaClass.classLoader ?: ClassLoader.getSystemClassLoader()
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackLoader,
                arrayOf(cbInterface)
            ) { _, method, args ->
                when (method.name) {
                    "asBinder" -> stubBinder
                    "equals" -> args?.firstOrNull() === stubBinder
                    "hashCode" -> stubBinder.hashCode()
                    "toString" -> "IVirtualDisplayCallbackProxy"
                    else -> null
                }
            }

            // Path A: Android 14+ (VirtualDisplayConfig)
            val hasVdConfig = runCatching {
                Class.forName("android.hardware.display.VirtualDisplayConfig\$Builder")
            }.isSuccess

            if (hasVdConfig) {
                val builderClass = Class.forName("android.hardware.display.VirtualDisplayConfig\$Builder")
                val constructor = builderClass.getConstructor(
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                val builder = constructor.newInstance(name, width, height, dpi)
                builderClass.getMethod("setFlags", Int::class.javaPrimitiveType).invoke(builder, flags)
                if (surface != null) {
                    builderClass.getMethod("setSurface", Surface::class.java).invoke(builder, surface)
                }
                runCatching {
                    builderClass.getMethod("setPackageName", String::class.java).invoke(builder, "android")
                }
                val config = builderClass.getMethod("build").invoke(builder)

                val methods = iDm.javaClass.methods.filter { it.name == "createVirtualDisplay" }
                for (m in methods) {
                    val params = m.parameterTypes
                    if (params.isEmpty() || !params[0].name.contains("VirtualDisplayConfig")) continue
                    m.isAccessible = true
                    val args = arrayOfNulls<Any>(params.size)
                    args[0] = config
                    if (params.size > 1) {
                        args[1] = callback
                    }
                    if (params.last() == String::class.java) {
                        args[params.size - 1] = "android"
                    }
                    val res = m.invoke(iDm, *args)
                    val displayId = (res as? Number)?.toInt() ?: -1
                    if (displayId > 0) {
                        Log.i("ZXFloatRoot", "createDisplayViaBinder (A14+) SUCCESS: id=$displayId flags=0x${flags.toString(16)}")
                        displayCallbacks[displayId] = callback
                        return displayId
                    }
                }
            }

            // Path B: Android 10-13 (Legacy IDisplayManager.createVirtualDisplay overloads)
            val methods = iDm.javaClass.methods.filter { it.name == "createVirtualDisplay" }
            for (m in methods) {
                val params = m.parameterTypes
                if (params.any { it.name.contains("VirtualDisplayConfig") }) continue
                m.isAccessible = true
                val args = arrayOfNulls<Any>(params.size)
                var intCount = 0
                for (i in params.indices) {
                    val p = params[i]
                    when {
                        p.name.contains("IVirtualDisplayCallback") || p.isAssignableFrom(cbInterface) -> args[i] = callback
                        p.name.contains("Surface") -> args[i] = surface
                        p == Int::class.javaPrimitiveType || p == java.lang.Integer::class.java -> {
                            when (intCount) {
                                0 -> args[i] = width
                                1 -> args[i] = height
                                2 -> args[i] = dpi
                                3 -> args[i] = flags
                                else -> args[i] = 0
                            }
                            intCount++
                        }
                        p == String::class.java -> {
                            if (i == 0 || i == 2) {
                                args[i] = "android"
                            } else {
                                args[i] = name
                            }
                        }
                        else -> args[i] = null
                    }
                }
                val res = runCatching { m.invoke(iDm, *args) }.getOrNull()
                val displayId = (res as? Number)?.toInt() ?: -1
                if (displayId > 0) {
                    Log.i("ZXFloatRoot", "createDisplayViaBinder (legacy A10-13) SUCCESS: id=$displayId flags=0x${flags.toString(16)}")
                    displayCallbacks[displayId] = callback
                    return displayId
                }
            }
            -1
        }.onFailure {
            val cause = if (it is java.lang.reflect.InvocationTargetException) it.targetException else it
            Log.w("ZXFloatRoot", "createDisplayViaBinder failed: ${cause?.message}", cause)
        }.getOrDefault(-1)
    }

    private fun createTrustedVirtualDisplay(
        name: String,
        width: Int,
        height: Int,
        dpi: Int,
        surface: Surface?
    ): Int {
        Log.i("ZXFloatRoot", "createTrustedVirtualDisplay: $name (${width}x${height}@$dpi)")
        val flagCandidates = intArrayOf(
            FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_TRUSTED or FLAG_SUPPORTS_TOUCH or FLAG_DESTROY_CONTENT_ON_REMOVAL or FLAG_ALWAYS_UNLOCKED,
            FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_TRUSTED or FLAG_SUPPORTS_TOUCH or FLAG_DESTROY_CONTENT_ON_REMOVAL,
            FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY or FLAG_SUPPORTS_TOUCH or FLAG_DESTROY_CONTENT_ON_REMOVAL,
            FLAG_PUBLIC or FLAG_OWN_CONTENT_ONLY,
            FLAG_OWN_CONTENT_ONLY,
            0
        )

        // 1. Direct IDisplayManager IPC passing "android" to match UID 1000
        for (flags in flagCandidates) {
            val id = createDisplayViaBinder(name, width, height, dpi, surface, flags)
            if (id > 0) {
                enforcePortraitOnDisplay(id)
                return id
            }
        }

        // 2. Fallback via DisplayManager with patched sysContext
        val dm = (sysContext ?: this).getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (dm != null) {
            for (flags in flagCandidates) {
                val vd = runCatching {
                    dm.createVirtualDisplay(name, width, height, dpi, surface, flags)
                }.onFailure {
                    Log.w("ZXFloatRoot", "Failed to create display with flags 0x${flags.toString(16)}: ${it.message}")
                }.getOrNull()

                if (vd != null) {
                    val id = vd.display.displayId
                    Log.i("ZXFloatRoot", "VirtualDisplay created: id=$id flags=0x${flags.toString(16)}")
                    virtualDisplays[id] = vd
                    enforcePortraitOnDisplay(id)
                    return id
                }
            }
        }
        Log.e("ZXFloatRoot", "All VirtualDisplay candidates failed")
        return -1
    }

    private fun setVirtualDisplaySurface(displayId: Int, surface: Surface?) {
        mainHandler.post {
            virtualDisplays[displayId]?.surface = surface
            displayCallbacks[displayId]?.let { cb ->
                val iDm = getIDisplayManager() ?: return@let
                runCatching {
                    val m = iDm.javaClass.methods.firstOrNull { it.name == "setVirtualDisplaySurface" }
                    m?.isAccessible = true
                    m?.invoke(iDm, cb, surface)
                }
            }
        }
    }

    private fun releaseVirtualDisplay(displayId: Int) {
        mainHandler.post {
            virtualDisplays.remove(displayId)?.let { vd ->
                runCatching { vd.surface = null }
                runCatching { vd.release() }
            }
            displayCallbacks.remove(displayId)?.let { cb ->
                val iDm = getIDisplayManager() ?: return@let
                runCatching {
                    val m = iDm.javaClass.methods.firstOrNull { it.name == "releaseVirtualDisplay" }
                    m?.isAccessible = true
                    m?.invoke(iDm, cb)
                }
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
        Log.i("ZXFloatRoot", "launchPackageOnDisplay: pkg=$pkg displayId=$displayId")
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
                var component = launchIntent?.component?.flattenToShortString()
                if (component == null) {
                    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
                    val resolve = pm.queryIntentActivities(query, 0).firstOrNull()
                    if (resolve != null) {
                        component = "${resolve.activityInfo.packageName}/${resolve.activityInfo.name}"
                    }
                }

                // Lock virtual display rotation to portrait before starting the activity
                val lockCmd = "cmd window set-ignore-orientation-request -d $displayId true; " +
                    "cmd window set-fix-to-user-rotation -d $displayId enabled; " +
                    "cmd window user-rotation -d $displayId lock 0"
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", lockCmd))
                        .waitFor(1000, TimeUnit.MILLISECONDS)
                }

                if (component != null) {
                    val startCmd = "cmd activity start-activity --user 0 --display $displayId --windowingMode 1 " +
                        "--activity-multiple-task -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                        "-f 0x18080000 -n $component"
                    Log.i("ZXFloatRoot", "Executing startCmd on display $displayId: $startCmd")
                    val proc = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", startCmd))
                    val out = proc.inputStream.bufferedReader().readText().trim()
                    val err = proc.errorStream.bufferedReader().readText().trim()
                    proc.waitFor(1500, TimeUnit.MILLISECONDS)
                    Log.i("ZXFloatRoot", "startCmd finished: exit=${proc.exitValue()} out='$out' err='$err'")

                    if (proc.exitValue() != 0) {
                        val amCmd = "am start --user 0 --display $displayId --windowingMode 1 " +
                            "--activity-multiple-task -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                            "-f 0x18080000 -n $component"
                        Log.i("ZXFloatRoot", "Falling back to amCmd on display $displayId: $amCmd")
                        val amProc = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", amCmd))
                        amProc.waitFor(1500, TimeUnit.MILLISECONDS)
                    }
                } else {
                    Log.w("ZXFloatRoot", "Could not resolve component for $pkg")
                }

                // Re-apply portrait lock after Activity startup in case onResume calls setRequestedOrientation
                Thread.sleep(650L)
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", lockCmd))
                        .waitFor(1000, TimeUnit.MILLISECONDS)
                }
            } catch (e: Exception) {
                Log.e("ZXFloatRoot", "Error in launchPackageOnDisplay", e)
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
