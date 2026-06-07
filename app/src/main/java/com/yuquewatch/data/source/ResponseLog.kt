package com.yuquewatch.data.source

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Optional in-memory recorder of raw API responses, for the on-watch debug viewer.
 * When [enabled], every HTTP response opened afterwards is appended (capped).
 */
object ResponseLog {
    @Volatile
    var enabled: Boolean = false

    private const val MAX = 40
    val entries = MutableStateFlow<List<String>>(emptyList())

    fun record(url: String, body: String) {
        if (!enabled) return
        val snippet = if (body.length > 2000) body.take(2000) + "…(截断)" else body
        val entry = "▶ $url\n$snippet"
        entries.value = (listOf(entry) + entries.value).take(MAX)
    }

    fun clear() {
        entries.value = emptyList()
    }
}
