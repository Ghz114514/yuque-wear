package com.yuquewatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.DocRef
import com.yuquewatch.data.DocTreeItem
import com.yuquewatch.data.Note
import com.yuquewatch.data.NoteRepository
import com.yuquewatch.data.Repo
import com.yuquewatch.data.SettingsStore
import com.yuquewatch.data.source.HitokotoApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Generic async state for a screen. */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Error(val message: String) : Resource<Nothing>
    data class Content<T>(val data: T) : Resource<T>
}

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    private val repo = NoteRepository(settings, com.yuquewatch.data.DocCache(app))

    val config: StateFlow<AppConfig> = settings.config

    init {
        com.yuquewatch.data.source.ResponseLog.enabled = settings.config.value.recordResponses
    }

    // ---- 快记 ----
    fun loadQuick() {
        val ns = settings.config.value.quickRepoNamespace
        if (ns.isBlank()) { quickNotes.value = Resource.Error("未设置快记知识库，请到 设置→个性化 选择"); return }
        load(quickNotes) {
            repo.docTree(ns).filterIsInstance<DocTreeItem.Doc>().map { Note(id = it.slug, title = it.title) }
        }
    }

    // ---- favorites ----
    fun isFavorite(ns: String, slug: String) = settings.isFavorite(ns, slug)
    fun toggleFavorite(ref: com.yuquewatch.data.DocRef): Boolean {
        val now = settings.toggleFavorite(ref)
        favorites.value = settings.favorites()
        return now
    }

    /** Called before navigating to the editor; null = 小记, else official namespace. */
    fun prepareCreate(namespace: String?) { createTarget = namespace }

    private val _greeting = MutableStateFlow(greet(settings.config.value.userName))
    val greeting: StateFlow<String> = _greeting

    val repos = MutableStateFlow<Resource<List<Repo>>>(Resource.Loading)
    val miniNotes = MutableStateFlow<Resource<List<Note>>>(Resource.Loading)
    val docs = MutableStateFlow<Resource<List<DocTreeItem>>>(Resource.Loading)

    /** 快记 tab: docs inside the configured quick-record repo, shown as a flat list. */
    val quickNotes = MutableStateFlow<Resource<List<Note>>>(Resource.Loading)

    /** Local favorites (官方无公开收藏接口). */
    val favorites = MutableStateFlow(settings.favorites())

    /** Recently opened official docs (local). */
    val recents = MutableStateFlow(settings.recents())

    /** Search results. */
    val searchResults = MutableStateFlow<Resource<List<com.yuquewatch.data.DocRef>>>(Resource.Content(emptyList()))

    /** Create target: null = 小记, else official namespace (快记/知识库). */
    private var createTarget: String? = null

    /** 一言 sentence under the greeting (null = hidden). */
    val hitokoto = MutableStateFlow<String?>(null)
    val hitokotoLoading = MutableStateFlow(false)

    /** Whether more mini notes may exist (show "加载更多"). */
    val miniHasMore = MutableStateFlow(false)
    private var miniLimit = 20

    /** Debug: online API reachability test result. */
    val apiTest = MutableStateFlow<Resource<String>>(Resource.Content(""))

    private val _currentRepo = MutableStateFlow<Repo?>(null)
    val currentRepo: StateFlow<Repo?> = _currentRepo

    /** Selected home tab — held here so it survives navigating into/out of a repo. */
    val homeTab = MutableStateFlow(initialHomeTab())
    fun setHomeTab(t: com.yuquewatch.data.HomeTab) { homeTab.value = t }
    private fun initialHomeTab(): com.yuquewatch.data.HomeTab {
        val cfg = settings.config.value
        return when {
            cfg.defaultTab == com.yuquewatch.data.HomeTab.QUICK && cfg.noteMode != com.yuquewatch.data.NoteMode.MINI ->
                com.yuquewatch.data.HomeTab.QUICK
            cfg.defaultTab == com.yuquewatch.data.HomeTab.MINI && cfg.noteMode != com.yuquewatch.data.NoteMode.QUICK ->
                com.yuquewatch.data.HomeTab.MINI
            else -> com.yuquewatch.data.HomeTab.MINE
        }
    }

    // ---- caching / throttling (keeps us well under Yuque's rate limit) ----
    private val treeCache = HashMap<String, List<DocTreeItem>>()
    private var lastReposFetch = 0L
    private var lastMiniFetch = 0L
    private var nameTried = false
    private val throttleMs = 30_000L

    /** Call once on launch: greeting/name + 一言 + due auto cache-clear. */
    fun start() {
        ensureName()
        loadHitokoto()
        maybeAutoClear()
    }

    private fun maybeAutoClear() {
        val cfg = settings.config.value
        val now = System.currentTimeMillis()
        val due = when (cfg.autoClear) {
            com.yuquewatch.data.AutoClear.WEEKLY -> now - cfg.lastCacheClear > 7L * 86_400_000
            com.yuquewatch.data.AutoClear.MONTHLY -> now - cfg.lastCacheClear > 30L * 86_400_000
            else -> false
        }
        if (!due) return
        val app = getApplication<android.app.Application>()
        runCatching { java.io.File(app.filesDir, "doc_cache").deleteRecursively() }
        runCatching { coil.Coil.imageLoader(app).let { it.diskCache?.clear(); it.memoryCache?.clear() } }
        treeCache.clear()
        settings.update(settings.config.value.copy(lastCacheClear = now))
    }

    /** 我的 tab: knowledge bases. */
    fun ensureRepos(force: Boolean = false) {
        ensureName()
        val cfg = settings.config.value
        if (!cfg.officialReady) { repos.value = Resource.Error("未配置官方 Token，请到设置填写"); return }
        if (!force && repos.value is Resource.Content && System.currentTimeMillis() - lastReposFetch < throttleMs) return
        lastReposFetch = System.currentTimeMillis()
        load(repos) { repo.listRepos(cachedLogin()) }
    }

    /** 小记 tab. */
    fun ensureMini(force: Boolean = false) {
        val cfg = settings.config.value
        if (!cfg.miniReady) { miniNotes.value = Resource.Error("未配置小记 Cookie，请到设置填写"); return }
        if (!force && miniNotes.value is Resource.Content && System.currentTimeMillis() - lastMiniFetch < throttleMs) return
        lastMiniFetch = System.currentTimeMillis()
        loadMini()
    }

    /** Fetches the display name at most once per process (avoids repeated /user → 429). */
    private fun ensureName() {
        val cfg = settings.config.value
        if (nameTried || !cfg.officialReady || cfg.userName.isNotBlank()) return
        nameTried = true
        viewModelScope.launch { runCatching { cachedLogin() } }
    }

    /** Returns the user login, calling /user at most once and caching login + name. */
    private suspend fun cachedLogin(): String {
        val cfg = settings.config.value
        if (cfg.login.isNotBlank()) return cfg.login
        val info = repo.userInfo()
        settings.update(settings.config.value.copy(
            login = info.login, userName = info.name, avatarUrl = info.avatarUrl,
        ))
        _greeting.value = greet(info.name)
        return info.login
    }

    // ---- drill-down (cached) ----

    fun openRepo(r: Repo) {
        _currentRepo.value = r
        treeCache[r.namespace]?.let { docs.value = Resource.Content(it); return }
        docs.value = Resource.Loading
        viewModelScope.launch {
            docs.value = try {
                val tree = repo.docTree(r.namespace)
                treeCache[r.namespace] = tree
                Resource.Content(tree)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "加载失败")
            }
        }
    }

    fun reloadRepo() {
        _currentRepo.value?.let { treeCache.remove(it.namespace); openRepo(it) }
    }

    /**
     * Loads one note for a detail screen. [ns] null = mini note (by id), else official doc.
     * Each detail back-stack entry calls this with its OWN identity, so navigating back shows
     * the correct doc (state is held per-entry via produceState, not shared).
     */
    suspend fun fetchNote(ns: String?, idOrSlug: String): Note {
        if (ns.isNullOrBlank()) return repo.getMini(idOrSlug)
        val note = repo.getDoc(ns, idOrSlug)
        settings.addRecent(com.yuquewatch.data.DocRef(ns, idOrSlug, note.title))
        recents.value = settings.recents()
        return note
    }

    fun clearRecents() { settings.clearRecents(); recents.value = emptyList() }

    fun search(q: String) {
        if (q.isBlank()) { searchResults.value = Resource.Content(emptyList()); return }
        load(searchResults) { repo.searchDocs(q) }
    }

    // ---- create / delete ----

    fun create(text: String, onResult: (String?) -> Unit) {
        if (text.isBlank()) { onResult("内容为空"); return }
        viewModelScope.launch {
            try {
                val ns = createTarget
                if (ns != null) {
                    repo.createDoc(ns, text)
                    treeCache.remove(ns)
                    if (ns == settings.config.value.quickRepoNamespace) loadQuick()
                } else {
                    repo.createMini(text)
                    ensureMini(force = true)
                }
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "保存失败")
            }
        }
    }

    /** Edits an existing official doc. */
    fun updateDoc(ns: String, slug: String, text: String, onResult: (String?) -> Unit) {
        if (text.isBlank()) { onResult("内容为空"); return }
        viewModelScope.launch {
            try {
                repo.updateDoc(ns, slug, text)
                treeCache.remove(ns)
                if (ns == settings.config.value.quickRepoNamespace) loadQuick()
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "保存失败")
            }
        }
    }

    /** Fetch raw doc body (Markdown) for the editor. */
    suspend fun docBody(ns: String, slug: String): String = repo.getDoc(ns, slug).body.orEmpty()

    /** Deletes a note. [ns] null = mini note, else official doc in that namespace. */
    fun deleteEntry(ns: String?, id: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                if (ns.isNullOrBlank()) {
                    repo.deleteMini(id)
                    ensureMini(force = true)
                } else {
                    repo.deleteDoc(ns, id)
                    treeCache.remove(ns)
                    _currentRepo.value?.let { if (it.namespace == ns) reloadRepo() }
                }
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "删除失败")
            }
        }
    }

    private fun loadMini() {
        miniNotes.value = Resource.Loading
        viewModelScope.launch {
            try {
                val list = repo.listMini(miniLimit)
                miniHasMore.value = list.size >= miniLimit
                val shown = if (settings.config.value.hideUnviewable) list.filter { viewable(it) } else list
                miniNotes.value = Resource.Content(shown)
            } catch (e: Exception) {
                miniNotes.value = Resource.Error(e.message ?: "加载失败")
            }
        }
    }

    /** A note is viewable if it has real text (not empty / not the placeholder). */
    private fun viewable(n: Note): Boolean {
        val b = n.body?.trim().orEmpty()
        return b.isNotEmpty() && b != "（无内容）"
    }

    /** Show more mini notes by raising the page size and refetching. */
    fun loadMoreMini() {
        miniLimit += 20
        loadMini()
    }

    private fun loadHitokoto() {
        val cfg = settings.config.value
        if (!cfg.hitokotoEnabled) { hitokoto.value = null; hitokotoLoading.value = false; return }
        if (hitokoto.value != null) return
        hitokotoLoading.value = true
        viewModelScope.launch {
            hitokoto.value = runCatching { HitokotoApi.fetch(cfg.hitokotoType) }.getOrNull()
            hitokotoLoading.value = false
        }
    }

    fun testApi() = load(apiTest) {
        val u = repo.userInfo()
        "✓ 连接成功\n用户：${u.name}\n@${u.login}"
    }

    fun saveSettings(newConfig: AppConfig) {
        val old = settings.config.value
        val cleared = if (newConfig.token != old.token) {
            nameTried = false
            newConfig.copy(login = "", userName = "")
        } else newConfig
        // Credential/source changes should invalidate caches.
        if (cleared.token != old.token || cleared.cookie != old.cookie) {
            treeCache.clear(); lastReposFetch = 0L; lastMiniFetch = 0L
        }
        settings.update(cleared)
        com.yuquewatch.data.source.ResponseLog.enabled = cleared.recordResponses
        _greeting.value = greet(cleared.userName)
        hitokoto.value = null // re-fetch with possibly-changed 一言 settings
        miniLimit = 20
        loadHitokoto()
        if (cleared.officialReady) ensureRepos(force = true)
        if (cleared.miniReady) ensureMini(force = true)
        if (cleared.quickRepoNamespace.isNotBlank()) loadQuick()
    }

    // ---- helpers ----

    private fun <T> load(flow: MutableStateFlow<Resource<T>>, block: suspend () -> T) {
        flow.value = Resource.Loading
        viewModelScope.launch {
            flow.value = try {
                Resource.Content(block())
            } catch (e: Exception) {
                Resource.Error(e.message ?: "加载失败")
            }
        }
    }

    private fun greet(name: String): String {
        val part = when (LocalTime.now().hour) {
            in 5..10 -> "上午好"
            in 11..13 -> "中午好"
            in 14..18 -> "下午好"
            else -> "晚上好"
        }
        return if (name.isBlank()) part else "$part，$name"
    }
}
