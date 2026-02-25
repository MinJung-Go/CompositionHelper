package com.example.compositionhelper

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.compositionhelper.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 构图分析器
 */
class ImageAnalyzer {
    companion object {
        private const val TAG = "ImageAnalyzer"
        private const val SYMMETRY_THRESHOLD = 20.0

        // 单例检测器，避免每次分析都创建新实例
        private val objectDetector: ObjectDetector by lazy {
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .build()
            ObjectDetection.getClient(options)
        }

        /**
         * 主分析方法
         */
        suspend fun analyze(bitmap: Bitmap): CompositionAnalysis {
            val detectedSubjects = detectObjects(bitmap)
            val detectedLines = detectEdges(bitmap)
            val characteristics = analyzeCharacteristics(bitmap)

            val recommendations = recommendCompositions(
                subjects = detectedSubjects,
                lines = detectedLines,
                characteristics = characteristics
            )

            return CompositionAnalysis(
                recommendedCompositions = recommendations.map { it.type },
                confidenceScores = recommendations.associate { it.type to it.score },
                detectedSubjects = detectedSubjects,
                detectedLines = detectedLines,
                imageCharacteristics = characteristics
            )
        }

        /**
         * 使用 ML Kit 检测对象
         */
        private suspend fun detectObjects(bitmap: Bitmap): List<DetectedSubject> {
            val subjects = mutableListOf<DetectedSubject>()

            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = objectDetector.process(image).await()

                for (detectedObj in result) {
                    val boundingBox = detectedObj.boundingBox

                    subjects.add(
                        DetectedSubject(
                            boundingBox = RectF(
                                left = boundingBox.left.toFloat() / bitmap.width,
                                top = boundingBox.top.toFloat() / bitmap.height,
                                right = boundingBox.right.toFloat() / bitmap.width,
                                bottom = boundingBox.bottom.toFloat() / bitmap.height
                            ),
                            confidence = detectedObj.trackingId?.toFloat() ?: 0.8f,
                            type = SubjectType.OBJECT
                        )
                    )
                }

                Log.d(TAG, "检测到 ${subjects.size} 个对象")
            } catch (e: Exception) {
                Log.e(TAG, "对象检测失败: ${e.message}")
            }

            return removeOverlappingSubjects(subjects)
        }

        /**
         * 边缘检测（使用 Sobel 算子）
         */
        private fun detectEdges(bitmap: Bitmap): List<Line> {
            val lines = mutableListOf<Line>()
            val width = bitmap.width
            val height = bitmap.height

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // 检测水平线
            for (y in 0 until height step 10) {
                var startX = -1
                var endX = -1

                for (x in 0 until width step 10) {
                    val idx = y * width + x
                    val brightness = getBrightness(pixels[idx])

                    if (brightness > 200 && startX == -1) {
                        startX = x
                    } else if (brightness < 50 && startX != -1 && endX == -1) {
                        endX = x

                        val line = Line(
                            startX = startX.toFloat() / width,
                            startY = y.toFloat() / height,
                            endX = endX.toFloat() / width,
                            endY = y.toFloat() / height,
                            angle = 0.0,
                            length = (endX - startX).toDouble() / width
                        )
                        lines.add(line)

                        startX = -1
                        endX = -1
                    }
                }
            }

            // 检测垂直线
            for (x in 0 until width step 10) {
                var startY = -1
                var endY = -1

                for (y in 0 until height step 10) {
                    val idx = y * width + x
                    val brightness = getBrightness(pixels[idx])

                    if (brightness > 200 && startY == -1) {
                        startY = y
                    } else if (brightness < 50 && startY != -1 && endY == -1) {
                        endY = y

                        val line = Line(
                            startX = x.toFloat() / width,
                            startY = startY.toFloat() / height,
                            endX = x.toFloat() / width,
                            endY = endY.toFloat() / height,
                            angle = Math.PI / 2,
                            length = (endY - startY).toDouble() / height
                        )
                        lines.add(line)

                        startY = -1
                        endY = -1
                    }
                }
            }

            val diagonalLines = detectDiagonalLines(lines)
            lines.addAll(diagonalLines)

            return lines.filter { it.length > 0.1 }
        }

