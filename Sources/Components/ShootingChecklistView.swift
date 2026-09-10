import SwiftUI

private struct ShootingCheckItem: Identifiable {
    let id: String
    let group: String
    let title: String
    let detail: String
    static let all: [ShootingCheckItem] = [
        .init(id: "prepare_permission", group: "拍摄前", title: "相机能够取景", detail: "允许相机权限后，确认预览正常、没有黑屏或错误提示。"),
        .init(id: "prepare_lens", group: "拍摄前", title: "镜头与画面清晰", detail: "擦净镜头，确认主体清晰；遇到模糊时调整距离和光线。"),
        .init(id: "prepare_scene", group: "拍摄前", title: "主体与拍摄意图明确", detail: "确定要突出的人或物，观察背景，避免杂物抢占注意力。"),
        .init(id: "prepare_composition", group: "拍摄前", title: "选择合适的构图", detail: "切换所需构图方式，确认辅助线可见且与预期一致。"),
        .init(id: "capture_alignment", group: "取景拍摄", title: "主体与辅助线关系正确", detail: "移动手机，对照辅助线检查主体位置，注意地平线和垂直线。"),
        .init(id: "capture_edges", group: "取景拍摄", title: "四周边缘没有意外裁切", detail: "检查头顶、手脚和画面边缘，保留需要的主体与留白。"),
        .init(id: "capture_light", group: "取景拍摄", title: "亮部和暗部保留细节", detail: "观察主体与背景，避免重要区域过亮、过暗或出现明显偏色。"),
        .init(id: "capture_shutter", group: "取景拍摄", title: "完成一次真实拍摄", detail: "稳定手机后按快门，确认获得拍摄结果，而非仅停留在预览。"),
        .init(id: "review_photo", group: "拍摄后", title: "成片清晰且构图符合预期", detail: "查看真实成片，确认主体、方向、比例和裁切正确，没有明显抖动。"),
        .init(id: "review_color", group: "拍摄后", title: "调色结果经过前后对比", detail: "若使用调色，对照原图检查肤色、文字和细节；未调色可标记不适用。"),
        .init(id: "review_save", group: "拍摄后", title: "照片确实出现在系统相册", detail: "完成保存后，到系统相册打开照片核对，不能只依据成功提示。"),
        .init(id: "review_record", group: "拍摄后", title: "记录问题并决定是否重拍", detail: "对有问题的项目填写备注；必要时重拍，再重新检查。")
    ]
}

private enum ShootingCheckStatus: String, CaseIterable, Identifiable {
    case pending, confirmed, problem, skipped
    var id: String { rawValue }
    var title: String {
        switch self {
        case .pending: return "待检查"
        case .confirmed: return "已确认"
        case .problem: return "有问题"
        case .skipped: return "不适用"
        }
    }
    var icon: String {
        switch self {
        case .pending: return "circle"
        case .confirmed: return "checkmark.circle.fill"
        case .problem: return "exclamationmark.circle.fill"
        case .skipped: return "minus.circle"
        }
    }
    var color: Color {
        switch self {
        case .pending, .skipped: return .secondary
        case .confirmed: return Color(red: 0.89, green: 0.79, blue: 0.63)
        case .problem: return .orange
        }
    }
}

struct ShootingChecklistView: View {
    @AppStorage("shooting.checklist.v1") private var stored = ""
    @AppStorage("shooting.checklist.notes.v1") private var notes = ""
    @State private var confirmReset = false
    @Environment(\.dismiss) private var dismiss
    private let groups = ["拍摄前", "取景拍摄", "拍摄后"]
    private var statuses: [String: ShootingCheckStatus] {
        Dictionary(stored.split(separator: ";").compactMap { entry -> (String, ShootingCheckStatus)? in
            let parts = entry.split(separator: "=", maxSplits: 1)
            guard parts.count == 2, let status = ShootingCheckStatus(rawValue: String(parts[1])) else { return nil }
            return (String(parts[0]), status)
        }, uniquingKeysWith: { _, last in last })
    }
    private func status(_ item: ShootingCheckItem) -> ShootingCheckStatus { statuses[item.id] ?? .pending }
    private func count(_ value: ShootingCheckStatus) -> Int { ShootingCheckItem.all.filter { status($0) == value }.count }
    private func update(_ item: ShootingCheckItem, _ value: ShootingCheckStatus) {
        var values = statuses
        values[item.id] = value
        stored = values.keys.sorted().map { $0 + "=" + values[$0]!.rawValue }.joined(separator: ";")
    }
    private var report: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "未知"
        let rows = ShootingCheckItem.all.map { "[\(status($0).title)] \($0.group) · \($0.title)" }
        return (["拍摄检查记录 · iOS \(version)", Date().formatted(),
                 "本记录由使用者手动确认，不代表自动测试通过。"] + rows + ["备注：", notes.isEmpty ? "无" : notes]).joined(separator: "\n")
    }
    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("已确认 \(count(.confirmed))/\(ShootingCheckItem.all.count) · 有问题 \(count(.problem)) · 不适用 \(count(.skipped))")
                        .font(.headline).accessibilityIdentifier("shootingChecklistProgress")
                    ProgressView(value: Double(count(.confirmed)), total: Double(ShootingCheckItem.all.count))
                    Text("请实际操作后逐项确认。进度保存在本机，不会自动判定拍摄合格；新一轮拍摄前请重置。")
                        .font(.caption).foregroundColor(.secondary)
                }
                ForEach(groups, id: \.self) { group in
                    Section(group) {
                        ForEach(ShootingCheckItem.all.filter { $0.group == group }) { item in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(item.title).font(.headline)
                                Text(item.detail).font(.subheadline).foregroundColor(.secondary)
                                Menu {
                                    ForEach(ShootingCheckStatus.allCases) { value in
                                        Button { update(item, value) } label: { Label(value.title, systemImage: value.icon) }
                                    }
                                } label: {
                                    Label(status(item).title, systemImage: status(item).icon)
                                        .foregroundColor(status(item).color).padding(.vertical, 8)
                                        .frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                                }
                                .accessibilityLabel(item.title + "，" + status(item).title + "，更改检查结果")
                                .accessibilityIdentifier("shootingChecklist." + item.id)
                            }.padding(.vertical, 6)
                        }
                    }
                }
                Section("问题与备注") {
                    TextField("记录项目、问题现象和是否需要重拍", text: $notes, axis: .vertical)
                        .lineLimit(3...8)
                }
                Section {
                    ShareLink(item: report) { Label("导出检查记录", systemImage: "square.and.arrow.up") }
                    Button("开始新一轮检查", role: .destructive) { confirmReset = true }
                }
            }
            .navigationTitle("拍摄检查清单").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("完成") { dismiss() } } }
            .confirmationDialog("清空本轮检查结果和备注？", isPresented: $confirmReset, titleVisibility: .visible) {
                Button("清空并开始新一轮", role: .destructive) { stored = ""; notes = "" }
                Button("取消", role: .cancel) { }
            } message: { Text("如需保留，请先导出检查记录。此操作不会删除照片。") }
        }
        .tint(Color(red: 0.89, green: 0.79, blue: 0.63)).preferredColorScheme(.dark)
    }
}
