package dev.lackluster.redmagichelper.hook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import dev.lackluster.redmagichelper.hook.HostAdapter;
import dev.lackluster.redmagichelper.hook.HookSession;
import dev.lackluster.redmagichelper.util.BrowserIntentPolicy;

/** Runtime-adaptive UI and entry-point hooks for cn.nubia.browser. */
public final class BrowserAdapter implements HostAdapter {
    private static final String[] BOTTOM_HIDDEN_IDS = {
            "cont_news",
            "cont_video",
            "iv_news",
            "tv_news",
            "iv_video",
            "tv_video",
            "video_superscript",
            "iv_video_guide"
    };

    private static final String[] BOTTOM_CLICK_IDS = {
            "cont_news",
            "cont_video",
            "iv_news",
            "tv_news",
            "iv_video",
            "tv_video"
    };

    private static final String[] MINE_HIDDEN_IDS = {
            "banner",
            "part_app_user_center_for_mime",
            "myAppLayout",
            "appLayout",
            "moreAppLayout",
            "appListView",
            "part_drama_user_center_for_mime",
            "myDramaLayout",
            "dramaLayout",
            "moreDramaLayout",
            "dramaGridView",
            "part_service_user_center_for_mime",
            "moreSeviceLayout",
            "serviceGridView",
            "box_container",
            "home_root_view"
    };

    private static final String[] SEARCH_CLEAN_IDS = {
            "searchSupposeView",
            "searchHisSlidingTabLayout",
            "searchHisVp",
            "ivAction",
            "hotNewsView",
            "hotNewsRv",
            "hot_word_recommend_container",
            "hot_word_recommend",
            "hot_word_rv",
            "hot_words_layout",
            "hw_ll",
            "hot_word_tv",
            "tv_tab_title",
            "viewMoreHots",
            "search_hot_news",
            "news_container",
            // SeekView's current ROM build renders “今日热点” through the
            // generic BannerView row rather than HotNewsView.
            "banner_view",
            "active",
            "activeView",
            "activeLayout",
            "active_news",
            "activeNews",
            "active_container",
            "activeContent",
            "active_banner",
            // Keep the delayed home information-flow page from showing under
            // the bottom search overlay during the launch race.
            "newsViewPager",
            "infoFlowTitle"
    };

    private static final String[] SEARCH_SHORTCUT_LABELS = {
            "红魔商城",
            "58同城",
            "问AI",
            "京东",
            "百度搜索"
    };

    private static final String[] SEARCH_SHORTCUT_VIEW_IDS = {
            "rv_famous",
            "ll_index",
            "iv_first",
            "iv_second"
    };

    private static final String[] HOME_HOT_NEWS_IDS = {
            "homeHotNewsLayout",
            "hotNewsView",
            "hotNewsRv",
            "hot_news_more",
            "hot_news_icon",
            "viewMoreRightArrow",
            // Newer Browser builds wrap 今日热点 in a generic Active block.
            "active",
            "activeView",
            "activeLayout",
            "active_news",
            "activeNews",
            "active_container",
            "activeContent",
            "active_banner"
    };


    /**
     * BrowserLauncher constructs the embedded BrowserActivity synchronously
     * from onCreate().  A ThreadLocal lets the BottomBar hook select the tab
     * while its layout is being inflated, before the first window draw.
     */
    private static final ThreadLocal<Boolean> EARLY_SEARCH_REQUEST =
            new ThreadLocal<>();
    private static final ThreadLocal<Boolean> EARLY_SEARCH_SELECTED =
            new ThreadLocal<>();
    private static volatile boolean FORCE_PLAIN_SEARCH_MODE;
    /**
     * BrowserActivity initializes BottomBar from an idle callback after
     * BrowserLauncher.onCreate() has already returned. Keep the plain-launch
     * decision attached to that BrowserActivity until BottomBar.c0() consumes
     * it; a ThreadLocal cannot span that queue boundary.
     */
    private static final Map<Object, Boolean> PENDING_PLAIN_LAUNCHES =
            new WeakHashMap<>();
    private static final Map<Object, Boolean> SEARCH_SELECTION_GUARD =
            new WeakHashMap<>();
    private static final Map<View, Boolean> SEARCH_SHORTCUTS_CLEANED =
            new WeakHashMap<>();
    private static final AtomicReference<WeakReference<Object>> ACTIVE_POP_MENU =
            new AtomicReference<>(null);

    @Override
    public void install(HookSession session) {
        if (session.config().browserDisableTelemetry()) {
            installTelemetryRemoval(session);
        }
        if (session.config().browserSkipSplash()) {
            installSplashBypass(session);
        }
        boolean openSearch = session.config().browserOpenSearch();
        boolean removeInfoTabs = session.config().browserRemoveInfoTabs();
        if (openSearch) {
            installLauncherBottomSearch(session);
        }
        if (removeInfoTabs) {
            installInfoTabRemoval(session);
            installInfoFlowGuard(session);
        }
        // Opening the browser directly on the bottom search tab must not let
        // HomeFirstPageHelper select the information-flow page for one frame.
        // Install this guard even when the user only enabled default search
        // and did not request permanent removal of the news/video tabs.
        if (openSearch && !removeInfoTabs) {
            installInfoFlowGuard(session);
        }
        if (session.config().browserRemoveHomePageInfo()) {
            installHomePageInfoRemoval(session);
        }
        if (session.config().browserSlimMine()) {
            installMineSlimming(session);
        }
        if (session.config().browserRemoveHotWords()) {
            installSearchCleaning(session);
        }
        if (session.config().browserRemoveSearchShortcuts()) {
            installSearchShortcutRemoval(session);
        }
        if (session.config().browserRemoveDownloadHot()) {
            installDownloadHotRemoval(session);
        }
        if (session.config().browserRemoveDownloadRecommend()) {
            installDownloadRecommendRemoval(session);
        }
        if (session.config().browserHideSwitchModel()
                || session.config().browserRemoveHomePageInfo()
                || session.config().browserRemoveSearchShortcuts()) {
            installSwitchModelButtonRemoval(session);
        }
        if (session.config().browserComposeSettings()) {
            installBrowserSettingsCompose(session);
        }
        installBasicModelFullServiceBypass(session);
        installAdaptiveNavigationButtons(session);
        session.log("BROWSER_CAPABILITY_SCAN_COMPLETE");
    }

    /**
     * NuReportManager is used by almost every browser interaction.  The
     * individual event methods all funnel through s2(), so disabling the
     * report gate is enough to stop both queued and immediate reports.  init()
     * is also suppressed so the browser does not create report targets at
     * startup, while v2() remains untouched because callers still expect the
     * normal singleton object.
     */
    private static void installTelemetryRemoval(HookSession session) {
        Class<?> report = session.findOptionalClass(
                "com.android.browser.data.report.NuReportManager");
        if (report != null) {
            session.hook(report, "s2", chain -> Boolean.FALSE);
            session.hook(report, "y2", chain -> Boolean.FALSE);
            session.hook(report, "init", chain -> null);
            session.hook(report, "t2", chain -> null);
        }

        Class<?> trace = session.findOptionalClass(
                "com.android.browser.data.report.NuTraceManager");
        if (trace != null) {
            session.hook(trace, "b", chain -> null);
            session.hook(trace, "c", chain -> null);
            session.hook(trace, "d", chain -> null);
        }

        Class<?> reyun = session.findOptionalClass(
                "com.android.browser.data.report.ReYunReport");
        if (reyun != null) {
            session.hook(reyun, "init", chain -> null);
        }

        Class<?> adReport = session.findOptionalClass(
                "com.android.browser.news.ad.report.UmengReyunInfoAdReport");
        if (adReport != null) {
            session.hook(adReport, "a", chain -> null);
            session.hook(adReport, "b", chain -> null);
            session.hook(adReport, "c", chain -> null);
            session.hook(adReport, "d", chain -> null);
        }

        session.log("BROWSER_TELEMETRY_DISABLED");
    }

    private static void installSplashBypass(HookSession session) {
        Class<?> guide = session.findClass("com.android.browser.GuideActivity");
        HookSession.HookCallback bypassAction = chain -> {
            Object owner = chain.getThisObject();
            Method gotoBrowser = HookValues.findNoArgMethod(owner, "G");
            if (gotoBrowser == null) {
                session.missing("method=com.android.browser.GuideActivity#G()");
                return chain.proceed();
            }
            try {
                gotoBrowser.invoke(owner);
                return null;
            } catch (Throwable error) {
                session.missing("method=com.android.browser.GuideActivity#G() invoke failed "
                        + error.getClass().getSimpleName());
                return chain.proceed();
            }
        };
        session.hook(guide, "H", bypassAction);
        session.hook(guide, "K", bypassAction);

        Class<?> dataCenter = session.findClass("com.android.browser.datacenter.DataCenter");
        session.hook(dataCenter, "isNeedShowSplash", chain -> Boolean.FALSE);
        // BrowserActivity.s() gates the whole startup path on this combined
        // state.  Overriding only isNeedShowSplash() still enters
        // GuideActivity when CTA has not been acknowledged; that guide can
        // rewrite the launch intent to SearchActivity for one frame.
        session.hook(dataCenter, "isCtaAndSplashOK", chain -> Boolean.TRUE);
        session.hook(dataCenter, "isCTAOK", chain -> Boolean.TRUE);

        Class<?> adConfigs = session.findClass("com.android.browser.ad.AdConfigs");
        if (adConfigs != null) {
            session.hook(adConfigs, "g", chain -> Boolean.FALSE, Intent.class);
            session.hook(adConfigs, "i", chain -> Boolean.FALSE);
            session.hook(adConfigs, "j", chain -> Boolean.FALSE);
            session.hook(adConfigs, "k", chain -> Boolean.FALSE);
        }

        Class<?> splashAdManager = session.findOptionalClass(
                "com.android.browser.ad.splash.SplashAdManager");
        if (splashAdManager != null) {
            session.hook(splashAdManager, "c", chain -> null);
            session.hook(splashAdManager, "d", chain -> null);
        }

        Class<?> hjSplashAd = session.findOptionalClass(
                "com.huanju.ssp.sdk.normal.SplashAd");
        if (hjSplashAd != null) {
            session.hook(hjSplashAd, "loadAd", chain -> null);
            session.hook(hjSplashAd, "showAd", chain -> null);
        }

        Class<?> ttAdSdk = session.findOptionalClass(
                "com.bytedance.sdk.openadsdk.TTAdSdk");
        if (ttAdSdk != null) {
            session.hook(ttAdSdk, "isInitSuccess", chain -> Boolean.FALSE);
            session.hook(ttAdSdk, "start", chain -> null);
        }

        Class<?> ttAdHolder = session.findOptionalClass(
                "com.android.browser.news.util.TTAdManagerHolder");
        if (ttAdHolder != null) {
            session.hook(ttAdHolder, "b", chain -> null);
            session.hook(ttAdHolder, "c", chain -> null);
        }

        Class<?> hjTtAdHolder = session.findOptionalClass(
                "com.huanju.ssp.base.core.common.TTAdManagerHolder");
        if (hjTtAdHolder != null) {
            session.hook(hjTtAdHolder, "init", chain -> null);
        }
    }

    private static void installLauncherBottomSearch(HookSession session) {
        Class<?> launcher = session.findClass("com.android.browser.BrowserLauncher");
        if (launcher == null) {
            return;
        }
        final Class<?> launcherType = launcher;
        installBrowserActivityBottomSearch(session);
        // Some browser builds finish BottomBar inflation from an idle
        // callback. Keep the c0() hook installed for the first layout too.
        installEarlyBottomSearch(session);
        session.hook(launcher, "onCreate", chain -> {
            preloadAccountInfo(session);
            Object owner = chain.getThisObject();
            Object savedState = HookValues.arg(chain, 0);
            boolean plainLaunch = launcherType.isInstance(owner)
                    && owner instanceof Activity
                    && savedState == null
                    && isPlainLauncherIntent(((Activity) owner).getIntent());
            if (plainLaunch) {
                EARLY_SEARCH_REQUEST.set(Boolean.TRUE);
                // The launcher-to-browser transition is otherwise rendered
                // together with the browser's late bottom-tab selection,
                // which looks like a one-frame jump from the desktop. Keep
                // this specific plain launch visually stable.
                ((Activity) owner).overridePendingTransition(0, 0);
                hidePlainLaunchUntilSearch((Activity) owner);
            }

            Object result;
            try {
                result = chain.proceed();
            } finally {
                if (plainLaunch) {
                    EARLY_SEARCH_REQUEST.remove();
                }
            }
            if (plainLaunch) {
                Object browserActivity = HookValues.readField(owner, "n");
                if (browserActivity != null) {
                    synchronized (PENDING_PLAIN_LAUNCHES) {
                        PENDING_PLAIN_LAUNCHES.put(browserActivity, Boolean.TRUE);
                    }
                }
            }
            return result;
        }, Bundle.class);

        session.hook(launcher, "onNewIntent", chain -> {
            preloadAccountInfo(session);
            Object owner = chain.getThisObject();
            Intent intent = (Intent) HookValues.arg(chain, 0);
            Object result = chain.proceed();
            if (launcherType.isInstance(owner)
                    && owner instanceof Activity
                    && isPlainLauncherIntent(intent)) {
                selectBottomSearch(session, (Activity) owner);
            }
            return result;
        }, Intent.class);

        try {
            session.hook(launcher, "onResume", chain -> {
                preloadAccountInfo(session);
                return chain.proceed();
            });
        } catch (Throwable ignored) {}
    }

