# iOS 版本详细文档

> CompositionHelper iOS 版本的完整安装、构建和配置指南

## 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| macOS | 12.0 (Monterey) | 13.0+ |
| Xcode | 14.0 | 15.0+ |
| iOS 部署目标 | 15.0 | 17.0+ |
| Swift | 5.0 | 5.9+ |

## 安装步骤

### 方式一：克隆并直接打开

```bash
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
open CompositionHelper.xcodeproj
```

### 方式二：使用 Git 切换分支

```bash
git fetch origin
git checkout ios
git pull origin ios
```

## 运行到设备

### 模拟器

1. 在 Xcode 中选择目标设备（推荐 iPhone 14 Pro+）
2. 按 `Command + R` 运行
3. **注意**: 模拟器不支持实时相机，仅可测试相册分析模式

### 真机（推荐）

1. 用 USB 线连接设备
2. 在 Xcode → Preferences → Accounts 添加 Apple ID
3. 选择项目 → Target → Signing & Capabilities → 选择开发团队
4. 选择设备，按 `Command + R` 运行

## 技术架构

### 核心技术

```yaml
语言: Swift 5.9+
UI 框架: SwiftUI
最低版本: iOS 15.0
目标版本: iOS 17.0+
```

### 系统框架

| 框架 | 用途 |
|------|------|
| SwiftUI | 声明式 UI |
| AVFoundation | 实时相机预览、拍照、帧分析 |
| Vision | 人脸检测、矩形检测、物体分析 |
| Core Image | 图像处理 |
| PhotosUI | PHPicker 相册选择 |

无第三方依赖。

## 项目结构

```
Sources/
├── CompositionHelperApp.swift          # App 入口 → ContentView
├── Models/
│   ├── CompositionType.swift           # 19 种构图枚举 + 3 分类
│   └── AnalysisModels.swift            # 分析数据模型
├── Views/
│   ├── ContentView.swift               # 导航入口（默认相机模式）
│   ├── CompositionHelper.swift         # 相册分析模式 UI
│   └── Overlays/
│       └── CompositionOverlays.swift   # 19 种构图 overlay 绘制
├── Camera/
│   ├── CameraManager.swift             # AVCaptureSession 管理
│   │   - 视频输入（后置摄像头）
│   │   - 拍照输出（AVCapturePhotoOutput）
│   │   - 帧分析输出（AVCaptureVideoDataOutput）
│   │   - CameraPreviewView (UIViewRepresentable)
│   ├── FrameAnalyzer.swift             # 实时帧分析
│   │   - 2.5 秒节流
│   │   - Vision 人脸/矩形检测
│   │   - LightweightAnalyzer 构图匹配
│   └── CameraCompositionView.swift     # 全屏相机 UI
│       - 相机预览 + 构图叠加
│       - 主体追踪框（SubjectTrackingOverlay）
│       - 手动/智能模式切换
│       - 拍照和预览
├── Components/
│   ├── ControlPanel.swift              # 底部构图选择面板
│   ├── ShutterButton.swift             # 快门按钮
│   └── RecommendationChip.swift        # AI 推荐浮层
└── Analyzers/
    └── CompositionAnalyzer.swift       # 全量静态分析（相册模式）
```

## 实时分析流程

```
AVCaptureVideoDataOutput (每帧回调)
        │
        ▼ (2.5秒节流)
   FrameAnalyzer.captureOutput()
        │
   ┌────┴────┐
   │ Vision  │
   │ ├── VNDetectFaceRectanglesRequest
   │ └── VNDetectRectanglesRequest
   └────┬────┘
        │
        ▼
   LightweightAnalyzer.analyze()
   ├── 找最大主体
   ├── 检查三分点/中心/对角线距离
   ├── 检查充满画面/负空间
   └── 生成方向提示
        │
        ▼
   FrameAnalysisResult
   ├── recommendedType (最佳构图)
   ├── confidence (置信度)
   ├── detectedSubjects (检测到的主体)
   └── guidanceHint (方向提示)
        │
        ▼ (DispatchQueue.main.async)
   UI 更新
   ├── RecommendationChip (推荐浮层)
   ├── CompositionOverlayView (构图辅助线)
   └── SubjectTrackingOverlay (主体追踪)
       ├── 绿色 = 已对齐关键点
       ├── 黄色 = 接近关键点
       └── 青色 = 未对齐
```

## 权限说明

在 `Info.plist` 中添加：

```xml
<key>NSCameraUsageDescription</key>
<string>需要访问相机以进行实时构图引导</string>

<key>NSPhotoLibraryUsageDescription</key>
<string>需要访问相册以选择照片进行构图分析</string>

<key>NSPhotoLibraryAddUsageDescription</key>
<string>需要保存拍摄的照片到相册</string>
```

## 构建命令

```bash
# Debug 构建
xcodebuild -scheme CompositionHelper -configuration Debug build

# Release 构建
xcodebuild -scheme CompositionHelper -configuration Release build

# 清理
xcodebuild clean
```

## 常见问题

### 相机黑屏？
1. 确保在真机上运行（模拟器不支持实时相机）
2. 确认 Info.plist 中添加了 `NSCameraUsageDescription`
3. 检查 设置 → 隐私 → 相机 → CompositionHelper

### AI 推荐不显示？
1. 确认已切换到"智能"模式（点击右上角按钮）
2. 对准有明显主体的场景（人脸、建筑等）
3. 等待 2-3 秒让分析完成

### Xcode 编译错误？
1. `Command + Shift + K` 清理构建
2. `Command + B` 重新编译
3. 确保 Xcode 版本 >= 14.0

### 真机运行失败？
- 确保开发者账号已配置
- Bundle Identifier 唯一
- 设备版本 >= iOS 15.0
- 已信任开发者证书（设置 → 通用 → VPN与设备管理）

## 参考文档

- [SwiftUI](https://developer.apple.com/documentation/swiftui)
- [AVFoundation](https://developer.apple.com/documentation/avfoundation)
- [Vision Framework](https://developer.apple.com/documentation/vision)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)

---

**返回 [主 README](../README.md)**
