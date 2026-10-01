package dev.lackluster.redmagichelper.hook;

import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lackluster.redmagichelper.config.ModuleConfig;
import dev.lackluster.redmagichelper.util.HookLog;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * Runtime reflection and hook registration for one LibXposed package callback.
 *
 * Every lookup is capability based. A missing OEM class or a changed method
 * signature disables only that capability and never aborts the rest of the
 * target process.
 */
public final class HookSession {
    @FunctionalInterface
    public interface HookCallback {
        Object intercept(XposedInterface.Chain chain) throws Throwable;
    }

    private final XposedModule module;
    private final String packageName;
    private final ClassLoader classLoader;
    private final ApplicationInfo applicationInfo;
    private final ModuleConfig config;
    private final List<XposedInterface.HookHandle> handles = new ArrayList<>();
    private final AtomicInteger installedHooks = new AtomicInteger();
    private final AtomicInteger missingCapabilities = new AtomicInteger();
    private final AtomicInteger failedHooks = new AtomicInteger();

    public HookSession(
            XposedModule module,
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo applicationInfo,
            ModuleConfig config
    ) {
        this.module = module;
        this.packageName = packageName;
        this.classLoader = classLoader;
        this.applicationInfo = applicationInfo;
        this.config = config;
    }

    public String packageName() {
        return packageName;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public ApplicationInfo applicationInfo() {
        return applicationInfo;
    }

    public ModuleConfig config() {
        return config;
    }

    public Class<?> findClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name, false, classLoader);
            } catch (Throwable ignored) {
                // Try the next build-specific candidate.
            }
        }
        recordMissing("class_candidates=" + String.join("|", candidates)
                + " classLoader=" + classLoaderName(classLoader));
        return null;
    }

    /** Resolve an optional build-specific class without reporting a failure. */
    public Class<?> findOptionalClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name, false, classLoader);
            } catch (Throwable ignored) {
                // Optional capability; try the next candidate silently.
            }
        }
        return null;
    }

    public List<Method> findMethods(Class<?> type, String methodName, Class<?>... exactParameters) {
        if (type == null) {
            return Collections.emptyList();
        }

        // Resolve the nearest concrete declaration only.  Walking every
        // superclass and hooking all methods with the same name causes one
        // callback to run once for Activity.onCreate, FragmentActivity.onCreate,
        // BaseActivity.onCreate and the host's own onCreate.  That is especially
        // harmful for lifecycle callbacks: delayed work is duplicated and the
        // host may be moved to a blank/finished window.  If the requested type
        // does not declare the method, fall back to the nearest superclass so
        // inherited APIs such as View.setVisibility still remain hookable.
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            List<Method> methods = new ArrayList<>();
            try {
                for (Method method : current.getDeclaredMethods()) {
                    if (matches(method, methodName, exactParameters)) {
                        methods.add(method);
                    }
                }
            } catch (Throwable ignored) {
                // Some hidden framework classes can reject reflective enumeration.
            }
            if (!methods.isEmpty()) {
                return sortMethods(methods);
            }
        }

        // Interface/default methods are not always returned by
        // getDeclaredMethods() on the concrete class.  Keep a narrow fallback
        // for those cases, still returning only concrete matching methods.
        List<Method> inherited = new ArrayList<>();
        try {
            for (Method method : type.getMethods()) {
                if (matches(method, methodName, exactParameters)) {
                    inherited.add(method);
                }
            }
        } catch (Throwable ignored) {
            // Optional vendor API; an empty result is handled by the caller.
        }
        return sortMethods(inherited);
    }

    private static boolean matches(
            Method method,
            String methodName,
            Class<?>... exactParameters
    ) {
        // Hooking an interface/abstract declaration does not intercept an
        // implementation and produces noisy failures on recent ART.
        if (Modifier.isAbstract(method.getModifiers())
                || !method.getName().equals(methodName)) {
            return false;
        }
        Class<?>[] actual = method.getParameterTypes();
        if (exactParameters.length == 0) {
            return true;
        }
        if (actual.length != exactParameters.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!isParameterCompatible(actual[i], exactParameters[i])) {
                return false;
            }
        }
        return true;
    }

    private static List<Method> sortMethods(List<Method> methods) {
        methods.sort((left, right) -> {
            int leftSynthetic = left.isSynthetic() ? 1 : 0;
            int rightSynthetic = right.isSynthetic() ? 1 : 0;
            return Integer.compare(leftSynthetic, rightSynthetic);
        });
        return methods;
    }

    public Method firstMethod(Class<?> type, String methodName, Class<?>... exactParameters) {
        List<Method> methods = findMethods(type, methodName, exactParameters);
        if (methods.isEmpty()) {
            if (type != null) {
                logMissingMethod(type, new String[]{methodName}, exactParameters);
            }
            return null;
        }
        return methods.get(0);
    }

    public void hook(
            Class<?> type,
            String methodName,
            HookCallback callback,
            Class<?>... exactParameters
    ) {
        List<Method> methods = findMethods(type, methodName, exactParameters);
        if (methods.isEmpty()) {
            if (type != null) {
                logMissingMethod(type, new String[]{methodName}, exactParameters);
            }
            return;
        }
        for (Method method : methods) {
            hook(method, callback);
        }
    }

    /**
     * Hooks the first method name that exists in this host build.  OEM apps
     * often keep the class name but R8-obfuscate the method name, so the
     * adapter can provide the extracted name and the known semantic fallback
     * without widening the parameter match.
     */
    public void hookAny(
            Class<?> type,
            String[] methodNames,
            HookCallback callback,
            Class<?>... exactParameters
    ) {
        if (type == null || methodNames == null) {
            return;
        }
        for (String methodName : methodNames) {
            List<Method> methods = findMethods(type, methodName, exactParameters);
            if (!methods.isEmpty()) {
                for (Method method : methods) {
                    hook(method, callback);
                }
                return;
            }
        }
        logMissingMethod(type, methodNames, exactParameters);
    }

    public void hookFirst(
            Class<?> type,
            String methodName,
            HookCallback callback,
            Class<?>... exactParameters
    ) {
        Method method = firstMethod(type, methodName, exactParameters);
        if (method != null) {
            hook(method, callback);
        }
    }

    private void logMissingMethod(
            Class<?> type,
            String[] methodNames,
            Class<?>... exactParameters
    ) {
        StringBuilder requested = new StringBuilder();
        for (String name : methodNames) {
            if (requested.length() > 0) {
                requested.append('|');
            }
            requested.append(typeName(type)).append('#').append(name)
                    .append('(').append(parameterNames(exactParameters)).append(')');
        }
        recordMissing("target=" + typeName(type)
                + " requested=" + requested
                + " available_overloads=" + availableOverloads(type, methodNames));
    }

    private void recordMissing(String detail) {
        missingCapabilities.incrementAndGet();
        HookLog.missing(packageName, detail);
    }

    private static String parameterNames(Class<?>[] parameters) {
        StringBuilder result = new StringBuilder();
        for (Class<?> parameter : parameters) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(parameter == null ? "<null>" : parameter.getTypeName());
        }
        return result.toString();
    }

    private static String availableOverloads(Class<?> type, String[] methodNames) {
        if (type == null) {
            return "<class-unavailable>";
        }
        List<String> overloads = new ArrayList<>();
        try {
            for (Class<?> current = type; current != null && overloads.size() < 12;
                 current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    for (String name : methodNames) {
                        if (method.getName().equals(name)) {
                            overloads.add(method.toGenericString());
                            break;
                        }
                    }
                    if (overloads.size() >= 12) {
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Keep the requested signature even if ART blocks enumeration.
        }
        return overloads.isEmpty() ? "<none>" : String.join(";", overloads);
    }

    private static String classLoaderName(ClassLoader loader) {
        return loader == null ? "<bootstrap>" : loader.getClass().getName();
    }

    private static final AtomicInteger HOOK_SEQUENCE = new AtomicInteger();

    public void hook(Method method, HookCallback callback) {
        if (method == null) {
            return;
        }
        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
            // LibXposed can still hook some inaccessible framework methods.
        }
        String id = "rmh-rewrite/" + packageName + "/" + method.getDeclaringClass().getName()
                + "#" + method.getName() + "/" + method.getParameterCount()
                + "#" + HOOK_SEQUENCE.incrementAndGet();
        try {
            ThreadLocal<Boolean> inProgress = new ThreadLocal<>();
            HookCallback normalizedCallback = chain -> {
                if (Boolean.TRUE.equals(inProgress.get())) {
                    return chain.proceed();
                }
                inProgress.set(Boolean.TRUE);
                try {
                    Object value = callback.intercept(chain);
                    if (value == null && method.getReturnType().isPrimitive()
                            && method.getReturnType() != void.class) {
                        return primitiveDefault(method.getReturnType());
                    }
                    return value;
                } finally {
                    inProgress.remove();
                }
            };
            XposedInterface.HookHandle handle = module.hook(method)
                    .setId(id)
                    .intercept(normalizedCallback::intercept);
            handles.add(handle);
            installedHooks.incrementAndGet();
        } catch (Throwable error) {
            failedHooks.incrementAndGet();
            HookLog.error(packageName, "HOOK_FAILED " + method, error);
        }
    }

    public void log(String message) {
        HookLog.info(packageName, message);
    }

    public void logSmali(String smaliNode, String action) {
        HookLog.info(packageName, "[" + smaliNode + "] " + action);
    }

    public void missing(String capability) {
        recordMissing(capability);
    }

    public void logScanSummary() {
        String apkPath = applicationInfo == null || applicationInfo.sourceDir == null
                ? "<system-or-unavailable>" : applicationInfo.sourceDir;
        int missing = missingCapabilities.get();
        if (missing > 0) {
            HookLog.warn(packageName, "HOOK_SCAN_CAPABILITY_WARNINGS missing=" + missing
                    + "; details are in the helper app logs");
        }
        HookLog.info(packageName, "HOOK_SCAN_SUMMARY installed="
                + installedHooks.get() + " missing=" + missing
                + " failed=" + failedHooks.get() + " apk=" + apkPath);
    }

    public void unhookAll() {
        for (XposedInterface.HookHandle handle : handles) {
            try {
                handle.unhook();
            } catch (Throwable error) {
                HookLog.error(packageName, "UNHOOK_FAILED", error);
            }
        }
        handles.clear();
    }

    private static boolean isParameterCompatible(Class<?> actual, Class<?> requested) {
        if (actual == null || requested == null) {
            return false;
        }
        if (actual.equals(requested)) {
            return true;
        }
        if (actual.isPrimitive() || requested.isPrimitive()) {
            return boxed(actual).equals(boxed(requested));
        }
        return actual.isAssignableFrom(requested) || requested.isAssignableFrom(actual);
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == void.class) return Void.class;
        return type;
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) return Boolean.FALSE;
        if (type == char.class) return Character.valueOf('\0');
        if (type == byte.class) return Byte.valueOf((byte) 0);
        if (type == short.class) return Short.valueOf((short) 0);
        if (type == int.class) return Integer.valueOf(0);
        if (type == long.class) return Long.valueOf(0L);
        if (type == float.class) return Float.valueOf(0.0f);
        if (type == double.class) return Double.valueOf(0.0d);
        return null;
    }

    public void trySyncConfigFromProvider(android.content.Context context) {
        if (context == null || config == null) {
            return;
        }
        HookLog.attachContentResolver(context.getContentResolver());
        try {
            android.os.Bundle res = context.getContentResolver().call(
                    dev.lackluster.redmagichelper.config.ConfigProvider.CONTENT_URI,
                    "getAll",
                    null,
                    null
            );
            if (res != null) {
                config.updateFromProvider(res);
                HookLog.info(packageName, "CONFIG_SYNC_FROM_PROVIDER_SUCCESS keys=" + res.keySet().size());
            }
        } catch (Throwable t) {
            HookLog.error(packageName, "CONFIG_SYNC_FROM_PROVIDER_FAIL", t);
        }
    }

    private static String typeName(Class<?> type) {
        return type == null ? "<null>" : type.getName();
    }
}
