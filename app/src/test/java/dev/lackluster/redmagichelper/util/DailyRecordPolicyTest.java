package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DailyRecordPolicyTest {
    @Test
    public void scalesMapValuesAndMarksItForDeduplication() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("steps", 100);
        source.put("distance", 20L);
        source.put("calories", 1.5d);

        Map<String, Object> adjusted = DailyRecordPolicy.scaleMap(source, 3);
        assertEquals(300, adjusted.get("steps"));
        assertEquals(60L, adjusted.get("distance"));
        assertEquals(4.5d, (Double) adjusted.get("calories"), 0.0001d);
        assertEquals(Boolean.TRUE, adjusted.get(DailyRecordPolicy.ADJUSTED_MARKER));
        assertNull(DailyRecordPolicy.scaleMap(adjusted, 3));
    }

    @Test
    public void scalesListOnlyWhenARecordChanged() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("steps", 12);
        List<Object> source = new ArrayList<>();
        source.add(record);
        source.add("unsupported");

        List<Object> adjusted = DailyRecordPolicy.scaleRecords(source, 2);
        assertEquals(24, ((Map<?, ?>) adjusted.get(0)).get("steps"));
        assertEquals("unsupported", adjusted.get(1));
        assertNull(DailyRecordPolicy.scaleRecords(adjusted, 0));
    }

    @Test
    public void protectsIntegerAndLongOverflow() {
        assertEquals(Integer.MAX_VALUE,
                DailyRecordPolicy.scaleNumber(Integer.MAX_VALUE, 2));
        assertEquals(Long.MAX_VALUE,
                DailyRecordPolicy.scaleNumber(Long.MAX_VALUE, 2));
        assertNull(DailyRecordPolicy.scaleMap(
                java.util.Collections.singletonMap("other", 1), 2));
    }

    public static final class Bean {
        public int steps;
        public long distance;
        public double calories;

        Bean(int steps, long distance, double calories) {
            this.steps = steps;
            this.distance = distance;
            this.calories = calories;
        }
    }

    @Test
    public void adjustsBeanAndDoesNotDoubleTheSameInstance() {
        Bean bean = new Bean(10, 20L, 2.0d);
        assertEquals(bean, DailyRecordPolicy.scaleObject(bean, 2));
        assertEquals(20, bean.steps);
        assertEquals(40L, bean.distance);
        assertNull(DailyRecordPolicy.scaleObject(bean, 2));
    }
}
