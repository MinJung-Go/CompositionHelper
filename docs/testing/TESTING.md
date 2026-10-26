# 构建与测试运行指南

更新：2026-09-10。所有命令从所属分支根目录执行。运行前记录 `git rev-parse HEAD`；结果只属于该提交。应用内拍摄清单是人工记录，不代替这些测试。

## Android：master

环境：JDK 17、Gradle Wrapper 8.2、AGP 8.2.0、Android SDK platform 34 / build-tools 34.0.0。通过 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 指向自己的 SDK，不提交机器路径。首次运行需下载依赖；只有依赖缓存齐全才加 `--offline`。

```bash
java -version
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --max-workers=2
./gradlew :app:assembleDebugAndroidTest --no-daemon --max-workers=2
```

| 产物 | 位置 |
|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| 测试 APK | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` |
| JVM XML / HTML | `app/build/test-results/testDebugUnitTest/`、`app/build/reports/tests/testDebugUnitTest/` |
| lint | `app/build/reports/lint-results-debug.html` 及同目录 XML |

设备开启 USB 调试并授权；模拟器须完成系统启动。选择目标设备，避免误装到不相关设备：

```bash
adb devices
adb -s DEVICE_SERIAL shell getprop sys.boot_completed
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

替换 `DEVICE_SERIAL`。设备测试时只连接待测设备，或配置 `ANDROID_SERIAL`：

```bash
./gradlew :app:connectedDebugAndroidTest --no-daemon --max-workers=2
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.compositionhelper.checklist.ShootingChecklistTest --no-daemon --max-workers=2
```

报告在 `app/build/reports/androidTests/connected/` 和 `app/build/outputs/androidTest-results/connected/`。清单测试会清空测试设备内本应用的清单偏好；使用专用测试安装，不在有待保留记录的手机上直接执行。导出真人记录后再开始测试。

当前宿主缺少 `/dev/kvm`，软件模式启动未完成，清单两项交互测试仅编译成功，不能记为运行通过。Android CI 目前仅构建 APK，JVM/lint/设备测试结果需单独保存。

## iOS：ios

App 部署目标 iOS 16；使用具备 Swift 5.9+ 和 iOS 16+ SDK 的 Xcode。实际成功工具链版本应从对应 CI 日志读取，不把推荐环境当成已验证机型。

```bash
xcodebuild -version
swift --version
tools/test-color-core.sh
xcodebuild build -project CompositionHelper.xcodeproj -scheme CompositionHelper -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
xcodebuild build -project CompositionHelper.xcodeproj -scheme CompositionHelper -sdk iphoneos -configuration Release CODE_SIGNING_ALLOWED=NO
```

最后一条仅构建未签名 App，不等于可直接安装。IPA 打包由 `.github/workflows/ios-build-ipa.yml` 完成。`tools/test-color-core.sh` 也可在 Linux Swift 5.9+ 环境运行；它只验证 Foundation 核心逻辑，不检查 SwiftUI 类型或运行界面。

现有 iOS CI 包含模拟器编译、Info.plist 检查、核心回归及截图步骤。使用以下 **Debug 专用**启动参数检查页面：

- `--review-color`：载入城市样片的编辑器。
- `--review-color --review-empty`：空状态及样片区。
- `--review-checklist`：真人清单页面；该入口属于本轮尚未完成云端编译的新代码。

CI 截图上传为 `editor-review`，IPA 上传为 `CompositionHelper-unsigned`。iOS lint 步骤允许失败，不能把 workflow 绿色等同于零 lint 违规。没有为真人清单添加 iOS UI 自动交互测试；截图仅验证当时画面，不能证明持久化、分享或实际保存成功。

## 两端一致性检查

1. 对照 `docs/reference/SHOOTING_CHECKLIST.json`，确认 12 个稳定 ID、分组、标题、描述与两端原生数组一致。
2. 同一配方和参考像素比较输出，记录舍入容差；不要用不同尺寸预览或 JPEG 二次压缩后的文件代替原始像素比较。
3. 用 [验收表](CROSS_PLATFORM_ACCEPTANCE.md) 按真实设备分别执行；不同平台独立记录，不能互相代签。
4. 截图至少覆盖小屏、大字体、空状态、图片加载及错误恢复；Android 横屏当前受锁定配置限制，记录为未支持，而非通过。

## 证据保留与判定

记录平台、设备/系统、提交号、命令、退出码、测试数量、报告、截图和安装包 SHA256。区分：未执行、环境阻塞、编译失败、测试失败、通过。报告保存在 [版本交付记录](RELEASES.md) 所指的提交/CI 或受维护的文档目录；`build/` 下的临时文件不应作为唯一证据。

问题复现从 [已知问题](KNOWN_ISSUES.md) 的对应项开始；修复后只复跑相关检查和必要的回归，保留前后证据。
