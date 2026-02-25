# Android 详细文档

> CompositionHelper Android 版本的安装、构建、架构和配置指南

---

## 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Flamingo | Jellyfish+ |
| JDK | 17 | 17 |
| Android SDK | API 24 | API 34 |
| Gradle | 8.0 | 8.5+ |

> **注意**: JDK 17 是必须的（AGP 8.2.0 要求），JDK 11 无法编译。

---

## 安装步骤

### 方式一：克隆并打开

```bash
git clone https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
# Android Studio > Open > 选择项目目录
```

### 方式二：已克隆仓库

```bash
git fetch origin
git checkout master
git pull origin master
```

---

## 运行

### 模拟器

1. Tools > Device Manager > 创建/选择模拟器 (API 29+)
2. Run (`Shift + F10`)

### 真机

1. **开启开发者选项**: 设置 > 关于手机 > 连续点击"版本号" 7 次
2. **开启 USB 调试**: 设置 > 开发者选项 > USB 调试
3. 连接设备，授权 USB 调试，在 Android Studio 中选择设备运行

---

## 技术栈

| 技术 | 版本/说明 |
|------|---------|
| Kotlin | 1.9.20+ |
| Jetpack Compose | UI 框架 |
| Material Design 3 | 设计系统 |
| CameraX | 1.3.1 (Preview + ImageAnalysis + ImageCapture) |
| ML Kit | Object Detection (STREAM_MODE) |
| Navigation Compose | 页面导航 |
| Accompanist Permissions | 运行时权限 |
| Coil Compose | 图片加载 |
| minSdk | API 24 (Android 7.0) |
| targetSdk | API 34 (Android 14) |
| AGP | 8.2.0 |
| Gradle | 8.2 |

---

## 项目结构

```
app/src/main/java/com/example/compositionhelper/
├── MainActivity.kt                     # 入口 Activity + Navigation + 沉浸式
├── CompositionHelperApp.kt             # 相册分析模式 UI
├── ImageAnalyzer.kt                    # 全量图像分析器（相册模式用）
│
├── model/                              # 共享数据模型
│   ├── CompositionModels.kt            # CompositionType 枚举 (19种)
│   │                                   # CompositionCategory 枚举 (3类)
│   └── AnalysisModels.kt              # DetectedSubject, RectF, PointF,
│                                       # FrameAnalysisResult 等数据类
│
├── overlay/                            # 构图叠加绘制
│   ├── CompositionDrawing.kt           # CompositionRenderer 接口
│   │                                   # CanvasRenderer (Bitmap 模式)
│   │                                   # DrawScopeRenderer (Compose 模式)
│   │                                   # 19 种构图的绘制函数
│   ├── CameraCompositionOverlay.kt     # Compose Canvas 实时叠加层
│   │                                   # 主体边框 + 关键点对齐提示
│   └── CompositionOverlay.kt          # Bitmap 叠加 (相册模式用)
│
├── camera/                             # 实时相机模块
│   ├── CameraManager.kt               # CameraX 生命周期封装
│   │                                   # initialize / bindPreview / capturePhoto
│   ├── FrameAnalyzer.kt               # ImageAnalysis.Analyzer 实现
│   │                                   # 2.5s 节流 + ML Kit 物体检测
│   │                                   # LightweightAnalyzer 推荐算法
│   └── CameraCompositionScreen.kt     # 全屏沉浸式相机 UI
│
└── ui/
    ├── components/
    │   ├── ControlPanel.kt             # 底部控制栏 + 设置 BottomSheet
    │   ├── ShutterButton.kt           # 拍照按钮
    │   └── RecommendationChip.kt      # AI 推荐浮层 (分数环 + 方向提示)
    └── theme/
        └── Theme.kt                    # Material 3 主题 + 透明系统栏
```

---

## 架构设计

### 双模式架构

| 模式 | 入口 | 功能 |
|------|------|------|
| 实时相机 (默认) | `CameraCompositionScreen` | 取景器 + 实时构图叠加 + AI 推荐 |
| 相册分析 | `CompositionHelperApp` | 选照片 + 全量分析 + 构图叠加 |

