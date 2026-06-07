package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.yuquewatch.data.DocTreeItem

@Composable
fun DocsScreen(
    repoName: String,
    tree: Resource<List<DocTreeItem>>,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onRetry: () -> Unit,
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
            autoCentering = null,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item {
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.title3.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            item {
                CompactChip(
                    onClick = onNew,
                    label = { Text("＋ 在此知识库新建") },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (tree) {
                is Resource.Loading -> item { LoadingRow("加载目录…") }
                is Resource.Error -> item { ErrorChip(tree.message, onRetry) }
                is Resource.Content -> {
                    if (tree.data.isEmpty()) {
                        item { CenterHint("该知识库暂无文档") }
                    } else {
                        // ScalingLazyColumn only composes visible rows, so large repos stay smooth.
                        items(tree.data, key = { rowKey(it) }) { node ->
                            when (node) {
                                is DocTreeItem.Folder -> FolderRow(node)
                                is DocTreeItem.Doc -> Chip(
                                    onClick = { onOpen(node.slug) },
                                    label = { Text(node.title, maxLines = 2) },
                                    colors = ChipDefaults.secondaryChipColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = indent(node.depth)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: DocTreeItem.Folder) {
    Text(
        text = "▸ ${folder.title}",
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.secondary,
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent(folder.depth), top = 6.dp, bottom = 2.dp),
    )
}

private fun rowKey(item: DocTreeItem): String = when (item) {
    is DocTreeItem.Folder -> "f:${item.depth}:${item.title}"
    is DocTreeItem.Doc -> "d:${item.slug}"
}

private fun indent(depth: Int) = (depth.coerceIn(0, 4) * 8).dp
