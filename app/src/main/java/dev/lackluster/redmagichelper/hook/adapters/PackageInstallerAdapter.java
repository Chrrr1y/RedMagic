package dev.lackluster.redmagichelper.hook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Package installer hooks kept opt-in because they change install policy. */
public final class PackageInstallerAdapter implements HostAdapter {

    @Override
    public void install(HookSession session) {
        if (session.config().getBoolean("hide_evolution_mode_toggle", false)) {
            Class<?> installer = session.findOptionalClass(
                    "com.android.packageinstaller.PackageInstallerActivity");
            if (installer != null) {
                // Hook getCookUI to move pure mode switch to top-right settings, clean bg, hide warning layout
                session.hook(installer, "getCookUI", chain -> {
                    Object cookUi = chain.proceed();
                    if (cookUi != null) {
                        HookValues.writeField(cookUi, true, "cleanBgColor");
                        HookValues.writeField(cookUi, true, "hidePureModeSwitchLayout");
                        HookValues.writeField(cookUi, true, "hideWarningLayout");
                        HookValues.writeField(cookUi, false, "needIsolate");
                        HookValues.writeField(cookUi, false, "showSwlimitLearnMore");
                        HookValues.writeField(cookUi, false, "showRiskCheckbox");
                    }
                    return cookUi;
                });

                // Auto-check risk acknowledgment checkbox, hide "Learn More" button, and enable install button
                for (String name : new String[]{"bindUi", "bindUiPerm", "bindUiPermRed", "onResume", "onStart"}) {
                    session.hook(installer, name, chain -> {
                        Object result = chain.proceed();
                        applyPackageInstallerRiskAndLearnMore(chain.getThisObject());
                        return result;
                    });
                }
            }

            installPureModeSettingsHook(session);
        }

        if (session.config().getBoolean("hide_store_install_prompt", false)) {
            Class<?> installer = session.findOptionalClass(
                    "com.android.packageinstaller.PackageInstallerActivity");
            if (installer != null) {
                for (String name : new String[]{"bindUi", "bindUiPerm", "bindUiPermRed"}) {
                    session.hook(installer, name, chain -> {
                        Object result = chain.proceed();
                        hideInstallerViews(chain.getThisObject());
                        return result;
                    }, int.class, boolean.class);
                }
            }
        }

        installSkipScanHook(session);

        if (session.config().getBoolean("cts_test_installer", false)) {
            Class<?> installStart = session.findOptionalClass("com.android.packageinstaller.InstallStart");
            if (installStart != null) {
                session.hook(installStart, "getCallingPackageNameForUid", chain -> "zuji.cts", int.class);
            }
        }

        if (session.config().getBoolean("package_update_system_app", false)
                || session.config().getBoolean("others_package_update_system_app", false)) {
            session.missing("package update-system-app capability unavailable");
        }
        session.log("PACKAGE_INSTALLER_CAPABILITY_SCAN_COMPLETE");
    }

    private static void hideInstallerViews(Object owner) {
        if (owner == null) {
            return;
        }
        for (String name : new String[]{
                "mMarketContainer", "mWarningUnknown", "mDivider", "mMarketGuide",
                "mMarket", "marketReplace", "mWarningLayout", "mStorePrompt", "mMarketLayout"}) {
            Object value = HookValues.readField(owner, name);
            if (value instanceof View) {
                HookValues.runOnMain((View) value,
                        () -> ((View) value).setVisibility(View.GONE));
            }
        }
    }

