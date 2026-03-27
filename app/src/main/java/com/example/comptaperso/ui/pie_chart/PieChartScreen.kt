package com.example.comptaperso.ui.pie_chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comptaperso.data.Account
import com.example.comptaperso.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import com.example.comptaperso.ui.components.RollingInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * `PieChartScreen` est l'écran principal qui affiche un graphique en anneau (donut chart)
 * représentant la répartition des finances de l'utilisateur par compte et par catégorie.
 * Il inclut également une légende détaillée et un menu pour des actions supplémentaires.
 *
 * @param viewModel Le ViewModel qui fournit les données pour le graphique.
 * @param onAccountClick Callback déclenché lorsqu'un compte est cliqué (sur le graphique ou la légende).
 * @param onNavigate Callback pour la navigation vers d'autres écrans.
 * @param onSaveToJson Callback pour sauvegarder les données localement.
 * @param onRestoreFromJson Callback pour restaurer les données localement.
 * @param onSaveToFirebase Callback pour sauvegarder les données sur le cloud.
 * @param onRestoreFromFirebase Callback pour restaurer les données depuis le cloud.
 * @param onLogout Callback pour déconnecter l'utilisateur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PieChartScreen(
    viewModel: PieChartViewModel = viewModel(),
    accounts: List<Account> = emptyList(),
    onAccountClick: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
    onSaveToJson: () -> Unit,
    onRestoreFromJson: () -> Unit,
    onSaveToFirebase: () -> Unit,
    onRestoreFromFirebase: () -> Unit,
    onLogout: () -> Unit
) {
    val groupedData by viewModel.groupedData.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var advancedMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (groupedData.isNotEmpty()) {
            val totalValue = remember(groupedData) { groupedData.sumOf { it.totalValue.toDouble() }.toFloat() }
            val colorPalettes = remember { generateColorPalettes() }

            // Assigne une couleur à chaque compte en utilisant les palettes de couleurs.
            val accountColors = remember(groupedData, colorPalettes) {
                groupedData.flatMapIndexed { groupIndex, group ->
                    val palette = colorPalettes[groupIndex % colorPalettes.size]
                    group.accounts.mapIndexed { accountIndex, _ ->
                        palette[accountIndex % palette.size]
                    }
                }
            }

            // Assigne une couleur à chaque groupe (catégorie) de comptes.
            val groupColors = remember(groupedData, colorPalettes) {
                groupedData.mapIndexed { index, _ ->
                    colorPalettes[index % colorPalettes.size].first()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp)) // Espace pour le menu.
                DonutChart(
                    modifier = Modifier.size(220.dp),
                    groupedData = groupedData,
                    totalValue = totalValue,
                    colors = accountColors,
                    onAccountClick = onAccountClick
                )
                Spacer(Modifier.height(56.dp))
                ChartLegend(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    groupedData = groupedData,
                    groupColors = groupColors,
                    accountColors = accountColors,
                    accounts = accounts,
                    onAccountClick = onAccountClick
                )
            }
        } else {
            // Affiche un message si aucune donnée n'est disponible.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune donnée à afficher", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Menu déroulant pour les actions.
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, "Menu")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Gérer les comptes") }, onClick = { onNavigate(Screen.AccountManagement); menuExpanded = false })
                HorizontalDivider()
                // Sous-menu pour les options avancées.
                Box {
                    DropdownMenuItem(text = { Text("Avancé...") }, onClick = { advancedMenuExpanded = true })
                    DropdownMenu(expanded = advancedMenuExpanded, onDismissRequest = { advancedMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Sauvegarde locale") }, onClick = { onSaveToJson(); advancedMenuExpanded = false; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Restauration locale") }, onClick = { onRestoreFromJson(); advancedMenuExpanded = false; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Sauvegarde Cloud") }, onClick = { onSaveToFirebase(); advancedMenuExpanded = false; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Restauration Cloud") }, onClick = { onRestoreFromFirebase(); advancedMenuExpanded = false; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Se déconnecter") }, onClick = { onLogout(); advancedMenuExpanded = false; menuExpanded = false })
                    }
                }
            }
        }
    }
}

