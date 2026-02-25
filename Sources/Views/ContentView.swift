import SwiftUI

// MARK: - 应用入口视图
struct ContentView: View {
    @State private var showGalleryMode = false

    var body: some View {
        ZStack {
            // 默认：实时相机模式
            CameraCompositionView()

            // 导航到相册模式
            if showGalleryMode {
                CompositionHelperView()
                    .transition(.move(edge: .trailing))
            }
        }
    }
}
