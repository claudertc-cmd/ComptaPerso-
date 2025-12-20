package com.example.comptaperso.ui.pie_chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PieChartScreen(
    viewModel: PieChartViewModel = viewModel()
) {
    val groupedData by viewModel.groupedData.collectAsState()

    if (groupedData.isNotEmpty()) {
        val totalValue = remember(groupedData) { groupedData.sumOf { it.totalValue.toDouble() }.toFloat() }
        val colorPalettes = remember { generateColorPalettes() }

        val accountsChartData = remember(groupedData) { groupedData.flatMap { it.accounts } }

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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Répartition des Actifs",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            DonutChart(
                modifier = Modifier.size(220.dp),
                data = accountsChartData,
                totalValue = totalValue,
                colors = accountColors
            )
            Spacer(Modifier.height(32.dp))
            ChartLegend(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                groupedData = groupedData,
                colors = groupColors
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

@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    data: List<ChartData>,
    totalValue: Float,
    colors: List<Color>
) {
    var startAngle = -90f
    val strokeWidth = 50.dp
    val gapAngle = 1f // Espace entre les segments

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalAngleToDraw = if (totalValue > 0) 360f - data.size * gapAngle else 360f
            data.forEachIndexed { index, chartData ->
                if (chartData.value > 0) {
                    val sweepAngle = (chartData.value / totalValue) * totalAngleToDraw
                    drawArc(
                        color = colors[index],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle + gapAngle
                }
            }
        }
        Text(
            text = "%.0f€".format(totalValue),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChartLegend(
    modifier: Modifier = Modifier,
    groupedData: List<GroupedChartData>,
    colors: List<Color>
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedData.forEachIndexed { groupIndex, group ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colors[groupIndex], CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = group.groupName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.0f€".format(group.totalValue),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(group.accounts) { accountData ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 32.dp)) {
                    Text(
                        text = accountData.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "%.0f€".format(accountData.value),
                        style = MaterialTheme.typography.bodyMedium
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
        listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF009688), Color(0xFF4DB6AC), Color(0xFFB2DFDB)),
        // Reds
        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFFF44336), Color(0xFFE57373), Color(0xFFFFCDD2))
    )
}
