package com.example.ui.theme

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

private val SkyBlueColorScheme = lightColorScheme(
    primary = SkyNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = SkyLightContainer,
    onPrimaryContainer = SkyOnLightContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = Color(0xFF1F618D),
    onTertiary = Color.White,
    tertiaryContainer = SkyLightContainer,
    background = SkyBackground,
    onBackground = SkyOnLightContainer,
    surface = Color.White,
    onSurface = SkyOnLightContainer,
    surfaceVariant = SkySurfaceVariant,
    onSurfaceVariant = Color(0xFF3E5669),
    outline = Color(0xFFC2D6E8),
    error = ErrorCrimson,
    onError = Color.White
)

private val EmeraldCreamColorScheme = lightColorScheme(
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
    onBackground = Color(0xFF18241C),
    surface = Color.White,
    onSurface = Color(0xFF18241C),
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = Color(0xFF56665C),
    outline = Color(0xFFD8CEBC),
    error = ErrorCrimson,
    onError = Color.White
)

private val SandGoldColorScheme = lightColorScheme(
    primary = SandBrownPrimary,
    onPrimary = Color.White,
    primaryContainer = SandGoldContainer,
    onPrimaryContainer = SandOnContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = Color(0xFF8C5D27),
    onTertiary = Color.White,
    tertiaryContainer = SandGoldContainer,
    background = SandWarmBackground,
    onBackground = SandOnContainer,
    surface = Color.White,
    onSurface = SandOnContainer,
    surfaceVariant = SandCardVariant,
    onSurfaceVariant = Color(0xFF665138),
    outline = Color(0xFFDECBA8),
    error = ErrorCrimson,
    onError = Color.White
)

private val WhiteBlackColorScheme = lightColorScheme(
    primary = MonoBlackPrimary,
    onPrimary = Color.White,
    primaryContainer = MonoLightContainer,
    onPrimaryContainer = MonoOnLightContainer,
    secondary = MonoCharcoalSecondary,
    onSecondary = Color.White,
    secondaryContainer = MonoSoftGrayContainer,
    onSecondaryContainer = MonoOnSoftGrayContainer,
    tertiary = Color(0xFF333333),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDEDED),
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

private val DarkNightColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF242429),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C32),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFD0D0D6),
    onTertiary = Color.Black,
    background = DarkPureBlackBackground,
    onBackground = Color.White,
    surface = DarkElevatedSurface,
    onSurface = Color.White,
    surfaceVariant = DarkElevatedSurfaceVariant,
    onSurfaceVariant = Color(0xFFC8C8D0),
    outline = Color(0xFF3E3E46)
)

val SirahShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SirahKidsTheme(
    selectedTheme: AppBackgroundTheme = AppBackgroundTheme.SKY_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppBackgroundTheme.SKY_BLUE -> SkyBlueColorScheme
        AppBackgroundTheme.EMERALD_CREAM -> EmeraldCreamColorScheme
        AppBackgroundTheme.SAND_GOLD -> SandGoldColorScheme
        AppBackgroundTheme.WHITE_BLACK -> WhiteBlackColorScheme
        AppBackgroundTheme.DARK_NIGHT -> DarkNightColorScheme
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SirahTypography,
            shapes = SirahShapes,
            content = content
        )
    }
}
