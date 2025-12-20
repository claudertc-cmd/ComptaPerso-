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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------
// TappableRollingInt : wrapper cliquable autour du compteur
// - Démarre l'animation seul après 3 secondes.
// - Au clic, exécute l'action `onTapped`.
// ---------------------------------------------------------------------
@Composable
fun TappableRollingInt(
    value: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    onTapped: () -> Unit // Paramètre pour l'action de clic
) {
    // Indique si l'animation a démarré.
    var started by remember { mutableStateOf(false) }

    // Démarre l'animation automatiquement après 3 secondes.
    // Se relance si la valeur change.
    LaunchedEffect(value) {
        started = false // Affiche les placeholders pendant le délai
        delay(2000L)
        started = true
    }

    Box(
        modifier = modifier
            .clickable { onTapped() } // Exécute l'action de navigation au clic
    ) {
        RollingInt(
            value = value,
            fontSize = fontSize,
            showPlaceholdersOnly = !started
        )
    }
}

// ---------------------------------------------------------------------
// RollingInt : compteur principal
// - Formate la valeur sur 6 chiffres.
// - Orchestré "colonnes" de droite à gauche (unités → dizaines → centaines…).
// - Décide pour chaque digit s’il est en placeholder, animé, ou final.
// ---------------------------------------------------------------------
@Composable
fun RollingInt(
    value: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    showPlaceholdersOnly: Boolean = false
) {
    // Sécurise la valeur (évite les négatifs).
    val safeValue = value.coerceAtLeast(0)

    // Formate la valeur sur toujours 6 digits (zéros à gauche).
    // Exemple : 42 → "000042"
    val text = String.format("%06d", safeValue)
    val length = text.length

    // Index de la colonne actuellement en ANIMATION, compté depuis la droite :
    // 0 = unités, 1 = dizaines, 2 = centaines, etc.
    // -1 = aucune colonne animée (tout en placeholder).
    var currentIndexFromRight by remember { mutableStateOf(-1) }

    // Séquence d’animation colonne par colonne, déclenchée dès que
    // la valeur ou le mode "placeholders seulement" change.
    LaunchedEffect(value, showPlaceholdersOnly) {
        // Si on est en mode placeholders uniquement, on ne lance pas l’animation.
        if (showPlaceholdersOnly) {
            currentIndexFromRight = -1
            return@LaunchedEffect
        }

        // Petit délai avant de démarrer l'animation (cosmétique).
        delay(200L)

        // Paramètres pour chaque rouleau (digit).
        val loops = 8          // Nombre de tours complets 9→0 pour un digit.
        val stepDelay = 5L      // Temps (ms) entre deux changements de chiffre.
        // Durée totale approximative d'un rouleau (10 chiffres * loops) + petite marge.
        val perDigitDuration = loops * 10 * stepDelay + 5L

        // On fait avancer currentIndexFromRight pour chaque position :
        // i = 0 → unités, i = 1 → dizaines, etc.
        for (i in 0 until length) {
            currentIndexFromRight = i
            // On laisse le temps au rouleau courant d’achever ses tours.
            delay(perDigitDuration)
        }

        // À la fin, on met currentIndexFromRight au-delà de la dernière colonne
        // pour que toutes soient considérées comme FINAL.
        currentIndexFromRight = length
    }

    // Cadre global autour des rouleaux + symbole "€".
    Box(
        modifier = modifier
            .background(
                color = Color.DarkGray.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pour chaque caractère de la chaîne formatée.
            text.forEachIndexed { index, c ->
                if (c.isDigit()) {
                    // Position depuis la droite : 0 = unités, 1 = dizaines, etc.
                    val positionFromRight = (length - 1) - index
                    val targetDigit = c.digitToInt()

                    // Détermine l’état du digit (PLACEHOLDER, ANIMATED, FINAL)
                    // selon l’avancement de currentIndexFromRight et du flag global.
                    val state = if (showPlaceholdersOnly) {
                        // Mode "------" : tous les digits restent en placeholder.
                        DigitState.PLACEHOLDER
                    } else {
                        when {
                            // Colonne déjà passée : elle doit afficher sa valeur finale.
                            positionFromRight < currentIndexFromRight -> DigitState.FINAL
                            // Colonne en cours : elle est en animation.
                            positionFromRight == currentIndexFromRight -> DigitState.ANIMATED
                            // Colonne pas encore atteinte : "-" (en attente).
                            else -> DigitState.PLACEHOLDER
                        }
                    }

                    // Dessine un seul digit selon l’état calculé.
                    SequencedRollingDigit(
                        target = targetDigit,
                        fontSize = fontSize,
                        state = state
                    )

                    // Légère séparation horizontale entre les rouleaux.
                    Spacer(modifier = Modifier.width(0.1.dp))
                } else {
                    // Si un jour il y avait un caractère non numérique dans text,
                    // on l’afficherait directement ici.
                    Text(
                        c.toString(),
                        fontSize = fontSize,
                        color = Color.White
                    )
                }
            }

            // Petit espace avant le rouleau "€".
            Spacer(modifier = Modifier.width(4.dp))

            // Rouleau fixe pour le symbole "€" (pas d’animation).
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 48.dp)
                    .shadow(5.dp, RoundedCornerShape(7.dp))
                    .background(Color.Black, RoundedCornerShape(7.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "€",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// États possibles pour un digit :
// - PLACEHOLDER : tiret "-"
// - ANIMATED : rouleau qui tourne 9→0 en boucle
// - FINAL : chiffre cible affiché sans animation
// ---------------------------------------------------------------------
private enum class DigitState { PLACEHOLDER, ANIMATED, FINAL }

// ---------------------------------------------------------------------
// SequencedRollingDigit : gère l’affichage d’un seul chiffre
// selon son état (PLACEHOLDER, ANIMATED, FINAL).
// ---------------------------------------------------------------------
@Composable
private fun SequencedRollingDigit(
    target: Int,
    fontSize: TextUnit,
    state: DigitState
) {
    // Style commun à tous les rouleaux (digit individuel).
    val baseModifier = Modifier
        .size(width = 34.dp, height = 48.dp)
        .shadow(5.dp, RoundedCornerShape(7.dp))
        .background(Color.Black, RoundedCornerShape(7.dp))
        .border(1.dp, Color.DarkGray, RoundedCornerShape(7.dp))

    when (state) {
        // 1) PLACEHOLDER : affiche juste un tiret, sans animation.
        DigitState.PLACEHOLDER -> {
            Box(
                modifier = baseModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 2) FINAL : affiche directement le chiffre cible, sans animation.
        DigitState.FINAL -> {
            Box(
                modifier = baseModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = target.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 3) ANIMATED : boucle 9→0 plusieurs fois, puis termine sur target.
        DigitState.ANIMATED -> {
            // Chiffre actuellement affiché sur ce rouleau.
            var current by remember { mutableStateOf(0) }

            LaunchedEffect(target) {
                // Nombre de tours complets 9→0.
                val loops = 5
                // Délai entre deux changements de chiffre (ms).
                val stepDelay = 20L

                // Valeur de départ (peu importe, on va immédiatement la faire tourner).
                current = 0

                // Chaque tour a 10 chiffres (0→9 ou 9→0 selon la logique).
                repeat(loops * 10) {
                    // On décrémente : 0 → 9 → 8 → ... → 1 → 0
                    current = if (current == 0) 9 else current - 1
                    delay(stepDelay)
                }

                // À la fin de la boucle, on se cale sur la vraie valeur cible.
                current = target
            }

            Box(
                modifier = baseModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = current.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
