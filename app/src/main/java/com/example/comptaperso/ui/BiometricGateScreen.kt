package com.example.comptaperso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * `BiometricGateScreen` est un écran "porte d'entrée" qui déclenche l'authentification biométrique.
 * Il ne contient pas d'interface utilisateur visible, mais lance immédiatement la demande d'authentification.
 *
 * @param onRequestAuth Callback qui déclenche la logique d'authentification biométrique.
 */
@Composable
fun BiometricGateScreen(
    onRequestAuth: () -> Unit
) {
    // `LaunchedEffect(Unit)` s'exécute une seule fois lorsque le composant est affiché.
    LaunchedEffect(Unit) {
        onRequestAuth() // Appelle la fonction pour démarrer l'authentification.
    }
    // Ici, on pourrait afficher un indicateur de chargement ou un simple texte
    // pour informer l'utilisateur que la vérification est en cours.
    // Par exemple: 
    // Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    //     Text("Vérification biométrique en cours...")
    // }
}
