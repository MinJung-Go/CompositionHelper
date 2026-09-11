package com.example.compositionhelper.color

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class GlmColorTest {
    private fun send(d: ColorStreamDecoder, text: String, finish: String? = null, reasoning: String = ""): ColorPlan? {
        val choice = mapOf("index" to 0, "delta" to mapOf("content" to text, "reasoning_content" to reasoning), "finish_reason" to finish)
        d.line("data: " + Gson().toJson(mapOf("choices" to listOf(choice))))
        return d.line("")
    }
    private fun rejected(block: () -> Unit) { try { block(); fail("should reject") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {} }
    @Test fun streamsValidGroupsAndIgnoresReasoning() {
        val d = ColorStreamDecoder(true)
        assertNull(send(d, "", reasoning = "not JSON"))
        assertNull(send(d, """{"basic":{"exposure":0."""))
        assertEquals(.2f, send(d, """2},"intent":""" )!!.basic.exposure, 0f)
        send(d, "\"自然肤色\"}", "stop")
        d.line("data: {\"choices\":[],\"usage\":{}}"); d.line("")
        d.line("data: [DONE]"); d.line("")
        assertEquals("自然肤色", d.finish().explanation)
    }
    @Test fun missingStopRejected() { val d = ColorStreamDecoder(true); send(d, """{"basic":{}}"""); rejected { d.finish() } }
    @Test fun tokenLimitRejected() { rejected { send(ColorStreamDecoder(true), "", "length") } }
    @Test fun filteredOutputRejected() { rejected { send(ColorStreamDecoder(true), "", "sensitive") } }
    @Test fun invalidRecipeRejected() { val d = ColorStreamDecoder(true); send(d, """{"basic":{"exposure":99}}""", "stop"); rejected { d.finish() } }
    @Test fun truncatedJsonRejected() { val d = ColorStreamDecoder(true); send(d, """{"basic":{},""", "stop"); rejected { d.finish() } }
    @Test fun errorEnvelopeRejected() { val d = ColorStreamDecoder(true); d.line("data: {\"error\":{\"code\":\"1301\"}}"); rejected { d.line("") } }
    @Test fun contentAfterStopRejected() { val d = ColorStreamDecoder(true); send(d, """{"basic":{}}""", "stop"); rejected { send(d, "extra") } }
    @Test fun requestContainsImageAndKeepsThinking() {
        val r = JsonParser.parseString(GeminiColorProtocol.glmRequest("aGVsbG8=", "glm-5.3-flash")).asJsonObject
        assertEquals("glm-5.3-flash", r["model"].asString)
        assertTrue(r["stream"].asBoolean)
        assertEquals("enabled", r.getAsJsonObject("thinking")["type"].asString)
        val image = r.getAsJsonArray("messages")[0].asJsonObject.getAsJsonArray("content")[1].asJsonObject
        assertEquals("data:image/jpeg;base64,aGVsbG8=", image.getAsJsonObject("image_url")["url"].asString)
        assertFalse(r.has("contents"))
        assertEquals("max", r["reasoning_effort"].asString)
        assertEquals("json_object", r.getAsJsonObject("response_format")["type"].asString)
    }
    @Test fun onlySupportedModelsAccepted() {
        assertTrue(GeminiColorProtocol.supportedModel("glm-5.3-flash"))
        assertFalse(GeminiColorProtocol.supportedModel("glm-4.6v"))
        assertTrue(GeminiColorProtocol.supportedModel(GeminiColorProtocol.DEFAULT_MODEL))
        assertFalse(GeminiColorProtocol.supportedModel("glm-4.6"))
        assertFalse(GeminiColorProtocol.supportedModel("https://other.example/model"))
    }
}
