package dev.lackluster.redmagichelper.hook.adapters;

import android.os.Bundle;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** Online theme/AOD trial hooks from the Beautify host. */
public final class BeautifyAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        if (session.config().beautifySkipSplashAds()) {
            installSplashBypass(session);
        }
        if (!session.config().getBoolean("cancel_trial_login", true)) {
            session.log("cancel_trial_login 开关未启用");
            return;
        }

        // 1. ZTE 账号登录绕过：使得 getCurrentUserInfo() 恒定返回非空有效账号，绕过登录弹窗
        Class<?> zteAccountInfoClass = session.findOptionalClass(
                "com.zte.beautify.model.remote.zteaccount.ZteAccountInfo");
        Class<?> zteAccountManager = session.findOptionalClass(
                "com.zte.beautify.model.remote.zteaccount.ZteAccountManager");
        if (zteAccountManager != null) {
            session.hook(zteAccountManager, "getCurrentUserInfo", chain -> {
                if (zteAccountInfoClass != null) {
                    try {
                        Object info = zteAccountInfoClass.getDeclaredConstructor().newInstance();
                        HookValues.writeField(info, "RedMagicUser", "userName");
                        HookValues.writeField(info, "token_rmh", "token");
                        return info;
                    } catch (Throwable ignored) {
                    }
                }
                return chain.proceed();
            });
            session.hook(zteAccountManager, "isLogined", chain -> Boolean.TRUE);
            session.log("ZteAccountManager 登录状态劫持已就绪 (isLogined -> true)");
        }

        // 2. Bean 资源数据模型：全部放行为免费、已购买、限免状态
        Class<?> bean = session.findOptionalClass("com.zte.beautify.model.local.data.Bean");
        if (bean != null) {
            session.hook(bean, "getIsCharge", chain -> Boolean.FALSE);
            session.hook(bean, "getIsPay", chain -> Integer.valueOf(1));
            session.hook(bean, "isFree", chain -> Boolean.TRUE);
            session.hook(bean, "isOnlineFreeTheme", chain -> Boolean.TRUE);
            session.hook(bean, "isTimeLimitFree", chain -> Boolean.TRUE);
            session.hook(bean, "isChargedStaticWallpaper", chain -> Boolean.FALSE);
            session.log("Bean 资源收费状态劫持已就绪 (isFree/isPay/isTimeLimitFree -> 放行)");
        }

        // 3. BeautyPreviewActivity：返回支付状态为已支付 (1)，阻断登录跳转，重定向试用为正常应用
        Class<?> beautyPreview = session.findOptionalClass(
                "com.zte.beautify.view.common.preview.BeautyPreviewActivity");
        if (beautyPreview != null) {
            session.hook(beautyPreview, "getPayStateOfBean", chain -> Integer.valueOf(1));
            session.hook(beautyPreview, "startAccountManager", chain -> null);
            session.hook(beautyPreview, "setUserClickTry", chain -> HookValues.proceedWithArgument(chain, 0, false), boolean.class);
            session.log("BeautyPreviewActivity 试用与支付状态拦截已就绪");
        }

        // 4. BaseSetWallPaperManager：应用主题时强制 isTrial=false，实现永久生效
        Class<?> wpManager = session.findOptionalClass(
                "com.zte.beautify.view.base.BaseSetWallPaperManager");
        if (wpManager != null && bean != null) {
            session.hook(wpManager, "setThemeAsync", chain -> HookValues.proceedWithArgument(chain, 3, false),
                    android.content.Context.class, bean, boolean.class, boolean.class);
            session.log("BaseSetWallPaperManager setThemeAsync 永久应用拦截已就绪");
        }

        // 5. BeautifyAdapterHelper：阻断系统级试用计时器
        Class<?> adapterHelper = session.findOptionalClass(
                "com.zte.beautify.model.remote.utils.BeautifyAdapterHelper");
        if (adapterHelper != null) {
            session.hook(adapterHelper, "startTrial", chain -> null, int.class);
            session.log("BeautifyAdapterHelper startTrial 计时器阻断已就绪");
        }

        // 6. TryUseJobHelper：阻断倒计时重置与试用状态标记
        Class<?> tryHelper = session.findOptionalClass(
                "com.zte.beautify.view.common.preview.tryuse.TryUseJobHelper");
        if (tryHelper != null) {
            session.hook(tryHelper, "isTringFont", chain -> Boolean.FALSE);
            session.hook(tryHelper, "isTringLiveWallpaper", chain -> Boolean.FALSE);
            session.hook(tryHelper, "isTringTheme", chain -> Boolean.FALSE);
            session.hook(tryHelper, "setTringFont", chain -> null, boolean.class);
            session.hook(tryHelper, "setTringLiveWallpaper", chain -> null, boolean.class);
            session.hook(tryHelper, "setTringTheme", chain -> null, boolean.class);
            session.hook(tryHelper, "ReceiverResetMessage", chain -> null, int.class, String.class);
            session.log("TryUseJobHelper 试用重置与状态标记阻断已就绪");
        }

        // 7. PaymentManager：阻断支付调用
        Class<?> paymentManager = session.findOptionalClass(
                "com.zte.beautify.model.remote.payment.PaymentManager");
        if (paymentManager != null && bean != null) {
            session.hook(paymentManager, "startPayment", chain -> null, bean, int.class, android.app.Activity.class);
            session.log("PaymentManager 支付调用阻断已就绪");
        }

        session.log("桌面功能主题资源免登录及付费试用绕过 Hook 注入完成");
    }

    private static void installSplashBypass(HookSession session) {
        Class<?> logoActivity = session.findOptionalClass(
                "com.zte.beautify.view.beauty.splashscreen.LogoActivity");
        if (logoActivity != null) {
            session.hook(logoActivity, "onCreate", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                boolean permissionReady = HookValues.writeField(owner, true, "hasPermission");
                boolean progressStopped = HookValues.writeField(owner, false, "mShowProgressFlag");
                Object launched = HookValues.invoke(owner, "onClick", (Object) null);
                session.log("启动广告绕过: permission=" + permissionReady
                        + " progress=" + progressStopped
                        + " launched=" + (launched == null));
                return result;
            }, Bundle.class);
        }
    }
}
