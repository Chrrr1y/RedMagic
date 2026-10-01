package dev.lackluster.redmagichelper.hook.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import dev.lackluster.redmagichelper.util.HookLog;

final class HookValues {
    private HookValues() {
    }

    static Object returnValue(Object value, XposedInterface.Chain chain) throws Throwable {
        // Proceeding first keeps side effects and diagnostics from the OEM code.
        chain.proceed();
        return value;
    }

    static Object forceBooleanArgument(
            XposedInterface.Chain chain,
            int index,
            boolean value
    ) throws Throwable {
        return proceedWithArgument(chain, index, value);
    }

    /** LibXposed exposes Chain#getArgs() as a read-only view. */
    static Object proceedWithArgument(
            XposedInterface.Chain chain,
            int index,
            Object value
    ) throws Throwable {
        Object[] args = chain.getArgs().toArray();
        if (index < 0 || index >= args.length) {
            return chain.proceed();
        }
        args[index] = value;
        return chain.proceed(args);
    }

    static Object arg(XposedInterface.Chain chain, int index) {
        return chain.getArgs().size() > index ? chain.getArgs().get(index) : null;
    }

    static String stringArg(XposedInterface.Chain chain, int index) {
        Object value = arg(chain, index);
        return value instanceof String ? (String) value : null;
    }

