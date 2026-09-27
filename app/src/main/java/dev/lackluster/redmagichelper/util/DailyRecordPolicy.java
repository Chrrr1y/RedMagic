package dev.lackluster.redmagichelper.util;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Host-independent part of the Mi Health daily-record compatibility policy.
 *
 * The database API has used maps, JSON strings and model objects across app
 * revisions. This class deliberately returns {@code null} when no safe
 * adjustment is possible, allowing the caller to pass the OEM value through.
 */
public final class DailyRecordPolicy {
    public static final String ADJUSTED_MARKER = "rmh_mi_health_adjusted";
    private static final String[] VALUE_KEYS = {"steps", "distance", "calories"};
    private static final Set<Object> MARKED_BEANS = Collections.newSetFromMap(
            Collections.synchronizedMap(new WeakHashMap<>()));

    private DailyRecordPolicy() {
    }

    /** Keeps the OEM feature's documented 1..10 range. */
    public static int normalizeMultiplier(int multiplier) {
        if (multiplier < 1) {
            return 1;
        }
        return Math.min(multiplier, 10);
    }

    /** Returns a copied list only when at least one record was adjusted. */
    public static List<Object> scaleRecords(List<?> source, int multiplier) {
        int safeMultiplier = normalizeMultiplier(multiplier);
        if (source == null || source.isEmpty() || safeMultiplier <= 1) {
            return null;
        }
        List<Object> result = new ArrayList<>(source.size());
        boolean changed = false;
        for (Object item : source) {
            Object adjusted = scaleObject(item, safeMultiplier);
            if (adjusted != null) {
                result.add(adjusted);
                changed = true;
            } else {
                result.add(item);
            }
        }
        return changed ? result : null;
    }

    /** Scales one supported record, or returns null when it must be preserved. */
    @SuppressWarnings("unchecked")
    public static Object scaleObject(Object source, int multiplier) {
        int safeMultiplier = normalizeMultiplier(multiplier);
        if (source == null || safeMultiplier <= 1) {
            return null;
        }
        if (source instanceof Map<?, ?>) {
            return scaleMap((Map<String, ?>) source, safeMultiplier);
        }
        if (source instanceof JSONObject) {
            return scaleJsonObject((JSONObject) source, safeMultiplier);
        }
        if (source instanceof String) {
            return scaleJsonString((String) source, safeMultiplier);
        }
        return scaleBean(source, safeMultiplier);
    }

