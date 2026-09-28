package dev.lackluster.redmagichelper.util;

public final class HookLogFormatter {
    private HookLogFormatter() {
    }

    public static String format(
            String time,
            String level,
            String feature,
            String hostPackage,
            String message
    ) {
        return "[" + clean(time) + "][" + clean(level) + "][" + clean(feature)
                + "][host=" + clean(hostPackage) + "][" + clean(message) + "]";
    }

    public static String compactError(Throwable error) {
        if (error == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(error.getClass().getName());
        String message = error.getMessage();
        if (message != null && !message.isEmpty()) {
            result.append(": ").append(clean(message));
        }
        StackTraceElement[] stack = error.getStackTrace();
        int frameCount = Math.min(stack.length, 6);
        for (int i = 0; i < frameCount; i++) {
            result.append(" <- ").append(clean(stack[i].toString()));
        }
        return result.toString();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
