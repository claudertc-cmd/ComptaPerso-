package com.example.comptaperso.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType
import com.example.comptaperso.navigation.Screen
import com.example.comptaperso.ui.components.BalanceSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Formate une date ISO (ex: "2023-10-27") en une chaîne de caractères lisible (ex: "27 oct. 2023").
 * @param isoDate La date au format ISO.
 * @return La date formatée ou la chaîne originale en cas d'erreur.
 */
private fun formatDateForDisplay(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (e: DateTimeParseException) {
        isoDate
    }
}

/**
 * Formate une date ISO en une chaîne de caractères relative (ex: "Aujourd'hui", "Hier", "Il y a 3 jours").
 * @param isoDate La date au format ISO.
 * @return La date formatée en format relatif ou la chaîne originale en cas d'erreur.
 */
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

/**
 * `HomeScreen` est l'écran principal qui affiche un résumé des comptes de l'utilisateur.
 * Il présente un solde total, puis une liste de comptes regroupés par type (Bancaire, Epargne, etc.).
 * Chaque compte est cliquable pour naviguer vers son écran de détails.
 *
 * @param accounts La liste de tous les comptes de l'utilisateur.
 * @param balances La map des soldes pour les comptes simples (Epargne, etc.).
 * @param allTransactions La map de toutes les transactions pour les comptes complexes.
 * @param accountExtras La map des informations supplémentaires pour les comptes.
 * @param onNavigate Callback pour gérer la navigation vers d'autres écrans.
 */
@Composable
fun HomeScreen(
    accounts: List<Account>,
    balances: Map<String, Double>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>,
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
                balances = balances,
                allTransactions = allTransactions,
                accountExtras = accountExtras,
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
                                    val extras = accountExtras[account.id]
                                    val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                                    val deferred = if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
                                    val transactions = allTransactions[account.id] ?: emptyList()
                                    val sumOfUnpaidCredits = transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
                                    val sumOfUnpaidDebits = transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
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
                                val extras = accountExtras[account.id]
                                val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                                val deferred = if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
                                val transactions = allTransactions[account.id] ?: emptyList()
                                val sumOfUnpaidCredits = transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
                                val sumOfUnpaidDebits = transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
                                provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
                            }
                            else -> balances[account.id] ?: 0.0
                        }

                        val extraInfo = accountExtras[account.id]
                        val formattedDate = remember(extraInfo) {
                            extraInfo?.balanceDate?.takeIf { it.isNotBlank() }?.let { formatDateAsTimeAgo(it) }
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
