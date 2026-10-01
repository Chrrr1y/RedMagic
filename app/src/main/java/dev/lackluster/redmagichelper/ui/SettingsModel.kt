package dev.lackluster.redmagichelper.ui

import dev.lackluster.redmagichelper.R

enum class SettingCategory(
    val title: String,
    val summary: String,
    val iconRes: Int
) {
    BROWSER(
        "浏览器适配",
        "去除启动广告、默认搜索页与界面精简",
        R.drawable.ic_header_browser
    ),
    SYSTEM_UI(
        "系统界面",
        "息屏与状态栏时钟、通知限制与录屏策略",
        R.drawable.ic_header_systemui
    ),
    GAMING(
        "通用与游戏",
        "游戏空间解锁、连招加速、AI 触发器与风扇",
        R.drawable.ic_game_space
    ),
    FILES(
        "MTP分类浏览",
        "连接电脑时 MTP 分类浏览与根目录名称适配",
        R.drawable.ic_file_manager
    ),
    STORE(
        "应用商店与游戏中心",
        "去广告、自动签到、本地会员与多线程下载",
        R.drawable.ic_app_center
    ),
    DESKTOP(
        "主题/桌面",
        "锁屏时钟自定义字体、主题资源与桌面配置",
        R.drawable.ic_header_home
    ),
    SETTINGS(
        "安装与系统设置",
        "系统更新、安装扫描、USB 验证与小窗限制",
        R.drawable.ic_system_app_settings
    ),
    OTHERS(
        "其他功能与 NFC",
        "NFC 刷卡静音与熄屏感应识别",
        R.drawable.ic_header_others
    )
}

sealed interface SettingItem {
    val id: String
    val title: String
    val summary: String?
    val dependencyKey: String?

    data class Toggle(
        override val title: String,
        val key: String,
        override val summary: String? = null,
        val defaultValue: Boolean = false,
        override val dependencyKey: String? = null
    ) : SettingItem {
        override val id: String = "toggle:$key"
    }

    data class Number(
        override val title: String,
        val key: String,
        val defaultValue: Int,
        override val summary: String? = null,
        override val dependencyKey: String? = null
    ) : SettingItem {
        override val id: String = "number:$key"
    }

    data class Text(
        override val title: String,
        val key: String,
        val defaultValue: String,
        override val summary: String? = null,
        override val dependencyKey: String? = null
    ) : SettingItem {
        override val id: String = "text:$key"
    }

    data class Slider(
        override val title: String,
        val key: String,
        val defaultValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val step: Int = 1,
        val unit: String = "dp",
        override val summary: String? = null,
        override val dependencyKey: String? = null
    ) : SettingItem {
        override val id: String = "slider:$key"
    }

    data class Action(
        override val title: String,
        val key: String,
        override val summary: String? = null,
        override val dependencyKey: String? = null
    ) : SettingItem {
        override val id: String = "action:$key"
    }
}

