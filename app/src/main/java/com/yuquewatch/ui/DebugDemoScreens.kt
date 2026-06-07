package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

/** Online demo: verifies the official Yuque API is reachable with the current token. */
@Composable
fun ApiTestScreen(state: Resource<String>, onRun: () -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("语雀API连通测试", style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            item {
                Button(onClick = onRun, colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("调用 /user 测试")
                }
            }
            item {
                val (msg, color) = when (state) {
                    is Resource.Loading -> "请求中…" to MaterialTheme.colors.onSurface
                    is Resource.Error -> state.message to MaterialTheme.colors.error
                    is Resource.Content -> (state.data.ifBlank { "点上方按钮开始" }) to MaterialTheme.colors.onBackground
                }
                Text(msg, color = color, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
        }
    }
}

/** Offline demo: renders sample content to verify the reader, theme and scaling — no network. */
@Composable
fun ReaderTestScreen(readingFontSp: Float) {
    val listState = rememberScalingLazyListState()
    val sample = """
        # 阅读器自检
        这是 **加粗**、*斜体*、`行内代码` 与[语雀链接](/yuque/dev/x)。

        ## 列表
        - 第一项
        - 第二项
        1. 有序一
        2. 有序二

        ## 表格
        | 城市 | 温度 |
        | --- | --- |
        | 北京 | 26 |
        | 上海 | 29 |

        ## 代码块
        ```
        fun hello() = println("hi")
        ```

        ## HTML 表格/字体
        <table><tr><td>序号</td><td>内容</td></tr><tr><td>1</td><td><font>会展介绍</font></td></tr></table>

        > 引用：以上若都正常即说明阅读器可用（代码块等宽、表格转文字、清理 font）。
    """.trimIndent()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("离线阅读器自检", style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            item {
                Text(text = renderMarkdown(sample),
                    style = MaterialTheme.typography.body2.copy(fontSize = readingFontSp.sp),
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
            }
        }
    }
}
