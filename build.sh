#!/bin/bash

# CompositionHelper iOS 项目编译脚本
# 作者: CompositionHelper Team
# 日期: 2026-02-19

set -e  # 遇到错误时退出

echo "🚀 CompositionHelper iOS 编译脚本"
echo "=================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否在 Mac 上
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo -e "${RED}❌ 错误: 此脚本需要在 macOS 上运行${NC}"
    echo "请在 Mac 上打开此项目并使用 Xcode 编译"
    exit 1
fi

# 检查 Xcode 是否安装
if ! command -v xcodebuild &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到 Xcode${NC}"
    echo "请从 App Store 安装 Xcode"
    exit 1
fi

# 显示 Xcode 版本
echo -e "${GREEN}✓ 检测到 Xcode${NC}"
xcodebuild -version
echo ""

# 检查 git 是否安装
if ! command -v git &> /dev/null; then
    echo -e "${YELLOW}⚠️  警告: 未找到 git，版本检查将被跳过${NC}"
fi

# 切换到项目目录
cd "$(dirname "$0")"

# 编译配置
PROJECT_NAME="CompositionHelper"
SCHEME_NAME="CompositionHelper"
CONFIGURATION="Debug"  # 可选: Debug 或 Release

echo -e "${GREEN}📋 编译配置:${NC}"
echo "  项目名: $PROJECT_NAME"
echo "  Scheme: $SCHEME_NAME"
echo "  配置: $CONFIGURATION"
echo ""

# 清理之前的构建
echo -e "${YELLOW}🧹 清理之前的构建...${NC}"
xcodebuild clean \
    -project "$PROJECT_NAME.xcodeproj" \
    -scheme "$SCHEME_NAME" \
    -configuration "$CONFIGURATION" || echo "⚠️  清理步骤跳过"

echo ""
echo -e "${GREEN}🔨 开始编译...${NC}"
echo "这可能需要几分钟，请耐心等待..."
echo ""

# 编译项目
if xcodebuild build \
    -project "$PROJECT_NAME.xcodeproj" \
    -scheme "$SCHEME_NAME" \
    -configuration "$CONFIGURATION" \
    -destination 'platform=iOS Simulator,name=iPhone 15' \
    CODE_SIGN_IDENTITY="" \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGNING_ALLOWED=NO; then
    
    echo ""
    echo -e "${GREEN}✅ 编译成功！${NC}"
    echo ""
    echo "下一步:"
    echo "1. 打开 Xcode: open $PROJECT_NAME.xcodeproj"
    echo "2. 选择模拟器或真机"
    echo "3. 点击 ▶️ 运行按钮 (⌘R)"
    
else
    echo ""
    echo -e "${RED}❌ 编译失败${NC}"
    echo ""
    echo "常见问题:"
    echo "1. 检查 Xcode 版本 (需要 14.0+)"
    echo "2. 检查 Swift 版本 (需要 5.0+)"
    echo "3. 查看上面的错误信息"
    echo ""
    echo "建议在 Xcode 中打开项目以查看详细错误:"
    echo "  open $PROJECT_NAME.xcodeproj"
    exit 1
fi
