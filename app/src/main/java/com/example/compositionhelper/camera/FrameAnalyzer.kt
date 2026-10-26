package com.example.compositionhelper.camera

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.compositionhelper.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * CameraX 帧分析器
 * 每 2.5 秒抽帧做 ML Kit 物体检测，推荐构图类型
 */
class FrameAnalyzer(
    private val onAnalysisResult: (FrameAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FrameAnalyzer"
        private const val ANALYSIS_INTERVAL_MS = 2500L
    }

    private val objectDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastAnalysisTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalysisTimestamp < ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastAnalysisTimestamp = currentTimestamp

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val imageWidth = imageProxy.width
                val imageHeight = imageProxy.height

                val subjects = detectedObjects.map { obj ->
                    DetectedSubject(
                        boundingBox = RectF(
                            left = obj.boundingBox.left.toFloat() / imageWidth,
                            top = obj.boundingBox.top.toFloat() / imageHeight,
                            right = obj.boundingBox.right.toFloat() / imageWidth,
                            bottom = obj.boundingBox.bottom.toFloat() / imageHeight
                        ),
                        confidence = 0.8f,
                        type = SubjectType.OBJECT
                    )
                }

                val result = LightweightAnalyzer.recommend(subjects)

                // 回到主线程更新 Compose 状态
                mainHandler.post {
                    onAnalysisResult(result)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Frame analysis failed: ${e.message}")
            }
            .addOnCompleteListener {
                // 必须在所有路径上关闭 imageProxy
                imageProxy.close()
            }
    }

    fun close() {
        objectDetector.close()
    }
}

/**
 * 轻量级构图推荐算法
 * 仅基于主体位置做推荐，跳过像素级分析
 */
object LightweightAnalyzer {

    fun recommend(subjects: List<DetectedSubject>): FrameAnalysisResult {
        if (subjects.isEmpty()) {
            return FrameAnalysisResult(
                recommendedType = CompositionType.RULE_OF_THIRDS,
                confidence = 0.5f,
                detectedSubjects = emptyList(),
                guidanceHint = "未检测到主体"
            )
        }

        // 找到面积最大的主体
        val mainSubject = subjects.maxByOrNull {
            it.boundingBox.width() * it.boundingBox.height()
        }!!

        val cx = mainSubject.boundingBox.centerX()
        val cy = mainSubject.boundingBox.centerY()

        // 三分法关键点
        val thirdPoints = listOf(
            PointF(1f / 3f, 1f / 3f), PointF(2f / 3f, 1f / 3f),
            PointF(1f / 3f, 2f / 3f), PointF(2f / 3f, 2f / 3f)
        )

        val nearestThird = thirdPoints.minByOrNull {
            sqrt(((cx - it.x) * (cx - it.x) + (cy - it.y) * (cy - it.y)).toDouble())
        }!!

        val distToThird = sqrt(
            ((cx - nearestThird.x) * (cx - nearestThird.x) +
                    (cy - nearestThird.y) * (cy - nearestThird.y)).toDouble()
        ).toFloat()

        val distToCenter = sqrt(
            ((cx - 0.5f) * (cx - 0.5f) + (cy - 0.5f) * (cy - 0.5f)).toDouble()
        ).toFloat()

        // 判断最佳构图
        val bestType: CompositionType
        val bestScore: Float
        val hint: String?

        if (distToCenter < 0.1f) {
            bestType = CompositionType.CENTER
            bestScore = (1.0f - distToCenter * 5f).coerceIn(0.5f, 1.0f)
            hint = if (distToCenter < 0.03f) "完美居中" else buildDirectionHint(0.5f - cx, 0.5f - cy)
        } else if (distToThird < 0.15f) {
            bestType = CompositionType.RULE_OF_THIRDS
            bestScore = (1.0f - distToThird * 4f).coerceIn(0.5f, 1.0f)
            hint = if (distToThird < 0.03f) "完美对齐三分点" else buildDirectionHint(nearestThird.x - cx, nearestThird.y - cy)
        } else {
            // 检查是否接近对角线
            val distToDiag1 = abs(cx - cy) // 到主对角线距离
            val distToDiag2 = abs(cx - (1f - cy)) // 到副对角线距离

            if (distToDiag1 < 0.1f || distToDiag2 < 0.1f) {
                bestType = CompositionType.DIAGONAL
                bestScore = 0.7f
                hint = "主体在对角线上"
            } else {
                // 默认推荐三分法
                bestType = CompositionType.RULE_OF_THIRDS
                bestScore = 0.5f
                hint = buildDirectionHint(nearestThird.x - cx, nearestThird.y - cy)
            }
        }

        return FrameAnalysisResult(
            recommendedType = bestType,
            confidence = bestScore,
            detectedSubjects = subjects,
            guidanceHint = hint
        )
    }

    private fun buildDirectionHint(dx: Float, dy: Float): String? {
        if (abs(dx) < 0.02f && abs(dy) < 0.02f) return "位置很好"

        val parts = mutableListOf<String>()
        when {
            dx > 0.03f -> parts.add("向右")
            dx < -0.03f -> parts.add("向左")
        }
        when {
            dy > 0.03f -> parts.add("向下")
            dy < -0.03f -> parts.add("向上")
        }
        return if (parts.isNotEmpty()) "稍微${parts.joinToString("")}移动" else null
    }
}
