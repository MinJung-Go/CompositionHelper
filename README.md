# CompositionHelper iOS

> iOS 版本 - 智能摄影构图辅助工具（实时相机 + AI 推荐）

[![iOS Version](https://img.shields.io/badge/iOS-15.0%2B-brightgreen)](https://developer.apple.com/ios/)
[![Swift](https://img.shields.io/badge/Swift-5.9+-orange.svg)](https://swift.org)
[![SwiftUI](https://img.shields.io/badge/SwiftUI-4.0+-blue.svg)](https://developer.apple.com/xcode/swiftui/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **这是 CompositionHelper 的 iOS 版本分支（ios）。查看 [Android 版本](https://github.com/MinJung-Go/CompositionHelper/tree/master) 了解 Android 版本。**

---

## 核心功能

- **实时相机构图引导** - 全屏沉浸式相机预览 + 实时构图辅助线叠加
- **AI 智能推荐** - Vision Framework 实时帧分析，每 2.5 秒自动推荐最佳构图
- **19 种构图类型** - 经典(7) + 现代(7) + 视角(5) 三大分类
- **主体追踪** - 实时检测画面主体，对齐关键点时高亮提示
- **相册分析模式** - 选择照片进行全量构图分析
- **自定义辅助线** - 透明度、颜色可调

## 双模式架构

```
┌──────────────────────────────┐
│  实时相机模式（默认启动）      │
│  ┌──────────────────────┐    │
│  │ AVCaptureVideoPreview│    │
│  │ + 构图 overlay        │    │
│  │ + 主体追踪框          │    │
│  │ + AI 推荐浮层         │    │
│  └──────────────────────┘    │
│  [手动/智能] [构图选择] [拍照] │
├──────────────────────────────┤
│  相册分析模式                 │
│  选照片 → Vision 全量分析     │
│  → 推荐构图 + 置信度排序      │
└──────────────────────────────┘
```

## 快速开始

```bash
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
open CompositionHelper.xcodeproj
```

1. 在 Xcode 中选择目标设备（推荐 iPhone 14 Pro+）
2. 按 `Command + R` 运行

> 实时相机功能需要在真机上测试，模拟器仅支持相册分析模式。

## 技术栈

| 技术 | 用途 |
|------|------|
| Swift 5.9+ | 开发语言 |
| SwiftUI | UI 框架 |
| AVFoundation | 实时相机预览与拍照 |
| Vision | 人脸/矩形检测、实时帧分析 |
| Core Image | 图像处理 |
| PhotosUI | PHPicker 相册选择 |

**无第三方依赖**，全部使用系统框架。

## 19 种构图类型

### 经典构图 (7)
三分法 | 中心构图 | 对角线 | 框架构图 | 引导线 | S形曲线 | 黄金螺旋

### 现代构图 (7)
黄金三角 | 对称构图 | 负空间 | 模式重复 | 隧道式 | 分割构图 | 透视焦点

### 视角构图 (5)
隐形线 | 充满画面 | 低角度 | 高角度 | 深度层次

详见 [功能详解](docs/FEATURES.md)

## 项目结构

```
Sources/
├── CompositionHelperApp.swift          # App 入口 → ContentView
├── Models/
│   ├── CompositionType.swift           # 19 种构图 + 3 分类枚举
│   └── AnalysisModels.swift            # 分析数据模型
├── Views/
│   ├── ContentView.swift               # 导航入口（默认相机模式）
│   ├── CompositionHelper.swift         # 相册分析模式 UI
│   └── Overlays/
│       └── CompositionOverlays.swift   # 19 种构图 overlay
├── Camera/
│   ├── CameraManager.swift             # AVCaptureSession 管理
│   ├── FrameAnalyzer.swift             # 实时帧分析 + LightweightAnalyzer
│   └── CameraCompositionView.swift     # 全屏相机 UI + 主体追踪
├── Components/
│   ├── ControlPanel.swift              # 底部构图选择面板
│   ├── ShutterButton.swift             # 快门按钮
│   └── RecommendationChip.swift        # AI 推荐浮层
└── Analyzers/
    └── CompositionAnalyzer.swift       # 全量静态分析（Vision）
```

## 实时分析流程

```
AVCaptureVideoDataOutput
        │ (每 2.5 秒抽帧)
        ▼
   FrameAnalyzer
   ├── VNDetectFaceRectanglesRequest
   └── VNDetectRectanglesRequest
        │
        ▼
   LightweightAnalyzer
   ├── 主体位置 → 最佳构图匹配
   ├── 方向引导提示
   └── 对齐检测
        │
        ▼
   UI 更新
   ├── 推荐构图浮层
   ├── 主体追踪框
   └── 对齐高亮（绿/黄/青）
```

## 权限

| 权限 | 用途 |
|------|------|
| `NSCameraUsageDescription` | 实时相机预览和拍照 |
| `NSPhotoLibraryUsageDescription` | 相册模式选择照片 |
| `NSPhotoLibraryAddUsageDescription` | 保存拍摄的照片 |

## 详细文档

- [iOS 详细安装和配置指南](docs/IOS.md)
- [功能详解 - 19 种构图类型](docs/FEATURES.md)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

**其他平台**: [Android 版本](https://github.com/MinJung-Go/CompositionHelper/tree/master)
