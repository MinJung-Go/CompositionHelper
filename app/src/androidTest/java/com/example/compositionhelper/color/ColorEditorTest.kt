package com.example.compositionhelper.color

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** No paid API calls. Exercises the real editor and Bitmap/MediaStore pipeline with an injected analyzer. */
class ColorEditorTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var file: File
    private val uri get() = Uri.fromFile(file)
    private val aiPlan = ColorPlan(basic = ColorAdjustment(shadows = .08f),
        hsl = listOf(HslBand("blue", saturationPercent = -20f)), scene = "测试湖景", explanation = "适度提亮暗部")
    @Before fun setup() {
        GeminiColorSession.apiKey = "test-only-no-network"
        file = File(context.cacheDir, "color-editor-test.jpg")
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff305080.toInt())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
    }
    @After fun cleanup() { GeminiColorSession.apiKey = ""; file.delete() }
    private fun waitForText(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun waitReady() {
        compose.waitUntil(15000) { compose.onAllNodes(hasText("保存副本") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun click(text: String) {
        val node = compose.onNodeWithText(text)
        runCatching { node.performScrollTo() } // Bottom bar actions have no scroll ancestor.
        node.performClick(); compose.waitForIdle()
    }
    @Test fun aiRecipeSurvivesRestorationAndSupportsResetUndo() {
        val restoration = StateRestorationTester(compose)
        var calls = 0
        restoration.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, _ -> calls++; aiPlan }) } }
        waitReady()
        compose.onNodeWithContentDescription("原图").assertExists()
        compose.onNodeWithContentDescription("调色后").assertExists()
        click("AI 调色"); waitForText("测试湖景")
        restoration.emulateSavedInstanceStateRestore()
        waitForText("测试湖景")
        assertEquals(1, calls)
        click("重置")
        compose.onNodeWithText("测试湖景").assertDoesNotExist()
        click("撤销"); waitForText("测试湖景")
    }
    @Test fun failedReanalysisKeepsPreviousRecipe() {
        var calls = 0
        compose.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, _ ->
            if (++calls == 1) aiPlan else throw IllegalStateException("调用额度不足")
        }) } }
        waitReady()
        click("AI 调色"); waitForText("测试湖景")
        click("AI 调色"); waitForText("调用额度不足")
        compose.onNodeWithText("测试湖景").assertExists()
    }
    @Test fun cancelAnalysisKeepsOriginalAndAllowsRetry() {
        compose.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, _ -> awaitCancellation() }) } }
        waitReady()
        click("AI 调色"); waitForText("取消分析"); click("取消分析")
        compose.onNodeWithText("AI 调色").assertIsEnabled()
        compose.onAllNodesWithText("操作已取消").onFirst().assertExists()
        compose.onNodeWithText("取消分析").assertDoesNotExist()
    }
    @Test fun streamedPreviewCannotBeSavedAndCancelKeepsOriginal() {
        compose.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, emit ->
            emit(aiPlan); awaitCancellation()
        }) } }
        waitReady(); click("AI 调色")
        waitForText("临时预览 · AI 仍在完善调色…")
        compose.onNodeWithText("保存副本").assertIsNotEnabled()
        compose.onNodeWithText("测试湖景").assertDoesNotExist()
        click("取消分析")
        compose.onNodeWithText("保存副本").assertIsEnabled()
        compose.onNodeWithText("测试湖景").assertDoesNotExist()
    }
    @Test fun failedStreamRestoresCommittedRecipe() {
        var calls = 0
        compose.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, emit ->
            if (++calls == 1) aiPlan else {
                emit(ColorPlan(basic = ColorAdjustment(exposure = .4f), scene = "临时方案"))
                throw IllegalStateException("流连接中断")
            }
        }) } }
        waitReady(); click("AI 调色"); waitForText("测试湖景")
        click("AI 调色"); waitForText("流连接中断")
        compose.onNodeWithText("测试湖景").assertExists()
        compose.onNodeWithText("临时方案").assertDoesNotExist()
        compose.onNodeWithText("保存副本").assertIsEnabled()
    }
    @Test fun missingCredentialOpensSettingsWithoutUploading() {
        GeminiColorSession.apiKey = ""
        var called = false
        compose.setContent { MaterialTheme { ColorEditorContent(uri, {}, { _, _, _, _ -> called = true; aiPlan }) } }
        waitReady()
        click("AI 调色")
        compose.onNodeWithText("Gemini 调色设置").assertExists()
        assertFalse(called)
    }
    @Test fun allPackagedPhotosDecodeAndRenderThroughResourceUris() = runBlocking {
        for (sample in com.example.compositionhelper.samples.PhotoSamples.all) {
            val source = ColorPhotoStore.load(context,
                com.example.compositionhelper.samples.PhotoSamples.uri(context.packageName, sample), true)
            try {
                assertTrue(source.width > 0 && source.height > 0)
                assertTrue(maxOf(source.width,source.height) <= 1200)
                val preview = ColorPhotoStore.render(source, aiPlan, .65f)
                try { assertEquals(source.width,preview.width); assertEquals(source.height,preview.height) }
                finally { preview.recycle() }
            } finally { source.recycle() }
        }
    }
    @Test fun invalidPhotoShowsRecoveryWithoutDisabledEditingControls() {
        compose.setContent { MaterialTheme { ColorEditorContent(Uri.fromFile(File(context.cacheDir,"missing-photo.jpg")), {}) } }
        waitForText("照片未能打开")
        compose.onNodeWithText("重新读取").assertExists()
        compose.onNodeWithText("重新选图").assertExists()
        compose.onNodeWithText("手动精调").assertDoesNotExist()
        compose.onNodeWithText("AI 调色").assertDoesNotExist()
    }
    @Test fun previewMetricsMeasureActualPixelsAndMatchExportRenderer() = runBlocking {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.eraseColor(0xff305080.toInt())
        val unchanged = ColorPhotoStore.renderPreview(source, aiPlan, 0f)
        val adjusted = ColorPhotoStore.renderPreview(source, aiPlan, 1f)
        val export = ColorPhotoStore.render(source, aiPlan, 1f)
        try {
            assertEquals(0f, unchanged.changedPercent, 0f)
            assertEquals(0f, unchanged.meanDifference, 0f)
            assertTrue(adjusted.changedPercent > 0f)
            assertTrue(adjusted.meanDifference > 0f)
            assertTrue(adjusted.bitmap.sameAs(export))
        } finally {
            source.recycle(); unchanged.bitmap.recycle(); adjusted.bitmap.recycle(); export.recycle()
        }
    }
    @Test fun exportPreservesDimensionsOriginalAndUsesSameRecipe() = runBlocking {
        val before = file.readBytes()
        val source = ColorPhotoStore.load(context, uri, false)
        val preview = ColorPhotoStore.render(source, aiPlan, .65f)
        val outputUri = ColorPhotoStore.save(context, uri, aiPlan, .65f)
        try {
            val saved = ColorPhotoStore.load(context, outputUri, false)
            try {
                assertEquals(source.width, saved.width); assertEquals(source.height, saved.height)
                val expected = preview.getPixel(100,100); val actual = saved.getPixel(100,100)
                for (shift in listOf(0,8,16)) assertEquals((expected shr shift and 255).toFloat(),
                    (actual shr shift and 255).toFloat(), 4f) // JPEG is lossy.
                assertArrayEquals(before, file.readBytes())
            } finally { saved.recycle() }
        } finally {
            context.contentResolver.delete(outputUri, null, null)
            source.recycle(); preview.recycle()
        }
    }
}
