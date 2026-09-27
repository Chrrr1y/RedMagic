package dev.lackluster.redmagichelper.hook.adapters;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Third-party launcher compatibility for ZTE's permission controller. */
public final class PermissionControllerAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (!session.config().getBoolean("allow_thirdparty_launcher", false)) {
            session.log("THIRDPARTY_LAUNCHER_DISABLED");
            return;
        }

        // The extracted API 36 build contains this stable, obfuscated helper.
        // Keep the package-qualified alternatives for vendor rebuilds where R8
        // mapping is disabled.
        Class<?> utils = session.findClass(
                "K2.D",
                "com.android.permissioncontroller.permission.utils.PermissionUtils",
                "com.android.permissioncontroller.permission.utils.Utils");
        session.hook(utils, "m", chain -> Boolean.TRUE, String.class);
        session.log("PERMISSION_CONTROLLER_CAPABILITY_SCAN_COMPLETE");
    }
}
