import SwiftUI
import AVFoundation

// MARK: - 实时相机构图视图
struct CameraCompositionView: View {
    @StateObject private var cameraManager = CameraManager()
    @StateObject private var frameAnalyzer = FrameAnalyzer()

    @State private var selectedComposition: CompositionType = .ruleOfThirds
    @State private var selectedCategory: CompositionCategory = .classic
    @State private var lineOpacity: Double = 0.7
    @State private var lineColor: Color = .yellow
    @State private var isSmartMode = false
    @State private var showControls = true
    @State private var showSettings = false
    @State private var showCapturedPhoto = false

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            // 全屏相机预览
            CameraPreviewView(session: cameraManager.session)
                .ignoresSafeArea()

            // 构图辅助线叠加
            CompositionOverlayView(
                compositionType: isSmartMode ? (frameAnalyzer.analysisResult?.recommendedType ?? selectedComposition) : selectedComposition,
                opacity: lineOpacity,
                color: lineColor
            )
            .ignoresSafeArea()
            .allowsHitTesting(false)

            // 主体追踪框
            if isSmartMode {
                SubjectTrackingOverlay(
                    subjects: frameAnalyzer.detectedSubjects,
                    compositionType: frameAnalyzer.analysisResult?.recommendedType ?? selectedComposition
                )
                .ignoresSafeArea()
                .allowsHitTesting(false)
            }

