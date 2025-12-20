package com.example.comptaperso.ui.account_management

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Account
import com.example.comptaperso.ui.components.AppPicker

/**
 * Écran de gestion des comptes, permettant d'ajouter, modifier et supprimer des comptes.
 *
 * @param accounts La liste des comptes existants.
 * @param onAddAccount Callback pour ajouter un nouveau compte.
 * @param onUpdateAccount Callback pour mettre à jour un compte existant.
 * @param onDeleteAccount Callback pour supprimer un compte.
 * @param onAccountAdded Callback pour naviguer vers l'écran précédent (popBackStack).
 */
@Composable
fun AccountManagementScreen(
    accounts: List<Account>,
    onAddAccount: (name: String, type: String, includeDeferred: Boolean, packageName: String?) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onAccountAdded: () -> Unit
) {
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    var accountName by remember { mutableStateOf("") }
    val accountTypes = listOf("Bancaire", "Epargne", "Assurance", "Paypal", "Carte de Crédit")
    var selectedAccountType by remember { mutableStateOf(accountTypes.first()) }
    var includeDeferred by remember { mutableStateOf(false) }
    var packageName by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }

    LaunchedEffect(accountToEdit) {
        if (accountToEdit != null) {
            accountName = accountToEdit!!.name
            selectedAccountType = accountToEdit!!.type
            includeDeferred = accountToEdit!!.includeDeferredDebits
            packageName = accountToEdit?.packageName ?: ""
        } else {
            accountName = ""
            selectedAccountType = accountTypes.first()
            includeDeferred = false
            packageName = ""
        }
    }

    val isEditing = accountToEdit != null
    val title = if (isEditing) "Modifier le compte" else "Ajouter un nouveau compte"
    val buttonText = if (isEditing) "Enregistrer" else "Ajouter le compte"

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Nom du compte") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Nom du package (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(onClick = { showAppPicker = true }) {
                    Text("Choisir une application")
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Type de compte", style = MaterialTheme.typography.titleMedium)
                Column {
                    accountTypes.forEach { accountType ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { selectedAccountType = accountType })
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (accountType == selectedAccountType),
                                onClick = { selectedAccountType = accountType }
                            )
                            Text(text = accountType, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (selectedAccountType == "Bancaire") {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { includeDeferred = !includeDeferred }
                    ) {
                        Checkbox(
                            checked = includeDeferred,
                            onCheckedChange = { includeDeferred = it }
                        )
                        Text("Inclure les prélèvements différés", modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedName = accountName.trim()
                            if (trimmedName.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Veuillez donner un nom au compte",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (isEditing) {
                                val updatedAccount = accountToEdit!!.copy(
                                    name = trimmedName,
                                    type = selectedAccountType,
                                    includeDeferredDebits = if (selectedAccountType == "Bancaire") includeDeferred else false,
                                    packageName = packageName.ifBlank { null }
                                )
                                onUpdateAccount(updatedAccount)
                            } else {
                                val finalPackageName = packageName.ifBlank { null }
                                onAddAccount(
                                    trimmedName,
                                    selectedAccountType,
                                    if (selectedAccountType == "Bancaire") includeDeferred else false,
                                    finalPackageName
                                )
                            }

                            // Dans les deux cas (ajout ou édition), on quitte la page
                            onAccountAdded()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(buttonText)
                    }

                    if (isEditing) {
                        OutlinedButton(
                            onClick = { accountToEdit = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Comptes existants", style = MaterialTheme.typography.titleMedium)
            }
            items(accounts, key = { it.id }) { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(account.type, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { accountToEdit = account }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier le compte")
                        }
                        IconButton(
                            onClick = {
                                accountToDelete = account
                                showDeleteConfirmationDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer le compte")
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPicker(
            onAppSelected = {
                packageName = it
                showAppPicker = false
            },
            onDismissRequest = { showAppPicker = false }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                accountToDelete = null
            },
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "Êtes-vous sûr de vouloir supprimer le compte \"${accountToDelete?.name}\" ? Cette action est irréversible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let {
                            onDeleteAccount(it)
                            if (accountToEdit == it) {
                                accountToEdit = null
                            }
                        }
                        showDeleteConfirmationDialog = false
                        accountToDelete = null
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        accountToDelete = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
