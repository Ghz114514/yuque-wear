package com.yuquewatch.data

/** Which backend the *home list* currently shows. Both can be configured at once. */
enum class DataSourceType { OFFICIAL, MININOTE }

/** Appearance: fixed green, system Monet palette, or a user-picked accent. */
enum class ThemeMode { DEFAULT, MONET, CUSTOM }

/** Background style. Perf order (cheap→costly): NONE ≈ SOLID < GLOW < POPDOT. */
enum class BackgroundStyle { NONE, SOLID, GLOW, POPDOT }

/** How document images are shown. */
enum class ImageMode { HIDE, TAP, AUTO }

/** Quick-record method(s) enabled. */
enum class NoteMode { MINI, QUICK, BOTH }

/** Home tabs. MINE = 我的(全部知识库/头像/收藏/设置). */
enum class HomeTab { QUICK, MINI, MINE }

/** Auto cache-clear policy. */
enum class AutoClear { NEVER, EXIT, WEEKLY, MONTHLY }

/** A knowledge base (语雀 repo). */
data class Repo(
    val id: Long,
    val namespace: String,
    val name: String,
    val docCount: Int = 0,
)

/** A reference to another 语雀 doc, used for in-app jumps from a doc's body. */
data class DocRef(
    val namespace: String,
    val slug: String,
    val title: String,
)

/** A note / document shown in lists or detail. */
data class Note(
    val id: String,
    val title: String,
    val body: String? = null,
    val updatedAt: String = "",
    /** True if [body] is Markdown (render it); false if already cleaned plain text. */
    val markdown: Boolean = true,
    /** Links to other 语雀 docs found in the body (for jump chips). */
    val links: List<DocRef> = emptyList(),
    /** Image URLs (used for non-markdown notes like 小记, rendered after text). */
    val images: List<String> = emptyList(),
    /** True when this note was served from the offline cache. */
    val fromCache: Boolean = false,
)

/** One row in a knowledge base's table of contents: a folder header or a doc. */
sealed interface DocTreeItem {
    val depth: Int

    data class Folder(val title: String, override val depth: Int) : DocTreeItem
    data class Doc(val title: String, val slug: String, override val depth: Int) : DocTreeItem
}

/**
 * All persisted settings. Official and MiniNote credentials are stored side by side and
 * never overwrite each other; [activeSource] only selects which the home screen shows.
 */
data class AppConfig(
    val activeSource: DataSourceType = DataSourceType.OFFICIAL,

    // ---- Official Open API (X-Auth-Token) ----
    val token: String = "",
    val officialBase: String = "https://www.yuque.com/api/v2",
    /** Optional default repo for new notes, e.g. "login/notes". Blank = use first repo. */
    val defaultNamespace: String = "",
    /** Cached current-user login, so we stop re-calling /user (rate-limit friendly). */
    val login: String = "",
    /** Cached avatar URL (for the 我的 tab). */
    val avatarUrl: String = "",

    // ---- quick-record (快记) ----
    /** Which quick-record methods are enabled. */
    val noteMode: NoteMode = NoteMode.MINI,
    /** Knowledge base used as the 快记 store, e.g. "login/notes". */
    val quickRepoNamespace: String = "",
    /** Default tab shown on launch. */
    val defaultTab: HomeTab = HomeTab.MINE,

    // ---- MiniNote (experimental, Cookie) ----
    val cookie: String = "",
    val miniBase: String = "https://www.yuque.com",
    val miniNotesPath: String = "/api/modules/note/notes/NoteController/index",
    /** Create endpoint (often a different action than the list path). */
    val miniCreatePath: String = "/api/modules/note/notes/NoteController/create",
    /** Update endpoint (PUT) — confirmed payload carries lake abstract/html/source. */
    val miniUpdatePath: String = "/api/modules/note/notes/NoteController/update",
    /** Delete endpoint (DELETE, body {"ids":[...]}) — confirmed. */
    val miniDeletePath: String = "/api/modules/note/notes/NoteOperateController/batchDelete",
    /** Optional account + password for experimental Cookie auto-renew. */
    val account: String = "",
    val password: String = "",

    // ---- appearance ----
    val hapticEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    /** Seed accent for CUSTOM theme (ARGB). Default 语雀 green. */
    val customColorArgb: Long = 0xFF25B864L,
    /** Pure-black (AMOLED) background vs soft dark. */
    val pureBlack: Boolean = true,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.NONE,
    /** Animate the background (off = static, saves battery). */
    val backgroundDynamic: Boolean = false,
    /** POPDOT density (columns across). Higher = denser & smaller dots. */
    val backgroundDensity: Int = 10,
    /** Decoration strength 0.4–1.6 (scales dot/glow opacity). */
    val backgroundIntensity: Float = 1.0f,
    /** Global UI scale (0.85–1.25). Scales the whole interface. */
    val uiScale: Float = 1.0f,
    /** Reading body font size in sp (11–22). */
    val readingFontSize: Float = 14f,
    /** Greeting font size in sp (14–28). */
    val greetingFontSize: Float = 18f,
    /** Vertical bleed (dp) for reading screens so round screens don't clip top/bottom. */
    val bleedVertical: Int = 72,
    /** Separate, usually smaller bleed for the home screen. */
    val homeBleed: Int = 16,
    /** Show home tabs as icon-only (no label). */
    val tabsIconOnly: Boolean = false,
    /** Auto cache-clear policy + last-clear time. */
    val autoClear: AutoClear = AutoClear.NEVER,
    val lastCacheClear: Long = 0,
    /** Document image display mode. */
    val imageMode: ImageMode = ImageMode.TAP,
    /** Hide notes whose content can't be shown (empty / image-only). Default off. */
    val hideUnviewable: Boolean = false,
    /** Show the 复制 (copy whole text) button in the reader. */
    val showCopyButton: Boolean = true,
    /** Allow long-press native text selection in the reader. */
    val nativeTextSelection: Boolean = false,
    /** Tapping the 一言 copies it. */
    val hitokotoCopy: Boolean = false,
    /** Custom editor quick-insert snippets, one per line. Blank = built-in defaults. */
    val quickInserts: String = "",

    // ---- hitokoto (一言) ----
    val hitokotoEnabled: Boolean = false,
    /** Hitokoto category letters, e.g. "a,d,i"; blank = any. */
    val hitokotoType: String = "",

    // ---- onboarding ----
    val onboardingDone: Boolean = false,

    // ---- debug ----
    /** Show a toast with the remaining Yuque API quota on each official call. */
    val debugQuota: Boolean = false,
    /** Record raw API responses for the debug viewer. */
    val recordResponses: Boolean = false,

    // ---- cached ----
    val userName: String = "",
) {
    val officialReady: Boolean get() = token.isNotBlank()
    val miniReady: Boolean get() = cookie.isNotBlank()

    val isConfigured: Boolean
        get() = when (activeSource) {
            DataSourceType.OFFICIAL -> officialReady
            DataSourceType.MININOTE -> miniReady
        }
}
