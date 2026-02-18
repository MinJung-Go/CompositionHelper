# 📱 CompositionHelper Android APK

> 智能摄影构图辅助工具 - Android 版本
>
> 18 种构图类型 + 智能分析 + Jetpack Compose UI

## ✨ 功能特性

### 核心功能

- **18 种构图类型**（7经典 + 11现代）
  - 经典：三分法、中心构图、对角线、框架构图、引导线、S形曲线、黄金螺旋
  - 现代：黄金三角、对称构图、负空间、模式重复、隧道式、分割构图、透视焦点
  - 视角：隐形线、充满画面、低角度、高角度、深度层次

- **智能构图分析**
  - ML Kit 图像分析
  - 主体检测
  - 智能推荐构图
  - 置信度评分

- **自定义辅助线**
  - 透明度调节（0.1 - 1.0）
  - 7 种预设颜色
  - 实时预览

### 输入方式

- 📷 相机实时拍摄
- 🖼️ 相册照片选择
- ✨ 支持 JPEG、PNG 格式

### UI 特性

- 🎨 现代 Jetpack Compose UI
- 📱 Material Design 3
- 🔄 流畅的动画
- 📊 构图分类筛选
- 🌙 自动深色模式

---

## 🚀 快速开始

### 前置要求

- **Android Studio**: Flamingo 或更高版本
- **JDK**: 11 或更高版本
- **Android SDK**: API 24 (Android 7.0) 或更高
- **Gradle**: 8.0 或更高版本

### 克隆项目

```bash
git clone -b android-apk https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
```

### 在 Android Studio 中打开

1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `CompositionHelper` 目录
4. 等待 Gradle 同步完成

### 运行到模拟器

1. 在 AVD Manager 中创建模拟器
2. 在 Android Studio 中选择模拟器
3. 点击 ▶️ 运行按钮（或 Shift + F10）

### 运行到真机

1. **启用开发者选项**
   - 设置 → 关于手机
   - 连续点击"版本号" 7 次

2. **启用 USB 调试**
   - 设置 → 开发者选项
   - 勾选"USB 调试"

3. **连接设备**
   - 用 USB 线连接 Android 设备
   - 在设备上授权 USB 调试

4. **运行**
   - 在 Android Studio 中选择设备
   - 点击 ▶️ 运行

---

## 🛠 技术栈

### 核心技术

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)

### 主要依赖

#### Jetpack
- `androidx.compose` - 现代 UI 框架
- `androidx.camera` - CameraX 相机库
- `androidx.navigation` - 导航组件
- `androidx.lifecycle` - 生命周期管理

#### Google ML Kit
- `object-detection` - 物体检测
- `pose-detection` - 姿态检测
- `segmentation-selfie` - 人像分割

#### 第三方库
- `accompanist-permissions` - 权限请求
- `coil-kt` - 图片加载

---

## 📁 项目结构

```
CompositionHelper/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/compositionhelper/
│           │   ├── MainActivity.kt                    # 主 Activity
│           │   ├── CompositionHelperApp.kt            # 主 UI 组件
│           │   └── ui/
│           │       ├── composition/
│           │       │   └── CompositionOverlay.kt   # 构图绘制
│           │       └── theme/
│           │           └── Theme.kt                 # 主题配置
│           ├── res/
│           │   └── values/
│           │       └── strings.xml                 # 字符串资源
│           └── AndroidManifest.xml                  # 应用清单
├── build.gradle.kts                                 # 项目级 Gradle
├── settings.gradle.kts                               # Gradle 设置
├── gradle.properties                                 # Gradle 配置
└── README_ANDROID.md                                 # 本文档
```

---

## 🎯 构图类型详解

### 经典构图（7种）

| 构图类型 | 适用场景 | 效果 |
|---------|---------|------|
| 三分法 | 风景、人像 | 经典、平衡 |
| 中心构图 | 对称场景 | 强调主体 |
| 对角线 | 运动场景 | 增强动感 |
| 框架构图 | 门框、窗户 | 突出主体 |
| 引导线 | 道路、河流 | 引导视线 |
| S形曲线 | 河流、道路 | 柔美流畅 |
| 黄金螺旋 | 艺术创作 | 自然美感 |

### 现代热门构图（7种）

| 构图类型 | 适用场景 | 效果 |
|---------|---------|------|
| 黄金三角 | 建筑、产品 | 黄金比例 |
| 对称构图 | 对称建筑 | 平衡有序 |
| 负空间 | 极简摄影 | 突出主体 |
| 模式重复 | 建筑、纹理 | 节奏感 |
| 隧道式 | 桥洞、走廊 | 深度感 |
| 分割构图 | 多主题 | 对比效果 |
| 透视焦点 | 道路、建筑 | 空间感 |

