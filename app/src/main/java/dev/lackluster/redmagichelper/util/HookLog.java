package dev.lackluster.redmagichelper.util;

import android.util.Log;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Writes errors and one hook result per outcome for each host process. */
public final class HookLog {
    public static final String TAG = "RMH_REWRITE";

    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED_HOOK_RESULTS = ConcurrentHashMap.newKeySet();

    private HookLog() {
    }

    public static void error(String component, String message, Throwable error) {
        String normalizedComponent = clean(component);
        String normalizedMessage = clean(message);
        String key = normalizedComponent + '\n' + errorEvent(normalizedMessage);
        if (!REPORTED_ERRORS.add(key)) {
            return;
        }

        String line = "ERROR component=" + normalizedComponent
                + " event=" + normalizedMessage;
        if (error != null) {
            line += " cause=" + HookLogFormatter.compactError(error);
        }
        Log.e(TAG, line);
    }

    /**
     * Emits a single installation result for each target and outcome in this
     * process. Registration failures are collected by HookSession and appear
     * only in this aggregate line.
     */
    public static void hookResult(
            String component,
            int installed,
            int missing,
            int failed,
            String firstFailure,
            Throwable error
    ) {
        String normalizedComponent = clean(component);
        boolean hasErrors = failed > 0;
        String resultKey = normalizedComponent + '\n' + (hasErrors ? "failed" : "success");
        if (!REPORTED_HOOK_RESULTS.add(resultKey)) {
            return;
        }

        String line = "HOOK_RESULT component=" + normalizedComponent
                + " status=" + (hasErrors ? "failed" : "success")
                + " installed=" + installed
                + " missing=" + missing
                + " failed=" + failed;
        if (firstFailure != null && !firstFailure.isEmpty()) {
            line += " firstFailure=" + clean(firstFailure);
        }
        if (error != null) {
            line += " cause=" + HookLogFormatter.compactError(error);
        }
        if (hasErrors) {
            Log.e(TAG, line);
        } else {
            Log.i(TAG, line);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String errorEvent(String message) {
        int end = message.length();
        int space = message.indexOf(' ');
        int colon = message.indexOf(':');
        if (space >= 0) {
            end = Math.min(end, space);
        }
        if (colon >= 0) {
            end = Math.min(end, colon);
        }
        return message.substring(0, end);
    }
}
