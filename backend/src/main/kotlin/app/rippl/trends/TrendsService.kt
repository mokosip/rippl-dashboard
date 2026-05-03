package app.rippl.trends

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TrendsService(private val jdbc: JdbcTemplate) {

    fun weekly(userId: UUID, from: LocalDate, to: LocalDate): List<WeeklyTrend> =
        jdbc.query(
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

    fun monthly(userId: UUID, from: LocalDate, to: LocalDate): List<MonthlyTrend> =
        jdbc.query(
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

    fun timeSaved(userId: UUID): TimeSaved {
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
            SELECT activity_type, COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND activity_type IS NOT NULL AND time_saved_minutes IS NOT NULL
            GROUP BY activity_type ORDER BY saved DESC
            """,
            { rs, _ -> rs.getString("activity_type") to rs.getInt("saved") },
            userId
        ).toMap()

        return TimeSaved(total, byDomain, byActivity)
    }
}
