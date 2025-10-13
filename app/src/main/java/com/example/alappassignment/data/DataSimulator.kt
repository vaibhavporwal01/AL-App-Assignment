package com.example.alappassignment.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataSimulator(
	private val csvIo: CsvIo,
	private val scope: CoroutineScope,
	private val intervalMs: Long = 600L,
	private val sampleStride: Int = 5 // read every Nth row to reduce load
) {
	fun start(onRecord: (SensorRecord) -> Unit) {
		scope.launch {
			withContext(Dispatchers.IO) {
				var i = 0
				csvIo.readStream { record ->
					if (!isActive) return@readStream
					// sampling: only emit every Nth record
					if (i % sampleStride == 0) {
						withContext(Dispatchers.Main) {
							onRecord(record)
						}
						delay(intervalMs)
					}
					i++
				}
			}
		}
	}
}
