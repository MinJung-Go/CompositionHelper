import SwiftUI

// MARK: - AI 推荐浮层
struct RecommendationChip: View {
    let result: FrameAnalysisResult

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "brain")
                .font(.subheadline)
                .foregroundColor(.blue)

            Image(systemName: result.recommendedType.icon)
                .font(.subheadline)

            Text(result.recommendedType.rawValue)
                .font(.subheadline)
                .fontWeight(.medium)

            Text("\(Int(result.confidence * 100))%")
                .font(.caption)
                .foregroundColor(.secondary)

            if let hint = result.guidanceHint {
                Divider()
                    .frame(height: 16)

                Text(hint)
                    .font(.caption)
                    .foregroundColor(.orange)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.ultraThinMaterial)
        .cornerRadius(20)
    }
}
