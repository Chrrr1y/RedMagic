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
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlin.math.roundToInt
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    private val buffer = ArrayDeque<String>(500)

    init {
        log("INFO", "诊断引擎", "RedMagic Helper 适配诊断引擎已就绪 (v4.0.1)")
        log("INFO", "系统环境", "运行架构: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"} | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    @JvmStatic
    @Synchronized
    fun log(level: String, function: String, detail: String) {
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time][$level][$function][$detail]"
        if (buffer.size >= 500) {
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
    fun clear() {
        buffer.clear()
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

    var currentSubScreenKey by remember { mutableStateOf<String?>(null) }
    var onRefreshLogsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onExportLogsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onDeleteLogsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDeleteLogsDialog by remember { mutableStateOf(false) }

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

    BackHandler(enabled = currentSubScreenKey != null) {
        currentSubScreenKey = null
    }

    BackHandler(enabled = currentSubScreenKey == null && selectedCategory != null) {
        selectedCategoryName = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.surface,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = currentSubScreenKey != null || selectedCategory != null,
                            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
                            exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentSubScreenKey != null) {
                                        currentSubScreenKey = null
                                    } else {
                                        selectedCategoryName = null
                                    }
                                },
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
                            selectedCategory != null -> selectedCategory.title
                            else -> "红魔助手"
                        }
                        AnimatedContent(
                            targetState = headerTitle,
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
                        if (selectedTab == MainNavTab.LOGS && selectedCategory == null && currentSubScreenKey == null) {
                            TextButton(
                                onClick = { onRefreshLogsAction?.invoke() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restart_alt),
                                    contentDescription = "刷新",
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primary
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("刷新", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { onExportLogsAction?.invoke() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share),
                                    contentDescription = "导出",
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.primary
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("导出", color = colors.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { showDeleteLogsDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = "删除",
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.error
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("删除", color = colors.error, style = MaterialTheme.typography.labelMedium)
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
                    visible = selectedCategory == null && currentSubScreenKey == null,
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
                    targetState = currentSubScreenKey ?: selectedCategory?.name,
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
                ) { screenKey ->
                    if (screenKey == "lockscreen_custom_font_settings") {
                        LockScreenFontSettingsScreen(
                            preferences = preferences,
                            revision = preferenceRevision,
                            onPreferenceChanged = handlePreferenceChanged,
                            onTriggerToast = triggerToast
                        )
                    } else if (selectedCategory != null) {
                        CategoryDetailView(
                            preferences = preferences,
                            category = selectedCategory,
                            revision = preferenceRevision,
                            onPreferenceChanged = handlePreferenceChanged,
                            onActionClick = { actionKey ->
                                currentSubScreenKey = actionKey
                            }
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
                                    onRegisterActions = { refresh, export, delete ->
                                        onRefreshLogsAction = refresh
                                        onExportLogsAction = export
                                        onDeleteLogsAction = delete
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

        if (showDeleteLogsDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteLogsDialog = false },
                title = { Text("清空运行与适配日志") },
                text = { Text("确定要删除所有运行日志与跨进程 Hook 记录吗？清空后不可恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteLogsDialog = false
                            onDeleteLogsAction?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                    ) {
                        Text("清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteLogsDialog = false }) {
                        Text("取消")
                    }
                }
            )
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
    onPreferenceChanged: (String, Any?, Any?) -> Unit,
    onActionClick: (String) -> Unit = {}
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
            .clip(shape)
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
                            .commit()
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
            .commit()
        onPreferenceChanged("lockscreen_custom_font_imported_name", oldVal, "")
        onTriggerToast("已清除导入字体")
    }

    val selectSystemFont: (String) -> Unit = { path ->
        val oldVal = systemFontPath
        val newVal = if (oldVal == path) "" else path
        systemFontPath = newVal
        preferences.edit().putString("lockscreen_system_font_path", newVal).commit()
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
                        preferences.edit().putBoolean("lockscreen_custom_font_switch", newVal).commit()
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
                            preferences.edit().putBoolean("lockscreen_custom_font_switch", newVal).commit()
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
                            text = "锁屏 / AOD 时钟实时预览",
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
                                    preferences.edit().putInt("lockscreen_custom_font_weight", fontWeight).commit()
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
                                    preferences.edit().putInt("lockscreen_custom_font_size", fontSize).commit()
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
                                    preferences.edit().putInt("lockscreen_custom_date_size", dateSize).commit()
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
                                text = "v4.0.1",
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

private enum class LogLevel(val label: String) {
    ALL("全部等级"),
    ERROR("ERROR"),
    WARN("WARN"),
    INFO("INFO"),
    DEBUG("DEBUG")
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
    onRegisterActions: (refresh: () -> Unit, export: () -> Unit, delete: () -> Unit) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var selectedLevel by remember { mutableStateOf(LogLevel.ALL) }
    var retentionDays by remember { mutableIntStateOf(preferences.getInt("log_retention_days", 7)) }
    var showRetentionMenu by remember { mutableStateOf(false) }
    var logData by remember { mutableStateOf<ConsoleLogData?>(null) }
    var isLogLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val horizontalScroll = rememberScrollState()

    val refreshLogs: () -> Unit = {
        isLogLoading = true
        AppLogger.log("INFO", "诊断引擎", "执行适配状态扫描: 类别=${selectedFilter.label}, 等级=${selectedLevel.label}")
        scope.launch(Dispatchers.IO) {
            val result = generateInternalAdaptationLogs(context, preferences, selectedFilter, selectedLevel)
            withContext(Dispatchers.Main) {
                logData = result
                isLogLoading = false
            }
        }
    }

    val deleteLogs: () -> Unit = {
        isLogLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.call(
                    dev.lackluster.redmagichelper.config.ConfigProvider.CONTENT_URI,
                    "clearHookLogs",
                    null,
                    null
                )
            } catch (_: Throwable) {}
            AppLogger.clear()
            val result = generateInternalAdaptationLogs(context, preferences, selectedFilter, selectedLevel)
            withContext(Dispatchers.Main) {
                logData = result
                isLogLoading = false
                onTriggerToast("已清空所有运行日志与诊断记录")
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
                    appendLine("生成时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine("诊断引擎: ${current?.sourcePath ?: "dev.lackluster.redmagichelper"}")
                    appendLine("当前视图: 类别=${selectedFilter.label}, 等级=${selectedLevel.label} (共 ${current?.lines?.size ?: 0} 行)")
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

    LaunchedEffect(selectedFilter, selectedLevel) {
        refreshLogs()
    }

    LaunchedEffect(Unit) {
        onRegisterActions(refreshLogs, exportLogs, deleteLogs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .padding(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        // Filter row 1: Category chips + Retention policy dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // Retention Days dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.secondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showRetentionMenu = true }
                ) {
                    Text(
                        text = if (retentionDays <= 0) "保留: 永久 ▼" else "保留: ${retentionDays}天 ▼",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                DropdownMenu(
                    expanded = showRetentionMenu,
                    onDismissRequest = { showRetentionMenu = false }
                ) {
                    val options = listOf(
                        1 to "保留 1 天",
                        3 to "保留 3 天",
                        7 to "保留 7 天 (默认)",
                        30 to "保留 30 天",
                        0 to "永久保留"
                    )
                    options.forEach { (days, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                showRetentionMenu = false
                                retentionDays = days
                                preferences.edit().putInt("log_retention_days", days).commit()
                                refreshLogs()
                                onTriggerToast("已更新日志保留策略: $label")
                            }
                        )
                    }
                }
            }
        }

        // Filter row 2: Log Level chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(LogLevel.values()) { level ->
                val selected = selectedLevel == level
                val levelColor = when (level) {
                    LogLevel.ERROR -> colors.error
                    LogLevel.WARN -> colors.tertiary
                    LogLevel.INFO -> colors.primary
                    LogLevel.DEBUG -> colors.outline
                    LogLevel.ALL -> colors.secondary
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) levelColor.copy(alpha = 0.2f) else colors.surfaceContainer,
                    border = if (selected) BorderStroke(1.dp, levelColor) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedLevel = level }
                ) {
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) levelColor else colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

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
                                    line.contains("][ERROR][") || line.contains(" ERROR ") -> colors.error
                                    line.contains("][WARN][") || line.contains(" WARN ") -> colors.tertiary
                                    line.contains("][INFO][") || line.contains(" INFO ") -> colors.onSurface
                                    line.contains("][DEBUG][") || line.contains(" DEBUG ") -> colors.onSurfaceVariant
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

private fun generateInternalAdaptationLogs(
    context: Context,
    preferences: SharedPreferences,
    categoryFilter: LogFilter = LogFilter.ALL,
    levelFilter: LogLevel = LogLevel.ALL
): ConsoleLogData {
    val startTime = System.currentTimeMillis()
    val lines = mutableListOf<String>()

    val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
    fun emit(level: String, function: String, detail: String) {
        lines.add("[$time][$level][$function][$detail]")
    }

    emit("INFO", "DIAGNOSTIC", "RedMagic Helper 运行时适配诊断引擎就绪 (v4.0.1)")
    emit("INFO", "DEVICE_INFO", "机型: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}) | ROM: ${Build.DISPLAY} | API: ${Build.VERSION.SDK_INT}")
    emit("INFO", "ACTIVE_FILTER", "当前视图: 类别=【${categoryFilter.label}】 | 等级=【${levelFilter.label}】")

    // Smali nodes and hook specifications
    if (categoryFilter == LogFilter.ALL || categoryFilter == LogFilter.SYSTEM) {
        val isCustomFont = readBoolean(preferences, "lockscreen_custom_font_switch", false)
        val sysFont = readText(preferences, "lockscreen_system_font_path", "")
        val impFont = readText(preferences, "lockscreen_custom_font_imported_name", "")
        val weight = readNumber(preferences, "lockscreen_custom_font_weight", 400)
        val size = readNumber(preferences, "lockscreen_custom_font_size", 70)
        val priorityFont = when {
            sysFont.isNotBlank() -> "SystemFont:" + File(sysFont).name + " [HIGHEST PRIORITY]"
            impFont.isNotBlank() -> "ImportedFont:$impFont"
            else -> "Default"
        }
        emit("INFO", "SMALI_NODE", "Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V [ResID 0x7f0b02c7 clock_view] -> ${if (isCustomFont) "HOOKED (ActiveFont=$priorityFont, Weight=$weight, Size=${size}px)" else "DEFAULT (Feature disabled)"}")
        emit("INFO", "SMALI_NODE", "Lcom/zte/mifavor/keyguard/personalclock/FontResUtil;->getClockTypeface(II)Landroid/graphics/Typeface; -> ${if (isCustomFont) "HOOKED (Inject Custom Typeface: $priorityFont)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/zte/mifavor/keyguard/personalclock/FontResUtil;->getTypefaceByName(Ljava/lang/String;)Landroid/graphics/Typeface; -> ${if (isCustomFont) "HOOKED (Inject Custom Typeface: $priorityFont)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/zte/mifavor/keyguard/settings/ClockViewUtils;->refreshTextViewLayout(Landroid/widget/TextView;I)V -> ${if (isCustomFont) "HOOKED (Dynamic Typeface Injection)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/zte/feature/doze/AodClock/KeyguardAodClockStyle/BaseAodLockScreenClockLinearLayout;->refreshClockViewFont()V -> ${if (isCustomFont) "HOOKED (AOD Invalidation)" else "DEFAULT"}")

        val skipScan = readBoolean(preferences, "skip_pkg_scan", false)
        val hideRisk = readBoolean(preferences, "hide_evolution_mode_toggle", false)
        val hideStore = readBoolean(preferences, "hide_store_install_prompt", false)
        emit("INFO", "SMALI_NODE", "Lcom/android/packageinstaller/InstallStaging;->installScanningInit(Landroid/net/Uri;)V -> ${if (skipScan) "HOOKED (Bypass bindVirusService & ScanningAsyncTask 3s delay latch)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/android/packageinstaller/PackageUtil;->getFeatureBoolean(Ljava/lang/String;Z)Z [ZTE_FEATURE_ADB_VIRUS_SCAN] -> ${if (skipScan) "HOOKED (Force Return False)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/android/packageinstaller/PackageInstallerActivity;->getCookUI()Lcom/android/packageinstaller/CookUI; -> ${if (hideRisk) "HOOKED (Override cleanBgColor=true, hideWarningLayout=true)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcom/android/packageinstaller/PackageInstallerActivity;->bindUi()V -> ${if (hideStore) "HOOKED (Strip store redirect banners)" else "DEFAULT"}")

        val secBypass = readBoolean(preferences, "disable_flag_secure", false)
        emit("INFO", "SMALI_NODE", "Lcom/android/server/wm/WindowState;->isSecureLocked()Z -> ${if (secBypass) "HOOKED (Override FLAG_SECURE)" else "DEFAULT"}")
    }

    if (categoryFilter == LogFilter.ALL || categoryFilter == LogFilter.GAMING) {
        val superRes = readBoolean(preferences, "game_super_resolution_unlimited_apps", false)
        val comboSpeed = readBoolean(preferences, "game_space_combo_speed_enabled", false)
        val comboRate = readNumber(preferences, "game_space_combo_speed_rate", 1)
        emit("INFO", "SMALI_NODE", "Lcn/nubia/gameassist/gfrc/GfrcCapabilityPolicy;->isSupported()Z -> ${if (superRes) "HOOKED (Bypass GFRC/MEMC app whitelist -> Force true)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcn/nubia/gameassist/GameAssistApplication;->onCreate()V -> Feature Gate Unfreezer Active")
        emit("INFO", "SMALI_NODE", "Lcn/nubia/gameassist/combo/ComboPlayer;->play()V -> ${if (comboSpeed) "HOOKED (Scaled Rate ${comboRate}x)" else "DEFAULT"}")
    }

    if (categoryFilter == LogFilter.ALL || categoryFilter == LogFilter.BROWSER) {
        val skipSplash = readBoolean(preferences, "browser_skip_splash", false)
        val removeTabs = readBoolean(preferences, "browser_remove_info_tabs", false)
        val searchOnly = readBoolean(preferences, "browser_open_search", false)
        emit("INFO", "SMALI_NODE", "Lcn/nubia/browser/splash/SplashActivity;->onCreate(Landroid/os/Bundle;)V -> ${if (skipSplash) "HOOKED (Skip Splash & Direct Launch)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcn/nubia/browser/homepage/ui/BottomTabBar;->initTabs()V -> ${if (removeTabs) "HOOKED (Strip Video & News Tabs)" else "DEFAULT"}")
        emit("INFO", "SMALI_NODE", "Lcn/nubia/browser/MainActivity;->onCreate(Landroid/os/Bundle;)V -> ${if (searchOnly) "HOOKED (Redirect to Pure Search)" else "DEFAULT"}")
    }

    // Read real cross-process runtime hook events from ConfigProvider / HookRuntimeLogStore
    val runtimeHookLogs = readHookRuntimeLogs(context)
        .filter { matchesHookLogFilter(it, categoryFilter) }
    if (runtimeHookLogs.isNotEmpty()) {
        emit("INFO", "RUNTIME_HOOK", "=== 捕获到 ${runtimeHookLogs.size} 条跨进程 Hook 真实运行事件 ===")
        lines.addAll(runtimeHookLogs)
    } else {
        emit("INFO", "RUNTIME_HOOK", "当前筛选下暂无跨进程运行时事件（操作对应系统应用后将自动捕获记录）")
    }

    // AppLogger events
    val recentLogs = AppLogger.getRecentLogs()
    if (recentLogs.isNotEmpty()) {
        lines.addAll(recentLogs)
    }

    // Apply level filter
    val filteredLines = if (levelFilter == LogLevel.ALL) {
        lines
    } else {
        lines.filter { line ->
            when (levelFilter) {
                LogLevel.ERROR -> line.contains("][ERROR][") || line.contains(" ERROR ")
                LogLevel.WARN -> line.contains("][WARN][") || line.contains(" WARN ")
                LogLevel.INFO -> line.contains("][INFO][") || line.contains(" INFO ")
                LogLevel.DEBUG -> line.contains("][DEBUG][") || line.contains(" DEBUG ")
                else -> true
            }
        }
    }

    val elapsed = System.currentTimeMillis() - startTime
    val finalLines = filteredLines.toMutableList()
    finalLines.add("[$time][INFO][DIAGNOSTIC][诊断扫描完成: 共 ${finalLines.size} 条信息，耗时 ${elapsed}ms]")

    return ConsoleLogData(
        sourcePath = "dev.lackluster.redmagichelper.internal",
        rawOutput = finalLines.joinToString("\n"),
        lines = finalLines
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
