package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.ClockPolicy;
import dev.lackluster.redmagichelper.util.HookLog;

/** Settings-page compatibility hooks for the extracted MFV build. */
public final class SettingsAdapter implements HostAdapter {
    private static final ThreadLocal<Boolean> TIME_PICKER_GUARD =
            ThreadLocal.withInitial(() -> false);
    private static final int[] PERIOD_HOURS = {2, 7, 10, 12, 15, 18, 21};

    @Override
    public void install(HookSession session) {
        if (session.config().getBoolean("disable_attery_percentage_display_setting_option", false)) {
            Class<?> controller = session.findClass(
                    "com.zte.settings.notification.MfvBatteryPercentagePreferenceController");
            session.hook(controller, "isAvailable", chain -> Boolean.FALSE);
        }

        if (session.config().getBoolean(
                "disable_usb_installation_and_switch_account_verification", false)) {
            installAdbInstallBypass(session);
        }

        if (session.config().getBoolean("automatic_screen_off", false)) {
            installScreenTimeoutPolicy(session);
        }

        if (session.config().getBoolean("time_picker_period", false)) {
            installTimePickerPeriod(session);
        }

        if (session.config().getBoolean("disable_system_update", false)) {
            Class<?> updateController = session.findOptionalClass(
                    "com.android.settings.system.SystemUpdatePreferenceController",
                    "com.zte.settings.system.SystemUpdatePreferenceController",
                    "com.zte.settings.system.MfvSystemUpdatePreferenceController"
            );
            if (updateController != null) {
                session.hook(updateController, "isAvailable", chain -> Boolean.FALSE);
            }
        }
        session.log("SETTINGS_CAPABILITY_SCAN_COMPLETE");
    }

    private static void installAdbInstallBypass(HookSession session) {
        Class<?> adb = session.findClass(
                "com.zte.settings.development.EnableAdbInstallPreferenceController");
        if (adb == null) {
            return;
        }

        // Force the USB installation setting to always be available and visible
        session.hook(adb, "isAvailable", chain -> Boolean.TRUE);
        session.hook(adb, "getAvailabilityStatus", chain -> 0);

        // Intercept preference switch clicks and state updates
        Class<?> prefClass = session.findClass("androidx.preference.Preference");
        if (prefClass != null) {
            session.hook(adb, "updateState", chain -> {
                chain.proceed();
                List<Object> args = chain.getArgs();
                if (args != null && !args.isEmpty()) {
                    Object pref = args.get(0);
                    if (pref != null) {
                        try {
                            Method setEnabledMethod = pref.getClass().getMethod("setEnabled", boolean.class);
                            setEnabledMethod.invoke(pref, true);
                        } catch (Throwable ignored) {}
                    }
                }
                return null;
            }, prefClass);

            session.hook(adb, "handlePreferenceTreeClick", chain -> {
                Object pref = chain.getArgs().get(0);
                if (pref != null) {
                    Object key = HookValues.invokeNoArg(pref, "getKey");
                    if ("enable_adb_install".equals(key)) {
                        Object controller = chain.getThisObject();
                        Object switchPref = HookValues.readField(controller, "mEnableAdbInstall");
                        if (switchPref == null) {
                            switchPref = pref;
                        }
                        Context context = (Context) HookValues.readField(controller, "mContext");
                        if (context == null && switchPref != null) {
                            context = (Context) HookValues.invokeNoArg(switchPref, "getContext");
                        }
                        int currentVal = 0;
                        if (context != null) {
                            try {
                                currentVal = android.provider.Settings.System.getInt(
                                        context.getContentResolver(), "adb_install_enabled", 0);
                            } catch (Throwable ignored) {}
                        }
                        boolean enable = (currentVal == 0);
                        applyAdbInstallState(session, controller, switchPref, enable);
                        return Boolean.TRUE;
                    }
                }
                return chain.proceed();
            }, prefClass);
        }

        // Direct bypass fallbacks for internal invocations
        session.hook(adb, "launchAuthCompActivity", chain -> {
            applyAdbInstallState(session, chain.getThisObject(), null, true);
            return null;
        });
        session.hook(adb, "launchNubiaActivity", chain -> {
            applyAdbInstallState(session, chain.getThisObject(), null, true);
            return null;
        });
        session.hook(adb, "onActivityResult", chain -> {
            applyAdbInstallState(session, chain.getThisObject(), null, true);
            return Boolean.TRUE;
        }, int.class, int.class, Intent.class);
    }

