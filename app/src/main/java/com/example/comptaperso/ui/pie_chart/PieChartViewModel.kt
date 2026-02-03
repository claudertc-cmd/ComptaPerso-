package com.example.comptaperso.ui.pie_chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comptaperso.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
     * NOUVEAU FORMAT : Un seul Flow suffit car toutes les données sont dans les objets Account.
     * Il réagit aux changements des comptes et recalcule automatiquement les soldes et les groupes.
     */
    val groupedData: StateFlow<List<GroupedChartData>> = repository.accounts
        .map { accounts ->
            accounts
                .groupBy { it.type } // Regroupe les comptes par leur type (ex: "Bancaire").
                .map { (type, accountsInGroup) ->
                    // Pour chaque groupe, calcule le solde de chaque compte.
                    val accountsWithBalance = accountsInGroup.map { account ->
                        val balance = when (account.type) {
                            "Bancaire", "Carte de Crédit", "Paypal" ->
                                calculateFinalBalance(account)
                            else ->
                                account.balance
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
        }
        .stateIn(
            scope = viewModelScope, // Le scope du ViewModel.
            started = SharingStarted.WhileSubscribed(5000), // Le Flow reste actif 5s après le dernier collecteur.
            initialValue = emptyList() // Valeur initiale pendant le chargement.
        )

    /**
     * Calcule le solde final pour un compte de type "Bancaire" en prenant en compte
     * le solde prévisionnel, les débits différés et les transactions non payées.
     * NOUVEAU FORMAT : Toutes les données sont dans l'objet Account.
     */
    private fun calculateFinalBalance(account: Account): Double {
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
        return provisional + sumOfUnpaidCredits - sumOfUnpaidDebits - deferred
    }
}
