package dev.lackluster.redmagichelper.util;

/** Normalizes the packed OEM GFRC capability limits used by the game stack. */
public final class GfrcCapabilityPolicy {
    private static final int HIGHEST_EXPOSED_MODE = 2;

    private GfrcCapabilityPolicy() {
    }

    public static int includeUltraModes(int capability) {
        if (capability <= 0) {
            return 22;
        }
        int interpolationLimit = Math.max(capability / 10, HIGHEST_EXPOSED_MODE);
        int imageQualityLimit = Math.max(capability % 10, HIGHEST_EXPOSED_MODE);
        return interpolationLimit * 10 + imageQualityLimit;
    }

    /**
     * The service packs interpolation in the tens digit and image quality in
     * the ones digit. GameAssist's controller parses those digits in the
     * opposite order when it clamps its imageQuality/frameRate selections.
     */
    public static int forGameAssistController(int capability) {
        int normalized = includeUltraModes(capability);
        int interpolationLimit = normalized / 10;
        int imageQualityLimit = normalized % 10;
        return imageQualityLimit * 10 + interpolationLimit;
    }
}
