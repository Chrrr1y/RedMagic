@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package dev.lackluster.redmagichelper.hook.browser

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import android.content.Intent
import android.net.Uri
import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import dev.lackluster.redmagichelper.hook.HookSession
import java.util.WeakHashMap

/**
 * System-Native UI (com.android.settings) replacement for Browser Settings & Sub-Settings.
 * Styled to perfectly match RedMagic / Nubia OS system settings:
 * - System light/dark mode and Android dynamic color
 * - System surface, content, outline, and accent color roles
 * - Crisp system typography and layout
 */
object BrowserSettingsCompose {

    private val bridges = WeakHashMap<Activity, BrowserComposeBridge>()
    private val refreshNotifiers = WeakHashMap<Activity, () -> Unit>()
    private val nativeAdaptedActivities = java.util.Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    val NubiaRed = Color(0xFFE53935)
    val LightBg = Color(0xFFF1F2F6)
    val LightCard = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF1A1A1A)
    val LightTextSecondary = Color(0xFF8A8A8E)
    val LightDivider = Color(0xFFE5E5EA)
    val LightChevron = Color(0xFFC7C7CC)
    val SwitchTrackOffLight = Color(0xFFE5E5EA)

    val DarkBg = Color(0xFF121212)
    val DarkCard = Color(0xFF1E1E20)
    val DarkTextPrimary = Color(0xFFF0F0F0)
    val DarkTextSecondary = Color(0xFF8E8E93)
    val DarkDivider = Color(0xFF2C2C2E)
    val DarkChevron = Color(0xFF545458)
    val SwitchTrackOffDark = Color(0xFF39393D)

    private var lightSystemColors = fallbackLightColorScheme()
    private var darkSystemColors = fallbackDarkColorScheme()
    private var currentSystemColors = lightSystemColors

    private val SettingsBgLight get() = LightBg
    private val SettingsBgDark get() = DarkBg
    private val CardBgLight get() = LightCard
    private val CardBgDark get() = DarkCard
    private val TextPrimaryLight get() = LightTextPrimary
    private val TextPrimaryDark get() = DarkTextPrimary
    private val TextSecondaryLight get() = LightTextSecondary
    private val TextSecondaryDark get() = DarkTextSecondary
    private val DividerLight get() = LightDivider
    private val DividerDark get() = DarkDivider
    private val RedMagicRed get() = NubiaRed
    private val ChevronLight get() = LightChevron
    private val ChevronDark get() = DarkChevron

    private fun fallbackLightColorScheme() = lightColorScheme(
        primary = NubiaRed,
        onPrimary = Color.White,
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightBg,
        onSurface = LightTextPrimary,
        surfaceVariant = LightCard,
        onSurfaceVariant = LightTextSecondary,
        outline = LightChevron,
        outlineVariant = LightDivider
    )

    private fun fallbackDarkColorScheme() = darkColorScheme(
        primary = NubiaRed,
        onPrimary = Color.White,
        background = DarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkBg,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkCard,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkChevron,
        outlineVariant = DarkDivider
    )

    private fun systemColorSchemes(context: Context): Pair<ColorScheme, ColorScheme> {
        return Pair(fallbackLightColorScheme(), fallbackDarkColorScheme())
    }

    private fun isNightMode(uiMode: Int): Boolean? =
        when (uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> null
        }

    class BrowserComposeBridge(val activity: Activity) :
        LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val vmStore = ViewModelStore()

        init {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = vmStore

        fun attach(view: View) {
            attachViewTreeOwners(view, this)
        }

        fun onResume() {
            if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            }
        }

        fun onPause() {
            if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
            }
        }

        fun onDestroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            vmStore.clear()
        }
    }

    private fun attachViewTreeOwners(view: View, owner: BrowserComposeBridge) {
        // 1. ViewTreeLifecycleOwner
        try {
            val clazz = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
            val method = clazz.getMethod("set", View::class.java, LifecycleOwner::class.java)
            method.invoke(null, view, owner)
        } catch (t: Throwable) {
            try {
                val rId = Class.forName("androidx.lifecycle.runtime.R\$id")
                val field = rId.getField("view_tree_lifecycle_owner")
                view.setTag(field.getInt(null), owner)
            } catch (ignored: Throwable) {}
        }

        // 2. ViewTreeSavedStateRegistryOwner
        try {
            val clazz = Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
            val method = clazz.getMethod("set", View::class.java, SavedStateRegistryOwner::class.java)
            method.invoke(null, view, owner)
        } catch (t: Throwable) {
            try {
                val rId = Class.forName("androidx.savedstate.R\$id")
                val field = rId.getField("view_tree_saved_state_registry_owner")
                view.setTag(field.getInt(null), owner)
            } catch (ignored: Throwable) {}
        }

        // 3. ViewTreeViewModelStoreOwner
        try {
            val clazz = Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
            val method = clazz.getMethod("set", View::class.java, ViewModelStoreOwner::class.java)
            method.invoke(null, view, owner)
        } catch (t: Throwable) {
            try {
                val rId = Class.forName("androidx.lifecycle.viewmodel.R\$id")
                val field = rId.getField("view_tree_view_model_store_owner")
                view.setTag(field.getInt(null), owner)
            } catch (ignored: Throwable) {}
        }
    }

    @JvmStatic
    fun updateSystemBars(activity: Activity, isDark: Boolean) {
        try {
            val window = activity.window ?: return
            val decorView = window.decorView

            val bgColor = if (isDark) SettingsBgDark.toArgb() else SettingsBgLight.toArgb()
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
            decorView.setBackgroundColor(bgColor)

            WindowCompat.setDecorFitsSystemWindows(window, false)

            ViewCompat.setOnApplyWindowInsetsListener(decorView) { v, insets ->
                v.setPadding(0, 0, 0, 0)
                insets
            }
            decorView.setPadding(0, 0, 0, 0)

            val content = activity.findViewById<View>(android.R.id.content)
            if (content != null) {
                content.fitsSystemWindows = false
                content.setPadding(0, 0, 0, 0)
                ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
                    v.setPadding(0, 0, 0, 0)
                    insets
                }
                content.post {
                    content.setPadding(0, 0, 0, 0)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = bgColor

            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark

            ViewCompat.requestApplyInsets(decorView)
        } catch (ignored: Throwable) {}
    }

    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        var height = 0
        if (context is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val window = context.window
                    if (window?.decorView != null) {
                        val insets = window.decorView.rootWindowInsets
                        if (insets != null) {
                            height = insets.getInsetsIgnoringVisibility(
                                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.displayCutout()
                            ).top
                        }
                    }
                } catch (ignored: Throwable) {}
            }
        }
        if (height <= 0) {
            try {
                val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    height = context.resources.getDimensionPixelSize(resourceId)
                }
            } catch (ignored: Throwable) {}
        }
        if (height <= 0 && context is Activity) {
            try {
                val utilClz = context.classLoader.loadClass("com.android.browser.util.AndroidUtil")
                val pMethod = utilClz.getDeclaredMethod("P", Activity::class.java)
                pMethod.isAccessible = true
                val res = pMethod.invoke(null, context)
                if (res is Number) {
                    height = res.toInt()
                }
            } catch (ignored: Throwable) {}
        }
        if (height <= 0) {
            try {
                val density = context.resources.displayMetrics.density
                height = Math.round(36 * density)
            } catch (ignored: Throwable) {}
        }
        return height
    }

    @JvmStatic
    fun applyNativeStatusBarAdaptive(activity: Activity) {
        try {
            val window = activity.window ?: return
            val decor = window.decorView
            val content = decor.findViewById<View>(android.R.id.content) ?: decor
            val isDark = isDarkTheme(activity)

            var topColor = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.WHITE
            val titleBarId = activity.resources.getIdentifier("title_bar", "id", activity.packageName)
            if (titleBarId != 0) {
                val tb = decor.findViewById<View>(titleBarId)
                val bg = tb?.background
                if (bg is android.graphics.drawable.ColorDrawable) {
                    topColor = bg.color
                }
            }
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(topColor))
            decor.setBackgroundColor(topColor)

            val applyInsets = {
                val statusBarHeight = getStatusBarHeight(activity)
                if (statusBarHeight > 0 && content.paddingTop != statusBarHeight) {
                    content.setPadding(content.paddingLeft, statusBarHeight, content.paddingRight, content.paddingBottom)
                }
            }

            applyInsets()
            content.post(applyInsets)

            decor.setOnApplyWindowInsetsListener { v, insets ->
                var top = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val barInsets = insets.getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.displayCutout()
                    )
                    top = barInsets.top
                }
                if (top <= 0) {
                    top = insets.systemWindowInsetTop
                }
                if (top <= 0) {
                    top = getStatusBarHeight(activity)
                }
                if (top > 0 && content.paddingTop != top) {
                    content.setPadding(content.paddingLeft, top, content.paddingRight, content.paddingBottom)
                }
                v.onApplyWindowInsets(insets)
            }
            decor.requestApplyInsets()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            val navBg = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.WHITE
            window.navigationBarColor = navBg

            val controller = WindowCompat.getInsetsController(window, decor)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        } catch (ignored: Throwable) {}
    }

    @JvmStatic
    fun attach(activity: Activity, session: HookSession) {
        var origMain: View? = null
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return
            if (bridges.containsKey(activity)) return

            updateSystemBars(activity, isDarkTheme(activity))

            val bridge = BrowserComposeBridge(activity)
            bridges[activity] = bridge
            bridge.attach(decorView)

            val contentParent = activity.findViewById<ViewGroup>(android.R.id.content) ?: decorView

            val origMainId = activity.resources.getIdentifier(
                "setting_main_layout", "id", activity.packageName
            )
            if (origMainId != 0) {
                origMain = activity.findViewById<View>(origMainId)
            }

            val composeView = ComposeView(activity).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                bridge.attach(this)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    BrowserSettingsTheme(activity) {
                        BrowserSettingsScreen(activity, session)
                    }
                }
            }
            contentParent.addView(composeView)
            origMain?.visibility = View.GONE
            session.log("BROWSER_COMPOSE_SETTINGS_ATTACHED")
        } catch (t: Throwable) {
            origMain?.visibility = View.VISIBLE
            session.log("BROWSER_COMPOSE_SETTINGS_FAILED " + t.message)
        }
    }

    @JvmStatic
    fun attachSubSettings(activity: Activity, session: HookSession) {
        var origMain: View? = null
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return
            if (bridges.containsKey(activity)) return
            if (nativeAdaptedActivities.contains(activity)) return

            val contentParent = activity.findViewById<ViewGroup>(android.R.id.content) ?: decorView
            origMain = contentParent.getChildAt(0)
            if (origMain == null) {
                contentParent.post {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachSubSettings(activity, session)
                    }
                }
                return
            }

            val activityName = activity.javaClass.simpleName
            val isDedicated = activityName == "SettingAboutActivity" ||
                    activityName == "SettingClearDataActivity" ||
                    activityName == "SettingTextSizeActivity"

            // If it's a generic settings activity and we cannot inspect any rows,
            // keep the original native view visible and do not hijack.
            if (!isDedicated && inspectViewTree(origMain).isEmpty()) {
                nativeAdaptedActivities.add(activity)
                applyNativeStatusBarAdaptive(activity)
                return
            }

            updateSystemBars(activity, isDarkTheme(activity))

            val bridge = BrowserComposeBridge(activity)
            bridges[activity] = bridge
            bridge.attach(decorView)

            val composeView = ComposeView(activity).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                bridge.attach(this)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    BrowserSettingsTheme(activity) {
                        SubSettingsScreen(activity, origMain, session)
                    }
                }
            }

            origMain.visibility = View.INVISIBLE
            contentParent.addView(composeView)
            session.log("BROWSER_COMPOSE_SUB_SETTINGS_ATTACHED for " + activity.javaClass.simpleName)
        } catch (t: Throwable) {
            origMain?.visibility = View.VISIBLE
            session.log("BROWSER_COMPOSE_SUB_SETTINGS_FAILED " + t.message)
        }
    }

    @JvmStatic
    fun attachUserCenter(activity: Activity, session: HookSession) {
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return
            if (bridges.containsKey(activity)) return

            updateSystemBars(activity, isDarkTheme(activity))

            val contentParent = activity.findViewById<ViewGroup>(android.R.id.content) ?: decorView
            val origMain = contentParent.getChildAt(0)
            origMain?.visibility = View.GONE

            val bridge = BrowserComposeBridge(activity)
            bridges[activity] = bridge
            bridge.attach(decorView)

            val composeView = ComposeView(activity).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                bridge.attach(this)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    BrowserSettingsTheme(activity) {
                        UserCenterScreen(activity, session)
                    }
                }
            }
            contentParent.addView(composeView)
            session.log("BROWSER_COMPOSE_USER_CENTER_ATTACHED")
        } catch (t: Throwable) {
            session.log("BROWSER_COMPOSE_USER_CENTER_FAILED " + t.message)
        }
    }

    @JvmStatic
    fun onResume(owner: Any?) {
        if (owner is Activity) {
            updateSystemBars(owner, isDarkTheme(owner))
            bridges[owner]?.onResume()
            refreshNotifiers[owner]?.invoke()
        }
    }

    @JvmStatic
    fun onPause(owner: Any?) {
        if (owner is Activity) {
            bridges[owner]?.onPause()
        }
    }

    @JvmStatic
    fun onDestroy(owner: Any?) {
        if (owner is Activity) {
            bridges.remove(owner)?.onDestroy()
            refreshNotifiers.remove(owner)
        }
    }

    @JvmStatic
    fun notifyAccountUpdated() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            for (notifier in refreshNotifiers.values) {
                try {
                    notifier.invoke()
                } catch (ignored: Throwable) {}
            }
        }
    }

    @JvmStatic
    fun isDarkTheme(activity: Activity? = null): Boolean {
        val systemNightMode = try {
            isNightMode(Resources.getSystem().configuration.uiMode)
        } catch (ignored: Throwable) {
            null
        }
        if (systemNightMode != null) return systemNightMode

        if (activity != null) {
            try {
                return isNightMode(activity.resources.configuration.uiMode) ?: false
            } catch (ignored: Throwable) {}
        }
        return false
    }

    @Composable
    fun BrowserSettingsTheme(activity: Activity, content: @Composable () -> Unit) {
        val configuration = LocalConfiguration.current
        val isDark = remember(configuration.uiMode) { isDarkTheme(activity) }
        val schemes = remember(activity, configuration.uiMode, configuration.colorMode) {
            systemColorSchemes(activity)
        }
        lightSystemColors = schemes.first
        darkSystemColors = schemes.second
        val colors = if (isDark) darkSystemColors else lightSystemColors
        currentSystemColors = colors

        DisposableEffect(isDark, colors.surface) {
            updateSystemBars(activity, isDark)
            onDispose {}
        }

        MaterialTheme(
            colorScheme = colors,
            typography = Typography(),
            content = content
        )
    }

    // --- System UI (com.android.settings) Reusable Components ---

    @Composable
    fun SettingsCardGroup(
        title: String? = null,
        modifier: Modifier = Modifier,
        isDark: Boolean,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }

    @Composable
    fun SettingsRowDivider(isDark: Boolean) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = if (isDark) DividerDark else DividerLight
        )
    }

    @Composable
    fun SettingsNavigationRow(
        title: String,
        summary: String? = null,
        isDark: Boolean,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.985f else 1f,
            animationSpec = tween(120),
            label = "nav_row_scale"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        fontSize = 13.sp,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            ChevronRightIcon(
                tint = if (isDark) ChevronDark else ChevronLight
            )
        }
    }

    @Composable
    fun SettingsSwitchRow(
        title: String,
        summary: String? = null,
        isChecked: Boolean,
        isDark: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isChecked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        fontSize = 13.sp,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = currentSystemColors.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = if (isDark) darkSystemColors.onSurfaceVariant else lightSystemColors.onPrimary,
                    uncheckedTrackColor = if (isDark) SwitchTrackOffDark else SwitchTrackOffLight,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }

    // --- Main Browser Settings Screen ---

    sealed class SettingRowItem {
        data class Navigation(
            val idName: String,
            val title: String,
            val summary: String? = null,
            val dynamicSummary: ((Activity) -> String?)? = null
        ) : SettingRowItem()

        data class Toggle(
            val idName: String,
            val prefKey: String,
            val title: String,
            val summary: String? = null,
            val defaultVal: Boolean = false
        ) : SettingRowItem()
    }

    data class SettingGroup(
        val title: String,
        val items: List<SettingRowItem>
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BrowserSettingsScreen(activity: Activity, session: HookSession) {
        var refreshRevision by remember { mutableIntStateOf(0) }
        val isDark = remember(refreshRevision) { isDarkTheme(activity) }
        var showScreenRotationDialog by remember { mutableStateOf(false) }
        var showSearchEngineDialog by remember { mutableStateOf(false) }
        var showHomepageSettingDialog by remember { mutableStateOf(false) }
        var showMainModeDialog by remember { mutableStateOf(false) }

        remember(activity) {
            refreshNotifiers[activity] = {
                refreshRevision++
            }
            true
        }

        LaunchedEffect(Unit) {
            delay(300)
            refreshRevision++
            delay(700)
            refreshRevision++
            delay(1200)
            refreshRevision++
        }

        val sp = remember(activity) {
            try {
                PreferenceManager.getDefaultSharedPreferences(activity)
            } catch (t: Throwable) {
                activity.getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
            }
        }

        val hasGesture = remember(activity) {
            try {
                val res = activity.resources
                val id = res.getIdentifier("gesture_switch", "bool", activity.packageName)
                if (id != 0) res.getBoolean(id) else false
            } catch (ignored: Throwable) {
                false
            }
        }

        val groups = remember(hasGesture) {
            val list = mutableListOf<SettingGroup>()

            // 1. 常用设置
            list.add(
                SettingGroup(
                    title = "常用设置",
                    items = listOf(
                        SettingRowItem.Navigation(
                            idName = "search_engine",
                            title = "搜索引擎",
                            dynamicSummary = { act -> getCurrentSearchEngine(act).second }
                        ),
                        SettingRowItem.Navigation(
                            idName = "homepage_setting",
                            title = "主页设置",
                            dynamicSummary = { act -> if (getHomepageMode(act) == 1) "中国移动" else "我的主页" }
                        ),
                        SettingRowItem.Navigation(
                            idName = "main_setting",
                            title = "主页版式",
                            dynamicSummary = { act -> if (getMainMode(act) == 1) "简洁" else "默认" }
                        ),
                        SettingRowItem.Navigation(
                            idName = "screen_setting",
                            title = "屏幕旋转",
                            dynamicSummary = { act ->
                                when (getScreenRotationMode(act)) {
                                    1 -> "锁定横屏"
                                    2 -> "锁定竖屏"
                                    else -> "跟随系统"
                                }
                            }
                        ),
                        SettingRowItem.Navigation(
                            idName = "font_size_setter",
                            title = "字体大小",
                            summary = "调整网页文字大小"
                        ),
                        SettingRowItem.Navigation(
                            idName = "skin_setter",
                            title = "个性皮肤",
                            summary = "更换浏览器主题风格"
                        )
                    )
                )
            )

            // 2. 浏览体验
            val expItems = mutableListOf<SettingRowItem>()
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "smart_read_mode_button",
                    prefKey = "smart_read",
                    title = "智能阅读",
                    summary = "自动隐藏工具栏，沉浸式浏览",
                    defaultVal = true
                )
            )
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "quick_paste_button",
                    prefKey = "quick_paste",
                    title = "快速粘贴",
                    summary = "智能识别并询问打开剪贴板网址",
                    defaultVal = true
                )
            )
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "launcher_recovery_button",
                    prefKey = "launcher_recovery",
                    title = "恢复未关闭网页",
                    summary = "启动时自动恢复上次退出的页面",
                    defaultVal = true
                )
            )
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "wifi_auto_video_button",
                    prefKey = "auto_play_video",
                    title = "仅在 Wi-Fi 下自动播放视频",
                    summary = "移动网络下不自动播放视频，节省流量",
                    defaultVal = true
                )
            )
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "webView_location_button",
                    prefKey = "network_location",
                    title = "网页获取位置信息",
                    summary = "允许网页申请获取地理位置权限",
                    defaultVal = true
                )
            )
            if (hasGesture) {
                expItems.add(
                    SettingRowItem.Toggle(
                        idName = "gesture_left_and_right_button",
                        prefKey = "gesture_left_and_right",
                        title = "左右滑动手势",
                        summary = "在网页中左右滑动切换上一页/下一页",
                        defaultVal = false
                    )
                )
            }
            expItems.add(
                SettingRowItem.Toggle(
                    idName = "push_turn_button",
                    prefKey = "push_turn",
                    title = "消息推送",
                    summary = "接收浏览器资讯与服务消息推送",
                    defaultVal = true
                )
            )
            list.add(SettingGroup(title = "浏览体验", items = expItems))

            // 3. 功能与数据管理
            list.add(
                SettingGroup(
                    title = "功能与数据管理",
                    items = listOf(
                        SettingRowItem.Navigation(
                            idName = "download_setting",
                            title = "下载设置",
                            summary = "下载存储路径与下载网络设置"
                        ),
                        SettingRowItem.Navigation(
                            idName = "pass_management",
                            title = "密码管理",
                            summary = "管理网页账号密码自动填充"
                        ),
                        SettingRowItem.Navigation(
                            idName = "ad_filter",
                            title = "广告过滤",
                            summary = "拦截恶意弹窗与网页广告"
                        ),
                        SettingRowItem.Navigation(
                            idName = "clear_data",
                            title = "清除数据",
                            summary = "清除浏览历史、缓存与 Cookie"
                        ),
                        SettingRowItem.Navigation(
                            idName = "setting_advanced",
                            title = "高级设置",
                            summary = "内核选项、UA 标识与实验性功能"
                        )
                    )
                )
            )

            // 4. 隐私与系统
            list.add(
                SettingGroup(
                    title = "隐私与系统",
                    items = listOf(
                        SettingRowItem.Navigation(
                            idName = "privacy_settings",
                            title = "隐私与权限",
                            summary = "权限管理、防追踪与隐私保护"
                        ),
                        SettingRowItem.Navigation(
                            idName = "about",
                            title = "关于浏览器",
                            summary = "版本信息与版权说明"
                        ),
                        SettingRowItem.Navigation(
                            idName = "reset_default_setter",
                            title = "恢复默认设置",
                            summary = "将浏览器所有设置项恢复至初始状态"
                        )
                    )
                )
            )

            list
        }

        if (showScreenRotationDialog) {
            ScreenRotationDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showScreenRotationDialog = false
                    refreshRevision++
                }
            )
        }
        if (showSearchEngineDialog) {
            SearchEngineDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showSearchEngineDialog = false
                    refreshRevision++
                }
            )
        }
        if (showHomepageSettingDialog) {
            HomepageSettingDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showHomepageSettingDialog = false
                    refreshRevision++
                }
            )
        }
        if (showMainModeDialog) {
            MainModeDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showMainModeDialog = false
                    refreshRevision++
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "设置",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { activity.finish() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            BackArrowIcon(tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
                        titleContentColor = if (isDark) TextPrimaryDark else TextPrimaryLight
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
            contentWindowInsets = WindowInsets.navigationBars
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // User Center Profile Header Card
                item(key = "user_center") {
                    val accountInfo = remember(refreshRevision) {
                        getUserAccountInfo(activity)
                    }
                    UserCenterCard(
                        activity = activity,
                        isDark = isDark,
                        accountInfo = accountInfo,
                        onClick = {
                            if (!accountInfo.isLoggedIn) {
                                openAccountCenter(activity)
                            } else {
                                openUserCenterActivity(activity)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Render each setting group inside a single SettingsCardGroup
                groups.forEach { group ->
                    item(key = "group_${group.title}") {
                        SettingsCardGroup(title = group.title, isDark = isDark) {
                            group.items.forEachIndexed { index, item ->
                                when (item) {
                                    is SettingRowItem.Navigation -> {
                                        val summaryText = remember(item, refreshRevision) {
                                            item.dynamicSummary?.invoke(activity) ?: item.summary
                                        }
                                        SettingsNavigationRow(
                                            title = item.title,
                                            summary = summaryText,
                                            isDark = isDark,
                                            onClick = {
                                                when (item.idName) {
                                                    "screen_setting" -> showScreenRotationDialog = true
                                                    "search_engine", "search_engine_choicer" -> showSearchEngineDialog = true
                                                    "homepage_setting" -> showHomepageSettingDialog = true
                                                    "main_setting" -> showMainModeDialog = true
                                                    else -> clickSetting(activity, item.idName)
                                                }
                                            }
                                        )
                                    }
                                    is SettingRowItem.Toggle -> {
                                        var checked by remember(item.prefKey, refreshRevision) {
                                            mutableStateOf(readToggleState(activity, sp, item))
                                        }
                                        SettingsSwitchRow(
                                            title = item.title,
                                            summary = item.summary,
                                            isChecked = checked,
                                            isDark = isDark,
                                            onCheckedChange = { newChecked ->
                                                checked = newChecked
                                                toggleSetting(activity, item.idName, item.prefKey, newChecked)
                                            }
                                        )
                                    }
                                }
                                if (index < group.items.size - 1) {
                                    SettingsRowDivider(isDark = isDark)
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    @Composable
    private fun UserCenterCard(
        activity: Activity,
        isDark: Boolean,
        accountInfo: UserAccountInfo,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.985f else 1f,
            animationSpec = tween(120),
            label = "account_card_scale"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) CardBgDark else CardBgLight,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        onClick = onClick
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar View
                UserAvatarView(activity = activity, isDark = isDark, avatar = accountInfo.avatar, size = 46.dp)

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accountInfo.nickname,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                    Text(
                        text = if (accountInfo.isLoggedIn) "已登录中兴账号" else "登录同步书签、历史与多端设置",
                        fontSize = 13.sp,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                ChevronRightIcon(tint = if (isDark) ChevronDark else ChevronLight)
            }
        }
    }

    // --- Sub-Settings Screen (Generic & Dedicated System UI) ---

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SubSettingsScreen(activity: Activity, origMain: View, session: HookSession) {
        var refreshRevision by remember { mutableIntStateOf(0) }
        val isDark = remember(refreshRevision) { isDarkTheme(activity) }
        var showScreenRotationDialog by remember { mutableStateOf(false) }
        var showSearchEngineDialog by remember { mutableStateOf(false) }
        var showHomepageSettingDialog by remember { mutableStateOf(false) }
        var showMainModeDialog by remember { mutableStateOf(false) }

        remember(activity) {
            refreshNotifiers[activity] = {
                refreshRevision++
            }
            true
        }

        if (showScreenRotationDialog) {
            ScreenRotationDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showScreenRotationDialog = false
                    refreshRevision++
                }
            )
        }
        if (showSearchEngineDialog) {
            SearchEngineDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showSearchEngineDialog = false
                    refreshRevision++
                }
            )
        }
        if (showHomepageSettingDialog) {
            HomepageSettingDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showHomepageSettingDialog = false
                    refreshRevision++
                }
            )
        }
        if (showMainModeDialog) {
            MainModeDialog(
                activity = activity,
                isDark = isDark,
                onDismiss = {
                    showMainModeDialog = false
                    refreshRevision++
                }
            )
        }

        val title = remember(activity) {
            extractTitle(activity, origMain)
        }

        val activityName = activity.javaClass.simpleName

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { activity.finish() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            BackArrowIcon(tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
                        titleContentColor = if (isDark) TextPrimaryDark else TextPrimaryLight
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
            contentWindowInsets = WindowInsets.navigationBars
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Check if specific layout is recognized
                when {
                    activityName == "SettingAboutActivity" -> {
                        item { AboutPageHeader(activity, isDark) }
                        item { AboutPageCards(activity, isDark, refreshRevision) }
                    }
                    activityName == "SettingClearDataActivity" -> {
                        item { ClearDataCards(activity, isDark, refreshRevision) }
                    }
                    activityName == "SettingTextSizeActivity" -> {
                        item { TextSizeCards(activity, origMain, isDark) }
                    }
                    else -> {
                        // Universal System Settings Inspector
                        item {
                            UniversalSettingsCard(
                                activity = activity,
                                origMain = origMain,
                                isDark = isDark,
                                revision = refreshRevision,
                                onOpenSearchEngineDialog = { showSearchEngineDialog = true },
                                onOpenScreenRotationDialog = { showScreenRotationDialog = true },
                                onOpenHomepageSettingDialog = { showHomepageSettingDialog = true },
                                onOpenMainModeDialog = { showMainModeDialog = true }
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    // --- Sub-Settings: About Page ---
    @Composable
    private fun AboutPageHeader(activity: Activity, isDark: Boolean) {
        val appVersion = remember(activity) {
            try {
                val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                pInfo.versionName ?: "1.0.0"
            } catch (t: Throwable) {
                "1.0.0"
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val brandColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.size(46.dp)) {
                        val w = size.width
                        val h = size.height
                        // Draw stylized RedMagic browser icon with dynamic theme accent
                        drawCircle(
                            color = brandColor,
                            radius = w * 0.42f,
                            center = Offset(w / 2f, h / 2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.22f,
                            center = Offset(w / 2f, h / 2f)
                        )
                        drawCircle(
                            color = brandColor,
                            radius = w * 0.10f,
                            center = Offset(w / 2f, h / 2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "红魔浏览器",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "版本 $appVersion",
                fontSize = 13.sp,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight
            )
        }
    }

    @Composable
    private fun AboutPageCards(activity: Activity, isDark: Boolean, revision: Int) {
        val wifiToggle = remember(activity, revision) {
            val resId = activity.resources.getIdentifier("update_in_wifi", "id", activity.packageName)
            if (resId != 0) activity.findViewById<CompoundButton>(resId) else null
        }
        var wifiChecked by remember(wifiToggle, revision) {
            mutableStateOf(wifiToggle?.isChecked ?: true)
        }

        SettingsCardGroup(isDark = isDark) {
            // 1. 版本更新
            SettingsNavigationRow(
                title = "检查新版本",
                summary = "已是最新版本",
                isDark = isDark,
                onClick = { clickSetting(activity, "current_version") }
            )
            SettingsRowDivider(isDark = isDark)

            // 2. 仅在 Wi-Fi 下自动下载安装包
            SettingsSwitchRow(
                title = "仅在 Wi-Fi 下自动下载",
                summary = "有新版本时在 Wi-Fi 网络下静默下载",
                isChecked = wifiChecked,
                isDark = isDark,
                onCheckedChange = { chk ->
                    wifiChecked = chk
                    wifiToggle?.isChecked = chk
                    if (activity is CompoundButton.OnCheckedChangeListener && wifiToggle != null) {
                        try {
                            activity.onCheckedChanged(wifiToggle, chk)
                        } catch (ignored: Throwable) {}
                    }
                    refreshNotifiers[activity]?.invoke()
                }
            )
            SettingsRowDivider(isDark = isDark)

            // 3. 用户协议
            SettingsNavigationRow(
                title = "用户协议",
                isDark = isDark,
                onClick = { clickSetting(activity, "user_agreement_layout") }
            )
            SettingsRowDivider(isDark = isDark)

            // 4. 隐私政策
            SettingsNavigationRow(
                title = "隐私政策",
                isDark = isDark,
                onClick = { clickSetting(activity, "nubia_privacy_layout") }
            )
            SettingsRowDivider(isDark = isDark)

            // 5. 备案信息
            SettingsNavigationRow(
                title = "互联网 ICP 备案",
                summary = "粤ICP备18069660号-18A",
                isDark = isDark,
                onClick = { clickSetting(activity, "current_ICP") }
            )
        }
    }

    // --- Sub-Settings: Clear Data Page ---
    @Composable
    private fun ClearDataCards(activity: Activity, isDark: Boolean, revision: Int) {
        // Clear data items: History, Cache, Cookies, Form Data, Passwords
        val itemLabels = listOf(
            "浏览历史记录" to "清除访问过的网页记录",
            "缓存文件" to "释放临时图片与网页缓存",
            "Cookie 与网站数据" to "清除网站登录状态与偏好",
            "表单自动填充数据" to "清除已保存的表单输入记录",
            "保存的密码" to "清除保存的网站账号密码"
        )

        val checkStates = remember(activity) {
            mutableListOf(
                mutableStateOf(true),
                mutableStateOf(true),
                mutableStateOf(true),
                mutableStateOf(false),
                mutableStateOf(false)
            )
        }

        SettingsCardGroup(title = "选择清除项目", isDark = isDark) {
            itemLabels.forEachIndexed { idx, (label, desc) ->
                SettingsSwitchRow(
                    title = label,
                    summary = desc,
                    isChecked = checkStates.getOrNull(idx)?.value ?: false,
                    isDark = isDark,
                    onCheckedChange = { chk ->
                        checkStates.getOrNull(idx)?.value = chk
                        // Sync with original views if available
                        syncClearDataCheckbox(activity, idx, chk)
                    }
                )
                if (idx < itemLabels.size - 1) {
                    SettingsRowDivider(isDark = isDark)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Prominent Clear Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    clickSetting(activity, "clear_layout")
                    clickSetting(activity, "clear_button")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "清除数据",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    private fun syncClearDataCheckbox(activity: Activity, index: Int, checked: Boolean) {
        try {
            val resId = activity.resources.getIdentifier("item_container", "id", activity.packageName)
            if (resId != 0) {
                val container = activity.findViewById<ViewGroup>(resId)
                if (container != null && index < container.childCount) {
                    val row = container.getChildAt(index)
                    if (row is ViewGroup) {
                        for (i in 0 until row.childCount) {
                            val c = row.getChildAt(i)
                            if (c is CompoundButton) {
                                c.isChecked = checked
                                break
                            }
                        }
                    }
                }
            }
        } catch (ignored: Throwable) {}
    }

    // --- Sub-Settings: Text Size Page ---
    @Composable
    private fun TextSizeCards(activity: Activity, origMain: View, isDark: Boolean) {
        val exampleTv = remember(activity) {
            val resId = activity.resources.getIdentifier("fontsize_example", "id", activity.packageName)
            if (resId != 0) activity.findViewById<TextView>(resId) else null
        }
        val exampleText = exampleTv?.text?.toString() ?: "这是一段预览文字，调整滑块可改变文字大小。"

        val seekbar = remember(activity) {
            val resId = activity.resources.getIdentifier("seekbar", "id", activity.packageName)
            if (resId != 0) activity.findViewById<View>(resId) else null
        }

        SettingsCardGroup(title = "效果预览", isDark = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exampleText,
                    fontSize = 17.sp,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (seekbar != null) {
            SettingsCardGroup(title = "调整字体大小", isDark = isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AndroidView(
                        factory = {
                            val parent = seekbar.parent as? ViewGroup
                            parent?.removeView(seekbar)
                            seekbar
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SettingsCardGroup(isDark = isDark) {
            Text(
                text = "提示：文字大小设置将在网页加载时生效，部分固定样式的网页可能无法缩放。",
                fontSize = 13.sp,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // --- Universal Sub-Settings Engine ---
    data class ParsedRow(
        val originalView: View,
        val title: String,
        val summary: String? = null,
        val toggleButton: CompoundButton? = null
    )

    data class ParsedSection(
        val header: String? = null,
        val rows: List<ParsedRow>
    )

    @Composable
    private fun UniversalSettingsCard(
        activity: Activity,
        origMain: View,
        isDark: Boolean,
        revision: Int,
        onOpenSearchEngineDialog: () -> Unit,
        onOpenScreenRotationDialog: () -> Unit,
        onOpenHomepageSettingDialog: () -> Unit,
        onOpenMainModeDialog: () -> Unit
    ) {
        val sections = remember(origMain, revision) {
            inspectViewTree(origMain)
        }

        if (sections.isEmpty()) {
            // Fallback: Embed the original view if no rows could be parsed
            SettingsCardGroup(isDark = isDark) {
                AndroidView(
                    factory = {
                        val parent = origMain.parent as? ViewGroup
                        parent?.removeView(origMain)
                        origMain.visibility = View.VISIBLE
                        origMain
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }

        sections.forEach { section ->
            SettingsCardGroup(title = section.header, isDark = isDark) {
                section.rows.forEachIndexed { index, row ->
                    if (row.toggleButton != null) {
                        var checked by remember(row.toggleButton, revision) {
                            mutableStateOf(row.toggleButton.isChecked)
                        }
                        SettingsSwitchRow(
                            title = row.title,
                            summary = row.summary,
                            isChecked = checked,
                            isDark = isDark,
                            onCheckedChange = { chk ->
                                checked = chk
                                row.toggleButton.isChecked = chk
                                if (activity is CompoundButton.OnCheckedChangeListener) {
                                    try {
                                        activity.onCheckedChanged(row.toggleButton, chk)
                                    } catch (ignored: Throwable) {}
                                }
                                refreshNotifiers[activity]?.invoke()
                            }
                        )
                    } else {
                        val entryName = try {
                            row.originalView.resources.getResourceEntryName(row.originalView.id)
                        } catch (t: Throwable) {
                            ""
                        }
                        val dynamicSummary = when {
                            entryName == "search_engine_choicer" || row.title.contains("搜索引擎") -> {
                                getCurrentSearchEngine(activity).second
                            }
                            entryName == "screen_setting" || row.title.contains("屏幕旋转") -> {
                                when (getScreenRotationMode(activity)) {
                                    1 -> "锁定横屏"
                                    2 -> "锁定竖屏"
                                    else -> "跟随系统"
                                }
                            }
                            entryName == "homepage_setting" || (row.title == "主页设置" && entryName != "main_setting") -> {
                                if (getHomepageMode(activity) == 1) "中国移动" else "我的主页"
                            }
                            entryName == "main_setting" || row.title.contains("主页版式") -> {
                                if (getMainMode(activity) == 1) "简洁" else "默认"
                            }
                            else -> row.summary
                        }
                        SettingsNavigationRow(
                            title = row.title,
                            summary = dynamicSummary,
                            isDark = isDark,
                            onClick = {
                                when {
                                    entryName == "search_engine_choicer" || row.title.contains("搜索引擎") -> {
                                        onOpenSearchEngineDialog()
                                    }
                                    entryName == "screen_setting" || row.title.contains("屏幕旋转") -> {
                                        onOpenScreenRotationDialog()
                                    }
                                    entryName == "homepage_setting" || (row.title == "主页设置" && entryName != "main_setting") -> {
                                        onOpenHomepageSettingDialog()
                                    }
                                    entryName == "main_setting" || row.title.contains("主页版式") -> {
                                        onOpenMainModeDialog()
                                    }
                                    else -> {
                                        if (activity is View.OnClickListener) {
                                            try {
                                                activity.onClick(row.originalView)
                                            } catch (ignored: Throwable) {
                                                row.originalView.performClick()
                                            }
                                        } else {
                                            row.originalView.performClick()
                                        }
                                        refreshNotifiers[activity]?.invoke()
                                    }
                                }
                            }
                        )
                    }
                    if (index < section.rows.size - 1) {
                        SettingsRowDivider(isDark = isDark)
                    }
                }
            }
        }
    }

    private fun inspectViewTree(root: View): List<ParsedSection> {
        val sections = mutableListOf<ParsedSection>()
        val currentRows = mutableListOf<ParsedRow>()
        var currentHeader: String? = null

        fun flushSection() {
            if (currentRows.isNotEmpty()) {
                sections.add(ParsedSection(currentHeader, currentRows.toList()))
                currentRows.clear()
                currentHeader = null
            }
        }

        fun traverse(view: View) {
            val resName = getResourceEntryName(view) ?: ""
            if (resName == "title_bar" || resName == "status_bar" || resName.startsWith("separator")) {
                return
            }

            // Check if this view itself is a setting row
            val parsedRow = parseSettingRow(view)
            if (parsedRow != null) {
                currentRows.add(parsedRow)
                return
            }

            // Check if this view is a section header (e.g., manager_ad_record_tv)
            if (view is TextView && view !is CompoundButton && !view.isClickable) {
                val txt = view.text?.toString()?.trim() ?: ""
                if (txt.length in 2..20 && !txt.contains("Copyright") && !txt.contains("ICP")) {
                    flushSection()
                    currentHeader = txt
                    return
                }
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    traverse(view.getChildAt(i))
                }
            }
        }

        traverse(root)
        flushSection()
        return sections
    }

    private fun getResourceEntryName(view: View?): String? {
        if (view == null || view.id == View.NO_ID) return null
        return try {
            view.resources.getResourceEntryName(view.id)
        } catch (t: Throwable) {
            null
        }
    }

    private fun parseSettingRow(view: View): ParsedRow? {
        if (view !is ViewGroup) return null
        if (view.childCount == 0) return null

        var title: String? = null
        var summary: String? = null
        var toggle: CompoundButton? = null
        var hasArrow = false

        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is CompoundButton) {
                toggle = child
            } else if (child is TextView) {
                val txt = child.text?.toString()?.trim()
                if (!txt.isNullOrBlank()) {
                    if (title == null) {
                        title = txt
                    } else if (summary == null) {
                        summary = txt
                    }
                }
            } else {
                val childName = getResourceEntryName(child) ?: ""
                if (childName.contains("next") || childName.contains("arrow")) {
                    hasArrow = true
                }
            }
        }

        if (title != null && (toggle != null || hasArrow || view.isClickable)) {
            return ParsedRow(
                originalView = view,
                title = title,
                summary = summary,
                toggleButton = toggle
            )
        }
        return null
    }

    private fun extractTitle(activity: Activity, root: View): String {
        try {
            val resId = activity.resources.getIdentifier("title_text", "id", activity.packageName)
            if (resId != 0) {
                val tv = root.findViewById<TextView>(resId)
                val t = tv?.text?.toString()?.trim()
                if (!t.isNullOrBlank()) return t
            }
        } catch (ignored: Throwable) {}

        val actTitle = activity.title?.toString()?.trim()
        if (!actTitle.isNullOrBlank()) return actTitle

        return when (activity.javaClass.simpleName) {
            "SettingAdvancedActivity" -> "高级设置"
            "ADFilterSettingsActivity" -> "广告过滤"
            "SettingClearDataActivity" -> "清除浏览数据"
            "SettingAboutActivity" -> "关于浏览器"
            "PrivacySettingsActivity" -> "隐私与权限"
            "PersonalSettingsActivity" -> "个性化设置"
            "AdSettingsActivity" -> "个性化广告设置"
            "SystemPermissionActivity" -> "系统权限管理"
            "SettingTextSizeActivity" -> "字体大小"
            "DownloadSettingActivity" -> "下载设置"
            "ServiceSettingActivity" -> "服务设置"
            "PasswordSettingsActivity" -> "密码管理"
            else -> "设置"
        }
    }

    // --- Helper Logic for Preferences and View Dispatch ---

    private fun readToggleState(activity: Activity, sp: SharedPreferences, item: SettingRowItem.Toggle): Boolean {
        if (sp.contains(item.prefKey)) {
            return sp.getBoolean(item.prefKey, item.defaultVal)
        }
        try {
            val resId = activity.resources.getIdentifier(item.idName, "id", activity.packageName)
            if (resId != 0) {
                val button = activity.findViewById<CompoundButton>(resId)
                if (button != null) {
                    return button.isChecked
                }
            }
        } catch (ignored: Throwable) {}

        return try {
            sp.getBoolean(item.prefKey, item.defaultVal)
        } catch (t: Throwable) {
            item.defaultVal
        }
    }

    private fun clickSetting(activity: Activity, idName: String) {
        try {
            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
            if (resId != 0) {
                val view = activity.findViewById<View>(resId)
                if (view != null) {
                    if (activity is View.OnClickListener) {
                        activity.onClick(view)
                    } else {
                        view.performClick()
                    }
                    return
                }
            }
        } catch (t: Throwable) {
            activity.runOnUiThread {
                try {
                    val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
                    if (resId != 0) {
                        activity.findViewById<View>(resId)?.performClick()
                    }
                } catch (ignored: Throwable) {}
            }
        }
    }

    private fun toggleSetting(activity: Activity, idName: String, prefKey: String, checked: Boolean) {
        try {
            val sp = PreferenceManager.getDefaultSharedPreferences(activity)
            sp.edit().putBoolean(prefKey, checked).apply()

            try {
                val bsClz = activity.classLoader.loadClass("com.android.browser.BrowserSettings")
                val getInstance = bsClz.getMethod("getInstance")
                val bsInstance = getInstance.invoke(null)
                if (bsInstance is SharedPreferences.OnSharedPreferenceChangeListener) {
                    bsInstance.onSharedPreferenceChanged(sp, prefKey)
                }

                when (prefKey) {
                    "gesture_left_and_right" -> {
                        val f = bsClz.getDeclaredField("D")
                        f.isAccessible = true
                        f.setBoolean(null, checked)
                    }
                    "smart_read" -> {
                        val f = bsClz.getDeclaredField("F")
                        f.isAccessible = true
                        f.setBoolean(null, checked)
                    }
                    "push_turn" -> {
                        val m = bsClz.getDeclaredMethod("M0", Context::class.java, Boolean::class.javaPrimitiveType)
                        m.invoke(null, activity.applicationContext, checked)
                    }
                }
            } catch (ignored: Throwable) {}
        } catch (ignored: Throwable) {}

        try {
            val resId = activity.resources.getIdentifier(idName, "id", activity.packageName)
            if (resId != 0) {
                val button = activity.findViewById<CompoundButton>(resId)
                if (button != null) {
                    button.isChecked = checked
                    if (activity is CompoundButton.OnCheckedChangeListener) {
                        activity.onCheckedChanged(button, checked)
                    }
                }
            }
        } catch (ignored: Throwable) {}

        refreshNotifiers[activity]?.invoke()
    }

    @Composable
    fun BackArrowIcon(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier.size(22.dp)) {
            val stroke = 2.4.dp.toPx()
            val midY = size.height / 2f
            drawLine(
                color = tint,
                start = Offset(size.width * 0.82f, midY),
                end = Offset(size.width * 0.18f, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.18f, midY),
                end = Offset(size.width * 0.44f, midY - size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.18f, midY),
                end = Offset(size.width * 0.44f, midY + size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun ChevronRightIcon(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier.size(16.dp)) {
            val stroke = 2.dp.toPx()
            val midY = size.height / 2f
            drawLine(
                color = tint,
                start = Offset(size.width * 0.35f, midY - size.height * 0.3f),
                end = Offset(size.width * 0.65f, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(size.width * 0.65f, midY),
                end = Offset(size.width * 0.35f, midY + size.height * 0.3f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun SettingsIcon(activity: Activity, tint: Color, modifier: Modifier = Modifier) {
        val drawable = remember(activity) {
            try {
                val resId = activity.resources.getIdentifier("menu_setting", "drawable", activity.packageName)
                if (resId != 0) ContextCompat.getDrawable(activity, resId) else null
            } catch (t: Throwable) {
                null
            }
        }
        val bmp = remember(drawable) {
            if (drawable != null) {
                try {
                    val w = drawable.intrinsicWidth.coerceAtLeast(1)
                    val h = drawable.intrinsicHeight.coerceAtLeast(1)
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(b)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(c)
                    b.asImageBitmap()
                } catch (t: Throwable) {
                    null
                }
            } else null
        }
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "设置",
                modifier = modifier.size(22.dp),
                colorFilter = ColorFilter.tint(tint)
            )
        } else {
            Canvas(modifier = modifier.size(22.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerR = size.minDimension * 0.38f
                val strokeW = size.minDimension * 0.12f
                drawCircle(color = tint, radius = outerR, center = center, style = Stroke(width = strokeW))
                drawCircle(color = tint, radius = size.minDimension * 0.16f, center = center)
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60.0))
                    val cos = Math.cos(angle).toFloat()
                    val sin = Math.sin(angle).toFloat()
                    val toothCenter = Offset(
                        center.x + (outerR + strokeW * 0.3f) * cos,
                        center.y + (outerR + strokeW * 0.3f) * sin
                    )
                    drawCircle(color = tint, radius = strokeW * 0.7f, center = toothCenter)
                }
            }
        }
    }

    @Composable
    private fun DefaultAvatarPlaceholder(isDark: Boolean) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val r = size.minDimension / 2f
                val headColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF68686E)
                // Head
                drawCircle(
                    color = headColor,
                    radius = r * 0.44f,
                    center = Offset(size.width / 2f, size.height * 0.35f)
                )
                // Body
                drawArc(
                    color = headColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.15f, size.height * 0.55f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.7f)
                )
            }
        }
    }

    @Composable
    private fun UserAvatarView(
        activity: Activity,
        isDark: Boolean,
        avatar: Bitmap?,
        size: Dp = 46.dp
    ) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFEDEDF0)
        ) {
            if (avatar != null && !avatar.isRecycled && avatar.width > 0 && avatar.height > 0) {
                Image(
                    bitmap = avatar.asImageBitmap(),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                val defaultDrawable = remember(activity) {
                    try {
                        val resId = activity.resources.getIdentifier("user_avatar_default", "drawable", activity.packageName)
                        if (resId != 0) ContextCompat.getDrawable(activity, resId) else null
                    } catch (t: Throwable) {
                        null
                    }
                }
                val bmp = remember(defaultDrawable) {
                    if (defaultDrawable != null) {
                        try {
                            val w = defaultDrawable.intrinsicWidth.coerceAtLeast(1)
                            val h = defaultDrawable.intrinsicHeight.coerceAtLeast(1)
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val c = android.graphics.Canvas(b)
                            defaultDrawable.setBounds(0, 0, w, h)
                            defaultDrawable.draw(c)
                            b.asImageBitmap()
                        } catch (t: Throwable) {
                            null
                        }
                    } else null
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "默认头像",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultAvatarPlaceholder(isDark = isDark)
                }
            }
        }
    }

    fun openBrowserSettings(activity: Activity) {
        try {
            val intent = Intent(activity, activity.classLoader.loadClass("com.android.browser.settings.BrowserSettingActivity"))
            activity.startActivity(intent)
        } catch (t: Throwable) {
            activity.finish()
        }
    }

    // --- Screen Rotation Settings & Dialog ---

    @Composable
    fun ScreenRotationDialog(
        activity: Activity,
        isDark: Boolean,
        onDismiss: () -> Unit
    ) {
        val currentMode = remember { getScreenRotationMode(activity) }
        var selectedMode by remember { mutableIntStateOf(currentMode) }

        val options = listOf(
            0 to "跟随系统",
            1 to "锁定横屏",
            2 to "锁定竖屏"
        )

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "屏幕旋转",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val accentColor = MaterialTheme.colorScheme.primary
                    options.forEach { (mode, label) ->
                        val isSelected = selectedMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedMode = mode
                                    setScreenRotationMode(activity, mode)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) accentColor else (if (isDark) TextPrimaryDark else TextPrimaryLight),
                                modifier = Modifier.weight(1f)
                            )

                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    if (isSelected) {
                                        drawCircle(
                                            color = accentColor,
                                            radius = size.minDimension / 2f
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension * 0.22f
                                        )
                                    } else {
                                        drawCircle(
                                            color = if (isDark) Color(0xFF545458) else Color(0xFFC7C7CC),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 1.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "取消",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Screen Rotation Settings & Dialog ---

    fun getScreenRotationMode(activity: Activity): Int {
        return try {
            val dcClass = activity.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInstance = dcClass.getMethod("getInstance")
            val dcInstance = getInstance.invoke(null)
            val getDataKeeper = dcClass.getMethod("getDataKeeper")
            val dataKeeper = getDataKeeper.invoke(dcInstance)
            val dkClass = dataKeeper.javaClass
            val iMethod = dkClass.getMethod("i", String::class.java, Int::class.javaPrimitiveType)
            iMethod.invoke(dataKeeper, "screen_mode", 0) as Int
        } catch (t: Throwable) {
            0
        }
    }

    fun setScreenRotationMode(activity: Activity, mode: Int) {
        try {
            val dcClass = activity.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInstance = dcClass.getMethod("getInstance")
            val dcInstance = getInstance.invoke(null)
            val getDataKeeper = dcClass.getMethod("getDataKeeper")
            val dataKeeper = getDataKeeper.invoke(dcInstance)
            val dkClass = dataKeeper.javaClass
            val oMethod = dkClass.getMethod("o", String::class.java, Int::class.javaPrimitiveType)
            oMethod.invoke(dataKeeper, "screen_mode", mode)

            try {
                val xMethod = activity.javaClass.getMethod("x")
                xMethod.isAccessible = true
                xMethod.invoke(activity)
            } catch (t: Throwable) {
                try {
                    val superClz = activity.javaClass.superclass
                    val xMethod = superClz?.getDeclaredMethod("x")
                    xMethod?.isAccessible = true
                    xMethod?.invoke(activity)
                } catch (ignored: Throwable) {}
            }

            try {
                val resId = activity.resources.getIdentifier("screen_setting_current", "id", activity.packageName)
                if (resId != 0) {
                    val tv = activity.findViewById<TextView>(resId)
                    val summary = when (mode) {
                        1 -> "锁定横屏"
                        2 -> "锁定竖屏"
                        else -> "跟随系统"
                    }
                    tv?.text = summary
                }
            } catch (ignored: Throwable) {}

            refreshNotifiers[activity]?.invoke()
        } catch (ignored: Throwable) {}
    }

    // --- Search Engine Settings & Dialog ---

    data class SearchEngineOption(val id: Int, val name: String, val rawItem: Any?)

    fun getCurrentSearchEngine(context: Context): Pair<Int, String> {
        try {
            val dcClz = context.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInst = dcClz.getMethod("getInstance")
            val dc = getInst.invoke(null)
            if (dc != null) {
                val defMethod = dcClz.getMethod("getDefaultSearchEngine")
                val item = defMethod.invoke(dc)
                if (item != null) {
                    val getId = item.javaClass.getMethod("getId")
                    val getShowName = item.javaClass.getMethod("getShowName")
                    val id = getId.invoke(item) as? Int ?: 0
                    val name = getShowName.invoke(item) as? String ?: "百度"
                    return Pair(id, name)
                }
            }
        } catch (ignored: Throwable) {}

        try {
            val bsClz = context.classLoader.loadClass("com.android.browser.BrowserSettings")
            val getV = bsClz.getMethod("V")
            val bs = getV.invoke(null)
            if (bs != null) {
                val k0Method = bsClz.getMethod("k0")
                val se = k0Method.invoke(bs)
                if (se != null) {
                    val getName = se.javaClass.getMethod("getName")
                    val getId = se.javaClass.getMethod("getId")
                    val name = getName.invoke(se) as? String ?: "百度"
                    val idStr = getId.invoke(se) as? String ?: "1"
                    return Pair(idStr.toIntOrNull() ?: 1, name)
                }
            }
        } catch (ignored: Throwable) {}

        return Pair(1, "百度")
    }

    fun getAvailableSearchEngines(context: Context): List<SearchEngineOption> {
        try {
            val dcClz = context.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInst = dcClz.getMethod("getInstance")
            val dc = getInst.invoke(null)
            if (dc != null) {
                val listMethod = dcClz.getMethod("getSearchEnginesList")
                val list = listMethod.invoke(dc) as? List<*>
                if (!list.isNullOrEmpty()) {
                    val result = mutableListOf<SearchEngineOption>()
                    for (item in list) {
                        if (item != null) {
                            val getId = item.javaClass.getMethod("getId")
                            val getShowName = item.javaClass.getMethod("getShowName")
                            val id = getId.invoke(item) as? Int ?: 0
                            val name = getShowName.invoke(item) as? String ?: ""
                            if (name.isNotBlank()) {
                                result.add(SearchEngineOption(id, name, item))
                            }
                        }
                    }
                    if (result.isNotEmpty()) return result
                }
            }
        } catch (ignored: Throwable) {}

        return listOf(
            SearchEngineOption(1, "百度", null),
            SearchEngineOption(3, "必应", null),
            SearchEngineOption(2, "搜狗", null),
            SearchEngineOption(4, "360", null),
            SearchEngineOption(5, "谷歌", null)
        )
    }

    fun setSearchEngine(activity: Activity, option: SearchEngineOption) {
        try {
            val dcClz = activity.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInst = dcClz.getMethod("getInstance")
            val dc = getInst.invoke(null)
            if (dc != null) {
                var itemToSet = option.rawItem
                if (itemToSet == null) {
                    try {
                        val getById = dcClz.getMethod("getSearchEngineItemById", String::class.java)
                        itemToSet = getById.invoke(dc, option.id.toString())
                    } catch (ignored: Throwable) {}
                }
                if (itemToSet != null) {
                    val setDef = dcClz.getMethod("setDefaultSearchEngine", itemToSet.javaClass)
                    setDef.invoke(dc, itemToSet)
                }
            }
        } catch (ignored: Throwable) {}

        try {
            val bsClz = activity.classLoader.loadClass("com.android.browser.BrowserSettings")
            val getV = bsClz.getMethod("V")
            val bs = getV.invoke(null)
            if (bs != null) {
                val d1Method = bsClz.getMethod("d1", String::class.java)
                d1Method.invoke(bs, option.id.toString())
            }
        } catch (ignored: Throwable) {}

        try {
            val resId = activity.resources.getIdentifier("search_engine_text", "id", activity.packageName)
            if (resId != 0) {
                activity.findViewById<TextView>(resId)?.text = option.name
            }
            val mainCurId = activity.resources.getIdentifier("main_setting_current", "id", activity.packageName)
            if (mainCurId != 0) {
                activity.findViewById<TextView>(mainCurId)?.text = option.name
            }
        } catch (ignored: Throwable) {}

        refreshNotifiers[activity]?.invoke()
    }

    @Composable
    fun SearchEngineDialog(
        activity: Activity,
        isDark: Boolean,
        onDismiss: () -> Unit
    ) {
        val engines = remember { getAvailableSearchEngines(activity) }
        val currentEngine = remember { getCurrentSearchEngine(activity) }
        var selectedId by remember { mutableIntStateOf(currentEngine.first) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "搜索引擎",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val accentColor = MaterialTheme.colorScheme.primary
                    engines.forEach { option ->
                        val isSelected = selectedId == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedId = option.id
                                    setSearchEngine(activity, option)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.name,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) accentColor else (if (isDark) TextPrimaryDark else TextPrimaryLight),
                                modifier = Modifier.weight(1f)
                            )

                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    if (isSelected) {
                                        drawCircle(
                                            color = accentColor,
                                            radius = size.minDimension / 2f
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension * 0.22f
                                        )
                                    } else {
                                        drawCircle(
                                            color = if (isDark) Color(0xFF545458) else Color(0xFFC7C7CC),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 1.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "取消",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Homepage Settings & Dialog ---

    fun getHomepageMode(context: Context): Int {
        return try {
            val hcClz = context.classLoader.loadClass("com.android.browser.HomeConfigManager")
            val bMethod = hcClz.getMethod("b")
            bMethod.invoke(null) as? Int ?: 0
        } catch (ignored: Throwable) {
            0
        }
    }

    fun setHomepageMode(activity: Activity, mode: Int) {
        try {
            val hcClz = activity.classLoader.loadClass("com.android.browser.HomeConfigManager")
            val gMethod = hcClz.getMethod("g", Int::class.javaPrimitiveType)
            gMethod.invoke(null, mode)
        } catch (ignored: Throwable) {}

        try {
            val resId = activity.resources.getIdentifier("homepage_setting_description", "id", activity.packageName)
            if (resId != 0) {
                val desc = if (mode == 1) "中国移动" else "我的主页"
                activity.findViewById<TextView>(resId)?.text = desc
            }
        } catch (ignored: Throwable) {}

        refreshNotifiers[activity]?.invoke()
    }

    @Composable
    fun HomepageSettingDialog(
        activity: Activity,
        isDark: Boolean,
        onDismiss: () -> Unit
    ) {
        val currentMode = remember { getHomepageMode(activity) }
        var selectedMode by remember { mutableIntStateOf(currentMode) }

        val options = listOf(
            0 to "我的主页",
            1 to "中国移动"
        )

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "主页设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val accentColor = MaterialTheme.colorScheme.primary
                    options.forEach { (mode, label) ->
                        val isSelected = selectedMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedMode = mode
                                    setHomepageMode(activity, mode)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) accentColor else (if (isDark) TextPrimaryDark else TextPrimaryLight),
                                modifier = Modifier.weight(1f)
                            )

                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    if (isSelected) {
                                        drawCircle(
                                            color = accentColor,
                                            radius = size.minDimension / 2f
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension * 0.22f
                                        )
                                    } else {
                                        drawCircle(
                                            color = if (isDark) Color(0xFF545458) else Color(0xFFC7C7CC),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 1.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "取消",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Main Mode (主页版式) Settings & Dialog ---

    fun getMainMode(context: Context): Int {
        return try {
            val dcClz = context.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInst = dcClz.getMethod("getInstance")
            val dc = getInst.invoke(null)
            if (dc != null) {
                val dkMethod = dcClz.getMethod("getDataKeeper")
                val dk = dkMethod.invoke(dc)
                if (dk != null) {
                    val hMethod = dk.javaClass.getMethod("h", String::class.java, Int::class.javaPrimitiveType)
                    return hMethod.invoke(dk, "main_mode", 0) as? Int ?: 0
                }
            }
            0
        } catch (ignored: Throwable) {
            0
        }
    }

    fun setMainMode(activity: Activity, mode: Int) {
        try {
            val dcClz = activity.classLoader.loadClass("com.android.browser.datacenter.DataCenter")
            val getInst = dcClz.getMethod("getInstance")
            val dc = getInst.invoke(null)
            if (dc != null) {
                val dkMethod = dcClz.getMethod("getDataKeeper")
                val dk = dkMethod.invoke(dc)
                if (dk != null) {
                    val oMethod = dk.javaClass.getMethod("o", String::class.java, Int::class.javaPrimitiveType)
                    oMethod.invoke(dk, "main_mode", mode)
                }
            }
        } catch (ignored: Throwable) {}

        try {
            val xMethod = activity.javaClass.getMethod("x")
            xMethod.isAccessible = true
            xMethod.invoke(activity)
        } catch (ignored: Throwable) {}

        try {
            val resId = activity.resources.getIdentifier("main_setting_current", "id", activity.packageName)
            if (resId != 0) {
                val desc = if (mode == 1) "简洁" else "默认"
                activity.findViewById<TextView>(resId)?.text = desc
            }
        } catch (ignored: Throwable) {}

        refreshNotifiers[activity]?.invoke()
    }

    @Composable
    fun MainModeDialog(
        activity: Activity,
        isDark: Boolean,
        onDismiss: () -> Unit
    ) {
        val currentMode = remember { getMainMode(activity) }
        var selectedMode by remember { mutableIntStateOf(currentMode) }

        val options = listOf(
            0 to "默认",
            1 to "简洁"
        )

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) CardBgDark else CardBgLight,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "主页版式",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val accentColor = MaterialTheme.colorScheme.primary
                    options.forEach { (mode, label) ->
                        val isSelected = selectedMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedMode = mode
                                    setMainMode(activity, mode)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) accentColor else (if (isDark) TextPrimaryDark else TextPrimaryLight),
                                modifier = Modifier.weight(1f)
                            )

                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    if (isSelected) {
                                        drawCircle(
                                            color = accentColor,
                                            radius = size.minDimension / 2f
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension * 0.22f
                                        )
                                    } else {
                                        drawCircle(
                                            color = if (isDark) Color(0xFF545458) else Color(0xFFC7C7CC),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 1.8.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "取消",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }

    // --- User Center & ZTE Account Data & UI ---

    data class UserAccountInfo(
        val nickname: String,
        val isLoggedIn: Boolean,
        val avatar: Bitmap? = null
    )

    fun getUserAccountInfo(activity: Activity): UserAccountInfo {
        return try {
            val amClz = activity.classLoader.loadClass("cn.nubia.account.AccountManager")
            val getInstance = amClz.getMethod("I")
            val instance = getInstance.invoke(null)
            if (instance != null) {
                try {
                    val yMethod = amClz.getMethod("y")
                    yMethod.invoke(instance)
                } catch (ignored: Throwable) {}

                val isLoginMethod = amClz.getMethod("C")
                val isLogin = isLoginMethod.invoke(instance) as? Boolean ?: false
                if (isLogin) {
                    var avatar: Bitmap? = null
                    try {
                        val mMethod = amClz.getMethod("M")
                        val bmp = mMethod.invoke(instance) as? Bitmap
                        if (bmp != null && !bmp.isRecycled) {
                            avatar = bmp
                        }
                    } catch (ignored: Throwable) {}

                    val nickMethod = amClz.getMethod("N")
                    val nick = nickMethod.invoke(instance) as? String
                    if (!nick.isNullOrBlank()) {
                        return UserAccountInfo(nickname = nick, isLoggedIn = true, avatar = avatar)
                    }
                    val nameMethod = amClz.getMethod("L")
                    val name = nameMethod.invoke(instance) as? String
                    if (!name.isNullOrBlank()) {
                        return UserAccountInfo(nickname = name, isLoggedIn = true, avatar = avatar)
                    }
                    return UserAccountInfo(nickname = "中兴用户", isLoggedIn = true, avatar = avatar)
                }
            }
            UserAccountInfo(nickname = "中兴账号", isLoggedIn = false, avatar = null)
        } catch (t: Throwable) {
            UserAccountInfo(nickname = "中兴账号", isLoggedIn = false, avatar = null)
        }
    }

    fun openAccountCenter(activity: Activity) {
        // Direct call to AccountManager.U(activity) which opens AccountIntroActivity for login
        try {
            val amClz = activity.classLoader.loadClass("cn.nubia.account.AccountManager")
            val getInstance = amClz.getMethod("I")
            val instance = getInstance.invoke(null)
            if (instance != null) {
                try {
                    val uMethod = amClz.getMethod("U", Activity::class.java)
                    uMethod.invoke(instance, activity)
                    return
                } catch (ignored: Throwable) {}

                val oMethod = amClz.getMethod("O", Activity::class.java)
                oMethod.invoke(instance, activity)
                return
            }
        } catch (ignored: Throwable) {}

        // Fallback: direct Intent to AccountIntroActivity
        try {
            val intent = Intent()
            intent.setClassName("cn.nubia.accounts", "cn.nubia.accounts.AccountIntroActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            return
        } catch (ignored: Throwable) {}

        try {
            clickSetting(activity, "usercenter_outer_layout")
        } catch (ignored: Throwable) {}
    }

    fun openBrowserScheme(activity: Activity, uriStr: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
            intent.setPackage(activity.packageName)
            activity.startActivity(intent)
        } catch (t: Throwable) {
            try {
                val jumperClz = activity.classLoader.loadClass("cn.nubia.usercenter.utils.UrlJumperUtil")
                val fieldA = jumperClz.getField("a")
                val instance = fieldA.get(null)
                val aMethod = jumperClz.getMethod("a", Context::class.java, String::class.java)
                aMethod.invoke(instance, activity, uriStr)
            } catch (ignored: Throwable) {}
        }
    }

    fun openUserCenterActivity(activity: Activity) {
        try {
            val intent = Intent()
            intent.setClassName(activity.packageName, "cn.nubia.usercenter.UserCenterActivity")
            intent.putExtra("from", "settings")
            activity.startActivity(intent)
            return
        } catch (ignored: Throwable) {}

        try {
            val ucClz = activity.classLoader.loadClass("cn.nubia.usercenter.UserCenterActivity")
            val companionField = ucClz.getField("g0")
            val companion = companionField.get(null)
            val aMethod = companion.javaClass.getMethod("a", Context::class.java, String::class.java)
            aMethod.invoke(companion, activity, "settings")
            return
        } catch (ignored: Throwable) {}

        try {
            val amClz = activity.classLoader.loadClass("cn.nubia.account.AccountManager")
            val getInstance = amClz.getMethod("I")
            val instance = getInstance.invoke(null)
            if (instance != null) {
                val sMethod = amClz.getMethod("S", Activity::class.java)
                sMethod.invoke(instance, activity)
                return
            }
        } catch (ignored: Throwable) {}

        try {
            val intent = Intent()
            intent.setClassName(activity.packageName, "cn.nubia.account.OldUserCenterActivity")
            intent.putExtra("from", "settings")
            activity.startActivity(intent)
            return
        } catch (ignored: Throwable) {}

        clickSetting(activity, "usercenter_outer_layout")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UserCenterScreen(activity: Activity, session: HookSession) {
        var refreshRevision by remember { mutableIntStateOf(0) }
        val isDark = remember(refreshRevision) { isDarkTheme(activity) }

        remember(activity) {
            refreshNotifiers[activity] = {
                refreshRevision++
            }
            true
        }

        LaunchedEffect(Unit) {
            delay(300)
            refreshRevision++
            delay(700)
            refreshRevision++
            delay(1200)
            refreshRevision++
        }

        val accountInfo = remember(refreshRevision) {
            getUserAccountInfo(activity)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "中兴账号",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { activity.finish() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            BackArrowIcon(tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { openBrowserSettings(activity) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            SettingsIcon(activity = activity, tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
                        titleContentColor = if (isDark) TextPrimaryDark else TextPrimaryLight
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = if (isDark) SettingsBgDark else SettingsBgLight,
            contentWindowInsets = WindowInsets.navigationBars
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Account Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) CardBgDark else CardBgLight,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openAccountCenter(activity)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarView(activity = activity, isDark = isDark, avatar = accountInfo.avatar, size = 52.dp)

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = accountInfo.nickname,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                                )
                                Text(
                                    text = if (accountInfo.isLoggedIn) "已登录中兴账号" else "点击登录或管理中兴账号",
                                    fontSize = 13.sp,
                                    color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            ChevronRightIcon(tint = if (isDark) ChevronDark else ChevronLight)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }


                // Group: 常用数据
                item {
                    SettingsCardGroup(title = "常用数据", isDark = isDark) {
                        SettingsNavigationRow(
                            title = "书签",
                            summary = "查看与管理收藏的网页",
                            isDark = isDark,
                            onClick = {
                                openBrowserScheme(activity, "browser://cn.nubia.browser/bookmark")
                            }
                        )
                        SettingsRowDivider(isDark = isDark)
                        SettingsNavigationRow(
                            title = "历史记录",
                            summary = "浏览历史与搜索记录",
                            isDark = isDark,
                            onClick = {
                                openBrowserScheme(activity, "browser://cn.nubia.browser/history")
                            }
                        )
                        SettingsRowDivider(isDark = isDark)
                        SettingsNavigationRow(
                            title = "下载管理",
                            summary = "查看已下载的文件与任务",
                            isDark = isDark,
                            onClick = {
                                openBrowserScheme(activity, "browser://cn.nubia.browser/download")
                            }
                        )
                        SettingsRowDivider(isDark = isDark)
                        SettingsNavigationRow(
                            title = "离线网页与云服务",
                            summary = "保存离线内容并同步至云端",
                            isDark = isDark,
                            onClick = {
                                openBrowserScheme(activity, "browser://cn.nubia.browser/cloudService")
                            }
                        )
                    }
                }

                // Group: 账号服务
                item {
                    SettingsCardGroup(title = "账号与同步", isDark = isDark) {
                        SettingsNavigationRow(
                            title = "中兴账号中心",
                            summary = "管理个人资料、安全设置与关联设备",
                            isDark = isDark,
                            onClick = {
                                openAccountCenter(activity)
                            }
                        )
                        SettingsRowDivider(isDark = isDark)
                        SettingsNavigationRow(
                            title = "云同步数据",
                            summary = "自动同步书签与浏览数据到中兴云",
                            isDark = isDark,
                            onClick = {
                                openBrowserScheme(activity, "browser://cn.nubia.browser/cloudService")
                            }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
