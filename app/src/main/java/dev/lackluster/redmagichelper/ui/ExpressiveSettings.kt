@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.lackluster.redmagichelper.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlin.math.roundToInt
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import dev.lackluster.redmagichelper.HelperApplication
import dev.lackluster.redmagichelper.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import androidx.compose.runtime.Immutable
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Immutable
private data class AppearanceSettings(
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val predictiveBackEnabled: Boolean = false,
    val horizontalBackEnabled: Boolean = true,
    val pageScale: Float = 1f
) {
    companion object {
        fun read(preferences: SharedPreferences) = AppearanceSettings(
            colorStyle = preferences.getString("color_style", "TonalSpot") ?: "TonalSpot",
            colorSpec = preferences.getString("color_spec", "SPEC_2025") ?: "SPEC_2025",
            predictiveBackEnabled = preferences.getBoolean("predictive_back_enabled", false),
            horizontalBackEnabled = preferences.getBoolean("horizontal_back_enabled", true),
            pageScale = preferences.getFloat("page_scale", 1f).coerceIn(0.8f, 1.2f)
        )
    }
}

private val colorStyleOptions = listOf(
    "TonalSpot", "Neutral", "Vibrant", "Expressive", "Rainbow",
    "FruitSalad", "Monochrome", "Fidelity", "Content"
)

private val colorSpecOptions = listOf("SPEC_2025", "SPEC_2021")

/**
 * Main Activity implementing the Material 3 Expressive design specifications:
 * - Portrait phone (412x892dp)
 * - Light mode Purple colorScheme roles
 * - TopAppBar: 64dp, "红魔助手" aligned left
 * - List: 72dp item height, 3dp spacing, outer corners 28dp, inner corners 8dp
 * - Split button: "重启" with dropdown menu for restart & cancel
 * - Bottom NavigationBar: 80dp, "首页", "关于"
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Unlock high refresh rate (120Hz / 144Hz) asynchronously after initial window attachment
        // to avoid stalling first frame rendering with synchronous DisplayManager IPC.
        window.decorView.post {
            runCatching {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val highestMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
                if (highestMode != null) {
                    val lp = window.attributes
                    if (lp.preferredDisplayModeId != highestMode.modeId) {
                        lp.preferredDisplayModeId = highestMode.modeId
                        lp.preferredRefreshRate = highestMode.refreshRate
                        window.attributes = lp
                    }
                }
            }
        }

        val featurePreferences = getSharedPreferences("config", MODE_PRIVATE)
        val appearancePreferences = getSharedPreferences("appearance", MODE_PRIVATE)
        val initialAppearance = AppearanceSettings.read(appearancePreferences)
        setContent {
            var appearance by remember { mutableStateOf(initialAppearance) }
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity.density, systemDensity.fontScale, appearance.pageScale) {
                Density(systemDensity.density * appearance.pageScale, systemDensity.fontScale)
            }
            val updateAppearance: (AppearanceSettings) -> Unit = { updated ->
                appearance = updated.copy(pageScale = updated.pageScale.coerceIn(0.8f, 1.2f))
                appearancePreferences.edit()
                    .putString("color_style", appearance.colorStyle)
                    .putString("color_spec", appearance.colorSpec)
                    .putBoolean("predictive_back_enabled", appearance.predictiveBackEnabled)
                    .putBoolean("horizontal_back_enabled", appearance.horizontalBackEnabled)
                    .putFloat("page_scale", appearance.pageScale)
                    .apply()
            }
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                RedMagicTheme(appearance) {
                    MainAppScreen(featurePreferences, appearance, updateAppearance)
                }
            }
        }
    }
}

/**
 * Pure Material 3 Light Purple Color Scheme adhering to the exact requested roles.
 */
val PurpleColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF635A75),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFDED8E1),
    surfaceBright = Color(0xFFFEF7FF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
private fun RedMagicTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val seedColor = remember(context) {
        try {
            androidx.compose.material3.dynamicLightColorScheme(context).primary
        } catch (_: Throwable) {
            Color(0xFF6750A4)
        }
    }
    val colorScheme = remember(seedColor, isDark, appearance.colorStyle, appearance.colorSpec) {
        runCatching {
            makeDynamicColorScheme(seedColor, isDark, appearance.colorStyle, appearance.colorSpec)
        }.getOrDefault(PurpleColorScheme)
    }
    LaunchedEffect(isDark) {
        val window = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        content = content
    )
}

private val dynamicColorSchemeCache = java.util.concurrent.ConcurrentHashMap<String, ColorScheme>()

private fun makeDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    styleName: String,
    specName: String
): ColorScheme {
    val cacheKey = "${seedColor.value}_${isDark}_${styleName}_${specName}"
    dynamicColorSchemeCache[cacheKey]?.let { return it }

    val style = styleName.takeIf { it in colorStyleOptions } ?: "TonalSpot"
    val requestedSpec = runCatching { ColorSpec.SpecVersion.valueOf(specName) }
        .getOrDefault(ColorSpec.SpecVersion.SPEC_2025)
    val spec = if (requestedSpec == ColorSpec.SpecVersion.SPEC_2025 &&
        style !in setOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
    ) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        requestedSpec
    }
    val sourceColor = Hct.fromInt(seedColor.toArgb())
    val dynamicScheme = when (style) {
        "Neutral" -> SchemeNeutral(sourceColor, isDark, 0.0, spec)
        "Vibrant" -> SchemeVibrant(sourceColor, isDark, 0.0, spec)
        "Expressive" -> SchemeExpressive(sourceColor, isDark, 0.0, spec)
        "Rainbow" -> SchemeRainbow(sourceColor, isDark, 0.0, spec)
        "FruitSalad" -> SchemeFruitSalad(sourceColor, isDark, 0.0, spec)
        "Monochrome" -> SchemeMonochrome(sourceColor, isDark, 0.0, spec)
        "Fidelity" -> SchemeFidelity(sourceColor, isDark, 0.0, spec)
        "Content" -> SchemeContent(sourceColor, isDark, 0.0, spec)
        else -> SchemeTonalSpot(sourceColor, isDark, 0.0, spec)
    }
    val scheme = dynamicScheme.toMaterialColorScheme()
    dynamicColorSchemeCache[cacheKey] = scheme
    return scheme
}

private fun DynamicScheme.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(surfaceTint),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        primaryFixed = Color(primaryFixed),
        primaryFixedDim = Color(primaryFixedDim),
        onPrimaryFixed = Color(onPrimaryFixed),
        onPrimaryFixedVariant = Color(onPrimaryFixedVariant),
        secondaryFixed = Color(secondaryFixed),
        secondaryFixedDim = Color(secondaryFixedDim),
        onSecondaryFixed = Color(onSecondaryFixed),
        onSecondaryFixedVariant = Color(onSecondaryFixedVariant),
        tertiaryFixed = Color(tertiaryFixed),
        tertiaryFixedDim = Color(tertiaryFixedDim),
        onTertiaryFixed = Color(onTertiaryFixed),
        onTertiaryFixedVariant = Color(onTertiaryFixedVariant)
    )
}

