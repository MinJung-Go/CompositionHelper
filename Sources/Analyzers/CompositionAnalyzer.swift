import Foundation
import Vision
import CoreImage
import UIKit

// MARK: - 构图分析器（全量分析，相册模式用）
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

        return removeOverlappingSubjects(subjects)
    }

    // MARK: - 线条检测
    private static func detectLines(in image: CIImage) async throws -> [Line] {
        guard let cgImage = CIContext().createCGImage(image, from: image.extent) else {
            return []
        }

        let width = cgImage.width
        let height = cgImage.height
        let bytesPerRow = width * 4

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
        var lines: [Line] = []

        // 检测水平线
        for row in stride(from: 0, to: height - 1, by: 10) {
            var startX: Int = -1
            var endX: Int = -1

            for col in stride(from: 0, to: width - 1, by: 10) {
                let idx = row * bytesPerRow + col * 4
                let brightness = Double(pixels[idx] + pixels[idx + 1] + pixels[idx + 2]) / 3.0

                if brightness > 200 && startX == -1 {
                    startX = col
                } else if brightness < 50 && startX != -1 && endX == -1 {
                    endX = col
                    lines.append(Line(
                        startPoint: CGPoint(
                            x: CGFloat(startX) / CGFloat(width),
                            y: CGFloat(row) / CGFloat(height)
                        ),
                        endPoint: CGPoint(
                            x: CGFloat(endX) / CGFloat(width),
                            y: CGFloat(row) / CGFloat(height)
                        ),
                        angle: 0,
                        length: Double(endX - startX) / Double(width)
                    ))
                    startX = -1
                    endX = -1
                }
            }
        }

        // 检测垂直线
        for col in stride(from: 0, to: width - 1, by: 10) {
            var startY: Int = -1
            var endY: Int = -1

            for row in stride(from: 0, to: height - 1, by: 10) {
                let idx = row * bytesPerRow + col * 4
                let brightness = Double(pixels[idx] + pixels[idx + 1] + pixels[idx + 2]) / 3.0

                if brightness > 200 && startY == -1 {
                    startY = row
                } else if brightness < 50 && startY != -1 && endY == -1 {
                    endY = row
                    lines.append(Line(
                        startPoint: CGPoint(
                            x: CGFloat(col) / CGFloat(width),
                            y: CGFloat(startY) / CGFloat(height)
                        ),
                        endPoint: CGPoint(
                            x: CGFloat(col) / CGFloat(width),
                            y: CGFloat(endY) / CGFloat(height)
                        ),
                        angle: .pi / 2,
                        length: Double(endY - startY) / Double(height)
                    ))
                    startY = -1
                    endY = -1
                }
            }
        }

        free(pixelData)

        let diagonalLines = detectDiagonalLines(from: lines)
        lines.append(contentsOf: diagonalLines)

        return lines.filter { $0.length > 0.1 }
    }

    // MARK: - 图像特征分析
    private static func analyzeCharacteristics(of image: CIImage) async throws -> ImageCharacteristics {
        let brightnessDistribution = analyzeBrightness(image: image)
        let hasSymmetry = await detectSymmetry(image: image)

        async let lines = detectLines(in: image)
        let detectedLines = try await lines
        let hasStrongLeadingLines = detectedLines.count > 3

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
            colorHarmony: 0.7
        )
    }

    // MARK: - 构图推荐
    private static func recommendCompositions(
        subjects: [DetectedSubject],
        lines: [Line],
        characteristics: ImageCharacteristics
    ) -> [(type: CompositionType, score: Double)] {
        var recommendations: [(type: CompositionType, score: Double)] = []

        recommendBySubjects(subjects, into: &recommendations)
        recommendByLines(lines, into: &recommendations)
        recommendByCharacteristics(characteristics, into: &recommendations)

        if recommendations.isEmpty {
            recommendations.append((type: .ruleOfThirds, score: 0.6))
            recommendations.append((type: .center, score: 0.5))
        }

        recommendations.sort { $0.score > $1.score }
        return Array(recommendations.prefix(5))
    }

    // MARK: - 基于主体位置的推荐
    private static func recommendBySubjects(
        _ subjects: [DetectedSubject],
        into recommendations: inout [(type: CompositionType, score: Double)]
    ) {
        for subject in subjects {
            let position = subject.boundingBox
            let thirdX1: CGFloat = 1.0 / 3.0
            let thirdX2: CGFloat = 2.0 / 3.0
            let thirdY1: CGFloat = 1.0 / 3.0
            let thirdY2: CGFloat = 2.0 / 3.0

            let isOnThirdPoint = abs(position.midX - thirdX1) < 0.15 ||
                                abs(position.midX - thirdX2) < 0.15 ||
                                abs(position.midY - thirdY1) < 0.15 ||
                                abs(position.midY - thirdY2) < 0.15

            if isOnThirdPoint {
                addOrUpdate(&recommendations, type: .ruleOfThirds, score: 0.9)
            }

            if abs(position.midX - 0.5) < 0.1 && abs(position.midY - 0.5) < 0.1 {
                addOrUpdate(&recommendations, type: .center, score: 0.85)
            }

            if subject.type == .person || subject.type == .animal {
                addOrUpdate(&recommendations, type: .ruleOfThirds, score: 0.8)
            }

            if subject.type == .object {
                addOrUpdate(&recommendations, type: .center, score: 0.75)
            }
        }
    }

    // MARK: - 基于线条的推荐
    private static func recommendByLines(
        _ lines: [Line],
        into recommendations: inout [(type: CompositionType, score: Double)]
    ) {
        guard !lines.isEmpty else { return }

        let hLines = lines.filter { abs($0.angle) < 0.2 || abs($0.angle - .pi) < 0.2 }
        let vLines = lines.filter {
            abs($0.angle - .pi / 2) < 0.2 || abs($0.angle + .pi / 2) < 0.2
        }
        let dLines = lines.filter {
            abs($0.angle - .pi / 4) < 0.2 || abs($0.angle + .pi / 4) < 0.2
        }

        if hLines.count > 2 || vLines.count > 2 {
            addOrUpdate(&recommendations, type: .leadingLines, score: 0.8)
        }
        if dLines.count > 1 {
            addOrUpdate(&recommendations, type: .diagonal, score: 0.75)
        }
    }

    // MARK: - 基于图像特征的推荐
    private static func recommendByCharacteristics(
        _ characteristics: ImageCharacteristics,
        into recommendations: inout [(type: CompositionType, score: Double)]
    ) {
        if characteristics.hasSymmetry {
            addOrUpdate(&recommendations, type: .center, score: 0.9)
        }

        switch characteristics.brightnessDistribution {
        case .centerWeighted:
            addOrUpdate(&recommendations, type: .center, score: 0.7)
        case .balanced:
            addOrUpdate(&recommendations, type: .ruleOfThirds, score: 0.75)
        case .leftWeighted, .rightWeighted:
            addOrUpdate(&recommendations, type: .sCurve, score: 0.65)
        case .cornerWeighted:
            addOrUpdate(&recommendations, type: .frame, score: 0.7)
        }

        if characteristics.hasStrongLeadingLines {
            addOrUpdate(&recommendations, type: .leadingLines, score: 0.85)
        }
    }
}

