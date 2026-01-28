package com.example.comptaperso

/**
 * Représente les différents états possibles du processus de restauration des données.
 * Utilisé pour afficher l'état de l'interface utilisateur pendant le chargement initial de l'application.
 */
enum class RestoreState {
    /**
     * Indique que la restauration des données est en cours.
     * L'interface utilisateur devrait afficher un indicateur de chargement.
     */
    InProgress,

    /**
     * Indique que la restauration des données a réussi.
     * L'application peut maintenant afficher le contenu principal.
     */
    Success,

    /**
     * Indique qu'une erreur est survenue lors de la restauration des données.
     * L'interface utilisateur devrait afficher un message d'erreur et une option pour réessayer.
     */
    Failure,

    /**
     * Indique que la restauration a dépassé le temps imparti.
     * L'interface utilisateur devrait offrir la possibilité de continuer sans les données ou de quitter.
     */
    Timeout
}