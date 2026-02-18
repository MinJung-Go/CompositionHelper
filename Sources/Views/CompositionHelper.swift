import SwiftUI
import Vision
import CoreImage
import AVFoundation

// MARK: - 构图类型枚举
enum CompositionType: String, CaseIterable, Identifiable {
    // 经典构图
    case ruleOfThirds = "三分法"
    case center = "中心构图"
    case diagonal = "对角线"
    case frame = "框架构图"
    case leadingLines = "引导线"
    case sCurve = "S形曲线"
    case goldenSpiral = "黄金螺旋"
    
    // 现代热门构图
    case goldenTriangle = "黄金三角"
    case symmetry = "对称构图"
    case negativeSpace = "负空间"
    case patternRepeat = "模式重复"
    case tunnel = "隧道式"
    case split = "分割构图"
    case perspective = "透视焦点"
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
enum CompositionCategory: String, CaseIterable {
    case classic = "经典"
    case modern = "现代"
    case perspective = "视角"
}

// MARK: - 主视图
struct CompositionHelperView: View {
    @State private var selectedImage: UIImage?
    @State private var selectedComposition: CompositionType = .ruleOfThirds
    @State private var lineOpacity: Double = 0.7
    @State private var lineColor: Color = .yellow
    @State private var showImagePicker = false
    @State private var showCamera = false
    @State private var autoAnalyzed = false
    @State private var recommendedCompositions: [CompositionType] = []
    @State private var selectedCategory: CompositionCategory = .classic
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 图像显示区域
                ZStack {
                    if let image = selectedImage {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .overlay(compositionOverlay)
                    } else {
                        // 占位图
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.gray.opacity(0.2))
                            .overlay(
                                VStack(spacing: 12) {
                                    Image(systemName: "camera.aperture")
                                        .font(.system(size: 60))
                                        .foregroundColor(.gray)
                                    Text("选择照片或拍照")
                                        .font(.headline)
                                        .foregroundColor(.gray)
                                }
                            )
                    }
                }
                .padding()
                .frame(maxHeight: UIScreen.main.bounds.height * 0.6)
                
                Divider()

                // 构图分类选择
                Picker("构图分类", selection: $selectedCategory) {
                    ForEach(CompositionCategory.allCases) { category in
                        Text(category.rawValue).tag(category)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .padding(.vertical, 8)

                // 构图选择器
                ScrollView {
                    let filteredCompositions = CompositionType.allCases.filter { $0.category == selectedCategory }
                    
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 4), spacing: 12) {
                        ForEach(filteredCompositions) { composition in
                            CompositionButton(
                                type: composition,
                                isSelected: selectedComposition == composition,
                                isRecommended: recommendedCompositions.contains(composition) && autoAnalyzed
                            ) {
                                selectedComposition = composition
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 12)
                }
                .frame(maxHeight: UIScreen.main.bounds.height * 0.25)
                
                Divider()
                
                // 控制面板
                VStack(spacing: 16) {
                    // 辅助线透明度
                    HStack {
                        Text("辅助线透明度")
                            .font(.subheadline)
                        Slider(value: $lineOpacity, in: 0.1...1.0)
                        Text("\(Int(lineOpacity * 100))%")
                            .font(.subheadline)
                            .frame(width: 40)
                    }
                    .padding(.horizontal)
                    
                    // 颜色选择
                    HStack {
                        Text("辅助线颜色")
                            .font(.subheadline)
                        Spacer()
                        ForEach([Color.yellow, .red, .blue, .white, .green], id: \.self) { color in
                            Circle()
                                .fill(color)
                                .frame(width: 30, height: 30)
                                .overlay(
                                    Circle()
                                        .stroke(lineColor == color ? Color.white : Color.clear, lineWidth: 3)
                                )
                                .onTapGesture {
                                    lineColor = color
                                }
                        }
                    }
                    .padding(.horizontal)
                    
                    // 操作按钮
                    HStack(spacing: 16) {
                        Button(action: { showCamera = true }) {
                            Label("拍照", systemImage: "camera")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        
                        Button(action: { showImagePicker = true }) {
                            Label("相册", systemImage: "photo.on.rectangle")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    }
                    .padding(.horizontal)
                    
                    Button(action: analyzeComposition) {
                        Label("自动分析构图", systemImage: "brain")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(autoAnalyzed ? Color.purple : Color.gray.opacity(0.8))
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    .padding(.horizontal)
                    .disabled(selectedImage == nil)
                }
                .padding()
            }
            .navigationTitle("构图辅助")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("编辑") {
                        // 编辑功能
                    }
                }
            }
        }
        .sheet(isPresented: $showImagePicker) {
            ImagePicker { image in
                selectedImage = image
                autoAnalyzed = false
                recommendedCompositions = []
            }
        }
        .sheet(isPresented: $showCamera) {
            CameraView { image in
                selectedImage = image
                autoAnalyzed = false
                recommendedCompositions = []
            }
        }
    }
    
