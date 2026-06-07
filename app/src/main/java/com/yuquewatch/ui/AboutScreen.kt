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
fun AboutScreen() {
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
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                Text(
                    AppInfo.APP_NAME,
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            item { Kv("版本", AppInfo.version) }
            item { Kv("编译时间", AppInfo.buildTime) }
            item { Kv("项目", "语雀 Wear OS 客户端") }
            item { Kv("代码", "Claude (Anthropic) 编写") }
            item { Kv("创建者", "1Ghz") }
            item {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                fun open(url: String) = runCatching {
                    ctx.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    androidx.wear.compose.material.CompactChip(
                        onClick = { open("https://github.com/Ghz114514/yuque-wear") },
                        label = { Text("项目地址 (GitHub)") },
                        colors = androidx.wear.compose.material.ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    androidx.wear.compose.material.CompactChip(
                        onClick = { open("https://space.bilibili.com/1199808099") },
                        label = { Text("作者 B 站主页") },
                        colors = androidx.wear.compose.material.ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Text(
                    "致谢",
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 2.dp),
                )
            }
            item {
                Text(
                    AppInfo.credits.joinToString("\n") { "• $it" },
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                )
            }
            item {
                Text(
                    "计划未来版本完善后在 GitHub 开源",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
            item {
                Text(
                    "版本记录",
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 2.dp),
                )
            }
            items(AppInfo.changelog) { c ->
                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("${c.version} · ${c.date}",
                        style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.primary)
                    Text(c.items.joinToString("\n") { "• $it" },
                        style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun Kv(key: String, value: String) {
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(key, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.secondary)
        Text(value, style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.onBackground)
    }
}
