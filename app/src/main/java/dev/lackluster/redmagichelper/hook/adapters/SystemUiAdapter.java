package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;

import dev.lackluster.redmagichelper.config.ModuleConfig;
import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.ClockPolicy;

/** SystemUI clock, volume-warning and notification-icon compatibility hooks. */
public final class SystemUiAdapter implements HostAdapter {
    private static final ThreadLocal<Boolean> TEXT_CLOCK_GUARD =
            ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> FONT_SIZE_GUARD =
            ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> DATE_SIZE_GUARD =
            ThreadLocal.withInitial(() -> false);

    @Override
    public void install(HookSession session) {
        ModuleConfig config = session.config();
        boolean aodSeconds = config.getBoolean("systemui_screen_off_show_seconds", false);
        boolean aodPeriod = config.getBoolean("systemui_screen_off_period", false)
                || config.getBoolean("systemui_screen_off_show_period", false);
        boolean statusbarPeriod = config.getBoolean(
                "statusbar_clock_pull_down_show_period", false);
        boolean statusbarSeconds = config.getBoolean("statusbar_clock_show_seconds", false)
                || config.getBoolean("systemui_statusbar_show_seconds", false);

        if (aodSeconds || aodPeriod) {
            installTextClock(session, aodSeconds, aodPeriod);
        }
        if (statusbarPeriod || statusbarSeconds) {
            installStatusBarClock(session, statusbarPeriod, statusbarSeconds);
        }
        if (config.getBoolean("volume_disable_safety_warning", false)) {
            Class<?> volume = session.findClass("com.android.systemui.volume.VolumeDialogImpl");
            session.hook(volume, "showSafetyWarningH", chain -> null, int.class);
        }
        if (config.getBoolean("disable_flag_secure", false)
                || config.getBoolean("disable_flag_secure_enhanced", false)) {
            installFlagSecureBypass(session);
        }
        if (config.getBoolean("remove_alert_windows_notification", false)) {
            installAlertWindowNotificationBypass(session);
        }
        if (config.getBoolean("lockscreen_custom_font_switch", false)) {
            installLockScreenClockFont(session);
        }
        installNotificationLimit(session);
        session.log("SYSTEMUI_CAPABILITY_SCAN_COMPLETE");
    }

    private static Typeface sCachedTypeface = null;
    private static String sCachedFontKey = "";

