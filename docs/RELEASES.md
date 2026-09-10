# 版本、构建与交付记录

更新：2026-09-10。当前为开发测试交付，没有完成正式商店发布流程。本表区分源码、已生成安装包和已验证设备范围。

## 可追溯基线

| 平台 / 代码提交 | 交付与证据 | 范围 |
|---|---|---|
| iOS `1d9a096` | [CI](https://github.com/MinJung-Go/CompositionHelper/actions/runs/34342014271)、[IPA 构建](https://github.com/MinJung-Go/CompositionHelper/actions/runs/34342014342)；未签名 IPA SHA256 `ac303e34e8ecd23c38b1cd5860ca5d8d95677a8557ed8992a8d14fc5fe7cc5df` | 已编译、780 项核心检查、小屏/空状态/大字体编辑器截图；**不含新真人清单** |
| Android `4e284f7` | 本地 Debug APK SHA256 `9bbb06335d2b6a09fbd4ef71efb74560cdecd5c43b069f6e055bf93c70912af6`；37 项 JVM 通过，lint 0 错误/44 警告 | 包含真人清单；测试 APK 已编译，清单交互未执行 |
| iOS `7535b84` | 本地清单代码；780 项核心检查不覆盖 SwiftUI 新清单 | **尚无此清单版本的新 IPA 或 Xcode 编译结果** |

本地包相对 iOS 工作区位置：`build/ios-cloud/1d9a096/CompositionHelper.ipa`、`build/checklists/2026-09-10/CompositionHelper-android.apk`。这些目录被忽略，不随 Git 克隆下载；GitHub Artifacts 也有保留期限，应在失效前按项目交付方式归档。后续文档提交不改变上述二进制来源。

## 安装与更新

Android Debug：使用对应设备安装，见 [测试指南](TESTING.md)。更新必须考虑 package ID、签名证书与 versionCode；签名不一致不能直接覆盖。不要通过先卸载来默认解决安装问题，重要清单先导出。iOS 未签名 IPA 需要再签名，可参照 [iloader 指南](ILOADER_IOS_TESTING.md)。签名后的包与本表未签名包 SHA256 不同是正常现象。

回退先导出待保留记录，确认旧包版本、来源和签名兼容性；旧版本未必识别新增状态。不要承诺降级一定保留应用数据。调色副本在系统相册中，应用数据与相册照片是不同存储范围。

## 新一轮交付流程

1. 记录两个分支的完整提交号与未提交改动；共享规范分别提交到对应分支，避免混入另一端源码。
2. 按 [TESTING.md](TESTING.md) 编译与回归，保存结果；签名凭据不入库。
3. 对安装包计算 SHA256，记录构建配置、工具链、平台标识、是否签名、下载位置及保留期限。
4. 填写 [开发与真人验收表](CROSS_PLATFORM_ACCEPTANCE.md)，披露未统一项。
5. 经授权发布到目标远端或分发渠道；本地提交不代表远端已更新，文档齐全也不代表验收完成。

```bash
# Linux
sha256sum PATH_TO_PACKAGE
# macOS
shasum -a 256 PATH_TO_PACKAGE
```

Windows PowerShell 可用 `Get-FileHash -Algorithm SHA256 PATH_TO_PACKAGE`。将占位符替换为实际文件路径。

## 发布记录模板

```text
发布日期 / 发布人：
分支 / 完整提交号：
版本名 / 构建号 / applicationId 或 bundle ID：
工具链 / 构建配置：
包名 / 大小 / SHA256 / 签名状态：
下载位置 / 有效期：
自动检查及报告：
真人验收设备和结论：
已知问题 / 升级与回退说明：
```

正式发布前还需确定标识、版本递增与签名管理，并重新核查实际目标渠道要求；此仓库当前没有完成这些发布配置。
