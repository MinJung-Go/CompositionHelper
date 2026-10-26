package com.example.compositionhelper.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.compositionhelper.model.CompositionType
import com.example.compositionhelper.model.DetectedSubject
import com.example.compositionhelper.model.PointF
import kotlin.math.sqrt

/**
 * 实时相机构图叠加层
 * 使用 Compose Canvas 直接绘制在 CameraX 预览上方
 */
@Composable
fun CameraCompositionOverlay(
    compositionType: CompositionType,
    lineOpacity: Float,
    lineColor: Color,
    detectedSubjects: List<DetectedSubject> = emptyList(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val renderer = DrawScopeRenderer(
            drawScope = this,
            color = lineColor,
            alpha = lineOpacity,
            strokeWidth = 2.dp.toPx()
        )

        // Layer 1: 绘制构图引导线
        drawComposition(renderer, compositionType, size.width, size.height)

        // Layer 3: 绘制检测到的主体边界框
        detectedSubjects.forEach { subject ->
            val box = subject.boundingBox
            val left = box.left * size.width
            val top = box.top * size.height
            val w = box.width() * size.width
            val h = box.height() * size.height

            drawRoundRect(
                color = Color.Cyan.copy(alpha = 0.7f),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Layer 3: 绘制对齐指示器
        if (detectedSubjects.isNotEmpty()) {
            val keyPoints = getCompositionKeyPoints(compositionType)
            val alignThreshold = 40.dp.toPx()
            val nearThreshold = 100.dp.toPx()

            keyPoints.forEach { point ->
                val px = point.x * size.width
                val py = point.y * size.height

                val nearestSubject = detectedSubjects.minByOrNull { subject ->
                    val dx = subject.boundingBox.centerX() * size.width - px
                    val dy = subject.boundingBox.centerY() * size.height - py
                    sqrt(dx * dx + dy * dy)
                }

                nearestSubject?.let { subject ->
                    val dx = subject.boundingBox.centerX() * size.width - px
                    val dy = subject.boundingBox.centerY() * size.height - py
                    val distance = sqrt(dx * dx + dy * dy)

                    when {
                        distance < alignThreshold -> {
                            // 已对齐 — 绿色发光
                            drawCircle(
                                color = Color.Green.copy(alpha = 0.8f),
                                radius = 12.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                        distance < nearThreshold -> {
                            // 接近 — 黄色指示
                            drawCircle(
                                color = Color.Yellow.copy(alpha = 0.5f),
                                radius = 8.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 返回每种构图类型的关键对齐点（归一化坐标 0~1）
 */
fun getCompositionKeyPoints(type: CompositionType): List<PointF> {
    return when (type) {
        CompositionType.RULE_OF_THIRDS -> listOf(
            PointF(1f / 3f, 1f / 3f), PointF(2f / 3f, 1f / 3f),
            PointF(1f / 3f, 2f / 3f), PointF(2f / 3f, 2f / 3f)
        )
        CompositionType.CENTER -> listOf(PointF(0.5f, 0.5f))
        CompositionType.GOLDEN_SPIRAL -> listOf(PointF(0.382f, 0.618f))
        CompositionType.GOLDEN_TRIANGLE -> listOf(
            PointF(0.382f, 0.618f), PointF(0.618f, 0.382f)
        )
        CompositionType.DIAGONAL -> listOf(
            PointF(0.25f, 0.25f), PointF(0.5f, 0.5f), PointF(0.75f, 0.75f)
        )
        CompositionType.SYMMETRY -> listOf(
            PointF(0.25f, 0.25f), PointF(0.75f, 0.25f),
            PointF(0.25f, 0.75f), PointF(0.75f, 0.75f)
        )
        CompositionType.PERSPECTIVE -> listOf(PointF(0.5f, 0.6f))
        CompositionType.TUNNEL -> listOf(PointF(0.5f, 0.5f))
        CompositionType.FRAME -> listOf(
            PointF(0.5f, 0.5f),
            PointF(0.35f, 0.35f), PointF(0.65f, 0.65f)
        )
        CompositionType.FILL_FRAME -> listOf(PointF(0.5f, 0.5f))
        else -> emptyList()
    }
}
