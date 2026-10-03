package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GfrcCapabilityPolicyTest {
    @Test
    public void expandsEachPackedLimitIndependentlyToUltra() {
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(0));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(1));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(10));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(11));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(20));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(21));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(12));
        assertEquals(22, GfrcCapabilityPolicy.includeUltraModes(22));
        assertEquals(32, GfrcCapabilityPolicy.includeUltraModes(31));
    }

    @Test
    public void remapsServicePackingForGameAssistController() {
        assertEquals(22, GfrcCapabilityPolicy.forGameAssistController(0));
        assertEquals(23, GfrcCapabilityPolicy.forGameAssistController(31));
        assertEquals(32, GfrcCapabilityPolicy.forGameAssistController(13));
        assertEquals(22, GfrcCapabilityPolicy.forGameAssistController(21));
    }
}
