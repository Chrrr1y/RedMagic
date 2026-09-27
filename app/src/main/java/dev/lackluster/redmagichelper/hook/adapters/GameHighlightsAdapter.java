package dev.lackluster.redmagichelper.hook.adapters;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Screen-recording compatibility hooks for GameHighlights. */
public final class GameHighlightsAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (!session.config().getBoolean("disable_flag_secure_enhanced", false)
                && !session.config().getBoolean("disable_flag_secure", false)) {
            session.log("GAME_HIGHLIGHTS_SCREEN_CAPTURE_DISABLED");
            return;
        }

        Class<?> gate = session.findClass("p.p");
        // The current p.p gate exposes no-argument boolean accessors.  The
        // older build accepted a boolean parameter; passing that stale
        // signature made screen-recording bypass silently miss both gates.
        session.hook(gate, "L", GameHighlightsAdapter::recordingGate);
        session.hook(gate, "M", GameHighlightsAdapter::recordingGate);

        Class<?> callback = session.findClass(
                "cn.nubia.gamehighlights.Activity.MainActivity$g$a");
        session.hook(callback, "c", chain -> chain.proceed(), int.class);
        session.log("GAME_HIGHLIGHTS_CAPABILITY_SCAN_COMPLETE");
    }

    private static Object recordingGate(io.github.libxposed.api.XposedInterface.Chain chain)
            throws Throwable {
        Object result = chain.proceed();
        if (Boolean.TRUE.equals(result)
                && HookValues.stackContains("MainActivity", "ScreenRecordService", "TopService")) {
            return Boolean.FALSE;
        }
        return result;
    }
}
