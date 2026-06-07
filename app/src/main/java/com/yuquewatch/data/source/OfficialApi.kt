package com.yuquewatch.data.source

import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.DocRef
import com.yuquewatch.data.DocTreeItem
import com.yuquewatch.data.Note
import com.yuquewatch.data.Repo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Official Yuque Open API. Lists every knowledge base (repo) the token can see, then the
 * docs inside each — grouped by the knowledge base's folder structure (TOC). Non-document
 * types (Board/Mind/Sheet) and embedded tables are flattened to readable text.
 * Docs: https://www.yuque.com/yuque/developer/api
 */
class OfficialApi(private val config: AppConfig) {

    private val base = config.officialBase.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json".toMediaType()

    private fun req(url: String) = Request.Builder()
        .url(url)
        .header("X-Auth-Token", config.token)
        .header("User-Agent", "YuqueWatch/1.0")
        .header("Content-Type", "application/json")

    data class UserInfo(val login: String, val name: String, val avatarUrl: String)

    /** One round-trip for login + display name + avatar (callers cache it). */
    suspend fun userInfo(): UserInfo {
        val body = Http.client.string(req("$base/user").get().build())
        val u = json.decodeFromString<UserResponse>(body).data
        return UserInfo(login = u.login, name = u.name.ifBlank { u.login }, avatarUrl = u.avatarUrl)
    }

    /** All knowledge bases. Reuses the cached login if available (no extra /user call). */
    suspend fun listRepos(login: String): List<Repo> {
        val resolved = login.ifBlank { userInfo().login }
        val body = Http.client.string(req("$base/users/$resolved/repos").get().build())
        return json.decodeFromString<RepoListResponse>(body).data
            .map { Repo(id = it.id, namespace = it.namespace, name = it.name.ifBlank { it.namespace }) }
            .sortedBy { it.name }
    }

    /**
     * The knowledge base's table of contents: folder headers + docs in display order.
     * Lightweight (titles only), so it stays fast even for large repos. Falls back to a
     * flat doc list if the TOC is empty.
     */
    suspend fun docTree(namespace: String): List<DocTreeItem> {
        val ns = namespace.trim().trim('/')
        val tocBody = runCatching {
            Http.client.string(req("$base/repos/$ns/toc").get().build())
        }.getOrNull()

        val tree = tocBody?.let { body ->
            json.decodeFromString<TocResponse>(body).data.mapNotNull { node ->
                when (node.type.uppercase()) {
                    "TITLE" -> node.title.takeIf { it.isNotBlank() }
                        ?.let { DocTreeItem.Folder(it, node.depth.coerceAtLeast(0)) }
                    "DOC" -> if (node.slug.isNotBlank() && node.slug != "#")
                        DocTreeItem.Doc(node.title.ifBlank { "无标题" }, node.slug, node.depth.coerceAtLeast(0))
                    else null
                    else -> null
                }
            }
        }.orEmpty()

        if (tree.any { it is DocTreeItem.Doc }) return tree

        // Fallback: flat list of docs.
        val body = Http.client.string(req("$base/repos/$ns/docs").get().build())
        return json.decodeFromString<DocListResponse>(body).data
            .sortedByDescending { it.updatedAt }
            .map { DocTreeItem.Doc(it.title.ifBlank { "无标题" }, it.slug, 0) }
    }

    /** Best-effort doc search. Result urls are parsed into namespace/slug refs. */
    suspend fun search(q: String): List<DocRef> {
        val enc = java.net.URLEncoder.encode(q, "UTF-8")
        val body = Http.client.string(req("$base/search?q=$enc&type=doc").get().build())
        val arr = (json.parseToJsonElement(body) as? JsonObject)?.get("data") as? JsonArray
            ?: return emptyList()
        val path = Regex("/([\\w.-]+)/([\\w.-]+)/([\\w.-]+)")
        val out = LinkedHashMap<String, DocRef>()
        for (el in arr) {
            val o = el as? JsonObject ?: continue
            val title = (o["title"] as? JsonPrimitive)?.contentOrNull
                ?.replace(Regex("</?em>"), "")?.ifBlank { null } ?: "(无标题)"
            val urlStr = listOf("url", "web_url", "path").firstNotNullOfOrNull {
                (o[it] as? JsonPrimitive)?.contentOrNull
            } ?: o.toString()
            val m = path.find(urlStr.substringBefore('#').substringBefore('?')) ?: continue
            val (g, b, s) = m.destructured
            out.putIfAbsent("$g/$b/$s", DocRef("$g/$b", s, title.take(40)))
        }
        return out.values.toList().take(30)
    }

    /** Full doc with reading-cleaned body and extracted in-repo doc links. */
    suspend fun getDoc(namespace: String, slug: String): Note {
        val ns = namespace.trim().trim('/')
        // No raw=1 → `body` comes back as Markdown (raw=1 returns lake/HTML source).
        val body = Http.client.string(req("$base/repos/$ns/docs/$slug").get().build())
        val d = json.decodeFromString<DocDetailResponse>(body).data
        val raw = d.body ?: d.bodyDraft ?: ""
        val isDoc = d.type.equals("Doc", true) || d.format.equals("markdown", true)
        return if (isDoc) {
            Note(d.slug, d.title.ifBlank { "无标题" }, raw, d.updatedAt, markdown = true, links = extractRefs(raw))
        } else {
            Note(d.slug, d.title.ifBlank { "无标题" }, cleanToText(raw), d.updatedAt, markdown = false)
        }
    }

