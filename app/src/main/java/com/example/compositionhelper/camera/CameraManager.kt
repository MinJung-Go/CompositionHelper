package com.example.compositionhelper.camera

import android.content.Context
import android.net.Uri
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 生命周期管理器
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null

    suspend fun initialize() {
        cameraProvider = suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun bindPreview(
        previewView: PreviewView,
        captureWidth: Int,
        captureHeight: Int,
        enableAnalysis: Boolean = false,
        analyzer: ImageAnalysis.Analyzer? = null
    ) {
        val provider = cameraProvider ?: return
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .build()

        val useCases = mutableListOf<UseCase>(preview!!, imageCapture!!)

        if (enableAnalysis && analyzer != null) {
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        analyzer
                    )
                }
            useCases.add(imageAnalysis!!)
        }

        try {
            provider.unbindAll()
            val viewPort = ViewPort.Builder(
                Rational(captureWidth.coerceAtLeast(1), captureHeight.coerceAtLeast(1)),
                rotation
            )
                .setScaleType(ViewPort.FILL_CENTER)
                .build()
            val useCaseGroup = UseCaseGroup.Builder()
                .apply { useCases.forEach { addUseCase(it) } }
                .setViewPort(viewPort)
                .build()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
            Log.d(TAG, "Camera bound with ${useCases.size} use cases")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed: ${e.message}")
        }
    }

    fun capturePhoto(
        onSaved: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("ImageCapture not initialized"))
            return
        }

        val displayName = "composition_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(output.savedUri ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
    }
}
