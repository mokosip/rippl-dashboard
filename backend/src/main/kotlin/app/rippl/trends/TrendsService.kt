package app.rippl.trends

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TrendsService(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun weightedConfidence(rows: List<Pair<String, Int>>): String {
        val levelMap = mapOf("low" to 1, "medium" to 2, "high" to 3)
        var weightedSum = 0L
        var totalWeight = 0L
        for ((conf, saved) in rows) {
            val level = levelMap[conf] ?: 2
            weightedSum += level.toLong() * saved
            totalWeight += saved
        }
        if (totalWeight == 0L) return "low"
        val avg = weightedSum.toDouble() / totalWeight
        return when {
            avg >= 2.5 -> "high"
            avg >= 1.5 -> "medium"
            else -> "low"
        }
    }

    fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> {
        log.debug("Querying weekly trends for userId: {}, from: {}, to: {}", userId, from, to)

        data class RawRow(val week: LocalDate, val domain: String, val activeMs: Long, val savedMs: Long, val confidence: String?)

        val rows = jdbc.query(
            """
            SELECT date_trunc('week', a.started_at)::date AS week,
                   a.domain,
                   a.active_ms,
                   COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
                   s.confidence::text AS confidence
            FROM activity_sessions a
            LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
            WHERE a.user_id = ? AND a.started_at::date >= ? AND a.started_at::date <= ?
            ORDER BY week
            """,
            { rs, _ ->
                RawRow(
                    rs.getDate("week").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("active_ms"),
                    rs.getLong("saved_ms"),
                    rs.getString("confidence")
                )
            },
            userId, from, to
        )

        return rows.groupBy { it.week to it.domain }.map { (key, group) ->
            val (week, domain) = key
            val totalSeconds = group.sumOf { it.activeMs } / 1000
            val totalSaved = (group.sumOf { it.savedMs } / 60000).toInt()
            val confPairs = group.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
            WeeklyTrend(week, domain, totalSeconds, totalSaved, weightedConfidence(confPairs))
        }
    }

    fun monthly(userId: UUID, from: LocalDate, to: LocalDate): List<MonthlyTrend> {
        log.debug("Querying monthly trends for userId: {}, from: {}, to: {}", userId, from, to)

        data class RawRow(val month: LocalDate, val domain: String, val activeMs: Long, val savedMs: Long, val confidence: String?)

        val rows = jdbc.query(
            """
            SELECT date_trunc('month', a.started_at)::date AS month,
                   a.domain,
                   a.active_ms,
                   COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
                   s.confidence::text AS confidence
            FROM activity_sessions a
            LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
            WHERE a.user_id = ? AND a.started_at::date >= ? AND a.started_at::date <= ?
            ORDER BY month
            """,
            { rs, _ ->
                RawRow(
                    rs.getDate("month").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("active_ms"),
                    rs.getLong("saved_ms"),
                    rs.getString("confidence")
                )
            },
            userId, from, to
        )

        return rows.groupBy { it.month to it.domain }.map { (key, group) ->
            val (month, domain) = key
            val totalSeconds = group.sumOf { it.activeMs } / 1000
            val totalSaved = (group.sumOf { it.savedMs } / 60000).toInt()
            val confPairs = group.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
            MonthlyTrend(month, domain, totalSeconds, totalSaved, weightedConfidence(confPairs))
        }
    }

    fun timeSaved(userId: UUID): TimeSaved {
        log.debug("Querying time-saved for userId: {}", userId)

        data class SessionRow(val domain: String, val savedMs: Long, val confidence: String?, val taskMixJson: String?)

        val rows = jdbc.query(
            """
            SELECT a.domain,
                   COALESCE(s.estimated_time_saved_ms, 0) AS saved_ms,
                   s.confidence::text AS confidence,
                   s.inferred_task_mix::text AS task_mix_json
            FROM activity_sessions a
            LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
            WHERE a.user_id = ?
            """,
            { rs, _ ->
                SessionRow(
                    rs.getString("domain"),
                    rs.getLong("saved_ms"),
                    rs.getString("confidence"),
                    rs.getString("task_mix_json")
                )
            },
            userId
        )

        val totalSavedMin = (rows.sumOf { it.savedMs } / 60000).toInt()

        val byDomain = rows.groupBy { it.domain }
            .mapValues { (_, group) -> (group.sumOf { it.savedMs } / 60000).toInt() }
            .filterValues { it > 0 }
            .toSortedMap(compareByDescending { key -> rows.filter { it.domain == key }.sumOf { it.savedMs } })

        // Weighted task mix: proportion × time_saved per task key
        val taskTotals = mutableMapOf<String, Long>()
        for (row in rows) {
            if (row.taskMixJson == null || row.savedMs <= 0) continue
            try {
                @Suppress("UNCHECKED_CAST")
                val mix = objectMapper.readValue(
                    row.taskMixJson, Map::class.java
                ) as Map<String, Number>
                for ((task, proportion) in mix) {
                    val contribution = (proportion.toDouble() * row.savedMs).toLong()
                    taskTotals[task] = (taskTotals[task] ?: 0L) + contribution
                }
            } catch (_: Exception) { }
        }
        val byTaskMix = taskTotals
            .mapValues { (it.value / 60000).toInt() }
            .filterValues { it > 0 }
            .toSortedMap(compareByDescending { taskTotals[it] })

        val confPairs = rows.mapNotNull { r -> r.confidence?.let { it to (r.savedMs / 60000).toInt() } }
        val confidence = weightedConfidence(confPairs)

        log.debug("Time-saved for userId: {} — total: {}min, domains: {}, tasks: {}, confidence: {}",
            userId, totalSavedMin, byDomain.size, byTaskMix.size, confidence)
        return TimeSaved(totalSavedMin, confidence, byDomain, byTaskMix)
    }

    fun activityHeatmap(userId: UUID): List<List<Int>> {
        val rows = jdbc.query(
            """
            SELECT EXTRACT(DOW FROM a.started_at)::int AS dow,
                   EXTRACT(HOUR FROM a.started_at)::int AS hour,
                   (COALESCE(SUM(s.estimated_time_saved_ms), 0) / 60000)::int AS saved
            FROM activity_sessions a
            LEFT JOIN scored_sessions s ON s.activity_session_id = a.id
            WHERE a.user_id = ?
            GROUP BY dow, hour
            """,
            { rs, _ -> Triple(rs.getInt("dow"), rs.getInt("hour"), rs.getInt("saved")) },
            userId
        )
        val grid = Array(7) { IntArray(24) }
        for ((dow, hour, saved) in rows) {
            val mondayBased = if (dow == 0) 6 else dow - 1
            grid[mondayBased][hour] = saved
        }
        return grid.map { it.toList() }
    }
}