// Classe de données pour stocker les informations de layout de chaque arc du graphique.
private data class ArcLayoutInfo(val accountId: String, val startAngle: Float, val sweepAngle: Float, val groupName: String)

/**
 * Affiche le graphique en anneau.
 */
@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    groupedData: List<GroupedChartData>,
    totalValue: Float,
    colors: List<Color>,
    onAccountClick: (String) -> Unit
) {
    var startAngle = -80f
    val strokeWidth = 80.dp
    val accountGapAngle = 0.5f // Espace entre les comptes.
    val categoryGapAngle = 6f // Espace entre les catégories.
    val explosion = 10.dp // Décalage pour la catégorie "Bancaire".

    val arcLayouts = remember { mutableListOf<ArcLayoutInfo>() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(groupedData) { // Re-initialize when data changes
            detectTapGestures { tapOffset ->
                val strokeWidthPx = strokeWidth.toPx()
                val chartRadius = min(size.width, size.height) / 2f
                val explosionPx = explosion.toPx()
                val canvasCenter = Offset(size.width / 2f, size.height / 2f)

                val clickedArc = arcLayouts.lastOrNull { layout ->
                    val groupExplosion = if (layout.groupName == "Bancaire") explosionPx else 0f

                    val middleAngleRad =
                        Math.toRadians((layout.startAngle + layout.sweepAngle / 2).toDouble())
                    val offsetX = (groupExplosion * cos(middleAngleRad)).toFloat()
                    val offsetY = (groupExplosion * sin(middleAngleRad)).toFloat()

                    val arcCenter = canvasCenter + Offset(offsetX, offsetY)
                    val translatedTap = tapOffset - arcCenter

                    val radius = translatedTap.getDistance()
                    val isRadiusInDonut = radius in (chartRadius - strokeWidthPx)..chartRadius
                    if (!isRadiusInDonut) return@lastOrNull false

                    var tapAngle =
                        Math
                            .toDegrees(atan2(translatedTap.y.toDouble(), translatedTap.x.toDouble()))
                            .toFloat()
                    if (tapAngle < 0) tapAngle += 360f

                    val normalizedStartAngle = layout.startAngle.mod(360f).let { if (it < 0f) it + 360f else it }

                    var angleInArc = tapAngle - normalizedStartAngle
                    if (angleInArc < 0) angleInArc += 360f

                    angleInArc <= layout.sweepAngle
                }

                clickedArc?.let { onAccountClick(it.accountId) }
            }
        }) {
            arcLayouts.clear()
            if (totalValue > 0f) {
                val numAccounts = groupedData.sumOf { it.accounts.size }
                val numCategories = groupedData.count { it.accounts.isNotEmpty() }

                val totalGapsAngle = (numAccounts - numCategories) * accountGapAngle + numCategories * categoryGapAngle
                val angleForData = 360f - totalGapsAngle

                var colorIndex = 0
                groupedData.forEach { group ->
                    val isBancaire = group.groupName == "Bancaire"
                    val groupExplosion = if (isBancaire) explosion.toPx() else 0f

                    if (group.accounts.isNotEmpty()) {
                        group.accounts.forEachIndexed { accountIndex, account ->
                            val sweepAngle = (account.value / totalValue) * angleForData

                            arcLayouts.add(ArcLayoutInfo(account.id, startAngle, sweepAngle, group.groupName))

                            val angleInRadians = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                            val offsetX = (groupExplosion * cos(angleInRadians)).toFloat()
                            val offsetY = (groupExplosion * sin(angleInRadians)).toFloat()

                            translate(left = offsetX, top = offsetY) {
                                drawArc(
                                    color = colors[colorIndex],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                                )
                            }
                            colorIndex++

                            val isLastAccountInGroup = accountIndex == group.accounts.lastIndex
                            val gap = if (isLastAccountInGroup) categoryGapAngle else accountGapAngle
                            startAngle += sweepAngle + gap
                        }
                    }
                }
            }
        }
        // Affiche le total au centre du graphique.
        RollingInt(
            value = totalValue.toInt(),
            fontSize = 26.sp,
            showBackground = false,
            showDigitFrames = false,
            showEuroSymbol = true
        )
    }
}

