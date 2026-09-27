package dev.lackluster.redmagichelper.hook.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import java.lang.reflect.Method;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Game-space hooks reconstructed from the offline dispatcher and host APKs. */
public final class GameLauncherAdapter implements HostAdapter {
    private static volatile String lastKnownLinePackage = null;

    @Override
    public void install(HookSession session) {
        boolean shoulderEnabled = session.config().getBoolean(
                "game_shoulder_key_enabled", false)
                || session.config().getBoolean("game_assist_shoulder_enabled", false);
        boolean comboEnabled = session.config().getBoolean(
                "game_space_combo_speed_enabled", false);
        boolean superResolutionEnabled = session.config().gameSuperResolutionUnlimitedApps();
        boolean forceOverlay = session.config().getBoolean("game_force_overlay_capability", false);
        if (!shoulderEnabled && !comboEnabled && !superResolutionEnabled && !forceOverlay) {
            session.log("GAME_FEATURES_DISABLED");
            return;
        }

        // GameHelperLine has its own hard-coded RedMagic blacklist. For
        // 王者荣耀 it returns true from this method and LineService stops
        // itself before the shoulder-key panel can be created. This process
        // uses the exact final gate, so bypass it here instead of relying on
        // GameLauncher/TGK hooks in another process.
        if (shoulderEnabled) {
            Class<?> lineBlacklist = session.findOptionalClass(
                    "cn.nubia.gamehelperline.utils.BlacklistUtils");
            if (lineBlacklist != null) {
                session.hook(lineBlacklist, "isDisableRangeLine",
                        chain -> Boolean.FALSE, String.class);
                session.log("GAME_SHOULDER_LINE_BLACKLIST_BYPASSED");
            }
        }
        Class<?> lineService = session.findOptionalClass(
                "cn.nubia.gamehelperline.LineService");
        if (shoulderEnabled && lineService != null) {
            session.hook(lineService, "isLineNotSupported",
                    chain -> Boolean.FALSE, String.class);
            session.hook(lineService, "isNotSupportedShowView",
                    chain -> Boolean.FALSE);
            session.hook(android.provider.Settings.class, "canDrawOverlays",
                    chain -> Boolean.TRUE, Context.class);

            session.hook(lineService, "onStartCommand", chain -> {
                Object intentObj = HookValues.arg(chain, 0);
                if (intentObj instanceof android.content.Intent) {
                    android.content.Intent intent = (android.content.Intent) intentObj;
                    String pkg = intent.getStringExtra("packageName");
                    if (pkg == null || pkg.isEmpty()) {
                        pkg = intent.getStringExtra("packagename");
                    }
                    if (pkg != null && !pkg.isEmpty()) {
                        lastKnownLinePackage = pkg;
                        intent.putExtra("packageName", pkg);
                        intent.putExtra("packagename", pkg);
                    }
                    String action = intent.getAction();
                    if ("cn.nubia.gamehelperline.ACTION_OPEN_LINE_MAIN_VIEW".equals(action)) {
                        Object owner = chain.getThisObject();
                        Object sceneController = HookValues.readField(owner, "mSceneController");
                        if (sceneController != null && pkg != null && !pkg.isEmpty()) {
                            Method setPkg = HookValues.findMethod(sceneController, "setCurrentPackageName", String.class);
                            if (setPkg != null) {
                                setPkg.invoke(sceneController, pkg);
                                session.log("LINE_SERVICE_OPEN_MAIN_SET_PKG pkg=" + pkg);
                            }
                        }
                    }
                }
                return chain.proceed();
            }, android.content.Intent.class, int.class, int.class);

            Class<?> pkgUtils = session.findOptionalClass("cn.nubia.gamehelperline.utils.PackageUtils");
            if (pkgUtils != null) {
                session.hook(pkgUtils, "getGamePkgList", chain -> {
                    Object orig = chain.proceed();
                    final java.util.List<?> originalList = (orig instanceof java.util.List)
                            ? (java.util.List<?>) orig : java.util.Collections.emptyList();
                    Context ctx = (Context) HookValues.arg(chain, 0);
                    return new java.util.AbstractList<Object>() {
                        @Override
                        public Object get(int index) {
                            return originalList.get(index);
                        }
                        @Override
                        public int size() {
                            return originalList.size();
                        }
                        @Override
                        public boolean contains(Object o) {
                            if (originalList.contains(o)) {
                                return true;
                            }
                            if (o instanceof String) {
                                String s = (String) o;
                                if (isIgnoredLinePackage(s)) {
                                    return false;
                                }
                                if ("com.tencent.tmgp.sgame".equals(s) || "com.tencent.tmgp.dfm".equals(s)) {
                                    return true;
                                }
                                if (ctx != null) {
                                    try {
                                        String enabledPkgs = Settings.Global.getString(ctx.getContentResolver(), "gamehelperline_enable_pkgs");
                                        if (enabledPkgs != null && enabledPkgs.contains(s)) {
                                            return true;
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                            return false;
                        }
                    };
                }, Context.class);
            }

            session.log("GAME_AUX_LINE_SUPPORT_CHECKS_BYPASSED");
        }

        installSafeEntryHooks(session, shoulderEnabled, comboEnabled, superResolutionEnabled, forceOverlay);
        session.log("GAME_LAUNCHER_SAFE_ENTRY_HOOKS_INSTALLED");
    }

    private static void installSafeEntryHooks(
            HookSession session, boolean shoulderEnabled, boolean comboEnabled,
            boolean superResolutionEnabled, boolean forceOverlay) {
        Class<?> featureUtil = session.findOptionalClass(
                "cn.nubia.common.util.FeatureUtil");
        if (featureUtil != null && (shoulderEnabled || comboEnabled)) {
            session.hook(featureUtil, "supportGameKey", chain -> Boolean.TRUE);
            session.log("GAME_SPACE_GAME_KEY_ENTRY_ENABLED");
        }

        Class<?> gameSpaceConfig = session.findOptionalClass(
                "cn.nubia.common.config.GameSpaceConfig");
        if (gameSpaceConfig != null && (shoulderEnabled || comboEnabled)) {
            session.hook(gameSpaceConfig, "supportGameKey", chain -> Boolean.TRUE);
        }

        Class<?> controlPanelFeatures = session.findOptionalClass(
                "cn.nubia.gamelauncher.gamecontrolpanel.utils.ControlPanelFeatureHelper");
        if (controlPanelFeatures != null && (shoulderEnabled || comboEnabled)) {
            // This is the concrete shoulder-key capability gate used by the
            // allocation panel. It is deliberately opt-in and feature-scoped.
            session.hook(controlPanelFeatures, "isTouchGameKeySupported",
                    chain -> Boolean.TRUE);
            // The OEM precision/quick-turn tile has a hard-coded game list
            // that contains DFM but omits 王者荣耀. Expose the tile while
            // leaving its per-game on/off setting in Settings.Global intact.
            session.hook(controlPanelFeatures, "supportPrecisionByFeature",
                    chain -> Boolean.TRUE, String.class);
        }

        Class<?> superResolution = session.findOptionalClass(
                "cn.nubia.gamelauncher.gamecontrolpanel.superresolution.SuperResolutionHelper");
        if (superResolution != null && superResolutionEnabled) {
            session.hook(superResolution, "supportSuperResolution",
                    chain -> Boolean.TRUE);
            session.hook(superResolution, "supportSuperResolutionByPkgName",
                    chain -> Boolean.TRUE, String.class);
            // The OEM helper uses this list to populate the settings dialog.
            // Its own DEFAULT_SUPPORT_ITEM is exactly these two gears.
            session.hook(superResolution, "getSupportResolutionGear", chain -> {
                Object result = chain.proceed();
                if (!(result instanceof String[]) || ((String[]) result).length == 0) {
                    return new String[]{"1116", "1440"};
                }
                return result;
            }, String.class);
            session.hook(superResolution, "openSuperResolution", chain -> {
                Object ctx = HookValues.arg(chain, 0);
                Object pkg = HookValues.arg(chain, 1);
                if (pkg instanceof String && !((String) pkg).isEmpty()) {
                    GameAssistAdapter.applyGameMasoProperties((String) pkg);
                    if (ctx instanceof Context) {
                        GameAssistAdapter.syncGfrcModeSettings((Context) ctx, (String) pkg);
                    }
                }
                return chain.proceed();
            }, Context.class, String.class, int.class);
            session.log("GAME_SUPER_RESOLUTION_ALL_APPS_ENABLED");
        }

        Class<?> tgkHelper = session.findOptionalClass("cn.nubia.tgk.TgkHelper");
        if (tgkHelper != null && (shoulderEnabled || comboEnabled)) {
            for (Method method : session.findMethods(tgkHelper, "disableTgkFunction")) {
                session.hook(method, chain -> Boolean.FALSE);
            }
            for (Method method : session.findMethods(tgkHelper, "getTgkDisableOpt")) {
                session.hook(method, chain -> Integer.valueOf(0));
            }
            session.log("GAME_KEY_ENTRY_RESTRICTIONS_DISABLED");
        }

        Class<?> tgkMap = session.findOptionalClass("cn.nubia.tgk.TgkMapView");
        if (tgkMap != null && (shoulderEnabled || comboEnabled)) {
            session.hook(tgkMap, "getGameKeyLinkMotionState", chain -> {
                Object result = chain.proceed();
                HookValues.writeField(chain.getThisObject(), Boolean.TRUE,
                        "mSupportedGameKeyLink", "supportedGameKeyLink",
                        "mGameKeyLinkSupported", "gameKeyLinkSupported");
                return result;
            });
        }

        Class<?> pluginConfig = session.findOptionalClass(
                "cn.nubia.gamelauncher.gamecontrolpanel.config.PluginConfig");
        if (pluginConfig != null) {
            session.hook(pluginConfig, "getBlackList", chain -> {
                Object result = chain.proceed();
                Object key = HookValues.arg(chain, 1);
                if (key instanceof String && isSafeEntryFeature(
                        (String) key, shoulderEnabled, comboEnabled, superResolutionEnabled)) {
                    return HookValues.removeKnownPackages(result);
                }
                return result;
            }, Context.class, String.class);
        }

        if (comboEnabled) {
            int speedRate = HookValues.clamp(
                    session.config().getInt("game_space_combo_speed_rate", 1), 1, 10);
            if (speedRate > 1) {
                session.hook(Handler.class, "sendMessageDelayed", chain -> {
                    Object owner = chain.getThisObject();
                    if (owner != null && owner.getClass().getName().contains("tgk")) {
                        long delay = HookValues.longArg(chain, 1, 0L);
                        if (delay > 0) {
                            return HookValues.proceedWithArgument(chain, 1, Math.max(1L, delay / speedRate));
                        }
                    }
                    return chain.proceed();
                }, Message.class, long.class);

                session.hook(Handler.class, "sendEmptyMessageDelayed", chain -> {
                    Object owner = chain.getThisObject();
                    if (owner != null && owner.getClass().getName().contains("tgk")) {
                        long delay = HookValues.longArg(chain, 1, 0L);
                        if (delay > 0) {
                            return HookValues.proceedWithArgument(chain, 1, Math.max(1L, delay / speedRate));
                        }
                    }
                    return chain.proceed();
                }, int.class, long.class);
                session.log("GAME_COMBO_SPEED_ACCELERATION_APPLIED rate=" + speedRate);
            }
        }

        if (forceOverlay) {
            session.hook(Settings.class, "canDrawOverlays", chain -> Boolean.TRUE, Context.class);
            if (controlPanelFeatures != null) {
                session.hook(controlPanelFeatures, "isFloatAppSupported", chain -> Boolean.TRUE);
                session.hook(controlPanelFeatures, "isSupportFloatWindow", chain -> Boolean.TRUE);
                session.hook(controlPanelFeatures, "isSupportAppFloat", chain -> Boolean.TRUE);
            }
            session.log("GAME_FORCE_OVERLAY_CAPABILITY_ENABLED");
        }
    }

    private static boolean isSafeEntryFeature(
            String feature, boolean shoulderEnabled, boolean comboEnabled,
            boolean superResolutionEnabled) {
        if ("range_line".equals(feature)) {
            return shoulderEnabled;
        }
        if ("keylink".equals(feature) || "touch_long_keylink_point".equals(feature)) {
            return comboEnabled;
        }
        return superResolutionEnabled
                && ("super_resolution".equals(feature)
                || "super_resolution_old".equals(feature));
    }

    private static boolean isIgnoredLinePackage(String pkg) {
        if (pkg == null || pkg.isEmpty()) return true;
        return "com.zte.mifavor.launcher".equals(pkg)
                || "cn.nubia.gamelauncher".equals(pkg)
                || "com.android.systemui".equals(pkg)
                || "dev.lackluster.redmagichelper".equals(pkg)
                || "cn.nubia.gameassist".equals(pkg)
                || "cn.nubia.gamehelperline".equals(pkg);
    }
}
