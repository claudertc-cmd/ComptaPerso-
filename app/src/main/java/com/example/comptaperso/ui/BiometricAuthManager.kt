package com.example.comptaperso

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val activity: FragmentActivity
) {

    // État observable par Compose
    var isAuthenticated by mutableStateOf(false)
        private set

    private val executor = ContextCompat.getMainExecutor(activity)
    private val biometricManager = BiometricManager.from(activity)

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentification biométrique pour Comptaperso")
        .setSubtitle("Connectez-vous en utilisant vos informations biométriques")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    private val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated = true
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    activity.applicationContext,
                    "Erreur d'authentification: $errString",
                    Toast.LENGTH_SHORT
                ).show()
                activity.finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    activity.applicationContext,
                    "Authentification échouée",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    fun authenticate() {
        when (
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Fallback : pour l’instant on laisse entrer
                isAuthenticated = true
            }
        }
    }
}
