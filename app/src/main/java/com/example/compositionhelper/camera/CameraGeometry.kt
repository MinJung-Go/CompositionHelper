package com.example.compositionhelper.camera

import com.example.compositionhelper.model.RectF

/** Coordinates in an upright analysis image, mapped to the rotated CameraX viewport crop. */
object CameraGeometry {
    fun rotatedCrop(c: RectF, width: Float, height: Float, rotation: Int): RectF = when(rotation) {
        0 -> c
        90 -> RectF(height-c.bottom,c.left,height-c.top,c.right)
        180 -> RectF(width-c.right,height-c.bottom,width-c.left,height-c.top)
        270 -> RectF(c.top,width-c.right,c.bottom,width-c.left)
        else -> error("Unsupported image rotation")
    }
    fun toViewport(box: RectF, crop: RectF): RectF? {
        if (crop.width() <= 0 || crop.height() <= 0) return null
        val left=maxOf(box.left,crop.left); val top=maxOf(box.top,crop.top)
        val right=minOf(box.right,crop.right); val bottom=minOf(box.bottom,crop.bottom)
        if (right<=left || bottom<=top) return null
        return RectF((left-crop.left)/crop.width(),(top-crop.top)/crop.height(),
            (right-crop.left)/crop.width(),(bottom-crop.top)/crop.height())
    }
    data class Frame(val width: Float,val height: Float,val top: Float)
    fun fit(width: Float,height: Float,top: Float,bottom: Float,aspect: Float): Frame {
        require(width>0 && height>0 && aspect>0)
        val available=(height-top-bottom).coerceAtLeast(0f)
        val w=minOf(width,available*aspect)
        return Frame(w,w/aspect,top+(available-w/aspect)/2)
    }
}
