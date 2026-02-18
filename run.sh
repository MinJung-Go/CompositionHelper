#!/bin/bash

# CompositionHelper 运行脚本
# 使用 Xcode 构建并运行项目

set -e

echo "🚀 CompositionHelper 运行脚本"
echo "=================================="
echo ""

# 检查是否在 Mac 上
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "❌ 错误: 此脚本需要在 macOS 上运行"
    echo "请在 Mac 上使用此脚本"
    exit 1
fi

PROJECT_NAME="CompositionHelper"
SCHEME_NAME="CompositionHelper"

echo "📱 正在构建并运行项目..."
echo ""

# 使用 Xcode 构建并运行
xcodebuild \
    -project "$PROJECT_NAME.xcodeproj" \
    -scheme "$SCHEME_NAME" \
    -configuration Debug \
    -destination 'platform=iOS Simulator,name=iPhone 15' \
    build

echo ""
echo "✅ 构建完成！"
echo ""
echo "正在启动模拟器..."
echo ""

# 使用 simctl 启动应用
xcrun simctl boot "iPhone 15" 2>/dev/null || true

echo "🎯 打开项目进行运行..."
echo "在 Xcode 中点击 ▶️ 按钮运行应用"
echo ""
echo "或者使用以下命令直接打开 Xcode:"
echo "  open $PROJECT_NAME.xcodeproj"
