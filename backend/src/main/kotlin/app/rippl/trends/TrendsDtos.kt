package app.rippl.trends

import java.time.LocalDate

data class WeeklyTrend(
    val week: LocalDate,
    val domain: String,
    val totalSeconds: Long,
    val totalSaved: Int,
    val confidence: String = "high"
)

data class MonthlyTrend(
    val month: LocalDate,
    val domain: String,
    val totalSeconds: Long,
    val totalSaved: Int,
    val confidence: String = "high"
)

data class TimeSaved(
    val total: Int,
    val confidence: String,
    val byDomain: Map<String, Int>,
    val byTaskMix: Map<String, Int>
)
