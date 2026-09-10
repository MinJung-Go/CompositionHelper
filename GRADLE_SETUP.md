# Gradle Wrapper 说明

项目使用 Gradle Wrapper 确保构建环境一致性。

## 版本信息

- Gradle: 8.2
- Android Gradle Plugin: 8.2.0
- JDK: 17 (必须)

## 常见问题

### gradle-wrapper.jar 缺失

```bash
# 安装 Gradle 后生成 wrapper
gradle wrapper --gradle-version 8.2

# 提交
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "Add gradle-wrapper.jar"
```

### JDK 版本不兼容

本项目 AGP 8.2.0 使用 JDK 17。JDK 不兼容时错误文本可能不同，请同时记录 `java -version` 和 Gradle 输出，不凭单条错误猜测版本。

下载 JDK 17: https://adoptium.net/

## 更多信息

- [完整 Android 文档](docs/ANDROID.md)

Wrapper 下载地址由 `gradle/wrapper/gradle-wrapper.properties` 控制，当前为腾讯镜像的 Gradle 8.2 ZIP。不要绕过 Wrapper 任意改用系统 Gradle；如更换镜像，应作为独立构建配置改动验证。统一测试命令见 [TESTING.md](docs/TESTING.md)。
