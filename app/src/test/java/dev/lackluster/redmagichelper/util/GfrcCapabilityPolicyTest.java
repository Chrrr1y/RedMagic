package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GfrcCapabilityPolicyTest {
    @Test
    public void expandsEachPackedLimitIndependentlyToUltra() {
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(0));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(11));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(21));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(12));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(22));
        assertEquals(32, GfrcCapabilityPolicy.includeUltraModes(31));
    }
}
