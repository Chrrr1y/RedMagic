package dev.lackluster.redmagichelper.hook.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import dev.lackluster.redmagichelper.hook.HookSession;

/** Filters status toasts emitted by the game-assist super-resolution controls. */
final class SuperResolutionToastPolicy {
    private SuperResolutionToastPolicy() {
    }

    static void install(HookSession session, boolean enabled) {
        if (!enabled) {
            return;
        }

        session.hook(Toast.class, "show", chain -> {
            Object toast = chain.getThisObject();
            String message = readToastText(toast);
            if (isSuperResolutionMessage(message)
                    || HookValues.stackContains(
                    "SuperResolutionViewController", "SuperResolutionHelper")) {
                session.log("GAME_SUPER_RESOLUTION_TOAST_SUPPRESSED message="
                        + summarize(message));
                return null;
            }
            return chain.proceed();
        });
        session.log("GAME_SUPER_RESOLUTION_TOAST_SUPPRESSION_ENABLED");
    }

    private static boolean isSuperResolutionMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("超境")
                || normalized.contains("超分辨率")
                || normalized.contains("超分")
                || normalized.contains("super resolution")
                || normalized.contains("super_resolution")
                || normalized.contains("过热")
                || normalized.contains("温度过高")
                || normalized.contains("高温")
                || normalized.contains("发热")
                || normalized.contains("overheat")
                || normalized.contains("thermal");
    }

    private static String readToastText(Object toast) {
        Object text = HookValues.invokeNoArg(toast, "getText");
        if (text instanceof CharSequence) {
            return text.toString();
        }
        text = HookValues.readField(toast, "mText", "text");
        if (text instanceof CharSequence) {
            return text.toString();
        }

        Object view = HookValues.invokeNoArg(toast, "getView");
        StringBuilder result = new StringBuilder();
        appendViewText(view, result, 0);
        return result.length() == 0 ? null : result.toString();
    }

    private static void appendViewText(Object value, StringBuilder result, int depth) {
        if (!(value instanceof View) || depth > 8) {
            return;
        }
        View view = (View) value;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(text);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                appendViewText(group.getChildAt(index), result, depth + 1);
            }
        }
    }

    private static String summarize(String message) {
        if (message == null || message.isEmpty()) {
            return "<matched-source>";
        }
        String singleLine = message.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() <= 80 ? singleLine : singleLine.substring(0, 80);
    }
}
