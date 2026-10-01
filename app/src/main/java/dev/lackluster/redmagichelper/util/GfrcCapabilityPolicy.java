package dev.lackluster.redmagichelper.util;

/** Expands packed decimal quality/frame-rate limits to include both exposed enhancement tiers. */
public final class GfrcCapabilityPolicy {
    private static final int HIGHEST_EXPOSED_MODE = 2;

    private GfrcCapabilityPolicy() {
    }

    public static int includeUltraModes(int capability) {
        if (capability <= 0) {
            return 22;
        }
        int imageQualityLimit = Math.max(capability / 10, HIGHEST_EXPOSED_MODE);
        int frameRateLimit = Math.max(capability % 10, HIGHEST_EXPOSED_MODE);
        return imageQualityLimit * 10 + frameRateLimit;
    }
}
