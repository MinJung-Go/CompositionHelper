# GLM 调色测试指南

更新：2026-09-11。Android/master 与 iOS 使用同一请求约定。

## 配置与使用

1. 安装本次 GLM 适配构建；旧流式 Gemini 安装包不含此功能。
2. 在色彩工作室打开 AI 设置，默认服务为「智谱 GLM」，模型固定为 `glm-5.3-flash`。
3. 点击「获取 API Key」，按智谱官方指南注册/登录并创建自己的通用 API Key，回到应用填写。
4. 点击 AI 调色后，才会向智谱发送 JPEG 缩略图与调色提示词。模型思考期间继续显示等待状态；只在正式内容含有完整、合法的参数组后更新临时预览。
5. 最终方案完成后，检查左右对比，再保存副本。取消或失败应恢复请求前的已提交结果。

Key 当前仍在内存保存：Android 为本次进程，iOS 为当前编辑器；本机安全持久化属于后续任务。切换服务商会清空输入框，防止误发 Key。可以手动切回 Gemini，不会因 GLM 失败自动切服务或重复扣费。

本适配使用通用模型 API；不使用 Coding Plan 专用端点，不承诺 Coding Plan 订阅能抵扣此处调用。模型费用、可用权限和额度以用户账号控制台为准，GLM-5.3-Flash 不标注为永久免费。

## 请求约定

- 固定端点：`https://open.bigmodel.cn/api/paas/v4/chat/completions`；Bearer 请求头鉴权，禁止重定向。
- `model: glm-5.3-flash`，`thinking.type: enabled`，`thinking.clear_thinking: false`，`reasoning_effort: max`。
- `temperature: 1`、`top_p: 0.95`、`stream: true`、`response_format.type: json_object`。
- `max_tokens: 32768` 是本应用输出预算，不是模型上限。请求总时限 360 秒，读取等待时限 300 秒；SSE 总响应最多 2 MB，配方最多 64 KB。
- `image_url.url` 使用 JPEG Base64 Data URL；图像不需要上传至公开图床。
- 只解析 `choices[0].delta.content`；`reasoning_content` 不显示、不记录、不参与配方解析。空 choices 的用量事件允许忽略。
- 必须收到 `finish_reason: stop` 和合法完整配方才提交；`[DONE]` 不能替代 stop。截断、内容过滤、工具调用或错误事件均不保存临时结果。

深度思考保持开启，首张预览需要等模型开始输出正式参数；不承诺比 Gemini 更快。当前提示词与本地像素引擎保持一致。

## 真人验收记录

请至少测试室内、人像、夜景、风景各一张，记录：版本/提交、机型、首张预览耗时、总耗时、肤色/高光是否自然、保存副本是否与预览一致。另测：

- 思考中取消、出现预览后取消，均恢复原结果。
- 错误 Key、无额度、断网后可重试，且不会保存半成品。
- GLM/Gemini 切换后填写对应服务 Key，上传告知一致。
- 用量尾事件、中文与分片参数由自动化协议测试覆盖；真实 API 质量与账户权限需真机测试。

未使用用户 Key 发起真实付费请求，不能将编译及协议测试通过等同于端到端通过。

## 官方依据

- [GLM-5.3-Flash 模型与参数](https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5.3-flash)
- [注册与获取 Key](https://docs.bigmodel.cn/cn/guide/start/quick-start)
- [通用 HTTP API](https://docs.bigmodel.cn/cn/guide/develop/http/introduction)
