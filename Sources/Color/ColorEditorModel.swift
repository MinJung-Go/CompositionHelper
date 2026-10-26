import SwiftUI
import PhotosUI

@MainActor
final class ColorEditorModel: ObservableObject {
    @Published var original: UIImage?
    @Published var preview: UIImage?
    @Published var transientPreview: UIImage?
    @Published var timing = ""
    @Published var plan = ColorPlan()
    @Published var strength = 1.0
    @Published var key = ""
    @Published var model = "glm-5.3-flash"
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
        generation = UUID(); task?.cancel(); task = nil; busy = false; transientPreview = nil
        status = "操作已取消" // Last complete preview is retained; ready still checks recipe identity.
    }
    private func run(_ message: String, operation: @escaping () async throws -> Void) {
        cancel(); let ticket = generation
        busy = true; error = nil; status = message
        task = Task {
            do { try await operation() }
            catch is CancellationError { }
            catch { if ticket == generation { self.error = error.localizedDescription; status = "操作失败，可重试" } }
            if ticket == generation { busy = false; task = nil; transientPreview = nil }
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
        timing = ""; source = data; thumbnail = image.jpeg; original = image.image; preview = image.image
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
        timing = ""
        run("正在发送照片并等待首个结果…") {
            let ticket = self.generation
            let started = ProcessInfo.processInfo.systemUptime
            var firstParameters: Double?
            var firstPreview: Double?
            var lastFrame = -Double.infinity
            let result = try await GeminiColorService().analyze(jpeg: jpeg, key: apiKey, model: modelName) { partial in
                try Task.checkCancellation()
                guard ticket == self.generation else { throw CancellationError() }
                let now = ProcessInfo.processInfo.systemUptime
                if firstParameters == nil { firstParameters = now - started }
                if now - lastFrame >= 0.5 {
                    lastFrame = now
                    let rendered = try await self.photos.render(jpeg, plan: partial, strength: 1, previewLimit: 640)
                    try Task.checkCancellation()
                    guard ticket == self.generation else { throw CancellationError() }
                    self.transientPreview = rendered.image
                    if firstPreview == nil { firstPreview = ProcessInfo.processInfo.systemUptime - started }
                    self.status = "临时预览 · AI 仍在完善调色…"
                }
            }
            try Task.checkCancellation()
            let elapsed = ProcessInfo.processInfo.systemUptime - started
            self.timing = "有效参数 " + (firstParameters.map { String(format: "%.1f 秒", $0) } ?? "随完整响应到达") +
                " · 首次预览 " + (firstPreview.map { String(format: "%.1f 秒", $0) } ?? "未生成临时预览") + String(format: " · 完整响应 %.1f 秒", elapsed)
            self.status = "完整方案已收到，正在渲染…"
            guard let data = self.source else { throw CancellationError() }
            let renderStarted = ProcessInfo.processInfo.systemUptime
            let rendered = try await self.photos.render(data, plan: result, strength: 1)
            try Task.checkCancellation()
            guard ticket == self.generation else { throw CancellationError() }
            self.checkpoint(); self.plan = result; self.strength = 1; self.lastAI = result
            self.preview = rendered.image; self.renderedPlan = result; self.renderedStrength = 1
            self.transientPreview = nil
            self.difference = String(format: "预览变化像素 %.1f%% · 平均通道差 %.2f / 255", rendered.changedPercent, rendered.meanDifference)
            self.timing += String(format: " · 最终渲染 %.1f 秒", ProcessInfo.processInfo.systemUptime - renderStarted)
            self.status = rendered.changedPercent == 0 ? "渲染完成，当前输出与原图相同" : "调色已渲染完成"
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
