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

    func point(_ point: CGPoint) -> CGPoint? {
        guard (crop.minX...crop.maxX).contains(point.x), (crop.minY...crop.maxY).contains(point.y) else {
            return nil
        }
        return normalize(point)
    }

    func imagePoint(_ point: CGPoint) -> CGPoint {
        CGPoint(x: crop.minX + point.x * crop.width, y: crop.minY + point.y * crop.height)
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
    static func analyze(
        subjects: [DetectedSubject], diagonalScore: Float, portrait: Portrait? = nil
    ) -> FrameAnalysisResult? {
        var type: CompositionType? = diagonalScore > 0 ? .diagonal : nil
        var score = diagonalScore
        var hint: String?
        if let subject = portrait?.subject ?? subjects.max(by: { area($0) < area($1) }) {
            let box = subject.boundingBox
            let centerX = portrait?.anchor.x ?? box.midX
            let centerY = portrait?.anchor.y ?? box.midY
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

/// Looks for a coherent slanted boundary that crosses the middle of the visible frame.
enum DiagonalEvidence {
    private struct Edge {
        let start: CGPoint
        let end: CGPoint
        let unitX: CGFloat
        let unitY: CGFloat
        let length: CGFloat
    }

    static func score(_ segments: [(CGPoint, CGPoint)], luma: (CGFloat, CGFloat) -> Int) -> Float {
        let edges = segments.compactMap { segment -> Edge? in
            let (start, end) = segment.0.x <= segment.1.x ? segment : (segment.1, segment.0)
            let deltaX = end.x - start.x
            let deltaY = end.y - start.y
            let length = hypot(deltaX, deltaY)
            guard length >= 0.09, abs(deltaY) >= 0.41 * deltaX, deltaX >= 0.41 * abs(deltaY) else { return nil }
            return Edge(start: start, end: end, unitX: deltaX / length, unitY: deltaY / length, length: length)
        }
        var best: Float = 0
        for seed in edges.sorted(by: { $0.length > $1.length }).prefix(24) {
            let normalX = -seed.unitY
            let normalY = seed.unitX
            func offset(_ point: CGPoint) -> CGFloat {
                (point.x - seed.start.x) * normalX + (point.y - seed.start.y) * normalY
            }
            func projection(_ point: CGPoint) -> CGFloat {
                (point.x - seed.start.x) * seed.unitX + (point.y - seed.start.y) * seed.unitY
            }
            func alignment(_ edge: Edge) -> CGFloat { edge.unitX * seed.unitX + edge.unitY * seed.unitY }

            guard abs(offset(CGPoint(x: 0.5, y: 0.5))) <= 0.15 else { continue }
            let parallelOffsets = edges.filter {
                $0.length >= 0.32 && alignment($0) >= 0.9903
            }.map { (offset($0.start) + offset($0.end)) / 2 }.sorted()
            var groups = 0
            var previousOffset = -CGFloat.infinity
            for value in parallelOffsets where value - previousOffset >= 0.027 {
                groups += 1
                previousOffset = value
            }
            guard groups < 4 else { continue }

            let intervals = edges.compactMap { edge -> (CGFloat, CGFloat)? in
                guard alignment(edge) >= 0.978, abs(offset(edge.start)) <= 0.028,
                      abs(offset(edge.end)) <= 0.028 else { return nil }
                return (projection(edge.start), projection(edge.end))
            }.sorted { $0.0 < $1.0 }

            var first: CGFloat = 0
            var last: CGFloat = 0
            var covered: CGFloat = 0
            for (start, end) in intervals {
                if covered == 0 || start > last + 0.13 {
                    best = max(best, boundaryScore(seed, first, last, covered, luma))
                    first = start
                    last = start
                    covered = 0
                }
                covered += max(0, end - max(start, last))
                last = max(last, end)
            }
            best = max(best, boundaryScore(seed, first, last, covered, luma))
        }
        return best
    }

    private static func boundaryScore(
        _ edge: Edge, _ first: CGFloat, _ last: CGFloat, _ covered: CGFloat, _ luma: (CGFloat, CGFloat) -> Int
    ) -> Float {
        let extent = last - first
        guard extent >= 0.53, covered >= 0.71 * extent else { return 0 }
        let startX = edge.start.x + edge.unitX * first
        let endX = edge.start.x + edge.unitX * last
        let startY = edge.start.y + edge.unitY * first
        let endY = edge.start.y + edge.unitY * last
        // A perspective line ending at a central vanishing point lacks support on both sides.
        guard startX < 0.39, endX > 0.61, min(startY, endY) < 0.39, max(startY, endY) > 0.61 else { return 0 }

        let offsetX = -0.035 * edge.unitY
        let offsetY = 0.035 * edge.unitX
        var valid = 0
        var brighter = 0
        var darker = 0
        for index in 0..<32 {
            let distance = first + (0.08 + 0.84 * CGFloat(index) / 31) * extent
            let sampleX = edge.start.x + edge.unitX * distance
            let sampleY = edge.start.y + edge.unitY * distance
            guard sampleX - abs(offsetX) >= 0, sampleX + abs(offsetX) <= 1,
                  sampleY - abs(offsetY) >= 0, sampleY + abs(offsetY) <= 1 else { continue }
            let difference = luma(sampleX + offsetX, sampleY + offsetY) - luma(sampleX - offsetX, sampleY - offsetY)
            valid += 1
            if difference >= 14 { brighter += 1 }
            if difference <= -14 { darker += 1 }
        }
        guard valid >= 8, max(brighter, darker) * 2 >= valid else { return 0 }
        return Float(0.70 + 0.20 * min(extent / sqrt(2), 1))
    }
}

struct Portrait {
    let subject: DetectedSubject
    let anchor: CGPoint

    init?(subject: DetectedSubject, leftEye: CGPoint?, rightEye: CGPoint?) {
        guard let leftEye, let rightEye else { return nil }
        let box = subject.boundingBox
        let spacing = abs(leftEye.x - rightEye.x)
        guard box.width >= 0.10, box.height >= 0.10,
              [leftEye, rightEye].allSatisfy({
                  (box.minX...box.maxX).contains($0.x) && (box.minY...box.maxY).contains($0.y)
              }),
              (box.width * 0.16...box.width * 0.85).contains(spacing),
              abs(leftEye.y - rightEye.y) <= spacing * 0.65 else { return nil }
        self.subject = subject
        anchor = CGPoint(x: (leftEye.x + rightEye.x) / 2, y: (leftEye.y + rightEye.y) / 2)
    }
}

/// Keeps the current face when confidence or apparent area changes between frames.
struct PortraitSelection {
    private var selected: Portrait?
    private var missed = false

    mutating func update(_ faces: [Portrait]) -> Portrait? {
        if let previous = selected {
            let previousBox = previous.subject.boundingBox
            let matches = faces.filter { face in
                let box = face.subject.boundingBox
                let overlap = previousBox.intersection(box)
                return !overlap.isEmpty &&
                    overlap.width * overlap.height > 0.30 * min(area(box), area(previousBox)) &&
                    distance(face.anchor, previous.anchor) <= 0.12
            }
            if let match = matches.min(by: {
                distance($0.anchor, previous.anchor) < distance($1.anchor, previous.anchor)
            }) {
                selected = match
                missed = false
                return match
            }
            if !missed {
                missed = true
                return nil
            }
        }
        missed = false
        let center = CGPoint(x: 0.5, y: 0.5)
        selected = faces.max { first, second in
            let firstArea = area(first.subject.boundingBox)
            let secondArea = area(second.subject.boundingBox)
            if firstArea != secondArea { return firstArea < secondArea }
            let firstDistance = distance(first.anchor, center)
            let secondDistance = distance(second.anchor, center)
            if firstDistance != secondDistance { return firstDistance > secondDistance }
            return first.anchor.x > second.anchor.x
        }
        return selected
    }

    private func area(_ rect: CGRect) -> CGFloat { rect.width * rect.height }

    private func distance(_ first: CGPoint, _ second: CGPoint) -> CGFloat {
        hypot(first.x - second.x, first.y - second.y)
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
