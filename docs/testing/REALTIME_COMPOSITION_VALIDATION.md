# 实时构图识别验证报告

测试日期 2026-11-08。验证分支 `docs/realtime-composition-validation`，目标分支 `master`，被测 Android 提交 `ffb7af9`。同期 iOS 基线为 `eeb126a`。本轮比较候选方案，验证代码运行在 Windows 桌面。

## 结论

- **当前对角线方案未通过实拍准确性验证。** 四种组合在 3 段人工标注的正例上均未提示。M-LSD 多出 1 段持续误提示，沿用筛选后，误提示从 11 个采样时点缩短到 2 个。当前证据不足以支持用 M-LSD 替换 LSD。
- **BlazeFace 的双眼中点值得继续验证。** 在成功检出的近景标注帧上，双眼中点匹配三分法 5/5，框中心匹配 0/5。模型漏掉整段极近景，误检过浪花，并在合影里切换最高置信度人脸。接入前需要验证眼睛可见性、远近景回退和主角跟踪。

## 测试范围

| 检查项 | 覆盖情况 |
| --- | --- |
| Android 对角线几何评分与稳定逻辑 | 编译 `ffb7af9` 的 `LightweightAnalyzer` 和 `RecommendationStability`，直接回放实拍录像 |
| 线段候选 | OpenCV LSD 和 M-LSD Tiny，分别比较加入同一证据筛选前后的结果 |
| 人像锚点 | BlazeFace 短距模型，验证双眼中点、极近景、合影、局部脸和无脸画面 |
| 原生相机端到端 | 本机未检测到连接的真机。CameraX Y 平面、ML Kit 主体选择、iOS Vision、预览流畅度、耗电与热表现待验 |