// MARK: - 辅助方法
extension CompositionAnalyzer {

    private static func detectDiagonalLines(from lines: [Line]) -> [Line] {
        var diagonalLines: [Line] = []
        let horizontalLines = lines.filter { abs($0.angle) < 0.2 }
        let verticalLines = lines.filter { abs($0.angle - .pi / 2) < 0.2 }

        for hLine in horizontalLines {
            for vLine in verticalLines {
                let deltaX = abs(hLine.startPoint.x - vLine.startPoint.x)
                let deltaY = abs(hLine.startPoint.y - vLine.startPoint.y)

                if deltaX < 0.15 && deltaY < 0.15 {
                    let length = sqrt(deltaX * deltaX + deltaY * deltaY)
                    let angle = atan2(deltaY, deltaX)

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

    private static func analyzeBrightness(image: CIImage) -> BrightnessDistribution {
        guard let cgImage = CIContext().createCGImage(image, from: image.extent) else {
            return .balanced
        }

        let imageWidth = cgImage.width
        let imageHeight = cgImage.height
        let bytesPerRow = imageWidth * 4

        let pixelData = malloc(imageHeight * bytesPerRow)!
        let context = CGContext(
            data: pixelData,
            width: imageWidth,
            height: imageHeight,
            bitsPerComponent: 8,
            bytesPerRow: bytesPerRow,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        )
        context?.draw(
            cgImage,
            in: CGRect(x: 0, y: 0, width: imageWidth, height: imageHeight)
        )

        let pixels = pixelData.assumingMemoryBound(to: UInt8.self)

        var centerSum: Int = 0, centerCount: Int = 0
        var leftSum: Int = 0, leftCount: Int = 0
        var rightSum: Int = 0, rightCount: Int = 0

        for row in stride(from: 0, to: imageHeight, by: 20) {
            for col in stride(from: 0, to: imageWidth, by: 20) {
                let idx = row * bytesPerRow + col * 4
                let brightness = (Int(pixels[idx]) + Int(pixels[idx + 1]) + Int(pixels[idx + 2])) / 3
                let normalizedX = Double(col) / Double(imageWidth)

                if normalizedX > 0.3 && normalizedX < 0.7 {
                    centerSum += brightness; centerCount += 1
                } else if normalizedX <= 0.3 {
                    leftSum += brightness; leftCount += 1
                } else {
                    rightSum += brightness; rightCount += 1
                }
            }
        }

        free(pixelData)

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

    private static func detectSymmetry(image: CIImage) async -> Bool {
        guard let cgImage = CIContext().createCGImage(image, from: image.extent) else {
            return false
        }

        let width = cgImage.width
        let height = cgImage.height
        let bytesPerRow = width * 4

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

        var totalDifference: Int = 0
        var totalPixels: Int = 0

        for row in stride(from: 0, to: min(height, 500), by: 1) {
            for col in stride(from: 0, to: min(width / 2, 250), by: 1) {
                let leftIdx = row * bytesPerRow + col * 4
                let rightIdx = row * bytesPerRow + (width - 1 - col) * 4

                let leftB = (Int(pixels[leftIdx]) + Int(pixels[leftIdx + 1]) + Int(pixels[leftIdx + 2])) / 3
                let rightB = (Int(pixels[rightIdx]) + Int(pixels[rightIdx + 1]) + Int(pixels[rightIdx + 2])) / 3

                totalDifference += abs(leftB - rightB)
                totalPixels += 1
            }
        }

        free(pixelData)

        guard totalPixels > 0 else { return false }
        return Double(totalDifference) / Double(totalPixels) < 20.0
    }

    private static func addOrUpdate(
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

    private static func removeOverlappingSubjects(_ subjects: [DetectedSubject]) -> [DetectedSubject] {
        var filtered: [DetectedSubject] = []
        for subject in subjects {
            var hasOverlap = false
            for (idx, existing) in filtered.enumerated() {
                let intersection = subject.boundingBox.intersection(existing.boundingBox)
                let intersectionArea = intersection.width * intersection.height
                let selfArea = subject.boundingBox.width * subject.boundingBox.height
                let existingArea = existing.boundingBox.width * existing.boundingBox.height

                let isOverlapping = intersectionArea > 0 &&
                    (intersectionArea / selfArea > 0.3 || intersectionArea / existingArea > 0.3)
                if isOverlapping {
                    hasOverlap = true
                    if subject.confidence > existing.confidence {
                        filtered[idx] = subject
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