/**
 * Navigation tabs specification: "首页"(home), "关于"(about).
 */
enum class MainNavTab(val title: String, val iconRes: Int) {
    HOME("首页", R.drawable.ic_home),
    SETTINGS("设置", R.drawable.ic_about)
}

@Serializable
private sealed interface MainDestination : NavKey {
    @Serializable
    @SerialName("root")
    data object Root : MainDestination

    @Serializable
    @SerialName("home")
    data object Home : MainDestination

    @Serializable
    @SerialName("settings")
    data object Settings : MainDestination

    @Serializable
    @SerialName("category")
    data class Category(val name: String) : MainDestination

    @Serializable
    @SerialName("about_device")
    data object AboutDevice : MainDestination

    @Serializable
    @SerialName("sub_screen")
    data class SubScreen(val key: String) : MainDestination
}

/**
 * Offscreen reference component explicitly required by user:
 * 放在屏幕之外的组件（公共部件或参考）：首先放置标题为“标题”的顶部应用栏，左侧是 menu，右侧是 more_vert 图标按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceTopAppBar(
    onMenuClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    TopAppBar(
        modifier = Modifier.height(64.dp),
        title = {
            Text(
                text = "标题",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "菜单",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "更多选项",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScreen(
    preferences: SharedPreferences,
    appearance: AppearanceSettings,
    onAppearanceChanged: (AppearanceSettings) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val backStack = rememberNavBackStack<MainDestination>(MainDestination.Root)
    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
    // The navigation host keeps both the outgoing and incoming entry composed during a
    // transition. Keep tab UI state at the host level so those two entry instances never
    // claim the same SaveableStateHolder key at once.
    val homeListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    val disabledBackDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
    }
    val currentDestination = backStack.lastOrNull() as? MainDestination ?: MainDestination.Root
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext
            as? HelperApplication

    var preferenceRevision by remember { mutableIntStateOf(0) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("已保存") }
    var isLoading by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    val modifiedKeys = remember { mutableSetOf<String>() }
    val originalValues = remember { mutableMapOf<String, Any?>() }

    val selectedCategory = (currentDestination as? MainDestination.Category)?.name?.let { name ->
        SettingCategory.values().firstOrNull { it.name == name }
    }
    val currentSubScreenKey = (currentDestination as? MainDestination.SubScreen)?.key
    val canNavigateBack = backStack.size > 1
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else if (selectedTab != MainNavTab.HOME) {
            selectedTab = MainNavTab.HOME
        }
    }
    val swipeBackDirection = when {
        !appearance.horizontalBackEnabled -> NavSwipeDirection.None
        LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }
    val triggerToast: (String) -> Unit = { msg ->
        toastMessage = msg
        toastVisible = true
        scope.launch {
            delay(2500)
            toastVisible = false
        }
    }

    val handlePreferenceChanged: (String, Any?, Any?) -> Unit = { key, oldValue, newValue ->
        if (!originalValues.containsKey(key)) {
            originalValues[key] = oldValue
        }
        val orig = originalValues[key]
        if (orig == newValue) {
            modifiedKeys.remove(key)
            originalValues.remove(key)
        } else {
            modifiedKeys.add(key)
        }
        hasChanges = modifiedKeys.isNotEmpty()
        preferenceRevision += 1
        triggerToast("已保存")
    }

    val handleCancel: () -> Unit = {
        val editor = preferences.edit()
        for ((key, origVal) in originalValues) {
            when (origVal) {
                is Boolean -> editor.putBoolean(key, origVal)
                is Int -> editor.putInt(key, origVal)
                is String -> editor.putString(key, origVal)
                null -> editor.remove(key)
            }
        }
        editor.apply()
        originalValues.clear()
        modifiedKeys.clear()
        hasChanges = false
        preferenceRevision += 1
        triggerToast("已撤销更改")
    }

    val handleRestart: () -> Unit = {
        val resolvedPkgs: List<String> = modifiedKeys.flatMap { getPackagesForSettingKey(it) }
            .filter { it.isNotBlank() }
            .distinct()
        val packagesToRestart: List<String> = if (resolvedPkgs.isEmpty()) {
            selectedCategory?.let { getPackagesForCategory(it) } ?: emptyList()
        } else {
            resolvedPkgs
        }
        isLoading = true
        hasChanges = false
        val count = packagesToRestart.size
        originalValues.clear()
        modifiedKeys.clear()
        scope.launch(Dispatchers.IO) {
            application?.awaitPendingConfigSync()
            val success = restartPackages(packagesToRestart)
            withContext(Dispatchers.Main) {
                isLoading = false
                val msg = if (count > 0) {
                    "已重启对应作用域进程 ($count 个)"
                } else {
                    "设置已保存并生效"
                }
                triggerToast(msg)
            }
        }
    }

    BackHandler(
        enabled = (canNavigateBack || selectedTab != MainNavTab.HOME) && !appearance.predictiveBackEnabled,
        onBack = navigateBack
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.surface,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = canNavigateBack,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium))
                                    + expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
                                    + shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            IconButton(
                                onClick = navigateBack,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = "返回",
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    title = {
                        val headerTitle = when {
                            currentSubScreenKey == "lockscreen_custom_font_settings" -> "锁屏时钟自定义字体"
                            currentDestination == MainDestination.AboutDevice -> "关于设备"
                            selectedCategory != null -> selectedCategory.title
                            selectedTab == MainNavTab.SETTINGS -> "设置"
                            else -> "红魔助手"
                        }
                        AnimatedContent(
                            targetState = headerTitle,
                            contentAlignment = Alignment.CenterStart,
                            transitionSpec = {
                                fadeIn(animationSpec = androidx.compose.animation.core.tween(150))
                                    .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
                            },
                            label = "topbar-title"
                        ) { currentTitle ->
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        scrolledContainerColor = colors.surfaceContainer,
                        titleContentColor = colors.onSurface
                    )
                )
            },
            bottomBar = {
                if (!canNavigateBack) {
                    NavigationBar(
                        modifier = Modifier.height(80.dp),
                        containerColor = colors.surfaceContainer,
                        contentColor = colors.onSurface
                    ) {
                        MainNavTab.values().forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (selectedTab != tab) {
                                        selectedTab = tab
                                    }
                                },
                                icon = {
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (selected) 1.08f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "nav-icon-scale"
                                    )
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp).scale(iconScale)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onSecondaryContainer,
                                    selectedTextColor = colors.onSurface,
                                    indicatorColor = colors.secondaryContainer,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                val showPredictiveBack = appearance.predictiveBackEnabled
                val navigationContent: @Composable () -> Unit = {
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                        onBack = navigateBack,
                        transition = NavTransitions.MiuixDefault,
                        effects = NavDisplayEffects.None
                    ) {
                        entry<MainDestination.Root> {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState == MainNavTab.SETTINGS) {
                                        (slideInHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            initialOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() }
                                        ) + fadeIn(
                                            animationSpec = tween(180, easing = LinearOutSlowInEasing)
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                targetOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() }
                                            ) + fadeOut(
                                                animationSpec = tween(140, easing = FastOutLinearInEasing)
                                            )
                                        )
                                    } else {
                                        (slideInHorizontally(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            initialOffsetX = { fullWidth -> -(fullWidth * 0.12f).toInt() }
                                        ) + fadeIn(
                                            animationSpec = tween(180, easing = LinearOutSlowInEasing)
                                        )).togetherWith(
                                            slideOutHorizontally(
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                targetOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() }
                                            ) + fadeOut(
                                                animationSpec = tween(140, easing = FastOutLinearInEasing)
                                            )
                                        )
                                    }.using(SizeTransform(clip = false))
                                },
                                label = "root-tab-transition"
                            ) { tab ->
                                when (tab) {
                                    MainNavTab.HOME -> HomeScreen(
                                        preferences = preferences,
                                        searchQuery = homeSearchQuery,
                                        onSearchQueryChange = { homeSearchQuery = it },
                                        listState = homeListState,
                                        onCategoryClick = { category ->
                                            val destination = MainDestination.Category(category.name)
                                            if (destination !in backStack) backStack.add(destination)
                                        }
                                    )
                                    MainNavTab.SETTINGS -> SettingsScreen(
                                        appearance = appearance,
                                        listState = settingsListState,
                                        onAppearanceChanged = onAppearanceChanged,
                                        onOpenAboutDevice = {
                                            if (MainDestination.AboutDevice !in backStack) {
                                                backStack.add(MainDestination.AboutDevice)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        entry<MainDestination.Home> {
                            HomeScreen(
                                preferences = preferences,
                                searchQuery = homeSearchQuery,
                                onSearchQueryChange = { homeSearchQuery = it },
                                listState = homeListState,
                                onCategoryClick = { category ->
                                    val destination = MainDestination.Category(category.name)
                                    if (destination !in backStack) backStack.add(destination)
                                }
                            )
                        }
                        entry<MainDestination.Settings> {
                            SettingsScreen(
                                appearance = appearance,
                                listState = settingsListState,
                                onAppearanceChanged = onAppearanceChanged,
                                onOpenAboutDevice = {
                                    if (MainDestination.AboutDevice !in backStack) {
                                        backStack.add(MainDestination.AboutDevice)
                                    }
                                }
                            )
                        }
                        entry<MainDestination.Category>(swipeDismiss = swipeBackDirection) { destination ->
                            val category = SettingCategory.values()
                                .firstOrNull { it.name == destination.name }
                            if (category != null) {
                                CategoryDetailView(
                                    preferences = preferences,
                                    category = category,
                                    revision = preferenceRevision,
                                    onPreferenceChanged = handlePreferenceChanged,
                                    onActionClick = { actionKey ->
                                        val next = MainDestination.SubScreen(actionKey)
                                        if (next !in backStack) backStack.add(next)
                                    }
                                )
                            }
                        }
                        entry<MainDestination.AboutDevice>(swipeDismiss = swipeBackDirection) {
                            AboutDeviceScreen()
                        }
                        entry<MainDestination.SubScreen>(swipeDismiss = swipeBackDirection) { destination ->
                            if (destination.key == "lockscreen_custom_font_settings") {
                                LockScreenFontSettingsScreen(
                                    preferences = preferences,
                                    revision = preferenceRevision,
                                    onPreferenceChanged = handlePreferenceChanged,
                                    onTriggerToast = triggerToast
                                )
                            }
                        }
                    }
                }
                if (showPredictiveBack) {
                    navigationContent()
                } else {
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides disabledBackDispatcherOwner
                    ) {
                        navigationContent()
                    }
                }

                // Specification: "下部居中放置“已保存”消息条（不带“撤销”操作）（部分覆盖在列表项组之上，绘制在前面）此处为Toast通知。显示在距屏幕底部 16dp 处，数秒后消失。高 48dp，圆角 8dp，背景 inverseSurface，文字 inverseOnSurface。"
                AnimatedVisibility(
                    visible = toastVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (selectedCategory == null) 96.dp else 24.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = colors.inverseSurface,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = toastMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inverseOnSurface
                            )
                        }
                    }
                }

                // Specification: 下部靠右放置“重启”（带 restart 图标）的填充拆分按钮（右侧为带向下箭头的菜单段）
                // 规范：重启的图标 只有在改动的时候 显示出来，右侧下拉支持“重启”与“撤销”
                AnimatedVisibility(
                    visible = hasChanges,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                            slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it / 3 },
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it / 3 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = if (selectedCategory == null) {
                                96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            } else {
                                24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            }
                        )
                ) {
                    ExpressiveRestartSplitButton(
                        onRestart = handleRestart,
                        onCancel = handleCancel
                    )
                }
            }
        }

        // Full-screen loading scrim covering the top and bottom navigation bars.
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = 0.38f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.primaryContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LoadingIndicator(
                            modifier = Modifier.size(40.dp),
                            color = colors.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 1: Home Screen with search functionality and M3 Expressive split button.
 */
