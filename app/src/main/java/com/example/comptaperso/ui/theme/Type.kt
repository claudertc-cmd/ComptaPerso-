package com.example.comptaperso.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Ce fichier définit les styles de typographie pour l'application, en utilisant le système de types de Material 3.
// Vous pouvez personnaliser la police, la graisse, la taille, etc., pour chaque style de texte.

// `Typography` est un conteneur pour les styles de texte de Material Design.
val Typography = Typography(
    // `bodyLarge` est le style de texte par défaut pour le corps du texte.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // Utilise la police par défaut du système.
        fontWeight = FontWeight.Normal, // Graisse de police normale.
        fontSize = 16.sp, // Taille de la police.
        lineHeight = 24.sp, // Hauteur de ligne pour une meilleure lisibilité.
        letterSpacing = 0.5.sp // Espacement entre les lettres.
    )

    /*
    // Vous pouvez décommenter et personnaliser d'autres styles de texte ici.
    // Par exemple, pour les titres ou les légendes.

    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
