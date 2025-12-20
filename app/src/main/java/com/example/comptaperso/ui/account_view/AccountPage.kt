package com.example.comptaperso.ui.account_view

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    account: Account,
    transactions: List<Transaction>,
    extras: AccountExtraInfo?,
    onUpdate: (transactions: List<Transaction>) -> Unit,
    onUpdateExtras: (AccountExtraInfo) -> Unit,
    onBack: () -> Unit
) {
    var operationDialogState by remember { mutableStateOf(DialogState.NONE) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionTypeToAdd by remember { mutableStateOf(TransactionType.DEBIT) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var showOverdueDialog by remember { mutableStateOf(false) }
    var overdueTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    // Nouveaux états pour la confirmation de suppression
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // NOUVEL ETAT pour déclencher la vérification manuellement
    var triggerOverdueCheck by remember { mutableStateOf(false) }

    // MODIFICATION: La vérification ne se déclenche que si `triggerOverdueCheck` est vrai
    LaunchedEffect(triggerOverdueCheck) {
        if (triggerOverdueCheck) {
            val today = LocalDate.now()

            val foundOverdue = transactions.filter { transaction ->
                if (transaction.isPaid) {
                    false
                } else {
                    try {
                        var transactionDueDate = today.withDayOfMonth(transaction.dayOfMonth)

                        if (transactionDueDate.dayOfWeek == DayOfWeek.SATURDAY) {
                            transactionDueDate = transactionDueDate.plusDays(2)
                        } else if (transactionDueDate.dayOfWeek == DayOfWeek.SUNDAY) {
                            transactionDueDate = transactionDueDate.plusDays(1)
                        }

                        today.isAfter(transactionDueDate)
                    } catch (e: java.time.DateTimeException) {
                        false
                    }
                }
            }

            if (foundOverdue.isNotEmpty()) {
                overdueTransactions = foundOverdue
                showOverdueDialog = true
            }

            // Réinitialiser le déclencheur
            triggerOverdueCheck = false
        }
    }

    val finalBalance = remember(extras, transactions, account) {
        val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
        val deferred =
            if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
        val sumOfUnpaidCredits =
            transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
        val sumOfUnpaidDebits =
            transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
        provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
    }

    val resetOperationDialog = {
        operationDialogState = DialogState.NONE
        transactionToEdit = null
    }

    val (credits, debits) = remember(transactions) { transactions.partition { it.type == TransactionType.CREDIT } }
    val sortedCredits = remember(credits) { credits.sortedBy { it.dayOfMonth } }
    val sortedDebits = remember(debits) { debits.sortedBy { it.dayOfMonth } }
    val currentDate = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    fun formatDateForDisplay(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        } catch (e: Exception) {
            isoDate
        }
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
                .padding(
                    top = 0.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            // Titre du compte (sans icône banque ici)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 0.dp,
                            bottom = 12.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                }
            }

            // Solde réel
            item {
                Text(
                    text = "Solde Réel: %.2f€".format(finalBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                )
            }

            // Section soldes banque / différés + icônes à droite
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Solde Banque au ${
                                formatDateForDisplay(
                                    extras?.balanceDate?.ifEmpty { currentDate } ?: currentDate
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "%.2f€".format(
                                extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (account.includeDeferredDebits) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Prélèvements Différés", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "%.2f€".format(
                                    extras?.deferredDebits?.toDoubleOrNull() ?: 0.0
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Icône banque + crayon alignés à droite
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val packageName = account.packageName
                        if (!packageName.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    val intent =
                                        context.packageManager.getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "Application non installée",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = "Lancer l'application bancaire"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(end = 4.dp))

                        IconButton(
                            onClick = { showBalanceDialog = true }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier les soldes")
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Crédits
            if (sortedCredits.isNotEmpty()) {
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillParentMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Crédits",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(sortedCredits, key = { "credit-${it.id}" }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onStatusChange = { newStatus ->
                            val updatedList = transactions.map {
                                if (it.id == transaction.id) it.copy(isPaid = newStatus) else it
                            }
                            onUpdate(updatedList)
                        },
                        onClick = {
                            transactionToEdit = transaction
                            operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION
                        }
                    )
                }
            }

            // Débits
            if (sortedDebits.isNotEmpty()) {
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillParentMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Débits",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(sortedDebits, key = { "debit-${it.id}" }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onStatusChange = { newStatus ->
                            val updatedList = transactions.map {
                                if (it.id == transaction.id) it.copy(isPaid = newStatus) else it
                            }
                            onUpdate(updatedList)
                        },
                        onClick = {
                            transactionToEdit = transaction
                            operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION
                        }
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "Aucune opération pour ce compte.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showOverdueDialog) {
        val transactionNames = remember(overdueTransactions) {
            overdueTransactions.joinToString(separator = "\n") { "- ${it.name}" }
        }

        AlertDialog(
            onDismissRequest = {
                showOverdueDialog = false
                overdueTransactions = emptyList()
            },
            title = { Text("Opérations Passées") },
            text = {
                Column {
                    Text(
                        "Les opérations suivantes sont passées. Voulez-vous les marquer comme payées ?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = transactionNames,
                        modifier = Modifier.padding(top = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedList = transactions.map { trans ->
                        if (overdueTransactions.any { it.id == trans.id }) {
                            trans.copy(isPaid = true)
                        } else {
                            trans
                        }
                    }
                    onUpdate(updatedList)
                    showOverdueDialog = false
                    overdueTransactions = emptyList()
                }) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showOverdueDialog = false
                    overdueTransactions = emptyList()
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showBalanceDialog) {
        var provisionalBalance by remember { mutableStateOf(extras?.provisionalBalance ?: "") }
        var deferredDebits by remember { mutableStateOf(extras?.deferredDebits ?: "") }

        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Modifier les soldes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = provisionalBalance,
                        onValueChange = { provisionalBalance = it },
                        label = { Text("Solde prévisionnel") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    if (account.includeDeferredDebits) {
                        OutlinedTextField(
                            value = deferredDebits,
                            onValueChange = { deferredDebits = it },
                            label = { Text("Prélèvements différés") },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newExtras = AccountExtraInfo(
                            provisionalBalance = provisionalBalance,
                            deferredDebits = deferredDebits,
                            balanceDate = LocalDate.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        )
                        onUpdateExtras(newExtras)
                        showBalanceDialog = false

                        // MODIFICATION: Activer le déclencheur ici
                        triggerOverdueCheck = true
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                Button(onClick = { showBalanceDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (operationDialogState == DialogState.CHOOSE_ACTION) {
        ChooseActionDialog(
            onDismiss = resetOperationDialog,
            onCredit = {
                transactionTypeToAdd = TransactionType.CREDIT
                operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION
            },
            onDebit = {
                transactionTypeToAdd = TransactionType.DEBIT
                operationDialogState = DialogState.ADD_OR_EDIT_TRANSACTION
            }
        )
    }

    if (operationDialogState == DialogState.ADD_OR_EDIT_TRANSACTION) {
        EditTransactionDialog(
            transaction = transactionToEdit,
            transactionType = transactionTypeToAdd,
            onDismiss = resetOperationDialog,
            onSave = { updatedTransaction ->
                val updatedList = if (transactionToEdit == null) {
                    transactions + updatedTransaction
                } else {
                    transactions.map {
                        if (it.id == updatedTransaction.id) updatedTransaction else it
                    }
                }
                onUpdate(updatedList)
                resetOperationDialog()
            },
            onDelete = { transaction ->
                transactionToDelete = transaction
                showDeleteConfirmDialog = true
            }
        )
    }

    // Dialog de confirmation de suppression
    if (showDeleteConfirmDialog && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                transactionToDelete = null
            },
            title = { Text("Confirmer la suppression") },
            text = {
                Column {
                    Text(
                        "Êtes-vous sûr de vouloir supprimer l'opération",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "\"${transactionToDelete!!.name}\" (${transactionToDelete!!.type.name})",
                        modifier = Modifier.padding(top = 8.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Cette action est irréversible.",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { transToDelete ->
                            onUpdate(transactions.filterNot { it.id == transToDelete.id })
                        }
                        showDeleteConfirmDialog = false
                        transactionToDelete = null
                        resetOperationDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    transactionToDelete = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}
