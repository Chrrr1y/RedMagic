package dev.lackluster.redmagichelper.util;

/** Expands the packed OEM GFRC limits to include all exposed quality and frame-rate modes. */
public final class GfrcCapabilityPolicy {
    private static final int ULTRA_MODE = 2;

    private GfrcCapabilityPolicy() {
    }

    public static int includeUltraModes(int capability) {
        if (capability <= 0) {
            return 22;
        }
        int interpolationLimit = Math.max(capability / 10, ULTRA_MODE);
        int imageQualityLimit = Math.max(capability % 10, ULTRA_MODE);
        return interpolationLimit * 10 + imageQualityLimit;
    }
}
