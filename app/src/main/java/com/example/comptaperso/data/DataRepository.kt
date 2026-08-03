package com.example.comptaperso.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.comptaperso.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

// Extension pour accéder facilement au DataStore depuis le contexte de l'application.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * `AllData` est une classe de données qui encapsule l'ensemble des données de l'application.
 * NOUVEAU FORMAT : Les comptes contiennent maintenant toutes leurs données (transactions, soldes, etc.)
 */
@Keep
@Serializable
data class AllData(
    val accounts: List<Account> = emptyList()
)

/**
 * Structure pour l'ancien format (utilisée uniquement pour la conversion des imports JSON).
 * Conservée pour compatibilité avec d'anciennes sauvegardes.
 */
@Keep
@Serializable
private data class OldAllData(
    val accounts: List<OldAccount> = emptyList(),
    val transactions: Map<String, List<Transaction>> = emptyMap(),
    val balances: Map<String, Double> = emptyMap(),
    val accountExtras: Map<String, AccountExtraInfo> = emptyMap()
)

@Keep
@Serializable
private data class OldAccount(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var includeDeferredDebits: Boolean = false,
    var packageName: String? = null
)

/**
 * `DataRepository` est la source de vérité unique de l'application.
 * Il gère la persistance des données en local (via DataStore) et sur le cloud (via Firebase).
 * Il expose les données sous forme de `Flow` pour que l'interface utilisateur puisse réagir aux changements.
 */
class DataRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    // Instance Json réutilisable pour l'export avec formatage.
    private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // Clé unique pour le DataStore (nouveau format).
    private val accountsKey = stringPreferencesKey("accounts")

    // Expose les comptes sous forme de `Flow` pour une observation réactive.
    val accounts: Flow<List<Account>> = context.dataStore.data.map {
        Json.decodeFromString(it[accountsKey] ?: "[]")
    }

    // --- Authentification ---
    fun isSignedIn(): Boolean = auth.currentUser != null
    fun signOut() = auth.signOut()
    suspend fun signIn(email: String, pass: String) = runCatching {
        auth.signInWithEmailAndPassword(email, pass).await()
    }
    suspend fun signUp(email: String, pass: String) = runCatching {
        auth.createUserWithEmailAndPassword(email, pass).await()
    }

    /**
     * Sauvegarde tous les comptes (avec toutes leurs données) dans le DataStore local.
     */
    suspend fun saveAllData(accounts: List<Account>) {
        context.dataStore.edit {
            it[accountsKey] = Json.encodeToString(accounts)
        }
    }

    /**
     * Sauvegarde les comptes.
     */
    suspend fun saveAccounts(data: List<Account>) = context.dataStore.edit {
        it[accountsKey] = Json.encodeToString(data)
    }

    /**
     * Sauvegarde les soldes actuels sur Firestore pour un suivi historique.
     */
    suspend fun saveBalancesToFirestore(accounts: List<Account>) = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let { uid ->
            val date = DateUtils.getTodayIso()
            val data = accounts.map { acc ->
                mapOf(
                    "id" to acc.id,
                    "name" to acc.name,
                    "balance" to acc.balance
                )
            }
            firestore.collection("users")
                .document(uid)
                .collection("balances")
                .document(date)
                .set(mapOf("balances" to data))
                .await()
        }
    }

    /**
     * Sauvegarde l'intégralité des données de l'application dans un fichier JSON local (dans le dossier Téléchargements).
     */
    suspend fun saveDataToJson(accounts: List<Account>) {
        val allData = AllData(accounts)
        val jsonString = prettyJson.encodeToString(allData)
        val fileName = "comptaperso_backup_${System.currentTimeMillis()}.json"

        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let {
                resolver.openOutputStream(it)?.use { o -> o.write(jsonString.toByteArray()) }
            } ?: throw IOException("Impossible de créer l'entrée MediaStore.")

            saveBalancesToFirestore(accounts) // Sauvegarde aussi l'historique sur Firestore.
        } catch (_: Exception) {
            Log.e("JsonSaveError", "Erreur lors de la sauvegarde du fichier JSON")
        }
    }

    /**
     * Restaure les données de l'application à partir d'un fichier JSON local.
     * Détecte automatiquement le format (ancien ou nouveau) et convertit si nécessaire.
     */
    suspend fun restoreDataFromJson(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            }?.let { jsonString ->
                restoreData(jsonString)
            }
        } catch (e: Exception) {
            Log.e("JsonRestoreError", "Erreur de restauration JSON", e)
        }
    }

    /**
     * Sauvegarde les données actuelles sur Firebase Firestore.
     */
    suspend fun saveDataToFirebase() = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let { uid ->
            try {
                val data = AllData(accounts.first())
                firestore.collection("users").document(uid).set(data).await()
            } catch (_: Exception) {
                Log.e("FirebaseSaveError", "Erreur de sauvegarde sur Firestore")
            }
        }
    }

    /**
     * Restaure les données depuis Firebase Firestore.
     * Détecte automatiquement le format (ancien ou nouveau) et convertit si nécessaire.
     */
    suspend fun restoreDataFromFirebase() = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let { uid ->
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    // Tentative de chargement du nouveau format
                    try {
                        val data = doc.toObject(AllData::class.java)
                        if (data != null) {
                            saveAllData(data.accounts)
                            return@withContext
                        }
                    } catch (_: Exception) {
                        Log.w("FirebaseRestore", "Nouveau format non disponible, tentative ancien format")
                    }

                    // Si échec, tentative avec l'ancien format
                    try {
                        val oldData = doc.toObject(OldAllData::class.java)
                        if (oldData != null) {
                            val convertedAccounts = convertOldDataToNew(oldData)
                            saveAllData(convertedAccounts)
                            Log.i("Conversion", "Données converties depuis l'ancien format Firebase")
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseRestoreError", "Erreur de restauration depuis Firestore", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseRestoreError", "Erreur de restauration depuis Firestore", e)
            }
        }
    }

    /**
     * Logique interne pour mettre à jour le DataStore avec les données désérialisées.
     * Détecte automatiquement si les données sont dans l'ancien ou le nouveau format.
     */
    private suspend fun restoreData(jsonString: String) {
        try {
            // Tentative de chargement du nouveau format
            try {
                val allData = Json.decodeFromString<AllData>(jsonString)
                saveAllData(allData.accounts)
                Log.i("Restore", "Données restaurées (nouveau format)")
                return
            } catch (e: Exception) {
                Log.w("Restore", "Nouveau format non détecté, tentative ancien format")
            }

            // Si échec, tentative avec l'ancien format et conversion
            val oldData = Json.decodeFromString<OldAllData>(jsonString)
            val convertedAccounts = convertOldDataToNew(oldData)
            saveAllData(convertedAccounts)
            Log.i("Conversion", "Données converties depuis l'ancien format")

        } catch (e: Exception) {
            Log.e("DataRestoreError", "Erreur de désérialisation", e)
        }
    }

    /**
     * 🔄 CONVERSION : Convertit l'ancien format vers le nouveau format.
     * Utilisée uniquement lors de l'import de vieilles sauvegardes JSON.
     */
    private fun convertOldDataToNew(oldData: OldAllData): List<Account> {
        // Récupère tous les IDs de comptes valides
        val validAccountIds = oldData.accounts.map { it.id }.toSet()

        return oldData.accounts.map { oldAccount ->
            // Filtre accountExtras pour ne garder que les vraies données de compte
            val extras = oldData.accountExtras
                .filterKeys { key -> validAccountIds.contains(key) }[oldAccount.id] ?: AccountExtraInfo()

            Account(
                id = oldAccount.id,
                name = oldAccount.name,
                type = oldAccount.type,
                includeDeferredDebits = oldAccount.includeDeferredDebits,
                packageName = oldAccount.packageName,
                balance = oldData.balances[oldAccount.id] ?: 0.0,
                extraInfo = extras,
                transactions = oldData.transactions[oldAccount.id] ?: emptyList()
            )
        }
    }
}