import Foundation
import Vision
import CoreImage
import UIKit

// MARK: - 构图分析结果
struct CompositionAnalysis {
    var recommendedCompositions: [CompositionType]
    var confidenceScores: [CompositionType: Double]
    var detectedSubjects: [DetectedSubject]
    var detectedLines: [Line]
    var imageCharacteristics: ImageCharacteristics
}

// MARK: - 检测到的主体
struct DetectedSubject {
    var boundingBox: CGRect
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

// MARK: - 构图分析器
class CompositionAnalyzer {
    
    // MARK: - 主分析方法
    static func analyze(image: UIImage) async throws -> CompositionAnalysis {
        guard let ciImage = CIImage(image: image) else {
            throw AnalysisError.invalidImage
        }
        
        async let subjects = detectSubjects(in: ciImage)
        async let lines = detectLines(in: ciImage)
        async let characteristics = analyzeCharacteristics(of: ciImage)
        
        let detectedSubjects = try await subjects
        let detectedLines = try await lines
        let imageCharacteristics = try await characteristics
        
        let recommendations = recommendCompositions(
            subjects: detectedSubjects,
            lines: detectedLines,
            characteristics: imageCharacteristics
        )
        
        return CompositionAnalysis(
            recommendedCompositions: recommendations.map { $0.type },
            confidenceScores: Dictionary(uniqueKeysWithValues: recommendations.map { ($0.type, $0.score) }),
            detectedSubjects: detectedSubjects,
            detectedLines: detectedLines,
            imageCharacteristics: imageCharacteristics
        )
    }
    
    // MARK: - 主体检测
    private static func detectSubjects(in image: CIImage) async throws -> [DetectedSubject] {
        var subjects: [DetectedSubject] = []
        
        // 1. 检测人脸
        let faceRequest = VNDetectFaceRectanglesRequest()
        let faceHandler = VNImageRequestHandler(ciImage: image)
        try faceHandler.perform([faceRequest])
        
        if let faceObservations = faceRequest.results {
            for face in faceObservations {
                subjects.append(DetectedSubject(
                    boundingBox: face.boundingBox,
                    confidence: 1.0,
                    type: .person
                ))
            }
        }
        
        // 2. 检测动物
        let animalRequest = VNRecognizeAnimalsRequest()
        let animalHandler = VNImageRequestHandler(ciImage: image)
        try animalHandler.perform([animalRequest])
        
        if let animalObservations = animalRequest.results {
            for animal in animalObservations {
                for label in animal.labels where label.confidence > 0.6 {
                    subjects.append(DetectedSubject(
                        boundingBox: animal.boundingBox,
                        confidence: Double(label.confidence),
                        type: .animal
                    ))
                }
            }
        }
        
        // 3. 检测物体
        let objectRequest = VNRecognizeObjectsRequest()
        let objectHandler = VNImageRequestHandler(ciImage: image)
        try objectHandler.perform([objectRequest])
        
        if let objectObservations = objectRequest.results {
            for object in objectObservations {
                for label in object.labels where label.confidence > 0.7 {
                    subjects.append(DetectedSubject(
                        boundingBox: object.boundingBox,
                        confidence: Double(label.confidence),
                        type: .object
                    ))
                }
            }
        }
        
        // 4. 检测文字
        let textRequest = VNRecognizeTextRequest()
        textRequest.recognitionLevel = .accurate
        let textHandler = VNImageRequestHandler(ciImage: image)
        try textHandler.perform([textRequest])
        
        if let textObservations = textRequest.results {
            for text in textObservations where text.confidence > 0.8 {
                subjects.append(DetectedSubject(
                    boundingBox: text.boundingBox,
                    confidence: Double(text.confidence),
                    type: .text
                ))
            }
        }
        
        // 去重（重叠的主体只保留置信度最高的）
        return removeOverlappingSubjects(subjects)
    }
    
