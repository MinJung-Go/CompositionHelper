import SwiftUI
import PhotosUI

@MainActor
final class ColorEditorModel: ObservableObject {
    @Published var original: UIImage?
    @Published var preview: UIImage?
    @Published var plan = ColorPlan()
    @Published var strength = 1.0
    @Published var key = ""
    @Published var model = "gemini-3.5-flash"
    @Published var busy = false
    @Published var status = "选择照片开始调色"
    @Published var error: String?
    @Published var difference = ""
    @Published private(set) var lastAI: ColorPlan?
    @Published private(set) var history: [(ColorPlan, Double)] = []
    private var source: Data?
    private var thumbnail: Data?
    private var renderedPlan: ColorPlan?
    private var renderedStrength: Double?
    private var task: Task<Void, Never>?
    private var generation = UUID()
    private let photos = ColorPhotoService()
    var ready: Bool { !busy && preview != nil && renderedPlan == plan && renderedStrength == strength }

    func checkpoint() {
        if let last = history.last, last.0 == plan, last.1 == strength { return }
        history.append((plan,strength))
        if history.count > 20 { history.removeFirst() }
    }
    func cancel() {
        generation = UUID(); task?.cancel(); task = nil; busy = false
        status = "操作已取消" // Last complete preview is retained; ready still checks recipe identity.
    }
    private func run(_ message: String, operation: @escaping () async throws -> Void) {
        cancel(); let ticket = generation
        busy = true; error = nil; status = message
        task = Task {
            do { try await operation() }
            catch is CancellationError { }
            catch { if ticket == generation { self.error = error.localizedDescription; status = "操作失败，可重试" } }
            if ticket == generation { busy = false; task = nil }
        }
    }
    func load(_ data: Data) {
        run("正在打开照片…") { try await self.loadData(data) }
    }
    func load(_ item: PhotosPickerItem) {
        run("正在读取所选照片…") {
            guard let data = try await item.loadTransferable(type: Data.self) else { throw ColorFailure("无法读取照片，请重选") }
            try Task.checkCancellation()
            try await self.loadData(data)
        }
    }
    private func loadData(_ data: Data) async throws {
        let image = try await photos.render(data, plan: ColorPlan(), strength: 0)
        try Task.checkCancellation()
        source = data; thumbnail = image.jpeg; original = image.image; preview = image.image
        plan = ColorPlan(); strength = 1; lastAI = nil; history = []
        renderedPlan = plan; renderedStrength = strength; difference = "当前为原图，尚未调色"
        status = "照片已就绪"
    }
    private func renderCurrent() async throws {
        guard let data = source else { return }
        let snapshot = plan, amount = strength
        let result = try await photos.render(data, plan: snapshot, strength: amount)
        try Task.checkCancellation()
        preview = result.image; renderedPlan = snapshot; renderedStrength = amount
        difference = String(format: "预览变化像素 %.1f%% · 平均通道差 %.2f / 255", result.changedPercent, result.meanDifference)
        status = result.changedPercent == 0 ? "渲染完成，当前输出与原图相同" : "调色已渲染完成"
    }
    func render() { run("正在渲染调色…") { try await self.renderCurrent() } }
    func analyze() {
        guard let jpeg = thumbnail else { return }
        let apiKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        let modelName = model.trimmingCharacters(in: .whitespacesAndNewlines)
        run("AI 正在分析光线与色彩…") {
            let result = try await GeminiColorService().analyze(jpeg: jpeg, key: apiKey, model: modelName)
            try Task.checkCancellation()
            self.checkpoint(); self.plan = result; self.strength = 1; self.lastAI = result
            self.status = "AI 方案已收到，正在渲染…"
            try await self.renderCurrent()
        }
    }
    func reset() { checkpoint(); plan = ColorPlan(); strength = 1; render() }
    func undo() {
        guard let previous = history.popLast() else { return }
        plan = previous.0; strength = previous.1; render()
    }
    func restoreAI() { guard let value = lastAI else { return }; checkpoint(); plan = value; strength = 1; render() }
    func save() {
        guard ready, let data = source else { return }
        let snapshot = plan, amount = strength
        run("正在导出原尺寸副本…") {
            try await self.photos.save(data, plan: snapshot, strength: amount)
            try Task.checkCancellation()
            self.status = "副本已保存到相册，原片保留"
        }
    }
}