@Composable
private fun HomeScreen(
    preferences: SharedPreferences,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    listState: LazyListState,
    onCategoryClick: (SettingCategory) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val categories = remember { SettingCategory.values().toList() }
    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { cat ->
                cat.title.contains(searchQuery, ignoreCase = true) ||
                        cat.summary.contains(searchQuery, ignoreCase = true) ||
                        categorySettings[cat].orEmpty().any { item ->
                            item.title.contains(searchQuery, ignoreCase = true) ||
                                    (item.summary?.contains(searchQuery, ignoreCase = true) == true)
                        }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "搜索 Hook 选项或功能…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "搜索",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "清除",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerLow,
                    unfocusedContainerColor = colors.surfaceContainerLow,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outlineVariant
                )
            )

            // Category list with M3 Expressive grouped items
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(
                    items = filteredCategories,
                    key = { _, cat -> cat.name },
                    contentType = { _, _ -> "category_item" }
                ) { index, category ->
                    val shape = getExpressiveItemShape(index, filteredCategories.size)
                    ExpressiveCategoryItem(
                        category = category,
                        shape = shape,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Filled Split Button:
 * Left side: Primary action "重启" with restart icon.
 * Right side: Dropdown anchor with downward arrow; expands 2 menu items: "重启" and "撤销".
 * Flips upward automatically when bottom space is constrained.
 */
@Composable
private fun ExpressiveRestartSplitButton(
    onRestart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colors.primary,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action button: restart icon + "重启"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .clickable(onClick = onRestart)
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_restart_alt),
                    contentDescription = "重启",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "重启",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary
                )
            }

            // Segment divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.onPrimary.copy(alpha = 0.25f))
            )

            // Menu trigger segment: downward arrow
            Box {
                IconButton(
                    onClick = { menuExpanded = !menuExpanded },
                    modifier = Modifier
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = "展开操作",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (menuExpanded) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "重启",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restart_alt),
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRestart()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "撤销",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onCancel()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates the M3 Expressive grouped list item shape:
 * - Single item: 28dp on all four corners
 * - First item: 28dp outer top, 8dp inner bottom
 * - Middle item: 8dp on all corners
 * - Last item: 8dp inner top, 28dp outer bottom
 */
fun getExpressiveItemShape(index: Int, count: Int): RoundedCornerShape {
    return when {
        count <= 1 -> RoundedCornerShape(28.dp)
        index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        index == count - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        else -> RoundedCornerShape(8.dp)
    }
}

/**
 * 72dp List item with 40dp primaryContainer circular icon container, 24dp icon,
 * bodyLarge title, bodyMedium summary, surfaceContainerLow background, and ripple feedback.
 */
@Composable
private fun ExpressiveCategoryItem(
    category: SettingCategory,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val count = remember(category) { categorySettings[category].orEmpty().size }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(
                indication = ripple(color = colors.primary.copy(alpha = 0.12f)),
                interactionSource = null,
                onClick = onClick
            ),
        shape = shape,
        color = colors.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 40dp circle with primaryContainer background, 24dp icon inside
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(category.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(Modifier.width(14.dp))

            // Main text (bodyLarge) and supporting text (bodyMedium in onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.secondaryContainer
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = category.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Category Detail View showing all settings with M3 Expressive card grouping.
 */
@Composable
private fun CategoryDetailView(
    preferences: SharedPreferences,
    category: SettingCategory,
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit,
    onActionClick: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val settingsList = remember(category, revision) {
        categorySettings[category].orEmpty().filter { setting ->
            setting.dependencyKey == null || readBoolean(preferences, setting.dependencyKey!!, false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = colors.primaryContainer
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(category.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = category.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(
                items = settingsList,
                key = { _, item -> item.id },
                contentType = { _, item -> item::class.java.simpleName }
            ) { index, item ->
                val shape = getExpressiveItemShape(index, settingsList.size)
                when (item) {
                    is SettingItem.Toggle -> ExpressiveToggleItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Number -> ExpressiveNumberItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Text -> ExpressiveTextItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Slider -> ExpressiveSliderItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Action -> ExpressiveActionItem(
                        setting = item,
                        shape = shape,
                        onClick = { onActionClick(item.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveActionItem(
    setting: SettingItem.Action,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = shape,
        color = colors.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                setting.summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveToggleItem(
    preferences: SharedPreferences,
    setting: SettingItem.Toggle,
    shape: RoundedCornerShape,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var checked by remember(setting.key) {
        mutableStateOf(readBoolean(preferences, setting.key, setting.defaultValue))
    }
    val update: (Boolean) -> Unit = { value ->
        if (checked != value) {
            val oldValue = checked
            checked = value
            preferences.edit().putBoolean(setting.key, value).apply()
            onPreferenceChanged(setting.key, oldValue, value)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { update(!checked) },
        shape = shape,
        color = colors.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                setting.summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = update,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary
                )
            )
        }
    }
}

@Composable
private fun ExpressiveNumberItem(
    preferences: SharedPreferences,
    setting: SettingItem.Number,
    shape: RoundedCornerShape,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var isEdited by remember(setting.key) { mutableStateOf(false) }
    var textValue by remember(setting.key) {
        mutableStateOf(readNumber(preferences, setting.key, setting.defaultValue).toString())
    }
    var hasHadFocus by remember { mutableStateOf(false) }

    val save = {
        if (isEdited) {
            isEdited = false
            val parsed = textValue.toIntOrNull() ?: setting.defaultValue
            val current = readNumber(preferences, setting.key, setting.defaultValue)
            if (parsed != current) {
                val oldValue = current
                textValue = parsed.toString()
                preferences.edit().putInt(setting.key, parsed).apply()
                onPreferenceChanged(setting.key, oldValue, parsed)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it.filter(Char::isDigit)
                    isEdited = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasHadFocus = true
                        } else if (hasHadFocus && isEdited) {
                            save()
                        }
                    },
                label = { Text(setting.title) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outlineVariant
                )
            )
            setting.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveTextItem(
    preferences: SharedPreferences,
    setting: SettingItem.Text,
    shape: RoundedCornerShape,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var isEdited by remember(setting.key) { mutableStateOf(false) }
    var textValue by remember(setting.key) {
        mutableStateOf(readText(preferences, setting.key, setting.defaultValue))
    }
    var hasHadFocus by remember { mutableStateOf(false) }

    val save = {
        if (isEdited) {
            isEdited = false
            val normalized = textValue.ifBlank { setting.defaultValue }
            val current = readText(preferences, setting.key, setting.defaultValue)
            if (normalized != current) {
                val oldValue = current
                textValue = normalized
                preferences.edit().putString(setting.key, normalized).apply()
                onPreferenceChanged(setting.key, oldValue, normalized)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    isEdited = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasHadFocus = true
                        } else if (hasHadFocus && isEdited) {
                            save()
                        }
                    },
                label = { Text(setting.title) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outlineVariant
                )
            )
            setting.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSliderItem(
    preferences: SharedPreferences,
    setting: SettingItem.Slider,
    shape: RoundedCornerShape,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val initialValue = remember(setting.key) {
        readNumber(preferences, setting.key, setting.defaultValue).coerceIn(setting.minValue, setting.maxValue)
    }
    var sliderPosition by remember(setting.key) {
        mutableFloatStateOf(initialValue.toFloat())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Surface(
                    color = colors.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${sliderPosition.toInt()} ${setting.unit}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            setting.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            val steps = if (setting.step > 0) {
                ((setting.maxValue - setting.minValue) / setting.step) - 1
            } else 0

            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                },
                onValueChangeFinished = {
                    val finalInt = sliderPosition.toInt()
                    val oldValue = readNumber(preferences, setting.key, setting.defaultValue)
                    if (oldValue != finalInt) {
                        preferences.edit().putInt(setting.key, finalInt).apply()
                        onPreferenceChanged(setting.key, oldValue, finalInt)
                    }
                },
                valueRange = setting.minValue.toFloat()..setting.maxValue.toFloat(),
                steps = if (steps > 0) steps else 0,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class SystemFontItem(
    val file: File,
    val name: String,
    val isRedMagic: Boolean
)

private fun scanSystemFonts(): List<SystemFontItem> {
    val dirs = listOf(
        File("/system/fonts"),
        File("/product/fonts"),
        File("/system_ext/fonts")
    )
    val result = mutableListOf<SystemFontItem>()
    val seenPaths = HashSet<String>()
    for (dir in dirs) {
        if (!dir.exists() || !dir.isDirectory) continue
        val files = dir.listFiles() ?: continue
        for (f in files) {
            if (!f.isFile || !f.canRead()) continue
            val name = f.name
            if (name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)) {
                if (seenPaths.add(f.absolutePath)) {
                    val isRm = name.contains("RedMagic", ignoreCase = true) ||
                            name.contains("ZDigit", ignoreCase = true) ||
                            name.contains("Nubia", ignoreCase = true)
                    result.add(SystemFontItem(f, name, isRm))
                }
            }
        }
    }
    result.sortWith(compareByDescending<SystemFontItem> { it.isRedMagic }.thenBy { it.name })
    return result
}

private fun queryFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) return cursor.getString(idx)
                }
            }
        } catch (_: Throwable) {}
    }
    return uri.path?.let { File(it).name }
}

private fun getFontWeightName(weight: Int): String = when {
    weight <= 150 -> "极细 Thin"
    weight <= 250 -> "特细 ExtraLight"
    weight <= 350 -> "细 Light"
    weight <= 450 -> "常规 Regular"
    weight <= 550 -> "中等 Medium"
    weight <= 650 -> "半粗 SemiBold"
    weight <= 750 -> "粗体 Bold"
    weight <= 850 -> "特粗 ExtraBold"
    else -> "极粗 Black"
}

/**
 * Secondary screen for configuring lockscreen clock custom font, system-installed fonts,
 * imported fonts, font weight (100-900), and font size (30-120sp).
 */
@Composable
private fun LockScreenFontSettingsScreen(
    preferences: SharedPreferences,
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit,
    onTriggerToast: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var masterEnabled by remember(revision) {
        mutableStateOf(readBoolean(preferences, "lockscreen_custom_font_switch", false))
    }
    var systemFontPath by remember(revision) {
        mutableStateOf(readText(preferences, "lockscreen_system_font_path", ""))
    }
    var importedFontName by remember(revision) {
        mutableStateOf(readText(preferences, "lockscreen_custom_font_imported_name", ""))
    }
    var fontWeight by remember(revision) {
        mutableIntStateOf(readNumber(preferences, "lockscreen_custom_font_weight", 400))
    }
    var fontSize by remember(revision) {
        mutableIntStateOf(readNumber(preferences, "lockscreen_custom_font_size", 70))
    }
    var dateSize by remember(revision) {
        mutableIntStateOf(readNumber(preferences, "lockscreen_custom_date_size", 24))
    }

    var systemFonts by remember { mutableStateOf<List<SystemFontItem>>(emptyList()) }
    var fontSearchQuery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = scanSystemFonts()
            withContext(Dispatchers.Main) {
                systemFonts = list
            }
        }
    }

    val filteredSystemFonts = remember(systemFonts, fontSearchQuery) {
        if (fontSearchQuery.isBlank()) systemFonts
        else systemFonts.filter { it.name.contains(fontSearchQuery, ignoreCase = true) }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val fileName = queryFileName(context, uri) ?: "custom_font.ttf"
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val dest = File(context.filesDir, "custom_lockscreen_font.ttf")
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        val oldVal = importedFontName
                        importedFontName = fileName
                        preferences.edit()
                            .putString("lockscreen_custom_font_imported_name", fileName)
                            .putLong("lockscreen_custom_font_timestamp", System.currentTimeMillis())
                            .apply()
                        onPreferenceChanged("lockscreen_custom_font_imported_name", oldVal, fileName)
                        onTriggerToast("已导入外部字体: $fileName")
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        onTriggerToast("导入失败: ${e.message}")
                    }
                }
            }
        }
    }

    val clearImportedFont: () -> Unit = {
        val dest = File(context.filesDir, "custom_lockscreen_font.ttf")
        if (dest.exists()) dest.delete()
        val oldVal = importedFontName
        importedFontName = ""
        preferences.edit()
            .remove("lockscreen_custom_font_imported_name")
            .remove("lockscreen_custom_font_timestamp")
            .apply()
        onPreferenceChanged("lockscreen_custom_font_imported_name", oldVal, "")
        onTriggerToast("已清除导入字体")
    }

    val selectSystemFont: (String) -> Unit = { path ->
        val oldVal = systemFontPath
        val newVal = if (oldVal == path) "" else path
        systemFontPath = newVal
        preferences.edit().putString("lockscreen_system_font_path", newVal).apply()
        onPreferenceChanged("lockscreen_system_font_path", oldVal, newVal)
        if (newVal.isNotEmpty()) {
            onTriggerToast("已选择系统字体: ${File(newVal).name}")
        } else {
            onTriggerToast("已取消选择系统字体")
        }
    }

    val previewTypeface = remember(systemFontPath, importedFontName, fontWeight) {
        var tf: Typeface? = null
        if (systemFontPath.isNotBlank()) {
            val f = File(systemFontPath)
            if (f.exists()) {
                try { tf = Typeface.createFromFile(f) } catch (_: Throwable) {}
            }
        }
        if (tf == null && importedFontName.isNotBlank()) {
            val f = File(context.filesDir, "custom_lockscreen_font.ttf")
            if (f.exists()) {
                try { tf = Typeface.createFromFile(f) } catch (_: Throwable) {}
            }
        }
        if (tf != null) {
            try {
                Typeface.create(tf, fontWeight.coerceIn(100, 1000), false)
            } catch (_: Throwable) { tf }
        } else {
            Typeface.defaultFromStyle(if (fontWeight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Master Toggle Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val newVal = !masterEnabled
                        val oldVal = masterEnabled
                        masterEnabled = newVal
                        preferences.edit().putBoolean("lockscreen_custom_font_switch", newVal).apply()
                        onPreferenceChanged("lockscreen_custom_font_switch", oldVal, newVal)
                    },
                shape = RoundedCornerShape(20.dp),
                color = if (masterEnabled) colors.primaryContainer else colors.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "开启锁屏时钟自定义字体",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (masterEnabled) colors.onPrimaryContainer else colors.onSurface
                        )
                        Text(
                            text = "开启后支持选择系统内置字体、导入外部字体，并调节粗细与显示大小",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (masterEnabled) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { newVal ->
                            val oldVal = masterEnabled
                            masterEnabled = newVal
                            preferences.edit().putBoolean("lockscreen_custom_font_switch", newVal).apply()
                            onPreferenceChanged("lockscreen_custom_font_switch", oldVal, newVal)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary
                        )
                    )
                }
            }
        }

        if (!masterEnabled) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "功能已禁用",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "请开启上方总开关后，配置系统内置字体、导入自定义外部字体，以及调节字体粗细与大小。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Live Clock Preview Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141218),
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "锁屏时钟实时预览",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCAC4D0)
                        )
                        Spacer(Modifier.height(8.dp))
                        val previewSp = (fontSize * 0.7f).coerceIn(28f, 76f).sp
                        Text(
                            text = "12:38",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily(previewTypeface),
                                fontSize = previewSp,
                                lineHeight = previewSp
                            ),
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "10月1日 星期四 · 88%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = dateSize.sp),
                            color = Color(0xFFE6E0E9)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "时间: ${fontSize}sp | 日期: ${dateSize}sp | 粗细: $fontWeight (${getFontWeightName(fontWeight)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF938F99)
                        )
                    }
                }
            }

            // Section 1: System-installed fonts
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "系统内安装字体",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            if (systemFontPath.isNotBlank()) {
                                TextButton(
                                    onClick = { selectSystemFont(systemFontPath) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("清除选择", color = colors.error, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Text(
                            text = "已扫描系统目录 (/system/fonts, /product/fonts)，列表形式展现，点击即可选用：",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = fontSearchQuery,
                            onValueChange = { fontSearchQuery = it },
                            placeholder = { Text("搜索系统字体 (共 ${systemFonts.size} 个)...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outlineVariant
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Scrollable bounded list of fonts
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            if (filteredSystemFonts.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("未找到匹配的系统字体", color = colors.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(filteredSystemFonts, key = { it.file.absolutePath }) { item ->
                                        val isSelected = systemFontPath == item.file.absolutePath
                                        val itemTypeface = remember(item.file.absolutePath) {
                                            try { Typeface.createFromFile(item.file) } catch (_: Throwable) { null }
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectSystemFont(item.file.absolutePath) },
                                            color = if (isSelected) colors.primaryContainer else colors.surface
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = item.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                                                        )
                                                        if (item.isRedMagic) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = colors.primary
                                                            ) {
                                                                Text(
                                                                    text = "红魔定制",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = colors.onPrimary,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (itemTypeface != null) {
                                                        Text(
                                                            text = "12:38  RedMagic 88%",
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                fontFamily = FontFamily(itemTypeface),
                                                                fontSize = 17.sp
                                                            ),
                                                            color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = colors.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "✓",
                                                                color = colors.onPrimary,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Import custom font (.ttf/.otf)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "导入外部自定义字体",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "从手机存储导入 .ttf 或 .otf 格式字体文件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        if (importedFontName.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "已导入: $importedFontName",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "已保存在应用私有存储中，跨进程由 ConfigProvider 自动共享",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { clearImportedFont() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("删除", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                fontPickerLauncher.launch("font/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (importedFontName.isNotBlank()) "重新选择导入字体文件 (.ttf/.otf)" else "选择并导入外部字体文件 (.ttf/.otf)")
                        }
                    }
                }
            }

            // Section 3: Sliders for Weight & Size
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "时钟字体粗细与大小调节",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(14.dp))

                        // Slider 1: Font Weight (100 - 900)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "字体粗细 (Weight)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                            Surface(
                                color = colors.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$fontWeight (${getFontWeightName(fontWeight)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = fontWeight.toFloat(),
                            onValueChange = { fontWeight = it.roundToInt() },
                            onValueChangeFinished = {
                                val oldVal = readNumber(preferences, "lockscreen_custom_font_weight", 400)
                                if (oldVal != fontWeight) {
                                    preferences.edit().putInt("lockscreen_custom_font_weight", fontWeight).apply()
                                    onPreferenceChanged("lockscreen_custom_font_weight", oldVal, fontWeight)
                                }
                            },
                            valueRange = 100f..900f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // Slider 2: Clock font size (30 - 120 sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "时钟大小 (Size)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                            Surface(
                                color = colors.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$fontSize sp",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { fontSize = it.roundToInt() },
                            onValueChangeFinished = {
                                val oldVal = readNumber(preferences, "lockscreen_custom_font_size", 70)
                                if (oldVal != fontSize) {
                                    preferences.edit().putInt("lockscreen_custom_font_size", fontSize).apply()
                                    onPreferenceChanged("lockscreen_custom_font_size", oldVal, fontSize)
                                }
                            },
                            valueRange = 30f..120f,
                            steps = 44,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "日期大小 (Size)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                            Surface(
                                color = colors.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$dateSize sp",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = dateSize.toFloat(),
                            onValueChange = { dateSize = it.roundToInt() },
                            onValueChangeFinished = {
                                val oldVal = readNumber(preferences, "lockscreen_custom_date_size", 24)
                                if (oldVal != dateSize) {
                                    preferences.edit().putInt("lockscreen_custom_date_size", dateSize).apply()
                                    onPreferenceChanged("lockscreen_custom_date_size", oldVal, dateSize)
                                }
                            },
                            valueRange = 12f..48f,
                            steps = 35,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SettingsScreen(
    appearance: AppearanceSettings,
    listState: LazyListState,
    onAppearanceChanged: (AppearanceSettings) -> Unit,
    onOpenAboutDevice: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                Column {
                    SettingsDropdownRow(
                        title = "色彩风格",
                        iconRes = R.drawable.ic_palette_settings,
                        summary = "选择 Material You 配色方案",
                        selected = appearance.colorStyle,
                        options = colorStyleOptions,
                        onSelected = { onAppearanceChanged(appearance.copy(colorStyle = it)) }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp))
                    SettingsDropdownRow(
                        title = "色彩标准",
                        iconRes = R.drawable.ic_tune_settings,
                        summary = "选择 Material 色彩算法版本",
                        selected = appearance.colorSpec.toColorSpecLabel(),
                        options = colorSpecOptions.map { it.toColorSpecLabel() },
                        onSelected = { label ->
                            val selectedSpec = if (label == "Spec_2021") "SPEC_2021" else "SPEC_2025"
                            onAppearanceChanged(appearance.copy(colorSpec = selectedSpec))
                        }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp))
                    SettingsSwitchRow(
                        title = "启用预测性返回手势",
                        iconRes = R.drawable.ic_gesture_settings,
                        summary = "显示 Android 返回手势的页面预览",
                        checked = appearance.predictiveBackEnabled,
                        onCheckedChange = {
                            onAppearanceChanged(appearance.copy(predictiveBackEnabled = it))
                        }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp))
                    SettingsSwitchRow(
                        title = "横移返回手势",
                        iconRes = R.drawable.ic_arrow_back,
                        summary = "从屏幕左侧向右滑动返回上一级",
                        checked = appearance.horizontalBackEnabled,
                        onCheckedChange = {
                            onAppearanceChanged(appearance.copy(horizontalBackEnabled = it))
                        }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp))
                    PageScaleRow(
                        iconRes = R.drawable.ic_zoom_in_settings,
                        scale = appearance.pageScale,
                        onScaleChanged = { onAppearanceChanged(appearance.copy(pageScale = it)) }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp))
                    SettingsActionRow(
                        title = "关于设备",
                        iconRes = R.drawable.ic_about,
                        summary = "查看 SELinux 状态、设备型号、系统指纹和版本",
                        onClick = onOpenAboutDevice
                    )
                }
            }
        }
    }
}

