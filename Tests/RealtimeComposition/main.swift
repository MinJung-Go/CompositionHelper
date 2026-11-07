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
let diagonalScore = LightweightAnalyzer.diagonalScore(a, b)
precondition(diagonalScore > 0)
precondition(abs(diagonalScore - LightweightAnalyzer.diagonalScore(
    CGPoint(x: 0.1, y: 0.9), CGPoint(x: 0.9, y: 0.1)
)) < 0.001)
precondition(LightweightAnalyzer.diagonalScore(CGPoint(x: 0.4, y: 0.4), CGPoint(x: 0.6, y: 0.6)) == 0)
precondition(LightweightAnalyzer.diagonalScore(CGPoint(x: 0, y: 0.3), CGPoint(x: 0.7, y: 1)) == 0)
precondition(LightweightAnalyzer.diagonalScore(CGPoint(x: 0.1, y: 0.5), CGPoint(x: 0.9, y: 0.5)) == 0)
precondition(LightweightAnalyzer.analyze(subjects: [], diagonalScore: diagonalScore)?.recommendedType == .diagonal)

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
precondition(LightweightAnalyzer.diagonalScore(start, end) > 0)
guard let (clippedStart, clippedEnd) = viewport.segment(a, b) else { fatalError("Missing clipped segment") }
precondition(abs(clippedStart.y) < 0.001 && abs(clippedEnd.y - 1) < 0.001)
print("Realtime composition checks passed")
