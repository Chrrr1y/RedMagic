package dev.lackluster.redmagichelper.util;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedModule;

/** Routes hook diagnostics to the helper app and keeps LSPosed output actionable. */
public final class HookLog {
    public static final String TAG = "RMH_REWRITE";
    private static final String PROVIDER_AUTHORITY =
            "content://dev.lackluster.redmagichelper.config_provider";
    private static final Uri PROVIDER_URI = Uri.parse(PROVIDER_AUTHORITY);
    private static final int BATCH_SIZE = 32;
    private static final int MAX_LOG_LINE_LENGTH = 4096;
    private static final long INFO_DUPLICATE_WINDOW_MS = 5000L;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm:ss", Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private static final ArrayBlockingQueue<String> INTERNAL_QUEUE =
            new ArrayBlockingQueue<>(512);
    private static final AtomicBoolean DRAIN_SCHEDULED = new AtomicBoolean();
    private static final Set<String> FRAMEWORK_WARNINGS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Long> RECENT_INFO_EVENTS =
            new ConcurrentHashMap<>();
    private static final AtomicBoolean PROVIDER_ERROR_REPORTED = new AtomicBoolean();
    private static final AtomicInteger DROPPED_LOG_EVENTS = new AtomicInteger();
    private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "RMH-HookLog");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private static volatile XposedModule framework;
    private static volatile ContentResolver contentResolver;

    private HookLog() {
    }

    public static void attach(XposedModule module) {
        attach(module, null);
    }

    public static void attach(XposedModule module, ContentResolver resolver) {
        if (module != null) {
            framework = module;
        }
        attachContentResolver(resolver);
    }

    public static void attachContentResolver(ContentResolver resolver) {
        if (resolver != null) {
            contentResolver = resolver;
            scheduleDrain();
        }
    }

    public static void debug(String component, String message) {
        write(Log.DEBUG, component, message, null);
    }

    public static void info(String component, String message) {
        write(Log.INFO, component, message, null);
    }

    public static void warn(String component, String message) {
        write(Log.WARN, component, message, null);
    }

    public static void missing(String component, String what) {
        write(Log.WARN, component, "CAPABILITY_MISSING " + what, null);
    }

    public static void error(String component, String message, Throwable error) {
        write(Log.ERROR, component, message, error);
    }

    private static String getLevelString(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "VERBOSE";
            case Log.DEBUG: return "DEBUG";
            case Log.INFO: return "INFO";
            case Log.WARN: return "WARN";
            case Log.ERROR: return "ERROR";
            default: return "LOG";
        }
    }

    private static String getFriendlyFeature(String component) {
        if (component == null) return "未知模块";
        switch (component) {
            case "cn.nubia.browser": return "浏览器适配";
            case "com.zte.beautify": return "桌面与个性化";
            case "com.android.systemui": return "系统界面";
            case "cn.nubia.gameassist":
            case "cn.nubia.gamelauncher":
            case "cn.nubia.gamehelperline":
            case "cn.nubia.gamehelpmodule":
            case "cn.zte.gamefloat":
            case "com.zte.game.plugintrigger": return "游戏优化";
            case "cn.nubia.filebrowser": return "MTP分类浏览";
            case "cn.nubia.neostore":
            case "cn.nubia.neogamecenter": return "应用中心";
            case "com.android.settings":
            case "com.android.packageinstaller": return "系统设置";
            case "com.android.nfc": return "NFC服务";
            case "android": return "系统核心";
            case "com.zte.mifavor.launcher": return "桌面";
            default: return component;
        }
    }

    private static void write(
            int priority,
            String component,
            String message,
            Throwable error
    ) {
        if (priority == Log.INFO && !shouldLogInfo(component, message)) {
            return;
        }
        String time = TIME_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
        String detail = message == null ? "" : message;
        if (error != null) {
            detail += " error=" + HookLogFormatter.compactError(error);
        }
        String line = HookLogFormatter.format(
                time, getLevelString(priority), getFriendlyFeature(component),
                component, detail);
        if (line.length() > MAX_LOG_LINE_LENGTH) {
            line = line.substring(0, MAX_LOG_LINE_LENGTH);
        }
        enqueueInternal(line);

        if (priority >= Log.ERROR) {
            Log.println(priority, TAG, line);
        }
        if (priority < Log.WARN) {
            return;
        }

        XposedModule current = framework;
        if (current == null) {
            return;
        }
        if (priority == Log.WARN
                && !detail.startsWith("HOOK_SCAN_CAPABILITY_WARNINGS")) {
            return;
        }
        if (priority == Log.WARN && !FRAMEWORK_WARNINGS.add(component + "\n" + detail)) {
            return;
        }
        try {
            if (error == null) {
                current.log(priority, TAG, line);
            } else {
                current.log(priority, TAG, line, error);
            }
        } catch (Throwable ignored) {
            // Logging must never prevent a host hook from being installed.
        }
    }

    private static void enqueueInternal(String line) {
        if (contentResolver == null) {
            return;
        }
        if (!INTERNAL_QUEUE.offer(line)) {
            DROPPED_LOG_EVENTS.incrementAndGet();
            return;
        }
        scheduleDrain();
    }

    private static boolean shouldLogInfo(String component, String message) {
        long now = SystemClock.elapsedRealtime();
        String key = String.valueOf(component) + '\n' + String.valueOf(message);
        AtomicBoolean accepted = new AtomicBoolean();
        RECENT_INFO_EVENTS.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= INFO_DUPLICATE_WINDOW_MS) {
                accepted.set(true);
                return now;
            }
            return previous;
        });
        if (RECENT_INFO_EVENTS.size() > 512) {
            RECENT_INFO_EVENTS.clear();
        }
        return accepted.get();
    }

    private static void scheduleDrain() {
        if (contentResolver != null && DRAIN_SCHEDULED.compareAndSet(false, true)) {
            LOG_EXECUTOR.execute(HookLog::drainQueue);
        }
    }

    private static void drainQueue() {
        try {
            SystemClock.sleep(100L);
            while (contentResolver != null && !INTERNAL_QUEUE.isEmpty()) {
                ArrayList<String> batch = new ArrayList<>(BATCH_SIZE);
                INTERNAL_QUEUE.drainTo(batch, BATCH_SIZE);
                if (batch.isEmpty()) {
                    break;
                }
                Bundle extras = new Bundle();
                extras.putStringArrayList("lines", batch);
                String failure = "provider unavailable";
                boolean stored = false;
                for (int attempt = 0; attempt < 4 && !stored; attempt++) {
                    try {
                        ContentResolver resolver = contentResolver;
                        if (resolver == null) {
                            failure = "resolver unavailable";
                            break;
                        }
                        Bundle result = resolver.call(
                                PROVIDER_URI, "appendHookLogs", null, extras);
                        if (result != null && result.getBoolean("success", false)) {
                            stored = true;
                        } else {
                            failure = result == null ? "empty provider response"
                                    : result.getString("error", "write rejected");
                        }
                    } catch (Throwable error) {
                        failure = HookLogFormatter.compactError(error);
                    }
                    if (!stored && attempt < 3) {
                        SystemClock.sleep(200L * (attempt + 1));
                    }
                }
                if (!stored) {
                    reportProviderFailure(failure);
                }
            }
        } finally {
            int dropped = DROPPED_LOG_EVENTS.getAndSet(0);
            if (dropped > 0) {
                String summary = HookLogFormatter.format(
                        TIME_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis())),
                        "WARN", "日志系统", "logger",
                        "LOG_EVENTS_DROPPED count=" + dropped);
                if (!INTERNAL_QUEUE.offer(summary)) {
                    DROPPED_LOG_EVENTS.addAndGet(dropped);
                }
            }
            DRAIN_SCHEDULED.set(false);
            if (!INTERNAL_QUEUE.isEmpty()) {
                scheduleDrain();
            }
        }
    }

    private static void reportProviderFailure(String detail) {
        if (PROVIDER_ERROR_REPORTED.compareAndSet(false, true)) {
            Log.w(TAG, "internal hook log provider write failed: " + detail);
        }
    }
}