private fun String.toColorSpecLabel(): String = when (this) {
    "SPEC_2021", "Spec_2021" -> "Spec_2021"
    else -> "Spec_2025"
}

@Composable
private fun SettingsDropdownRow(
    title: String,
    iconRes: Int,
    summary: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember(title) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsLeadingIcon(iconRes)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.secondaryContainer.copy(alpha = 0.65f),
                contentColor = colors.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selected,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = "选择$title",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (expanded) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.surfaceContainerHigh)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            trailingIcon = if (option == selected) {
                                { Text("✓", color = colors.primary) }
                            } else null,
                            onClick = {
                                expanded = false
                                onSelected(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    iconRes: Int,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsLeadingIcon(iconRes)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PageScaleRow(iconRes: Int, scale: Float, onScaleChanged: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var sliderValue by remember(scale) { mutableFloatStateOf(scale) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SettingsLeadingIcon(iconRes)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("页面缩放", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Text("调整应用界面和文字的整体尺寸", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Text("${(sliderValue * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        val snapped = (sliderValue * 20f).roundToInt() / 20f
                        sliderValue = snapped
                        onScaleChanged(snapped)
                    },
                    valueRange = 0.8f..1.2f,
                    steps = 7
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    iconRes: Int,
    summary: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsLeadingIcon(iconRes)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsLeadingIcon(iconRes: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = colors.secondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AboutDeviceScreen() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var selinuxStatus by remember { mutableStateOf<String?>(null) }
    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (_: Throwable) {
            "未知"
        }
    }
    LaunchedEffect(Unit) {
        selinuxStatus = withContext(Dispatchers.IO) { readSelinuxStatus() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                Column {
                    DeviceInfoRow(R.drawable.ic_security, "SELinux状态", selinuxStatus, isLoading = selinuxStatus == null)
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp, end = 18.dp))
                    DeviceInfoRow(R.drawable.ic_device, "设备型号", "${Build.MANUFACTURER} ${Build.MODEL}")
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp, end = 18.dp))
                    DeviceInfoRow(R.drawable.ic_fingerprint, "系统指纹", Build.FINGERPRINT)
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f), modifier = Modifier.padding(start = 74.dp, end = 18.dp))
                    DeviceInfoRow(R.drawable.ic_info, "软件版本", "v$versionName")
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(iconRes: Int, label: String, value: String?, isLoading: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsLeadingIcon(iconRes)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.primary
                    )
                    Text("正在检查…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                }
            } else {
                Text(
                    text = value.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    softWrap = true
                )
            }
        }
    }
}

