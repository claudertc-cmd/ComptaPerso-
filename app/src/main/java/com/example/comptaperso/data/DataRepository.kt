package com.example.comptaperso.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension pour accéder facilement au DataStore depuis le contexte de l'application.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * `AllData` est une classe de données qui encapsule l'ensemble des données de l'application.
 * Elle est utilisée pour la sérialisation/désérialisation en JSON lors des sauvegardes et restaurations.
 */
@Serializable
data class AllData(
    val accounts: List<Account>,
    val transactions: Map<String, List<Transaction>>,
    val balances: Map<String, Double>,
    val accountExtras: Map<String, AccountExtraInfo>
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
    private val prettyJson = Json { prettyPrint = true }

    // Clés pour le DataStore.
    private val accountsKey = stringPreferencesKey("accounts")
    private val transactionsKey = stringPreferencesKey("transactions")
    private val balancesKey = stringPreferencesKey("balances")
    private val accountExtrasKey = stringPreferencesKey("account_extras")

    // Expose les données sous forme de `Flow` pour une observation réactive.
    val accounts: Flow<List<Account>> = context.dataStore.data.map { Json.decodeFromString(it[accountsKey] ?: "[]") }
    val transactions: Flow<Map<String, List<Transaction>>> = context.dataStore.data.map { Json.decodeFromString(it[transactionsKey] ?: "{}") }
    val balances: Flow<Map<String, Double>> = context.dataStore.data.map { Json.decodeFromString(it[balancesKey] ?: "{}") }
    val accountExtras: Flow<Map<String, AccountExtraInfo>> = context.dataStore.data.map { Json.decodeFromString(it[accountExtrasKey] ?: "{}") }

    // --- Authentification --- 
    fun isSignedIn(): Boolean = auth.currentUser != null
    fun signOut() = auth.signOut()
    suspend fun signIn(email: String, pass: String) = runCatching { auth.signInWithEmailAndPassword(email, pass).await() }
    suspend fun signUp(email: String, pass: String) = runCatching { auth.createUserWithEmailAndPassword(email, pass).await() }

    /**
     * Sauvegarde toutes les données (comptes, transactions, etc.) dans le DataStore local.
     */
    suspend fun saveAllData(accounts: List<Account>, transactions: Map<String, List<Transaction>>, balances: Map<String, Double>, accountExtras: Map<String, AccountExtraInfo>) {
        context.dataStore.edit {
            it[accountsKey] = Json.encodeToString(accounts)
            it[transactionsKey] = Json.encodeToString(transactions)
            it[balancesKey] = Json.encodeToString(balances)
            it[accountExtrasKey] = Json.encodeToString(accountExtras)
        }
    }
    
    // Fonctions de sauvegarde individuelle pour chaque type de données.
    suspend fun saveAccounts(data: List<Account>) = context.dataStore.edit { it[accountsKey] = Json.encodeToString(data) }
    suspend fun saveTransactions(data: Map<String, List<Transaction>>) = context.dataStore.edit { it[transactionsKey] = Json.encodeToString(data) }
    suspend fun saveBalances(data: Map<String, Double>) = context.dataStore.edit { it[balancesKey] = Json.encodeToString(data) }
    suspend fun saveAccountExtras(data: Map<String, AccountExtraInfo>) = context.dataStore.edit { it[accountExtrasKey] = Json.encodeToString(data) }

    /**
     * Sauvegarde les soldes actuels sur Firestore pour un suivi historique.
     */
    suspend fun saveBalancesToFirestore(accounts: List<Account>, balancesToSave: Map<String, Double>) = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val data = accounts.map { acc -> mapOf("id" to acc.id, "name" to acc.name, "balance" to (balancesToSave[acc.id] ?: 0.0)) }
            firestore.collection("users").document(it).collection("balances").document(date).set(mapOf("balances" to data)).await()
        }
    }

    /**
     * Sauvegarde l'intégralité des données de l'application dans un fichier JSON local (dans le dossier Téléchargements).
     */
    suspend fun saveDataToJson(accounts: List<Account>, allTransactions: Map<String, List<Transaction>>, balances: Map<String, Double>, accountExtras: Map<String, AccountExtraInfo>) {
        val allData = AllData(accounts, allTransactions, balances, accountExtras)
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

            saveBalancesToFirestore(accounts, balances) // Sauvegarde aussi l'historique sur Firestore.
        } catch (e: Exception) {
            Log.e("JsonSaveError", "Erreur lors de la sauvegarde du fichier JSON", e)
        }
    }

    /**
     * Restaure les données de l'application à partir d'un fichier JSON local.
     */
    suspend fun restoreDataFromJson(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }?.let { restoreData(it) }
        } catch (e: Exception) {
            Log.e("JsonRestoreError", "Erreur de restauration JSON", e)
        }
    }

    /**
     * Sauvegarde les données actuelles sur Firebase Storage.
     */
    suspend fun saveDataToFirebase() = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let {
            try {
                val data = AllData(accounts.first(), transactions.first(), balances.first(), accountExtras.first())
                val jsonString = Json.encodeToString(data)
                firestore.collection("users").document(it).set(data).await()
            } catch (e: Exception) {
                Log.e("FirebaseSaveError", "Erreur de sauvegarde sur Firestore", e)
            }
        }
    }

    /**
     * Restaure les données depuis Firebase Storage.
     */
    suspend fun restoreDataFromFirebase() = withContext(Dispatchers.IO) {
        auth.currentUser?.uid?.let {
            try {
                val doc = firestore.collection("users").document(it).get().await()
                if (doc.exists()) {
                    val data = doc.toObject(AllData::class.java)
                    if (data != null) saveAllData(data.accounts, data.transactions, data.balances, data.accountExtras)
                }
            } catch (e: Exception) {
                Log.e("FirebaseRestoreError", "Erreur de restauration depuis Firestore", e)
            }
        }
    }

    /**
     * Logique interne pour mettre à jour le DataStore avec les données désérialisées.
     */
    private suspend fun restoreData(jsonString: String) {
        try {
            val allData = Json.decodeFromString<AllData>(jsonString)
            saveAllData(allData.accounts, allData.transactions, allData.balances, allData.accountExtras)
        } catch (e: Exception) {
            Log.e("DataRestoreError", "Erreur de désérialisation", e)
        }
    }
}