按基线提交核对已有 GitHub Actions。Android CI、iOS CI 和 iOS IPA 打包均成功，覆盖各自的基线构建检查。iOS 的 Vision 候选与 Android LSD 来源不同，下面的回放指标只适用于本轮测试组合。 [Android CI](https://github.com/MinJung-Go/CompositionHelper/actions/runs/42391498264) · [iOS CI](https://github.com/MinJung-Go/CompositionHelper/actions/runs/42391527546) · [iOS IPA](https://github.com/MinJung-Go/CompositionHelper/actions/runs/42391527543)

回放每段视频的 1.0–4.8 秒，每 200 ms 取一帧，共 20 帧。取原视频中心正方形，与 Android 三分法的预览比例一致。运行前按画面主结构做单人标注。明显的贯穿型斜向结构计为正例，水平线、透视汇聚、重复窗格和孤立细线计为负例。边界弯曲或随镜头变化、难以给整段统一定性的素材单独记录。同一视频内的帧高度相关，独立样本量以视频数为准。

## 1. 对角线回放

OpenCV 4.12.0 LSD 使用从录像解码转换的灰度，缩至最长边 320。M-LSD 使用官方 Tiny 320 FP32 TFLite 权重，候选分数大于 0.10 且向量距离大于 20。所有候选都使用现版 Kotlin 几何评分。证据筛选沿用首轮固定参数，要求至少 62% 的有效采样在线段两侧形成同向、至少 18 灰度级的亮度差，发现至少四组近似平行的长边时否决候选。每个方案将每帧最高分输入 Android 的两帧稳定逻辑，每段录像独立初始化。回放中对角线单独竞争，未提供 ML Kit 主体。 [Android 实时构图设计](../architecture/REALTIME_COMPOSITION.md) · [M-LSD 官方实现和权重](https://github.com/navervision/mlsd)

### 实拍结果

| 方案 | 正例单帧检出 / 60 | 正例稳定提示 / 60 | 负例单帧误判 / 200 | 负例稳定误提示 / 200 | 有稳定误提示的负例 / 10 段 |
| --- | ---: | ---: | ---: | ---: | ---: |
| OpenCV LSD | 0 | 0 | 0 | 0 | 0 |
| OpenCV LSD + 筛选 | 0 | 0 | 0 | 0 | 0 |
| M-LSD Tiny | 0 | 0 | 12 | 11 | 1 |
| M-LSD Tiny + 筛选 | 0 | 0 | 5 | 2 | 1 |

所有四个方案均漏掉 3/3 段正例。两段边界样本各 20 帧，排除在上表之外。误提示帧计数以每 200 ms 的回放采样为准。

| 录像 | 人工标注 | 稳定后结果 |
| --- | --- | --- |
| [工业铁路](https://www.pexels.com/video/aerial-footage-of-an-industrial-train-by-the-town-14143463/) | 正例，铁路走廊向右上贯穿 | 四组均静默 |
| [城市跨线桥](https://www.pexels.com/video/aerial-view-of-urban-bridge-and-train-tracks-34461084/) | 正例，道路向右下贯穿 | 四组均静默 |
| [巴塞罗那车道](https://www.pexels.com/video/aerial-view-of-busy-urban-traffic-in-barcelona-36545194/) | 正例，主车道向右上贯穿 | 四组均静默 |
| [居中林间道路](https://www.pexels.com/video/an-empty-road-in-between-pine-trees-11180777/) | 负例，两侧边缘向中心消失点汇聚 | LSD 两组均静默。M-LSD 从 2.8 秒误提示到回放结束。加入筛选后只在 3.2、3.4 秒误提示 |
| 其余九段负例 | [缓斜铁路](https://www.pexels.com/video/aerial-view-of-a-train-on-the-tracks-27468746/)、[纵向铁路与横向公路](https://www.pexels.com/video/aerial-view-of-rural-highway-and-rail-intersection-33336362/)、[扶梯](https://www.pexels.com/video/using-an-escalator-going-up-a-building-floor-4088488/)、[岸边浪花](https://www.pexels.com/video/tranquil-sunset-waves-by-the-shoreline-32599724/)、[窗格](https://www.pexels.com/video/glass-windows-on-a-high-rise-building-7065828/)、[日间电线](https://www.pexels.com/video/power-lines-and-rolling-clouds-in-open-field-37209237/)、[夜间电线](https://www.pexels.com/video/video-footage-of-an-electric-post-and-power-lines-5766693/)、[横向小桥](https://www.pexels.com/video/view-from-above-of-bridge-across-river-10142276/)、[水平海面](https://www.pexels.com/video/drone-footage-of-beach-waves-8678057/) | 四组均静默 |
| [弯曲海岸](https://www.pexels.com/video/drone-footage-a-beach-shoreline-4206989/) | 边界样本，移动的浪边沿斜向延伸 | 四组均静默 |
| [旋转中的楼体与阴影](https://www.pexels.com/video/drone-shot-of-a-city-in-grayscale-12698594/) | 边界样本，多条边缘竞争 | LSD 加筛选前后均提示 2/20 帧、各出现 1 次。M-LSD 从 13/20 降到 7/20 帧，提示出现次数从 1 次增至 2 次 |

这批录像的桌面线段提取 P95 为 LSD **23.84 ms**、M-LSD **103.09 ms**。计时包含检测器的预处理和候选解码，每帧测一次。录像解码、JVM 评分、筛选和相机渲染另计。

### 漏检定位

从三段正例各抽 5 帧，拆查现版几何门槛。长度条件要求两轴跨度各至少 0.24，且总跨度至少 0.38 条画幅对角线。位置条件要求两个端点都落在同一条画幅对角线的允许带内，最大归一化偏差为 0.12。15 帧中，LSD 在 3 帧有足够长的候选，M-LSD 在 12 帧有足够长的候选。这些长候选的最小偏差分别是 0.2195 和 0.1344，均越出允许带。逐帧拆查与 Kotlin 原规则输出一致。

另从两段竖向或横向结构的实拍录像各抽 5 帧，旋转 45° 后裁切，作为已对齐画幅对角线的受控样本。桥面上 LSD 和 M-LSD 均通过 5/5 帧的几何门槛。雪林道路上 LSD 通过 0/5，M-LSD 通过 5/5。旋转控制单独统计。 [俯视桥面](https://www.pexels.com/video/aerial-view-of-bridge-over-serene-lake-33256865/) · [雪林道路](https://www.pexels.com/video/directly-above-view-of-road-through-winter-forest-11124065/)

后续应先把「贴近画幅对角线」「贯穿画面的斜向主结构」「透视引导线」分开标注，再确定提示范围。直接放宽位置门槛可能放过居中道路这类干扰。筛选的收益目前只在部分负例上复现，三个主结构漏例在筛选前就已被几何门槛排除。

### 合成图对照

| 方案 | 正例检出 / 36 | 负例误报 / 36 |
| --- | ---: | ---: |
| OpenCV LSD | 30 | 12 |
| OpenCV LSD + 筛选 | 29 | 0 |
| M-LSD Tiny | 36 | 7 |
| M-LSD Tiny + 筛选 | 35 | 0 |

合成图结果来自同日首轮离线压力测试。实拍回放沿用门槛和评分，结果独立统计。

## 2. 人像回放

用 MediaPipe Tasks 0.10.35 的 IMAGE 模式和 BlazeFace 短距 v1 FP16 权重，检测阈值 0.50。取置信度最高的人脸，分别把同一张检测框和双眼中点传给现版 Kotlin 规则。另对两段正脸视频各预先目测标注 5 帧双眼中点，用于计算锚点误差和构图类型的一致性。合影按左、中、右位置记录排序变化。 [MediaPipe 人脸检测](https://developers.google.com/edge/mediapipe/solutions/vision/face_detector) · [官方模型权重](https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite)

| 录像 | 标注 | 实测 |
| --- | --- | --- |
| [近景正脸](https://www.pexels.com/video/man-looking-at-the-camera-5319070/) | 单人，双眼可见，靠近左边 | 20/20 帧检出，每帧一个框 |
| [极近景正脸](https://www.pexels.com/video/young-man-looking-at-camera-headshot-7350235/) | 单人，双眼可见，额头和下巴被裁 | 0/20 帧检出 |
| [侧脸局部](https://www.pexels.com/video/close-up-of-a-person-s-face-7297902/) | 鼻口可见，眼睛出画 | 5/20 帧仍输出人脸和双眼关键点，其中 4 帧连续。该段单独统计 |
| [三人合影](https://www.pexels.com/video/people-saying-hello-8684626/) | 三张脸 | 20/20 帧均检出三张脸，最高置信度在右侧和中间人脸之间切换 3 次 |
| [岸边浪花](https://www.pexels.com/video/tranquil-sunset-waves-by-the-shoreline-32599724/) | 无脸 | 连续 3/20 帧误检，置信度 0.534–0.579，本段误检锚点均未触发 Kotlin 的居中或三分法 |
| [玻璃窗格](https://www.pexels.com/video/glass-windows-on-a-high-rise-building-7065828/) | 无脸 | 0/20 帧误检 |

两段正脸的 10 个标注帧中仅有 5 帧检出，均来自近景正脸一段。按已检出的 5 帧配对计算，双眼中点误差中位数为 **1.79% 画宽**，人脸框中心为 **20.45% 画宽**。误差是二维欧氏距离除以画宽，包含目测标注误差。人工锚点在这 5 帧均对应三分法，模型双眼中点与之匹配 5/5，框中心匹配 0/5，其中 4 帧判为居中，1 帧静默。把极近景漏检一并计入，则双眼锚点是 5/10 帧成功输出匹配的构图类型。

首轮合成变体中，脸宽约 5% 或 8.5% 的 4 帧均漏检。本轮又复现极近景漏检、无脸误检和合影排序切换。模型可用于有可靠双眼证据的近景人像候选。进入实现验证时应独立检验双眼是否可见，并保持同一主体的跨帧关联，避免把置信度最高者逐帧当作主角。Python 运行时日志出现 Clearcut 连接失败。该 SDK 的隐私说明区分端侧输入处理和使用指标，移动端所选运行时的实际出站行为仍待测。 [MediaPipe Tasks 隐私说明](https://developers.google.com/edge/mediapipe/solutions/privacy)

## 3. 复跑与验收

- [x] 固定录像区间、人工标签和候选参数，比较单帧与稳定后提示。
- [x] 复用 Android 基线 Kotlin 评分和稳定逻辑，记录输入校验值、逐帧结果和模型版本。
- [ ] 扩充独立实拍样本，加入低对比对角线、近远景、遮挡、手持运动和明确的主体切换。由第二人复核标签。
- [ ] 在 Android、iOS 真机上验证主体与线段竞争、相机画幅、提示首次出现与撤销延迟、预览帧率及温升。

离线环境为 Windows 11、Intel Core Ultra 7 265K、Python 3.13.11、Java 17、Kotlin 1.8.20、OpenCV 4.12.0 和 LiteRT 2.3.0。线段提取使用单线程。逐帧结果、标签、输入校验值和复跑脚本另存于本地 `artifacts/realtime-composition-validation-2026-11-08/evidence.zip`。复跑时按素材链接下载原片至测试脚本目录，使用视频 ID 命名。录像来自 Pexels。 [Pexels 素材许可](https://www.pexels.com/license/)

| 模型 | SHA-256 |
| --- | --- |
| `M-LSD_320_tiny_fp32.tflite` | `b17de2b4b4754d563c47b29b4132aceb39d40d079c0b9154ec0932b5292ec11f` |
| `blaze_face_short_range.tflite` | `b4578f35940bf5a1a655214a1cce5cab13eba73c1297cd78e1a04c2380b0152f` |
