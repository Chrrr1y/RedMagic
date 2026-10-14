package dev.lackluster.redmagichelper.util;

public final class HookLogFormatter {
    private HookLogFormatter() {
    }

    public static String format(String time, String level, String tag, String host, String message) {
        StringBuilder sb = new StringBuilder();
        if (time != null && !time.isEmpty()) sb.append("[").append(time).append("]");
        if (level != null && !level.isEmpty()) sb.append("[").append(level).append("]");
        if (tag != null && !tag.isEmpty()) sb.append("[").append(tag).append("]");
        if (host != null && !host.isEmpty()) sb.append("[host=").append(host).append("]");
        if (message != null && !message.isEmpty()) sb.append("[").append(clean(message)).append("]");
        return sb.toString();
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
