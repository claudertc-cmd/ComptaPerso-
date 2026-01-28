package com.example.comptaperso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.DataRepository
import kotlinx.coroutines.launch

/**
 * `LoginScreen` gère l'authentification de l'utilisateur (connexion et inscription).
 * Il affiche un formulaire de connexion ou, si l'utilisateur est déjà connecté, il affiche le `AppShell` principal.
 *
 * @param dataRepository L'instance de `DataRepository` utilisée pour gérer les opérations d'authentification avec Firebase.
 * @param activity L'instance de `MainActivity` requise par `AppShell` pour gérer les callbacks du cycle de vie.
 */
@Composable
fun LoginScreen(dataRepository: DataRepository, activity: MainActivity) {
    // États pour les champs de saisie de l'email et du mot de passe.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // État pour afficher un message d'erreur en cas d'échec de l'authentification.
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // État pour suivre si l'utilisateur est actuellement connecté.
    var isSignedIn by remember { mutableStateOf(dataRepository.isSignedIn()) }
    // `CoroutineScope` pour lancer les opérations d'authentification asynchrones.
    val coroutineScope = rememberCoroutineScope()

    // Condition pour afficher l'écran principal ou l'écran de connexion.
    if (isSignedIn) {
        // Si l'utilisateur est connecté, affiche le `AppShell`.
        AppShell(activity = activity, onLogout = {
            dataRepository.signOut() // Déconnecte l'utilisateur.
            isSignedIn = false // Met à jour l'état pour réafficher l'écran de connexion.
        })
    } else {
        // Si l'utilisateur n'est pas connecté, affiche le formulaire de connexion/inscription.
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation() // Masque le mot de passe.
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Affiche un message d'erreur s'il y en a un.
            errorMessage?.let {
                Text(it, color = androidx.compose.ui.graphics.Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bouton pour la connexion.
            Button(onClick = {
                coroutineScope.launch {
                    val result = dataRepository.signIn(email, password)
                    if (result.isSuccess) {
                        isSignedIn = true
                        errorMessage = null
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Erreur de connexion"
                    }
                }
            }) {
                Text("Se connecter")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Bouton pour l'inscription.
            Button(onClick = {
                coroutineScope.launch {
                    val result = dataRepository.signUp(email, password)
                    if (result.isSuccess) {
                        isSignedIn = true
                        errorMessage = null
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Erreur d'inscription"
                    }
                }
            }) {
                Text("S'inscrire")
            }
        }
    }
}
