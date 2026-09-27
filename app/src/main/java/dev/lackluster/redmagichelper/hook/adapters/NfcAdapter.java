package dev.lackluster.redmagichelper.hook.adapters;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** NFC hooks with no dependency on an NFC implementation class version. */
public final class NfcAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        Class<?> nfc = session.findClass("com.android.nfc.NfcService");
        if (session.config().getBoolean("nfc_allow_screen_off_recognition", false)) {
            session.hook(nfc, "sendMessage", chain -> {
                // The vendor service uses message types 2/4 for the information
                // screen.  The original hook changes only that argument.
                if (chain.getArgs().size() > 1) {
                    Object type = chain.getArgs().get(1);
                    if (Integer.valueOf(2).equals(type) || Integer.valueOf(4).equals(type)) {
                        return HookValues.proceedWithArgument(chain, 1, 8);
                    }
                }
                return chain.proceed();
            });
        }
        if (session.config().getBoolean("mute_nfc_sound", false)) {
            session.hook(nfc, "playSound", chain -> null, int.class);
        }
        session.log("NFC_CAPABILITY_SCAN_COMPLETE");
    }
}
