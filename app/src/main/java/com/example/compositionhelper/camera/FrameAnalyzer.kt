package com.example.compositionhelper.camera

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.compositionhelper.model.*
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/** Samples the latest CameraX frame while smart mode is enabled. */
class FrameAnalyzer(
    private val onAnalysisResult: (FrameAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {
    private val objectDetector = lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .build()
        ObjectDetection.getClient(options)
    }
    private val faceDetector = lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
        )
    }
    private val lineDetector = FrameLineDetector()
    private val stability = RecommendationStability()
    private val portraitSelection = PortraitSelection()
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
        portraitSelection.reset()
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
        val image = try {
            InputImage.fromMediaImage(mediaImage, rotation)
        } catch (_: IllegalArgumentException) {
            imageProxy.close()
            return
        }
        val diagonalScore = lineDetector.diagonalScore(imageProxy, crop)
        val pending = startDetection(token, "face") { faceDetector.value.process(image) }
        if (pending == null) {
            analyzeObjects(imageProxy, image, viewport, token, diagonalScore)
            return
        }
        pending.addOnCompleteListener { task ->
            var delegated = false
            try {
                val faces = if (task.isSuccessful) task.result.mapNotNull { it.toPortrait(viewport) } else emptyList()
                val portrait = synchronized(this) {
                    if (!isCurrent(token)) return@addOnCompleteListener
                    portraitSelection.update(faces)
                }
                if (portrait != null) {
                    publish(token, LightweightAnalyzer.recommend(faces.map { it.subject }, diagonalScore, portrait.anchor))
                } else {
                    delegated = true
                    analyzeObjects(imageProxy, image, viewport, token, diagonalScore)
                }
            } finally {
                if (!delegated) imageProxy.close()
            }
        }
    }

    private fun Face.toPortrait(viewport: RectF): Portrait? {
        if (kotlin.math.abs(headEulerAngleY) > 36f) return null
        val bounds = boundingBox
        val box = CameraGeometry.toViewport(
            RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat()),
            viewport
        ) ?: return null
        val left = getLandmark(FaceLandmark.LEFT_EYE)?.position?.let {
            CameraGeometry.toViewport(PointF(it.x, it.y), viewport)
        }
        val right = getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let {
            CameraGeometry.toViewport(PointF(it.x, it.y), viewport)
        }
        return Portrait.fromEyes(DetectedSubject(box, .8f, SubjectType.PERSON), left, right)
    }

    private fun analyzeObjects(
        imageProxy: ImageProxy, image: InputImage, viewport: RectF, token: Int, diagonalScore: Float
    ) {
        val pending = startDetection(token, "object") { objectDetector.value.process(image) }
        if (pending == null) {
            try {
                publish(token, LightweightAnalyzer.recommend(emptyList(), diagonalScore))
            } finally {
                imageProxy.close()
            }
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
    private fun <T> startDetection(token: Int, kind: String, process: () -> Task<T>): Task<T>? {
        if (!isCurrent(token)) return null
        return try {
            process()
        } catch (_: RuntimeException) {
            Log.w("FrameAnalyzer", "Unable to start $kind detection")
            null
        } catch (_: LinkageError) {
            Log.w("FrameAnalyzer", "Unable to load $kind detection")
            null
        }
    }

    private fun isCurrent(token: Int) = !closed && enabled && token == generation

    @Synchronized
    private fun publish(token: Int, candidate: FrameAnalysisResult) {
        if (!isCurrent(token)) return
        onAnalysisResult(stability.update(candidate))
    }

    @Synchronized
    fun close() {
        closed = true
        if (faceDetector.isInitialized()) faceDetector.value.close()
        if (objectDetector.isInitialized()) objectDetector.value.close()
    }
}
