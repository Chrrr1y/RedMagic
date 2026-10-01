package dev.lackluster.redmagichelper.hook.adapters;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.GfrcCapabilityPolicy;
import dev.lackluster.redmagichelper.util.GfrcInputFramePolicy;
import io.github.libxposed.api.XposedInterface;

/** Android/system-server hooks for API 36 with candidate-based OEM fallbacks. */
public final class SystemServerAdapter implements HostAdapter {
    private static final String GFRC_DATA_CONTROLLER_CLASS =
            "cn.nubia.server.cube.datacenter.f0";
    private static final String CUBE_SYSTEM_SERVICE_CLASS =
            "cn.nubia.server.cube.CubeService";

    @Override
    public void install(HookSession session) {
        if (session.config().getBoolean("remove_alert_windows_notification", false)) {
            Class<?> notification = session.findClass("com.android.server.wm.AlertWindowNotification");
            session.hook(notification, "post", chain -> null);
            session.hook(notification, "onPostNotification", chain -> null);
        }

        if (session.config().getBoolean("volume_disable_safety_warning", false)) {
            Class<?> soundDose = session.findClass(
                    "com.android.server.audio.SoundDoseHelper",
                    "com.android.server.audio.AudioService$SoundDoseHelper",
                    "com.android.server.audio.AudioService"
            );
            session.hook(soundDose, "checkSafeMediaVolume", chain -> {
                Object original = chain.proceed();
                return isBooleanReturn(chain) ? Boolean.TRUE : original;
            });
            session.hook(soundDose, "raiseVolumeDisplaySafeMediaVolume", chain -> null);
            session.hook(soundDose, "willDisplayWarningAfterCheckVolume", chain -> Boolean.FALSE);
            Class<?> audioService = session.findClass("com.android.server.audio.AudioService");
            // Recent builds keep this gate only in SoundDoseHelper.  Hook the
            // legacy AudioService declaration only when a vendor build still
            // exposes it, otherwise avoid a false capability warning.
            if (!session.findMethods(audioService, "checkSafeMediaVolume").isEmpty()) {
                session.hook(audioService, "checkSafeMediaVolume", chain -> {
                    Object original = chain.proceed();
                    return isBooleanReturn(chain) ? Boolean.TRUE : original;
                });
            }
        }

        boolean disableSecure = session.config().getBoolean("disable_flag_secure", false);
        boolean disableSecureEnhanced = session.config().getBoolean(
                "disable_flag_secure_enhanced", false);
        if (disableSecure || disableSecureEnhanced) {
            Class<?> windowState = session.findClass(
                    "com.android.server.wm.WindowState",
                    "com.android.server.wm.WindowStateAnimator"
            );
            session.hook(windowState, "isSecureLocked", chain -> {
                Object original = chain.proceed();
                if (HookValues.stackContainsMethod(
                        "setInitialSurfaceControlProperties", "createSurfaceLocked")) {
                    return original;
                }
                return Boolean.FALSE;
            });

            if (disableSecureEnhanced) {
                Class<?> buffer = session.findClass(
                        "android.window.ScreenCapture$ScreenshotHardwareBuffer");
                session.hook(buffer, "containsSecureLayers", chain -> Boolean.FALSE);
                // API 36 on this ROM folded the internal entry points into
                // ScreenCapture and no longer ships ScreenCaptureInternal.
                // Keep this optional so the valid ScreenCapture hooks below
                // remain usable without a false capability failure.
                Class<?> captureInternal = session.findOptionalClass(
                        "android.window.ScreenCaptureInternal");
                Class<?> capture = session.findClass("android.window.ScreenCapture");
                installCapturePolicyHooks(session, captureInternal);
                installCapturePolicyHooks(session, capture);
            }
        }

        if (session.config().getBoolean("android_remove_intent_hijack_content", false)) {
            // RedMagicOS 11 moved the vendor CTS decision into the companion
            // hook class; older releases put it directly on ResolveIntentHelper.
            Class<?> resolver = session.findClass(
                    "com.android.server.pm.ResolveIntentHelperHook",
                    "com.android.server.pm.ResolveIntentHelper");
            session.hook(resolver, "isCtsTesting", chain -> Boolean.TRUE);
        }

        if (session.config().getBoolean("remove_restrictions_window", false)) {
            Class<?> replyUtils = session.findOptionalClass("android.app.WindowReplyUtils");
            if (replyUtils != null) {
                session.hook(replyUtils, "isForceSupportWhiteListForWR", chain -> Boolean.TRUE);
            }
            Class<?> atm = session.findOptionalClass("com.android.server.wm.ActivityTaskManagerService");
            if (atm != null) {
                session.hookAny(atm, new String[]{
                        "isSupportFreeformWindow", "supportMultiWindow", "isSupportSplitScreen"
                }, chain -> Boolean.TRUE);
            }
            Class<?> task = session.findOptionalClass("com.android.server.wm.Task");
            if (task != null) {
                session.hook(task, "isResizeable", chain -> Boolean.TRUE);
            }
            Class<?> zteFeature = session.findOptionalClass("android.util.ZteFeature");
            if (zteFeature != null) {
                session.hook(zteFeature, "isSupportFreeform", chain -> Boolean.TRUE);
            }
        }
        if (session.config().getBoolean("remove_restrictions_window_number", false)) {
            Class<?> atm = session.findOptionalClass("com.android.server.wm.ActivityTaskManagerService");
            if (atm != null) {
                session.hook(atm, "isReachWrMaxSizeForMulti", chain -> Boolean.FALSE);
            }
        }

        if (session.config().getBoolean("system_framework_lock_screen_timeout", false)) {
            Class<?> power = session.findClass("com.android.server.power.PowerManagerService");
            int timeout = Math.max(1, session.config().getInt(
                    "system_framework_lock_screen_timeout_value", 10));
            long timeoutMillis = timeout * 60_000L;
            // Android 16's vendor service has two real declarations: the
            // framework calculation receives two long values, while the ZTE
            // policy path is a separate no-arg method. Hook whichever
            // declaration the ROM exposes, and retain the old no-arg path for
            // earlier builds without generating a false missing-method log.
            if (!session.findMethods(power, "getScreenOffTimeoutLocked",
                    long.class, long.class).isEmpty()) {
                session.hook(power, "getScreenOffTimeoutLocked",
                        chain -> timeoutMillis, long.class, long.class);
            }
            for (Method method : session.findMethods(power, "getScreenOffTimeoutLockedZte")) {
                if (method.getParameterTypes().length == 0) {
                    session.hook(method, chain -> timeoutMillis);
                }
            }
            for (Method method : session.findMethods(power, "getScreenOffTimeoutLocked")) {
                if (method.getParameterTypes().length == 0) {
                    session.hook(method, chain -> timeoutMillis);
                }
            }
        }

        if (session.config().gameSuperResolutionUnlimitedApps()) {
            try {
                Class<?> sysProp = Class.forName("android.os.SystemProperties");
                java.lang.reflect.Method setMethod = sysProp.getMethod("set", String.class, String.class);
                setMethod.invoke(null, "vendor.gpp.allgame.enable", "1");
            } catch (Throwable ignored) {
            }
            Class<?> trigger = session.findOptionalClass(
                    "com.zte.performance.mindsync.MindSyncManager$Trigger");
            if (trigger != null) {
                session.hook(trigger, "getGfrcCapByPkg", chain -> {
                    Object pkg = HookValues.arg(chain, 0);
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                    }
                    Object original = chain.proceed();
                    int capability = original instanceof Integer ? (Integer) original : 0;
                    return GfrcCapabilityPolicy.includeUltraModes(capability);
                }, String.class);
            }
            Class<?> cubeE = session.findOptionalClass("cn.nubia.server.cube.cpuperf.E");
            if (cubeE != null) {
                session.hook(cubeE, "getGfrcCapByPkg", chain -> {
                    Object pkg = HookValues.arg(chain, 0);
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                    }
                    Object original = chain.proceed();
                    int capability = original instanceof Integer ? (Integer) original : 0;
                    return GfrcCapabilityPolicy.includeUltraModes(capability);
                }, String.class);
            }
            Class<?> cubeN = session.findOptionalClass("cn.nubia.server.cube.plugs.N");
            if (cubeN != null) {
                session.hook(cubeN, "j", chain -> {
                    Object pkg = HookValues.arg(chain, 0);
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                    }
                    Object original = chain.proceed();
                    int capability = original instanceof Integer ? (Integer) original : 0;
                    return GfrcCapabilityPolicy.includeUltraModes(capability);
                }, String.class);
            }
            session.log("SYSTEM_SERVER_SUPER_RESOLUTION_ALL_APPS_ENABLED");
        } else {
            try {
                Class<?> sysProp = Class.forName("android.os.SystemProperties");
                java.lang.reflect.Method setMethod = sysProp.getMethod("set", String.class, String.class);
                setMethod.invoke(null, "vendor.gpp.allgame.enable", "0");
            } catch (Throwable ignored) {
            }
        }
        installSm8750GfrcInputLimitHook(session);

        boolean gameOverlayAllowed = session.config().getBoolean("game_force_overlay_capability", false)
                || session.config().getBoolean("game_fuction_unfreeze_switch", false)
                || session.config().getBoolean("game_shoulder_key_enabled", false);
        if (gameOverlayAllowed) {
            Class<?> appOpsService = session.findOptionalClass("com.android.server.appop.AppOpsService");
            if (appOpsService != null) {
                for (Method m : session.findMethods(appOpsService, "checkOperation")) {
                    session.hook(m, chain -> {
                        Object codeObj = HookValues.arg(chain, 0);
                        Object pkgObj = HookValues.arg(chain, 2);
                        if (codeObj instanceof Integer && pkgObj instanceof String) {
                            int code = (Integer) codeObj;
                            String pkg = (String) pkgObj;
                            if (code == 24 && isGameOverlayPackage(pkg)) {
                                return 0; // AppOpsManager.MODE_ALLOWED
                            }
                        }
                        return chain.proceed();
                    });
                }
                for (Method m : session.findMethods(appOpsService, "noteOperation")) {
                    session.hook(m, chain -> {
                        Object codeObj = HookValues.arg(chain, 0);
                        Object pkgObj = HookValues.arg(chain, 2);
                        if (codeObj instanceof Integer && pkgObj instanceof String) {
                            int code = (Integer) codeObj;
                            String pkg = (String) pkgObj;
                            if (code == 24 && isGameOverlayPackage(pkg)) {
                                if (chain.getExecutable() instanceof Method && ((Method) chain.getExecutable()).getReturnType() == int.class) {
                                    return 0; // AppOpsManager.MODE_ALLOWED
                                }
                            }
                        }
                        return chain.proceed();
                    });
                }
            }
        }

        session.log("SYSTEM_SERVER_CAPABILITY_SCAN_COMPLETE");
    }

    private static boolean isGameOverlayPackage(String pkg) {
        return "cn.nubia.gamehelperline".equals(pkg)
                || "cn.nubia.gameassist".equals(pkg)
                || "cn.zte.gamefloat".equals(pkg);
    }

    private static boolean isBooleanReturn(XposedInterface.Chain chain) {
        if (!(chain.getExecutable() instanceof java.lang.reflect.Method)) {
            return false;
        }
        Class<?> returnType = ((java.lang.reflect.Method) chain.getExecutable()).getReturnType();
        return returnType == boolean.class || returnType == Boolean.class;
    }

    private static void installCapturePolicyHooks(HookSession session, Class<?> capture) {
        session.hook(capture, "nativeCaptureDisplay", chain -> {
            forceCaptureArgsPolicy(chain);
            return chain.proceed();
        });
        session.hook(capture, "nativeCaptureLayers", chain -> {
            forceCaptureArgsPolicy(chain);
            return chain.proceed();
        });
    }

    private static void forceCaptureArgsPolicy(XposedInterface.Chain chain) {
        for (Object arg : chain.getArgs()) {
            if (arg == null) {
                continue;
            }
            java.lang.reflect.Field policy = HookValues.findField(arg, "mSecureContentPolicy");
            if (policy != null) {
                try {
                    policy.set(arg, 1);
                } catch (Throwable ignored) {
                    // The field is optional across API 35/36 vendor builds.
                }
            }
        }
    }

    public static void applyGameMasoProperties(String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        try {
            Class<?> sysProp = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = sysProp.getMethod("set", String.class, String.class);
            java.lang.reflect.Method getMethod = sysProp.getMethod("get", String.class, String.class);

            setMethod.invoke(null, "vendor.gpp.allgame.enable", "1");

            String suffix = pkg.length() > 25 ? pkg.substring(pkg.length() - 25) : pkg;
            setMethod.invoke(null, "persist.maso." + suffix, "2");

            String currentMagic = (String) getMethod.invoke(null, "persist.magic.super.resolution", "");
            if (currentMagic == null || !currentMagic.contains(pkg + ":")) {
                String newMagic = (currentMagic == null || currentMagic.isEmpty()) ? (pkg + ":2,") : (currentMagic + pkg + ":2,");
                setMethod.invoke(null, "persist.magic.super.resolution", newMagic);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            Method get = properties.getMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, "");
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void installSm8750GfrcInputLimitHook(HookSession session) {
        String socModel = readSystemProperty("ro.soc.model");
        if (!"SM8750".equals(socModel)) {
            return;
        }

        AtomicBoolean installed = new AtomicBoolean();
        Class<?> gfrcData = session.findOptionalClass(GFRC_DATA_CONTROLLER_CLASS);
        if (gfrcData != null) {
            installSm8750GfrcInputLimitHook(session, gfrcData, socModel, installed);
            return;
        }

        session.log("SYSTEM_SERVER_GFRC_DATA_CLASS_DEFERRED_UNTIL_CUBE_START");
        Class<?> serviceManager = session.findOptionalClass(
                "com.android.server.SystemServiceManager");
        if (serviceManager == null) {
            session.log("SYSTEM_SERVER_GFRC_SERVICE_START_HOOK_UNAVAILABLE");
            return;
        }
        session.hook(serviceManager, "startService", chain -> {
            Object serviceArg = HookValues.arg(chain, 0);
            Object result = chain.proceed();
            if (!(serviceArg instanceof Class<?>)
                    || !CUBE_SYSTEM_SERVICE_CLASS.equals(((Class<?>) serviceArg).getName())
                    || installed.get()) {
                return result;
            }

            ClassLoader cubeClassLoader = ((Class<?>) serviceArg).getClassLoader();
            if (cubeClassLoader == null) {
                cubeClassLoader = session.classLoader();
            }
            try {
                Class<?> lateGfrcData = Class.forName(
                        GFRC_DATA_CONTROLLER_CLASS, false, cubeClassLoader);
                installSm8750GfrcInputLimitHook(
                        session, lateGfrcData, socModel, installed);
            } catch (Throwable error) {
                session.log("SYSTEM_SERVER_GFRC_DATA_CLASS_UNAVAILABLE_AFTER_CUBE_START type="
                        + error.getClass().getSimpleName());
            }
            return result;
        }, Class.class);
        session.log("SYSTEM_SERVER_GFRC_CUBE_START_HOOKED");
    }

    private static void installSm8750GfrcInputLimitHook(
            HookSession session,
            Class<?> gfrcData,
            String socModel,
            AtomicBoolean installed
    ) {
        List<Method> methods = session.findMethods(
                gfrcData, "getData", int.class, String[].class);
        if (methods.isEmpty()) {
            session.log("SYSTEM_SERVER_GFRC_DATA_API_MISSING");
            return;
        }
        if (!installed.compareAndSet(false, true)) {
            return;
        }
        Set<String> loggedPackages = ConcurrentHashMap.newKeySet();

        for (Method method : methods) {
            session.hook(method, chain -> {
                Object original = chain.proceed();
                if (!(original instanceof Integer)) {
                    return original;
                }
                Object owner = chain.getThisObject();
                int gfrcEnabled = HookValues.readIntField(owner, -1, "b");
                int interpolationRate = HookValues.readIntField(owner, -1, "c");
                int adjusted = GfrcInputFramePolicy.adjustUltraInputLimit(
                        socModel,
                        HookValues.intArg(chain, 0, -1),
                        ((Integer) original).intValue(),
                        gfrcEnabled,
                        interpolationRate);
                if (adjusted == ((Integer) original).intValue()) {
                    return original;
                }
                Object packageArg = HookValues.arg(chain, 1);
                String pkg = packageArg instanceof String[] && ((String[]) packageArg).length > 0
                        ? ((String[]) packageArg)[0] : "unknown";
                if (loggedPackages.add(pkg)) {
                    session.log("SYSTEM_SERVER_GFRC_ULTRA_INPUT_LIMIT pkg=" + pkg
                            + " interp=" + interpolationRate + " from=" + original
                            + " to=" + adjusted);
                }
                return Integer.valueOf(adjusted);
            });
        }
        session.log("SYSTEM_SERVER_GFRC_SM8750_ULTRA_LIMIT_HOOKED");
    }
}