    private static Context getAnyContext() {
        try {
            Class<?> atCls = Class.forName("android.app.ActivityThread");
            Method m = atCls.getMethod("currentApplication");
            Object app = m.invoke(null);
            if (app instanceof Context) {
                return ((Context) app).getApplicationContext();
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> appTools = Class.forName("com.zte.tools.AppTools");
            Method m = appTools.getMethod("getAppContext");
            Object ctx = m.invoke(null);
            if (ctx instanceof Context) {
                return ((Context) ctx).getApplicationContext();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void installLockScreenClockFont(HookSession session) {
        session.logSmali("Lcom/android/systemui/lockscreen/CustomClockFont;", "INIT: Registering lockscreen clock font hooks");

        // 1. Hook TextView.setTypeface. AOD is excluded by the view
        // classifier below, so its original typeface and size remain intact.
        session.hook(TextView.class, "setTypeface", chain -> {
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (!cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                return chain.proceed();
            }
            if (isLockScreenClockOrDateView(view)) {
                Context ctx = ((View) view).getContext();
                Typeface customTf = getLockscreenTypeface(ctx, session, cfg);
                if (customTf != null) {
                    Object[] args = chain.getArgs().toArray();
                    args[0] = customTf;
                    return chain.proceed(args);
                }
            }
            return chain.proceed();
        }, Typeface.class);

        session.hook(TextView.class, "setTypeface", chain -> {
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (!cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                return chain.proceed();
            }
            if (isLockScreenClockOrDateView(view)) {
                Context ctx = ((View) view).getContext();
                Typeface customTf = getLockscreenTypeface(ctx, session, cfg);
                if (customTf != null) {
                    Object[] args = chain.getArgs().toArray();
                    args[0] = customTf;
                    return chain.proceed(args);
                }
            }
            return chain.proceed();
        }, Typeface.class, int.class);

        // OEM clock layouts frequently restore their original text size after
        // applying a style. Re-apply the configured pixel size after that call.
        session.hook(TextView.class, "setTextSize", chain -> {
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            boolean isDate = isLockScreenDateView(view);
            if ((!isDate && Boolean.TRUE.equals(FONT_SIZE_GUARD.get()))
                    || (isDate && Boolean.TRUE.equals(DATE_SIZE_GUARD.get()))
                    || !cfg.getBoolean("lockscreen_custom_font_switch", false)
                    || (!isDate && !isLockScreenClockView(view))
                    || !(view instanceof TextView)) {
                return chain.proceed();
            }
            Object result = chain.proceed();
            if (isDate) {
                applyConfiguredDateSize((TextView) view, cfg);
            } else {
                applyConfiguredTextSize((TextView) view, cfg);
            }
            return result;
        }, int.class, float.class);

        session.hook(TextView.class, "setTextSize", chain -> {
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (!(view instanceof TextView)
                    || !cfg.getBoolean("lockscreen_custom_font_switch", false)
                    || !isLockScreenClockOrDateView(view)
                    || isSizeGuarded(view)) {
                return chain.proceed();
            }
            Object result = chain.proceed();
            applyConfiguredViewTextSize((TextView) view, cfg);
            return result;
        }, float.class);

        session.hook(TextView.class, "setTextAppearance", chain -> {
            Object result = chain.proceed();
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (cfg.getBoolean("lockscreen_custom_font_switch", false) && view instanceof TextView) {
                if (isLockScreenDateView(view)) {
                    applyConfiguredDateSize((TextView) view, cfg);
                } else if (isLockScreenClockView(view)) {
                    applyConfiguredTextSize((TextView) view, cfg);
                }
            }
            return result;
        }, Context.class, int.class);

        session.hook(TextView.class, "setTextAppearance", chain -> {
            Object result = chain.proceed();
            Object view = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (cfg.getBoolean("lockscreen_custom_font_switch", false) && view instanceof TextView) {
                if (isLockScreenDateView(view)) {
                    applyConfiguredDateSize((TextView) view, cfg);
                } else if (isLockScreenClockView(view)) {
                    applyConfiguredTextSize((TextView) view, cfg);
                }
            }
            return result;
        }, int.class);

        // 2. Hook TextView.onAttachedToWindow
        session.hook(TextView.class, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            Object viewObj = chain.getThisObject();
            ModuleConfig cfg = session.config();
            if (cfg.getBoolean("lockscreen_custom_font_switch", false)
                    && isLockScreenClockOrDateView(viewObj) && viewObj instanceof TextView) {
                applyFontToTextView((TextView) viewObj, session, cfg);
            }
            return result;
        });

        // 3. Hook Mifavor TextClock
        Class<?> textClockCls = session.findOptionalClass("com.zte.mifavor.views.TextClock");
        if (textClockCls != null) {
            session.hookAny(textClockCls, new String[]{"onAttachedToWindow", "onTimeChanged", "refreshTime"}, chain -> {
                Object result = chain.proceed();
                ModuleConfig cfg = session.config();
                if (cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                    Object thiz = chain.getThisObject();
                    if (thiz instanceof TextView && isLockScreenClockView(thiz)) {
                        applyFontToTextView((TextView) thiz, session, cfg);
                    }
                }
                return result;
            });
        }

        // 4. Hook normal lockscreen clock styles and containers.
        String[] clockStyleClasses = new String[]{
                "com.zte.mifavor.keyguard.settings.BaseLockScreenClock",
                "com.zte.mifavor.keyguard.settings.LockScreenClockDefault",
                "com.zte.mifavor.keyguard.settings.LockScreenClockHorizen",
                "com.zte.mifavor.keyguard.settings.LockScreenClockVertical",
                "com.zte.mifavor.keyguard.settings.LockScreenClockClip",
                "com.zte.mifavor.keyguard.settings.LockScreenClockArtword",
                "com.zte.mifavor.keyguard.settings.LockScreenClockIncarnation",
                "com.zte.mifavor.keyguard.settings.LockScreenClockStyleItem",
        };
        for (String clsName : clockStyleClasses) {
            Class<?> cls = session.findOptionalClass(clsName);
            if (cls != null) {
        session.hookAny(cls, new String[]{
                        "refreshClockViewFont", "refreshClockViewLayout", "onFinishInflate",
                        "onAttachedToWindow", "refreshTime", "updateClockFont"
                }, chain -> {
                    Object result = chain.proceed();
                    ModuleConfig cfg = session.config();
                    if (cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                        Object target = chain.getThisObject();
                        if (target instanceof View) {
                            applyFontToClockHierarchy((View) target, session, cfg);
                        }
                    }
                    return result;
                });
            }
        }

        // 5. Hook ClockViewUtils.refreshTextViewLayout
        Class<?> clockUtils = session.findOptionalClass("com.zte.mifavor.keyguard.settings.ClockViewUtils");
        if (clockUtils != null) {
            session.hook(clockUtils, "refreshTextViewLayout", chain -> {
                Object result = chain.proceed();
                ModuleConfig cfg = session.config();
                if (cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                    Object arg1 = chain.getArgs().get(1);
                    if (arg1 instanceof TextView && isLockScreenClockOrDateView(arg1)) {
                        applyFontToTextView((TextView) arg1, session, cfg);
                    }
                }
                return result;
            }, Context.class, TextView.class, int.class);

            session.hook(clockUtils, "refreshTextViewLayout", chain -> {
                Object result = chain.proceed();
                ModuleConfig cfg = session.config();
                if (cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                    Object arg0 = chain.getArgs().get(0);
                    if (arg0 instanceof TextView && isLockScreenClockOrDateView(arg0)) {
                        applyFontToTextView((TextView) arg0, session, cfg);
                    }
                }
                return result;
            }, TextView.class, int.class);
        }

        // 6. KeyguardStatusView
        Class<?> statusView = session.findOptionalClass("com.zte.mifavor.keyguard.KeyguardStatusView");
        if (statusView != null) {
            session.hook(statusView, "onFinishInflate", chain -> {
                Object result = chain.proceed();
                ModuleConfig cfg = session.config();
                Object target = chain.getThisObject();
                if (cfg.getBoolean("lockscreen_custom_font_switch", false)) {
                    if (target instanceof View) {
                        applyFontToClockHierarchy((View) target, session, cfg);
                    }
                }
                return result;
            });
        }
    }

    private static final byte TAG_NONE = 1;
    private static final byte TAG_CLOCK = 2;
    private static final byte TAG_DATE = 3;
    private static final byte TAG_AOD = 4;

    private static final java.util.Map<View, Byte> sViewClassificationCache =
            new java.util.WeakHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, String> sResNameCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static String getResourceEntryNameFast(View view) {
        int id = view.getId();
        if (id == View.NO_ID) {
            return "";
        }
        String cached = sResNameCache.get(id);
        if (cached != null) {
            return cached;
        }
        try {
            android.content.res.Resources res = view.getResources();
            if (res != null) {
                String name = res.getResourceEntryName(id);
                if (name != null) {
                    sResNameCache.put(id, name);
                    return name;
                }
            }
        } catch (Throwable ignored) {}
        sResNameCache.put(id, "");
        return "";
    }

    private static boolean containsIgnoreCase(String str, String search) {
        if (str == null || search == null) return false;
        final int len = search.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, search, 0, len)) {
                return true;
            }
        }
        return false;
    }