    private static void installSkipScanHook(HookSession session) {
        // 1. Hook InstallStaging to bypass bindVirusService and ScanningAsyncTask
        Class<?> stagingClass = session.findOptionalClass("com.android.packageinstaller.InstallStaging");
        if (stagingClass != null) {
            Class<?> uriClass = android.net.Uri.class;
            session.hook(stagingClass, "installScanningInit", chain -> {
                if (!session.config().getBoolean("skip_pkg_scan", false)) {
                    return chain.proceed();
                }
                Object staging = chain.getThisObject();
                android.net.Uri uri = (android.net.Uri) chain.getArgs().get(0);
                session.logSmali("Lcom/android/packageinstaller/InstallStaging;->installScanningInit(Landroid/net/Uri;)V",
                        "INTERCEPT: Bypassing virus scan service & latch delay");
                try {
                    if (uri != null) {
                        HookValues.writeField(staging, uri.getPath(), "scan_cache_path");
                    }
                    Method parseMethod = staging.getClass().getDeclaredMethod("parsePackageUri", uriClass);
                    parseMethod.setAccessible(true);
                    boolean parsed = (Boolean) parseMethod.invoke(staging, uri);
                    if (!parsed) {
                        Method delete = staging.getClass().getDeclaredMethod("gotoDeleteStagedFile");
                        delete.setAccessible(true);
                        delete.invoke(staging);
                        return null;
                    }

                    Class<?> wrapperClass = session.findClass("com.android.packageinstaller.InstallStaging$Wrapper");
                    Constructor<?> ctor = wrapperClass.getDeclaredConstructor(staging.getClass());
                    ctor.setAccessible(true);
                    Object wrapper = ctor.newInstance(staging);

                    Method launch = staging.getClass().getDeclaredMethod("LaunchNextActivity", wrapperClass);
                    launch.setAccessible(true);
                    launch.invoke(staging, wrapper);
                    session.logSmali("Lcom/android/packageinstaller/InstallStaging;->LaunchNextActivity(Lcom/android/packageinstaller/InstallStaging$Wrapper;)V",
                            "BYPASS_SUCCESS: Launched installer activity with 0s delay");
                    return null;
                } catch (Throwable t) {
                    session.logSmali("Lcom/android/packageinstaller/InstallStaging;->installScanningInit",
                            "FALLBACK: " + t.getMessage());
                    return chain.proceed();
                }
            }, uriClass);

            // Safety net: force bindVirusService to return false and countdown latch
            session.hook(stagingClass, "bindVirusService", chain -> {
                if (!session.config().getBoolean("skip_pkg_scan", false)) {
                    return chain.proceed();
                }
                session.logSmali("Lcom/android/packageinstaller/InstallStaging;->bindVirusService()Z",
                        "OVERRIDE bindVirusService -> false");
                return Boolean.FALSE;
            });

            // ScanningAsyncTask.onPostExecute bypass fallback
            Class<?> taskClass = session.findOptionalClass("com.android.packageinstaller.InstallStaging$ScanningAsyncTask");
            if (taskClass != null) {
                Class<?> wrapperClass = session.findOptionalClass("com.android.packageinstaller.InstallStaging$Wrapper");
                if (wrapperClass != null) {
                    session.hook(taskClass, "onPostExecute", chain -> {
                        if (!session.config().getBoolean("skip_pkg_scan", false)) {
                            return chain.proceed();
                        }
                        Object wrapper = chain.getArgs().get(0);
                        if (wrapper != null) {
                            Object task = chain.getThisObject();
                            Object staging = HookValues.readField(task, "this$0");
                            if (staging != null) {
                                try {
                                    Method launch = staging.getClass().getDeclaredMethod("LaunchNextActivity", wrapperClass);
                                    launch.setAccessible(true);
                                    launch.invoke(staging, wrapper);
                                    session.logSmali("Lcom/android/packageinstaller/InstallStaging$ScanningAsyncTask;->onPostExecute",
                                            "BYPASS_DELAY: direct LaunchNextActivity");
                                    return null;
                                } catch (Throwable ignored) {}
                            }
                        }
                        return chain.proceed();
                    }, wrapperClass);
                }
            }
        }

        // 2. Hook InstallScanning
        Class<?> scanningClass = session.findOptionalClass("com.android.packageinstaller.InstallScanning");
        if (scanningClass != null) {
            session.hook(scanningClass, "onResume", chain -> {
                if (!session.config().getBoolean("skip_pkg_scan", false)) {
                    return chain.proceed();
                }
                Object scanning = chain.getThisObject();
                session.logSmali("Lcom/android/packageinstaller/InstallScanning;->onResume()V",
                        "INTERCEPT: Bypassing InstallScanning async scan");
                try {
                    Class<?> wrapperClass = session.findClass("com.android.packageinstaller.InstallScanning$Wrapper");
                    Constructor<?> ctor = wrapperClass.getDeclaredConstructor(scanning.getClass());
                    ctor.setAccessible(true);
                    Object wrapper = ctor.newInstance(scanning);

                    Method launch = scanning.getClass().getDeclaredMethod("LaunchNextActivity", wrapperClass);
                    launch.setAccessible(true);
                    launch.invoke(scanning, wrapper);
                    session.logSmali("Lcom/android/packageinstaller/InstallScanning;->LaunchNextActivity",
                            "BYPASS_SUCCESS: Launched installer activity immediately");
                    return null;
                } catch (Throwable t) {
                    return chain.proceed();
                }
            });
        }

        // 3. Hook PackageUtil features and switches
        Class<?> utilClass = session.findOptionalClass("com.android.packageinstaller.PackageUtil");
        if (utilClass != null) {
            session.hook(utilClass, "getFeatureBoolean", chain -> {
                if (!session.config().getBoolean("skip_pkg_scan", false)) {
                    return chain.proceed();
                }
                Object name = chain.getArgs().get(0);
                if ("ZTE_FEATURE_ADB_VIRUS_SCAN".equals(name)) {
                    session.logSmali("Lcom/android/packageinstaller/PackageUtil;->getFeatureBoolean",
                            "OVERRIDE ZTE_FEATURE_ADB_VIRUS_SCAN -> false");
                    return Boolean.FALSE;
                }
                return chain.proceed();
            }, String.class, boolean.class);

            session.hook(utilClass, "getSettingSwitchSP", chain -> {
                if (!session.config().getBoolean("skip_pkg_scan", false)) {
                    return chain.proceed();
                }
                Object key = chain.getArgs().get(1);
                if ("virus_scan".equals(key)) {
                    session.logSmali("Lcom/android/packageinstaller/PackageUtil;->getSettingSwitchSP",
                            "OVERRIDE virus_scan SP -> false");
                    return Boolean.FALSE;
                }
                return chain.proceed();
            }, Context.class, String.class, boolean.class);
        }

        // 4. Ensure install button is enabled immediately in PackageInstallerActivity
        Class<?> installer = session.findOptionalClass(
                "com.android.packageinstaller.PackageInstallerActivity");
        if (installer != null) {
            for (String name : new String[]{"bindUi", "bindUiPerm", "bindUiPermRed", "onResume"}) {
                session.hook(installer, name, chain -> {
                    Object result = chain.proceed();
                    skipScanAndEnableInstall(chain.getThisObject());
                    return result;
                });
            }
        }
    }

