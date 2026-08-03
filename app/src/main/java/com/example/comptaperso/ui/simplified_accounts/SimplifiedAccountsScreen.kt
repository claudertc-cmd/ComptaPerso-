package com.example.comptaperso.ui.simplified_accounts

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Account
import com.example.comptaperso.DateUtils

/**
 * `SimplifiedAccountsScreen` affiche une liste de comptes pour lesquels seule la modification du solde est nécessaire
 * (par exemple, les comptes Epargne ou Assurance). L'interface est plus simple que celle de `AccountPage`.
 *
 * @param accounts La liste des comptes de type simplifié à afficher (contenant leurs soldes).
 * @param onUpdateBalance Callback déclenché pour mettre à jour le solde d'un compte.
 */
@Composable
fun SimplifiedAccountsScreen(
    accounts: List<Account>,
    onUpdateBalance: (accountId: String, newBalance: Double) -> Unit
) {
    // `showDialogForAccount` stocke le compte pour lequel la boîte de dialogue de modification doit être affichée.
    // Si `null`, aucune boîte de dialogue n'est montrée.
    var showDialogForAccount by remember { mutableStateOf<Account?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(accounts, key = { it.id }) { account ->
            // NOUVEAU FORMAT : Le solde est directement dans l'objet account
            val balance = account.balance

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        val dateStr = account.extraInfo.balanceDate
                        val isToday = remember(dateStr) {
                            DateUtils.isToday(dateStr)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            if (dateStr.isNotBlank() && !isToday) {
                                Text(
                                    text = "${DateUtils.getRelativeDateLabel(dateStr)} : ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("%.2f€".format(balance), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Icône pour ouvrir la boîte de dialogue de modification.
                    IconButton(onClick = { showDialogForAccount = account }) {
                        Icon(Icons.Default.Edit, "Modifier le solde")
                    }
                }
            }
        }
    }

    // Boîte de dialogue pour la saisie du nouveau solde.
    showDialogForAccount?.let { accountToEdit ->
        var tempBalance by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        AlertDialog(
            onDismissRequest = { showDialogForAccount = null },
            title = { Text("Modifier le solde de ${accountToEdit.name}") },
            text = {
                Column {
                    Text("Solde actuel : %.2f€".format(accountToEdit.balance), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempBalance,
                        onValueChange = { tempBalance = it },
                        label = { Text("Nouveau solde") },
                        suffix = { Text("€") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            tempBalance.toDoubleOrNull()?.let { onUpdateBalance(accountToEdit.id, it) }
                            focusManager.clearFocus()
                            showDialogForAccount = null
                        }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    tempBalance.toDoubleOrNull()?.let { onUpdateBalance(accountToEdit.id, it) }
                    showDialogForAccount = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                Button(onClick = { showDialogForAccount = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
