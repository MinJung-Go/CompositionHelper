# 🍎 CompositionHelper iOS

> iOS 版本 - 智能摄影构图辅助工具

[![iOS Version](https://img.shields.io/badge/iOS-15.0%2B-brightgreen)](https://developer.apple.com/ios/)
[![Swift](https://img.shields.io/badge/Swift-5.9+-orange.svg)](https://swift.org)
[![SwiftUI](https://img.shields.io/badge/SwiftUI-4.0+-blue.svg)](https://developer.apple.com/xcode/swiftui/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/CI-Passing-success)](https://github.com/MinJung-Go/CompositionHelper/actions)

---

> **🍎 这是 CompositionHelper 的 iOS 版本分支（ios）。查看 [主项目 README](https://github.com/MinJung-Go/CompositionHelper) 了解 Android 版本和完整功能说明。**

---

## ✨ 快速开始

### 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| macOS | 12.0 (Monterey) | 13.0+ |
| Xcode | 14.0 | 15.0+ |
| iOS 部署目标 | 15.0 | 17.0+ |
| Swift | 5.0 | 5.9+ |

### 安装

```bash
# 克隆 ios 分支（iOS 版本）
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper

# 使用 Xcode 打开
open CompositionHelper.xcodeproj
# 或
xed .
```

### 运行

**模拟器**:
1. 在 Xcode 中选择目标设备（推荐 iOS 15+）
2. 点击 ▶️ 或按 `Command + R`

**真机**:
1. 用 USB 线连接设备
2. 在 Xcode 中选择你的设备
3. 点击 ▶️ 运行（首次需要信任开发者）

---

## 🛠 技术栈

- **语言**: Swift 5.9+
- **UI 框架**: SwiftUI 4.0+
- **最低 iOS**: iOS 15.0
- **目标 iOS**: iOS 17.0+
- **图像分析**: Vision Framework
- **Live Photo**: PHPickerViewController

---

## 📚 详细文档

- [iOS 详细安装和配置指南](docs/IOS.md)
- [功能详解 - 18种构图类型](docs/FEATURES.md)
- [主项目 README](https://github.com/MinJung-Go/CompositionHelper)

---

## ✨ 核心功能

- **18 种构图类型**（7 经典 + 11 现代）
- **🤖 智能构图分析** - 基于 Vision Framework
- **🎨 自定义辅助线** - 透明度和颜色可调
- **📷 多样化输入** - 相机和相册（支持 Live Photo）
- **🎨 现代 SwiftUI UI** - iOS 原生设计

---

## 📖 使用指南

### 基本操作

1. **选择照片** - 从相册选择或直接拍摄（支持 Live Photo）
2. **选择构图类型** - 滑动底部选择器切换构图
3. **自动分析** - 点击按钮获取 AI 推荐（⭐ 标记）
4. **自定义辅助线** - 调整透明度和颜色

### 权限说明

应用需要以下权限：

| 权限 | 用途 |
|------|------|
| `NSPhotoLibraryUsageDescription` | 访问相册 |
| `NSCameraUsageDescription` | 拍摄照片 |

---

## 🐛 常见问题

### Q: Xcode 编译失败？
**A:** 运行 `Command + Shift + K` 清理构建，然后重新编译

### Q: 找不到连接的设备？
**A:** 确保设备已信任电脑，USB 线连接正常

### Q: Vision Framework 分析失败？
**A:** 确保权限已授予，照片格式支持

更多问题请查看 [iOS 详细文档](docs/IOS.md)

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

```bash
# 1. Fork 本仓库
# 2. 创建特性分支
git checkout -b feature/AmazingFeature

# 3. 提交更改
git commit -m 'Add some AmazingFeature'

# 4. 推送到分支
git push origin feature/AmazingFeature

# 5. 创建 Pull Request
```

---

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

## 🙏 致谢

- [Apple Vision Framework](https://developer.apple.com/documentation/vision)
- [SwiftUI](https://developer.apple.com/xcode/swiftui/)
- [Swift Package Manager](https://swift.org/package-manager/)
- 所有贡献者和支持者

---

## 📞 联系方式

- **项目主页**: [https://github.com/MinJung-Go/CompositionHelper](https://github.com/MinJung-Go/CompositionHelper)
- **问题反馈**: [GitHub Issues](https://github.com/MinJung-Go/CompositionHelper/issues)

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**

**Made with ❤️ using SwiftUI and Swift**

---

**其他平台**: [🤖 Android 版本](https://github.com/MinJung-Go/CompositionHelper/tree/master)
