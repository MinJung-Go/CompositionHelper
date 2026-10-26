package com.example.compositionhelper.overlay

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF as AndroidRectF
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path as ComposePath
import com.example.compositionhelper.model.CompositionType

// ============================================================
// 绘制路径命令
// ============================================================

sealed class PathCommand {
    data class MoveTo(val x: Float, val y: Float) : PathCommand()
    data class LineTo(val x: Float, val y: Float) : PathCommand()
    data class CubicTo(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val x3: Float, val y3: Float
    ) : PathCommand()
    data class ArcTo(
        val left: Float, val top: Float,
        val right: Float, val bottom: Float,
        val startAngle: Float, val sweepAngle: Float
    ) : PathCommand()
    object Close : PathCommand()
}

// ============================================================
// 渲染器抽象接口
// ============================================================

interface CompositionRenderer {
    fun drawLine(startX: Float, startY: Float, endX: Float, endY: Float)
    fun drawCircle(cx: Float, cy: Float, radius: Float)
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float)
    fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float)
    fun drawPath(commands: List<PathCommand>)
    fun drawFilledRect(left: Float, top: Float, right: Float, bottom: Float, alpha: Float)
    fun setDashEffect(intervals: FloatArray)
    fun clearDashEffect()
}

// ============================================================
// Android Canvas 渲染器（用于 Bitmap 叠加）
// ============================================================

class CanvasRenderer(
    private val canvas: Canvas,
    private val paint: Paint
) : CompositionRenderer {

    override fun drawLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, paint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float) {
        canvas.drawArc(AndroidRectF(left, top, right, bottom), startAngle, sweepAngle, false, paint)
    }

    override fun drawPath(commands: List<PathCommand>) {
        val path = AndroidPath()
        for (cmd in commands) {
            when (cmd) {
                is PathCommand.MoveTo -> path.moveTo(cmd.x, cmd.y)
                is PathCommand.LineTo -> path.lineTo(cmd.x, cmd.y)
                is PathCommand.CubicTo -> path.cubicTo(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x3, cmd.y3)
                is PathCommand.ArcTo -> path.addArc(AndroidRectF(cmd.left, cmd.top, cmd.right, cmd.bottom), cmd.startAngle, cmd.sweepAngle)
                is PathCommand.Close -> path.close()
            }
        }
        canvas.drawPath(path, paint)
    }

    override fun drawFilledRect(left: Float, top: Float, right: Float, bottom: Float, alpha: Float) {
        val fillPaint = Paint(paint).apply {
            this.alpha = (alpha * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(left, top, right, bottom, fillPaint)
    }

    override fun setDashEffect(intervals: FloatArray) {
        paint.pathEffect = DashPathEffect(intervals, 0f)
    }

    override fun clearDashEffect() {
        paint.pathEffect = null
    }
}

// ============================================================
// Compose DrawScope 渲染器（用于实时相机叠加）
// ============================================================

class DrawScopeRenderer(
    private val drawScope: DrawScope,
    private val color: Color,
    private val alpha: Float,
    private val strokeWidth: Float
) : CompositionRenderer {

    private var currentPathEffect: PathEffect? = null

    override fun drawLine(startX: Float, startY: Float, endX: Float, endY: Float) {
        drawScope.drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            alpha = alpha,
            pathEffect = currentPathEffect,
            cap = StrokeCap.Round
        )
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float) {
        drawScope.drawCircle(
            color = color,
            radius = radius,
            center = Offset(cx, cy),
            alpha = alpha,
            style = Stroke(width = strokeWidth, pathEffect = currentPathEffect)
        )
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            alpha = alpha,
            style = Stroke(width = strokeWidth, pathEffect = currentPathEffect)
        )
    }

    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float) {
        drawScope.drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            alpha = alpha,
            style = Stroke(width = strokeWidth, pathEffect = currentPathEffect)
        )
    }

    override fun drawPath(commands: List<PathCommand>) {
        val path = ComposePath()
        for (cmd in commands) {
            when (cmd) {
                is PathCommand.MoveTo -> path.moveTo(cmd.x, cmd.y)
                is PathCommand.LineTo -> path.lineTo(cmd.x, cmd.y)
                is PathCommand.CubicTo -> path.cubicTo(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x3, cmd.y3)
                is PathCommand.ArcTo -> path.addArc(
                    androidx.compose.ui.geometry.Rect(cmd.left, cmd.top, cmd.right, cmd.bottom),
                    cmd.startAngle,
                    cmd.sweepAngle
                )
                is PathCommand.Close -> path.close()
            }
        }
        drawScope.drawPath(
            path = path,
            color = color,
            alpha = alpha,
            style = Stroke(width = strokeWidth, pathEffect = currentPathEffect)
        )
    }

    override fun drawFilledRect(left: Float, top: Float, right: Float, bottom: Float, alpha: Float) {
        drawScope.drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            alpha = alpha
        )
    }

    override fun setDashEffect(intervals: FloatArray) {
        currentPathEffect = PathEffect.dashPathEffect(intervals, 0f)
    }

    override fun clearDashEffect() {
        currentPathEffect = null
    }
}

