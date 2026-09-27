package dev.lackluster.redmagichelper.hook.adapters;

import android.os.Handler;
import android.os.Message;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/**
 * Rewrites only delays that belong to the known PluginTrigger handler classes.
 * This keeps the adaptation in code rather than loosening a device/version gate.
 */
public final class GamePluginTriggerAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (!session.config().getBoolean("ai_trigger_switch", false)) {
            session.log("AI_TRIGGER_DISABLED");
            return;
        }
        session.hook(Handler.class, "sendEmptyMessageDelayed", chain -> {
            long delay = longArg(chain, 1);
            int what = HookValues.intArg(chain, 0, Integer.MIN_VALUE);
            long adjusted = adjustDelay(chain.getThisObject(), what, delay, session);
            return chain.proceed(new Object[]{chain.getArgs().get(0), adjusted});
        }, int.class, long.class);
        session.hook(Handler.class, "sendMessageDelayed", chain -> {
            long delay = longArg(chain, 1);
            Object message = HookValues.arg(chain, 0);
            int what = message instanceof Message ? ((Message) message).what : Integer.MIN_VALUE;
            long adjusted = adjustDelay(chain.getThisObject(), what, delay, session);
            return chain.proceed(new Object[]{chain.getArgs().get(0), adjusted});
        }, Message.class, long.class);

        Class<?> policy = session.findClass("com.zte.game.plugintrigger.policy.PolicyManager");
        session.hook(policy, "getLegalRangeValue", chain -> {
            Object result = chain.proceed();
            if (result instanceof Long && chain.getArgs().size() >= 3 && chain.getArgs().get(2) instanceof Long) {
                long requested = (Long) chain.getArgs().get(2);
                long cooldown = Math.max(50L, Math.min(30000L,
                        session.config().getInt("ai_trigger_cooldown_ms", 180)));
                return Math.min((Long) result, Math.min(requested, cooldown));
            }
            return result;
        }, long.class, long.class, long.class);
        session.log("AI_TRIGGER_CAPABILITY_SCAN_COMPLETE");
    }

    private static long longArg(io.github.libxposed.api.XposedInterface.Chain chain, int index) {
        Object value = chain.getArgs().size() > index ? chain.getArgs().get(index) : null;
        return value instanceof Long ? (Long) value : -1L;
    }

    private static long adjustDelay(Object owner, int what, long delay, HookSession session) {
        if (delay <= 0 || owner == null) {
            return delay;
        }
        String name = owner.getClass().getName();
        int cooldown = HookValues.clamp(
                session.config().getInt("ai_trigger_cooldown_ms", 180), 50, 30000);
        int click = HookValues.clamp(
                session.config().getInt("ai_trigger_click_ms", 25), 10, 500);
        int yolo = HookValues.clamp(
                session.config().getInt("ai_trigger_yolo_scan_ms", 400), 50, 2000);
        if (name.contains("Yolo") || name.contains("Detection") || name.contains("Scan")) {
            return Math.min(delay, yolo);
        }
        if (name.equals("com.zte.game.plugintrigger.service.PluginTriggerService$WorkHandler")
                && delay == 2000) {
            int template = HookValues.clamp(
                    session.config().getInt("ai_trigger_template_scan_ms", 180), 80, 2000);
            return Math.min(delay, template);
        }
        if (name.equals("com.zte.game.plugintrigger.plugins.click.TouchScreenPlugin$WorkHandler")) {
            if (what == 102 && delay == 50) {
                return Math.min(delay, click);
            }
            if (what == 103 && delay == 450) {
                return Math.max(click + 25L, click * 2L);
            }
            if (what == 104 && delay == 500) {
                return Math.max(click + 50L, click * 3L);
            }
            return delay;
        }
        if (name.equals("com.zte.game.plugintrigger.policy.PolicyManager$PolicyIntervalHandler")
                && delay >= 2000 && delay < 30001) {
            return Math.min(delay, cooldown);
        }
        if ((name.equals("com.zte.game.plugintrigger.service.PluginsController$2")
                || name.equals("com.zte.game.plugintrigger.service.PluginsController$MyHandler"))) {
            if (what == 1011 && delay >= 2000 && delay < 30001) {
                return Math.min(delay, cooldown);
            }
            if (what == 1012 || what == 1013) {
                return Math.min(delay, yolo);
            }
        }
        return delay;
    }
}
