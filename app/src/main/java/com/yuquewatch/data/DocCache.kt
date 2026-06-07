package com.yuquewatch.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Simple persistent disk cache for offline reading: the knowledge-base TOC trees and the
 * documents the user has actually opened. Stored under filesDir so it survives restarts and
 * is only removed by the manual "清理缓存". Does NOT bulk-cache whole repos.
 */
class DocCache(context: Context) {

    private val dir = File(context.filesDir, "doc_cache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    fun saveRepos(login: String, repos: List<Repo>) = runCatching {
        val dto = repos.map { RepoDto(it.id, it.namespace, it.name, it.docCount) }
        file("repos_$login").writeText(json.encodeToString(ReposDto.serializer(), ReposDto(dto)))
    }.let { Unit }

    fun loadRepos(login: String): List<Repo>? = runCatching {
        val f = file("repos_$login")
        if (!f.exists()) return null
        json.decodeFromString(ReposDto.serializer(), f.readText()).items
            .map { Repo(it.id, it.namespace, it.name, it.docCount) }
    }.getOrNull()

    fun saveTree(namespace: String, tree: List<DocTreeItem>) = runCatching {
        val dto = tree.map {
            when (it) {
                is DocTreeItem.Folder -> NodeDto("TITLE", it.title, "", it.depth)
                is DocTreeItem.Doc -> NodeDto("DOC", it.title, it.slug, it.depth)
            }
        }
        file("tree_$namespace").writeText(json.encodeToString(TreeDto.serializer(), TreeDto(dto)))
    }

    fun loadTree(namespace: String): List<DocTreeItem>? = runCatching {
        val f = file("tree_$namespace")
        if (!f.exists()) return null
        json.decodeFromString(TreeDto.serializer(), f.readText()).items.map {
            if (it.type == "TITLE") DocTreeItem.Folder(it.title, it.depth)
            else DocTreeItem.Doc(it.title, it.slug, it.depth)
        }
    }.getOrNull()

    fun saveNote(key: String, note: Note) = runCatching {
        val dto = NoteDto(
            note.id, note.title, note.body.orEmpty(), note.updatedAt, note.markdown,
            note.links.map { RefDto(it.namespace, it.slug, it.title) }, note.images,
        )
        file("note_$key").writeText(json.encodeToString(NoteDto.serializer(), dto))
    }

    fun loadNote(key: String): Note? = runCatching {
        val f = file("note_$key")
        if (!f.exists()) return null
        val d = json.decodeFromString(NoteDto.serializer(), f.readText())
        Note(
            id = d.id, title = d.title, body = d.body, updatedAt = d.updatedAt, markdown = d.markdown,
            links = d.links.map { DocRef(it.namespace, it.slug, it.title) },
            images = d.images, fromCache = true,
        )
    }.getOrNull()

    fun clear() = runCatching { dir.deleteRecursively(); dir.mkdirs() }.let { Unit }

    private fun file(name: String) = File(dir, name.replace(Regex("[^A-Za-z0-9_]"), "_") + ".json")

    @Serializable private data class RepoDto(val id: Long, val namespace: String, val name: String, val docCount: Int)
    @Serializable private data class ReposDto(val items: List<RepoDto>)
    @Serializable private data class TreeDto(val items: List<NodeDto>)
    @Serializable private data class NodeDto(val type: String, val title: String, val slug: String, val depth: Int)
    @Serializable private data class RefDto(val namespace: String, val slug: String, val title: String)
    @Serializable private data class NoteDto(
        val id: String, val title: String, val body: String, val updatedAt: String,
        val markdown: Boolean, val links: List<RefDto>, val images: List<String>,
    )
}
