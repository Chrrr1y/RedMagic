package dev.lackluster.redmagichelper.config;

import android.content.SharedPreferences;

/**
 * A deliberately small, process-safe view of the module configuration.
 *
 * The LSPosed side reads the remote preference file exposed by LibXposed. The
 * settings activity writes the same keys to its local preference file; the
 * original module uses this same remote/local fallback pattern.
 */
public final class ModuleConfig {
    private final SharedPreferences preferences;
    private final java.util.Map<String, Object> providerValues = new java.util.concurrent.ConcurrentHashMap<>();

    public ModuleConfig(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void updateFromProvider(android.os.Bundle bundle) {
        if (bundle == null) return;
        providerValues.clear();
        for (String key : bundle.keySet()) {
            Object v = bundle.get(key);
            if (v != null) {
                providerValues.put(key, v);
            }
        }
    }

    public boolean enabled() {
        return getBoolean("enable_module", true);
    }

    public boolean gameFeaturesEnabled() {
        return getBoolean("game_fuction_unfreeze_switch", false);
    }

    public boolean browserEnabled() {
        return getBoolean("browser_open_search", false)
                || getBoolean("browser_skip_splash", false)
                || getBoolean("browser_remove_info_tabs", false)
                || getBoolean("browser_remove_homepage_info", false)
                || getBoolean("browser_slim_mine", false)
                || getBoolean("browser_remove_hot_words", false)
                || getBoolean("browser_remove_search_shortcuts", false)
                || getBoolean("browser_remove_download_hot", false)
                || getBoolean("browser_remove_download_recommend", false)
                || getBoolean("browser_compose_settings", false)
                || getBoolean("browser_hide_switch_model", true)
                || getBoolean("browser_disable_telemetry", false);
    }

    public boolean browserDisableTelemetry() {
        return getBoolean("browser_disable_telemetry", false);
    }

    public boolean browserSkipSplash() {
        return getBoolean("browser_skip_splash", false);
    }

    public boolean browserOpenSearch() {
        return getBoolean("browser_open_search", false);
    }

    public boolean browserRemoveInfoTabs() {
        return getBoolean("browser_remove_info_tabs", false);
    }

    public boolean browserRemoveHomePageInfo() {
        return getBoolean("browser_remove_homepage_info", false);
    }

    public boolean browserSlimMine() {
        return getBoolean("browser_slim_mine", false);
    }

    public boolean browserRemoveHotWords() {
        return getBoolean("browser_remove_hot_words", false);
    }

    public boolean browserRemoveSearchShortcuts() {
        return getBoolean("browser_remove_search_shortcuts", false);
    }

    public boolean browserRemoveDownloadHot() {
        return getBoolean("browser_remove_download_hot", false);
    }

    public boolean browserRemoveDownloadRecommend() {
        return getBoolean("browser_remove_download_recommend", false);
    }

    public boolean browserComposeSettings() {
        return getBoolean("browser_compose_settings", false);
    }

    public boolean browserHideSwitchModel() {
        return getBoolean("browser_hide_switch_model", true);
    }

    public boolean neostoreSkipSplash() {
        return getBoolean("neostore_skip_splash", false);
    }

    public boolean neogamecenterSkipSplash() {
        return getBoolean("neogamecenter_skip_splash", false);
    }

    public boolean neostoreAutoSign() {
        return getBoolean("neostore_auto_sign", false);
    }

    public boolean neogameLocalSvip() {
        return getBoolean("neogame_local_svip", false);
    }

    public boolean gameSuperResolutionUnlimitedApps() {
        return getBoolean("game_super_resolution_unlimited_apps", false);
    }

    public boolean desktopRecentTaskDisplayMemory() {
        return getBoolean("system_desktop_recent_task_display_memory", false);
    }

    public boolean beautifySkipSplashAds() {
        return getBoolean("beautify_skip_splash_ads", false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (providerValues.containsKey(key)) {
            Object val = providerValues.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
            if (val instanceof String) {
                return Boolean.parseBoolean((String) val);
            }
        }
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        if (providerValues.containsKey(key)) {
            Object val = providerValues.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            if (val instanceof String) {
                try {
                    return Integer.parseInt((String) val);
                } catch (Throwable ignored) {}
            }
        }
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getInt(key, defaultValue);
        } catch (Throwable ignored) {
            // EditTextPreference and older module versions may persist the
            // same numeric setting as a String. Keep the hook side tolerant
            // without changing the stored value.
            try {
                Object value = preferences.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
            } catch (Throwable ignoredAgain) {
                // Fall through to the caller's safe default.
            }
            return defaultValue;
        }
    }

    public int getIntFlexible(String key, int defaultValue) {
        return getInt(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        if (providerValues.containsKey(key)) {
            Object val = providerValues.get(key);
            if (val instanceof Number) {
                return ((Number) val).floatValue();
            }
            if (val instanceof String) {
                try {
                    return Float.parseFloat((String) val);
                } catch (Throwable ignored) {}
            }
        }
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getFloat(key, defaultValue);
        } catch (Throwable ignored) {
            try {
                Object value = preferences.getAll().get(key);
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                if (value instanceof String) {
                    return Float.parseFloat((String) value);
                }
            } catch (Throwable ignoredAgain) {
            }
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        if (providerValues.containsKey(key)) {
            Object val = providerValues.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
            if (val instanceof String) {
                try {
                    return Long.parseLong((String) val);
                } catch (Throwable ignored) {}
            }
        }
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getLong(key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        if (providerValues.containsKey(key)) {
            Object val = providerValues.get(key);
            if (val != null) {
                return String.valueOf(val);
            }
        }
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getString(key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }
}
