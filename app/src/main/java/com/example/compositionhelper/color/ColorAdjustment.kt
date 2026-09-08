package com.example.compositionhelper.color

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Parameters are applied to the source image once; strength zero is an exact identity. */
data class ColorAdjustment(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 0f
)

data class ColorSuggestion(val adjustment: ColorAdjustment, val explanation: String)

object ColorAdjustmentEngine {
    fun analyze(pixels: IntArray): ColorSuggestion {
        require(pixels.isNotEmpty())
        var luminance = 0.0
        var luminanceSquared = 0.0
        var dark = 0
        var bright = 0
        var neutralCount = 0
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        var chroma = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16 and 255) / 255.0
            val g = (pixel shr 8 and 255) / 255.0
            val b = (pixel and 255) / 255.0
            val y = .2126 * r + .7152 * g + .0722 * b
            luminance += y
            luminanceSquared += y * y
            if (y < .2) dark++
            if (y > .85) bright++
            val spread = maxOf(r, g, b) - minOf(r, g, b)
            chroma += spread
            // Only near-neutral midtones inform a deliberately conservative white balance.
            if (y in .2.. .8 && spread < .12) {
                red += r; green += g; blue += b; neutralCount++
            }
        }
        val mean = luminance / pixels.size
        val exposure = ((.46 - mean) * 1.4).toFloat().coerceIn(-.45f, .45f)
        val shadows = if (dark.toDouble() / pixels.size > .2) .18f else 0f
        val highlights = if (bright.toDouble() / pixels.size > .12) -.16f else 0f
        val temperature = if (neutralCount > pixels.size * .15) {
            ((blue - red) / neutralCount * .5).toFloat().coerceIn(-.04f, .04f)
        } else 0f
        val tint = if (neutralCount > pixels.size * .15) {
            ((green - (red + blue) / 2) / neutralCount * .5).toFloat().coerceIn(-.03f, .03f)
        } else 0f
        val variance = (luminanceSquared / pixels.size - mean * mean).coerceAtLeast(0.0)
        val contrast = if (variance < .015 && mean in .25.. .7) .06f else 0f
        val saturation = if (chroma / pixels.size > .4) -.08f else 0f
        val notes = mutableListOf<String>()
        if (exposure > .08) notes += "适度提亮画面"
        if (exposure < -.08) notes += "适度降低曝光"
        if (shadows > 0) notes += "提亮阴影"
        if (highlights < 0) notes += "压低高光亮度"
        if (abs(temperature) > .005 || abs(tint) > .005) notes += "轻微校正中性色偏色"
        if (contrast > 0) notes += "适度增强对比度"
        if (saturation < 0) notes += "减轻过度饱和"
        return ColorSuggestion(
            ColorAdjustment(exposure = exposure, contrast = contrast, shadows = shadows, highlights = highlights,
                temperature = temperature, tint = tint, saturation = saturation),
            notes.joinToString("，").ifEmpty { "画面较均衡，保留原有色调" }
        )
    }

    fun transform(pixel: Int, adjustment: ColorAdjustment, strength: Float): Int =
        prepare(adjustment, strength)(pixel)

    fun prepare(adjustment: ColorAdjustment, strength: Float): (Int) -> Int {
        val amount = strength.coerceIn(0f, 1f)
        if (amount == 0f || adjustment == ColorAdjustment()) return { it }
        val gain = 2f.pow(adjustment.exposure)
        return { pixel ->
            val r = (pixel shr 16 and 255) / 255f
            val g = (pixel shr 8 and 255) / 255f
            val b = (pixel and 255) / 255f
            val y = .2126f * r + .7152f * g + .0722f * b
            val tonal = adjustment.shadows * (1 - y) * (1 - y) + adjustment.highlights * y * y
            fun channel(value: Float, balance: Float): Float =
                ((value * gain + tonal + balance - .5f) * (1 + adjustment.contrast) + .5f)
            val rr = channel(r, adjustment.temperature + adjustment.tint * .5f)
            val gg = channel(g, -adjustment.tint)
            val bb = channel(b, -adjustment.temperature + adjustment.tint * .5f)
            val grey = .2126f * rr + .7152f * gg + .0722f * bb
            fun encode(original: Float, corrected: Float): Int {
                val saturated = grey + (corrected - grey) * (1 + adjustment.saturation)
                return ((original + (saturated - original) * amount).coerceIn(0f, 1f) * 255).roundToInt()
            }
            (pixel and -0x1000000) or (encode(r, rr) shl 16) or (encode(g, gg) shl 8) or encode(b, bb)
        }
    }
}
