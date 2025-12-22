package com.example.comptaperso.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.comptaperso.FirebaseStorageManager
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


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class AllData(
    val accounts: List<Account>,
    val transactions: Map<String, List<Transaction>>,
    val balances: Map<String, Double>,
    val accountExtras: Map<String, AccountExtraInfo>
)

class DataRepository(private val context: Context) {

    private val firebaseStorageManager = FirebaseStorageManager()
    private val firestore = Firebase.firestore
    private val accountsKey = stringPreferencesKey("accounts")
    private val transactionsKey = stringPreferencesKey("transactions")
    private val balancesKey = stringPreferencesKey("balances")
    private val accountExtrasKey = stringPreferencesKey("account_extras")

    val accounts: Flow<List<Account>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[accountsKey]
            if (jsonString != null && jsonString.isNotEmpty()) {
                Json.decodeFromString<List<Account>>(jsonString)
            } else {
                getSampleAccounts() // Provide sample data on first launch
            }
        }

    val transactions: Flow<Map<String, List<Transaction>>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[transactionsKey]
            if (jsonString != null && jsonString.isNotEmpty()) {
                Json.decodeFromString<Map<String, List<Transaction>>>(jsonString)
            } else {
                val sampleAccounts = getSampleAccounts()
                val sampleTransactions = mutableMapOf<String, List<Transaction>>()
                sampleAccounts.forEach { account ->
                    sampleTransactions[account.id] = getSampleTransactionsForAccount(account.id)
                }
                sampleTransactions
            }
        }

    val balances: Flow<Map<String, Double>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[balancesKey]
            if (jsonString != null && jsonString.isNotEmpty()) {
                Json.decodeFromString<Map<String, Double>>(jsonString)
            } else {
                emptyMap()
            }
        }

    val accountExtras: Flow<Map<String, AccountExtraInfo>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[accountExtrasKey]
            if (jsonString != null && jsonString.isNotEmpty()) {
                Json.decodeFromString<Map<String, AccountExtraInfo>>(jsonString)
            } else {
                emptyMap()
            }
        }

    suspend fun saveAccounts(accounts: List<Account>) {
        context.dataStore.edit { settings ->
            val jsonString = Json.encodeToString(accounts)
            settings[accountsKey] = jsonString
        }
    }

    suspend fun saveTransactions(transactions: Map<String, List<Transaction>>) {
        context.dataStore.edit { settings ->
            val jsonString = Json.encodeToString(transactions)
            settings[transactionsKey] = jsonString
        }
    }

    suspend fun saveBalances(balances: Map<String, Double>) {
        context.dataStore.edit { settings ->
            val jsonString = Json.encodeToString(balances)
            settings[balancesKey] = jsonString
        }
    }

    suspend fun saveAccountExtras(accountExtras: Map<String, AccountExtraInfo>) {
        context.dataStore.edit { settings ->
            val jsonString = Json.encodeToString(accountExtras)
            settings[accountExtrasKey] = jsonString
        }
    }

    suspend fun saveBalancesToFirestore() {
        withContext(Dispatchers.IO) {
            try {
                val balancesToSave = balances.first()
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    firestore.collection("users").document(userId).collection("balances").document(date)
                        .set(mapOf("balances" to balancesToSave))
                        .await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Soldes sauvegardés sur Firestore", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FirestoreSaveError", "Erreur de sauvegarde des soldes sur Firestore", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur de sauvegarde Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun saveDataToJson(
        accounts: List<Account>,
        allTransactions: Map<String, List<Transaction>>,
        balances: Map<String, Double>,
        accountExtras: Map<String, AccountExtraInfo>
    ) {
        val calculatedBalances = mutableMapOf<String, Double>()
        accounts.forEach { account ->
            if (account.type == "Bancaire") {
                val extras = accountExtras[account.id]
                val provisionalBalance = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                val deferredDebits = extras?.deferredDebits?.toDoubleOrNull() ?: 0.0
                val accountTransactions = allTransactions[account.id] ?: emptyList()
                val unpaidDebits = accountTransactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
                val unpaidCredits = accountTransactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
                calculatedBalances[account.id] = provisionalBalance - deferredDebits - unpaidDebits + unpaidCredits
            } else {
                calculatedBalances[account.id] = balances[account.id] ?: 0.0
            }
        }

        val allData = AllData(
            accounts = accounts,
            transactions = allTransactions,
            balances = calculatedBalances,
            accountExtras = accountExtras
        )

        val jsonString = Json { prettyPrint = true }.encodeToString(allData)
        val fileName = "comptaperso_backup_${System.currentTimeMillis()}.json"

        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.write(jsonString.toByteArray())
                    Toast.makeText(context, "Sauvegarde JSON réussie dans Téléchargements", Toast.LENGTH_LONG).show()
                } ?: throw IOException("Impossible d\'ouvrir le flux de sortie pour l\'URI: $uri")
            } else {
                throw IOException("Impossible de créer l\'entrée MediaStore.")
            }

            saveBalancesToFirestore()

        } catch (e: Exception) {
            Log.e("JsonSaveError", "Erreur lors de la sauvegarde du fichier JSON", e)
            Toast.makeText(context, "Erreur de sauvegarde JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    suspend fun restoreDataFromJson(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (jsonString != null) {
                    restoreData(jsonString)
                } else {
                     withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Impossible de lire le fichier", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("JsonRestoreError", "Erreur de restauration", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun restoreData(jsonString: String) {
        val allData = Json.decodeFromString<AllData>(jsonString)
        saveAccounts(allData.accounts)
        saveTransactions(allData.transactions)
        saveBalances(allData.balances)
        saveAccountExtras(allData.accountExtras)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Données restaurées avec succès", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun saveDataToFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val accounts = accounts.first()
                val allTransactions = transactions.first()
                val balances = balances.first()
                val accountExtras = accountExtras.first()

                val calculatedBalances = mutableMapOf<String, Double>()
                accounts.forEach { account ->
                    if (account.type == "Bancaire") {
                        val extras = accountExtras[account.id]
                        val provisionalBalance = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                        val deferredDebits = extras?.deferredDebits?.toDoubleOrNull() ?: 0.0
                        val accountTransactions = allTransactions[account.id] ?: emptyList()
                        val unpaidDebits = accountTransactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
                        val unpaidCredits = accountTransactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
                        calculatedBalances[account.id] = provisionalBalance - deferredDebits - unpaidDebits + unpaidCredits
                    } else {
                        calculatedBalances[account.id] = balances[account.id] ?: 0.0
                    }
                }
                
                val allData = AllData(
                    accounts = accounts,
                    transactions = allTransactions,
                    balances = calculatedBalances,
                    accountExtras = accountExtras
                )
                val jsonString = Json.encodeToString(allData)
                firebaseStorageManager.uploadData(jsonString.toByteArray(), "comptaperso_backup.json")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSaveError", "Erreur de sauvegarde Firebase", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur de sauvegarde Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun restoreDataFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val data = firebaseStorageManager.downloadData("comptaperso_backup.json")
                if (data != null) {
                    restoreData(data.decodeToString())
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Aucune sauvegarde Firebase trouvée", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseRestoreError", "Erreur de restauration Firebase", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur de restauration Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

// Sample Data
fun getSampleAccounts(): List<Account> {
    return listOf(
        Account(id = "1", name = "Compte Courant", type = "Bancaire", packageName = "com.boursorama.android"),
        Account(id = "2", name = "Livret A", type = "Epargne", packageName = "com.creditagricole.particuliers")
    )
}

fun getSampleTransactionsForAccount(accountId: String): MutableList<Transaction> {
    return when (accountId) {
        "1" -> mutableListOf(
            Transaction(
                name = "Loyer",
                dayOfMonth = 1,
                amount = 750.0,
                type = TransactionType.DEBIT
            ),
            Transaction(
                name = "Salaire",
                dayOfMonth = 25,
                amount = 2000.0,
                type = TransactionType.CREDIT
            ),
            Transaction(
                name = "Internet",
                dayOfMonth = 5,
                amount = 30.0,
                type = TransactionType.DEBIT
            ),
            Transaction(
                name = "Téléphone",
                dayOfMonth = 15,
                amount = 20.0,
                type = TransactionType.DEBIT
            )
        )
        else -> mutableListOf()
    }
}
