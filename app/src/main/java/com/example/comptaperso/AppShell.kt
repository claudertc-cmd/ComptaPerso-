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
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * `AppShell` est le composant principal de l'interface utilisateur de l'application.
 * Il gère la navigation entre les écrans, la logique de restauration des données,
 * et la structure globale de l'application (barre supérieure, etc.).
 *
 * @param activity L'instance de `MainActivity` pour gérer les callbacks du cycle de vie (ex: bouton retour).
 * @param onLogout Callback pour gérer la déconnexion de l'utilisateur.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppShell(activity: MainActivity, onLogout: () -> Unit) {
    // Gère l'affichage de l'écran de démarrage (splash screen).
    var showSplashScreen by remember { mutableStateOf(true) }
    // Suit l'état de la restauration des données (en cours, succès, échec).
    var restoreState by remember { mutableStateOf(RestoreState.InProgress) }
    // Déclencheur pour relancer la restauration des données en cas d'échec.
    var retryTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    // `DataRepository` est la source de vérité unique pour les données de l'application.
    val dataRepository = remember { DataRepository(context) }
    // `CoroutineScope` pour lancer des opérations asynchrones (ex: accès réseau).
    val scope = rememberCoroutineScope()

    // `LaunchedEffect` pour la restauration des données. Se déclenche au démarrage et si `retryTrigger` change.
    LaunchedEffect(retryTrigger) {
        if (showSplashScreen) {
            restoreState = RestoreState.InProgress
            scope.launch {
                // Tente de restaurer les données depuis Firebase avec un timeout de 30 secondes.
                val result = withTimeoutOrNull(30_000L) {
                    try {
                        dataRepository.restoreDataFromFirebase()
                        RestoreState.Success
                    } catch (_: Exception) {
                        RestoreState.Failure
                    }
                }
                // Met à jour l'état de la restauration en fonction du résultat.
                restoreState = result ?: RestoreState.Timeout
            }
        }
    }

    // Une fois la restauration réussie, masque le splash screen après un court délai.
    LaunchedEffect(restoreState) {
        if (restoreState == RestoreState.Success) {
            delay(1500)
            showSplashScreen = false
        }
    }

    // Affiche le splash screen ou le contenu principal de l'application.
    if (showSplashScreen) {
        // Affiche l'écran de chargement avec différents états (en cours, échec, timeout).
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
                            // Affiche une vue vide pendant une courte période après le succès.
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
        // `currentScreen` gère l'écran actuellement affiché après le splash screen.
        var currentScreen by remember { mutableStateOf<Screen>(Screen.PieChart) }

        // Observe les flux de données depuis le `DataRepository`.
        val accounts by dataRepository.accounts.collectAsState(initial = emptyList())
        val allTransactions by dataRepository.transactions.collectAsState(initial = emptyMap())
        val balances by dataRepository.balances.collectAsState(initial = emptyMap())
        val accountExtras by dataRepository.accountExtras.collectAsState(initial = emptyMap())

        // Sauvegarde automatiquement les données sur Firebase dès qu'une modification est détectée.
        LaunchedEffect(accounts, allTransactions, balances, accountExtras) {
            // Ne sauvegarde pas l'état initial vide pour éviter d'écraser les données distantes.
            if (accounts.isNotEmpty() || allTransactions.isNotEmpty() || balances.isNotEmpty() || accountExtras.isNotEmpty()) {
                scope.launch {
                    dataRepository.saveDataToFirebase()
                }
            }
        }

        // Lanceur pour le sélecteur de fichiers (utilisé pour la restauration depuis un JSON).
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

        // Gère le bouton "retour" du système : revient à l'écran `PieChart`.
        activity.onBackPressedDispatcher.addCallback(owner = activity) {
            if (currentScreen !is Screen.PieChart) {
                currentScreen = Screen.PieChart
            }
        }

        // Logique de changement de thème centralisée.
        val theme = if (currentScreen is Screen.AccountViewPager) {
            val screen = currentScreen as Screen.AccountViewPager
            // Récupère le compte actuellement affiché dans le ViewPager.
            val account = accounts.filter { it.type == screen.accountType }[screen.initialIndex]
            // Applique un thème spécifique basé sur le nom du compte.
            when {
                account.name.contains("Boursobank", ignoreCase = true) -> Theme.BOURSOBANK
                account.name.contains("Fortuneo", ignoreCase = true) -> Theme.FORTUNEO
                else -> Theme.SPRING
            }
        } else {
            Theme.SPRING // Thème par défaut pour tous les autres écrans.
        }

        AppTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        // La barre supérieure est conditionnellement affichée.
                        if (currentScreen !is Screen.AccountViewPager &&
                            currentScreen !is Screen.Home &&
                            currentScreen !is Screen.PieChart // La TopAppBar n'est pas montrée sur certains écrans.
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
                                    // Affiche une icône de retour qui ramène à l'écran `PieChart`.
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
                        // Le `when` détermine quel écran afficher en fonction de `currentScreen`.
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
                                        val updatedAccounts = accounts + newAccount

                                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val updatedExtras: Map<String, AccountExtraInfo>
                                        val updatedBalances: Map<String, Double>
                                        val updatedTransactions: Map<String, List<com.example.comptaperso.data.Transaction>>

                                        if (type == "Bancaire") {
                                            updatedExtras = accountExtras.toMutableMap().apply {
                                                this[newAccount.id] = AccountExtraInfo(
                                                    provisionalBalance = "0.0",
                                                    deferredDebits = "0.0",
                                                    balanceDate = today
                                                )
                                            }
                                            updatedBalances = balances.toMutableMap().apply {
                                                this[newAccount.id] = 0.0
                                            }
                                            updatedTransactions = allTransactions.toMutableMap().apply {
                                                this[newAccount.id] = mutableListOf()
                                            }
                                        } else {
                                            updatedExtras = accountExtras.toMutableMap().apply {
                                                this[newAccount.id] = AccountExtraInfo(balanceDate = today)
                                            }
                                            updatedBalances = balances // Pas de changement pour les autres types de comptes.
                                            updatedTransactions = allTransactions // Pas de changement pour les autres types de comptes.
                                        }

                                        dataRepository.saveAllData(
                                            updatedAccounts,
                                            updatedTransactions,
                                            updatedBalances,
                                            updatedExtras
                                        )
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
                                        val updatedAccounts = accounts.filterNot { it.id == account.id }
                                        val updatedExtras = accountExtras.toMutableMap().apply {
                                            remove(account.id)
                                        }
                                        val updatedBalances = balances.toMutableMap().apply {
                                            remove(account.id)
                                        }
                                        val updatedTransactions = allTransactions.toMutableMap().apply {
                                            remove(account.id)
                                        }

                                        dataRepository.saveAllData(
                                            updatedAccounts,
                                            updatedTransactions,
                                            updatedBalances,
                                            updatedExtras
                                        )
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
                                        currentScreen = if (account.type == "Epargne" || account.type == "Assurance") {
                                            Screen.SimplifiedAccounts(accountType, initialIndex)
                                        } else {
                                            Screen.AccountViewPager(accountType, initialIndex)
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
                                    }
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
