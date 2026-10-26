import SwiftUI

// MARK: - 应用入口视图
struct ContentView: View {
    @State private var showGalleryMode = false

    var body: some View {
        Group {
            if showGalleryMode {
                CompositionHelperView(onOpenCamera: { showGalleryMode = false })
            } else {
                CameraCompositionView(onOpenGallery: { showGalleryMode = true })
            }
        }
    }
}
