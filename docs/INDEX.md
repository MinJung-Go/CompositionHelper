# 文档索引与维护规则

更新：2026-09-10。两个分支使用同一分类；平台专属文档保留在对应分支。`master` 是 Android，`ios` 是 iOS。各文档中的实现、规划和验证状态保持独立，不因移动目录改变结论。

## 快速入口

- 产品方向：[核心买断 + 自带 Key](product/PRODUCT_STRATEGY.md)。
- 技术设计：[双端架构](architecture/ARCHITECTURE.md)、[流式调色](architecture/AI_STREAMING.md)、[Key 首次配置设计](architecture/BYOK_ONBOARDING.md)。
- 安装使用：[Android](guides/ANDROID.md)、[iOS](guides/IOS.md)、[Windows / iloader](guides/ILOADER_IOS_TESTING.md)。
- 两份清单：[开发与测试验收](testing/CROSS_PLATFORM_ACCEPTANCE.md)供开发测试；[应用内拍摄清单说明](guides/SHOOTING_CHECKLIST.md)供使用者实际检查照片。
- 当前结果：[交付记录](testing/RELEASES.md)、[已知问题](testing/KNOWN_ISSUES.md)。

## 分类目录

| 目录 | 用途 |
|---|---|
| `product/` | 产品定位、功能说明与产品调研 |
| `architecture/` | 架构、UI 约定、技术实现与待实施设计 |
| `guides/` | 安装、构建入门与使用操作指南 |
| `testing/` | 测试命令、开发验收、问题台账与构建交付 |
| `reference/` | 数据隐私、素材许可、图标与清单数据标准 |
| `history/` | 按当时日期和提交保留的审查、旧检查记录 |

## 产品：product

- [产品定位与买断路线](product/PRODUCT_STRATEGY.md)：用户确认的方向；支付与 Key 持久化尚待实现。
- [构图功能说明](product/FEATURES.md)：概念和使用建议，不能当作全部算法已实现的证明。

## 架构：architecture

- [双端技术架构](architecture/ARCHITECTURE.md)：区分现有实现和后续买断/BYOK 设计。
- [UI 与交互约定](architecture/DESIGN_SYSTEM.md)：统一目标与当前差异。
- [AI 流式调色](architecture/AI_STREAMING.md)：协议、临时预览、取消与验证边界。
- [Key 首次配置设计](architecture/BYOK_ONBOARDING.md)：获取、主动粘贴、安全保存与错误处理，待实施。
- [iOS 调色实现](architecture/IOS_COLOR.md)：iOS 专属实现与验证说明。

## 使用指南：guides

- [Android 安装、构建与使用](guides/ANDROID.md)。
- [iOS 安装、构建与使用](guides/IOS.md)。
- [Windows 使用 iloader](guides/ILOADER_IOS_TESTING.md)。
- [应用内拍摄检查清单](guides/SHOOTING_CHECKLIST.md)：真人操作、记录、导出和重置。

## 测试与交付：testing

- [构建与测试运行指南](testing/TESTING.md)：完整命令及执行环境。
- [双端功能与测试验收清单](testing/CROSS_PLATFORM_ACCEPTANCE.md)：开发、自测和真人验收状态。
- [已知问题](testing/KNOWN_ISSUES.md)：当前缺口与复测条件。
- [版本及交付记录](testing/RELEASES.md)：提交、安装包、校验值与验证证据。

## 参考资料：reference

- [数据与隐私](reference/DATA_PRIVACY.md)：当前实现的数据去向。
- [内置样片与许可](reference/SAMPLE_PHOTOS.md)。
- [拍摄清单数据标准](reference/SHOOTING_CHECKLIST.json)：两端原生数组的共同标准，不是运行时加载配置。

## 历史记录：history

历史文件保留原日期与结果，仅修正链接；当前发布状态应查看 `testing/`。

- [iOS 色彩工作室 review · 2026-09-09](history/REVIEW_IOS_20260909.md)。

## 平台差异

本分支保留 iOS 调色说明和界面 review；Android AI 调色说明、图标制作、产品调研与历史记录见 `master` 分支对应目录。不能用 Android 单测代替 iOS 真机验收。

## 维护规则

1. 新文档按用途进入上述目录，`docs/` 根目录只保留此索引。文件名保持稳定，移动时同时修复 README、根目录指南及文档间引用。
2. 共通架构、产品方向、测试、隐私和清单标准在两分支保留同路径同内容；平台实现与历史报告允许不同，索引必须标明所属平台。
3. 已确认产品方向、尚未实施方案、源码实现、测试编译、测试执行和真人通过分别描述。
4. 旧检查记录进入 `history/`，不以旧未勾选项目覆盖当前台账；新证据记录日期、提交、环境和范围。
5. 文档中的命令和反引号源码/产物路径默认相对所属分支仓库根目录；Markdown 链接相对文档本身。编译产物和历史截图可能受归档/保留期限限制。
6. 文档整理只检查文件和链接，不代表重新构建或验收 App。本地提交和远端公开分别记录。