    private static void installBrowserActivityBottomSearch(HookSession session) {
        Class<?> browserActivity = session.findClass("com.android.browser.BrowserActivity");
        session.hook(browserActivity, "s", chain -> {
            preloadAccountInfo(session);
            Object result = chain.proceed();
            Object activity = HookValues.invokeNoArg(chain.getThisObject(), "getActivity");
            if (activity instanceof Activity
                    && isPlainLauncherIntent(((Activity) activity).getIntent())) {
                selectBottomSearch(session, (Activity) activity);
                session.log("BROWSER_BOTTOM_SEARCH_AFTER_BROWSER_INIT");
            }
            return result;
        }, Bundle.class);
    }

    private static void installEarlyBottomSearch(HookSession session) {
        Class<?> bottomBar = session.findClass("com.android.browser.BottomBar");
        session.hook(bottomBar, "c0", chain -> {
            Object result = chain.proceed();
            if (!(chain.getThisObject() instanceof View)) {
                return result;
            }

            View root = (View) chain.getThisObject();
            Object browserActivity = HookValues.readField(root, "s");
            Object selectionKey = browserActivity == null ? root : browserActivity;
            synchronized (SEARCH_SELECTION_GUARD) {
                if (Boolean.TRUE.equals(SEARCH_SELECTION_GUARD.get(selectionKey))) {
                    return result;
                }
            }
            boolean pendingInstance;
            synchronized (PENDING_PLAIN_LAUNCHES) {
                pendingInstance = browserActivity != null
                        && Boolean.TRUE.equals(PENDING_PLAIN_LAUNCHES.get(browserActivity));
            }
            if ((!Boolean.TRUE.equals(EARLY_SEARCH_REQUEST.get()) && !pendingInstance)
                    || Boolean.TRUE.equals(EARLY_SEARCH_SELECTED.get())) {
                return result;
            }
            View searchTab = HookValues.findView(root, session.packageName(), "cont_seek");
            if (searchTab != null && searchTab.getVisibility() == View.VISIBLE) {
                boolean clicked = searchTab.performClick();
                if (clicked && root != null) {
                    synchronized (SEARCH_SELECTION_GUARD) {
                        SEARCH_SELECTION_GUARD.put(selectionKey, Boolean.TRUE);
                    }
                    FORCE_PLAIN_SEARCH_MODE = true;
                    revealPlainLaunch(root);
                    scheduleSearchShortcutCleanup(session, root);
                    if (browserActivity != null) {
                        synchronized (PENDING_PLAIN_LAUNCHES) {
                            PENDING_PLAIN_LAUNCHES.put(browserActivity, Boolean.FALSE);
                        }
                    }
                }
                session.log("BROWSER_BOTTOM_SEARCH_SELECTED clicked=" + clicked
                        + " early=" + (!pendingInstance));
            }
            return result;
        });
    }

    /**
     * Let the browser's own BottomBar click listener open search.  Starting
     * SearchActivity directly produces the standalone search screen, while
     * the OEM bottom tab also updates BrowserActivity's selected page and
     * keeps the normal bottom navigation state.
     */
    private static void selectBottomSearch(HookSession session, Activity activity) {
        final View decor;
        try {
            decor = activity.getWindow().getDecorView();
        } catch (Throwable error) {
            session.log("BROWSER_BOTTOM_SEARCH_VIEW_FAILED "
                    + error.getClass().getSimpleName());
            return;
        }
        if (decor == null) {
            session.missing("view=BrowserLauncher decor");
            return;
        }
        if (session.config().browserRemoveHomePageInfo()) {
            scheduleHomeHotNewsCleanup(session, decor);
        }
        selectBottomSearch(session, decor, 0);
    }

    private static void selectBottomSearch(HookSession session, View decor, int attempt) {
        HookValues.runOnMain(decor, () -> {
            View searchTab = HookValues.findView(decor, session.packageName(), "cont_seek");
            if (searchTab != null && searchTab.getVisibility() == View.VISIBLE) {
                View bottomBar = findBottomBar(searchTab);
                Object browserActivity = bottomBar == null
                        ? null : HookValues.readField(bottomBar, "s");
                Object selectionKey = browserActivity == null ? decor : browserActivity;
                synchronized (SEARCH_SELECTION_GUARD) {
                    if (Boolean.TRUE.equals(SEARCH_SELECTION_GUARD.get(selectionKey))) {
                        return;
                    }
                }
                synchronized (PENDING_PLAIN_LAUNCHES) {
                    if (browserActivity != null
                            && Boolean.FALSE.equals(PENDING_PLAIN_LAUNCHES.get(browserActivity))
                            && isSearchViewVisible(decor)) {
                        return;
                    }
                }
                boolean clicked = searchTab.performClick();
                if (clicked && browserActivity != null) {
                    synchronized (PENDING_PLAIN_LAUNCHES) {
                        PENDING_PLAIN_LAUNCHES.put(browserActivity, Boolean.FALSE);
                    }
                }
                if (clicked) {
                    synchronized (SEARCH_SELECTION_GUARD) {
                        SEARCH_SELECTION_GUARD.put(selectionKey, Boolean.TRUE);
                    }
                    FORCE_PLAIN_SEARCH_MODE = true;
                    revealPlainLaunch(decor);
                    scheduleSearchShortcutCleanup(session, decor);
                }
                session.log("BROWSER_BOTTOM_SEARCH_SELECTED clicked=" + clicked
                        + " early=false");
                return;
            }

            // BottomBar is inflated after the launcher/home bootstrap and can
            // be delayed by the ROM's first-unlock transition. Keep polling
            // the same window long enough for the real view tree to appear.
            if (attempt < 80 && decor.postDelayed(
                    () -> selectBottomSearch(session, decor, attempt + 1), 100L)) {
                return;
            }
            session.missing("view=bottom_bar/cont_seek");
        });
    }

    /**
     * The OEM click posts message 0x130 and then lets HomeNavView attach the
     * seek page. A few RedMagic builds race that message with home-page
     * bootstrap, so repeat the semantic UI transition after the click.
     */
    private static void forceBottomSearch(
            HookSession session,
            View decor,
            Object browserActivity
    ) {
        try {
            Object controller = HookValues.readField(browserActivity, "q");
            Object ui = HookValues.readField(controller, "c");
            Method showHome = HookValues.findMethod(ui, "H", boolean.class);
            if (showHome != null) {
                // BaseUi.H(true) is the OEM information-flow entry. Calling
                // it here was the reason the bottom search tab stayed lit
                // while newsViewPager/今日热点 remained underneath. Close
                // the information-flow page before attaching SeekView.
                showHome.invoke(ui, false);
            }

            View homeNav = findViewByClassName(
                    decor, "com.android.browser.view.HomeNavView");
            Method showSearch = HookValues.findMethod(homeNav, "E", boolean.class);
            if (showSearch != null) {
                showSearch.invoke(homeNav, true);
                session.log("BROWSER_BOTTOM_SEARCH_VIEW_FORCED");
            }
        } catch (Throwable error) {
            session.log("BROWSER_BOTTOM_SEARCH_FORCE_FAILED "
                    + error.getClass().getSimpleName());
        }
    }

    private static boolean isSearchViewVisible(View root) {
        View search = findViewByClassName(root, "com.android.browser.ui.SeekView");
        return search != null && search.getVisibility() == View.VISIBLE;
    }