internal val categorySettings: Map<SettingCategory, List<SettingItem>> = mapOf(
    SettingCategory.BROWSER to listOf(
        SettingItem.Toggle("浏览器启动时打开搜索页", "browser_open_search", "启动时默认打开搜索页，支持冷热启动自动重定向到纯净搜索。"),
        SettingItem.Toggle("跳过浏览器启动广告", "browser_skip_splash", "跳过冷启动开屏欢迎广告及引导页广告。"),
        SettingItem.Toggle("去除底部视频与资讯", "browser_remove_info_tabs", "彻底移除底部导航栏中的资讯与视频入口，阻止信息流加载。"),
        SettingItem.Toggle("删除浏览器主页资讯", "browser_remove_homepage_info", "隐藏设置中的“主页资讯”，并移除主页热门资讯内容。"),
        SettingItem.Toggle("删除常用服务与短剧", "browser_slim_mine", "精简“我的”与首页，移除常用服务、热门短剧、应用推荐等内容。"),
        SettingItem.Toggle("删除搜索页热门短剧", "browser_remove_hot_words", "移除搜索页猜你想搜、热门短剧及热搜榜单。"),
        SettingItem.Toggle("删除搜索框下方快捷图标", "browser_remove_search_shortcuts", "隐藏红魔商城、58同城、问AI、京东、百度搜索快捷入口及页面指示圆点。"),
        SettingItem.Toggle("删除下载管理下载热榜", "browser_remove_download_hot", "移除下载管理界面侧边的“下载热榜”标签页及下载广告推荐。"),
        SettingItem.Toggle("删除弹出文件下载推荐", "browser_remove_download_recommend", "在网页下载文件弹出确认框时，删除应用商店劫持推荐与横幅广告，仅保留原网页下载。"),
        SettingItem.Toggle("NUBIA UI样式", "browser_compose_settings", "使用系统原生 NUBIA UI 规范重塑浏览器设置界面，并保持原生功能与交互同步。"),
        SettingItem.Toggle("隐藏切换完整功能服务按钮", "browser_hide_switch_model", "在浏览器首页基础模式中隐藏“切换完整功能服务”按钮。"),
        SettingItem.Toggle("禁用浏览器遥测与数据追踪", "browser_disable_telemetry", "拦截并彻底屏蔽 NuReport 埋点上报、热云统计、Umeng 广告追踪与南极星事件监控。")
    ),
    SettingCategory.SYSTEM_UI to listOf(
        SettingItem.Toggle("息屏显示秒数", "systemui_screen_off_show_seconds", "在息屏 AOD 界面显示实时秒数。"),
        SettingItem.Toggle("息屏显示中文时段", "systemui_screen_off_period", "在息屏 AOD 时间前显示上午/下午/晚上。"),
        SettingItem.Toggle("下拉状态栏时钟显示中文时段", "statusbar_clock_pull_down_show_period", "下拉通知中心时钟附带中文时段。"),
        SettingItem.Toggle("状态栏时钟显示秒数", "statusbar_clock_show_seconds", "顶部状态栏时钟实时显示秒数。"),
        SettingItem.Toggle("状态栏通知图标数量限制", "statusbar_notif_max", "开启后自定义状态栏最多允许显示的通知图标数。"),
        SettingItem.Number("状态栏最大通知图标数", "statusbar_notif_max_value", 10, "默认 10 个，超出部分将自动隐藏。", "statusbar_notif_max"),
        SettingItem.Toggle("移除悬浮窗通知", "remove_alert_windows_notification", "隐藏“此应用正显示在其他应用上层”提示通知。"),
        SettingItem.Toggle("禁用安全音量提示", "volume_disable_safety_warning", "调高音量时不再弹出可能损伤听力的高音量警告弹窗。"),
        SettingItem.Toggle("屏幕录制忽略 FLAG_SECURE（谨慎）", "disable_flag_secure", "允许在受系统保护界面（如隐私/密码）进行截图或录屏。"),
        SettingItem.Toggle("屏幕录制增强忽略 FLAG_SECURE（谨慎）", "disable_flag_secure_enhanced", "扩大系统截图策略修改范围，只在确认需要时启用。")
    ),
    SettingCategory.GAMING to listOf(
        SettingItem.Toggle("游戏功能限制解锁", "game_fuction_unfreeze_switch", "控制游戏功能限制解除；放行肩键、辅助线、连招、变声器和超境等入口与系统限制。"),
        SettingItem.Toggle("允许第三方悬浮窗", "game_force_overlay_capability", "解除系统限制，强制允许第三方应用在游戏内显示悬浮窗。"),
        SettingItem.Toggle("允许第三方小窗挂起", "game_allow_third_party_freeform", "解除游戏空间小窗挂起限制，允许第三方应用挂起为悬浮小窗。"),
        SettingItem.Toggle("游戏变声器限制解除", "game_magic_voice_unfreeze", "解除游戏变声器机型限制，适配全机型魔音变声与悬浮窗。"),
        SettingItem.Toggle("游戏空间音效限制解除", "game_space_sound_effects_unfreeze", "解除游戏空间音效机型限制，支持DTS/杜比全景声音效配置。"),
        SettingItem.Toggle("准星助手全机型解锁", "game_sight_assist_unfreeze", "解锁准星辅助全机型支持，支持自定义十字准星悬浮窗。"),
        SettingItem.Toggle("游戏空间肩键解锁", "game_shoulder_key_enabled", "开启后放行游戏空间与游戏助手肩键/辅助线入口（含王者荣耀等内置黑名单绕过）。"),
        SettingItem.Toggle("游戏空间主动模式解锁", "acive_mode_switch", "解除游戏空间主动模式限制。"),
        SettingItem.Toggle("超境模式与插帧允许所有应用", "game_super_resolution_unlimited_apps", "解除超境画质（超分辨率）与补帧（GFRC/MEMC）内置应用白名单限制，强制对所有游戏生效。"),
        SettingItem.Toggle("全局绕过游戏助手黑名单", "game_safe_feature_gate", "拦截黑名单检测，使侧边栏在任意应用中均可呼出，肩键连招等功能正常使用。"),
        SettingItem.Toggle("游戏空间强制全屏横屏", "force_landscape_gamespace", "解决系统更新后游戏空间可能出现的旋转异常，强制以全屏横屏展示。"),
        SettingItem.Toggle("游戏空间任意应用浮窗", "force_freeform_gamespace", "在游戏空间内支持将任意第三方应用作为小窗/悬浮窗运行。"),
        SettingItem.Toggle("禁用游戏投屏悬浮图标", "disable_screen_projection_floating_icon", "隐藏游戏投屏时常驻在屏幕边缘的浮动悬浮球。"),
        SettingItem.Toggle("隐藏吃鸡模式连招弹窗", "hide_chicken_mode_dialog", "启用连招功能时不再弹出吃鸡模式安全风险提示框。"),
        SettingItem.Toggle("游戏连招加速", "game_space_combo_speed_enabled", "一键连招宏时间戳按倍率缩放，加速播放执行。"),
        SettingItem.Number("游戏连招播放倍率（1-10）", "game_space_combo_speed_rate", 1, "连招播放速度倍率（范围 1 到 10）。", "game_space_combo_speed_enabled"),
        SettingItem.Toggle("隐藏破坏神模式退出提示", "game_space_devil_mode_hide_prompt", "退出破坏神模式时不再弹出确认对话框。"),
        SettingItem.Toggle("AI 触发器总开关", "ai_trigger_switch", "开启基于视觉模型的红魔 AI 游戏辅助触发器。"),
        SettingItem.Number("AI 模板扫描间隔（毫秒）", "ai_trigger_template_scan_ms", 180, "AI 模板扫描执行周期。", "ai_trigger_switch"),
        SettingItem.Number("AI 点击延迟（毫秒）", "ai_trigger_click_ms", 25, "触发点击操作的延迟时长。", "ai_trigger_switch"),
        SettingItem.Number("AI 触发冷却（毫秒）", "ai_trigger_cooldown_ms", 180, "两次触发动作之间的冷却间隔。", "ai_trigger_switch"),
        SettingItem.Number("AI YOLO 扫描间隔（毫秒）", "ai_trigger_yolo_scan_ms", 400, "YOLO 视觉模型扫描识别周期。", "ai_trigger_switch"),
        SettingItem.Toggle("允许双开任意应用", "double_any_app", "解除系统白名单，允许为任意已安装应用开启应用分身。"),
        SettingItem.Toggle("移除双开低内存限制", "rm_low_memory_limit", "解除低内存配置设备禁用双开的判定限制。")
    ),
    SettingCategory.FILES to listOf(
        SettingItem.Toggle("隐藏 MTP 分类浏览", "hide_mtp_category_browse", "连接电脑时不再聚合显示图片/音乐等分类浏览文件夹。"),
        SettingItem.Toggle("重命名 MTP 根目录", "mtp_rename_root_name_switch", "自定义电脑上显示的手机内部存储根目录名称。"),
        SettingItem.Text("MTP 根目录名称", "mtp_rename_root_name", "内部存储设备", "连接电脑时显示的设备卷标名称。", "mtp_rename_root_name_switch")
    ),
    SettingCategory.STORE to listOf(
        SettingItem.Toggle("应用中心跳过开屏广告", "neostore_skip_splash", "仅作用于 cn.nubia.neostore。"),
        SettingItem.Toggle("应用中心去除推荐广告", "neostore_remove_recommend_ads", "隐藏应用中心推荐卡片与 SSP 广告。"),
        SettingItem.Toggle("游戏中心跳过开屏广告", "neogamecenter_skip_splash", "仅作用于 cn.nubia.neogamecenter。"),
        SettingItem.Toggle("游戏中心去除推荐广告", "neogamecenter_remove_recommend_ads", "阻断游戏中心开屏、轮播和推荐广告请求。"),
        SettingItem.Toggle("应用商店下载启用多线程", "neostore_download_enabled", "按设定线程数创建应用商店与游戏中心的下载队列。"),
        SettingItem.Number("下载线程数（1-32）", "neostore_download_count", 5, "同时使用的下载线程数，默认 5。", "neostore_download_enabled"),
        SettingItem.Toggle("应用中心自动签到", "neostore_auto_sign", "进入应用中心后查询签到状态，未签到时调用应用中心自己的签到接口。"),
        SettingItem.Toggle("游戏中心本地超级会员", "neogame_local_svip", "在游戏中心本地状态模型中保持超级会员有效状态。")
    ),
    SettingCategory.DESKTOP to listOf(
        SettingItem.Action("锁屏时钟自定义字体", "lockscreen_custom_font_settings", "开启后选择系统已安装字体、导入自定义字体，支持粗细与大小调节。"),
        SettingItem.Toggle("主题资源免登录并绕过广告/付费判断", "cancel_trial_login", "主题壁纸应用直接放行资源下载、试用和收费判断。"),
        SettingItem.Toggle("主题启动页跳过广告", "beautify_skip_splash_ads", "打开主题时直接进入主题首页，不显示启动广告页。"),
        SettingItem.Toggle("允许第三方桌面", "allow_thirdparty_launcher", "解除系统限制，支持将第三方 Launcher 设置为默认桌面。"),
        SettingItem.Toggle("最近任务显示内存信息", "system_desktop_recent_task_display_memory", "在多任务最近任务界面清除全部按钮旁显示系统剩余与总内存信息。"),
        SettingItem.Toggle("桌面时钟适配", "system_time_component_desktop_switch", "适配红魔官方桌面时钟小组件显示样式。"),
        SettingItem.Toggle("桌面时钟显示秒数", "system_time_component_desktop_show_seconds", "桌面时钟小组件实时刷新并显示秒数。"),
        SettingItem.Toggle("桌面时钟显示中文时段", "system_time_component_desktop_show_period", "桌面时钟小组件显示上午/下午/晚上等时段信息。"),
        SettingItem.Toggle("自定义桌面应用图标与名称", "desktop_app_icon_customize_switch", "开启后支持替换桌面应用图标与名称，需重启桌面生效。")
    ),
    SettingCategory.SETTINGS to listOf(
        SettingItem.Toggle("系统更新拦截", "disable_system_update", "拦截系统服务自动检查与静默下载 OTA 更新。"),
        SettingItem.Toggle("系统更新 URL 复制到剪贴板", "update_package_address", "检测到系统更新时自动捕获 OTA 升级包下载直链并存入剪贴板。"),
        SettingItem.Toggle("隐藏安装器商店提示", "hide_store_install_prompt", "安装应用时隐藏推荐前往官方应用商店下载的安全提示。"),
        SettingItem.Toggle("跳过 APK 扫描", "skip_pkg_scan", "安装应用时跳过漫长的系统病毒与安全扫描。"),
        SettingItem.Toggle("CTS 安装器调用者", "cts_test_installer", "使用原生简洁的 CTS 测试安装界面。"),
        SettingItem.Toggle("纯净安装模式移入右上角与自动同意风险", "hide_evolution_mode_toggle", "将应用安装界面的纯净安装模式开关收进右上角菜单，并默认勾选已了解该应用存在的风险，隐藏了解更多按钮。"),
        SettingItem.Toggle("设置中免验证与免调试开启USB安装", "disable_usb_installation_and_switch_account_verification", "在系统设置中即使未开启ADB调试也可直接开启USB安装应用，免输账号密码。"),
        SettingItem.Toggle("设置中隐藏电池百分比选项", "disable_attery_percentage_display_setting_option", "在系统设置中隐藏电池百分比显示选项。"),
        SettingItem.Toggle("设置中关闭自动息屏改写", "automatic_screen_off", "解锁设置-显示-自动息屏中的“从不”和“30分钟”选项。"),
        SettingItem.Toggle("时间选择器显示 AM/PM", "time_picker_period", "系统时间选择器中支持显示 12 小时制上下午标识。"),
        SettingItem.Toggle("允许所有应用使用小窗", "remove_restrictions_window", "解除系统黑名单和小窗限制，强制允许所有应用开启小窗口。"),
        SettingItem.Toggle("窗口回复图标数量限制", "remove_restrictions_window_number", "解除最多挂起 3 个小窗图标的系统数量上限。"),
        SettingItem.Toggle("意图劫持兼容", "android_remove_intent_hijack_content", "去除安装应用强制跳转官方安装器，并还原系统原生分享界面。"),
        SettingItem.Toggle("系统锁屏超时改写", "system_framework_lock_screen_timeout", "自定义系统空闲锁屏等待时长。"),
        SettingItem.Number("系统锁屏超时（分钟）", "system_framework_lock_screen_timeout_value", 10, "锁屏超时分钟数（默认为 10 分钟）。", "system_framework_lock_screen_timeout")
    ),
    SettingCategory.OTHERS to listOf(
        SettingItem.Toggle("NFC 静音", "mute_nfc_sound", "连接或感应 NFC 标签卡片时静音系统提示音。"),
        SettingItem.Toggle("允许熄屏识别 NFC", "nfc_allow_screen_off_recognition", "支持在手机熄屏锁定状态下正常读取与感应 NFC。")
    )
)
