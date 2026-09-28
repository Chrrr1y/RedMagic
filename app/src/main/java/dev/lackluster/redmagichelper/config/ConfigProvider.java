package dev.lackluster.redmagichelper.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import dev.lackluster.redmagichelper.util.HookRuntimeLogStore;

/**
 * Cross-process configuration provider allowing all hooked target applications
 * to reliably fetch the user's latest settings directly from the helper application's
 * private SharedPreferences without depending on LSPosed framework IPC push callbacks.
 */
public final class ConfigProvider extends ContentProvider {
    public static final String AUTHORITY = "dev.lackluster.redmagichelper.config_provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        if ("appendHookLogs".equals(method)) {
            ArrayList<String> lines = extras == null
                    ? null : extras.getStringArrayList("lines");
            if (lines == null || lines.size() > 64) {
                return failure("invalid_log_batch");
            }
            ArrayList<String> boundedLines = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (line != null) {
                    boundedLines.add(line.length() > 4096 ? line.substring(0, 4096) : line);
                }
            }
            try {
                HookRuntimeLogStore.append(context, boundedLines);
                return result(true);
            } catch (Throwable error) {
                return failure(error.getClass().getSimpleName() + ":" + error.getMessage());
            }
        } else if ("getHookLogs".equals(method)) {
            if (Binder.getCallingUid() != Process.myUid()) {
                return result(false);
            }
            try {
                Bundle result = result(true);
                result.putStringArrayList("lines", HookRuntimeLogStore.readRecent(context));
                return result;
            } catch (Throwable ignored) {
                return failure(ignored.getClass().getSimpleName());
            }
        } else if ("clearHookLogs".equals(method)) {
            if (Binder.getCallingUid() != Process.myUid()) {
                return result(false);
            }
            return result(HookRuntimeLogStore.clear(context));
        }

        SharedPreferences sp = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        Bundle bundle = new Bundle();

        if ("getAll".equals(method)) {
            Map<String, ?> all = sp.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Boolean) {
                    bundle.putBoolean(key, (Boolean) val);
                } else if (val instanceof Integer) {
                    bundle.putInt(key, (Integer) val);
                } else if (val instanceof Long) {
                    bundle.putLong(key, (Long) val);
                } else if (val instanceof Float) {
                    bundle.putFloat(key, (Float) val);
                } else if (val instanceof String) {
                    bundle.putString(key, (String) val);
                } else if (val instanceof Set<?>) {
                    @SuppressWarnings("unchecked")
                    Set<String> set = (Set<String>) val;
                    bundle.putStringArrayList(key, new ArrayList<>(set));
                }
            }
            return bundle;
        } else if ("get".equals(method) && arg != null) {
            Object val = sp.getAll().get(arg);
            if (val instanceof Boolean) {
                bundle.putBoolean(arg, (Boolean) val);
            } else if (val instanceof Integer) {
                bundle.putInt(arg, (Integer) val);
            } else if (val instanceof Long) {
                bundle.putLong(arg, (Long) val);
            } else if (val instanceof Float) {
                bundle.putFloat(arg, (Float) val);
            } else if (val instanceof String) {
                bundle.putString(arg, (String) val);
            }
            return bundle;
        }
        return bundle;
    }

    private static Bundle result(boolean success) {
        Bundle result = new Bundle();
        result.putBoolean("success", success);
        return result;
    }

    private static Bundle failure(String reason) {
        Bundle result = result(false);
        result.putString("error", reason == null ? "unknown" : reason);
        return result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
