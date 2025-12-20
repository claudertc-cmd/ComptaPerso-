package com.example.comptaperso.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType

/**
 * Affiche un résumé des soldes des différents types de comptes.
 *
 * @param accounts La liste de tous les comptes.
 * @param balances La map des soldes pour les comptes d'épargne et d'assurance.
 * @param allTransactions L'ensemble des transactions pour tous les comptes.
 * @param accountExtras Les informations supplémentaires pour chaque compte.
 */


@Composable
fun BalanceSummary(
    accounts: List<Account>,
    balances: Map<String, Double>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>
) {
    val totalAll = accounts.sumOf { account ->
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

    // Carte optionnelle, tu peux la remettre si tu veux un fond
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
//        )
//    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp), // padding réduit
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Text(
//                text = "Total des comptes",
//                style = shadowStyle,
//                color = MaterialTheme.colorScheme.onSurface
//            )
            TappableRollingInt(
                value = totalAll.toInt(),
                fontSize = 24.sp,              // un peu plus petit
                modifier = Modifier
                    .padding(top = 4.dp)      // moins de marge
            )
        }
        // }
    }