    public static Map<String, Object> scaleMap(Map<String, ?> source, int multiplier) {
        int safeMultiplier = normalizeMultiplier(multiplier);
        if (source == null || source.isEmpty() || safeMultiplier <= 1
                || isMarked(source.get(ADJUSTED_MARKER))) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (isValueKey(entry.getKey()) && value instanceof Number) {
                Object scaled = scaleNumber((Number) value, safeMultiplier);
                changed |= !sameNumber(value, scaled);
                value = scaled;
            }
            result.put(entry.getKey(), value);
        }
        if (!changed) {
            return null;
        }
        result.put(ADJUSTED_MARKER, Boolean.TRUE);
        return result;
    }

    public static JSONObject scaleJsonObject(JSONObject source, int multiplier) {
        int safeMultiplier = normalizeMultiplier(multiplier);
        if (source == null || safeMultiplier <= 1
                || isMarked(source.opt(ADJUSTED_MARKER))) {
            return null;
        }
        try {
            JSONObject result = new JSONObject(source.toString());
            boolean changed = false;
            for (String key : VALUE_KEYS) {
                Object value = result.opt(key);
                if (value instanceof Number) {
                    result.put(key, scaleNumber((Number) value, safeMultiplier));
                    changed = true;
                }
            }
            if (!changed) {
                return null;
            }
            result.put(ADJUSTED_MARKER, true);
            return result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String scaleJsonString(String source, int multiplier) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject adjusted = scaleJsonObject(new JSONObject(source), multiplier);
            return adjusted == null ? null : adjusted.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object scaleNumber(Number value, int multiplier) {
        int safeMultiplier = normalizeMultiplier(multiplier);
        if (value == null || safeMultiplier <= 1) {
            return value;
        }
        if (value instanceof Double) {
            double scaled = value.doubleValue() * safeMultiplier;
            return Double.isFinite(scaled) ? scaled : value;
        }
        if (value instanceof Float) {
            double scaled = value.doubleValue() * safeMultiplier;
            float result = (float) scaled;
            return Double.isFinite(scaled) && Float.isFinite(result) ? result : value;
        }

        long scaled;
        try {
            scaled = Math.multiplyExact(value.longValue(), (long) safeMultiplier);
        } catch (ArithmeticException overflow) {
            scaled = value.longValue() < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        if (value instanceof Byte) {
            return (byte) clampLong(scaled, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }
        if (value instanceof Short) {
            return (short) clampLong(scaled, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        if (value instanceof Integer) {
            return (int) clampLong(scaled, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return scaled;
    }

    public static boolean isValueKey(String key) {
        if (key == null) {
            return false;
        }
        for (String candidate : VALUE_KEYS) {
            if (candidate.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static Object scaleBean(Object source, int multiplier) {
        if (MARKED_BEANS.contains(source) || isMarked(readProperty(source, ADJUSTED_MARKER))) {
            return null;
        }
        boolean changed = false;
        for (String key : VALUE_KEYS) {
            Object value = readProperty(source, key);
            if (!(value instanceof Number)) {
                continue;
            }
            Object scaled = scaleNumber((Number) value, multiplier);
            if (writeProperty(source, key, scaled)) {
                changed = true;
            }
        }
        if (!changed) {
            return null;
        }
        if (!writeProperty(source, ADJUSTED_MARKER, Boolean.TRUE)) {
            // A vendor bean may not expose an extension marker. Keep an
            // identity-scoped marker so the same instance is never doubled.
            MARKED_BEANS.add(source);
        }
        return source;
    }

    private static Object readProperty(Object owner, String name) {
        if (owner == null || name == null) {
            return null;
        }
        try {
            Field field = findField(owner.getClass(), name);
            if (field != null) {
                return field.get(owner);
            }
            Method getter = findMethod(owner.getClass(), "get" + capitalize(name));
            if (getter == null) {
                getter = findMethod(owner.getClass(), "is" + capitalize(name));
            }
            return getter == null ? null : getter.invoke(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean writeProperty(Object owner, String name, Object value) {
        if (owner == null || name == null) {
            return false;
        }
        try {
            Field field = findField(owner.getClass(), name);
            if (field != null) {
                field.set(owner, coerce(value, field.getType()));
                return true;
            }
            String setterName = "set" + capitalize(name);
            for (Class<?> current = owner.getClass(); current != null;
                    current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(setterName)
                            && method.getParameterTypes().length == 1) {
                        method.setAccessible(true);
                        method.invoke(owner, coerce(value, method.getParameterTypes()[0]));
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Unsupported/immutable beans are passed through by the caller.
        }
        return false;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // Try the superclass.
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // Try the superclass.
            }
        }
        return null;
    }

    private static Object coerce(Object value, Class<?> type) {
        if (!(value instanceof Number)
                || (!type.isPrimitive() && !Number.class.isAssignableFrom(type))) {
            return value;
        }
        Number number = (Number) value;
        if (type == byte.class || type == Byte.class) return number.byteValue();
        if (type == short.class || type == Short.class) return number.shortValue();
        if (type == int.class || type == Integer.class) return number.intValue();
        if (type == long.class || type == Long.class) return number.longValue();
        if (type == float.class || type == Float.class) return number.floatValue();
        if (type == double.class || type == Double.class) return number.doubleValue();
        return value;
    }

    private static boolean isMarked(Object value) {
        return Boolean.TRUE.equals(value)
                || (value instanceof String && Boolean.parseBoolean((String) value))
                || (value instanceof Number && ((Number) value).intValue() != 0);
    }

    private static boolean sameNumber(Object left, Object right) {
        return left == right || left != null && left.equals(right);
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String capitalize(String value) {
        return value.isEmpty()
                ? value
                : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
