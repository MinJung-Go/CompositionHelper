package com.example.compositionhelper.color

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class RgbCorrectionTest {
    private val grey = 0x7f808080
    private fun render(c: RgbCorrection, p: Int = grey, strength: Float = 1f) =
        ColorPlanEngine.prepare(ColorPlan(rgb = c), strength)(p)
    private fun channels(p: Int) = listOf(p shr 16 and 255, p shr 8 and 255, p and 255)

    @Test fun addingRedAndSubtractingCyanHaveDifferentBrightness() {
        val direct = channels(render(RgbCorrection(global = listOf(.04f,0f,0f))))
        val complement = channels(render(RgbCorrection(mode="complement",global=listOf(.04f,0f,0f))))
        assertEquals(listOf(138,128,128), direct)
        assertEquals(listOf(128,118,118), complement)
    }
    @Test fun allThreeAxesAndNegativeDirectionsAffectCorrectChannels() {
        for (axis in 0..2) for (sign in listOf(-1f,1f)) {
            val c = List(3) { if (it == axis) sign*.04f else 0f }
            val direct = channels(render(RgbCorrection(global=c)))
            val complement = channels(render(RgbCorrection(mode="complement",global=c)))
            for (channel in 0..2) {
                assertEquals(128+(if(channel==axis) (sign*10).toInt() else 0),direct[channel])
                assertEquals(128+(if(channel!=axis) (-sign*10).toInt() else 0),complement[channel])
            }
        }
    }
    @Test fun tonalRangesFavorTheirIntendedBrightness() {
        val dark=0xff202020.toInt(); val light=0xffdddddd.toInt()
        val shadow=RgbCorrection(shadows=listOf(.08f,0f,0f))
        val high=RgbCorrection(highlights=listOf(.08f,0f,0f))
        fun change(c: RgbCorrection,p: Int)=(render(c,p) shr 16 and 255)-(p shr 16 and 255)
        assertTrue(change(shadow,dark)>change(shadow,light))
        assertTrue(change(high,light)>change(high,dark))
        val mid=RgbCorrection(midtones=listOf(.08f,0f,0f))
        assertTrue(change(mid,grey)>change(mid,dark))
        assertTrue(change(mid,grey)>change(mid,light))
    }
    @Test fun zeroStrengthResetAndAlphaRemainExact() {
        val correction=RgbCorrection(global=listOf(.05f,-.03f,.01f))
        assertEquals(grey,render(correction,strength=0f))
        assertEquals(grey,render(RgbCorrection()))
        assertEquals(0x7f,render(correction) ushr 24)
        assertEquals(133,channels(render(RgbCorrection(global=listOf(.04f,0f,0f)),strength=.5f))[0])
    }
    @Test fun extremesClampInsteadOfWrapping() {
        assertEquals(listOf(255,255,255),channels(render(RgbCorrection(global=listOf(.08f,.08f,.08f)), -1)))
        assertEquals(listOf(0,0,0),channels(render(RgbCorrection(mode="complement",global=listOf(.08f,.08f,.08f)),0xff000000.toInt())))
    }
    @Test fun savedRecipeAndModelResponsePreserveRgbConfiguration() {
        val plan=ColorPlan(rgb=RgbCorrection("complement",listOf(.02f,0f,-.01f),
            listOf(.01f,0f,0f),listOf(0f,.03f,0f),listOf(0f,0f,-.02f)))
        val recipe=ColorPlanCodec.encode(plan)
        assertEquals(plan,ColorPlanCodec.decode(recipe))
        val response=Gson().toJson(mapOf("candidates" to listOf(mapOf("finishReason" to "STOP",
            "content" to mapOf("parts" to listOf(mapOf("text" to recipe)))))))
        assertEquals(plan,GeminiColorProtocol.response(response))
    }
    @Test fun oldRecipesDefaultToNoRgbCorrection() {
        assertEquals(RgbCorrection(),ColorPlanCodec.decode("""{"basic":{}}""").rgb)
    }
    @Test fun rejectsMalformedModesLengthsAndNonFiniteOrOutOfRangeOffsets() {
        for (rgb in listOf("""{"mode":"unknown"}""", """{"global":[0,0]}""",
            """{"shadows":[0.2,0,0]}""", """{"highlights":["NaN",0,0]}""",
            """{"global":[1e999,0,0]}""")) {
            assertThrows(Exception::class.java) { ColorPlanCodec.decode("{\"basic\":{},\"rgb_correction\":$rgb}") }
        }
    }
}
