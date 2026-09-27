package dev.lackluster.redmagichelper.hook.adapters;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.ClockPolicy;

/** Launcher clock and icon customization hooks for the extracted MFV build. */
public final class LauncherAdapter implements HostAdapter {
    private static final String[] CLOCK_PERIOD_IDS = {
            "widget_am_pm_mfvclr",
            "appwidget_text_am_mfvclr",
            "appwidget_text_ampm_mfvclr"
    };

    private static WeakReference<TextView> sMemoryTextViewRef;
    private static WeakReference<RelativeLayout> sActionButtonsRef;
    private static WeakReference<Activity> sActivityRef;
    private static ActivityManager sActivityManager;
    private static BroadcastReceiver sDarkReceiver;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final ActivityManager.MemoryInfo sMemInfo = new ActivityManager.MemoryInfo();
    private static volatile HookSession sSession;

    private static final Runnable sMemoryUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            TextView tv = sMemoryTextViewRef != null ? sMemoryTextViewRef.get() : null;
            ActivityManager am = sActivityManager;
            if (tv == null || am == null) {
                sMainHandler.postDelayed(this, 1000L);
                return;
            }
            try {
                am.getMemoryInfo(sMemInfo);
                Context ctx = tv.getContext();
                String text = Formatter.formatFileSize(ctx, sMemInfo.availMem)
                        + "/"
                        + Formatter.formatFileSize(ctx, sMemInfo.totalMem);
                Activity act = sActivityRef != null ? sActivityRef.get() : null;
                if (act != null) {
                    act.runOnUiThread(() -> tv.setText(text));
                } else {
                    tv.post(() -> tv.setText(text));
                }
            } catch (Throwable ignored) {
            }
            sMainHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void install(HookSession session) {
        sSession = session;
        boolean clockEnabled = session.config().getBoolean(
                "system_time_component_desktop_switch", false);
        if (clockEnabled) {
            installClock(session);
        }
        if (session.config().getBoolean("desktop_app_icon_customize_switch", false)) {
            installIconCustomization(session);
        }
        if (session.config().getBoolean("system_desktop_recent_task_display_memory", false)) {
            installRecentTasksMemory(session);
        }
        session.log("LAUNCHER_CAPABILITY_SCAN_COMPLETE");
    }

