package dev.lackluster.redmagichelper.hook.adapters;

import android.widget.TextClock;

import java.util.Calendar;
import java.util.Locale;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.ClockPolicy;

/** Weather widget adapter for the confirmed widget_am_pm TextClock. */
public final class WeatherAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        session.hook(TextClock.class, "onTimeChanged", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (!(owner instanceof TextClock)) {
                return result;
            }
            TextClock clock = (TextClock) owner;
            String resourceName = HookValues.resourceEntryName(clock);
            Locale locale;
            try {
                locale = clock.getResources().getConfiguration().getLocales().get(0);
            } catch (Throwable ignored) {
                locale = Locale.getDefault();
            }
            if (!ClockPolicy.matchesResourceName(resourceName, "widget_am_pm")
                    || !ClockPolicy.isChinese(locale)) {
                return result;
            }
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String label = ClockPolicy.periodLabel(hour);
            if (label != null) {
                HookValues.runOnMain(clock, () -> clock.setText(label));
            }
            return result;
        });
        session.log("WEATHER_CAPABILITY_SCAN_COMPLETE");
    }
}
