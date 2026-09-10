package com.example.compositionhelper.color

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ColorStreamDecoderTest {
    private fun event(text: String, finish: String? = null, thought: Boolean = false): String = Gson().toJson(mapOf(
        "candidates" to listOf(mapOf("index" to 0, "content" to mapOf("parts" to listOf(mapOf("text" to text, "thought" to thought)))) +
            (finish?.let { mapOf("finishReason" to it) } ?: emptyMap<String, Any>()))))
    private fun send(d: ColorStreamDecoder, text: String, finish: String? = null, thought: Boolean = false): ColorPlan? {
        d.line("data: " + event(text, finish, thought)); return d.line("")
    }
    private fun rejected(block: () -> Unit) { try { block(); fail("should reject") } catch (_: IllegalArgumentException) {} catch (_: IllegalStateException) {} }
    @Test fun emitsOnlyCompleteGroups() {
        val d = ColorStreamDecoder()
        assertNull(send(d, "{\"basic\":{\"exposure\":0."))
        val partial = send(d, "2},\"curve_y\":[0,32")!!
        assertEquals(.2f, partial.basic.exposure, 0f)
        assertEquals(ColorPlan.CURVE_X, partial.curve)
        send(d, ",64,96,128,160,192,224,255]}", "STOP")
        assertEquals(.2f, d.finish().basic.exposure, 0f)
    }
    @Test fun handlesCharacterChunksEscapesUnicodeAndThoughts() {
        val d = ColorStreamDecoder()
        send(d, "ignore this thought", thought = true)
        val json = "{\"basic\":{},\"intent\":\"湖泊，含\\\"引号\\\"和}逗号,\",\"hsl\":[]}"
        json.forEach { send(d, it.toString()) }
        send(d, "", "STOP")
        assertEquals("湖泊，含\"引号\"和}逗号,", d.finish().explanation)
    }
    @Test fun rejectsMissingStop() { val d = ColorStreamDecoder(); send(d, "{\"basic\":{}}"); rejected { d.finish() } }
    @Test fun rejectsTruncatedJsonWithStop() { val d = ColorStreamDecoder(); send(d, "{\"basic\":{},\"curve_y\":[0", "STOP"); rejected { d.finish() } }
    @Test fun rejectsTokenLimit() { rejected { send(ColorStreamDecoder(), "{\"basic\":{}}", "MAX_TOKENS") } }
    @Test fun rejectsBlockedPrompt() { val d = ColorStreamDecoder(); d.line("data: {\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"); rejected { d.line("") } }
    @Test fun rejectsLateInvalidParameters() {
        val d = ColorStreamDecoder(); assertNotNull(send(d, "{\"basic\":{},"))
        send(d, "\"rgb_correction\":{\"global\":[99,0,0]}}", "STOP")
        rejected { d.finish() }
    }
    @Test fun handlesCommentsMultilineEventsAndUsage() {
        val d = ColorStreamDecoder(); d.line(": ping"); d.line("")
        val payload = event("{\"basic\":{}}", "STOP")
        d.line("data: {"); d.line("data: " + payload.drop(1)); assertNotNull(d.line(""))
        d.line("data: {\"usageMetadata\":{}}"); d.line("")
        assertEquals(ColorPlan(), d.finish())
    }
    @Test fun doneDoesNotReplaceStop() { val d = ColorStreamDecoder(); d.line("data: [DONE]"); d.line(""); rejected { d.finish() } }
    @Test fun rejectsOversizedPayload() { rejected { ColorStreamDecoder().line("data: " + "x".repeat(256001)) } }
    @Test fun rejectsUnterminatedEvent() { val d = ColorStreamDecoder(); d.line("data: " + event("{\"basic\":{}}", "STOP")); rejected { d.finish() } }
}
