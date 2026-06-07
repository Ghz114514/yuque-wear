package com.yuquewatch.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.AutoClear
import java.io.File

@Composable
fun CacheSettingsScreen(
    initial: AppConfig,
    onSave: (AppConfig) -> Unit,
    onClearRecents: () -> Unit,
) {
    val context = LocalContext.current
    var auto by remember { mutableStateOf(initial.autoClear) }
    var refresh by remember { mutableIntStateOf(0) }

    fun build() = initial.copy(autoClear = auto)
    AutoSaveOnExit(initial, ::build, onSave)

    val docDir = remember { File(context.filesDir, "doc_cache") }
    val docSize = remember(refresh) { dirSize(docDir) }
    val imgSize = remember(refresh) {
        runCatching { coil.Coil.imageLoader(context).diskCache?.size ?: 0L }.getOrDefault(0L) +
            dirSize(File(context.cacheDir, "image_cache"))
    }
    val total = docSize + imgSize

    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { SettingTitle("缓存") }
            item { SettingHint("总占用 ${fmt(total)}") }
            item { Kv("文档缓存", fmt(docSize)) }
            item { Kv("图片缓存", fmt(imgSize)) }

            item { SettingSection("清理") }
            item {
                CompactChip(onClick = {
                    runCatching { File(context.filesDir, "doc_cache").deleteRecursively() }
                    refresh++; toast(context, "已清除文档缓存")
                }, label = { Text("清除文档缓存") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = {
                    runCatching {
                        val l = coil.Coil.imageLoader(context); l.diskCache?.clear(); l.memoryCache?.clear()
                        File(context.cacheDir, "image_cache").deleteRecursively()
                    }
                    refresh++; toast(context, "已清除图片缓存")
                }, label = { Text("清除图片缓存") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = { onClearRecents(); toast(context, "已清除最近记录") },
                    label = { Text("清除最近记录") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = {
                    runCatching {
                        File(context.filesDir, "doc_cache").deleteRecursively()
                        val l = coil.Coil.imageLoader(context); l.diskCache?.clear(); l.memoryCache?.clear()
                        context.cacheDir?.deleteRecursively()
                    }
                    onClearRecents(); refresh++; toast(context, "已清除全部缓存")
                }, label = { Text("清除全部") },
                    colors = ChipDefaults.primaryChipColors(), modifier = Modifier.fillMaxWidth())
            }

            item { SettingSection("自动清理") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSeg("从不", auto == AutoClear.NEVER, Modifier.weight(1f)) { auto = AutoClear.NEVER }
                    SettingSeg("退出", auto == AutoClear.EXIT, Modifier.weight(1f)) { auto = AutoClear.EXIT }
                    SettingSeg("每周", auto == AutoClear.WEEKLY, Modifier.weight(1f)) { auto = AutoClear.WEEKLY }
                    SettingSeg("每月", auto == AutoClear.MONTHLY, Modifier.weight(1f)) { auto = AutoClear.MONTHLY }
                }
            }
            item { SettingHint("小记/一言为即时获取，不单独落盘缓存") }
        }
    }
}

@Composable
private fun Kv(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = androidx.wear.compose.material.MaterialTheme.typography.caption1,
            color = androidx.wear.compose.material.MaterialTheme.colors.onBackground)
        Text(v, style = androidx.wear.compose.material.MaterialTheme.typography.caption1,
            color = androidx.wear.compose.material.MaterialTheme.colors.secondary)
    }
}

private fun toast(c: android.content.Context, m: String) =
    Toast.makeText(c, m, Toast.LENGTH_SHORT).show()

private fun dirSize(dir: File): Long =
    if (!dir.exists()) 0L else dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

private fun fmt(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