### 视角构图（4种）

| 构图类型 | 适用场景 | 效果 |
|---------|---------|------|
| 隐形线 | 人物视线 | 引导流动 |
| 充满画面 | 微距、纹理 | 强调细节 |
| 低角度 | 建筑仰视 | 夸张透视 |
| 高角度 | 风景俯视 | 全局视角 |
| 深度层次 | 风景、叙事 | 立体感 |

---

## 🔧 构建配置

### Build Types

- **Debug**: 调试版本，包含调试信息
- **Release**: 发布版本，优化代码

### 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 清理构建
./gradlew clean
```

### 生成 APK

```bash
# Debug APK
./gradlew assembleDebug
# APK 位置: app/build/outputs/apk/debug/app-debug.apk

# Release APK
./gradlew assembleRelease
# APK 位置: app/build/outputs/apk/release/app-release.apk
```

---

## 📸 功能使用指南

### 基本操作

1. **选择构图类型**
   - 滑动底部的构图网格
   - 或使用分类筛选
   - 点击构图按钮切换

2. **选择照片**
   - 点击"相册"按钮
   - 从图库选择照片

3. **拍照**
   - 点击"拍照"按钮
   - 直接拍摄照片

4. **自动分析**
   - 点击"自动分析构图"按钮
   - 等待分析完成
   - 查看推荐的构图类型（带 ⭐ 标记）

5. **自定义辅助线**
   - 拖动"透明度"滑块
   - 点击颜色圆点切换颜色

---

## 🔐 权限说明

应用需要以下权限：

### 相机权限
- **用途**: 拍摄照片进行分析
- **请求时机**: 首次点击"拍照"按钮

### 存储权限
- **用途**: 访问相册中的照片
- **请求时机**: 首次点击"相册"按钮

### 权限配置

在 `AndroidManifest.xml` 中：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## 🐛 常见问题

### 问题 1: Gradle 同步失败

**原因**: Gradle 版本不兼容或网络问题

**解决**:
1. 检查 `gradle-wrapper.properties` 中的 Gradle 版本
2. 尝试使用代理（如果在中国）
3. 清理缓存：`./gradlew clean`

### 问题 2: 找不到设备

**原因**: USB 调试未启用或驱动问题

**解决**:
1. 确认开发者选项已启用
2. 启用 USB 调试
3. 重新连接 USB 线
4. 检查 ADB：`adb devices`

### 问题 3: 构建失败

**原因**: 依赖冲突或 SDK 不兼容

**解决**:
1. 检查 Android SDK 版本
2. 清理项目：`./gradlew clean`
3. 删除 `.gradle` 和 `.idea` 文件夹
4. 重新同步 Gradle

### 问题 4: ML Kit 错误

**原因**: Google Play 服务未安装

**解决**:
1. 检查设备是否安装 Google Play 服务
2. 添加到应用：`implementation 'com.google.android.gms:play-services-mlkit:xxx'`

---

## 📊 性能优化

### 内存优化

- 使用 `bitmap.recycle()` 释放位图
- 使用 Coil 的内存缓存
- 避免同时加载多张大图

### 渲染优化

- 使用 Jetpack Compose 的懒加载
- 避免不必要的重组
- 使用 `remember` 缓存计算结果

---

## 🚀 发布准备

### 签名配置

1. **创建密钥库**
   ```bash
   keytool -genkey -v -keystore release.keystore -alias compositionhelper -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **配置签名**
   在 `app/build.gradle.kts` 中：
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("release.keystore")
           storePassword = "your_password"
           keyAlias = "compositionhelper"
           keyPassword = "your_password"
       }
   }
   ```

3. **构建签名的 APK**
   ```bash
   ./gradlew assembleRelease
   ```

### 上传到 Google Play

1. 创建 Google Play 开发者账号
2. 创建应用
3. 上传签名的 APK
4. 填写应用信息
5. 提交审核

---

## 📚 参考文档

- [Jetpack Compose 官方文档](https://developer.android.com/jetpack/compose)
- [ML Kit 官方文档](https://developers.google.com/ml-kit)
- [CameraX 官方文档](https://developer.android.com/training/camerax)
- [Material Design 3](https://m3.material.io/)

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

---

## 📄 许可证

MIT License

---

**最后更新**: 2026-02-19
**版本**: 1.0.0
