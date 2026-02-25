package com.example.compositionhelper.model

/**
 * 构图分析结果
 */
data class CompositionAnalysis(
    val recommendedCompositions: List<CompositionType>,
    val confidenceScores: Map<CompositionType, Double>,
    val detectedSubjects: List<DetectedSubject>,
    val detectedLines: List<Line>,
    val imageCharacteristics: ImageCharacteristics
)

/**
 * 检测到的主体
 */
data class DetectedSubject(
    val boundingBox: RectF,
    val confidence: Float,
    val type: SubjectType
)

/**
 * 主体类型
 */
enum class SubjectType {
    PERSON,
    ANIMAL,
    OBJECT,
    TEXT,
    UNKNOWN
}

/**
 * 检测到的线条
 */
data class Line(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val angle: Double,
    val length: Double
)

/**
 * 图像特征
 */
data class ImageCharacteristics(
    val hasStrongLeadingLines: Boolean,
    val hasSymmetry: Boolean,
    val mainSubjectPosition: PointF,
    val brightnessDistribution: BrightnessDistribution,
    val colorHarmony: Double
)

/**
 * 亮度分布类型
 */
enum class BrightnessDistribution {
    CENTER_WEIGHTED,
    BALANCED,
    CORNER_WEIGHTED,
    LEFT_WEIGHTED,
    RIGHT_WEIGHTED
}

/**
 * RectF
 */
data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun centerX() = (left + right) / 2
    fun centerY() = (top + bottom) / 2
    fun width() = right - left
    fun height() = bottom - top

    fun intersects(other: RectF): Boolean {
        val intersection = RectF(
            left = maxOf(left, other.left),
            top = maxOf(top, other.top),
            right = minOf(right, other.right),
            bottom = minOf(bottom, other.bottom)
        )
        val intersectionArea = intersection.width() * intersection.height()
        val selfArea = width() * height()
        val otherArea = other.width() * other.height()

        return intersectionArea > 0 && (intersectionArea / selfArea > 0.3f || intersectionArea / otherArea > 0.3f)
    }
}

/**
 * PointF
 */
data class PointF(val x: Float, val y: Float)

/**
 * 实时帧分析结果
 */
data class FrameAnalysisResult(
    val recommendedType: CompositionType?,
    val confidence: Float,
    val detectedSubjects: List<DetectedSubject>,
    val guidanceHint: String?
)
