package com.example.alappassignment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alappassignment.data.CsvIo
import com.example.alappassignment.data.DataSimulator
import com.example.alappassignment.data.InsightSummary
import com.example.alappassignment.data.SensorRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SensorViewModel(app: Application) : AndroidViewModel(app) {
	private val csvIo = CsvIo(app)
	private val simulator = DataSimulator(csvIo, viewModelScope, intervalMs = 600L)

	private val _regions = MutableStateFlow<List<String>>(emptyList())
	val regions: StateFlow<List<String>> = _regions.asStateFlow()

	private val _selectedRegion = MutableStateFlow<String?>(null)
	val selectedRegion: StateFlow<String?> = _selectedRegion.asStateFlow()

	// Keep last N points per region
	private val historyLimit = 150
	private val _historyByRegion = MutableStateFlow<Map<String, List<SensorRecord>>>(emptyMap())
	val historyByRegion: StateFlow<Map<String, List<SensorRecord>>> = _historyByRegion.asStateFlow()

	private val _insight = MutableStateFlow(InsightSummary(null, null, null, anomalyDetected = false, anomaliesCount = 0))
	val insight: StateFlow<InsightSummary> = _insight.asStateFlow()

	fun start() {
		simulator.start { record ->
			_historyByRegion.update { current ->
				val list = (current[record.region] ?: emptyList()) + record
				val trimmed = if (list.size > historyLimit) list.takeLast(historyLimit) else list
				val newMap = current.toMutableMap()
				newMap[record.region] = trimmed
				newMap
			}
			// regions list
			_regions.update { prev ->
				if (prev.contains(record.region)) prev else (prev + record.region).sorted()
			}
			if (_selectedRegion.value == null && record.region.isNotEmpty()) {
				_selectedRegion.value = record.region
			}
			recomputeInsights()
		}
	}

	fun selectRegion(region: String) {
		_selectedRegion.value = region
		recomputeInsights()
	}

	private fun recomputeInsights() {
		val region = _selectedRegion.value ?: return
		val series = _historyByRegion.value[region] ?: emptyList()
		val currentValue = series.lastOrNull()?.value
		val window = series.takeLast(30)
		val avg = if (window.isNotEmpty()) window.sumOf { it.value.toDouble() }.toFloat() / window.size else null
		val peak = series.maxByOrNull { it.value }?.value
		val anomalies = detectAnomalies(series)
		_insight.value = InsightSummary(currentValue, avg, peak, anomalyDetected = anomalies.isNotEmpty(), anomaliesCount = anomalies.size)
	}

	private fun detectAnomalies(series: List<SensorRecord>): List<SensorRecord> {
		if (series.size < 10) return emptyList()
		// Simple z-score like heuristic on rolling average deviation
		val tail = series.takeLast(50)
		val mean = tail.map { it.value }.average().toFloat()
		val variance = tail.map { val d = it.value - mean; d * d }.average().toFloat()
		val std = kotlin.math.sqrt(variance)
		if (std == 0f) return emptyList()
		val threshold = mean + 2.5f * std
		return tail.filter { it.value > threshold || it.value < (mean - 2.5f * std) }
	}

	fun getSeriesForSelected(): List<SensorRecord> {
		val region = _selectedRegion.value ?: return emptyList()
		return _historyByRegion.value[region] ?: emptyList()
	}

	// Chatbot answers based on current selected region
	fun answer(question: String): String {
		val q = question.trim().lowercase()
		val region = _selectedRegion.value
		val series = region?.let { _historyByRegion.value[it] } ?: emptyList()
		return when {
			q.contains("peak") || q.contains("max") -> {
				val max = series.maxByOrNull { it.value }
				if (max != null) "Peak reading: %.2f in %s".format(max.value, region) else "No data yet."
			}
			q.contains("anomal") || q.contains("alert") -> {
				val anomalies = detectAnomalies(series)
				if (anomalies.isNotEmpty()) "Anomalies detected: ${anomalies.size}" else "No anomalies detected."
			}
			q.contains("average") || q.contains("avg") || q.contains("mean") -> {
				val window = series.takeLast(30)
				if (window.isNotEmpty()) {
					val avg = window.sumOf { it.value.toDouble() } / window.size
					"Current rolling average: %.2f".format(avg)
				} else "No data yet."
			}
			else -> "I can answer: 'What was the peak reading?', 'Were there any anomalies?', 'What is the current average?'"
		}
	}
}