    private static void skipScanAndEnableInstall(Object owner) {
        if (owner == null) {
            return;
        }
        HookValues.writeField(owner, false, "mScanning", "mIsScanning");
        for (String viewName : new String[]{
                "mScanningLayout", "mScanProgress", "mScanningView", "mScanView", "mRiskLayout", "mSecurityCheckLayout"}) {
            Object view = HookValues.readField(owner, viewName);
            if (view instanceof View) {
                HookValues.runOnMain((View) view, () -> ((View) view).setVisibility(View.GONE));
            }
        }
        for (String btnName : new String[]{
                "mOk", "mInstallButton", "mOkButton", "mButtonConfirm", "mAllowButton", "mBtnInstall"}) {
            Object btn = HookValues.readField(owner, btnName);
            if (btn instanceof View) {
                HookValues.runOnMain((View) btn, () -> {
                    ((View) btn).setEnabled(true);
                    ((View) btn).setVisibility(View.VISIBLE);
                });
            }
        }
    }

    private static void applyPackageInstallerRiskAndLearnMore(Object owner) {
        if (owner == null) {
            return;
        }
        if (owner instanceof Activity) {
            Activity activity = (Activity) owner;
            View decor = activity.getWindow().getDecorView();
            HookValues.runOnMain(decor, () -> {
                processInstallerRiskViews(decor);

                for (String pureName : new String[]{
                        "mPureModeSwitchLayout", "mPureModeLayout", "pure_mode_switch_layout"}) {
                    Object v = HookValues.readField(owner, pureName);
                    if (v instanceof View) {
                        ((View) v).setVisibility(View.GONE);
                    }
                }
                for (String cbName : new String[]{
                        "mRiskCheckBox", "mCheckRisk", "mCheckBox", "mRiskCheck", "mAgreeCheckBox", "mRiskCb", "mCheckbox"}) {
                    Object cb = HookValues.readField(owner, cbName);
                    if (cb instanceof CompoundButton) {
                        ((CompoundButton) cb).setChecked(true);
                    }
                }
                for (String learnMoreName : new String[]{
                        "mLearnMore", "mSwlimitLearnMore", "mMoreInfo", "mDetailInfo", "mDetailBtn"}) {
                    Object lm = HookValues.readField(owner, learnMoreName);
                    if (lm instanceof View) {
                        ((View) lm).setVisibility(View.GONE);
                    }
                }
                for (String btnName : new String[]{
                        "mOk", "mInstallButton", "mOkButton", "mButtonConfirm", "mAllowButton", "mBtnInstall"}) {
                    Object btn = HookValues.readField(owner, btnName);
                    if (btn instanceof View) {
                        ((View) btn).setEnabled(true);
                    }
                }
            });
        }
    }

