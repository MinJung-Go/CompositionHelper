# 文档索引与维护规则

更新：2026-09-10。此索引区分当前指南、验证证据和历史快照。代码实际行为与目标不一致时，应同时更新已知问题，不能只改文案宣称完成。

## 使用者

- [Android 安装与使用](ANDROID.md)、[iOS 安装与使用](IOS.md)。源码必须切换到对应分支。
- [Windows / iloader 安装指南](ILOADER_IOS_TESTING.md)。
- [构图说明](FEATURES.md)、[样片来源](SAMPLE_PHOTOS.md)、[真人拍摄清单](SHOOTING_CHECKLIST.md)。
- [数据与隐私](DATA_PRIVACY.md)。

## 开发与测试

- [AI 流式调色计划、实现与验收](AI_STREAMING.md)。

- [双端架构](ARCHITECTURE.md)：模块、数据流、状态与存储边界。
- [UI 与交互约定](DESIGN_SYSTEM.md)：统一目标及当前差异。
- [测试命令与环境](TESTING.md)、[开发/真人验收表](CROSS_PLATFORM_ACCEPTANCE.md)。
- [当前已知问题](KNOWN_ISSUES.md)、[版本及交付证据](RELEASES.md)。
- [拍摄清单项目标准](SHOOTING_CHECKLIST.json)：原生数组需要同步维护，当前不是运行时配置。

## 平台专题与历史材料

- [Android AI 调色说明](AI_COLOR.md)：当前状态与历史增补分开阅读。
- [Checklist 兼容入口](CHECKLIST.md)、[2026-09-08 Android 历史 Checklist](history/ANDROID_CHECKLIST_20260908.md)。
- [2026-09-08 Android review](REVIEW_20260908.md)、[构图产品调研](COMPOSITION_MARKET.md)、[图标说明](APP_ICON.md)。
- iOS 界面 review 在 `ios` 分支，Android 没有执行结果不能借用其截图结论。

## 维护规则

1. 架构、UI 约定、测试、隐私、已知问题、交付、安装指南、验收表和清单标准在两端保持同一份内容；索引的平台专题可以不同。
2. 共通行为改动同步更新两端文档；历史材料保留当时日期、提交与结果，不用旧的未勾选项表示当前状态。
3. 新验证记录必须含提交、环境、命令/步骤与证据；区分源码实现、测试编译、测试执行和真人通过。
4. 只修改文档时检查链接和描述，不因此宣称重新验证了 App；需要修改代码或构建配置的问题留在台账。
5. 本地提交与远端发布分开；没有推送的文档不会出现在 GitHub 克隆结果中。
