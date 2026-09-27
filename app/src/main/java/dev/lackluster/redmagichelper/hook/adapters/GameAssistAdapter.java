package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.GfrcCapabilityPolicy;

/** Game-assistant and game-helper capability hooks. */
public final class GameAssistAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        installFixedScreenStateGuard(session);

        boolean unfreeze = session.config().gameFeaturesEnabled();
        boolean shoulderEnabled = session.config().getBoolean(
                "game_shoulder_key_enabled", false)
                || session.config().getBoolean("game_assist_shoulder_enabled", false);
        boolean comboEnabled = session.config().getBoolean(
                "game_space_combo_speed_enabled", false);
        boolean superResolutionUnlimited = session.config().gameSuperResolutionUnlimitedApps();
        boolean forceOverlay = session.config().getBoolean("game_force_overlay_capability", false);
        boolean voiceUnfreeze = session.config().getBoolean("game_magic_voice_unfreeze", false);
        boolean soundUnfreeze = session.config().getBoolean("game_space_sound_effects_unfreeze", false);
        boolean sightUnfreeze = session.config().getBoolean("game_sight_assist_unfreeze", false);
        boolean freeformUnfreeze = session.config().getBoolean("game_allow_third_party_freeform", false)
                || forceOverlay;
        boolean safeFeatureGate = shoulderEnabled || comboEnabled || superResolutionUnlimited || forceOverlay
                || voiceUnfreeze || soundUnfreeze || sightUnfreeze || freeformUnfreeze || unfreeze;
        if (!safeFeatureGate) {
            session.log("GAME_FEATURES_DISABLED");
            return;
        }

        // RedMagicOS stores the final per-game deny list in Settings.Global
        // and consults it after the Java capability checks. The stock value
        // contains package-specific entries for keylink/range-line, which is
        // why a successful TGK hook can still be blocked. Return an empty deny
        // list only for the three explicitly supported entries.
        session.hook(Settings.Global.class, "getString", chain -> {
            Object key = HookValues.arg(chain, 1);
            if (key instanceof String) {
                String strKey = (String) key;
                if (isSafeBlacklistKey(strKey, shoulderEnabled, comboEnabled, superResolutionUnlimited)) {
                    session.log("GAME_ASSIST_GLOBAL_BLACKLIST_BYPASSED key=" + key);
                    return "";
                }
            }
            return chain.proceed();
        }, android.content.ContentResolver.class, String.class);

        Class<?> rangeLineTile = session.findOptionalClass(
                "cn.nubia.gameassist.plugin.tiles.RangeLineTile");
        if (rangeLineTile != null && shoulderEnabled) {
            // Handle clicking RangeLineTile: toggle off or open line configuration and enable
            session.hook(rangeLineTile, "S", chain -> {
                Object owner = chain.getThisObject();
                HookValues.writeField(owner, Boolean.TRUE, "r", "mIsGameScene");
                Object pkg = HookValues.readField(owner, "s", "mCurPackage");
                if (pkg == null || "".equals(pkg)) {
                    try {
                        Class<?> systemMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                        if (systemMgr != null) {
                            pkg = systemMgr.getMethod("t").invoke(null);
                            if (pkg != null) {
                                HookValues.writeField(owner, pkg, "s", "mCurPackage");
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                Boolean enabled = (Boolean) HookValues.readField(owner, "t", "mEnabled");
                if (enabled == null) enabled = false;

                if (enabled) {
                    HookValues.invoke(owner, "B0", 0);
                    HookValues.writeField(owner, Boolean.FALSE, "t", "mEnabled");
                    session.log("GAME_ASSIST_RANGE_LINE_TOGGLED_OFF");
                } else {
                    boolean firstClick = true;
                    Object contextObj = HookValues.readField(owner, "i", "mContext");
                    try {
                        Class<?> spUtil = session.findOptionalClass("com.zte.gameassist.utils.SharedPreferencesUtil");
                        if (spUtil != null && contextObj instanceof Context) {
                            Object instance = spUtil.getMethod("k", Context.class).invoke(null, contextObj);
                            if (instance != null) {
                                firstClick = (Boolean) spUtil.getMethod("n", String.class).invoke(instance, String.valueOf(pkg));
                                if (firstClick) {
                                    spUtil.getMethod("W", String.class).invoke(instance, String.valueOf(pkg));
                                }
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (firstClick) {
                        HookValues.invokeNoArg(owner, "C0");
                    } else {
                        Object hostObj = HookValues.readField(owner, "h", "mHost");
                        if (hostObj != null) {
                            HookValues.invokeNoArg(hostObj, "b");
                        }
                        HookValues.invoke(owner, "B0", 1);
                    }
                    HookValues.writeField(owner, Boolean.TRUE, "t", "mEnabled");
                    session.log("GAME_ASSIST_RANGE_LINE_ENABLED pkg=" + pkg + " firstClick=" + firstClick);
                }
                HookValues.invokeNoArg(owner, "p0");
                return Boolean.TRUE;
            });

            // When clicking settings / gear on RangeLineTile, ALWAYS open line settings
            session.hook(rangeLineTile, "a0", chain -> {
                Object owner = chain.getThisObject();
                HookValues.invokeNoArg(owner, "C0");
                return Boolean.TRUE;
            });

            // When C0() is called (opening the line UI), ensure the intent carries packagename & packageName.
            session.hook(rangeLineTile, "C0", chain -> {
                Object owner = chain.getThisObject();
                Object pkg = HookValues.readField(owner, "s", "mCurPackage");
                Object contextObj = HookValues.readField(owner, "i", "mContext");
                if (pkg == null || "".equals(pkg)) {
                    try {
                        Class<?> systemMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                        if (systemMgr != null) {
                            pkg = systemMgr.getMethod("t").invoke(null);
                        }
                    } catch (Throwable ignored) {}
                }
                if ((pkg == null || "".equals(pkg)) && contextObj instanceof Context) {
                    try {
                        pkg = Settings.Global.getString(((Context) contextObj).getContentResolver(), "nubia_redmagic_game_type");
                        if (pkg == null || "".equals(pkg)) {
                            pkg = Settings.Global.getString(((Context) contextObj).getContentResolver(), "performance_mode_package");
                        }
                    } catch (Throwable ignored) {}
                }
                if (pkg == null || "".equals(pkg)) {
                    pkg = "com.tencent.tmgp.sgame";
                }
                HookValues.writeField(owner, pkg, "s", "mCurPackage");
                Object hostObj = HookValues.readField(owner, "h", "mHost");
                if (hostObj != null) {
                    HookValues.invokeNoArg(hostObj, "b");
                }
                if (contextObj instanceof Context) {
                    android.content.Intent intent = new android.content.Intent("cn.nubia.gamehelperline.ACTION_OPEN_LINE_MAIN_VIEW");
                    intent.setPackage("cn.nubia.gamehelperline");
                    intent.putExtra("packagename", (String) pkg);
                    intent.putExtra("packageName", (String) pkg);
                    try {
                        ((Context) contextObj).startForegroundService(intent);
                    } catch (Throwable t) {
                        try {
                            ((Context) contextObj).startService(intent);
                        } catch (Throwable ignored) {}
                    }
                    android.content.Intent bIntent = new android.content.Intent("cn.nubia.gamelauncher.action.START_RANGELINE");
                    bIntent.setPackage("cn.nubia.gamehelperline");
                    bIntent.putExtra("packagename", (String) pkg);
                    bIntent.putExtra("packageName", (String) pkg);
                    bIntent.putExtra("enable", 1);
                    ((Context) contextObj).sendBroadcast(bIntent);
                    session.log("GAME_ASSIST_RANGE_LINE_C0_DISPATCHED pkg=" + pkg);
                    return null;
                }
                return chain.proceed();
            });

            // When B0(int) is called, ensure intent carries packageName & packagename
            session.hook(rangeLineTile, "B0", chain -> {
                Object owner = chain.getThisObject();
                Object pkg = HookValues.readField(owner, "s", "mCurPackage");
                if (pkg == null || "".equals(pkg)) {
                    try {
                        Class<?> systemMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                        if (systemMgr != null) {
                            pkg = systemMgr.getMethod("t").invoke(null);
                            if (pkg != null) {
                                HookValues.writeField(owner, pkg, "s", "mCurPackage");
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                int enable = (Integer) HookValues.arg(chain, 0);
                Object contextObj = HookValues.readField(owner, "i", "mContext");
                if (contextObj instanceof Context) {
                    android.content.Intent intent = new android.content.Intent("cn.nubia.gamelauncher.action.START_RANGELINE");
                    intent.setPackage("cn.nubia.gamehelperline");
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        intent.putExtra("packagename", (String) pkg);
                        intent.putExtra("packageName", (String) pkg);
                    }
                    intent.putExtra("enable", enable);
                    ((Context) contextObj).sendBroadcast(intent);
                    session.log("GAME_ASSIST_RANGE_LINE_B0_SENT enable=" + enable + " pkg=" + pkg);
                    return null;
                }
                return chain.proceed();
            }, int.class);
            session.log("GAME_ASSIST_RANGE_LINE_TILE_HOOKED");
        }

        Class<?> rotationMgr = session.findOptionalClass("com.zte.gameassist.common.RotationMgr");
        if (rotationMgr != null) {
            session.hook(rotationMgr, "j", chain -> Boolean.TRUE);
            session.log("GAME_ASSIST_ROTATION_CHECK_BYPASSED");
        }

        Class<?> qsTile = session.findOptionalClass("cn.nubia.gameassist.common.QSTile");
        if (qsTile != null) {
            session.hook(qsTile, "S", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                HookValues.writeField(owner, Boolean.TRUE, "r", "mIsGameScene");
                return res;
            });
        }
        if (superResolutionUnlimited) {
            try {
                Class<?> sysProp = Class.forName("android.os.SystemProperties");
                java.lang.reflect.Method setMethod = sysProp.getMethod("set", String.class, String.class);
                setMethod.invoke(null, "vendor.gpp.allgame.enable", "1");
            } catch (Throwable ignored) {
            }

            // Prevent writing null / empty string to game_gfrc_mode to avoid wiping active game configs
            session.hook(Settings.Global.class, "putString", chain -> {
                Object key = HookValues.arg(chain, 1);
                Object value = HookValues.arg(chain, 2);
                if ("game_gfrc_mode".equals(key) && (value == null || "".equals(value))) {
                    if (HookValues.stackContains("SuperResolutionViewController")) {
                        session.log("GAME_ASSIST_PREVENT_WIPING_GFRC_MODE");
                        return Boolean.FALSE;
                    }
                }
                return chain.proceed();
            }, android.content.ContentResolver.class, String.class, String.class);

            Class<?> performance = session.findOptionalClass(
                    "cn.nubia.gameassist.performance.PerformanceModeController");

            Class<?> superResolution = session.findOptionalClass(
                    "cn.nubia.plugin.superresolution.SuperResolutionViewController");
            if (superResolution != null) {
                // x(package, value) is the final package/value gate used when SuperResolution settings are applied
                session.hook(superResolution, "x", chain -> {
                    Object value = HookValues.arg(chain, 1);
                    if ("0".equals(value)) {
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE;
                }, String.class, String.class);

                // u(mode) and h(mode) auto-disable super resolution / interpolation if performance mode != 3
                session.hook(superResolution, "u", chain -> Boolean.FALSE, int.class);
                session.hook(superResolution, "h", chain -> Boolean.FALSE, int.class);

                // Block silent auto-disable from onGameStart / performance mode callbacks, while allowing explicit user toggle
                session.hook(superResolution, "K", chain -> {
                    Object pkgObj = HookValues.arg(chain, 0);
                    Object enableObj = HookValues.arg(chain, 1);
                    if (Boolean.FALSE.equals(enableObj)) {
                        if (HookValues.stackContains("PerformanceMode")
                                || HookValues.stackContains("checkGameMode")
                                || HookValues.stackContainsMethod("onGameStart")
                                || HookValues.stackContainsMethod("i")) {
                            session.log("GAME_ASSIST_SUPER_RESOLUTION_AUTO_DISABLE_BLOCKED");
                            return null;
                        }
                    } else if (Boolean.TRUE.equals(enableObj) && pkgObj instanceof String) {
                        String pkg = (String) pkgObj;
                        applyGameMasoProperties(pkg);
                        Object ctx = HookValues.readField(chain.getThisObject(), "c", "mContext");
                        if (ctx instanceof Context) {
                            syncGfrcModeSettings((Context) ctx, pkg);
                        }
                    }
                    return chain.proceed();
                }, String.class, boolean.class);

                // On game start, ensure MASO properties & GFRC settings are active
                session.hook(superResolution, "onGameStart", chain -> {
                    Object owner = chain.getThisObject();
                    Object ctx = HookValues.readField(owner, "c", "mContext");
                    try {
                        Class<?> sysMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                        if (sysMgr != null) {
                            Object pkg = sysMgr.getMethod("v").invoke(null);
                            if (pkg instanceof String && !((String) pkg).isEmpty()) {
                                applyGameMasoProperties((String) pkg);
                                if (ctx instanceof Context) {
                                    syncGfrcModeSettings((Context) ctx, (String) pkg);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                    return chain.proceed();
                });
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

            Class<?> pluginUtils = session.findOptionalClass(
                    "cn.nubia.gameassist.plugin.PluginUtils");
            if (pluginUtils != null) {
                session.hook(pluginUtils, "e", chain -> {
                    Object pkg = HookValues.arg(chain, 0);
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                        Object ctx = HookValues.readField(chain.getThisObject(), "a", "mContext");
                        if (ctx instanceof Context) {
                            syncGfrcModeSettings((Context) ctx, (String) pkg);
                        }
                    }
                    Object original = chain.proceed();
                    int capability = original instanceof Integer ? (Integer) original : 0;
                    return GfrcCapabilityPolicy.includeUltraModes(capability);
                }, String.class);
                session.hook(pluginUtils, "d", chain -> {
                    Object cap = HookValues.arg(chain, 1);
                    if (cap instanceof Integer && ((Integer) cap) == 0) {
                        // Prevent clearing game from game_gfrc_mode
                        return null;
                    }
                    return chain.proceed();
                }, String.class, int.class);
                session.hook(pluginUtils, "p", chain -> Boolean.TRUE, String.class);
            }

            // Hook SuperResolutionTile and SuperResolutionOldTile to ensure mIsGameScene is true, package is set,
            // and MASO properties & settings are synced when user interacts with the tile.
            Class<?> superResTile = session.findOptionalClass(
                    "cn.nubia.gameassist.plugin.tiles.SuperResolutionTile");
            if (superResTile != null) {
                session.hook(superResTile, "S", chain -> {
                    Object owner = chain.getThisObject();
                    HookValues.writeField(owner, Boolean.TRUE, "r", "mIsGameScene");
                    Object pkg = HookValues.readField(owner, "s", "mCurPackage");
                    if (pkg == null || "".equals(pkg)) {
                        try {
                            Class<?> systemMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                            if (systemMgr != null) {
                                pkg = systemMgr.getMethod("v").invoke(null);
                                if (pkg == null) {
                                    pkg = systemMgr.getMethod("t").invoke(null);
                                }
                                if (pkg != null) {
                                    HookValues.writeField(owner, pkg, "s", "mCurPackage");
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                        Object ctx = HookValues.readField(owner, "i", "mContext");
                        if (ctx instanceof Context) {
                            syncGfrcModeSettings((Context) ctx, (String) pkg);
                        }
                    }
                    return chain.proceed();
                });
            }
            Class<?> superResOldTile = session.findOptionalClass(
                    "cn.nubia.gameassist.plugin.tiles.SuperResolutionOldTile");
            if (superResOldTile != null) {
                session.hook(superResOldTile, "S", chain -> {
                    Object owner = chain.getThisObject();
                    HookValues.writeField(owner, Boolean.TRUE, "r", "mIsGameScene");
                    Object pkg = HookValues.readField(owner, "s", "mCurPackage");
                    if (pkg == null || "".equals(pkg)) {
                        try {
                            Class<?> systemMgr = session.findOptionalClass("com.zte.gameassist.common.SystemMgr");
                            if (systemMgr != null) {
                                pkg = systemMgr.getMethod("v").invoke(null);
                                if (pkg == null) {
                                    pkg = systemMgr.getMethod("t").invoke(null);
                                }
                                if (pkg != null) {
                                    HookValues.writeField(owner, pkg, "s", "mCurPackage");
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (pkg instanceof String && !((String) pkg).isEmpty()) {
                        applyGameMasoProperties((String) pkg);
                        Object ctx = HookValues.readField(owner, "i", "mContext");
                        if (ctx instanceof Context) {
                            syncGfrcModeSettings((Context) ctx, (String) pkg);
                        }
                    }
                    return chain.proceed();
                });
            }

            // SuperResolutionTile.S() refuses to enter the OEM enable path while low_power is set
            session.hook(Settings.Global.class, "getInt", chain -> {
                Object key = HookValues.arg(chain, 1);
                if ("low_power".equals(key)
                        && (HookValues.stackContains("SuperResolutionTile") || HookValues.stackContains("SuperResolutionOldTile"))
                        && HookValues.stackContainsMethod("S")) {
                    return 0;
                }
                return chain.proceed();
            }, android.content.ContentResolver.class, String.class, int.class);

            if (performance != null) {
                session.hook(performance, "getPerformanceMode", chain -> {
                    if (HookValues.arg(chain, 0) instanceof String
                            && (HookValues.stackContains("SuperResolutionTile") || HookValues.stackContains("SuperResolutionOldTile"))
                            && HookValues.stackContainsMethod("S")) {
                        return 3;
                    }
                    return chain.proceed();
                }, String.class);
            }
            session.log("GAME_SUPER_RESOLUTION_ALL_APPS_ENABLED");
        }

        // Utils.k() builds the source tile list and calls the private
        // Utils.d(ArrayList) capability filter before PluginConfig.g() is
        // reached.  On devices whose ROM feature flags are false this filter
        // removes the shoulder-key and super-resolution specs outright.  Put
        // back only the entries explicitly enabled in this module; do not
        // touch the other OEM tiles.
        Class<?> utils = session.findOptionalClass(
                "cn.nubia.gameassist.utils.Utils");
        if (utils != null) {
            session.hook(utils, "d", chain -> {
                Object result = chain.proceed();
                if (!(result instanceof List)) {
                    return result;
                }
                ArrayList<Object> entries = new ArrayList<>((List<?>) result);
                addFeatureEntry(entries, "range_line", shoulderEnabled);
                addFeatureEntry(entries, "operation_devices", shoulderEnabled);
                addFeatureEntry(entries, "keylink", comboEnabled);
                addFeatureEntry(entries, "super_resolution", superResolutionUnlimited);
                return entries;
            }, ArrayList.class);
            if (shoulderEnabled) {
                // The stock Utils.L() is the final hide-key decision used by
                // SubViewController.  Its built-in package list contains
                // com.tencent.tmgp.sgame, so the shoulder-key button is
                // hidden for King of Glory even after the capability gates
                // above report support. Keep the bypass limited to the
                // affected official King of Glory package family.
                session.hook(utils, "L", chain -> {
                    Object packageName = HookValues.arg(chain, 0);
                    if (isKingOfGloryPackage(packageName)) {
                        session.log("GAME_ASSIST_SHOULDER_ENTRY_BLACKLIST_BYPASSED package="
                                + packageName);
                        return Boolean.FALSE;
                    }
                    return chain.proceed();
                }, String.class);
                session.log("GAME_ASSIST_SHOULDER_ENTRY_VISIBILITY_REPAIRED");
            }
            if (superResolutionUnlimited) {
                session.hook(utils, "P", chain -> {
                    if ((HookValues.stackContains("SuperResolutionTile") || HookValues.stackContains("SuperResolutionOldTile"))
                            && HookValues.stackContainsMethod("S")) {
                        return Boolean.FALSE;
                    }
                    return chain.proceed();
                }, Context.class);
            }
            session.log("GAME_ASSIST_TILE_CAPABILITY_FILTER_REPAIRED");
        }

        // These are the concrete feature gates used by the tile constructors,
        // labels and the shoulder-key editor.  They are opt-in and scoped to
        // the matching module switch, so disabling a switch restores the OEM
        // result instead of leaving a permanently enabled feature behind.
        Class<?> zteFeature = session.findOptionalClass(
                "com.zte.gameassist.config.ZteFeature");
        if (zteFeature != null) {
            if (shoulderEnabled) {
                session.hook(zteFeature, "isSupportXGravityPlugin",
                        chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportPeripheralControl",
                        chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportTouchGameKey",
                        chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_SHOULDER_FEATURE_GATES_ENABLED");
            }
            if (comboEnabled) {
                // This method is private in the current GameAssist build;
                // HookSession resolves private declarations as well.
                session.hook(zteFeature, "isSupportOneKeyLink",
                        chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_COMBO_FEATURE_GATE_ENABLED");
            }
            if (superResolutionUnlimited) {
                session.hook(zteFeature, "isSupportSuperResolution",
                        chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportSuperResolutionSettings",
                        chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_SUPER_RESOLUTION_FEATURE_GATES_ENABLED");
            }
            if (forceOverlay) {
                session.hook(zteFeature, "isSupportFloatWindow",
                        chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportAppFloat",
                        chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_FORCE_OVERLAY_FEATURE_GATES_ENABLED");
            }
            if (voiceUnfreeze) {
                session.hook(zteFeature, "isSupportVoiceController", chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportGameVoiceAssist", chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_VOICE_FEATURE_GATES_ENABLED");
            }
            if (soundUnfreeze) {
                session.hook(zteFeature, "isSupportSoundProbe", chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportAudioEqualizer", chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_SOUND_FEATURE_GATES_ENABLED");
            }
            if (freeformUnfreeze) {
                session.hook(zteFeature, "isSupportHostFreeform", chain -> Boolean.TRUE);
                session.hook(zteFeature, "isSupportChatAssistInPlugin", chain -> Boolean.TRUE);
                session.log("GAME_ASSIST_FREEFORM_FEATURE_GATES_ENABLED");
            }
        }

        if (forceOverlay) {
            session.hook(Settings.class, "canDrawOverlays", chain -> Boolean.TRUE, Context.class);
        }

        Class<?> pluginConfig = session.findClass(
                "cn.nubia.gameassist.plugin.config.PluginConfig");
        // PluginConfig.d/l are the actual policy path used by the tiles.
        // Keep this override in addition to Settings.Global: on some
        // RedMagicOS builds the value is read before the Settings cache
        // is initialized, so the global getter is not reached again.
        session.hook(pluginConfig, "d", chain -> {
            Object feature = HookValues.arg(chain, 1);
            if (isSafeFeature(feature, shoulderEnabled, comboEnabled,
                    superResolutionUnlimited)) {
                session.log("GAME_ASSIST_PLUGIN_BLACKLIST_BYPASSED feature=" + feature);
                return new String[0];
            }
            return chain.proceed();
        }, Context.class, String.class);
        // l() is the capability decision used by g() to build the floating
        // panel tile list. It is separate from f0(), which represents the
        // current package's tile state. Preserve explicit OEM off switches.
        session.hook(pluginConfig, "l", chain -> {
            Object feature = HookValues.arg(chain, 1);
            if (!isSafeFeature(feature, shoulderEnabled, comboEnabled,
                    superResolutionUnlimited)) {
                return chain.proceed();
            }
            if ("range_line".equals(feature)) {
                return Boolean.TRUE;
            }
            Object context = HookValues.arg(chain, 0);
            if (context instanceof Context && feature instanceof String
                    && !isPluginEntryEnabled((Context) context, (String) feature)) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }, Context.class, String.class, String.class);
        // A few builds cache g() before the first l() evaluation. Ensure the
        // three requested tile specs are present without changing their on/off
        // state or bypassing an explicit plugin disable switch.
        session.hook(pluginConfig, "g", chain -> {
            Object result = chain.proceed();
            Object context = HookValues.arg(chain, 0);
            if (!(result instanceof List) || !(context instanceof Context)) {
                return result;
            }
            List<Object> entries = new ArrayList<>((List<?>) result);
            addUiEntry(entries, (Context) context, "range_line", shoulderEnabled);
            addUiEntry(entries, (Context) context, "operation_devices", shoulderEnabled);
            addUiEntry(entries, (Context) context, "keylink", comboEnabled);
            addUiEntry(entries, (Context) context, "super_resolution",
                    superResolutionUnlimited);
            return entries;
        }, Context.class);
        session.log("GAME_ASSIST_SAFE_ENTRY_HOOKS_INSTALLED");
    }

    /**
     * GameAssist registers fold-screen observers before InflaterHelper has
     * published its first FixedScreenState.  ObserverData immediately invokes
     * a newly registered observer, and the stock panel callback dereferences
     * that null value in GameAssistWindowManager.A0().  Skip only that one
     * invalid callback; the normal non-null callback still runs unchanged.
     */
    private static void installFixedScreenStateGuard(HookSession session) {
        Class<?> fixedScreenState = session.findOptionalClass(
                "com.zte.gameassist.common.InflaterHelper$FixedScreenState");
        if (fixedScreenState == null) {
            return;
        }

        AtomicBoolean logged = new AtomicBoolean();
        hookNullFixedScreenStateCallback(session,
                "cn.nubia.gameassist.panel.GameAssistWindowManager", "A0",
                fixedScreenState, logged);
        // These observers use the same OEM state object. They currently guard
        // some null paths themselves, but keeping the callback boundary safe
        // avoids the same regression if a vendor build removes that check.
        hookNullFixedScreenStateCallback(session,
                "cn.nubia.plugin.gameratio.GameRatioIndicatorWindow", "V",
                fixedScreenState, logged);
        hookNullFixedScreenStateCallback(session,
                "com.zte.plugin.reminder.GameReminderWindowManager", "M",
                fixedScreenState, logged);
        session.log("GAME_ASSIST_FIXED_SCREEN_STATE_GUARD_INSTALLED");
    }

    private static void hookNullFixedScreenStateCallback(
            HookSession session,
            String ownerName,
            String methodName,
            Class<?> fixedScreenState,
            AtomicBoolean logged
    ) {
        Class<?> owner = session.findOptionalClass(ownerName);
        if (owner == null) {
            return;
        }
        session.hook(owner, methodName, chain -> {
            if (HookValues.arg(chain, 0) == null) {
                if (logged.compareAndSet(false, true)) {
                    session.log("GAME_ASSIST_NULL_FIXED_SCREEN_STATE_SKIPPED owner="
                            + ownerName);
                }
                return null;
            }
            return chain.proceed();
        }, fixedScreenState);
    }

    private static void addFeatureEntry(
            List<Object> entries, String feature, boolean requested) {
        if (requested && !entries.contains(feature)) {
            entries.add(feature);
        }
    }

    private static void addUiEntry(
            List<Object> entries, Context context, String feature, boolean requested) {
        if (requested && !entries.contains(feature)) {
            if ("range_line".equals(feature) || isPluginEntryEnabled(context, feature)) {
                entries.add(feature);
            }
        }
    }

    private static boolean isPluginEntryEnabled(Context context, String feature) {
        try {
            android.content.ContentResolver resolver = context.getContentResolver();
            return Settings.Global.getInt(resolver,
                    "game_assist_enable_plugin_" + feature, 1) == 1
                    && Settings.Global.getInt(resolver,
                    "game_assist_trigger_space_" + feature, 3) != 0;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean isKingOfGloryPackage(Object packageName) {
        if (!(packageName instanceof String)) {
            return false;
        }
        String value = (String) packageName;
        return "com.tencent.tmgp.sgame".equals(value)
                || "com.tencent.tmgp.sgamece".equals(value)
                || "com.levelinfinite.sgameGlobal".equals(value)
                || "com.levelinfinite.sgameGlobal.midaspay".equals(value);
    }

    private static boolean isSafeBlacklistKey(
            String key, boolean shoulderEnabled, boolean comboEnabled,
            boolean superResolutionEnabled) {
        if ("game_assist_black_list_range_line".equals(key)) {
            return shoulderEnabled;
        }
        if ("game_assist_black_list_keylink".equals(key)
                || "game_assist_black_list_touch_long_keylink_point".equals(key)) {
            return comboEnabled;
        }
        return ("game_assist_black_list_super_resolution".equals(key)
                || "game_assist_black_list_super_resolution_old".equals(key))
                && superResolutionEnabled;
    }

    private static boolean isSafeFeature(
            Object feature, boolean shoulderEnabled, boolean comboEnabled,
            boolean superResolutionEnabled) {
        if (!(feature instanceof String)) {
            return false;
        }
        String value = (String) feature;
        if ("range_line".equals(value)) {
            return shoulderEnabled;
        }
        if ("keylink".equals(value) || "touch_long_keylink_point".equals(value)) {
            return comboEnabled;
        }
        return ("super_resolution".equals(value) || "super_resolution_old".equals(value))
                && superResolutionEnabled;
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

    public static void syncGfrcModeSettings(Context context, String pkg) {
        if (context == null || pkg == null || pkg.isEmpty()) return;
        try {
            android.content.ContentResolver resolver = context.getContentResolver();
            String gfrcMode = Settings.Global.getString(resolver, "game_gfrc_mode");
            if (gfrcMode == null || !gfrcMode.contains(pkg + "+")) {
                String entry = pkg + "+221";
                String newGfrc = (gfrcMode == null || gfrcMode.isEmpty()) ? entry : (gfrcMode + "," + entry);
                Settings.Global.putString(resolver, "game_gfrc_mode", newGfrc);
            }
            String suffix = pkg.length() > 25 ? pkg.substring(pkg.length() - 25) : pkg;
            String masoKey = "persist.maso." + suffix;
            String switchKey = Settings.Global.getString(resolver, "super_resolution_switch_key");
            if (switchKey == null || (!switchKey.contains(masoKey) && !switchKey.contains(pkg))) {
                String entry = masoKey + ":2,";
                String newSwitch = (switchKey == null || switchKey.isEmpty()) ? entry : (switchKey + entry);
                Settings.Global.putString(resolver, "super_resolution_switch_key", newSwitch);
            }
        } catch (Throwable ignored) {
        }
    }
}
