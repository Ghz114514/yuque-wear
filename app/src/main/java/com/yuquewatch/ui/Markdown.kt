package com.yuquewatch.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * Tiny Markdown renderer good enough for reading notes on a watch — no WebView needed.
 * Handles headings, bullet/numbered lists, blockquotes, inline **bold**, *italic*, `code`,
 * and [text](url) (shown as the link text). Everything else renders as plain text.
 */
fun renderMarkdown(md: String): AnnotatedString = buildAnnotatedString {
    val cleaned = preprocessTables(cleanHtml(md.replace("\r\n", "\n")))
    val lines = cleaned.split("\n")
    var inCode = false
    lines.forEachIndexed { index, raw ->
        var line = raw
        when {
            line.trimStart().startsWith("```") -> { inCode = !inCode } // toggle, skip fence
            inCode -> withStyle(
                SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = androidx.compose.ui.graphics.Color(0xFFE0C9A6),
                    background = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                ),
            ) { append(line) }
            line.startsWith("### ") -> heading(line.removePrefix("### "), 1.1.em)
            line.startsWith("## ") -> heading(line.removePrefix("## "), 1.25.em)
            line.startsWith("# ") -> heading(line.removePrefix("# "), 1.45.em)
            line.startsWith("> ") -> {
                withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF9AA0A6))) {
                    append("┃ "); inline(line.removePrefix("> "))
                }
            }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                append("•  "); inline(line.trimStart().drop(2))
            }
            Regex("^\\s*\\d+\\. ").containsMatchIn(line) -> {
                val m = Regex("^\\s*(\\d+)\\. (.*)").find(line)
                append("${m?.groupValues?.get(1) ?: "1"}.  "); inline(m?.groupValues?.get(2) ?: line)
            }
            line.isBlank() -> { /* paragraph gap */ }
            else -> inline(line)
        }
        if (index != lines.lastIndex) append("\n")
    }
}

/**
 * Strips embedded HTML (lake docs export `<font>`, `<span>`, `<table>` etc. even inside
 * Markdown). HTML tables become readable "表头：值" rows; other tags are removed; entities
 * decoded. Pure Markdown is unaffected (it has no tags).
 */