两种模式通过 Navigation Compose 管理（`"camera"` / `"gallery"` 路由）。

### 绘制代码复用

`CompositionRenderer` 接口抽象了所有绘制操作（drawLine, drawCircle, drawRect, drawArc, drawPath），19 种构图的绘制函数只写一份：

- **相册模式**: `CanvasRenderer` 包装 `android.graphics.Canvas`，在 Bitmap 上绘制
- **相机模式**: `DrawScopeRenderer` 包装 Compose `DrawScope`，在 Canvas composable 上绘制

### 实时分析流水线

```
CameraX ImageAnalysis (640x480, YUV)
    ↓ 2.5s 节流
FrameAnalyzer (InputImage.fromMediaImage)
    ↓
ML Kit ObjectDetector (STREAM_MODE)
    ↓
LightweightAnalyzer.recommend()
    ↓ 基于主体位置判断
FrameAnalysisResult {
    recommendedType: CompositionType
    confidence: Float
    detectedSubjects: List<DetectedSubject>
    guidanceHint: String?  // "稍微向右移动"
}
    ↓ Handler.post → 主线程
CameraCompositionOverlay 更新
```

---

## 构建

### 命令行

```bash
./gradlew clean              # 清理
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK
./gradlew installDebug       # 安装到设备
```

### APK 输出

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

### GitHub Actions CI

每次推送到 `master` 分支会自动触发 CI 构建。配置文件: `.github/workflows/android-ci.yml`

---

## 权限

| 权限 | 用途 | 请求时机 |
|------|------|---------|
| `CAMERA` | 实时预览和拍照 | 进入相机模式时 |
| `READ_EXTERNAL_STORAGE` | 访问相册 (Android 12-) | 进入相册模式时 |
| `READ_MEDIA_IMAGES` | 读取图片 (Android 13+) | 进入相册模式时 |

AndroidManifest.xml 中同时声明了 `android.hardware.camera` 和 `android.hardware.camera.autofocus`，均设为 `required="false"` 以兼容无相机设备。

---

## 常见问题

### Gradle 同步失败

```bash
# 确认 JDK 版本 (必须 17)
java -version

# 清理并刷新
./gradlew clean && ./gradlew --refresh-dependencies
```

### 找不到设备

```bash
adb devices           # 检查连接
adb kill-server       # 重启 ADB
adb start-server
```

确认：开发者选项已启用、USB 调试已开启、设备已授权。

### 构建失败

```bash
./gradlew clean
rm -rf .gradle app/build
# Android Studio: File > Invalidate Caches / Restart
```

### ML Kit 分析不工作

- 确认设备已安装 Google Play 服务
- 检查权限: 设置 > 应用 > CompositionHelper > 权限
- 查看日志: `adb logcat | grep -E "MLKit|FrameAnalyzer"`

### 相机预览问题

- 模拟器相机功能有限，建议真机测试
- 确认 `CAMERA` 权限已授予
- PreviewView 使用 `PERFORMANCE` 模式 (SurfaceView)，个别低端设备可能需要切换到 `COMPATIBLE` 模式

---

## 发布

### 签名配置

```bash
# 创建密钥库
keytool -genkey -v -keystore release.keystore \
    -alias compositionhelper -keyalg RSA -keysize 2048 -validity 10000
```

在 `app/build.gradle.kts` 中配置 signingConfig，然后 `./gradlew assembleRelease`。

### Google Play 发布

1. 注册 [Google Play Console](https://play.google.com/console)
2. 创建应用，上传图标和截图
3. 上传签名 APK/AAB
4. 完成内容分级问卷
5. 提交审核 (1-3 天)

---

## 参考文档

- [CameraX](https://developer.android.com/training/camerax) - 相机框架
- [ML Kit Object Detection](https://developers.google.com/ml-kit/vision/object-detection) - 物体检测
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI 框架
- [Material Design 3](https://m3.material.io/) - 设计系统
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - 导航

---

**返回 [主 README](../README.md)**
