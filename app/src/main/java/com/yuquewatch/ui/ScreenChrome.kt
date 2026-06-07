package com.yuquewatch.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yuquewatch.data.BackgroundStyle

/** Background parameters provided once (MainActivity) and consumed by every screen. */
data class BgSpec(
    val style: BackgroundStyle = BackgroundStyle.NONE,
    val dynamic: Boolean = false,
    val base: Color = Color.Black,
    val accent: Color = Color(0xFF25B864),
    val density: Int = 10,
    val intensity: Float = 1.0f,
)

val LocalBgSpec = compositionLocalOf { BgSpec() }

/**
 * Opaque per-screen background. Placed as the FIRST child inside a screen's Scaffold so the
 * destination paints its own background — swiping back no longer shows the screen underneath.
 */
@Composable
fun ScreenBg() {
    val spec = LocalBgSpec.current
    AppBackground(
        style = spec.style, dynamic = spec.dynamic,
        base = spec.base, accent = spec.accent,
        density = spec.density, intensity = spec.intensity,
    )
}

/** User-configurable vertical bleed (dp), provided by MainActivity. */
val LocalBleed = compositionLocalOf { 24 }

/** Top/bottom bleed so first/last items aren't clipped by the round screen. */
@Composable
fun bleedPadding(): PaddingValues =
    PaddingValues(horizontal = 10.dp, vertical = LocalBleed.current.dp)
