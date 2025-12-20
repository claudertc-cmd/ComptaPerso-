package com.example.comptaperso.ui.pie_chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comptaperso.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Version simple: juste un nom et une valeur
data class ChartData(val label: String, val value: Float)

class PieChartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application)

    // Expose une liste simple, pas de données groupées
    val chartData: StateFlow<List<ChartData>> = combine(
        repository.accounts,
        repository.transactions,
        repository.accountExtras,
        repository.balances
    ) { accounts, transactionsMap, extrasMap, balancesMap ->
        accounts.map { account ->
            val balance = when (account.type) {
                "Bancaire", "Carte de Crédit", "Paypal" -> {
                    calculateFinalBalance(account, extrasMap[account.id], transactionsMap[account.id] ?: emptyList())
                }
                else -> {
                    balancesMap[account.id] ?: 0.0
                }
            }
            ChartData(account.name, balance.toFloat())
        }
        .filter { it.value > 0 } // On garde ce filtre, c'est utile
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
