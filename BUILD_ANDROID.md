# Android APK 构建说明

## 🚀 快速开始

### 前置要求

1. **JDK 17 或更高版本**
   ```bash
   # 检查 Java 版本
   java -version

   # 如果未安装，访问 https://adoptium.net/ 下载
   ```

2. **Android Studio** (推荐用于开发)
   - 下载: https://developer.android.com/studio

3. **Android SDK**
   - Android SDK 34
   - Build Tools 34.0.0

---

## 📦 两种构建方式

### 方式一：使用 Android Studio (推荐)

```bash
# 1. 克隆项目
git clone -b android-apk https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper

# 2. 在 Android Studio 中打开
# File → Open → 选择 CompositionHelper 文件夹

# 3. 等待 Gradle 同步完成

# 4. 连接 Android 设备或启动模拟器

# 5. 点击 ▶️ 按钮运行
```

### 方式二：命令行构建

```bash
# 1. 初始化 Gradle Wrapper (首次运行)
# 需要先安装 Gradle
gradle wrapper --gradle-version 8.2

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 构建 Release APK
./gradlew assembleRelease

# 4. APK 位置
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 📱 安装到 Android 设备

### 方法一：ADB 安装

```bash
# 1. 启用 USB 调试
# 手机：设置 → 开发者选项 → 启用 USB 调试

# 2. 连接设备
adb devices

# 3. 安装 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. 如果已安装，使用 -r 替换
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 方法二：直接传输

1. 将 `app-debug.apk` 传输到手机
2. 在手机上点击 APK 文件
3. 允许安装未知来源
4. 完成安装

---

## 🔧 GitHub Actions 自动构建

### 当前状态

GitHub Actions 配置已完成，但需要先初始化 Gradle Wrapper。

### 初始化 Gradle Wrapper

```bash
# 在本地执行
cd CompositionHelper

# 安装 Gradle (如果未安装)
# macOS:
brew install gradle

# 或下载: https://gradle.org/install/

# 生成 gradle-wrapper.jar
gradle wrapper --gradle-version 8.2

# 提交到 GitHub
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add gradle-wrapper.jar for CI/CD"
git push origin android-apk
```

### 提交后的自动构建

初始化 Gradle Wrapper 后，GitHub Actions 会自动：
1. 构建 Debug APK
2. 构建 Release APK
3. 上传 APK 到 Artifacts
4. 保留 30 天供下载

### 下载 GitHub Actions 构建的 APK

1. 访问: https://github.com/MinJung-Go/CompositionHelper/actions
2. 找到最新的 Android CI workflow 运行
3. 滚动到 "Artifacts" 部分
4. 下载 `app-debug` 或 `app-release`

---

## 🐛 常见问题

### 问题 1: Gradle Wrapper 未找到

**错误**: `./gradlew: No such file or directory`

**解决**:
```bash
gradle wrapper --gradle-version 8.2
```

### 问题 2: Java 版本不兼容

**错误**: `Unsupported class file major version`

**解决**:
```bash
# 检查 Java 版本
java -version

# 如果不是 JDK 17，下载 JDK 17
# https://adoptium.net/
```

### 问题 3: Android SDK 未找到

**错误**: `SDK location not found`

**解决**:
1. 打开 Android Studio
2. SDK Manager → SDK Platforms → 安装 Android 14 (API 34)
3. SDK Manager → SDK Tools → 安装 Build Tools 34.0.0
4. 设置 `local.properties`:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

### 问题 4: ADB 设备未找到

**错误**: `adb: no devices/emulators found`

**解决**:
1. 确认 USB 调试已启用
2. 重新连接 USB 线
3. 检查设备授权
4. 重启 ADB:
   ```bash
   adb kill-server
   adb start-server
   ```

### 问题 5: 构建失败 - 编译错误

**错误**: Kotlin 编译错误

**解决**:
```bash
# 清理构建
./gradlew clean

# 重新构建
./gradlew assembleDebug
```

---

## 📊 构建配置

### Build Variants

| Variant | 用途 | 输出 |
|---------|------|------|
| Debug | 开发测试 | app-debug.apk |
| Release | 正式发布 | app-release-unsigned.apk |

### Release APK 签名

Release APK 需要签名才能安装到设备：

```bash
# 1. 创建密钥库
keytool -genkey -v -keystore release.keystore -alias compositionhelper -keyalg RSA -keysize 2048 -validity 10000

# 2. 在 build.gradle.kts 中配置签名
# (已包含在项目中，只需替换密钥库路径)

# 3. 构建签名的 APK
./gradlew assembleRelease

# 4. APK 位置
# app/build/outputs/apk/release/app-release.apk
```

---

## 🎯 功能测试

### 基本功能测试

1. **打开应用**
   - 应该看到构图辅助界面
   - 占位图显示在中间

2. **拍照功能**
   - 点击"拍照"按钮
   - 应该打开相机
   - 拍摄后照片应显示在应用中

3. **相册选择**
   - 点击"相册"按钮
   - 选择一张照片
   - 照片应显示在应用中

4. **构图切换**
   - 点击不同的构图按钮
   - 辅助线应相应变化

5. **透明度调节**
   - 拖动透明度滑块
   - 辅助线透明度应实时变化

6. **颜色切换**
   - 点击不同的颜色圆点
   - 辅助线颜色应实时变化

7. **自动分析**
   - 选择照片后点击"自动分析构图"
   - 应该看到推荐结果

---

## 📚 相关文档

- [README_ANDROID.md](README_ANDROID.md) - 完整的 Android 项目文档
- [GRADLE_SETUP.md](GRADLE_SETUP.md) - Gradle Wrapper 初始化说明
- [COMPOSITION_GUIDE.md](../COMPOSITION_GUIDE.md) - 18 种构图类型详解

---

## 🆘 获取帮助

如果遇到问题：

1. 查看 GitHub Issues: https://github.com/MinJung-Go/CompositionHelper/issues
2. 检查 Actions 运行日志: https://github.com/MinJung-Go/CompositionHelper/actions
3. 查看详细文档: [README_ANDROID.md](README_ANDROID.md)

---

**最后更新**: 2026-02-19
