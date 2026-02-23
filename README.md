# 🤖 CompositionHelper Android

> Android 版本 - 智能摄影构图辅助工具

[![Android Version](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com/studio)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/CI-Passing-success)](https://github.com/MinJung-Go/CompositionHelper/actions)

---

> **📱 这是 CompositionHelper 的 Android 版本分支（master）。查看 [iOS README](https://github.com/MinJung-Go/CompositionHelper/tree/ios) 了解 iOS 版本和完整功能说明。**

---

## ✨ 快速开始

### 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Flamingo | Jellyfish 或更高 |
| JDK | 11 | 17 |
| Android SDK | API 24 | API 34 |
| Gradle | 8.0 | 8.5+ |

### 安装

```bash
# 克隆 master 分支（Android 版本）
git clone -b master https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper

# 使用 Android Studio 打开
# 打开 Android Studio → Open → 选择 CompositionHelper 目录
```

### 运行

**模拟器**:
1. 在 Android Studio 中打开 AVD Manager（Tools → Device Manager）
2. 创建或选择一个模拟器（推荐 API 29+）
3. 点击 ▶️ 或按 `Shift + F10`

**真机**:
1. 启用开发者选项和 USB 调试
2. 用 USB 线连接设备
3. 在 Android Studio 中选择设备并运行

---

## 🛠 技术栈

- **语言**: Kotlin 1.9.20+
- **UI 框架**: Jetpack Compose
- **设计系统**: Material Design 3
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)
- **图像分析**: ML Kit

---

## 📚 详细文档

- [Android 详细安装和配置指南](docs/ANDROID.md)
- [功能详解 - 18种构图类型](docs/FEATURES.md)
- [主项目 README](https://github.com/MinJung-Go/CompositionHelper)

---

## ✨ 核心功能

- **18 种构图类型**（7 经典 + 11 现代）
- **🤖 智能构图分析** - 基于 ML Kit
- **🎨 自定义辅助线** - 透明度和颜色可调
- **📷 多样化输入** - 相机和相册
- **🎨 现代 Jetpack Compose UI** - Material Design 3

---

## 📖 使用指南

### 基本操作

1. **选择照片** - 从相册选择或直接拍摄
2. **选择构图类型** - 滑动底部选择器切换构图
3. **自动分析** - 点击按钮获取 AI 推荐（⭐ 标记）
4. **自定义辅助线** - 调整透明度和颜色

### 权限说明

应用需要以下权限：

| 权限 | 用途 |
|------|------|
| `CAMERA` | 拍摄照片 |
| `READ_EXTERNAL_STORAGE` | 访问相册 (Android 12 及以下) |
| `READ_MEDIA_IMAGES` | 读取图片 (Android 13+) |

---

## 🐛 常见问题

### Q: Gradle 同步失败？
**A:** 运行 `./gradlew clean && ./gradlew --refresh-dependencies`

### Q: 找不到连接的设备？
**A:** 运行 `adb devices` 检查设备连接

### Q: ML Kit 分析失败？
**A:** 确保 Google Play 服务已安装，权限已授予

更多问题请查看 [Android 详细文档](docs/ANDROID.md)

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

- [Google ML Kit](https://developers.google.com/ml-kit)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- 所有贡献者和支持者

---

## 📞 联系方式

- **项目主页**: [https://github.com/MinJung-Go/CompositionHelper](https://github.com/MinJung-Go/CompositionHelper)
- **问题反馈**: [GitHub Issues](https://github.com/MinJung-Go/CompositionHelper/issues)

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**

**Made with ❤️ using Jetpack Compose and Kotlin**

---

**其他平台**: [🍎 iOS 版本](https://github.com/MinJung-Go/CompositionHelper/tree/ios)
