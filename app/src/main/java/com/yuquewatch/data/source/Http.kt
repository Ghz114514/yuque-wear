package com.yuquewatch.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** Thrown for any non-2xx response or transport error, with a user-facing message. */
class YuqueException(
    message: String,
    val code: Int = 0,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Single shared OkHttp client. No GMS / WebView dependency. */
internal object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

/** Runs [request] on the IO dispatcher and returns the body string, or throws [YuqueException]. */
internal suspend fun OkHttpClient.string(request: Request): String = withContext(Dispatchers.IO) {
    val response: Response = try {
        newCall(request).execute()
    } catch (e: Exception) {
        throw YuqueException("网络请求失败：${e.message ?: "无法连接"}", cause = e)
    }
    response.use {
        // Surface the Yuque rate-limit budget for the debug toast (official api/v2 calls only).
        if (request.url.encodedPath.contains("/api/v2")) {
            val remaining = it.header("X-RateLimit-Remaining") ?: it.header("x-ratelimit-remaining")
            val limit = it.header("X-RateLimit-Limit") ?: it.header("x-ratelimit-limit")
            val text = when {
                remaining != null -> "语雀API剩余 $remaining" + (limit?.let { l -> "/$l" } ?: "")
                else -> "语雀API配额头未返回(HTTP ${it.code})"
            }
            ApiQuota.events.tryEmit(text)
        }
        val body = it.body?.string().orEmpty()
        ResponseLog.record(request.url.toString(), body)
        if (!it.isSuccessful) {
            val hint = when (it.code) {
                401 -> "认证失败（401）：Token/Cookie 无效或已过期"
                403 -> "无权限（403）：检查 Token 权限或知识库可见性"
                404 -> "未找到（404）：检查命名空间 / 接口路径"
                429 -> {
                    val retry = it.header("Retry-After")
                    "请求过于频繁（429）" + (retry?.let { s -> "，请约 ${s}s 后再试" } ?: "，请稍候再试")
                }
                else -> "请求失败（${it.code}）"
            }
            throw YuqueException(hint, code = it.code)
        }
        body
    }
}
