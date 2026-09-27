package dev.lackluster.redmagichelper.hook.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BrowserAdapterStaticInvokeTest {
    @Test
    public void invokesStaticFactoriesAndPrimitiveParameterMethods() throws Exception {
        assertEquals("account-manager", BrowserAdapter.invokeStatic(Target.class, "getInstance"));
        assertEquals(17, BrowserAdapter.invokeStatic(Target.class, "resolveColor", 17));
        assertNull(BrowserAdapter.invokeStatic(Target.class, "instanceMethod"));
    }

    private static final class Target {
        public static String getInstance() {
            return "account-manager";
        }

        public static int resolveColor(int resourceId) {
            return resourceId;
        }

        public String instanceMethod() {
            return "not-static";
        }
    }
}
