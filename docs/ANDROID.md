# Android 安装、构建与使用

更新：2026-09-10。源码在 `master` 分支；它是 Android 分支，不含 Xcode 工程。架构见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 构建环境

| 项目 | 当前项目配置 |
|---|---|
| JDK | 17 |
| Gradle Wrapper | 8.2，使用仓库 `gradlew` |
| Android Gradle Plugin | 8.2.0 |
| SDK | compileSdk / targetSdk 34，build-tools 34.0.0 用于当前构建环境 |
| 最低运行系统 | API 24 |
| 页面方向 | 当前 Activity 锁定竖屏 |

`minSdk 24` 是运行要求，不是编译只需下载 API 24。通过 Android Studio 打开项目，设置 SDK 路径；可在本地 `local.properties` 配置 `sdk.dir`，不要提交绝对路径。首次构建需联网下载缺少的依赖，缓存完整后再考虑 `--offline`。

```bash
git clone -b master https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
java -version
./gradlew :app:assembleDebug --no-daemon --max-workers=2
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。远端未推送的功能不会随 clone 出现，请核对 [交付记录](RELEASES.md) 的提交号。

## 安装与入口

手机开启开发者选项、USB 调试并授权电脑；选择具体设备后安装：

```bash
adb devices
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

替换设备序列号。运行默认进入相机，底部相册按钮进入构图工作室；参考照片可进入调色。拍照保存原片后进入色彩编辑器。清单版本从相机顶部图标打开检查清单，支持人工状态、备注、导出及重置。

仓库 `view-android.sh` 可辅助打开 `--camera` 或 `--gallery`，例如 `./view-android.sh --gallery --screenshot /tmp/compositionhelper.png --wait 30`。自动化脚本不替代对实际照片和保存结果的核对。

## 相机、权限与选图

CameraX 预览当前使用 **COMPATIBLE / TextureView**，并通过 ViewPort 关联画幅。Manifest 中相机硬件设为非必需；无设备或权限失败时仍需验证恢复入口。

- 相机权限用于预览与拍照。
- API 24–28 拍照保存涉及写存储权限，须验证授予、拒绝和重试。
- 构图工作室主要使用系统 `OpenDocument` 选图，并尝试保留所选 URI 的读取授权。
- 代码和 Manifest 仍保留部分独立相册读取权限逻辑；不能把整个相册权限描述为所有系统选图的固定前置条件。

数据和备份实际范围见 [DATA_PRIVACY.md](DATA_PRIVACY.md)。

## 测试与故障定位

[TESTING.md](TESTING.md)包含 JVM、lint、设备测试命令及报告位置。当前 CI 只执行 Debug APK 构建，测试结果需另外保存。

- 构建失败先记录第一条错误、JDK、SDK 和依赖缓存状态；不要默认删除所有缓存。
- 无设备先检查 `adb devices` 和手机授权；Linux 模拟器缺少 KVM 加速可能无法在合理时间启动。
- 相机无预览要区分初始化、绑定、首帧和权限；不要只看绑定成功日志。
- 模型初始化失败应保留具体错误，不将所有问题一概归因于某个系统服务。
- 更新安装失败先核对 package ID、签名与版本；卸载可能清除清单记录，先导出重要数据。

## 发布边界

当前 Debug APK 是测试包。项目仍使用示例 package ID，正式 release signingConfig 尚未配置；没有完成正式渠道发布验收。版本递增、签名、回退与证据要求见 [RELEASES.md](RELEASES.md)。真实渠道政策应在发布时重新核实，这里不承诺审核时长。
