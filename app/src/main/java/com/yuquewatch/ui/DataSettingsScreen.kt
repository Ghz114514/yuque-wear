package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
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

/** 账号与安全: official token + default repo, mini cookie, auto-renew account/password. */
@Composable
fun DataSettingsScreen(initial: AppConfig, onSave: (AppConfig) -> Unit) {
    var token by remember { mutableStateOf(initial.token) }
    var defaultNs by remember { mutableStateOf(initial.defaultNamespace) }
    var cookie by remember { mutableStateOf(initial.cookie) }
    var account by remember { mutableStateOf(initial.account) }
    var password by remember { mutableStateOf(initial.password) }

    fun build() = initial.copy(
        token = token.trim(), defaultNamespace = defaultNs.trim(),
        cookie = cookie.trim(), account = account.trim(), password = password,
    )
    AutoSaveOnExit(initial, ::build, onSave)

    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SettingTitle("账号与安全") }
            item { SettingHint("返回即自动保存；凭据仅存本机") }

            item { SettingSection("官方 (知识库/快记)") }
            item { SettingLabel("个人 Token") }
            item { WatchTextField(token, { token = it }, "X-Auth-Token", password = true) }
            item { SettingLabel("默认知识库（可空）") }
            item { WatchTextField(defaultNs, { defaultNs = it }, "如 login/notes") }

            item { SettingSection("小记 Cookie") }
            item {
                WatchTextField(cookie, { cookie = it }, "登录态 Cookie 全串", password = true,
                    singleLine = false, minHeight = 60)
            }

            item { SettingSection("小记自动续期（实验）") }
            item { SettingLabel("语雀账号") }
            item { WatchTextField(account, { account = it }, "手机号/邮箱") }
            item { SettingLabel("密码") }
            item { WatchTextField(password, { password = it }, "用于自动重登", password = true) }
            item { SettingHint("⚠ 加密存于本机；语雀登录有加密/风控，自动续期可能失效") }
        }
    }
}
