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
    fun diagonalRequiresALongSegmentNearEitherGuide() {
        val score = LightweightAnalyzer.diagonalScore(0.1f, 0.1f, 0.9f, 0.9f)
        assertTrue(score > 0f)
        assertEquals(score, LightweightAnalyzer.diagonalScore(0.1f, 0.9f, 0.9f, 0.1f), 0.001f)
        assertEquals(0f, LightweightAnalyzer.diagonalScore(0.4f, 0.4f, 0.6f, 0.6f), 0f)
        assertEquals(0f, LightweightAnalyzer.diagonalScore(0f, 0.3f, 0.7f, 1f), 0f)
        assertEquals(0f, LightweightAnalyzer.diagonalScore(0.1f, 0.5f, 0.9f, 0.5f), 0f)
        assertEquals(CompositionType.DIAGONAL, LightweightAnalyzer.recommend(emptyList(), score).recommendedType)
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
}
