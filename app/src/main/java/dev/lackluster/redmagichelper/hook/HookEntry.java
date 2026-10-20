package dev.lackluster.redmagichelper.hook;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;

import dev.lackluster.redmagichelper.config.ModuleConfig;
import dev.lackluster.redmagichelper.hook.adapters.BrowserAdapter;
import dev.lackluster.redmagichelper.hook.adapters.DoubleAppAdapter;
import dev.lackluster.redmagichelper.hook.adapters.FanAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameAssistAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameFloatAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameHelperAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameHighlightsAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameLabAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GameLauncherAdapter;
import dev.lackluster.redmagichelper.hook.adapters.GamePluginTriggerAdapter;
import dev.lackluster.redmagichelper.hook.adapters.BeautifyAdapter;
import dev.lackluster.redmagichelper.hook.adapters.FileBrowserAdapter;
import dev.lackluster.redmagichelper.hook.adapters.LauncherAdapter;
import dev.lackluster.redmagichelper.hook.adapters.PermissionControllerAdapter;
import dev.lackluster.redmagichelper.hook.adapters.NfcAdapter;
import dev.lackluster.redmagichelper.hook.adapters.NeoStoreAdapter;
import dev.lackluster.redmagichelper.hook.adapters.PackageInstallerAdapter;
import dev.lackluster.redmagichelper.hook.adapters.RecommendAdapter;
import dev.lackluster.redmagichelper.hook.adapters.SettingsAdapter;
import dev.lackluster.redmagichelper.hook.adapters.SystemServerAdapter;
import dev.lackluster.redmagichelper.hook.adapters.SystemUpdateAdapter;
import dev.lackluster.redmagichelper.hook.adapters.SystemUiAdapter;
import dev.lackluster.redmagichelper.hook.adapters.WeatherAdapter;
import dev.lackluster.redmagichelper.util.HookLog;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** LibXposed entry point rebuilt around runtime capability detection. */
public final class HookEntry extends XposedModule {
    private volatile ModuleConfig config;
    /**
     * LibXposed delivers hot-reload callbacks without a new PackageReadyParam.
     * Keep the target from the last package callback so a preference change can
     * reinstall the hooks in the already-running process.
     */
    private volatile String lastPackageName;
    private volatile ClassLoader lastClassLoader;
    private volatile ApplicationInfo lastApplicationInfo;
    private volatile String installedPackageName;
    private volatile ClassLoader installedClassLoader;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        refreshPreferences();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String packageName = normalizePackageName(param.getPackageName());
        boolean firstPackage = param.isFirstPackage();
        // On some LSPosed/RedMagic builds the framework callback arrives as a
        // non-first PackageReadyParam even though it is the only callback for
        // system_server. Do not discard that callback; otherwise every
        // SystemServerAdapter feature silently remains uninstalled.
        if (!"android".equals(packageName) && adapterFor(packageName) == null) {
            return;
        }
        rememberTarget(packageName, param.getClassLoader(), param.getApplicationInfo());
        refreshPreferences();
        installForPackage(packageName, param.getClassLoader(), param.getApplicationInfo());
    }

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        refreshPreferences();
        rememberTarget("android", param.getClassLoader(), null);
        installForPackage("android", param.getClassLoader(), null);
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        param.setSavedInstanceState(new Object[]{lastPackageName, lastClassLoader, lastApplicationInfo});
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        for (XposedInterface.HookHandle handle : param.getOldHookHandles()) {
            try {
                handle.unhook();
            } catch (Throwable error) {
                HookLog.error("entry", "HOT_RELOAD_UNHOOK_FAILED", error);
            }
        }
        installedPackageName = null;
        installedClassLoader = null;
        refreshPreferences();
        Object saved = param.getSavedInstanceState();
        if (saved instanceof Object[]) {
            Object[] arr = (Object[]) saved;
            if (arr.length > 0 && arr[0] instanceof String) lastPackageName = (String) arr[0];
            if (arr.length > 1 && arr[1] instanceof ClassLoader) lastClassLoader = (ClassLoader) arr[1];
            if (arr.length > 2 && arr[2] instanceof ApplicationInfo) lastApplicationInfo = (ApplicationInfo) arr[2];
        }

        String processPackage = normalizePackageName(param.getProcessName());
        String packageName = processPackage;
        ClassLoader classLoader = lastClassLoader;
        ApplicationInfo applicationInfo = lastApplicationInfo;

        // The process name is authoritative when it identifies one of our
        // supported targets.  For framework versions that report a service
        // or an abbreviated process name, fall back to the saved callback.
        if (adapterFor(processPackage) == null && lastPackageName != null) {
            packageName = lastPackageName;
        }
        if (adapterFor(packageName) == null) {
            if (processPackage.contains("tgk") || processPackage.contains("gamelauncher")) {
                packageName = "cn.nubia.gamelauncher";
            }
        }
        if (classLoader == null) {
            try {
                Class<?> atClass = Class.forName("android.app.ActivityThread");
                Object at = atClass.getMethod("currentActivityThread").invoke(null);
                if (at != null) {
                    Object app = atClass.getMethod("getApplication").invoke(at);
                    if (app instanceof android.content.Context) {
                        classLoader = ((android.content.Context) app).getClassLoader();
                        if (applicationInfo == null) {
                            applicationInfo = ((android.content.Context) app).getApplicationInfo();
                        }
                    }
                }
            } catch (Throwable ignored) {}
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
        }
        if (classLoader == null) {
            HookLog.error("entry", "HOT_RELOAD_TARGET_CLASSLOADER",
                    new IllegalStateException("No class loader available for " + packageName));
        } else {
            installForPackage(packageName, classLoader, applicationInfo);
        }
    }

    private void refreshPreferences() {
        try {
            SharedPreferences remote = getRemotePreferences("config");
            config = new ModuleConfig(remote);
        } catch (Throwable error) {
            config = new ModuleConfig(null);
            HookLog.error("entry", "REMOTE_PREFS_UNAVAILABLE", error);
        }
    }

    private synchronized void installForPackage(
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo info
    ) {
        ModuleConfig current = config;
        if (current == null || !current.enabled()) {
            return;
        }

        HostAdapter adapter = adapterFor(packageName);
        if (adapter == null) {
            return;
        }
        if (packageName.equals(installedPackageName) && classLoader == installedClassLoader) {
            return;
        }

        HookSession session = new HookSession(this, packageName, classLoader, info, current);
        try {
            if (!"android".equals(packageName)) {
                installConfigProviderSync(session);
            }
            adapter.install(session);
            installedPackageName = packageName;
            installedClassLoader = classLoader;
        } catch (Throwable error) {
            session.recordFailure("ADAPTER_FAILED", error);
            session.unhookAll();
            installedPackageName = null;
            installedClassLoader = null;
        } finally {
            session.logScanSummary();
        }
    }

    private void installConfigProviderSync(HookSession session) {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method m = atClass.getMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof android.content.Context) {
                session.trySyncConfigFromProvider((android.content.Context) app);
            }
        } catch (Throwable ignored) {}

        Class<?> appClass = session.findOptionalClass("android.app.Application");
        if (appClass != null) {
            session.hook(appClass, "onCreate", chain -> {
                Object res = chain.proceed();
                try {
                    Object thiz = chain.getThisObject();
                    if (thiz instanceof android.content.Context) {
                        session.trySyncConfigFromProvider((android.content.Context) thiz);
                    }
                } catch (Throwable ignored) {}
                return res;
            });
        }
    }

    private void rememberTarget(
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo applicationInfo
    ) {
        lastPackageName = normalizePackageName(packageName);
        lastClassLoader = classLoader;
        lastApplicationInfo = applicationInfo;
    }

    private static String normalizePackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        int colon = packageName.indexOf(':');
        return colon > 0 ? packageName.substring(0, colon) : packageName;
    }

    private HostAdapter adapterFor(String packageName) {
        switch (packageName) {
            case "android":
                return new SystemServerAdapter();
            case "com.android.systemui":
                return new SystemUiAdapter();
            case "com.zte.zdm":
                return new SystemUpdateAdapter();
            case "com.zte.beautify":
                return new BeautifyAdapter();
            case "cn.nubia.fan":
                return new FanAdapter();
            case "cn.nubia.gameassist":
                return new GameAssistAdapter();
            case "cn.nubia.gamehelpmodule":
                return new GameHelperAdapter();
            case "cn.nubia.gamelauncher":
            case "com.nubia.tgkservice":
            case "cn.nubia.tgk":
            case "cn.nubia.gamehelperline":
                // Shoulder-key line service is a separate package on
                // RedMagicOS 11 and owns the actual enable-list lookup.
                return new GameLauncherAdapter();
            case "cn.zte.gamefloat":
                return new GameFloatAdapter();
            case "cn.nubia.gamehighlights":
                return new GameHighlightsAdapter();
            case "com.zte.game.plugintrigger":
                return new GamePluginTriggerAdapter();
            case "com.android.nfc":
                return new NfcAdapter();
            case "com.zte.cn.doubleapp":
                return new DoubleAppAdapter();
            case "cn.nubia.filebrowser":
                return new FileBrowserAdapter();
            case "cn.nubia.neostore":
            case "cn.nubia.neogamecenter":
                return new NeoStoreAdapter();
            case "com.android.packageinstaller":
                return new PackageInstallerAdapter();
            case "com.android.permissioncontroller":
                return new PermissionControllerAdapter();
            case "com.zte.mifavor.weather":
                return new WeatherAdapter();
            case "com.zte.mifavor.launcher":
                return new LauncherAdapter();
            case "com.android.settings":
            case "cn.nubia.gamelab":
                if ("com.android.settings".equals(packageName)) {
                    return new SettingsAdapter();
                }
                return new GameLabAdapter();
            case "com.zte.recommend":
                return new RecommendAdapter();
            case "cn.nubia.browser":
                return new BrowserAdapter();
            default:
                return null;
        }
    }
}
