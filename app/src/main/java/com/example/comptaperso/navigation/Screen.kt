package com.example.comptaperso.navigation

sealed class Screen {
    object Home : Screen()
    object AccountManagement : Screen()
    data class AccountViewPager(val accountType: String, val initialIndex: Int) : Screen()
    data class SimplifiedAccounts(val accountType: String, val initialIndex: Int) : Screen()
}
