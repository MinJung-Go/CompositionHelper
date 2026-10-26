package com.example.compositionhelper.color

object PhotoDecodeSize {
    const val MAX_EXPORT_PIXELS = 32_000_000L

    fun sampleSize(width: Int, height: Int, preview: Boolean): Int {
        require(width > 0 && height > 0) { "图片尺寸无效" }
        require(preview || width.toLong() * height <= MAX_EXPORT_PIXELS) {
            "原图超过 3200 万像素，暂不支持导出；不会降低分辨率保存"
        }
        var sample = 1
        if (preview) while (maxOf(width, height) / sample > 1200) sample *= 2
        return sample
    }
}
