package com.example.compositionhelper.color

object PhotoDecodeSize {
    fun sampleSize(width: Int, height: Int, preview: Boolean): Int {
        require(width > 0 && height > 0) { "图片尺寸无效" }
        var sample = 1
        if (preview) while (maxOf(width, height) / sample > 1200) sample *= 2
        return sample
    }
}
