package dev.lackluster.redmagichelper.util;

import android.util.Log;

import io.github.libxposed.api.XposedModule;

/** Small logger that also works when the module is running inside a system process. */
public final class HookLog {
    public static final String TAG = "RMH_REWRITE";

    private static volatile XposedModule framework;

    private HookLog() {
    }

    /**
     * Keep a framework logger in addition to Android's log buffer.  On newer
     * Android builds the shell may not be allowed to read an application's
     * main log buffer, while the LSPosed log remains available for diagnosis.
     */
    public static void attach(XposedModule module) {
        framework = module;
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

    private static void write(int priority, String component, String message, Throwable error) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String level = getLevelString(priority);
        String feature = getFriendlyFeature(component);
        String line = "[" + time + "][" + level + "][" + feature + "][" + message + "]";
        if (error == null) {
            Log.println(priority, TAG, line);
        } else {
            Log.println(priority, TAG, line + "\n" + Log.getStackTraceString(error));
        }

        XposedModule current = framework;
        if (current == null) {
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
}
