package com.yuquewatch.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists [AppConfig]. Uses plain private SharedPreferences (no Tink/Keystore) for maximum
 * compatibility with the non-standard watch ROM — the file is app-private regardless.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("yuque_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(read())
    val config: StateFlow<AppConfig> = _config

    fun update(config: AppConfig) {
        prefs.edit().apply {
            putString(KEY_SOURCE, config.activeSource.name)
            putString(KEY_TOKEN, Crypto.encrypt(config.token))
            putString(KEY_OFFICIAL_BASE, config.officialBase)
            putString(KEY_DEFAULT_NS, config.defaultNamespace)
            putString(KEY_LOGIN, config.login)
            putString(KEY_AVATAR, config.avatarUrl)
            putString(KEY_NOTE_MODE, config.noteMode.name)
            putString(KEY_QUICK_NS, config.quickRepoNamespace)
            putString(KEY_DEFAULT_TAB, config.defaultTab.name)
            putString(KEY_COOKIE, Crypto.encrypt(config.cookie))
            putString(KEY_MINI_BASE, config.miniBase)
            putString(KEY_MINI_PATH, config.miniNotesPath)
            putString(KEY_MINI_CREATE, config.miniCreatePath)
            putString(KEY_MINI_UPDATE, config.miniUpdatePath)
            putString(KEY_MINI_DELETE, config.miniDeletePath)
            putString(KEY_ACCOUNT, config.account)
            putString(KEY_PASSWORD, Crypto.encrypt(config.password))
            putBoolean(KEY_HAPTIC, config.hapticEnabled)
            putString(KEY_THEME, config.themeMode.name)
            putLong(KEY_CUSTOM_COLOR, config.customColorArgb)
            putBoolean(KEY_PURE_BLACK, config.pureBlack)
            putString(KEY_BG_STYLE, config.backgroundStyle.name)
            putBoolean(KEY_BG_DYNAMIC, config.backgroundDynamic)
            putInt(KEY_BG_DENSITY, config.backgroundDensity)
            putFloat(KEY_BG_INTENSITY, config.backgroundIntensity)
            putFloat(KEY_UI_SCALE, config.uiScale)
            putFloat(KEY_READ_FONT, config.readingFontSize)
            putFloat(KEY_GREET_FONT, config.greetingFontSize)
            putInt(KEY_BLEED, config.bleedVertical)
            putInt(KEY_HOME_BLEED, config.homeBleed)
            putBoolean(KEY_TABS_ICON, config.tabsIconOnly)
            putString(KEY_AUTO_CLEAR, config.autoClear.name)
            putLong(KEY_LAST_CLEAR, config.lastCacheClear)
            putString(KEY_IMAGE_MODE, config.imageMode.name)
            putBoolean(KEY_HIDE_UNVIEW, config.hideUnviewable)
            putBoolean(KEY_COPY_BTN, config.showCopyButton)
            putBoolean(KEY_NATIVE_SEL, config.nativeTextSelection)
            putBoolean(KEY_HITOKOTO_COPY, config.hitokotoCopy)
            putString(KEY_QUICK_INSERTS, config.quickInserts)
            putBoolean(KEY_HITOKOTO, config.hitokotoEnabled)
            putString(KEY_HITOKOTO_TYPE, config.hitokotoType)
            putBoolean(KEY_ONBOARDING, config.onboardingDone)
            putBoolean(KEY_DEBUG_QUOTA, config.debugQuota)
            putBoolean(KEY_RECORD_RESP, config.recordResponses)
            putString(KEY_USER_NAME, config.userName)
            apply()
        }
        _config.value = config
    }

    // ---- local favorites (官方无公开收藏接口，故本地实现) ----
    private val sep = ""

    fun favorites(): List<DocRef> =
        prefs.getString(KEY_FAVS, "")!!.split("\n").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(sep); if (p.size >= 3) DocRef(p[0], p[1], p[2]) else null
        }

    fun isFavorite(namespace: String, slug: String): Boolean =
        favorites().any { it.namespace == namespace && it.slug == slug }

    fun recents(): List<DocRef> =
        prefs.getString(KEY_RECENTS, "")!!.split("\n").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split(sep); if (p.size >= 3) DocRef(p[0], p[1], p[2]) else null
        }

    fun clearRecents() = prefs.edit().remove(KEY_RECENTS).apply()

    fun addRecent(ref: DocRef) {
        val cur = recents().toMutableList()
        cur.removeAll { it.namespace == ref.namespace && it.slug == ref.slug }
        cur.add(0, ref)
        while (cur.size > 30) cur.removeAt(cur.size - 1)
        prefs.edit().putString(
            KEY_RECENTS, cur.joinToString("\n") { "${it.namespace}$sep${it.slug}$sep${it.title}" },
        ).apply()
    }

    /** Toggles a favorite; returns true if now favorited. */
    fun toggleFavorite(ref: DocRef): Boolean {
        val cur = favorites().toMutableList()
        val idx = cur.indexOfFirst { it.namespace == ref.namespace && it.slug == ref.slug }
        val added = if (idx >= 0) { cur.removeAt(idx); false } else { cur.add(0, ref); true }
        prefs.edit().putString(
            KEY_FAVS,
            cur.joinToString("\n") { "${it.namespace}$sep${it.slug}$sep${it.title}" },
        ).apply()
        return added
    }

    private fun read(): AppConfig {
        val d = AppConfig()
        return AppConfig(
            activeSource = runCatching {
                DataSourceType.valueOf(prefs.getString(KEY_SOURCE, d.activeSource.name)!!)
            }.getOrDefault(d.activeSource),
            token = Crypto.decrypt(prefs.getString(KEY_TOKEN, d.token)!!),
            officialBase = prefs.getString(KEY_OFFICIAL_BASE, d.officialBase)!!,
            defaultNamespace = prefs.getString(KEY_DEFAULT_NS, d.defaultNamespace)!!,
            login = prefs.getString(KEY_LOGIN, d.login)!!,
            avatarUrl = prefs.getString(KEY_AVATAR, d.avatarUrl)!!,
            noteMode = runCatching { NoteMode.valueOf(prefs.getString(KEY_NOTE_MODE, d.noteMode.name)!!) }.getOrDefault(d.noteMode),
            quickRepoNamespace = prefs.getString(KEY_QUICK_NS, d.quickRepoNamespace)!!,
            defaultTab = runCatching { HomeTab.valueOf(prefs.getString(KEY_DEFAULT_TAB, d.defaultTab.name)!!) }.getOrDefault(d.defaultTab),
            cookie = Crypto.decrypt(prefs.getString(KEY_COOKIE, d.cookie)!!),
            miniBase = prefs.getString(KEY_MINI_BASE, d.miniBase)!!,
            miniNotesPath = prefs.getString(KEY_MINI_PATH, d.miniNotesPath)!!,
            miniCreatePath = prefs.getString(KEY_MINI_CREATE, d.miniCreatePath)!!,
            miniUpdatePath = prefs.getString(KEY_MINI_UPDATE, d.miniUpdatePath)!!,
            miniDeletePath = prefs.getString(KEY_MINI_DELETE, d.miniDeletePath)!!,
            account = prefs.getString(KEY_ACCOUNT, d.account)!!,
            password = Crypto.decrypt(prefs.getString(KEY_PASSWORD, d.password)!!),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, d.hapticEnabled),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs.getString(KEY_THEME, d.themeMode.name)!!)
            }.getOrDefault(d.themeMode),
            customColorArgb = prefs.getLong(KEY_CUSTOM_COLOR, d.customColorArgb),
            pureBlack = prefs.getBoolean(KEY_PURE_BLACK, d.pureBlack),
            backgroundStyle = runCatching {
                BackgroundStyle.valueOf(prefs.getString(KEY_BG_STYLE, d.backgroundStyle.name)!!)
            }.getOrDefault(d.backgroundStyle),
            backgroundDynamic = prefs.getBoolean(KEY_BG_DYNAMIC, d.backgroundDynamic),
            backgroundDensity = prefs.getInt(KEY_BG_DENSITY, d.backgroundDensity),
            backgroundIntensity = prefs.getFloat(KEY_BG_INTENSITY, d.backgroundIntensity),
            uiScale = prefs.getFloat(KEY_UI_SCALE, d.uiScale),
            readingFontSize = prefs.getFloat(KEY_READ_FONT, d.readingFontSize),
            greetingFontSize = prefs.getFloat(KEY_GREET_FONT, d.greetingFontSize),
            bleedVertical = prefs.getInt(KEY_BLEED, d.bleedVertical),
            homeBleed = prefs.getInt(KEY_HOME_BLEED, d.homeBleed),
            tabsIconOnly = prefs.getBoolean(KEY_TABS_ICON, d.tabsIconOnly),
            autoClear = runCatching { AutoClear.valueOf(prefs.getString(KEY_AUTO_CLEAR, d.autoClear.name)!!) }.getOrDefault(d.autoClear),
            lastCacheClear = prefs.getLong(KEY_LAST_CLEAR, d.lastCacheClear),
            imageMode = runCatching {
                ImageMode.valueOf(prefs.getString(KEY_IMAGE_MODE, d.imageMode.name)!!)
            }.getOrDefault(d.imageMode),
            hideUnviewable = prefs.getBoolean(KEY_HIDE_UNVIEW, d.hideUnviewable),
            showCopyButton = prefs.getBoolean(KEY_COPY_BTN, d.showCopyButton),
            nativeTextSelection = prefs.getBoolean(KEY_NATIVE_SEL, d.nativeTextSelection),
            hitokotoCopy = prefs.getBoolean(KEY_HITOKOTO_COPY, d.hitokotoCopy),
            quickInserts = prefs.getString(KEY_QUICK_INSERTS, d.quickInserts)!!,
            hitokotoEnabled = prefs.getBoolean(KEY_HITOKOTO, d.hitokotoEnabled),
            hitokotoType = prefs.getString(KEY_HITOKOTO_TYPE, d.hitokotoType)!!,
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING, d.onboardingDone),
            debugQuota = prefs.getBoolean(KEY_DEBUG_QUOTA, d.debugQuota),
            recordResponses = prefs.getBoolean(KEY_RECORD_RESP, d.recordResponses),
            userName = prefs.getString(KEY_USER_NAME, d.userName)!!,
        )
    }

    private companion object {
        const val KEY_SOURCE = "source"
        const val KEY_TOKEN = "token"
        const val KEY_OFFICIAL_BASE = "official_base"
        const val KEY_DEFAULT_NS = "default_ns"
        const val KEY_LOGIN = "login"
        const val KEY_AVATAR = "avatar"
        const val KEY_NOTE_MODE = "note_mode"
        const val KEY_QUICK_NS = "quick_ns"
        const val KEY_DEFAULT_TAB = "default_tab"
        const val KEY_COOKIE = "cookie"
        const val KEY_MINI_BASE = "mini_base"
        const val KEY_MINI_PATH = "mini_path"
        const val KEY_MINI_CREATE = "mini_create"
        const val KEY_MINI_UPDATE = "mini_update"
        const val KEY_MINI_DELETE = "mini_delete"
        const val KEY_ACCOUNT = "account"
        const val KEY_PASSWORD = "password"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_THEME = "theme_mode"
        const val KEY_CUSTOM_COLOR = "custom_color"
        const val KEY_PURE_BLACK = "pure_black"
        const val KEY_BG_STYLE = "bg_style"
        const val KEY_BG_DYNAMIC = "bg_dynamic"
        const val KEY_BG_DENSITY = "bg_density"
        const val KEY_BG_INTENSITY = "bg_intensity"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_READ_FONT = "read_font"
        const val KEY_GREET_FONT = "greet_font"
        const val KEY_BLEED = "bleed_v"
        const val KEY_HOME_BLEED = "home_bleed"
        const val KEY_TABS_ICON = "tabs_icon"
        const val KEY_AUTO_CLEAR = "auto_clear"
        const val KEY_LAST_CLEAR = "last_clear"
        const val KEY_IMAGE_MODE = "image_mode"
        const val KEY_HIDE_UNVIEW = "hide_unviewable"
        const val KEY_COPY_BTN = "copy_btn"
        const val KEY_NATIVE_SEL = "native_sel"
        const val KEY_HITOKOTO_COPY = "hitokoto_copy"
        const val KEY_QUICK_INSERTS = "quick_inserts"
        const val KEY_HITOKOTO = "hitokoto"
        const val KEY_HITOKOTO_TYPE = "hitokoto_type"
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_DEBUG_QUOTA = "debug_quota"
        const val KEY_RECORD_RESP = "record_resp"
        const val KEY_USER_NAME = "user_name"
        const val KEY_FAVS = "favorites"
        const val KEY_RECENTS = "recents"
    }
}