    // MARK: - 构图辅助线叠加
    @ViewBuilder
    var compositionOverlay: some View {
        GeometryReader { geometry in
            let size = geometry.size
            
            switch selectedComposition {
            case .ruleOfThirds:
                RuleOfThirdsGrid(size: size, opacity: lineOpacity, color: lineColor)
            case .center:
                CenterComposition(size: size, opacity: lineOpacity, color: lineColor)
            case .diagonal:
                DiagonalLines(size: size, opacity: lineOpacity, color: lineColor)
            case .frame:
                FrameComposition(size: size, opacity: lineOpacity, color: lineColor)
            case .leadingLines:
                LeadingLines(size: size, opacity: lineOpacity, color: lineColor)
            case .sCurve:
                SCurvePath(size: size, opacity: lineOpacity, color: lineColor)
            case .goldenSpiral:
                GoldenSpiral(size: size, opacity: lineOpacity, color: lineColor)
            case .goldenTriangle:
                GoldenTriangle(size: size, opacity: lineOpacity, color: lineColor)
            case .symmetry:
                SymmetryGrid(size: size, opacity: lineOpacity, color: lineColor)
            case .negativeSpace:
                NegativeSpaceGrid(size: size, opacity: lineOpacity, color: lineColor)
            case .patternRepeat:
                PatternGrid(size: size, opacity: lineOpacity, color: lineColor)
            case .tunnel:
                TunnelView(size: size, opacity: lineOpacity, color: lineColor)
            case .split:
                SplitComposition(size: size, opacity: lineOpacity, color: lineColor)
            case .perspective:
                PerspectiveGrid(size: size, opacity: lineOpacity, color: lineColor)
            case .invisibleLine:
                InvisibleLine(size: size, opacity: lineOpacity, color: lineColor)
            case .fillTheFrame:
                FillTheFrameView(size: size, opacity: lineOpacity, color: lineColor)
            case .lowAngle:
                AngleView(size: size, type: .low, opacity: lineOpacity, color: lineColor)
            case .highAngle:
                AngleView(size: size, type: .high, opacity: lineOpacity, color: lineColor)
            case .depthLayer:
                DepthLayerView(size: size, opacity: lineOpacity, color: lineColor)
            }
        }
    }
    
    // MARK: - 自动构图分析
    func analyzeComposition() {
        guard let image = selectedImage else { return }
        
        Task {
            do {
                let analysis = try await CompositionAnalyzer.analyze(image: image)
                
                await MainActor.run {
                    self.recommendedCompositions = analysis.recommendedCompositions
                    self.autoAnalyzed = true
                    if let firstRecommendation = analysis.recommendedCompositions.first {
                        self.selectedComposition = firstRecommendation
                    }
                    
                    // 可以在这里显示分析详情
                    print("分析完成！")
                    print("检测到的主体: \(analysis.detectedSubjects.count)")
                    print("检测到的线条: \(analysis.detectedLines.count)")
                    print("推荐构图: \(analysis.recommendedCompositions.map { $0.rawValue }.joined(separator: ", "))")
                }
            } catch {
                await MainActor.run {
                    // 分析失败时的处理
                    self.recommendedCompositions = [
                        .ruleOfThirds,
                        .center,
                        .leadingLines
                    ]
                    self.autoAnalyzed = true
                    self.selectedComposition = .ruleOfThirds
                }
                print("分析失败: \(error)")
            }
        }
    }
}

