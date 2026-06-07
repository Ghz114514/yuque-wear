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
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.BackgroundStyle
import com.yuquewatch.data.HomeTab
import com.yuquewatch.data.NoteMode
import com.yuquewatch.data.ThemeMode

private val PRESET_COLORS = listOf(
    0xFF25B864L, 0xFF4DA3FFL, 0xFFFF6B6BL, 0xFFFFB300L, 0xFFAB7DF6L, 0xFF26C6DAL,
)
private val HITOKOTO_TYPES = listOf(
    "随机" to "", "文学" to "d", "诗词" to "i", "动画" to "a", "游戏" to "c", "网络" to "f",
)

/** 个人与外观: default tab, quick-record method, 一言, and full theme. */
@Composable
fun AppearanceSettingsScreen(initial: AppConfig, onSave: (AppConfig) -> Unit) {
    // personal
    var defaultTab by remember { mutableStateOf(initial.defaultTab) }
    var mode by remember { mutableStateOf(initial.noteMode) }
    var quickNs by remember { mutableStateOf(initial.quickRepoNamespace) }
    // hitokoto
    var hkOn by remember { mutableStateOf(initial.hitokotoEnabled) }
    var hkCopy by remember { mutableStateOf(initial.hitokotoCopy) }
    var hkType by remember { mutableStateOf(initial.hitokotoType) }
    // theme
    var themeMode by remember { mutableStateOf(initial.themeMode) }
    var customColor by remember { mutableStateOf(initial.customColorArgb) }
    var customHex by remember { mutableStateOf("#%06X".format(initial.customColorArgb and 0xFFFFFFL)) }
    var bgStyle by remember { mutableStateOf(initial.backgroundStyle) }
    var bgDynamic by remember { mutableStateOf(initial.backgroundDynamic) }
    var bgDensity by remember { mutableStateOf(initial.backgroundDensity) }
    var bgIntensity by remember { mutableStateOf(initial.backgroundIntensity) }
    var pureBlack by remember { mutableStateOf(initial.pureBlack) }
    var uiScale by remember { mutableStateOf(initial.uiScale) }
    var greetFont by remember { mutableStateOf(initial.greetingFontSize) }
    var tabsIconOnly by remember { mutableStateOf(initial.tabsIconOnly) }
    var showAvatar by remember { mutableStateOf(initial.showAvatar) }
    var haptic by remember { mutableStateOf(initial.hapticEnabled) }

    fun build() = initial.copy(
        defaultTab = defaultTab, noteMode = mode, quickRepoNamespace = quickNs.trim(),
        hitokotoEnabled = hkOn, hitokotoCopy = hkCopy, hitokotoType = hkType,
        themeMode = themeMode, customColorArgb = customColor,
        backgroundStyle = bgStyle, backgroundDynamic = bgDynamic,
        backgroundDensity = bgDensity, backgroundIntensity = bgIntensity, pureBlack = pureBlack,
        uiScale = uiScale, greetingFontSize = greetFont,
        tabsIconOnly = tabsIconOnly, showAvatar = showAvatar, hapticEnabled = haptic,
    )
    AutoSaveOnExit(initial, ::build, onSave)

    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SettingTitle("个性化") }
            item { SettingHint("返回即自动保存") }

            // ---- personal ----
            item { SettingSection("快速记录") }
            item { SettingLabel("默认开屏") }
            item {
                SettingSegRow(listOf(
                    "快记" to (defaultTab == HomeTab.QUICK),
                    "小记" to (defaultTab == HomeTab.MINI),
                    "我的" to (defaultTab == HomeTab.MINE),
                )) { defaultTab = listOf(HomeTab.QUICK, HomeTab.MINI, HomeTab.MINE)[it] }
            }
            item { SettingLabel("记录方式") }
            item {
                SettingSegRow(listOf(
                    "小记" to (mode == NoteMode.MINI),
                    "快记" to (mode == NoteMode.QUICK),
                    "共存" to (mode == NoteMode.BOTH),
                )) { mode = listOf(NoteMode.MINI, NoteMode.QUICK, NoteMode.BOTH)[it] }
            }
            item { SettingLabel("快记知识库 (namespace)") }
            item { WatchTextField(quickNs, { quickNs = it }, "如 login/notes") }

            // ---- hitokoto ----
            item { SettingSection("一言") }
            item { SettingToggle("主页显示一言", hkOn, "来自 hitokoto.cn") { hkOn = it } }
            if (hkOn) {
                item { SettingToggle("点击复制该条", hkCopy) { hkCopy = it } }
                item { SettingLabel("句子类型") }
                HITOKOTO_TYPES.chunked(3).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { (lbl, letters) ->
                                SettingSeg(lbl, hkType == letters, Modifier.weight(1f)) { hkType = letters }
                            }
                        }
                    }
                }
            }

            // ---- theme ----
            item { SettingSection("主题") }
            item {
                SettingSegRow(listOf(
                    "绿色" to (themeMode == ThemeMode.DEFAULT),
                    "Monet" to (themeMode == ThemeMode.MONET),
                    "自定义" to (themeMode == ThemeMode.CUSTOM),
                )) { themeMode = listOf(ThemeMode.DEFAULT, ThemeMode.MONET, ThemeMode.CUSTOM)[it] }
            }
            if (themeMode == ThemeMode.CUSTOM) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRESET_COLORS.forEach { argb ->
                            SettingSwatch(argb, customColor == argb) {
                                customColor = argb; customHex = "#%06X".format(argb and 0xFFFFFFL)
                            }
                        }
                    }
                }
                item { SettingLabel("自定义色值 (#RRGGBB)") }
                item {
                    WatchTextField(customHex, { input ->
                        customHex = input; settingParseHex(input)?.let { customColor = it }
                    }, "#25B864")
                }
            }
            item { SettingLabel("背景") }
            item {
                SettingSegRow(listOf(
                    "无" to (bgStyle == BackgroundStyle.NONE),
                    "纯色" to (bgStyle == BackgroundStyle.SOLID),
                    "光效" to (bgStyle == BackgroundStyle.GLOW),
                    "波普" to (bgStyle == BackgroundStyle.POPDOT),
                )) { bgStyle = listOf(BackgroundStyle.NONE, BackgroundStyle.SOLID, BackgroundStyle.GLOW, BackgroundStyle.POPDOT)[it] }
            }
            item { SettingToggle("动态背景", bgDynamic, "省电建议关闭") { bgDynamic = it } }
            if (bgStyle != BackgroundStyle.NONE && bgStyle != BackgroundStyle.SOLID) {
                if (bgStyle == BackgroundStyle.POPDOT) {
                    item { SettingLabel("点阵密度") }
                    item {
                        SettingStepper("$bgDensity",
                            onMinus = { bgDensity = (bgDensity - 1).coerceAtLeast(5) },
                            onPlus = { bgDensity = (bgDensity + 1).coerceAtMost(18) })
                    }
                }
                item { SettingLabel("背景强度") }
                item {
                    SettingStepper("%.0f%%".format(bgIntensity * 100),
                        onMinus = { bgIntensity = (bgIntensity - 0.1f).coerceAtLeast(0.4f) },
                        onPlus = { bgIntensity = (bgIntensity + 0.1f).coerceAtMost(1.6f) })
                }
            }
            item { SettingToggle("纯黑背景", pureBlack, "OLED 省电；关则深灰") { pureBlack = it } }
            item { SettingLabel("界面缩放") }
            item {
                SettingStepper("%.0f%%".format(uiScale * 100),
                    onMinus = { uiScale = (uiScale - 0.05f).coerceAtLeast(0.85f) },
                    onPlus = { uiScale = (uiScale + 0.05f).coerceAtMost(1.25f) })
            }
            item { SettingLabel("问候语字号") }
            item {
                SettingStepper("${greetFont.toInt()}sp",
                    onMinus = { greetFont = (greetFont - 1f).coerceAtLeast(14f) },
                    onPlus = { greetFont = (greetFont + 1f).coerceAtMost(28f) })
            }
            item { SettingToggle("显示头像", showAvatar, "在问候语上方显示") { showAvatar = it } }
            item { SettingToggle("主页标签仅图标", tabsIconOnly) { tabsIconOnly = it } }
            item { SettingToggle("表冠震动", haptic, "关闭后下次启动生效") { haptic = it } }
            item { SettingHint("Monet 取色需系统支持(安卓12+)，否则回退绿色") }
        }
    }
}
