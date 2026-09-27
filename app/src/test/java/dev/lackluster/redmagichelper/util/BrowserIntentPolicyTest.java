package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrowserIntentPolicyTest {
    @Test
    public void onlyPlainLauncherIntentIsRedirected() {
        assertTrue(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.MAIN", true, false, false, false));
        assertFalse(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.VIEW", true, false, false, false));
        assertFalse(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.MAIN", true, true, false, false));
        assertFalse(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.MAIN", true, false, true, false));
        assertTrue(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.MAIN", true, false, false, true));
        assertFalse(BrowserIntentPolicy.isPlainLauncher(
                "android.intent.action.VIEW", false, false, false, false));
        assertTrue(BrowserIntentPolicy.isPlainLauncher(
                null, false, false, false, false));
        assertFalse(BrowserIntentPolicy.isPlainLauncher(
                null, false, false, false, true));
    }
}
