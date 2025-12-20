package com.example.comptaperso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.comptaperso.ui.theme.AppTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class MainActivity : FragmentActivity() {

    private val biometricAuthManager by lazy {
        BiometricAuthManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val firebaseManager = FirebaseStorageManager()

                    if (biometricAuthManager.isAuthenticated) {
                        // Ton écran habituel
                        LoginScreen(firebaseManager, this)
                    } else {
                        // Écran “porte d’entrée” qui déclenche la biométrie
                        BiometricGateScreen(
                            onRequestAuth = { biometricAuthManager.authenticate() }
                        )
                    }
                }
            }
        }
    }
}