    private static void hidePlainLaunchUntilSearch(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            decor.setAlpha(0f);
            decor.postDelayed(() -> {
                if (decor.getAlpha() == 0f) {
                    decor.setAlpha(1f);
                }
            }, 600L);
        } catch (Throwable ignored) {
            // The fallback does not affect the normal browser launch path.
        }
    }

    private static void revealPlainLaunch(View view) {
        if (view == null) {
            return;
        }
        View decor = view.getRootView();
        if (decor != null && decor.getAlpha() == 0f) {
            decor.setAlpha(1f);
        }
    }

    private static View findViewByClassName(View view, String className) {
        if (view == null || className == null) {
            return null;
        }
        if (className.equals(view.getClass().getName())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = findViewByClassName(group.getChildAt(i), className);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static View findBottomBar(View child) {
        View current = child;
        for (int level = 0; level < 10 && current != null; level++) {
            if (HookValues.readField(current, "s") != null) {
                return current;
            }
            if (!(current.getParent() instanceof View)) {
                break;
            }
            current = (View) current.getParent();
        }
        return null;
    }

    private static boolean isPlainLauncherIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        try {
            Bundle extras = intent.getExtras();
            return BrowserIntentPolicy.isPlainLauncher(
                    intent.getAction(),
                    intent.hasCategory(Intent.CATEGORY_LAUNCHER),
                    intent.getData() != null,
                    intent.getClipData() != null,
                    extras != null && !extras.isEmpty());
        } catch (Throwable ignored) {
            // A malformed or unparcelable intent must follow the OEM path.
            return false;
        }
    }

    private static void installInfoTabRemoval(HookSession session) {
        Class<?> bottomBar = session.findClass("com.android.browser.BottomBar");
        session.hook(bottomBar, "c0", chain -> {
            Object result = chain.proceed();
            hideBottomInfoViews(session, chain.getThisObject());
            return result;
        });
        session.hook(bottomBar, "E0", chain -> {
            Object result = chain.proceed();
            hideBottomInfoViews(session, chain.getThisObject());
            return result;
        }, boolean.class, boolean.class);
        session.hook(bottomBar, "G0", chain -> {
            Object result = chain.proceed();
            hideBottomInfoViews(session, chain.getThisObject());
            return result;
        }, boolean.class, boolean.class);
        session.hook(bottomBar, "onWindowVisibilityChanged", chain -> {
            Object result = chain.proceed();
            hideBottomInfoViews(session, chain.getThisObject());
            return result;
        }, int.class);
        session.hook(bottomBar, "onLayout", chain -> {
            Object result = chain.proceed();
            hideBottomInfoViews(session, chain.getThisObject());
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);

        Class<?> clickListener = session.findClass("com.android.browser.BottomBar$4");
        session.hook(clickListener, "onClick", chain -> {
            Object value = HookValues.arg(chain, 0);
            if (value instanceof View
                    && HookValues.hasResourceId((View) value, session.packageName(),
                    BOTTOM_CLICK_IDS)) {
                session.log("BROWSER_INFO_TAB_CLICK_BLOCKED");
                return null;
            }
            return chain.proceed();
        }, View.class);
    }

    private static void hideBottomInfoViews(HookSession session, Object owner) {
        if (!(owner instanceof View)) {
            session.missing("view=com.android.browser.BottomBar");
            return;
        }
        View root = (View) owner;
        HookValues.runOnMain(root, () -> {
            for (String name : BOTTOM_HIDDEN_IDS) {
                View view = HookValues.findView(root, session.packageName(), name);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
            }
            try {
                Field fC = HookValues.findField(owner, "C");
                if (fC != null) {
                    Object vC = fC.get(owner);
                    if (vC instanceof View) ((View) vC).setVisibility(View.GONE);
                }
                Field fF = HookValues.findField(owner, "F");
                if (fF != null) {
                    Object vF = fF.get(owner);
                    if (vF instanceof View) ((View) vF).setVisibility(View.GONE);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private static void installInfoFlowGuard(HookSession session) {
        Class<?> helper = session.findClass(
                "com.android.browser.ui.helper.HomeFirstPageHelper");
        // T() constructs and wires the information-flow view. Suppressing it
        // leaves HomeNavView partially initialized and prevents the OEM seek
        // page from being attached later. Only guard the mode callback below.
        session.hook(helper, "H", chain -> {
            Object value = HookValues.arg(chain, 0);
            if (value instanceof Number && ((Number) value).intValue() == 1) {
                if (FORCE_PLAIN_SEARCH_MODE) {
                    session.log("BROWSER_INFO_FLOW_MODE_BLOCKED_FOR_SEARCH");
                    return HookValues.proceedWithArgument(chain, 0, 0);
                }
                // HomeFirstPageHelper invokes H(1) while HomeNavView is still
                // constructing. Calling the OEM method with H(0) at that
                // point reaches its K/N fields before ViewHolderHelper has
                // populated them and crashes BrowserLauncher. Defer that
                // bootstrap call; later calls run after the holder is ready.
                if (Boolean.TRUE.equals(EARLY_SEARCH_REQUEST.get())) {
                    session.log("BROWSER_INFO_FLOW_MODE_DEFERRED_BOOTSTRAP");
                    return null;
                }
                if (!isHomeFirstPageReady(chain.getThisObject())) {
                    return chain.proceed();
                }
                session.log("BROWSER_INFO_FLOW_MODE_BLOCKED");
                return HookValues.proceedWithArgument(chain, 0, 0);
            }
            return chain.proceed();
        }, int.class);
    }

    private static boolean isHomeFirstPageReady(Object helper) {
        Object holder = HookValues.invokeNoArg(helper, "k");
        return holder != null
                && HookValues.readField(holder, "H") != null
                && HookValues.readField(holder, "K") != null;
    }

    private static void installHomePageInfoRemoval(HookSession session) {
        // Remove the setting row itself, including its divider, so the user
        // cannot enter the OEM information-flow settings page.
        Class<?> settings = session.findClass(
                "com.android.browser.settings.BrowserSettingActivity");
        session.hook(settings, "onCreate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof Activity) {
                View decor = ((Activity) owner).getWindow().getDecorView();
                hideHomePageInfoSetting(session, decor);
            }
            return result;
        }, Bundle.class);

        // item_home_hot_news.xml uses a plain LinearLayout as its root, so a
        // resource-specific View hook is more stable than relying on a
        // particular home fragment or adapter method name.
        Class<?> view = session.findClass("android.view.View");
        session.hook(view, "onFinishInflate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof View) {
                View v = (View) owner;
                String resName = HookValues.resourceEntryName(v);
                if ("homeHotNewsLayout".equals(resName)) {
                    v.setVisibility(View.GONE);
                    session.log("BROWSER_HOME_INFO_CONTENT_HIDDEN");
                } else if ("switch_model".equals(resName)) {
                    v.setVisibility(View.GONE);
                    session.log("BROWSER_SWITCH_MODEL_CONTENT_HIDDEN");
                }
            }
            return result;
        });
    }

    private static void hideHomePageInfoSetting(HookSession session, View root) {
        if (root == null) {
            return;
        }
        View row = HookValues.findView(root, session.packageName(), "infoflow_setting");
        if (row != null) {
            row.setVisibility(View.GONE);
        }
        View divider = HookValues.findView(
                root, session.packageName(), "setting_separator_space7");
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
        if (row != null || divider != null) {
            session.log("BROWSER_HOME_INFO_SETTING_HIDDEN");
        }
    }

    private static void installBrowserSettingsCompose(HookSession session) {
        Class<?> androidUtil = session.findOptionalClass("com.android.browser.util.AndroidUtil");
        if (androidUtil != null) {
            session.hook(androidUtil, "r", chain -> {
                Object act = chain.getArg(0);
                if (act instanceof Activity) {
                    String name = act.getClass().getName();
                    if (name.startsWith("com.android.browser.settings.")
                            || name.contains("ServiceSettingActivity")
                            || name.contains("UserCenterActivity")) {
                        return null;
                    }
                }
                return chain.proceed();
            }, Activity.class, boolean.class);
        }

        Class<?> settings = session.findOptionalClass(
                "com.android.browser.settings.BrowserSettingActivity");
        if (settings != null) {
            session.hook(settings, "onCreate", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.attach(
                            (Activity) owner, session);
                }
                return result;
            }, Bundle.class);

            session.hook(settings, "onResume", chain -> {
                Object result = chain.proceed();
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onResume(
                        chain.getThisObject());
                return result;
            });

            session.hook(settings, "onPause", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onPause(
                        chain.getThisObject());
                return chain.proceed();
            });

            session.hook(settings, "onDestroy", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onDestroy(
                        chain.getThisObject());
                return chain.proceed();
            });
            session.log("BROWSER_SETTINGS_COMPOSE_HOOK_INSTALLED");
        }

        // Sub-setting activities: transform into system-styled Compose UI
        Class<?> baseNightActivity = session.findOptionalClass(
                "com.android.browser.BaseNightActivity");
        if (baseNightActivity != null) {
            session.hook(baseNightActivity, "onPostCreate", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    String name = owner.getClass().getName();
                    if ((name.startsWith("com.android.browser.settings.") || name.contains("ServiceSettingActivity"))
                            && !name.endsWith("BrowserSettingActivity")) {
                        dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.attachSubSettings(
                                (Activity) owner, session);
                    }
                }
                return res;
            }, Bundle.class);

            session.hook(baseNightActivity, "onResume", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onResume(owner);
                }
                return res;
            });

            session.hook(baseNightActivity, "onPause", chain -> {
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onPause(owner);
                }
                return chain.proceed();
            });

            session.hook(baseNightActivity, "onDestroy", chain -> {
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onDestroy(owner);
                }
                return chain.proceed();
            });
        }

        Class<?> baseActivity = session.findOptionalClass(
                "com.android.browser.BaseActivity");
        if (baseActivity != null) {
            session.hook(baseActivity, "onPostCreate", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    String name = owner.getClass().getName();
                    if ((name.startsWith("com.android.browser.settings.") || name.contains("ServiceSettingActivity"))
                            && !name.endsWith("BrowserSettingActivity")) {
                        dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.attachSubSettings(
                                (Activity) owner, session);
                    }
                }
                return res;
            }, Bundle.class);

            session.hook(baseActivity, "onResume", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onResume(owner);
                }
                return res;
            });

            session.hook(baseActivity, "onPause", chain -> {
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onPause(owner);
                }
                return chain.proceed();
            });

            session.hook(baseActivity, "onDestroy", chain -> {
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onDestroy(owner);
                }
                return chain.proceed();
            });
        }

        // Transform UserCenterActivity (ZTE Account / Mine page) into system Compose UI
        Class<?> userCenterAct = session.findOptionalClass("cn.nubia.usercenter.UserCenterActivity");
        if (userCenterAct != null) {
            session.hook(userCenterAct, "onCreate", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.attachUserCenter(
                            (Activity) owner, session);
                }
                return res;
            }, Bundle.class);

            session.hook(userCenterAct, "onResume", chain -> {
                Object res = chain.proceed();
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onResume(chain.getThisObject());
                return res;
            });

            session.hook(userCenterAct, "onPause", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onPause(chain.getThisObject());
                return chain.proceed();
            });

            session.hook(userCenterAct, "onDestroy", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onDestroy(chain.getThisObject());
                return chain.proceed();
            });
            session.log("BROWSER_USER_CENTER_COMPOSE_HOOK_INSTALLED");
        }

        Class<?> oldUserCenterAct = session.findOptionalClass("cn.nubia.account.OldUserCenterActivity");
        if (oldUserCenterAct != null) {
            session.hook(oldUserCenterAct, "onCreate", chain -> {
                Object res = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.attachUserCenter(
                            (Activity) owner, session);
                }
                return res;
            }, Bundle.class);

            session.hook(oldUserCenterAct, "onResume", chain -> {
                Object res = chain.proceed();
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onResume(chain.getThisObject());
                return res;
            });

            session.hook(oldUserCenterAct, "onPause", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onPause(chain.getThisObject());
                return chain.proceed();
            });

            session.hook(oldUserCenterAct, "onDestroy", chain -> {
                dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.onDestroy(chain.getThisObject());
                return chain.proceed();
            });
            session.log("BROWSER_OLD_USER_CENTER_COMPOSE_HOOK_INSTALLED");
        }
    }

    private static void replaceTogglesWithSystemSwitches(View root) {
        if (root == null) return;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof android.widget.ToggleButton) {
                    android.widget.ToggleButton tb = (android.widget.ToggleButton) child;
                    ViewGroup parent = (ViewGroup) tb.getParent();
                    if (parent != null && parent.findViewWithTag("injected_switch_for_" + tb.getId()) == null) {
                        android.widget.Switch sw = new android.widget.Switch(tb.getContext());
                        sw.setTag("injected_switch_for_" + tb.getId());
                        sw.setChecked(tb.isChecked());
                        sw.setLayoutParams(tb.getLayoutParams());
                        sw.setOnCheckedChangeListener((btn, isChecked) -> {
                            if (tb.isChecked() != isChecked) {
                                tb.setChecked(isChecked);
                            }
                        });
                        tb.setOnCheckedChangeListener((btn, isChecked) -> {
                            if (sw.isChecked() != isChecked) {
                                sw.setChecked(isChecked);
                            }
                        });
                        tb.setVisibility(View.GONE);
                        int idx = parent.indexOfChild(tb);
                        parent.addView(sw, idx);
                    }
                } else if (child instanceof ViewGroup) {
                    replaceTogglesWithSystemSwitches(child);
                }
            }
        }
    }

    private static void installSwitchModelButtonRemoval(HookSession session) {
        // 1. Hook ViewBasicModelHomeBinding binding
        Class<?> basicBinding = session.findOptionalClass(
                "cn.nubia.browser.databinding.ViewBasicModelHomeBinding");
        if (basicBinding != null) {
            session.hook(basicBinding, "a", chain -> {
                Object binding = chain.proceed();
                if (binding != null) {
                    Object tv = HookValues.readField(binding, "c");
                    if (tv instanceof View) {
                        ((View) tv).setVisibility(View.GONE);
                        session.log("BROWSER_SWITCH_MODEL_BUTTON_HIDDEN_BINDING");
                    }
                }
                return binding;
            }, View.class);
        }

        // 2. Hook HomePageFirstView.K(LayoutInflater)
        Class<?> homeFirstView = session.findOptionalClass("com.android.browser.HomePageFirstView");
        if (homeFirstView != null) {
            Class<?> inflaterClass = session.findOptionalClass("android.view.LayoutInflater");
            if (inflaterClass != null) {
                session.hook(homeFirstView, "K", chain -> {
                    Object res = chain.proceed();
                    Object binding = HookValues.readField(chain.getThisObject(), "O");
                    if (binding != null) {
                        Object tv = HookValues.readField(binding, "c");
                        if (tv instanceof View) {
                            ((View) tv).setVisibility(View.GONE);
                        }
                    }
                    return res;
                }, inflaterClass);
            }
        }

        // 3. Hook BasicModeView
        Class<?> basicModeView = session.findOptionalClass("com.android.browser.basicmodel.BasicModeView");
        if (basicModeView != null) {
            session.hook(basicModeView, "onFinishInflate", chain -> {
                Object res = chain.proceed();
                Object binding = HookValues.readField(chain.getThisObject(), "q");
                if (binding != null) {
                    Object tv = HookValues.readField(binding, "c");
                    if (tv instanceof View) {
                        ((View) tv).setVisibility(View.GONE);
                    }
                }
                return res;
            });
        }
    }

    private static void installBasicModelFullServiceBypass(HookSession session) {
        Class<?> globalConfig = session.findOptionalClass("com.android.browser.configs.GlobalConfig");
        if (globalConfig != null) {
            Class<?> function0Clz = session.findOptionalClass("kotlin.jvm.functions.Function0");
            if (function0Clz != null) {
                session.hook(globalConfig, "a", chain -> {
                    Object handle = chain.getArg(1);
                    if (handle != null) {
                        try {
                            Method invokeMethod = handle.getClass().getMethod("invoke");
                            invokeMethod.invoke(handle);
                        } catch (Throwable t) {
                            session.log("GLOBAL_CONFIG_A_INVOKE_FAILED: " + t.getMessage());
                        }
                    }
                    return null;
                }, Context.class, function0Clz);
            }
            session.hook(globalConfig, "g", chain -> Boolean.TRUE);
            session.log("BROWSER_FULL_SERVICE_GLOBAL_CONFIG_HOOKED");
        }

        Class<?> serviceSettingManager = session.findOptionalClass(
                "com.android.browser.basicmodel.ServiceSettingManager");
        if (serviceSettingManager != null) {
            Class<?> function0Clz = session.findOptionalClass("kotlin.jvm.functions.Function0");
            if (function0Clz != null) {
                session.hook(serviceSettingManager, "e", chain -> {
                    Object handle = chain.getArg(1);
                    if (handle != null) {
                        try {
                            Method invokeMethod = handle.getClass().getMethod("invoke");
                            invokeMethod.invoke(handle);
                        } catch (Throwable t) {
                            session.log("SERVICE_SETTING_E_INVOKE_FAILED: " + t.getMessage());
                        }
                    }
                    return null;
                }, Context.class, function0Clz);
            }
            session.log("BROWSER_FULL_SERVICE_MANAGER_HOOKED");
        }

        // Allow ZTE Account to sync in basic mode
        Class<?> amClz = session.findOptionalClass("cn.nubia.account.AccountManager");
        if (amClz != null) {
            Method zMethod = null;
            try {
                zMethod = amClz.getDeclaredMethod("z", WeakReference.class);
                zMethod.setAccessible(true);
            } catch (Throwable ignored) {}
            if (zMethod == null) {
                try {
                    zMethod = amClz.getDeclaredMethod("Z", WeakReference.class);
                    zMethod.setAccessible(true);
                } catch (Throwable ignored) {}
            }
            final Method finalZ = zMethod;
            session.hook(amClz, "y", chain -> {
                if (finalZ != null) {
                    try {
                        finalZ.invoke(chain.getThisObject(), (Object) null);
                        return null;
                    } catch (Throwable t) {
                        session.log("ACCOUNT_MGR_Y_INVOKE_FAILED: " + t.getMessage());
                    }
                }
                return chain.proceed();
            });

            // Ensure isLogin check (AccountManager.C) returns true if logged in even before AIDL finishes
            try {
                session.hook(amClz, "C", chain -> {
                    Boolean res = (Boolean) chain.proceed();
                    return Boolean.TRUE.equals(res)
                            || hasAccountIdentity(session, chain.getThisObject());
                });
            } catch (Throwable ignored) {}

            // Notify Compose and UI listeners whenever account state/avatar changes
            try {
                session.hook(amClz, "f0", chain -> {
                    Object res = chain.proceed();
                    try {
                        dev.lackluster.redmagichelper.hook.browser.BrowserSettingsCompose.notifyAccountUpdated();
                    } catch (Throwable ignored) {}
                    try {
                        WeakReference<Object> ref = ACTIVE_POP_MENU.get();
                        Object pop = ref != null ? ref.get() : null;
                        if (pop != null) {
                            new Handler(Looper.getMainLooper())
                                    .post(() -> updatePopMenuAccountUi(session, pop));
                        }
                    } catch (Throwable ignored) {}
                    return res;
                }, int.class);
            } catch (Throwable ignored) {}

            session.log("BROWSER_ACCOUNT_MANAGER_Y_HOOKED");
        }

        // Fix bottom PopMenu showing "未登录" and blocking clicks in basic mode
        Class<?> popMenuClz = session.findOptionalClass("com.android.browser.view.PopMenu");
        if (popMenuClz != null) {
            // Hook o0(boolean, boolean, boolean) - called whenever bottom menu opens
            try {
                session.hook(popMenuClz, "o0", chain -> {
                    Object res = chain.proceed();
                    try {
                        Object popMenu = chain.getThisObject();
                        ACTIVE_POP_MENU.set(new WeakReference<>(popMenu));
                        updatePopMenuAccountUi(session, popMenu);

                        View root = null;
                        Object n0 = HookValues.readField(popMenu, "n0");
                        if (n0 instanceof View) root = (View) n0;

                        int[] delays = {80, 200, 450, 800, 1400, 2200};
                        for (int delay : delays) {
                            if (root != null) {
                                root.postDelayed(() -> updatePopMenuAccountUi(session, popMenu), delay);
                            } else {
                                new Handler(Looper.getMainLooper())
                                        .postDelayed(() -> updatePopMenuAccountUi(session, popMenu), delay);
                            }
                        }
                    } catch (Throwable t) {
                        session.log("POPMENU_O0_HOOK_ERROR: " + t.getMessage());
                    }
                    return res;
                }, boolean.class, boolean.class, boolean.class);
            } catch (Throwable ignored) {}

            // Let the OEM update first, then apply the broader account state.
            session.hook(popMenuClz, "c0", chain -> {
                Object result = chain.proceed();
                Object popMenu = chain.getThisObject();
                ACTIVE_POP_MENU.set(new WeakReference<>(popMenu));
                updatePopMenuAccountUi(session, popMenu);
                return result;
            });

            // Hook Z(PopMenu) - static bridge method called by PopMenu$7 when AIDL account message 1000 arrives
            try {
                session.hook(popMenuClz, "Z", chain -> {
                    Object result = chain.proceed();
                    Object popMenu = chain.getArgs().size() > 0 ? chain.getArgs().get(0) : null;
                    if (popMenu != null) {
                        ACTIVE_POP_MENU.set(new WeakReference<>(popMenu));
                        updatePopMenuAccountUi(session, popMenu);
                    }
                    return result;
                }, popMenuClz);
            } catch (Throwable ignored) {}

            // Handle user center item click in PopMenu
            HookSession.HookCallback onUserCenterClick = chain -> {
                try {
                    Object popMenu = chain.getThisObject();
                    Context context = null;
                    if (chain.getArgs().size() > 0 && chain.getArgs().get(0) instanceof Context) {
                        context = (Context) chain.getArgs().get(0);
                    } else if (chain.getArgs().size() > 1 && chain.getArgs().get(1) instanceof Context) {
                        context = (Context) chain.getArgs().get(1);
                        popMenu = chain.getArgs().get(0);
                    }
                    handlePopMenuUserCenterClick(session, popMenu, context);
                    return null;
                } catch (Throwable t) {
                    session.log("POPMENU_CLICK_HOOK_ERROR: " + t.getMessage());
                }
                return chain.proceed();
            };

            try {
                session.hook(popMenuClz, "k0", onUserCenterClick, Context.class, View.class);
            } catch (Throwable ignored) {}
            try {
                session.hook(popMenuClz, "J", onUserCenterClick, popMenuClz, Context.class, View.class);
            } catch (Throwable ignored) {}

            Class<?> hClz = session.findOptionalClass("com.android.browser.view.h");
            if (hClz != null) {
                try {
                    session.hook(hClz, "onClick", chain -> {
                        Object hObj = chain.getThisObject();
                        Object popMenu = HookValues.readField(hObj, "n");
                        Object ctxObj = HookValues.readField(hObj, "o");
                        Context ctx = (ctxObj instanceof Context) ? (Context) ctxObj : null;
                        handlePopMenuUserCenterClick(session, popMenu, ctx);
                        return null;
                    }, View.class);
                } catch (Throwable ignored) {}
            }
        }

        // Fix settings icon on the right side of personal/user center interface (MineView)
        Class<?> mineViewClz = session.findOptionalClass("cn.nubia.usercenter.view.MineView");
        if (mineViewClz != null) {
            HookSession.HookCallback openSettings = chain -> {
                try {
                    Object mineView = chain.getArgs().size() > 0 ? chain.getArgs().get(0) : null;
                    Activity act = null;
                    if (mineView != null) {
                        Object n = HookValues.readField(mineView, "n");
                        if (n instanceof Activity) act = (Activity) n;
                    }
                    if (act == null && chain.getArgs().size() > 1 && chain.getArgs().get(1) instanceof View) {
                        Context ctx = ((View) chain.getArgs().get(1)).getContext();
                        if (ctx instanceof Activity) act = (Activity) ctx;
                    }
                    if (act != null) {
                        Class<?> settingAct = session.findOptionalClass("com.android.browser.settings.BrowserSettingActivity");
                        if (settingAct != null) {
                            Intent intent = new Intent(act, settingAct);
                            act.startActivity(intent);
                            return null;
                        }
                    }
                } catch (Throwable t) {
                    session.log("MINEVIEW_SETTING_LAUNCH_ERROR: " + t.getMessage());
                }
                return chain.proceed();
            };

            try {
                session.hook(mineViewClz, "r", openSettings, mineViewClz, View.class);
            } catch (Throwable ignored) {}
            try {
                session.hook(mineViewClz, "c", openSettings, mineViewClz, View.class);
            } catch (Throwable ignored) {}

            try {
                session.hook(mineViewClz, "q", chain -> {
                    Object res = chain.proceed();
                    try {
                        Object mineView = chain.getThisObject();
                        if (mineView instanceof View) {
                            View root = (View) mineView;
                            View settingIv = HookValues.findView(root, session.packageName(), "settingIv");
                            if (settingIv != null) {
                                settingIv.setOnClickListener(v -> {
                                    Context ctx = v.getContext();
                                    Activity act = (ctx instanceof Activity) ? (Activity) ctx : null;
                                    if (act == null) {
                                        Object n = HookValues.readField(mineView, "n");
                                        if (n instanceof Activity) act = (Activity) n;
                                    }
                                    if (act != null) {
                                        Class<?> settingAct = session.findOptionalClass("com.android.browser.settings.BrowserSettingActivity");
                                        if (settingAct != null) {
                                            Intent intent = new Intent(act, settingAct);
                                            act.startActivity(intent);
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Throwable t) {
                        session.log("MINEVIEW_Q_REBIND_ERROR: " + t.getMessage());
                    }
                    return res;
                });
            } catch (Throwable ignored) {}
        }

        Class<?> veClz = session.findOptionalClass("v.e");
        if (veClz != null) {
            try {
                session.hook(veClz, "onClick", chain -> {
                    try {
                        Object veObj = chain.getThisObject();
                        Object mineView = HookValues.readField(veObj, "n");
                        Activity act = null;
                        if (mineView != null) {
                            Object n = HookValues.readField(mineView, "n");
                            if (n instanceof Activity) act = (Activity) n;
                        }
                        if (act == null && chain.getArgs().size() > 0 && chain.getArgs().get(0) instanceof View) {
                            Context ctx = ((View) chain.getArgs().get(0)).getContext();
                            if (ctx instanceof Activity) act = (Activity) ctx;
                        }
                        if (act != null) {
                            Class<?> settingAct = session.findOptionalClass("com.android.browser.settings.BrowserSettingActivity");
                            if (settingAct != null) {
                                Intent intent = new Intent(act, settingAct);
                                act.startActivity(intent);
                                return null;
                            }
                        }
                    } catch (Throwable t) {
                        session.log("VE_ONCLICK_HOOK_ERROR: " + t.getMessage());
                    }
                    return chain.proceed();
                }, View.class);
            } catch (Throwable ignored) {}
        }
    }

    private static void installAdaptiveNavigationButtons(HookSession session) {
        Class<?> bottomBar = session.findOptionalClass("com.android.browser.BottomBar");
        if (bottomBar != null) {
            HookSession.HookCallback updateNav = chain -> {
                Object result = chain.proceed();
                updateBottomNavVisibility(chain.getThisObject());
                return result;
            };

            session.hook(bottomBar, "c0", updateNav);
            session.hook(bottomBar, "w0", updateNav);
            session.hook(bottomBar, "G0", updateNav, boolean.class, boolean.class);
            session.hook(bottomBar, "K0", updateNav, boolean.class, boolean.class, boolean.class);
            session.hook(bottomBar, "E0", updateNav, boolean.class, boolean.class);
        }

        Class<?> baseUi = session.findOptionalClass("com.android.browser.BaseUi");
        if (baseUi != null) {
            HookSession.HookCallback updateFromUi = chain -> {
                Object result = chain.proceed();
                Object ui = chain.getThisObject();
                if (ui != null) {
                    Object bb = HookValues.readField(ui, "u");
                    if (bb != null) {
                        updateBottomNavVisibility(bb);
                    }
                }
                return result;
            };

            session.hook(baseUi, "i2", updateFromUi);
            session.hook(baseUi, "H", updateFromUi, boolean.class);
        }

        Class<?> phoneUi = session.findOptionalClass("com.android.browser.PhoneUi");
        if (phoneUi != null) {
            Class<?> tabClz = session.findOptionalClass("com.android.browser.Tab");
            if (tabClz != null) {
                session.hook(phoneUi, "p2", chain -> {
                    Object result = chain.proceed();
                    Object ui = chain.getThisObject();
                    if (ui != null) {
                        Object bb = HookValues.readField(ui, "u");
                        if (bb != null) {
                            updateBottomNavVisibility(bb);
                        }
                    }
                    return result;
                }, tabClz);
            }
        }
        session.log("BROWSER_ADAPTIVE_NAV_BUTTONS_INSTALLED");
    }

    private static void updateBottomNavVisibility(Object bottomBarObj) {
        if (!(bottomBarObj instanceof View)) return;
        View bottomBar = (View) bottomBarObj;
        applyBottomNavVisibility(bottomBar);
        bottomBar.post(() -> applyBottomNavVisibility(bottomBar));
    }

    private static void applyBottomNavVisibility(View bottomBar) {
        try {
            boolean isWeb = isBrowsingWebPage(bottomBar);
            int target = isWeb ? View.VISIBLE : View.GONE;

            String pkg = bottomBar.getContext().getPackageName();
            int backId = bottomBar.getResources().getIdentifier("cont_backward", "id", pkg);
            int forwardId = bottomBar.getResources().getIdentifier("cont_forward", "id", pkg);
            int commentBackId = bottomBar.getResources().getIdentifier("comment_cont_backward", "id", pkg);
            int commentForwardId = bottomBar.getResources().getIdentifier("comment_cont_forward", "id", pkg);

            int[] ids = { backId, forwardId, commentBackId, commentForwardId };
            for (int id : ids) {
                if (id != 0) {
                    View v = bottomBar.findViewById(id);
                    if (v != null && v.getVisibility() != target) {
                        v.setVisibility(target);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isBrowsingWebPage(Object bottomBarObj) {
        if (bottomBarObj == null) return false;
        try {
            Object baseUi = HookValues.readField(bottomBarObj, "o");
            if (baseUi != null) {
                Method sMethod = HookValues.findNoArgMethod(baseUi, "S");
                if (sMethod != null) {
                    Object isHome = sMethod.invoke(baseUi);
                    if (Boolean.TRUE.equals(isHome)) {
                        return false;
                    }
                }
            }

            Object tab = null;
            if (baseUi != null) {
                tab = HookValues.readField(baseUi, "d");
            }
            if (tab == null) {
                Object controller = HookValues.readField(bottomBarObj, "n");
                if (controller != null) {
                    Method q0 = HookValues.findNoArgMethod(controller, "q0");
                    if (q0 != null) {
                        tab = q0.invoke(controller);
                    }
                }
            }

            if (tab == null) return false;

            Object c0 = HookValues.readField(tab, "c0");
            if (c0 != null) {
                String pageType = c0.toString().trim();
                if ("home".equalsIgnoreCase(pageType) || "news".equalsIgnoreCase(pageType)) {
                    return false;
                }
                if ("web".equalsIgnoreCase(pageType)) {
                    Method getUrl = HookValues.findNoArgMethod(tab, "K0");
                    if (getUrl != null) {
                        Object urlObj = getUrl.invoke(tab);
                        if (urlObj != null) {
                            String url = urlObj.toString().trim();
                            if (url.isEmpty() || url.startsWith("file:///android_asset/html/home") || "about:blank".equalsIgnoreCase(url)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }

            Method i1Method = HookValues.findNoArgMethod(tab, "i1");
            if (i1Method != null) {
                Object isWeb = i1Method.invoke(tab);
                if (Boolean.FALSE.equals(isWeb)) {
                    return false;
                }
            }

            Method getUrl = HookValues.findNoArgMethod(tab, "K0");
            if (getUrl != null) {
                Object urlObj = getUrl.invoke(tab);
                if (urlObj != null) {
                    String url = urlObj.toString().trim();
                    if (!url.isEmpty() && !url.startsWith("file:///android_asset/html/home") && !"about:blank".equalsIgnoreCase(url)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void installMineSlimming(HookSession session) {
        Class<?> mineView = session.findClass("cn.nubia.usercenter.view.MineView");
        session.hook(mineView, "n", chain -> {
            Object result = chain.proceed();
            enforceMinePolicy(session, chain.getThisObject(), true);
            return result;
        });
        session.hook(mineView, "o", chain -> {
            Object result = chain.proceed();
            enforceMinePolicy(session, chain.getThisObject(), true);
            return result;
        });
        session.hook(mineView, "m", chain -> {
            enforceMinePolicy(session, chain.getThisObject(), true);
            return chain.proceed();
        });
        session.hook(mineView, "onDetachedFromWindow", chain -> {
            Object result = chain.proceed();
            enforceMinePolicy(session, chain.getThisObject(), true);
            return result;
        });
    }

    private static void enforceMinePolicy(
            HookSession session,
            Object owner,
            boolean stopBanner
    ) {
        if (!(owner instanceof View)) {
            session.missing("view=cn.nubia.usercenter.view.MineView");
            return;
        }

        Object binding = findMineBinding(session, owner);
        View fallbackRoot = (View) owner;
        View root = findBindingRoot(session, binding, fallbackRoot);
        if (root == null) {
            session.missing("viewbinding=cn.nubia.browser.databinding.ViewMineBinding#getRoot()");
            return;
        }

        HookValues.runOnMain(root, () -> {
            for (String name : MINE_HIDDEN_IDS) {
                View view = HookValues.findView(root, session.packageName(), name);
                if (view == null) {
                    session.missing("resource=id/" + name);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
            if (stopBanner) {
                stopMineBanner(session, binding, root);
            }
        });
    }

    private static Object findMineBinding(HookSession session, Object owner) {
        Field field = HookValues.findField(owner, "q", "binding", "mBinding");
        if (field == null) {
            session.missing("field=cn.nubia.usercenter.view.MineView#binding");
            return null;
        }
        try {
            Object binding = field.get(owner);
            if (binding == null) {
                session.missing("field=cn.nubia.usercenter.view.MineView#binding value");
            }
            return binding;
        } catch (Throwable error) {
            session.missing("field=cn.nubia.usercenter.view.MineView#binding read failed "
                    + error.getClass().getSimpleName());
            return null;
        }
    }

    private static View findBindingRoot(
            HookSession session,
            Object binding,
            View fallback
    ) {
        if (binding == null) {
            return fallback;
        }
        Method getRoot = HookValues.findNoArgMethod(binding, "getRoot");
        if (getRoot == null) {
            session.missing("method=" + binding.getClass().getName() + "#getRoot()");
            return fallback;
        }
        try {
            Object root = getRoot.invoke(binding);
            if (root instanceof View) {
                return (View) root;
            }
            session.missing("method=" + binding.getClass().getName()
                    + "#getRoot() returned non-view");
        } catch (Throwable error) {
            session.missing("method=" + binding.getClass().getName()
                    + "#getRoot() invoke failed " + error.getClass().getSimpleName());
        }
        return fallback;
    }

    private static void stopMineBanner(HookSession session, Object binding, View root) {
        Object banner = binding == null ? null : HookValues.readField(
                binding, "b", "banner", "mBanner");
        if (banner == null) {
            banner = HookValues.findView(root, session.packageName(), "banner");
        }
        if (banner == null) {
            session.missing("resource=id/banner");
            return;
        }

        Method stop = HookValues.findNoArgMethod(banner, "stop");
        if (stop == null) {
            session.missing("method=" + banner.getClass().getName() + "#stop()");
            return;
        }
        try {
            stop.invoke(banner);
        } catch (Throwable error) {
            session.log("BROWSER_BANNER_STOP_FAILED "
                    + error.getClass().getSimpleName());
        }
    }

    private static void installSearchCleaning(HookSession session) {
        installSearchAdRemoval(session);
        installHomeHotNewsRemoval(session);
        // The search page can recreate the title row after all view IDs have
        // already been cleaned. Remove the complete block at the moment its
        // text is bound, so only an orphaned “今日热榜” label cannot remain.
        session.hook(TextView.class, "setText", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof TextView) {
                CharSequence textCs = ((TextView) owner).getText();
                String text = textCs == null ? "" : textCs.toString();
                if (isSearchHotNewsLabel(text)) {
                    hideSearchHotNewsBlock((View) owner);
                } else if (isSearchActiveLabel(text)) {
                    hideActiveBlock((View) owner);
                }
            }
            return result;
        }, CharSequence.class);
        session.hook(TextView.class, "setText", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof TextView) {
                CharSequence textCs = ((TextView) owner).getText();
                String text = textCs == null ? "" : textCs.toString();
                if (isSearchHotNewsLabel(text)) {
                    hideSearchHotNewsBlock((View) owner);
                } else if (isSearchActiveLabel(text)) {
                    hideActiveBlock((View) owner);
                }
            }
            return result;
        }, int.class);
        session.hook(ImageView.class, "setVisibility", chain -> {
            Object owner = chain.getThisObject();
            if (owner instanceof View
                    && "ivAction".equals(HookValues.resourceEntryName((View) owner))) {
                return HookValues.proceedWithArgument(chain, 0, View.GONE);
            }
            return chain.proceed();
        }, int.class);
        Class<?> bannerView = session.findClass("com.android.browser.view.BannerView");
        final Class<?> bannerViewType = bannerView;
        session.hook(bannerView, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            if (bannerViewType != null && bannerViewType.isInstance(chain.getThisObject())
                    && chain.getThisObject() instanceof View) {
                View v = (View) chain.getThisObject();
                makeEmptyPreserveHeight(v);
            }
            return result;
        });
        session.hook(bannerView, "onLayout", chain -> {
            Object result = chain.proceed();
            if (bannerViewType != null && bannerViewType.isInstance(chain.getThisObject())
                    && chain.getThisObject() instanceof View) {
                View v = (View) chain.getThisObject();
                makeEmptyPreserveHeight(v);
            }
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);
        session.hook(bannerView, "setVisibility", chain -> {
            if (bannerViewType != null && bannerViewType.isInstance(chain.getThisObject())) {
                return HookValues.proceedWithArgument(chain, 0, View.INVISIBLE);
            }
            return chain.proceed();
        }, int.class);
        Class<?> seekView = session.findClass("com.android.browser.ui.SeekView");
        final Class<?> seekViewType = seekView;
        session.hook(seekView, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (seekViewType != null && seekViewType.isInstance(owner)
                    && owner instanceof View) {
                cleanSearchViews(session, (View) owner);
            }
            return result;
        });
        session.hook(seekView, "onLayout", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (seekViewType != null && seekViewType.isInstance(owner)
                    && owner instanceof View) {
                cleanSearchViews(session, (View) owner);
            }
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);
        Class<?> searchActivity = session.findClass("com.android.browser.SearchActivity");
        session.hook(searchActivity, "onCreate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof Activity) {
                View decor = ((Activity) owner).getWindow().getDecorView();
                cleanSearchViews(session, decor);
                scheduleSearchDynamicCleanup(session, decor);
                if (session.config().browserRemoveHomePageInfo()) {
                    scheduleHomeHotNewsCleanup(session, decor);
                }
            }
            return result;
        }, Bundle.class);

        Class<?> searchMain = session.findClass(
                "com.android.browser.view.searchhis.SearchMainPartView");
        final Class<?> searchMainType = searchMain;
        session.hook(searchMain, "onFinishInflate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                cleanSearchViews(session, (View) owner);
            }
            return result;
        });
        session.hook(searchMain, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                hideSearchDynamicParts(session, owner);
            }
            return result;
        });
        session.hook(searchMain, "onLayout", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                hideSearchDynamicParts(session, owner);
            }
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);
        // Intercept r(Context) to hide dynamic parts immediately after setup
        session.hook(searchMain, "r", chain -> {
            Object result = chain.proceed();
            hideSearchDynamicParts(session, chain.getThisObject());
            return result;
        }, Context.class);
        // q() builds the tabs list (this.o). Clear this.o and return null so no tabs exist.
        session.hook(searchMain, "q", chain -> {
            Object owner = chain.getThisObject();
            Object o = HookValues.readField(owner, "o");
            if (o instanceof List) {
                ((List<?>) o).clear();
            }
            return null;
        });
        // p(Context) creates the ViewPager adapter. Returning null prevents ViewPager from having an adapter.
        session.hook(searchMain, "p", chain -> null, Context.class);
        // Suppress search_hotrank (今日热榜) and home_hotword tabs from ever being constructed.
        session.hook(searchMain, "v", chain -> Boolean.FALSE);
        session.hook(searchMain, "w", chain -> Boolean.FALSE);
        // B() repopulates NuHotWordsView from CacheManager every time the
        // search page is refreshed.  Suppressing the call prevents a hidden
        // view from being re-enabled after the first layout.
        session.hook(searchMain, "B", chain -> {
            hideSearchDynamicParts(session, chain.getThisObject());
            return null;
        });
        session.hook(searchMain, "setSearchSupposeData", chain -> null, List.class);

        Class<?> dataCenter = session.findOptionalClass("com.android.browser.datacenter.DataCenter");
        if (dataCenter != null) {
            session.hook(dataCenter, "isHotWordsOpen", chain -> Boolean.FALSE);
        }
        Class<?> configManager = session.findOptionalClass("com.android.browser.datacenter.base.ConfigManager");
        if (configManager != null) {
            session.hook(configManager, "getBooleanValue", chain -> {
                Object key = HookValues.arg(chain, 0);
                if ("search_hotrank".equals(key) || "home_hotword".equals(key)) {
                    return Boolean.FALSE;
                }
                return chain.proceed();
            }, String.class, boolean.class);
            session.hook(configManager, "getBooleanValue", chain -> {
                Object key = HookValues.arg(chain, 0);
                if ("search_hotrank".equals(key) || "home_hotword".equals(key)) {
                    return Boolean.FALSE;
                }
                return chain.proceed();
            }, String.class);
        }

        Class<?> hotWords = session.findClass("com.android.browser.ui.NuHotWordsView");
        final Class<?> hotWordsType = hotWords;
        session.hook(hotWords, "f", chain -> {
            hideSearchDynamicParts(session, chain.getThisObject());
            return null;
        }, List.class);
        session.hook(hotWords, "setVisibility", chain -> {
            if (hotWordsType == null
                    || !hotWordsType.isInstance(chain.getThisObject())) {
                return chain.proceed();
            }
            return HookValues.proceedWithArgument(chain, 0, View.GONE);
        }, int.class);
        session.hook(hotWords, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            if (hotWordsType != null
                    && hotWordsType.isInstance(chain.getThisObject())) {
                hideSearchDynamicParts(session, chain.getThisObject());
            }
            return result;
        });

        Class<?> hotNews = session.findClass(
                "com.android.browser.view.hotnews.HotNewsViewForSearch");
        final Class<?> hotNewsType = hotNews;
        // g() starts the NuContentApi request for today's hot list.
        session.hook(hotNews, "g", chain -> {
            hideSearchDynamicParts(session, chain.getThisObject());
            return null;
        });
        // setData() posts a NuUIRunnable, so blocking only the network entry
        // is insufficient when cached data arrives later.
        session.hook(hotNews, "setData", chain -> {
            hideSearchDynamicParts(session, chain.getThisObject());
            return null;
        }, List.class);
        session.hook(hotNews, "e", chain -> {
            Object result = chain.proceed();
            if (hotNewsType != null
                    && hotNewsType.isInstance(chain.getThisObject())) {
                hideSearchDynamicParts(session, chain.getThisObject());
                if (chain.getThisObject() instanceof View) {
                    ((View) chain.getThisObject()).setVisibility(View.GONE);
                }
            }
            return result;
        });
        session.hook(hotNews, "setHasView", chain -> {
            if (hotNewsType == null
                    || !hotNewsType.isInstance(chain.getThisObject())) {
                return chain.proceed();
            }
            return HookValues.proceedWithArgument(chain, 0, false);
        }, boolean.class);
        session.hook(hotNews, "h", chain -> {
            if (hotNewsType == null
                    || !hotNewsType.isInstance(chain.getThisObject())) {
                return chain.proceed();
            }
            return HookValues.proceedWithArgument(chain, 0, View.GONE);
        }, int.class);
        session.hook(hotNews, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            if (hotNewsType != null
                    && hotNewsType.isInstance(chain.getThisObject())) {
                hideSearchDynamicParts(session, chain.getThisObject());
                if (chain.getThisObject() instanceof View) {
                    ((View) chain.getThisObject()).setVisibility(View.GONE);
                }
            }
            return result;
        });
        session.hook(hotNews, "onLayout", chain -> {
            Object result = chain.proceed();
            if (hotNewsType != null
                    && hotNewsType.isInstance(chain.getThisObject())
                    && chain.getThisObject() instanceof View) {
                ((View) chain.getThisObject()).setVisibility(View.GONE);
            }
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);
        session.hook(hotNews, "setVisibility", chain -> {
            if (hotNewsType != null
                    && hotNewsType.isInstance(chain.getThisObject())) {
                return HookValues.proceedWithArgument(chain, 0, View.GONE);
            }
            return chain.proceed();
        }, int.class);

        Class<?> searchKeyWordView = session.findClass(
                "com.android.browser.view.searchnews.SearchKeyWordView");
        final Class<?> searchKeyWordViewType = searchKeyWordView;
        session.hook(searchKeyWordView, "setVisibility", chain -> {
            // setVisibility is inherited from View. HookSession can resolve
            // inherited methods, so guard the receiver to avoid hiding every
            // View in the browser process.
            if (searchKeyWordViewType == null
                    || !searchKeyWordViewType.isInstance(chain.getThisObject())) {
                return chain.proceed();
            }
            return HookValues.proceedWithArgument(chain, 0, View.GONE);
        }, int.class);
    }

    /**
     * The launch race can leave the home implementation's hot-news widget
     * attached for one frame even after the bottom search tab is selected.
     * This is a different class from HotNewsViewForSearch and was the reason
     * the visible “今日热榜” survived the existing search hooks.
     */
    private static void installHomeHotNewsRemoval(HookSession session) {
        Class<?> hotNewsHome = session.findClass(
                "com.android.browser.view.hotnews.HotNewsViewForHome");
        final Class<?> hotNewsHomeType = hotNewsHome;
        session.hook(hotNewsHome, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            if (hotNewsHomeType != null
                    && hotNewsHomeType.isInstance(chain.getThisObject())) {
                hideSearchDynamicParts(session, chain.getThisObject());
            }
            return result;
        });
        session.hook(hotNewsHome, "setVisibility", chain -> {
            if (hotNewsHomeType == null
                    || !hotNewsHomeType.isInstance(chain.getThisObject())) {
                return chain.proceed();
            }
            return HookValues.proceedWithArgument(chain, 0, View.GONE);
        }, int.class);
        session.hook(hotNewsHome, "setData", chain -> {
            hideSearchDynamicParts(session, chain.getThisObject());
            return null;
        }, List.class);
        session.log("BROWSER_HOME_HOT_NEWS_DISABLED");
    }

    /** Block the browser's search suggestion ad requests and hide rows that
     * were already returned by a request started before the hook ran. */
    private static void installSearchAdRemoval(HookSession session) {
        Class<?> adManager = session.findClass(
                "com.android.browser.ad.search.SearchAdManager");
        session.hook(adManager, "o", chain -> null);
        session.hook(adManager, "s", chain -> null);
        session.hook(adManager, "w", chain -> null);
        session.hook(adManager, "y", chain -> null);
        session.hook(adManager, "j", chain -> null);

        Class<?> suggestions = session.findClass("com.android.browser.SuggestionsAdapter");
        final Class<?> suggestionsType = suggestions;
        session.hook(suggestions, "getView", chain -> {
            Object result = chain.proceed();
            if (suggestionsType == null
                    || !suggestionsType.isInstance(chain.getThisObject())) {
                return result;
            }
            Object item = HookValues.invoke(
                    chain.getThisObject(), "L", HookValues.intArg(chain, 0, -1));
            if (isSearchAdItem(item)) {
                hideSearchAdRow(result);
            } else {
                restoreSearchRow(result);
            }
            return result;
        }, int.class, View.class, ViewGroup.class);
        session.hook(suggestions, "o0", chain -> {
            Object result = chain.proceed();
            Object item = HookValues.arg(chain, 4);
            if (isSearchAdItem(item)) {
                hideSearchAdRow(HookValues.arg(chain, 0));
            }
            return result;
        });
        Class<?> downloadAdManager = session.findOptionalClass(
                "com.android.browser.ad.download.DownloadAdManager");
        if (downloadAdManager != null) {
            session.hook(downloadAdManager, "o", chain -> null);
            session.hook(downloadAdManager, "p", chain -> null);
        }
        session.log("BROWSER_SEARCH_ADS_DISABLED");
    }

    private static boolean isSearchAdItem(Object item) {
        return item != null
                && (HookValues.invokeBoolean(item, "isSearchAd")
                || HookValues.invokeBoolean(item, "isNativeAd"));
    }

    private static void hideSearchAdRow(Object row) {
        if (!(row instanceof View)) {
            return;
        }
        View view = (View) row;
        view.setVisibility(View.GONE);
        LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = 0;
            view.setLayoutParams(params);
        }
    }

    private static void restoreSearchRow(Object row) {
        if (!(row instanceof View)) {
            return;
        }
        View view = (View) row;
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
        }
        LayoutParams params = view.getLayoutParams();
        if (params != null && params.height == 0) {
            params.height = LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(params);
        }
    }

    private static void installSearchShortcutRemoval(HookSession session) {
        session.hook(TextView.class, "setText", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof TextView) {
                hideShortcutForLabel((TextView) owner);
            }
            return result;
        }, CharSequence.class);
        session.hook(TextView.class, "setText", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof TextView) {
                hideShortcutForLabel((TextView) owner);
            }
            return result;
        }, int.class);

        Class<?> launcher = session.findClass("com.android.browser.BrowserLauncher");
        if (launcher != null) {
            session.hook(launcher, "onCreate", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (owner instanceof Activity) {
                    scheduleSearchShortcutCleanup(
                            session, ((Activity) owner).getWindow().getDecorView());
                }
                return result;
            }, Bundle.class);
            session.hook(launcher, "onWindowFocusChanged", chain -> {
                Object result = chain.proceed();
                if (Boolean.TRUE.equals(HookValues.arg(chain, 0))
                        && chain.getThisObject() instanceof Activity) {
                    Activity activity = (Activity) chain.getThisObject();
                    scheduleSearchShortcutCleanup(
                            session, activity.getWindow().getDecorView());
                }
                return result;
            }, boolean.class);
        }

        Class<?> seekView = session.findClass("com.android.browser.ui.SeekView");
        final Class<?> seekViewType = seekView;
        if (seekView != null) {
            session.hook(seekView, "onAttachedToWindow", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (seekViewType != null && seekViewType.isInstance(owner)
                        && owner instanceof View) {
                    hideSearchShortcuts(session, (View) owner);
                    scheduleSearchShortcutCleanup(session, (View) owner);
                }
                return result;
            });
            session.hook(seekView, "onLayout", chain -> {
                Object result = chain.proceed();
                Object owner = chain.getThisObject();
                if (seekViewType != null && seekViewType.isInstance(owner)
                        && owner instanceof View) {
                    hideSearchShortcuts(session, (View) owner);
                }
                return result;
            }, boolean.class, int.class, int.class, int.class, int.class);
        }

        Class<?> seekScrollListener = session.findClass("com.android.browser.ui.SeekView$2");
        if (seekScrollListener != null) {
            session.hook(seekScrollListener, "onScrolled", chain -> {
                return null;
            });
        }

        Class<?> searchActivity = session.findClass("com.android.browser.SearchActivity");
        session.hook(searchActivity, "onCreate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (owner instanceof Activity) {
                scheduleSearchShortcutCleanup(
                        session, ((Activity) owner).getWindow().getDecorView());
            }
            return result;
        }, Bundle.class);

        Class<?> searchMain = session.findClass(
                "com.android.browser.view.searchhis.SearchMainPartView");
        final Class<?> searchMainType = searchMain;
        session.hook(searchMain, "onFinishInflate", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                scheduleSearchShortcutCleanup(session, (View) owner);
            }
            return result;
        });
        session.hook(searchMain, "onAttachedToWindow", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                scheduleSearchShortcutCleanup(session, (View) owner);
            }
            return result;
        });
        session.hook(searchMain, "onLayout", chain -> {
            Object result = chain.proceed();
            Object owner = chain.getThisObject();
            if (searchMainType != null
                    && searchMainType.isInstance(owner)
                    && owner instanceof View) {
                scheduleSearchShortcutCleanup(session, (View) owner);
            }
            return result;
        }, boolean.class, int.class, int.class, int.class, int.class);
    }

    private static boolean collapseAndHideView(View view) {
        if (view == null) {
            return false;
        }
        boolean modified = false;
        try {
            if (view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
                modified = true;
            }
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && (lp.height != 0 || lp.width != 0)) {
                lp.height = 0;
                lp.width = 0;
                view.setLayoutParams(lp);
                modified = true;
            }
            if (view instanceof ImageView) {
                ImageView iv = (ImageView) view;
                if (iv.getDrawable() != null) {
                    iv.setImageDrawable(null);
                    modified = true;
                }
            }
        } catch (Throwable ignored) {
        }
        return modified;
    }

    private static void scheduleSearchShortcutCleanup(HookSession session, View root) {
        if (root == null) {
            return;
        }
        // Shortcut labels arrive from the browser's async data callback. Use
        // only a few attempts; repeatedly hiding the same rows every 100 ms
        // causes visible relayout/jitter during browser startup.
        for (int attempt = 0; attempt <= 4; attempt++) {
            final long delay = attempt * 120L;
            root.postDelayed(() -> hideSearchShortcuts(session, root), delay);
        }
    }

    private static void hideSearchShortcuts(HookSession session, View root) {
        if (root == null) {
            return;
        }
        HookValues.runOnMain(root, () -> {
            Set<View> hidden = new HashSet<>();
            for (String name : SEARCH_SHORTCUT_VIEW_IDS) {
                View target = HookValues.findView(root, session.packageName(), name);
                if (target != null && collapseAndHideView(target)) {
                    hidden.add(target);
                }
            }
            collectSearchShortcuts(root, root, hidden);
            if (!hidden.isEmpty()) {
                session.log("BROWSER_SEARCH_SHORTCUTS_HIDDEN count=" + hidden.size());
            }
        });
    }

    private static void collectSearchShortcuts(View view, View root, Set<View> hidden) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (isSearchShortcutLabel(text == null ? null : text.toString().trim())) {
                View container = shortcutContainer(view, root);
            if (container != null) {
                    if (container.getVisibility() != View.GONE) {
                        container.setVisibility(View.GONE);
                        hidden.add(container);
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectSearchShortcuts(group.getChildAt(i), root, hidden);
            }
        }
    }

    private static View shortcutContainer(View textView, View root) {
        View current = textView;
        View best = null;
        for (int level = 0; level < 3; level++) {
            if (!(current.getParent() instanceof View)) {
                break;
            }
            View parent = (View) current.getParent();
            if (parent == root) {
                break;
            }
            current = parent;
            if (current instanceof ViewGroup && hasSearchInputChild((ViewGroup) current)) {
                // NEVER hide a container that houses search views/bars
                break;
            }
            String name = HookValues.resourceEntryName(current);
            if ("container".equals(name)
                    || (current instanceof ViewGroup
                    && ((ViewGroup) current).getChildCount() <= 4)) {
                best = current;
            }
            if ("container".equals(name)) {
                break;
            }
        }
        return best;
    }

    private static boolean hasSearchInputChild(ViewGroup vg) {
        if (vg == null) return false;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof android.widget.EditText) {
                return true;
            }
            String name = HookValues.resourceEntryName(child);
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("search") || lower.contains("seek")) {
                    return true;
                }
            }
            if (child instanceof ViewGroup && hasSearchInputChild((ViewGroup) child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSearchShortcutLabel(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String label : SEARCH_SHORTCUT_LABELS) {
            if (label.equals(text)) {
                return true;
            }
        }
        return false;
    }

    private static void hideShortcutForLabel(TextView textView) {
        String text = String.valueOf(textView.getText()).trim();
        if (!isSearchShortcutLabel(text)) {
            return;
        }
        // Hide the label at bind time, before the containing shortcut row is
        // attached/drawn. The parent may be re-bound by the browser later,
        // but the label itself must not reappear for one frame.
        textView.setVisibility(View.GONE);
        View root = textView.getRootView();
        View container = shortcutContainer(textView, root);
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }

    private static void cleanSearchViews(HookSession session, View root) {
        if (root == null) {
            return;
        }
        HookValues.runOnMain(root, () -> {
            for (String name : SEARCH_CLEAN_IDS) {
                View view = HookValues.findView(root, session.packageName(), name);
                if (view != null) {
                    if (name.startsWith("active") || "banner_view".equals(name)) {
                        makeEmptyPreserveHeight(view);
                    } else {
                        if (view.getVisibility() != View.GONE) {
                            view.setVisibility(View.GONE);
                        }
                        LayoutParams lp = view.getLayoutParams();
                        if (lp != null && lp.height > 0) {
                            lp.height = 0;
                            view.setLayoutParams(lp);
                        }
                        if ("searchHisSlidingTabLayout".equals(name) && view.getParent() instanceof View) {
                            View parent = (View) view.getParent();
                            parent.setVisibility(View.GONE);
                            LayoutParams plp = parent.getLayoutParams();
                            if (plp != null && plp.height > 0) {
                                plp.height = 0;
                                parent.setLayoutParams(plp);
                            }
                        }
                    }
                }
            }
        });
    }

    private static boolean isSearchHotNewsLabel(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return "今日热榜".equals(trimmed)
                || "今日热点".equals(trimmed)
                || "Hot News".equals(trimmed)
                || "查看更多".equals(trimmed)
                || "搜索热榜".equals(trimmed);
    }

    private static boolean isSearchActiveLabel(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return "Active".equalsIgnoreCase(trimmed)
                || "今日热点 Active".equals(trimmed);
    }

    private static void hideSearchHotNewsBlock(View owner) {
        if (owner == null) {
            return;
        }
        View current = owner;
        for (int level = 0; level < 3 && current != null; level++) {
            current.setVisibility(View.GONE);
            LayoutParams lp = current.getLayoutParams();
            if (lp != null && lp.height > 0) {
                lp.height = 0;
                current.setLayoutParams(lp);
            }
            if (!(current.getParent() instanceof View)) {
                break;
            }
            View parent = (View) current.getParent();
            String parentName = HookValues.resourceEntryName(parent);
            if ("searchMainPartView".equals(parentName) || parent.getId() == android.R.id.content) {
                break;
            }
            current = parent;
        }
    }

    private static void hideActiveBlock(View owner) {
        if (owner == null) {
            return;
        }
        View current = owner;
        for (int level = 0; level < 2 && current.getParent() instanceof View; level++) {
            current = (View) current.getParent();
        }
        makeEmptyPreserveHeight(current);
    }

    private static void makeEmptyPreserveHeight(View view) {
        if (view == null) {
            return;
        }
        try {
            int currentHeight = view.getHeight();
            if (currentHeight > 0) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                if (lp != null && lp.height != currentHeight) {
                    lp.height = currentHeight;
                    view.setLayoutParams(lp);
                }
            } else {
                view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        int h = bottom - top;
                        if (h > 0) {
                            ViewGroup.LayoutParams lp = v.getLayoutParams();
                            if (lp != null && lp.height != h) {
                                lp.height = h;
                                v.setLayoutParams(lp);
                            }
                        }
                    }
                });
            }
            clearContentRecursively(view);
        } catch (Throwable ignored) {
        }
    }

    private static void clearContentRecursively(View view) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() != View.INVISIBLE) {
            view.setVisibility(View.INVISIBLE);
        }
        view.setAlpha(0f);
        view.setBackground(null);
        view.setClickable(false);
        view.setFocusable(false);
        view.setEnabled(false);
        view.setOnClickListener(null);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            vg.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearContentRecursively(vg.getChildAt(i));
            }
        }
    }

    private static void scheduleSearchDynamicCleanup(HookSession session, View root) {
        if (root == null) {
            return;
        }
        for (int attempt = 0; attempt <= 15; attempt++) {
            final long delay = attempt * 100L;
            root.postDelayed(() -> HookValues.runOnMain(root, () -> {
                cleanSearchViews(session, root);
                Set<View> hidden = new HashSet<>();
                collectHotNewsLabels(root, root, hidden);
            }), delay);
        }
    }

    private static void hideSearchDynamicParts(HookSession session, Object owner) {
        if (!(owner instanceof View)) {
            return;
        }
        View root = (View) owner;
        cleanSearchViews(session, root);
        Object hotWords = HookValues.readField(owner, "r");
        if (hotWords instanceof View) {
            ((View) hotWords).setVisibility(View.GONE);
            LayoutParams lp = ((View) hotWords).getLayoutParams();
            if (lp != null && lp.height > 0) {
                lp.height = 0;
                ((View) hotWords).setLayoutParams(lp);
            }
        }
        Object hotNews = HookValues.readField(owner, "s");
        if (hotNews instanceof View) {
            ((View) hotNews).setVisibility(View.GONE);
            LayoutParams lp = ((View) hotNews).getLayoutParams();
            if (lp != null && lp.height > 0) {
                lp.height = 0;
                ((View) hotNews).setLayoutParams(lp);
            }
        }
        Object actionIv = HookValues.readField(owner, "v");
        if (actionIv instanceof View) {
            ((View) actionIv).setVisibility(View.GONE);
        }
        Object binding = HookValues.readField(owner, "z");
        if (binding != null) {
            Object tabLayout = HookValues.readField(binding, "d", "searchHisSlidingTabLayout");
            if (tabLayout instanceof View) {
                View tabView = (View) tabLayout;
                tabView.setVisibility(View.GONE);
                if (tabView.getParent() instanceof View) {
                    View parent = (View) tabView.getParent();
                    parent.setVisibility(View.GONE);
                    LayoutParams lp = parent.getLayoutParams();
                    if (lp != null && lp.height > 0) {
                        lp.height = 0;
                        parent.setLayoutParams(lp);
                    }
                }
            }
            Object action = HookValues.readField(binding, "b", "ivAction");
            if (action instanceof View) {
                ((View) action).setVisibility(View.GONE);
            }
            Object vp = HookValues.readField(binding, "e", "searchHisVp");
            if (vp instanceof View) {
                View vpView = (View) vp;
                vpView.setVisibility(View.GONE);
                LayoutParams lp = vpView.getLayoutParams();
                if (lp != null && lp.height > 0) {
                    lp.height = 0;
                    vpView.setLayoutParams(lp);
                }
            }
        }
    }

    /**
     * The ROM can attach the hot-news header outside HotNewsViewForHome and
     * re-create it after the tab click. Resource-specific class hooks then
     * leave the header visible, so clean the complete decor tree for a short
     * bounded window around a plain launch/search transition.
     */
    private static void scheduleHomeHotNewsCleanup(HookSession session, View root) {
        if (root == null) {
            return;
        }
        for (int attempt = 0; attempt <= 15; attempt++) {
            final long delay = attempt * 100L;
            root.postDelayed(() -> hideHomeHotNews(session, root), delay);
        }
    }

    private static void hideHomeHotNews(HookSession session, View root) {
        if (root == null) {
            return;
        }
        HookValues.runOnMain(root, () -> {
            Set<View> hidden = new HashSet<>();
            for (String id : HOME_HOT_NEWS_IDS) {
                View view = HookValues.findView(root, session.packageName(), id);
                if (view != null && view.getVisibility() != View.GONE) {
                    view.setVisibility(View.GONE);
                    hidden.add(view);
                }
            }
            collectHotNewsLabels(root, root, hidden);
            if (!hidden.isEmpty()) {
                session.log("BROWSER_HOME_HOT_NEWS_VIEW_HIDDEN count=" + hidden.size());
            }
        });
    }

    private static void collectHotNewsLabels(View view, View root, Set<View> hidden) {
        if (view instanceof TextView) {
            CharSequence value = ((TextView) view).getText();
            String text = value == null ? "" : value.toString().trim();
            if (isSearchHotNewsLabel(text)) {
                hideSearchHotNewsBlock(view);
                hidden.add(view);
            } else if (isSearchActiveLabel(text)) {
                View container = view;
                for (int level = 0; level < 3
                        && container.getParent() instanceof View
                        && container != root; level++) {
                    container = (View) container.getParent();
                }
                if (container.getVisibility() != View.GONE) {
                    makeEmptyPreserveHeight(container);
                    hidden.add(container);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectHotNewsLabels(group.getChildAt(i), root, hidden);
            }
        }
    }

    private static void installDownloadHotRemoval(HookSession session) {
        Class<?> globalConfig = session.findOptionalClass("com.android.browser.configs.GlobalConfig");
        if (globalConfig != null) {
            session.hook(globalConfig, "f", chain -> Boolean.FALSE);
            session.log("BROWSER_DOWNLOAD_HOT_GLOBAL_CONFIG_DISABLED");
        }

        Class<?> appFragmentCompanion = session.findOptionalClass("cn.nubia.download.ui.AppFragment$Companion");
        if (appFragmentCompanion != null) {
            session.hook(appFragmentCompanion, "t", chain -> null, boolean.class);
            session.hook(appFragmentCompanion, "a", chain -> null, boolean.class);
            session.log("BROWSER_DOWNLOAD_APP_FRAGMENT_COMPANION_HOOKED");
        }

        Class<?> mineAdAdapter = session.findOptionalClass("cn.nubia.download.ui.adapter.MineDownloadAdAdapter");
        if (mineAdAdapter != null) {
            session.hook(mineAdAdapter, "getItemCount", chain -> 0);
        }
        Class<?> downloadAdAdapter = session.findOptionalClass("com.android.browser.ui.DownloadAdAdapter");
        if (downloadAdAdapter != null) {
            session.hook(downloadAdAdapter, "getItemCount", chain -> 0);
        }

        Class<?> downloadAdManager = session.findOptionalClass("com.android.browser.ad.download.DownloadAdManager");
        if (downloadAdManager != null) {
            Class<?> func1 = session.findOptionalClass("kotlin.jvm.functions.Function1");
            if (func1 != null) {
                session.hook(downloadAdManager, "p", chain -> null, Activity.class, func1);
            }
        }

        Class<?> downloadTab = session.findOptionalClass("com.android.browser.customdownload.ui.DownloadTab");
        if (downloadTab != null) {
            session.hook(downloadTab, "j", chain -> {
                Object listObj = HookValues.arg(chain, 0);
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    java.util.ArrayList<Object> cleaned = new java.util.ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof CharSequence) {
                            String s = item.toString();
                            if (s.contains("热榜") || s.contains("推荐") || s.contains("热门")) {
                                continue;
                            }
                        }
                        cleaned.add(item);
                    }
                    if (cleaned.isEmpty() && !list.isEmpty()) {
                        cleaned.add(list.get(0));
                    }
                    return chain.proceed(new Object[]{cleaned});
                }
                return chain.proceed();
            }, List.class);
        }
    }

    private static void installDownloadRecommendRemoval(HookSession session) {
        Class<?> configManager = session.findOptionalClass("com.android.browser.datacenter.base.ConfigManager");
        if (configManager != null) {
            session.hook(configManager, "getBooleanValue", chain -> {
                Object key = HookValues.arg(chain, 0);
                if ("app_recommend".equals(key)) {
                    session.log("BROWSER_APP_RECOMMEND_DISABLED");
                    return Boolean.FALSE;
                }
                return chain.proceed();
            }, String.class);
        }

        Class<?> adManager = session.findOptionalClass("com.huanju.ssp.sdk.AdManager");
        if (adManager != null) {
            session.hook(adManager, "getRecommendPackage", chain -> null, String.class);
            session.log("BROWSER_AD_MANAGER_RECOMMEND_PACKAGE_DISABLED");
        }

        Class<?> downloadHandler = session.findOptionalClass("com.android.browser.DownloadHandler");
        if (downloadHandler != null) {
            Class<?> commonRepoClass = session.findOptionalClass("com.huanju.ssp.base.core.sdk.CommonAd.CommonRepository");
            if (commonRepoClass != null) {
                session.hook(downloadHandler, "A", chain -> Boolean.FALSE, commonRepoClass);
                session.log("BROWSER_DOWNLOAD_HANDLER_A_DISABLED");
            }
        }

        Class<?> searchSuggestAdConfigBean = session.findOptionalClass("com.android.browser.ad.search.SearchSuggestAdConfigBean");
        if (searchSuggestAdConfigBean != null) {
            session.hook(searchSuggestAdConfigBean, "e", chain -> Boolean.FALSE);
            session.log("BROWSER_SEARCH_SUGGEST_AD_CONFIG_E_DISABLED");
        }

        Class<?> dialogClass = session.findOptionalClass("android.app.Dialog");
        if (dialogClass != null) {
            session.hook(dialogClass, "show", chain -> {
                Object res = chain.proceed();
                try {
                    Object thiz = chain.getThisObject();
                    if (thiz instanceof android.app.Dialog) {
                        android.app.Dialog dialog = (android.app.Dialog) thiz;
                        android.view.Window window = dialog.getWindow();
                        if (window != null) {
                            View decorView = window.getDecorView();
                            if (decorView != null) {
                                sanitizeDownloadDialogViews(session, decorView);
                                decorView.post(() -> sanitizeDownloadDialogViews(session, decorView));
                            }
                        }
                    }
                } catch (Throwable t) {
                    session.log("BROWSER_DIALOG_SANITIZE_ERROR " + t.getMessage());
                }
                return res;
            });
        }
    }

    private static void sanitizeDownloadDialogViews(HookSession session, View root) {
        if (root == null) return;
        HookValues.runOnMain(root, () -> {
            View recommendCl = HookValues.findView(root, session.packageName(), "recommend_download_cl");
            if (recommendCl != null && recommendCl.getVisibility() != View.GONE) {
                recommendCl.setVisibility(View.GONE);
                session.log("BROWSER_RECOMMEND_DOWNLOAD_CL_HIDDEN");
            }
            View webAdCl = HookValues.findView(root, session.packageName(), "web_ad_download_cl");
            if (webAdCl != null && webAdCl.getVisibility() != View.GONE) {
                webAdCl.setVisibility(View.GONE);
                session.log("BROWSER_WEB_AD_DOWNLOAD_CL_HIDDEN");
            }
            View webDownloadCl = HookValues.findView(root, session.packageName(), "web_download_cl");
            if (webDownloadCl != null) {
                ViewGroup.LayoutParams lp = webDownloadCl.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    if (mlp.topMargin > 0) {
                        mlp.topMargin = 0;
                        webDownloadCl.setLayoutParams(mlp);
                    }
                }
            }
        });
    }

    private static void preloadAccountInfo(HookSession session) {
        try {
            Class<?> amClz = session.findOptionalClass("cn.nubia.account.AccountManager");
            if (amClz != null) {
                Object am = invokeStatic(amClz, "I");
                if (am != null) {
                    HookValues.invokeNoArg(am, "y");
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isAccountLoggedIn(HookSession session) {
        return isAccountLoggedIn(session, getAccountManager(session));
    }

    private static boolean isAccountLoggedIn(HookSession session, Object accountManager) {
        Object login = invokeNoArgSafely(accountManager, "C");
        return Boolean.TRUE.equals(login) || hasAccountIdentity(session, accountManager);
    }

    private static Object getAccountManager(HookSession session) {
        try {
            Class<?> amClz = session.findOptionalClass("cn.nubia.account.AccountManager");
            return amClz == null ? null : invokeStatic(amClz, "I");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasAccountIdentity(HookSession session, Object accountManager) {
        if (hasText(invokeNoArgSafely(accountManager, "N"))
                || hasText(invokeNoArgSafely(accountManager, "L"))) {
            return true;
        }

        try {
            Class<?> dcClz = session.findOptionalClass(
                    "com.android.browser.datacenter.DataCenter");
            if (dcClz != null) {
                Object dc = invokeStatic(dcClz, "getInstance");
                return hasText(invokeNoArgSafely(dc, "getAccountUid"));
            }
        } catch (Throwable ignored) {}
        return false;
    }

    static Object invokeStatic(Class<?> owner, String methodName, Object... args)
            throws ReflectiveOperationException {
        if (owner == null) {
            return null;
        }
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] == null ? null : args[i].getClass();
        }
        Method method = HookValues.findMethod(owner, methodName, parameterTypes);
        if (method == null || !Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        return method.invoke(null, args);
    }

    private static Object invokeNoArgSafely(Object owner, String methodName) {
        try {
            return HookValues.invokeNoArg(owner, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasText(Object value) {
        return value instanceof String && !((String) value).trim().isEmpty();
    }

    private static Activity findActivity(Context context, Object popMenu) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        Context cur = context;
        while (cur instanceof ContextWrapper) {
            if (cur instanceof Activity) {
                return (Activity) cur;
            }
            cur = ((ContextWrapper) cur).getBaseContext();
        }
        if (popMenu != null) {
            Object h = HookValues.readField(popMenu, "H");
            if (h instanceof Activity) {
                return (Activity) h;
            }
            if (h != null) {
                try {
                    Object act = HookValues.invokeNoArg(h, "getActivity");
                    if (act instanceof Activity) {
                        return (Activity) act;
                    }
                } catch (Throwable ignored) {}
            }
            Object c = HookValues.readField(popMenu, "C");
            if (c instanceof Activity) {
                return (Activity) c;
            }
            if (c instanceof ContextWrapper) {
                cur = (Context) c;
                while (cur instanceof ContextWrapper) {
                    if (cur instanceof Activity) {
                        return (Activity) cur;
                    }
                    cur = ((ContextWrapper) cur).getBaseContext();
                }
            }
        }
        return null;
    }

    private static void updatePopMenuAccountUi(HookSession session, Object popMenu) {
        if (popMenu == null) return;
        try {
            Object am = getAccountManager(session);
            boolean isLogin = isAccountLoggedIn(session, am);
            String nickname = null;
            Bitmap avatarBitmap = null;

            if (am != null) {
                Object nicknameValue = invokeNoArgSafely(am, "N");
                if (!hasText(nicknameValue)) {
                    nicknameValue = invokeNoArgSafely(am, "L");
                }
                nickname = nicknameValue instanceof String ? (String) nicknameValue : null;
                Object bmpObj = invokeNoArgSafely(am, "M");
                if (bmpObj instanceof Bitmap) {
                    Bitmap b = (Bitmap) bmpObj;
                    if (!b.isRecycled() && b.getWidth() > 0 && b.getHeight() > 0) {
                        avatarBitmap = b;
                    }
                }
            }

            if (nickname != null && !nickname.trim().isEmpty()) {
                isLogin = true;
            } else if (isLogin) {
                nickname = "中兴用户";
            }

            TextView usercenterName = null;
            ImageView usercenterHeadImage = null;
            View usercenterLayout = null;

            Object n0 = HookValues.readField(popMenu, "n0");
            if (n0 instanceof View) {
                View root = (View) n0;
                usercenterName = (TextView) HookValues.findView(root, session.packageName(), "usercenter_name");
                usercenterHeadImage = (ImageView) HookValues.findView(root, session.packageName(), "usercenter_head_image");
                usercenterLayout = HookValues.findView(root, session.packageName(), "usercenter_layout");
            }
            if (usercenterName == null) {
                Object f0 = HookValues.readField(popMenu, "f0");
                if (f0 instanceof TextView) usercenterName = (TextView) f0;
            }
            if (usercenterHeadImage == null) {
                Object z = HookValues.readField(popMenu, "Z");
                if (z instanceof ImageView) usercenterHeadImage = (ImageView) z;
            }
            if (usercenterLayout == null) {
                Object g0 = HookValues.readField(popMenu, "g0");
                if (g0 instanceof View) usercenterLayout = (View) g0;
            }

            final boolean finalIsLogin = isLogin;
            final String finalNickname = nickname;
            final Bitmap finalAvatar = avatarBitmap;

            if (usercenterName != null) {
                final TextView tv = usercenterName;
                tv.post(() -> {
                    try {
                        Context ctx = tv.getContext();
                        int nightMode = ctx.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                        boolean isDark = (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

                        if (finalIsLogin) {
                            tv.setText(finalNickname != null ? finalNickname : "中兴用户");
                            boolean colorSet = false;
                            try {
                                Class<?> nuThemeHelper = session.findOptionalClass("com.android.browser.ui.helper.NuThemeHelper");
                                int colorLoginRes = ctx.getResources().getIdentifier("browser_popmenu_user_center_color_login", "color", ctx.getPackageName());
                                if (colorLoginRes != 0 && nuThemeHelper != null) {
                                    Integer color = (Integer) invokeStatic(
                                            nuThemeHelper, "b", colorLoginRes);
                                    if (color != null) {
                                        tv.setTextColor(color);
                                        colorSet = true;
                                    }
                                }
                            } catch (Throwable ignored) {}
                            if (!colorSet) {
                                tv.setTextColor(isDark ? 0xE6FFFFFF : 0xDE000000);
                            }
                        } else {
                            int logoutTextRes = ctx.getResources().getIdentifier("user_name_logout_defalut", "string", ctx.getPackageName());
                            if (logoutTextRes != 0) {
                                tv.setText(logoutTextRes);
                            } else {
                                tv.setText("未登录");
                            }
                            boolean colorSet = false;
                            try {
                                Class<?> nuThemeHelper = session.findOptionalClass("com.android.browser.ui.helper.NuThemeHelper");
                                int colorUnloginRes = ctx.getResources().getIdentifier("browser_popmenu_user_center_color_unlogin", "color", ctx.getPackageName());
                                if (colorUnloginRes != 0 && nuThemeHelper != null) {
                                    Integer color = (Integer) invokeStatic(
                                            nuThemeHelper, "b", colorUnloginRes);
                                    if (color != null) {
                                        tv.setTextColor(color);
                                        colorSet = true;
                                    }
                                }
                            } catch (Throwable ignored) {}
                            if (!colorSet) {
                                tv.setTextColor(isDark ? 0x80FFFFFF : 0x66000000);
                            }
                        }
                    } catch (Throwable ignored) {}
                });
            }

            if (usercenterHeadImage != null) {
                final ImageView iv = usercenterHeadImage;
                iv.post(() -> {
                    try {
                        if (finalIsLogin && finalAvatar != null) {
                            iv.setImageBitmap(finalAvatar);
                        } else {
                            Context ctx = iv.getContext();
                            int defaultAvatarRes = ctx.getResources().getIdentifier("user_avatar_default", "drawable", ctx.getPackageName());
                            if (defaultAvatarRes != 0) {
                                iv.setImageResource(defaultAvatarRes);
                            }
                        }
                    } catch (Throwable ignored) {}
                });
            }

            if (usercenterLayout != null) {
                final View ucl = usercenterLayout;
                ucl.post(() -> {
                    try {
                        ucl.setOnClickListener(v -> handlePopMenuUserCenterClick(session, popMenu, v.getContext()));
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable t) {
            session.log("UPDATE_POPMENU_UI_ERROR: " + t.getMessage());
        }
    }

    private static void handlePopMenuUserCenterClick(HookSession session, Object popMenu, Context context) {
        try {
            if (popMenu != null) {
                try {
                    HookValues.invokeNoArg(popMenu, "n");
                } catch (Throwable ignored) {}
            }

            Activity act = findActivity(context, popMenu);
            if (act == null) {
                session.log("POPMENU_CLICK_NO_ACTIVITY");
                return;
            }

            boolean isLogin = isAccountLoggedIn(session);
            if (isLogin) {
                try {
                    Intent intent = new Intent();
                    intent.setClassName(act.getPackageName(), "cn.nubia.usercenter.UserCenterActivity");
                    intent.putExtra("from", "bottomMenu");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(intent);
                } catch (Throwable t) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName(act.getPackageName(), "cn.nubia.account.OldUserCenterActivity");
                        intent.putExtra("from", "bottomMenu");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(intent);
                    } catch (Throwable t2) {
                        session.log("OPEN_USER_CENTER_FAILED: " + t2.getMessage());
                    }
                }
            } else {
                Class<?> amClz = session.findOptionalClass("cn.nubia.account.AccountManager");
                try {
                    Object am = amClz != null ? invokeStatic(amClz, "I") : null;
                    if (am != null) {
                        HookValues.invoke(am, "U", act);
                        return;
                    }
                } catch (Throwable ignored) {}

                try {
                    Intent intent = new Intent();
                    intent.setClassName("cn.nubia.accounts", "cn.nubia.accounts.AccountIntroActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(intent);
                } catch (Throwable t) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("cn.nubia.accounts", "cn.nubia.accounts.AccountCenterActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(intent);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            session.log("HANDLE_USER_CENTER_CLICK_ERROR: " + t.getMessage());
        }
    }
}
