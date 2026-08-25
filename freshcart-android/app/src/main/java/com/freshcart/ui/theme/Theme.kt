package com.freshcart.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * FreshCart design tokens from DESIGN.md. This sample deliberately ships light-only:
 * the token sheet defines a single light palette, and a dark variant would double the
 * surface area of a printer-SDK demo for no SDK value.
 *
 * Single accent lock: [Accent] is the only interactive brand color. [Favorite],
 * [BadgeBg]/[BadgeText] and [Fresh] are fixed semantic colors — never on buttons.
 */
object FreshCartColors {
    val Ground = Color(0xFFF4F6F3)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF171A17)
    val Muted = Color(0xFF8A8F8A)
    val Accent = Color(0xFF1E3B2C)
    val OnAccent = Color(0xFFFFFFFF)
    val Favorite = Color(0xFFE0489B)
    val BadgeBg = Color(0xFFE7EAFB)
    val BadgeText = Color(0xFF5261C6)
    val Fresh = Color(0xFF3DA35D)
}

/**
 * Radius scale (locked): cards 20, search/chips/CTAs full pill, small badges 6.
 * Material 3 buttons and chips are already full pills by default.
 */
object FreshCartShapes {
    val Card = RoundedCornerShape(20.dp)
    val Badge = RoundedCornerShape(6.dp)
}

private val LightColors = lightColorScheme(
    primary = FreshCartColors.Accent,
    onPrimary = FreshCartColors.OnAccent,
    primaryContainer = Color(0xFFE2EAE4),
    onPrimaryContainer = FreshCartColors.Ink,
    secondary = FreshCartColors.Accent,
    onSecondary = FreshCartColors.OnAccent,
    secondaryContainer = Color(0xFFE2EAE4),
    onSecondaryContainer = FreshCartColors.Ink,
    tertiary = FreshCartColors.Accent,
    onTertiary = FreshCartColors.OnAccent,
    background = FreshCartColors.Ground,
    onBackground = FreshCartColors.Ink,
    surface = FreshCartColors.Surface,
    onSurface = FreshCartColors.Ink,
    surfaceVariant = FreshCartColors.Surface,
    onSurfaceVariant = FreshCartColors.Muted,
    surfaceContainerLowest = FreshCartColors.Surface,
    surfaceContainerLow = FreshCartColors.Surface,
    surfaceContainer = FreshCartColors.Surface,
    surfaceContainerHigh = FreshCartColors.Surface,
    surfaceContainerHighest = FreshCartColors.Surface,
    surfaceTint = FreshCartColors.Accent,
    outline = Color(0xFFD3D8D3),
    outlineVariant = Color(0xFFE5E9E5),
)

@Composable
fun FreshCartTheme(content: @Composable () -> Unit) {
    // System font is intentional — Typography() defaults to the platform typeface.
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
