package com.example.compositionhelper.color

import com.google.gson.JsonParser

/** SSE framing and complete top-level JSON prefixes; never repairs an unfinished value. */
class ColorStreamDecoder {
    private var event = StringBuilder()
    private var text = ""
    private var total = 0
    private var stopped = false
    private var previous: ColorPlan? = null
    val isComplete: Boolean get() = stopped

    fun line(line: String): ColorPlan? {
        total += line.toByteArray(Charsets.UTF_8).size + 1
        require(total <= 256000) { "模型响应过长" }
        if (line.isNotEmpty()) {
            if (line.startsWith("data:")) {
                if (event.isNotEmpty()) event.append('\n')
                event.append(line.substring(5).removePrefix(" "))
            }
            return null
        }
        if (event.isEmpty()) return null
        val payload = event.toString(); event = StringBuilder()
        if (payload == "[DONE]") return null
        val root = JsonParser.parseString(payload).asJsonObject
        check(!root.has("error") && root.getAsJsonObject("promptFeedback")?.has("blockReason") != true) { "AI 响应被拦截" }
        val candidates = root.getAsJsonArray("candidates") ?: return null
        val candidate = candidates.firstOrNull()?.asJsonObject ?: return null
        check(!stopped) { "完成标记后出现额外内容" }
        check(candidate.get("index")?.asInt ?: 0 == 0) { "AI 候选结果无效" }
        val parts = candidate.getAsJsonObject("content")?.getAsJsonArray("parts")
        text += parts?.map { it.asJsonObject }?.filter { it.get("thought")?.asBoolean != true }
            ?.joinToString("") { it.get("text")?.asString ?: "" } ?: ""
        require(text.toByteArray(Charsets.UTF_8).size <= 64000) { "调色方案过长" }
        candidate.get("finishReason")?.asString?.let {
            check(it == "STOP") { "AI 未返回完整方案" }; stopped = true
        }
        val partial = completePrefix(text)?.let { runCatching { ColorPlanCodec.decode(it) }.getOrNull() }
        if (partial == previous) return null
        previous = partial
        return partial
    }

    fun finish(): ColorPlan {
        check(event.isEmpty() && stopped) { "AI 响应中断，请重试" }
        // Gson's parser accepts some incomplete/lenient forms. Require a closed root first.
        check(closedRoot(text)) { "AI 方案不完整" }
        return ColorPlanCodec.decode(text)
    }

    companion object {
        fun completePrefix(text: String): String? {
            var depth = 0; var quoted = false; var escaped = false; var end = -1
            if (!text.trimStart().startsWith("{")) return null
            for ((i, c) in text.withIndex()) {
                if (quoted) {
                    if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
                } else when (c) {
                    '"' -> quoted = true
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) return if (c == '}' && text.substring(i + 1).isBlank()) text else null
                    }
                    ',' -> if (depth == 1) end = i
                }
            }
            return if (end >= 0) text.substring(0, end) + "}" else null
        }
        private fun closedRoot(text: String) = text.trimEnd().endsWith("}") && completePrefix(text) == text
    }
}