// MARK: - 构图类型按钮
struct CompositionButton: View {
    let type: CompositionType
    let isSelected: Bool
    let isRecommended: Bool
    let action: () -> Void
    
    var body: some View {
        VStack(spacing: 8) {
            ZStack(alignment: .topTrailing) {
                Button(action: action) {
                    VStack(spacing: 6) {
                        Image(systemName: type.icon)
                            .font(.system(size: 22))
                        Text(type.rawValue)
                            .font(.caption2)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                    }
                    .frame(width: 72, height: 80)
                    .background(isSelected ? Color.blue.opacity(0.2) : Color.gray.opacity(0.1))
                    .foregroundColor(isSelected ? .blue : .primary)
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
                    )
                }
                
                // 推荐标记
                if isRecommended {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.yellow)
                        .padding(4)
                        .background(Color.yellow.opacity(0.3))
                        .clipShape(Circle())
                }
            }
        }
    }
}

// MARK: - 三分法网格
struct RuleOfThirdsGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            let width = size.width
            let height = size.height
            
            // 垂直线
            path.move(to: CGPoint(x: width / 3, y: 0))
            path.addLine(to: CGPoint(x: width / 3, y: height))
            
            path.move(to: CGPoint(x: width * 2 / 3, y: 0))
            path.addLine(to: CGPoint(x: width * 2 / 3, y: height))
            
            // 水平线
            path.move(to: CGPoint(x: 0, y: height / 3))
            path.addLine(to: CGPoint(x: width, y: height / 3))
            
            path.move(to: CGPoint(x: 0, y: height * 2 / 3))
            path.addLine(to: CGPoint(x: width, y: height * 2 / 3))
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
            // 中心十字
            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
                
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 2)
            
            // 中心圆
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
        let frameWidth: CGFloat = size.width * 0.7
        let frameHeight: CGFloat = size.height * 0.7
        let xOffset = (size.width - frameWidth) / 2
        let yOffset = (size.height - frameHeight) / 2
        
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
            // 从左下角向右上角的多条线
            for i in 0..<5 {
                let startX = CGFloat(i) * size.width / 6
                path.move(to: CGPoint(x: startX, y: size.height))
                path.addLine(to: CGPoint(x: size.width, y: CGFloat(i) * size.height / 6))
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
            var x: CGFloat = 0
            var y: CGFloat = 0
            var direction = 0 // 0=右, 1=下, 2=左, 3=上
            
            path.move(to: CGPoint(x: x, y: y))
            
            for _ in 0..<8 {
                switch direction {
                case 0:
                    x += currentSize
                case 1:
                    y += currentSize
                case 2:
                    x -= currentSize
                case 3:
                    y -= currentSize
                default:
                    break
                }
                
                path.addArc(
                    center: CGPoint(x: x, y: y),
                    radius: currentSize,
                    startAngle: .degrees(direction * 90),
                    endAngle: .degrees((direction + 1) * 90),
                    clockwise: false
                )
                
                currentSize /= phi
                direction = (direction + 1) % 4
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
    }
}

