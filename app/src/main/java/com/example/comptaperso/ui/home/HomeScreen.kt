package com.example.comptaperso.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.TransactionType
import com.example.comptaperso.navigation.Screen
import com.example.comptaperso.ui.components.BalanceSummary
import com.example.comptaperso.DateUtils

@Composable
fun HomeScreen(
    accounts: List<Account>,
    onNavigate: (Screen) -> Unit
) {
    // Définit l'ordre d'affichage des types de comptes.
    val accountTypeOrder = listOf("Bancaire", "Carte de Crédit", "Paypal", "Epargne", "Assurance")

    // Regroupe les comptes par type et les trie selon `accountTypeOrder`.
    val groupedAccounts = remember(accounts) {
        accounts.groupBy { it.type }.toSortedMap(compareBy { accountTypeOrder.indexOf(it) })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Affiche le résumé du solde total en haut de l'écran.
            BalanceSummary(
                accounts = accounts,
                onTotalTapped = { onNavigate(Screen.PieChart) } // Navigue vers le graphique en cas de clic.
            )

            // Affiche la liste scrollable des comptes, regroupés par type.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedAccounts.forEach { (type, accountsForType) ->

                    // Affiche le titre de la catégorie (ex: "Comptes Bancaires") et le solde total de la catégorie.
                    item {
                        val typeTotal = accountsForType.sumOf { account ->
                            // Le calcul du solde dépend du type de compte.
                            when (account.type) {
                                "Bancaire", "Carte de Crédit", "Paypal" -> {
                                    val provisional = account.extraInfo.provisionalBalance.toDoubleOrNull() ?: 0.0
                                    val deferred = if (account.includeDeferredDebits) {
                                        account.extraInfo.deferredDebits.toDoubleOrNull() ?: 0.0
                                    } else {
                                        0.0
                                    }
                                    val sumOfUnpaidCredits = account.transactions
                                        .filter { it.type == TransactionType.CREDIT && !it.isPaid }
                                        .sumOf { it.amount }
                                    val sumOfUnpaidDebits = account.transactions
                                        .filter { it.type == TransactionType.DEBIT && !it.isPaid }
                                        .sumOf { it.amount }
                                    provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
                                }
                                else -> account.balance
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Comptes $type", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "%.0f€".format(typeTotal),
                                style = if (type == "Bancaire") MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Affiche chaque compte de la catégorie dans une carte.
                    items(accountsForType) { account ->
                        val balance = when (account.type) {
                            "Bancaire", "Carte de Crédit", "Paypal" -> {
                                val provisional = account.extraInfo.provisionalBalance.toDoubleOrNull() ?: 0.0
                                val deferred = if (account.includeDeferredDebits) {
                                    account.extraInfo.deferredDebits.toDoubleOrNull() ?: 0.0
                                } else {
                                    0.0
                                }
                                val sumOfUnpaidCredits = account.transactions
                                    .filter { it.type == TransactionType.CREDIT && !it.isPaid }
                                    .sumOf { it.amount }
                                val sumOfUnpaidDebits = account.transactions
                                    .filter { it.type == TransactionType.DEBIT && !it.isPaid }
                                    .sumOf { it.amount }
                                provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
                            }
                            else -> account.balance
                        }

                        val formattedDate = remember(account.extraInfo) {
                            account.extraInfo.balanceDate.takeIf { it.isNotBlank() }?.let { DateUtils.getRelativeDateLabel(it) }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 16.dp)
                                .clickable {
                                    val filteredIndex = accounts.filter { it.type == account.type }.indexOf(account)
                                    val screen = if (account.type == "Epargne" || account.type == "Assurance") {
                                        Screen.SimplifiedAccounts(account.type, filteredIndex)
                                    } else {
                                        Screen.AccountViewPager(account.type, filteredIndex)
                                    }
                                    onNavigate(screen)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, style = MaterialTheme.typography.bodyLarge)
                                    formattedDate?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Text("%.0f€".format(balance), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
