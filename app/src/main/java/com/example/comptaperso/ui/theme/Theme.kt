package com.example.comptaperso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Définit les palettes de couleurs complètes pour chaque thème en mode clair et sombre.
// Ces `colorScheme` sont utilisés par `MaterialTheme` pour appliquer un style cohérent à l'application.

// Palette de couleurs pour le thème "Spring" en mode sombre.
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

// Palette de couleurs pour le thème "Spring" en mode clair.
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

// Palette de couleurs pour le thème "BoursoBank" en mode clair.
private val BoursoBankColorScheme = lightColorScheme(
    primary = boursoPrimary,
    onPrimary = onBoursoPrimary,
    primaryContainer = Color(0xFFFFD9E2), // Conteneur rose clair
    onPrimaryContainer = Color(0xFF3E001D), // Texte foncé sur conteneur
    secondary = boursoSecondary,
    onSecondary = onBoursoSecondary,
    tertiary = Color(0xFF7DDA9E), // Touche de vert
    background = boursoBackground,
    onBackground = onBoursoBackground,
    surface = boursoSurface,
    onSurface = onBoursoSurface,
    surfaceVariant = Color(0xFFF9FAFE)
)

// Palette de couleurs pour le thème "BoursoBank" en mode sombre.
private val BoursoBankDarkColorScheme = darkColorScheme(
    primary = boursoPrimary, // La couleur primaire reste la même
    onPrimary = onBoursoPrimary,
    primaryContainer = Color(0xFF5A002D), // Conteneur plus sombre
    onPrimaryContainer = Color(0xFFFFD9E2), // Texte clair sur conteneur
    secondary = boursoSecondary,
    onSecondary = onBoursoSecondary,
    tertiary = Color(0xFF7DDA9E),
    background = Color(0xFF1C1B1F), // Fond sombre
    onBackground = Color(0xFFE6E1E5), // Texte clair
    surface = Color(0xFF2E2D31), // Surface sombre
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2E2D31)
)

// Palette de couleurs pour le thème "Fortuneo" en mode clair.
private val FortuneoColorScheme = lightColorScheme(
    primary = fortuneoPrimary,
    onPrimary = onFortuneoPrimary,
    primaryContainer = Color(0xFFB9F6CA), // Conteneur vert clair
    onPrimaryContainer = Color(0xFF00210E), // Texte vert foncé sur conteneur
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

// Palette de couleurs pour le thème "Fortuneo" en mode sombre.
private val FortuneoDarkColorScheme = darkColorScheme(
    primary = fortuneoPrimary,
    onPrimary = onFortuneoPrimary,
    primaryContainer = Color(0xFF00522A), // Conteneur plus sombre
    onPrimaryContainer = Color(0xFFB9F6CA), // Texte clair sur conteneur
    secondary = fortuneoSecondary,
    onSecondary = onFortuneoSecondary,
    tertiary = fortuneoTertiary,
    background = Color(0xFF1A1C19), // Fond sombre
    onBackground = Color(0xFFE2E3DD), // Texte clair
    surface = Color(0xFF2E312D), // Surface sombre
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF2E312D)
)

/**
 * Énumération des thèmes disponibles dans l'application.
 * Permet de sélectionner un thème de manière programmatique.
 */
enum class Theme {
    SPRING, BOURSOBANK, FORTUNEO
}

/**
 * `AppTheme` est le composable qui applique un thème Material à l'ensemble de l'application ou à une partie de celle-ci.
 * Il sélectionne la palette de couleurs appropriée en fonction du thème choisi et du mode (clair/sombre).
 *
 * @param theme Le thème à appliquer (par défaut `Theme.SPRING`).
 * @param darkTheme Indique si le thème sombre doit être utilisé (par défaut, basé sur les paramètres système).
 * @param dynamicColor Indique si les couleurs dynamiques (Material You) doivent être utilisées (Android 12+).
 * @param content Le contenu de l'interface utilisateur auquel le thème sera appliqué.
 */
@Composable
fun AppTheme(
    theme: Theme = Theme.SPRING,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Si les couleurs dynamiques sont activées (Android 12+, minSdk 33 garantit le support).
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Sélectionne la palette de couleurs en fonction du thème et du mode (clair/sombre).
        theme == Theme.BOURSOBANK -> if (darkTheme) BoursoBankDarkColorScheme else BoursoBankColorScheme
        theme == Theme.FORTUNEO -> if (darkTheme) FortuneoDarkColorScheme else FortuneoColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Applique la typographie définie dans `Type.kt`.
        content = content
    )
}
