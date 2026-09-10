# iOS 安装、构建与使用

更新：2026-09-10。源码在 `ios` 分支；App 使用 Xcode 工程构建。完整结构见 [架构文档](ARCHITECTURE.md)。

## 环境

- App 最低 iOS **16.0**，与 Xcode 工程及 `Package.swift` 一致。
- 使用 Swift 5.9+ 工具链；Xcode 15 或更新版本，并安装对应 iOS SDK。
- macOS 版本须满足所选 Xcode 要求；本项目没有对所有 Xcode/macOS 组合完成验收。
- 无 Mac 时可使用已有云端未签名 IPA，在 Windows 按 [iloader 指南](ILOADER_IOS_TESTING.md)签名安装。

## 获取代码与运行

```bash
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
open CompositionHelper.xcodeproj
```

如果需要本轮未推送的新功能，应使用交付方提供的对应提交/构建，不能假定刚克隆的远端已包含所有本地改动。

在 Xcode 选择 `CompositionHelper` Scheme。模拟器可用于样片、调色及界面检查；真实相机、照片授权、保存与拍摄质量需要 iPhone。使用真机时配置自己的开发团队、连接并信任设备，按系统提示启用开发者模式。开发签名、未签名 IPA 和商店分发是不同交付路径。

命令行编译与截图入口见 [测试指南](TESTING.md)。`Package.swift` 提供库描述，但当前已验证的 App 打包入口是 `CompositionHelper.xcodeproj`，不要将 `swift build` 当作完整 iOS App 验收。

## 当前入口

- 默认进入实时相机，可切换构图、辅助线设置与智能建议。
- 相机顶部“调色”和拍摄结果页可进入色彩工作室；调色页自身可选相册照片、内置样片。
- 清单版本在相机顶部提供“拍摄检查清单”，由人标记结果并保存备注；此前 `1d9a096` 包不含此功能。
- 静态相册构图分析视图虽有代码，默认相机到该视图的导航尚未接通，见 [NAV-01](KNOWN_ISSUES.md)。不要用“存在页面源码”代替可达性验收。

## 权限与数据

`Info.plist` 包含相机、照片读取、添加照片的用途说明。系统 PhotosPicker 的选择行为、相册添加授权和相机授权分别验收；拒绝权限后不能把无图状态当作正常拍摄结果。照片、密钥和清单数据范围见 [数据说明](DATA_PRIVACY.md)。

## 排查顺序

1. 无法编译：记录 Xcode/Swift 版本、提交号及第一条编译错误，区分 SDK 问题、源码问题与签名问题。
2. 无法安装：先确认 IPA 是否签名、设备系统是否满足最低要求、工具的具体失败阶段。
3. 相机黑屏：确认真机、相机权限与会话错误；模拟器截图不是相机验收。
4. AI 失败：区分设置、网络/账号、配方校验和本地渲染阶段，不因登录或请求失败直接替换安装驱动。
5. 保存提示成功：到系统相册实际打开核对原片、副本、方向和尺寸。

最近已验证的编辑器版本及本轮待构建清单版本见 [交付记录](RELEASES.md)。使用者逐项验收见 [拍摄清单](SHOOTING_CHECKLIST.md)。
