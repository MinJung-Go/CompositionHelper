import AVFoundation
import Vision
import UIKit

// MARK: - 实时帧分析器
class FrameAnalyzer: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    @Published var analysisResult: FrameAnalysisResult?
    @Published var detectedSubjects: [DetectedSubject] = []

    private var lastAnalysisTime: CFAbsoluteTime = 0
    private let analysisInterval: CFAbsoluteTime = 2.5 // 2.5秒节流，与Android一致
    private var isAnalyzing = false

    // MARK: - 帧回调
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let now = CFAbsoluteTimeGetCurrent()
        guard now - lastAnalysisTime >= analysisInterval, !isAnalyzing else { return }

        lastAnalysisTime = now
        isAnalyzing = true

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            isAnalyzing = false
            return
        }

        analyzeFrame(pixelBuffer: pixelBuffer)
    }

    // MARK: - 分析单帧
    private func analyzeFrame(pixelBuffer: CVPixelBuffer) {
        let requestHandler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up)

        let faceRequest = VNDetectFaceRectanglesRequest()
        let rectangleRequest = VNDetectRectanglesRequest()
        rectangleRequest.minimumSize = 0.1
        rectangleRequest.maximumObservations = 5

        do {
            try requestHandler.perform([faceRequest, rectangleRequest])

            var subjects: [DetectedSubject] = []

            // 人脸结果
            if let faceResults = faceRequest.results {
                for face in faceResults {
                    subjects.append(DetectedSubject(
                        boundingBox: face.boundingBox,
                        confidence: Double(face.confidence),
                        type: .person
                    ))
                }
            }

            // 矩形结果（物体近似）
            if let rectResults = rectangleRequest.results {
                for rect in rectResults {
                    subjects.append(DetectedSubject(
                        boundingBox: rect.boundingBox,
                        confidence: Double(rect.confidence),
                        type: .object
                    ))
                }
            }

            // 轻量分析
            let result = LightweightAnalyzer.analyze(subjects: subjects)

            DispatchQueue.main.async { [weak self] in
                self?.detectedSubjects = subjects
                self?.analysisResult = result
                self?.isAnalyzing = false
            }
        } catch {
            DispatchQueue.main.async { [weak self] in
                self?.isAnalyzing = false
            }
        }
    }
}

// MARK: - 轻量分析器（与Android LightweightAnalyzer对齐）
struct LightweightAnalyzer {
    static func analyze(subjects: [DetectedSubject]) -> FrameAnalysisResult? {
        guard let mainSubject = subjects.max(by: {
            ($0.boundingBox.width * $0.boundingBox.height) < ($1.boundingBox.width * $1.boundingBox.height)
        }) else { return nil }

        let centerX = mainSubject.boundingBox.midX
        let centerY = mainSubject.boundingBox.midY
        let subjectArea = mainSubject.boundingBox.width * mainSubject.boundingBox.height

        var bestType: CompositionType = .ruleOfThirds
        var bestScore: Float = 0.0
        var hint: String?

        // 检查三分点
        let thirdPoints: [(CGFloat, CGFloat)] = [
            (1.0/3, 1.0/3), (2.0/3, 1.0/3),
            (1.0/3, 2.0/3), (2.0/3, 2.0/3)
        ]
        for (tx, ty) in thirdPoints {
            let dist = sqrt(pow(centerX - tx, 2) + pow(centerY - ty, 2))
            if dist < 0.15 {
                let score = Float(1.0 - dist / 0.15)
                if score > bestScore {
                    bestScore = score
                    bestType = .ruleOfThirds
                    hint = nil
                }
            }
        }

        // 检查中心
        let centerDist = sqrt(pow(centerX - 0.5, 2) + pow(centerY - 0.5, 2))
        if centerDist < 0.12 {
            let score = Float(1.0 - centerDist / 0.12)
            if score > bestScore {
                bestScore = score
                bestType = .center
                hint = nil
            }
        }

        // 检查对角线
        let diagDist1 = abs(centerX - centerY) / sqrt(2)
        let diagDist2 = abs(centerX - (1.0 - centerY)) / sqrt(2)
        let minDiagDist = min(diagDist1, diagDist2)
        if minDiagDist < 0.1 {
            let score = Float(1.0 - minDiagDist / 0.1) * 0.8
            if score > bestScore {
                bestScore = score
                bestType = .diagonal
                hint = nil
            }
        }

        // 检查充满画面
        if subjectArea > 0.6 {
            let score = Float(min(subjectArea / 0.85, 1.0)) * 0.85
            if score > bestScore {
                bestScore = score
                bestType = .fillTheFrame
                hint = nil
            }
        }

        // 检查负空间
        if subjectArea < 0.15 {
            let score: Float = 0.7
            if score > bestScore {
                bestScore = score
                bestType = .negativeSpace
                hint = nil
            }
        }

        // 方向提示
        if bestScore < 0.6 {
            hint = generateHint(centerX: centerX, centerY: centerY)
        }

        // 至少有默认推荐
        if bestScore < 0.3 {
            bestScore = 0.3
            bestType = .ruleOfThirds
            hint = generateHint(centerX: centerX, centerY: centerY)
        }

        return FrameAnalysisResult(
            recommendedType: bestType,
            confidence: bestScore,
            detectedSubjects: subjects,
            guidanceHint: hint
        )
    }

    private static func generateHint(centerX: CGFloat, centerY: CGFloat) -> String {
        var parts: [String] = []

        // 水平方向
        let nearestThirdX = centerX < 0.5 ? CGFloat(1.0/3) : CGFloat(2.0/3)
        let dx = nearestThirdX - centerX
        if abs(dx) > 0.08 {
            parts.append(dx > 0 ? "向右移动" : "向左移动")
        }

        // 垂直方向
        let nearestThirdY = centerY < 0.5 ? CGFloat(1.0/3) : CGFloat(2.0/3)
        let dy = nearestThirdY - centerY
        if abs(dy) > 0.08 {
            parts.append(dy > 0 ? "稍微向下" : "稍微向上")
        }

        return parts.isEmpty ? "位置不错" : parts.joined(separator: "，")
    }
}