// ============================================================
// 构图绘制入口
// ============================================================

fun drawComposition(renderer: CompositionRenderer, type: CompositionType, width: Float, height: Float) {
    when (type) {
        CompositionType.RULE_OF_THIRDS -> drawRuleOfThirds(renderer, width, height)
        CompositionType.CENTER -> drawCenterComposition(renderer, width, height)
        CompositionType.DIAGONAL -> drawDiagonalLines(renderer, width, height)
        CompositionType.FRAME -> drawFrameComposition(renderer, width, height)
        CompositionType.LEADING_LINES -> drawLeadingLines(renderer, width, height)
        CompositionType.S_CURVE -> drawSCurve(renderer, width, height)
        CompositionType.GOLDEN_SPIRAL -> drawGoldenSpiral(renderer, width, height)
        CompositionType.GOLDEN_TRIANGLE -> drawGoldenTriangle(renderer, width, height)
        CompositionType.SYMMETRY -> drawSymmetry(renderer, width, height)
        CompositionType.NEGATIVE_SPACE -> drawNegativeSpace(renderer, width, height)
        CompositionType.PATTERN_REPEAT -> drawPatternRepeat(renderer, width, height)
        CompositionType.TUNNEL -> drawTunnel(renderer, width, height)
        CompositionType.SPLIT -> drawSplitComposition(renderer, width, height)
        CompositionType.PERSPECTIVE -> drawPerspective(renderer, width, height)
        CompositionType.INVISIBLE_LINE -> drawInvisibleLine(renderer, width, height)
        CompositionType.FILL_FRAME -> drawFillFrame(renderer, width, height)
        CompositionType.LOW_ANGLE -> drawAngleView(renderer, width, height, isLowAngle = true)
        CompositionType.HIGH_ANGLE -> drawAngleView(renderer, width, height, isLowAngle = false)
        CompositionType.DEPTH_LAYER -> drawDepthLayer(renderer, width, height)
    }
}

// ============================================================
// 19 种构图绘制函数
// ============================================================

// 三分法
private fun drawRuleOfThirds(r: CompositionRenderer, w: Float, h: Float) {
    r.drawLine(w / 3, 0f, w / 3, h)
    r.drawLine(w * 2 / 3, 0f, w * 2 / 3, h)
    r.drawLine(0f, h / 3, w, h / 3)
    r.drawLine(0f, h * 2 / 3, w, h * 2 / 3)
}

// 中心构图
private fun drawCenterComposition(r: CompositionRenderer, w: Float, h: Float) {
    r.drawLine(w / 2, 0f, w / 2, h)
    r.drawLine(0f, h / 2, w, h / 2)
    r.drawCircle(w / 2, h / 2, 50f)
}

// 对角线
private fun drawDiagonalLines(r: CompositionRenderer, w: Float, h: Float) {
    r.drawLine(0f, 0f, w, h)
    r.drawLine(w, 0f, 0f, h)
}

