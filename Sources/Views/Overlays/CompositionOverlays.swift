import SwiftUI

// MARK: - 统一构图叠加入口
struct CompositionOverlayView: View {
    let compositionType: CompositionType
    let opacity: Double
    let color: Color

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            switch compositionType {
            case .ruleOfThirds:
                RuleOfThirdsGrid(size: size, opacity: opacity, color: color)
            case .center:
                CenterComposition(size: size, opacity: opacity, color: color)
            case .diagonal:
                DiagonalLines(size: size, opacity: opacity, color: color)
            case .frame:
                FrameComposition(size: size, opacity: opacity, color: color)
            case .leadingLines:
                LeadingLines(size: size, opacity: opacity, color: color)
            case .sCurve:
                SCurvePath(size: size, opacity: opacity, color: color)
            case .goldenSpiral:
                GoldenSpiral(size: size, opacity: opacity, color: color)
            case .goldenTriangle:
                GoldenTriangle(size: size, opacity: opacity, color: color)
            case .symmetry:
                SymmetryGrid(size: size, opacity: opacity, color: color)
            case .negativeSpace:
                NegativeSpaceGrid(size: size, opacity: opacity, color: color)
            case .patternRepeat:
                PatternGrid(size: size, opacity: opacity, color: color)
            case .tunnel:
                TunnelView(size: size, opacity: opacity, color: color)
            case .split:
                SplitComposition(size: size, opacity: opacity, color: color)
            case .perspective:
                PerspectiveGrid(size: size, opacity: opacity, color: color)
            case .invisibleLine:
                InvisibleLineView(size: size, opacity: opacity, color: color)
            case .fillTheFrame:
                FillTheFrameView(size: size, opacity: opacity, color: color)
            case .lowAngle:
                AngleView(size: size, type: .low, opacity: opacity, color: color)
            case .highAngle:
                AngleView(size: size, type: .high, opacity: opacity, color: color)
            case .depthLayer:
                DepthLayerView(size: size, opacity: opacity, color: color)
            }
        }
    }
}

// MARK: - 三分法
struct RuleOfThirdsGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: size.width / 3, y: 0))
            path.addLine(to: CGPoint(x: size.width / 3, y: size.height))
            path.move(to: CGPoint(x: size.width * 2 / 3, y: 0))
            path.addLine(to: CGPoint(x: size.width * 2 / 3, y: size.height))
            path.move(to: CGPoint(x: 0, y: size.height / 3))
            path.addLine(to: CGPoint(x: size.width, y: size.height / 3))
            path.move(to: CGPoint(x: 0, y: size.height * 2 / 3))
            path.addLine(to: CGPoint(x: size.width, y: size.height * 2 / 3))
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
    }
}

// MARK: - 中心构图
struct CenterComposition: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 2)

            Circle()
                .stroke(color.opacity(opacity), lineWidth: 2)
                .frame(width: 100, height: 100)
        }
    }
}

// MARK: - 对角线
struct DiagonalLines: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: size.width, y: size.height))
            path.move(to: CGPoint(x: size.width, y: 0))
            path.addLine(to: CGPoint(x: 0, y: size.height))
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
    }
}

// MARK: - 框架构图
struct FrameComposition: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let frameWidth = size.width * 0.7
        let frameHeight = size.height * 0.7

        Rectangle()
            .stroke(color.opacity(opacity), lineWidth: 4)
            .frame(width: frameWidth, height: frameHeight)
            .position(x: size.width / 2, y: size.height / 2)
    }
}

// MARK: - 引导线
struct LeadingLines: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        Path { path in
            for index in 0..<5 {
                let startX = CGFloat(index) * size.width / 6
                path.move(to: CGPoint(x: startX, y: size.height))
                path.addLine(to: CGPoint(x: size.width, y: CGFloat(index) * size.height / 6))
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 1)
    }
}

// MARK: - S形曲线
struct SCurvePath: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: 0, y: size.height))
            path.addCurve(
                to: CGPoint(x: size.width, y: 0),
                control1: CGPoint(x: size.width * 0.3, y: size.height),
                control2: CGPoint(x: size.width * 0.7, y: 0)
            )
        }
        .stroke(color.opacity(opacity), lineWidth: 3)
    }
}

// MARK: - 黄金螺旋
struct GoldenSpiral: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let phi = (1 + sqrt(5)) / 2
        let minSide = min(size.width, size.height)

        Path { path in
            var currentSize = minSide
            var posX: CGFloat = 0
            var posY: CGFloat = 0
            var direction = 0

            path.move(to: CGPoint(x: posX, y: posY))

            for _ in 0..<8 {
                switch direction {
                case 0: posX += currentSize
                case 1: posY += currentSize
                case 2: posX -= currentSize
                case 3: posY -= currentSize
                default: break
                }

                path.addArc(
                    center: CGPoint(x: posX, y: posY),
                    radius: currentSize,
                    startAngle: .degrees(Double(direction) * 90),
                    endAngle: .degrees(Double(direction + 1) * 90),
                    clockwise: false
                )

                currentSize /= phi
                direction = (direction + 1) % 4
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
    }
}

