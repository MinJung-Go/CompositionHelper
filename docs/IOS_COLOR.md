# iOS 智能调色适配

## 已实现

- iOS 16+ SwiftUI 深色编辑器：相机顶部、相册顶部和拍照结果均可进入。
- 系统 PhotosPicker 选图、四张离线参考照片，以及同步 Android 的暖金取景框图标。
- Gemini 输出照片专属方案；本地按基础参数 → 九点曲线 → HSL → 色彩平衡 → RGB 校色 → 强度混合执行，与 Android 的 gamma-encoded sRGB 算法一致。
- RGB 支持整体/阴影/中间调/高光和直接原色/反向互补色；不将缺少某种颜色视为偏色。
- 左右滑动对比、原图/效果切换、基础手动调整、撤销最近 20 次编辑、重置、恢复 AI 方案。
- 实际参数摘要保留小数，并展示预览变化像素比例和平均通道差。收到方案后先显示“正在渲染”；只有渲染成功才提示完成。零变化单独提示。
- 导出按原片尺寸保存 JPEG 副本，不覆盖原片；超过 3200 万像素明确拒绝导出以限制内存，不偷偷缩小。预览最多 1400 像素，方向按 ImageIO 元数据纠正。
- 网络使用临时 URLSession、禁止重定向、响应上限 256 KB，密钥仅在调色页面内存保存。只有点击 AI 才上传 JPEG 缩略图，不上传 EXIF。
- 返回相机时复用既有输入，避免重复配置；隐藏控件时不改变预留取景区域。清除了 Info.plist 中指向不存在 SceneDelegate 的配置。

## 验证

运行 `tools/test-color-core.sh`（需 Swift 5.9+）。本次在 Linux 官方 Swift 5.9.2 上完成 780 条断言：参数验证、直接/互补 RGB、零强度、RGB 为零时其他调整仍生效、小数精度，以及与 Android 共用 Python 参考像素（8-bit 通道容差 1）。

对新增及修改的 Swift 源码执行 `swiftc -frontend -parse`，语法检查通过；检查了 Info.plist、资源 JSON 与 Xcode 新文件注册，`git diff --check` 通过。CI 的占位归档步骤已替换为真实调色核心测试。

当前机器没有 macOS/Xcode/iOS SDK，**没有完成 UIKit/SwiftUI 类型检查、模拟器运行、签名或 IPA 构建**。在 Mac 上运行：

```bash
tools/test-color-core.sh
xcodebuild build -project CompositionHelper.xcodeproj -scheme CompositionHelper \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

真机验收：选择横/竖/带 EXIF 旋转的照片；打开四张样片；配置自己的 Key 并分析；核对实际参数与变化统计；拖动强度和 RGB、撤销；切换对比；导出并核对像素尺寸；拒绝照片添加权限；取消分析和更换照片；从编辑器返回相机。API 真实调用未在本次执行。

## 当前边界

- 没有对象分割，相近颜色的天空、水面可能同时变化。
- 当前 AI 默认沿用 Android 的模型名 `gemini-3.5-flash`，可在设置里更换；服务是否允许使用取决于用户的 API 权限。
- 不迁移 Android 的 CameraX 实现，iOS 保留 AVFoundation/Vision 构图逻辑；完整相机坐标、审美推荐等不是本次改写范围。
- 当前按 CPU 像素执行，较大原片的导出性能需真机测量；限制像素数量不保证所有设备无内存压力。
- 曲线、HSL、色彩平衡显示实际参数，但本版手动面板仅编辑基础项和 RGB。

## 平台依据

- [Apple PhotosPickerItem](https://developer.apple.com/documentation/PhotosUI/PhotosPickerItem)：异步读取选图数据。
- [Apple ImageIO 方向变换](https://developer.apple.com/documentation/imageio/kcgimagesourcecreatethumbnailwithtransform)：缩略图按方向和比例转换。
- 图片作者、来源与许可见 [SAMPLE_PHOTOS.md](SAMPLE_PHOTOS.md)。
