# 🚀 部署与发布指南

本文档说明如何将 CompositionHelper 应用部署到 GitHub、配置 CI/CD，以及发布到 App Store。

## 📋 目录

- [GitHub 部署](#github-部署)
- [CI/CD 配置](#cicd-配置)
- [本地开发](#本地开发)
- [App Store 发布](#app-store-发布)
- [版本管理](#版本管理)

---

## GitHub 部署

### 1. 创建 GitHub 仓库

#### 方式一：通过 GitHub CLI

```bash
# 安装 GitHub CLI (如果还没安装)
brew install gh

# 登录 GitHub
gh auth login

# 创建新仓库
gh repo create CompositionHelper --public --source=. --remote=origin --push
```

#### 方式二：通过 GitHub 网页界面

1. 访问 https://github.com/new
2. 填写仓库信息：
   - Repository name: `CompositionHelper`
   - Description: `智能摄影构图辅助 iOS 应用`
   - Public/Private: 选择 Public
3. 点击 "Create repository"
4. 复制仓库 URL

### 2. 推送到 GitHub

```bash
# 添加远程仓库
git remote add origin https://github.com/YOUR_USERNAME/CompositionHelper.git

# 或者使用 SSH
git remote add origin git@github.com:YOUR_USERNAME/CompositionHelper.git

# 推送代码
git push -u origin master

# 或者推送到 main 分支（如果使用 main）
git branch -M main
git push -u origin main
```

### 3. 设置仓库设置

#### 启用 GitHub Actions

1. 进入仓库 Settings → Actions
2. 勾选 "Allow all actions and reusable workflows"

#### 设置仓库主题

1. Settings → Appearance
2. 选择主题颜色

#### 添加仓库描述

1. Settings → General
2. Description: `智能摄影构图辅助 iOS 应用`
3. Website: 填写你的个人网站或博客
4. Topics: `ios`, `swiftui`, `photography`, `composition`, `vision-framework`

#### 启用 Issues 和 Pull Requests

Settings → Features:
- ✅ Issues
- ✅ Pull Requests
- ✅ Actions
- ✅ Projects (可选)
- ✅ Wiki (可选)

---

## CI/CD 配置

### GitHub Actions 工作流

项目已包含 `.github/workflows/ios-ci.yml`，提供以下功能：

- **代码检查**: SwiftLint
- **语法验证**: Swift 编译检查
- **文档验证**: MarkdownLint
- **自动构建**: 模拟 Xcode 构建流程

### 查看 CI 状态

1. 进入仓库的 Actions 标签页
2. 查看最近的工作流运行状态
3. 点击具体运行查看详细日志

### 添加自动标签

```bash
# 创建 .github/workflows/auto-tag.yml
name: Auto Tag
on:
  push:
    branches: [ master ]

jobs:
  tag:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions-ecosystem/action-push-tag@v1
        with:
          tag: v${{ github.run_number }}
          message: "Release v${{ github.run_number }}"
```

---

## 本地开发

### 环境配置

#### 1. 安装依赖

```bash
# 安装 SwiftLint (代码规范检查)
brew install swiftlint

# 安装 SwiftFormat (代码格式化)
brew install swiftformat
```

#### 2. 配置 Xcode

```bash
# 打开 Package.swift 或创建新项目
open Package.swift

# 或者使用 Xcode 创建新项目并导入文件
```

#### 3. 运行代码检查

```bash
# 运行 SwiftLint
swiftlint lint

# 自动修复一些问题
swiftlint lint --fix

# 格式化代码
swiftformat .
```

### 开发流程

1. **创建功能分支**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **进行开发**
   - 修改代码
   - 运行检查
   - 测试功能

3. **提交更改**
   ```bash
   git add .
   git commit -m "feat: add your feature"
   ```

4. **推送到 GitHub**
   ```bash
   git push origin feature/your-feature
   ```

5. **创建 Pull Request**
   - 在 GitHub 上创建 PR
   - 等待 CI 通过
   - 请求代码审查

---

## App Store 发布

### 1. 配置 Apple Developer 账户

#### 在 App Store Connect 创建应用

1. 登录 [App Store Connect](https://appstoreconnect.apple.com/)
2. 点击 "My Apps" → "+"
3. 填写应用信息：
   - Platform: iOS
   - Name: 构图辅助
   - Primary Language: 中文
   - Bundle ID: com.yourcompany.CompositionHelper
   - SKU: COMPOSITION-HELPER-001

4. 填写应用信息：
   - Category: 摄影与录像
   - Age Rating: 根据实际情况选择
   - URL: 你的网站 URL

### 2. 准备应用资料

#### 应用图标

需要以下尺寸的图标：
- 1024x1024 (App Store)
- 180x180 (iPhone @3x)
- 120x120 (iPhone @2x)
- 87x87 (iPad @3x)
- 80x80 (iPad @2x)

#### 截图

需要以下尺寸的截图：
- iPhone 6.7" (1290x2796)
- iPhone 6.5" (1242x2688)
- iPhone 5.5" (1242x2208)
- iPad Pro 12.9" (2048x2732)

### 3. 配置 Xcode 项目

#### 设置 Bundle Identifier

在 Xcode 中：
1. 选择项目 → Target
2. General → Bundle Identifier: `com.yourcompany.CompositionHelper`

#### 设置版本和构建号

- Version: `1.0.0`
- Build: `1` (每次提交增加)

#### 配置签名

1. 选择项目 → Target
2. Signing & Capabilities
3. Team: 选择你的开发者团队
4. 勾选 "Automatically manage signing"

#### 配置权限

Info.plist 中已包含：
- NSCameraUsageDescription
- NSPhotoLibraryUsageDescription
- NSPhotoLibraryAddUsageDescription

### 4. 构建和上传

#### 方法一：使用 Xcode Archive

1. Product → Archive
2. 等待构建完成
3. 在 Organizer 中选择 Archive
4. 点击 "Distribute App"
5. 选择 "App Store Connect"
6. 选择 "Automatically manage signing"
7. 上传

#### 方法二：使用命令行

```bash
# 构建 Archive
xcodebuild archive \
  -project CompositionHelper.xcodeproj \
  -scheme CompositionHelper \
  -archivePath build/CompositionHelper.xcarchive \
  -configuration Release

# 上传到 App Store Connect
xcrun altool --upload-app \
  --type ios \
  --file build/CompositionHelper.xcarchive/Products/Applications/CompositionHelper.app \
  --username YOUR_APPLE_ID \
  --password YOUR_APP_SPECIFIC_PASSWORD
```

#### 方法三：使用 fastlane

创建 `Fastfile`:

```ruby
lane :beta do
  increment_build_number
  build_app(scheme: "CompositionHelper")
  upload_to_testflight(skip_waiting_for_build_processing: true)
end

lane :release do
  increment_build_number
  build_app(scheme: "CompositionHelper")
  upload_to_app_store(
    submit_for_review: false,
    automatic_release: false,
    force: true
  )
end
```

运行：
```bash
# 测试版本
fastlane beta

# 正式发布
fastlane release
```

### 5. 提交审核

1. 在 App Store Connect 填写审核信息：
   - 应用描述
   - 关键词
   - 技术支持 URL
   - 营销 URL
   - 隐私政策 URL（如果需要）

2. 上传截图和图标

3. 填写审核说明（中文）

4. 点击 "Add for Review"

5. 等待审核（通常 1-3 天）

---

## 版本管理

### 版本号规范

采用语义化版本 (SemVer)：`MAJOR.MINOR.PATCH`

- **MAJOR**: 不兼容的 API 变更
- **MINOR**: 向下兼容的功能新增
- **PATCH**: 向下兼容的 bug 修复

示例：
- `1.0.0` - 初始版本
- `1.0.1` - Bug 修复
- `1.1.0` - 新增功能
- `2.0.0` - 重大更新

### 创建版本标签

```bash
# 创建标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送标签到 GitHub
git push origin v1.0.0

# 推送所有标签
git push origin --tags
```

### GitHub Release

1. 进入仓库的 Releases 页面
2. 点击 "Draft a new release"
3. 填写信息：
   - Tag version: `v1.0.0`
   - Release title: `Version 1.0.0`
   - Description: 版本更新说明
4. 上传构建产物（可选）
5. 点击 "Publish release"

---

## 监控与分析

### 集成 Firebase Analytics

1. 在 Firebase Console 创建项目
2. 添加 iOS 应用
3. 下载 `GoogleService-Info.plist`
4. 添加到 Xcode 项目
5. 安装 SDK：
   ```bash
   # Podfile
   pod 'Firebase/Analytics'
   
   # 安装
   pod install
   ```

### 集成 Crashlytics

```bash
# Podfile
pod 'Firebase/Crashlytics'

# 在 AppDelegate.swift
import Firebase
FirebaseApp.configure()
```

---

## 常见问题

### Q: GitHub Actions 失败怎么办？

**A:**
1. 查看 Actions 日志
2. 检查代码是否有语法错误
3. 确保依赖库版本兼容
4. 本地运行相同的命令测试

### Q: App Store 审核被拒怎么办？

**A:**
1. 仔细阅读拒绝原因
2. 修改相关问题
3. 重新提交
4. 如果认为错误，可以申诉

### Q: 如何处理 TestFlight 测试？

**A:**
1. 添加测试者邮箱到 App Store Connect
2. 上传 TestFlight 构建
3. 测试者接受邀请
4. 收集反馈

---

## 相关资源

- [Apple App Store 审核指南](https://developer.apple.com/app-store/review/guidelines/)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Fastlane 文档](https://docs.fastlane.tools/)
- [SwiftLint 文档](https://github.com/realm/SwiftLint)

---

**最后更新**: 2026-02-19
