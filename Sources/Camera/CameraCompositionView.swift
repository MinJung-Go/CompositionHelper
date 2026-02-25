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
            overlayView
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
                if showControls { topBar }
                Spacer()

                if isSmartMode, let result = frameAnalyzer.analysisResult {
                    RecommendationChip(result: result)
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                if showControls { bottomControls }
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
        .onChange(of: isSmartMode) { newValue in
            handleSmartModeChange(newValue)
        }
        .sheet(isPresented: $showSettings) {
            settingsSheet
        }
        .fullScreenCover(isPresented: $showCapturedPhoto) {
            capturedPhotoScreen
        }
        .onChange(of: cameraManager.capturedPhoto) { newValue in
            if newValue != nil {
                showCapturedPhoto = true
            }
        }
    }

    // MARK: - 构图叠加
    private var overlayView: some View {
        let activeType = isSmartMode
            ? (frameAnalyzer.analysisResult?.recommendedType ?? selectedComposition)
            : selectedComposition
        return CompositionOverlayView(
            compositionType: activeType,
            opacity: lineOpacity,
            color: lineColor
        )
    }

    private func handleSmartModeChange(_ smart: Bool) {
        if smart {
            cameraManager.frameDelegate = frameAnalyzer
        } else {
            cameraManager.frameDelegate = nil
            frameAnalyzer.analysisResult = nil
            frameAnalyzer.detectedSubjects = []
        }
    }

    // MARK: - 顶栏
    private var topBar: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title2)
                    .foregroundColor(.white)
                    .padding(12)
                    .background(Color.black.opacity(0.4))
                    .clipShape(Circle())
            }

            Spacer()

            Button {
                withAnimation { isSmartMode.toggle() }
            } label: {
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
            if !isSmartMode {
                ControlPanel(
                    selectedCategory: $selectedCategory,
                    selectedComposition: $selectedComposition
                )
            }

            HStack(spacing: 40) {
                Button {
                    showSettings = true
                } label: {
                    Image(systemName: "slider.horizontal.3")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 50, height: 50)
                        .background(Color.black.opacity(0.4))
                        .clipShape(Circle())
                }

                ShutterButton {
                    cameraManager.capturePhoto()
                }

                Button {
                    dismiss()
                } label: {
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
                        ForEach([Color.yellow, .red, .blue, .white, .green], id: \.self) { color in
                            Circle()
                                .fill(color)
                                .frame(width: 30, height: 30)
                                .overlay(
                                    Circle().stroke(
                                        lineColor == color ? Color.primary : Color.clear,
                                        lineWidth: 3
                                    )
                                )
                                .onTapGesture { lineColor = color }
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

    // MARK: - 拍照结果
    @ViewBuilder
    private var capturedPhotoScreen: some View {
        if let photo = cameraManager.capturedPhoto {
            CapturedPhotoView(image: photo) {
                showCapturedPhoto = false
                cameraManager.capturedPhoto = nil
            }
        }
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
                let rect = convertBoundingBox(subject.boundingBox, in: size)
                let color = alignmentColor(
                    subjectCenter: CGPoint(
                        x: rect.midX / size.width,
                        y: rect.midY / size.height
                    )
                )

                RoundedRectangle(cornerRadius: 6)
                    .stroke(color, lineWidth: 2.5)
                    .frame(width: rect.width, height: rect.height)
                    .position(x: rect.midX, y: rect.midY)
            }
        }
    }

    // Vision boundingBox: 左下角为原点，需要翻转 Y
    private func convertBoundingBox(_ box: CGRect, in size: CGSize) -> CGRect {
        CGRect(
            x: box.minX * size.width,
            y: (1 - box.maxY) * size.height,
            width: box.width * size.width,
            height: box.height * size.height
        )
    }

    private func alignmentColor(subjectCenter: CGPoint) -> Color {
        let keyPoints = compositionKeyPoints(for: compositionType)
        let minDist = keyPoints.map {
            sqrt(pow($0.x - subjectCenter.x, 2) + pow($0.y - subjectCenter.y, 2))
        }.min() ?? 1.0

        if minDist < 0.05 {
            return .green
        } else if minDist < 0.12 {
            return .yellow
        } else {
            return .cyan
        }
    }

    private func compositionKeyPoints(for type: CompositionType) -> [CGPoint] {
        switch type {
        case .ruleOfThirds:
            return [
                CGPoint(x: 1.0 / 3, y: 1.0 / 3), CGPoint(x: 2.0 / 3, y: 1.0 / 3),
                CGPoint(x: 1.0 / 3, y: 2.0 / 3), CGPoint(x: 2.0 / 3, y: 2.0 / 3)
            ]
        case .center:
            return [CGPoint(x: 0.5, y: 0.5)]
        case .goldenSpiral, .goldenTriangle:
            return [CGPoint(x: 0.382, y: 0.618), CGPoint(x: 0.618, y: 0.382)]
        case .diagonal:
            return [
                CGPoint(x: 0.25, y: 0.25), CGPoint(x: 0.75, y: 0.75),
                CGPoint(x: 0.75, y: 0.25), CGPoint(x: 0.25, y: 0.75)
            ]
        case .perspective:
            return [CGPoint(x: 0.5, y: 0.6)]
        default:
            return [
                CGPoint(x: 1.0 / 3, y: 1.0 / 3), CGPoint(x: 2.0 / 3, y: 1.0 / 3),
                CGPoint(x: 1.0 / 3, y: 2.0 / 3), CGPoint(x: 2.0 / 3, y: 2.0 / 3),
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
                    Button {
                        onDismiss()
                    } label: {
                        Text("返回")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color.black.opacity(0.5))
                            .cornerRadius(10)
                    }
                    Spacer()

                    Button {
                        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                        onDismiss()
                    } label: {
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
