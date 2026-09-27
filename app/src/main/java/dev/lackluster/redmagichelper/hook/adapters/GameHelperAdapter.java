package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.Settings;

import java.util.Collections;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Macro/recording compatibility for cn.nubia.gamehelpmodule. */
public final class GameHelperAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        boolean comboEnabled = session.config().getBoolean(
                "game_space_combo_speed_enabled", false);
        if (!comboEnabled) {
            session.log("GAME_HELPER_FEATURES_DISABLED");
            return;
        }

        Class<?> packageUtils = session.findClass("cn.nubia.gamehelper.utils.PackageUtils");
        session.hook(packageUtils, "isBlackPackageName", chain -> Boolean.FALSE, String.class);

        session.hook(packageUtils, "getBlackPackageSet",
                chain -> Collections.emptySet());
        Class<?> db = session.findClass("cn.nubia.gamehelper.db.DbHelper");
        Class<?> recordDb = session.findClass("cn.nubia.gamehelper.db.RecordMotionDatabase");
        // Both database implementations return true when a package is
        // absent from the blacklist. Returning false here was the exact
        // opposite of the unlock policy and made King of Glory show the
        // macro UI while refusing to execute it.
        session.hook(db, "isPackageCanUseMacro", chain -> Boolean.TRUE, String.class);
        session.hook(recordDb, "isPackageCanUseMacro", chain -> Boolean.TRUE, String.class);
        session.hook(db, "macroCanUse", chain -> Boolean.TRUE);

        Class<?> manager = session.findClass(
                "cn.nubia.gamehelper.manager.GameMotionHelperManager");
        session.hook(manager, "canUseMacro", chain -> Boolean.TRUE);
        session.hook(manager, "notifyChange", chain -> chain.proceed(), String.class);
        session.hook(manager, "recoveryMotion", chain -> chain.proceed(), String.class);

        Class<?> touchProvider = session.findClass(
                "cn.nubia.gamehelper.db.GameTouchProvider");
        session.hook(touchProvider, "query", chain -> {
            Object uri = HookValues.arg(chain, 0);
            Object result = chain.proceed();
            if (uri instanceof android.net.Uri
                    && "blacklist".equals(((android.net.Uri) uri).getLastPathSegment())
                    && result instanceof Cursor) {
                Cursor cursor = (Cursor) result;
                MatrixCursor empty = new MatrixCursor(cursor.getColumnNames());
                try {
                    cursor.close();
                } catch (Throwable ignored) {
                }
                return empty;
            }
            return result;
        });
        session.hook(touchProvider, "call", chain -> {
            Object bundle = HookValues.arg(chain, 2);
            Object adjusted = adjustBundle(bundle, session);
            Object[] args = chain.getArgs().toArray();
            if (args.length > 2) {
                args[2] = adjusted;
            }
            return chain.proceed(args);
        }, String.class, String.class, android.os.Bundle.class);
        session.hook(touchProvider, "recordMotionCall", chain ->
                adjustBundle(chain.proceed(), session));

        Class<?> deviceUtil = session.findClass("cn.nubia.gamehelper.utils.DeviceUtil");
        session.hook(deviceUtil, "isUI85", chain -> Boolean.TRUE);
        Class<?> motionBean = session.findClass("cn.nubia.gamehelper.bean.RecordMotionBean");
        int speed = HookValues.clamp(
                session.config().getInt("game_space_combo_speed_rate", 1), 1, 10);
        session.hook(motionBean, "getSpeed", chain -> speed);
        session.hook(Settings.class, "canDrawOverlays", chain -> {
            Object context = HookValues.arg(chain, 0);
            if (context instanceof Context
                    && "cn.nubia.gamehelpmodule".equals(((Context) context).getPackageName())) {
                return Boolean.TRUE;
            }
            return chain.proceed();
        }, Context.class);
        session.log("GAME_HELPER_CAPABILITY_SCAN_COMPLETE");
    }

    private static Object adjustBundle(Object value, HookSession session) {
        if (!(value instanceof android.os.Bundle)
                || !session.config().getBoolean("game_space_combo_speed_enabled", false)) {
            return value;
        }
        try {
            android.os.Bundle source = (android.os.Bundle) value;
            if (source.getBoolean("rmh_combo_speed_adjusted", false)
                    || !source.getBoolean("startRecoverRecord", false)
                    || !source.containsKey("motionRecoverFd")
                    || !source.containsKey("recoveryTime")) {
                return value;
            }

            long recoveryTime = source.getLong("recoveryTime", 0L);
            float oldRecoverRate = source.getFloat("recoverRate", 1.0f);
            int configuredSpeed = HookValues.clamp(
                    session.config().getInt("game_space_combo_speed_rate", 1), 1, 10);
            float targetRecoverRate = configuredSpeed;
            if (recoveryTime <= 0L
                    || !Float.isFinite(oldRecoverRate)
                    || oldRecoverRate <= 0.0f
                    || !Float.isFinite(targetRecoverRate)
                    || targetRecoverRate <= 0.0f) {
                return value;
            }

            long adjustedRecoveryTime = scaleRecoveryTime(
                    recoveryTime, oldRecoverRate, targetRecoverRate);
            if (adjustedRecoveryTime <= 0L) {
                return value;
            }

            android.os.Bundle result = new android.os.Bundle(source);
            result.putLong("recoveryTime", adjustedRecoveryTime);
            result.putFloat("recoverRate", targetRecoverRate);
            result.putBoolean("rmh_combo_speed_adjusted", true);
            return result;
        } catch (Throwable ignored) {
            // A malformed OEM Bundle must not break the provider call path.
            return value;
        }
    }

    /** Matches the original module's ceil(time * oldRate / targetRate) rule. */
    private static long scaleRecoveryTime(
            long recoveryTime, float oldRecoverRate, float targetRecoverRate) {
        double scaled = ((double) recoveryTime * (double) oldRecoverRate)
                / (double) targetRecoverRate;
        if (!Double.isFinite(scaled) || scaled >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) Math.ceil(scaled));
    }
}
