# 📱 CompositionHelper Android

> 智能摄影构图辅助工具 - Android 版本
>
> 18 种构图类型 + 智能分析 + Jetpack Compose UI

[![Android Version](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com/studio)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/CI-Passing-success)](https://github.com/MinJung-Go/CompositionHelper/actions)

---

## ✨ 功能特性

### 🎯 核心功能

- **18 种构图类型**（7 经典 + 11 现代）

**经典构图（7种）**
- ✅ 三分法（九宫格）
- ✅ 中心构图
- ✅ 对角线构图
- ✅ 框架构图
- ✅ 引导线
- ✅ S形曲线
- ✅ 黄金螺旋

**现代热门构图（11种）**
- ✅ 黄金三角 - 黄金比例三角形构图
- ✅ 对称构图 - 左右/上下完全对称
- ✅ 负空间 - 大面积留白突出主体
- ✅ 模式重复 - 重复元素创造节奏感
- ✅ 隧道式 - 向深处延伸的视觉效果
- ✅ 分割构图 - 多种分割方式组合
- ✅ 透视焦点 - 消失点透视法
- ✅ 隐形线 - 引导视线的流动线条
- ✅ 充满画面 - 主体填充85%以上画面
- ✅ 低角度 - 从下往上的仰视视角
- ✅ 高角度 - 从上往下的俯视视角

- **🤖 智能构图分析**
  - ML Kit 图像分析引擎
  - 主体检测与识别
  - 智能推荐最佳构图
  - 置信度评分系统

- **🎨 自定义辅助线**
  - 透明度调节（0.1 - 1.0）
  - 7 种预设颜色
  - 实时预览效果

- **📷 多样化输入**
  - 相机实时拍摄
  - 相册照片选择
  - 支持 JPEG、PNG 格式

### 🎨 UI/UX 特性

- 🎨 现代化 Jetpack Compose UI
- 📱 Material Design 3 设计语言
- 🔄 流畅的动画和过渡效果
- 📊 构图分类智能筛选
- 🌙 自动深色模式支持
- ⭐ AI 推荐标记显示
- 📱 响应式设计，适配各种屏幕尺寸

---

## 🚀 快速开始

### 📋 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Flamingo | Jellyfish 或更高 |
| JDK | 11 | 17 |
| Android SDK | API 24 | API 34 |
| Gradle | 8.0 | 8.5+ |

### 🛠️ 安装步骤

#### 方式一：克隆并直接打开

```bash
# 克隆主分支（Android 版本）
git clone https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper

# 使用 Android Studio 打开
# 打开 Android Studio → Open → 选择 CompositionHelper 目录
```

#### 方式二：使用 Git 切换分支

如果你已经克隆了仓库：

```bash
git fetch origin
git checkout master
git pull origin master
```

### ▶️ 运行到设备

#### 模拟器

1. 在 Android Studio 中打开 AVD Manager（Tools → Device Manager）
2. 创建或选择一个模拟器（推荐 API 29+）
3. 点击 ▶️ 运行按钮或按 `Shift + F10`

#### 真机

1. **启用开发者选项**
   ```
   设置 → 关于手机 → 连续点击"版本号" 7 次
   ```

2. **启用 USB 调试**
   ```
   设置 → 开发者选项 → USB 调试 ✓
   ```

3. **连接并运行**
   - 用 USB 线连接设备
   - 在设备上授权 USB 调试
   - 在 Android Studio 中选择设备并运行

---

## 🛠 技术栈

### 核心技术

```yaml
语言: Kotlin 1.9.20+
UI 框架: Jetpack Compose
最低 SDK: API 24 (Android 7.0)
目标 SDK: API 34 (Android 14)
```

### 主要依赖

#### Jetpack 组件
- `androidx.compose:ui` - Compose UI 基础
- `androidx.compose:material3` - Material Design 3
- `androidx.compose.animation` - 动画支持
- `androidx.navigation:navigation-compose` - 导航
- `androidx.lifecycle:lifecycle-*` - 生命周期管理
- `androidx.camera:camera-*` - CameraX 相机库

#### ML Kit
- `com.google.mlkit:object-detection` - 物体检测
- `com.google.mlkit:pose-detection` - 姿态检测
- `com.google.mlkit:segmentation-selfie` - 人像分割

#### 第三方库
- `com.google.accompanist:accompanist-permissions` - 权限请求
- `io.coil-kt:coil-compose` - 图片加载（Compose 版本）

---

## 📁 项目结构

```
CompositionHelper/
├── app/
│   └── src/
│       └── master/
│           ├── java/com/example/compositionhelper/
│           │   ├── MainActivity.kt                    # 主 Activity
│           │   ├── CompositionHelperApp.kt            # 主应用入口
│           │   └── ui/
│           │       ├── composition/
│           │       │   └── CompositionOverlay.kt   # 构图绘制
│           │       ├── camera/
│           │       │   └── CameraScreen.kt          # 相机界面
│           │       ├── gallery/
│           │       │   └── GalleryScreen.kt        # 相册界面
│           │       └── theme/
│           │           └── Theme.kt                 # 主题配置
│           ├── res/
│           │   ├── values/
│           │   │   └── strings.xml                 # 字符串资源
│           │   └── drawable/                        # 图片资源
│           └── AndroidManifest.xml                  # 应用清单
├── build.gradle.kts                                 # 项目级 Gradle
├── app/build.gradle.kts                             # 应用级 Gradle
├── settings.gradle.kts                               # Gradle 设置
├── gradle.properties                                 # Gradle 配置
├── gradlew                                          # Gradle Wrapper (Unix)
├── gradlew.bat                                       # Gradle Wrapper (Windows)
└── README_ANDROID.md                                 # 本文档
```

---

## 🎯 构图类型详解

### 经典构图（7种）

| 构图类型 | 适用场景 | 效果描述 |
|---------|---------|---------|
| 📐 三分法 | 风景、人像 | 经典平衡，主体位于九宫格交点 |
| 🎯 中心构图 | 对称场景、特写 | 强调主体，居中对称 |
| ➡️ 对角线 | 运动场景 | 增强动感，引导视线流动 |
| 🖼️ 框架构图 | 门框、窗户 | 突出主体，创造层次感 |
| 🛤️ 引导线 | 道路、河流 | 引导视线，增强深度 |
| 〰️ S形曲线 | 河流、道路 | 柔美流畅，自然美感 |
| 🌀 黄金螺旋 | 艺术创作 | 自然比例，黄金分割 |

### 现代热门构图（11种）

| 构图类型 | 适用场景 | 效果描述 |
|---------|---------|---------|
| 🔺 黄金三角 | 建筑、产品 | 黄金比例三角形 |
| 🔄 对称构图 | 对称建筑 | 平衡有序，稳定感 |
| ⬜ 负空间 | 极简摄影 | 突出主体，留白艺术 |
| 🔁 模式重复 | 建筑、纹理 | 节奏感，视觉韵律 |
| 🚇 隧道式 | 桥洞、走廊 | 深度感，透视效果 |
| 📊 分割构图 | 多主题 | 对比效果，分割画面 |
| 🔍 透视焦点 | 道路、建筑 | 空间感，消失点 |
| ➖ 隐形线 | 人物视线 | 引导流动，情感连接 |
| 🔳 充满画面 | 微距、纹理 | 强调细节，视觉冲击 |
| ⬆️ 低角度 | 建筑仰视 | 夸张透视，庄严感 |
| ⬇️ 高角度 | 风景俯视 | 全局视角，纵深感 |

---

## 🔧 构建配置

### Build Types

| 类型 | 说明 | 用途 |
|------|------|------|
| Debug | 调试版本，未签名 | 开发调试 |
| Release | 发布版本，已签名 | 正式发布 |

### 构建命令

```bash
# 清理构建缓存
./gradlew clean

# Debug 构建
./gradlew assembleDebug

# Release 构建（需要签名配置）
./gradlew assembleRelease

# 安装到连接的设备
./gradlew installDebug

# 卸载应用
./gradlew uninstallDebug
```

### APK 输出位置

```bash
# Debug APK
app/build/outputs/apk/debug/app-debug.apk

# Release APK
app/build/outputs/apk/release/app-release.apk
```

---

## 📖 使用指南

### 基本操作

#### 1️⃣ 选择构图类型
- 滑动底部的构图网格浏览
- 使用分类标签快速筛选（经典/现代/视角）
- 点击构图按钮即时切换

#### 2️⃣ 选择照片
```kotlin
// 方式 A: 从相册选择
点击"相册"按钮 → 浏览图库 → 选择照片

// 方式 B: 直接拍摄
点击"拍照"按钮 → 对准拍摄 → 自动分析
```

#### 3️⃣ 自动分析
```kotlin
// 触发 AI 分析
点击"自动分析构图"按钮
→ 等待分析完成（2-5秒）
→ 查看推荐构图（⭐ 标记）
```

#### 4️⃣ 自定义辅助线
```
透明度: 拖动滑块（0.1 - 1.0）
颜色: 点击预设颜色圆点切换
效果: 实时预览
```

### 📸 功能使用流程图

```
┌─────────────┐
│  启动应用   │
└──────┬──────┘
       │
       ▼
┌─────────────┐      ┌─────────────┐
│ 选择构图    │◀────►│ 自定义样式  │
└──────┬──────┘      └─────────────┘
       │
       ├─────────┬─────────┐
       │         │         │
       ▼         ▼         ▼
   ┌───────┐ ┌───────┐ ┌───────┐
   │ 相册  │ │ 拍照  │ │ 分析  │
   └───────┘ └───────┘ └───────┘
       │         │         │
       └─────────┴─────────┘
                 │
                 ▼
         ┌───────────────┐
         │  查看构图结果 │
         └───────────────┘
```

---

## 🔐 权限说明

### 必需权限

| 权限 | 用途 | 请求时机 |
|------|------|---------|
| `CAMERA` | 拍摄照片 | 首次点击"拍照" |
| `READ_EXTERNAL_STORAGE` | 访问相册 | 首次点击"相册" |
| `READ_MEDIA_IMAGES` | 读取图片 (Android 13+) | 首次访问相册 |

### 权限配置

在 `app/src/main/AndroidManifest.xml` 中：

```xml
<!-- 相机权限 -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- 相机功能声明 -->
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
<uses-feature
    android:name="android.hardware.camera.autofocus"
    android:required="false" />
```

---

## 🐛 常见问题

### ❓ Gradle 同步失败

**可能原因:**
- Gradle 版本不兼容
- 网络连接问题
- 依赖下载失败

**解决方案:**
```bash
# 1. 检查 Gradle 版本
cat gradle/wrapper/gradle-wrapper.properties

# 2. 清理并重新同步
./gradlew clean
./gradlew --refresh-dependencies

# 3. 如果在中国，配置镜像源
# 在 gradle.properties 中添加：
# systemProp.https.proxyHost=your-proxy
# systemProp.https.proxyPort=port
```

### ❓ 找不到连接的设备

**可能原因:**
- USB 调试未启用
- 驱动问题
- USB 连接问题

**解决方案:**
```bash
# 1. 检查设备连接
adb devices

# 2. 如果没有设备，尝试:
# - 重新插拔 USB 线
# - 切换 USB 模式（文件传输/充电）
# - 重启 ADB
adb kill-server
adb start-server

# 3. 确认开发者选项已启用
# 设置 → 开发者选项 → USB 调试 ✓
```

### ❓ 构建失败

**可能原因:**
- 依赖冲突
- SDK 版本不兼容
- 缓存问题

**解决方案:**
```bash
# 1. 清理所有缓存
./gradlew clean
rm -rf .gradle
rm -rf app/build
rm -rf ~/.gradle/caches/

# 2. 重新同步 Gradle
# Android Studio: File → Invalidate Caches / Restart

# 3. 检查 SDK 版本
# Tools → SDK Manager → 安装所需的 SDK 版本
```

### ❓ ML Kit 分析失败

**可能原因:**
- Google Play 服务未安装
- 权限未授予
- 网络问题

**解决方案:**
```kotlin
// 1. 检查 Google Play 服务
val playServiceAvailability =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)

// 2. 确保权限已授予
// 运行时请求权限：
// Settings → Apps → CompositionHelper → Permissions

// 3. 检查日志中的错误信息
adb logcat | grep MLKit
```

---

## 📊 性能优化

### 内存优化

```kotlin
// 1. 使用 Coil 的内存缓存
AsyncImage(
    model = imageUrl,
    contentDescription = null,
    modifier = Modifier.size(200.dp)
)

// 2. 及时释放位图
bitmap?.recycle()
bitmap = null

// 3. 避免同时加载多张大图
// 使用 LazyColumn 或 LazyVerticalGrid
```

### 渲染优化

```kotlin
// 1. 使用 remember 避免不必要的重组
@Composable
fun MyComposable() {
    val value = remember { expensiveComputation() }
}

// 2. 使用 key 稳定重组
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item)
    }
}

// 3. 使用 derivedStateOf 优化派生状态
val filteredItems by remember {
    derivedStateOf {
        items.filter { it.type == selectedType }
    }
}
```

---

## 🚀 发布准备

### 签名配置

#### 1. 创建密钥库

```bash
keytool -genkey \
    -v \
    -keystore release.keystore \
    -alias compositionhelper \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
```

#### 2. 配置签名

在 `app/build.gradle.kts` 中：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "compositionhelper"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### 3. 构建签名的 APK

```bash
./gradlew assembleRelease

# APK 位置:
# app/build/outputs/apk/release/app-release.apk
```

### Google Play 发布流程

1. **创建开发者账号**
   - 访问 [Google Play Console](https://play.google.com/console)
   - 注册并支付 $25 一次性费用

2. **创建应用**
   - 填写应用信息
   - 上传应用图标和截图
   - 配置商店列表

3. **上传 APK**
   - 进入"生产"或"测试"轨道
   - 上传签名的 APK 或 AAB（推荐 AAB）

4. **填写内容分级**
   - 完成内容分级问卷

5. **提交审核**
   - 审核通常需要 1-3 天
   - 审核通过后自动发布

---

## 📚 参考文档

### 官方文档

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 UI 框架
- [ML Kit](https://developers.google.com/ml-kit) - 机器学习套件
- [CameraX](https://developer.android.com/training/camerax) - 相机库
- [Material Design 3](https://m3.material.io/) - 设计系统
- [Kotlin 官方文档](https://kotlinlang.org/docs/) - Kotlin 语言
- [Gradle 用户手册](https://docs.gradle.org/current/userguide/userguide.html) - 构建工具

### 学习资源

- [Android Developers](https://developer.android.com/) - Android 开发官方资源
- [Kotlin for Android](https://developer.android.com/kotlin) - Kotlin Android 开发
- [Compose 教程](https://developer.android.com/codelabs/jetpack-compose-basics) - Compose 入门教程
- [相机最佳实践](https://developer.android.com/training/camera/cameradependencies) - 相机开发指南

---

## 🤝 贡献指南

我们欢迎任何形式的贡献！

### 如何贡献

```bash
# 1. Fork 本仓库
# 点击 GitHub 页面右上角的 Fork 按钮

# 2. 克隆你的 Fork
git clone https://github.com/YOUR_USERNAME/CompositionHelper.git
cd CompositionHelper

# 3. 创建特性分支
git checkout -b feature/AmazingFeature

# 4. 提交更改
git add .
git commit -m "Add some AmazingFeature"

# 5. 推送到分支
git push origin feature/AmazingFeature

# 6. 创建 Pull Request
# 在 GitHub 上创建 PR，描述你的更改
```

### 代码规范

- 遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 遵循 [Android 编码规范](https://developer.android.com/kotlin/style-guide)
- 添加适当的注释和文档
- 为新功能编写单元测试
- 确保所有测试通过：`./gradlew test`

### 提交信息规范

使用清晰的提交信息：

```
feat: 添加新的构图类型
fix: 修复相机权限问题
docs: 更新 README 文档
style: 代码格式调整
refactor: 重构代码结构
test: 添加单元测试
chore: 更新依赖版本
```

---

## 🔄 更新日志

### v1.0.0 (2026-02-23)
- ✅ 18 种构图类型实现
- ✅ 智能构图分析（ML Kit）
- ✅ 相机和相册支持
- ✅ 自定义辅助线
- ✅ Material Design 3 UI
- ✅ CI/CD 集成
- ✅ 基础单元测试

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

```
MIT License

Copyright (c) 2026 MinJung-Go

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 致谢

- [Google ML Kit](https://developers.google.com/ml-kit) - 图像分析引擎
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 UI 框架
- [Material Design 3](https://m3.material.io/) - 设计系统
- 所有贡献者和支持者

---

## 📞 联系方式

- **项目主页**: [https://github.com/MinJung-Go/CompositionHelper](https://github.com/MinJung-Go/CompositionHelper)
- **问题反馈**: [GitHub Issues](https://github.com/MinJung-Go/CompositionHelper/issues)
- **讨论区**: [GitHub Discussions](https://github.com/MinJung-Go/CompositionHelper/discussions)

---

## 🌟 相关项目

- [iOS 版本](https://github.com/MinJung-Go/CompositionHelper/tree/ios) - SwiftUI 实现
- [Desktop 版本](#) - 计划中...

---

**⭐ 如果这个项目对你有帮助，请给个 Star！**

**Made with ❤️ using Jetpack Compose and Kotlin**

---

**最后更新**: 2026-02-23
**当前版本**: 1.0.0
**分支**: master (Android 主版本)
**iOS 分支**: ios
