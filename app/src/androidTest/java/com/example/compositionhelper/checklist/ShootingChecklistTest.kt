package com.example.compositionhelper.checklist

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.example.compositionhelper.ui.theme.CompositionHelperTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShootingChecklistTest {
    @get:Rule val compose = createComposeRule()
    private val prefs get() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("shooting_checklist", Context.MODE_PRIVATE)
    @Before fun setup() { prefs.edit().clear().commit() }
    @After fun cleanup() { prefs.edit().clear().commit() }
    private fun mark(id: String, label: String) {
        compose.onNodeWithTag("shootingChecklist.$id").performClick()
        compose.onNodeWithText(label).performClick()
    }
    private fun scrollTo(text: String) {
        compose.onNodeWithTag("shootingChecklistList").performScrollToNode(hasText(text))
    }
    @Test fun manualResultAndNotesSurviveRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { CompositionHelperTheme { ShootingChecklistScreen {} } }
        compose.onNodeWithText("已确认 0/12 · 有问题 0 · 不适用 0").assertExists()
        mark("prepare_permission", "已确认")
        compose.onNodeWithText("已确认 1/12 · 有问题 0 · 不适用 0").assertExists()
        scrollTo("问题与备注")
        compose.onNodeWithTag("shootingChecklistNotes").performTextInput("相册已人工核对")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("shootingChecklistList").performScrollToIndex(0)
        compose.onNodeWithText("已确认 1/12 · 有问题 0 · 不适用 0").assertExists()
        scrollTo("问题与备注")
        compose.onNodeWithTag("shootingChecklistNotes").assertTextContains("相册已人工核对")
    }
    @Test fun problemsAreNotPassesAndResetRequiresConfirmation() {
        compose.setContent { CompositionHelperTheme { ShootingChecklistScreen {} } }
        mark("prepare_permission", "有问题")
        compose.onNodeWithText("已确认 0/12 · 有问题 1 · 不适用 0").assertExists()
        mark("prepare_permission", "不适用")
        compose.onNodeWithText("已确认 0/12 · 有问题 0 · 不适用 1").assertExists()
        scrollTo("开始新一轮检查")
        compose.onNodeWithText("开始新一轮检查").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("shootingChecklistList").performScrollToIndex(0)
        compose.onNodeWithText("已确认 0/12 · 有问题 0 · 不适用 1").assertExists()
        scrollTo("开始新一轮检查")
        compose.onNodeWithText("开始新一轮检查").performClick()
        compose.onNodeWithText("清空并开始新一轮").performClick()
        compose.onNodeWithTag("shootingChecklistList").performScrollToIndex(0)
        compose.onNodeWithText("已确认 0/12 · 有问题 0 · 不适用 0").assertExists()
    }
}