private fun readSelinuxStatus(): String {
    try {
        val enforcementFile = File("/sys/fs/selinux/enforce")
        if (enforcementFile.canRead()) {
            return when (enforcementFile.readText().trim()) {
                "1" -> "Enforcing"
                "0" -> "Permissive"
                else -> "无法读取"
            }
        }
    } catch (_: Throwable) {
    }
    readGetenforceStatus(listOf("/system/bin/getenforce"), 1200L)?.let { return it }
    return readGetenforceStatus(listOf("su", "-c", "/system/bin/getenforce"), 1800L)
        ?: "无权限（需要 Root）"
}

private fun readGetenforceStatus(command: List<String>, timeoutMillis: Long): String? {
    var process: Process? = null
    return try {
        process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            null
        } else {
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            if (process.exitValue() != 0) {
                null
            } else {
                when (output.lowercase()) {
                    "enforcing" -> "Enforcing"
                    "permissive" -> "Permissive"
                    "disabled" -> "Disabled"
                    else -> null
                }
            }
        }
    } catch (_: Throwable) {
        null
    } finally {
        try {
            process?.inputStream?.close()
        } catch (_: Throwable) {
        }
        try {
            process?.destroy()
        } catch (_: Throwable) {
        }
    }
}

// Preference reading helpers
private fun readBoolean(preferences: SharedPreferences, key: String, defaultValue: Boolean): Boolean = try {
    preferences.getBoolean(key, defaultValue)
} catch (_: Throwable) {
    defaultValue
}

