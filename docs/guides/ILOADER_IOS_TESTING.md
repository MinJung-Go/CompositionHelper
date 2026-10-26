# Windows 使用 iloader 测试 iOS 安装包

更新：2026-09-10。适用于没有 Mac、希望在自己的 iPhone 上测试项目 IPA 的使用者。**目前没有收到可追溯的 iloader 成功安装验收记录，以下为操作指南，不是成功保证。** 项目已有未签名 IPA 构建不等于已在手机安装。

## 1. 准备

从 [iloader 官网](https://iloader.app/) 选择 Windows 桌面 MSI；官网说明它免费、支持导入 IPA，Windows 需要安装 iTunes。沿用已能识别 iPhone 的驱动环境，先确认连接，不把卸载所有 Apple 组件作为固定前置步骤。

准备本项目的 iPhone IPA、数据线、自己的 Apple 账号及验证方式。CompositionHelper 最低 iOS 16；不要把其他安装工具的最低系统要求当成本 App 的要求。包版本和校验值见 [交付记录](../testing/RELEASES.md)。

## 2. 安装逻辑

```mermaid
flowchart LR
    IPA[未签名 IPA] --> Login[iloader 账号认证]
    Login --> Sign[开发证书和描述文件签名]
    Sign --> USB[传输到 iPhone 安装]
    USB --> Trust[按系统提示信任并启用开发者模式]
    Trust --> Test[打开 App 实际验收]
```

iloader 官网说明其流程会取得 anisette 信息、完成 Apple 认证、获取证书/描述文件后签名安装；仍依赖 Apple 认证成功，不保证解决之前的 `-22410`。它处理的是安装凭据，和 App 的 Gemini API Key 无关。[流程来源](https://iloader.app/)

## 3. 执行顺序

1. 手机解锁后用数据线连接 Windows，按手机提示信任电脑。
2. 打开 iloader，确认可以选择这台 iPhone。
3. 在工具内完成自己的 Apple 账号与双重认证；不要在项目文档或聊天记录中保存密码、验证码。
4. 使用导入 IPA 的功能选择 `CompositionHelper.ipa`，等待签名与安装结果，保留失败阶段及错误码。
5. 按 iPhone 实际提示完成信任/开发者模式设置，再启动 App；安装完成提示不能替代启动验证。
6. 从相机清单入口按项目操作，并填写 [软件验收表](../testing/CROSS_PLATFORM_ACCEPTANCE.md)。只有包含新清单的版本才有这个入口；旧 `1d9a096` IPA 没有。

这里采用直接安装项目 IPA 的路径。SideStore 是另外一种后续使用方式，它自己的安装/刷新前置要求不能套用为本流程的固定要求；如果选择 SideStore，请遵循它的[官方前置说明](https://docs.sidestore.io/docs/installation/prerequisites)。

## 4. 失败时收集什么

| 停在何处 | 下一步核对 |
|---|---|
| 找不到设备 | 解锁、数据线、信任提示及已安装的设备组件 |
| 账号认证 / 获取 team 失败 | 记录同一账号在工具中的完整错误阶段，勿据此直接认定 IPA 错误 |
| 签名失败 | 记录证书、App ID、描述文件相关信息并按工具提示排查 |
| 已安装但打不开 | 核对系统提示、信任、签名有效期与项目最低系统版本 |
| App 内功能失败 | 记录 App 提交版本、操作和照片条件，按验收用例复现 |

不要连续更换多套工具和配置而不记录结果。免费安装工具不代表签名永久有效，也不代表 Gemini API 免费；签名到期后需按实际工具提示重新签名。安装更新时优先保持同一账号和应用标识，并先导出清单记录。
