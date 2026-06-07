package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.ImageMode

@Composable
fun ReadingSettingsScreen(initial: AppConfig, onSave: (AppConfig) -> Unit) {
    var readFont by remember { mutableStateOf(initial.readingFontSize) }
    var bleed by remember { mutableStateOf(initial.bleedVertical) }
    var homeBleed by remember { mutableStateOf(initial.homeBleed) }
    var imageMode by remember { mutableStateOf(initial.imageMode) }
    var hideUnviewable by remember { mutableStateOf(initial.hideUnviewable) }
    var showCopy by remember { mutableStateOf(initial.showCopyButton) }
    var nativeSel by remember { mutableStateOf(initial.nativeTextSelection) }
    var quickInserts by remember { mutableStateOf(initial.quickInserts) }

    fun build() = initial.copy(
        readingFontSize = readFont, bleedVertical = bleed, homeBleed = homeBleed, imageMode = imageMode,
        hideUnviewable = hideUnviewable, showCopyButton = showCopy,
        nativeTextSelection = nativeSel, quickInserts = quickInserts,
    )
    AutoSaveOnExit(initial, ::build, onSave)

    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SettingTitle("阅读与编辑器") }
            item { SettingHint("返回即自动保存") }

            item { SettingLabel("阅读字号") }
            item {
                SettingStepper("${readFont.toInt()}sp",
                    onMinus = { readFont = (readFont - 1f).coerceAtLeast(11f) },
                    onPlus = { readFont = (readFont + 1f).coerceAtMost(22f) })
            }

            item { SettingLabel("阅读出血边（上下留白）") }
            item {
                SettingStepper("${bleed}dp",
                    onMinus = { bleed = (bleed - 4).coerceAtLeast(4) },
                    onPlus = { bleed = (bleed + 4).coerceAtMost(80) })
            }
            item { SettingLabel("主页出血边") }
            item {
                SettingStepper("${homeBleed}dp",
                    onMinus = { homeBleed = (homeBleed - 4).coerceAtLeast(0) },
                    onPlus = { homeBleed = (homeBleed + 4).coerceAtMost(80) })
            }
            item { SettingHint("圆屏裁切顶/底内容时调大；主页偏空时调小") }

            item { SettingLabel("图片显示") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SettingSeg("不显示", imageMode == ImageMode.HIDE, Modifier.weight(1f)) { imageMode = ImageMode.HIDE }
                    SettingSeg("点按显示", imageMode == ImageMode.TAP, Modifier.weight(1f)) { imageMode = ImageMode.TAP }
                    SettingSeg("直接显示", imageMode == ImageMode.AUTO, Modifier.weight(1f)) { imageMode = ImageMode.AUTO }
                }
            }
            item { SettingHint("点按显示可省流量，避免自动加载大图") }

            item { SettingLabel("内容过滤") }
            item {
                ToggleChip(
                    checked = hideUnviewable,
                    onCheckedChange = { hideUnviewable = it },
                    label = { Text("隐藏无法查看的内容") },
                    secondaryLabel = { Text("空内容/纯图片小记整条隐藏") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(hideUnviewable), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SettingLabel("复制与选择") }
            item {
                ToggleChip(
                    checked = showCopy, onCheckedChange = { showCopy = it },
                    label = { Text("显示复制按钮") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(showCopy), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ToggleChip(
                    checked = nativeSel, onCheckedChange = { nativeSel = it },
                    label = { Text("长按选择文字") },
                    secondaryLabel = { Text("用系统原生选择器复制") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(nativeSel), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SettingLabel("编辑器快捷插入（每行一个，空=默认）") }
            item {
                WatchTextField(quickInserts, { quickInserts = it }, "✅ \n📌 \n今日：",
                    singleLine = false, minHeight = 56)
            }
        }
    }
}
