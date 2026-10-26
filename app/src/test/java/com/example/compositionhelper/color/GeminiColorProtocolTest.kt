package com.example.compositionhelper.color

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class GeminiColorProtocolTest {
    private fun response(finish: String = "STOP", parts: List<Map<String, Any>>) = Gson().toJson(mapOf(
        "candidates" to listOf(mapOf("finishReason" to finish, "content" to mapOf("parts" to parts)))))
    @Test fun requestIncludesImageAndJsonConfigurationWithoutCredentials() {
        val payload = JsonParser.parseString(GeminiColorProtocol.request("image-data")).asJsonObject
        val parts = payload.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")
        assertTrue(parts[0].asJsonObject.get("text").asString.contains("No masks"))
        assertEquals("image-data", parts[1].asJsonObject.getAsJsonObject("inline_data").get("data").asString)
        assertEquals("application/json", payload.getAsJsonObject("generationConfig").get("responseMimeType").asString)
        assertFalse(payload.has("apiKey"))
    }
    @Test fun extractsPlanAndIgnoresThoughtParts() {
        val plan = ColorPlan(basic = ColorAdjustment(shadows = .08f), explanation = "自然提亮")
        val body = response(parts = listOf(mapOf("thought" to true, "text" to "reasoning"),
            mapOf("text" to ColorPlanCodec.encode(plan))))
        assertEquals(plan, GeminiColorProtocol.response(body))
    }
    @Test fun rejectsBlockedTruncatedAndEmptyResponses() {
        for (finish in listOf("MAX_TOKENS", "SAFETY", "RECITATION")) {
            assertThrows(Exception::class.java) {
                GeminiColorProtocol.response(response(finish, listOf(mapOf("text" to "{\"basic\":{}}"))))
            }
        }
        for (body in listOf("{}", "{\"candidates\":[]}", response(parts = emptyList()), "not json")) {
            assertThrows(Exception::class.java) { GeminiColorProtocol.response(body) }
        }
    }
    @Test fun refusesModelPathInjectionAndAcceptsConfigurableModel() {
        assertTrue(GeminiColorProtocol.validModel("gemini-3.5-flash"))
        for (model in listOf("../other", "gemini-x?key=foo", "gemini-x\n", "")) assertFalse(GeminiColorProtocol.validModel(model))
    }
    @Test fun httpErrorsExplainRecoveryWithoutRemotePayloads() {
        assertTrue(GeminiColorProtocol.httpError(429).contains("额度"))
        assertTrue(GeminiColorProtocol.httpError(404).contains("模型"))
        assertTrue(GeminiColorProtocol.httpError(503).contains("稍后"))
    }
}
