package dev.lackluster.redmagichelper.hook.adapters;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Floating game-panel boolean capability adapter. */
public final class GameFloatAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        boolean gameFeatures = session.config().gameFeaturesEnabled();
        boolean forceOverlay = session.config().getBoolean("game_force_overlay_capability", false);
        boolean hidePrompt = session.config().getBoolean("game_space_devil_mode_hide_prompt", false);
        if (!gameFeatures && !forceOverlay && !hidePrompt) {
            session.log("GAME_FEATURES_DISABLED");
            return;
        }
        if (hidePrompt) {
            Class<?> dialog = session.findClass(
                    "cn.zte.gamefloat.gamekeys.entity.GameChickenModeDialog$ChickenModeDialog");
            session.hook(dialog, "show", chain -> null);
        }
        if (forceOverlay || gameFeatures) {
            Class<?> floatUtil = session.findOptionalClass("cn.zte.gamefloat.utils.FloatWindowUtils");
            if (floatUtil != null) {
                session.hook(floatUtil, "isSupportFloatWindow", chain -> Boolean.TRUE);
                session.hook(floatUtil, "canDrawOverlays", chain -> Boolean.TRUE);
            }
            session.hook(android.provider.Settings.class, "canDrawOverlays", chain -> Boolean.TRUE, android.content.Context.class);
            session.log("GAME_FLOAT_FORCE_OVERLAY_CAPABILITY_ENABLED");
        }
        session.log("GAME_FLOAT_CAPABILITY_SCAN_COMPLETE");
    }
}