    private static void installClock(HookSession session) {
        boolean seconds = session.config().getBoolean(
                "system_time_component_desktop_show_seconds", false);
        boolean period = session.config().getBoolean(
                "system_time_component_desktop_show_period", false);

        if (seconds) {
            session.hook(TextClock.class, "setFormat12Hour", chain -> {
                return HookValues.proceedWithArgument(
                        chain, 0, ClockPolicy.desktopFormat(false));
            }, CharSequence.class);
            session.hook(TextClock.class, "setFormat24Hour", chain -> {
                return HookValues.proceedWithArgument(
                        chain, 0, ClockPolicy.desktopFormat(true));
            }, CharSequence.class);
        }
        if (period) {
            session.hook(TextClock.class, "onTimeChanged", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof TextClock) {
                    TextClock clock = (TextClock) owner;
                    String id = HookValues.resourceEntryName(clock);
                    if (ClockPolicy.matchesResourceName(id, CLOCK_PERIOD_IDS)) {
                        Locale locale;
                        try {
                            locale = clock.getResources().getConfiguration().getLocales().get(0);
                        } catch (Throwable ignored) {
                            locale = Locale.getDefault();
                        }
                        String label = ClockPolicy.isChinese(locale)
                                ? ClockPolicy.periodLabel(Calendar.getInstance()
                                .get(Calendar.HOUR_OF_DAY)) : null;
                        if (label != null) {
                            HookValues.runOnMain(clock, () -> clock.setText(label));
                        }
                    }
                }
                return result;
            });
        }
        if (!seconds && !period) {
            session.log("LAUNCHER_CLOCK_FEATURES_DISABLED");
        }
    }

    private static void installIconCustomization(HookSession session) {
        IconOverrides overrides = IconOverrides.parse(
                session.config().getString("desktop_app_icon_overrides", ""));
        if (overrides.isEmpty()) {
            session.log("LAUNCHER_ICON_OVERRIDES_EMPTY");
            return;
        }

        Class<?> itemWithIcon = session.findClass("com.android.launcher3.model.data.z");
        Class<?> itemInfo = session.findClass("com.android.launcher3.model.data.y");
        Class<?> workspaceItem = session.findClass("com.android.launcher3.model.data.I");
        Class<?> bubble = session.findClass("com.android.launcher3.BubbleTextView");
        Class<?> databaseValues = session.findClass("com.android.launcher3.util.G");
        if (itemWithIcon == null || itemInfo == null || bubble == null) {
            session.missing("launcher icon customization classes");
            return;
        }

        session.hook(itemWithIcon, "G", chain -> {
            Object owner = chain.getThisObject();
            IconOverride override = overrides.find(owner);
            if (override != null) {
                replaceLauncherIcon(session, owner, override);
            }
            return chain.proceed();
        }, Context.class, int.class);

        session.hook(bubble, "B", chain -> {
            Object item = HookValues.arg(chain, 0);
            applyNameOverride(item, overrides.find(item));
            return chain.proceed();
        }, itemInfo);

        if (databaseValues != null) {
            session.hook(itemInfo, "onAddToDatabase", chain -> {
                applyNameOverride(chain.getThisObject(), overrides.find(chain.getThisObject()));
                return chain.proceed();
            }, databaseValues);
            session.hook(itemInfo, "writeToValues", chain -> {
                applyNameOverride(chain.getThisObject(), overrides.find(chain.getThisObject()));
                return chain.proceed();
            }, databaseValues);
            if (workspaceItem != null) {
                session.hook(workspaceItem, "onAddToDatabase", chain -> {
                    applyNameOverride(chain.getThisObject(),
                            overrides.find(chain.getThisObject()));
                    return chain.proceed();
                }, databaseValues);
            }
        } else {
            session.missing("class=com.android.launcher3.util.G");
        }
        session.log("LAUNCHER_ICON_CAPABILITY_INSTALLED count=" + overrides.size());
    }

    private static void applyNameOverride(Object item, IconOverride override) {
        if (item == null || override == null || override.name == null
                || override.name.isEmpty()) {
            return;
        }
        HookValues.writeField(item, override.name, "title");
        HookValues.writeField(item, override.name, "contentDescription");
    }

    private static void replaceLauncherIcon(
            HookSession session,
            Object owner,
            IconOverride override
    ) {
        if (owner == null || override.iconPath == null || override.iconPath.isEmpty()) {
            return;
        }
        Bitmap bitmap = override.loadBitmap();
        if (bitmap == null) {
            session.log("LAUNCHER_ICON_FILE_UNAVAILABLE path=" + override.iconPath);
            return;
        }
        Object current = HookValues.readField(owner, "g");
        int shape = HookValues.readIntField(current, 0, "b");
        Class<?> iconClass = session.findClass("s1.d");
        if (iconClass == null) {
            return;
        }
        try {
            Constructor<?> constructor = iconClass.getConstructor(Bitmap.class, int.class);
            Object replacement = constructor.newInstance(bitmap, shape);
            if (!HookValues.writeField(owner, replacement, "g")) {
                session.missing("field=com.android.launcher3.model.data.z#g");
            }
        } catch (Throwable error) {
            session.log("LAUNCHER_ICON_REPLACE_FAILED "
                    + error.getClass().getSimpleName());
        }
    }

    private static final class IconOverrides {
        private final List<IconOverride> items;

        private IconOverrides(List<IconOverride> items) {
            this.items = items;
        }

        static IconOverrides parse(String json) {
            List<IconOverride> result = new ArrayList<>();
            if (json == null || json.trim().isEmpty() || json.length() > 512 * 1024) {
                return new IconOverrides(result);
            }
            int cursor = 0;
            while (cursor < json.length()) {
                int start = json.indexOf('{', cursor);
                if (start < 0) {
                    break;
                }
                int end = json.indexOf('}', start + 1);
                if (end < 0) {
                    break;
                }
                String object = json.substring(start + 1, end);
                Integer user = number(object, "user");
                String pkg = string(object, "pkg");
                String name = string(object, "name");
                String icon = string(object, "icon");
                if (user != null && pkg != null && !pkg.isEmpty()) {
                    result.add(new IconOverride(user, pkg, name, icon));
                }
                cursor = end + 1;
            }
            return new IconOverrides(result);
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        int size() {
            return items.size();
        }

        IconOverride find(Object item) {
            if (item == null) {
                return null;
            }
            String pkg = targetPackage(item);
            int user = userId(HookValues.readField(item, "user"));
            if (pkg == null) {
                return null;
            }
            for (IconOverride override : items) {
                if (override.user == user && override.pkg.equals(pkg)) {
                    return override;
                }
            }
            return null;
        }

        private static String string(String object, String key) {
            String marker = "\"" + key + "\"";
            int keyStart = object.indexOf(marker);
            if (keyStart < 0) {
                return null;
            }
            int colon = object.indexOf(':', keyStart + marker.length());
            int first = colon < 0 ? -1 : object.indexOf('"', colon + 1);
            if (first < 0) {
                return null;
            }
            int second = object.indexOf('"', first + 1);
            return second < 0 ? null : object.substring(first + 1, second);
        }

        private static Integer number(String object, String key) {
            String marker = "\"" + key + "\"";
            int keyStart = object.indexOf(marker);
            if (keyStart < 0) {
                return null;
            }
            int colon = object.indexOf(':', keyStart + marker.length());
            if (colon < 0) {
                return null;
            }
            int end = colon + 1;
            while (end < object.length() && Character.isWhitespace(object.charAt(end))) {
                end++;
            }
            int begin = end;
            while (end < object.length() && (object.charAt(end) == '-'
                    || Character.isDigit(object.charAt(end)))) {
                end++;
            }
            try {
                return Integer.valueOf(object.substring(begin, end));
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static final class IconOverride {
        final int user;
        final String pkg;
        final String name;
        final String iconPath;
        private Bitmap bitmap;

        IconOverride(int user, String pkg, String name, String iconPath) {
            this.user = user;
            this.pkg = pkg;
            this.name = name;
            this.iconPath = iconPath;
        }

        Bitmap loadBitmap() {
            if (bitmap != null) {
                return bitmap;
            }
            try {
                File file = new File(iconPath);
                if (!file.isFile() || file.length() <= 0 || file.length() > 8L * 1024L * 1024L) {
                    return null;
                }
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Throwable ignored) {
                bitmap = null;
            }
            return bitmap;
        }
    }

    private static String targetPackage(Object item) {
        Object packageValue = HookValues.invokeNoArg(item, "getTargetPackage");
        if (packageValue instanceof String) {
            return (String) packageValue;
        }
        Object intent = HookValues.readField(item, "k");
        if (intent instanceof Intent) {
            ComponentName component = ((Intent) intent).getComponent();
            return component == null ? null : component.getPackageName();
        }
        Object component = HookValues.invokeNoArg(item, "getTargetComponent");
        return component instanceof ComponentName
                ? ((ComponentName) component).getPackageName() : null;
    }

    private static int userId(Object value) {
        if (!(value instanceof UserHandle)) {
            return 0;
        }
        Integer identifier = HookValues.invokeInt(value, "getIdentifier");
        return identifier == null ? 0 : identifier;
    }

    private static void installRecentTasksMemory(HookSession session) {
        Class<?> overviewActionsClass = session.findOptionalClass(
                "com.android.quickstep.views.OverviewActionsView");
        if (overviewActionsClass == null) {
            session.missing("class=com.android.quickstep.views.OverviewActionsView");
            return;
        }

        session.hook(overviewActionsClass, "onFinishInflate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof View) {
                setupRecentTasksMemoryView(session, (View) owner);
            }
            return result;
        });

        Class<?> recentsOrientedStateClass = session.findOptionalClass(
                "com.android.quickstep.util.RecentsOrientedState");
        if (recentsOrientedStateClass != null) {
            session.hook(recentsOrientedStateClass, "update", chain -> {
                Object result = chain.proceed();
                int displayRotation = HookValues.intArg(chain, 1, 0);
                Activity act = sActivityRef != null ? sActivityRef.get() : null;
                if (act != null) {
                    act.runOnUiThread(() -> updateRecentTasksOrientation(displayRotation));
                } else {
                    updateRecentTasksOrientation(displayRotation);
                }
                return result;
            }, int.class, int.class);
        }

        Class<?> activityClass = session.findOptionalClass("android.app.Activity");
        if (activityClass != null) {
            session.hook(activityClass, "onResume", chain -> {
                Object res = chain.proceed();
                Activity current = sActivityRef != null ? sActivityRef.get() : null;
                if (current != null && current.equals(chain.getThisObject())) {
                    int rotation = current.getWindowManager().getDefaultDisplay().getRotation();
                    updateRecentTasksOrientation(rotation);
                }
                return res;
            });

            session.hook(activityClass, "onDestroy", chain -> {
                Object destroyed = chain.getThisObject();
                Activity current = sActivityRef != null ? sActivityRef.get() : null;
                if (destroyed != null && destroyed.equals(current)) {
                    sMainHandler.removeCallbacks(sMemoryUpdateRunnable);
                    if (sDarkReceiver != null) {
                        try {
                            current.unregisterReceiver(sDarkReceiver);
                        } catch (Throwable ignored) {
                        }
                        sDarkReceiver = null;
                    }
                }
                return chain.proceed();
            });
        }
        session.log("LAUNCHER_RECENT_TASKS_MEMORY_INSTALLED");
    }

    private static void setupRecentTasksMemoryView(HookSession session, View actionsView) {
        Context context = actionsView.getContext();
        int removeAllId = context.getResources().getIdentifier(
                "remove_all_button_layout", "id", context.getPackageName());
        if (removeAllId == 0) {
            removeAllId = context.getResources().getIdentifier(
                    "remove_all_button_layout", "id", "com.zte.mifavor.launcher");
        }
        View removeAllView = removeAllId != 0 ? actionsView.findViewById(removeAllId) : null;
        if (!(removeAllView instanceof LinearLayout)) {
            return;
        }
        LinearLayout removeAllLayout = (LinearLayout) removeAllView;
        if (removeAllLayout.findViewWithTag("memory_text_view") != null) {
            return;
        }

        removeAllLayout.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams layoutParams = removeAllLayout.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams.width > 0 && layoutParams.height > 0) {
                removeAllLayout.setMinimumWidth(layoutParams.width);
                removeAllLayout.setMinimumHeight(layoutParams.height);
            }
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            removeAllLayout.setLayoutParams(layoutParams);
        }

        int actionButtonsId = context.getResources().getIdentifier(
                "action_buttons", "id", context.getPackageName());
        if (actionButtonsId == 0) {
            actionButtonsId = context.getResources().getIdentifier(
                    "action_buttons", "id", "com.zte.mifavor.launcher");
        }
        View actionButtonsView = actionButtonsId != 0 ? actionsView.findViewById(actionButtonsId) : null;
        if (actionButtonsView instanceof RelativeLayout) {
            sActionButtonsRef = new WeakReference<>((RelativeLayout) actionButtonsView);
        }

        TextView memoryTextView = new TextView(context);
        memoryTextView.setId(View.generateViewId());
        memoryTextView.setTag("memory_text_view");
        memoryTextView.setGravity(Gravity.CENTER);
        memoryTextView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        memoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);

        updateMemoryTextColor(context, memoryTextView);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                TextView tv = sMemoryTextViewRef != null ? sMemoryTextViewRef.get() : null;
                if (tv != null) {
                    updateMemoryTextColor(ctx, tv);
                }
            }
        };
        sDarkReceiver = receiver;
        try {
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
        } catch (Throwable ignored) {
        }

        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.topMargin = 0;
        memoryTextView.setLayoutParams(tvParams);
        removeAllLayout.addView(memoryTextView);

        sMemoryTextViewRef = new WeakReference<>(memoryTextView);

        Activity activity = findActivity(context);
        if (activity != null) {
            sActivityRef = new WeakReference<>(activity);
            sActivityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        } else {
            sActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }

        sMainHandler.removeCallbacks(sMemoryUpdateRunnable);
        sMainHandler.post(sMemoryUpdateRunnable);
    }

    private static void updateMemoryTextColor(Context context, TextView textView) {
        if (context == null || textView == null) return;
        int uiMode = context.getResources().getConfiguration().uiMode;
        boolean isDark = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        textView.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
    }

    private static void updateRecentTasksOrientation(int displayRotation) {
        TextView tv = sMemoryTextViewRef != null ? sMemoryTextViewRef.get() : null;
        RelativeLayout buttons = sActionButtonsRef != null ? sActionButtonsRef.get() : null;
        boolean isLandscape = (displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270);
        if (tv != null) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, isLandscape ? 8 : 10);
        }
        if (buttons != null) {
            setParentLayoutParams(buttons, isLandscape ? 80 : 85);
        }
    }

    private static void setParentLayoutParams(RelativeLayout layout, int heightDp) {
        if (layout == null) return;
        ViewGroup.LayoutParams lp = layout.getLayoutParams();
        if (lp == null) return;
        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, heightDp,
                layout.getContext().getResources().getDisplayMetrics());
        lp.height = heightPx;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).topMargin = 0;
        }
        layout.setLayoutParams(lp);
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
