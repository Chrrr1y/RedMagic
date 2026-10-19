# RedMagic Helper Rewrite

RedMagic Helper Rewrite 是基于 LibXposed API 102 的 Android 模块，按宿主进程分别安装 Hook，不修改宿主 APK，也不通过替换设备型号绕过检查。

## 功能范围

模块覆盖系统更新、系统界面、游戏助手与游戏空间、桌面、设置、NFC、风扇、安装器、权限控制器、NeoStore、Beautify，以及 Nubia Browser。功能按设置项启用；宿主版本缺少目标类或方法时只跳过对应能力并记录 `CAPABILITY_MISSING`。

浏览器适配默认关闭。设置页可以单独控制启动广告、默认搜索页、资讯入口、“我的”页面和下载推荐等项目。

设备提取的 APK、反编译目录和临时工作文件不纳入 Git。

LSPosed 的 system_server 作用域使用包名 `android`，已包含在 `scope.list`。`zte.com.cn.filter` 也已加入可选作用域；该包目前没有专用 Hook 适配器。

## 本地构建

需要 JDK 17、Android SDK Platform 36 和网络连接以解析 Gradle 插件及依赖。

构建 Debug APK：

```bash
./gradlew --no-daemon :app:assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。构建不会自动上传 APK 到第三方文件服务。

构建 Release APK、生成版本号、校验和和更新说明：

```bash
./scripts/build-release.sh
```

发布到 GitHub Release：

```bash
gh auth login
./scripts/build-release.sh --publish
```

基础版本为 `4.0.1`，不会再带 `rewrite` 后缀。脚本会自动生成 `versionCode`，并将 APK、SHA-256 和 `RELEASE_NOTES.md` 写入 `dist/release/`。构建元数据同时写入 `app/build/generated/version/`。

## GitHub Release

Release 仅通过本地 `./scripts/build-release.sh --publish` 调用 GitHub CLI 发布，不配置 GitHub Actions 工作流。未设置 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS` 和 `RELEASE_KEY_PASSWORD` 时，APK 使用本机 debug 签名；要保持后续版本可覆盖安装，请在本机配置固定的 Release keystore。

本模块使用自己的签名，不能直接覆盖由原作者签名的旧版模块。升级前需在 LSPosed/管理器备份设置和作用域，并确认接受签名不同带来的重装要求。
