# RedMagic Helper

**RedMagic Helper** 是面向红魔手机的 LibXposed 模块，提供游戏空间、浏览器、系统界面、应用安装和 NFC 等功能适配。设置按类别排列，可以逐项启用；模块在对应的系统或应用进程中运行 Hook。

[![Android](https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/14)
[![LibXposed](https://img.shields.io/badge/LibXposed-API%20102-5263D8)](https://github.com/libxposed)

## 目录

- [功能](#功能)
- [运行要求](#运行要求)
- [安装与启用](#安装与启用)
- [检查更新](#检查更新)
- [本地构建](#本地构建)
- [兼容性说明](#兼容性说明)

## 功能

| 分类 | 功能 |
| --- | --- |
| 浏览器 | 跳过开屏广告、启动时打开搜索页；移除资讯和视频入口、主页资讯与热榜、搜索热词和短剧、快捷图标、下载热榜及下载推荐；精简“我的”页面；调整设置界面并关闭遥测。 |
| 系统界面 | 调整锁屏时钟字体、息屏和状态栏时钟，显示秒数或中文时段；设置通知图标数量；移除悬浮窗通知和安全音量提示；按需调整受保护页面的截图与录屏限制。 |
| 游戏空间 | 解除肩键、连招、变声器、音效、准星、主动模式和 4D 震感的部分机型或应用限制；调节肩键连点倍率和连招播放速度；控制超境画质、插帧、横屏、小窗、悬浮窗及游戏助手提示。 |
| 游戏辅助 | 提供视觉触发器参数设置；允许应用分身；隐藏部分游戏空间提示和投屏悬浮图标。 |
| 应用商店与游戏中心 | 跳过开屏广告、设置下载线程数、进入应用中心后自动签到，以及调整本地超级会员状态。 |
| 安装与系统设置 | 拦截系统更新检查或复制更新包地址；调整 APK 扫描、安装提示、USB 安装验证和安装器界面；修改自动息屏、时间选择器、小窗数量与锁屏超时设置。 |
| 桌面与主题 | 跳过主题启动广告、调整主题资源的登录和付费判断、允许第三方桌面；在最近任务显示内存信息；适配桌面时钟和自定义应用图标、名称。 |
| 文件与 NFC | 调整 MTP 分类浏览和存储卷标；静音 NFC 提示音，并允许熄屏时识别 NFC。 |
| 外观与设备信息 | 设置色彩风格、色彩标准、预测性返回、横移返回和页面缩放；查看 SELinux 状态、设备型号及系统指纹。 |

各项功能由设置开关控制。需要重启宿主进程的改动会在保存后提示；可以从设置页面重启对应作用域进程。

## 运行要求

- Android 14（API 34）或更新版本。
- 已安装并启用兼容 LibXposed API 102 的 Xposed 框架和管理器。
- 红魔系统应用及对应功能仍存在于当前 ROM 中。部分选项只对特定系统组件或应用版本有效。

模块预设作用域写在 [`scope.list`](app/src/main/resources/META-INF/xposed/scope.list) 中，包含系统服务、系统界面、系统设置、安装器、浏览器、游戏空间、应用商店、文件管理器、桌面、主题和 NFC 等目标进程。请在框架管理器中确认需要适配的目标已启用。

`zte.com.cn.filter` 虽列在默认作用域中，目前没有对应的 Hook 适配器。

## 安装与启用

1. 从 [GitHub Releases](https://github.com/Chrrr1y/RedMagic/releases) 下载 APK 并安装。
2. 在兼容的 Xposed 管理器中启用 RedMagic Helper，并检查目标作用域。
3. 打开红魔助手，进入对应功能分类，按需启用选项。
4. 若设置提示需要重启进程，请保存后重启对应应用；修改系统服务相关选项时按提示重启设备或相关进程。

## 检查更新

打开应用的“设置”页后，软件版本行会自动查询 GitHub 最新 Release。查询结果缓存 6 小时；点按软件版本名称可立即刷新。发现更新后点击“下载”会打开 Release APK 下载链接；安装仍需由用户确认。

## 本地构建

构建环境：JDK 21、Android SDK Platform 37，以及可访问 Google Maven、Maven Central 和 Gradle Plugin Portal 的网络。

默认构建 Release APK，并生成版本信息、校验和及更新说明：

```bash
./scripts/build-release.sh
```

产物写入 `dist/release/`，构建元数据写入 `app/build/generated/version/`。Release 标签格式为 `v<versionName>-build-<versionCode>`；版本号默认来自 `app/build.gradle`，也可通过 `VERSION_NAME`、`VERSION_CODE` 环境变量覆盖。

此流程只编译 Release。需要 Debug APK 时再单独执行：

```bash
./gradlew --no-daemon :app:assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

使用已认证的 GitHub CLI 发布 Release：

```bash
gh auth login
./scripts/build-release.sh --publish
```

如果没有配置 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS` 和 `RELEASE_KEY_PASSWORD`，Release APK 会使用本地 debug 签名。该 APK 不能覆盖由另一把密钥签名的安装包。

## 兼容性说明

本模块通过 LibXposed Hook 适配红魔系统组件和应用，不修改宿主 APK。Hook 点依赖具体 ROM 和应用版本；目标类或方法不存在时，对应能力会跳过，因此同一选项在不同系统版本上的表现可能不同。更新系统应用或 ROM 后，部分功能可能需要重新验证。

源码和问题反馈：[GitHub 仓库](https://github.com/Chrrr1y/RedMagic)。
