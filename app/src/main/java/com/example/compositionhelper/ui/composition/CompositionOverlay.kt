package com.example.compositionhelper.ui.composition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.example.compositionhelper.model.CompositionType
import com.example.compositionhelper.overlay.CanvasRenderer
import com.example.compositionhelper.overlay.drawComposition

// 带有构图叠加的图片组件
@Composable
fun ImageWithCompositionOverlay(
    bitmap: Bitmap,
    compositionType: CompositionType,
    lineOpacity: Float,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val compositionBitmap = remember(bitmap, compositionType, lineOpacity, lineColor) {
        drawCompositionOverlay(
            originalBitmap = bitmap,
            compositionType = compositionType,
            lineOpacity = lineOpacity,
            lineColor = lineColor
        )
    }

    android.graphics.Bitmap
        .createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)?.let { overlayBitmap ->
            val canvas = Canvas(overlayBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            canvas.drawBitmap(compositionBitmap, 0f, 0f, null)
            Image(
                bitmap = overlayBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier
            )
        } ?: run {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier
            )
        }
}

// 绘制构图辅助线（委托给 CompositionDrawing）
fun drawCompositionOverlay(
    originalBitmap: Bitmap,
    compositionType: CompositionType,
    lineOpacity: Float,
    lineColor: Color
): Bitmap {
    val overlayBitmap = android.graphics.Bitmap.createBitmap(
        originalBitmap.width,
        originalBitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(overlayBitmap)
    val width = originalBitmap.width.toFloat()
    val height = originalBitmap.height.toFloat()

    val paint = Paint().apply {
        color = AndroidColor.argb(
            (lineOpacity * 255).toInt(),
            (lineColor.red * 255).toInt(),
            (lineColor.green * 255).toInt(),
            (lineColor.blue * 255).toInt()
        )
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val renderer = CanvasRenderer(canvas, paint)
    drawComposition(renderer, compositionType, width, height)

    return overlayBitmap
}