private fun readNumber(preferences: SharedPreferences, key: String, defaultValue: Int): Int = try {
    preferences.getInt(key, defaultValue)
} catch (_: Throwable) {
    preferences.all[key]?.toString()?.toIntOrNull() ?: defaultValue
}

private fun readText(preferences: SharedPreferences, key: String, defaultValue: String): String = try {
    preferences.getString(key, defaultValue) ?: defaultValue
} catch (_: Throwable) {
    preferences.all[key]?.toString() ?: defaultValue
}

private fun detectRoot(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        try {
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor() == 0 && output.contains("uid=0")
        } finally {
            process.destroy()
        }
    } catch (_: Throwable) {
        false
    }
}

private fun restartGameServices(): Boolean {
    val packages = listOf(
        "cn.nubia.gameassist",
        "cn.nubia.gamelauncher",
        "cn.nubia.gamehelperline",
        "cn.nubia.gamehelpmodule"
    )
    val command = packages.joinToString("; ") { "am force-stop $it" }
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        try {
            process.waitFor() == 0
        } finally {
            process.destroy()
        }
    } catch (_: Throwable) {
        false
    }
}

private fun getPackagesForCategory(category: SettingCategory): List<String> = when (category) {
    SettingCategory.SYSTEM_UI -> listOf("com.android.systemui")
    SettingCategory.GAMING -> listOf(
        "cn.nubia.gameassist",
        "cn.nubia.gamelauncher",
        "cn.nubia.gamehelpmodule",
        "cn.nubia.gamehelperline",
        "cn.zte.gamefloat",
        "com.zte.game.plugintrigger"
    )
    SettingCategory.STORE -> listOf("cn.nubia.neostore", "cn.nubia.neogamecenter")
    SettingCategory.DESKTOP -> listOf(
        "com.zte.mifavor.launcher",
        "com.zte.beautify"
    )
    SettingCategory.SETTINGS -> listOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.zte.zdm",
        "com.zte.recommend"
    )
    SettingCategory.FILES -> listOf("cn.nubia.filebrowser")
    SettingCategory.OTHERS -> listOf("com.android.nfc")
    SettingCategory.BROWSER -> listOf("cn.nubia.browser")
}

