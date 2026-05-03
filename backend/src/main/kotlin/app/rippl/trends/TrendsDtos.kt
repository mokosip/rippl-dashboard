package app.rippl.trends

import java.time.LocalDate

data class WeeklyTrend(val week: LocalDate, val domain: String, val totalSeconds: Long, val totalSaved: Int)
data class MonthlyTrend(val month: LocalDate, val domain: String, val totalSeconds: Long, val totalSaved: Int)
data class TimeSaved(val total: Int, val byDomain: Map<String, Int>, val byActivity: Map<String, Int>)
