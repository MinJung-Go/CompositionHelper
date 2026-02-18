import SwiftUI
import Vision
import CoreImage
import AVFoundation

// MARK: - 构图类型枚举
enum CompositionType: String, CaseIterable, Identifiable {
    case ruleOfThirds = "三分法"
    case center = "中心构图"
    case diagonal = "对角线"
    case frame = "框架构图"
    case leadingLines = "引导线"
    case sCurve = "S形曲线"
    case goldenSpiral = "黄金螺旋"
    
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
        }
    }
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
                
                // 构图选择器
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(CompositionType.allCases) { composition in
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
                }
                .padding(.vertical, 12)
                
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
                    VStack(spacing: 4) {
                        Image(systemName: type.icon)
                            .font(.system(size: 24))
                        Text(type.rawValue)
                            .font(.caption)
                    }
                    .frame(width: 70, height: 70)
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
