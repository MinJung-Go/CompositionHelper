package com.example.compositionhelper.color

import org.junit.Assert.*
import org.junit.Test
import com.google.gson.JsonParser

class ColorAdjustmentEngineTest {
    @Test fun offlineSuggestionsMatchSharedIosReference() {
        val text = javaClass.getResource("/color/offline_reference.json")!!.readText()
        for (fixture in JsonParser.parseString(text).asJsonArray) {
            val item = fixture.asJsonObject
            val pixels = item.getAsJsonArray("pixels").map { value ->
                val rgb = value.asJsonArray.map { it.asInt }
                (255 shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
            }.toIntArray()
            val a = ColorAdjustmentEngine.analyze(pixels).adjustment
            val actual = mapOf("exposure" to a.exposure, "contrast" to a.contrast,
                "shadows" to a.shadows, "highlights" to a.highlights,
                "temperature" to a.temperature, "tint" to a.tint, "saturation" to a.saturation)
            for ((key, expected) in item.getAsJsonObject("expected").entrySet()) {
                assertEquals(item.get("name").asString + "/" + key, expected.asDouble, actual.getValue(key).toDouble(), 0.000001)
            }
        }
    }
    @Test fun zeroStrengthAndResetPreserveEveryChannelIncludingAlpha() {
        val pixels = intArrayOf(0, -1, 0x7f123456, 0xff123456.toInt())
        val adjustment = ColorAdjustment(exposure = .5f, shadows = .2f, temperature = .1f)
        for (pixel in pixels) {
            assertEquals(pixel, ColorAdjustmentEngine.transform(pixel, adjustment, 0f))
            assertEquals(pixel, ColorAdjustmentEngine.transform(pixel, ColorAdjustment(), 1f))
        }
    }

    @Test fun darkAndBrightImagesReceiveOppositeExposureCorrections() {
        val dark = ColorAdjustmentEngine.analyze(IntArray(100) { 0xff202020.toInt() }).adjustment
        val bright = ColorAdjustmentEngine.analyze(IntArray(100) { 0xffeeeeee.toInt() }).adjustment
        assertTrue(dark.exposure > 0f)
        assertTrue(dark.shadows > 0f)
        assertTrue(bright.exposure < 0f)
        assertTrue(bright.highlights < 0f)
    }

    @Test fun saturatedSunsetIsNotTreatedAsNeutralWhiteBalance() {
        val result = ColorAdjustmentEngine.analyze(IntArray(100) { 0xffff7020.toInt() })
        assertEquals(0f, result.adjustment.temperature, 0f)
        assertEquals(0f, result.adjustment.tint, 0f)
    }

    @Test fun correctionPreservesAlphaAndStrengthInterpolates() {
        val pixel = 0x7f404040
        val correction = ColorAdjustment(exposure = .5f)
        val full = ColorAdjustmentEngine.transform(pixel, correction, 1f)
        val half = ColorAdjustmentEngine.transform(pixel, correction, .5f)
        assertEquals(0x7f, full ushr 24)
        assertTrue((half and 255) > (pixel and 255))
        assertTrue((half and 255) < (full and 255))
        assertEquals(full shr 16 and 255, full and 255)
    }

    @Test fun extremeAdjustmentsClampWithoutWrappingChannels() {
        val result = ColorAdjustmentEngine.transform(0xffffffff.toInt(), ColorAdjustment(exposure = 1f), 1f)
        assertEquals(0xffffffff.toInt(), result)
        val black = ColorAdjustmentEngine.transform(0xff000000.toInt(), ColorAdjustment(shadows = -.4f), 1f)
        assertEquals(0xff000000.toInt(), black)
    }

    @Test fun lowContrastMidtonesGetGentleContrastWithoutTintingNeutralGrey() {
        val result = ColorAdjustmentEngine.analyze(IntArray(100) { 0xff777777.toInt() })
        assertTrue(result.adjustment.contrast > 0f)
        assertEquals(0f, result.adjustment.temperature, 0f)
        assertEquals(0f, result.adjustment.tint, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyImageIsRejected() { ColorAdjustmentEngine.analyze(intArrayOf()) }
}
