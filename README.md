# CompositionHelper Android

> 智能摄影构图辅助工具 — 实时相机引导 + AI 构图推荐

[![Android CI](https://github.com/MinJung-Go/CompositionHelper/actions/workflows/android-ci.yml/badge.svg)](https://github.com/MinJung-Go/CompositionHelper/actions)
[![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com/studio)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

> **iOS 版本**: 查看 [ios 分支](https://github.com/MinJung-Go/CompositionHelper/tree/ios)

---

## 功能亮点

- **实时相机构图引导** — 取景器中直接叠加构图辅助线，所见即所得
- **AI 智能推荐** — ML Kit 物体检测 + 轻量算法，自动推荐最佳构图并给出方向提示
- **19 种构图类型** — 经典(7) / 现代(7) / 视角(5) 三大分类，覆盖各类拍摄场景
- **主体追踪对齐** — 检测主体位置，对齐构图关键点时实时高亮提示
- **相册分析模式** — 对已拍照片进行全量构图分析和推荐
- **自定义辅助线** — 透明度和颜色可调

---

## 快速开始

### 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Flamingo | Jellyfish+ |
| JDK | 17 | 17 |
| Android SDK | API 24 | API 34 |
| Gradle | 8.0 | 8.5+ |

### 安装与运行

```bash
git clone https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
# 用 Android Studio 打开项目目录即可
```

**模拟器**: Tools > Device Manager > 创建/选择模拟器 (API 29+) > Run

**真机**: 开启 USB 调试 > 连接设备 > Run

---

## 使用指南

### 实时相机模式（默认）

应用启动后直接进入全屏实时取景器：

```
┌──────────────────────────┐
│ [<-]        [手动/智能]   │  <- 顶栏
│                          │
│    CameraX 实时预览       │
│    + 构图辅助线叠加       │  <- 全屏取景器
│                          │
│  ┌ AI 推荐 ────────────┐ │
│  │ 三分法  92%         │ │  <- 智能模式浮层
│  │ 稍微向右移动         │ │
│  └────────────────────┘ │
│ [构图类型选择器]          │
│ [设置]    [拍照]    [相册]│  <- 底部控制栏
└──────────────────────────┘
```

1. **选择构图** — 底部横滑选择器切换 19 种构图类型
2. **智能模式** — 顶部切换 "手动/智能"，AI 自动推荐最佳构图
3. **对齐引导** — 移动相机使主体对齐关键点（绿=已对齐，黄=接近）
4. **拍照** — 构图满意后点击快门

### 相册分析模式

点击右下角相册图标进入，可对已有照片进行构图分析和辅助线叠加。

---

## 19 种构图类型

| 分类 | 构图类型 |
|------|---------|
| **经典** (7) | 三分法 / 中心构图 / 对角线 / 框架构图 / 引导线 / S 形曲线 / 黄金螺旋 |
| **现代** (7) | 黄金三角 / 对称构图 / 负空间 / 模式重复 / 隧道式 / 分割构图 / 透视焦点 |
| **视角** (5) | 隐形线 / 充满画面 / 低角度 / 高角度 / 深度层次 |

> 每种构图的详细说明和使用技巧见 [docs/FEATURES.md](docs/FEATURES.md)

---

## 技术架构

### 分层设计

| 层级 | 功能 | 性能 |
|------|------|------|
| Layer 1 | 静态构图辅助线叠加 | < 1ms |
| Layer 2 | 每 2.5s ML Kit 检测 + 构图推荐 | < 200ms/次 |
| Layer 3 | 主体追踪 + 关键点对齐提示 | 随 Layer 2 |
| Layer 4 | 匹配度评分 + 方向引导 | 随 Layer 2 |

### 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin 1.9.20+ | 主语言 |
| Jetpack Compose | UI 框架 (Material Design 3) |
| CameraX 1.3.1 | 相机 (Preview + ImageAnalysis + ImageCapture) |
| ML Kit | 物体检测 (STREAM_MODE) |
| Navigation Compose | 页面导航 |
| Accompanist | 运行时权限请求 |

### 项目结构

```
com.example.compositionhelper/
├── MainActivity.kt                 # 入口 + Navigation + 沉浸式
├── CompositionHelperApp.kt         # 相册分析模式 UI
├── ImageAnalyzer.kt                # 全量图像分析（相册模式）
├── model/
│   ├── CompositionModels.kt        # 19 种构图枚举 + 3 分类
│   └── AnalysisModels.kt           # 检测/分析数据类
├── overlay/
│   ├── CompositionDrawing.kt       # CompositionRenderer 抽象 + 绘制函数
│   ├── CameraCompositionOverlay.kt # Compose Canvas 实时叠加层
│   └── CompositionOverlay.kt       # Bitmap 叠加（相册模式）
├── camera/
│   ├── CameraManager.kt           # CameraX 生命周期管理
│   ├── FrameAnalyzer.kt           # 帧分析 + 轻量推荐算法
│   └── CameraCompositionScreen.kt # 全屏相机 UI
└── ui/
    ├── components/
    │   ├── ControlPanel.kt         # 底部控制栏 + 设置面板
    │   ├── ShutterButton.kt        # 拍照按钮
    │   └── RecommendationChip.kt   # AI 推荐浮层
    └── theme/
        └── Theme.kt                # 主题 + 透明系统栏
```

### 绘制代码复用

`CompositionRenderer` 接口抽象绘制逻辑，同一套绘制代码服务于两种模式：
- `CanvasRenderer` — android.graphics.Canvas（Bitmap 叠加，相册模式）
- `DrawScopeRenderer` — Compose DrawScope（实时相机叠加）

---

## 权限说明

| 权限 | 用途 |
|------|------|
| `CAMERA` | 实时预览和拍照 |
| `READ_EXTERNAL_STORAGE` | 访问相册 (Android 12-) |
| `READ_MEDIA_IMAGES` | 读取图片 (Android 13+) |

---

## 常见问题

**Q: Gradle 同步失败?**
A: 确保 JDK 17 已安装。运行 `./gradlew clean && ./gradlew --refresh-dependencies`

**Q: 相机预览黑屏?**
A: 模拟器需要启用相机模拟，推荐真机测试。

**Q: ML Kit 分析不工作?**
A: 确保设备已安装 Google Play 服务并已授予相机权限。

更多问题见 [docs/ANDROID.md](docs/ANDROID.md)

---

## 详细文档

- [Android 安装和配置指南](docs/ANDROID.md)
- [19 种构图类型详解](docs/FEATURES.md)

---

## 贡献

```bash
git checkout -b feature/your-feature
git commit -m 'Add your feature'
git push origin feature/your-feature
# 创建 Pull Request
```

---

## 许可证

MIT License - 详见 [LICENSE](LICENSE)

---

**项目主页**: [GitHub](https://github.com/MinJung-Go/CompositionHelper) | **问题反馈**: [Issues](https://github.com/MinJung-Go/CompositionHelper/issues) | **iOS 版本**: [ios 分支](https://github.com/MinJung-Go/CompositionHelper/tree/ios)
