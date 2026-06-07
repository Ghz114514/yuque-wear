package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.yuquewatch.data.DocRef

@Composable
fun SearchScreen(
    state: Resource<List<DocRef>>,
    onSearch: (String) -> Unit,
    onOpen: (DocRef) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("搜索文档", style = MaterialTheme.typography.title3.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            item { WatchTextField(q, { q = it }, "关键词") }
            item {
                CompactChip(onClick = { onSearch(q) }, label = { Text("搜索") },
                    colors = ChipDefaults.primaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            when (state) {
                is Resource.Loading -> item { LoadingRow("搜索中…") }
                is Resource.Error -> item { CenterHint(state.message) }
                is Resource.Content -> {
                    if (state.data.isEmpty()) item { CenterHint("输入关键词后搜索（官方接口，范围有限）") }
                    else items(state.data, key = { it.namespace + "/" + it.slug }) { ref ->
                        Chip(
                            onClick = { onOpen(ref) },
                            label = { Text(ref.title, maxLines = 2) },
                            secondaryLabel = { Text(ref.namespace) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