    private static void applyAdbInstallState(
            HookSession session,
            Object controller,
            Object switchPref,
            boolean enable
    ) {
        if (controller == null) {
            return;
        }
        try {
            Context context = (Context) HookValues.readField(controller, "mContext");
            if (switchPref == null) {
                switchPref = HookValues.readField(controller, "mEnableAdbInstall");
            }
            if (context == null && switchPref != null) {
                context = (Context) HookValues.invokeNoArg(switchPref, "getContext");
            }

            if (context != null) {
                android.provider.Settings.System.putInt(
                        context.getContentResolver(),
                        "adb_install_enabled",
                        enable ? 1 : 0
                );
            }

            if (switchPref != null) {
                try {
                    Method setCheckedMethod = switchPref.getClass().getMethod("setChecked", boolean.class);
                    setCheckedMethod.invoke(switchPref, enable);
                } catch (Throwable ignored) {}
            }

            boolean isPMRequest = false;
            Object pmVal = HookValues.readField(controller, "isPMRequest");
            if (pmVal instanceof Boolean) {
                isPMRequest = (Boolean) pmVal;
            }
            if (isPMRequest) {
                HookValues.writeField(controller, true, "isSend");
                try {
                    Method sendMethod = controller.getClass().getDeclaredMethod("sendResultToPackageManager", boolean.class);
                    sendMethod.setAccessible(true);
                    sendMethod.invoke(controller, enable);
                } catch (Throwable ignored) {}
            }

            if (context != null) {
                try {
                    Intent intent = new Intent("zte.mfv.settings.ADB_INSTALL_ENABLE_CHANGED");
                    intent.putExtra("adb_install_enable", enable);
                    context.sendBroadcast(intent);
                } catch (Throwable ignored) {}
            }

            session.log("ADB_INSTALL_APPLIED enable=" + enable);
        } catch (Throwable t) {
            HookLog.error(session.packageName(), "APPLY_ADB_INSTALL_FAILED", t);
        }
    }

    private static void installScreenTimeoutPolicy(HookSession session) {
        Class<?> controller = session.findClass(
                "com.android.settings.display.ScreenTimeoutPreferenceController");
        // The OEM method writes screen_off_timeout=60000 before returning its
        // summary. Returning a safe empty summary prevents that side effect.
        session.hook(controller, "setDefaultTimeValue", chain -> {
            session.log("SETTINGS_SCREEN_TIMEOUT_DEFAULT_WRITE_BLOCKED");
            return "";
        }, long.class);

        Class<?> adapt = session.findClass("com.zte.adapt.display.ScreenTimeoutSettingsAdapt");
        session.hook(adapt, "getInitialValuesLength", chain -> {
            Object[] args = chain.getArgs().toArray();
            if (args.length > 1 && args[1] instanceof CharSequence[]) {
                return ((CharSequence[]) args[1]).length;
            }
            return chain.proceed();
        }, int.class, CharSequence[].class);
    }

    private static void installTimePickerPeriod(HookSession session) {
        Class<?> dialog = session.findClass("com.zte.mifavor.widget.TimePickerDialogZTE");
        Class<?> picker = session.findClass("com.zte.mifavor.widget.TimePickerZTE");
        Class<?> numberPicker = session.findClass("com.zte.mifavor.widget.NumberPickerZTE");

        session.hook(dialog, "updateTitle", chain -> {
            Object result = chain.proceed();
            updateDialogTitle(session, chain);
            return result;
        }, picker, int.class, int.class);

        session.hook(numberPicker, "setDisplayedValues", chain -> {
            Object[] args = chain.getArgs().toArray();
            if (args.length > 0 && args[0] instanceof String[]
                    && ((String[]) args[0]).length == 2
                    && isLikelyAmPmSpinner(chain.getThisObject())) {
                args[0] = ClockPolicy.periodLabels();
                return chain.proceed(args);
            }
            return chain.proceed();
        }, String[].class);

        session.hook(numberPicker, "setMaxValue", chain -> {
            if (isLikelyAmPmSpinner(chain.getThisObject())
                    && HookValues.intArg(chain, 0, -1) <= 1) {
                return HookValues.proceedWithArgument(chain, 0, 6);
            }
            return chain.proceed();
        }, int.class);

        session.hook(picker, "updateAmPmControl", chain -> {
            Object result = chain.proceed();
            applyPeriodSpinner(session, chain.getThisObject());
            return result;
        });
        session.hook(picker, "onTimeChanged", chain -> {
            Object result = chain.proceed();
            applyPeriodSpinner(session, chain.getThisObject());
            return result;
        });

        // $5 is the confirmed AM/PM listener in the extracted build. $2/$3
        // are retained as narrow fallbacks for vendor rebuilds; the callback
        // compares the listener's NumberPicker with mAmPmSpinner so an hour
        // or minute listener can never be mistaken for the period spinner.
        Class<?> periodListener = session.findClass(
                "com.zte.mifavor.widget.TimePickerZTE$5",
                "com.zte.mifavor.widget.TimePickerZTE$2",
                "com.zte.mifavor.widget.TimePickerZTE$3");
        session.hook(periodListener, "onValueChange", chain -> {
            Object result = chain.proceed();
            handlePeriodSelection(session, chain);
            return result;
        }, numberPicker, int.class, int.class);
    }

