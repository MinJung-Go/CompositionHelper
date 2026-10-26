package com.example.compositionhelper.color

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ColorPlanTest {
    @Test fun matchesPythonReferenceForBothPhotosAndOverlappingHslWithBalance() {
        val text = javaClass.getResource("/color/python_reference.json")!!.readText()
        for (fixture in JsonParser.parseString(text).asJsonArray) {
            val f = fixture.asJsonObject
            val plan = ColorPlanCodec.decode(f.get("plan").toString())
            val transform = ColorPlanEngine.prepare(plan, 1f)
            f.getAsJsonArray("input").forEachIndexed { index, rgb ->
                val c = rgb.asJsonArray.map { it.asInt }
                val pixel = (0x7f shl 24) or (c[0] shl 16) or (c[1] shl 8) or c[2]
                val result = transform(pixel)
                assertEquals(0x7f, result ushr 24)
                f.getAsJsonArray("expected")[index].asJsonArray.forEachIndexed { channel, expected ->
                    val actual = result shr (16-channel*8) and 255
                    assertTrue("pixel $index channel $channel: expected $expected got $actual", abs(expected.asInt-actual) <= 1)
                }
            }
        }
    }
    @Test fun zeroStrengthAndIdentityAreExact() {
        val plan = ColorPlan(basic = ColorAdjustment(exposure = .3f), hsl = listOf(HslBand("blue", saturationPercent = -20f)))
        for (pixel in listOf(0, -1, 0x7f123456, 0xffabcdef.toInt())) {
            assertEquals(pixel, ColorPlanEngine.prepare(plan, 0f)(pixel))
            assertEquals(pixel, ColorPlanEngine.prepare(ColorPlan(), 1f)(pixel))
        }
    }
    @Test fun hslLeavesNeutralAndUnrelatedColorsAlone() {
        val t = ColorPlanEngine.prepare(ColorPlan(hsl = listOf(HslBand("blue", saturationPercent = -25f))), 1f)
        for (v in 0..255) {
            val grey = (255 shl 24) or (v shl 16) or (v shl 8) or v
            assertEquals(grey, t(grey))
        }
        assertEquals(0xffff8000.toInt(), t(0xffff8000.toInt()))
        assertNotEquals(0xff2040d0.toInt(), t(0xff2040d0.toInt()))
    }
    @Test fun redBandWrapsAroundHueZero() {
        val t = ColorPlanEngine.prepare(ColorPlan(hsl = listOf(HslBand("red", lightnessPoints = -7f))), 1f)
        for (p in listOf(0xffff0110.toInt(),0xffff1001.toInt())) assertTrue((t(p) shr 16 and 255) < 255)
    }
    @Test fun strengthBlendsFinalEffectIncludingCurveAndHsl() {
        val plan = ColorPlan(basic = ColorAdjustment(shadows = .1f), hsl = listOf(HslBand("blue", lightnessPoints = 5f)))
        val pixel = 0x7f305080
        val full = ColorPlanEngine.prepare(plan, 1f)(pixel)
        val half = ColorPlanEngine.prepare(plan, .5f)(pixel)
        for (shift in listOf(0,8,16)) assertEquals(((pixel shr shift and 255)+(full shr shift and 255))/2f,
            (half shr shift and 255).toFloat(), 1f)
    }
    @Test fun recipeRoundTripPreservesAllAdjustments() {
        val plan = ColorPlan(basic = ColorAdjustment(temperature = .03f), hsl = listOf(HslBand("aqua", hueShift = 3f)),
            balance = ColorBalance(midtones = listOf(2f,-1f,1f)), explanation = "自然", scene = "湖水")
        assertEquals(plan, ColorPlanCodec.decode(ColorPlanCodec.encode(plan)))
    }
    @Test fun acceptsKnownNumericHueCenters() {
        val plan = ColorPlanCodec.decode("""{"basic":{},"hsl":[{"band":225}]}""")
        assertEquals("blue", plan.hsl.single().band)
    }
    @Test fun rejectsInvalidPlansInsteadOfSilentlyApplyingDefaults() {
        val invalid = listOf("{}", """{"basic":{"exposure":200}}""", """{"basic":{"exposure":"NaN"}}""",
            """{"basic":{},"curve_y":[0,32,64]}""", """{"basic":{},"curve_y":[0,32,80,74,128,160,192,224,255]}""",
            """{"basic":{},"hsl":[{"band":"water"}]}""", """{"basic":{},"hsl":[{"band":"blue"},{"band":225}]}""",
            """{"basic":{},"color_balance":{"shadows":[1,2]}}""", """{"basic":{},"color_balance":{"midtones":[9,0,0]}}""")
        invalid.forEach { json -> assertThrows(Exception::class.java) { ColorPlanCodec.decode(json) } }
    }
}
