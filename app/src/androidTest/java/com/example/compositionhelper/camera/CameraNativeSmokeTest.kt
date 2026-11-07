package com.example.compositionhelper.camera

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.compositionhelper.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.opencv.android.OpenCVLoader
import java.util.concurrent.TimeUnit

class CameraNativeSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensSmartCameraAndLoadsNativeDetectors() {
        compose.onNodeWithText("手动").performClick()
        compose.onNodeWithText("智能").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasContentDescription("拍照") and isEnabled()).fetchSemanticsNodes().size == 1
        }

        assertTrue("OpenCV failed to load", OpenCVLoader.initLocal())
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val detector = ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        try {
            Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
        } finally {
            detector.close()
            bitmap.recycle()
        }
    }
}
