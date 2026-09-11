import SwiftUI
import PhotosUI

private enum StudioStyle {
    static let background = Color(red: 0.063, green: 0.067, blue: 0.075)
    static let surface = Color(red: 0.105, green: 0.11, blue: 0.12)
    static let gold = Color(red: 0.89, green: 0.79, blue: 0.63)
}

private enum StudioSheet: String, Identifiable {
    case settings, samples
    var id: String { rawValue }
}

private enum PreviewMode: String, CaseIterable {
    case comparison = "对比", edited = "调色后", original = "原图"
}

struct ColorEditorView: View {
    var initialData: Data? = nil
    @StateObject private var editor = ColorEditorModel()
    @State private var selection: PhotosPickerItem?
    @State private var mode = PreviewMode.comparison
    @State private var divider = 0.5
    @State private var sheet: StudioSheet?
    @State private var range = 0
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    private let tools = [("曝光", "exposure"), ("对比度", "contrast"), ("阴影", "shadows"),
                         ("高光", "highlights"), ("色温", "temperature"), ("色调", "tint"), ("饱和度", "saturation")]

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if let original = editor.original {
                            photoToolbar
                            comparison(original, available: geometry.size)
                            previewControls
                            recipeControls
                        } else {
                            emptyState
                            sampleGallery
                        }
                    }
                    .frame(maxWidth: 720)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .frame(maxWidth: .infinity)
                }
            }
            .background(StudioStyle.background)
            .navigationTitle("色彩工作室")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark").font(.body.weight(.medium)).frame(width: 44, height: 44)
                    }.accessibilityLabel("关闭色彩工作室")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { sheet = .settings } label: {
                        Image(systemName: "slider.horizontal.3").frame(width: 44, height: 44)
                    }.accessibilityLabel("AI 设置")
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) { actionBar }
            .sheet(item: $sheet) { destination in
                switch destination {
                case .settings:
                    StudioSettingsView(key: $editor.key, model: $editor.model)
                case .samples:
                    NavigationStack {
                        ScrollView { sampleGallery.padding(20) }
                            .background(StudioStyle.background)
                            .navigationTitle("挑一张样片")
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .confirmationAction) {
                                    Button("完成") { sheet = nil }
                                }
                            }
                    }
                }
            }
            .onChange(of: selection) { item in
                if let item = item { editor.load(item) }
            }
            .task {
                if let data = initialData, editor.original == nil { editor.load(data) }
            }
            .onDisappear { editor.cancel() }
        }
        .tint(StudioStyle.gold)
        .preferredColorScheme(.dark)
    }

    private var photoToolbar: some View {
        HStack {
            if !dynamicTypeSize.isAccessibilitySize { Text("你的影像").font(.headline) }
            Spacer()
            PhotosPicker(selection: $selection, matching: .images) {
                if dynamicTypeSize.isAccessibilitySize {
                    Image(systemName: "photo.on.rectangle")
                        .font(.system(size: 20)).frame(width: 44, height: 44)
                } else {
                    Label("换照片", systemImage: "photo.on.rectangle")
                        .font(.subheadline).padding(.vertical, 10)
                }
            }.disabled(editor.busy).accessibilityLabel("更换照片")
            Button { sheet = .samples } label: {
                Image(systemName: "square.grid.2x2").font(.system(size: 20)).frame(width: 44, height: 44)
            }.disabled(editor.busy).accessibilityLabel("选择内置样片")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 18) {
            Image(systemName: "camera.filters")
                .font(.system(size: 42, weight: .light)).foregroundColor(StudioStyle.gold)
                .frame(width: 88, height: 88)
                .background(StudioStyle.gold.opacity(0.08), in: RoundedRectangle(cornerRadius: 28))
            Text("让光影，有自己的色彩").font(.title2.weight(.semibold))
            Text("从一张照片开始，探索 AI 灵感与手动调色。")
                .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
            PhotosPicker(selection: $selection, matching: .images) {
                Label("从相册选择", systemImage: "plus")
                    .font(.headline).frame(maxWidth: .infinity).padding(.vertical, 15)
            }.buttonStyle(StudioButtonStyle(primary: true)).disabled(editor.busy)
        }.padding(.vertical, 24)
    }

    private var sampleGallery: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("先用样片试一试").font(.headline)
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 130), spacing: 12)], spacing: 12) {
                ForEach(StudioSample.all) { sample in
                    Button {
                        guard let url = sample.url, let data = try? Data(contentsOf: url) else {
                            editor.error = "样片读取失败，请从相册选择照片。"
                            return
                        }
                        editor.load(data)
                        sheet = nil
                    } label: {
                        ZStack(alignment: .bottomLeading) {
                            if let image = sample.image {
                                GeometryReader { geometry in
                                    Image(uiImage: image).resizable().scaledToFill()
                                        .frame(width: geometry.size.width, height: 128).clipped()
                                }
                            } else {
                                Rectangle().fill(StudioStyle.surface).frame(height: 128)
                                    .overlay(Image(systemName: "photo"))
                            }
                            LinearGradient(colors: [.clear, .black.opacity(0.75)], startPoint: .center, endPoint: .bottom)
                            Text(sample.title).font(.subheadline.weight(.medium)).padding(12)
                        }
                        .frame(height: 128).clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .foregroundColor(.white)
                    }.buttonStyle(.plain).disabled(editor.busy)
                        .accessibilityLabel("使用" + sample.title + "样片")
                }
            }
            DisclosureGroup("摄影作品与来源") {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(StudioSample.all) { sample in
                        Link(sample.credit + " · " + sample.title, destination: sample.creditURL)
                    }
                    Link("Pexels 许可", destination: URL(string: "https://www.pexels.com/license/")!)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(.top, 12)
            }.font(.caption).foregroundColor(.secondary)
        }
    }

    private func comparison(_ original: UIImage, available: CGSize) -> some View {
        let ratio = original.size.width / max(original.size.height, 1)
        let width = min(max(available.width - 40, 1), 720)
        let minimumHeight: CGFloat = dynamicTypeSize.isAccessibilitySize ? 140 : 220
        let height = min(width / ratio, max(minimumHeight, min(available.height * 0.62, 520)))
        return ZStack {
            RoundedRectangle(cornerRadius: 20).fill(Color.black)
            Image(uiImage: mode == .edited ? (editor.transientPreview ?? editor.preview ?? original) : original)
                .resizable().scaledToFit()
                .overlay {
                    if mode == .comparison {
                        GeometryReader { geometry in
                            Image(uiImage: editor.transientPreview ?? editor.preview ?? original).resizable().scaledToFit()
                                .mask(alignment: .trailing) {
                                    Rectangle().frame(width: geometry.size.width * (1 - divider))
                                }
                            Rectangle().fill(.white.opacity(0.9)).frame(width: 1)
                                .offset(x: geometry.size.width * divider)
                        }
                    }
                }
                .frame(width: min(width, height * ratio), height: height)
        }
        .frame(height: height)
        .overlay(alignment: .top) {
            HStack {
                if mode != .edited { photoBadge("原图") }
                Spacer()
                if mode != .original { photoBadge("调色后") }
            }.padding(12)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .accessibilityLabel("照片预览，" + mode.rawValue)
    }

    private func photoBadge(_ text: String) -> some View {
        Text(text).font(.system(size: 12, weight: .medium)).foregroundColor(.white)
            .padding(.horizontal, 10).padding(.vertical, 5)
            .background(.black.opacity(0.5), in: Capsule())
    }

    private var previewControls: some View {
        VStack(spacing: 10) {
            Picker("预览方式", selection: $mode) {
                ForEach(PreviewMode.allCases, id: \.self) { Text($0.rawValue).tag($0) }
            }.pickerStyle(.segmented)
            if mode == .comparison {
                HStack(spacing: 12) {
                    Image(systemName: "arrow.left.and.right").foregroundColor(.secondary)
                    Slider(value: $divider, in: 0...1).accessibilityLabel("前后对比分界")
                }
            }
        }
    }

    private var recipeControls: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Label("调色方案", systemImage: "camera.filters").font(.headline)
                Spacer()
                Button { editor.undo() } label: { Image(systemName: "arrow.uturn.backward").frame(width: 44, height: 44) }
                    .disabled(editor.history.isEmpty || editor.busy).accessibilityLabel("撤销调整")
                Button("重置") { editor.reset() }.font(.subheadline).disabled(editor.busy)
            }
            if !editor.plan.scene.isEmpty { Text(editor.plan.scene).font(.subheadline.weight(.medium)) }
            if !editor.plan.intent.isEmpty { Text(editor.plan.intent).font(.subheadline).foregroundColor(.secondary) }
            adjustment("效果强度", value: $editor.strength, limits: 0...1, percentage: true)
            DisclosureGroup("手动精调") {
                VStack(spacing: 20) {
                    ForEach(tools, id: \.1) { tool in
                        adjustment(tool.0, value: Binding(get: { editor.plan.basic[tool.1] ?? 0 },
                            set: { editor.plan.basic[tool.1] = $0 }), limits: ColorPlan.limits[tool.1]!)
                    }
                    rgbControls
                }.padding(.top, 20)
            }
            if editor.lastAI != nil {
                Button("恢复 AI 方案") { editor.restoreAI() }.disabled(editor.busy)
            }
            DisclosureGroup("调整详情") {
                VStack(alignment: .leading, spacing: 12) {
                    if !editor.timing.isEmpty { Text(editor.timing).foregroundColor(.secondary) }
                    Text(editor.difference).foregroundColor(.secondary)
                    Text(editor.plan.summary).font(.system(.caption, design: .monospaced)).textSelection(.enabled)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(.top, 12)
            }.font(.subheadline)
        }
        .padding(18)
        .background(StudioStyle.surface, in: RoundedRectangle(cornerRadius: 20))
    }

    private var actionBar: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let error = editor.error {
                Label(error, systemImage: "exclamationmark.circle").foregroundColor(.orange).font(.caption)
                    .fixedSize(horizontal: false, vertical: true)
            }
            HStack(spacing: 10) {
                if editor.busy { ProgressView().tint(StudioStyle.gold) }
                Text(editor.status).font(.caption).foregroundColor(.secondary)
                    .accessibilityIdentifier("colorStatus")
                Spacer(minLength: 0)
                if editor.busy {
                    Button("取消") { editor.cancel() }.font(.caption.weight(.semibold))
                } else if editor.original != nil && !editor.ready {
                    Button("重试渲染") { editor.render() }.font(.caption.weight(.semibold))
                }
            }
            if editor.original != nil {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 12) { primaryActions }
                    VStack(spacing: 10) { primaryActions }
                }
            }
        }
        .frame(maxWidth: 720).padding(.horizontal, 20).padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(StudioStyle.background)
        .overlay(alignment: .top) { Rectangle().fill(.white.opacity(0.08)).frame(height: 1) }
    }

    @ViewBuilder private var primaryActions: some View {
        Button {
            if editor.key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { sheet = .settings }
            else { editor.analyze() }
        } label: {
            Label("AI 调色", systemImage: "sparkles").font(.headline)
                .fixedSize(horizontal: true, vertical: false).frame(maxWidth: .infinity).padding(.vertical, 14)
        }.buttonStyle(StudioButtonStyle(primary: true)).disabled(editor.busy)
        Button { editor.save() } label: {
            Label("保存副本", systemImage: "square.and.arrow.down").font(.headline)
                .fixedSize(horizontal: true, vertical: false).frame(maxWidth: .infinity).padding(.vertical, 14)
        }.buttonStyle(StudioButtonStyle(primary: false)).disabled(!editor.ready)
    }

    private func adjustment(_ title: String, value: Binding<Double>, limits: ClosedRange<Double>, percentage: Bool = false) -> some View {
        VStack(spacing: 6) {
            HStack {
                Text(title).font(.subheadline)
                Spacer()
                Text(percentage ? "\(Int(value.wrappedValue * 100))%" : String(format: "%+.4f", value.wrappedValue))
                    .font(.caption.monospacedDigit()).foregroundColor(StudioStyle.gold)
            }
            Slider(value: value, in: limits, onEditingChanged: { editing in
                if editing { editor.checkpoint() } else { editor.render() }
            }).accessibilityLabel(title)
        }.disabled(editor.busy)
    }

    private var rgbControls: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("RGB 校色").font(.headline)
            Picker("调整方式", selection: Binding(get: { editor.plan.complement }, set: {
                editor.checkpoint(); editor.plan.complement = $0; editor.render()
            })) {
                Text("原色").tag(false); Text("互补色").tag(true)
            }.pickerStyle(.segmented)
            Picker("明暗范围", selection: $range) {
                ForEach(0..<4) { i in Text(["整体", "阴影", "中间调", "高光"][i]).tag(i) }
            }.pickerStyle(.segmented)
            ForEach(0..<3) { i in
                adjustment(["青 — 红", "品红 — 绿", "黄 — 蓝"][i],
                    value: Binding(get: { editor.plan.rgb[range][i] }, set: { editor.plan.rgb[range][i] = $0 }), limits: -0.08...0.08)
            }
        }.disabled(editor.busy)
    }
}

