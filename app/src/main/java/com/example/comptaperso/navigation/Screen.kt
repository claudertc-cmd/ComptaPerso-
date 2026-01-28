package com.example.comptaperso.navigation

/**
 * `Screen` est une classe scellée (sealed class) qui définit tous les écrans possibles de l'application.
 * L'utilisation d'une classe scellée garantit que tous les écrans sont connus au moment de la compilation,
 * ce qui aide à prévenir les erreurs de navigation.
 */
sealed class Screen {
    /**
     * Représente l'écran d'accueil principal de l'application (non utilisé actuellement).
     */
    object Home : Screen()

    /**
     * Représente l'écran de gestion des comptes, où l'utilisateur peut ajouter, modifier ou supprimer des comptes.
     */
    object AccountManagement : Screen()

    /**
     * Représente l'écran principal affichant un graphique circulaire (camembert) des soldes des comptes.
     */
    object PieChart : Screen()

    /**
     * Représente l'écran qui affiche un ViewPager (carrousel) pour les comptes de type "Bancaire".
     * @param accountType Le type de compte à afficher (ex: "Bancaire").
     * @param initialIndex L'index du compte à afficher en premier dans le ViewPager.
     */
    data class AccountViewPager(val accountType: String, val initialIndex: Int) : Screen()

    /**
     * Représente l'écran qui affiche une vue simplifiée pour les comptes comme "Epargne" ou "Assurance".
     * @param accountType Le type de compte à afficher.
     * @param initialIndex L'index du compte à afficher en premier.
     */
    data class SimplifiedAccounts(val accountType: String, val initialIndex: Int) : Screen()
}
