package com.example.comptaperso.ui.pie_chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    val chartData by viewModel.chartData.collectAsState()

    if (chartData.isNotEmpty()) {
        val totalValue = chartData.sumOf { it.value.toDouble() }.toFloat()
        val colors = remember(chartData.size) { generateColors(chartData.size) }

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
                data = chartData,
                totalValue = totalValue,
                colors = colors
            )
            Spacer(Modifier.height(32.dp))
            ChartLegend(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                data = chartData,
                colors = colors
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            data.forEachIndexed { index, chartData ->
                val sweepAngle = (chartData.value / totalValue) * 360f
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
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
    data: List<ChartData>,
    colors: List<Color>
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(data) { index, chartData ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(colors[index], CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = chartData.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "%.0f€".format(chartData.value),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun generateColors(count: Int): List<Color> {
    val predefinedColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFF3700B3),
        Color(0xFF018786), Color(0xFFB00020), Color(0xFFD50000),
        Color(0xFF2962FF), Color(0xFFC51162)
    )
    return List(count) { i -> predefinedColors[i % predefinedColors.size] }
}