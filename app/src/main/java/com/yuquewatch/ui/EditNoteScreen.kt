package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

val DEFAULT_INSERTS = listOf("✅ ", "📌 ", "今日：", "TODO：", "「」", "—— ")

/**
 * Line-based editor that copes with the fullscreen IME: content is a list of lines; tap a line
 * to load it into the input box and edit it, then 加行/更新. Quick inserts drop snippets into the
 * input box (not straight into the text), and live in a collapsible menu.
 */
@Composable
fun EditNoteScreen(
    title: String = "新建",
    initialText: String = "",
    inserts: List<String> = DEFAULT_INSERTS,
    onSave: (String, (String?) -> Unit) -> Unit,
    onDone: () -> Unit,
) {
    val lines = remember {
        mutableStateListOf<String>().apply {
            addAll(initialText.split("\n").filter { it.isNotEmpty() })
        }
    }
    var draft by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var showInserts by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val listState = rememberScalingLazyListState()

    // allowBlank: 加行允许空白行；保存时折叠仅在有内容时进行。
    fun commitDraft(allowBlank: Boolean) {
        if (!allowBlank && draft.isEmpty()) return
        if (editingIndex in lines.indices) lines[editingIndex] = draft else lines.add(draft)
        draft = ""; editingIndex = -1
    }

    Scaffold(timeText = { TimeText() }) {
        ScreenBg()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(title, style = MaterialTheme.typography.title3.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            // Existing lines — tap to edit.
            if (lines.isEmpty()) {
                item { CenterHint("（空，下面输入并「加行」）") }
            } else {
                items(lines.size) { i ->
                    val editing = i == editingIndex
                    Chip(
                        onClick = { draft = lines[i]; editingIndex = i },
                        label = { Text(lines[i].ifBlank { " " }, maxLines = 3,
                            style = MaterialTheme.typography.caption2) },
                        colors = if (editing) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Input box.
            item {
                WatchTextField(
                    value = draft, onValueChange = { draft = it },
                    placeholder = if (editingIndex >= 0) "编辑该行…" else "输入一行…",
                    singleLine = false, minHeight = 50,
                )
            }
            // Three buttons under the box.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CompactChip(onClick = { commitDraft(allowBlank = true) },
                        label = { Text(if (editingIndex >= 0) "更新" else "加行") },
                        colors = ChipDefaults.primaryChipColors(), modifier = Modifier.weight(1f))
                    CompactChip(
                        onClick = {
                            if (editingIndex in lines.indices) lines.removeAt(editingIndex)
                            draft = ""; editingIndex = -1
                        },
                        label = { Text(if (editingIndex >= 0) "删除行" else "清空") },
                        colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f))
                    CompactChip(onClick = { showInserts = !showInserts }, label = { Text("快捷") },
                        colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f))
                }
            }
            // Quick inserts (secondary menu) → into the input box.
            if (showInserts) {
                inserts.chunked(3).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { t ->
                                CompactChip(onClick = { draft += t }, label = { Text(t.trim().ifEmpty { t }) },
                                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            status?.let { msg ->
                item { Text(msg, color = MaterialTheme.colors.error, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1, modifier = Modifier.fillMaxWidth()) }
            }
            item {
                Button(
                    onClick = {
                        if (saving) return@Button
                        commitDraft(allowBlank = false)
                        saving = true; status = null
                        onSave(lines.joinToString("\n")) { err ->
                            saving = false; if (err == null) onDone() else status = err
                        }
                    },
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                ) { Text(if (saving) "保存中…" else "保存") }
            }
        }
    }
}
