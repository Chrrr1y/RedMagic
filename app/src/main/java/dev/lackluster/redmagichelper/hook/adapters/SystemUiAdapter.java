package dev.lackluster.redmagichelper.hook.adapters;

import android.text.TextUtils;
import android.view.Window;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

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
        installNotificationLimit(session);
        session.log("SYSTEMUI_CAPABILITY_SCAN_COMPLETE");
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
