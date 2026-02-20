package com.example.compositionhelper.ui.composition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.DashPathEffect
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.example.compositionhelper.CompositionType

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
            // 如果创建失败，只显示原图
            Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = modifier
                )
        }
}

// 绘制构图辅助线
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

    when (compositionType) {
        CompositionType.RULE_OF_THIRDS -> drawRuleOfThirds(canvas, width, height, paint)
        CompositionType.CENTER -> drawCenterComposition(canvas, width, height, paint)
        CompositionType.DIAGONAL -> drawDiagonalLines(canvas, width, height, paint)
        CompositionType.FRAME -> drawFrameComposition(canvas, width, height, paint)
        CompositionType.LEADING_LINES -> drawLeadingLines(canvas, width, height, paint)
        CompositionType.S_CURVE -> drawSCurve(canvas, width, height, paint)
        CompositionType.GOLDEN_SPIRAL -> drawGoldenSpiral(canvas, width, height, paint)
        CompositionType.GOLDEN_TRIANGLE -> drawGoldenTriangle(canvas, width, height, paint)
        CompositionType.SYMMETRY -> drawSymmetry(canvas, width, height, paint)
        CompositionType.NEGATIVE_SPACE -> drawNegativeSpace(canvas, width, height, paint)
        CompositionType.PATTERN_REPEAT -> drawPatternRepeat(canvas, width, height, paint)
        CompositionType.TUNNEL -> drawTunnel(canvas, width, height, paint)
        CompositionType.SPLIT -> drawSplitComposition(canvas, width, height, paint)
        CompositionType.PERSPECTIVE -> drawPerspective(canvas, width, height, paint)
        CompositionType.INVISIBLE_LINE -> drawInvisibleLine(canvas, width, height, paint)
        CompositionType.FILL_FRAME -> drawFillFrame(canvas, width, height, paint)
        CompositionType.LOW_ANGLE -> drawAngleView(canvas, width, height, paint, true)
        CompositionType.HIGH_ANGLE -> drawAngleView(canvas, width, height, paint, false)
        CompositionType.DEPTH_LAYER -> drawDepthLayer(canvas, width, height, paint)
    }

    return overlayBitmap
}

// 三分法
private fun drawRuleOfThirds(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    // 垂直线
    canvas.drawLine(width / 3, 0f, width / 3, height, paint)
    canvas.drawLine(width * 2 / 3, 0f, width * 2 / 3, height, paint)
    
    // 水平线
    canvas.drawLine(0f, height / 3, width, height / 3, paint)
    canvas.drawLine(0f, height * 2 / 3, width, height * 2 / 3, paint)
}

// 中心构图
private fun drawCenterComposition(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    // 中心十字
    canvas.drawLine(width / 2, 0f, width / 2, height, paint)
    canvas.drawLine(0f, height / 2, width, height / 2, paint)
    
    // 中心圆
    canvas.drawCircle(width / 2, height / 2, 50f, paint)
}

// 对角线
private fun drawDiagonalLines(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    canvas.drawLine(0f, 0f, width, height, paint)
    canvas.drawLine(width, 0f, 0f, height, paint)
}

// 框架构图
private fun drawFrameComposition(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val frameWidth = width * 0.7f
    val frameHeight = height * 0.7f
    val left = (width - frameWidth) / 2
    val top = (height - frameHeight) / 2
    
    canvas.drawRect(left, top, left + frameWidth, top + frameHeight, paint)
}

// 引导线
private fun drawLeadingLines(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    for (i in 0 until 5) {
        val startX = width * i / 6
        canvas.drawLine(startX, height, width, height * i / 6, paint)
    }
}

// S形曲线
private fun drawSCurve(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val path = Path()
    path.moveTo(0f, height)
    
    // 贝塞尔曲线
    path.cubicTo(
        width * 0.3f, height,
        width * 0.7f, 0f,
        width, 0f
    )
    
    canvas.drawPath(path, paint)
}

