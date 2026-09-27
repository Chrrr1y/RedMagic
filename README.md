# RedMagic Helper Rewrite

这是从设备 APK 离线分析后重新编写的 LibXposed API 102 模块，不是对原 APK 的机型条件或
白名单打补丁。

当前工作区包含：

- `app/src/main/java/`：新的 Hook 入口、运行时反射适配层和按作用域拆分的宿主适配器。

离线提取的设备 APK、反编译文件和临时工作产物不纳入 Git。

适配策略：不读取或替换设备型号，也不把功能开关改成无条件开启。模块使用 APK 内声明的静态
作用域，避免直接更新模块后依赖旧的动态作用域缓存；每个作用域只在
对应配置打开、且当前宿主类/方法签名存在时安装 Hook；找不到能力时记录
`CAPABILITY_MISSING` 并继续运行其他作用域。当前已覆盖原静态清单中的系统更新、Beautify、
游戏助手/游戏空间/游戏高光/游戏实验室、MTP、NeoStore、安装器、权限控制器、桌面、设置、
推荐图标、Mi Health、NFC、风扇、系统服务入口，以及设备提取版本
`cn.nubia.browser`（`V8.1.4.2026081811a` / versionCode `22974`）。当前作用域共 24 个。

浏览器适配默认关闭，可在设置页单独控制跳过启动广告、启动搜索页、隐藏资讯/视频入口和精简
“我的”页面。它只通过 `BrowserAdapter` 在浏览器进程内 Hook 已确认的类、方法和资源，不修改
原浏览器 APK；后续版本缺失能力时只记录 `CAPABILITY_MISSING`。

构建：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

生成 APK：`app/build/outputs/apk/debug/app-debug.apk`。

推送到 `main` 的每个 commit 都会运行单元测试、构建 Release APK，并创建 GitHub Release。
Release 包默认使用 CI 临时 debug 签名，可直接安装，但不同 CI runner 之间不能保证签名一致。
要让后续版本覆盖升级，请在 GitHub 仓库 Secrets 中配置 `ANDROID_KEYSTORE_BASE64`、
`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS` 和 `ANDROID_KEY_PASSWORD`，供 Action 使用同一
Release keystore。

本地 Release 构建：`./gradlew :app:assembleRelease`。
