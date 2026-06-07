package com.yuquewatch.data.source

import com.yuquewatch.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * EXPERIMENTAL Cookie auto-renew. Replays 语雀's web password login to obtain a fresh session
 * cookie when the saved one expires. This is best-effort: 语雀 may encrypt the password (RSA)
 * or require a captcha / risk check, in which case it fails with a clear message and the user
 * must update the Cookie manually. Account + password are stored locally in plain prefs.
 */
class MiniAuth(private val config: AppConfig) {

    private val base = config.miniBase.trimEnd('/')
    private val ua = "Mozilla/5.0 (Linux; Android) YuqueWatch/1.0"
    private val jsonMedia = "application/json".toMediaType()

    /** Returns a fresh full Cookie string, or throws YuqueException on failure. */
    suspend fun relogin(): String = withContext(Dispatchers.IO) {
        val jar = LinkedHashMap<String, String>()

        // 1) Seed cookies (ctoken / csrf) from the login page.
        runCatching {
            Http.client.newCall(
                Request.Builder().url("$base/login").header("User-Agent", ua).get().build()
            ).execute().use { collectCookies(it, jar) }
        }
        val ctoken = jar["ctoken"] ?: randomCtoken().also { jar["ctoken"] = it }

        // 2) Submit credentials.
        val payload = buildString {
            append("{\"login\":").append(JsonPrimitive(config.account))
            append(",\"password\":").append(JsonPrimitive(config.password))
            append(",\"loginType\":\"password\"}")
        }
        val req = Request.Builder()
            .url("$base/api/accounts/login")
            .header("User-Agent", ua)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Referer", "$base/login")
            .header("x-csrf-token", ctoken)
            .header("Cookie", cookieString(jar))
            .post(payload.toRequestBody(jsonMedia))
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw YuqueException(
                    "自动续期登录失败（${resp.code}）：语雀可能需要验证码或加密登录，请手动更新 Cookie。",
                    code = resp.code,
                )
            }
            collectCookies(resp, jar)
        }

        if (jar.keys.none { it.contains("session") || it.startsWith("_yuque") }) {
            throw YuqueException("自动续期未拿到会话 Cookie，请手动更新 Cookie。")
        }
        cookieString(jar)
    }

    private fun collectCookies(resp: Response, jar: MutableMap<String, String>) {
        for (h in resp.headers("Set-Cookie")) {
            val pair = h.substringBefore(';')
            val name = pair.substringBefore('=').trim()
            val value = pair.substringAfter('=', "").trim()
            if (name.isNotEmpty() && value.isNotEmpty() && value != "deleted") jar[name] = value
        }
    }

    private fun cookieString(jar: Map<String, String>): String =
        jar.entries.joinToString("; ") { "${it.key}=${it.value}" }

    private fun randomCtoken(): String =
        (1..16).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
}