private fun cleanHtml(s: String): String {
    if (!s.contains('<')) return s
    var t = htmlTablesToText(s)
    t = t.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</(p|div|li|tr|h[1-6])>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'")
        .replace(Regex("[ \\t]+\n"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
    return t
}

/** Replaces each <table> with newline-separated "表头：单元格" rows. */
private fun htmlTablesToText(s: String): String {
    val tableRe = Regex("<table[\\s\\S]*?</table>", RegexOption.IGNORE_CASE)
    val cellRe = Regex("<t[dh][^>]*>([\\s\\S]*?)</t[dh]>", RegexOption.IGNORE_CASE)
    val rowRe = Regex("<tr[\\s\\S]*?</tr>", RegexOption.IGNORE_CASE)
    return tableRe.replace(s) { m ->
        val rows = rowRe.findAll(m.value).map { tr ->
            cellRe.findAll(tr.value).map { c ->
                c.groupValues[1].replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").trim()
            }.toList()
        }.toList()
        if (rows.isEmpty()) return@replace ""
        val header = rows.first()
        val body = rows.drop(1)
        buildString {
            append("\n")
            val src = if (body.isEmpty()) rows else body
            src.forEach { row ->
                val line = if (body.isNotEmpty()) {
                    row.indices.joinToString("，") { i ->
                        val h = header.getOrNull(i)?.takeIf { it.isNotBlank() }
                        if (h != null) "$h：${row[i]}" else row[i]
                    }
                } else row.joinToString(" | ")
                if (line.isNotBlank()) append("• ").append(line).append("\n")
            }
        }
    }
}

/**
 * Converts Markdown tables into watch-readable text (#3). Each data row becomes
 * "表头：单元格" pairs so a narrow screen never shows raw `| --- |` pipe syntax.
 */
private fun preprocessTables(md: String): String {
    val lines = md.split("\n")
    val sep = Regex("^\\s*\\|?[\\s|:\\-]*-[\\s|:\\-]*\\|?\\s*$")
    val out = StringBuilder()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val isHeader = line.contains('|') &&
            i + 1 < lines.size && sep.matches(lines[i + 1]) && lines[i + 1].contains('-')
        if (isHeader) {
            val headers = cells(line)
            var j = i + 2
            val rows = mutableListOf<List<String>>()
            while (j < lines.size && lines[j].contains('|') && !lines[j].isBlank()) {
                rows.add(cells(lines[j])); j++
            }
            for (row in rows) {
                val text = if (headers.size == row.size && headers.any { it.isNotBlank() }) {
                    row.indices.joinToString("，") { k ->
                        val h = headers[k]
                        if (h.isBlank()) row[k] else "$h：${row[k]}"
                    }
                } else {
                    row.joinToString(" | ")
                }
                if (text.isNotBlank()) out.append("• ").append(text).append("\n")
            }
            out.append("\n")
            i = j
        } else {
            out.append(line).append("\n")
            i++
        }
    }
    return out.toString().trimEnd('\n')
}

private fun cells(row: String): List<String> =
    row.trim().trim('|').split("|").map { it.trim() }

/** Image URLs in a doc body: Markdown ![](url) and HTML <img src="url">. */
fun extractImageUrls(body: String): List<String> {
    val out = LinkedHashSet<String>()
    Regex("!\\[[^\\]]*]\\(([^)\\s]+)").findAll(body).forEach { out.add(it.groupValues[1]) }
    Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(body).forEach { out.add(it.groupValues[1]) }
    return out.filter { it.startsWith("http") }.take(20)
}

/** A document split into ordered text / image blocks so images render in place. */
sealed interface DocBlock {
    data class Md(val text: String) : DocBlock
    data class Img(val url: String) : DocBlock
}

private val imageRe =
    Regex("!\\[[^\\]]*]\\(([^)\\s]+)[^)]*\\)|<img[^>]+src=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)

/** Splits [body] at image markers; image markdown is consumed (not left in the text). */
fun parseDocBlocks(body: String): List<DocBlock> {
    val blocks = mutableListOf<DocBlock>()
    var last = 0
    for (m in imageRe.findAll(body)) {
        if (m.range.first > last) {
            val t = body.substring(last, m.range.first)
            if (t.isNotBlank()) blocks.add(DocBlock.Md(t))
        }
        val url = m.groupValues[1].ifBlank { m.groupValues[2] }.substringBefore(' ').trim()
        if (url.startsWith("http")) blocks.add(DocBlock.Img(url))
        last = m.range.last + 1
    }
    if (last < body.length) {
        val t = body.substring(last)
        if (t.isNotBlank()) blocks.add(DocBlock.Md(t))
    }
    if (blocks.isEmpty()) blocks.add(DocBlock.Md(body))
    return blocks
}

/**
 * Turns doc-reference links into inline numbered markers: `[文本](…/slug)` → `文本[n]`,
 * matching the order of [refSlugs]. Other links collapse to their text (url removed).
 */
fun numberDocLinks(text: String, refSlugs: List<String>): String {
    val linkRe = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
    return linkRe.replace(text) { m ->
        val label = m.groupValues[1]
        val url = m.groupValues[2].substringBefore('#').substringBefore('?')
        val idx = refSlugs.indexOfFirst { it.isNotBlank() && url.contains(it) }
        if (idx >= 0) "$label[${idx + 1}]" else label
    }
}

private fun AnnotatedString.Builder.heading(text: String, size: TextUnit) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)) { inline(text) }
}

/** Parses inline emphasis within a single line. */
private fun AnnotatedString.Builder.inline(text: String) {
    // Order matters: links first, then code, bold, italic.
    val token = Regex("\\[([^\\]]+)]\\(([^)]+)\\)|`([^`]+)`|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*")
    var cursor = 0
    for (m in token.findAll(text)) {
        if (m.range.first > cursor) append(text.substring(cursor, m.range.first))
        val (_, _, code, bold, italic) = m.destructured
        when {
            m.groupValues[1].isNotEmpty() -> // link
                withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF4DA3FF))) {
                    append(m.groupValues[1])
                }
            code.isNotEmpty() ->
                withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFE08F6B))) { append(code) }
            bold.isNotEmpty() ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            italic.isNotEmpty() ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
        }
        cursor = m.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}
