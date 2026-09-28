@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package dev.lackluster.redmagichelper.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lackluster.redmagichelper.HelperApplication
import dev.lackluster.redmagichelper.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Internal APK adaptation and runtime event logger.
 * Stores live console events in an in-memory ring buffer without filesystem IO.
 */
object AppLogger {
    private val buffer = ArrayDeque<String>(200)

    init {
        log("INFO", "诊断引擎", "RedMagic Helper 适配诊断引擎已就绪 (v4.0.0)")
        log("INFO", "系统环境", "运行架构: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"} | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    @JvmStatic
    @Synchronized
    fun log(level: String, function: String, detail: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time][$level][$function][$detail]"
        if (buffer.size >= 200) {
            buffer.removeFirst()
        }
        buffer.add(entry)
    }

    @JvmStatic
    @Synchronized
    fun log(tag: String, message: String) {
        log("INFO", tag, message)
    }

    @JvmStatic
    @Synchronized
    fun getRecentLogs(): List<String> = buffer.toList()
}

/**
 * Main Activity implementing the Material 3 Expressive design specifications:
 * - Portrait phone (412x892dp)
 * - Light mode Purple colorScheme roles
 * - TopAppBar: 64dp, "红魔助手" aligned left
 * - List: 72dp item height, 3dp spacing, outer corners 28dp, inner corners 8dp
 * - Split button: "重启" with dropdown menu for restart & cancel
 * - Bottom NavigationBar: 80dp, "首页", "日志", "关于"
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("config", MODE_PRIVATE)
        setContent {
            RedMagicTheme {
                MainAppScreen(preferences)
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
fun RedMagicTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
        } catch (t: Throwable) {
            PurpleColorScheme
        }
    } else {
        PurpleColorScheme
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * Navigation tabs specification: "首页"(home), "日志"(logs), "关于"(about).
 */