// MARK: - 黄金三角形
struct GoldenTriangle: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        let phi = (1 + sqrt(5)) / 2
        let width = size.width
        let height = size.height
        
        Path { path in
            // 主三角形
            path.move(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: width, y: height))
            path.addLine(to: CGPoint(x: 0, y: height))
            path.closeSubpath()
            
            // 次级三角形（黄金分割点）
            let splitX = width / phi
            path.move(to: CGPoint(x: splitX, y: height))
            path.addLine(to: CGPoint(x: splitX, y: height - (height / phi)))
            path.addLine(to: CGPoint(x: 0, y: height - (height / phi)))
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
            // 垂直对称线
            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)
            
            // 水平对称线
            Path { path in
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)
            
            // 四角对称标记
            ForEach(0..<4) { i in
                let x: CGFloat
                let y: CGFloat
                
                switch i {
                case 0: x = size.width * 0.25; y = size.height * 0.25
                case 1: x = size.width * 0.75; y = size.height * 0.25
                case 2: x = size.width * 0.25; y = size.height * 0.75
                default: x = size.width * 0.75; y = size.height * 0.75
                }
                
                Circle()
                    .stroke(color.opacity(opacity), lineWidth: 2)
                    .frame(width: 20, height: 20)
                    .position(x: x, y: y)
            }
        }
    }
}

// MARK: - 负空间构图
struct NegativeSpaceGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            // 创建主体区域（约 1/3）
            let subjectWidth = size.width * 0.35
            let subjectHeight = size.height * 0.4
            
            // 左上角区域
            path.addRect(CGRect(
                x: size.width * 0.1,
                y: size.height * 0.15,
                width: subjectWidth,
                height: subjectHeight
            ))
            
            // 右下角区域
            path.addRect(CGRect(
                x: size.width * 0.55,
                y: size.height * 0.45,
                width: subjectWidth,
                height: subjectHeight
            ))
        }
        .stroke(color.opacity(opacity), lineWidth: 3, dash: [10, 5])
    }
}

// MARK: - 模式重复
struct PatternGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            // 创建 3x3 重复模式
            let cols = 3
            let rows = 3
            let cellWidth = size.width / CGFloat(cols)
            let cellHeight = size.height / CGFloat(rows)
            
            for row in 0..<rows {
                for col in 0..<cols {
                    let x = CGFloat(col) * cellWidth + cellWidth * 0.2
                    let y = CGFloat(row) * cellHeight + cellHeight * 0.2
                    let width = cellWidth * 0.6
                    let height = cellHeight * 0.6
                    
                    path.addRect(CGRect(x: x, y: y, width: width, height: height))
                }
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
        
        // 节点标记
        Path { path in
            let cols = 3
            let rows = 3
            let cellWidth = size.width / CGFloat(cols)
            let cellHeight = size.height / CGFloat(rows)
            
            for row in 0..<rows {
                for col in 0..<cols {
                    let x = CGFloat(col) * cellWidth + cellWidth * 0.5
                    let y = CGFloat(row) * cellHeight + cellHeight * 0.5
                    
                    path.move(to: CGPoint(x: x - 3, y: y))
                    path.addLine(to: CGPoint(x: x + 3, y: y))
                    path.move(to: CGPoint(x: x, y: y - 3))
                    path.addLine(to: CGPoint(x: x, y: y + 3))
                }
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 1)
    }
}

// MARK: - 隧道式构图
struct TunnelView: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            let centerX = size.width / 2
            let centerY = size.height / 2
            let maxRadius = min(size.width, size.height) * 0.45
            
            // 创建向内的矩形
            for i in 0..<5 {
                let scale = 1.0 - CGFloat(i) * 0.18
                let currentRadius = maxRadius * scale
                let currentWidth = size.width * scale
                let currentHeight = size.height * scale
                
                let rect = CGRect(
                    x: centerX - currentWidth / 2,
                    y: centerY - currentHeight / 2,
                    width: currentWidth,
                    height: currentHeight
                )
                
                path.addRect(rect)
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
        
        // 中心消失点
        Circle()
            .fill(color.opacity(opacity * 0.5))
            .frame(width: 8, height: 8)
            .position(x: size.width / 2, y: size.height / 2)
    }
}

// MARK: - 分割构图
struct SplitComposition: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        ZStack {
            // 水平分割
            Path { path in
                path.move(to: CGPoint(x: 0, y: size.height / 2))
                path.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)
            
