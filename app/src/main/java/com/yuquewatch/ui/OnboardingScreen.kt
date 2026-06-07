package com.yuquewatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.yuquewatch.R
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.BackgroundStyle
import com.yuquewatch.data.NoteMode
import com.yuquewatch.data.ThemeMode

private const val AGREEMENT = """语雀wear 使用协议与免责声明

1. 本应用为「语雀」的第三方非官方客户端，与语雀官方无关，未获其授权或背书。
2. 仅供个人学习与便捷访问之用，禁止任何商业用途与二次售卖。
3. 你的 Token / Cookie / 账号信息仅保存在本机，不会上传至任何第三方服务器。
4. 「小记」「自动续期」依赖语雀网页内部接口，非官方、可能随时失效或被风控，使用风险自负。
5. 因使用本应用导致的账号异常、数据丢失等，作者不承担任何责任。
6. 继续使用即代表你已阅读并同意以上条款。"""

@Composable
fun OnboardingScreen(initial: AppConfig, onFinish: (AppConfig) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(initial.noteMode) }
    var token by remember { mutableStateOf(initial.token) }
    var cookie by remember { mutableStateOf(initial.cookie) }
    var uiScale by remember { mutableStateOf(initial.uiScale) }
    var themeMode by remember { mutableStateOf(initial.themeMode) }
    var bg by remember { mutableStateOf(initial.backgroundStyle) }
    var hideUnviewable by remember { mutableStateOf(initial.hideUnviewable) }

    val listState = rememberScalingLazyListState()
    val last = 5

    fun finish() = onFinish(
        initial.copy(
            onboardingDone = true,
            noteMode = mode,
            token = token.trim(),
            cookie = cookie.trim(),
            uiScale = uiScale,
            themeMode = themeMode,
            backgroundStyle = bg,
            hideUnviewable = hideUnviewable,
        )
    )

    Scaffold(timeText = {}) {
        ScreenBg()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (step) {
                0 -> {
                    item {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).padding(top = 8.dp),
                        )
                    }
                    item { Title("你好") }
                    item { Body("欢迎使用 语雀wear\n在手表上记录与阅读语雀") }
                }
                1 -> {
                    item { Title("使用协议") }
                    item {
                        Text(
                            AGREEMENT,
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        )
                    }
                }
                2 -> {
                    item { Title("快记方式") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Seg("小记", mode == NoteMode.MINI, Modifier.weight(1f)) { mode = NoteMode.MINI }
                            Seg("快记", mode == NoteMode.QUICK, Modifier.weight(1f)) { mode = NoteMode.QUICK }
                            Seg("共存", mode == NoteMode.BOTH, Modifier.weight(1f)) { mode = NoteMode.BOTH }
                        }
                    }
                    item { Body("小记=语雀官方(不稳定)；快记=指定知识库(稳定)") }
                    item { Body("个人 Token（知识库/快记/我的 需要）") }
                    item { WatchTextField(token, { token = it }, "X-Auth-Token", password = true) }
                    if (mode != NoteMode.QUICK) {
                        item { Body("小记 Cookie（用小记才需要）") }
                        item {
                            WatchTextField(cookie, { cookie = it }, "Cookie 全串", password = true,
                                singleLine = false, minHeight = 60)
                        }
                    }
                    item { Body("也可稍后在设置中完善") }
                }
                3 -> {
                    item { Title("界面缩放") }
                    item {
                        Text(
                            "示例文字 Aa 语雀",
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body1.copy(fontSize = (16 * uiScale).sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactChip(onClick = { uiScale = (uiScale - 0.05f).coerceAtLeast(0.85f) },
                                label = { Text("－") }, modifier = Modifier.weight(1f))
                            CompactChip(onClick = {}, label = { Text("%.0f%%".format(uiScale * 100)) },
                                colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f))
                            CompactChip(onClick = { uiScale = (uiScale + 0.05f).coerceAtMost(1.25f) },
                                label = { Text("＋") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                4 -> {
                    item { Title("主题与背景") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Seg("绿色", themeMode == ThemeMode.DEFAULT, Modifier.weight(1f)) { themeMode = ThemeMode.DEFAULT }
                            Seg("Monet", themeMode == ThemeMode.MONET, Modifier.weight(1f)) { themeMode = ThemeMode.MONET }
                            Seg("自定", themeMode == ThemeMode.CUSTOM, Modifier.weight(1f)) { themeMode = ThemeMode.CUSTOM }
                        }
                    }
                    item { Body("背景") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Seg("无", bg == BackgroundStyle.NONE, Modifier.weight(1f)) { bg = BackgroundStyle.NONE }
                            Seg("纯色", bg == BackgroundStyle.SOLID, Modifier.weight(1f)) { bg = BackgroundStyle.SOLID }
                            Seg("光效", bg == BackgroundStyle.GLOW, Modifier.weight(1f)) { bg = BackgroundStyle.GLOW }
                            Seg("波普", bg == BackgroundStyle.POPDOT, Modifier.weight(1f)) { bg = BackgroundStyle.POPDOT }
                        }
                    }
                }
                else -> {
                    item { Title("欢迎使用") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Seg("显示全部内容", !hideUnviewable, Modifier.weight(1f)) { hideUnviewable = false }
                            Seg("隐藏无法查看", hideUnviewable, Modifier.weight(1f)) { hideUnviewable = true }
                        }
                    }
                    item { Body("一切就绪，开始你的语雀之旅") }
                }
            }

            // navigation
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step > 0) {
                        CompactChip(onClick = { step-- }, label = { Text("上一步") },
                            colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1f))
                    }
                    if (step < last) {
                        Button(onClick = { step++ }, colors = ButtonDefaults.primaryButtonColors(),
                            modifier = Modifier.weight(1f)) {
                            Text(if (step == 1) "同意并继续" else "下一步")
                        }
                    } else {
                        Button(onClick = { finish() }, colors = ButtonDefaults.primaryButtonColors(),
                            modifier = Modifier.weight(1f)) { Text("开始使用") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(t: String) {
    Text(t, style = MaterialTheme.typography.title2, color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}

@Composable
private fun Body(t: String) {
    Text(t, style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
}

@Composable
private fun Seg(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CompactChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.caption2,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        modifier = modifier,
    )
}
