# 双端实时构图检测清单

[需求与技术设计](../architecture/REALTIME_COMPOSITION.md)

| 平台 | 需求分支 | 目标分支 | 基线 |
| --- | --- | --- | --- |
| Android | `feat/realtime-composition-android` | `master` | `5a597c2` |
| iOS | `feat/realtime-composition-ios` | `ios` | `be29e08` |

- [x] 同步远端基线并保留旧分支。
- [x] 定义两端检测证据、刷新频率和切换条件。
- [x] 接入两端相机帧，覆盖候选评分与稳定逻辑。
- [ ] 完成 Android Gradle 构建、iOS Xcode 编译和两端安装包验证。
- [ ] 分别在真机上核对推荐延迟、预览流畅度、误报和提示跳变。
- [ ] 提交、推送需求分支并开 PR。

## 本分支验证 · 2026-11-07

- 修改的 Swift 文件通过语法解析检查。当前 Windows 主机缺少 Swift 和 Xcode，未运行 Swift 回归、模拟器编译或 IPA 构建。
- 在 macOS 上执行以下命令，再按[构建与测试运行指南](TESTING.md)完成模拟器编译、IPA 打包和相机真机验收。

```bash
swiftc Sources/Models/CompositionType.swift Sources/Models/AnalysisModels.swift \
  Sources/Camera/RealtimeComposition.swift Tests/RealtimeComposition/main.swift \
  -o "${TMPDIR:-/tmp}/composition-realtime-tests"
"${TMPDIR:-/tmp}/composition-realtime-tests"
```
## 合并前相机修复

- [x] iOS 仅在用户点选推荐时切换辅助线和画幅，成片使用当前预览画幅。
- [x] 会话队列管理启动和停止意图，页面退出后忽略迟到的授权回调。
- [ ] 真机验证黄金螺旋与正方形画幅、推荐消失、快速进出相机及权限回调。
