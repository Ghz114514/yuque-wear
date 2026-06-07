package com.yuquewatch.data.source

import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.Note
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * EXPERIMENTAL: private web "小记" endpoint via a browser Cookie.
 *
 * 语雀 does not publish a 小记 API; the path/shape can change. This client scans the response
 * for the first array of note-like objects and digs content out of many possible fields,
 * including lake-JSON / HTML bodies.
 */
class MiniNoteApi(private val config: AppConfig) {

    private val base = config.miniBase.trimEnd('/')
    private val path = config.miniNotesPath
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json".toMediaType()

    private val csrf: String? = Regex("ctoken=([^;]+)").find(config.cookie)?.groupValues?.get(1)

    private val contentKeys = listOf(
        "content", "body_asl", "body_draft_asl", "body", "body_html", "html",
        "description", "abstract", "summary", "word", "text", "content_html", "source",
    )
    private val textKeys = setOf("text", "title", "content", "value", "label", "name", "html", "insert")

    private fun req(url: String): Request.Builder {
        val b = Request.Builder()
            .url(url)
            .header("Cookie", config.cookie)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) YuqueWatch/1.0")
            .header("Referer", "$base/dashboard/notes")
            .header("x-requested-with", "XMLHttpRequest")
            .header("Accept", "application/json")
        csrf?.let { b.header("x-csrf-token", it) }
        return b
    }

    suspend fun list(limit: Int = 20): List<Note> {
        val sep = if (path.contains('?')) "&" else "?"
        val url = "$base$path${sep}limit=$limit&offset=0"
        val body = Http.client.string(req(url).get().build())
        val root = json.parseToJsonElement(body)
        // Real 语雀 shape: { "pin_notes": [...], "notes": [...] }. Pinned first.
        val objs = if (root is JsonObject && (root["notes"] is JsonArray || root["pin_notes"] is JsonArray)) {
            val pinned = (root["pin_notes"] as? JsonArray).orEmpty()
            val notes = (root["notes"] as? JsonArray).orEmpty()
            (pinned + notes).mapNotNull { it as? JsonObject }
        } else {
            (findNoteArray(root) ?: throw YuqueException(
                "未能解析小记列表。打开 设置→调试→查看原始响应记录，把 JSON 截图反馈即可修复字段。"
            )).mapNotNull { it as? JsonObject }
        }
        return objs.map { it.toNote() }
    }

    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()

    /** Raw response of the list endpoint, for the on-watch debug viewer. */
    suspend fun rawList(): String = Http.client.string(req("$base$path").get().build())

    suspend fun get(id: String): Note =
        list().firstOrNull { it.id == id } ?: throw YuqueException("未找到该小记")

    /**
     * Two-step like the web UI: POST create to obtain an id, then PUT update with the lake
     * content (abstract/html/source) — payload shape confirmed from a captured request.
     */
    suspend fun create(text: String): Note {
        val abstract = lakeAbstract(text)
        val html = lakeHtml(text)
        val wc = text.replace(Regex("\\s"), "").length

        fun contentObj() = buildJsonObject {
            put("abstract", abstract)
            put("html", html)
            put("source", abstract)
            put("word_count", wc)
            put("has_image", false)
            put("has_attachment", false)
            put("has_bookmark", false)
            put("save_type", "user")
            put("sync_dynamic_data", false)
            put("real_save_type", 4)
        }

        // 1) create -> id
        val createUrl = "$base${config.miniCreatePath.ifBlank { path }}"
        val createResp = runCatching {
            Http.client.string(req(createUrl).post(contentObj().toString().toRequestBody(jsonMedia)).build())
        }.getOrNull()
        val id = createResp?.let { findId(json.parseToJsonElement(it)) }

        // 2) update with content (the step that actually persists text)
        if (id != null) {
            val updatePayload = buildJsonObject {
                contentObj().forEach { (k, v) -> put(k, v) }
                put("id", id)
            }
            Http.client.string(
                req("$base${config.miniUpdatePath}")
                    .put(updatePayload.toString().toRequestBody(jsonMedia)).build()
            )
        } else {
            throw YuqueException("新建失败：未拿到小记 id。请在 设置→小记 校对新建/更新接口路径。")
        }

        val title = stripHtml(text).lineSequence().firstOrNull { it.isNotBlank() }?.take(40) ?: "小记"
        return Note(id = id.toString(), title = title, body = stripHtml(text), markdown = false)
    }

    private fun lakeAbstract(text: String): String {
        val ps = text.split("\n").joinToString("") {
            "<p><span>${escapeXml(it)}</span></p>"
        }
        return "<!doctype lake><meta name=\"doc-version\" content=\"1\" />" +
            "<meta name=\"viewport\" content=\"adapt\" />" +
            "<meta name=\"typography\" content=\"classic\" />$ps"
    }

    private fun lakeHtml(text: String): String {
        val ps = text.split("\n").joinToString("") {
            "<p class=\"ne-p\"><span class=\"ne-text\">${escapeXml(it)}</span></p>"
        }
        return "<div class=\"lake-content\" typography=\"classic\">$ps</div>"
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Recursively finds the first numeric "id" in a JSON tree. */
    private fun findId(el: JsonElement): Long? {
        when (el) {
            is JsonObject -> {
                (el["id"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()?.let { return it }
                el.values.forEach { findId(it)?.let { v -> return v } }
            }
            is JsonArray -> el.forEach { findId(it)?.let { v -> return v } }
            else -> {}
        }
        return null
    }

    suspend fun delete(id: String) {
        // Confirmed: DELETE batchDelete with body {"ids":[<id>]}.
        val numericId = id.toLongOrNull()
        val payload = buildJsonObject {
            put("ids", kotlinx.serialization.json.buildJsonArray {
                if (numericId != null) add(kotlinx.serialization.json.JsonPrimitive(numericId))
                else add(kotlinx.serialization.json.JsonPrimitive(id))
            })
        }
        Http.client.string(
            req("$base${config.miniDeletePath}")
                .delete(payload.toString().toRequestBody(jsonMedia)).build()
        )
    }

    // ---- parsing ----

    private fun JsonObject.toNote(): Note {
        val id = str("id", "note_id", "noteId", "uuid", "_id") ?: System.nanoTime().toString()
        val raw = firstContent(this)
        val plain = normalize(raw)
        val title = plain.lineSequence().firstOrNull { it.isNotBlank() }?.take(40) ?: "（无内容）"
        val timeRaw = str("created_at", "created", "content_updated_at", "updated_at", "publish_time")
        return Note(
            id = id,
            title = title,
            body = plain.ifBlank { "（无内容）" },
            updatedAt = formatTime(timeRaw),
            markdown = false,
            images = extractImages(raw),
        )
    }

    /** Best-effort image URLs from lake/HTML: <img src>, markdown ![](), and bare image URLs. */
    private fun extractImages(raw: String): List<String> {
        val out = LinkedHashSet<String>()
        Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(raw).forEach { out.add(it.groupValues[1]) }
        Regex("!\\[[^\\]]*]\\(([^)\\s]+)").findAll(raw).forEach { out.add(it.groupValues[1]) }
        Regex("https?://[^\\s\"'<>)]+\\.(?:png|jpe?g|gif|webp)", RegexOption.IGNORE_CASE)
            .findAll(raw).forEach { out.add(it.value) }
        return out.filter { it.startsWith("http") }.take(20)
    }

    /** Pull the note text. 语雀 nests it as content.abstract (lake HTML); also try flat fields. */
    private fun firstContent(o: JsonObject): String {
        // Preferred: nested content object → abstract / description / body_asl.
        (o["content"] as? JsonObject)?.let { c ->
            for (k in listOf("abstract", "description", "body_asl", "body", "text")) {
                val s = (c[k] as? JsonPrimitive)?.contentOrNull
                if (!s.isNullOrBlank()) return s
            }
        }
        // Fallbacks for other shapes.
        for (k in contentKeys) {
            val v = o[k]
            if (v is JsonPrimitive && v.isString) {
                val s = v.contentOrNull
                if (!s.isNullOrBlank()) return s
            } else if (v is JsonObject || v is JsonArray) {
                val out = LinkedHashSet<String>()
                v?.let { collect(it, out) }
                if (out.isNotEmpty()) return out.joinToString("\n")
            }
        }
        return ""
    }

    private fun normalize(raw: String): String {
        val t = raw.trimStart()
        return if (t.startsWith("{") || t.startsWith("[")) {
            runCatching {
                val out = LinkedHashSet<String>()
                collect(json.parseToJsonElement(raw), out)
                out.joinToString("\n")
            }.getOrDefault(stripHtml(raw))
        } else {
            stripHtml(raw)
        }
    }

    private fun collect(el: JsonElement, out: MutableSet<String>) {
        when (el) {
            is JsonObject -> el.forEach { (k, v) ->
                if (v is JsonPrimitive) {
                    if (k in textKeys && v.isString) v.contentOrNull?.let { s ->
                        val x = stripHtml(s)
                        if (x.isNotBlank()) out.add(x)
                    }
                } else collect(v, out)
            }
            is JsonArray -> el.forEach { collect(it, out) }
            else -> {}
        }
    }

    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val v = this[k]
            if (v is JsonPrimitive) v.contentOrNull?.let { if (it.isNotBlank() && it != "null") return it }
        }
        return null
    }

    private fun findNoteArray(el: JsonElement): JsonArray? {
        when (el) {
            is JsonArray -> {
                if (el.any { it is JsonObject && looksLikeNote(it) }) return el
                el.forEach { child -> findNoteArray(child)?.let { return it } }
            }
            is JsonObject -> el.values.forEach { child -> findNoteArray(child)?.let { return it } }
            else -> {}
        }
        return null
    }

    private fun looksLikeNote(o: JsonObject): Boolean {
        val hasId = listOf("id", "note_id", "noteId", "uuid", "_id").any { o.containsKey(it) }
        val hasContent = contentKeys.any { o.containsKey(it) }
        return hasId && hasContent
    }

    /** ISO-8601 or epoch(ms/s) → local "MM-dd HH:mm" (minute precision). */
    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val zone = ZoneId.systemDefault()
        val instant = runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(raw) }.getOrNull()
            ?: raw.toLongOrNull()?.let { Instant.ofEpochMilli(if (it < 1_000_000_000_000L) it * 1000 else it) }
            ?: return raw.take(16)
        return DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(zone).format(instant)
    }

    private fun stripHtml(s: String): String =
        s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</div>|</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .lines().joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private fun textToHtml(s: String): String =
        s.split("\n").joinToString("") { "<p>${it.ifEmpty { "<br>" }}</p>" }
}
