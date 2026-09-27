package dev.lackluster.redmagichelper.hook.adapters;

import android.os.Handler;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** GameLab detector scheduling adapter. */
public final class GameLabAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (!session.config().getBoolean("ai_trigger_switch", false)) {
            session.log("GAME_LAB_AI_TRIGGER_DISABLED");
            return;
        }
        session.hook(Handler.class, "postDelayed", chain -> {
            Object runnable = HookValues.arg(chain, 0);
            long delay = HookValues.longArg(chain, 1, -1L);
            if (delay > 0 && isDetectorRunnable(runnable)) {
                long configured = HookValues.clamp(
                        session.config().getInt("ai_trigger_template_scan_ms", 180), 80, 2000);
                if (configured < delay) {
                    return HookValues.proceedWithArgument(
                            chain, 1, Math.max(80L, configured));
                }
            }
            return chain.proceed();
        }, Runnable.class, long.class);

        for (String name : new String[]{
                "cn.nubia.gamelab.toy.SGameToy",
                "cn.nubia.gamelab.toy.CommonToy"}) {
            Class<?> toy = session.findClass(name);
            session.hook(toy, "scheduleDetectRunnable", chain -> {
                Object handler = HookValues.readField(chain.getThisObject(), "mHandler");
                Object runnable = HookValues.readField(chain.getThisObject(), "mDetectRunnable");
                Object delayValue = HookValues.readField(chain.getThisObject(), "MSG_DELAY_TIME");
                if (handler instanceof Handler && runnable instanceof Runnable) {
                    long delay = delayValue instanceof Number
                            ? ((Number) delayValue).longValue() : 2000L;
                    long configured = HookValues.clamp(
                            session.config().getInt("ai_trigger_template_scan_ms", 180), 80, 2000);
                    Handler target = (Handler) handler;
                    target.removeCallbacks((Runnable) runnable);
                    target.postDelayed((Runnable) runnable, Math.min(delay, configured));
                    return null;
                }
                return chain.proceed();
            });
            session.hook(toy, "startDetectBitmap", chain -> chain.proceed());
        }
        session.log("GAME_LAB_CAPABILITY_SCAN_COMPLETE");
    }

    private static boolean isDetectorRunnable(Object runnable) {
        if (runnable == null) {
            return false;
        }
        String name = runnable.getClass().getName();
        return name.equals("cn.nubia.gamelab.toy.SGameToy$1")
                || name.equals("cn.nubia.gamelab.toy.CommonToy$2");
    }
}
