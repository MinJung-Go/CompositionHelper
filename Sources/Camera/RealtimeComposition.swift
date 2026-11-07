import Foundation

/// Maps Vision coordinates to the centered crop shown by the aspect-fill preview.
struct RealtimeViewport {
    private let crop: CGRect

    init(imageAspect: CGFloat, previewAspect: CGFloat) {
        let width = min(1, previewAspect / imageAspect)
        let height = min(1, imageAspect / previewAspect)
        crop = CGRect(x: (1 - width) / 2, y: (1 - height) / 2, width: width, height: height)
    }

    func subject(_ box: CGRect, confidence: Double, type: SubjectType) -> DetectedSubject? {
        let visible = box.intersection(crop)
        guard !visible.isEmpty else { return nil }
        let origin = normalize(visible.origin)
        let bounds = CGRect(x: origin.x, y: origin.y,
                            width: visible.width / crop.width, height: visible.height / crop.height)
        return DetectedSubject(boundingBox: bounds, confidence: confidence, type: type)
    }

    func segment(_ start: CGPoint, _ end: CGPoint) -> (CGPoint, CGPoint)? {
        let horizontalSpan = end.x - start.x
        let verticalSpan = end.y - start.y
        var first: CGFloat = 0
        var last: CGFloat = 1
        // Intersect the segment's parameter interval with each crop axis.
        for (origin, delta, lower, upper) in [
            (start.x, horizontalSpan, crop.minX, crop.maxX),
            (start.y, verticalSpan, crop.minY, crop.maxY)
        ] {
            if delta == 0 {
                guard origin >= lower && origin <= upper else { return nil }
                continue
            }
            let lowerIntersection = (lower - origin) / delta
            let upperIntersection = (upper - origin) / delta
            first = max(first, min(lowerIntersection, upperIntersection))
            last = min(last, max(lowerIntersection, upperIntersection))
            guard first < last else { return nil }
        }
        return (
            normalize(CGPoint(x: start.x + first * horizontalSpan, y: start.y + first * verticalSpan)),
            normalize(CGPoint(x: start.x + last * horizontalSpan, y: start.y + last * verticalSpan))
        )
    }

    private func normalize(_ point: CGPoint) -> CGPoint {
        CGPoint(x: (point.x - crop.minX) / crop.width, y: (point.y - crop.minY) / crop.height)
    }
}

enum LightweightAnalyzer {
    static func analyze(subjects: [DetectedSubject], diagonalScore: Float) -> FrameAnalysisResult? {
        var type: CompositionType? = diagonalScore > 0 ? .diagonal : nil
        var score = diagonalScore
        var hint: String?
        if let subject = subjects.max(by: { area($0) < area($1) }) {
            let box = subject.boundingBox
            let centerX = box.midX
            let centerY = box.midY
            let thirdX: CGFloat = centerX < 0.5 ? 1.0 / 3 : 2.0 / 3
            let thirdY: CGFloat = centerY < 0.5 ? 1.0 / 3 : 2.0 / 3
            let centerDistance = hypot(centerX - 0.5, centerY - 0.5)
            let thirdDistance = hypot(centerX - thirdX, centerY - thirdY)
            let centerScore: Float = centerDistance < 0.10 ? Float(1 - centerDistance * 4) : 0
            let thirdScore: Float = thirdDistance < 0.13 ? Float(1 - thirdDistance * 3) : 0
            if centerScore >= thirdScore && centerScore > score {
                type = .center
                score = centerScore
                hint = directionHint(horizontal: 0.5 - centerX, vertical: 0.5 - centerY) ?? "完美居中"
            } else if thirdScore > score {
                type = .ruleOfThirds
                score = thirdScore
                hint = directionHint(horizontal: thirdX - centerX, vertical: thirdY - centerY) ?? "完美对齐三分点"
            }

            let subjectArea = area(subject)
            let fillScore = subjectArea > 0.6 ? Float(min(subjectArea, 0.85)) : 0
            if fillScore > score {
                type = .fillTheFrame
                score = fillScore
                hint = nil
            } else if subjectArea < 0.15 && score < 0.7 {
                type = .negativeSpace
                score = 0.7
                hint = nil
            }
        }
        guard let type else { return nil }
        return FrameAnalysisResult(
            recommendedType: type, confidence: score, detectedSubjects: subjects, guidanceHint: hint
        )
    }

    static func diagonalScore(_ start: CGPoint, _ end: CGPoint) -> Float {
        let horizontalSpan = abs(start.x - end.x)
        let verticalSpan = abs(start.y - end.y)
        let span = hypot(horizontalSpan, verticalSpan) / hypot(CGFloat(1), CGFloat(1))
        guard min(horizontalSpan, verticalSpan) >= 0.24, span >= 0.38 else { return 0 }
        let descendingError = max(abs(start.x - start.y), abs(end.x - end.y))
        let ascendingError = max(abs(start.x + start.y - 1), abs(end.x + end.y - 1))
        guard min(descendingError, ascendingError) <= 0.12 else { return 0 }
        return Float(0.70 + 0.20 * min(span, 1))
    }

    private static func directionHint(horizontal: CGFloat, vertical: CGFloat) -> String? {
        let horizontalHint = abs(horizontal) <= 0.03 ? "" : (horizontal > 0 ? "向右" : "向左")
        // Vision uses an upward Y axis.
        let verticalHint = abs(vertical) <= 0.03 ? "" : (vertical > 0 ? "向上" : "向下")
        if horizontalHint.isEmpty && verticalHint.isEmpty { return nil }
        return "让主体在画面中稍微\(horizontalHint)\(verticalHint)移动"
    }

    private static func area(_ subject: DetectedSubject) -> CGFloat {
        subject.boundingBox.width * subject.boundingBox.height
    }
}

struct RecommendationStability {
    private var pendingType: CompositionType?
    private var repeats = 0
    private var displayed: FrameAnalysisResult?

    init(displayed: FrameAnalysisResult? = nil) {
        self.displayed = displayed
    }

    mutating func update(_ candidate: FrameAnalysisResult?) -> FrameAnalysisResult? {
        if candidate?.recommendedType == pendingType {
            repeats = min(repeats + 1, 2)
        } else {
            pendingType = candidate?.recommendedType
            repeats = 1
        }
        if repeats == 2 || displayed?.recommendedType == candidate?.recommendedType {
            displayed = candidate
        }
        return displayed
    }
}