private fun getPackagesForSettingKey(key: String): List<String> = when (key) {
    // 浏览器适配
    "browser_open_search",
    "browser_skip_splash",
    "browser_remove_info_tabs",
    "browser_remove_homepage_info",
    "browser_slim_mine",
    "browser_remove_hot_words",
    "browser_remove_search_shortcuts",
    "browser_remove_download_hot",
    "browser_remove_download_recommend" -> listOf("cn.nubia.browser")

    // 系统界面 (SystemUI)
    "systemui_screen_off_show_seconds",
    "systemui_screen_off_period",
    "statusbar_clock_pull_down_show_period",
    "statusbar_clock_show_seconds",
    "lockscreen_custom_font_switch",
    "lockscreen_system_font_path",
    "lockscreen_custom_font_imported_name",
    "lockscreen_custom_font_timestamp",
    "lockscreen_custom_font_weight",
    "lockscreen_custom_font_size",
    "lockscreen_custom_date_size",
    "statusbar_notif_max",
    "statusbar_notif_max_value",
    "remove_alert_windows_notification",
    "volume_disable_safety_warning",
    "disable_flag_secure",
    "disable_flag_secure_enhanced" -> listOf("com.android.systemui")

    // 通用与游戏
    "game_fuction_unfreeze_switch" -> listOf(
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist",
        "cn.zte.gamefloat",
        "cn.nubia.gamehelpmodule"
    )
    "game_force_overlay_capability" -> listOf(
        "cn.zte.gamefloat",
        "cn.nubia.gameassist"
    )
    "game_allow_third_party_freeform" -> listOf(
        "cn.zte.gamefloat",
        "cn.nubia.gameassist",
        "com.zte.recommend"
    )
    "game_magic_voice_unfreeze" -> listOf(
        "cn.nubia.gameassist",
        "cn.zte.gamefloat"
    )
    "game_space_sound_effects_unfreeze" -> listOf(
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist"
    )
    "game_sight_assist_unfreeze" -> listOf(
        "cn.nubia.gameassist",
        "cn.zte.gamefloat"
    )
    "game_shoulder_key_enabled" -> listOf(
        "cn.nubia.gamelauncher",
        "com.nubia.tgkservice",
        "cn.nubia.tgk",
        "cn.nubia.gamehelperline",
        "cn.nubia.gameassist",
        "cn.nubia.gamehelpmodule"
    )
    "game_shoulder_rapid_fire_multiplier" -> listOf(
        "cn.nubia.gamelauncher",
        "com.nubia.tgkservice",
        "cn.nubia.tgk"
    )
    "game_safe_feature_gate" -> listOf(
        "cn.nubia.gamehelperline",
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist",
        "cn.nubia.gamehelpmodule"
    )
    "game_4d_vibration_unlimited_apps" -> listOf("cn.nubia.gameassist")
    "game_super_resolution_suppress_toasts" -> listOf(
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist"
    )
    "acive_mode_switch",
    "game_super_resolution_unlimited_apps",
    "game_space_devil_mode_hide_prompt" -> listOf(
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist"
    )
    "game_space_combo_speed_enabled",
    "game_space_combo_speed_rate" -> listOf(
        "cn.nubia.gamelauncher",
        "cn.nubia.gameassist"
    )
    "ai_trigger_switch",
    "ai_trigger_template_scan_ms",
    "ai_trigger_click_ms",
    "ai_trigger_cooldown_ms",
    "ai_trigger_yolo_scan_ms" -> listOf(
        "com.zte.game.plugintrigger",
        "cn.nubia.gamelauncher"
    )
    "double_any_app",
    "rm_low_memory_limit" -> listOf(
        "com.android.settings",
        "com.zte.mifavor.launcher"
    )

    // MTP分类浏览 (仅限 cn.nubia.filebrowser)
    "hide_mtp_category_browse",
    "mtp_rename_root_name_switch",
    "mtp_rename_root_name" -> listOf("cn.nubia.filebrowser")

    // 应用商店与游戏中心
    "neostore_skip_splash",
    "neostore_download_enabled",
    "neostore_download_count",
    "neostore_auto_sign" -> listOf("cn.nubia.neostore")

    "neogamecenter_skip_splash",
    "neogame_local_svip" -> listOf("cn.nubia.neogamecenter")

    // 桌面
    "cancel_trial_login",
    "beautify_skip_splash_ads" -> listOf("com.zte.beautify")

    "allow_thirdparty_launcher",
    "system_desktop_recent_task_display_memory",
    "system_time_component_desktop_switch",
    "system_time_component_desktop_show_seconds",
    "system_time_component_desktop_show_period",
    "desktop_app_icon_customize_switch" -> listOf("com.zte.mifavor.launcher")

    // 安装与系统设置
    "disable_system_update",
    "update_package_address" -> listOf("com.zte.zdm")

    "hide_store_install_prompt",
    "skip_pkg_scan",
    "cts_test_installer",
    "hide_evolution_mode_toggle" -> listOf("com.android.packageinstaller")

    "disable_usb_installation_and_switch_account_verification",
    "disable_attery_percentage_display_setting_option",
    "automatic_screen_off",
    "time_picker_period" -> listOf("com.android.settings")

    "remove_restrictions_window",
    "remove_restrictions_window_number" -> listOf("com.zte.recommend", "com.android.systemui")

    "android_remove_intent_hijack_content" -> listOf("com.android.packageinstaller")

    "system_framework_lock_screen_timeout",
    "system_framework_lock_screen_timeout_value" -> listOf("com.android.settings", "com.android.systemui")

    // 其他功能与 NFC
    "mute_nfc_sound",
    "nfc_allow_screen_off_recognition" -> listOf("com.android.nfc")

    else -> emptyList()
}

