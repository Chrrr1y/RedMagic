package dev.lackluster.redmagichelper.hook.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.lang.reflect.Method;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** System-update hooks resolved against the framework's UpdateEngine API. */
public final class SystemUpdateAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        boolean disable = session.config().getBoolean("disable_system_update", false);
        boolean copyUrl = session.config().getBoolean("update_package_address", false);
        if (!disable && !copyUrl) {
            session.log("SYSTEM_UPDATE_DISABLED");
            return;
        }

        Class<?> updateEngine = session.findClass("android.os.UpdateEngine");
        session.hook(updateEngine, "applyPayload", chain -> {
            String url = HookValues.stringArg(chain, 0);
            if (copyUrl && url != null) {
                copyToSystemUiClipboard(url);
            }
            if (disable) {
                session.log("SYSTEM_UPDATE_BLOCKED url=" + url);
                return null;
            }
            return chain.proceed();
        }, String.class, long.class, long.class, String[].class);
        session.log("SYSTEM_UPDATE_CAPABILITY_SCAN_COMPLETE");
    }

    private static void copyToSystemUiClipboard(String url) {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method current = activityThread.getDeclaredMethod("currentActivityThread");
            Object thread = current.invoke(null);
            Method getSystemContext = activityThread.getDeclaredMethod("getSystemContext");
            Context context = thread == null ? null : (Context) getSystemContext.invoke(thread);
            if (context == null) {
                return;
            }
            Context systemUi = context.createPackageContext("com.android.systemui", Context.CONTEXT_INCLUDE_CODE);
            ClipboardManager manager = (ClipboardManager) systemUi.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("SystemUpdateURL", url));
            }
        } catch (Throwable ignored) {
            // Clipboard support is optional; update handling must remain fail-open.
        }
    }
}