    static long longArg(XposedInterface.Chain chain, int index, long fallback) {
        Object value = arg(chain, index);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    static int intArg(XposedInterface.Chain chain, int index, int fallback) {
        Object value = arg(chain, index);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static boolean stackContains(String... fragments) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            for (String fragment : fragments) {
                if (className.contains(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean stackContainsMethod(String... methodNames) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            for (String methodName : methodNames) {
                if (methodName.equals(element.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    static String[] removeKnownPackages(String[] values) {
        if (values == null) {
            return null;
        }
        List<String> result = new ArrayList<>(values.length);
        for (String value : values) {
            if (!KNOWN_HOOK_PACKAGES.contains(value)) {
                result.add(value);
            }
        }
        return result.toArray(new String[0]);
    }

    static Object removeKnownPackages(Object value) {
        if (value instanceof String[]) {
            return removeKnownPackages((String[]) value);
        }
        if (value instanceof Collection<?>) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                if (!KNOWN_HOOK_PACKAGES.contains(item)) {
                    result.add(item);
                }
            }
            if (value instanceof Set<?>) {
                return new HashSet<>(result);
            }
            return result;
        }
        return value;
    }

    static Field findField(Object owner, String... names) {
        if (owner == null) {
            return null;
        }
        if (owner instanceof Class<?>) {
            return findField((Class<?>) owner, names);
        }
        return findField(owner.getClass(), names);
    }

    static Field findField(Class<?> type, String... names) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (Throwable ignored) {
                    // Try the next superclass/name combination.
                }
            }
        }
        return null;
    }

    static Object readField(Object owner, String... names) {
        try {
            Field field = findField(owner, names);
            return field == null ? null : field.get(owner instanceof Class<?> ? null : owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean writeField(Object owner, Object value, String... names) {
        try {
            Field field = findField(owner, names);
            if (field == null) {
                return false;
            }
            field.set(owner instanceof Class<?> ? null : owner, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static int readIntField(Object owner, int fallback, String... names) {
        Object value = readField(owner, names);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    static boolean readBooleanField(Object owner, boolean fallback, String... names) {
        Object value = readField(owner, names);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    static Method findNoArgMethod(Object owner, String... names) {
        if (owner == null) {
            return null;
        }
        return findNoArgMethod(owner.getClass(), names);
    }

    static Method findNoArgMethod(Class<?> type, String... names) {
        if (type == null || names == null) {
            return null;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Method method = current.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method;
                } catch (Throwable ignored) {
                    // Try the next name or superclass.
                }
            }
        }
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // Keep the capability optional across vendor revisions.
            }
        }
        return null;
    }

    static Object invokeNoArg(Object owner, String... names) {
        Method method = findNoArgMethod(owner, names);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Method findMethod(Object owner, String name, Class<?>... parameterTypes) {
        return owner == null ? null : findMethod(owner.getClass(), name, parameterTypes);
    }

    static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null || name == null) {
            return null;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name)
                            && parametersMatch(method.getParameterTypes(), parameterTypes)) {
                        try {
                            method.setAccessible(true);
                        } catch (Throwable ignored) {
                            // Best effort only.
                        }
                        return method;
                    }
                }
            } catch (Throwable ignored) {
                // Keep searching optional vendor methods.
            }
        }
        try {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name)
                        && parametersMatch(method.getParameterTypes(), parameterTypes)) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                        // Best effort only.
                    }
                    return method;
                }
            }
        } catch (Throwable ignored) {
            // Keep the capability optional across vendor revisions.
        }
        return null;
    }

    static Object invoke(Object owner, String name, Object... args) {
        if (owner == null || name == null) {
            return null;
        }
        Method method = findMethodForArguments(owner.getClass(), name, args);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(owner, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Long invokeLong(Object owner, String... names) {
        for (String name : names) {
            Object result = invokeNoArg(owner, name);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
        }
        return null;
    }

    static Integer invokeInt(Object owner, String... names) {
        for (String name : names) {
            Object result = invokeNoArg(owner, name);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        }
        return null;
    }

    static boolean invokeBoolean(Object owner, String... names) {
        for (String name : names) {
            Object result = invokeNoArg(owner, name);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
        return false;
    }

    static int resourceId(View view, String packageName, String name) {
        if (view == null || name == null) {
            return 0;
        }
        try {
            String resolvedPackage = packageName;
            if (resolvedPackage == null || resolvedPackage.isEmpty()) {
                resolvedPackage = view.getContext().getPackageName();
            }
            return view.getResources().getIdentifier(name, "id", resolvedPackage);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static View findView(View root, String packageName, String name) {
        if (root == null) {
            return null;
        }
        int id = resourceId(root, packageName, name);
        if (id == 0) {
            return null;
        }
        try {
            return root.findViewById(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean hasResourceId(View view, String packageName, String... names) {
        if (view == null || names == null) {
            return false;
        }
        int viewId;
        try {
            viewId = view.getId();
        } catch (Throwable ignored) {
            return false;
        }
        if (viewId == View.NO_ID) {
            return false;
        }
        for (String name : names) {
            int resourceId = resourceId(view, packageName, name);
            if (resourceId != 0 && viewId == resourceId) {
                return true;
            }
        }
        return false;
    }

    static String resourceEntryName(View view) {
        if (view == null || view.getId() == View.NO_ID) {
            return null;
        }
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (Throwable ignored) {
            return null;
        }
    }

    static void runOnMain(View anchor, Runnable action) {
        if (action == null) {
            return;
        }
        Runnable guarded = () -> {
            try {
                action.run();
            } catch (Throwable error) {
                HookLog.error("runtime", "UI_ACTION_FAILED", error);
            }
        };
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                guarded.run();
                return;
            }
            if (anchor != null && anchor.post(guarded)) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(guarded);
        } catch (Throwable error) {
            HookLog.error("runtime", "UI_DISPATCH_FAILED", error);
        }
    }

    private static Method findMethodForArguments(Class<?> type, String name, Object[] args) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name)
                            && argumentsMatch(method.getParameterTypes(), args)) {
                        try {
                            method.setAccessible(true);
                        } catch (Throwable ignored) {
                            // Best effort only.
                        }
                        return method;
                    }
                }
            } catch (Throwable ignored) {
                // Continue with the superclass.
            }
        }
        try {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name)
                        && argumentsMatch(method.getParameterTypes(), args)) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                        // Best effort only.
                    }
                    return method;
                }
            }
        } catch (Throwable ignored) {
            // Optional method.
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] actual, Class<?>[] requested) {
        if (requested == null || actual.length != requested.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (requested[i] == null || !isParameterCompatible(actual[i], requested[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean argumentsMatch(Class<?>[] actual, Object[] args) {
        if (args == null || actual.length != args.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            Object value = args[i];
            if (value == null) {
                if (actual[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (!isParameterCompatible(actual[i], value.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isParameterCompatible(Class<?> actual, Class<?> requested) {
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

    private static final Set<String> KNOWN_HOOK_PACKAGES = new HashSet<>(Arrays.asList(
            "dev.lackluster.redmagichelper",
            "me.weishu.kernelsu",
            "me.weishu.kernelsu.debug",
            "com.rifsxd.ksunext",
            "org.lsposed.manager",
            "com.topjohnwu.magisk",
            "me.bmax.apatch"
    ));
}
