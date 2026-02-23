# iOS 版本详细文档

> CompositionHelper iOS 版本的完整安装、构建和配置指南

## 📋 前置要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| macOS | 12.0 (Monterey) | 13.0+ |
| Xcode | 14.0 | 15.0+ |
| iOS 部署目标 | 15.0 | 17.0+ |
| Swift | 5.0 | 5.9+ |

## 🛠️ 安装步骤

### 方式一：克隆并直接打开

```bash
# 克隆 ios 分支（iOS 版本）
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper

# 使用 Xcode 打开
open CompositionHelper.xcodeproj
# 或
xed .
```

### 方式二：使用 Git 切换分支

如果你已经克隆了仓库：

```bash
git fetch origin
git checkout ios
git pull origin ios
```

## ▶️ 运行到设备

### 模拟器

1. 在 Xcode 中选择目标设备（⌘⇧2 打开设备列表）
2. 推荐选择 iPhone 14 Pro 或更高机型
3. 点击 ▶️ 运行按钮或按 `⌘R`

### 真机

1. **连接 iPhone**
   - 使用 USB 线连接设备
   - 在设备上信任此电脑

2. **配置开发者账号**
   - Xcode → Preferences → Accounts
   - 添加 Apple ID

3. **配置签名**
   - 选择项目 → Target → Signing & Capabilities
   - 选择你的开发团队
   - Xcode 会自动处理签名

4. **运行**
   - 选择设备
   - 按 `⌘R` 运行

## 🛠 技术栈

### 核心技术

```yaml
语言: Swift 5.0+
UI 框架: SwiftUI
最低版本: iOS 15.0
目标版本: iOS 17.0+
```

### 主要框架

#### 系统框架
- `SwiftUI` - 声明式 UI 框架
- `Vision` - 图像分析框架
- `Core Image` - 图像处理
- `AVFoundation` - 相机功能
- `PhotosUI` - 相册选择器

#### 依赖管理（如果使用）

项目使用标准 SwiftUI 开发，无需额外依赖。

## 📁 项目结构

```
CompositionHelper/
├── Sources/
│   ├── CompositionHelperApp.swift      # App 入口
│   ├── Views/
│   │   └── CompositionHelper.swift     # 主视图
│   ├── Analyzers/
│   │   └── CompositionAnalyzer.swift   # 图像分析器
│   ├── Models/                          # 数据模型
│   │   ├── CompositionType.swift       # 构图类型
│   │   └── PhotoPicker.swift           # 照片选择器
│   └── Utils/                           # 工具类
│       ├── ImageProcessor.swift        # 图像处理
│       └── OverlayRenderer.swift       # 辅助线渲染
├── Resources/                           # 资源文件
│   ├── Assets.xcassets                  # 图片资源
│   └── Info.plist                       # 应用配置
├── CompositionHelper.xcodeproj          # Xcode 项目
└── Package.swift                        # Swift Package 配置（可选）
```

## 🔐 权限说明

### 必需权限

在 `Info.plist` 中添加以下权限描述：

```xml
<!-- 相机权限 -->
<key>NSCameraUsageDescription</key>
<string>需要访问相机以拍摄照片进行构图分析</string>

<!-- 相册权限 -->
<key>NSPhotoLibraryUsageDescription</key>
<string>需要访问相册以选择照片进行构图分析</string>

<key>NSPhotoLibraryAddUsageDescription</key>
<string>需要保存分析后的照片到相册</string>
```

### 配置步骤

1. 在 Xcode 中打开 `Info.plist`
2. 添加上述键值对
3. 自定义描述文本以适应你的应用

## 🔧 构建配置

### Build Configurations

| 配置 | 说明 | 用途 |
|------|------|------|
| Debug | 调试版本 | 开发调试 |
| Release | 发布版本 | App Store 发布 |

### 构建命令

```bash
# 使用 Xcode 命令行工具
xcodebuild -scheme CompositionHelper -configuration Debug build

# Release 构建
xcodebuild -scheme CompositionHelper -configuration Release build

# 清理构建
xcodebuild clean
```

### 归档和导出

