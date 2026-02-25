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

AGP 8.2.0 要求 JDK 17，使用 JDK 11 会报 `Unsupported class file major version` 错误。

下载 JDK 17: https://adoptium.net/

## 更多信息

- [完整 Android 文档](docs/ANDROID.md)
