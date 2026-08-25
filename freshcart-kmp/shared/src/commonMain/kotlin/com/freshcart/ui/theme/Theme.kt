package com.freshcart.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// DESIGN.md tokens — the single source of truth for color in this app.
object FreshTokens {
    val Ground = Color(0xFFF4F6F3)      // screen background
    val Surface = Color(0xFFFFFFFF)     // cards, search field, chips
    val Ink = Color(0xFF171A17)         // primary text, prices
    val Muted = Color(0xFF8A8F8A)       // secondary text, MRP strikethrough
    val Accent = Color(0xFF1E3B2C)      // the only interactive brand color
    val OnAccent = Color(0xFFFFFFFF)
    val Favorite = Color(0xFFE0489B)    // heart toggle only
    val BadgeBg = Color(0xFFE7EAFB)     // weight pill background
    val BadgeText = Color(0xFF5261C6)   // weight pill text
    val Fresh = Color(0xFF3DA35D)       // the "10 MINS" dot only
}

// Radius scale is locked by DESIGN.md: cards 20, pills full, small badges 6. Nothing else.
object FreshShapes {
    val Card = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val Badge = RoundedCornerShape(6.dp)
}

private val FreshColorScheme = lightColorScheme(
    primary = FreshTokens.Accent,
    onPrimary = FreshTokens.OnAccent,
    primaryContainer = FreshTokens.Accent,
    onPrimaryContainer = FreshTokens.OnAccent,
    background = FreshTokens.Ground,
    onBackground = FreshTokens.Ink,
    surface = FreshTokens.Surface,
    onSurface = FreshTokens.Ink,
    surfaceVariant = FreshTokens.Surface,
    onSurfaceVariant = FreshTokens.Muted,
    secondary = FreshTokens.Muted,
    onSecondary = FreshTokens.OnAccent,
    // Component slots that would otherwise fall back to baseline Material purple.
    secondaryContainer = FreshTokens.Ground,
    onSecondaryContainer = FreshTokens.Ink,
    tertiary = FreshTokens.Accent,
    onTertiary = FreshTokens.OnAccent,
    surfaceTint = FreshTokens.Accent,
    surfaceContainerLowest = FreshTokens.Surface,
    surfaceContainerLow = FreshTokens.Surface,
    surfaceContainer = FreshTokens.Surface,
    surfaceContainerHigh = FreshTokens.Surface,
    surfaceContainerHighest = FreshTokens.Surface,
    outline = FreshTokens.Muted,
    outlineVariant = FreshTokens.Muted.copy(alpha = 0.4f),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

/**
 * Light-only on purpose: the FreshCart brand tokens are a light palette (white cards on a
 * near-white ground), and DESIGN.md defines no dark counterparts — auto-inverting them would
 * break the card/ground contrast the whole layout leans on.
 */
@Composable
fun FreshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FreshColorScheme,
        content = content,
    )
}
