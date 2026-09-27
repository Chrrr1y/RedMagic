package dev.lackluster.redmagichelper.hook.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.List;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;

/** NeoStore & NeoGameCenter download queue and ad removal adaptation. */
public final class NeoStoreAdapter implements HostAdapter {
    @Override
    public void install(HookSession session) {
        boolean gameCenter = "cn.nubia.neogamecenter".equals(session.packageName());
        if (session.config().getBoolean("neostore_download_enabled", false)) {
            installDownloadQueueOptimization(session);
        }

        boolean skipSplash = gameCenter
                ? session.config().neogamecenterSkipSplash()
                : session.config().neostoreSkipSplash();
        if (skipSplash) {
            installSplashBypass(session);
        }

        boolean removeAds = gameCenter
                ? session.config().neogamecenterRemoveRecommendAds()
                : session.config().neostoreRemoveRecommendAds();
        if (removeAds) {
            installAdRemoval(session);
            if (gameCenter) {
                installGameCenterAdRemoval(session);
            }
        }

        if ("cn.nubia.neostore".equals(session.packageName())
                && session.config().neostoreAutoSign()) {
            installAutoSign(session);
        }

        if ("cn.nubia.neogamecenter".equals(session.packageName())
                && session.config().neogameLocalSvip()) {
            installLocalSvip(session);
        }

        session.log("NEOSTORE_ADAPTER_INSTALLED package=" + session.packageName());
    }

