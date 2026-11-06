package com.example.compositionhelper.camera

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.compositionhelper.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/** Samples the latest CameraX frame while smart mode is enabled. */
class FrameAnalyzer(
    private val onAnalysisResult: (FrameAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {
    private val detector = lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .build()
        ObjectDetection.getClient(options)
    }
    private val lineDetector = FrameLineDetector()
    private val stability = RecommendationStability()
    private var enabled = false
    private var closed = false
    private var generation = 0
    private var lastAnalysisTimestamp: Long? = null

    @Synchronized
    fun setEnabled(value: Boolean) {
        if (closed || value == enabled) return
        enabled = value
        generation++
        lastAnalysisTimestamp = null
        stability.reset()
    }

    @Synchronized
    private fun startFrame(now: Long): Int? {
        val last = lastAnalysisTimestamp
        if (closed || !enabled || (last != null && now - last < 200L)) return null
        lastAnalysisTimestamp = now
        return generation
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val token = startFrame(SystemClock.elapsedRealtime())
        if (token == null) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val crop = imageProxy.cropRect
        val rotation = imageProxy.imageInfo.rotationDegrees
        val viewport = CameraGeometry.rotatedCrop(
            RectF(crop.left.toFloat(), crop.top.toFloat(), crop.right.toFloat(), crop.bottom.toFloat()),
            imageProxy.width.toFloat(), imageProxy.height.toFloat(), rotation
        )
        val diagonalScore = lineDetector.diagonalScore(imageProxy, crop)

        val pending = synchronized(this) {
            if (closed || !enabled || token != generation) null
            else try {
                detector.value.process(InputImage.fromMediaImage(mediaImage, rotation))
            } catch (e: Exception) {
                Log.w("FrameAnalyzer", "Unable to start object detection", e)
                null
            }
        }
        if (pending == null) {
            imageProxy.close()
            return
        }

        pending.addOnCompleteListener { task ->
            try {
                val subjects = if (task.isSuccessful) task.result.mapNotNull { obj ->
                    val box = obj.boundingBox
                    CameraGeometry.toViewport(
                        RectF(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat()),
                        viewport
                    )?.let { DetectedSubject(it, .8f, SubjectType.OBJECT) }
                } else emptyList()
                publish(token, LightweightAnalyzer.recommend(subjects, diagonalScore))
            } finally {
                imageProxy.close()
            }
        }
    }

    @Synchronized
    private fun publish(token: Int, candidate: FrameAnalysisResult) {
        if (closed || !enabled || token != generation) return
        onAnalysisResult(stability.update(candidate))
    }

    @Synchronized
    fun close() {
        closed = true
        if (detector.isInitialized()) detector.value.close()
    }
}
