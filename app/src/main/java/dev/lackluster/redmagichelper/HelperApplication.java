package dev.lackluster.redmagichelper;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * Connects the settings process to LibXposed's remote preference service.
 *
 * The settings UI edits the local {@code config} file.  LibXposed modules
 * read the corresponding remote group inside each hooked process, so the
 * values must be copied whenever the framework service becomes available.
 */
public final class HelperApplication extends Application {
    private static final String TAG = "RMH_REWRITE";
    private static final String GROUP = "config";
    private static final String FEATURE_DEFAULTS_MIGRATION = "rmh_feature_defaults_v4";
    private static final AtomicBoolean REMOTE_BIND_ERROR_REPORTED = new AtomicBoolean();
    private static final AtomicBoolean REMOTE_SYNC_ERROR_REPORTED = new AtomicBoolean();
    private static final ExecutorService REMOTE_SYNC_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "RMH-ConfigSync");
                thread.setDaemon(true);
                return thread;
            });

    private SharedPreferences localPreferences;
    private volatile SharedPreferences remotePreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (preferences, key) -> {
                if (key == null) {
                    return;
                }
                SharedPreferences remote = remotePreferences;
                if (remote != null) {
                    Object value = preferences.getAll().get(key);
                    REMOTE_SYNC_EXECUTOR.execute(() -> {
                        try {
                            if (!copyValue(remote.edit(), key, value).commit()) {
                                reportRemoteSyncError(new IllegalStateException(
                                        "Remote preference commit returned false for " + key));
                            }
                        } catch (Throwable error) {
                            reportRemoteSyncError(error);
                        }
                    });
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        localPreferences = getSharedPreferences(GROUP, MODE_PRIVATE);
        localPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        // Reading the existing config file for its one-time migration can block cold start.
        // Queue it before service synchronization so first-run defaults are still copied first.
        REMOTE_SYNC_EXECUTOR.execute(() -> migrateRequestedFeatureDefaults(localPreferences));

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                REMOTE_SYNC_EXECUTOR.execute(() -> {
                    try {
                        SharedPreferences remote = service.getRemotePreferences(GROUP);
                        remotePreferences = remote;
                        syncAll(remote);
                    } catch (Throwable error) {
                        if (BuildConfig.DEBUG && REMOTE_BIND_ERROR_REPORTED.compareAndSet(false, true)) {
                            Log.e(TAG, "remote preference bind failed", error);
                        }
                    }
                });
            }

            @Override
            public void onServiceDied(XposedService service) {
                remotePreferences = null;
            }
        });
    }

    /** Enable the capabilities explicitly requested for this device once. */
    private static void migrateRequestedFeatureDefaults(SharedPreferences preferences) {
        if (preferences.getBoolean(FEATURE_DEFAULTS_MIGRATION, false)) {
            return;
        }
        preferences.edit()
                .putBoolean("enable_module", true)
                .putBoolean("game_fuction_unfreeze_switch", false)
                .putBoolean("fan_unlock_max", false)
                .putBoolean("fan_fixed_enabled", false)
                .putBoolean("ai_trigger_switch", false)
                .putBoolean("double_any_app", false)
                .putBoolean("rm_low_memory_limit", false)
                .putBoolean("neostore_download_enabled", false)
                .putBoolean("hide_mtp_category_browse", false)
                .putBoolean("hide_store_install_prompt", false)
                .putBoolean("skip_pkg_scan", false)
                .putBoolean("cts_test_installer", false)
                .putBoolean("disable_usb_installation_and_switch_account_verification", false)
                .putBoolean("automatic_screen_off", false)
                .putBoolean("time_picker_period", false)
                .putBoolean("remove_restrictions_window", false)
                .putBoolean("remove_restrictions_window_number", false)
                .putBoolean("mute_nfc_sound", false)
                .putBoolean("nfc_allow_screen_off_recognition", false)
                .putBoolean(FEATURE_DEFAULTS_MIGRATION, true)
                .apply();
    }

    private void syncAll(SharedPreferences remote) {
        SharedPreferences.Editor editor = remote.edit();
        for (Map.Entry<String, ?> entry : localPreferences.getAll().entrySet()) {
            copyValue(editor, entry.getKey(), entry.getValue());
        }
        if (!editor.commit()) {
            reportRemoteSyncError(new IllegalStateException("Initial remote preference sync failed"));
        }
    }

    /** Waits for queued preference writes before restarting the affected host processes. */
    public void awaitPendingConfigSync() {
        if (remotePreferences == null) {
            return;
        }
        CountDownLatch completed = new CountDownLatch(1);
        try {
            REMOTE_SYNC_EXECUTOR.execute(completed::countDown);
            completed.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException error) {
            reportRemoteSyncError(error);
        }
    }

    private static void reportRemoteSyncError(Throwable error) {
        if (BuildConfig.DEBUG && REMOTE_SYNC_ERROR_REPORTED.compareAndSet(false, true)) {
            Log.e(TAG, "remote preference sync failed", error);
        }
    }

    private static SharedPreferences.Editor copyValue(
            SharedPreferences.Editor editor,
            String key,
            Object value
    ) {
        if (value instanceof Boolean) {
            return editor.putBoolean(key, (Boolean) value);
        }
        if (value instanceof Integer) {
            return editor.putInt(key, (Integer) value);
        }
        if (value instanceof Long) {
            return editor.putLong(key, (Long) value);
        }
        if (value instanceof Float) {
            return editor.putFloat(key, (Float) value);
        }
        if (value instanceof String) {
            return editor.putString(key, (String) value);
        }
        if (value instanceof Set<?>) {
            @SuppressWarnings("unchecked")
            Set<String> strings = (Set<String>) value;
            return editor.putStringSet(key, strings);
        }
        if (value == null) {
            return editor.remove(key);
        }
        return editor.putString(key, value.toString());
    }
}
