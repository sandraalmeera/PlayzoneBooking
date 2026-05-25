package com.playzone.booking.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colors ─────────────────────────────────────────────────────
val PurplePrimary   = Color(0xFF6C3FC7)
val PurpleLight     = Color(0xFF9B6EF3)
val DarkBackground  = Color(0xFF0F0E1A)
val SurfaceDark     = Color(0xFF1C1A2E)
val CardDark        = Color(0xFF252340)
val GoldAccent      = Color(0xFFFFCA28)
val GreenSuccess    = Color(0xFF4CAF50)
val RedCancel       = Color(0xFFEF5350)
val TextPrimary     = Color(0xFFF5F5F5)
val TextSecondary   = Color(0xFFB0A8D0)

private val DarkColorScheme = darkColorScheme(
    primary          = PurplePrimary,
    onPrimary        = Color.White,
    primaryContainer = PurpleLight,
    secondary        = GoldAccent,
    onSecondary      = Color(0xFF1A1200),
    background       = DarkBackground,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDark,
    error            = RedCancel,
    outline          = Color(0xFF4A4468)
)

@Composable
fun PlayzoneBookingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
// Extra colors used in some screens (aligned with admin app)
val OrangeWarning   = Color(0xFFFF9800)
val BlueInfo        = Color(0xFF2196F3)
