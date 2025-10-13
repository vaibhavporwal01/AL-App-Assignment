package com.example.alappassignment.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alappassignment.SensorViewModel
import com.example.alappassignment.data.SensorRecord

@Composable
fun DashboardScreen(vm: SensorViewModel) {
	val regions by vm.regions.collectAsState()
	val selected by vm.selectedRegion.collectAsState()
	val insight by vm.insight.collectAsState()
	val series = vm.getSeriesForSelected()

	Column(Modifier.fillMaxSize().padding(12.dp)) {
		Text("AI-IoT Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
		Spacer(Modifier.height(8.dp))

		RegionSelector(regions = regions, selected = selected, onSelect = { vm.selectRegion(it) })
		Spacer(Modifier.height(8.dp))

		Chart(
			points = series,
			avg = insight.rollingAverage,
			anomaly = insight.anomalyDetected
		)
		Spacer(Modifier.height(8.dp))

		Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
			StatBox("Current", insight.currentValue?.let { String.format("%.2f", it) } ?: "-")
			StatBox("Average", insight.rollingAverage?.let { String.format("%.2f", it) } ?: "-")
			StatBox("Peak", insight.peakValue?.let { String.format("%.2f", it) } ?: "-")
			StatBox("Anoms", if (insight.anomalyDetected) insight.anomaliesCount.toString() else "0")
		}
		Spacer(Modifier.height(8.dp))

		Text("Recent Values", style = MaterialTheme.typography.titleMedium)
		LazyColumn(Modifier.weight(1f)) {
			items(series.takeLast(30).asReversed()) { rec ->
				Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
					Text(rec.region)
					Text(String.format("%.2f", rec.value))
				}
			}
		}

		Spacer(Modifier.height(8.dp))
		Chatbot(vm)
	}
}

@Composable
private fun RegionSelector(regions: List<String>, selected: String?, onSelect: (String) -> Unit) {
	if (regions.isEmpty()) {
		Text("Waiting for data (place CSV in assets)...", color = Color.Gray)
		return
	}
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text("Region:", modifier = Modifier.padding(end = 8.dp))
		var expanded by remember { mutableStateOf(false) }
		Box {
			Button(onClick = { expanded = true }) { Text(selected ?: "Select") }
			DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
				regions.forEach { r ->
					DropdownMenuItem(text = { Text(r) }, onClick = { expanded = false; onSelect(r) })
				}
			}
		}
	}
}

@Composable
private fun Chart(points: List<SensorRecord>, avg: Float?, anomaly: Boolean) {
	val lineColor = if (anomaly) Color(0xFFB00020) else Color(0xFF1E88E5)
	val avgColor = Color(0xFF43A047)
	Box(
		Modifier
			.fillMaxWidth()
			.height(180.dp)
			.background(Color(0xFFF3F3F3))
			.padding(8.dp)
	) {
		Canvas(Modifier.fillMaxSize()) {
			if (points.size < 2) return@Canvas
			val minVal = points.minOf { it.value }
			val maxVal = points.maxOf { it.value }
			val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f
			val stepX = size.width / (points.lastIndex.coerceAtLeast(1))

			val path = Path()
			points.forEachIndexed { i, r ->
				val x = i * stepX
				val y = size.height - ((r.value - minVal) / range) * size.height
				if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
			}
			drawPath(path, color = lineColor)

			if (avg != null) {
				val yAvg = size.height - ((avg - minVal) / range) * size.height
				drawLine(avgColor, start = androidx.compose.ui.geometry.Offset(0f, yAvg), end = androidx.compose.ui.geometry.Offset(size.width, yAvg), strokeWidth = 2f)
			}
		}
	}
}

@Composable
private fun StatBox(label: String, value: String) {
	Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
		Text(label, color = Color.Gray)
		Text(value, fontWeight = FontWeight.Bold)
	}
}

@Composable
private fun Chatbot(vm: SensorViewModel) {
	var input by remember { mutableStateOf("") }
	var answer by remember { mutableStateOf("") }

	Column(Modifier.fillMaxWidth()) {
		Text("Chatbot", style = MaterialTheme.typography.titleMedium)
		Row(verticalAlignment = Alignment.CenterVertically) {
			OutlinedTextField(
				value = input,
				onValueChange = { input = it },
				modifier = Modifier.weight(1f),
				placeholder = { Text("Ask: peak? anomalies? average?") }
			)
			Spacer(Modifier.width(8.dp))
			Button(onClick = { answer = vm.answer(input) }) { Text("Ask") }
		}
		if (answer.isNotEmpty()) {
			Text(answer, modifier = Modifier.padding(top = 6.dp))
		}
	}
}
