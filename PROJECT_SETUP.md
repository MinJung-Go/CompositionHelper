# 项目设置指南

## 第一步：创建 Xcode 项目

1. 打开 Xcode
2. 选择 **File → New → Project**
3. 选择 **iOS** → **App**
4. 填写项目信息：
   - **Product Name**: CompositionHelper
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None
5. 保存到你的工作目录

## 第二步：配置权限

在 `Info.plist` 中添加以下权限说明：

### 方法1：直接编辑 Info.plist

右键点击 `Info.plist` → Open As → Source Code，在 `<dict>` 标签内添加：

```xml
<key>NSCameraUsageDescription</key>
<string>需要相机权限来拍摄照片并进行构图分析</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>需要访问相册来选择照片进行构图分析</string>
```

### 方法2：使用 Xcode 图形界面

1. 选择项目 → Target → Info
2. 点击 "Custom iOS Target Properties" 的 "+" 按钮
3. 添加：
   - Key: `Privacy - Camera Usage Description`
   - Value: `需要相机权限来拍摄照片并进行构图分析`
   
4. 再次点击 "+"
   - Key: `Privacy - Photo Library Usage Description`
   - Value: `需要访问相册来选择照片进行构图分析`

## 第三步：导入代码

1. 将 `CompositionHelper.swift` 文件拖入 Xcode 项目
2. 在弹出的对话框中选择：
   - ✅ Copy items if needed
   - ✅ Create groups
   - ✅ Add to targets: 勾选你的项目

## 第四步：修改主入口

打开 `CompositionHelperApp.swift`，替换为：

```swift
import SwiftUI

@main
struct CompositionHelperApp: App {
    var body: some Scene {
        WindowGroup {
            CompositionHelperView()
        }
    }
}
```

## 第五步：运行项目

1. 选择目标设备（真机或模拟器）
2. 点击 Run 按钮（⌘R）
3. 首次运行时，系统会请求相机和照片库权限，点击"允许"

## 注意事项

### 真机测试（推荐）

相机功能在模拟器上可能受限，建议使用真机测试：

1. 将 iPhone 连接到 Mac
2. 在 Xcode 顶部设备选择器中选择你的 iPhone
3. 确保设备已信任开发者（设置 → 通用 → VPN与设备管理）

### 模拟器限制

- 相机功能：模拟器会显示黑色或相机不可用
- 照片库：需要在模拟器的 Photos 应用中先添加照片
- 建议：主要在真机上测试完整功能

## 故障排除

### 问题1：相机无法打开

**解决方案**：
- 确保已在 Info.plist 中添加相机权限描述
- 检查设备的隐私设置（设置 → 隐私 → 相机）
- 在真机上测试，模拟器可能不支持

### 问题2：照片选择器不工作

**解决方案**：
- 确保 Info.plist 中有照片库权限描述
- 检查系统权限设置（设置 → 隐私 → 照片）
- 尝试删除应用后重新安装

### 问题3：编译错误

**解决方案**：
- 确保 iOS 部署目标至少为 iOS 15.0
- 清理项目（Product → Clean Build Folder）
- 重新编译（⌘ShiftK）

### 问题4：Vision Framework 警告

**解决方案**：
- Vision Framework 需要真机才能完整测试
- 模拟器上部分功能可能受限，这是正常的

## 增强功能（可选）

### 添加图标

1. 准备 1024x1024 的 App Icon
2. 在 Xcode 中：Assets.xcassets → AppIcon
3. 拖入不同尺寸的图标

### 添加启动屏幕

1. 创建 `LaunchScreen.storyboard`
2. 设计简洁的启动界面
3. 在项目设置中选择 Launch Screen File

### 配置版本号

在项目设置的 General 标签中：
- **Version**: 1.0
- **Build**: 1

## 下一步

- [ ] 测试所有构图类型
- [ ] 优化自动分析算法
- [ ] 添加更多自定义选项
- [ ] 设计应用图标
- [ ] 准备 App Store 截图

## 需要帮助？

- Apple 官方文档：https://developer.apple.com/documentation/
- SwiftUI 教程：https://developer.apple.com/tutorials/swiftui
- Vision Framework：https://developer.apple.com/documentation/vision