// MARK: - 黄金三角
struct GoldenTriangle: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let phi = (1 + sqrt(5)) / 2

        Path { path in
            path.move(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: size.width, y: size.height))
            path.addLine(to: CGPoint(x: 0, y: size.height))
            path.closeSubpath()

            let splitX = size.width / phi
            path.move(to: CGPoint(x: splitX, y: size.height))
            path.addLine(to: CGPoint(x: splitX, y: size.height - (size.height / phi)))
            path.addLine(to: CGPoint(x: 0, y: size.height - (size.height / phi)))
            path.closeSubpath()
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
    }
}

// MARK: - 对称构图
struct SymmetryGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)

            Path { path in
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)

            ForEach(0..<4, id: \.self) { idx in
                let posX: CGFloat = (idx % 2 == 0) ? size.width * 0.25 : size.width * 0.75
                let posY: CGFloat = (idx < 2) ? size.height * 0.25 : size.height * 0.75
                Circle()
                    .stroke(color.opacity(opacity), lineWidth: 2)
                    .frame(width: 20, height: 20)
                    .position(x: posX, y: posY)
            }
        }
    }
}

// MARK: - 负空间
struct NegativeSpaceGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        Path { path in
            let subjectWidth = size.width * 0.35
            let subjectHeight = size.height * 0.4

            path.addRect(CGRect(
                x: size.width * 0.1, y: size.height * 0.15,
                width: subjectWidth, height: subjectHeight
            ))
            path.addRect(CGRect(
                x: size.width * 0.55, y: size.height * 0.45,
                width: subjectWidth, height: subjectHeight
            ))
        }
        .stroke(color.opacity(opacity), style: StrokeStyle(lineWidth: 3, dash: [10, 5]))
    }
}

// MARK: - 模式重复
struct PatternGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                let cellWidth = size.width / 3
                let cellHeight = size.height / 3
                for row in 0..<3 {
                    for col in 0..<3 {
                        path.addRect(CGRect(
                            x: CGFloat(col) * cellWidth + cellWidth * 0.2,
                            y: CGFloat(row) * cellHeight + cellHeight * 0.2,
                            width: cellWidth * 0.6, height: cellHeight * 0.6
                        ))
                    }
                }
            }
            .stroke(color.opacity(opacity), lineWidth: 2)

            Path { path in
                let cellWidth = size.width / 3
                let cellHeight = size.height / 3
                for row in 0..<3 {
                    for col in 0..<3 {
                        let centerX = CGFloat(col) * cellWidth + cellWidth * 0.5
                        let centerY = CGFloat(row) * cellHeight + cellHeight * 0.5
                        path.move(to: CGPoint(x: centerX - 3, y: centerY))
                        path.addLine(to: CGPoint(x: centerX + 3, y: centerY))
                        path.move(to: CGPoint(x: centerX, y: centerY - 3))
                        path.addLine(to: CGPoint(x: centerX, y: centerY + 3))
                    }
                }
            }
            .stroke(color.opacity(opacity), lineWidth: 1)
        }
    }
}

// MARK: - 隧道式
struct TunnelView: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                let centerX = size.width / 2
                let centerY = size.height / 2
                for idx in 0..<5 {
                    let scale = 1.0 - CGFloat(idx) * 0.18
                    let rectW = size.width * scale
                    let rectH = size.height * scale
                    path.addRect(CGRect(x: centerX - rectW / 2, y: centerY - rectH / 2, width: rectW, height: rectH))
                }
            }
            .stroke(color.opacity(opacity), lineWidth: 2)

            Circle()
                .fill(color.opacity(opacity * 0.5))
                .frame(width: 8, height: 8)
                .position(x: size.width / 2, y: size.height / 2)
        }
    }
}

// MARK: - 分割构图
struct SplitComposition: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)

            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)

            Path { path in
                path.move(to: .zero)
                path.addLine(to: CGPoint(x: size.width, y: size.height))
                path.move(to: CGPoint(x: size.width, y: 0))
                path.addLine(to: CGPoint(x: 0, y: size.height))
            }
            .stroke(color.opacity(opacity * 0.5), style: StrokeStyle(lineWidth: 1.5, dash: [5, 5]))
        }
    }
}

// MARK: - 透视焦点
struct PerspectiveGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let centerX = size.width / 2
        let centerY = size.height * 0.6

        ZStack {
            Path { path in
                let points: [(CGFloat, CGFloat)] = [
                    (0, 0), (size.width * 0.25, 0), (size.width * 0.5, 0),
                    (size.width * 0.75, 0), (size.width, 0),
                    (0, size.height * 0.3), (size.width, size.height * 0.3)
                ]
                for (ptX, ptY) in points {
                    path.move(to: CGPoint(x: centerX, y: centerY))
                    path.addLine(to: CGPoint(x: ptX, y: ptY))
                }
            }
            .stroke(color.opacity(opacity), lineWidth: 1.5)

            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
                .position(x: centerX, y: centerY)
        }
    }
}

