# 贡献指南

感谢你考虑为 CompositionHelper 项目做出贡献！我们欢迎所有形式的贡献。

## 🤝 如何贡献

### 报告问题

如果你发现了 bug 或有功能建议：

1. 检查 [Issues](https://github.com/yourusername/CompositionHelper/issues) 确认问题未被报告
2. 创建新的 Issue，使用清晰的标题
3. 详细描述问题或功能建议
4. 提供复现步骤（针对 bug）
5. 附上截图或录屏（如果适用）

### 提交代码

#### 开发流程

1. **Fork 仓库**
   ```bash
   # 在 GitHub 页面点击 Fork 按钮
   ```

2. **克隆你的 fork**
   ```bash
   git clone https://github.com/yourusername/CompositionHelper.git
   cd CompositionHelper
   ```

3. **创建特性分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

4. **进行开发**
   - 遵循代码风格规范
   - 添加适当的注释
   - 编写测试（如果需要）
   - 确保代码通过编译

5. **提交更改**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   # 或
   git commit -m "fix: fix bug description"
   ```

6. **推送到你的 fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **创建 Pull Request**
   - 在 GitHub 页面创建 PR
   - 填写 PR 模板
   - 等待代码审查

#### 提交信息规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

- `feat:` 新功能
- `fix:` 修复 bug
- `docs:` 文档更新
- `style:` 代码格式（不影响功能）
- `refactor:` 重构
- `perf:` 性能优化
- `test:` 测试相关
- `chore:` 构建/工具相关

示例：
```
feat: add photo cropping functionality
fix: improve analysis accuracy for portrait images
docs: update README with new features
```

### 文档改进

如果你只是想改进文档：

1. 找到需要改进的文档
2. 直接在 GitHub 上编辑文件
3. 提交更改，创建 Pull Request

## 📝 代码规范

### Swift 代码风格

- 遵循 [Swift API Design Guidelines](https://swift.org/documentation/api-design-guidelines/)
- 使用 4 个空格缩进，不要用 tab
- 行宽建议不超过 120 字符
- 使用有意义的变量和函数名
- 添加适当的注释解释复杂逻辑

### SwiftUI 最佳实践

- 使用 `@State`、`@Binding` 等属性包装器
- 将大视图拆分为小组件
- 使用 `@ViewBuilder` 提高可读性
- 避免在 View 中进行复杂计算

### 示例代码

```swift
// ✅ 好的代码
struct UserProfileView: View {
    @State private var userName: String = ""
    @Binding var isLoggedIn: Bool
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Welcome")
                .font(.largeTitle)
            
            TextField("Username", text: $userName)
                .textFieldStyle(.roundedBorder)
        }
        .padding()
    }
}

// ❌ 不好的代码
struct V: View {
    @State var s: String = ""
    var b: Binding<Bool>
    var body: some View {VStack{Text("Welcome").font(.largeTitle);TextField("Username",text:$s)}}}
```

## 🧪 测试

### 单元测试

```swift
import XCTest
@testable import CompositionHelper

class CompositionAnalyzerTests: XCTestCase {
    func testRuleOfThirdsDetection() {
        // 测试三分法检测
        // ...
    }
}
```

### UI 测试

使用 XCUITest 框架进行 UI 测试。

## 📋 Pull Request 检查清单

在提交 PR 之前，请确保：

- [ ] 代码符合项目风格规范
- [ ] 添加了适当的注释
- [ ] 没有编译警告
- [ ] 所有测试通过
- [ ] 更新了相关文档
- [ ] 提交信息清晰且符合规范
- [ ] PR 描述详细说明了更改内容

## 🎯 功能贡献方向

我们特别欢迎以下方面的贡献：

1. **新的构图类型** - 实现更多构图算法
2. **分析准确率提升** - 优化 Vision Framework 使用或集成 Core ML
3. **UI/UX 改进** - 提升用户体验
4. **性能优化** - 加快分析速度
5. **文档完善** - 中英文文档翻译
6. **测试覆盖** - 增加单元测试和 UI 测试
7. **国际化** - 添加多语言支持

## 💬 交流与讨论

- GitHub Issues: 报告问题或提出建议
- GitHub Discussions: 讨论功能和设计
- Email: your.email@example.com

## 📜 行为准则

- 尊重所有贡献者
- 建设性的反馈
- 避免争吵和个人攻击
- 专注于代码和功能

## 🏆 贡献者

感谢所有贡献者！

<!-- 自动生成贡献者列表 -->

## ❓ 遇到问题？

如果在贡献过程中遇到问题：

1. 查看 [文档](README.md)
2. 搜索 [Issues](https://github.com/yourusername/CompositionHelper/issues)
3. 创建新的 Issue 提问

---

再次感谢你的贡献！🎉
