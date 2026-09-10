# CompositionHelper iOS

原生摄影构图与拍后调色工具。本分支为 `ios`；另一端见 [master 分支](https://github.com/MinJung-Go/CompositionHelper/tree/master)。两端各自实现，当前尚未完全统一。

## 功能与状态

- 实时相机、19 种构图辅助线、端侧主体检测与构图建议。建议分数不是专业审美评分。
- 原图导入、四张内置样片、Gemini 拍后调色及本地手动精调、前后对比、撤销、保存副本。
- 本轮新增真人拍摄清单：12 项、人工状态、备注、本机保存、导出和重置。
- 编译与自动检查不替代真人验收；清单代码和旧安装包不能混为同一版本。状态见 [交付记录](docs/RELEASES.md)。

## 开始使用

```bash
git clone -b ios https://github.com/MinJung-Go/CompositionHelper.git
cd CompositionHelper
```

App 最低 iOS 16，使用 Swift 5.9+ 对应的 Xcode 工具链：

```bash
open CompositionHelper.xcodeproj
```

完整安装见 [iOS 指南](docs/IOS.md)。Windows 测试见 [iloader 指南](docs/ILOADER_IOS_TESTING.md)。当前默认入口尚未接通独立相册构图分析视图；调色页自身可选照片，详情见 [已知问题](docs/KNOWN_ISSUES.md)。

## 产品方向

建议以轻量摄影工具为定位：本地 Pro 买断，可选云端 AI 次数包；支付和业务后端尚未实施。详见 [产品定位与路线](docs/PRODUCT_STRATEGY.md) 和 [技术架构](docs/ARCHITECTURE.md)。

## 文档导航

- [全部文档与维护规则](docs/INDEX.md)
- [双端架构](docs/ARCHITECTURE.md) · [UI 与交互约定](docs/DESIGN_SYSTEM.md)
- [构图功能说明](docs/FEATURES.md) · [参考照片与许可](docs/SAMPLE_PHOTOS.md)
- [开发/测试验收表](docs/CROSS_PLATFORM_ACCEPTANCE.md) · [真人拍摄清单](docs/SHOOTING_CHECKLIST.md)
- [测试运行指南](docs/TESTING.md) · [当前已知问题](docs/KNOWN_ISSUES.md)
- [数据与隐私](docs/DATA_PRIVACY.md) · [版本与交付](docs/RELEASES.md)

## 许可

代码许可见 [LICENSE](LICENSE)。内置照片作者和许可单独记录在 [SAMPLE_PHOTOS.md](docs/SAMPLE_PHOTOS.md)。
