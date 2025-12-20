package com.example.comptaperso.ui.pie_chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comptaperso.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChartData(val label: String, val value: Float)

class PieChartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application)

    val chartData: StateFlow<List<ChartData>> = combine(
        repository.accounts,
        repository.transactions,
        repository.accountExtras,
        repository.balances // Ajouter les soldes pour les comptes simplifiés
    ) { accounts, transactionsMap, extrasMap, balancesMap ->
        accounts.map { account ->
            val balance = when (account.type) {
                "Bancaire", "Carte de Crédit", "Paypal" -> {
                    val extras = extrasMap[account.id]
                    val transactions = transactionsMap[account.id] ?: emptyList()
                    calculateFinalBalance(account, extras, transactions)
                }
                else -> {
                    // Pour "Epargne", "Assurance", etc.
                    balancesMap[account.id] ?: 0.0
                }
            }
            ChartData(account.name, balance.toFloat())
        }
        .filter { it.value > 0 } // Ne pas montrer les comptes avec un solde nul ou négatif
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun calculateFinalBalance(
        account: Account,
        extras: AccountExtraInfo?,
        transactions: List<Transaction>
    ): Double {
        val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
        val deferred =
            if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
        val sumOfUnpaidCredits =
            transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
        val sumOfUnpaidDebits =
            transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
        return provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
    }
}
