package com.example.comptaperso.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceVariantDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceVariantLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight
)

private val BoursoBankColorScheme = lightColorScheme(
    primary = boursoPrimary,
    onPrimary = onBoursoPrimary,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = boursoSecondary,
    onSecondary = onBoursoSecondary,
    tertiary = Color(0xFF7DDA9E),
    background = boursoBackground,
    onBackground = onBoursoBackground,
    surface = boursoSurface,
    onSurface = onBoursoSurface,
    surfaceVariant = Color(0xFFF9FAFE)
)

private val BoursoBankDarkColorScheme = darkColorScheme(
    primary = boursoPrimary, // Keeping the same primary color
    onPrimary = onBoursoPrimary,
    primaryContainer = Color(0xFF5A002D), // Darker container
    onPrimaryContainer = Color(0xFFFFD9E2), // Lighter text on dark container
    secondary = boursoSecondary, // Keeping the same secondary color
    onSecondary = onBoursoSecondary,
    tertiary = Color(0xFF7DDA9E), // Keeping the same tertiary color
    background = Color(0xFF1C1B1F), // Dark background
    onBackground = Color(0xFFE6E1E5), // Light text on dark background
    surface = Color(0xFF2E2D31), // Dark surface
    onSurface = Color(0xFFE6E1E5), // Light text on dark surface
    surfaceVariant = Color(0xFF2E2D31) // Dark surface variant
)

private val FortuneoColorScheme = lightColorScheme(
    primary = fortuneoPrimary,
    onPrimary = onFortuneoPrimary,
    primaryContainer = Color(0xFFB9F6CA), // Light Green for containers
    onPrimaryContainer = Color(0xFF00210E), // Dark Green for text on container
    secondary = fortuneoSecondary,
    onSecondary = onFortuneoSecondary,
    tertiary = fortuneoTertiary,
    onTertiary = onFortuneoTertiary,
    background = fortuneoBackground,
    onBackground = onFortuneoBackground,
    surface = fortuneoSurface,
    onSurface = onFortuneoSurface,
    surfaceVariant = Color(0xFFF5F6F8)
)

private val FortuneoDarkColorScheme = darkColorScheme(
    primary = fortuneoPrimary, // Keeping the same primary color
    onPrimary = onFortuneoPrimary,
    primaryContainer = Color(0xFF00522A), // Darker container
    onPrimaryContainer = Color(0xFFB9F6CA), // Lighter text on dark container
    secondary = fortuneoSecondary, // Keeping the same secondary color
    onSecondary = onFortuneoSecondary,
    tertiary = fortuneoTertiary, // Keeping the same tertiary color
    background = Color(0xFF1A1C19), // Dark background
    onBackground = Color(0xFFE2E3DD), // Light text on dark background
    surface = Color(0xFF2E312D), // Dark surface
    onSurface = Color(0xFFE2E3DD), // Light text on dark surface
    surfaceVariant = Color(0xFF2E312D) // Dark surface variant
)


enum class Theme {
    SPRING, BOURSOBANK, FORTUNEO
}

@Composable
fun AppTheme(
    theme: Theme = Theme.SPRING,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        theme == Theme.BOURSOBANK -> if (darkTheme) BoursoBankDarkColorScheme else BoursoBankColorScheme
        theme == Theme.FORTUNEO -> if (darkTheme) FortuneoDarkColorScheme else FortuneoColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = AppTypography,
        content = content
    )
}