            // 垂直分割
            Path { path in
                path.move(to: CGPoint(x: size.width / 2, y: 0))
                path.addLine(to: CGPoint(x: size.width / 2, y: size.height))
            }
            .stroke(color.opacity(opacity), lineWidth: 3)
            
            // 对角分割
            Path { path in
                path.move(to: CGPoint(x: 0, y: 0))
                path.addLine(to: CGPoint(x: size.width, y: size.height))
                path.move(to: CGPoint(x: size.width, y: 0))
                path.addLine(to: CGPoint(x: 0, y: size.height))
            }
            .stroke(color.opacity(opacity * 0.5), lineWidth: 1.5, dash: [5, 5])
        }
    }
}

// MARK: - 透视焦点
struct PerspectiveGrid: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            let centerX = size.width / 2
            let centerY = size.height * 0.6
            
            // 从消失点发出的射线
            let points: [(CGFloat, CGFloat)] = [
                (0, 0),
                (size.width * 0.25, 0),
                (size.width * 0.5, 0),
                (size.width * 0.75, 0),
                (size.width, 0),
                (0, size.height * 0.3),
                (size.width, size.height * 0.3)
            ]
            
            for (x, y) in points {
                path.move(to: CGPoint(x: centerX, y: centerY))
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 1.5)
        
        // 消失点
        Circle()
            .fill(color)
            .frame(width: 6, height: 6)
            .position(x: size.width / 2, y: size.height * 0.6)
    }
}

// MARK: - 隐形线
struct InvisibleLine: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        Path { path in
            // 创建 S 形隐形线引导
            let startX = size.width * 0.2
            let startY = size.height * 0.8
            let endX = size.width * 0.8
            let endY = size.height * 0.2
            
            path.move(to: CGPoint(x: startX, y: startY))
            path.addCurve(
                to: CGPoint(x: endX, y: endY),
                control1: CGPoint(x: size.width * 0.2, y: size.height * 0.3),
                control2: CGPoint(x: size.width * 0.8, y: size.height * 0.7)
            )
            
            // 箭头
            let arrowSize: CGFloat = 15
            path.move(to: CGPoint(x: endX - arrowSize, y: endY - arrowSize * 0.5))
            path.addLine(to: CGPoint(x: endX, y: endY))
            path.addLine(to: CGPoint(x: endX - arrowSize, y: endY + arrowSize * 0.5))
        }
        .stroke(color.opacity(opacity), lineWidth: 3, dash: [15, 10])
        
        // 视觉焦点标记
        Circle()
            .stroke(color.opacity(opacity), lineWidth: 3)
            .frame(width: 40, height: 40)
            .position(x: endX, y: endY)
    }
}

// MARK: - 充满画面
struct FillTheFrameView: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        ZStack {
            // 外边框（应该填充的边缘）
            Path { path in
                let margin: CGFloat = 10
                let insetRect = CGRect(
                    x: margin,
                    y: margin,
                    width: size.width - margin * 2,
                    height: size.height - margin * 2
                )
                path.addRect(insetRect)
            }
            .stroke(color.opacity(opacity * 0.7), lineWidth: 2)
            
            // 内部填充提示（表示主体应该填充的区域）
            let fillRatio: CGFloat = 0.85
            let fillWidth = size.width * fillRatio
            let fillHeight = size.height * fillRatio
            
            Path { path in
                path.addRect(CGRect(
                    x: (size.width - fillWidth) / 2,
                    y: (size.height - fillHeight) / 2,
                    width: fillWidth,
                    height: fillHeight
                ))
            }
            .stroke(color.opacity(opacity), lineWidth: 3, dash: [8, 4])
            
