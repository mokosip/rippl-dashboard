package app.rippl.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class IngestionRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    fun ingest(userId: UUID, payload: ActivitySessionRequest, rawPayload: String): IngestWriteResult {
        val collectorJson = objectMapper.writeValueAsString(payload.collector)
        val sourceJson = objectMapper.writeValueAsString(payload.source)
        val sessionJson = objectMapper.writeValueAsString(payload.session)
        val privacyJson = objectMapper.writeValueAsString(payload.privacy)
        val metricsJson = objectMapper.writeValueAsString(payload.metrics)
        val contextJson = objectMapper.writeValueAsString(payload.context)

        // ON CONFLICT updates only synced_at to preserve raw payload immutability.
        // xmax = 0 on the returned tuple means the row was freshly inserted;
        // xmax != 0 means it was updated (i.e. a duplicate retry).
        val (sessionId, wasInserted) = jdbc.query(
            """
            INSERT INTO activity_sessions (
                user_id,
                collector_type,
                collector_session_id,
                collector,
                source,
                session,
                privacy,
                metrics,
                context,
                raw_payload,
                started_at,
                ended_at
            ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
            ON CONFLICT (user_id, collector_type, collector_session_id)
            DO UPDATE SET synced_at = now()
            RETURNING id, (xmax = 0) AS was_inserted
            """.trimIndent(),
            { rs, _ -> Pair(UUID.fromString(rs.getString("id")), rs.getBoolean("was_inserted")) },
            userId,
            payload.collector.type,
            payload.session.id,
            collectorJson,
            sourceJson,
            sessionJson,
            privacyJson,
            metricsJson,
            contextJson,
            rawPayload,
            payload.session.startedAt,
            payload.session.endedAt
        ).first()

        return IngestWriteResult(sessionId = sessionId, deduped = !wasInserted)
    }

    fun findOwnedSession(sessionId: UUID, userId: UUID): UUID? {
        return jdbc.query(
            """
            SELECT id
            FROM activity_sessions
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
            { rs, _ -> UUID.fromString(rs.getString("id")) },
            sessionId,
            userId
        ).firstOrNull()
    }

    fun upsertFeedback(sessionId: UUID, type: String, valueJson: String) {
        jdbc.update(
            """
            INSERT INTO activity_feedback (session_id, feedback_type, feedback_value)
            VALUES (?, ?, ?::jsonb)
            ON CONFLICT (session_id, feedback_type)
            DO UPDATE SET feedback_value = EXCLUDED.feedback_value, updated_at = now()
            """.trimIndent(),
            sessionId,
            type,
            valueJson
        )
    }

    fun countFeedbackRows(sessionId: UUID, type: String): Int {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM activity_feedback WHERE session_id = ? AND feedback_type = ?",
            Int::class.java,
            sessionId,
            type
        ) ?: 0
    }

    fun findLastSyncedAt(userId: UUID): Instant? {
        return jdbc.query(
            "SELECT MAX(synced_at) AS last_sync FROM activity_sessions WHERE user_id = ?",
            { rs, _ -> rs.getTimestamp("last_sync")?.toInstant() },
            userId
        ).firstOrNull()
    }
}
