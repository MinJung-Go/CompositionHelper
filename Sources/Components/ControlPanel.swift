import SwiftUI

// MARK: - 底部构图控制面板
struct ControlPanel: View {
    @Binding var selectedCategory: CompositionCategory
    @Binding var selectedComposition: CompositionType

    var body: some View {
        VStack(spacing: 8) {
            // 分类选择
            HStack(spacing: 12) {
                ForEach(CompositionCategory.allCases) { category in
                    Button(action: { selectedCategory = category }) {
                        Text(category.rawValue)
                            .font(.caption)
                            .fontWeight(selectedCategory == category ? .bold : .regular)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(
                                selectedCategory == category
                                    ? Color.white.opacity(0.3)
                                    : Color.white.opacity(0.1)
                            )
                            .foregroundColor(.white)
                            .cornerRadius(16)
                    }
                }
            }

            // 构图类型滚动选择
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    let filtered = CompositionType.allCases.filter { $0.category == selectedCategory }
                    ForEach(filtered) { type in
                        Button(action: { selectedComposition = type }) {
                            VStack(spacing: 4) {
                                Image(systemName: type.icon)
                                    .font(.system(size: 18))
                                Text(type.rawValue)
                                    .font(.system(size: 9))
                                    .lineLimit(1)
                            }
                            .frame(width: 56, height: 52)
                            .background(
                                selectedComposition == type
                                    ? Color.blue.opacity(0.6)
                                    : Color.black.opacity(0.4)
                            )
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(
                                        selectedComposition == type ? Color.blue : Color.clear,
                                        lineWidth: 2
                                    )
                            )
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.3))
    }
}