            // 文字提示
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
        Path { path in
            let centerX = size.width / 2
            let centerY = size.height / 2
            
            switch type {
            case .low:
                // 低角度 - 从下往上
                path.move(to: CGPoint(x: centerX, y: size.height))
                path.addLine(to: CGPoint(x: centerX, y: size.height * 0.2))
                
                // 扇形视野
                path.move(to: CGPoint(x: centerX, y: size.height * 0.2))
                path.addArc(
                    center: CGPoint(x: centerX, y: size.height * 0.2),
                    radius: size.width * 0.3,
                    startAngle: .degrees(220),
                    endAngle: .degrees(320),
                    clockwise: false
                )
                
            case .high:
                // 高角度 - 从上往下
                path.move(to: CGPoint(x: centerX, y: 0))
                path.addLine(to: CGPoint(x: centerX, y: size.height * 0.8))
                
                // 扇形视野
                path.move(to: CGPoint(x: centerX, y: size.height * 0.8))
                path.addArc(
                    center: CGPoint(x: centerX, y: size.height * 0.8),
                    radius: size.width * 0.3,
                    startAngle: .degrees(40),
                    endAngle: .degrees(140),
                    clockwise: false
                )
            }
        }
        .stroke(color.opacity(opacity), lineWidth: 2)
        
        // 角度标记
        Text(type == .low ? "低角度 ↑" : "高角度 ↓")
            .font(.caption2)
            .foregroundColor(color.opacity(opacity))
            .position(x: size.width / 2, y: type == .low ? size.height - 20 : 20)
    }
}

// MARK: - 深度层次
struct DepthLayerView: View {
    let size: CGSize
    let opacity: Double
    let color: Color
    
    var body: some View {
        ZStack {
            // 前景层（底部）
            Path { path in
                let height = size.height * 0.25
                path.move(to: CGPoint(x: 0, y: size.height))
                path.addLine(to: CGPoint(x: size.width, y: size.height))
                path.addLine(to: CGPoint(x: size.width, y: size.height - height))
                path.addLine(to: CGPoint(x: 0, y: size.height - height))
                path.closeSubpath()
            }
            .fill(color.opacity(opacity * 0.15))
            .stroke(color.opacity(opacity), lineWidth: 2)
            
            Text("前景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: size.height - 30)
            
            // 中景层（中间）
            Path { path in
                let height = size.height * 0.25
                let startY = size.height * 0.35
                path.addRect(CGRect(
                    x: 0,
                    y: startY,
                    width: size.width,
                    height: height
                ))
            }
            .fill(color.opacity(opacity * 0.1))
            .stroke(color.opacity(opacity), lineWidth: 2)
            
            Text("中景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: startY + 20)
            
            // 背景层（顶部）
            Path { path in
                let startY = size.height * 0.1
                path.addRect(CGRect(
                    x: 0,
                    y: startY,
                    width: size.width,
                    height: size.height * 0.25
                ))
            }
            .fill(color.opacity(opacity * 0.05))
            .stroke(color.opacity(opacity), lineWidth: 2)
            
            Text("背景")
                .font(.caption)
                .foregroundColor(color.opacity(opacity))
                .position(x: 30, y: startY + 20)
        }
    }
}

// MARK: - 图片选择器
struct ImagePicker: UIViewControllerRepresentable {
    let onImagePicked: (UIImage) -> Void
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .photoLibrary
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onImagePicked: onImagePicked)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImagePicked: (UIImage) -> Void
        
        init(onImagePicked: @escaping (UIImage) -> Void) {
            self.onImagePicked = onImagePicked
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                onImagePicked(image)
            }
            picker.dismiss(animated: true)
        }
    }
}

// MARK: - 相机视图
struct CameraView: UIViewControllerRepresentable {
    let onImageCaptured: (UIImage) -> Void
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .camera
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onImageCaptured: onImageCaptured)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImageCaptured: (UIImage) -> Void
        
        init(onImageCaptured: @escaping (UIImage) -> Void) {
            self.onImageCaptured = onImageCaptured
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                onImageCaptured(image)
            }
            picker.dismiss(animated: true)
        }
    }
}

// MARK: - 预览
struct CompositionHelperView_Previews: PreviewProvider {
    static var previews: some View {
        CompositionHelperView()
    }
}
