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

正常发布不依赖 GitHub Actions；本地 `./scripts/build-release.sh --publish` 会直接调用 GitHub CLI 创建 Release。`.github/workflows/release.yml` 仅保留为手动触发的备用流程，不会因推送 `main` 消耗 Actions 额度。

未配置签名 Secrets 时，Release 使用 GitHub runner 的临时 debug keystore，APK 可以安装，但不同 runner 的签名不保证相同，后续版本可能无法覆盖升级。要保持签名一致，请配置以下仓库 Secrets：

- `ANDROID_KEYSTORE_BASE64`：Release keystore 的 Base64 内容，例如用 `base64 -w0 redmagic-release.jks` 生成。
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

本模块使用自己的签名，不能直接覆盖由原作者签名的旧版模块。升级前需在 LSPosed/管理器备份设置和作用域，并确认接受签名不同带来的重装要求。
