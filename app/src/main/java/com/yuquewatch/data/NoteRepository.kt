package com.yuquewatch.data

import com.yuquewatch.data.source.MiniAuth
import com.yuquewatch.data.source.MiniNoteApi
import com.yuquewatch.data.source.OfficialApi
import com.yuquewatch.data.source.YuqueException

/** Thin orchestration over the two API clients; rebuilds them from the latest config. */
class NoteRepository(private val settings: SettingsStore, private val cache: DocCache) {

    private fun official() = OfficialApi(settings.config.value)
    private fun mini() = MiniNoteApi(settings.config.value)

    /**
     * Runs a mini call; on a 401 (expired Cookie) with stored credentials, auto-renews the
     * Cookie once and retries. Otherwise rethrows.
     */
    private suspend fun <T> withMiniAuth(block: suspend () -> T): T = try {
        block()
    } catch (e: YuqueException) {
        val c = settings.config.value
        if (e.code == 401 && c.account.isNotBlank() && c.password.isNotBlank()) {
            val fresh = MiniAuth(c).relogin()
            settings.update(c.copy(cookie = fresh))
            block()
        } else throw e
    }

    // ---- official ----
    suspend fun userInfo(): OfficialApi.UserInfo = official().userInfo()
    suspend fun listRepos(login: String): List<Repo> = try {
        official().listRepos(login).also { cache.saveRepos(login, it) }
    } catch (e: Exception) {
        cache.loadRepos(login) ?: throw e
    }
    /** Online → fetch + cache; offline/error → fall back to cached tree if present. */
    suspend fun docTree(namespace: String): List<DocTreeItem> = try {
        official().docTree(namespace).also { cache.saveTree(namespace, it) }
    } catch (e: Exception) {
        cache.loadTree(namespace) ?: throw e
    }

    suspend fun getDoc(namespace: String, slug: String): Note = try {
        official().getDoc(namespace, slug).also { cache.saveNote("$namespace/$slug", it) }
    } catch (e: Exception) {
        cache.loadNote("$namespace/$slug") ?: throw e
    }
    suspend fun createDoc(namespace: String, text: String): Note = official().createDoc(namespace, text)
    suspend fun updateDoc(namespace: String, slug: String, text: String): Note =
        official().updateDoc(namespace, slug, text).also { cache.saveNote("$namespace/$slug", it) }
    suspend fun deleteDoc(namespace: String, slug: String) = official().deleteDoc(namespace, slug)
    suspend fun searchDocs(q: String): List<DocRef> = official().search(q)

    // ---- mini ----
    suspend fun listMini(limit: Int = 20): List<Note> = withMiniAuth { mini().list(limit) }
    suspend fun getMini(id: String): Note = withMiniAuth { mini().get(id) }
    suspend fun createMini(text: String): Note = withMiniAuth { mini().create(text) }
    suspend fun deleteMini(id: String) = withMiniAuth { mini().delete(id) }
    suspend fun miniRaw(): String = withMiniAuth { mini().rawList() }
}