// MARK: - 隐形线
struct InvisibleLineView: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let endX = size.width * 0.8
        let endY = size.height * 0.2

        ZStack {
            Path { path in
                path.move(to: CGPoint(x: size.width * 0.2, y: size.height * 0.8))
                path.addCurve(
                    to: CGPoint(x: endX, y: endY),
                    control1: CGPoint(x: size.width * 0.2, y: size.height * 0.3),
                    control2: CGPoint(x: size.width * 0.8, y: size.height * 0.7)
                )
                let arrowSize: CGFloat = 15
                path.move(to: CGPoint(x: endX - arrowSize, y: endY - arrowSize * 0.5))
                path.addLine(to: CGPoint(x: endX, y: endY))
                path.addLine(to: CGPoint(x: endX - arrowSize, y: endY + arrowSize * 0.5))
            }
            .stroke(color.opacity(opacity), style: StrokeStyle(lineWidth: 3, dash: [15, 10]))

            Circle()
                .stroke(color.opacity(opacity), lineWidth: 3)
                .frame(width: 40, height: 40)
                .position(x: endX, y: endY)
        }
    }
}

// MARK: - 充满画面
struct FillTheFrameView: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let fillRatio: CGFloat = 0.85
        let fillWidth = size.width * fillRatio
        let fillHeight = size.height * fillRatio

        ZStack {
            Path { path in
                path.addRect(CGRect(x: 10, y: 10, width: size.width - 20, height: size.height - 20))
            }
            .stroke(color.opacity(opacity * 0.7), lineWidth: 2)

            Path { path in
                path.addRect(CGRect(
                    x: (size.width - fillWidth) / 2,
                    y: (size.height - fillHeight) / 2,
                    width: fillWidth, height: fillHeight
                ))
            }
            .stroke(color.opacity(opacity), style: StrokeStyle(lineWidth: 3, dash: [8, 4]))

            Text("主体应占 85% 以上")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: size.width / 2, y: size.height - 30)
        }
    }
}

// MARK: - 角度视图
enum AngleType {
    case low
    case high
}

struct AngleView: View {
    let size: CGSize
    let type: AngleType
    let opacity: Double
    let color: Color

    var body: some View {
        ZStack {
            Path { path in
                let centerX = size.width / 2
                switch type {
                case .low:
                    path.move(to: CGPoint(x: centerX, y: size.height))
                    path.addLine(to: CGPoint(x: centerX, y: size.height * 0.2))
                    path.move(to: CGPoint(x: centerX, y: size.height * 0.2))
                    path.addArc(
                        center: CGPoint(x: centerX, y: size.height * 0.2),
                        radius: size.width * 0.3,
                        startAngle: .degrees(220), endAngle: .degrees(320), clockwise: false
                    )
                case .high:
                    path.move(to: CGPoint(x: centerX, y: 0))
                    path.addLine(to: CGPoint(x: centerX, y: size.height * 0.8))
                    path.move(to: CGPoint(x: centerX, y: size.height * 0.8))
                    path.addArc(
                        center: CGPoint(x: centerX, y: size.height * 0.8),
                        radius: size.width * 0.3,
                        startAngle: .degrees(40), endAngle: .degrees(140), clockwise: false
                    )
                }
            }
            .stroke(color.opacity(opacity), lineWidth: 2)

            Text(type == .low ? "低角度" : "高角度")
                .font(.caption2)
                .foregroundColor(color.opacity(opacity))
                .position(x: size.width / 2, y: type == .low ? size.height - 20 : 20)
        }
    }
}

// MARK: - 深度层次
struct DepthLayerView: View {
    let size: CGSize
    let opacity: Double
    let color: Color

    var body: some View {
        let bgY = size.height * 0.1
        let midY = size.height * 0.35
        let fgY = size.height * 0.75
        let layerH = size.height * 0.25

        ZStack {
            // 背景层
            Path { path in
                path.addRect(CGRect(x: 0, y: bgY, width: size.width, height: layerH))
            }
            .fill(color.opacity(opacity * 0.05))
            Path { path in
                path.addRect(CGRect(x: 0, y: bgY, width: size.width, height: layerH))
            }
            .stroke(color.opacity(opacity), lineWidth: 2)
            Text("背景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: bgY + 20)

            // 中景层
            Path { path in
                path.addRect(CGRect(x: 0, y: midY, width: size.width, height: layerH))
            }
            .fill(color.opacity(opacity * 0.1))
            Path { path in
                path.addRect(CGRect(x: 0, y: midY, width: size.width, height: layerH))
            }
            .stroke(color.opacity(opacity), lineWidth: 2)
            Text("中景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: midY + 20)

            // 前景层
            Path { path in
                path.addRect(CGRect(x: 0, y: fgY, width: size.width, height: layerH))
            }
            .fill(color.opacity(opacity * 0.15))
            Path { path in
                path.addRect(CGRect(x: 0, y: fgY, width: size.width, height: layerH))
            }
            .stroke(color.opacity(opacity), lineWidth: 2)
            Text("前景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: fgY + 20)
        }
    }
}
