package com.example.comptaperso.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.util.UUID

// Correction for removing Firebase annotations

@Keep
@Serializable
data class Account(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var type: String = "",
    var includeDeferredDebits: Boolean = false,
    var packageName: String? = null
)

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

@Keep
@Serializable
data class AccountExtraInfo(
    var provisionalBalance: String = "",
    var deferredDebits: String = "",
    var balanceDate: String = ""
)

@Keep
@Serializable
enum class TransactionType {
    CREDIT,
    DEBIT
}
