package com.example.compositionhelper.camera

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.compositionhelper.model.PointF
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

/** Reads only the cropped luma plane. Diagonal scoring is invariant under quarter turns. */
internal class FrameLineDetector {
    private val detector by lazy {
        if (OpenCVLoader.initLocal()) Imgproc.createLineSegmentDetector() else null
    }

    fun diagonalScore(image: ImageProxy, crop: Rect): Float = try {
        val lsd = detector
        if (lsd == null || crop.width() < 2 || crop.height() < 2) {
            0f
        } else {
            val scale = minOf(1f, 320f / maxOf(crop.width(), crop.height()))
            val width = (crop.width() * scale).roundToInt().coerceAtLeast(2)
            val height = (crop.height() * scale).roundToInt().coerceAtLeast(2)
            val plane = image.planes[0]
            val buffer = plane.buffer
            val offset = buffer.position()
            val luma = ByteArray(width * height)
            for (y in 0 until height) {
                val sourceY = crop.top + y * crop.height() / height
                val row = offset + sourceY * plane.rowStride
                for (x in 0 until width) {
                    val sourceX = crop.left + x * crop.width() / width
                    luma[y * width + x] = buffer.get(row + sourceX * plane.pixelStride)
                }
            }

            val grayscale = Mat()
            val segments = Mat()
            try {
                grayscale.create(height, width, CvType.CV_8UC1)
                grayscale.put(0, 0, luma)
                lsd.detect(grayscale, segments)
                val edges = List(segments.rows()) { row ->
                    val segment = segments.get(row, 0)
                    FrameSegment(
                        PointF(segment[0].toFloat() / width, segment[1].toFloat() / height),
                        PointF(segment[2].toFloat() / width, segment[3].toFloat() / height)
                    )
                }
                DiagonalEvidence.score(edges) { x, y ->
                    val index = (y * (height - 1)).roundToInt() * width + (x * (width - 1)).roundToInt()
                    luma[index].toInt() and 0xff
                }
            } finally {
                grayscale.release()
                segments.release()
            }
        }
    } catch (e: Exception) {
        Log.w("FrameLineDetector", "Unable to detect line segments", e)
        0f
    } catch (e: LinkageError) {
        Log.w("FrameLineDetector", "OpenCV is unavailable", e)
        0f
    }
}
