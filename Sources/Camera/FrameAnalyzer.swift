import AVFoundation
import Combine
import Vision

final class FrameAnalyzer: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    @Published private(set) var analysisResult: FrameAnalysisResult?
    @Published private(set) var detectedSubjects: [DetectedSubject] = []

    private struct Configuration {
        var enabled = false
        var aspectRatio: CGFloat = 1
        var generation = 0
    }

    private let configurationLock = NSLock()
    private var configuration = Configuration()
    // Frame timing is owned by the serial AVFoundation delegate queue.
    private var lastGeneration: Int?
    private var lastAnalysisTime: TimeInterval = 0
    // Recommendation state is owned by the main queue.
    private var stability = RecommendationStability()

    func configure(enabled: Bool, aspectRatio: CGFloat) {
        configurationLock.lock()
        let changed = configuration.enabled != enabled || configuration.aspectRatio != aspectRatio
        if changed {
            configuration = Configuration(
                enabled: enabled, aspectRatio: aspectRatio, generation: configuration.generation + 1
            )
        }
        configurationLock.unlock()

        if changed {
            stability = RecommendationStability(displayed: enabled ? analysisResult : nil)
            detectedSubjects = []
            if !enabled { analysisResult = nil }
        }
    }

    private func snapshot() -> Configuration {
        configurationLock.lock()
        defer { configurationLock.unlock() }
        return configuration
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let settings = snapshot()
        let now = ProcessInfo.processInfo.systemUptime
        guard settings.enabled,
              lastGeneration != settings.generation || now - lastAnalysisTime >= 0.2,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        lastGeneration = settings.generation
        lastAnalysisTime = now

        let viewport = RealtimeViewport(
            imageAspect: CGFloat(CVPixelBufferGetWidth(pixelBuffer)) / CGFloat(CVPixelBufferGetHeight(pixelBuffer)),
            previewAspect: settings.aspectRatio
        )
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up)
        let subjects = detectSubjects(with: handler, viewport: viewport)
        let diagonalScore = detectDiagonal(with: handler, viewport: viewport)
        let candidate = LightweightAnalyzer.analyze(subjects: subjects, diagonalScore: diagonalScore)

        DispatchQueue.main.async { [weak self] in
            guard let self, self.snapshot().generation == settings.generation else { return }
            self.detectedSubjects = subjects
            self.analysisResult = self.stability.update(candidate)
        }
    }

    private func detectSubjects(with handler: VNImageRequestHandler, viewport: RealtimeViewport) -> [DetectedSubject] {
        let faces = VNDetectFaceRectanglesRequest()
        let rectangles = VNDetectRectanglesRequest()
        rectangles.minimumSize = 0.1
        rectangles.maximumObservations = 5
        guard (try? handler.perform([faces, rectangles])) != nil else { return [] }

        let faceSubjects = (faces.results ?? []).compactMap { observation in
            viewport.subject(observation.boundingBox, confidence: Double(observation.confidence), type: .person)
        }
        let rectangleSubjects = (rectangles.results ?? []).compactMap { observation in
            viewport.subject(observation.boundingBox, confidence: Double(observation.confidence), type: .object)
        }
        return faceSubjects + rectangleSubjects
    }

    private func detectDiagonal(with handler: VNImageRequestHandler, viewport: RealtimeViewport) -> Float {
        let request = VNDetectContoursRequest()
        request.maximumImageDimension = 320
        guard (try? handler.perform([request])) != nil,
              let observation = request.results?.first else { return 0 }

        var best: Float = 0
        for index in 0..<observation.contourCount {
            guard let contour = try? observation.contour(at: index),
                  let polygon = try? contour.polygonApproximation(epsilon: 0.008) else { continue }
            let points = polygon.normalizedPoints
            for (start, end) in zip(points, points.dropFirst()) {
                let a = CGPoint(x: CGFloat(start.x), y: CGFloat(start.y))
                let b = CGPoint(x: CGFloat(end.x), y: CGFloat(end.y))
                if let (visibleStart, visibleEnd) = viewport.segment(a, b) {
                    best = max(best, LightweightAnalyzer.diagonalScore(visibleStart, visibleEnd))
                }
            }
        }
        return best
    }
}
