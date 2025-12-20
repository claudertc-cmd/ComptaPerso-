package com.example.comptaperso.ui.account_view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.ui.theme.AppTheme
import com.example.comptaperso.ui.theme.Theme

/**
 * Affiche un carrousel horizontal (ViewPager) pour naviguer entre les pages de différents comptes.
 *
 * @param pagerState L'état du pager, qui contrôle la page actuellement affichée.
 * @param accounts La liste des comptes à afficher dans le pager.
 * @param allTransactions L'ensemble des transactions de tous les comptes.
 * @param accountExtras Les informations supplémentaires pour chaque compte.
 * @param onUpdate Callback pour mettre à jour les transactions d'un compte.
 * @param onUpdateExtras Callback pour mettre à jour les informations supplémentaires d'un compte.
 * @param onBack Callback pour revenir à l'écran précédent.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountViewPagerScreen(
    pagerState: PagerState,
    accounts: List<Account>,
    allTransactions: Map<String, List<Transaction>>,
    accountExtras: Map<String, AccountExtraInfo>,
    onUpdate: (accountId: String, transactions: List<Transaction>) -> Unit,
    onUpdateExtras: (accountId: String, extraInfo: AccountExtraInfo) -> Unit,
    onBack: () -> Unit
) {
    // Le `HorizontalPager` est le conteneur qui permet de balayer horizontalement entre les pages.
    HorizontalPager(state = pagerState) { page ->
        // Pour chaque page, on récupère le compte, ses transactions et ses extras correspondants.
        val account = accounts[page]
        val transactions = allTransactions[account.id] ?: emptyList()
        val extras = accountExtras[account.id]

        // Détermine le thème à appliquer en fonction du nom du compte.
        val theme = if (account.name.contains("Boursobank", ignoreCase = true)) {
            Theme.BOURSOBANK
        } else if (account.name.contains("Fortuneo", ignoreCase = true)) {
            Theme.FORTUNEO
        } else {
            Theme.SPRING // Thème par défaut si aucun nom de banque ne correspond.
        }

        // Applique le thème dynamique à la page de compte individuelle.
        AppTheme(theme = theme) {
            // Affiche le contenu détaillé de la page du compte.
            AccountPage(
                account = account,
                transactions = transactions,
                extras = extras,
                onUpdate = { updatedTransactions -> onUpdate(account.id, updatedTransactions) },
                onUpdateExtras = { newExtras -> onUpdateExtras(account.id, newExtras) },
                onBack = onBack
            )
        }
    }
}