            // UI 层
            VStack {
                // 顶栏
                if showControls {
                    topBar
                }

                Spacer()

                // AI 推荐浮层
                if isSmartMode, let result = frameAnalyzer.analysisResult {
                    RecommendationChip(result: result)
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                // 底部控制栏
                if showControls {
                    bottomControls
                }
            }
        }
        .statusBarHidden(true)
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) {
                showControls.toggle()
            }
        }
        .onAppear {
            cameraManager.checkPermission()
        }
        .onDisappear {
            cameraManager.stopSession()
            cameraManager.frameDelegate = nil
        }
        .onChange(of: isSmartMode) { smart in
            if smart {
                cameraManager.frameDelegate = frameAnalyzer
            } else {
                cameraManager.frameDelegate = nil
                frameAnalyzer.analysisResult = nil
                frameAnalyzer.detectedSubjects = []
            }
        }
        .sheet(isPresented: $showSettings) {
            settingsSheet
        }
        .fullScreenCover(isPresented: $showCapturedPhoto) {
            if let photo = cameraManager.capturedPhoto {
                CapturedPhotoView(image: photo) {
                    showCapturedPhoto = false
                    cameraManager.capturedPhoto = nil
                }
            }
        }
        .onChange(of: cameraManager.capturedPhoto) { photo in
            if photo != nil {
                showCapturedPhoto = true
            }
        }
    }

    // MARK: - 顶栏
    private var topBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.title2)
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.4))
                    .clipShape(Circle())
            }

            Spacer()

            // 手动/智能模式切换
            Button(action: {
                withAnimation { isSmartMode.toggle() }
            }) {
                HStack(spacing: 6) {
                    Image(systemName: isSmartMode ? "brain" : "hand.draw")
                    Text(isSmartMode ? "智能" : "手动")
                        .font(.subheadline)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSmartMode ? Color.blue.opacity(0.8) : Color.black.opacity(0.4))
                .foregroundColor(.white)
                .cornerRadius(20)
            }
        }
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    // MARK: - 底部控制
    private var bottomControls: some View {
        VStack(spacing: 12) {
            // 构图类型选择器
            if !isSmartMode {
                ControlPanel(
                    selectedCategory: $selectedCategory,
                    selectedComposition: $selectedComposition
                )
            }

            // 底部按钮行
            HStack(spacing: 40) {
                // 设置按钮
                Button(action: { showSettings = true }) {
                    Image(systemName: "slider.horizontal.3")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 50, height: 50)
                        .background(Color.black.opacity(0.4))
                        .clipShape(Circle())
                }

                // 快门按钮
                ShutterButton {
                    cameraManager.capturePhoto()
                }

                // 相册模式按钮
                Button(action: { dismiss() }) {
                    Image(systemName: "photo.on.rectangle")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 50, height: 50)
                        .background(Color.black.opacity(0.4))
                        .clipShape(Circle())
                }
            }
            .padding(.bottom, 24)
        }
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }

    // MARK: - 设置面板
    private var settingsSheet: some View {
        NavigationView {
            Form {
                Section("辅助线") {
                    HStack {
                        Text("透明度")
                        Slider(value: $lineOpacity, in: 0.1...1.0)
                        Text("\(Int(lineOpacity * 100))%")
                            .frame(width: 40)
                    }

                    HStack {
                        Text("颜色")
                        Spacer()
                        ForEach([Color.yellow, .red, .blue, .white, .green], id: \.self) { c in
                            Circle()
                                .fill(c)
                                .frame(width: 30, height: 30)
                                .overlay(
                                    Circle().stroke(lineColor == c ? Color.primary : Color.clear, lineWidth: 3)
                                )
                                .onTapGesture { lineColor = c }
                        }
                    }
                }

                Section("分析") {
                    Toggle("智能推荐", isOn: $isSmartMode)
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") { showSettings = false }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

// MARK: - 主体追踪叠加层
struct SubjectTrackingOverlay: View {
    let subjects: [DetectedSubject]
    let compositionType: CompositionType

    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            ForEach(Array(subjects.enumerated()), id: \.offset) { _, subject in
                // Vision boundingBox: 左下角为原点，需要翻转 Y
                let rect = CGRect(
                    x: subject.boundingBox.minX * size.width,
                    y: (1 - subject.boundingBox.maxY) * size.height,
                    width: subject.boundingBox.width * size.width,
                    height: subject.boundingBox.height * size.height
                )

                let alignmentColor = alignmentIndicatorColor(
                    subjectCenter: CGPoint(x: rect.midX / size.width, y: rect.midY / size.height),
                    compositionType: compositionType
                )

                RoundedRectangle(cornerRadius: 6)
                    .stroke(alignmentColor, lineWidth: 2.5)
                    .frame(width: rect.width, height: rect.height)
                    .position(x: rect.midX, y: rect.midY)
            }
        }
    }

    private func alignmentIndicatorColor(subjectCenter: CGPoint, compositionType: CompositionType) -> Color {
        let keyPoints = compositionKeyPoints(for: compositionType)
        let minDist = keyPoints.map { sqrt(pow($0.x - subjectCenter.x, 2) + pow($0.y - subjectCenter.y, 2)) }.min() ?? 1.0

        if minDist < 0.05 {
            return .green // 已对齐
        } else if minDist < 0.12 {
            return .yellow // 接近
        } else {
            return .cyan // 未对齐
        }
    }

    private func compositionKeyPoints(for type: CompositionType) -> [CGPoint] {
        switch type {
        case .ruleOfThirds:
            return [
                CGPoint(x: 1.0/3, y: 1.0/3), CGPoint(x: 2.0/3, y: 1.0/3),
                CGPoint(x: 1.0/3, y: 2.0/3), CGPoint(x: 2.0/3, y: 2.0/3)
            ]
        case .center:
            return [CGPoint(x: 0.5, y: 0.5)]
        case .goldenSpiral:
            return [CGPoint(x: 0.382, y: 0.618), CGPoint(x: 0.618, y: 0.382)]
        case .goldenTriangle:
            return [CGPoint(x: 0.382, y: 0.618), CGPoint(x: 0.618, y: 0.382)]
        case .diagonal:
            return [CGPoint(x: 0.25, y: 0.25), CGPoint(x: 0.75, y: 0.75),
                    CGPoint(x: 0.75, y: 0.25), CGPoint(x: 0.25, y: 0.75)]
        case .perspective:
            return [CGPoint(x: 0.5, y: 0.6)]
        default:
            return [
                CGPoint(x: 1.0/3, y: 1.0/3), CGPoint(x: 2.0/3, y: 1.0/3),
                CGPoint(x: 1.0/3, y: 2.0/3), CGPoint(x: 2.0/3, y: 2.0/3),
                CGPoint(x: 0.5, y: 0.5)
            ]
        }
    }
}

// MARK: - 拍照结果预览
struct CapturedPhotoView: View {
    let image: UIImage
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)

            VStack {
                HStack {
                    Button(action: onDismiss) {
                        Text("返回")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(10)
                    }
                    Spacer()

                    Button(action: {
                        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                        onDismiss()
                    }) {
                        Text("保存")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color.blue)
                            .cornerRadius(10)
                    }
                }
                .padding()

                Spacer()
            }
        }
    }
}
