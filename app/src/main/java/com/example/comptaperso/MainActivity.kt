package com.example.comptaperso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.comptaperso.data.DataRepository
import com.example.comptaperso.ui.theme.AppTheme

/**
 * `MainActivity` est l'activité principale et le point d'entrée de l'application.
 * Elle est responsable de la gestion de l'authentification biométrique,
 * et de la configuration de l'interface utilisateur avec Jetpack Compose.
 */
class MainActivity : FragmentActivity() {

    // `BiometricAuthManager` gère la logique de l'authentification biométrique.
    private val biometricAuthManager by lazy {
        BiometricAuthManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Active l'affichage bord à bord pour une interface utilisateur immersive.
        enableEdgeToEdge()

        // Définit le contenu de l'interface utilisateur avec Jetpack Compose.
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Initialise le `DataRepository` qui sert de source de vérité pour les données.
                    val dataRepository = DataRepository(applicationContext)

                    // Vérifie si l'utilisateur est déjà authentifié via la biométrie.
                    if (biometricAuthManager.isAuthenticated) {
                        // Si authentifié, affiche l'écran de connexion/principal.
                        LoginScreen(dataRepository, this)
                    } else {
                        // Sinon, affiche l'écran `BiometricGateScreen` pour demander l'authentification.
                        BiometricGateScreen(
                            onRequestAuth = { biometricAuthManager.authenticate() }
                        )
                    }
                }
            }
        }
    }
}
