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
    private var portraitSelection = PortraitSelection()
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
        if lastGeneration != settings.generation { portraitSelection = PortraitSelection() }
        lastGeneration = settings.generation
        lastAnalysisTime = now

        let viewport = RealtimeViewport(
            imageAspect: CGFloat(CVPixelBufferGetWidth(pixelBuffer)) / CGFloat(CVPixelBufferGetHeight(pixelBuffer)),
            previewAspect: settings.aspectRatio
        )
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up)
        let faces = detectPortraits(with: handler, viewport: viewport)
        let portrait = portraitSelection.update(faces)
        let subjects = portrait == nil
            ? detectObjects(with: handler, viewport: viewport)
            : faces.map(\.subject)
        let diagonalScore = detectDiagonal(with: handler, viewport: viewport, pixelBuffer: pixelBuffer)
        let candidate = LightweightAnalyzer.analyze(
            subjects: subjects, diagonalScore: diagonalScore, portrait: portrait
        )

        DispatchQueue.main.async { [weak self] in
            guard let self, self.snapshot().generation == settings.generation else { return }
            self.detectedSubjects = subjects
            self.analysisResult = self.stability.update(candidate)
        }
    }

    private func detectPortraits(with handler: VNImageRequestHandler, viewport: RealtimeViewport) -> [Portrait] {
        let request = VNDetectFaceLandmarksRequest()
        guard (try? handler.perform([request])) != nil else { return [] }
        return (request.results ?? []).compactMap { observation in
            let box = observation.boundingBox
            guard abs(observation.yaw?.doubleValue ?? 0) <= .pi / 5,
                  let subject = viewport.subject(box, confidence: Double(observation.confidence), type: .person)
            else { return nil }

            func eyeCenter(_ eye: VNFaceLandmarkRegion2D?) -> CGPoint? {
                guard let points = eye?.normalizedPoints, !points.isEmpty else { return nil }
                let count = CGFloat(points.count)
                let meanX = points.reduce(0) { $0 + $1.x } / count
                let meanY = points.reduce(0) { $0 + $1.y } / count
                return viewport.point(CGPoint(x: box.minX + meanX * box.width, y: box.minY + meanY * box.height))
            }

            return Portrait(
                subject: subject, leftEye: eyeCenter(observation.landmarks?.leftEye),
                rightEye: eyeCenter(observation.landmarks?.rightEye)
            )
        }
    }

    private func detectObjects(with handler: VNImageRequestHandler, viewport: RealtimeViewport) -> [DetectedSubject] {
        let rectangles = VNDetectRectanglesRequest()
        rectangles.minimumSize = 0.1
        rectangles.maximumObservations = 5
        guard (try? handler.perform([rectangles])) != nil else { return [] }
        return (rectangles.results ?? []).compactMap { observation in
            viewport.subject(observation.boundingBox, confidence: Double(observation.confidence), type: .object)
        }
    }

    private func detectDiagonal(
        with handler: VNImageRequestHandler, viewport: RealtimeViewport, pixelBuffer: CVPixelBuffer
    ) -> Float {
        let request = VNDetectContoursRequest()
        request.maximumImageDimension = 320
        guard (try? handler.perform([request])) != nil,
              let observation = request.results?.first else { return 0 }

        var segments: [(CGPoint, CGPoint)] = []
        for index in 0..<observation.contourCount {
            guard let contour = try? observation.contour(at: index),
                  let polygon = try? contour.polygonApproximation(epsilon: 0.008) else { continue }
            let points = polygon.normalizedPoints
            for (start, end) in zip(points, points.dropFirst()) {
                let normalizedStart = CGPoint(x: CGFloat(start.x), y: CGFloat(start.y))
                let normalizedEnd = CGPoint(x: CGFloat(end.x), y: CGFloat(end.y))
                if let (visibleStart, visibleEnd) = viewport.segment(normalizedStart, normalizedEnd) {
                    segments.append((visibleStart, visibleEnd))
                }
            }
        }
        guard !segments.isEmpty, CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly) == kCVReturnSuccess else {
            return 0
        }
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }
        let format = CVPixelBufferGetPixelFormatType(pixelBuffer)
        let planar = format == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange ||
            format == kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
        guard planar || format == kCVPixelFormatType_32BGRA else { return 0 }
        let address = planar
            ? CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0)
            : CVPixelBufferGetBaseAddress(pixelBuffer)
        guard let data = address?.assumingMemoryBound(to: UInt8.self) else { return 0 }
        let width = planar ? CVPixelBufferGetWidthOfPlane(pixelBuffer, 0) : CVPixelBufferGetWidth(pixelBuffer)
        let height = planar ? CVPixelBufferGetHeightOfPlane(pixelBuffer, 0) : CVPixelBufferGetHeight(pixelBuffer)
        let stride = planar
            ? CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 0)
            : CVPixelBufferGetBytesPerRow(pixelBuffer)
        guard width > 0, height > 0 else { return 0 }
        return DiagonalEvidence.score(segments) { viewportX, viewportY in
            let point = viewport.imagePoint(CGPoint(x: viewportX, y: viewportY))
            let row = Int(((1 - point.y) * CGFloat(height - 1)).rounded()) * stride
            let column = Int((point.x * CGFloat(width - 1)).rounded())
            if planar { return Int(data[row + column]) }
            let offset = row + 4 * column
            return (29 * Int(data[offset]) + 150 * Int(data[offset + 1]) + 77 * Int(data[offset + 2])) / 256
        }
    }
}
