/*
 * Copyright (C) 2026 Rem01Gaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith


import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.BatteryManager
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
import org.lsposed.hiddenapibypass.HiddenApiBypass


@SuppressLint("StaticFieldLeak", "DiscouragedPrivateApi", "PrivateApi")
object AppMonitor {
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
    private var batteryManager: BatteryManager? = null
    private var getZenModeMethod: Method? = null

    private var bruteForceCandidates: List<Method>? = null

    @Volatile
    private var lastStatus = ""
    
    @Volatile
    private var lastBackgroundApps = ""

    private var outputPath = ""
    private var backgroundOutputPath = ""
    private var lockFilePath: String? = null

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            System.err.println("Usage: <status_output_path> <background_output_path> [lock_file_path]")
            System.err.println("ERROR: Missing required output paths.")
            return
        }
        
        outputPath = args[0]
        backgroundOutputPath = args[1]

        if (args.size >= 3) {
            lockFilePath = args[2]
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
                writeBackgroundApps()
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun writeStatus() {
        val focusedApp = waitForValidFocusedApp() ?: return
        val currentStatus = buildStatus(focusedApp)
        if (currentStatus == lastStatus) return

        try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            
            val tmpFile = File("$outputPath.tmp")

            FileOutputStream(tmpFile).use { fos ->
                fos.write(currentStatus.toByteArray(Charsets.UTF_8))
                fos.fd.sync()
            }

            tmpFile.renameTo(file)
            
            lastStatus = currentStatus
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * MENGAMBIL DAFTAR PACKAGE YANG ADA DI RECENT APPS (TASK MANAGER)
     */
    @Suppress("DEPRECATION")
    private fun getRecentAppPackages(): Set<String> {
        val packages = mutableSetOf<String>()
        try {
            val recentTasks = activityManager?.getRecentTasks(30, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
            recentTasks?.forEach { task ->
                val pkg = task.baseIntent.component?.packageName ?: task.topActivity?.packageName
                if (pkg != null) {
                    packages.add(pkg)
                }
            }
            
            val currentFocused = lastStatus.substringAfter("focused_app ").substringBefore(" ")
            if (currentFocused.isNotBlank() && currentFocused != "unknown" && currentFocused != "none") {
                packages.add(currentFocused)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return packages
    }


    private fun writeBackgroundApps() {
        val processes = activityManager?.runningAppProcesses ?: return
        
        val recentPackages = getRecentAppPackages()

        val currentApps = buildString {
            for (process in processes) {
                val pkgName = process.pkgList?.firstOrNull() ?: process.processName
                
                if (recentPackages.contains(pkgName)) {
                    appendLine("$pkgName ${process.pid} ${process.uid}")
                }
            }
        }

        if (currentApps == lastBackgroundApps) return

        try {
            val file = File(backgroundOutputPath)
            file.parentFile?.mkdirs()
            
            val tmpFile = File("$backgroundOutputPath.tmp")

            FileOutputStream(tmpFile).use { fos ->
                fos.write(currentApps.toByteArray(Charsets.UTF_8))
                fos.fd.sync()
            }

            tmpFile.renameTo(file)

            lastBackgroundApps = currentApps
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


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

        return focusedApp
    }

    private fun hasMissingPid(appInfo: String): Boolean =
        appInfo != NONE_APP && appInfo.endsWith(" 0 0")

        private fun buildStatus(focusedApp: String): String {
            val screenAwake = if (powerManager?.isInteractive == true) 1 else 0
            val batterySaver = if (powerManager?.isPowerSaveMode == true) 1 else 0
            val zenMode = getZenMode()
            val batteryLevel = getBatteryLevel()
            val isCharging = getChargingStatus()
            val pkgName = focusedApp.substringBefore(" ")
            val appName = getAppName(pkgName)
        
            return buildString {
                appendLine("focused_app $focusedApp")
                appendLine("screen_awake $screenAwake")
                appendLine("battery_saver $batterySaver")
                appendLine("zen_mode $zenMode")
                appendLine("battery_level $batteryLevel")
                appendLine("is_charging $isCharging")
                appendLine("app_name $appName")
            }
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
                    "0 0"
                }
        } catch (e: Exception) {
            "0 0"
        }
    }

    private fun setupSystemContext() {
        try {
            val looperClass = Class.forName("android.os.Looper")
            if (looperClass.getMethod("getMainLooper").invoke(null) == null) {
                looperClass.getMethod("prepareMainLooper").invoke(null)
            }
    
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            var thread: Any? = null
    
            try {
                thread = activityThreadClass.getMethod("systemMain").invoke(null)
            } catch (e: Exception) {
                System.err.println("WARN: systemMain() failed (MIUI?): ${e.cause?.message ?: e.message}")
            }
    
            if (thread == null) {
                thread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            }
    
            thread ?: error("Both systemMain() and currentActivityThread() returned null")
    
            systemContext = activityThreadClass.getMethod("getSystemContext").invoke(thread) as? Context
                ?: error("getSystemContext() returned null")
    
        } catch (e: Exception) {
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
            batteryManager = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
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

    private fun getDeclaredMethods(cls: Class<*>): List<Method> {
        return HiddenApiBypass.getDeclaredMethods(cls).filterIsInstance<Method>()
    }

    private fun getInstanceFields(cls: Class<*>): List<Field> {
        return HiddenApiBypass.getInstanceFields(cls).filterIsInstance<Field>()
    }
    
    private fun getAppName(pkgName: String): String {
        if (pkgName == "unknown" || pkgName == "none" || pkgName.isBlank()) {
            return "Unknown"
        }
        return try {
            val pm = systemContext?.packageManager ?: return "Unknown"
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            appInfo.loadLabel(pm).toString()
        } catch (e: Exception) {


            pkgName 
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) {
            -1
        }
    }
    
    private fun getChargingStatus(): Int {
        return try {
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) 1 else 0
        } catch (e: Exception) {
            0
        }
    }

}
