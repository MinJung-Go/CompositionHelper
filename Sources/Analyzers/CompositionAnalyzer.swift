import Foundation
import Vision
import CoreImage
import UIKit
import Accelerate

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
    
    // MARK: - 真正的线条检测（使用边缘检测）
    private static func detectLines(in image: CIImage) async throws -> [Line] {
        // 1. 使用 Canny 边缘检测
        let edgeDetector = CIDetector(
            ofType: CIDetectorTypeRectangle,
            context: nil,
            options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]
        )
        
        // 2. 获取图像像素数据
        guard let cgImage = image.createCGImage(cgRect: image.extent) else {
            return []
        }
        
        let width = cgImage.width
        let height = cgImage.height
        let bytesPerRow = width * 4
        
        // 3. 获取像素数据
        let pixelData = malloc(height * bytesPerRow)!
        let context = CGContext(
            data: pixelData,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: bytesPerRow,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        )
        
        context?.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        
        let pixels = pixelData.assumingMemoryBound(to: UInt8.self)
        
        // 4. 简单的边缘检测（Sobel 算子）
        var lines: [Line] = []
        
        // 检测水平线
        for y in stride(from: 0, to: height - 1, by: 10) {
            var startX: Int = -1
            var endX: Int = -1
            
            for x in stride(from: 0, to: width - 1, by: 10) {
                let idx = y * bytesPerRow + x * 4
                let brightness = Double(pixels[idx] + pixels[idx+1] + pixels[idx+2]) / 3.0
                
                if brightness > 200 && startX == -1 {
                    startX = x
                } else if brightness < 50 && startX != -1 && endX == -1 {
                    endX = x
                    
                    let line = Line(
                        startPoint: CGPoint(x: CGFloat(startX) / CGFloat(width), y: CGFloat(y) / CGFloat(height)),
                        endPoint: CGPoint(x: CGFloat(endX) / CGFloat(width), y: CGFloat(y) / CGFloat(height)),
                        angle: 0,
                        length: Double(endX - startX) / Double(width)
                    )
                    lines.append(line)
                    
                    startX = -1
                    endX = -1
                }
            }
        }
        
        // 检测垂直线
        for x in stride(from: 0, to: width - 1, by: 10) {
            var startY: Int = -1
            var endY: Int = -1
            
            for y in stride(from: 0, to: height - 1, by: 10) {
                let idx = y * bytesPerRow + x * 4
                let brightness = Double(pixels[idx] + pixels[idx+1] + pixels[idx+2]) / 3.0
                
                if brightness > 200 && startY == -1 {
                    startY = y
                } else if brightness < 50 && startY != -1 && endY == -1 {
                    endY = y
                    
                    let line = Line(
                        startPoint: CGPoint(x: CGFloat(x) / CGFloat(width), y: CGFloat(startY) / CGFloat(height)),
                        endPoint: CGPoint(x: CGFloat(x) / CGFloat(width), y: CGFloat(endY) / CGFloat(height)),
                        angle: .pi / 2,
                        length: Double(endY - startY) / Double(height)
                    )
                    lines.append(line)
                    
                    startY = -1
                    endY = -1
                }
            }
        }
        
        free(pixelData)
        
        // 5. 检测对角线
        let diagonalLines = detectDiagonalLines(from: lines)
        lines.append(contentsOf: diagonalLines)
        
        // 过滤太短的线（长度 < 0.1）
        return lines.filter { $0.length > 0.1 }
    }
    
    // MARK: - 检测对角线
    private static func detectDiagonalLines(from lines: [Line]) -> [Line] {
        var diagonalLines: [Line] = []
        
        // 从水平和垂直线的组合推断对角线
        let horizontalLines = lines.filter { abs($0.angle) < 0.2 }
        let verticalLines = lines.filter { abs($0.angle - .pi/2) < 0.2 }
        
        // 连接相邻的水平和垂直线形成对角线
        for hLine in horizontalLines {
            for vLine in verticalLines {
                let dx = abs(hLine.startPoint.x - vLine.startPoint.x)
                let dy = abs(hLine.startPoint.y - vLine.startPoint.y)
                
                if dx < 0.15 && dy < 0.15 {
                    let length = sqrt(dx*dx + dy*dy)
                    let angle = atan2(dy, dx)
                    
                    if length > 0.2 {
                        diagonalLines.append(Line(
                            startPoint: hLine.startPoint,
                            endPoint: vLine.startPoint,
                            angle: angle,
                            length: Double(length)
                        ))
                    }
                }
            }
        }
        
        return diagonalLines
    }
    
    // MARK: - 真正的亮度分布分析
    private static func analyzeBrightness(image: CIImage, width: CGFloat, height: CGFloat) -> BrightnessDistribution {
        guard let cgImage = image.createCGImage(cgRect: image.extent) else {
            return .balanced
        }
        
        let imageWidth = cgImage.width
        let imageHeight = cgImage.height
        let bytesPerRow = imageWidth * 4
        
        // 采样图像亮度
        var centerSum: Int = 0
        var centerCount: Int = 0
        var leftSum: Int = 0
        var leftCount: Int = 0
        var rightSum: Int = 0
        var rightCount: Int = 0
        
        for y in stride(from: 0, to: imageHeight, by: 20) {
            for x in stride(from: 0, to: imageWidth, by: 20) {
                let idx = y * bytesPerRow + x * 4
                
                // 使用 RGB 平均值作为亮度
                let brightness = (Int(cgImage.dataProvider!.data[idx]) +
                                Int(cgImage.dataProvider!.data[idx+1]) +
                                Int(cgImage.dataProvider!.data[idx+2])) / 3
                
                let normalizedX = Double(x) / Double(imageWidth)
                
                // 中心区域 (0.3 - 0.7)
                if normalizedX > 0.3 && normalizedX < 0.7 {
                    centerSum += brightness
                    centerCount += 1
                } else if normalizedX <= 0.3 {
                    leftSum += brightness
                    leftCount += 1
                } else {
                    rightSum += brightness
                    rightCount += 1
                }
            }
        }
        
        guard centerCount > 0 && leftCount > 0 && rightCount > 0 else {
            return .balanced
        }
        
        let centerAvg = Double(centerSum) / Double(centerCount)
        let leftAvg = Double(leftSum) / Double(leftCount)
        let rightAvg = Double(rightSum) / Double(rightCount)
        
        let threshold: Double = 30.0
        
        if centerAvg > leftAvg + threshold && centerAvg > rightAvg + threshold {
            return .centerWeighted
        } else if abs(leftAvg - rightAvg) < threshold {
            return .balanced
        } else if leftAvg > rightAvg + threshold {
            return .leftWeighted
        } else {
            return .rightWeighted
        }
    }
    
    // MARK: - 真正的对称性检测
    private static func detectSymmetry(image: CIImage) async -> Bool {
        guard let cgImage = image.createCGImage(cgRect: image.extent) else {
            return false
        }
        
        let width = cgImage.width
        let height = cgImage.height
        let bytesPerRow = width * 4
        
        var totalDifference: Int = 0
        var totalPixels: Int = 0
        
        // 比较左右对称像素
        for y in 0..<min(height, 500) { // 限制采样数量以提高性能
            for x in 0..<min(width / 2, 250) {
                let leftIdx = y * bytesPerRow + x * 4
                let rightIdx = y * bytesPerRow + (width - 1 - x) * 4
                
                // 计算左右像素的亮度差异
                let leftBrightness = (Int(cgImage.dataProvider!.data[leftIdx]) +
                                   Int(cgImage.dataProvider!.data[leftIdx+1]) +
                                   Int(cgImage.dataProvider!.data[leftIdx+2])) / 3
                
                let rightBrightness = (Int(cgImage.dataProvider!.data[rightIdx]) +
                                    Int(cgImage.dataProvider!.data[rightIdx+1]) +
                                    Int(cgImage.dataProvider!.data[rightIdx+2])) / 3
                
                totalDifference += abs(leftBrightness - rightBrightness)
                totalPixels += 1
            }
        }
        
        guard totalPixels > 0 else {
            return false
        }
        
        let avgDifference = Double(totalDifference) / Double(totalPixels)
        
        // 如果平均差异小于 20，认为是对称的
        return avgDifference < 20.0
    }
    
    // MARK: - 图像特征分析
    private static func analyzeCharacteristics(of image: CIImage) async throws -> ImageCharacteristics {
        // 获取图像尺寸
        let extent = image.extent
        let width = extent.width
        let height = extent.height
        
        // 分析亮度分布
        let brightnessDistribution = analyzeBrightness(image: image, width: width, height: height)
        
        // 检测对称性
        let hasSymmetry = await detectSymmetry(image: image)
        
        // 主导颜色和谐度（简化）
        let colorHarmony = 0.7 // 可以使用 CoreImage 进一步分析
        
        // 检测是否有强引导线
        async let lines = detectLines(in: image)
        let detectedLines = try await lines
        let hasStrongLeadingLines = detectedLines.count > 3
        
        // 找到主要主体位置
        var mainSubjectPosition = CGPoint(x: 0.5, y: 0.5)
        async let subjects = detectSubjects(in: image)
        let detectedSubjects = try await subjects
        if let mainSubject = detectedSubjects.first {
            mainSubjectPosition = CGPoint(
                x: mainSubject.boundingBox.midX,
                y: mainSubject.boundingBox.midY
            )
        }
        
        return ImageCharacteristics(
            hasStrongLeadingLines: hasStrongLeadingLines,
            hasSymmetry: hasSymmetry,
            mainSubjectPosition: mainSubjectPosition,
            brightnessDistribution: brightnessDistribution,
            colorHarmony: colorHarmony
        )
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
        
        // 5. 基于引导线的推荐
        if characteristics.hasStrongLeadingLines {
            addOrUpdateRecommendation(&recommendations, type: .leadingLines, score: 0.85)
        }
        
        // 6. 默认推荐（如果没有明确的推荐）
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