// 框架构图
private fun drawFrameComposition(r: CompositionRenderer, w: Float, h: Float) {
    val frameWidth = w * 0.7f
    val frameHeight = h * 0.7f
    val left = (w - frameWidth) / 2
    val top = (h - frameHeight) / 2
    r.drawRect(left, top, left + frameWidth, top + frameHeight)
}

// 引导线
private fun drawLeadingLines(r: CompositionRenderer, w: Float, h: Float) {
    for (i in 0 until 5) {
        val startX = w * i / 6
        r.drawLine(startX, h, w, h * i / 6)
    }
}

// S形曲线
private fun drawSCurve(r: CompositionRenderer, w: Float, h: Float) {
    r.drawPath(listOf(
        PathCommand.MoveTo(0f, h),
        PathCommand.CubicTo(w * 0.3f, h, w * 0.7f, 0f, w, 0f)
    ))
}

// 黄金螺旋
private fun drawGoldenSpiral(r: CompositionRenderer, w: Float, h: Float) {
    val phi = 1.618f
    val minSide = w.coerceAtMost(h)
    var currentSize = minSide
    var x = 0f
    var y = 0f
    var direction = 0

    for (i in 0 until 8) {
        when (direction) {
            0 -> x += currentSize
            1 -> y += currentSize
            2 -> x -= currentSize
            3 -> y -= currentSize
        }
        r.drawArc(
            x - currentSize, y - currentSize,
            x + currentSize, y + currentSize,
            (direction * 90).toFloat(), 90f
        )
        currentSize /= phi
        direction = (direction + 1) % 4
    }
}

// 黄金三角
private fun drawGoldenTriangle(r: CompositionRenderer, w: Float, h: Float) {
    r.drawPath(listOf(
        PathCommand.MoveTo(0f, 0f),
        PathCommand.LineTo(w, h),
        PathCommand.LineTo(0f, h),
        PathCommand.Close
    ))
    val splitX = w / 1.618f
    r.drawPath(listOf(
        PathCommand.MoveTo(splitX, h),
        PathCommand.LineTo(splitX, h - (h / 1.618f)),
        PathCommand.LineTo(0f, h - (h / 1.618f)),
        PathCommand.Close
    ))
}

// 对称构图
private fun drawSymmetry(r: CompositionRenderer, w: Float, h: Float) {
    r.drawLine(w / 2, 0f, w / 2, h)
    r.drawLine(0f, h / 2, w, h / 2)
    val corners = listOf(
        Pair(w * 0.25f, h * 0.25f),
        Pair(w * 0.75f, h * 0.25f),
        Pair(w * 0.25f, h * 0.75f),
        Pair(w * 0.75f, h * 0.75f)
    )
    corners.forEach { (cx, cy) -> r.drawCircle(cx, cy, 10f) }
}

// 负空间
private fun drawNegativeSpace(r: CompositionRenderer, w: Float, h: Float) {
    val subjectWidth = w * 0.35f
    val subjectHeight = h * 0.4f
    r.setDashEffect(floatArrayOf(20f, 10f))
    r.drawRect(w * 0.1f, h * 0.15f, w * 0.1f + subjectWidth, h * 0.15f + subjectHeight)
    r.drawRect(w * 0.55f, h * 0.45f, w * 0.55f + subjectWidth, h * 0.45f + subjectHeight)
    r.clearDashEffect()
}

// 模式重复
private fun drawPatternRepeat(r: CompositionRenderer, w: Float, h: Float) {
    val cols = 3
    val rows = 3
    val cellWidth = w / cols
    val cellHeight = h / rows

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val x = col * cellWidth + cellWidth * 0.2f
            val y = row * cellHeight + cellHeight * 0.2f
            val rw = cellWidth * 0.6f
            val rh = cellHeight * 0.6f
            r.drawRect(x, y, x + rw, y + rh)
            val cx = x + rw / 2
            val cy = y + rh / 2
            r.drawLine(cx - 3, cy, cx + 3, cy)
            r.drawLine(cx, cy - 3, cx, cy + 3)
        }
    }
}

