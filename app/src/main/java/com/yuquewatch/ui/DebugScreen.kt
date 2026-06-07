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
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

/**
 * Shows recorded raw API responses (when recording is on in 调试). Screenshot to feed back
 * real JSON shapes. Each opened request appends one entry.
 */
@Composable
fun DebugScreen(entries: List<String>, recording: Boolean, onClear: () -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("原始响应记录", style = MaterialTheme.typography.title3.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            item {
                Text(
                    if (recording) "记录中：之后打开的内容会被记录" else "未开启记录，请到 调试 打开「记录响应数据」",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                )
            }
            item {
                CompactChip(onClick = onClear, label = { Text("清空记录") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            if (entries.isEmpty()) {
                item { CenterHint("暂无记录") }
            } else {
                items(entries) { e ->
                    Text(e, style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        }
    }
}
