package com.healthos.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = MidnightInk,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealBright,
    secondary = BlueElectric,
    onSecondary = MidnightInk,
    secondaryContainer = BlueDeep,
    onSecondaryContainer = BlueBright,
    tertiary = PurpleAccent,
    onTertiary = MidnightInk,
    tertiaryContainer = PurpleDeep,
    onTertiaryContainer = PurpleAccent,
    background = MidnightInk,
    onBackground = TextPrimary,
    surface = PanelSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderMedium,
    error = CoralCritical,
    onError = TextPrimary,
    errorContainer = CoralDeep,
    onErrorContainer = CoralBright,
)

@Composable
fun HealthOsTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = HealthTypography,
        shapes = HealthShapes,
        content = content,
    )
}
