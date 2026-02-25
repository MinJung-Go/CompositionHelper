import SwiftUI

// MARK: - 构图类型枚举
enum CompositionType: String, CaseIterable, Identifiable {
    // 经典构图 (7)
    case ruleOfThirds = "三分法"
    case center = "中心构图"
    case diagonal = "对角线"
    case frame = "框架构图"
    case leadingLines = "引导线"
    case sCurve = "S形曲线"
    case goldenSpiral = "黄金螺旋"

    // 现代构图 (7)
    case goldenTriangle = "黄金三角"
    case symmetry = "对称构图"
    case negativeSpace = "负空间"
    case patternRepeat = "模式重复"
    case tunnel = "隧道式"
    case split = "分割构图"
    case perspective = "透视焦点"

    // 视角构图 (5)
    case invisibleLine = "隐形线"
    case fillTheFrame = "充满画面"
    case lowAngle = "低角度"
    case highAngle = "高角度"
    case depthLayer = "深度层次"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .ruleOfThirds: return "grid.3x3"
        case .center: return "target"
        case .diagonal: return "line.diagonal"
        case .frame: return "square.on.square"
        case .leadingLines: return "line.3.horizontal"
        case .sCurve: return "waveform.path"
        case .goldenSpiral: return "tornado"
        case .goldenTriangle: return "triangle"
        case .symmetry: return "arrow.left.and.right.righttriangle.left.righttriangle"
        case .negativeSpace: return "square.dashed"
        case .patternRepeat: return "square.grid.3x3.fill"
        case .tunnel: return "rectangle.compress.vertical"
        case .split: return "square.split.diagonal.2x2"
        case .perspective: return "perspective"
        case .invisibleLine: return "line.diagonal.arrow"
        case .fillTheFrame: return "rectangle.fill"
        case .lowAngle: return "arrow.up.forward.app"
        case .highAngle: return "arrow.down.forward.app"
        case .depthLayer: return "square.stack.3d.up.fill"
        }
    }

    var category: CompositionCategory {
        switch self {
        case .ruleOfThirds, .center, .diagonal, .frame, .leadingLines, .sCurve, .goldenSpiral:
            return .classic
        case .goldenTriangle, .symmetry, .negativeSpace, .patternRepeat, .tunnel, .split, .perspective:
            return .modern
        case .invisibleLine, .fillTheFrame, .lowAngle, .highAngle, .depthLayer:
            return .perspective
        }
    }
}

// MARK: - 构图分类
enum CompositionCategory: String, CaseIterable, Identifiable {
    case classic = "经典"
    case modern = "现代"
    case perspective = "视角"

    var id: String { rawValue }
}
