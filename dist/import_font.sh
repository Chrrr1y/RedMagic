#!/bin/bash
# 导入 GoogleSans 字体到红魔/努比亚手机 (个性主题/com.zte.beautify)
# 基于系统内建 loadFontFromAndroidQ 机制，无需 Root

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ZIP_FILE="$SCRIPT_DIR/googlesans.zip"

if [ ! -f "$ZIP_FILE" ]; then
    echo "[-] 错误: 找不到 $ZIP_FILE"
    exit 1
fi

echo "[*] 检测 adb 设备..."
adb wait-for-device

echo "[*] 创建手机端导入目录: /sdcard/launcherBeautify/font/googlesans/"
adb shell "mkdir -p /sdcard/launcherBeautify/font/googlesans"

echo "[*] 推送字体包到设备..."
adb push "$ZIP_FILE" /sdcard/launcherBeautify/font/googlesans/googlesans.zip

echo "[*] 重启 '个性主题' (com.zte.beautify) 触发自动解压与入库..."
adb shell am force-stop com.zte.beautify
sleep 1
adb shell am start -n com.zte.beautify/com.zte.beautify.BeautifyActivity

echo "[+] 字体导入成功！"
echo "[+] 请在手机上打开 '个性主题' -> '我的' -> '字体'，即可看到并应用 'Google Sans' 字体。"