    suspend fun createDoc(namespace: String, text: String): Note {
        val ns = namespace.trim().trim('/')
        val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(40) ?: "小记"
        val slug = "n" + System.currentTimeMillis().toString(36)
        val payload = CreateRequest(title = title, slug = slug, body = text)
        val rb = json.encodeToString(CreateRequest.serializer(), payload).toRequestBody(jsonMedia)
        val body = Http.client.string(req("$base/repos/$ns/docs").post(rb).build())
        val d = json.decodeFromString<DocDetailResponse>(body).data
        return Note(d.slug, d.title, text, d.updatedAt, markdown = true)
    }

    suspend fun updateDoc(namespace: String, slug: String, text: String): Note {
        val ns = namespace.trim().trim('/')
        val getBody = Http.client.string(req("$base/repos/$ns/docs/$slug").get().build())
        val id = json.decodeFromString<DocDetailResponse>(getBody).data.id
        val payload = UpdateRequest(body = text)
        val rb = json.encodeToString(UpdateRequest.serializer(), payload).toRequestBody(jsonMedia)
        Http.client.string(req("$base/repos/$ns/docs/$id").put(rb).build())
        return getDoc(ns, slug)
    }

    suspend fun deleteDoc(namespace: String, slug: String) {
        val ns = namespace.trim().trim('/')
        val body = Http.client.string(req("$base/repos/$ns/docs/$slug").get().build())
        val id = json.decodeFromString<DocDetailResponse>(body).data.id
        Http.client.string(req("$base/repos/$ns/docs/$id").delete().build())
    }

    // ---- doc-link extraction (#4) ----

    /** Finds links to other 语雀 docs: [text](.../group/book/slug). */
    private fun extractRefs(markdown: String): List<DocRef> {
        val link = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
        val path = Regex("(?:https?://[^/]*yuque\\.com)?/([\\w.-]+)/([\\w.-]+)/([\\w.-]+)")
        val seen = LinkedHashMap<String, DocRef>()
        for (m in link.findAll(markdown)) {
            val text = m.groupValues[1]
            val url = m.groupValues[2].substringBefore('#').substringBefore('?')
            // Only in-Yuque links: a yuque.com URL or a site-relative path.
            if (!url.contains("yuque.com") && !url.startsWith("/")) continue
            val p = path.find(url) ?: continue
            val (group, book, slug) = p.destructured
            if (group.isBlank() || book.isBlank() || slug.isBlank()) continue
            val ns = "$group/$book"
            seen.putIfAbsent("$ns/$slug", DocRef(ns, slug, text.take(40)))
        }
        return seen.values.toList().take(20)
    }

    // ---- content flattening for Board / Mind / Sheet ----

    private val textKeys = setOf(
        "text", "title", "content", "label", "name", "value", "plainText", "html", "abstract",
    )

    private fun cleanToText(raw: String): String {
        val trimmed = raw.trimStart()
        val collected = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching {
                val out = LinkedHashSet<String>()
                collect(json.parseToJsonElement(raw), out)
                out.joinToString("\n")
            }.getOrDefault(stripHtml(raw))
        } else {
            stripHtml(raw)
        }
        return collected.ifBlank { "（该内容无可提取的文字）" }
    }

    private fun collect(el: JsonElement, out: MutableSet<String>) {
        when (el) {
            is JsonObject -> el.forEach { (k, v) ->
                if (v is JsonPrimitive) {
                    if (k in textKeys && v.isString) v.contentOrNull?.let { s ->
                        val t = stripHtml(s)
                        if (t.isNotBlank() && t.length in 1..2000) out.add(t)
                    }
                } else {
                    collect(v, out)
                }
            }
            is JsonArray -> el.forEach { collect(it, out) }
            else -> {}
        }
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

    // ---- DTOs ----

    @Serializable private data class UserResponse(val data: User)
    @Serializable private data class User(
        val id: Long = 0, val login: String = "", val name: String = "",
        @SerialName("avatar_url") val avatarUrl: String = "",
    )

    @Serializable private data class RepoListResponse(val data: List<RepoDto> = emptyList())
    @Serializable private data class RepoDto(
        val id: Long = 0, val name: String = "", val namespace: String = "",
    )

    @Serializable private data class TocResponse(val data: List<TocNode> = emptyList())
    @Serializable private data class TocNode(
        val type: String = "", val title: String = "", val slug: String = "",
        val depth: Int = 0,
    )

    @Serializable private data class DocListResponse(val data: List<DocSummary> = emptyList())
    @Serializable private data class DocSummary(
        val id: Long = 0, val slug: String = "", val title: String = "",
        @SerialName("updated_at") val updatedAt: String = "",
    )

    @Serializable private data class DocDetailResponse(val data: DocDetail)
    @Serializable private data class DocDetail(
        val id: Long = 0, val slug: String = "", val title: String = "",
        val type: String = "", val format: String = "",
        val body: String? = null,
        @SerialName("body_draft") val bodyDraft: String? = null,
        @SerialName("updated_at") val updatedAt: String = "",
    )

    @Serializable private data class CreateRequest(
        val title: String, val slug: String, val body: String,
        val format: String = "markdown", val public: Int = 0,
    )

    @Serializable private data class UpdateRequest(
        val body: String, val format: String = "markdown",
    )
}
