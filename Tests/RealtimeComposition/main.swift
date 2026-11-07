import Foundation

func recommendationAt(_ x: CGFloat, _ y: CGFloat) -> FrameAnalysisResult? {
    let box = CGRect(x: x - 0.2, y: y - 0.2, width: 0.4, height: 0.4)
    let subject = DetectedSubject(boundingBox: box, confidence: 0.8, type: .object)
    return LightweightAnalyzer.analyze(subjects: [subject], diagonalScore: 0)
}

let center = recommendationAt(0.5, 0.5)
let thirds = recommendationAt(1.0 / 3, 2.0 / 3)
precondition(center?.recommendedType == .center)
precondition(thirds?.recommendedType == .ruleOfThirds)
precondition(recommendationAt(0.2, 0.2) == nil)

let a = CGPoint(x: 0.1, y: 0.1)
let b = CGPoint(x: 0.9, y: 0.9)
func segment(_ x1: CGFloat, _ y1: CGFloat, _ x2: CGFloat, _ y2: CGFloat) -> (CGPoint, CGPoint) {
    (CGPoint(x: x1, y: y1), CGPoint(x: x2, y: y2))
}
func slantedBoundary(_ x: CGFloat, _ y: CGFloat) -> Int { y > 0.2375 + 0.525 * x ? 180 : 60 }
let parts = [segment(0.10, 0.29, 0.46, 0.479), segment(0.90, 0.71, 0.54, 0.521)]
let diagonalScore = DiagonalEvidence.score(parts, luma: slantedBoundary)
precondition(diagonalScore > 0)
let reflected = parts.map { segment($0.0.x, 1 - $0.0.y, $0.1.x, 1 - $0.1.y) }
precondition(abs(diagonalScore - DiagonalEvidence.score(reflected) { x, y in slantedBoundary(x, 1 - y) }) < 0.001)
precondition(DiagonalEvidence.score([parts[0]], luma: slantedBoundary) == 0)
let vanishingPoint = [segment(0.1, 0.9, 0.48, 0.51), segment(0.52, 0.51, 0.9, 0.9)]
precondition(DiagonalEvidence.score(vanishingPoint, luma: slantedBoundary) == 0)
let mainEdge = segment(0.1, 0.29, 0.9, 0.71)
precondition(DiagonalEvidence.score([mainEdge]) { _, _ in 120 } == 0)
let repeated = [mainEdge] + [-0.10, 0.05, 0.10].map { (offset: CGFloat) in
    segment(0.1, 0.29 + offset, 0.9, 0.71 + offset)
}
precondition(DiagonalEvidence.score(repeated, luma: slantedBoundary) == 0)
precondition(LightweightAnalyzer.analyze(subjects: [], diagonalScore: diagonalScore)?.recommendedType == .diagonal)

let face = DetectedSubject(boundingBox: CGRect(x: 0.22, y: 0.22, width: 0.56, height: 0.56), confidence: 0.8, type: .person)
let left = CGPoint(x: 0.25, y: 1.0 / 3)
let right = CGPoint(x: 0.42, y: 1.0 / 3)
guard let portrait = Portrait(subject: face, leftEye: left, rightEye: right) else { fatalError("Missing portrait") }
precondition(Portrait(subject: face, leftEye: left, rightEye: nil) == nil)
precondition(Portrait(subject: face, leftEye: CGPoint(x: 0.19, y: left.y), rightEye: right) == nil)
precondition(LightweightAnalyzer.analyze(subjects: [face], diagonalScore: 0, portrait: portrait)?.recommendedType == .ruleOfThirds)
let otherFace = DetectedSubject(boundingBox: CGRect(x: 0.50, y: 0.10, width: 0.49, height: 0.80), confidence: 0.99, type: .person)
guard let other = Portrait(subject: otherFace, leftEye: CGPoint(x: 0.63, y: 0.35),
                           rightEye: CGPoint(x: 0.86, y: 0.35)) else { fatalError("Missing second portrait") }
var selection = PortraitSelection()
precondition(selection.update([portrait])?.anchor == portrait.anchor)
precondition(selection.update([other, portrait])?.anchor == portrait.anchor)
precondition(selection.update([other]) == nil)
precondition(selection.update([other, portrait])?.anchor == portrait.anchor)
precondition(selection.update([other]) == nil)
precondition(selection.update([other])?.anchor == other.anchor)

var stability = RecommendationStability()
precondition(stability.update(center) == nil)
precondition(stability.update(center)?.recommendedType == .center)
precondition(stability.update(thirds)?.recommendedType == .center)
precondition(stability.update(center)?.recommendedType == .center)
precondition(stability.update(nil)?.recommendedType == .center)
precondition(stability.update(nil) == nil)
precondition(stability.update(thirds) == nil)
precondition(stability.update(thirds)?.recommendedType == .ruleOfThirds)

let viewport = RealtimeViewport(imageAspect: 0.75, previewAspect: 1)
let box = CGRect(x: 0.3, y: 0.04, width: 0.1, height: 0.05)
precondition(viewport.subject(box, confidence: 0.8, type: .object) == nil)
guard let (start, end) = viewport.segment(
    CGPoint(x: 0, y: 0.125), CGPoint(x: 1, y: 0.875)
) else { fatalError("Missing visible diagonal") }
precondition(DiagonalEvidence.score([(start, end)]) { x, y in y > x ? 180 : 60 } > 0)
guard let (clippedStart, clippedEnd) = viewport.segment(a, b) else { fatalError("Missing clipped segment") }
precondition(abs(clippedStart.y) < 0.001 && abs(clippedEnd.y - 1) < 0.001)
print("Realtime composition checks passed")
