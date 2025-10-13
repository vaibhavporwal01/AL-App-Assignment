package com.example.alappassignment.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvIo(private val context: Context) {
	private val fileName = "annual_conc_by_monitor_2021.csv"

	data class DetectedColumns(
		val regionIndex: Int,
		val valueIndex: Int,
		val timeIndex: Int? // nullable, synthetic time if absent
	)

	// Heuristic to detect region (first non-numeric text col), value (first numeric col), time (if header matches)
	private fun detectColumns(header: List<String>, sampleRow: List<String>): DetectedColumns {
		val lowerHeaders = header.map { it.trim().lowercase() }
		val timeIdxByName = lowerHeaders.indexOfFirst {
			it.contains("time") || it.contains("date") || it.contains("timestamp")
		}.let { if (it >= 0) it else null }

		var regionIdx: Int? = null
		var valueIdx: Int? = null
		for (i in sampleRow.indices) {
			val cell = sampleRow[i].trim()
			val asNumber = cell.toFloatOrNull()
			if (asNumber != null && valueIdx == null) {
				valueIdx = i
			}
			if (asNumber == null && regionIdx == null && cell.isNotEmpty()) {
				regionIdx = i
			}
			if (regionIdx != null && valueIdx != null) break
		}
		return DetectedColumns(
			regionIndex = regionIdx ?: 0,
			valueIndex = valueIdx ?: 1.coerceAtMost(sampleRow.lastIndex),
			timeIndex = timeIdxByName
		)
	}

	fun openBufferedReader(): BufferedReader? {
		return try {
			val input = context.assets.open(fileName)
			BufferedReader(InputStreamReader(input))
		} catch (_: Exception) {
			null
		}
	}

	// Suspends while reading and invokes onRecord sequentially for each row
	suspend fun readStream(onRecord: suspend (SensorRecord) -> Unit) {
		val reader = openBufferedReader() ?: return
		reader.use { br ->
			val headerLine = br.readLine() ?: return
			val header = headerLine.split(',')
			// Peek first data row to detect columns
			val firstDataLine = br.readLine() ?: return
			val sample = firstDataLine.split(',')
			val cols = detectColumns(header, sample)

			var indexTime = 0L
			parseRow(sample, cols, indexTime)?.let { onRecord(it) }
			indexTime++
			while (true) {
				val line = br.readLine() ?: break
				val parts = line.split(',')
				parseRow(parts, cols, indexTime)?.let { onRecord(it) }
				indexTime++
			}
		}
	}

	private fun parseRow(parts: List<String>, cols: DetectedColumns, indexTime: Long): SensorRecord? {
		if (parts.isEmpty()) return null
		val region = parts.getOrNull(cols.regionIndex)?.trim().orEmpty()
		val valueStr = parts.getOrNull(cols.valueIndex)?.trim().orEmpty()
		val value = valueStr.toFloatOrNull() ?: return null
		val ts = cols.timeIndex?.let { parts.getOrNull(it)?.trim() }
		return SensorRecord(indexTime = indexTime, region = region, value = value, rawTimestamp = ts)
	}
}
