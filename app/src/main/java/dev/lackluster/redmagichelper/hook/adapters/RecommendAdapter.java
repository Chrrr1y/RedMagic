package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Intent;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Window-reply recommendation/icon capacity adapter. */
public final class RecommendAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (!session.config().getBoolean("remove_restrictions_window_number", false)) {
            session.log("RECOMMEND_WINDOW_LIMIT_DISABLED");
            return;
        }
        // Older RedMagic builds exposed the reply-window UI in this package.
        // RedMagicOS 11 moved the actual limit enforcement to system_server;
        // SystemServerAdapter handles that path. Keep this legacy branch
        // optional so enabling the setting cannot report a broken hook on the
        // current empty compatibility APK.
        Class<?> manager = session.findOptionalClass(
                "com.zte.wr.WindowReplyIconManager");
        if (manager != null) {
            session.hook(manager, "iconShowIsError", chain -> Boolean.FALSE, int.class);
        }
        Class<?> service = session.findOptionalClass("com.zte.wr.WindowReplyService");
        if (service != null) {
            session.hook(service, "onStartCommand", chain -> chain.proceed(),
                    Intent.class, int.class, int.class);
        }
        session.log("RECOMMEND_CAPABILITY_SCAN_COMPLETE");
    }
}
