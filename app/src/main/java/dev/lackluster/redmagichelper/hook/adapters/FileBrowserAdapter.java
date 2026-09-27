package dev.lackluster.redmagichelper.hook.adapters;

import java.lang.reflect.Field;


import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** MTP category and storage-name hooks for Nubia FileBrowser. */
public final class FileBrowserAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {

        if (session.config().getBoolean("hide_mtp_category_browse", false)) {
            // MtpDatabase.addEmulatedStorage() creates Nubia's virtual "分类浏览" (Category Browse)
            // storage in MTP. Hooking it to return null removes the virtual category root from MTP
            // without disturbing real storage volumes (handled in addStorage).
            Class<?> database = session.findOptionalClass(
                    "cn.nubia.filebrowser.mtpserver.mtp.MtpDatabase");
            if (database != null) {
                session.hook(database, "addEmulatedStorage", chain -> null);
                session.log("MTP_CATEGORY_BROWSE_SUPPRESSED_DATABASE");
            }
            Class<?> manager = session.findOptionalClass(
                    "cn.nubia.filebrowser.mtpserver.NubiaMtpManager");
            Class<?> storageType = session.findOptionalClass(
                    "cn.nubia.filebrowser.mtpserver.mtp.MtpStorage");
            if (manager != null && storageType != null) {
                session.hook(manager, "startCreateCategoryDentryTree", chain -> null,
                        storageType);
                session.log("MTP_CATEGORY_BROWSE_DISABLED_MANAGER");
            }
        }

        if (session.config().getBoolean("mtp_rename_root_name_switch", false)) {
            Class<?> storage = session.findClass(
                    "cn.nubia.filebrowser.mtpserver.mtp.MtpStorage");
            Class<?> server = session.findClass(
                    "cn.nubia.filebrowser.mtpserver.mtp.MtpServer");
            Field storageId = HookValues.findField(storage, "mStorageId");
            Field description = HookValues.findField(storage, "mDescription");
            if (storage != null && server != null && storageId != null && description != null) {
                session.hook(server, "addStorage", chain -> {
                    Object value = HookValues.arg(chain, 0);
                    if (storage.isInstance(value)) {
                        try {
                            if (storageId.getInt(value) <= 65537) {
                                String name = session.config().getString(
                                        "mtp_rename_root_name", "内部存储设备");
                                description.set(value, name);
                            }
                        } catch (Throwable error) {
                            session.log("MTP_RENAME_FAILED " + error.getClass().getSimpleName());
                        }
                    }
                    return chain.proceed();
                }, storage);
            } else {
                session.log("MTP_RENAME_CAPABILITY_MISSING");
            }
        }
        session.log("FILE_BROWSER_CAPABILITY_SCAN_COMPLETE");
    }
}
