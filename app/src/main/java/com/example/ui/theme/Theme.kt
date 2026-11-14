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
    primary = MonoBlackPrimary,
    onPrimary = Color.White,
    primaryContainer = MonoLightContainer,
    onPrimaryContainer = MonoOnLightContainer,
    secondary = MonoCharcoalSecondary,
    onSecondary = Color.White,
    secondaryContainer = MonoSoftGrayContainer,
    onSecondaryContainer = MonoOnSoftGrayContainer,
    tertiary = MonoSlateTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MonoTertiaryContainer,
    onTertiaryContainer = MonoOnLightContainer,
    background = PureWhiteBackground,
    onBackground = CrispBlackInk,
    surface = SoftWhiteSurface,
    onSurface = CrispBlackInk,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = MutedGrayInk,
    outline = OutlineGray,
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMonoWhitePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF242424),
    onPrimaryContainer = Color.White,
    secondary = DarkMonoCharcoalSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2B2B2B),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFD0D0D0),
    onTertiary = Color.Black,
    background = DarkPureBlackBackground,
    onBackground = Color.White,
    surface = DarkElevatedSurface,
    onSurface = Color.White,
    surfaceVariant = DarkElevatedSurfaceVariant,
    onSurfaceVariant = Color(0xFFC8C8C8),
    outline = Color(0xFF3E3E3E)
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
