/*
 * Copyright (C) 2026 Rem01Gaming
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

package com.rem01gaming.systemmonitor

import org.lsposed.hiddenapibypass.HiddenApiBypass

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

// @SuppressLint("StaticFieldLeak") is intentional, this runs as a CLI tool via app_process,
// not inside an Android Activity lifecycle, so there is no real Context leak risk here.

@SuppressLint("StaticFieldLeak", "DiscouragedPrivateApi", "PrivateApi")
object MainKt {
    private const val POLL_INTERVAL_MS = 500L
    private const val PID_RETRY_INTERVAL_MS = 50L
    private const val UNKNOWN_APP = "unknown 0 0"
    private const val NONE_APP = "none 0 0"

    private val FOREGROUND_METHOD_CANDIDATES = listOf(
        "getFocusedRootTaskInfo",
        "getFocusedRootTask",
        "getFocusedTaskInfo",
        "getFocusedStackInfo",
        "getTopActivity",
        "getTasks",
        "getRunningTasks"
    )

    private val COMPONENT_NAME_FIELDS = listOf(
        "topActivity",
        "topActivityComponent",
        "realActivity",
        "baseActivity",
        "origActivity",
        "activity"
    )

    private var systemContext: Context? = null

    private var activityTaskManager: Any? = null
    private var foregroundMethod: Method? = null
    private var powerManager: PowerManager? = null
    private var activityManager: ActivityManager? = null
    private var notificationManager: Any? = null
    private var getZenModeMethod: Method? = null

    private var bruteForceCandidates: List<Method>? = null

    @Volatile
    private var lastStatus = ""

    private var outputPath = ""
    private var lockFilePath: String? = null

    @JvmStatic
    fun main(args: Array<String>) {
        // Usage: app_process / com.rem01gaming.systemmonitor.MainKt <output_path> [lock_file_path]
        if (args.isEmpty()) {
            System.err.println("Usage: <output_path> [lock_file_path]")
            System.err.println("ERROR: output path is required.")
            return
        }
        outputPath = args[0]

        if (args.size >= 2) {
            lockFilePath = args[1]
        }

        bypassHiddenApiRestrictions()
        setupSystemContext()

        if (systemContext == null) {
            System.err.println("ERROR: System context is null.")
            return
        }

        if (!initializeServices()) {
            System.err.println("ERROR: Failed to initialize services, exiting.")
            return
        }

        val lockChannel = acquireLock()

        val monitorThread = Thread.currentThread()
        Runtime.getRuntime().addShutdownHook(Thread {
            lockChannel?.close()
            monitorThread.interrupt()
        })

        runMonitorLoop()
    }

    private fun acquireLock(): FileChannel? {
        val path = lockFilePath ?: return null
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()

            val channel = FileChannel.open(
                file.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )

            val lock: FileLock? = channel.tryLock()
            if (lock == null) {
                System.err.println("ERROR: Another instance holds the lock at '$path'.")
                channel.close()
                System.exit(1)
                null
            } else {
                channel
            }
        } catch (e: Exception) {
            System.err.println("ERROR: Failed to acquire lock at '$path': ${e.message}")
            System.exit(1)
            null
        }
    }

    private fun runMonitorLoop() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                writeStatus()
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun setupSystemContext() {
        try {
            val looperClass = Class.forName("android.os.Looper")
            if (looperClass.getMethod("getMainLooper").invoke(null) == null) {
                looperClass.getMethod("prepareMainLooper").invoke(null)
            }

            val activityThreadClass = Class.forName("android.app.ActivityThread")

            val thread = activityThreadClass.getMethod("systemMain").invoke(null)
                ?: activityThreadClass.getMethod("currentActivityThread").invoke(null)
                ?: error("Both systemMain() and currentActivityThread() returned null")

            systemContext =
                activityThreadClass.getMethod("getSystemContext").invoke(thread) as? Context
                    ?: error("getSystemContext() returned null")

        } catch (e: Exception) {
            System.err.println("ERROR: Failed to set up system context:")
            e.printStackTrace()
        }
    }

    private fun bypassHiddenApiRestrictions() {
        HiddenApiBypass.addHiddenApiExemptions("")
    }

    private fun initializeServices(): Boolean {
        return try {
            val ctx = systemContext ?: return false
            powerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            initActivityTaskManager()
            initNotificationManager()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun initActivityTaskManager() {
        val binder = getSystemService(resolveAtmServiceName())
            ?: error("ServiceManager returned null binder for '${resolveAtmServiceName()}'")
        val atm = bindInterface("${resolveAtmInterfaceName()}\$Stub", binder)
        activityTaskManager = atm
        foregroundMethod = findForegroundMethod(atm)
    }

    private fun initNotificationManager() {
        val binder = getSystemService(Context.NOTIFICATION_SERVICE)
            ?: error("ServiceManager returned null binder for notification service")
        notificationManager = bindInterface("android.app.INotificationManager\$Stub", binder)
        notificationManager?.let { manager ->
            getDeclaredMethods(manager.javaClass).forEach { member ->
                if (member.name == "getZenMode" && member.parameterTypes.isEmpty()) {
                    getZenModeMethod = member
                }
            }
        }
    }

    private fun resolveAtmServiceName() =
        if (Build.VERSION.SDK_INT >= 29) "activity_task" else Context.ACTIVITY_SERVICE

    private fun resolveAtmInterfaceName() =
        if (Build.VERSION.SDK_INT >= 29) "android.app.IActivityTaskManager" else "android.app.IActivityManager"

    private fun getSystemService(name: String): IBinder? {
        val serviceManager = Class.forName("android.os.ServiceManager")
        return serviceManager.getMethod("getService", String::class.java)
            .invoke(null, name) as? IBinder
    }

    private fun bindInterface(stubClassName: String, binder: IBinder): Any {
        return Class.forName(stubClassName)
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
            ?: error("asInterface returned null for $stubClassName")
    }

    private fun findForegroundMethod(atm: Any): Method? {
        val methods = getDeclaredMethods(atm.javaClass).associateBy { it.name }

        return FOREGROUND_METHOD_CANDIDATES
            .mapNotNull { candidate -> methods[candidate] }
            .find { method ->
                method.parameterTypes.isEmpty() ||
                        (method.parameterTypes.size == 1 && method.parameterTypes[0] == Int::class.java) ||
                        method.name == "getTasks" || method.name == "getRunningTasks"
            }
            ?.apply { isAccessible = true }
    }

    private fun writeStatus() {
        // Resolve the focused app, retrying if the PID is not yet available.
        // Returns null if the timeout elapsed without a valid PID, in that case we
        // skip this update so that stale "0 0" data is never written to the output file.
        val focusedApp = waitForValidFocusedApp() ?: return

        val currentStatus = buildStatus(focusedApp)
        if (currentStatus == lastStatus) return

        try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()

            FileOutputStream(file).use { fos ->
                fos.write(currentStatus.toByteArray(Charsets.UTF_8))
                fos.fd.sync()
            }

            lastStatus = currentStatus
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the focused-app string once its PID is known.
     *
     * When the foreground app was just launched it may not yet be visible to
     * [ActivityManager.getRunningAppProcesses], causing [getPidUid] to return "0 0".
     * Rather than writing that bogus value immediately, we poll every [PID_RETRY_INTERVAL_MS] ms
     * until the process shows up or [POLL_INTERVAL_MS] elapses.
     * If the timeout expires and the PID is still unknown, the app string with "0 0" is returned
     * so the output file is still updated rather than silently skipped.
     * Returns null only if interrupted.
     */
    private fun waitForValidFocusedApp(): String? {
        var focusedApp = getFocusedAppInfo()
        if (!hasMissingPid(focusedApp)) return focusedApp

        val deadline = System.currentTimeMillis() + POLL_INTERVAL_MS
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(PID_RETRY_INTERVAL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
            focusedApp = getFocusedAppInfo()
            if (!hasMissingPid(focusedApp)) return focusedApp
        }

        // Timed out, write the app with 0 0 as PID/UID anyway.
        System.err.println("DEBUG: PID still unresolved after ${POLL_INTERVAL_MS}ms for '$focusedApp'.")
        return focusedApp
    }

    /**
     * Returns true when [appInfo] represents a real foreground app whose PID could not
     * yet be resolved (i.e. ends with " 0 0" but is not the sentinel [NONE_APP] value).
     */
    private fun hasMissingPid(appInfo: String): Boolean =
        appInfo != NONE_APP && appInfo.endsWith(" 0 0")

    private fun buildStatus(focusedApp: String): String {
        val screenAwake = if (powerManager?.isInteractive == true) 1 else 0
        val batterySaver = if (powerManager?.isPowerSaveMode == true) 1 else 0
        val zenMode = getZenMode()

        return buildString {
            appendLine("focused_app $focusedApp")
            appendLine("screen_awake $screenAwake")
            appendLine("battery_saver $batterySaver")
            appendLine("zen_mode $zenMode")
        }
    }

    private fun getZenMode(): Int {
        return try {
            getZenModeMethod?.invoke(notificationManager) as? Int ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun getFocusedAppInfo(): String {
        return try {
            val result = invokeForegroundMethod() ?: return UNKNOWN_APP
            if (result is List<*>) {
                getFocusedAppFromList(result)
            } else {
                resolveAppInfoFromObject(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UNKNOWN_APP
        }
    }

    private fun getFocusedAppFromList(list: List<*>): String {
        if (list.isEmpty()) return NONE_APP
        list.forEach { element ->
            extractComponentName(element)?.let { return buildAppInfo(it.packageName) }
        }
        return resolveAppInfoFromObject(list[0]!!)
    }

    private fun resolveAppInfoFromObject(obj: Any): String {
        extractComponentName(obj)?.let { return buildAppInfo(it.packageName) }
        return findPackageLikeString(obj)?.let { buildAppInfo(it) } ?: UNKNOWN_APP
    }

    private fun invokeForegroundMethod(): Any? {
        val method = foregroundMethod ?: return null
        return tryInvokeForegroundMethod(method) ?: bruteForceForegroundMethod()
    }

    private fun tryInvokeForegroundMethod(method: Method): Any? {
        val name = method.name
        return try {
            when {
                name == "getTasks" || name == "getRunningTasks" -> {
                    tryInvokeWithArgs(
                        method,
                        activityTaskManager!!,
                        arrayOf(1),
                        arrayOf(1, 0),
                        arrayOf(1, false, false)
                    )
                }

                method.parameterTypes.isEmpty() -> method.invoke(activityTaskManager)
                else -> tryInvokeWithArgs(method, activityTaskManager!!, arrayOf(0))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryInvokeWithArgs(method: Method, target: Any, vararg argSets: Array<Any>): Any? {
        for (args in argSets) {
            try {
                return method.invoke(target, *args)
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun bruteForceForegroundMethod(): Any? {
        return try {
            val candidates =
                bruteForceCandidates ?: getDeclaredMethods(activityTaskManager!!.javaClass)
                    .filter {
                        val name = it.name.lowercase()
                        name.contains("focus") || name.contains("top") || name.contains("task")
                    }
                    .onEach { it.isAccessible = true }
                    .also { bruteForceCandidates = it }

            candidates.firstNotNullOfOrNull { method ->
                when {
                    method.parameterTypes.isEmpty() ->
                        tryInvokeQuietly { method.invoke(activityTaskManager) }

                    method.parameterTypes.size == 1 && method.parameterTypes[0] == Int::class.java ->
                        tryInvokeQuietly { method.invoke(activityTaskManager, 1) }

                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private inline fun tryInvokeQuietly(block: () -> Any?): Any? {
        return try {
            block()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractComponentName(obj: Any?): ComponentName? {
        if (obj == null) return null
        if (obj is ComponentName) return obj

        COMPONENT_NAME_FIELDS.forEach { fieldName ->
            getComponentNameFromField(obj, obj.javaClass, fieldName)?.let { return it }
        }

        return scanHierarchyForComponentName(obj)
    }

    private fun getComponentNameFromField(
        obj: Any,
        cls: Class<*>,
        fieldName: String
    ): ComponentName? {
        return try {
            val field = cls.getDeclaredField(fieldName).apply { isAccessible = true }
            field.get(obj) as? ComponentName
        } catch (_: Exception) {
            null
        }
    }

    private fun scanHierarchyForComponentName(obj: Any): ComponentName? {
        var cls: Class<*>? = obj.javaClass
        while (cls != null && cls != Any::class.java) {
            getInstanceFields(cls).forEach { field ->
                try {
                    field.isAccessible = true
                    val value = field.get(obj)
                    if (value is ComponentName) return value
                } catch (_: Exception) {
                }
            }
            cls = cls.superclass
        }
        return null
    }

    private fun findPackageLikeString(obj: Any?): String? {
        if (obj == null) return null
        extractPackageName(obj.toString())?.let { return it }

        getInstanceFields(obj.javaClass).forEach { field ->
            if (field.type == String::class.java) {
                try {
                    field.isAccessible = true
                    (field.get(obj) as? String)?.let { str ->
                        extractPackageName(str)?.let { return it }
                    }
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    private fun extractPackageName(input: String?): String? {
        if (input == null || input.indexOf('.') <= 0) return null
        val normalized = input.lowercase().replace(Regex("[^a-z0-9._-]"), " ")
        return normalized.split(Regex("\\s+")).find {
            it.contains(".") && it.matches(Regex("[a-z0-9]+(\\.[a-z0-9]+)+"))
        }
    }

    private fun buildAppInfo(pkg: String): String {
        val pidUid = getPidUid(pkg)
        return "$pkg $pidUid"
    }

    private fun getPidUid(pkg: String): String {
        return try {
            activityManager?.runningAppProcesses
                ?.find { it.processName == pkg || it.pkgList?.contains(pkg) == true }
                ?.let { "${it.pid} ${it.uid}" }
                ?: run {
                    System.err.println("DEBUG: No running process found for package '$pkg'")
                    "0 0"
                }
        } catch (e: Exception) {
            System.err.println("DEBUG: getPidUid failed for '$pkg': ${e.message}")
            "0 0"
        }
    }

    private fun getDeclaredMethods(cls: Class<*>): List<Method> {
        return HiddenApiBypass.getDeclaredMethods(cls).filterIsInstance<Method>()
    }

    private fun getInstanceFields(cls: Class<*>): List<Field> {
        return HiddenApiBypass.getInstanceFields(cls).filterIsInstance<Field>()
    }
}
