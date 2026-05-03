package app.rippl.sessions

import app.rippl.sync.SyncSessionDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

data class UpsertResult(val accepted: Int, val duplicates: Int, val syncedAt: Long)

@Repository
class SessionUpsertRepository(private val jdbc: JdbcTemplate) {

    fun upsertSessions(userId: UUID, sessions: List<SyncSessionDto>): UpsertResult {
        var accepted = 0
        var duplicates = 0
        for (s in sessions) {
            val inserted = jdbc.queryForObject(
                """
                INSERT INTO sessions (id, user_id, domain, started_at, ended_at, active_seconds, date,
                                     activity_type, estimated_without_minutes, time_saved_minutes, logged)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET synced_at = NOW()
                RETURNING (xmax = 0)
                """,
                Boolean::class.java,
                s.id, userId, s.domain, s.startedAt, s.endedAt, s.activeSeconds,
                s.date, s.activityType, s.estimatedWithoutMinutes, s.timeSavedMinutes, s.logged ?: false
            )
            if (inserted == true) accepted++ else duplicates++
        }
        return UpsertResult(accepted, duplicates, Instant.now().toEpochMilli())
    }
}
