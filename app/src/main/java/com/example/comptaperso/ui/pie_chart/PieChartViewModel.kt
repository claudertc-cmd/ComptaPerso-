package com.example.comptaperso.ui.pie_chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comptaperso.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * `ChartData` est une classe de données simple qui représente une tranche du graphique.
 * @param id L'identifiant unique du compte.
 * @param label Le nom du compte à afficher dans la légende.
 * @param value La valeur (solde) du compte, qui détermine la taille de la tranche.
 */
data class ChartData(val id: String, val label: String, val value: Float)

/**
 * `GroupedChartData` représente un groupe de comptes (par exemple, "Bancaire", "Epargne").
 * @param groupName Le nom de la catégorie.
 * @param totalValue La somme des soldes de tous les comptes de ce groupe.
 * @param accounts La liste des `ChartData` individuels appartenant à ce groupe.
 */
data class GroupedChartData(val groupName: String, val totalValue: Float, val accounts: List<ChartData>)

/**
 * `PieChartViewModel` est le ViewModel pour l'écran `PieChartScreen`.
 * Sa responsabilité principale est de collecter les données brutes depuis le `DataRepository`,
 * de les transformer en données prêtes à être affichées par le graphique (`GroupedChartData`),
 * et de les exposer via un `StateFlow`.
 */
class PieChartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application)

    /**
     * `groupedData` est un `StateFlow` qui expose la liste des données groupées pour le graphique.
     * Il utilise `combine` pour réagir aux changements de n'importe quelle source de données
     * (comptes, transactions, etc.) et recalcule automatiquement les soldes et les groupes.
     */
    val groupedData: StateFlow<List<GroupedChartData>> = combine(
        repository.accounts,
        repository.transactions,
        repository.accountExtras,
        repository.balances
    ) { accounts, transactionsMap, extrasMap, balancesMap ->
        accounts
            .groupBy { it.type } // Regroupe les comptes par leur type (ex: "Bancaire").
            .map { (type, accountsInGroup) ->
                // Pour chaque groupe, calcule le solde de chaque compte.
                val accountsWithBalance = accountsInGroup.map { account ->
                    val balance = when (account.type) {
                        "Bancaire", "Carte de Crédit", "Paypal" ->
                            calculateFinalBalance(account, extrasMap[account.id], transactionsMap[account.id] ?: emptyList())
                        else ->
                            balancesMap[account.id] ?: 0.0
                    }
                    ChartData(account.id, account.name, balance.toFloat())
                }

                // Crée un objet `GroupedChartData` pour la catégorie.
                GroupedChartData(
                    groupName = type,
                    accounts = accountsWithBalance,
                    totalValue = accountsWithBalance.sumOf { it.value.toDouble() }.toFloat()
                )
            }
            .filter { it.accounts.isNotEmpty() } // Exclut les groupes sans comptes.
            // Trie les groupes pour que "Bancaire" apparaisse toujours en premier.
            .sortedWith(compareBy<GroupedChartData> { it.groupName != "Bancaire" }.thenBy { it.groupName })
    }.stateIn(
        scope = viewModelScope, // Le scope du ViewModel.
        started = SharingStarted.WhileSubscribed(5000), // Le Flow reste actif 5s après le dernier collecteur.
        initialValue = emptyList() // Valeur initiale pendant le chargement.
    )

    /**
     * Calcule le solde final pour un compte de type "Bancaire" en prenant en compte
     * le solde prévisionnel, les débits différés et les transactions non payées.
     */
    private fun calculateFinalBalance(
        account: Account,
        extras: AccountExtraInfo?,
        transactions: List<Transaction>
    ): Double {
        val provisional = extras?.provisionalBalance?.toDoubleOrNull() ?: 0.0
        val deferred = if (account.includeDeferredDebits) extras?.deferredDebits?.toDoubleOrNull() ?: 0.0 else 0.0
        val sumOfUnpaidCredits = transactions.filter { it.type == TransactionType.CREDIT && !it.isPaid }.sumOf { it.amount }
        val sumOfUnpaidDebits = transactions.filter { it.type == TransactionType.DEBIT && !it.isPaid }.sumOf { it.amount }
        return provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
    }
}
