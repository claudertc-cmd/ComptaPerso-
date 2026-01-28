package com.example.comptaperso.ui.account_view

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.comptaperso.DialogState
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType
import com.example.comptaperso.ui.components.ChooseActionDialog
import com.example.comptaperso.ui.components.EditTransactionDialog
import com.example.comptaperso.ui.components.TransactionRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * `AccountPage` affiche les détails d'un compte bancaire, y compris le solde, les transactions,
 * et fournit des actions pour modifier les données.
 *
 * @param account Le compte à afficher.
 * @param transactions La liste des transactions (crédits et débits) pour ce compte.
 * @param extras Les informations supplémentaires du compte (solde prévisionnel, etc.).
 * @param onUpdate Callback pour mettre à jour la liste des transactions.
 * @param onUpdateExtras Callback pour mettre à jour les informations supplémentaires du compte.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    account: Account,
    transactions: List<Transaction>,
    extras: AccountExtraInfo?,
    onUpdate: (transactions: List<Transaction>) -> Unit,
    onUpdateExtras: (AccountExtraInfo) -> Unit
) {
    // États pour gérer les différentes boîtes de dialogue.
    var operationDialogState by remember { mutableStateOf(DialogState.NONE) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionTypeToAdd by remember { mutableStateOf(TransactionType.DEBIT) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var showOverdueDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // États pour le contenu des dialogues.
    var overdueTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Déclencheur pour la vérification manuelle des transactions en retard.
    var triggerOverdueCheck by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // `LaunchedEffect` pour vérifier les transactions en retard lorsque `triggerOverdueCheck` est activé.
    LaunchedEffect(triggerOverdueCheck) {
        if (triggerOverdueCheck) {
            val today = LocalDate.now()
            val foundOverdue = transactions.filter { transaction ->
                !transaction.isPaid && runCatching {
                    var dueDate = today.withDayOfMonth(transaction.dayOfMonth)
                    if (dueDate.dayOfWeek == DayOfWeek.SATURDAY) dueDate = dueDate.plusDays(2)
                    if (dueDate.dayOfWeek == DayOfWeek.SUNDAY) dueDate = dueDate.plusDays(1)
                    today.isAfter(dueDate)
                }.getOrDefault(false)
            }

            if (foundOverdue.isNotEmpty()) {
                overdueTransactions = foundOverdue
                showOverdueDialog = true
            }
            triggerOverdueCheck = false // Réinitialise le déclencheur.
        }
    }

    // Calcule le solde final en temps réel en se basant sur les transactions non payées.
    val finalBalance = remember(extras, transactions, account) {
        val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
        val deferred = if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
        val unpaidCredits = transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
        val unpaidDebits = transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
        provisional + unpaidCredits - unpaidDebits - deferred
    }

    // Sépare et trie les transactions en crédits et débits.
    val (credits, debits) = remember(transactions) { transactions.partition { it.type == TransactionType.CREDIT } }
    val sortedCredits = remember(credits) { credits.sortedBy { it.dayOfMonth } }
    val sortedDebits = remember(debits) { debits.sortedBy { it.dayOfMonth } }
    val currentDate = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // Fonction utilitaire pour formater une date ISO en format lisible.
    fun formatDateForDisplay(isoDate: String): String {
        return runCatching {
            LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }.getOrDefault(isoDate)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { operationDialogState = DialogState.CHOOSE_ACTION }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une opération")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // En-tête de la page : nom du compte et soldes.
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                    Text(
                        text = "Solde Réel: %.2f€".format(finalBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Affiche le solde bancaire et la date.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Solde Banque au ${formatDateForDisplay(extras?.balanceDate ?: currentDate)}", style = MaterialTheme.typography.bodyMedium)
                        Text("%.2f€".format(extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }

                    // Affiche les débits différés si l'option est activée pour le compte.
                    if (account.includeDeferredDebits) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Prélèvements Différés", style = MaterialTheme.typography.bodyMedium)
                            Text("%.2f€".format(extras?.deferredDebits?.toDoubleOrNull() ?: 0.0), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Icônes pour lancer l'application bancaire et modifier les soldes.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        account.packageName?.takeIf { it.isNotBlank() }?.let {
                            IconButton(onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(it)
                                if (intent != null) context.startActivity(intent)
                                else Toast.makeText(context, "Application non installée", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.AccountBalance, "Lancer l'application bancaire")
                            }
                        }
                        IconButton(onClick = { showBalanceDialog = true }) {
                            Icon(Icons.Default.Edit, "Modifier les soldes")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }

            // Section pour les crédits.
            if (sortedCredits.isNotEmpty()) {
                stickyHeader {
                    Surface(modifier = Modifier.fillParentMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Crédits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                items(sortedCredits, key = { "credit-${it.id}" }) { transaction ->
                    TransactionRow(transaction, onStatusChange = { onUpdate(transactions.map { t -> if (t.id == transaction.id) t.copy(isPaid = it) else t }) }, onClick = { transactionToEdit = transaction; operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION })
                }
            }

            // Section pour les débits.
            if (sortedDebits.isNotEmpty()) {
                stickyHeader {
                    Surface(modifier = Modifier.fillParentMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Débits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                items(sortedDebits, key = { "debit-${it.id}" }) { transaction ->
                    TransactionRow(transaction, onStatusChange = { onUpdate(transactions.map { t -> if (t.id == transaction.id) t.copy(isPaid = it) else t }) }, onClick = { transactionToEdit = transaction; operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION })
                }
            }

            // Message si aucune transaction n'est disponible.
            if (transactions.isEmpty()) {
                item { Text("Aucune opération pour ce compte.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    // Dialogue pour les opérations en retard.
    if (showOverdueDialog) {
        val names = remember(overdueTransactions) { overdueTransactions.joinToString("\n") { "- ${it.name}" } }
        AlertDialog(
            onDismissRequest = { showOverdueDialog = false },
            title = { Text("Opérations Passées") },
            text = { Column { Text("Les opérations suivantes sont passées. Voulez-vous les marquer comme payées ?"); Text(names, Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold) } },
            confirmButton = {
                Button(onClick = {
                    onUpdate(transactions.map { t -> if (overdueTransactions.any { it.id == t.id }) t.copy(isPaid = true) else t })
                    showOverdueDialog = false
                }) { Text("Confirmer") }
            },
            dismissButton = { Button(onClick = { showOverdueDialog = false }) { Text("Annuler") } }
        )
    }

    // Dialogue pour modifier le solde.
    if (showBalanceDialog) {
        var provisional by remember { mutableStateOf(extras?.provisionalBalance ?: "") }
        var deferred by remember { mutableStateOf(extras?.deferredDebits ?: "") }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = {
                Text(
                    text = account.name,
                    modifier = if (account.packageName?.isNotBlank() == true) {
                        Modifier.clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(account.packageName!!)
                            if (intent != null) context.startActivity(intent)
                            else Toast.makeText(context, "Application non installée", Toast.LENGTH_SHORT).show()
                        }
                    } else Modifier,
                    color = if (account.packageName?.isNotBlank() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = provisional,
                        onValueChange = { provisional = it },
                        label = { Text("Solde prévisionnel") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_MAIN)
                                intent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Calculatrice non trouvée", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Calculate, "Lancer la calculatrice")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    if (account.includeDeferredDebits) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = deferred,
                            onValueChange = { deferred = it },
                            label = { Text("Prélèvements différés") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_MAIN)
                                    intent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Calculatrice non trouvée", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.Calculate, "Lancer la calculatrice")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateExtras(AccountExtraInfo(provisional, deferred, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    showBalanceDialog = false
                    triggerOverdueCheck = true
                }) { Text("Enregistrer") }
            },
            dismissButton = { Button(onClick = { showBalanceDialog = false }) { Text("Annuler") } }
        )
    }

    // Gère l'affichage des dialogues pour ajouter ou modifier une transaction.
    val resetOpDialog = { operationDialogState = DialogState.NONE; transactionToEdit = null }
    if (operationDialogState == DialogState.CHOOSE_ACTION) {
        ChooseActionDialog(resetOpDialog, onCredit = { transactionTypeToAdd = TransactionType.CREDIT; operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION }, onDebit = { transactionTypeToAdd = TransactionType.DEBIT; operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION })
    }
    if (operationDialogState == DialogState.ADD_OR_EDIT_TRANSACTION) {
        EditTransactionDialog(transactionToEdit, transactionTypeToAdd, resetOpDialog,
            onSave = { updated ->
                onUpdate(if (transactionToEdit == null) transactions + updated else transactions.map { if (it.id == updated.id) updated else it })
                resetOpDialog()
            },
            onDelete = { transactionToDelete = it; showDeleteConfirmDialog = true })
    }

    // Dialogue pour confirmer la suppression d'une transaction.
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer l'opération \"${transactionToDelete?.name}\"?") },
            confirmButton = {
                Button(onClick = {
                    transactionToDelete?.let { toDelete -> onUpdate(transactions.filterNot { it.id == toDelete.id }) }
                    showDeleteConfirmDialog = false
                    resetOpDialog()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") }
            },
            dismissButton = { TextButton({ showDeleteConfirmDialog = false }) { Text("Annuler") } }
        )
    }
}
