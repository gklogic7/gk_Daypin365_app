package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalDayPinDarkTheme = staticCompositionLocalOf { false }

private val DayPinLightColors = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = HoneySecondary,
    onSecondary = HoneyOnSecondary,
    secondaryContainer = HoneyContainer,
    onSecondaryContainer = HoneyOnContainer,
    tertiary = SageTealTertiary,
    onTertiary = SageTealOnTertiary,
    tertiaryContainer = SageTealContainer,
    onTertiaryContainer = SageTealOnContainer,
    background = WarmLinenBackground,
    onBackground = EspressoInk,
    surface = SoftIvorySurface,
    onSurface = EspressoInk,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = MutedUmber,
    surfaceContainerLow = WarmLinenBackground,
    surfaceContainer = ParchmentSurfaceVariant,
    surfaceContainerHigh = WarmSandContainer,
    outline = WarmOutline
)

private val DayPinDarkColors = darkColorScheme(
    primary = ApricotPrimaryDark,
    onPrimary = ApricotOnPrimaryDark,
    primaryContainer = ApricotContainerDark,
    onPrimaryContainer = ApricotOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldOnContainerDark,
    tertiary = SeafoamTertiaryDark,
    onTertiary = SeafoamOnTertiaryDark,
    tertiaryContainer = SeafoamContainerDark,
    onTertiaryContainer = SeafoamOnContainerDark,
    background = CharcoalBackground,
    onBackground = CreamInk,
    surface = ObsidianSurface,
    onSurface = CreamInk,
    surfaceVariant = WarmSlateSurfaceVariant,
    onSurfaceVariant = WarmStone,
    surfaceContainerLow = CharcoalBackground,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = WarmSlateSurfaceVariant,
    outline = DarkOutline
)

private val OceanMistColors = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = Color.White,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanInk,
    tertiary = SageTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = OceanSecondaryContainer,
    onTertiaryContainer = OceanInk,
    background = OceanBg,
    onBackground = OceanInk,
    surface = OceanSurface,
    onSurface = OceanInk,
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = OceanMutedInk,
    surfaceContainerLow = OceanBg,
    surfaceContainer = OceanSurfaceVariant,
    surfaceContainerHigh = OceanSurfaceHigh,
    outline = OceanOutline
)

private val ForestSageColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = Color.White,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestInk,
    tertiary = SageTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SageTealContainer,
    onTertiaryContainer = SageTealOnContainer,
    background = ForestBg,
    onBackground = ForestInk,
    surface = ForestSurface,
    onSurface = ForestInk,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = ForestMutedInk,
    surfaceContainerLow = ForestBg,
    surfaceContainer = ForestSurfaceVariant,
    surfaceContainerHigh = ForestSurfaceHigh,
    outline = ForestOutline
)

private val SunsetCoralColors = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.White,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = Color.White,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetInk,
    tertiary = TerracottaPrimary,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = TerracottaOnContainer,
    background = SunsetBg,
    onBackground = SunsetInk,
    surface = SunsetSurface,
    onSurface = SunsetInk,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = SunsetMutedInk,
    surfaceContainerLow = SunsetBg,
    surfaceContainer = SunsetSurfaceVariant,
    surfaceContainerHigh = SunsetSurfaceHigh,
    outline = SunsetOutline
)

private val LavenderDuskColors = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = Color.White,
    primaryContainer = LavenderPrimaryContainer,
    onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary,
    onSecondary = Color.White,
    secondaryContainer = LavenderSecondaryContainer,
    onSecondaryContainer = LavenderInk,
    tertiary = OceanPrimary,
    onTertiary = Color.White,
    tertiaryContainer = OceanPrimaryContainer,
    onTertiaryContainer = OceanOnPrimaryContainer,
    background = LavenderBg,
    onBackground = LavenderInk,
    surface = LavenderSurface,
    onSurface = LavenderInk,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = LavenderMutedInk,
    surfaceContainerLow = LavenderBg,
    surfaceContainer = LavenderSurfaceVariant,
    surfaceContainerHigh = LavenderSurfaceHigh,
    outline = LavenderOutline
)

private val MidnightIndigoColors = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = IndigoOnPrimaryContainer,
    secondary = IndigoSecondary,
    onSecondary = IndigoOnPrimary,
    secondaryContainer = IndigoSecondaryContainer,
    onSecondaryContainer = IndigoInk,
    tertiary = SeafoamTertiaryDark,
    onTertiary = SeafoamOnTertiaryDark,
    tertiaryContainer = SeafoamContainerDark,
    onTertiaryContainer = SeafoamOnContainerDark,
    background = IndigoBg,
    onBackground = IndigoInk,
    surface = IndigoSurface,
    onSurface = IndigoInk,
    surfaceVariant = IndigoSurfaceVariant,
    onSurfaceVariant = IndigoMutedInk,
    surfaceContainerLow = IndigoBg,
    surfaceContainer = IndigoSurface,
    surfaceContainerHigh = IndigoSurfaceVariant,
    outline = IndigoOutline
)

private val AmberEmberColors = darkColorScheme(
    primary = EmberPrimary,
    onPrimary = EmberOnPrimary,
    primaryContainer = EmberPrimaryContainer,
    onPrimaryContainer = EmberOnPrimaryContainer,
    secondary = EmberSecondary,
    onSecondary = EmberOnPrimary,
    secondaryContainer = EmberSecondaryContainer,
    onSecondaryContainer = EmberInk,
    tertiary = GoldSecondaryDark,
    onTertiary = GoldOnSecondaryDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = GoldOnContainerDark,
    background = EmberBg,
    onBackground = EmberInk,
    surface = EmberSurface,
    onSurface = EmberInk,
    surfaceVariant = EmberSurfaceVariant,
    onSurfaceVariant = EmberMutedInk,
    surfaceContainerLow = EmberBg,
    surfaceContainer = EmberSurface,
    surfaceContainerHigh = EmberSurfaceVariant,
    outline = EmberOutline
)