private fun restartPackages(packages: Collection<String>): Boolean {
    val pkgs = packages.filter { it.isNotBlank() }.distinct()
    if (pkgs.isEmpty()) return true
    val command = pkgs.joinToString("; ") { pkg ->
        if (pkg == "com.android.systemui") {
            "pkill -f com.android.systemui"
        } else {
            "am force-stop $pkg"
        }
    }
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        try {
            process.waitFor()
            process.exitValue() == 0
        } finally {
            process.destroy()
        }
    } catch (_: Throwable) {
        false
    }
}

private fun restartCategoryProcesses(category: SettingCategory): Boolean {
    return restartPackages(getPackagesForCategory(category))
}

private data class PackageInspectionResult(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val isSystemApp: Boolean = false,
    val isPrivileged: Boolean = false,
    val sourceDir: String = ""
)

private fun inspectPackage(context: Context, packageName: String, appName: String): PackageInspectionResult {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageInfo(packageName, 0)
        val appInfo = info.applicationInfo
        val isSys = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
        val isPriv = appInfo != null && ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) || (appInfo.sourceDir?.contains("/priv-app/") == true))
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        PackageInspectionResult(
            packageName = packageName,
            appName = appName,
            isInstalled = true,
            versionName = info.versionName ?: "未知",
            versionCode = vCode,
            isSystemApp = isSys,
            isPrivileged = isPriv,
            sourceDir = appInfo?.sourceDir ?: ""
        )
    } catch (_: Throwable) {
        PackageInspectionResult(
            packageName = packageName,
            appName = appName,
            isInstalled = false
        )
    }
}
