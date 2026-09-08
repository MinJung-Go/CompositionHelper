package com.example.compositionhelper.color

import org.junit.Assert.*
import org.junit.Test

class PhotoDecodeSizeTest {
    @Test fun packagedAndCameraPhotosHaveNonzeroBoundedPreviewSamples() {
        for ((w,h) in listOf(1600 to 1200,1067 to 1600,5712 to 4284,4032 to 3024,320 to 240)) {
            val sample = PhotoDecodeSize.sampleSize(w,h,true)
            assertTrue(sample >= 1)
            assertTrue(maxOf(w,h)/sample <= 1200)
            assertEquals(0,sample and (sample-1))
        }
    }
    @Test fun exportAlwaysUsesOriginalSizeAndSmallImagesAreNotDownsampled() {
        assertEquals(1,PhotoDecodeSize.sampleSize(5712,4284,false))
        assertEquals(1,PhotoDecodeSize.sampleSize(1200,800,true))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUndecodableDimensions() { PhotoDecodeSize.sampleSize(-1,-1,true) }
}
