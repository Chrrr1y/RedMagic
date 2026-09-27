package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

public class ClockPolicyTest {
    @Test
    public void mapsSevenPeriodsAtBoundaries() {
        assertEquals(0, ClockPolicy.periodIndex(0));
        assertEquals(0, ClockPolicy.periodIndex(5));
        assertEquals(1, ClockPolicy.periodIndex(6));
        assertEquals(2, ClockPolicy.periodIndex(9));
        assertEquals(3, ClockPolicy.periodIndex(12));
        assertEquals(4, ClockPolicy.periodIndex(13));
        assertEquals(5, ClockPolicy.periodIndex(18));
        assertEquals(6, ClockPolicy.periodIndex(19));
        assertEquals(21, ClockPolicy.representativeHour(6));
    }

    @Test
    public void handlesLocaleAndSecondsWithoutDuplicatingThem() {
        assertTrue(ClockPolicy.isChinese(Locale.SIMPLIFIED_CHINESE));
        assertFalse(ClockPolicy.isChinese(Locale.US));
        assertEquals("HH:mm:ss", ClockPolicy.withSeconds("HH:mm", true));
        assertEquals("h:mm:ss a", ClockPolicy.withSeconds("h:mm a", false));
        assertEquals("HH:mm:ss", ClockPolicy.withSeconds("HH:mm:ss", true));
        assertEquals("下午 12:30", ClockPolicy.withPeriodPrefix("上午 12:30", 15));
    }
}
