# 🎬 Xcode 项目使用指南

## 📋 快速开始

### 前置要求

- **macOS**: 12.0 (Monterey) 或更高版本
- **Xcode**: 14.0 或更高版本
- **iOS 设备**: iPhone 12 或更高版本（用于真机测试）
- **Apple Developer Account**: 可选，用于发布到 App Store

### 打开项目

1. **克隆仓库**（如果还没克隆）
   ```bash
   git clone https://github.com/MinJung-Go/CompositionHelper.git
   cd CompositionHelper
   ```

2. **打开 Xcode 项目**
   ```bash
   open CompositionHelper.xcodeproj
   ```

   或者在 Finder 中双击 `CompositionHelper.xcodeproj`

---

## 🛠 编译项目

### 方式一：使用 Xcode（推荐）

1. 在 Xcode 顶部选择目标设备
   - 模拟器：iPhone 15 / iPhone 15 Pro
   - 真机：连接你的 iPhone

2. 点击 Product → Build 或按 `⌘B`

3. 查看编译状态
   - 成功：左侧工具栏显示 ✓
   - 失败：查看下方 Issue Navigator

### 方式二：使用命令行

```bash
# 使用编译脚本
chmod +x build.sh
./build.sh

# 或直接使用 xcodebuild
xcodebuild build \
    -project CompositionHelper.xcodeproj \
    -scheme CompositionHelper \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 15'
```

---

## 📱 运行应用

### 在模拟器中运行

1. 在 Xcode 中选择模拟器（如 iPhone 15）
2. 点击 ▶️ 按钮或按 `⌘R`
3. 模拟器将自动启动并运行应用

### 在真机中运行

1. **连接 iPhone**
   - 使用 USB 线连接 Mac
   - 在 iPhone 上信任此电脑

2. **选择设备**
   - 在 Xcode 顶部设备选择器中选择你的 iPhone

3. **配置签名**
   - 选择项目 → Target → Signing & Capabilities
   - Team: 选择你的开发者团队
   - 或勾选 "Automatically manage signing"

4. **运行**
   - 点击 ▶️ 或按 `⌘R`
   - 首次运行需要在 iPhone 上信任开发者

### 授予开发者信任（iPhone）

1. 打开 **设置** → **通用** → **VPN与设备管理**
2. 找到你的开发者账号
3. 点击信任

---

## 🎯 项目结构

```
CompositionHelper/
├── CompositionHelper.xcodeproj/     # Xcode 项目文件
│   └── project.pbxproj              # 项目配置
├── Sources/                        # 源代码
│   ├── CompositionHelperApp.swift    # App 入口
│   ├── Views/                       # 视图组件
│   │   └── CompositionHelper.swift  # 主视图（18种构图）
│   └── Analyzers/                   # 分析器
│       └── CompositionAnalyzer.swift # 图像分析
├── Info.plist                      # 应用配置
├── build.sh                        # 编译脚本
├── run.sh                          # 运行脚本
└── Documentation/                  # 文档
```

---

## 🔧 配置说明

### Bundle Identifier

当前设置：`com.example.CompositionHelper`

**修改步骤**：
1. 选择项目 → Target
2. General → Bundle Identifier
3. 修改为你的标识符（如：`com.yourname.CompositionHelper`）

### 版本号

- **Version**: 1.0（用户看到的版本）
- **Build**: 1（内部构建版本）

**修改位置**：Target → General → Identity

### iOS 部署目标

最低版本：**iOS 15.0**

**修改步骤**：
1. Target → General
2. Deployment Info → Minimum Deployments

---

## 🎨 自定义配置

### 修改应用名称

1. 打开 `Info.plist`
2. 找到 `CFBundleDisplayName`
3. 修改值（如：`构图辅助`）

### 修改默认构图类型

打开 `Sources/Views/CompositionHelper.swift`：
```swift
@State private var selectedComposition: CompositionType = .ruleOfThirds
```

修改为其他类型：
```swift
@State private var selectedComposition: CompositionType = .goldenSpiral
```

### 添加新的辅助线颜色

打开 `Sources/Views/CompositionHelper.swift`，找到颜色选择器：
```swift
ForEach([Color.yellow, .red, .blue, .white, .green], id: \.self) { color in
    // ...
}
```

添加更多颜色：
```swift
ForEach([Color.yellow, .red, .blue, .white, .green, .orange, .purple], id: \.self) { color in
    // ...
}
```

---

## 🐛 常见问题

### ❌ "Command SwiftCompile failed"

