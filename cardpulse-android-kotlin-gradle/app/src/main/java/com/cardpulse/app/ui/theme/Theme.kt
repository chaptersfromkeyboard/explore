package com.cardpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF080C14)
val CardSurface = Color(0xFF0F172A)
val CardFooterBg = Color(0xFF0B111E)
val SheetSurface = Color(0xFF0D1424)
val InputBg = Color(0xFF111827)
val SlateBorder = Color(0xFF1E293B)

val NeonCyan = Color(0xFF22D3EE)
val ElectricBlue = Color(0xFF3B82F6)
val EmeraldOk = Color(0xFF34D399)
val AmberWarn = Color(0xFFFBBF24)
val RoseUrgent = Color(0xFFFB7185)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

private val CardPulseDarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF04121A),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = InputBg,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = RoseUrgent,
    onError = Color.White
)

@Composable
fun CardPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CardPulseDarkColors,
        content = content
    )
}
