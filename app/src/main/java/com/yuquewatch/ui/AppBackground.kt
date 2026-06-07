package com.yuquewatch.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yuquewatch.data.BackgroundStyle
import kotlin.math.sin

/**
 * App-wide background drawn behind all screens. Static by default (painted once, no per-frame
 * cost); [dynamic] adds a slow, cheap drift on GLOW/POPDOT. NONE is fully transparent.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle,
    dynamic: Boolean,
    base: Color,
    accent: Color,
    density: Int = 10,
    intensity: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val animated = dynamic && (style == BackgroundStyle.GLOW || style == BackgroundStyle.POPDOT)
    val phase = if (animated) animatedPhase() else 0f

    // Always paint the opaque base first (theme background is transparent), then decorate.
    Canvas(modifier.fillMaxSize()) {
        drawRect(base)
        when (style) {
            BackgroundStyle.NONE -> Unit
            BackgroundStyle.SOLID -> {
                // Solid base with a faint diagonal accent tint so it reads as a chosen colour.
                drawRect(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
            }
            BackgroundStyle.GLOW -> {
                val k = intensity.coerceIn(0.4f, 1.6f)
                val drift = (phase - 0.5f) * size.minDimension * 0.18f
                val c1 = Offset(size.width * 0.28f + drift, size.height * 0.22f)
                val c2 = Offset(size.width * 0.80f - drift, size.height * 0.82f)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.55f * k), Color.Transparent),
                        center = c1, radius = size.minDimension * 0.62f),
                    radius = size.minDimension * 0.62f, center = c1,
                )
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.35f * k), Color.Transparent),
                        center = c2, radius = size.minDimension * 0.58f),
                    radius = size.minDimension * 0.58f, center = c2,
                )
            }
            BackgroundStyle.POPDOT -> {
                val k = intensity.coerceIn(0.4f, 1.6f)
                drawRect(Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.22f * k), Color.Transparent, accent.copy(alpha = 0.14f * k)),
                    start = Offset.Zero, end = Offset(size.width, size.height)))
                val cols = density.coerceIn(5, 18)
                val step = size.width / cols
                val rows = (size.height / step).toInt() + 2
                for (r in 0..rows) for (c in 0..cols) {
                    val wobble = if (animated) sin((r + c) * 0.7f + phase * 6.283f) * step * 0.12f else 0f
                    // Denser & smaller dots.
                    val rad = step * (0.05f + 0.04f * ((r + c) % 3))
                    drawCircle(accent.copy(alpha = 0.30f * k), radius = rad,
                        center = Offset(c * step, r * step + wobble))
                }
            }
        }
    }
}

@Composable
private fun animatedPhase(): Float {
    val transition = rememberInfiniteTransition(label = "bg")
    val v by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    return v
}
