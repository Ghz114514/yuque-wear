package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun NoteDetailScreen(
    state: Resource<com.yuquewatch.data.Note>,
    readingFontSp: Float,
    imageMode: com.yuquewatch.data.ImageMode,
    hideTitle: Boolean,
    showCopy: Boolean,
    nativeSelect: Boolean,
    canFav: Boolean,
    initialFavorited: Boolean,
    onToggleFav: () -> Boolean,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onOpenRef: (com.yuquewatch.data.DocRef) -> Unit,
    onZoom: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScreenBg()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            autoCentering = null, // start from the top, not centered
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (state) {
                is Resource.Loading -> item { LoadingRow("读取中…") }
                is Resource.Error -> item {
                    Text(
                        state.message,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
                is Resource.Content -> {
                    val note = state.data
                    if (!hideTitle) {
                        item {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.title3.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                    if (note.fromCache) {
                        item {
                            Text(
                                "离线 · 缓存内容",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (note.markdown) {
                        // Split into ordered text/image blocks so images sit in place and
                        // image markdown is removed from the text. Doc links become [n] markers.
                        val refSlugs = note.links.map { it.slug }
                        val blocks = parseDocBlocks(note.body.orEmpty())
                        itemsIndexed(blocks) { _, b ->
                            when (b) {
                                is DocBlock.Md -> {
                                    val rendered = remember(b.text, refSlugs) {
                                        renderMarkdown(numberDocLinks(b.text, refSlugs))
                                    }
                                    SelectableText(nativeSelect) {
                                        Text(
                                            text = rendered,
                                            style = MaterialTheme.typography.body2.copy(fontSize = readingFontSp.sp),
                                            color = MaterialTheme.colors.onBackground,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                                is DocBlock.Img ->
                                    if (imageMode != com.yuquewatch.data.ImageMode.HIDE) {
                                        DocImage(
                                            b.url,
                                            autoLoad = imageMode == com.yuquewatch.data.ImageMode.AUTO,
                                            onZoom = onZoom,
                                        )
                                    }
                            }
                        }
                    } else {
                        item {
                            SelectableText(nativeSelect) {
                                Text(
                                    text = note.body.orEmpty(),
                                    style = MaterialTheme.typography.body2.copy(fontSize = readingFontSp.sp),
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                )
                            }
                        }
                        if (imageMode != com.yuquewatch.data.ImageMode.HIDE && note.images.isNotEmpty()) {
                            itemsIndexed(note.images) { _, url ->
                                DocImage(url, autoLoad = imageMode == com.yuquewatch.data.ImageMode.AUTO, onZoom = onZoom)
                            }
                        }
                    }
                    if (note.links.isNotEmpty()) {
                        item {
                            Text(
                                "引用文档",
                                style = MaterialTheme.typography.caption1,
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                            )
                        }
                        itemsIndexed(note.links) { i, ref ->
                            Chip(
                                onClick = { onOpenRef(ref) },
                                label = { Text("[${i + 1}] ${ref.title}", maxLines = 2) },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (showCopy) CompactChip(
                                onClick = {
                                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("note", note.body.orEmpty()))
                                    android.widget.Toast.makeText(ctx, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("复制") },
                                colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f),
                            )
                            CompactChip(
                                onClick = {
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(android.content.Intent.EXTRA_TEXT, note.body.orEmpty())
                                    runCatching {
                                        ctx.startActivity(android.content.Intent.createChooser(send, "分享"))
                                    }
                                },
                                label = { Text("分享") },
                                colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (canEdit) {
                        item {
                            CompactChip(
                                onClick = onEdit,
                                label = { Text("✎ 编辑") },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (canFav) {
                        item {
                            var fav by remember(note.id) { mutableStateOf(initialFavorited) }
                            Chip(
                                onClick = { fav = onToggleFav() },
                                label = { Text(if (fav) "★ 已收藏" else "☆ 收藏") },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                    item {
                        var confirming by androidx.compose.runtime.remember(note.id, "del") {
                            androidx.compose.runtime.mutableStateOf(false)
                        }
                        Chip(
                            onClick = { if (confirming) onDelete(note.id) else confirming = true },
                            label = { Text(if (confirming) "确认删除？再点一次" else "删除") },
                            colors = ChipDefaults.secondaryChipColors(
                                contentColor = MaterialTheme.colors.error,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Wraps content in a SelectionContainer (native long-press selection) when [enabled]. */
@Composable
private fun SelectableText(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) androidx.compose.foundation.text.selection.SelectionContainer { content() } else content()
}

@Composable
private fun DocImage(url: String, autoLoad: Boolean, onZoom: (String) -> Unit) {
    var show by remember(url) { mutableStateOf(autoLoad) }
    if (show) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                .padding(vertical = 4.dp).clickable { onZoom(url) },
        )
    } else {
        Chip(
            onClick = { show = true },
            label = { Text("点按显示图片", maxLines = 1) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
