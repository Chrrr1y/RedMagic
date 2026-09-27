package dev.lackluster.redmagichelper.hook.adapters;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Dual-app support hooks copied as capability-based method resolutions. */
public final class DoubleAppAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        Class<?> update = session.findClass("com.zte.cn.doubleapp.common.UpdateUtils");
        if (session.config().getBoolean("double_any_app", false)) {
            session.hook(update, "getSupportApps", chain -> {
                Object owner = chain.getThisObject();
                Object context = HookValues.readField(owner, "mContext", "context");
                if (context instanceof Context) {
                    List<String> result = new ArrayList<>();
                    for (android.content.pm.PackageInfo info
                            : ((Context) context).getPackageManager().getInstalledPackages(0)) {
                        if (info != null && info.packageName != null) {
                            result.add(info.packageName);
                        }
                    }
                    return result;
                }
            return chain.proceed();
            });
        }
        Class<?> utils = session.findClass("com.zte.cn.doubleapp.common.Utils");
        if (session.config().getBoolean("rm_low_memory_limit", false)) {
            session.hook(utils, "showLimitedApps", chain -> null, Context.class);
        }
        session.log("DOUBLE_APP_CAPABILITY_SCAN_COMPLETE");
    }
}
