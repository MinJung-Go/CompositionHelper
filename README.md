# CompositionHelper Android

原生摄影构图与拍后调色工具。本分支为 `master`；另一端见 [ios 分支](https://github.com/MinJung-Go/CompositionHelper/tree/ios)。两端各自实现，当前尚未完全统一。

## 功能与状态

- 实时相机、19 种构图辅助线、端侧主体检测与构图建议。建议分数不是专业审美评分。
- 原图导入、四张内置样片、Gemini 拍后调色及本地手动精调、前后对比、撤销、保存副本。
- 本轮新增真人拍摄清单：12 项、人工状态、备注、本机保存、导出和重置。
- 编译与自动检查不替代真人验收；清单代码和旧安装包不能混为同一版本。状态见 [交付记录](docs/RELEASES.md)。

## 开始使用

```bash
git clone -b master https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
```

使用 JDK 17、仓库 Gradle Wrapper 8.2 和 Android SDK 34：

```bash
./gradlew :app:assembleDebug --no-daemon --max-workers=2
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。运行最低 API 24；当前锁定竖屏。详细安装见 [Android 指南](docs/ANDROID.md)。

## 产品方向

定位为轻量摄影工具：核心功能全部买断，AI 使用用户自己的 Key 并计划在本机安全保存；支付、安全存储和新手引导尚未实施，无业务后端计划。详见 [产品定位与路线](docs/PRODUCT_STRATEGY.md) 和 [技术架构](docs/ARCHITECTURE.md)。

## 文档导航

- [全部文档与维护规则](docs/INDEX.md)
- [双端架构](docs/ARCHITECTURE.md) · [UI 与交互约定](docs/DESIGN_SYSTEM.md)
- [构图功能说明](docs/FEATURES.md) · [参考照片与许可](docs/SAMPLE_PHOTOS.md)
- [开发/测试验收表](docs/CROSS_PLATFORM_ACCEPTANCE.md) · [真人拍摄清单](docs/SHOOTING_CHECKLIST.md)
- [测试运行指南](docs/TESTING.md) · [当前已知问题](docs/KNOWN_ISSUES.md)
- [数据与隐私](docs/DATA_PRIVACY.md) · [版本与交付](docs/RELEASES.md)

## 许可

代码许可见 [LICENSE](LICENSE)。内置照片作者和许可单独记录在 [SAMPLE_PHOTOS.md](docs/SAMPLE_PHOTOS.md)。
