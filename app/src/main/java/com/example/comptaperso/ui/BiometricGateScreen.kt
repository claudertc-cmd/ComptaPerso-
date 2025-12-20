package com.example.comptaperso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun BiometricGateScreen(
    onRequestAuth: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRequestAuth()
    }
    // Tu peux ajouter ici un écran de chargement / texte “Vérification en cours…”
}
