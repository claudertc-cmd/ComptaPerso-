package com.example.comptaperso.ui.account_view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction

/**
 * `AccountViewPagerScreen` est un composant qui affiche un carrousel horizontal (ViewPager)
 * permettant à l'utilisateur de naviguer entre les pages de détails de plusieurs comptes.
 * Chaque page du ViewPager est une instance de `AccountPage`.
 *
 * @param pagerState L'état du `HorizontalPager`, qui contrôle la page actuellement affichée.
 * @param accounts La liste des comptes à afficher dans le carrousel.
 * @param allTransactions Une map contenant les transactions de tous les comptes, la clé étant l'ID du compte.
 * @param accountExtras Une map contenant les informations supplémentaires pour chaque compte.
 * @param onUpdate Un callback déclenché pour mettre à jour la liste des transactions d'un compte.
 * @param onUpdateExtras Un callback déclenché pour mettre à jour les informations supplémentaires d'un compte.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountViewPagerScreen(
    pagerState: PagerState,
    accounts: List<Account>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>,
    onUpdate: (accountId: String, transactions: List<Transaction>) -> Unit,
    onUpdateExtras: (accountId: String, extraInfo: AccountExtraInfo) -> Unit
) {
    // `HorizontalPager` est le composant de Compose qui permet de créer un carrousel.
    HorizontalPager(state = pagerState) { pageIndex ->
        // Pour chaque page, on récupère les informations spécifiques au compte.
        val account = accounts[pageIndex]
        val transactions = allTransactions[account.id] ?: emptyList()
        val extras = accountExtras[account.id]

        // Chaque page du carrousel est une `AccountPage`.
        AccountPage(
            account = account,
            transactions = transactions,
            extras = extras,
            // Les callbacks sont passés à `AccountPage` pour permettre les mises à jour.
            onUpdate = { updatedTransactions -> onUpdate(account.id, updatedTransactions) },
            onUpdateExtras = { newExtras -> onUpdateExtras(account.id, newExtras) }
        )
    }
}