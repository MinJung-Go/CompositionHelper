package com.example.compositionhelper.color

import kotlin.math.*

/** Legacy sRGB pipeline plus optional RGB correction; blends once at the end. */
object ColorPlanEngine {
    fun prepare(plan: ColorPlan, strength: Float): (Int) -> Int {
        require(strength.isFinite())
        val amount = strength.coerceIn(0f, 1f)
        if (amount == 0f || plan.copy(explanation = "", scene = "") == ColorPlan()) return { it }
        val a = plan.basic
        val gain = 2f.pow(a.exposure)
        fun curve(value: Float): Float {
            val x = value.coerceIn(0f, 1f) * 255f
            val i = (x / 32).toInt().coerceAtMost(7)
            val t = (x - ColorPlan.CURVE_X[i]) / (ColorPlan.CURVE_X[i+1] - ColorPlan.CURVE_X[i])
            return (plan.curve[i] + t * (plan.curve[i+1] - plan.curve[i])) / 255f
        }
        val rgb = plan.rgb
        val hasRgb = rgb.global.any { it != 0f } || rgb.shadows.any { it != 0f } ||
            rgb.midtones.any { it != 0f } || rgb.highlights.any { it != 0f }
        val centers = plan.hsl.map { ColorPlan.BANDS.getValue(it.band) }
        return { pixel ->
            val r = (pixel shr 16 and 255) / 255f
            val g = (pixel shr 8 and 255) / 255f
            val b = (pixel and 255) / 255f
            val y = .2126f*r + .7152f*g + .0722f*b
            val tonal = a.shadows*(1-y).pow(2) + a.highlights*y*y
            fun basic(v: Float, balance: Float) = (v*gain + tonal + balance - .5f)*(1+a.contrast)+.5f
            var rr = basic(r, a.temperature+a.tint*.5f)
            var gg = basic(g, -a.tint)
            var bb = basic(b, -a.temperature+a.tint*.5f)
            val grey = .2126f*rr + .7152f*gg + .0722f*bb
            rr = curve(grey + (rr-grey)*(1+a.saturation))
            gg = curve(grey + (gg-grey)*(1+a.saturation))
            bb = curve(grey + (bb-grey)*(1+a.saturation))
            if (plan.hsl.isNotEmpty()) {
                val max = maxOf(rr, gg, bb); val min = minOf(rr, gg, bb); val d = max-min
                var l = (max+min)*.5f
                val denominator = 1-abs(2*l-1)
                var s = if (denominator > 1e-6f) d/denominator else 0f
                var h = if (d <= 1e-6f) 0f else when (max) {
                    rr -> ((gg-bb)/d*60+360)%360
                    gg -> ((bb-rr)/d+2)*60
                    else -> ((rr-gg)/d+4)*60
                }
                val neutral = ((s-.05f)/.2f).coerceIn(0f, 1f).let { it*it*(3-2*it) }
                var total = 0f; var dh = 0f; var ds = 0f; var dl = 0f
                plan.hsl.forEachIndexed { i, band ->
                    val distance = abs((h-centers[i]+540)%360-180)
                    val w = if (distance < band.halfWidth) (.5f+.5f*cos(PI.toFloat()*distance/band.halfWidth))*neutral else 0f
                    total += w; dh += w*band.hueShift
                    ds += w*band.saturationPercent/100; dl += w*band.lightnessPoints/100
                }
                val normal = maxOf(total, 1f)
                h = (h+dh/normal+360)%360
                s = (s*(1+ds/normal)).coerceIn(0f, 1f); l = (l+dl/normal).coerceIn(0f, 1f)
                val c = (1-abs(2*l-1))*s; val hp = h/60
                val x = c*(1-abs(hp%2-1)); val m = l-c/2
                when (hp.toInt()) {
                    0 -> { rr=c; gg=x; bb=0f }; 1 -> { rr=x; gg=c; bb=0f }
                    2 -> { rr=0f; gg=c; bb=x }; 3 -> { rr=0f; gg=x; bb=c }
                    4 -> { rr=x; gg=0f; bb=c }; else -> { rr=c; gg=0f; bb=x }
                }
                rr += m; gg += m; bb += m
            }
            val lum = .2126f*rr+.7152f*gg+.0722f*bb
            fun balance(v: Float, channel: Int) = (v + ((1-lum).pow(2)*plan.balance.shadows[channel] +
                4*lum*(1-lum)*plan.balance.midtones[channel] + lum*lum*plan.balance.highlights[channel])/255).coerceIn(0f,1f)
            rr = balance(rr,0); gg = balance(gg,1); bb = balance(bb,2)
            if (hasRgb) {
                val yRgb = .2126f*rr + .7152f*gg + .0722f*bb
                val shadowWeight = (1-yRgb)*(1-yRgb)
                val midWeight = 2*yRgb*(1-yRgb)
                val highWeight = yRgb*yRgb
                fun axis(i: Int) = rgb.global[i] + shadowWeight*rgb.shadows[i] +
                    midWeight*rgb.midtones[i] + highWeight*rgb.highlights[i]
                val red = axis(0); val green = axis(1); val blue = axis(2)
                if (rgb.mode == "complement") {
                    rr -= green+blue; gg -= red+blue; bb -= red+green
                } else { rr += red; gg += green; bb += blue }
                rr = rr.coerceIn(0f,1f); gg = gg.coerceIn(0f,1f); bb = bb.coerceIn(0f,1f)
            }
            fun encode(original: Float, value: Float) = ((original+(value-original)*amount)*255).roundToInt().coerceIn(0,255)
            (pixel and -0x1000000) or (encode(r,rr) shl 16) or
                (encode(g,gg) shl 8) or encode(b,bb)
        }
    }
}