    private static byte classifyView(View view) {
        if (view == null) {
            return TAG_NONE;
        }
        Byte cached;
        synchronized (sViewClassificationCache) {
            cached = sViewClassificationCache.get(view);
        }
        if (cached != null) {
            return cached.byteValue();
        }

        byte result = computeViewClassification(view);
        synchronized (sViewClassificationCache) {
            sViewClassificationCache.put(view, Byte.valueOf(result));
        }
        return result;
    }

    private static byte computeViewClassification(View view) {
        if (isAodViewInternal(view)) {
            return TAG_AOD;
        }
        if (isLockScreenDateViewInternal(view)) {
            return TAG_DATE;
        }
        if (isLockScreenClockViewInternal(view)) {
            return TAG_CLOCK;
        }
        return TAG_NONE;
    }

    private static void applyFontToTextView(TextView tv, HookSession session, ModuleConfig cfg) {
        if (tv == null || !isLockScreenClockOrDateView(tv)) return;
        Context ctx = tv.getContext();
        Typeface customTf = getLockscreenTypeface(ctx, session, cfg);
        HookValues.runOnMain(tv, () -> {
            if (customTf != null) {
                tv.setTypeface(customTf);
            }
            applyConfiguredViewTextSize(tv, cfg);
        });
    }

    private static void applyConfiguredViewTextSize(TextView tv, ModuleConfig cfg) {
        if (isLockScreenDateView(tv)) {
            applyConfiguredDateSize(tv, cfg);
        } else {
            applyConfiguredTextSize(tv, cfg);
        }
    }

