package com.example.compositionhelper.color

import com.google.gson.Gson
import com.google.gson.JsonParser

/** Pure wire protocol so malformed, blocked and truncated responses can be tested on the JVM. */
object GeminiColorProtocol {
    const val DEFAULT_MODEL = "gemini-3.5-flash"
    fun validModel(model: String) = model.matches(Regex("gemini-[A-Za-z0-9._-]{1,80}"))
    fun request(imageBase64: String): String = Gson().toJson(mapOf(
        "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to PROMPT),
            mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to imageBase64))))),
        "generationConfig" to mapOf("temperature" to .15, "responseMimeType" to "application/json", "maxOutputTokens" to 8192)))

    fun response(body: String): ColorPlan {
        require(body.length <= 256000) { "模型响应过长" }
        val root = JsonParser.parseString(body).asJsonObject
        val candidate = root.getAsJsonArray("candidates")?.firstOrNull()?.asJsonObject
            ?: error("模型未返回可用方案，请更换照片或重试")
        check(candidate.get("finishReason")?.asString == "STOP") { "模型响应未完成或被拦截，请重试" }
        val parts = candidate.getAsJsonObject("content")?.getAsJsonArray("parts") ?: error("模型响应为空")
        val text = parts.map { it.asJsonObject }.filter { it.get("thought")?.asBoolean != true }
            .joinToString("") { it.get("text")?.asString ?: "" }
        return ColorPlanCodec.decode(text)
    }
    fun httpError(code: Int): String = when (code) {
        400 -> "请求无效，请检查模型是否支持图片和 JSON 输出"
        401, 403 -> "API Key 无效、无权限或服务在当前地区不可用"
        404 -> "模型不可用，请在 AI 设置中更换模型名称"
        429 -> "调用额度不足或请求过于频繁，请稍后重试"
        in 500..599 -> "模型服务暂时不可用，请稍后重试"
        else -> "请求失败（HTTP $code）"
    }
    private val PROMPT = """
        Analyze this photograph as a professional Photoshop colorist. Produce a natural, restrained edit
        appropriate to its actual scene, subjects and lighting. Preserve skin, text and realistic water/foliage.
        No masks or spatial selections are available. Similar-colored objects are affected together.
        Return ONE JSON object with scene and intent (short Chinese descriptions), basic, curve_y, hsl,
        color_balance, rgb_correction. Describe only adjustments the engine can actually perform. No markdown.
        Processing order, on gamma-encoded sRGB [0,1]:
        1) basic: exposure [-0.5,0.5] stops; contrast [-0.2,0.2]; shadows [-0.2,0.25];
        highlights [-0.25,0.15]; temperature [-0.05,0.05]; tint [-0.04,0.04]; saturation [-0.25,0.3].
        All default zero. y=.2126*r+.7152*g+.0722*b.
        tonal=shadows*(1-y)^2+highlights*y^2;
        rgb=(rgb*2^exposure+tonal+[temperature+tint/2,-tint,-temperature+tint/2]-.5)*(1+contrast)+.5;
        saturation multiplier 1+saturation about luminance, then clamp.
        2) curve_y: nine RGB output levels at x=[0,32,64,96,128,160,192,224,255], linear interpolation.
        Monotonic, endpoints 0 and 255, each point deviates at most 22. Identity curve is valid.
        3) hsl: array of {band,half_width,hue_shift,saturation_percent,lightness_points}.
        band MUST be a name: red,orange,yellow,green,aqua,blue,purple,magenta (centers 0,30,60,110,175,225,275,315).
        half_width 20..70 degrees; hue_shift -10..10 degrees; saturation_percent -25..25 relative percent;
        lightness_points -7..7 additive HSL lightness points. Cosine feather weights over circular hue distance,
        neutral protection smoothstep(.05,.25,saturation); normalize overlapping total weights by max(total,1).
        4) color_balance: {shadows:[r,g,b],midtones:[r,g,b],highlights:[r,g,b]} byte offsets -4..4.
        Weights (1-y)^2,4*y*(1-y),y^2 based on luminance after HSL. Zero offsets valid.
        5) rgb_correction: {mode:"direct" or "complement",global:[r,g,b],shadows:[r,g,b],
        midtones:[r,g,b],highlights:[r,g,b]}. All entries default zero, each [-0.08,0.08] in sRGB units.
        On clamped output of step 4, compute y=.2126*r+.7152*g+.0722*b once;
        axis_i=global_i+(1-y)^2*shadows_i+2*y*(1-y)*midtones_i+y^2*highlights_i.
        direct: add [axis_r,axis_g,axis_b] to RGB.
        complement: add [-axis_g-axis_b,-axis_r-axis_b,-axis_r-axis_g] to RGB. Then clamp.
        Thus positive red in direct mode raises R; positive red in complement mode lowers G and B.
        These methods change brightness differently and are NOT equivalent. Keep offsets subtle, usually <=0.02.
        Judge casts from plausible neutral objects and scene lighting. A lake or forest having little red is
        NOT evidence of a cast. Do not equalize RGB histograms or neutralize intentional warm/cool lighting.
        Use this only for a visible cast or justified stylistic intent; preserve natural water and skin.
        Avoid stacking it with white balance and color_balance to correct the same cast multiple times.
        Explain the chosen direction and tonal range briefly in Chinese intent. Zero is valid.
        Be conservative: avoid cumulative shadow/curve brightening, HDR look, cyan water and neon greens.
        For daylight landscapes start exposure near zero, shadows around .05.. .10 only when needed,
        and reduce blue saturation if already strong. Choose parameters per photo, not a fixed preset.
    """.trimIndent()
}
