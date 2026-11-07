package com.example.compositionhelper.camera

import com.example.compositionhelper.model.*
import kotlin.math.abs
import kotlin.math.hypot

internal object LightweightAnalyzer {
    private val thirdPoints = listOf(
        PointF(1f / 3f, 1f / 3f), PointF(2f / 3f, 1f / 3f),
        PointF(1f / 3f, 2f / 3f), PointF(2f / 3f, 2f / 3f)
    )

    fun recommend(subjects: List<DetectedSubject>, diagonalScore: Float): FrameAnalysisResult {
        var type = if (diagonalScore > 0f) CompositionType.DIAGONAL else null
        var score = diagonalScore
        var hint: String? = null
        val subject = subjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        if (subject != null) {
            val x = subject.boundingBox.centerX()
            val y = subject.boundingBox.centerY()
            val centerDistance = hypot(x - 0.5f, y - 0.5f)
            val third = thirdPoints.minBy { hypot(x - it.x, y - it.y) }
            val thirdDistance = hypot(x - third.x, y - third.y)
            val centerScore = if (centerDistance < 0.10f) 1f - centerDistance * 4f else 0f
            val thirdScore = if (thirdDistance < 0.13f) 1f - thirdDistance * 3f else 0f
            if (centerScore >= thirdScore && centerScore > score) {
                type = CompositionType.CENTER
                score = centerScore
                hint = directionHint(0.5f - x, 0.5f - y) ?: "完美居中"
            } else if (thirdScore > score) {
                type = CompositionType.RULE_OF_THIRDS
                score = thirdScore
                hint = directionHint(third.x - x, third.y - y) ?: "完美对齐三分点"
            }
        }
        return FrameAnalysisResult(type, score, subjects, hint)
    }

    private fun directionHint(dx: Float, dy: Float): String? {
        val horizontal = if (abs(dx) <= 0.03f) "" else if (dx > 0f) "向右" else "向左"
        val vertical = if (abs(dy) <= 0.03f) "" else if (dy > 0f) "向下" else "向上"
        if (horizontal.isEmpty() && vertical.isEmpty()) return null
        return "让主体在画面中稍微${horizontal}${vertical}移动"
    }

    fun diagonalScore(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = abs(x1 - x2)
        val dy = abs(y1 - y2)
        val span = hypot(dx, dy) / hypot(1f, 1f)
        if (minOf(dx, dy) < 0.24f || span < 0.38f) return 0f
        val descendingError = maxOf(abs(x1 - y1), abs(x2 - y2))
        val ascendingError = maxOf(abs(x1 + y1 - 1f), abs(x2 + y2 - 1f))
        if (minOf(descendingError, ascendingError) > 0.12f) return 0f
        return 0.70f + 0.20f * span.coerceAtMost(1f)
    }
}

internal class RecommendationStability {
    private var pendingType: CompositionType? = null
    private var repeats = 0
    private var displayed: FrameAnalysisResult? = null

    fun reset() {
        repeats = 0
        displayed = null
    }

    fun update(candidate: FrameAnalysisResult): FrameAnalysisResult {
        if (candidate.recommendedType == pendingType) repeats = (repeats + 1).coerceAtMost(2)
        else {
            pendingType = candidate.recommendedType
            repeats = 1
        }
        if (repeats == 2 || displayed?.recommendedType == candidate.recommendedType) displayed = candidate
        return displayed?.copy(detectedSubjects = candidate.detectedSubjects)
            ?: FrameAnalysisResult(null, 0f, candidate.detectedSubjects, null)
    }
}
