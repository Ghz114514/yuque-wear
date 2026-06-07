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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun ChangelogScreen() {
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    "版本日志",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            items(AppInfo.changelog, key = { it.version }) { c ->
                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "${c.version}  ·  ${c.date}",
                        style = MaterialTheme.typography.button,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 2.dp),
                    )
                    Text(
                        text = c.items.joinToString("\n") { "• $it" },
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp),
                    )
                }
            }
        }
    }
}
