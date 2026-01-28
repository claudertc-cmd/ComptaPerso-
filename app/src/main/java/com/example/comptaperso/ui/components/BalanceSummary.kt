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
 * `BalanceSummary` est un composant qui affiche un résumé du solde total de tous les comptes.
 * Il calcule le solde total en tenant compte des différents types de comptes et de leurs transactions.
 *
 * @param accounts La liste de tous les comptes de l'utilisateur.
 * @param balances La map des soldes pour les comptes simples (Epargne, Assurance, etc.).
 * @param allTransactions La map de toutes les transactions pour les comptes complexes (Bancaire, etc.).
 * @param accountExtras La map des informations supplémentaires pour les comptes.
 * @param onTotalTapped Le callback déclenché lorsque le montant total est cliqué.
 */
@Composable
fun BalanceSummary(
    accounts: List<Account>,
    balances: Map<String, Double>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>,
    onTotalTapped: () -> Unit // Callback pour gérer le clic sur le total.
) {
    // Calcule le solde total en itérant sur tous les comptes.
    val totalAll = accounts.sumOf { account ->
        when (account.type) {
            // Pour les comptes complexes, le solde est calculé en temps réel.
            "Bancaire", "Carte de Crédit", "Paypal" -> {
                val extras = accountExtras[account.id]
                val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
                val deferred = if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
                val transactions = allTransactions[account.id] ?: emptyList()
                val sumOfUnpaidCredits = transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
                val sumOfUnpaidDebits = transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
                provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
            }
            // Pour les comptes simples, on utilise directement le solde stocké.
            else -> balances[account.id] ?: 0.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Affiche le solde total avec une animation de "roulement".
        TappableRollingInt(
            value = totalAll.toInt(),
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 4.dp),
            onTapped = onTotalTapped // Passe le callback au composant.
        )
    }
}
