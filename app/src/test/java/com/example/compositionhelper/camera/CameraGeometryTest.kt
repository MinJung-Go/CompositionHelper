package com.example.compositionhelper.camera

import com.example.compositionhelper.model.RectF
import org.junit.Assert.*
import org.junit.Test

class CameraGeometryTest {
    @Test fun portraitPreviewDoesNotEnterToolbarOrShutterArea() {
        for (aspect in listOf(1f,8f/13f,13f/8f)) {
            val f=CameraGeometry.fit(405f,900f,88f,240f,aspect)
            assertTrue(f.top>=88f)
            assertTrue(f.top+f.height<=660.01f)
            assertEquals(aspect,f.width/f.height,.0001f)
        }
    }
    @Test fun landscapeAndSmallScreensStillRespectReservedSpace() {
        for ((w,h) in listOf(900f to 405f,320f to 568f)) {
            val f=CameraGeometry.fit(w,h,88f,240f,13f/8f)
            assertTrue(f.width<=w); assertTrue(f.top>=88f)
            assertTrue(f.top+f.height<=h-240f+.01f)
        }
    }
    @Test fun cropCoordinatesRotateForAllFourOrientations() {
        val c=RectF(100f,50f,500f,400f)
        assertEquals(c,CameraGeometry.rotatedCrop(c,640f,480f,0))
        assertEquals(RectF(80f,100f,430f,500f),CameraGeometry.rotatedCrop(c,640f,480f,90))
        assertEquals(RectF(140f,80f,540f,430f),CameraGeometry.rotatedCrop(c,640f,480f,180))
        assertEquals(RectF(50f,140f,400f,540f),CameraGeometry.rotatedCrop(c,640f,480f,270))
    }
    @Test fun croppedSubjectUsesViewportInsteadOfFullSensorSize() {
        val crop=RectF(100f,0f,500f,400f)
        assertEquals(RectF(.25f,.25f,.75f,.75f),CameraGeometry.toViewport(RectF(200f,100f,400f,300f),crop))
        assertNull(CameraGeometry.toViewport(RectF(0f,0f,50f,50f),crop))
        assertEquals(RectF(0f,0f,.25f,.25f),CameraGeometry.toViewport(RectF(50f,-50f,200f,100f),crop))
    }
    @Test fun spiralAlignmentMatchesPortraitAndLandscapeEndpointsAndFlips() {
        val p=com.example.compositionhelper.model.SpiralGeometry.terminal(8f,13f,0)
        assertEquals(.25f,p.x,.0001f); assertEquals(10f/13f,p.y,.0001f)
        val flipped=com.example.compositionhelper.model.SpiralGeometry.terminal(8f,13f,3)
        assertEquals(1-p.x,flipped.x,.0001f); assertEquals(1-p.y,flipped.y,.0001f)
        val landscape=com.example.compositionhelper.model.SpiralGeometry.terminal(13f,8f,0)
        assertEquals(10f/13f,landscape.x,.0001f); assertEquals(.75f,landscape.y,.0001f)
    }
    @Test fun diagonallySeparatedBoxesNeverIntersect() {
        assertFalse(RectF(0f,0f,1f,1f).intersects(RectF(2f,2f,3f,3f)))
        assertFalse(RectF(0f,0f,0f,0f).intersects(RectF(0f,0f,1f,1f)))
        assertTrue(RectF(0f,0f,1f,1f).intersects(RectF(.1f,.1f,.9f,.9f)))
    }
}
