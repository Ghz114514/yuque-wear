package com.yuquewatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.yuquewatch.data.AppConfig

/** Persists the edited config when the sub-screen leaves composition (back / swipe). */
@Composable
fun AutoSaveOnExit(initial: AppConfig, current: () -> AppConfig, onSave: (AppConfig) -> Unit) {
    val latest by rememberUpdatedState(current)
    DisposableEffect(Unit) {
        onDispose {
            val c = latest()
            if (c != initial) onSave(c)
        }
    }
}

fun settingParseHex(input: String): Long? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it.digitToIntOrNull(16) == null }) return null
    return 0xFF000000L or hex.toLong(16)
}

@Composable
fun SettingToggle(label: String, checked: Boolean, secondary: String? = null, onChange: (Boolean) -> Unit) {
    ToggleChip(
        checked = checked, onCheckedChange = onChange,
        label = { Text(label) },
        secondaryLabel = secondary?.let { { Text(it) } },
        toggleControl = { Icon(ToggleChipDefaults.switchIcon(checked), contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A simple 2–4 option segmented row. */
@Composable
fun SettingSegRow(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
    ) {
        options.forEachIndexed { i, (label, sel) ->
            SettingSeg(label, sel, Modifier.weight(1f)) { onSelect(i) }
        }
    }
}

@Composable
fun SettingSeg(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CompactChip(
        onClick = onClick,
        label = {
            Text(label, style = MaterialTheme.typography.caption2,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        modifier = modifier,
    )
}

@Composable
fun SettingStepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactChip(onClick = onMinus, label = { Text("－") }, modifier = Modifier.weight(1f))
        CompactChip(onClick = {}, label = {
            Text(value, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }, colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.weight(1.4f))
        CompactChip(onClick = onPlus, label = { Text("＋") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(Color(argb), CircleShape)
            .border(if (selected) 3.dp else 1.dp,
                if (selected) Color.White else Color(0x33FFFFFF), CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
fun SettingTitle(text: String) {
    Text(text, style = MaterialTheme.typography.title3.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = MaterialTheme.colors.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
}

@Composable
fun SettingSection(text: String) {
    Text(text, style = MaterialTheme.typography.title3.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = MaterialTheme.colors.secondary,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
}

@Composable
fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp))
}

@Composable
fun SettingHint(text: String) {
    Text(text, style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
}
