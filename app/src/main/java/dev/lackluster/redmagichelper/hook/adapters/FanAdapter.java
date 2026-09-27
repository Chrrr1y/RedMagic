package dev.lackluster.redmagichelper.hook.adapters;

import java.io.File;
import java.lang.reflect.Method;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/**
 * RedMagic fan adapter. There is intentionally no hardware-model allowlist.
 * The host exposes the OEM controller and node utility or it does not.
 */
public final class FanAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        Class<?> controller = session.findClass("cn.nubia.fan.policy.FanControllerImpl");
        Class<?> utils = session.findClass("cn.nubia.fan.util.Utils");
        Method readNode = session.firstMethod(utils, "readNodeValue", String.class);
        Method setNode = session.firstMethod(utils, "setNodeValue", String.class, String.class);
        if (controller == null || utils == null || readNode == null || setNode == null) {
            session.log("FAN_CAPABILITY_MISSING; OEM API not available in this host APK");
            return;
        }

        session.log("FAN_API_FOUND controller=" + controller.getName());
        boolean unlockMax = session.config().getBoolean("fan_unlock_max", false);
        boolean fixedEnabled = session.config().getBoolean("fan_fixed_enabled", false);

        if (unlockMax || fixedEnabled) {
            session.hook(controller, "setFanEnable", chain -> {
                Object result = chain.proceed();
                if (!chain.getArgs().isEmpty() && Boolean.TRUE.equals(chain.getArgs().get(0))) {
                    if (unlockMax) {
                        invokeOptional(chain.getThisObject(), controller, "fanMaxSpeed");
                    }
                    if (fixedEnabled) {
                        applyFixedLevel(session, setNode);
                    }
                }
                return result;
            }, boolean.class);
        }

        if (fixedEnabled || unlockMax) {
            session.hook(controller, "start", chain -> {
                Object result = chain.proceed();
                if (fixedEnabled) {
                    applyFixedLevel(session, setNode);
                    session.log("FAN_START_OBSERVED");
                }
                return result;
            });
            session.hook(controller, "notifyCubeFan", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (unlockMax && HookValues.readBooleanField(owner, false,
                        "mCurrentFanEnable", "mLastFanEnable")) {
                    invokeOptional(owner, controller, "fanMaxSpeed");
                }
                if (fixedEnabled) {
                    applyFixedLevel(session, setNode);
                }
                return result;
            });
        }
        if (fixedEnabled || unlockMax) {
            session.hook(controller, "fanFullSpeed", chain -> {
                Object result = chain.proceed();
                if (unlockMax) {
                    // The OEM state machine can choose fanFullSpeed() again
                    // after setFanEnable() selected the max lock.
                    invokeOptional(chain.getThisObject(), controller, "fanMaxSpeed");
                }
                if (fixedEnabled) {
                    applyFixedLevel(session, setNode);
                }
                return result;
            });
        }
        if (fixedEnabled) {
            // notifyCubeFan() delegates to the performance lock and can run
            // after setFanEnable(). Reapply at both private speed transitions
            // so the OEM state machine cannot immediately overwrite the
            // configured node value.
            session.hook(controller, "fanMaxSpeed", chain -> {
                Object result = chain.proceed();
                applyFixedLevel(session, setNode);
                return result;
            });
        }
        session.log("FAN_CAPABILITY_SCAN_COMPLETE");
    }

    private static void invokeOptional(Object owner, Class<?> type, String methodName) {
        if (owner == null) {
            return;
        }
        try {
            Method method = type.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(owner);
        } catch (Throwable ignored) {
            // The max-speed method is optional across fan firmware revisions.
        }
    }

    private static void applyFixedLevel(HookSession session, Method setNode) {
        int level = HookValues.clamp(
                session.config().getInt("fan_fixed_level", 5), 1, 5);
        try {
            String node = selectSpeedNode();
            if (node == null) {
                session.log("FAN_FIXED_LEVEL_FAILED no writable speed node");
                return;
            }
            setNode.invoke(null, node, String.valueOf(level));
            session.log("FAN_FIXED_LEVEL_APPLIED level=" + level + " node=" + node);
        } catch (Throwable error) {
            session.log("FAN_FIXED_LEVEL_FAILED " + error.getClass().getSimpleName());
        }
    }

    private static String selectSpeedNode() {
        // The OEM constants intentionally omit the leading slash because
        // they are passed through GameAssistControllerWrapper. Keep the
        // same values here: Utils.setNodeValue() delegates to that wrapper,
        // which resolves the node in the system service. Adding '/' here
        // would make the path differ from the verified OEM API contract.
        String[] candidates = {
                "sys/devices/platform/zte_pwm_gpios/zte_pwm_freq",
                "sys/kernel/fan/fan_speed_level",
                "sys/class/leds/fan/fan_speed_level"
        };
        for (String candidate : candidates) {
            try {
                // The OEM wrapper accepts the historical relative-looking
                // node name, but File.exists() runs in this app process and
                // therefore needs the absolute sysfs path. Return the OEM
                // spelling after checking the real filesystem path.
                if (new File("/" + candidate).exists()) {
                    return candidate;
                }
            } catch (Throwable ignored) {
                // Continue with the next firmware-specific node.
            }
        }
        return null;
    }
}
