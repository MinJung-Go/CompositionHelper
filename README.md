# 📸 CompositionHelper - iOS 构图辅助应用

> 智能摄影构图辅助工具，支持自动构图分析和多种经典构图类型

[![iOS Version](https://img.shields.io/badge/iOS-15.0%2B-blue)](https://developer.apple.com/ios/)
[![Swift](https://img.shields.io/badge/Swift-5.0+-orange.svg)](https://swift.org)
[![SwiftUI](https://img.shields.io/badge/SwiftUI-UI%20Framework-red.svg)](https://developer.apple.com/xcode/swiftui/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## ✨ 功能特性

### 🎯 核心功能

- **7种经典构图类型**
  - 三分法（九宫格）
  - 中心构图
  - 对角线构图
  - 框架构图
  - 引导线
  - S形曲线
  - 黄金螺旋

- **智能构图分析**
  - 基于 Vision Framework 的自动分析
  - 检测主体位置、线条、对称性
  - 智能推荐最佳构图
  - 置信度评分系统

- **自定义辅助线**
  - 可调节透明度（0.1 - 1.0）
  - 5种预设颜色
  - 实时预览

- **便捷输入方式**
  - 相机实时拍摄
  - 相册照片选择
  - Live Photo 支持

### 🎨 UI特性

- 简洁现代的 SwiftUI 界面
- 流畅的动画和过渡效果
- 横向滚动的构图选择器
- 推荐标记（⭐显示）
- 响应式设计，适配所有iPhone尺寸

## 🚀 快速开始

### 前置要求

- macOS 12.0+ (Monterey)
- Xcode 14.0+
- iOS 15.0+ 设备（推荐真机测试）
- Apple Developer Account（可选，用于发布）

### 安装步骤

#### 方式一：直接使用 Xcode

1. **克隆或下载项目**
   ```bash
   git clone https://github.com/yourusername/CompositionHelper.git
   cd CompositionHelper
   ```

2. **创建 Xcode 项目**
   - 打开 Xcode
   - 选择 `File → New → Project → iOS → App`
   - 配置项目：
     - Product Name: `CompositionHelper`
     - Interface: `SwiftUI`
     - Language: `Swift`
     - Storage: `None`

3. **导入代码文件**
   - 将 `Sources/` 目录下的所有 `.swift` 文件拖入项目
   - 将 `Info.plist` 添加到项目中（替换默认的）

4. **配置权限**
   - 在 Info.plist 中确保已添加：
     - `NSCameraUsageDescription`
     - `NSPhotoLibraryUsageDescription`
     - `NSPhotoLibraryAddUsageDescription`

5. **运行项目**
   - 连接 iPhone
   - 选择设备
   - 按 `⌘R` 运行

#### 方式二：使用 Swift Package

```bash
# 克隆项目
git clone https://github.com/yourusername/CompositionHelper.git
cd CompositionHelper

# 在 Xcode 中打开 Package.swift
open Package.swift
```

## 📖 使用指南

### 基本操作

1. **选择照片**
   - 点击"相册"按钮从照片库选择
   - 或点击"拍照"按钮直接拍摄

2. **选择构图类型**
   - 滑动底部的构图选择器
   - 点击任意图案切换构图
   - 带⭐标记的为AI推荐

3. **自定义辅助线**
   - 拖动"辅助线透明度"滑块调整
   - 点击颜色圆点切换颜色

4. **自动分析**
   - 点击"自动分析构图"按钮
   - 等待分析完成（2-5秒）
   - 查看推荐构图

### 构图类型说明

| 构图类型 | 适用场景 | 效果 |
|---------|---------|------|
| 三分法 | 风景、人像 | 经典、平衡 |
| 中心构图 | 对称场景、特写 | 强调主体 |
| 对角线 | 动态场景 | 增强张力 |
| 框架构图 | 门框、窗户 | 突出主体 |
| 引导线 | 道路、河流 | 引导视线 |
| S形曲线 | 河流、道路 | 柔美流畅 |
| 黄金螺旋 | 艺术创作 | 自然美感 |

## 🛠 技术栈

- **UI 框架**: SwiftUI
- **图像分析**: Vision Framework
- **图像处理**: Core Image
- **相机功能**: AVFoundation
- **最低版本**: iOS 15.0+
- **语言**: Swift 5.0+

## 📁 项目结构

```
CompositionHelper/
├── Sources/
│   ├── CompositionHelperApp.swift      # App 入口
│   ├── Views/
│   │   └── CompositionHelper.swift     # 主视图
│   ├── Analyzers/
│   │   └── CompositionAnalyzer.swift   # 图像分析器
│   ├── Models/                          # 数据模型（待开发）
│   └── Utils/                           # 工具类（待开发）
├── Info.plist                          # 应用配置
├── Package.swift                        # Swift Package 配置
├── README.md                           # 项目说明
├── QUICKSTART.md                       # 快速开始
├── PROJECT_SETUP.md                    # 项目设置
├── FEATURES.md                         # 功能详解
├── PROJECT_SUMMARY.md                  # 项目总结
└── .gitignore                          # Git 忽略规则
```

## 🎯 功能规划

### v1.0 (当前) - MVP
- [x] 基础UI框架
- [x] 7种构图类型
- [x] 自动分析（简化版）
- [x] 相机和相册支持
- [x] 自定义辅助线

### v2.0 (进阶)
- [ ] 照片裁剪
- [ ] 构图对比模式
- [ ] Core ML 增强
- [ ] 构图评分可视化
- [ ] 导出功能

### v3.0 (高级)
- [ ] AR 实时取景
- [ ] AI 重构图图
- [ ] 社区分享
- [ ] 大师案例库
- [ ] 学习模式

## 🐛 常见问题

### Q: 相机无法打开？
**A:** 确保：
1. 已在 Info.plist 中添加相机权限描述
2. 在真机上测试（模拟器不支持相机）
3. 检查设置 → 隐私 → 相机

### Q: 分析速度慢？
**A:** 大尺寸图片分析会较慢，建议：
1. 压缩图片到 1920x1920 以下
2. 关闭其他后台应用
3. 使用性能更好的设备

### Q: 自动分析不准确？
**A:** 这是正常现象，Vision Framework 在复杂场景下精度有限。可以：
1. 尝试手动选择构图类型
2. 使用更清晰的图片
3. 未来版本会集成 Core ML 提升准确率

## 📸 截图演示

![截图1](https://via.placeholder.com/300x600?text=主界面)
![截图2](https://via.placeholder.com/300x600?text=构图选择)
![截图3](https://via.placeholder.com/300x600?text=自动分析)

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 如何贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 遵循 Swift 官方代码风格
- 添加适当的注释
- 编写单元测试
- 更新相关文档

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 👨‍💻 作者

- 项目创建者 - [Your Name]
- Twitter - [@yourusername](https://twitter.com/yourusername)

## 🙏 致谢

- Apple Vision Framework
- SwiftUI 社区
- 所有贡献者

## 📞 联系方式

- 项目主页: [https://github.com/yourusername/CompositionHelper](https://github.com/yourusername/CompositionHelper)
- 问题反馈: [GitHub Issues](https://github.com/yourusername/CompositionHelper/issues)
- 邮箱: your.email@example.com

## 📚 相关资源

- [Vision Framework 官方文档](https://developer.apple.com/documentation/vision)
- [SwiftUI 教程](https://developer.apple.com/tutorials/swiftui)
- [摄影构图指南](https://www.photographymad.com/pages/view/10-top-photography-composition-rules)

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**

**Made with ❤️ by Your Name**