**解决方案**：
1. Clean Build Folder：`⌘ShiftK`
2. 删除 Derived Data：`~/Library/Developer/Xcode/DerivedData`
3. 重新编译

### ❌ "Signing for \"CompositionHelper\" requires a development team"

**解决方案**：
1. Target → Signing & Capabilities
2. Team: 选择你的团队（需要 Apple Developer 账号）
3. 或勾选 "Automatically manage signing"

### ❌ "No such module 'Vision'"

**解决方案**：
1. 检查 iOS 部署目标是否 >= 15.0
2. Target → General → Deployment Info
3. 确保 Minimum Deployments 设置为 iOS 15.0+

### ❌ 模拟器无法启动

**解决方案**：
1. 关闭所有模拟器
2. Xcode → Window → Devices and Simulators
3. 删除有问题的模拟器
4. 重新添加模拟器

### ❌ 真机上无法运行

**解决方案**：
1. 检查 USB 线是否连接
2. 在 iPhone 上：设置 → 通用 → 传输或还原 iPhone → 信任此电脑
3. 检查开发者证书是否有效
4. 在 iPhone 设置中信任开发者

---

## 📊 性能优化

### 加快编译速度

1. **使用增量编译**
   - Xcode → Preferences → Behaviors
   - 勾选 "Build" → "Show navigator"

2. **并行编译**
   - Xcode → Preferences → Locations
   - Number of parallel builds: 根据CPU核心数设置

3. **使用预编译头文件**
   - Target → Build Settings
   - 搜索 "Precompile Prefix Header"
   - 设置为 Yes

### 减小应用大小

1. **启用 Bitcode**
   - Target → Build Settings
   - Enable Bitcode: Yes

2. **优化资源**
   - 压缩图片资源
   - 移除未使用的资源

---

## 🎓 开发技巧

### 使用预览

Xcode 支持 SwiftUI 预览：

```swift
struct CompositionHelperView_Previews: PreviewProvider {
    static var previews: some View {
        CompositionHelperView()
    }
}
```

在代码编辑器右侧查看实时预览。

### 调试技巧

1. **断点调试**
   - 点击行号左侧设置断点
   - 运行到断点自动暂停

2. **Print 调试**
   ```swift
   print("Debug: value = \(someValue)")
   ```

3. **LLDB 调试**
   ```swift
   po variableName  // 打印变量
   expr 2 + 2      // 执行表达式
   ```

### 查看日志

```bash
# 查看系统日志
xcrun simctl spawn booted log stream --predicate 'process == "CompositionHelper"'

# 或在 Xcode 中
# Window → Devices and Simulators → View Device Logs
```

---

## 🚀 发布准备

### App Store 配置

1. **修改 Bundle ID**
   - 使用你的反向域名：`com.yourcompany.CompositionHelper`

2. **配置签名**
   - Team: 选择你的开发团队
   - Signing Certificate: 生成或使用现有的

3. **设置版本**
   - Version: 1.0
   - Build: 1（每次发布增加）

### Archive 构建

1. Product → Archive (⌘ShiftB)
2. 等待构建完成
3. 在 Organizer 中选择 Archive
4. Distribute App → App Store Connect

### 提交到 App Store

详细步骤请查看 [DEPLOYMENT.md](DEPLOYMENT.md)

---

## 📞 获取帮助

### 文档资源
- [README.md](README.md) - 项目主文档
- [QUICKSTART.md](QUICKSTART.md) - 快速开始
- [COMPOSITION_GUIDE.md](COMPOSITION_GUIDE.md) - 构图指南
- [FEATURES.md](FEATURES.md) - 功能详解

### 在线资源
- [Apple Developer Documentation](https://developer.apple.com/documentation/)
- [SwiftUI Tutorials](https://developer.apple.com/tutorials/swiftui)
- [Xcode Help](https://help.apple.com/xcode/)

### 问题反馈
- GitHub Issues: https://github.com/MinJung-Go/CompositionHelper/issues
- Email: your.email@example.com

---

## ✅ 检查清单

在编译和运行前，确保：

- [ ] macOS 版本 >= 12.0
- [ ] Xcode 版本 >= 14.0
- [ ] Swift 版本 >= 5.0
- [ ] 已克隆或下载项目
- [ ] 已在 Xcode 中打开项目
- [ ] 已配置签名（真机测试需要）
- [ ] 已授予相机和照片库权限（真机）

---

**祝开发愉快！** 🎉

**文档版本**: 1.0
**最后更新**: 2026-02-19