    /** Use NeoStore's own sign-status request and its own record-sign API. */
    private static void installAutoSign(HookSession session) {
        Class<?> callback = session.findClass("cn.nubia.neostore.model.w2$d");
        session.hook(callback, "onSuccess", chain -> {
            Object result = chain.proceed();
            Object response = HookValues.arg(chain, 0);
            Integer status = HookValues.invokeInt(response, "a");
            if (status == null) {
                session.missing("method=cn.nubia.neostore.model.w2$d response#a()");
            } else if (status == 0) {
                requestNeoStoreSign(session);
            } else if (status == 1) {
                session.log("NEOSTORE_AUTO_SIGN_ALREADY_DONE");
            }
            return result;
        }, Object.class, String.class);

        Class<?> home = session.findClass("cn.nubia.neostore.ui.main.HomeActivity");
        final Class<?> homeType = home;
        session.hook(home, "onResume", chain -> {
            Object result = chain.proceed();
            if (homeType != null && homeType.isInstance(chain.getThisObject())) {
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> requestNeoStoreSignStatus(session), 1200L);
            }
            return result;
        });
        // HomeActivity.A0() is the real page-entry bootstrap on the shipped
        // build. onResume() is not guaranteed to run again when returning
        // from the sign-in dialog, so query after both lifecycle paths.
        session.hook(home, "A0", chain -> {
            Object result = chain.proceed();
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> requestNeoStoreSignStatus(session), 300L);
            return result;
        });
        session.log("NEOSTORE_AUTO_SIGN_HOOKED");
    }

    private static void requestNeoStoreSignStatus(HookSession session) {
        try {
            Class<?> model = session.findClass("cn.nubia.neostore.model.w2");
            Field singleton = HookValues.findField(model, "n");
            Object instance = singleton == null ? null : singleton.get(null);
            if (instance == null) {
                session.missing("field=cn.nubia.neostore.model.w2#n");
                return;
            }
            Method getStatus = HookValues.findNoArgMethod(instance, "j");
            if (getStatus == null) {
                session.missing("method=cn.nubia.neostore.model.w2#j()");
                return;
            }
            getStatus.invoke(instance);
            session.log("NEOSTORE_AUTO_SIGN_STATUS_REQUESTED");
        } catch (Throwable error) {
            session.log("NEOSTORE_AUTO_SIGN_STATUS_FAILED "
                    + error.getClass().getSimpleName());
        }
    }

    private static void requestNeoStoreSign(HookSession session) {
        try {
            Class<?> model = session.findClass("cn.nubia.neostore.model.w2");
            Field singleton = HookValues.findField(model, "n");
            Object instance = singleton == null ? null : singleton.get(null);
            Method sign = HookValues.findMethod(instance, "k", String.class);
            if (sign == null) {
                session.missing("method=cn.nubia.neostore.model.w2#k(String)");
                return;
            }
            // w2.k() delegates to the record-sign endpoint.  The shipped
            // NeoStore build uses this exact event name; request_sign is
            // only the UI/request log label and does not record the sign-in.
            sign.invoke(instance, "request_record_sign");
            session.log("NEOSTORE_AUTO_SIGN_REQUESTED");
        } catch (Throwable error) {
            session.log("NEOSTORE_AUTO_SIGN_FAILED "
                    + error.getClass().getSimpleName());
        }
    }

    /** Keep NeoGameCenter's local status model in the joined/active state. */
    private static void installLocalSvip(HookSession session) {
        Class<?> manager = session.findClass("t0.a");
        Class<?> statusType = session.findClass("t0.d");
        Class<?> cardType = session.findClass("t0.f");
        Object localStatus = newInstance(statusType);
        Object localCard = newInstance(cardType);

        if (localStatus != null) {
            // t0.d.f is the server membership status.  The shipped model's
            // own predicates are: 0 = not joined, 2 = expired, 1 = joined.
            // 3 is an intermediate/unknown value and is not the joined state
            // (t0.a.m() ultimately checks d.n(), which is f == 1).
            HookValues.invoke(localStatus, "u", 1);
            // Fill the fields consumed by UserCenterFragment.updateSvipView().
            // Leaving the JSON model's end_date/logo fields null produces the
            // reported “null 到期” text even when the status predicate is
            // joined.
            HookValues.invoke(localStatus, "p", "2099-12-31");
            HookValues.invoke(localStatus, "q", "");
            HookValues.invoke(localStatus, "r", 0);
            HookValues.invoke(localStatus, "s", 0.0d);
            HookValues.invoke(localStatus, "t", System.currentTimeMillis());
            HookValues.invoke(localStatus, "v", 0.0d);
            HookValues.invoke(localStatus, "o", 0);
            session.hook(manager, "i", chain -> localStatus);
            session.hook(statusType, "u", chain ->
                    HookValues.proceedWithArgument(chain, 0, 1), int.class);
            session.hook(statusType, "g", chain -> 1);
            // Keep the predicates consistent with t0/d.smali: l() is the
            // explicit expired branch used by UserCenterFragment, so it must
            // never be forced true for a local joined account.
            session.hook(statusType, "j", chain -> Boolean.TRUE);
            session.hook(statusType, "k", chain -> Boolean.FALSE);
            session.hook(statusType, "l", chain -> Boolean.FALSE);
            session.hook(statusType, "m", chain -> Boolean.FALSE);
            session.hook(statusType, "n", chain -> Boolean.TRUE);
        }

        // UserCenterPresenter does not only consume t0.d directly. It also
        // asks the singleton manager for k()/l()/m(); those methods read the
        // manager's private server object and therefore bypass the i() return
        // hook above. Force the manager's final state decision as well.
        session.hook(manager, "k", chain -> Boolean.FALSE);
        session.hook(manager, "l", chain -> Boolean.FALSE);
        session.hook(manager, "m", chain -> Boolean.TRUE);

        if (statusType != null) {
            // a()/d() are benefit-count/over-date fields, not the membership
            // status. Keep them valid for the joined display model.
            session.hook(statusType, "a", chain -> 0);
            session.hook(statusType, "d", chain -> 0);
            session.hook(statusType, "g", chain -> 1);
        }

        if (localCard != null) {
            HookValues.invoke(localCard, "z", true);
            HookValues.invoke(localCard, "y", 999999.0d);
            session.hook(manager, "f", chain -> {
                chain.proceed();
                return localCard;
            });
        }
        if (cardType != null) {
            // r() checks the card's positive balance and s() checks its active
            // flag. Both are used by the card UI before network data is ready.
            session.hook(cardType, "r", chain -> Boolean.TRUE);
            session.hook(cardType, "s", chain -> Boolean.TRUE);
        }

        // The actual Game Center user-center presenter is the abstract
        // j1.g base class (the log tag is UserCenterPresenter). Its callback
        // passes the server object directly to UserCenterFragment, bypassing
        // t0.a.i(). Replace that UI input with the same local joined model.
        Class<?> gameUserCenterPresenter = session.findOptionalClass("j1.g");
        if (gameUserCenterPresenter != null) {
            session.hook(gameUserCenterPresenter, "onShowMySvipSuccess", chain -> {
                Object view = HookValues.readField(chain.getThisObject(), "b");
                if (view != null) {
                    HookValues.invoke(view, "updateSvipView", localStatus);
                    session.log("NEOGAME_SVIP_JOINED_VIEW_FORCED");
                }
                return null;
            }, statusType);
        }
        // UserCenterActivity itself implements the UI interface but its
        // updateSvipView() is an empty shell; the visible status is rendered
        // by the activity's TextViews. Force the displayed membership label
        // and date after the presenter callback so the page cannot retain the
        // server's null/expired text.
        Class<?> userCenterActivity = session.findOptionalClass(
                "cn.nubia.neostore.ui.usercenter.UserCenterActivity");
        if (userCenterActivity != null) {
            session.hook(userCenterActivity, "onResume", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                View decor = owner instanceof android.app.Activity
                        ? ((android.app.Activity) owner).getWindow().getDecorView() : null;
                View vipStatus = HookValues.findView(decor, session.packageName(), "tv_vip_status");
                if (vipStatus instanceof android.widget.TextView) {
                    ((android.widget.TextView) vipStatus).setText("超级会员 · 有效期至 2099-12-31");
                }
                session.log("NEOGAME_SVIP_ACTIVITY_STATUS_FORCED");
                return result;
            });
        }
        // The presenter updates tv_vip_status after onResume, replacing the
        // local text with “消费1元即可成为VIP”. Intercept only this concrete
        // resource so the asynchronous refresh cannot regress the visible
        // local-membership state.
        session.hook(TextView.class, "setText", chain -> {
            Object owner = chain.getThisObject();
            if (owner instanceof View
                    && "tv_vip_status".equals(HookValues.resourceEntryName((View) owner))) {
                return HookValues.proceedWithArgument(chain, 0,
                        "超级会员 · 有效期至 2099-12-31");
            }
            return chain.proceed();
        }, CharSequence.class);
        session.hook(TextView.class, "setText", chain -> {
            Object owner = chain.getThisObject();
            if (owner instanceof View
                    && "tv_vip_status".equals(HookValues.resourceEntryName((View) owner))) {
                return HookValues.proceedWithArgument(chain, 0,
                        "超级会员 · 有效期至 2099-12-31");
            }
            return chain.proceed();
        }, int.class);
        session.log("NEOGAME_LOCAL_SVIP_ENABLED");
    }

    private static Object newInstance(Class<?> type) {
        if (type == null) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void installDownloadQueueOptimization(HookSession session) {
        int requested = HookValues.clamp(
                session.config().getInt("neostore_download_count", 5), 1, 32);
        boolean gameCenter = "cn.nubia.neogamecenter".equals(session.packageName());
        boolean installed = false;

        if (gameCenter) {
            // NeoGameCenter ships the same downloader under its obfuscated
            // n0.* package.  h()/t0() are the real thread-count/mode accessors
            // used by DownloadService; n0.j0 itself is abstract, so prefer the
            // concrete singleton class and let HookSession walk its parent.
            // The downloaded NeoGameCenter build keeps the thread-count
            // accessors on n0.j0. n0.t0 is a separate singleton in this
            // release, so it must remain a fallback rather than the first
            // candidate.
            Class<?> config = session.findClass("n0.j0", "n0.t0");
            if (config != null) {
                session.hook(config, "h", chain -> requested);
                session.hook(config, "t0", chain -> requested);
                installed = true;
            }
            Class<?> queue = session.findClass("o4.j");
            if (queue != null) {
                session.hook(queue, "f", chain -> {
                    if (!chain.getArgs().isEmpty()) {
                        return HookValues.proceedWithArgument(chain, 0, requested);
                    }
                    return chain.proceed();
                }, int.class);
                installed = true;
            }
        } else {
            Class<?> config = session.findClass("cn.nubia.neostore.model.ConfigMgrImp");
            if (config != null) {
                // H0() is the server-provided string setting in the shipped
                // build. Keep its return type intact while changing the
                // configured value; E0/K0 are the runtime integer and
                // thermal adjustment paths.
                session.hook(config, "H0", chain -> String.valueOf(requested));
                // E0() reads the server's multi_download_thread_num value and
                // is consumed when the segmented downloader is initialized.
                session.hook(config, "E0", chain -> requested);
                // K0() is the runtime adjustment path used by temperature and
                // network changes. Keep the pool at the configured size.
                session.hook(config, "K0", chain -> requested);
                installed = true;
            }
            Class<?> downloadService = session.findClass(
                    "cn.nubia.neostore.service.DownloadService");
            if (downloadService != null) {
                session.hook(downloadService, "f", chain -> {
                    if (!chain.getArgs().isEmpty()) {
                        return HookValues.proceedWithArgument(chain, 0, requested);
                    }
                    return chain.proceed();
                }, int.class);
                installed = true;
            }
            Class<?> queue = session.findClass("o5.o");
            if (queue != null) {
                session.hook(queue, "e", chain -> {
                    if (!chain.getArgs().isEmpty()) {
                        return HookValues.proceedWithArgument(chain, 0, requested);
                    }
                    return chain.proceed();
                }, int.class);
                installed = true;
            }
        }

        if (installed) {
            session.log("NEOSTORE_DOWNLOAD_QUEUE_HOOKED count=" + requested
                    + " target=" + session.packageName());
        } else {
            session.missing("download-thread-capability");
        }
    }

    private static void installSplashBypass(HookSession session) {
        if ("cn.nubia.neogamecenter".equals(session.packageName())) {
            installGameCenterSplashBypass(session);
            return;
        }
        // Do not finish the OEM SplashActivity from onCreate. Both NeoStore
        // and NeoGameCenter initialize their HomeActivity from the splash
        // callback; forcing that callback while the fragment is still being
        // created is what produces the reported black window. Let the OEM
        // state machine close itself and only report an ad as unavailable.
        Class<?> gromoreSplash = session.findClass("com.alex.AlexGromoreSplashAdapter");
        if (gromoreSplash != null) {
            session.hook(gromoreSplash, "isAdReady", chain -> Boolean.FALSE);
        }

        // RedMagicOS 11's shipped store/game-center path does not use the
        // Alex adapter for the launch ad. SplashFragment.g0() creates the
        // SSP SplashAd, attaches its view, and starts the impression request
        // directly. Complete the OEM no-data path instead of creating that
        // ad view; G() schedules the normal onCloseSplash transition.
        Class<?> splashFragment = session.findOptionalClass(
                "cn.nubia.neostore.ui.start.SplashFragment");
        Class<?> splashModel = session.findOptionalClass("g2.d", "s2.c");
        if (splashFragment != null && splashModel != null) {
            session.hookAny(splashFragment, new String[]{"g0", "H0"}, chain -> {
                // g0/G is the NeoStore branch; H0/V is the current
                // NeoGameCenter branch. Both V/G enter the OEM no-data path
                // and schedule the normal main-activity transition.
                if (HookValues.findNoArgMethod(chain.getThisObject(), "V") != null) {
                    HookValues.invokeNoArg(chain.getThisObject(), "V");
                } else {
                    HookValues.invokeNoArg(chain.getThisObject(), "G");
                }
                return null;
            }, splashModel, boolean.class, long.class);
            session.hook(splashFragment, "onCreateView", chain -> {
                Object result = chain.proceed();
                if (result instanceof View) {
                    View splash = HookValues.findView((View) result,
                            session.packageName(), "splash");
                    if (splash != null) {
                        splash.setVisibility(View.GONE);
                    }
                }
                return result;
            }, android.view.LayoutInflater.class, android.view.ViewGroup.class,
                    android.os.Bundle.class);
            session.log("NEOSTORE_SSP_SPLASH_AD_DISABLED model="
                    + splashModel.getName());
        }

        // The post-home recommendation is a DialogFragment and does not use
        // the banner factory. Suppress its show entry for both store and game
        // center processes while keeping normal FragmentManager operation.
        Class<?> popup = session.findOptionalClass(
                "cn.nubia.neostore.ui.popupad.PopupAdFragment");
        Class<?> fragmentManager = session.findOptionalClass(
                "androidx.fragment.app.FragmentManager");
        Class<?> popupModel = session.findOptionalClass("cn.nubia.neostore.model.t0");
        Class<?> popupData = session.findOptionalClass("cn.nubia.neostore.model.s0");
        if (popup != null && fragmentManager != null
                && popupModel != null && popupData != null) {
            session.hook(popup, "O0", chain -> null,
                    fragmentManager, popupModel, popupData);
            session.log("NEOSTORE_POPUP_AD_DISABLED");
        } else if (popup != null) {
            Class<?> currentPopupData = session.findOptionalClass("n0.q0");
            if (currentPopupData != null) {
                session.hook(popup, "O0", chain -> null,
                        Map.class, currentPopupData);
                session.log("NEOSTORE_POPUP_AD_DISABLED model="
                        + currentPopupData.getName());
            }
        }

        // NeoStore's application-center presenter has a second entry point
        // before PopupAdFragment.O0(): f(Activity,t0) creates the fragment
        // and then invokes O0().  Suppressing only O0() leaves the controller
        // alive and permits a cached/native ad window to be attached by the
        // presenter.  Stop that presentation path as well; this method is
        // private and is therefore intentionally resolved through HookSession.
        Class<?> popupController = session.findOptionalClass("f2.a2");
        Class<?> popupHostActivity = session.findOptionalClass("android.app.Activity");
        Class<?> popupModelType = session.findOptionalClass(
                "cn.nubia.neostore.model.t0");
        if (popupController != null && popupHostActivity != null
                && popupModelType != null) {
            session.hook(popupController, "f", chain -> null,
                    popupHostActivity, popupModelType);
            session.log("NEOSTORE_POPUP_CONTROLLER_DISABLED");
        }

        session.log("NEOSTORE_SPLASH_AD_READY_BYPASS_HOOKED");
    }

    private static void installGameCenterSplashBypass(HookSession session) {
        Class<?> splashFragment = session.findOptionalClass(
                "cn.nubia.neostore.ui.start.SplashFragment");
        Class<?> splashModel = session.findOptionalClass("s2.c");
        if (splashFragment == null || splashModel == null) {
            session.missing("game-center-splash-capability");
            return;
        }
        session.hook(splashFragment, "H0", chain -> {
            // V() is the game-center no-ad transition used by SplashActivity.
            HookValues.invokeNoArg(chain.getThisObject(), "V");
            return null;
        }, splashModel, boolean.class, long.class);
        session.hook(splashFragment, "onCreateView", chain -> {
            Object result = chain.proceed();
            if (result instanceof View) {
                ((View) result).setVisibility(View.GONE);
            }
            return result;
        }, android.view.LayoutInflater.class, ViewGroup.class, Bundle.class);
        session.log("NEOGAMECENTER_SPLASH_AD_DISABLED");
    }

    private static void installAdRemoval(HookSession session) {
        // NeoStore's remaining home-page cards are SSP recommendation ads,
        // not the splash/popup path. They are requested through AdManager's
        // recommendPackage() API and can therefore still reach the network
        // after every visual adapter has been hidden.
        Class<?> sspManager = session.findOptionalClass("com.huanju.ssp.sdk.AdManager");
        if (sspManager != null) {
            session.hook(sspManager, "setRecDisable", chain ->
                    HookValues.proceedWithArgument(chain, 0, 1), int.class);
            Class<?> recommendListener = session.findOptionalClass(
                    "com.huanju.ssp.sdk.inf.IRecommendListener");
            if (recommendListener != null) {
                session.hook(sspManager, "recommendPackage", chain -> null,
                        recommendListener, List.class, String.class, int.class,
                        String.class);
                session.hook(sspManager, "recommendPackage", chain -> null,
                        recommendListener, List.class, String.class,
                        String.class);
            }
            session.hook(sspManager, "showBanner", chain -> null,
                    Activity.class, String.class, int.class);
            session.hook(sspManager, "showInsertAdView", chain -> null,
                    Activity.class, String.class);
            Class<?> splashAd = session.findOptionalClass(
                    "com.huanju.ssp.sdk.normal.SplashAd");
            Class<?> adListener = session.findOptionalClass(
                    "com.huanju.ssp.sdk.listener.AdListener");
            if (splashAd != null) {
                session.hook(sspManager, "showSplashAd", chain -> null,
                        Activity.class, splashAd);
                if (adListener != null) {
                    session.hook(sspManager, "showSplashAd", chain -> null,
                            Activity.class, splashAd, adListener);
                }
            }
            session.hook(sspManager, "showSplashAd", chain -> null,
                    Activity.class, Class.class, String.class);
            if (adListener != null) {
                session.hook(sspManager, "showBanner", chain -> null,
                        Activity.class, String.class, int.class, adListener,
                        boolean.class);
                session.hook(sspManager, "showInsertAdView", chain -> null,
                        Activity.class, String.class, adListener);
                session.hook(sspManager, "showSplashAd", chain -> null,
                        Activity.class, Class.class, String.class, adListener);
                session.hook(sspManager, "showSplashAd", chain -> null,
                        Activity.class, Class.class, String.class, adListener, int.class);
            }
            Class<?> requestManager = session.findOptionalClass(
                    "com.huanju.ssp.base.core.request.ad.RequestAdManager");
            Class<?> adParameter = session.findOptionalClass(
                    "com.huanju.ssp.base.core.request.ad.bean.AdParameter");
            Class<?> errorInfo = session.findOptionalClass(
                    "com.huanju.ssp.base.core.report.error.ErrorInfo");
            Class<?> stateListener = session.findOptionalClass(
                    "com.huanju.ssp.base.core.frame.listeners.AdStateChangListener");
            if (requestManager != null && adParameter != null
                    && errorInfo != null && stateListener != null) {
                session.hook(requestManager, "requestAd", chain -> null,
                        adParameter, errorInfo, stateListener);
                session.hook(requestManager, "requestBidingAd", chain -> null,
                        adParameter, errorInfo, stateListener);
                session.hook(requestManager, "sendReqTTAd", chain -> null);
                session.log("NEOSTORE_SSP_REQUESTS_BLOCKED");
            }
            session.log("NEOSTORE_SSP_RECOMMEND_ADS_DISABLED");
        }

        Class<?> homeActivity = session.findOptionalClass(
                "cn.nubia.neostore.ui.main.HomeActivity");
        if (homeActivity != null) {
            session.hook(homeActivity, "onResume", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    View decor = ((Activity) owner).getWindow().getDecorView();
                    scheduleStoreAdViewCleanup(session, decor);
                }
                return result;
            });
            session.hook(homeActivity, "A0", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    scheduleStoreAdViewCleanup(session,
                            ((Activity) owner).getWindow().getDecorView());
                }
                return result;
            });
        }

        Class<?> homeBanner = session.findOptionalClass(
                "cn.nubia.neostore.ui.main.view.banner.fragment.HomeWeisuoBannerAutoPlayFragment");
        if (homeBanner != null) {
            session.hook(homeBanner, "A0", chain -> {
                Object result = chain.proceed();
                if (result instanceof View) {
                    ((View) result).setVisibility(View.GONE);
                }
                return result;
            }, android.view.LayoutInflater.class, ViewGroup.class, Bundle.class);
        }
        Class<?> homeCarousel = session.findOptionalClass(
                "cn.nubia.neostore.ui.main.view.banner.HomeWeisuoCarouselViewPager");
        if (homeCarousel != null) {
            session.hook(homeCarousel, "onAttachedToWindow", chain -> {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof View) {
                    ((View) chain.getThisObject()).setVisibility(View.GONE);
                }
                return result;
            });
        }

        session.hook(TextView.class, "setText", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof TextView
                    && isKnownStoreAdText(String.valueOf(((TextView) owner).getText()))) {
                View current = (View) owner;
                for (int level = 0; level < 5 && current.getParent() instanceof View; level++) {
                    current = (View) current.getParent();
                    if ("app_item_layout".equals(HookValues.resourceEntryName(current))) {
                        current.setVisibility(View.GONE);
                        break;
                    }
                }
            }
            return result;
        }, CharSequence.class);

        // 1. RecommendAdsView (in-feed / recommend ad containers)
        // RecommendAdsView exists in some NeoStore revisions but was removed
        // from the shipped NeoGameCenter build. The remaining adapter hooks
        // below are the actual ad pipeline for that build, so this lookup is
        // optional and must not report a false broken-hook warning.
        Class<?> recommendAds = session.findOptionalClass(
                "cn.nubia.neostore.view.RecommendAdsView");
        if (recommendAds != null) {
            final Class<?> recommendAdsType = recommendAds;
            session.hook(recommendAds, "d", chain -> {
                Object obj = chain.getThisObject();
                Object result = chain.proceed();
                if (obj instanceof View) {
                    ((View) obj).setVisibility(View.GONE);
                }
                return result;
            });
            session.hook(recommendAds, "setVisibility", chain -> {
                // setVisibility is inherited from View on some releases.
                // HookSession may therefore resolve android.view.View's
                // method; never force every NeoStore View to GONE.
                if (!recommendAdsType.isInstance(chain.getThisObject())) {
                    return chain.proceed();
                }
                if (!chain.getArgs().isEmpty()) {
                    return HookValues.proceedWithArgument(chain, 0, View.GONE);
                }
                return chain.proceed();
            }, int.class);
            // The recommendation container is populated asynchronously after
            // its initial d() call. Re-hide it after both response paths.
            session.hook(recommendAds, "i", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (recommendAdsType.isInstance(owner) && owner instanceof View) {
                    ((View) owner).setVisibility(View.GONE);
                }
                return result;
            });
            session.hook(recommendAds, "j", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (recommendAdsType.isInstance(owner) && owner instanceof View) {
                    ((View) owner).setVisibility(View.GONE);
                }
                return result;
            });
        }

        // 2. Banner & Interstitial Gromore Ad Adapters
        Class<?> bannerAdapter = session.findClass("com.alex.AlexGromoreBannerAdapter");
        if (bannerAdapter != null) {
            session.hook(bannerAdapter, "getBannerView", chain -> {
                Object result = chain.proceed();
                if (result instanceof View) {
                    ((View) result).setVisibility(View.GONE);
                }
                return result;
            });
        }

        Class<?> interstitial = session.findClass("com.alex.AlexGromoreInterstitialAdapter");
        if (interstitial != null) {
            session.hook(interstitial, "isAdReady", chain -> Boolean.FALSE);
        }

        Class<?> nativeAdapter = session.findClass("com.alex.AlexGromoreNativeAdapter");
        session.hook(nativeAdapter, "isAdReady", chain -> Boolean.FALSE);
        Class<?> rewardedAdapter = session.findClass(
                "com.alex.AlexGromoreRewardedVideoAdapter");
        session.hook(rewardedAdapter, "isAdReady", chain -> Boolean.FALSE);

        // NeoGameCenter's visible top carousel is not always backed by the
        // Alex/Gromore adapters above. In the shipped build, ad-type banner
        // items enter through the Huanju SSP factory enum s2.f and are
        // created by d(Activity,String). Keep the OEM BannerAd object and
        // hide only its returned View: callers still receive the object they
        // expect, while the ad cannot paint over the page or break the
        // surrounding game-list initialization.
        if ("cn.nubia.neogamecenter".equals(session.packageName())) {
            // s2.f is a NeoGameCenter-only SSP factory.  Do not resolve it in
            // NeoStore: both packages use this adapter, but the application
            // center does not ship the game-center class and must not emit a
            // false CAPABILITY_MISSING/HOOK_FAILED diagnostic.
            Class<?> sspBannerFactory = session.findOptionalClass("s2.f");
            if (sspBannerFactory != null) {
                session.hook(sspBannerFactory, "d", chain -> {
                    Object result = chain.proceed();
                    Object adView = HookValues.invokeNoArg(result, "getAdView");
                    if (adView instanceof View) {
                        ((View) adView).setVisibility(View.GONE);
                    }
                    return result;
                }, Activity.class, String.class);
                session.log("NEOSTORE_SSP_BANNER_ADS_DISABLED");
            }

            // PopupAdHelper.i(Activity, q0) is the actual post-home entry
            // observed on the current build. It creates PopupAdFragment and
            // calls its O0() later, so blocking only O0() is too late for the
            // helper's own banner path.
            Class<?> popupHelper = session.findOptionalClass("r2.z1");
            Class<?> popupActivity = session.findOptionalClass(
                    "androidx.fragment.app.FragmentActivity");
            Class<?> popupData = session.findOptionalClass("n0.q0");
            if (popupHelper != null && popupActivity != null && popupData != null) {
                session.hook(popupHelper, "i", chain -> null,
                        popupActivity, popupData);
                session.log("NEOSTORE_POPUP_HELPER_DISABLED");
            }
        }

        // The current build's main banner adapter is an AnyThink custom
        // adapter backed by Huanju SSP. It can create/show an ad without
        // passing through s2.f or AlexGromore.
        Class<?> nbAdBanner = session.findOptionalClass(
                "com.nubia.ad.adapter.NBADBannerAdapter");
        if (nbAdBanner != null) {
            session.hook(nbAdBanner, "loadCustomNetworkAd", chain -> null,
                    Context.class, Map.class, Map.class);
            session.log("NEOSTORE_NBAD_BANNER_ADS_DISABLED");
        }

        session.log("NEOSTORE_IN_APP_ADS_REMOVED");
    }

    private static void installGameCenterAdRemoval(HookSession session) {
        String[] bannerClasses = new String[]{
                "cn.nubia.neostore.ui.main.GPBannerAutoPlayFragment",
                "cn.nubia.neostore.ui.main.view.banner.fragment.HomeDefaultBannerAutoPlayFragment",
                "cn.nubia.neostore.ui.main.HomeRecFragment"
        };
        for (String name : bannerClasses) {
            Class<?> banner = session.findOptionalClass(name);
            if (banner != null) {
                session.hook(banner, "onCreateView", chain -> {
                    Object result = chain.proceed();
                    if (result instanceof View) {
                        ((View) result).setVisibility(View.GONE);
                    }
                    return result;
                }, android.view.LayoutInflater.class, ViewGroup.class, Bundle.class);
                session.log("NEOGAMECENTER_BANNER_AD_DISABLED class=" + name);
            }
        }

        Class<?> hub = session.findOptionalClass(
                "cn.nubia.neostore.ui.newstart.NewStartHubFragment");
        if (hub != null) {
            session.hook(hub, "N0", chain -> {
                Object result = chain.proceed();
                if (result instanceof View) {
                    ((View) result).setVisibility(View.GONE);
                }
                return result;
            }, android.view.LayoutInflater.class, ViewGroup.class, Bundle.class);
            session.log("NEOGAMECENTER_NEWSTART_AD_DISABLED");
        }
    }

    private static void scheduleStoreAdViewCleanup(HookSession session, View root) {
        if (root == null) {
            return;
        }
        for (int attempt = 0; attempt <= 60; attempt++) {
            final long delay = attempt * 100L;
            root.postDelayed(() -> {
                View banner = HookValues.findView(root, session.packageName(),
                        "image_viewpager_parent");
                if (banner != null) {
                    banner.setVisibility(View.GONE);
                }
                View floating = HookValues.findView(root, session.packageName(),
                        "float_layout_root");
                if (floating != null) {
                    floating.setVisibility(View.GONE);
                }
                hideKnownStoreAdRows(session, root, root);
            }, delay);
        }
    }

    private static void hideKnownStoreAdRows(HookSession session, View view, View root) {
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        if ("app_item_layout".equals(HookValues.resourceEntryName(view))) {
            TextView name = findTextView(group, session, "tv_app_list_name");
            TextView intro = findTextView(group, session, "tv_app_list_intro");
            String text = (name == null ? "" : String.valueOf(name.getText())) + " "
                    + (intro == null ? "" : String.valueOf(intro.getText()));
            if (isKnownStoreAdText(text)) {
                group.setVisibility(View.GONE);
                return;
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            hideKnownStoreAdRows(session, group.getChildAt(i), root);
        }
    }

    private static TextView findTextView(ViewGroup root, HookSession session, String name) {
        View view = HookValues.findView(root, session.packageName(), name);
        return view instanceof TextView ? (TextView) view : null;
    }

    private static boolean isKnownStoreAdText(String text) {
        String value = text == null ? "" : text;
        String[] markers = {"短剧", "小说", "免费看", "领现金", "赚钱", "腾讯视频",
                "快手", "番茄", "红果", "王者万象棋", "福利", "红包"};
        for (String marker : markers) {
            if (value.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