        /**
         * 检测对角线
         */
        private fun detectDiagonalLines(horizontalAndVerticalLines: List<Line>): List<Line> {
            val diagonalLines = mutableListOf<Line>()

            val horizontalLines = horizontalAndVerticalLines.filter { abs(it.angle) < 0.2 }
            val verticalLines = horizontalAndVerticalLines.filter { abs(it.angle - Math.PI / 2) < 0.2 }

            for (hLine in horizontalLines) {
                for (vLine in verticalLines) {
                    val dx = abs(hLine.startX - vLine.startX)
                    val dy = abs(hLine.startY - vLine.startY)

                    if (dx < 0.15f && dy < 0.15f) {
                        val length = sqrt((dx * dx + dy * dy).toDouble())
                        val angle = atan2(dy.toDouble(), dx.toDouble())

                        if (length > 0.2) {
                            diagonalLines.add(
                                Line(
                                    startX = hLine.startX,
                                    startY = hLine.startY,
                                    endX = vLine.startX,
                                    endY = vLine.startY,
                                    angle = angle,
                                    length = length
                                )
                            )
                        }
                    }
                }
            }

            return diagonalLines
        }

        /**
         * 图像特征分析
         */
        private suspend fun analyzeCharacteristics(bitmap: Bitmap): ImageCharacteristics {
            val lines = detectEdges(bitmap)
            val hasStrongLeadingLines = lines.size > 3
            val hasSymmetry = detectSymmetry(bitmap)
            val brightnessDistribution = analyzeBrightnessDistribution(bitmap)
            val colorHarmony = 0.7
            val mainSubjectPosition = PointF(0.5f, 0.5f)

            return ImageCharacteristics(
                hasStrongLeadingLines = hasStrongLeadingLines,
                hasSymmetry = hasSymmetry,
                mainSubjectPosition = mainSubjectPosition,
                brightnessDistribution = brightnessDistribution,
                colorHarmony = colorHarmony
            )
        }

        /**
         * 检测对称性
         */
        private fun detectSymmetry(bitmap: Bitmap): Boolean {
            val width = bitmap.width
            val height = bitmap.height

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var totalDifference = 0
            var totalPixels = 0

            for (y in 0 until minOf(height, 500)) {
                for (x in 0 until minOf(width / 2, 250)) {
                    val leftIdx = y * width + x
                    val rightIdx = y * width + (width - 1 - x)

                    val leftBrightness = getBrightness(pixels[leftIdx])
                    val rightBrightness = getBrightness(pixels[rightIdx])

                    totalDifference += abs(leftBrightness - rightBrightness)
                    totalPixels++
                }
            }

            if (totalPixels == 0) return false

            val avgDifference = totalDifference.toDouble() / totalPixels
            return avgDifference < SYMMETRY_THRESHOLD
        }

        /**
         * 分析亮度分布
         */
        private fun analyzeBrightnessDistribution(bitmap: Bitmap): BrightnessDistribution {
            val width = bitmap.width
            val height = bitmap.height

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var centerSum = 0
            var centerCount = 0
            var leftSum = 0
            var leftCount = 0
            var rightSum = 0
            var rightCount = 0

            for (y in 0 until height step 20) {
                for (x in 0 until width step 20) {
                    val idx = y * width + x
                    val brightness = getBrightness(pixels[idx])

                    val normalizedX = x.toDouble() / width

                    if (normalizedX > 0.3 && normalizedX < 0.7) {
                        centerSum += brightness
                        centerCount++
                    } else if (normalizedX <= 0.3) {
                        leftSum += brightness
                        leftCount++
                    } else {
                        rightSum += brightness
                        rightCount++
                    }
                }
            }

            if (centerCount == 0 || leftCount == 0 || rightCount == 0) {
                return BrightnessDistribution.BALANCED
            }

            val centerAvg = centerSum.toDouble() / centerCount
            val leftAvg = leftSum.toDouble() / leftCount
            val rightAvg = rightSum.toDouble() / rightCount

            val threshold = 30.0

            return when {
                centerAvg > leftAvg + threshold && centerAvg > rightAvg + threshold ->
                    BrightnessDistribution.CENTER_WEIGHTED
                abs(leftAvg - rightAvg) < threshold ->
                    BrightnessDistribution.BALANCED
                leftAvg > rightAvg + threshold ->
                    BrightnessDistribution.LEFT_WEIGHTED
                else ->
                    BrightnessDistribution.RIGHT_WEIGHTED
            }
        }

