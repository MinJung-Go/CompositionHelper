import SwiftUI
import PhotosUI

struct ColorEditorView: View {
    var initialData: Data? = nil
    @StateObject private var editor = ColorEditorModel()
    @State private var selection: PhotosPickerItem?
    @State private var mode = "左右对比"
    @State private var divider = 0.5
    @State private var showSettings = false
    @State private var range = 0
    @Environment(\.dismiss) private var dismiss
    private let gold = Color(red:0.89, green:0.79, blue:0.63)
    private let tools = [("曝光","exposure"),("对比度","contrast"),("阴影","shadows"),("高光","highlights"),
                         ("色温","temperature"),("色调","tint"),("饱和度","saturation")]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment:.leading, spacing:20) {
                    PhotosPicker(selection:$selection, matching:.images) {
                        Label(editor.original == nil ? "选择照片" : "更换照片", systemImage:"photo.on.rectangle")
                            .frame(maxWidth:.infinity).padding()
                    }.buttonStyle(.bordered).disabled(editor.busy)
                    samples
                    DisclosureGroup("参考照片来源") {
                        Link("Kata · 湖泊", destination: URL(string:"https://www.pexels.com/photo/mountain-landscape-with-lake-14958494/")!)
                        Link("Aysegul Aytoren · 森林", destination: URL(string:"https://www.pexels.com/photo/a-forest-with-mossy-rocks-14755971/")!)
                        Link("Negative Space · 咖啡", destination: URL(string:"https://www.pexels.com/photo/caffeine-coffee-cup-mug-134577/")!)
                        Link("Mo Eid · 城市", destination: URL(string:"https://www.pexels.com/photo/drone-shot-of-city-with-skyscrapers-17910086/")!)
                        Link("Pexels 许可", destination: URL(string:"https://www.pexels.com/license/")!)
                    }.font(.caption)
                    if let original = editor.original {
                        Picker("预览方式", selection:$mode) {
                            ForEach(["左右对比","调色后","原图"], id:\.self) { Text($0) }
                        }.pickerStyle(.segmented)
                        comparison(original)
                        if mode == "左右对比" { Slider(value:$divider, in:0...1) { Text("前后对比分界") } }
                        Text(editor.status).foregroundColor(gold).accessibilityIdentifier("colorStatus")
                        Text(editor.difference).font(.caption).foregroundColor(.secondary)
                        if !editor.ready && !editor.busy { Text("当前预览尚未对应最新参数").font(.caption); Button("重试渲染") { editor.render() } }
                        if !editor.plan.scene.isEmpty { Text(editor.plan.scene).font(.headline) }
                        if !editor.plan.intent.isEmpty { Text(editor.plan.intent).foregroundColor(.secondary) }
                        adjustment("效果强度", value:$editor.strength, limits:0...1)
                        HStack {
                            Button("撤销") { editor.undo() }.disabled(editor.history.isEmpty)
                            Spacer(); Button("重置") { editor.reset() }
                            Spacer(); Button("恢复 AI 方案") { editor.restoreAI() }.disabled(editor.lastAI == nil)
                        }.disabled(editor.busy)
                        DisclosureGroup("实际调整参数") {
                            Text(editor.plan.summary).font(.system(.caption,design:.monospaced))
                                .frame(maxWidth:.infinity,alignment:.leading).textSelection(.enabled)
                        }
                        DisclosureGroup("手动精调") {
                            VStack(spacing:18) {
                                ForEach(tools, id:\.1) { tool in
                                    adjustment(tool.0, value:Binding(get:{ editor.plan.basic[tool.1] ?? 0 },
                                        set:{ editor.plan.basic[tool.1] = $0 }), limits:ColorPlan.limits[tool.1]!)
                                }
                                rgbControls
                            }.padding(.top)
                        }
                    } else if !editor.busy {
                        VStack(spacing:14) {
                            Image(systemName:"camera.aperture").font(.system(size:56))
                            Text("让照片呈现自己的色彩").font(.title2)
                            Text("选择照片或内置样片，体验 AI 与手动调色").font(.caption)
                        }.frame(maxWidth:.infinity).padding(.vertical,50)
                    }
                    if editor.busy { ProgressView(editor.status); Button("取消") { editor.cancel() } }
                    if let error = editor.error { Text(error).foregroundColor(.red) }
                    Text("点击 AI 调色才会发送缩略图给 Gemini；手动调色在本机完成。密钥只保留在本页内存中。").font(.caption).foregroundColor(.secondary)
                }.padding(20)
            }
            .background(Color(red:0.063,green:0.067,blue:0.075))
            .navigationTitle("色彩工作室").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement:.navigationBarLeading) { Button("完成") { dismiss() } }
                ToolbarItem(placement:.navigationBarTrailing) { Button("AI 设置") { showSettings = true }.disabled(editor.busy) }
            }
            .safeAreaInset(edge:.bottom) {
                HStack {
                    Button("AI 调色") { if editor.key.isEmpty { showSettings = true } else { editor.analyze() } }
                        .buttonStyle(.borderedProminent).disabled(editor.original == nil || editor.busy)
                    Button("保存副本") { editor.save() }.buttonStyle(.bordered).disabled(!editor.ready)
                }.frame(maxWidth:.infinity).padding().background(.ultraThinMaterial)
            }
            .sheet(isPresented:$showSettings) { settings }
            .onChange(of:selection) { item in if let item = item { editor.load(item) } }
            .task { if let data = initialData, editor.original == nil { editor.load(data) } }
            .onDisappear { editor.cancel() }
        }.tint(gold).preferredColorScheme(.dark)
    }
    private var samples: some View {
        ScrollView(.horizontal,showsIndicators:false) {
            HStack {
                ForEach(["lake","forest","coffee","city"],id:\.self) { name in
                    Button {
                        if let url = Bundle.main.url(forResource:"sample_"+name,withExtension:"jpg"), let data = try? Data(contentsOf:url) { editor.load(data) }
                    } label: {
                        Image("sample_"+name).resizable().scaledToFill().frame(width:92,height:68).clipped().cornerRadius(12)
                    }.disabled(editor.busy).accessibilityLabel("参考照片 "+name)
                }
            }
        }
    }
    private func comparison(_ original: UIImage) -> some View {
        Image(uiImage:mode == "调色后" ? (editor.preview ?? original) : original)
            .resizable().scaledToFit()
            .overlay {
                if mode == "左右对比" {
                    GeometryReader { geometry in
                        Image(uiImage:editor.preview ?? original).resizable().scaledToFit()
                            .mask(alignment:.trailing) { Rectangle().frame(width:geometry.size.width*(1-divider)) }
                        Rectangle().fill(.white).frame(width:2).offset(x:geometry.size.width*divider)
                        HStack { Text("原图"); Spacer(); Text("调色后") }.font(.caption).padding(8).foregroundColor(.white)
                    }
                }
            }.clipShape(RoundedRectangle(cornerRadius:18))
    }
    private func adjustment(_ title: String, value: Binding<Double>, limits: ClosedRange<Double>) -> some View {
        VStack {
            HStack { Text(title); Spacer(); Text(String(format:"%+.4f",value.wrappedValue)).monospacedDigit().foregroundColor(gold) }
            Slider(value:value,in:limits,onEditingChanged:{ editing in if editing { editor.checkpoint() } else { editor.render() } })
                .accessibilityLabel(title)
        }.disabled(editor.busy)
    }
    private var rgbControls: some View {
        VStack(alignment:.leading,spacing:16) {
            Text("RGB 校色").font(.headline)
            Text("RGB 为零不代表其他调色未生效，请查看实际调整参数。").font(.caption).foregroundColor(.secondary)
            Picker("调整方式",selection:Binding(get:{ editor.plan.complement },set:{ editor.checkpoint(); editor.plan.complement = $0; editor.render() })) {
                Text("直接调原色").tag(false); Text("反向调互补色").tag(true)
            }.pickerStyle(.segmented)
            Picker("明暗范围",selection:$range) {
                ForEach(0..<4) { i in Text(["整体","阴影","中间调","高光"][i]).tag(i) }
            }.pickerStyle(.segmented)
            ForEach(0..<3) { i in
                adjustment(["青 ← R → 红","品红 ← G → 绿","黄 ← B → 蓝"][i],
                    value:Binding(get:{ editor.plan.rgb[range][i] },set:{ editor.plan.rgb[range][i] = $0 }),limits: -0.08...0.08)
            }
        }.disabled(editor.busy)
    }
    private var settings: some View {
        NavigationStack {
            Form {
                SecureField("Gemini API Key",text:$editor.key).textInputAutocapitalization(.never).autocorrectionDisabled()
                TextField("模型",text:$editor.model).textInputAutocapitalization(.never).autocorrectionDisabled()
                Text("使用自己的密钥。关闭调色页面后清除，不写入偏好设置或备份。")
                Button("清除密钥",role:.destructive) { editor.key = "" }
            }.navigationTitle("AI 设置").toolbar { ToolbarItem(placement:.confirmationAction) { Button("完成") { showSettings = false } } }
        }
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
