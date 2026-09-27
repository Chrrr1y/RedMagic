package dev.lackluster.redmagichelper.util;

import java.util.Arrays;
import java.util.Locale;

/** Small, host-independent clock rules shared by weather, launcher and SystemUI hooks. */
public final class ClockPolicy {
    private static final String[] PERIOD_LABELS = {
            "凌晨", "早上", "上午", "中午", "下午", "傍晚", "晚上"
    };
    private static final int[] PERIOD_REPRESENTATIVE_HOURS = {
            2, 7, 10, 12, 15, 18, 21
    };

    private ClockPolicy() {
    }

    public static int periodIndex(int hourOfDay) {
        if (hourOfDay < 0 || hourOfDay > 23) {
            return -1;
        }
        if (hourOfDay < 6) {
            return 0;
        }
        if (hourOfDay < 9) {
            return 1;
        }
        if (hourOfDay < 12) {
            return 2;
        }
        if (hourOfDay == 12) {
            return 3;
        }
        if (hourOfDay < 18) {
            return 4;
        }
        if (hourOfDay == 18) {
            return 5;
        }
        return 6;
    }

    public static String periodLabel(int hourOfDay) {
        int index = periodIndex(hourOfDay);
        return index < 0 ? null : PERIOD_LABELS[index];
    }

    public static String[] periodLabels() {
        return Arrays.copyOf(PERIOD_LABELS, PERIOD_LABELS.length);
    }

    /** Returns a stable hour used when a seven-item period spinner is selected. */
    public static int representativeHour(int periodIndex) {
        if (periodIndex < 0 || periodIndex >= PERIOD_REPRESENTATIVE_HOURS.length) {
            return -1;
        }
        return PERIOD_REPRESENTATIVE_HOURS[periodIndex];
    }

    public static boolean isChinese(Locale locale) {
        return locale != null && "zh".equalsIgnoreCase(locale.getLanguage());
    }

    public static String desktopFormat(boolean is24Hour) {
        return is24Hour ? "HH:mm:ss" : "hh:mm:ss";
    }

    /**
     * Adds seconds to a TextClock pattern while preserving the OEM's other
     * fields. Patterns without minutes receive a conservative ":ss" suffix.
     */
    public static String withSeconds(String pattern, boolean is24Hour) {
        if (pattern == null || pattern.isEmpty()) {
            return desktopFormat(is24Hour);
        }
        if (containsSeconds(pattern)) {
            return pattern;
        }
        int minute = pattern.lastIndexOf(":mm");
        if (minute >= 0) {
            return pattern.substring(0, minute + 3) + ":ss"
                    + pattern.substring(minute + 3);
        }
        return pattern + ":ss";
    }

    public static boolean containsSeconds(String pattern) {
        if (pattern == null) {
            return false;
        }
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && c == 's') {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesResourceName(String resourceName, String... candidates) {
        if (resourceName == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && candidate.equals(resourceName)) {
                return true;
            }
        }
        return false;
    }

    /** Adds one Chinese period prefix and removes a prefix previously added by this module. */
    public static String withPeriodPrefix(String text, int hourOfDay) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        String label = periodLabel(hourOfDay);
        if (label == null) {
            return text;
        }
        String value = text.trim();
        for (String existing : PERIOD_LABELS) {
            if (value.equals(existing)) {
                value = "";
                break;
            }
            if (value.startsWith(existing)) {
                value = value.substring(existing.length()).trim();
                break;
            }
        }
        return value.isEmpty() ? label : label + " " + value;
    }
}
