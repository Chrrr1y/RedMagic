package dev.lackluster.redmagichelper.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookLogFormatterTest {
    @Test
    public void includesHostPackageAndKeepsEachEventOnOneLine() {
        assertEquals("[12:34:56][WARN][游戏优化][host=cn.nubia.gameassist]"
                        + "[CAPABILITY_MISSING method=Tile#bind() next line]", HookLogFormatter.format(
                        "12:34:56", "WARN", "游戏优化", "cn.nubia.gameassist",
                        "CAPABILITY_MISSING method=Tile#bind()\nnext line"));
    }

    @Test
    public void compactsExceptionWithUsefulStackFrames() {
        IllegalStateException error = new IllegalStateException("bad\nstate");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("pkg.Target", "run", "Target.java", 12)
        });

        String compact = HookLogFormatter.compactError(error);
        assertTrue(compact.startsWith("java.lang.IllegalStateException: bad state"));
        assertTrue(compact.contains("pkg.Target.run(Target.java:12)"));
    }
}
