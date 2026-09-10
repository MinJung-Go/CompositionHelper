# 构图辅助产品调研 · 2026-09-08

依据厂商公开页面，未进行同场景实机效果排名。具体功能会随机型、系统版本和地区变化。

| 产品/能力 | 厂商描述 | 对本项目的启发 |
|---|---|---|
| Samsung Shot suggestions | 显示目标，引导用户对齐两个圆来调整构图 | 给出一个明确目标及对齐反馈，减少复杂线条 |
| Google Pixel Camera Coach | Gemini 分析场景，对取景、构图、光线和相机模式提出建议 | 将“构图类型推荐”升级为具体、简短的拍摄指引 |
| Apple Camera | 网格与水平仪辅助拍摄和校正倾斜 | 水平校准属于优先落地的基础能力 |
| OPPO Reno14 AI Editor 2.0 | 提供基于构图规则的更佳取景建议 | 拍后提供几个可比较的构图候选 |

来源：
- [Samsung Shot suggestions](https://www.samsung.com/uk/support/mobile-devices/how-to-use-shot-suggestions/)
- [Google Camera Coach](https://blog.google/products-and-platforms/devices/pixel/how-to-use-camera-coach/)
- [Apple 相机网格与水平仪](https://support.apple.com/guide/iphone/set-up-your-shot-iph3dc593597/26/ios/26)
- [OPPO Reno14](https://www.oppo.com/ng/smartphones/series-reno/reno14/)

## 建议的实施顺序（产品判断）

1. 先把取景框、成片裁剪、对齐提示与水平仪做可靠。
2. 根据场景挑选 2–3 个合适构图，而不是默认展示越来越多模板。
3. 人物检查头顶留白和视线方向；风景检查地平线；静物检查边缘截断和背景干扰。
4. 让多模态模型返回目标框、建议焦点与一条具体指令，叠加到准确映射的预览上；不在每帧调用云模型。
5. 拍后生成保持原图内容的候选裁剪，通过左右比较选择，不使用生成式扩图冒充原片构图。

没有一种构图规则普遍优于其他规则。相比新增第 20 种辅助线，更值得投入的是根据场景选择、解释和辅助实现合适构图。