private struct StudioButtonStyle: ButtonStyle {
    var primary: Bool
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(isEnabled ? (primary ? StudioStyle.background : StudioStyle.gold) : Color.white.opacity(0.45))
            .background(isEnabled && primary ? StudioStyle.gold : StudioStyle.surface, in: RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(.white.opacity(primary ? 0 : 0.1)))
            .opacity(configuration.isPressed ? 0.75 : 1)
    }
}

private struct StudioSample: Identifiable {
    let id: String
    let title: String
    let credit: String
    let creditURL: URL
    var url: URL? { Bundle.main.url(forResource: "sample_" + id, withExtension: "jpg") }
    // Decode the bundled JPEG once, instead of asking the asset catalog for a missing named image.
    let image: UIImage?

    init(_ id: String, _ title: String, _ credit: String, _ link: String) {
        self.id = id; self.title = title; self.credit = credit
        creditURL = URL(string: link)!
        image = Bundle.main.url(forResource: "sample_" + id, withExtension: "jpg")
            .flatMap { UIImage(contentsOfFile: $0.path) }
    }

    static let all = [
        StudioSample("lake", "湖泊", "Kata", "https://www.pexels.com/photo/mountain-landscape-with-lake-14958494/"),
        StudioSample("forest", "森林", "Aysegul Aytoren", "https://www.pexels.com/photo/a-forest-with-mossy-rocks-14755971/"),
        StudioSample("coffee", "咖啡", "Negative Space", "https://www.pexels.com/photo/caffeine-coffee-cup-mug-134577/"),
        StudioSample("city", "城市", "Mo Eid", "https://www.pexels.com/photo/drone-shot-of-city-with-skyscrapers-17910086/")
    ]
}

