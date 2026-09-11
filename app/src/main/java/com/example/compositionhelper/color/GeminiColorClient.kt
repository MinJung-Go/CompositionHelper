package com.example.compositionhelper.color

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Credentials intentionally live only in process memory, never in preferences, bundles or logs. */
object GeminiColorSession {
    var apiKey: String = ""
    var model: String = GeminiColorProtocol.GLM_MODEL
}

object GeminiColorClient {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS).callTimeout(180, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    private val glmClient = client.newBuilder().readTimeout(300, TimeUnit.SECONDS)
        .callTimeout(360, TimeUnit.SECONDS).build()

    suspend fun analyze(bitmap: Bitmap, apiKey: String, model: String, onPreview: suspend (ColorPlan) -> Unit = {}): ColorPlan = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank() && apiKey.all { it.code in 33..126 }) { "请填写有效的 API Key" }
        require(GeminiColorProtocol.supportedModel(model)) { "模型名称格式无效" }
        val data = ByteArrayOutputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) { "缩略图编码失败" }
            Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
        }
        val glm = model.startsWith("glm-")
        val request = Request.Builder()
            .url(if (glm) "https://open.bigmodel.cn/api/paas/v4/chat/completions" else "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse")
            .header(if (glm) "Authorization" else "x-goog-api-key", if (glm) "Bearer $apiKey" else apiKey)
            .post((if (glm) GeminiColorProtocol.glmRequest(data, model) else GeminiColorProtocol.request(data)).toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        var result: ColorPlan? = null
        callbackFlow {
            val call = (if (glm) glmClient else client).newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    close(IOException("网络连接失败或超时，请重试"))
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val message = GeminiColorProtocol.httpError(response.code)
                        response.close(); close(IOException(message)); return
                    }
                    try {
                        response.use {
                            val source = it.body?.source() ?: error("模型响应为空")
                            val decoder = ColorStreamDecoder(glm)
                            while (!source.exhausted()) {
                                val line = source.readUtf8LineStrict(256000)
                                decoder.line(line)?.let { plan -> if (!decoder.isComplete) trySend(plan to false) }
                            }
                            trySend(decoder.finish() to true)
                            close()
                        }
                    } catch (e: Exception) {
                        close(IOException("AI 响应中断或方案无效，请重试"))
                    }
                }
            })
            awaitClose { call.cancel() }
        }.buffer(Channel.CONFLATED).collect { (plan, final) ->
            if (final) result = plan else onPreview(plan)
        }
        result ?: error("AI 未返回完整方案")
    }
}
