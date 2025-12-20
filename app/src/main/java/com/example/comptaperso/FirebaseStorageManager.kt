package com.example.comptaperso

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseStorageManager {

    private val storageRef = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private fun getUserDataRef() = auth.currentUser?.uid?.let { storageRef.child("users/$it") }

    /**
     * Crée un nouveau compte utilisateur avec une adresse e-mail et un mot de passe.
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Result.failure(e)
        }
    }

    /**
     * Connecte un utilisateur existant avec son adresse e-mail et son mot de passe.
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Result.failure(e)
        }
    }

    /**
     * Déconnecte l'utilisateur actuel.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Vérifie si un utilisateur est actuellement connecté.
     */
    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Uploade les données (représentées en ByteArray) vers Firebase Storage.
     * L'utilisateur doit être connecté.
     */
    suspend fun uploadData(data: ByteArray, fileName: String) {
        val userRef = getUserDataRef() ?: throw IllegalStateException("User not signed in")
        val fileRef = userRef.child(fileName)
        fileRef.putBytes(data).await()
    }

    /**
     * Télécharge les données depuis Firebase Storage.
     * L'utilisateur doit être connecté.
     * @return Un ByteArray contenant les données, ou null si une erreur survient.
     */
    suspend fun downloadData(fileName: String): ByteArray? {
        val userRef = getUserDataRef() ?: run {
            println("User not signed in, cannot download data.")
            return null
        }
        return try {
            val fileRef = userRef.child(fileName)
            fileRef.getBytes(Long.MAX_VALUE).await()
        } catch (e: Exception) {
            null
        }
    }
}
