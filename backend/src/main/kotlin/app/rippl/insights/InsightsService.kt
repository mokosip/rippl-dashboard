package app.rippl.insights

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

data class MirrorMoment(val type: String, val message: String)

@Service
class InsightsService(private val jdbc: JdbcTemplate) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun mirrorMoments(userId: UUID): List<MirrorMoment> {
        val moments = mutableListOf<MirrorMoment>()
        weeklyUsage(userId)?.let { moments.add(it) }
        topTool(userId)?.let { moments.add(it) }
        timeSavingActivity(userId)?.let { moments.add(it) }
        busiestDay(userId)?.let { moments.add(it) }
        log.debug("Generated {} mirror moments for userId: {} — types: {}", moments.size, userId, moments.map { it.type })
        return moments
    }

    private fun weeklyUsage(userId: UUID): MirrorMoment? {
        val rows = jdbc.query(
            """
            SELECT
                COALESCE(SUM(CASE WHEN date >= date_trunc('week', CURRENT_DATE) THEN active_seconds END), 0) AS this_week,
                COALESCE(SUM(CASE WHEN date >= date_trunc('week', CURRENT_DATE) - INTERVAL '7 days'
                                   AND date < date_trunc('week', CURRENT_DATE) THEN active_seconds END), 0) AS last_week
            FROM sessions WHERE user_id = ?
            """,
            { rs, _ -> rs.getLong("this_week") to rs.getLong("last_week") },
            userId
        )
        val (thisWeek, lastWeek) = rows.firstOrNull() ?: return null
        if (thisWeek == 0L) return null

        val timeStr = if (thisWeek < 3600) {
            "${thisWeek / 60} minutes"
        } else {
            "%.1f hours".format(java.util.Locale.GERMAN, thisWeek / 3600.0)
        }
        val comparison = when {
            lastWeek == 0L -> "your first tracked week"
            thisWeek > lastWeek -> "${(thisWeek - lastWeek) * 100 / lastWeek}% more than last week"
            thisWeek < lastWeek -> "${(lastWeek - thisWeek) * 100 / lastWeek}% less than last week"
            else -> "same as last week"
        }
        return MirrorMoment("weekly_usage", "You used AI for $timeStr this week — $comparison.")
    }

    private fun topTool(userId: UUID): MirrorMoment? {
        val tools = jdbc.query(
            """
            SELECT domain, SUM(active_seconds)::bigint AS total
            FROM sessions WHERE user_id = ?
            GROUP BY domain ORDER BY total DESC LIMIT 2
            """,
            { rs, _ -> rs.getString("domain") to rs.getLong("total") },
            userId
        )
        if (tools.size < 2) return null
        val ratio = "%.1f".format(tools[0].second.toDouble() / tools[1].second)
        return MirrorMoment("top_tool",
            "${tools[0].first} is your most-used tool. You spend ${ratio}x more time there than ${tools[1].first}.")
    }

    private fun timeSavingActivity(userId: UUID): MirrorMoment? {
        val rows = jdbc.query(
            """
            SELECT unnest(string_to_array(activity_type, ', ')) AS activity,
                   COALESCE(SUM(time_saved_minutes), 0)::int AS saved
            FROM sessions WHERE user_id = ? AND activity_type IS NOT NULL
              AND date >= date_trunc('month', CURRENT_DATE)
            GROUP BY activity ORDER BY saved DESC LIMIT 1
            """,
            { rs, _ -> rs.getString("activity") to rs.getInt("saved") },
            userId
        )
        val (activity, saved) = rows.firstOrNull() ?: return null
        if (saved == 0) return null
        val timeStr = if (saved < 60) {
            "$saved minutes"
        } else {
            "%.1f hours".format(java.util.Locale.GERMAN, saved / 60.0)
        }
        return MirrorMoment("time_saving_activity", "$activity is where AI saves you the most time — $timeStr freed this month.")
    }

    private fun busiestDay(userId: UUID): MirrorMoment? {
        val byDay = jdbc.query(
            """
            SELECT EXTRACT(DOW FROM date)::int AS dow, SUM(active_seconds)::bigint AS total
            FROM sessions WHERE user_id = ?
            GROUP BY dow ORDER BY total DESC
            """,
            { rs, _ -> rs.getInt("dow") to rs.getLong("total") },
            userId
        )
        if (byDay.size < 2) return null
        return MirrorMoment("busiest_day",
            "Your busiest AI day is ${dayNames[byDay.first().first]}. You barely touch AI on ${dayNames[byDay.last().first]}.")
    }
}
