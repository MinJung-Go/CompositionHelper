# 文档索引与维护规则

更新：2026-09-10。此索引区分当前指南、验证证据和历史快照。代码实际行为与目标不一致时，应同时更新已知问题，不能只改文案宣称完成。

## 使用者

- [Android 安装与使用](ANDROID.md)、[iOS 安装与使用](IOS.md)。源码必须切换到对应分支。
- [Windows / iloader 安装指南](ILOADER_IOS_TESTING.md)。
- [构图说明](FEATURES.md)、[样片来源](SAMPLE_PHOTOS.md)、[真人拍摄清单](SHOOTING_CHECKLIST.md)。
- [数据与隐私](DATA_PRIVACY.md)。

## 产品与商业化

- [自带 Key 的最简首次配置](BYOK_ONBOARDING.md)：官方获取、主动粘贴、安全保存与错误提示。

- [产品定位、收费结构与分阶段路线](PRODUCT_STRATEGY.md)：买断与自带 Key 方向已确认，支付/安全存储/引导待实施。
- [技术架构](ARCHITECTURE.md)：区分当前实现与买断、设备安全存储目标设计。

## 开发与测试

- [AI 流式调色计划、实现与验收](AI_STREAMING.md)。

- [双端架构](ARCHITECTURE.md)：模块、数据流、状态与存储边界。
- [UI 与交互约定](DESIGN_SYSTEM.md)：统一目标及当前差异。
- [测试命令与环境](TESTING.md)、[开发/真人验收表](CROSS_PLATFORM_ACCEPTANCE.md)。
- [当前已知问题](KNOWN_ISSUES.md)、[版本及交付证据](RELEASES.md)。
- [拍摄清单项目标准](SHOOTING_CHECKLIST.json)：原生数组需要同步维护，当前不是运行时配置。

## 平台专题与历史材料

- [iOS 调色实现与验证范围](IOS_COLOR.md)。
- [2026-09-09 iOS 界面 review](REVIEW_IOS_20260909.md)：只覆盖报告中提交，不覆盖后来清单。
- Android 的调色历史、图标和产品调研资料在 `master` 分支文档中；本分支不复制 Android 历史结论充当 iOS 验证。

## 维护规则

1. 架构、UI 约定、测试、隐私、已知问题、交付、安装指南、验收表和清单标准在两端保持同一份内容；索引的平台专题可以不同。
2. 共通行为改动同步更新两端文档；历史材料保留当时日期、提交与结果，不用旧的未勾选项表示当前状态。
3. 新验证记录必须含提交、环境、命令/步骤与证据；区分源码实现、测试编译、测试执行和真人通过。
4. 只修改文档时检查链接和描述，不因此宣称重新验证了 App；需要修改代码或构建配置的问题留在台账。
5. 本地提交与远端发布分开；没有推送的文档不会出现在 GitHub 克隆结果中。
