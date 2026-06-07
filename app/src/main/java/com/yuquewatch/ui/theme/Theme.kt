package com.yuquewatch.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.yuquewatch.data.AppConfig
import com.yuquewatch.data.ThemeMode

private val GreenPrimary = Color(0xFF25B864)

/** Opaque base background color that AppBackground paints first. */
fun baseBackground(config: AppConfig): Color =
    if (config.pureBlack) Color(0xFF000000) else Color(0xFF101317)

@Composable
fun YuqueTheme(config: AppConfig, content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(colors = buildColors(config, context), content = content)
}

/** Picks the accent seed per [ThemeMode], then derives a full dark Wear palette from it. */
private fun buildColors(config: AppConfig, context: Context): Colors {
    val seed = when (config.themeMode) {
        ThemeMode.DEFAULT -> GreenPrimary
        ThemeMode.CUSTOM -> Color(config.customColorArgb.toInt())
        ThemeMode.MONET -> monetSeed(context) ?: GreenPrimary
    }
    val secondary = when (config.themeMode) {
        ThemeMode.MONET -> monetSecondary(context) ?: shift(seed, 1.25f)
        else -> shift(seed, 1.25f)
    }
    return Colors(
        primary = seed,
        primaryVariant = shift(seed, 0.78f),
        secondary = secondary,
        secondaryVariant = shift(secondary, 0.78f),
        // Transparent so the nav swipe-container doesn't paint over AppBackground.
        // AppBackground itself fills the opaque base color (see baseBackground()).
        background = Color.Transparent,
        surface = if (config.pureBlack) Color(0xFF111418) else Color(0xFF1B1F24),
        error = Color(0xFFFF6B6B),
        onPrimary = onColorFor(seed),
        onSecondary = onColorFor(secondary),
        onBackground = Color(0xFFECEFF1),
        onSurface = Color(0xFFECEFF1),
        onError = Color(0xFF1A1A1A),
    )
}

/** System Monet accent (Android 12+); null when unavailable so callers fall back. */
private fun monetSeed(context: Context): Color? =
    systemColor(context, "system_accent1_300")

private fun monetSecondary(context: Context): Color? =
    systemColor(context, "system_accent2_300")

private fun systemColor(context: Context, name: String): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val id = context.resources.getIdentifier(name, "color", "android")
    if (id == 0) return null
    return runCatching { Color(context.getColor(id)) }.getOrNull()
}

/** Lighten (factor > 1) or darken (factor < 1) toward/away from white. */
private fun shift(c: Color, factor: Float): Color {
    return if (factor >= 1f) {
        val t = (factor - 1f).coerceIn(0f, 1f)
        Color(
            red = c.red + (1f - c.red) * t,
            green = c.green + (1f - c.green) * t,
            blue = c.blue + (1f - c.blue) * t,
            alpha = c.alpha,
        )
    } else {
        Color(c.red * factor, c.green * factor, c.blue * factor, c.alpha)
    }
}

/** Black on light accents, white on dark accents, for readable text/icons. */
private fun onColorFor(c: Color): Color =
    if (c.luminance() > 0.5f) Color(0xFF101010) else Color(0xFFFFFFFF)
