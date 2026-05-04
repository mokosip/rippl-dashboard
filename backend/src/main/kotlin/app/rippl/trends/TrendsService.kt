package app.rippl.trends

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TrendsService(private val jdbc: JdbcTemplate) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> {
        log.debug("Querying weekly trends for userId: {}, from: {}, to: {}", userId, from, to)
        val results = jdbc.query(
            """
            SELECT date_trunc('week', date)::date AS week, domain,
                   SUM(active_seconds)::bigint AS total_seconds,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS total_saved
            FROM sessions
            WHERE user_id = ? AND date >= ? AND date <= ?
            GROUP BY week, domain
            ORDER BY week
            """,
            { rs, _ ->
                WeeklyTrend(
                    rs.getDate("week").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("total_seconds"),
                    rs.getInt("total_saved")
                )
            },
            userId, from, to
        )
        log.debug("Weekly trends result count: {}", results.size)
        return results
    }

    fun monthly(userId: UUID, from: LocalDate, to: LocalDate): List<MonthlyTrend> {
        log.debug("Querying monthly trends for userId: {}, from: {}, to: {}", userId, from, to)
        val results = jdbc.query(
            """
            SELECT date_trunc('month', date)::date AS month, domain,
                   SUM(active_seconds)::bigint AS total_seconds,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS total_saved
            FROM sessions
            WHERE user_id = ? AND date >= ? AND date <= ?
            GROUP BY month, domain
            ORDER BY month
            """,
            { rs, _ ->
                MonthlyTrend(
                    rs.getDate("month").toLocalDate(),
                    rs.getString("domain"),
                    rs.getLong("total_seconds"),
                    rs.getInt("total_saved")
                )
            },
            userId, from, to
        )
        log.debug("Monthly trends result count: {}", results.size)
        return results
    }

    fun timeSaved(userId: UUID): TimeSaved {
        log.debug("Querying time-saved for userId: {}", userId)
        val total = jdbc.queryForObject(
            "SELECT COALESCE(SUM(time_saved_minutes), 0) FROM sessions WHERE user_id = ?",
            Int::class.java, userId
        ) ?: 0

        val byDomain = jdbc.query(
            """
            SELECT domain, COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND time_saved_minutes IS NOT NULL
            GROUP BY domain ORDER BY saved DESC
            """,
            { rs, _ -> rs.getString("domain") to rs.getInt("saved") },
            userId
        ).toMap()

        val byActivity = jdbc.query(
            """
            SELECT unnest(string_to_array(activity_type, ', ')) AS activity,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND activity_type IS NOT NULL AND time_saved_minutes IS NOT NULL
            GROUP BY activity ORDER BY saved DESC
            """,
            { rs, _ -> rs.getString("activity") to rs.getInt("saved") },
            userId
        ).toMap()

        log.debug("Time-saved for userId: {} — total: {}min, domains: {}, activities: {}", userId, total, byDomain.size, byActivity.size)
        return TimeSaved(total, byDomain, byActivity)
    }

    fun activityHeatmap(userId: UUID): List<List<Int>> {
        val rows = jdbc.query(
            """
            SELECT EXTRACT(DOW FROM date)::int AS dow,
                   EXTRACT(HOUR FROM to_timestamp(started_at / 1000))::int AS hour,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND time_saved_minutes IS NOT NULL
            GROUP BY dow, hour
            """,
            { rs, _ -> Triple(rs.getInt("dow"), rs.getInt("hour"), rs.getInt("saved")) },
            userId
        )
        // PostgreSQL DOW: 0=Sunday. Remap to 0=Monday
        val grid = Array(7) { IntArray(24) }
        for ((dow, hour, saved) in rows) {
            val mondayBased = if (dow == 0) 6 else dow - 1
            grid[mondayBased][hour] = saved
        }
        return grid.map { it.toList() }
    }
}
