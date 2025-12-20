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

// Nouvelle classe pour les données groupées
data class GroupedChartData(val groupName: String, val totalValue: Float, val accounts: List<ChartData>)

class PieChartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application)

    val groupedData: StateFlow<List<GroupedChartData>> = combine(
        repository.accounts,
        repository.transactions,
        repository.accountExtras,
        repository.balances
    ) { accounts, transactionsMap, extrasMap, balancesMap ->
        accounts
            .groupBy { it.type }
            .map { (type, accountsInGroup) ->
                val accountsWithBalance = accountsInGroup.map { account ->
                    val balance = when (account.type) {
                        "Bancaire", "Carte de Crédit", "Paypal" -> {
                            calculateFinalBalance(account, extrasMap[account.id], transactionsMap[account.id] ?: emptyList())
                        }
                        else -> {
                            balancesMap[account.id] ?: 0.0
                        }
                    }
                    ChartData(account.name, balance.toFloat())
                }.filter { it.value > 0 }

                GroupedChartData(
                    groupName = type,
                    accounts = accountsWithBalance,
                    totalValue = accountsWithBalance.sumOf { it.value.toDouble() }.toFloat()
                )
            }
            .filter { it.accounts.isNotEmpty() }
            .sortedWith(compareBy<GroupedChartData> { it.groupName != "Bancaire" }.thenBy { it.groupName })
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
