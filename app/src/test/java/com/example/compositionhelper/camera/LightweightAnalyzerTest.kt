package com.example.compositionhelper.camera

import com.example.compositionhelper.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightweightAnalyzerTest {
    @Test
    fun recommendsUsingVisibleSubjectPosition() {
        assertEquals(CompositionType.CENTER, recommendationAt(0.5f, 0.5f).recommendedType)
        assertEquals(CompositionType.RULE_OF_THIRDS, recommendationAt(1f / 3f, 2f / 3f).recommendedType)
        assertNull(recommendationAt(0.10f, 0.10f).recommendedType)
    }

    @Test
    fun diagonalJoinsBrokenEdgesAcrossTheFrame() {
        val parts = listOf(
            segment(0.10f, 0.29f, 0.46f, 0.479f),
            segment(0.90f, 0.71f, 0.54f, 0.521f)
        )
        val score = DiagonalEvidence.score(parts, ::slantedBoundary)
        assertTrue(score > 0f)
        val reflected = parts.map { (a, b) -> segment(a.x, 1 - a.y, b.x, 1 - b.y) }
        assertEquals(score, DiagonalEvidence.score(reflected) { x, y -> slantedBoundary(x, 1 - y) }, 0.001f)
        assertEquals(0f, DiagonalEvidence.score(parts.take(1), ::slantedBoundary), 0f)
        assertEquals(CompositionType.DIAGONAL, LightweightAnalyzer.recommend(emptyList(), score).recommendedType)
    }

    @Test
    fun diagonalRejectsPerspectiveThinEdgesAndParallelTexture() {
        val vanishingPoint = listOf(
            segment(0.1f, 0.9f, 0.48f, 0.51f),
            segment(0.52f, 0.51f, 0.9f, 0.9f)
        )
        assertEquals(0f, DiagonalEvidence.score(vanishingPoint, ::slantedBoundary), 0f)
        val main = segment(0.1f, 0.29f, 0.9f, 0.71f)
        assertEquals(0f, DiagonalEvidence.score(listOf(main)) { _, _ -> 120 }, 0f)
        val repeated = listOf(main) + listOf(-0.10f, 0.05f, 0.10f).map { offset ->
            segment(0.1f, 0.29f + offset, 0.9f, 0.71f + offset)
        }
        assertEquals(0f, DiagonalEvidence.score(repeated, ::slantedBoundary), 0f)
    }

    @Test
    fun visibleEyesTakePrecedenceAndPrimaryFaceSurvivesRankChanges() {
        val subject = DetectedSubject(RectF(0.22f, 0.22f, 0.78f, 0.78f), 0.8f, SubjectType.PERSON)
        val left = PointF(0.25f, 1f / 3f)
        val right = PointF(0.42f, 1f / 3f)
        val portrait = requireNotNull(Portrait.fromEyes(subject, left, right))
        assertNull(Portrait.fromEyes(subject, left, null))
        assertNull(Portrait.fromEyes(subject, PointF(0.19f, left.y), right))
        assertEquals(
            CompositionType.RULE_OF_THIRDS,
            LightweightAnalyzer.recommend(listOf(subject), 0f, portrait.anchor).recommendedType
        )

        val other = requireNotNull(Portrait.fromEyes(
            DetectedSubject(RectF(0.50f, 0.10f, 0.99f, 0.90f), 0.99f, SubjectType.PERSON),
            PointF(0.63f, 0.35f), PointF(0.86f, 0.35f)
        ))
        val selection = PortraitSelection()
        assertEquals(portrait, selection.update(listOf(portrait)))
        assertEquals(portrait, selection.update(listOf(other, portrait)))
        assertNull(selection.update(listOf(other)))
        assertEquals(portrait, selection.update(listOf(other, portrait)))
        assertNull(selection.update(listOf(other)))
        assertEquals(other, selection.update(listOf(other)))
    }

    @Test
    fun changesRecommendationOnlyAfterTwoConsistentFrames() {
        val stability = RecommendationStability()
        val centered = recommendationAt(0.5f, 0.5f)
        val thirds = recommendationAt(1f / 3f, 1f / 3f)
        val empty = LightweightAnalyzer.recommend(emptyList(), 0f)
        assertNull(stability.update(centered).recommendedType)
        assertEquals(CompositionType.CENTER, stability.update(centered).recommendedType)
        assertEquals(CompositionType.CENTER, stability.update(thirds).recommendedType)
        assertEquals(CompositionType.CENTER, stability.update(centered).recommendedType)
        assertEquals(CompositionType.CENTER, stability.update(empty).recommendedType)
        assertNull(stability.update(empty).recommendedType)
        assertNull(stability.update(thirds).recommendedType)
        assertEquals(CompositionType.RULE_OF_THIRDS, stability.update(thirds).recommendedType)
        stability.reset()
        assertNull(stability.update(thirds).recommendedType)
    }

    private fun recommendationAt(x: Float, y: Float): FrameAnalysisResult {
        val box = RectF(x - 0.04f, y - 0.04f, x + 0.04f, y + 0.04f)
        return LightweightAnalyzer.recommend(listOf(DetectedSubject(box, 0.8f, SubjectType.OBJECT)), 0f)
    }

    private fun segment(x1: Float, y1: Float, x2: Float, y2: Float) =
        FrameSegment(PointF(x1, y1), PointF(x2, y2))

    private fun slantedBoundary(x: Float, y: Float) = if (y > 0.2375f + 0.525f * x) 180 else 60
}
