package com.example.comptaperso

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * `BiometricAuthManager` est une classe qui encapsule toute la logique de l'authentification biométrique.
 * Elle vérifie si l'authentification est disponible, configure et affiche l'invite biométrique,
 * et gère les callbacks de succès, d'erreur ou d'échec.
 *
 * @param activity L'activité `FragmentActivity` nécessaire pour afficher l'invite biométrique.
 */
class BiometricAuthManager(
    private val activity: FragmentActivity
) {

    // `isAuthenticated` est un état observable par Compose qui indique si l'utilisateur est authentifié.
    var isAuthenticated by mutableStateOf(false)
        private set // Ne peut être modifié qu'à l'intérieur de cette classe.

    private val executor = ContextCompat.getMainExecutor(activity)
    private val biometricManager = BiometricManager.from(activity)

    // Configure l'apparence de l'invite biométrique (titre, sous-titre, etc.).
    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentification biométrique pour Comptaperso")
        .setSubtitle("Connectez-vous en utilisant vos informations biométriques")
        .setAllowedAuthenticators(
            // Autorise l'authentification forte (empreinte, visage) ou les identifiants de l'appareil (code PIN, schéma).
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    // Crée l'instance de `BiometricPrompt` avec les callbacks pour gérer les résultats.
    private val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            // Appelé lorsque l'authentification réussit.
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated = true // Met à jour l'état pour déverrouiller l'application.
            }

            // Appelé en cas d'erreur (ex: capteur indisponible, trop de tentatives).
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                // Affiche un message d'erreur et ferme l'application pour des raisons de sécurité.
                Toast.makeText(activity.applicationContext, "Erreur d'authentification: $errString", Toast.LENGTH_SHORT).show()
                activity.finish()
            }

            // Appelé lorsque l'authentification échoue (ex: empreinte non reconnue).
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(activity.applicationContext, "Authentification échouée", Toast.LENGTH_SHORT).show()
            }
        }
    )

    /**
     * Lance le processus d'authentification.
     * Vérifie d'abord si une méthode d'authentification est disponible sur l'appareil.
     */
    fun authenticate() {
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            // Si l'authentification est possible, affiche l'invite.
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            // Si aucune méthode biométrique n'est configurée, l'accès est accordé par défaut (fallback).
            // Pour une meilleure sécurité, on pourrait implémenter un autre mécanisme ici.
            else -> {
                isAuthenticated = true
            }
        }
    }
}