private val EmeraldNightColors = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnPrimary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = EmeraldInk,
    tertiary = IndigoSecondary,
    onTertiary = EmeraldOnPrimary,
    tertiaryContainer = IndigoSecondaryContainer,
    onTertiaryContainer = EmeraldInk,
    background = EmeraldBg,
    onBackground = EmeraldInk,
    surface = EmeraldSurface,
    onSurface = EmeraldInk,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = EmeraldMutedInk,
    surfaceContainerLow = EmeraldBg,
    surfaceContainer = EmeraldSurface,
    surfaceContainerHigh = EmeraldSurfaceVariant,
    outline = EmeraldOutline
)

private val CrimsonVelvetColors = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = CrimsonOnPrimary,
    primaryContainer = CrimsonPrimaryContainer,
    onPrimaryContainer = CrimsonOnPrimaryContainer,
    secondary = CrimsonSecondary,
    onSecondary = CrimsonOnPrimary,
    secondaryContainer = CrimsonSecondaryContainer,
    onSecondaryContainer = CrimsonInk,
    tertiary = ApricotPrimaryDark,
    onTertiary = ApricotOnPrimaryDark,
    tertiaryContainer = ApricotContainerDark,
    onTertiaryContainer = ApricotOnContainerDark,
    background = CrimsonBg,
    onBackground = CrimsonInk,
    surface = CrimsonSurface,
    onSurface = CrimsonInk,
    surfaceVariant = CrimsonSurfaceVariant,
    onSurfaceVariant = CrimsonMutedInk,
    surfaceContainerLow = CrimsonBg,
    surfaceContainer = CrimsonSurface,
    surfaceContainerHigh = CrimsonSurfaceVariant,
    outline = CrimsonOutline
)

val DayPinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

enum class ThemeMode(
    val displayName: String,
    val isDarkPalette: Boolean?,
    val previewPrimary: Color,
    val previewSurface: Color
) {
    SYSTEM("System", null, TerracottaPrimary, WarmLinenBackground),
    LIGHT("Warm Paper", false, TerracottaPrimary, WarmLinenBackground),
    OCEAN_MIST("Ocean Mist", false, OceanPrimary, OceanBg),
    FOREST_SAGE("Forest Sage", false, ForestPrimary, ForestBg),
    SUNSET_CORAL("Sunset Coral", false, SunsetPrimary, SunsetBg),
    LAVENDER_DUSK("Lavender Dusk", false, LavenderPrimary, LavenderBg),
    DARK("Slate Dark", true, ApricotPrimaryDark, ObsidianSurface),
    MIDNIGHT_INDIGO("Midnight Indigo", true, IndigoPrimary, IndigoSurface),
    AMBER_EMBER("Amber Ember", true, EmberPrimary, EmberSurface),
    EMERALD_NIGHT("Emerald Night", true, EmeraldPrimary, EmeraldSurface),
    CRIMSON_VELVET("Crimson Velvet", true, CrimsonPrimary, CrimsonSurface);

    companion object {
        val curatedThemes: List<ThemeMode>
            get() = entries.filter { it != SYSTEM }
    }
}

fun resolveColorScheme(themeMode: ThemeMode, systemDark: Boolean): ColorScheme {
    return when (themeMode) {
        ThemeMode.SYSTEM -> if (systemDark) DayPinDarkColors else DayPinLightColors
        ThemeMode.LIGHT -> DayPinLightColors
        ThemeMode.OCEAN_MIST -> OceanMistColors
        ThemeMode.FOREST_SAGE -> ForestSageColors
        ThemeMode.SUNSET_CORAL -> SunsetCoralColors
        ThemeMode.LAVENDER_DUSK -> LavenderDuskColors
        ThemeMode.DARK -> DayPinDarkColors
        ThemeMode.MIDNIGHT_INDIGO -> MidnightIndigoColors
        ThemeMode.AMBER_EMBER -> AmberEmberColors
        ThemeMode.EMERALD_NIGHT -> EmeraldNightColors
        ThemeMode.CRIMSON_VELVET -> CrimsonVelvetColors
    }
}

@Composable
fun DayPinTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = themeMode.isDarkPalette ?: systemDark

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> resolveColorScheme(themeMode, systemDark)
    }

    CompositionLocalProvider(LocalDayPinDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DayPinTypography,
            shapes = DayPinShapes,
            content = content
        )
    }
}

@Composable
fun stickySurfaceColor(colorName: String, darkTheme: Boolean = LocalDayPinDarkTheme.current): Color {
    return when (colorName) {
        "SAGE" -> if (darkTheme) StickySageDark else StickySageLight
        "SKY" -> if (darkTheme) StickySkyDark else StickySkyLight
        "BLUSH" -> if (darkTheme) StickyBlushDark else StickyBlushLight
        else -> if (darkTheme) StickyButtercupDark else StickyButtercupLight
    }
}

fun stickyBorderColor(colorName: String): Color {
    return when (colorName) {
        "SAGE" -> StickySageBorder
        "SKY" -> StickySkyBorder
        "BLUSH" -> StickyBlushBorder
        else -> StickyButtercupBorder
    }
}