        /**
         * 构图推荐算法
         */
        internal fun recommendCompositions(
            subjects: List<DetectedSubject>,
            lines: List<Line>,
            characteristics: ImageCharacteristics
        ): List<CompositionRecommendation> {
            val recommendations = mutableListOf<CompositionRecommendation>()

            for (subject in subjects) {
                val position = subject.boundingBox

                val thirdX1 = 1.0f / 3.0f
                val thirdX2 = 2.0f / 3.0f
                val thirdY1 = 1.0f / 3.0f
                val thirdY2 = 2.0f / 3.0f

                val centerX = position.centerX()
                val centerY = position.centerY()

                val isOnThirdPoint = abs(centerX - thirdX1) < 0.15f ||
                        abs(centerX - thirdX2) < 0.15f ||
                        abs(centerY - thirdY1) < 0.15f ||
                        abs(centerY - thirdY2) < 0.15f

                if (isOnThirdPoint) {
                    addOrUpdateRecommendation(recommendations, CompositionType.RULE_OF_THIRDS, 0.9)
                }

                val isCentered = abs(centerX - 0.5f) < 0.1f && abs(centerY - 0.5f) < 0.1f
                if (isCentered) {
                    addOrUpdateRecommendation(recommendations, CompositionType.CENTER, 0.85)
                }
            }

            if (lines.isNotEmpty()) {
                val horizontalLines = lines.filter { abs(it.angle) < 0.2 || abs(it.angle - Math.PI) < 0.2 }
                val verticalLines = lines.filter { abs(it.angle - Math.PI / 2) < 0.2 || abs(it.angle + Math.PI / 2) < 0.2 }
                val diagonalLines = lines.filter { abs(it.angle - Math.PI / 4) < 0.2 || abs(it.angle + Math.PI / 4) < 0.2 }

                if (horizontalLines.size > 2 || verticalLines.size > 2) {
                    addOrUpdateRecommendation(recommendations, CompositionType.LEADING_LINES, 0.8)
                }

                if (diagonalLines.size > 1) {
                    addOrUpdateRecommendation(recommendations, CompositionType.DIAGONAL, 0.75)
                }
            }

            if (characteristics.hasSymmetry) {
                addOrUpdateRecommendation(recommendations, CompositionType.SYMMETRY, 0.9)
                addOrUpdateRecommendation(recommendations, CompositionType.CENTER, 0.85)
            }

            when (characteristics.brightnessDistribution) {
                BrightnessDistribution.CENTER_WEIGHTED -> {
                    addOrUpdateRecommendation(recommendations, CompositionType.CENTER, 0.7)
                }
                BrightnessDistribution.BALANCED -> {
                    addOrUpdateRecommendation(recommendations, CompositionType.RULE_OF_THIRDS, 0.75)
                }
                BrightnessDistribution.LEFT_WEIGHTED, BrightnessDistribution.RIGHT_WEIGHTED -> {
                    addOrUpdateRecommendation(recommendations, CompositionType.S_CURVE, 0.65)
                }
                BrightnessDistribution.CORNER_WEIGHTED -> {
                    addOrUpdateRecommendation(recommendations, CompositionType.FRAME, 0.7)
                }
            }

            if (characteristics.hasStrongLeadingLines) {
                addOrUpdateRecommendation(recommendations, CompositionType.LEADING_LINES, 0.85)
            }

            if (recommendations.isEmpty()) {
                recommendations.add(CompositionRecommendation(type = CompositionType.RULE_OF_THIRDS, score = 0.6))
                recommendations.add(CompositionRecommendation(type = CompositionType.CENTER, score = 0.5))
            }

            recommendations.sortByDescending { it.score }
            return recommendations.take(5)
        }

        internal data class CompositionRecommendation(
            val type: CompositionType,
            val score: Double
        )

        private fun addOrUpdateRecommendation(
            recommendations: MutableList<CompositionRecommendation>,
            type: CompositionType,
            score: Double
        ) {
            val index = recommendations.indexOfFirst { it.type == type }
            if (index != -1) {
                recommendations[index] = CompositionRecommendation(
                    type = type,
                    score = maxOf(recommendations[index].score, score)
                )
            } else {
                recommendations.add(CompositionRecommendation(type, score))
            }
        }

        private fun removeOverlappingSubjects(subjects: List<DetectedSubject>): List<DetectedSubject> {
            val filtered = mutableListOf<DetectedSubject>()

            for (subject in subjects) {
                var hasOverlap = false
                for (existing in filtered) {
                    if (subject.boundingBox.intersects(existing.boundingBox)) {
                        hasOverlap = true
                        if (subject.confidence > existing.confidence) {
                            val index = filtered.indexOf(existing)
                            if (index != -1) {
                                filtered[index] = subject
                            }
                        }
                        break
                    }
                }

                if (!hasOverlap) {
                    filtered.add(subject)
                }
            }

            return filtered
        }

        private fun getBrightness(pixel: Int): Int {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            return (r + g + b) / 3
        }
    }
}
