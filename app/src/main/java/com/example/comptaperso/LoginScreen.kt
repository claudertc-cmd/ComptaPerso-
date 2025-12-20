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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(firebaseManager: FirebaseStorageManager, activity: MainActivity) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSignedIn by remember { mutableStateOf(firebaseManager.isSignedIn()) }
    val coroutineScope = rememberCoroutineScope()

    if (isSignedIn) {
        AppShell(activity = activity, onLogout = {
            firebaseManager.signOut()
            isSignedIn = false
        })
    } else {
        // Affiche les champs de connexion/inscription
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
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            errorMessage?.let {
                Text(it, color = androidx.compose.ui.graphics.Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = {
                coroutineScope.launch {
                    val result = firebaseManager.signIn(email, password)
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
            Button(onClick = {
                coroutineScope.launch {
                    val result = firebaseManager.signUp(email, password)
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
