# 双端实时构图检测清单

[需求与技术设计](../architecture/REALTIME_COMPOSITION.md)

| 平台 | 需求分支 | 目标分支 | 基线 |
| --- | --- | --- | --- |
| Android | `feat/realtime-composition-android` | `master` | `5a597c2` |
| iOS | `feat/realtime-composition-ios` | `ios` | `be29e08` |

- [x] 同步远端基线并保留旧分支。
- [x] 定义两端检测证据、刷新频率和切换条件。
- [x] 接入两端相机帧，覆盖候选评分与稳定逻辑。
- [ ] 完成 Android Gradle 构建、iOS Xcode 编译和两端安装包验证。
- [ ] 分别在真机上核对推荐延迟、预览流畅度、误报和提示跳变。
- [ ] 提交、推送需求分支并开 PR。

## 本分支验证 · 2026-11-07

- 使用 Java 17、Gradle 8.2 随附的 Kotlin JVM 编译器和 JUnit 直接编译相机几何与构图规则。运行 `CameraGeometryTest` 和 `LightweightAnalyzerTest`，9 项通过。
- 已核对 OpenCV AAR 的 SDK 元数据。新增 Kotlin 文件通过语法检查。
- Gradle 8.2 已配置 app 项目，在解析 `:app:compileDebugJavaWithJavac` 依赖时报告 `SDK location not found`。Windows 主机缺完整 Android SDK 34，下载尝试超时或连接中断。未生成 APK，未运行设备测试。
- SDK 就绪后执行 `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --max-workers=2`，再从生成的 APK 验收相机智能模式。
## 合并前相机修复

- [x] Android 使用专用串行执行器分析帧，预览重绑或退出时解除旧分析器。
- [ ] 在真机切换智能模式、重绑画幅并快速离开相机，核对预览、触控和拍照响应。
## Android 16 KB 兼容

- [x] 更新 CameraX 和 ML Kit Object Detection，移除应用源码未使用的姿态与自拍分割依赖。
- [x] 使用压缩 JNI 库打包，在 Android CI 检查最终 APK 的 ELF 和 ZIP 对齐。
- [ ] 在 16 KB Android 环境验收 APK 安装、启动和智能相机。
