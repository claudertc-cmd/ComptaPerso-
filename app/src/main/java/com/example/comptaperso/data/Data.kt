package com.example.comptaperso.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.util.UUID

// Ce fichier définit les modèles de données (data classes) de l'application.
// L'annotation `@Serializable` permet à Kotlinx Serialization de les convertir en JSON et vice-versa.
// L'annotation `@Keep` empêche Proguard d'obfusquer ces classes, ce qui est crucial pour la sérialisation.

/**
 * Représente un compte utilisateur (bancaire, épargne, etc.).
 *
 * @param id Un identifiant unique pour le compte.
 * @param name Le nom du compte (ex: "Compte Courant").
 * @param type Le type de compte (ex: "Bancaire", "Epargne").
 * @param includeDeferredDebits Indique si les débits différés doivent être inclus dans les calculs de solde.
 * @param packageName Le nom de package de l'application bancaire associée (optionnel).
 */
@Keep
@Serializable
data class Account(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var type: String = "",
    var includeDeferredDebits: Boolean = false,
    var packageName: String? = null
)

/**
 * Représente une transaction récurrente (crédit ou débit).
 *
 * @param id Un identifiant unique pour la transaction.
 * @param name Le nom de la transaction (ex: "Loyer", "Salaire").
 * @param dayOfMonth Le jour du mois où la transaction a lieu.
 * @param amount Le montant de la transaction.
 * @param type Le type de transaction (`CREDIT` or `DEBIT`).
 * @param isPaid Indique si la transaction a été effectuée pour le mois en cours.
 */
@Keep
@Serializable
data class Transaction(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var dayOfMonth: Int = 1,
    var amount: Double = 0.0,
    var type: TransactionType = TransactionType.DEBIT,
    var isPaid: Boolean = false
)

/**
 * Représente les informations supplémentaires pour un compte, généralement celles qui sont mises à jour manuellement.
 *
 * @param provisionalBalance Le solde prévisionnel ou le solde affiché par la banque.
 * @param deferredDebits Le montant total des débits différés.
 * @param balanceDate La date à laquelle le solde a été mis à jour pour la dernière fois (au format ISO, ex: "2023-10-27").
 */
@Keep
@Serializable
data class AccountExtraInfo(
    var provisionalBalance: String = "",
    var deferredDebits: String = "",
    var balanceDate: String = ""
)

/**
 * Énumération pour les types de transactions.
 */
@Keep
@Serializable
enum class TransactionType {
    CREDIT,
    DEBIT
}
