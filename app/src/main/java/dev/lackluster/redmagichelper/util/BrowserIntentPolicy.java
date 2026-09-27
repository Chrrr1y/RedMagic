package dev.lackluster.redmagichelper.util;

/** Pure decision rule for the browser's launcher-only redirect. */
public final class BrowserIntentPolicy {
    private BrowserIntentPolicy() {
    }

    public static boolean isPlainLauncher(
            String action,
            boolean hasLauncherCategory,
            boolean hasData,
            boolean hasClipData,
            boolean hasExtras
    ) {
        if (hasData || hasClipData) {
            return false;
        }
        // Explicit component launches (for example a launcher shortcut or
        // `am start -n`) can omit the MAIN/LAUNCHER fields. They still mean
        // “open the browser” when no URL or payload is present.
        if (action == null || action.isEmpty()) {
            return !hasExtras;
        }
        // MAIN without a URL/payload is also a plain browser launch when the
        // launcher (or an explicit component start) omits CATEGORY_LAUNCHER.
        // Requiring the category here caused the browser to keep the news
        // page for one frame, and skipped the bottom-search transition.
        return "android.intent.action.MAIN".equals(action);
    }
}
