package dev.lackluster.redmagichelper.util;

/** Corrects the SM8750 Cube fallback for the GFRC Ultra input-frame limit. */
public final class GfrcInputFramePolicy {
    private static final String TARGET_SOC_MODEL = "SM8750";
    private static final int INPUT_FRAME_LIMIT_DATA_ID = 0x1918;
    private static final int ULTRA_INTERPOLATION_RATE = 2;
    private static final int SM8750_FALLBACK_FPS = 60;
    private static final int ULTRA_144HZ_INPUT_FPS = 48;

    private GfrcInputFramePolicy() {
    }

    public static int adjustUltraInputLimit(
            String socModel,
            int dataId,
            int originalFps,
            int gfrcEnabled,
            int interpolationRate
    ) {
        if (!TARGET_SOC_MODEL.equals(socModel)
                || dataId != INPUT_FRAME_LIMIT_DATA_ID
                || originalFps != SM8750_FALLBACK_FPS
                || gfrcEnabled != 1
                || interpolationRate != ULTRA_INTERPOLATION_RATE) {
            return originalFps;
        }
        return ULTRA_144HZ_INPUT_FPS;
    }
}
