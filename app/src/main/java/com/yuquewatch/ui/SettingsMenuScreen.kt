package com.yuquewatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun SettingsMenuScreen(
    onAppearance: () -> Unit,
    onReading: () -> Unit,
    onData: () -> Unit,
    onCache: () -> Unit,
    onDebug: () -> Unit,
    onAbout: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScreenBg()
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth(),
            contentPadding = bleedPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text("设置", style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            item { MenuChip("个性化", AppIcons.personal, onAppearance) }
            item { MenuChip("阅读与编辑器", AppIcons.reading, onReading) }
            item { MenuChip("账号与安全", AppIcons.account, onData) }
            item { MenuChip("缓存", AppIcons.cache, onCache) }
            item { MenuChip("调试", AppIcons.debug, onDebug) }
            item { MenuChip("关于 语雀wear", AppIcons.about, onAbout) }
        }
    }
}

@Composable
private fun MenuChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
