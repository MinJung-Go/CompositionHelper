package com.example.compositionhelper.camera

import com.example.compositionhelper.model.PointF
import kotlin.math.abs
import kotlin.math.hypot

internal data class FrameSegment(val start: PointF, val end: PointF)

/** Looks for a coherent slanted boundary that crosses the middle of the visible frame. */
internal object DiagonalEvidence {
    private data class Edge(val start: PointF, val end: PointF, val ux: Float, val uy: Float, val length: Float)

    fun score(segments: List<FrameSegment>, luma: (Float, Float) -> Int): Float {
        val edges = segments.mapNotNull { segment ->
            val (start, end) = if (segment.start.x <= segment.end.x) segment else
                FrameSegment(segment.end, segment.start)
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = hypot(dx, dy)
            if (length < 0.09f || abs(dy) < 0.41f * dx || dx < 0.41f * abs(dy)) null
            else Edge(start, end, dx / length, dy / length, length)
        }
        var best = 0f
        for (seed in edges.sortedByDescending { it.length }.take(24)) {
            val nx = -seed.uy
            val ny = seed.ux
            fun offset(point: PointF) = (point.x - seed.start.x) * nx + (point.y - seed.start.y) * ny
            fun projection(point: PointF) =
                (point.x - seed.start.x) * seed.ux + (point.y - seed.start.y) * seed.uy

            if (abs(offset(PointF(0.5f, 0.5f))) > 0.15f) continue
            val parallelOffsets = edges.filter {
                it.length >= 0.32f && it.ux * seed.ux + it.uy * seed.uy >= 0.9903f
            }.map { (offset(it.start) + offset(it.end)) / 2 }.sorted()
            var groups = 0
            var previousOffset = Float.NEGATIVE_INFINITY
            for (value in parallelOffsets) {
                if (value - previousOffset >= 0.027f) {
                    groups++
                    previousOffset = value
                }
            }
            if (groups >= 4) continue
            val intervals = edges.mapNotNull { edge ->
                if (edge.ux * seed.ux + edge.uy * seed.uy < 0.978f ||
                    abs(offset(edge.start)) > 0.028f || abs(offset(edge.end)) > 0.028f
                ) null else projection(edge.start) to projection(edge.end)
            }.sortedBy { it.first }

            var first = 0f
            var last = 0f
            var covered = 0f
            for ((start, end) in intervals) {
                if (covered == 0f || start > last + 0.13f) {
                    best = maxOf(best, boundaryScore(seed, first, last, covered, luma))
                    first = start
                    last = start
                    covered = 0f
                }
                covered += (end - maxOf(start, last)).coerceAtLeast(0f)
                last = maxOf(last, end)
            }
            best = maxOf(best, boundaryScore(seed, first, last, covered, luma))
        }
        return best
    }

    private fun boundaryScore(
        edge: Edge, first: Float, last: Float, covered: Float, luma: (Float, Float) -> Int
    ): Float {
        val extent = last - first
        if (extent < 0.53f || covered < 0.71f * extent) return 0f
        val x1 = edge.start.x + edge.ux * first
        val x2 = edge.start.x + edge.ux * last
        val y1 = edge.start.y + edge.uy * first
        val y2 = edge.start.y + edge.uy * last
        // A perspective line ending at a central vanishing point lacks support on both sides.
        if (x1 >= 0.39f || x2 <= 0.61f || minOf(y1, y2) >= 0.39f || maxOf(y1, y2) <= 0.61f) return 0f

        val dx = -0.035f * edge.uy
        val dy = 0.035f * edge.ux
        var valid = 0
        var brighter = 0
        var darker = 0
        for (index in 0 until 32) {
            val distance = first + (0.08f + 0.84f * index / 31) * extent
            val x = edge.start.x + edge.ux * distance
            val y = edge.start.y + edge.uy * distance
            if (x - abs(dx) < 0f || x + abs(dx) > 1f || y - abs(dy) < 0f || y + abs(dy) > 1f) continue
            val difference = luma(x + dx, y + dy) - luma(x - dx, y - dy)
            valid++
            if (difference >= 14) brighter++
            if (difference <= -14) darker++
        }
        if (valid < 8 || maxOf(brighter, darker) * 2 < valid) return 0f
        return 0.70f + 0.20f * (extent / hypot(1f, 1f)).coerceAtMost(1f)
    }
}
