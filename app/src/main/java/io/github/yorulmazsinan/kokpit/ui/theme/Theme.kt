package io.github.yorulmazsinan.kokpit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = KokpitColors.Accent,
    onPrimary = KokpitColors.Background,
    primaryContainer = KokpitColors.AccentContainer,
    onPrimaryContainer = KokpitColors.OnAccentContainer,
    background = KokpitColors.Background,
    onBackground = KokpitColors.TextPrimary,
    surface = KokpitColors.Card,
    onSurface = KokpitColors.TextPrimary,
    surfaceVariant = KokpitColors.Card,
    onSurfaceVariant = KokpitColors.TextSecondary,
    outline = KokpitColors.CardBorder,
    tertiary = KokpitColors.Warm,
)

private val LightScheme = lightColorScheme(
    primary = KokpitColors.LightAccent,
    onPrimary = KokpitColors.LightCard,
    primaryContainer = KokpitColors.LightAccentContainer,
    onPrimaryContainer = KokpitColors.LightOnAccentContainer,
    background = KokpitColors.LightBackground,
    onBackground = KokpitColors.LightTextPrimary,
    surface = KokpitColors.LightCard,
    onSurface = KokpitColors.LightTextPrimary,
    surfaceVariant = KokpitColors.LightCard,
    onSurfaceVariant = KokpitColors.LightTextSecondary,
    tertiary = KokpitColors.LightAccent,
)

@Composable
fun KokpitTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = KokpitTypography,
        content = content,
    )
}
