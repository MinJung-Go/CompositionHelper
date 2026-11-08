package com.example.compositionhelper.camera

import com.example.compositionhelper.model.*
import kotlin.math.abs
import kotlin.math.hypot

internal object LightweightAnalyzer {
    private val thirdPoints = listOf(
        PointF(1f / 3f, 1f / 3f), PointF(2f / 3f, 1f / 3f),
        PointF(1f / 3f, 2f / 3f), PointF(2f / 3f, 2f / 3f)
    )

    fun recommend(
        subjects: List<DetectedSubject>,
        diagonalScore: Float,
        portraitAnchor: PointF? = null
    ): FrameAnalysisResult {
        var type = if (diagonalScore > 0f) CompositionType.DIAGONAL else null
        var score = diagonalScore
        var hint: String? = null
        val subject = subjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        val anchor = portraitAnchor ?: subject?.boundingBox?.let { PointF(it.centerX(), it.centerY()) }
        if (anchor != null) {
            val (x, y) = anchor
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

}

internal data class Portrait(val subject: DetectedSubject, val anchor: PointF) {
    companion object {
        fun fromEyes(subject: DetectedSubject, leftEye: PointF?, rightEye: PointF?): Portrait? {
            if (leftEye == null || rightEye == null) return null
            val box = subject.boundingBox
            val x = (leftEye.x + rightEye.x) / 2
            val y = (leftEye.y + rightEye.y) / 2
            val spacing = abs(leftEye.x - rightEye.x)
            if (box.width() < 0.10f || box.height() < 0.10f ||
                !listOf(leftEye, rightEye).all {
                    it.x in box.left..box.right && it.y in box.top..box.bottom
                } ||
                spacing !in (box.width() * 0.16f)..(box.width() * 0.85f) ||
                abs(leftEye.y - rightEye.y) > spacing * 0.65f
            ) return null
            return Portrait(subject, PointF(x, y))
        }
    }
}

/** Keeps the current face when confidence or apparent area changes between frames. */
internal class PortraitSelection {
    private var selected: Portrait? = null
    private var missed = false

    fun reset() {
        selected = null
        missed = false
    }

    fun update(faces: List<Portrait>): Portrait? {
        val previous = selected
        if (previous != null) {
            val match = faces.filter {
                it.subject.boundingBox.intersects(previous.subject.boundingBox) &&
                    distance(it.anchor, previous.anchor) <= 0.12f
            }.minByOrNull { distance(it.anchor, previous.anchor) }
            if (match != null) {
                selected = match
                missed = false
                return match
            }
            if (!missed) {
                missed = true
                return null
            }
        }
        missed = false
        return faces.maxWithOrNull(
            compareBy<Portrait> { it.subject.boundingBox.width() * it.subject.boundingBox.height() }
                .thenBy { -distance(it.anchor, PointF(0.5f, 0.5f)) }
                .thenBy { -it.anchor.x }
        ).also { selected = it }
    }

    private fun distance(a: PointF, b: PointF) = hypot(a.x - b.x, a.y - b.y)
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