private struct StudioSettingsView: View {
    @Binding var key: String
    @Binding var model: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("AI 服务") {
                    Picker("服务商", selection: Binding(
                        get: { model.hasPrefix("glm-") },
                        set: { useGLM in
                            key = ""
                            model = useGLM ? "glm-5.3-flash" : "gemini-3.5-flash"
                        }
                    )) {
                        Text("智谱 GLM").tag(true)
                        Text("Google Gemini").tag(false)
                    }
                    SecureField("API Key", text: $key)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                    if model.hasPrefix("glm-") {
                        Text("模型：GLM-5.3-Flash · 深度思考已开启")
                    } else {
                        TextField("模型", text: Binding(get: { model }, set: { if !$0.hasPrefix("glm-") { model = $0 } }))
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                    }
                    Link("获取 API Key", destination: URL(string: model.hasPrefix("glm-")
                        ? "https://docs.bigmodel.cn/cn/guide/start/quick-start"
                        : "https://aistudio.google.com/apikey")!)
                }
                Section {
                    Text("点击 AI 调色时，会将照片缩略图发送给所选服务商。手动调色在本机完成。")
                    Text("密钥只保留在当前编辑器内存中，关闭编辑器后清除。")
                }
                Section { Button("清除密钥", role: .destructive) { key = "" } }
            }
            .navigationTitle("AI 设置").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } } }
        }.tint(StudioStyle.gold).preferredColorScheme(.dark)
    }
}

struct ColorEditorView_Previews: PreviewProvider {
    static var previews: some View {
        ColorEditorView().previewDisplayName("选择照片")
        ColorEditorView(initialData: Bundle.main.url(forResource: "sample_city", withExtension: "jpg")
            .flatMap { try? Data(contentsOf: $0) }).previewDisplayName("照片编辑")
    }
}

// UIImage pickers/camera previews may keep rotation in imageOrientation rather than pixels.
// Normalize that orientation before creating a data-backed editor source.
extension UIImage {
    func colorEditorSourceData() -> Data? {
        guard imageOrientation != .up else { return pngData() }
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        let normalized = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
        return normalized.pngData()
    }
}
