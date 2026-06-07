package com.yuquewatch.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.source.ResponseLog

@Composable
fun DebugSettingsScreen(
    initial: AppConfig,
    onSave: (AppConfig) -> Unit,
    onApiTest: () -> Unit,
    onReaderTest: () -> Unit,
    onOpenRecords: () -> Unit,
) {
    var debugQuota by remember { mutableStateOf(initial.debugQuota) }
    var record by remember { mutableStateOf(initial.recordResponses) }
    var miniPath by remember { mutableStateOf(initial.miniNotesPath) }
    var miniCreate by remember { mutableStateOf(initial.miniCreatePath) }
    var miniUpdate by remember { mutableStateOf(initial.miniUpdatePath) }
    var miniDelete by remember { mutableStateOf(initial.miniDeletePath) }
    val context = LocalContext.current

    fun build() = initial.copy(
        debugQuota = debugQuota, recordResponses = record,
        miniNotesPath = miniPath.trim(), miniCreatePath = miniCreate.trim(),
        miniUpdatePath = miniUpdate.trim(), miniDeletePath = miniDelete.trim(),
    )
    AutoSaveOnExit(initial, ::build, onSave)

    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SettingTitle("调试") }
            item {
                ToggleChip(checked = debugQuota, onCheckedChange = { debugQuota = it },
                    label = { Text("显示API剩余次数") }, secondaryLabel = { Text("调用语雀时弹 Toast") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(debugQuota), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth())
            }
            item {
                ToggleChip(
                    checked = record,
                    onCheckedChange = { record = it; ResponseLog.enabled = it },
                    label = { Text("记录响应数据") }, secondaryLabel = { Text("开启后记录之后打开的内容") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(record), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = onOpenRecords, label = { Text("查看原始响应记录") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(
                    onClick = {
                        ResponseLog.clear()
                        runCatching { context.cacheDir?.deleteRecursively() }
                        runCatching { java.io.File(context.filesDir, "doc_cache").deleteRecursively() }
                        Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("清理缓存") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = onApiTest, label = { Text("语雀API连通测试") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item {
                CompactChip(onClick = onReaderTest, label = { Text("离线阅读器自检") },
                    colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
            }
            item { SettingHint("记录用于排查接口") }

            item { SettingSection("小记接口路径") }
            item { SettingLabel("列表") }
            item { WatchTextField(miniPath, { miniPath = it }, "/api/.../index", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri) }
            item { SettingLabel("新建") }
            item { WatchTextField(miniCreate, { miniCreate = it }, "/api/.../create", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri) }
            item { SettingLabel("更新") }
            item { WatchTextField(miniUpdate, { miniUpdate = it }, "/api/.../update", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri) }
            item { SettingLabel("删除") }
            item { WatchTextField(miniDelete, { miniDelete = it }, "/api/.../batchDelete", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri) }
        }
    }
}
