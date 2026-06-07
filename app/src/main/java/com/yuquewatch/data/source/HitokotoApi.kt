package com.yuquewatch.data.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/** Fetches a short quote from hitokoto.cn (一言). Independent of Yuque, so no 429 impact. */
object HitokotoApi {
    private val json = Json { ignoreUnknownKeys = true }

    /** [types] = comma letters like "a,d,i" (categories); blank = any. Returns sentence or null. */
    suspend fun fetch(types: String): String? {
        val params = types.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .joinToString("") { "&c=$it" }
        val url = "https://v1.hitokoto.cn/?encode=json$params"
        val body = Http.client.string(
            Request.Builder().url(url).header("User-Agent", "YuqueWatch/1.0").get().build()
        )
        val obj = json.parseToJsonElement(body) as? JsonObject ?: return null
        return obj["hitokoto"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    }
}
