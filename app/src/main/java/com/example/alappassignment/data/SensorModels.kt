package com.example.alappassignment.data

// Represents a single sensor reading parsed from CSV
data class SensorRecord(
    val indexTime: Long, // synthetic monotonically increasing time index
    val region: String,
    val value: Float,
    val rawTimestamp: String? = null
)

// Windowed statistics for basic AI insights
data class InsightSummary(
    val currentValue: Float?,
    val rollingAverage: Float?,
    val peakValue: Float?,
    val anomalyDetected: Boolean,
    val anomaliesCount: Int
)