    // MARK: - 线条检测
    private static func detectLines(in image: CIImage) async throws -> [Line] {
        // 使用 Vision 检测文本作为线条参考
        let textRequest = VNRecognizeTextRequest()
        textRequest.recognitionLevel = .fast
        
        let handler = VNImageRequestHandler(ciImage: image)
        try handler.perform([textRequest])
        
        var lines: [Line] = []
        
        // 从文字边界框提取线条
        if let textObservations = textRequest.results {
            for text in textObservations {
                let box = text.boundingBox
                
                // 水平线
                lines.append(Line(
                    startPoint: CGPoint(x: box.minX, y: box.midY),
                    endPoint: CGPoint(x: box.maxX, y: box.midY),
                    angle: 0,
                    length: box.width
                ))
                
                // 垂直线
                lines.append(Line(
                    startPoint: CGPoint(x: box.midX, y: box.minY),
                    endPoint: CGPoint(x: box.midX, y: box.maxY),
                    angle: .pi / 2,
                    length: box.height
                ))
            }
        }
        
        return lines
    }
    
    // MARK: - 图像特征分析
    private static func analyzeCharacteristics(of image: CIImage) async throws -> ImageCharacteristics {
        // 获取图像尺寸
        let extent = image.extent
        let width = extent.width
        let height = extent.height
        
        // 分析亮度分布（简化版）
        let brightnessDistribution = analyzeBrightness(image: image, width: width, height: height)
        
        // 检测对称性
        let hasSymmetry = await detectSymmetry(image: image)
        
        // 主导颜色和谐度（简化）
        let colorHarmony = 0.7 // 可以使用 CoreImage 进一步分析
        
        return ImageCharacteristics(
            hasStrongLeadingLines: false, // 需要更复杂的边缘检测
            hasSymmetry: hasSymmetry,
            mainSubjectPosition: CGPoint(x: 0.5, y: 0.5), // 中心点
            brightnessDistribution: brightnessDistribution,
            colorHarmony: colorHarmony
        )
    }
    
    // MARK: - 亮度分布分析
    private static func analyzeBrightness(image: CIImage, width: CGFloat, height: CGFloat) -> BrightnessDistribution {
        // 使用平均值近似分析
        // 实际应用中应该采样图像的不同区域
        return .balanced
    }
    
    // MARK: - 对称性检测
    private static func detectSymmetry(image: CIImage) async -> Bool {
        // 简化版对称性检测
        // 实际应用中应该比较左右两侧的像素差异
        return false
    }
    
