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
        let dx = end.x - start.x
        let dy = end.y - start.y
        var first: CGFloat = 0
        var last: CGFloat = 1
        // Intersect the segment's parameter interval with each crop axis.
        for (origin, delta, lower, upper) in [
            (start.x, dx, crop.minX, crop.maxX),
            (start.y, dy, crop.minY, crop.maxY)
        ] {
            if delta == 0 {
                guard origin >= lower && origin <= upper else { return nil }
                continue
            }
            let a = (lower - origin) / delta
            let b = (upper - origin) / delta
            first = max(first, min(a, b))
            last = min(last, max(a, b))
            guard first < last else { return nil }
        }
        return (
            normalize(CGPoint(x: start.x + first * dx, y: start.y + first * dy)),
            normalize(CGPoint(x: start.x + last * dx, y: start.y + last * dy))
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
            let x = box.midX
            let y = box.midY
            let thirdX: CGFloat = x < 0.5 ? 1.0 / 3 : 2.0 / 3
            let thirdY: CGFloat = y < 0.5 ? 1.0 / 3 : 2.0 / 3
            let centerDistance = hypot(x - 0.5, y - 0.5)
            let thirdDistance = hypot(x - thirdX, y - thirdY)
            let centerScore: Float = centerDistance < 0.10 ? Float(1 - centerDistance * 4) : 0
            let thirdScore: Float = thirdDistance < 0.13 ? Float(1 - thirdDistance * 3) : 0
            if centerScore >= thirdScore && centerScore > score {
                type = .center
                score = centerScore
                hint = directionHint(dx: 0.5 - x, dy: 0.5 - y) ?? "完美居中"
            } else if thirdScore > score {
                type = .ruleOfThirds
                score = thirdScore
                hint = directionHint(dx: thirdX - x, dy: thirdY - y) ?? "完美对齐三分点"
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
        let dx = abs(start.x - end.x)
        let dy = abs(start.y - end.y)
        let span = hypot(dx, dy) / hypot(CGFloat(1), CGFloat(1))
        guard min(dx, dy) >= 0.24, span >= 0.38 else { return 0 }
        let descendingError = max(abs(start.x - start.y), abs(end.x - end.y))
        let ascendingError = max(abs(start.x + start.y - 1), abs(end.x + end.y - 1))
        guard min(descendingError, ascendingError) <= 0.12 else { return 0 }
        return Float(0.70 + 0.20 * min(span, 1))
    }

    private static func directionHint(dx: CGFloat, dy: CGFloat) -> String? {
        let horizontal = abs(dx) <= 0.03 ? "" : (dx > 0 ? "向右" : "向左")
        // Vision uses an upward Y axis.
        let vertical = abs(dy) <= 0.03 ? "" : (dy > 0 ? "向上" : "向下")
        if horizontal.isEmpty && vertical.isEmpty { return nil }
        return "让主体在画面中稍微\(horizontal)\(vertical)移动"
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