    private static boolean isLockScreenClockOrDateView(Object view) {
        if (!(view instanceof View)) {
            return false;
        }
        byte tag = classifyView((View) view);
        return tag == TAG_CLOCK || tag == TAG_DATE;
    }

    private static boolean isSizeGuarded(Object view) {
        return isLockScreenDateView(view)
                ? Boolean.TRUE.equals(DATE_SIZE_GUARD.get())
                : Boolean.TRUE.equals(FONT_SIZE_GUARD.get());
    }

    private static void applyConfiguredTextSize(TextView tv, ModuleConfig cfg) {
        int fontSizeSp = cfg.getIntFlexible("lockscreen_custom_font_size", 70);
        if (tv == null || fontSizeSp <= 0 || Boolean.TRUE.equals(FONT_SIZE_GUARD.get())) {
            return;
        }
        FONT_SIZE_GUARD.set(true);
        try {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        } finally {
            FONT_SIZE_GUARD.set(false);
        }
    }

    private static void applyConfiguredDateSize(TextView tv, ModuleConfig cfg) {
        int dateSizeSp = cfg.getIntFlexible("lockscreen_custom_date_size", 24);
        if (tv == null || dateSizeSp <= 0 || Boolean.TRUE.equals(DATE_SIZE_GUARD.get())) {
            return;
        }
        DATE_SIZE_GUARD.set(true);
        try {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSizeSp);
        } finally {
            DATE_SIZE_GUARD.set(false);
        }
    }

    private static boolean isLockScreenDateView(Object obj) {
        if (!(obj instanceof View)) {
            return false;
        }
        return classifyView((View) obj) == TAG_DATE;
    }

    private static boolean isLockScreenDateViewInternal(View view) {
        if (view.getId() != View.NO_ID) {
            String name = getResourceEntryNameFast(view);
            if ("date_view".equals(name) || name.contains("keyguard_date")) {
                return true;
            }
        }
        ViewParent parent = view.getParent();
        int depth = 0;
        while (parent instanceof View && depth++ < 5) {
            View parentView = (View) parent;
            if (parentView.getId() != View.NO_ID) {
                String pname = getResourceEntryNameFast(parentView);
                if ("date_view_area".equals(pname)) {
                    return true;
                }
            }
            parent = parentView.getParent();
        }
        return false;
    }

    private static void applyFontToClockHierarchy(View root, HookSession session, ModuleConfig cfg) {
        if (root == null) return;
        if (root instanceof TextView && isLockScreenClockOrDateView(root)) {
            applyFontToTextView((TextView) root, session, cfg);
        }
        View clockView = root.findViewById(0x7f0b02c7);
        if (clockView instanceof TextView) {
            applyFontToTextView((TextView) clockView, session, cfg);
        }
        Object mClockView = HookValues.readField(root, "mClockView");
        if (mClockView instanceof TextView) {
            applyFontToTextView((TextView) mClockView, session, cfg);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView && isLockScreenClockOrDateView(child)) {
                    applyFontToTextView((TextView) child, session, cfg);
                } else if (child instanceof ViewGroup) {
                    applyFontToClockHierarchy(child, session, cfg);
                }
            }
        }
    }

    private static boolean isLockScreenClockView(Object obj) {
        if (!(obj instanceof View)) return false;
        return classifyView((View) obj) == TAG_CLOCK;
    }

    private static boolean isLockScreenClockViewInternal(View view) {
        if (view.getId() == 0x7f0b02c7) return true;
        if (view.getId() != View.NO_ID) {
            String name = getResourceEntryNameFast(view);
            if ("clock_view".equals(name) || "keyguard_clock_view".equals(name)
                    || "lockscreen_clock".equals(name)) {
                return true;
            }
        }

        ViewParent parent = view.getParent();
        int depth = 0;
        while (parent instanceof View && depth++ < 6) {
            View pv = (View) parent;
            String pvClassName = pv.getClass().getName();
            if (containsIgnoreCase(pvClassName, "aod") || containsIgnoreCase(pvClassName, "doze")) {
                return false;
            }
            if (pvClassName.contains("LockScreenClock")
                    || pvClassName.contains("ClockStatusView")
                    || pvClassName.contains("KeyguardStatusView")) {
                if (view instanceof TextView) return true;
            }
            if (pv.getId() != View.NO_ID) {
                String pname = getResourceEntryNameFast(pv);
                if ("keyguard_clock_container".equals(pname)
                        || "keyguard_clock_style_default".equals(pname)
                        || "clock_area".equals(pname)
                        || pname.startsWith("keyguard_clock_style")) {
                    if (view instanceof TextView) return true;
                }
            }
            parent = pv.getParent();
        }
        return false;
    }

    private static boolean isAodView(Object obj) {
        if (!(obj instanceof View)) {
            return false;
        }
        return classifyView((View) obj) == TAG_AOD;
    }

    private static boolean isAodViewInternal(View view) {
        View current = view;
        for (int depth = 0; current != null && depth < 8; depth++) {
            String className = current.getClass().getName();
            if (containsIgnoreCase(className, "aod")
                    || containsIgnoreCase(className, "doze")
                    || containsIgnoreCase(className, "alwaysondisplay")
                    || containsIgnoreCase(className, "screenoff")) {
                return true;
            }
            if (current.getId() != View.NO_ID) {
                String resourceName = getResourceEntryNameFast(current);
                if (containsIgnoreCase(resourceName, "aod")
                        || containsIgnoreCase(resourceName, "doze")
                        || containsIgnoreCase(resourceName, "screen_off")
                        || containsIgnoreCase(resourceName, "screenoff")) {
                    return true;
                }
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static Typeface getLockscreenTypeface(android.content.Context context, HookSession session, ModuleConfig config) {
        String systemFontPath = config.getString("lockscreen_system_font_path", "");
        String importedFontName = config.getString("lockscreen_custom_font_imported_name", "");
        int weight = config.getIntFlexible("lockscreen_custom_font_weight", 400);
        long timestamp = config.getLong("lockscreen_custom_font_timestamp", 0L);

        Context ctx = context != null ? context : getAnyContext();
        String cacheKey = (systemFontPath == null ? "" : systemFontPath) + "|"
                + (importedFontName == null ? "" : importedFontName) + "|"
                + weight + "|" + timestamp;
        if (sCachedTypeface != null && cacheKey.equals(sCachedFontKey)) {
            return sCachedTypeface;
        }

        Typeface baseTypeface = null;
        // Priority 1: System-installed font (highest priority)
        if (systemFontPath != null && !systemFontPath.trim().isEmpty()) {
            File fontFile = new File(systemFontPath.trim());
            if (fontFile.exists() && fontFile.canRead()) {
                try {
                    baseTypeface = Typeface.createFromFile(fontFile);
                    session.logSmali("Landroid/graphics/Typeface;->createFromFile(Ljava/io/File;)Landroid/graphics/Typeface;",
                            "LOADED system font: " + systemFontPath);
                } catch (Throwable t) {
                    session.logSmali("Landroid/graphics/Typeface;->createFromFile(Ljava/io/File;)Landroid/graphics/Typeface;",
                            "FAILED to load system font: " + t.getMessage());
                }
            }
        }

        // Priority 2: Custom imported font
        if (baseTypeface == null && ctx != null && importedFontName != null && !importedFontName.trim().isEmpty()) {
            File cacheFile = new File(ctx.getCacheDir(), "lockscreen_" + Math.abs(importedFontName.hashCode()) + (timestamp > 0 ? ("_" + timestamp) : "") + ".ttf");
            if (!cacheFile.exists() || cacheFile.length() == 0) {
                try {
                    android.net.Uri fontUri = android.net.Uri.parse("content://dev.lackluster.redmagichelper.config_provider/font");
                    try (InputStream is = ctx.getContentResolver().openInputStream(fontUri);
                         FileOutputStream fos = new FileOutputStream(cacheFile)) {
                        if (is != null) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                            session.logSmali("Landroid/content/ContentResolver;->openInputStream(Landroid/net/Uri;)Ljava/io/InputStream;",
                                    "CACHED imported font file size=" + cacheFile.length());
                        }
                    }
                } catch (Throwable t) {
                    session.logSmali("Landroid/content/ContentResolver;->openInputStream", "Stream fallback: " + t.getMessage());
                    // Fallback to getCustomFontBytes bundle call
                    try {
                        android.net.Uri providerUri = android.net.Uri.parse("content://dev.lackluster.redmagichelper.config_provider");
                        Bundle bundle = ctx.getContentResolver().call(providerUri, "getCustomFontBytes", null, null);
                        if (bundle != null && bundle.getBoolean("success", false)) {
                            byte[] bytes = bundle.getByteArray("font_bytes");
                            if (bytes != null && bytes.length > 0) {
                                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                                    fos.write(bytes);
                                }
                                session.logSmali("ConfigProvider->getCustomFontBytes", "CACHED imported font bytes=" + bytes.length);
                            }
                        }
                    } catch (Throwable t2) {
                        session.logSmali("ConfigProvider->getCustomFontBytes", "FAILED: " + t2.getMessage());
                    }
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    baseTypeface = Typeface.createFromFile(cacheFile);
                    session.logSmali("Landroid/graphics/Typeface;->createFromFile", "LOADED imported font: " + importedFontName);
                } catch (Throwable t) {
                    session.logSmali("Landroid/graphics/Typeface;->createFromFile", "FAILED to load imported font: " + t.getMessage());
                }
            }
        }

        if (baseTypeface == null) {
            baseTypeface = Typeface.DEFAULT;
        }

        try {
            int clampedWeight = HookValues.clamp(weight, 100, 1000);
            sCachedTypeface = Typeface.create(baseTypeface, clampedWeight, false);
            sCachedFontKey = cacheKey;
            return sCachedTypeface;
        } catch (Throwable t) {
            sCachedTypeface = baseTypeface;
            sCachedFontKey = cacheKey;
            return baseTypeface;
        }
    }

    private static void installFlagSecureBypass(HookSession session) {
        // SystemUI owns several protected preview/screenshot windows. Remove
        // only FLAG_SECURE from Window.setFlags arguments in this process;
        // ordinary app windows remain untouched because this adapter is scoped
        // to com.android.systemui.
        session.hook(Window.class, "setFlags", chain -> {
            Object flags = HookValues.arg(chain, 0);
            Object mask = HookValues.arg(chain, 1);
            if (flags instanceof Integer && mask instanceof Integer
                    && ((((Integer) mask) & WindowManagerFlags.FLAG_SECURE) != 0)) {
                Object[] args = chain.getArgs().toArray();
                args[0] = ((Integer) flags) & ~WindowManagerFlags.FLAG_SECURE;
                args[1] = ((Integer) mask) & ~WindowManagerFlags.FLAG_SECURE;
                return chain.proceed(args);
            }
            return chain.proceed();
        }, int.class, int.class);
        session.log("SYSTEMUI_FLAG_SECURE_BYPASS_INSTALLED");
    }

    private static void installAlertWindowNotificationBypass(HookSession session) {
        String[] candidates = new String[]{
                "com.android.systemui.statusbar.notification.AlertWindowNotification",
                "com.android.systemui.statusbar.notification.row.AlertWindowNotification",
                "com.android.systemui.statusbar.notification.AlertingNotificationManager"
        };
        for (String name : candidates) {
            Class<?> type = session.findOptionalClass(name);
            if (type != null) {
                session.hookAny(type, new String[]{
                        "show", "post", "add", "onNotificationPosted"
                }, chain -> null);
                session.log("SYSTEMUI_ALERT_WINDOW_NOTIFICATION_BYPASS_INSTALLED class=" + name);
                return;
            }
        }
        session.missing("systemui-alert-window-notification-capability");
    }

    private static final class WindowManagerFlags {
        private static final int FLAG_SECURE = 0x00002000;

        private WindowManagerFlags() {
        }
    }

    private static void installTextClock(
            HookSession session,
            boolean secondsEnabled,
            boolean periodEnabled
    ) {
        if (secondsEnabled) {
            session.hook(TextClock.class, "setFormat12Hour", chain -> {
                Object value = HookValues.arg(chain, 0);
                if (value != null) {
                    return HookValues.proceedWithArgument(chain, 0, ClockPolicy.withSeconds(
                            String.valueOf(value), false));
                }
                return chain.proceed();
            }, CharSequence.class);
            session.hook(TextClock.class, "setFormat24Hour", chain -> {
                Object value = HookValues.arg(chain, 0);
                if (value != null) {
                    return HookValues.proceedWithArgument(chain, 0, ClockPolicy.withSeconds(
                            String.valueOf(value), true));
                }
                return chain.proceed();
            }, CharSequence.class);
        }

        session.hook(TextClock.class, "onTimeChanged", chain -> {
            Object result = chain.proceed();
            if (Boolean.TRUE.equals(TEXT_CLOCK_GUARD.get())
                    || !(chain.getThisObject() instanceof TextClock)) {
                return result;
            }
            TextClock clock = (TextClock) chain.getThisObject();
            TEXT_CLOCK_GUARD.set(true);
            try {
                if (secondsEnabled) {
                    CharSequence format24 = clock.getFormat24Hour();
                    CharSequence format12 = clock.getFormat12Hour();
                    clock.setFormat24Hour(ClockPolicy.withSeconds(
                            format24 == null ? null : format24.toString(), true));
                    clock.setFormat12Hour(ClockPolicy.withSeconds(
                            format12 == null ? null : format12.toString(), false));
                }
                if (periodEnabled) {
                    Locale locale = localeOf(clock);
                    if (ClockPolicy.isChinese(locale)) {
                        int hour = calendarOf(clock).get(Calendar.HOUR_OF_DAY);
                        String current = String.valueOf(clock.getText());
                        String adjusted = ClockPolicy.withPeriodPrefix(current, hour);
                        if (!TextUtils.equals(current, adjusted)) {
                            clock.setText(adjusted);
                        }
                    }
                }
            } catch (Throwable error) {
                session.log("SYSTEMUI_TEXT_CLOCK_UPDATE_FAILED "
                        + error.getClass().getSimpleName());
            } finally {
                TEXT_CLOCK_GUARD.set(false);
            }
            return result;
        });
    }

    private static void installStatusBarClock(
            HookSession session,
            boolean periodEnabled,
            boolean secondsEnabled
    ) {
        Class<?> clock = session.findClass("com.android.systemui.statusbar.policy.Clock");
        if (secondsEnabled) {
            session.hook(clock, "setShowSeconds", chain -> {
                return HookValues.proceedWithArgument(chain, 0, true);
            }, boolean.class);
            session.hook(clock, "onAttachedToWindow", chain -> {
                Object result = chain.proceed();
                HookValues.writeField(chain.getThisObject(), true, "mShowSeconds");
                return result;
            });
        }
        if (periodEnabled || secondsEnabled) {
            session.hook(clock, "updateClock", chain -> {
                Object owner = chain.getThisObject();
                if (secondsEnabled) {
                    HookValues.writeField(owner, true, "mShowSeconds");
                }
                Object result = chain.proceed();
                if (!(owner instanceof TextView)) {
                    return result;
                }
                TextView view = (TextView) owner;
                Calendar calendar = calendarOf(owner);
                String current = String.valueOf(view.getText());
                String adjusted = current;

                if (secondsEnabled && !adjusted.isEmpty()
                        && adjusted.indexOf(':') > 0 && adjusted.indexOf(':') == adjusted.lastIndexOf(':')) {
                    int sec = calendar.get(Calendar.SECOND);
                    adjusted = String.format(Locale.getDefault(), "%s:%02d", adjusted, sec);
                }
                if (periodEnabled && ClockPolicy.isChinese(localeOf(view))) {
                    adjusted = ClockPolicy.withPeriodPrefix(
                            adjusted, calendar.get(Calendar.HOUR_OF_DAY));
                }
                if (!TextUtils.equals(current, adjusted)) {
                    final String finalAdjusted = adjusted;
                    HookValues.runOnMain(view, () -> view.setText(finalAdjusted));
                }
                return result;
            });
        }
    }

    private static void installNotificationLimit(HookSession session) {
        ModuleConfig config = session.config();
        // remove_restrictions_window_number belongs to the WindowReply limit
        // in system_server; it must not change the unrelated status-bar
        // notification icon count.
        boolean enabled = config.getBoolean("statusbar_notif_max", false);
        if (!enabled) {
            return;
        }
        int configured = config.getIntFlexible("statusbar_notif_max_value", 10);
        int maximum = HookValues.clamp(configured, 1, 50);
        Class<?> container = session.findClass(
                "com.android.systemui.statusbar.phone.NotificationIconContainer");
        session.hook(container, "shouldForceOverflow", chain -> {
            if (chain.getArgs().size() > 3) {
                return HookValues.proceedWithArgument(chain, 3, maximum);
            }
            return chain.proceed();
        }, int.class, int.class, float.class, int.class);
        session.hook(container, "initResources", chain -> {
            Object result = chain.proceed();
            if (!HookValues.writeField(chain.getThisObject(), maximum, "mMaxStaticIcons")) {
                session.missing("field=com.android.systemui.statusbar.phone.NotificationIconContainer#mMaxStaticIcons");
            }
            return result;
        });
        session.log("SYSTEMUI_NOTIFICATION_LIMIT_INSTALLED max=" + maximum);
    }

    private static Locale localeOf(View view) {
        try {
            return view.getResources().getConfiguration().getLocales().get(0);
        } catch (Throwable ignored) {
            return Locale.getDefault();
        }
    }

    private static Calendar calendarOf(Object owner) {
        Object value = HookValues.readField(owner, "mCalendar");
        if (value instanceof Calendar) {
            return (Calendar) value;
        }
        return Calendar.getInstance();
    }
}