```bash
# 归档
xcodebuild archive -scheme CompositionHelper \
  -archivePath ~/Desktop/CompositionHelper.xcarchive

# 导出到 App Store
xcodebuild -exportArchive \
  -archivePath ~/Desktop/CompositionHelper.xcarchive \
  -exportPath ~/Desktop/CompositionHelperApp \
  -exportOptionsPlist ExportOptions.plist
```

## 🐛 常见问题

### ❓ 相机无法打开？

**解决方案:**
1. 确保 Info.plist 中添加了相机权限描述
2. 在真机上测试（模拟器可能不支持相机）
3. 检查 设置 → 隐私 → 相机 → 你的应用

### ❓ 分析速度慢？

**解决方案:**
1. 压缩图片到 1920x1920 以下
2. 关闭其他后台应用
3. 使用性能更好的设备

```swift
// 图片压缩示例
func scaleImage(_ image: UIImage, maxSize: CGFloat) -> UIImage {
    let size = image.size
    let ratio = min(maxSize / size.width, maxSize / size.height)
    let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)

    UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
    image.draw(in: CGRect(origin: .zero, size: newSize))
    let scaledImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return scaledImage ?? image
}
```

### ❓ 自动分析不准确？

**说明:**
这是正常现象，Vision Framework 在复杂场景下精度有限。建议：
1. 尝试手动选择构图类型
2. 使用更清晰的图片
3. 确保图片光线充足

### ❓ Xcode 编译错误

**解决方案:**
```bash
# 1. 清理构建
⌘⇧K (Clean Build Folder)

# 2. 重新构建
⌘B (Build)

# 3. 重启 Xcode

# 4. 更新 Xcode 到最新版本
```

### ❓ 真机运行失败

**检查清单:**
- [ ] 开发者账号已配置
- [ ] Bundle Identifier 唯一
- [ ] Provisioning Profile 有效
- [ ] 设备版本 >= iOS 15.0
- [ ] 信任开发者证书（设置 → 通用 → VPN与设备管理）

## 📊 性能优化

### 内存优化

```swift
// 1. 使用 @StateObject 管理对象生命周期
@StateObject private var analyzer = CompositionAnalyzer()

// 2. 及时释放大对象
deinit {
    // 清理资源
}

// 3. 使用 LazyVStack/LazyHStack
LazyVStack {
    ForEach(items) { item in
        ItemView(item: item)
    }
}
```

### 图像处理优化

```swift
// 1. 后台处理
DispatchQueue.global(qos: .userInitiated).async {
    let processed = self.processImage(image)
    DispatchQueue.main.async {
        self.result = processed
    }
}

// 2. 使用 lazy 加载
@Lazy var cachedOverlay: UIImage = {
    // 生成辅助线
}()

// 3. 缓存处理结果
NSCache<NSString, UIImage>().setObject(image, forKey: "key")
```

## 🚀 App Store 发布

### 发布前检查清单

- [ ] 应用图标（所有尺寸）
- [ ] 启动屏幕
- [ ] 截图（所有 iPhone 尺寸）
- [ ] 应用描述和关键词
- [ ] 隐私政策 URL
- [ ] 内容分级问卷
- [ ] 导出合规性
- [ ] 广告标识符（IDFA）设置

### 发布流程

1. **创建应用记录**
   - 访问 [App Store Connect](https://appstoreconnect.apple.com)
   - 创建新应用

2. **填写应用信息**
   - 应用名称
   - 副标题
   - 类别
   - 内容版权

3. **上传构建**
   - Product → Archive
   - Distribute App
   - 选择 "App Store Connect"

4. **提交审核**
   - 填写版本信息
   - 添加审核说明
   - 提交审核

## 📚 参考文档

### 官方文档

- [SwiftUI](https://developer.apple.com/documentation/swiftui) - UI 框架
- [Vision Framework](https://developer.apple.com/documentation/vision) - 图像分析
- [Core Image](https://developer.apple.com/documentation/coreimage) - 图像处理
- [AVFoundation](https://developer.apple.com/documentation/avfoundation) - 相机
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/) - 设计指南

### 学习资源

- [SwiftUI 教程](https://developer.apple.com/tutorials/swiftui) - 官方教程
- [WWDC Sessions](https://developer.apple.com/wwdc/) - 开发者大会
- [Swift by Sundell](https://www.swiftbysundell.com/) - Swift 社区

---

**返回 [主 README](../README.md)**