enum class MainNavTab(val title: String, val iconRes: Int) {
    HOME("首页", R.drawable.ic_home),
    LOGS("日志", R.drawable.ic_logs),
    ABOUT("关于", R.drawable.ic_about)
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
private fun MainAppScreen(preferences: SharedPreferences) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // Default selected tab is HOME
    var selectedTab by remember { mutableStateOf(MainNavTab.HOME) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var preferenceRevision by remember { mutableIntStateOf(0) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("已保存") }
    var isLoading by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    val modifiedKeys = remember { mutableSetOf<String>() }
    val originalValues = remember { mutableMapOf<String, Any?>() }

    val selectedCategory = selectedCategoryName?.let { name ->
        SettingCategory.values().firstOrNull { it.name == name }
    }

    var onRefreshLogsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onExportLogsAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
        AppLogger.log("INFO", "设置管理", "设置项已更新: key=$key (累计修改项: ${modifiedKeys.size})")
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
        editor.commit()
        originalValues.clear()
        modifiedKeys.clear()
        hasChanges = false
        preferenceRevision += 1
        AppLogger.log("INFO", "设置管理", "已撤销设置项更改")
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

    BackHandler(enabled = selectedCategory != null) {
        selectedCategoryName = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.surface,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = selectedCategory != null,
                            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
                            exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
                        ) {
                            IconButton(
                                onClick = { selectedCategoryName = null },
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
                        AnimatedContent(
                            targetState = selectedCategory?.title ?: "红魔助手",
                            contentAlignment = Alignment.CenterStart,
                            transitionSpec = {
                                if (targetState == "红魔助手") {
                                    (slideInHorizontally(tween(250)) { -it / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250)))
                                } else {
                                    (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally(tween(250)) { -it / 3 } + fadeOut(tween(250)))
                                }
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
                        if (selectedTab == MainNavTab.LOGS && selectedCategory == null) {
                            TextButton(
                                onClick = { onRefreshLogsAction?.invoke() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restart_alt),
                                    contentDescription = "刷新",
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("刷新", color = colors.primary, style = MaterialTheme.typography.labelLarge)
                            }
                            TextButton(
                                onClick = { onExportLogsAction?.invoke() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share),
                                    contentDescription = "导出",
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("导出", color = colors.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        scrolledContainerColor = colors.surfaceContainer,
                        titleContentColor = colors.onSurface
                    )
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedCategory == null,
                    enter = slideInVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) { it },
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) { it }
                ) {
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
                                        isLoading = true
                                        selectedTab = tab
                                        scope.launch {
                                            delay(150)
                                            isLoading = false
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
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
                // Screen Transitions
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        val motion = tween<androidx.compose.ui.unit.IntOffset>(300)
                        if (targetState == null) {
                            (slideInHorizontally(animationSpec = motion) { -it / 4 } +
                                    fadeIn(animationSpec = tween(300))) togetherWith
                                    (slideOutHorizontally(animationSpec = motion) { it } +
                                            fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInHorizontally(animationSpec = motion) { it / 4 } +
                                    fadeIn(animationSpec = tween(300))) togetherWith
                                    (slideOutHorizontally(animationSpec = motion) { -it } +
                                            fadeOut(animationSpec = tween(300)))
                        }
                    },
                    label = "category-navigation"
                ) { category ->
                    if (category != null) {
                        CategoryDetailView(
                            preferences = preferences,
                            category = category,
                            revision = preferenceRevision,
                            onPreferenceChanged = handlePreferenceChanged
                        )
                    } else {
                        when (selectedTab) {
                            MainNavTab.HOME -> {
                                HomeScreen(
                                    preferences = preferences,
                                    revision = preferenceRevision,
                                    onCategoryClick = { selectedCategoryName = it.name }
                                )
                            }
                            MainNavTab.LOGS -> {
                                LogsScreen(
                                    preferences = preferences,
                                    onTriggerToast = triggerToast,
                                    onRegisterActions = { refresh, export ->
                                        onRefreshLogsAction = refresh
                                        onExportLogsAction = export
                                    }
                                )
                            }
                            MainNavTab.ABOUT -> {
                                AboutScreen(
                                    preferences = preferences,
                                    onTriggerToast = triggerToast
                                )
                            }
                        }
                    }
                }

                // Specification: "下部居中放置“已保存”消息条（不带“撤销”操作）（部分覆盖在列表项组之上，绘制在前面）此处为Toast通知。显示在距屏幕底部 16dp 处，数秒后消失。高 48dp，圆角 8dp，背景 inverseSurface，文字 inverseOnSurface。"
                AnimatedVisibility(
                    visible = toastVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300)),
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
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
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

        // Full-screen loading scrim covering topBar ("红魔助手") and bottomBar ("首页", "日志", "关于")
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
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round,
                            color = colors.primary,
                            trackColor = colors.surfaceContainerHighest
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
    revision: Int,
    onCategoryClick: (SettingCategory) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
                onValueChange = { searchQuery = it },
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
                        IconButton(onClick = { searchQuery = "" }) {
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
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(
                    items = filteredCategories,
                    key = { _, cat -> cat.name }
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

                DropdownMenu(
                    expanded = menuExpanded,
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
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val count = categorySettings[category].orEmpty().size
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, animationSpec = tween(150), label = "item-scale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.primary.copy(alpha = 0.12f)),
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
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = colors.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(category.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }
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
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val settingsList = categorySettings[category].orEmpty().filter { setting ->
        setting.dependencyKey == null || readBoolean(preferences, setting.dependencyKey!!, false)
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(
                items = settingsList,
                key = { _, item -> item.id }
            ) { index, item ->
                val shape = getExpressiveItemShape(index, settingsList.size)
                when (item) {
                    is SettingItem.Toggle -> ExpressiveToggleItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        revision = revision,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Number -> ExpressiveNumberItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        revision = revision,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Text -> ExpressiveTextItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        revision = revision,
                        onPreferenceChanged = onPreferenceChanged
                    )
                    is SettingItem.Slider -> ExpressiveSliderItem(
                        preferences = preferences,
                        setting = item,
                        shape = shape,
                        revision = revision,
                        onPreferenceChanged = onPreferenceChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveToggleItem(
    preferences: SharedPreferences,
    setting: SettingItem.Toggle,
    shape: RoundedCornerShape,
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var checked by remember(setting.key, revision) {
        mutableStateOf(readBoolean(preferences, setting.key, setting.defaultValue))
    }
    val update: (Boolean) -> Unit = { value ->
        if (checked != value) {
            val oldValue = checked
            checked = value
            preferences.edit().putBoolean(setting.key, value).commit()
            onPreferenceChanged(setting.key, oldValue, value)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
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
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var isEdited by remember(setting.key, revision) { mutableStateOf(false) }
    var textValue by remember(setting.key, revision) {
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
                preferences.edit().putInt(setting.key, parsed).commit()
                onPreferenceChanged(setting.key, oldValue, parsed)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
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
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var isEdited by remember(setting.key, revision) { mutableStateOf(false) }
    var textValue by remember(setting.key, revision) {
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
                preferences.edit().putString(setting.key, normalized).commit()
                onPreferenceChanged(setting.key, oldValue, normalized)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
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
    revision: Int,
    onPreferenceChanged: (String, Any?, Any?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val initialValue = remember(setting.key, revision) {
        readNumber(preferences, setting.key, setting.defaultValue).coerceIn(setting.minValue, setting.maxValue)
    }
    var sliderPosition by remember(setting.key, revision) {
        mutableFloatStateOf(initialValue.toFloat())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
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
                        preferences.edit().putInt(setting.key, finalInt).commit()
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

/**
 * About Screen: Default landing screen containing module info, device info, root state, and restart tools.
 */
@Composable
private fun AboutScreen(
    preferences: SharedPreferences,
    onTriggerToast: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var rootState by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        rootState = detectRoot()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Card: Module Status
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = colors.primaryContainer
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = colors.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.redmagic_black),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "红魔助手",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer
                            )
                            Text(
                                text = "v4.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Device & Environment Info Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "设备与运行环境",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    EnvironmentInfoRow("设备型号", "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (${Build.DEVICE})")
                    EnvironmentInfoRow("系统版本", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    EnvironmentInfoRow("Root 状态", when (rootState) {
                        true -> "已授权 Root 权限"
                        false -> "未检测到 Root 权限"
                        null -> "检测中…"
                    })
                }
            }
        }
    }
}

@Composable
private fun EnvironmentInfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}

/**
 * Log filter tag specification
 */
private enum class LogFilter(val label: String) {
    ALL("全部"),
    GAMING("游戏"),
    BROWSER("浏览器"),
    SYSTEM("系统")
}

/**
 * Real Console Log data representation
 */
private data class ConsoleLogData(
    val sourcePath: String,
    val rawOutput: String,
    val lines: List<String>
)

/**
 * Log viewing and export screen: Terminal console view displaying APK internal adaptation logs
 */
@Composable
private fun LogsScreen(
    preferences: SharedPreferences,
    onTriggerToast: (String) -> Unit,
    onRegisterActions: (() -> Unit, () -> Unit) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var logData by remember { mutableStateOf<ConsoleLogData?>(null) }
    var isLogLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val horizontalScroll = rememberScrollState()

    val refreshLogs: () -> Unit = {
        isLogLoading = true
        AppLogger.log("INFO", "诊断引擎", "执行应用内适配状态扫描: 视图=${selectedFilter.label}")
        scope.launch(Dispatchers.IO) {
            val result = generateInternalAdaptationLogs(context, preferences, selectedFilter)
            withContext(Dispatchers.Main) {
                logData = result
                isLogLoading = false
            }
        }
    }

    val exportLogs: () -> Unit = {
        try {
            val current = logData
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                val text = buildString {
                    appendLine("==========================================")
                    appendLine("[红魔助手 模块应用内适配诊断日志]")
                    appendLine("生成时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine("诊断引擎: ${current?.sourcePath ?: "dev.lackluster.redmagichelper"}")
                    appendLine("当前视图: ${selectedFilter.label} (共 ${current?.lines?.size ?: 0} 行)")
                    appendLine("==========================================")
                    appendLine()
                    append(current?.rawOutput ?: "无日志内容")
                }
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "导出红魔助手适配日志")
            context.startActivity(shareIntent)
        } catch (e: Throwable) {
            onTriggerToast("导出失败: ${e.message}")
        }
    }

    LaunchedEffect(selectedFilter) {
        refreshLogs()
    }

    LaunchedEffect(Unit) {
        onRegisterActions(refreshLogs, exportLogs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .padding(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        // Filter chips row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(LogFilter.values()) { filter ->
                val selected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Native Material Design 3 container box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (isLogLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = colors.primary,
                            strokeWidth = 3.dp
                        )
                    }
                } else {
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScroll)
                        ) {
                            val lines = logData?.lines.orEmpty()
                            items(lines) { line ->
                                val lineColor = when {
                                    line.contains("][ERROR][") -> colors.error
                                    line.contains("][WARN][") -> colors.tertiary
                                    line.contains("][INFO][") -> colors.onSurface
                                    line.contains("][DEBUG][") -> colors.onSurfaceVariant
                                    else -> colors.onSurface
                                }
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = lineColor
                                )
                            }
                        }
                    }
                }
            }
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

private fun restartSystemUi(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f com.android.systemui"))
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
        "cn.nubia.gamehelperline",
        "cn.nubia.gameassist",
        "cn.nubia.gamehelpmodule"
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
    "neostore_remove_recommend_ads",
    "neostore_download_enabled",
    "neostore_download_count",
    "neostore_auto_sign" -> listOf("cn.nubia.neostore")

    "neogamecenter_skip_splash",
    "neogamecenter_remove_recommend_ads",
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
    val command = pkgs.joinToString("; ") { "am force-stop $it" }
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

private fun generateInternalAdaptationLogs(
    context: Context,
    preferences: SharedPreferences,
    filter: LogFilter = LogFilter.ALL
): ConsoleLogData {
    val startTime = System.currentTimeMillis()
    val lines = mutableListOf<String>()

    fun formatState(key: String, default: Boolean = false): String {
        val enabled = readBoolean(preferences, key, default)
        return if (enabled) "【已开启】" else "【未开启】"
    }

    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    fun emit(level: String, function: String, detail: String) {
        lines.add("[$time][$level][$function][$detail]")
    }

    emit("INFO", "诊断引擎", "RedMagic Helper 实时适配诊断引擎已就绪 (v4.0.0)")
    emit("INFO", "系统环境", "机型: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}) | 构建版本: ${Build.DISPLAY}")
    emit("INFO", "系统环境", "系统架构: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | 运行状态: Active")
    emit("INFO", "诊断引擎", "当前诊断视图: 【${filter.label}】")

    var moduleCount = 0

    // 1. GAMING
    if (filter == LogFilter.ALL || filter == LogFilter.GAMING) {
        val ga = inspectPackage(context, "cn.nubia.gameassist", "游戏助手")
        moduleCount++
        if (ga.isInstalled) {
            emit("INFO", "通用与游戏", "APK 已安装: cn.nubia.gameassist (v${ga.versionName}, code: ${ga.versionCode}, ${if (ga.isPrivileged) "特权应用" else if (ga.isSystemApp) "系统预装" else "普通应用"}); Hook 结果见运行日志")
            emit("DEBUG", "通用与游戏", "组件安装路径: ${ga.sourceDir}")
            emit(if (readBoolean(preferences, "game_fuction_unfreeze_switch", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏功能限制解锁 = ${formatState("game_fuction_unfreeze_switch")}")
            emit(if (readBoolean(preferences, "game_force_overlay_capability", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 允许第三方悬浮窗 = ${formatState("game_force_overlay_capability")}")
            emit(if (readBoolean(preferences, "game_allow_third_party_freeform", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 允许第三方小窗挂起 = ${formatState("game_allow_third_party_freeform")}")
            emit(if (readBoolean(preferences, "game_magic_voice_unfreeze", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏变声器限制解除 = ${formatState("game_magic_voice_unfreeze")}")
            emit(if (readBoolean(preferences, "game_space_sound_effects_unfreeze", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏空间音效限制解除 = ${formatState("game_space_sound_effects_unfreeze")}")
            emit(if (readBoolean(preferences, "game_sight_assist_unfreeze", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 准星助手全机型解锁 = ${formatState("game_sight_assist_unfreeze")}")
            emit(if (readBoolean(preferences, "game_shoulder_key_enabled", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏空间肩键解锁 = ${formatState("game_shoulder_key_enabled")}")
            emit(if (readBoolean(preferences, "acive_mode_switch", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏空间主动模式解锁 = ${formatState("acive_mode_switch")}")
            emit(if (readBoolean(preferences, "game_super_resolution_unlimited_apps", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 超境模式允许所有应用 = ${formatState("game_super_resolution_unlimited_apps")}")
            emit(if (readBoolean(preferences, "game_space_combo_speed_enabled", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 游戏连招加速 = ${formatState("game_space_combo_speed_enabled")} (倍率: ${readNumber(preferences, "game_space_combo_speed_rate", 1)}x)")
        } else {
            emit("WARN", "通用与游戏", "未检测到核心组件 cn.nubia.gameassist 安装，跳过游戏助手相关 Hook")
        }

        val ghl = inspectPackage(context, "cn.nubia.gamehelperline", "辅助线服务")
        moduleCount++
        if (ghl.isInstalled) {
            emit("INFO", "通用与游戏", "APK 已安装: cn.nubia.gamehelperline (v${ghl.versionName}, code: ${ghl.versionCode}); Hook 结果见运行日志")
        } else {
            emit("WARN", "通用与游戏", "未检测到辅助线服务 cn.nubia.gamehelperline 安装")
        }

        val gl = inspectPackage(context, "cn.nubia.gamelauncher", "游戏空间")
        val pt = inspectPackage(context, "com.zte.game.plugintrigger", "AI触发器")
        val gf = inspectPackage(context, "cn.zte.gamefloat", "游戏悬浮窗")
        moduleCount += 3
        emit("INFO", "通用与游戏", "游戏空间组件检测: gl: ${if (gl.isInstalled) "v${gl.versionName}" else "未安装"}, pt: ${if (pt.isInstalled) "v${pt.versionName}" else "未安装"}, gf: ${if (gf.isInstalled) "v${gf.versionName}" else "未安装"}")
        emit(if (readBoolean(preferences, "ai_trigger_switch", false)) "INFO" else "DEBUG", "通用与游戏", "开关: AI/插件触发加速 = ${formatState("ai_trigger_switch")}")
        emit(if (readBoolean(preferences, "double_any_app", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 允许双开任意应用 = ${formatState("double_any_app")}")
        emit(if (readBoolean(preferences, "rm_low_memory_limit", false)) "INFO" else "DEBUG", "通用与游戏", "开关: 移除双开低内存限制 = ${formatState("rm_low_memory_limit")}")
    }

    // 2. BROWSER
    if (filter == LogFilter.ALL || filter == LogFilter.BROWSER) {
        val br = inspectPackage(context, "cn.nubia.browser", "努比亚浏览器")
        moduleCount++
        if (br.isInstalled) {
            emit("INFO", "浏览器适配", "APK 已安装: cn.nubia.browser (v${br.versionName}, code: ${br.versionCode}); Hook 结果见运行日志")
            emit("DEBUG", "浏览器适配", "安装路径: ${br.sourceDir}")
            emit(if (readBoolean(preferences, "browser_open_search", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 浏览器启动时打开搜索页 = ${formatState("browser_open_search")}")
            emit(if (readBoolean(preferences, "browser_skip_splash", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 跳过浏览器启动广告 = ${formatState("browser_skip_splash")}")
            emit(if (readBoolean(preferences, "browser_remove_info_tabs", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 去除底部视频与资讯 = ${formatState("browser_remove_info_tabs")}")
            emit(if (readBoolean(preferences, "browser_remove_homepage_info", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 删除浏览器主页资讯 = ${formatState("browser_remove_homepage_info")}")
            emit(if (readBoolean(preferences, "browser_slim_mine", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 删除常用服务与短剧 = ${formatState("browser_slim_mine")}")
            emit(if (readBoolean(preferences, "browser_remove_hot_words", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 删除搜索页热门短剧 = ${formatState("browser_remove_hot_words")}")
            emit(if (readBoolean(preferences, "browser_remove_search_shortcuts", false)) "INFO" else "DEBUG", "浏览器适配", "开关: 删除搜索框下方快捷图标 = ${formatState("browser_remove_search_shortcuts")}")
        } else {
            emit("WARN", "浏览器适配", "未检测到努比亚浏览器 cn.nubia.browser 安装")
        }
    }

    // 3. SYSTEM
    if (filter == LogFilter.ALL || filter == LogFilter.SYSTEM) {
        val sui = inspectPackage(context, "com.android.systemui", "系统界面")
        moduleCount++
        emit("INFO", "系统界面", "SystemUI APK 状态: ${if (sui.isInstalled) "v${sui.versionName}, code: ${sui.versionCode}" else "常驻核心服务"}; Hook 结果见运行日志")
        emit(if (readBoolean(preferences, "statusbar_clock_show_seconds", false)) "INFO" else "DEBUG", "系统界面", "开关: 状态栏时钟显示秒数 = ${formatState("statusbar_clock_show_seconds")}")
        emit(if (readBoolean(preferences, "statusbar_clock_pull_down_show_period", false)) "INFO" else "DEBUG", "系统界面", "开关: 状态栏时钟显示中文时段 = ${formatState("statusbar_clock_pull_down_show_period")}")
        emit(if (readBoolean(preferences, "systemui_screen_off_show_seconds", false)) "INFO" else "DEBUG", "系统界面", "开关: 息屏显示秒数 = ${formatState("systemui_screen_off_show_seconds")}")
        emit(if (readBoolean(preferences, "systemui_screen_off_period", false)) "INFO" else "DEBUG", "系统界面", "开关: 息屏显示中文时段 = ${formatState("systemui_screen_off_period")}")
        emit(if (readBoolean(preferences, "remove_alert_windows_notification", false)) "INFO" else "DEBUG", "系统界面", "开关: 移除悬浮窗通知 = ${formatState("remove_alert_windows_notification")}")
        emit(if (readBoolean(preferences, "volume_disable_safety_warning", false)) "INFO" else "DEBUG", "系统界面", "开关: 禁用安全音量提示 = ${formatState("volume_disable_safety_warning")}")
        emit(if (readBoolean(preferences, "disable_flag_secure", false)) "INFO" else "DEBUG", "系统界面", "开关: 屏幕录制忽略 FLAG_SECURE = ${formatState("disable_flag_secure")}")
        emit(if (readBoolean(preferences, "statusbar_notif_max", false)) "INFO" else "DEBUG", "系统界面", "开关: 状态栏通知图标数量限制 = ${formatState("statusbar_notif_max")} (${readNumber(preferences, "statusbar_notif_max_value", 10)} 个)")

        moduleCount++
        emit("INFO", "系统核心", "system_server 宿主已扫描；实际 Hook 状态以运行时事件为准")
        emit(if (readBoolean(preferences, "remove_restrictions_window", false)) "INFO" else "DEBUG", "系统核心", "开关: 允许所有应用使用小窗 = ${formatState("remove_restrictions_window")}")
        emit(if (readBoolean(preferences, "remove_restrictions_window_number", false)) "INFO" else "DEBUG", "系统核心", "开关: 窗口回复图标数量限制 = ${formatState("remove_restrictions_window_number")}")
        emit(if (readBoolean(preferences, "android_remove_intent_hijack_content", false)) "INFO" else "DEBUG", "系统核心", "开关: 意图劫持兼容 = ${formatState("android_remove_intent_hijack_content")}")
        emit(if (readBoolean(preferences, "system_framework_lock_screen_timeout", false)) "INFO" else "DEBUG", "系统核心", "开关: 系统锁屏超时改写 = ${formatState("system_framework_lock_screen_timeout")} (${readNumber(preferences, "system_framework_lock_screen_timeout_value", 10)} 分钟)")

        val set = inspectPackage(context, "com.android.settings", "系统设置")
        val pkgInst = inspectPackage(context, "com.android.packageinstaller", "软件包安装程序")
        moduleCount += 2
        emit("INFO", "系统设置", "系统设置与安装器就绪 (set: ${if (set.isInstalled) "v${set.versionName}" else "未安装"}, pkg: ${if (pkgInst.isInstalled) "v${pkgInst.versionName}" else "未安装"})")
        emit(if (readBoolean(preferences, "disable_usb_installation_and_switch_account_verification", false)) "INFO" else "DEBUG", "系统设置", "开关: 设置中禁用 USB 安装验证 = ${formatState("disable_usb_installation_and_switch_account_verification")}")
        emit(if (readBoolean(preferences, "skip_pkg_scan", false)) "INFO" else "DEBUG", "系统设置", "开关: 跳过 APK 扫描 = ${formatState("skip_pkg_scan")}")
        emit(if (readBoolean(preferences, "hide_store_install_prompt", false)) "INFO" else "DEBUG", "系统设置", "开关: 隐藏安装器商店提示 = ${formatState("hide_store_install_prompt")}")
        emit(if (readBoolean(preferences, "cts_test_installer", false)) "INFO" else "DEBUG", "系统设置", "开关: CTS 安装器调用者 = ${formatState("cts_test_installer")}")
    }

    if (filter == LogFilter.ALL) {
        val ns = inspectPackage(context, "cn.nubia.neostore", "应用中心")
        val ng = inspectPackage(context, "cn.nubia.neogamecenter", "游戏中心")
        val la = inspectPackage(context, "com.zte.mifavor.launcher", "红魔桌面")
        val bt = inspectPackage(context, "com.zte.beautify", "主题壁纸")
        val fb = inspectPackage(context, "cn.nubia.filebrowser", "MTP分类浏览")
        val nfc = inspectPackage(context, "com.android.nfc", "NFC服务")
        moduleCount += 6

        emit("INFO", "应用商店", "应用商店与游戏中心就绪 (store: ${if (ns.isInstalled) "v${ns.versionName}" else "未安装"}, game: ${if (ng.isInstalled) "v${ng.versionName}" else "未安装"})")
        emit(if (readBoolean(preferences, "neostore_skip_splash", false)) "INFO" else "DEBUG", "应用商店", "开关: 应用中心跳过开屏广告 = ${formatState("neostore_skip_splash")}")
        emit(if (readBoolean(preferences, "neostore_remove_recommend_ads", false)) "INFO" else "DEBUG", "应用商店", "开关: 应用中心去除推荐广告 = ${formatState("neostore_remove_recommend_ads")}")
        emit(if (readBoolean(preferences, "neogamecenter_skip_splash", false)) "INFO" else "DEBUG", "应用商店", "开关: 游戏中心跳过开屏广告 = ${formatState("neogamecenter_skip_splash")}")
        emit(if (readBoolean(preferences, "neostore_download_enabled", false)) "INFO" else "DEBUG", "应用商店", "开关: 多线程下载 = ${formatState("neostore_download_enabled")} (${readNumber(preferences, "neostore_download_count", 5)} 线程)")
        emit(if (readBoolean(preferences, "neostore_auto_sign", false)) "INFO" else "DEBUG", "应用商店", "开关: 应用中心自动签到 = ${formatState("neostore_auto_sign")}")
        emit(if (readBoolean(preferences, "neogame_local_svip", false)) "INFO" else "DEBUG", "应用商店", "开关: 游戏中心本地超级会员 = ${formatState("neogame_local_svip")}")

        emit("INFO", "桌面与个性化", "桌面与主题组件就绪 (launcher: ${if (la.isInstalled) "v${la.versionName}" else "未安装"}, beautify: ${if (bt.isInstalled) "v${bt.versionName}" else "未安装"})")
        emit(if (readBoolean(preferences, "cancel_trial_login", true)) "INFO" else "DEBUG", "桌面与个性化", "开关: 主题资源免登录并绕过广告/付费判断 = ${formatState("cancel_trial_login", true)}")
        emit(if (readBoolean(preferences, "beautify_skip_splash_ads", false)) "INFO" else "DEBUG", "桌面与个性化", "开关: 主题启动页跳过广告 = ${formatState("beautify_skip_splash_ads")}")
        emit(if (readBoolean(preferences, "allow_thirdparty_launcher", false)) "INFO" else "DEBUG", "桌面与个性化", "开关: 允许第三方桌面 = ${formatState("allow_thirdparty_launcher")}")
        emit(if (readBoolean(preferences, "system_time_component_desktop_switch", false)) "INFO" else "DEBUG", "桌面与个性化", "开关: 桌面时钟适配 = ${formatState("system_time_component_desktop_switch")}")

        emit("INFO", "MTP分类浏览", "组件 cn.nubia.filebrowser 就绪 (${if (fb.isInstalled) "v${fb.versionName}" else "未安装"})")
        emit(if (readBoolean(preferences, "hide_mtp_category_browse", false)) "INFO" else "DEBUG", "MTP分类浏览", "开关: 隐藏 MTP 分类浏览 = ${formatState("hide_mtp_category_browse")}")
        emit(if (readBoolean(preferences, "mtp_rename_root_name_switch", false)) "INFO" else "DEBUG", "MTP分类浏览", "开关: 重命名 MTP 根目录 = ${formatState("mtp_rename_root_name_switch")} (名称: \"${readText(preferences, "mtp_rename_root_name", "内部存储设备")}\")")

        emit("INFO", "NFC服务", "组件 com.android.nfc 就绪 (${if (nfc.isInstalled) "v${nfc.versionName}" else "未安装"})")
        emit(if (readBoolean(preferences, "mute_nfc_sound", false)) "INFO" else "DEBUG", "NFC服务", "开关: NFC 静音 = ${formatState("mute_nfc_sound")}")
        emit(if (readBoolean(preferences, "nfc_allow_screen_off_recognition", false)) "INFO" else "DEBUG", "NFC服务", "开关: 允许熄屏识别 NFC = ${formatState("nfc_allow_screen_off_recognition")}")
    }

    val runtimeHookLogs = readHookRuntimeLogs(context)
        .filter { matchesHookLogFilter(it, filter) }
    if (runtimeHookLogs.isEmpty()) {
        emit("INFO", "Hook运行日志", "当前筛选下暂无运行时 Hook 记录")
    } else {
        emit("INFO", "Hook运行日志", "读取到 ${runtimeHookLogs.size} 条真实 Hook 进程记录")
        lines.addAll(runtimeHookLogs)
    }

    val recentLogs = AppLogger.getRecentLogs()
    if (recentLogs.isNotEmpty()) {
        for (logEntry in recentLogs) {
            if (logEntry.startsWith("[") && logEntry.contains("][")) {
                lines.add(logEntry)
            } else {
                emit("INFO", "实时事件", logEntry)
            }
        }
    }

    val elapsed = System.currentTimeMillis() - startTime
    emit("INFO", "诊断完成", "已完成核心适配组件诊断扫描，共 ${lines.size} 条诊断信息，耗时 ${elapsed}ms")

    return ConsoleLogData(
        sourcePath = "dev.lackluster.redmagichelper.internal",
        rawOutput = lines.joinToString("\n"),
        lines = lines
    )
}

private fun readHookRuntimeLogs(context: Context): List<String> {
    return try {
        val result = context.contentResolver.call(
            dev.lackluster.redmagichelper.config.ConfigProvider.CONTENT_URI,
            "getHookLogs",
            null,
            null
        ) ?: return emptyList()
        if (!result.getBoolean("success", false)) return emptyList()
        result.getStringArrayList("lines")?.toList().orEmpty()
    } catch (_: Throwable) {
        emptyList()
    }
}

private fun matchesHookLogFilter(line: String, filter: LogFilter): Boolean {
    if (filter == LogFilter.ALL) return true
    val isGaming = line.contains("[游戏优化]")
    val isBrowser = line.contains("[浏览器适配]")
    return when (filter) {
        LogFilter.ALL -> true
        LogFilter.GAMING -> isGaming
        LogFilter.BROWSER -> isBrowser
        LogFilter.SYSTEM -> !isGaming && !isBrowser
    }
}