    private static void processInstallerRiskViews(View view) {
        if (view == null) return;
        String resName = "";
        try {
            if (view.getId() != View.NO_ID && view.getResources() != null) {
                resName = view.getResources().getResourceEntryName(view.getId());
            }
        } catch (Throwable ignored) {}

        if ("pure_mode_switch_layout".equals(resName) || resName.contains("pure_mode")) {
            view.setVisibility(View.GONE);
            return;
        }

        if (view instanceof CompoundButton) {
            CompoundButton cb = (CompoundButton) view;
            CharSequence text = cb.getText();
            String textStr = text != null ? text.toString() : "";
            if (textStr.contains("风险") || textStr.contains("了解") || textStr.contains("同意")
                    || resName.contains("risk") || resName.contains("check") || resName.contains("agree")) {
                cb.setChecked(true);
            }
        }
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            String textStr = text != null ? text.toString() : "";
            if (textStr.contains("了解更多") || resName.contains("learn_more") || resName.contains("swlimit")) {
                view.setVisibility(View.GONE);
                return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                processInstallerRiskViews(vg.getChildAt(i));
            }
        }
    }

    private static void installPureModeSettingsHook(HookSession session) {
        // 1. Hook SettingListFragment to add pure mode SwitchPreference into top-right Settings
        Class<?> fragmentClass = session.findOptionalClass(
                "com.android.packageinstaller.setting.SettingListFragment",
                "com.android.packageinstaller.SettingListFragment"
        );
        if (fragmentClass != null) {
            for (String method : new String[]{"onCreate", "onResume"}) {
                session.hook(fragmentClass, method, chain -> {
                    Object result = chain.proceed();
                    injectPureModePreference(session, chain.getThisObject());
                    return result;
                });
            }
        }

    }

    private static void injectPureModePreference(HookSession session, Object fragment) {
        if (fragment == null) return;
        try {
            Object screen = HookValues.invokeNoArg(fragment, "getPreferenceScreen");
            if (screen == null) return;

            Context context = (Context) HookValues.invokeNoArg(fragment, "getContext");
            if (context == null && fragment instanceof Activity) {
                context = (Context) fragment;
            }
            if (context == null) return;

            final Context finalCtx = context;
            boolean isPure = isPureModeActive(finalCtx);

            Object existing = HookValues.invoke(screen, "findPreference", "pure_app_installation_mode");
            if (existing != null) {
                try {
                    Method setChecked = existing.getClass().getMethod("setChecked", boolean.class);
                    setChecked.invoke(existing, isPure);
                } catch (Throwable ignored) {}
                return;
            }

            Class<?> prefClass = session.findOptionalClass(
                    "com.zte.mifavor.androidx.preference.SwitchPreferenceZTE",
                    "androidx.preference.SwitchPreferenceCompat",
                    "androidx.preference.SwitchPreference"
            );
            if (prefClass == null) return;

            Object pref = null;
            try {
                pref = prefClass.getConstructor(Context.class).newInstance(finalCtx);
            } catch (Throwable t1) {
                try {
                    pref = prefClass.getConstructor(Context.class, AttributeSet.class)
                            .newInstance(finalCtx, null);
                } catch (Throwable ignored) {}
            }
            if (pref == null) return;

            HookValues.invoke(pref, "setKey", "pure_app_installation_mode");
            HookValues.invoke(pref, "setTitle", "纯净安装模式");
            HookValues.invoke(pref, "setSummary", "限制安装未经安全认证的第三方应用");
            HookValues.invoke(pref, "setPersistent", false);
            try {
                Method setOrder = pref.getClass().getMethod("setOrder", int.class);
                setOrder.invoke(pref, 0);
            } catch (Throwable ignored) {}

            try {
                Method setChecked = pref.getClass().getMethod("setChecked", boolean.class);
                setChecked.invoke(pref, isPure);
            } catch (Throwable ignored) {}

            Class<?> listenerInterface = session.findOptionalClass(
                    "androidx.preference.Preference$OnPreferenceChangeListener",
                    "android.preference.Preference$OnPreferenceChangeListener"
            );
            if (listenerInterface != null) {
                Object listenerProxy = Proxy.newProxyInstance(
                        session.classLoader(),
                        new Class<?>[]{listenerInterface},
                        (proxy, method, methodArgs) -> {
                            if ("onPreferenceChange".equals(method.getName())
                                    && methodArgs != null && methodArgs.length >= 2) {
                                Object val = methodArgs[1];
                                boolean enabled = Boolean.TRUE.equals(val);
                                setPureModeActive(finalCtx, enabled);
                                return Boolean.TRUE;
                            }
                            return null;
                        }
                );
                try {
                    Method setListener = pref.getClass().getMethod(
                            "setOnPreferenceChangeListener", listenerInterface);
                    setListener.invoke(pref, listenerProxy);
                } catch (Throwable t) {
                    HookValues.invoke(pref, "setOnPreferenceChangeListener", listenerProxy);
                }
            }

            try {
                Method addMethod = screen.getClass().getMethod("addPreference",
                        session.findClass("androidx.preference.Preference"));
                addMethod.invoke(screen, pref);
            } catch (Throwable t) {
                HookValues.invoke(screen, "addPreference", pref);
            }
            session.log("PACKAGE_INSTALLER_PURE_MODE_PREF_ADDED");
        } catch (Throwable t) {
            session.log("PACKAGE_INSTALLER_PURE_MODE_INJECT_FAILED " + t.getMessage());
        }
    }

    private static boolean isPureModeActive(Context ctx) {
        if (ctx == null) return false;
        try {
            return android.provider.Settings.System.getInt(
                    ctx.getContentResolver(), "pure_app_installation_mode", 0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void setPureModeActive(Context ctx, boolean enabled) {
        if (ctx == null) return;
        try {
            android.provider.Settings.System.putInt(
                    ctx.getContentResolver(), "pure_app_installation_mode", enabled ? 1 : 0);
        } catch (Throwable ignored) {}
    }
}