// 黄金螺旋
private fun drawGoldenSpiral(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val phi = 1.618f
    val minSide = width.coerceAtMost(height)
    var currentSize = minSide
    var x = 0f
    var y = 0f
    var direction = 0 // 0=右, 1=下, 2=左, 3=上
    
    for (i in 0 until 8) {
        when (direction) {
            0 -> x += currentSize
            1 -> y += currentSize
            2 -> x -= currentSize
            3 -> y -= currentSize
        }
        
        val rect = RectF(x - currentSize, y - currentSize, x + currentSize, y + currentSize)
        canvas.drawArc(rect, (direction * 90).toFloat(), 90f, false, paint)
        
        currentSize /= phi
        direction = (direction + 1) % 4
    }
}

// 黄金三角
private fun drawGoldenTriangle(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()
    
    canvas.drawPath(path, paint)
    
    // 次级三角形
    val splitX = width / 1.618f
    path.moveTo(splitX, height)
    path.lineTo(splitX, height - (height / 1.618f))
    path.lineTo(0f, height - (height / 1.618f))
    path.close()
    
    canvas.drawPath(path, paint)
}

// 对称构图
private fun drawSymmetry(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    // 垂直对称线
    canvas.drawLine(width / 2, 0f, width / 2, height, paint)
    
    // 水平对称线
    canvas.drawLine(0f, height / 2, width, height / 2, paint)
    
    // 四角标记
    val corners = listOf(
        Pair(width * 0.25f, height * 0.25f),
        Pair(width * 0.75f, height * 0.25f),
        Pair(width * 0.25f, height * 0.75f),
        Pair(width * 0.75f, height * 0.75f)
    )
    
    corners.forEach { (x, y) ->
        canvas.drawCircle(x, y, 10f, paint)
    }
}

// 负空间
private fun drawNegativeSpace(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val subjectWidth = width * 0.35f
    val subjectHeight = height * 0.4f
    
    paint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    
    // 左上角区域
    canvas.drawRect(
        width * 0.1f, height * 0.15f,
        width * 0.1f + subjectWidth, height * 0.15f + subjectHeight,
        paint
    )
    
    // 右下角区域
    canvas.drawRect(
        width * 0.55f, height * 0.45f,
        width * 0.55f + subjectWidth, height * 0.45f + subjectHeight,
        paint
    )
    
    paint.pathEffect = null
}

// 模式重复
private fun drawPatternRepeat(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val cols = 3
    val rows = 3
    val cellWidth = width / cols
    val cellHeight = height / rows
    
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val x = col * cellWidth + cellWidth * 0.2f
            val y = row * cellHeight + cellHeight * 0.2f
            val rectWidth = cellWidth * 0.6f
            val rectHeight = cellHeight * 0.6f
            
            canvas.drawRect(x, y, x + rectWidth, y + rectHeight, paint)
            
            // 中心点
            val cx = x + rectWidth / 2
            val cy = y + rectHeight / 2
            canvas.drawLine(cx - 3, cy, cx + 3, cy, paint)
            canvas.drawLine(cx, cy - 3, cx, cy + 3, paint)
        }
    }
}

// 隧道式
private fun drawTunnel(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val centerX = width / 2
    val centerY = height / 2
    val maxRadius = width.coerceAtMost(height) * 0.45f
    
    for (i in 0 until 5) {
        val scale = 1.0f - i * 0.18f
        val currentRadius = maxRadius * scale
        val currentWidth = width * scale
        val currentHeight = height * scale
        
        canvas.drawRect(
            centerX - currentWidth / 2,
            centerY - currentHeight / 2,
            centerX + currentWidth / 2,
            centerY + currentHeight / 2,
            paint
        )
    }
    
    // 消失点
    canvas.drawCircle(centerX, centerY, 8f, paint)
}

// 分割构图
private fun drawSplitComposition(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    // 水平分割
    canvas.drawLine(0f, height / 2, width, height / 2, paint)
    
    // 垂直分割
    canvas.drawLine(width / 2, 0f, width / 2, height, paint)
    
    // 对角分割
    paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    canvas.drawLine(0f, 0f, width, height, paint)
    canvas.drawLine(width, 0f, 0f, height, paint)
    paint.pathEffect = null
}

