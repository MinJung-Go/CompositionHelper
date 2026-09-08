package com.example.compositionhelper.color

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Immutable recipe shared by preview and full-resolution export. */
data class HslBand(val band: String, val halfWidth: Float = 40f, val hueShift: Float = 0f,
    val saturationPercent: Float = 0f, val lightnessPoints: Float = 0f)
data class ColorBalance(val shadows: List<Float> = listOf(0f, 0f, 0f),
    val midtones: List<Float> = listOf(0f, 0f, 0f), val highlights: List<Float> = listOf(0f, 0f, 0f))
data class RgbCorrection(val mode: String = "direct",
    val global: List<Float> = listOf(0f, 0f, 0f), val shadows: List<Float> = listOf(0f, 0f, 0f),
    val midtones: List<Float> = listOf(0f, 0f, 0f), val highlights: List<Float> = listOf(0f, 0f, 0f))
data class ColorPlan(val basic: ColorAdjustment = ColorAdjustment(),
    val curve: List<Float> = CURVE_X, val hsl: List<HslBand> = emptyList(),
    val balance: ColorBalance = ColorBalance(), val explanation: String = "", val scene: String = "",
    val rgb: RgbCorrection = RgbCorrection()) {
    companion object {
        val CURVE_X = listOf(0f, 32f, 64f, 96f, 128f, 160f, 192f, 224f, 255f)
        val BANDS = linkedMapOf("red" to 0f, "orange" to 30f, "yellow" to 60f,
            "green" to 110f, "aqua" to 175f, "blue" to 225f, "purple" to 275f, "magenta" to 315f)
    }
}

object ColorPlanCodec {
    private fun JsonObject.number(key: String, default: Float, low: Float, high: Float): Float {
        val v = get(key) ?: return default
        require(v.isJsonPrimitive && v.asJsonPrimitive.isNumber) { "参数 $key 必须为数值" }
        val n = v.asFloat
        require(n.isFinite() && n in low..high) { "参数 $key 超出范围" }
        return n
    }
    fun decode(json: String): ColorPlan {
        require(json.length <= 64000) { "调色方案过长" }
        val root = JsonParser.parseString(json).asJsonObject
        val basic = root.getAsJsonObject("basic") ?: error("缺少基础调色参数")
        val a = ColorAdjustment(basic.number("exposure", 0f, -1f, 1f),
            basic.number("contrast", 0f, -.5f, .5f), basic.number("shadows", 0f, -.4f, .4f),
            basic.number("highlights", 0f, -.4f, .4f), basic.number("temperature", 0f, -.2f, .2f),
            basic.number("tint", 0f, -.2f, .2f), basic.number("saturation", 0f, -.5f, .5f))
        val curve = root.getAsJsonArray("curve_y")?.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber)
            it.asFloat.also { v -> require(v.isFinite()) }
        } ?: ColorPlan.CURVE_X
        require(curve.size == 9 && curve.first() == 0f && curve.last() == 255f) { "曲线端点或长度无效" }
        require(curve.zip(ColorPlan.CURVE_X).all { (v, x) -> v in maxOf(0f, x-22f)..minOf(255f, x+22f) }
            && curve.zipWithNext().all { (a, b) -> a <= b }) { "曲线必须单调且调整幅度合理" }
        val hsl = root.getAsJsonArray("hsl")?.map { element ->
            val item = element.asJsonObject
            val value = item.get("band").asJsonPrimitive
            val band = if (value.isNumber) ColorPlan.BANDS.entries.firstOrNull { it.value == value.asFloat }?.key
                else value.asString.lowercase(java.util.Locale.ROOT)
            require(band in ColorPlan.BANDS) { "未知的 HSL 色域" }
            HslBand(band!!, item.number("half_width", 40f, 20f, 70f), item.number("hue_shift", 0f, -10f, 10f),
                item.number("saturation_percent", 0f, -25f, 25f), item.number("lightness_points", 0f, -7f, 7f))
        } ?: emptyList()
        require(hsl.map { it.band }.distinct().size == hsl.size) { "HSL 色域重复" }
        val balance = root.getAsJsonObject("color_balance") ?: JsonObject()
        fun channels(key: String): List<Float> = balance.getAsJsonArray(key)?.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber)
            it.asFloat.also { v -> require(v.isFinite() && v in -4f..4f) { "色彩平衡超出范围" } }
        }?.also { require(it.size == 3) } ?: listOf(0f, 0f, 0f)
        val rgb = root.getAsJsonObject("rgb_correction") ?: JsonObject()
        val mode = rgb.get("mode")?.asString ?: "direct"
        require(mode == "direct" || mode == "complement") { "未知的 RGB 调整方式" }
        fun offsets(key: String): List<Float> = rgb.getAsJsonArray(key)?.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber)
            it.asFloat.also { v -> require(v.isFinite() && v in -.08f.. .08f) { "RGB 调整超出范围" } }
        }?.also { require(it.size == 3) { "RGB 调整需要三个通道" } } ?: listOf(0f, 0f, 0f)
        fun label(key: String) = root.get(key)?.asString?.take(1200) ?: ""
        return ColorPlan(a, curve, hsl, ColorBalance(channels("shadows"), channels("midtones"), channels("highlights")),
            label("intent"), label("scene"), RgbCorrection(mode, offsets("global"), offsets("shadows"), offsets("midtones"), offsets("highlights")))
    }
    fun encode(plan: ColorPlan): String = com.google.gson.Gson().toJson(mapOf(
        "basic" to plan.basic, "curve_y" to plan.curve,
        "hsl" to plan.hsl.map { mapOf("band" to it.band, "half_width" to it.halfWidth,
            "hue_shift" to it.hueShift, "saturation_percent" to it.saturationPercent, "lightness_points" to it.lightnessPoints) },
        "color_balance" to plan.balance, "rgb_correction" to plan.rgb, "intent" to plan.explanation, "scene" to plan.scene))
}
