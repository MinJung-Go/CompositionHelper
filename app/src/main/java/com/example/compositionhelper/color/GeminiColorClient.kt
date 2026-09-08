package com.example.compositionhelper.color

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Credentials intentionally live only in process memory, never in preferences, bundles or logs. */
object GeminiColorSession {
    var apiKey: String = ""
    var model: String = GeminiColorProtocol.DEFAULT_MODEL
}

object GeminiColorClient {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS).callTimeout(180, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    suspend fun analyze(bitmap: Bitmap, apiKey: String, model: String): ColorPlan = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank() && apiKey.all { it.code in 33..126 }) { "请填写有效的 API Key" }
        require(GeminiColorProtocol.validModel(model)) { "模型名称格式无效" }
        val data = ByteArrayOutputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) { "缩略图编码失败" }
            Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
        }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(GeminiColorProtocol.request(data).toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val body = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(IOException("网络连接失败或超时，请检查网络后重试"))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            check(it.isSuccessful) { GeminiColorProtocol.httpError(it.code) }
                            val bodySource = it.body?.source() ?: error("模型响应为空")
                            check(!bodySource.request(256001)) { "模型响应过长" }
                            bodySource.readUtf8()
                        }
                        continuation.resume(result)
                    } catch (e: Exception) {
                        // Do not surface raw remote payloads or request headers.
                        continuation.resumeWithException(IllegalStateException(
                            if (e is IllegalStateException) e.message else "读取模型响应失败，请重试"))
                    }
                }
            })
        }
        try { GeminiColorProtocol.response(body) }
        catch (e: Exception) { throw IllegalStateException("模型未返回有效调色方案，请重试或更换模型") }
    }
}
