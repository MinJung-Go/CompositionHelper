package com.example.compositionhelper.color

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object ColorPhotoStore {
    suspend fun load(context: Context, uri: Uri, preview: Boolean): Bitmap = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片" }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            if (Build.VERSION.SDK_INT >= 26) inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            // BitmapFactory.Options.inSampleSize starts at 0; never divide by that default.
            inSampleSize = PhotoDecodeSize.sampleSize(bounds.outWidth, bounds.outHeight, preview)
        }
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("图片解码失败")
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }
                8 -> setRotate(-90f)
            }
        }
        try {
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (oriented !== bitmap) bitmap.recycle()
            oriented
        } catch (e: Throwable) { bitmap.recycle(); throw e }
    }

    suspend fun suggest(bitmap: Bitmap): ColorSuggestion = withContext(Dispatchers.Default) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        ColorAdjustmentEngine.analyze(pixels)
    }

    suspend fun render(source: Bitmap, plan: ColorPlan, strength: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val output = source.copy(Bitmap.Config.ARGB_8888, true) ?: error("无法创建调色图片")
            try {
                val row = IntArray(source.width)
                val transform = ColorPlanEngine.prepare(plan, strength)
                for (y in 0 until source.height) {
                    currentCoroutineContext().ensureActive()
                    source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                    for (x in row.indices) row[x] = transform(row[x])
                    output.setPixels(row, 0, source.width, 0, y, source.width, 1)
                }
                output
            } catch (e: Throwable) { output.recycle(); throw e }
        }

    suspend fun save(context: Context, sourceUri: Uri, plan: ColorPlan, strength: Float): Uri =
        withContext(Dispatchers.IO) {
            val source = load(context, sourceUri, preview = false)
            try {
                val output = render(source, plan, strength)
                try {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "composition_color_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= 29) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CompositionHelper")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("无法创建照片")
                    try {
                        resolver.openOutputStream(uri)?.use {
                            check(output.compress(Bitmap.CompressFormat.JPEG, 95, it)) { "照片编码失败" }
                        } ?: error("无法写入照片")
                        currentCoroutineContext().ensureActive()
                        if (Build.VERSION.SDK_INT >= 29) {
                            check(resolver.update(uri, ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }, null, null) > 0) { "无法完成照片保存" }
                        }
                        uri
                    } catch (e: Throwable) {
                        runCatching { resolver.delete(uri, null, null) }
                        throw e
                    }
                } finally { output.recycle() }
            } finally { source.recycle() }
        }
}
