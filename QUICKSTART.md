# 🚀 快速开始指南

## 5分钟上手

### 第一步：准备环境

- **macOS**: Monterey (12.0) 或更高版本
- **Xcode**: 14.0 或更高版本
- **iOS 设备**: iPhone 12 或更高版本（推荐真机测试）
- **Apple ID**: 用于真机调试

### 第二步：创建项目（2分钟）

```bash
# 1. 打开 Xcode
open -a Xcode

# 2. 创建新项目
File → New → Project → iOS → App

# 3. 配置项目
Product Name: CompositionHelper
Interface: SwiftUI
Language: Swift
```

### 第三步：添加代码（2分钟）

1. **拖入文件**
   - 将以下 3 个文件拖入 Xcode 项目：
     - `CompositionHelper.swift`
     - `CompositionAnalyzer.swift`
     - 确保勾选 "Copy items if needed"

2. **配置权限**
   - 打开 `Info.plist`
   - 添加以下权限：
     ```xml
     <key>NSCameraUsageDescription</key>
     <string>需要相机权限来拍摄照片并进行构图分析</string>
     <key>NSPhotoLibraryUsageDescription</key>
     <string>需要访问相册来选择照片进行构图分析</string>
     ```

3. **修改入口文件**
   - 打开 `CompositionHelperApp.swift`
   - 替换为：
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

### 第四步：运行（1分钟）

1. **连接 iPhone**
   - 用 USB 线连接 iPhone
   - 信任电脑（手机上会弹出提示）

2. **选择设备**
   - 在 Xcode 顶部工具栏选择你的 iPhone

3. **点击运行**
   - 按 `⌘R` 或点击 ▶️ 按钮
   - 等待应用安装到手机

4. **授予权限**
   - 首次打开相机/相册时，点击"允许"

---

## 🎯 核心功能测试

### 测试 1：手动选择构图
1. 点击"相册"按钮选择一张照片
2. 滑动底部的构图类型选择器
3. 观察不同的辅助线效果

### 测试 2：自动分析
1. 选择一张照片
2. 点击"自动分析构图"按钮
3. 查看推荐的构图类型（带 ⭐ 标记）

### 测试 3：自定义辅助线
1. 拖动"辅助线透明度"滑块
2. 点击不同的颜色圆点
3. 调整到你喜欢的效果

### 测试 4：相机拍照
1. 点击"拍照"按钮
2. 拍摄一张照片
3. 应用会自动显示并准备好分析

---

## 📸 推荐测试照片

为了获得最佳的分析效果，建议使用以下类型的照片：

| 类型 | 说明 | 预期推荐构图 |
|------|------|-------------|
| 人像 | 人物位于画面三分之一处 | 三分法 |
| 风景 | 有河流或道路的风景 | S形曲线、引导线 |
| 建筑 | 对称的建筑物 | 中心构图 |
| 街拍 | 有明显线条的街道 | 对角线、引导线 |
| 特写 | 物体位于中心 | 中心构图 |

---

## 🐛 常见问题

### ❌ 相机无法打开

**原因**: 权限未授予或模拟器限制

**解决**:
1. 检查 Info.plist 中的权限描述
2. 确保在真机上测试（模拟器不支持相机）
3. 删除应用后重新安装

### ❌ 照片选择器不工作

**原因**: 照片库权限未授予

**解决**:
1. 设置 → 隐私 → 照片 → 找到应用 → 允许访问
2. 重新启动应用

### ❌ 自动分析按钮是灰色的

**原因**: 没有选择照片

**解决**:
1. 先从相册选择一张照片
2. 按钮会自动变为可点击状态

### ❌ 编译错误 "Cannot find 'CompositionAnalyzer'"

**原因**: CompositionAnalyzer.swift 文件未正确导入

**解决**:
1. 确保文件已添加到项目
2. 检查文件是否在项目导航器中
3. Clean Build Folder (⌘ShiftK) 后重新编译

### ❌ 应用崩溃

**原因**: 通常是内存不足或权限问题

**解决**:
1. 确保照片不要太大（建议 < 10MB）
2. 重新授予相机和照片库权限
3. 重启应用

---

## 🎨 自定义你的应用

### 修改默认构图类型

在 `CompositionHelper.swift` 中找到：

```swift
@State private var selectedComposition: CompositionType = .ruleOfThirds
```

改为：

```swift
@State private var selectedComposition: CompositionType = .goldenSpiral
```

### 添加新的构图类型

1. 在 `CompositionType` 枚举中添加：
   ```swift
   case custom = "自定义"
   ```

2. 创建新的视图组件：
   ```swift
   struct CustomComposition: View {
       let size: CGSize
       let opacity: Double
       let color: Color
       
       var body: some View {
           // 你的绘图代码
       }
   }
   ```

3. 在 `compositionOverlay` 中添加 case：
   ```swift
   case .custom:
       CustomComposition(size: size, opacity: lineOpacity, color: lineColor)
   ```

### 修改辅助线颜色

在底部的颜色选择器中修改：

```swift
ForEach([Color.yellow, .red, .blue, .white, .green, .orange, .purple], id: \.self) { color in
    // 添加更多颜色...
}
```

---

## 📊 性能优化建议

### 大图处理慢？

在导入图片时添加压缩：

```swift
// 在 ImagePicker 中修改
if let image = info[.originalImage] as? UIImage {
    // 压缩到最大 1920x1920
    let resized = image.resized(toWidth: 1920)
    onImagePicked(resized)
}
```

### 分析卡顿？

添加加载状态：

```swift
@State private var isAnalyzing = false

// 在 analyzeComposition 中
func analyzeComposition() {
    isAnalyzing = true
    
    Task {
        // ... 分析代码 ...
        
        await MainActor.run {
            isAnalyzing = false
        }
    }
}
```

---

## 🚀 下一步

- [ ] 尝试不同的构图类型
- [ ] 测试各种场景的照片
- [ ] 调整辅助线样式
- [ ] 探索自动分析的准确性
- [ ] 分享你的作品！

---

## 📚 需要更多帮助？

- 查看 `PROJECT_SETUP.md` 了解详细配置
- 查看 `README.md` 了解完整功能列表
- 查看 `CompositionAnalyzer.swift` 了解分析算法

**祝你开发愉快！🎉**
