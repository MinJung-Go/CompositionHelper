import SwiftUI
import PhotosUI

// MARK: - 相册模式主视图
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
    @State private var confidenceScores: [CompositionType: Double] = [:]
    @State private var showAnalysisResults = false

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 图像显示区域
                ZStack {
                    if let image = selectedImage {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .overlay(
                                CompositionOverlayView(
                                    compositionType: selectedComposition,
                                    opacity: lineOpacity,
                                    color: lineColor
                                )
                            )
                    } else {
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
                                isRecommended: recommendedCompositions.contains(composition) && autoAnalyzed,
                                confidence: recommendedCompositions.contains(composition)
                                    ? confidenceScores[composition] : nil
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
                    HStack {
                        Text("辅助线透明度")
                            .font(.subheadline)
                        Slider(value: $lineOpacity, in: 0.1...1.0)
                        Text("\(Int(lineOpacity * 100))%")
                            .font(.subheadline)
                            .frame(width: 40)
                    }
                    .padding(.horizontal)

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
                                .onTapGesture { lineColor = color }
                        }
                    }
                    .padding(.horizontal)

                    HStack(spacing: 16) {
                        Button {
                            showCamera = true
                        } label: {
                            Label("拍照", systemImage: "camera")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }

                        Button {
                            showImagePicker = true
                        } label: {
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

                    // 分析结果
                    if showAnalysisResults && autoAnalyzed {
                        VStack(spacing: 12) {
                            Divider()
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                Text("分析完成")
                                    .font(.headline)
                                    .foregroundColor(.green)
                                Spacer()
                            }
                            .padding(.horizontal)

                            VStack(alignment: .leading, spacing: 8) {
                                Text("推荐构图:")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)

                                ForEach(recommendedCompositions.prefix(3), id: \.self) { composition in
                                    let confidence = confidenceScores[composition] ?? 0.0
                                    HStack {
                                        Image(systemName: composition.icon)
                                            .foregroundColor(.blue)
                                            .frame(width: 24)
                                        Text(composition.rawValue)
                                            .font(.subheadline)
                                        Spacer()
                                        Text("\(Int(confidence * 100))%")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                            .frame(width: 40, alignment: .trailing)
                                    }
                                }
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 8)
                        }
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(10)
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                    }
                }
                .padding()
            }
            .navigationTitle("构图辅助")
            .navigationBarTitleDisplayMode(.inline)
        }
        .sheet(isPresented: $showImagePicker) {
            ImagePicker { image in
                selectedImage = image
                autoAnalyzed = false
                recommendedCompositions = []
            }
        }
        .sheet(isPresented: $showCamera) {
            SimpleCameraView { image in
                selectedImage = image
                autoAnalyzed = false
                recommendedCompositions = []
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
                    self.confidenceScores = analysis.confidenceScores
                    self.autoAnalyzed = true
                    self.showAnalysisResults = true
                    if let first = analysis.recommendedCompositions.first {
                        self.selectedComposition = first
                    }
                }
            } catch {
                await MainActor.run {
                    self.recommendedCompositions = [.ruleOfThirds, .center, .leadingLines]
                    self.autoAnalyzed = true
                    self.selectedComposition = .ruleOfThirds
                }
            }
        }
    }
}

// MARK: - 构图类型按钮
struct CompositionButton: View {
    let type: CompositionType
    let isSelected: Bool
    let isRecommended: Bool
    let confidence: Double?
    let action: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button(action: action) {
                VStack(spacing: 6) {
                    Image(systemName: type.icon)
                        .font(.system(size: 22))
                    Text(type.rawValue)
                        .font(.caption2)
                        .lineLimit(2)
                        .multilineTextAlignment(.center)

                    if isRecommended, let conf = confidence {
                        Text("\(Int(conf * 100))%")
                            .font(.caption2)
                            .foregroundColor(.blue)
                            .fontWeight(.semibold)
                    }
                }
                .frame(width: 72, height: isRecommended && confidence != nil ? 100 : 80)
                .background(isSelected ? Color.blue.opacity(0.2) : Color.gray.opacity(0.1))
                .foregroundColor(isSelected ? .blue : .primary)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
                )
            }

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

// MARK: - 图片选择器
struct ImagePicker: UIViewControllerRepresentable {
    let onImagePicked: (UIImage) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.filter = .images
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onImagePicked: onImagePicked)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let onImagePicked: (UIImage) -> Void

        init(onImagePicked: @escaping (UIImage) -> Void) {
            self.onImagePicked = onImagePicked
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)
            guard let result = results.first else { return }
            if result.itemProvider.canLoadObject(ofClass: UIImage.self) {
                result.itemProvider.loadObject(ofClass: UIImage.self) { object, _ in
                    if let image = object as? UIImage {
                        DispatchQueue.main.async {
                            self.onImagePicked(image)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 简单相机视图（相册模式用）
struct SimpleCameraView: UIViewControllerRepresentable {
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

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
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
