package com.example.comptaperso.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType
import com.example.comptaperso.navigation.Screen
import com.example.comptaperso.ui.components.BalanceSummary
import com.example.comptaperso.ui.theme.errorContainerLight
import com.example.comptaperso.ui.theme.onSurfaceVariantLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

private fun formatDateForDisplay(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (e: DateTimeParseException) {
        isoDate
    }
}

private fun formatDateAsTimeAgo(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(date, today)

        when (daysBetween) {
            0L -> "Aujourd'hui"
            1L -> "Hier"
            else -> "Il y a $daysBetween jours"
        }
    } catch (e: DateTimeParseException) {
        isoDate
    }
}

@Composable
fun HomeScreen(
    accounts: List<Account>,
    balances: Map<String, Double>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>,
    onNavigate: (Screen) -> Unit,
    onSaveToJson: () -> Unit,
    onRestoreFromJson: () -> Unit,
    onSaveToFirebase: () -> Unit,
    onRestoreFromFirebase: () -> Unit,
    onLogout: () -> Unit
) {
    val accountTypeOrder = listOf("Bancaire", "Carte de Crédit", "Paypal", "Epargne", "Assurance")

    val groupedAccounts = remember(accounts) {
        accounts.groupBy { it.type }.toSortedMap(compareBy { accountTypeOrder.indexOf(it) })
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        topBar = { }   // pas de bandeau, on gère tout dans le contenu
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1) Colonne principale : BalanceSummary en haut + liste des comptes
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                BalanceSummary(
                    accounts = accounts,
                    balances = balances,
                    allTransactions = allTransactions,
                    accountExtras = accountExtras,
                    onTotalTapped = { onNavigate(Screen.PieChart) } // Lier le clic à la navigation
                )

                // Liste scrollable sous le résumé
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedAccounts.forEach { (type, accountsForType) ->

                        item {
                            val typeTotal = accountsForType.sumOf { account ->
                                when (account.type) {
                                    "Bancaire", "Carte de Crédit", "Paypal" -> {
                                        val extras = accountExtras[account.id]
                                        val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                                        val deferred = if (account.includeDeferredDebits)
                                            extras?.deferredDebits?.toDoubleOrNull() ?: 0.0
                                        else 0.0
                                        val transactions = allTransactions[account.id] ?: emptyList()
                                        val sumOfUnpaidCredits = transactions
                                            .filter { it.type == TransactionType.CREDIT && !it.isPaid }
                                            .sumOf { it.amount }
                                        val sumOfUnpaidDebits = transactions
                                            .filter { it.type == TransactionType.DEBIT && !it.isPaid }
                                            .sumOf { it.amount }
                                        provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
                                    }
                                    else -> balances[account.id] ?: 0.0
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Le titre "Comptes Bancaires", "Comptes Epargne", etc.
                                Text(
                                    "Comptes $type",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Le montant total de la catégorie
                                Text(
                                       "%.0f€  ".format(typeTotal),// Si le type est "Bancaire", on utilise un style plus grand ET on le met en gras.
                                        style = if (type == "Bancaire") {
                                            MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold) // <-- On ajoute .copy(fontWeight = FontWeight.Bold)
                                        } else {
                                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) // Style habituel pour les autres
                                        },
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        items(accountsForType) { account ->
                            val balance = when (account.type) {
                                "Bancaire", "Carte de Crédit", "Paypal" -> {
                                    val extras = accountExtras[account.id]
                                    val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                                    val deferred = if (account.includeDeferredDebits)
                                        extras?.deferredDebits?.toDoubleOrNull() ?: 0.0
                                    else 0.0
                                    val transactions = allTransactions[account.id] ?: emptyList()
                                    val sumOfUnpaidCredits = transactions
                                        .filter { it.type == TransactionType.CREDIT && !it.isPaid }
                                        .sumOf { it.amount }
                                    val sumOfUnpaidDebits = transactions
                                        .filter { it.type == TransactionType.DEBIT && !it.isPaid }
                                        .sumOf { it.amount }
                                    provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
                                }
                                else -> balances[account.id] ?: 0.0
                            }

                            val extraInfo = accountExtras[account.id]

                            val formattedDate = remember(extraInfo) {
                                val dateString = extraInfo?.balanceDate
                                if (!dateString.isNullOrBlank()) {
                                    formatDateAsTimeAgo(dateString)
                                } else {
                                    null
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                                    .clickable {
                                        val filteredIndex =
                                            accounts.filter { it.type == account.type }.indexOf(account)
                                        if (account.type == "Epargne" || account.type == "Assurance") {
                                            onNavigate(
                                                Screen.SimplifiedAccounts(
                                                    account.type,
                                                    filteredIndex
                                                )
                                            )
                                        } else {
                                            onNavigate(
                                                Screen.AccountViewPager(
                                                    account.type,
                                                    filteredIndex
                                                )
                                            )
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (formattedDate != null) {
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "%.0f€".format(balance),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2) Icône de menu superposée en haut à droite
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = onSurfaceVariantLight,
                    modifier = Modifier
                        .width(220.dp) // évite un menu trop large
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(errorContainerLight, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Répartition des actifs") },
                        onClick = {
                            onNavigate(Screen.PieChart)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Gérer les comptes") },
                        onClick = {
                            onNavigate(Screen.AccountManagement)
                            menuExpanded = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Sauvegarde locale") },
                        onClick = {
                            onSaveToJson()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Restauration locale") },
                        onClick = {
                            onRestoreFromJson()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sauvegarde Cloud") },
                        onClick = {
                            onSaveToFirebase()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Restauration Cloud") },
                        onClick = {
                            onRestoreFromFirebase()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Se déconnecter") },
                        onClick = {
                            onLogout()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
