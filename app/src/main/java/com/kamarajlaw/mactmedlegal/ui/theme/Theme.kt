package com.kamarajlaw.mactmedlegal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NavyPrimary = Color(0xFF0F2942)
val NavyContainer = Color(0xFFD6E4F0)
val TealAccent = Color(0xFF0D9488)
val TealContainer = Color(0xFFCCFBF1)
val AmberWarn = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)
val CrimsonDanger = Color(0xFFDC2626)
val CrimsonContainer = Color(0xFFFEE2E2)
val PurpleNerve = Color(0xFF7C3AED)
val SlateBg = Color(0xFFF1F5F9)
val CardWhite = Color(0xFFFFFFFF)
val SlateBorder = Color(0xFFE2E8F0)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)

private val AppColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = NavyPrimary,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberWarn,
    background = SlateBg,
    surface = CardWhite,
    surfaceVariant = Color(0xFFF8FAFC),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)

@Composable
fun MactMedLegalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