    private static void updateDialogTitle(
            HookSession session,
            io.github.libxposed.api.XposedInterface.Chain chain
    ) {
        Object dialog = chain.getThisObject();
        if (HookValues.readBooleanField(dialog, false, "mIsNeedCustomSubTitle")) {
            return;
        }
        Object picker = HookValues.arg(chain, 0);
        int hour = HookValues.intArg(chain, 1, -1);
        int minute = HookValues.intArg(chain, 2, -1);
        if (hour < 0 || hour > 23) {
            hour = HookValues.invokeInt(picker, "getHour", "getCurrentHour") == null
                    ? -1 : HookValues.invokeInt(picker, "getHour", "getCurrentHour");
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return;
        }

        Object subtitle = HookValues.readField(dialog, "tvSubTitle");
        if (!(subtitle instanceof TextView)) {
            session.missing("field=com.zte.mifavor.widget.TimePickerDialogZTE#tvSubTitle");
            return;
        }
        boolean is24Hour = HookValues.readBooleanField(dialog, false, "mIs24HourView");
        int displayedHour = is24Hour ? hour : hour % 12;
        if (displayedHour == 0) {
            displayedHour = 12;
        }
        String prefix = HookValues.readField(dialog, "mPrefixSubTitle") instanceof String
                ? (String) HookValues.readField(dialog, "mPrefixSubTitle") : "";
        String label = ClockPolicy.periodLabel(hour);
        String title = prefix + (label == null ? "" : label + " ")
                + twoDigits(displayedHour) + ":" + twoDigits(minute);
        ((TextView) subtitle).setText(title);
        applyPeriodSpinner(session, picker);
    }

    private static void applyPeriodSpinner(HookSession session, Object picker) {
        if (picker == null || Boolean.TRUE.equals(TIME_PICKER_GUARD.get())
                || HookValues.readBooleanField(picker, false, "mIs24HourView")) {
            return;
        }
        Object spinner = HookValues.readField(picker, "mAmPmSpinner");
        Integer hour = HookValues.invokeInt(picker, "getHour", "getCurrentHour");
        if (spinner == null || hour == null) {
            return;
        }
        int period = ClockPolicy.periodIndex(hour);
        if (period < 0) {
            return;
        }
        TIME_PICKER_GUARD.set(true);
        try {
            HookValues.invoke(spinner, "setMinValue", 0);
            HookValues.invoke(spinner, "setMaxValue", 6);
            HookValues.invoke(spinner, "setDisplayedValues",
                    (Object) ClockPolicy.periodLabels());
            HookValues.invoke(spinner, "setValue", period);
        } finally {
            TIME_PICKER_GUARD.set(false);
        }
    }

    private static void handlePeriodSelection(
            HookSession session,
            io.github.libxposed.api.XposedInterface.Chain chain
    ) {
        if (Boolean.TRUE.equals(TIME_PICKER_GUARD.get())) {
            return;
        }
        Object listener = chain.getThisObject();
        Object picker = HookValues.readField(listener, "this$0");
        Object changedSpinner = HookValues.arg(chain, 0);
        Object amPmSpinner = HookValues.readField(picker, "mAmPmSpinner");
        if (picker == null || changedSpinner == null
                || (amPmSpinner != null && changedSpinner != amPmSpinner)) {
            return;
        }
        int selected = HookValues.intArg(chain, 2, -1);
        int hour = ClockPolicy.representativeHour(selected);
        if (hour < 0) {
            return;
        }
        TIME_PICKER_GUARD.set(true);
        try {
            if (HookValues.invoke(picker, "setHour", hour) == null) {
                HookValues.invoke(picker, "setCurrentHour", Integer.valueOf(hour));
            }
        } finally {
            TIME_PICKER_GUARD.set(false);
        }
        applyPeriodSpinner(session, picker);
    }

    private static boolean isLikelyAmPmSpinner(Object spinner) {
        if (spinner == null) {
            return false;
        }
        if (!HookValues.stackContains("TimePickerZTE")) {
            return false;
        }
        // During construction the mAmPmSpinner field may not be assigned yet;
        // the OEM AM/PM picker is the only NumberPicker receiving max=1 and a
        // two-entry displayed-values array from this call stack.
        return true;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
