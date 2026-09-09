import SwiftUI

@main
struct CompositionHelperApp: App {
    var body: some Scene {
        WindowGroup {
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("--review-color") {
                ColorEditorView(initialData: reviewPhoto)
            } else {
                ContentView()
            }
            #else
            ContentView()
            #endif
        }
    }
    #if DEBUG
    private var reviewPhoto: Data? {
        guard !ProcessInfo.processInfo.arguments.contains("--review-empty") else { return nil }
        return Bundle.main.url(forResource: "sample_city", withExtension: "jpg")
            .flatMap { try? Data(contentsOf: $0) }
    }
    #endif
}