// 隧道式
private fun drawTunnel(r: CompositionRenderer, w: Float, h: Float) {
    val centerX = w / 2
    val centerY = h / 2

    for (i in 0 until 5) {
        val scale = 1.0f - i * 0.18f
        val currentWidth = w * scale
        val currentHeight = h * scale
        r.drawRect(
            centerX - currentWidth / 2, centerY - currentHeight / 2,
            centerX + currentWidth / 2, centerY + currentHeight / 2
        )
    }
    r.drawCircle(centerX, centerY, 8f)
}

// 分割构图
private fun drawSplitComposition(r: CompositionRenderer, w: Float, h: Float) {
    r.drawLine(0f, h / 2, w, h / 2)
    r.drawLine(w / 2, 0f, w / 2, h)
    r.setDashEffect(floatArrayOf(5f, 5f))
    r.drawLine(0f, 0f, w, h)
    r.drawLine(w, 0f, 0f, h)
    r.clearDashEffect()
}

// 透视焦点
private fun drawPerspective(r: CompositionRenderer, w: Float, h: Float) {
    val centerX = w / 2
    val centerY = h * 0.6f

    val points = listOf(
        Pair(0f, 0f), Pair(w * 0.25f, 0f), Pair(w * 0.5f, 0f),
        Pair(w * 0.75f, 0f), Pair(w, 0f),
        Pair(0f, h * 0.3f), Pair(w, h * 0.3f)
    )
    points.forEach { (x, y) -> r.drawLine(centerX, centerY, x, y) }
    r.drawCircle(centerX, centerY, 6f)
}

// 隐形线
private fun drawInvisibleLine(r: CompositionRenderer, w: Float, h: Float) {
    r.setDashEffect(floatArrayOf(15f, 10f))
    r.drawPath(listOf(
        PathCommand.MoveTo(w * 0.2f, h * 0.8f),
        PathCommand.CubicTo(w * 0.2f, h * 0.3f, w * 0.8f, h * 0.7f, w * 0.8f, h * 0.2f)
    ))
    r.clearDashEffect()
    r.drawCircle(w * 0.8f, h * 0.2f, 20f)
}

// 充满画面
private fun drawFillFrame(r: CompositionRenderer, w: Float, h: Float) {
    val margin = 10f
    r.drawRect(margin, margin, w - margin, h - margin)
    r.setDashEffect(floatArrayOf(8f, 4f))
    val fillRatio = 0.85f
    r.drawRect(
        (w - w * fillRatio) / 2, (h - h * fillRatio) / 2,
        (w + w * fillRatio) / 2, (h + h * fillRatio) / 2
    )
    r.clearDashEffect()
}

// 角度视图
private fun drawAngleView(r: CompositionRenderer, w: Float, h: Float, isLowAngle: Boolean) {
    val centerX = w / 2
    if (isLowAngle) {
        r.drawLine(centerX, h, centerX, h * 0.2f)
        r.drawPath(listOf(
            PathCommand.MoveTo(centerX, h * 0.2f),
            PathCommand.ArcTo(
                centerX, h * 0.2f, w * 0.3f, 0f,
                220f, 100f
            )
        ))
    } else {
        r.drawLine(centerX, 0f, centerX, h * 0.8f)
        r.drawPath(listOf(
            PathCommand.MoveTo(centerX, h * 0.8f),
            PathCommand.ArcTo(
                centerX, h * 0.8f, w * 0.3f, 0f,
                40f, 100f
            )
        ))
    }
}

// 深度层次
private fun drawDepthLayer(r: CompositionRenderer, w: Float, h: Float) {
    // 前景层
    val foregroundHeight = h * 0.25f
    r.drawFilledRect(0f, h - foregroundHeight, w, h, 0.15f)
    r.drawRect(0f, h - foregroundHeight, w, h)

    // 中景层
    val midHeight = h * 0.25f
    val midY = h * 0.35f
    r.drawFilledRect(0f, midY, w, midY + midHeight, 0.1f)
    r.drawRect(0f, midY, w, midY + midHeight)

    // 背景层
    val backHeight = h * 0.25f
    val backY = h * 0.1f
    r.drawFilledRect(0f, backY, w, backY + backHeight, 0.05f)
    r.drawRect(0f, backY, w, backY + backHeight)
}
