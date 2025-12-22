package com.example.comptaperso

import android.net.Uri
import androidx.activity.addCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Account
import com.example.comptaperso.data.AccountExtraInfo
import com.example.comptaperso.data.DataRepository
import com.example.comptaperso.navigation.Screen
import com.example.comptaperso.ui.account_management.AccountManagementScreen
import com.example.comptaperso.ui.account_view.AccountViewPagerScreen
import com.example.comptaperso.ui.home.HomeScreen
import com.example.comptaperso.ui.pie_chart.PieChartScreen
import com.example.comptaperso.ui.simplified_accounts.SimplifiedAccountsScreen
import com.example.comptaperso.ui.theme.AppTheme
import com.example.comptaperso.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppShell(activity: MainActivity, onLogout: () -> Unit) {
    var showSplashScreen by remember { mutableStateOf(true) }
    var restoreState by remember { mutableStateOf(RestoreState.InProgress) }
    var retryTrigger by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val dataRepository = remember { DataRepository(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(retryTrigger) {
        if (showSplashScreen) {
            restoreState = RestoreState.InProgress
            scope.launch {
                val result = withTimeoutOrNull(30_000L) { // 30 secondes de timeout
                    try {
                        dataRepository.restoreDataFromFirebase()
                        RestoreState.Success
                    } catch (e: Exception) {
                        RestoreState.Failure
                    }
                }
                restoreState = result ?: RestoreState.Timeout
            }
        }
    }

    LaunchedEffect(restoreState) {
        if (restoreState == RestoreState.Success) {
            delay(1500)
            showSplashScreen = false
        }
    }

    if (showSplashScreen) {
        AppTheme(theme = Theme.SPRING) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (restoreState) {
                        RestoreState.InProgress -> {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Connexion en cours...")
                        }
                        RestoreState.Success -> {
                            Text("")
                        }
                        RestoreState.Failure -> {
                            Text("La restauration des données a échoué.")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { retryTrigger++ }) {
                                Text("Réessayer")
                            }
                        }
                        RestoreState.Timeout -> {
                            Text("La restauration a pris trop de temps.")
                            Spacer(Modifier.height(16.dp))
                            Row {
                                OutlinedButton(onClick = { activity.finish() }) {
                                    Text("Quitter")
                                }
                                Spacer(Modifier.width(16.dp))
                                Button(onClick = { showSplashScreen = false }) {
                                    Text("Continuer sans restaurer")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Il est DECONSEILLE de continuer",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.PieChart) }

        val accounts by dataRepository.accounts.collectAsState(initial = emptyList())
        val allTransactions by dataRepository.transactions.collectAsState(initial = emptyMap())
        val balances by dataRepository.balances.collectAsState(initial = emptyMap())
        val accountExtras by dataRepository.accountExtras.collectAsState(initial = emptyMap())

        // Sauvegarde automatique des données sur Firebase lorsque les données changent
        LaunchedEffect(accounts, allTransactions, balances, accountExtras) {
            // Ne sauvegarde pas l'état initial vide
            if (accounts.isNotEmpty() || allTransactions.isNotEmpty() || balances.isNotEmpty() || accountExtras.isNotEmpty()) {
                scope.launch {
                    dataRepository.saveDataToFirebase()
                }
            }
        }

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                uri?.let {
                    scope.launch {
                        dataRepository.restoreDataFromJson(it)
                    }
                }
            }
        )

        activity.onBackPressedDispatcher.addCallback(owner = activity) {
            if (currentScreen !is Screen.PieChart) {
                currentScreen = Screen.PieChart
            }
        }


        val theme = if (currentScreen is Screen.AccountViewPager) {
            val screen = currentScreen as Screen.AccountViewPager
            val account = accounts.filter { it.type == screen.accountType }[screen.initialIndex]
            if (account.name.contains("Boursobank", ignoreCase = true)) {
                Theme.BOURSOBANK
            } else {
                Theme.SPRING
            }
        } else {
            Theme.SPRING // Thème par défaut
        }

        AppTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        if (currentScreen !is Screen.AccountViewPager &&
                            currentScreen !is Screen.Home &&
                            currentScreen !is Screen.PieChart // Ne pas montrer la barre pour le graphique
                        ) {
                            TopAppBar(
                                title = {
                                    val title = when (val screen = currentScreen) {
                                        is Screen.AccountManagement -> "Gestion des comptes"
                                        is Screen.SimplifiedAccounts -> when (screen.accountType) {
                                            "Epargne" -> "Comptes Epargne"
                                            "Assurance" -> "Comptes Assurance"
                                            else -> screen.accountType
                                        }

                                        else -> ""
                                    }
                                    Text(title)
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                ),
                                navigationIcon = {
                                    if (currentScreen !is Screen.PieChart) {
                                        IconButton(onClick = { currentScreen = Screen.PieChart }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Retour"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        when (val screen = currentScreen) {

                            is Screen.Home -> HomeScreen(
                                accounts = accounts,
                                balances = balances,
                                allTransactions = allTransactions,
                                accountExtras = accountExtras,
                                onNavigate = { currentScreen = it }
                            )

                            is Screen.AccountManagement -> AccountManagementScreen(
                                accounts = accounts,
                                onAddAccount = { name, type, includeDeferred, packageName ->
                                    scope.launch {
                                        val newAccount = Account(
                                            name = name,
                                            type = type,
                                            includeDeferredDebits = includeDeferred,
                                            packageName = packageName
                                        )
                                        dataRepository.saveAccounts(accounts + newAccount)

                                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        if (type == "Bancaire") {
                                            val updatedExtras = accountExtras.toMutableMap().apply {
                                                this[newAccount.id] = AccountExtraInfo(
                                                    provisionalBalance = "0.0",
                                                    deferredDebits = "0.0",
                                                    balanceDate = today
                                                )
                                            }
                                            dataRepository.saveAccountExtras(updatedExtras)

                                            val updatedBalances = balances.toMutableMap().apply {
                                                this[newAccount.id] = 0.0
                                            }
                                            dataRepository.saveBalances(updatedBalances)
                                        } else {
                                            val updatedExtras = accountExtras.toMutableMap().apply {
                                                this[newAccount.id] = AccountExtraInfo(balanceDate = today)
                                            }
                                            dataRepository.saveAccountExtras(updatedExtras)
                                        }

                                        if (type != "Epargne" && type != "Assurance") {
                                            val updatedTransactions =
                                                allTransactions.toMutableMap().apply {
                                                    this[newAccount.id] = mutableListOf()
                                                }
                                            dataRepository.saveTransactions(updatedTransactions)
                                        }
                                    }
                                },
                                onUpdateAccount = { accountToUpdate ->
                                    scope.launch {
                                        dataRepository.saveAccounts(
                                            accounts.map {
                                                if (it.id == accountToUpdate.id) accountToUpdate else it
                                            }
                                        )
                                    }
                                },
                                onDeleteAccount = { account ->
                                    scope.launch {
                                        dataRepository.saveAccounts(
                                            accounts.filterNot { it.id == account.id }
                                        )

                                        val updatedExtras = accountExtras.toMutableMap().apply {
                                            remove(account.id)
                                        }
                                        dataRepository.saveAccountExtras(updatedExtras)

                                        if (account.type == "Epargne" || account.type == "Assurance") {
                                            val updatedBalances = balances.toMutableMap().apply {
                                                remove(account.id)
                                            }
                                            dataRepository.saveBalances(updatedBalances)
                                        } else {
                                            val updatedTransactions =
                                                allTransactions.toMutableMap().apply {
                                                    remove(account.id)
                                                }
                                            dataRepository.saveTransactions(updatedTransactions)
                                        }
                                    }
                                },
                                onAccountAdded = { currentScreen = Screen.PieChart }
                            )

                            is Screen.PieChart -> PieChartScreen(
                                onAccountClick = { accountId ->
                                    val account = accounts.find { it.id == accountId }
                                    if (account != null) {
                                        val accountType = account.type
                                        val initialIndex = accounts.filter { it.type == accountType }.indexOfFirst { it.id == accountId }
                                        if (account.type == "Epargne" || account.type == "Assurance") {
                                            currentScreen = Screen.SimplifiedAccounts(accountType, initialIndex)
                                        } else {
                                            currentScreen = Screen.AccountViewPager(accountType, initialIndex)
                                        }
                                    }
                                },
                                onNavigate = { currentScreen = it },
                                onSaveToJson = {
                                    scope.launch {
                                        dataRepository.saveDataToJson(
                                            accounts,
                                            allTransactions,
                                            balances,
                                            accountExtras
                                        )
                                    }
                                },
                                onRestoreFromJson = {
                                    filePickerLauncher.launch("application/json")
                                },
                                onSaveToFirebase = {
                                    scope.launch {
                                        dataRepository.saveDataToFirebase()
                                    }
                                },
                                onRestoreFromFirebase = {
                                    scope.launch {
                                        dataRepository.restoreDataFromFirebase()
                                    }
                                },
                                onLogout = onLogout
                            )

                            is Screen.AccountViewPager -> {
                                val filteredAccounts = remember(accounts) {
                                    accounts.filter { it.type == screen.accountType }
                                }
                                val pagerState = rememberPagerState(
                                    initialPage = screen.initialIndex,
                                    pageCount = { filteredAccounts.size }
                                )
                                AccountViewPagerScreen(
                                    pagerState = pagerState,
                                    accounts = filteredAccounts,
                                    allTransactions = allTransactions,
                                    accountExtras = accountExtras,
                                    onUpdate = { accountId, updatedTransactions ->
                                        scope.launch {
                                            val newTransactions =
                                                allTransactions.toMutableMap().apply {
                                                    this[accountId] =
                                                        updatedTransactions.toMutableList()
                                                }
                                            dataRepository.saveTransactions(newTransactions)
                                        }
                                    },
                                    onUpdateExtras = { accountId, extras ->
                                        scope.launch {
                                            val updatedExtras = accountExtras.toMutableMap().apply {
                                                this[accountId] = extras
                                            }
                                            dataRepository.saveAccountExtras(updatedExtras)

                                            val updatedBalances = balances.toMutableMap().apply {
                                                this[accountId] = extras.provisionalBalance.toDoubleOrNull() ?: 0.0
                                            }
                                            dataRepository.saveBalances(updatedBalances)
                                        }
                                    },
                                    onBack = { currentScreen = Screen.PieChart }
                                )
                            }

                            is Screen.SimplifiedAccounts -> {
                                val filteredAccounts = remember(accounts) {
                                    accounts.filter { it.type == screen.accountType }
                                }
                                SimplifiedAccountsScreen(
                                    accounts = filteredAccounts,
                                    balances = balances,
                                    onUpdateBalance = { accountId, newBalance ->
                                        scope.launch {
                                            val updatedBalances = balances.toMutableMap().apply {
                                                this[accountId] = newBalance
                                            }
                                            dataRepository.saveBalances(updatedBalances)

                                            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val currentExtras = accountExtras[accountId] ?: AccountExtraInfo()
                                            val updatedExtras = accountExtras.toMutableMap().apply {
                                                this[accountId] = currentExtras.copy(balanceDate = today)
                                            }
                                            dataRepository.saveAccountExtras(updatedExtras)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
