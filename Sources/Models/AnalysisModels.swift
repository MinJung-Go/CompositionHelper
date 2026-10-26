import Foundation
import CoreGraphics

// MARK: - 构图分析结果（全量分析，相册模式用）
struct CompositionAnalysis {
    var recommendedCompositions: [CompositionType]
    var confidenceScores: [CompositionType: Double]
    var detectedSubjects: [DetectedSubject]
    var detectedLines: [Line]
    var imageCharacteristics: ImageCharacteristics
}

// MARK: - 实时帧分析结果（相机模式用）
struct FrameAnalysisResult {
    var recommendedType: CompositionType
    var confidence: Float
    var detectedSubjects: [DetectedSubject]
    var guidanceHint: String?
}

// MARK: - 检测到的主体
struct DetectedSubject {
    var boundingBox: CGRect // 归一化坐标 (0-1)
    var confidence: Double
    var type: SubjectType
}

enum SubjectType {
    case person
    case animal
    case object
    case text
    case landmark
}

// MARK: - 检测到的线条
struct Line {
    var startPoint: CGPoint
    var endPoint: CGPoint
    var angle: Double
    var length: Double
}

// MARK: - 图像特征
struct ImageCharacteristics {
    var hasStrongLeadingLines: Bool
    var hasSymmetry: Bool
    var mainSubjectPosition: CGPoint
    var brightnessDistribution: BrightnessDistribution
    var colorHarmony: Double
}

enum BrightnessDistribution {
    case centerWeighted
    case balanced
    case cornerWeighted
    case leftWeighted
    case rightWeighted
}

// MARK: - 错误类型
enum AnalysisError: Error {
    case invalidImage
    case processingFailed
    case noResults
}