// 透视焦点
private fun drawPerspective(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val centerX = width / 2
    val centerY = height * 0.6f
    
    // 从消失点发出的射线
    val points = listOf(
        Pair(0f, 0f),
        Pair(width * 0.25f, 0f),
        Pair(width * 0.5f, 0f),
        Pair(width * 0.75f, 0f),
        Pair(width, 0f),
        Pair(0f, height * 0.3f),
        Pair(width, height * 0.3f)
    )
    
    points.forEach { (x, y) ->
        canvas.drawLine(centerX, centerY, x, y, paint)
    }
    
    // 消失点
    canvas.drawCircle(centerX, centerY, 6f, paint)
}

// 隐形线
private fun drawInvisibleLine(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val path = Path()
    
    // S形隐形线
    path.moveTo(width * 0.2f, height * 0.8f)
    path.cubicTo(
        width * 0.2f, height * 0.3f,
        width * 0.8f, height * 0.7f,
        width * 0.8f, height * 0.2f
    )
    
    paint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    canvas.drawPath(path, paint)
    paint.pathEffect = null
    
    // 视觉焦点
    canvas.drawCircle(width * 0.8f, height * 0.2f, 20f, paint)
}

// 充满画面
private fun drawFillFrame(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    val margin = 10f
    
    // 外边框
    canvas.drawRect(
        margin, margin,
        width - margin, height - margin,
        paint
    )
    
    // 内部填充提示
    paint.pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
    val fillRatio = 0.85f
    canvas.drawRect(
        (width - width * fillRatio) / 2,
        (height - height * fillRatio) / 2,
        (width + width * fillRatio) / 2,
        (height + height * fillRatio) / 2,
        paint
    )
    paint.pathEffect = null
}

// 角度视图
private fun drawAngleView(canvas: Canvas, width: Float, height: Float, paint: Paint, isLowAngle: Boolean) {
    val centerX = width / 2
    val centerY = height / 2
    
    if (isLowAngle) {
        // 低角度 - 从下往上
        canvas.drawLine(centerX, height, centerX, height * 0.2f, paint)
        
        // 扇形视野
        val path = Path()
        path.moveTo(centerX, height * 0.2f)
        path.addArc(
            centerX, height * 0.2f,
            width * 0.3f, 0f,
            220f, 100f
        )
        canvas.drawPath(path, paint)
    } else {
        // 高角度 - 从上往下
        canvas.drawLine(centerX, 0f, centerX, height * 0.8f, paint)
        
        // 扇形视野
        val path = Path()
        path.moveTo(centerX, height * 0.8f)
        path.addArc(
            centerX, height * 0.8f,
            width * 0.3f, 0f,
            40f, 100f
        )
        canvas.drawPath(path, paint)
    }
}

// 深度层次
private fun drawDepthLayer(canvas: Canvas, width: Float, height: Float, paint: Paint) {
    // 前景层
    val foregroundHeight = height * 0.25f
    val foregroundPaint = Paint(paint).apply {
        alpha = (0.15f * 255).toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRect(
        0f, height - foregroundHeight,
        width, height,
        foregroundPaint
    )
    canvas.drawRect(
        0f, height - foregroundHeight,
        width, height,
        paint
    )
    
    // 中景层
    val midHeight = height * 0.25f
    val midY = height * 0.35f
    val midPaint = Paint(paint).apply {
        alpha = (0.1f * 255).toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRect(
        0f, midY, width, midY + midHeight,
        midPaint
    )
    canvas.drawRect(
        0f, midY, width, midY + midHeight,
        paint
    )
    
    // 背景层
    val backHeight = height * 0.25f
    val backY = height * 0.1f
    val backPaint = Paint(paint).apply {
        alpha = (0.05f * 255).toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRect(
        0f, backY, width, backY + backHeight,
        backPaint
    )
    canvas.drawRect(
        0f, backY, width, backY + backHeight,
        paint
    )
}