/**
 * Affiche la légende du graphique, listant les catégories et les comptes.
 */
@Composable
private fun ChartLegend(
    modifier: Modifier = Modifier,
    groupedData: List<GroupedChartData>,
    groupColors: List<Color>,
    accountColors: List<Color>,
    accounts: List<Account> = emptyList(),
    onAccountClick: (String) -> Unit
) {
    val groupAccountColorOffsets = remember(groupedData) {
        mutableListOf<Int>().apply {
            var currentOffset = 0
            groupedData.forEach {
                add(currentOffset)
                currentOffset += it.accounts.size
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedData.forEachIndexed { groupIndex, group ->
            // Affiche le titre du groupe (catégorie).
            item(key = "group_${group.groupName}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(groupColors[groupIndex], CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = group.groupName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge, //titre
                        fontWeight = FontWeight.Bold,
                        color = groupColors[groupIndex]
                    )
                    Text(
                        text = "%.0f€".format(group.totalValue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = groupColors[groupIndex]

                    )
                }
            }

            val accountColorOffset = groupAccountColorOffsets.getOrElse(groupIndex) { 0 }

            // Affiche les comptes individuels de la catégorie.
            itemsIndexed(
                items = group.accounts,
                key = { accountIndex, account -> "account_${group.groupName}_${account.label}_$accountIndex" }
            ) { accountIndex, accountData ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onAccountClick(accountData.id) } // Indent to align smaller dot
                ) {
                    val color = accountColors.getOrElse(accountColorOffset + accountIndex) { Color.Transparent }
                    Box(
                        modifier = Modifier
                            .size(12.dp) // Smaller dot
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(16.dp)) // Spacer to align text with category text
                    val balanceDate = accounts
                        .firstOrNull { it.id == accountData.id && it.type == "Bancaire" }
                        ?.extraInfo?.balanceDate
                    val displayLabel = if (!balanceDate.isNullOrBlank()) {
                        "${accountData.label} (${getRelativeDateLabel(balanceDate)})"
                    } else {
                        accountData.label
                    }
                    Text(
                        text = displayLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "%.0f€".format(accountData.value),
                        style = MaterialTheme.typography.titleMedium,
//                        color= color
                    )
                }
            }
        }
    }
}

private fun getRelativeDateLabel(isoDate: String): String {
    return runCatching {
        val date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val days = ChronoUnit.DAYS.between(date, LocalDate.now())
        when {
            days <= 0L -> "aujourd'hui"
            days == 1L -> "hier"
            days < 7L -> "il y a $days jours"
            days == 7L -> "il y a une semaine"
            days < 14L -> "il y a plus d'une semaine"
            else -> "il y a ${days / 7} semaines"
        }
    }.getOrDefault(isoDate)
}

private fun generateColorPalettes(): List<List<Color>> {
    return listOf(
        // Greens
        listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFC8E6C9)),
        // Blues
        listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFBBDEFB)),
        // Oranges
        listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFFFE0B2)),

        // Purples
        listOf(Color(0xFF311B92), Color(0xFF512DA8), Color(0xFF673AB7), Color(0xFF9575CD), Color(0xFFD1C4E9)),
        // Teals
        listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF009688),  Color(0xFF4DB6AC), Color(0xFFB2DFDB)),
        // Reds
        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFF44336), Color(0xFFE57373), Color(0xFFFFCDD2))
    )
}
