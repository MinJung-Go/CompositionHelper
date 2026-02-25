import SwiftUI

// MARK: - 快门按钮
struct ShutterButton: View {
    let action: () -> Void
    @State private var isPressed = false

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.1)) {
                isPressed = true
            }
            action()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                withAnimation(.easeInOut(duration: 0.1)) {
                    isPressed = false
                }
            }
        } label: {
            ZStack {
                Circle()
                    .stroke(Color.white, lineWidth: 4)
                    .frame(width: 72, height: 72)

                Circle()
                    .fill(Color.white)
                    .frame(width: 60, height: 60)
                    .scaleEffect(isPressed ? 0.85 : 1.0)
            }
        }
    }
}
