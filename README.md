# CompositionHelper - 智能摄影构图辅助工具

欢迎随时来撩! 公众号:民酱AIM

[![Android CI](https://github.com/MinJung-Go/CompositionHelper/actions/workflows/android-ci.yml/badge.svg)](https://github.com/MinJung-Go/CompositionHelper/actions/workflows/android-ci.yml)
[![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com/studio)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 前言

可能是年纪大了，小编最近迷上了摄影。但拍出来的照片总觉得差点意思——构图不对，画面不协调。于是闲来无事，撸了个小工具来帮忙。

**CompositionHelper** 是一款智能摄影构图辅助工具，支持 **Android** 和 **iOS** 双平台。简单说，就是帮你拍照时把构图搞定。

> iOS 版本见 [ios 分支](https://github.com/MinJung-Go/CompositionHelper/tree/ios)

---

## 功能亮点

| 功能 | 说明 |
|------|------|
| 实时构图引导 | 取景器里直接叠加辅助线，所见即所得 |
| AI 智能推荐 | ML Kit 物体检测，自动推荐最佳构图并给出方向提示 |
| 19 种构图类型 | 经典 / 现代 / 视角三大类，覆盖各类拍摄场景 |
| 主体追踪对齐 | 检测主体位置，对齐时实时高亮 |
| 相册分析模式 | 对已拍照片也能进行构图分析 |
| 自定义辅助线 | 透明度和颜色可调 |

---

## 技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 1.9.20+ |
| Jetpack Compose | Material Design 3 |
| CameraX | 1.3.1 |
| ML Kit | 物体检测 (STREAM_MODE) |

---

## 构图类型一览

小编把构图分成了三大类，共 19 种：

**经典 (7)**：三分法、中心构图、对角线、框架构图、引导线、S 形曲线、黄金螺旋

**现代 (7)**：黄金三角、对称构图、负空间、模式重复、隧道式、分割构图、透视焦点

**视角 (5)**：隐形线、充满画面、低角度、高角度、深度层次

---

## 快速上手

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

- **模拟器**: Tools > Device Manager > 创建/选择模拟器 (API 29+) > Run
- **真机**: 开启 USB 调试 > 连接设备 > Run

---

## 项目结构

```
com.example.compositionhelper/
├── camera/          # CameraX + 帧分析
├── overlay/         # 构图辅助线绘制
├── model/           # 数据模型
├── ui/              # Compose UI 组件
└── MainActivity.kt  # 入口
```

---

## 常见问题

**Q: Gradle 同步失败?**  
A: 确保 JDK 17 已安装。运行 `./gradlew clean && ./gradlew --refresh-dependencies`

**Q: 相机预览黑屏?**  
A: 模拟器需要启用相机模拟，推荐真机测试。

**Q: ML Kit 分析不工作?**  
A: 确保设备已安装 Google Play 服务并已授予相机权限。

---

## 写在最后

这个项目是小编学习 Compose 和 CameraX 的练手之作，代码写得比较随性，大佬们轻喷 🙏

有问题欢迎提 [Issue](https://github.com/MinJung-Go/CompositionHelper/issues)，也可以来公众号「民酱AIM」找我唠嗑。

---

## License

MIT License - 详见 [LICENSE](LICENSE)

---

项目主页: [GitHub](https://github.com/MinJung-Go/CompositionHelper) | 问题反馈: [Issues](https://github.com/MinJung-Go/CompositionHelper/issues) | iOS 版本: [ios 分支](https://github.com/MinJung-Go/CompositionHelper/tree/ios)
