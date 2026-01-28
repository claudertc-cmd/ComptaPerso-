package com.example.comptaperso.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType
import java.util.UUID

/**
 * `ChooseActionDialog` est une boîte de dialogue qui demande à l'utilisateur de choisir
 * entre l'ajout d'un crédit ou d'un débit.
 *
 * @param onDismiss Callback exécuté lorsque la boîte de dialogue est fermée (par le bouton "Annuler" ou en cliquant à l'extérieur).
 * @param onCredit Callback exécuté lorsque l'utilisateur choisit "Crédit".
 * @param onDebit Callback exécuté lorsque l'utilisateur choisit "Débit".
 */
@Composable
fun ChooseActionDialog(
    onDismiss: () -> Unit,
    onCredit: () -> Unit,
    onDebit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Que souhaitez-vous ajouter ?") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = onCredit) { Text("Crédit") }
                Button(onClick = onDebit) { Text("Débit") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * `EditTransactionDialog` est une boîte de dialogue qui permet d'ajouter ou de modifier une transaction.
 * Le formulaire s'adapte pour l'ajout (champs vides) ou la modification (champs pré-remplis).
 *
 * @param transaction La transaction à modifier. Si `null`, la boîte de dialogue est en mode "ajout".
 * @param transactionType Le type de transaction (crédit ou débit) à ajouter si `transaction` est `null`.
 * @param onDismiss Callback pour fermer la boîte de dialogue.
 * @param onSave Callback pour sauvegarder la transaction (nouvelle ou modifiée).
 * @param onDelete Callback pour supprimer la transaction (uniquement en mode modification).
 */
@Composable
fun EditTransactionDialog(
    transaction: Transaction?,
    transactionType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    // États pour les champs du formulaire, initialisés avec les valeurs de la transaction si elle existe.
    var name by remember(transaction) { mutableStateOf(transaction?.name ?: "") }
    var day by remember(transaction) { mutableStateOf(transaction?.dayOfMonth?.toString() ?: "1") }
    var amount by remember(transaction) { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var isEditingAmount by remember { mutableStateOf(false) } // Pour un formatage du montant plus agréable.
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Affiche le montant formaté (ex: "120") ou brut (ex: "120.50") si le champ est en cours d'édition.
    val displayAmount = if (isEditingAmount) amount else "%.0f".format(amount.toDoubleOrNull() ?: 0.0)

    // Titre dynamique de la boîte de dialogue.
    val dialogTitle = if (transaction == null) {
        if (transactionType == TransactionType.CREDIT) "Ajouter un crédit" else "Ajouter un débit"
    } else {
        if (transaction.type == TransactionType.CREDIT) "Modifier le crédit" else "Modifier le débit"
    }

    // Fonction pour valider les entrées et sauvegarder la transaction.
    val validateAndSave = {
        val dayInt = day.toIntOrNull()
        val amountDouble = amount.toDoubleOrNull()
        if (name.isNotBlank() && dayInt != null && amountDouble != null) {
            val newTransaction = Transaction(
                id = transaction?.id ?: UUID.randomUUID().toString(), // Utilise un nouvel ID pour une nouvelle transaction.
                name = name,
                dayOfMonth = dayInt,
                amount = amountDouble,
                type = transactionType,
                isPaid = transaction?.isPaid ?: false
            )
            onSave(newTransaction)
        } else {
            Toast.makeText(context, "Veuillez remplir tous les champs correctement", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it },
                    label = { Text("Jour du mois") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayAmount,
                    onValueChange = { amount = it },
                    label = { Text("Montant") },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); validateAndSave() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isEditingAmount = it.isFocused },
                    singleLine = true,
                    textStyle = TextStyle(textAlign = TextAlign.Right)
                )
            }
        },
        confirmButton = { Button(onClick = validateAndSave) { Text("Enregistrer") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Le bouton "Supprimer" n'est visible qu'en mode édition.
                if (transaction != null) {
                    TextButton(
                        onClick = { onDelete(transaction) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                }
                Button(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}
