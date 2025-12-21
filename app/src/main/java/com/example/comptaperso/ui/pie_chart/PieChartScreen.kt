package com.example.comptaperso.ui.pie_chart

import android.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comptaperso.ui.components.RollingInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PieChartScreen(
    viewModel: PieChartViewModel = viewModel(),
    onAccountClick: (String) -> Unit
) {
    val groupedData by viewModel.groupedData.collectAsState()

    if (groupedData.isNotEmpty()) {
        val totalValue = remember(groupedData) { groupedData.sumOf { it.totalValue.toDouble() }.toFloat() }
        val colorPalettes = remember { generateColorPalettes() }

        val accountColors = remember(groupedData, colorPalettes) {
            groupedData.flatMapIndexed { groupIndex, group ->
                val palette = colorPalettes[groupIndex % colorPalettes.size]
                group.accounts.mapIndexed { accountIndex, _ ->
                    palette[accountIndex % palette.size]
                }
            }
        }

        val groupColors = remember(groupedData, colorPalettes) {
            groupedData.mapIndexed { index, _ ->
                colorPalettes[index % colorPalettes.size].first()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            DonutChart(
                modifier = Modifier.size(220.dp),
                groupedData = groupedData,
                totalValue = totalValue,
                colors = accountColors,
                onAccountClick = onAccountClick
            )
            Spacer(Modifier.height(56.dp))
            ChartLegend(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                groupedData = groupedData,
                groupColors = groupColors,
                accountColors = accountColors,
                onAccountClick = onAccountClick
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Aucune donnée à afficher",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private data class ArcLayoutInfo(
    val accountId: String,
    val startAngle: Float,
    val sweepAngle: Float,
    val groupName: String
)

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
    val accountGapAngle = 0.5f
    val categoryGapAngle = 6f
    val explosion = 10.dp

    val arcLayouts = remember { mutableListOf<ArcLayoutInfo>() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(groupedData) { // Re-initialize when data changes
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
            }
        ) {
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
        //
        RollingInt(value= totalValue.toInt())
//        Text(
//            text = "%.0f€".format(totalValue),
//            style = MaterialTheme.typography.headlineSmall,
//            fontWeight = FontWeight.Bold
//        )
    }
}

@Composable
private fun ChartLegend(
    modifier: Modifier = Modifier,
    groupedData: List<GroupedChartData>,
    groupColors: List<Color>,
    accountColors: List<Color>,
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
                        style = MaterialTheme.typography.titleLarge , //titre
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
                    Text(
                        text = accountData.label,
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

private fun generateColorPalettes(): List<List<Color>> {
    return listOf(
        // Blues
        listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFBBDEFB)),
        // Oranges
        listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFFFE0B2)),
        // Greens
        listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFC8E6C9)),
        // Purples
        listOf(Color(0xFF311B92), Color(0xFF512DA8), Color(0xFF673AB7), Color(0xFF9575CD), Color(0xFFD1C4E9)),
        // Teals
        listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF009688),  Color(0xFF4DB6AC), Color(0xFFB2DFDB)),
        // Reds
        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFF44336), Color(0xFFE57373), Color(0xFFFFCDD2))
    ) 
}
