package com.example.comptaperso

/**
 * Énumération représentant les différents états possibles pour les boîtes de dialogue
 * liées aux transactions.
 */
enum class DialogState {
    NONE,                   // Aucune boîte de dialogue n'est affichée
    CHOOSE_ACTION,          // Boîte de dialogue pour choisir entre un crédit et un débit
    ADD_OR_EDIT_TRANSACTION // Boîte de dialogue pour ajouter ou modifier une transaction
}