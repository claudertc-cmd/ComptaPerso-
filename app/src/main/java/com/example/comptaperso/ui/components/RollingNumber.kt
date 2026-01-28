package com.example.comptaperso.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * `TappableRollingInt` est un composant qui encapsule `RollingInt` pour le rendre cliquable
 * et pour contrôler le démarrage de l'animation.
 *
 * @param value La valeur entière à afficher.
 * @param onTapped Callback déclenché lorsque le composant est cliqué.
 * @param modifier Modificateur pour personnaliser l'apparence.
 * @param fontSize Taille de la police.
 * @param showBackground Si vrai, affiche un fond sombre derrière le nombre.
 * @param showDigitFrames Si vrai, affiche des cadres individuels pour chaque chiffre.
 * @param showEuroSymbol Si vrai, affiche le symbole "€" à la fin.
 */
@Composable
fun TappableRollingInt(
    value: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    onTapped: () -> Unit,
    showBackground: Boolean = true,
    showDigitFrames: Boolean = true,
    showEuroSymbol: Boolean = true
) {
    var started by remember { mutableStateOf(false) }

    // Démarre l'animation automatiquement après un délai de 2 secondes.
    LaunchedEffect(value) {
        started = false // Affiche les placeholders pendant le délai.
        delay(2000L)
        started = true
    }

    Box(modifier = modifier.clickable { onTapped() }) {
        RollingInt(
            value = value,
            fontSize = fontSize,
            showPlaceholdersOnly = !started, // Contrôle l'état de l'animation.
            showBackground = showBackground,
            showDigitFrames = showDigitFrames,
            showEuroSymbol = showEuroSymbol
        )
    }
}

/**
 * `RollingInt` est le composant principal qui affiche un nombre entier avec une animation de roulement,
 * chiffre par chiffre, de droite à gauche.
 *
 * @param value La valeur à afficher.
 * @param showPlaceholdersOnly Si vrai, n'affiche que des placeholders ("-") sans lancer l'animation.
 */
@Composable
fun RollingInt(
    value: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    showPlaceholdersOnly: Boolean = false,
    showBackground: Boolean = true,
    showDigitFrames: Boolean = true,
    showEuroSymbol: Boolean = true
) {
    val safeValue = value.coerceAtLeast(0)
    val text = String.format(Locale.US, "%06d", safeValue) // Formate sur 6 chiffres.
    val length = text.length

    // Index de la colonne en cours d'animation (depuis la droite).
    var currentIndexFromRight by remember { mutableIntStateOf(-1) }

    // Gère la séquence d'animation colonne par colonne.
    LaunchedEffect(value, showPlaceholdersOnly) {
        if (showPlaceholdersOnly) {
            currentIndexFromRight = -1
            return@LaunchedEffect
        }
        delay(200L) // Délai cosmétique.
        for (i in 0 until length) {
            currentIndexFromRight = i
            delay(5 * 10 * 5L + 5L) // Durée de l'animation d'un chiffre.
        }
        currentIndexFromRight = length // Marque la fin de l'animation.
    }

    val containerModifier = if (showBackground) {
        modifier.background(Color.DarkGray.copy(alpha = 0.9f), RoundedCornerShape(12.dp)).padding(horizontal = 4.dp, vertical = 4.dp)
    } else {
        modifier
    }

    Box(modifier = containerModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            text.forEachIndexed { index, c ->
                if (c.isDigit()) {
                    val positionFromRight = (length - 1) - index
                    val targetDigit = c.digitToInt()
                    val state = if (showPlaceholdersOnly) {
                        DigitState.PLACEHOLDER
                    } else {
                        when {
                            positionFromRight < currentIndexFromRight -> DigitState.FINAL
                            positionFromRight == currentIndexFromRight -> DigitState.ANIMATED
                            else -> DigitState.PLACEHOLDER
                        }
                    }
                    SequencedRollingDigit(targetDigit, fontSize, state, showDigitFrames)
                    if (showDigitFrames) Spacer(modifier = Modifier.width(0.1.dp))
                }
            }
            if (showEuroSymbol) {
                Spacer(modifier = Modifier.width(4.dp))
                val digitWidth = with(LocalDensity.current) { fontSize.toDp() } * 1.4f
                val digitHeight = with(LocalDensity.current) { fontSize.toDp() } * 2.0f
                val euroBoxModifier = if (showDigitFrames) {
                    Modifier.size(digitWidth, digitHeight).shadow(5.dp, RoundedCornerShape(7.dp)).background(Color.Black, RoundedCornerShape(7.dp)).border(1.dp, Color.DarkGray, RoundedCornerShape(7.dp))
                } else {
                    Modifier
                }
                Box(euroBoxModifier, contentAlignment = Alignment.Center) {
                    Text("€", fontSize = fontSize, fontWeight = FontWeight.Bold, color = if (showDigitFrames) Color.White else LocalContentColor.current)
                }
            }
        }
    }
}

// États possibles pour un chiffre : placeholder ("-"), en animation, ou final.
private enum class DigitState { PLACEHOLDER, ANIMATED, FINAL }

/**
 * `SequencedRollingDigit` gère l'affichage et l'animation d'un seul chiffre en fonction de son état.
 */
@Composable
private fun SequencedRollingDigit(
    target: Int,
    fontSize: TextUnit,
    state: DigitState,
    showFrame: Boolean
) {
    val digitWidth = with(LocalDensity.current) { fontSize.toDp() } * 1.4f
    val digitHeight = with(LocalDensity.current) { fontSize.toDp() } * 2.0f
    val baseModifier = if (showFrame) {
        Modifier.size(digitWidth, digitHeight).shadow(5.dp, RoundedCornerShape(7.dp)).background(Color.Black, RoundedCornerShape(7.dp)).border(1.dp, Color.DarkGray, RoundedCornerShape(7.dp))
    } else {
        Modifier.padding(horizontal = 1.dp)
    }
    val textColor = if (showFrame) Color.White else LocalContentColor.current

    when (state) {
        DigitState.PLACEHOLDER -> {
            Box(baseModifier, contentAlignment = Alignment.Center) {
                Text("-", fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
        DigitState.FINAL -> {
            Box(baseModifier, contentAlignment = Alignment.Center) {
                Text(target.toString(), fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
        DigitState.ANIMATED -> {
            var current by remember { mutableIntStateOf(0) }
            LaunchedEffect(target) {
                repeat(5 * 10) { // 5 tours de 10 chiffres.
                    current = if (current == 0) 9 else current - 1
                    delay(20L)
                }
                current = target // Termine sur le chiffre cible.
            }
            Box(baseModifier, contentAlignment = Alignment.Center) {
                Text(current.toString(), fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
    }
}
