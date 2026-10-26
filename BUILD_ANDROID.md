# Android 构建说明

> 详细的安装、构建和配置指南已迁移至 [docs/ANDROID.md](docs/ANDROID.md)

## 快速构建

```bash
# 前置: JDK 17
java -version

# 构建 Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# APK 位置: app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions CI

每次推送到 `master` 分支会自动触发 CI 构建，配置文件: `.github/workflows/android-ci.yml`

在 [Actions 页面](https://github.com/MinJung-Go/CompositionHelper/actions) 查看构建状态和下载 Artifacts。

## 更多信息

- [完整 Android 文档](docs/ANDROID.md)
- [项目 README](README.md)