    // MARK: - 构图推荐算法
    private static func recommendCompositions(
        subjects: [DetectedSubject],
        lines: [Line],
        characteristics: ImageCharacteristics
    ) -> [(type: CompositionType, score: Double)] {
        
        var recommendations: [(type: CompositionType, score: Double)] = []
        
        // 1. 基于主体位置的推荐
        for subject in subjects {
            let position = subject.boundingBox
            
            // 检查是否在三分点附近
            let thirdX1: CGFloat = 1.0 / 3.0
            let thirdX2: CGFloat = 2.0 / 3.0
            let thirdY1: CGFloat = 1.0 / 3.0
            let thirdY2: CGFloat = 2.0 / 3.0
            
            let isOnThirdPoint = abs(position.midX - thirdX1) < 0.15 ||
                                abs(position.midX - thirdX2) < 0.15 ||
                                abs(position.midY - thirdY1) < 0.15 ||
                                abs(position.midY - thirdY2) < 0.15
            
            if isOnThirdPoint {
                addOrUpdateRecommendation(&recommendations, type: .ruleOfThirds, score: 0.9)
            }
            
            // 检查是否在中心
            let isCentered = abs(position.midX - 0.5) < 0.1 && abs(position.midY - 0.5) < 0.1
            if isCentered {
                addOrUpdateRecommendation(&recommendations, type: .center, score: 0.85)
            }
            
            // 检查主体类型
            if subject.type == .person || subject.type == .animal {
                addOrUpdateRecommendation(&recommendations, type: .ruleOfThirds, score: 0.8)
            }
            
            if subject.type == .object {
                addOrUpdateRecommendation(&recommendations, type: .center, score: 0.75)
            }
        }
            
        // 2. 基于线条的推荐
        if !lines.isEmpty {
            let horizontalLines = lines.filter { abs($0.angle) < 0.2 || abs($0.angle - .pi) < 0.2 }
            let verticalLines = lines.filter { abs($0.angle - .pi/2) < 0.2 || abs($0.angle + .pi/2) < 0.2 }
            let diagonalLines = lines.filter { abs($0.angle - .pi/4) < 0.2 || abs($0.angle + .pi/4) < 0.2 }
            
            if horizontalLines.count > 2 || verticalLines.count > 2 {
                addOrUpdateRecommendation(&recommendations, type: .leadingLines, score: 0.8)
            }
            
            if diagonalLines.count > 1 {
                addOrUpdateRecommendation(&recommendations, type: .diagonal, score: 0.75)
            }
        }
        
        // 3. 基于对称性的推荐
        if characteristics.hasSymmetry {
            addOrUpdateRecommendation(&recommendations, type: .center, score: 0.9)
        }
        
        // 4. 基于亮度分布的推荐
        switch characteristics.brightnessDistribution {
        case .centerWeighted:
            addOrUpdateRecommendation(&recommendations, type: .center, score: 0.7)
        case .balanced:
            addOrUpdateRecommendation(&recommendations, type: .ruleOfThirds, score: 0.75)
        case .leftWeighted, .rightWeighted:
            addOrUpdateRecommendation(&recommendations, type: .sCurve, score: 0.65)
        case .cornerWeighted:
            addOrUpdateRecommendation(&recommendations, type: .frame, score: 0.7)
        }
        
        // 5. 默认推荐（如果没有明确的推荐）
        if recommendations.isEmpty {
            recommendations.append((type: .ruleOfThirds, score: 0.6))
            recommendations.append((type: .center, score: 0.5))
        }
        
        // 按分数排序
        recommendations.sort { $0.score > $1.score }
        
        // 只返回分数最高的 3-5 个
        return Array(recommendations.prefix(5))
    }
    
    // MARK: - 添加或更新推荐
    private static func addOrUpdateRecommendation(
        _ recommendations: inout [(type: CompositionType, score: Double)],
        type: CompositionType,
        score: Double
    ) {
        if let index = recommendations.firstIndex(where: { $0.type == type }) {
            recommendations[index].score = max(recommendations[index].score, score)
        } else {
            recommendations.append((type: type, score: score))
        }
    }
    
    // MARK: - 去除重叠主体
    private static func removeOverlappingSubjects(_ subjects: [DetectedSubject]) -> [DetectedSubject] {
        var filtered: [DetectedSubject] = []
        
        for subject in subjects {
            var hasOverlap = false
            for existing in filtered {
                if subject.boundingBox.intersects(existing.boundingBox) {
                    hasOverlap = true
                    if subject.confidence > existing.confidence {
                        if let index = filtered.firstIndex(where: { $0.boundingBox == existing.boundingBox }) {
                            filtered[index] = subject
                        }
                    }
                    break
                }
            }
            
            if !hasOverlap {
                filtered.append(subject)
            }
        }
        
        return filtered
    }
}

// MARK: - 错误类型
enum AnalysisError: Error {
    case invalidImage
    case processingFailed
    case noResults
}

// MARK: - 扩展：矩形相交检查
extension CGRect {
    func intersects(_ other: CGRect) -> Bool {
        let intersection = self.intersection(other)
        let intersectionArea = intersection.width * intersection.height
        let selfArea = self.width * self.height
        let otherArea = other.width * other.height
        
        // 如果重叠面积超过任一矩形的 30%，视为重叠
        return intersectionArea > 0 && (intersectionArea / selfArea > 0.3 || intersectionArea / otherArea > 0.3)
    }
}
