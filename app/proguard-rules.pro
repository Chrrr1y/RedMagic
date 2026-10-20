# LibXposed loads this class by name from META-INF/xposed/java_init.list.
-keep class dev.lackluster.redmagichelper.hook.HookEntry { *; }

# Keep the exported provider endpoint and LibXposed service implementation,
# which are reached by framework and Binder entry points outside direct calls.
-keep class dev.lackluster.redmagichelper.config.ConfigProvider { *; }
-keep class io.github.libxposed.service.** { *; }

# The service JAR references this compile-time API annotation, but does not
# package it; it is not used by the app at runtime.
-dontwarn io.github.libxposed.annotation.SinceApi
