# Keep the main class used by app_process
-keep class zx.binder.MainKt {
    public static void main(java.lang.String[]);
}

# Keep all public classes and methods
-keep class zx.binder.** { *; }

# The platform's real android.window.TaskFpsCallback is shadowed at runtime, so this stub must
# keep its name or the callback would not be an instance of the class WMS checks against.
-keep class android.window.TaskFpsCallback { *; }

# Keep hiddenapibypass
-keep class org.lsposed.hiddenapibypass.** { *; }

# Prevent obfuscation of anything accessed via reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions