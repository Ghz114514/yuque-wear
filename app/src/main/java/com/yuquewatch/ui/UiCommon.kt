package com.yuquewatch.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun CenterHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

/** Indeterminate spinner + label, shown before content has loaded. */
@Composable
fun LoadingRow(label: String = "加载中…") {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val transition = rememberInfiniteTransition(label = "spin")
        val angle by transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "angle",
        )
        val color = MaterialTheme.colors.primary
        val track = MaterialTheme.colors.primary.copy(alpha = 0.25f)
        Canvas(Modifier.size(28.dp)) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            val arc = Size(size.width - stroke.width, size.height - stroke.width)
            val off = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2)
            drawArc(track, 0f, 360f, false, topLeft = off, size = arc, style = stroke)
            drawArc(color, angle, 90f, false, topLeft = off, size = arc, style = stroke)
        }
        Text(label, style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface, textAlign = TextAlign.Center)
    }
}

/** Background + spinner shown while the editor loads an existing doc. */
@Composable
fun EditorLoading() {
    ScreenBg()
    LoadingRow("载入文档…")
}

@Composable
fun ErrorChip(message: String, onRetry: () -> Unit) {
    Chip(
        onClick = onRetry,
        label = { Text(message, textAlign = TextAlign.Start) },
        secondaryLabel = { Text("点按重试") },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
