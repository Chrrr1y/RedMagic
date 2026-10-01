package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GfrcInputFramePolicyTest {
    @Test
    public void lowersOnlyEnabledUltraFallbackToFortyEightFps() {
        assertEquals(48, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1918, 60, 1, 2));
    }

    @Test
    public void preservesHighModeAndUnrelatedLimits() {
        assertEquals(60, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1918, 60, 1, 1));
        assertEquals(60, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1918, 60, 0, 2));
        assertEquals(60, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1917, 60, 1, 2));
        assertEquals(48, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1918, 48, 1, 2));
        assertEquals(90, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8750", 0x1918, 90, 1, 2));
    }

    @Test
    public void leavesOtherSocFallbackUntouched() {
        assertEquals(60, GfrcInputFramePolicy.adjustUltraInputLimit(
                "SM8850", 0x1918, 60, 1, 2));
    }
}
