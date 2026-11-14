package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    background = CreamBackground,
    onBackground = InkDark,
    surface = WarmSurface,
    onSurface = InkDark,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = OutlineSand,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = Color(0xFF062E17),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFD7EFE0),
    secondary = DarkGoldSecondary,
    onSecondary = Color(0xFF3E2900),
    secondaryContainer = Color(0xFF5A3D00),
    onSecondaryContainer = Color(0xFFFFF0CC),
    tertiary = Color(0xFF7FD0D1),
    onTertiary = Color(0xFF003738),
    background = DarkBackground,
    onBackground = Color(0xFFE9F0EA),
    surface = DarkSurface,
    onSurface = Color(0xFFE9F0EA),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C4BA),
    outline = Color(0xFF3A4D41)
)

val SirahShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SirahKidsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SirahTypography,
            shapes = SirahShapes,
            content = content
        )
    }
}
