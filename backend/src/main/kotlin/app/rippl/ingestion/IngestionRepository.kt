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
        val collectorMetrics = payload.metrics
            .filterKeys { it !in setOf("duration_ms", "active_ms") }
        val collectorContext = payload.context
            .filterKeys { it !in setOf("domain", "surface") }

        val collectorMetricsJson = objectMapper.writeValueAsString(collectorMetrics)
        val collectorContextJson = objectMapper.writeValueAsString(collectorContext)

        val startedAt = java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(payload.session.startedAt))
        val endedAt = java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(payload.session.endedAt))
        val durationMs = (payload.metrics["duration_ms"] as Number).toLong()
        val activeMs = (payload.metrics["active_ms"] as Number).toLong()
        val domain = payload.context["domain"] as String
        val surface = payload.context["surface"] as String

        val (sessionId, wasInserted) = jdbc.query(
            """
            INSERT INTO activity_sessions (
                user_id,
                collector_type,
                collector_version,
                collector_session_id,
                source_type,
                source_version,
                domain,
                surface,
                started_at,
                ended_at,
                duration_ms,
                active_ms,
                collector_metrics,
                collector_context,
                raw_payload
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
            ON CONFLICT (user_id, collector_type, collector_session_id)
            DO UPDATE SET synced_at = now()
            RETURNING id, (xmax = 0) AS was_inserted
            """.trimIndent(),
            { rs, _ -> Pair(UUID.fromString(rs.getString("id")), rs.getBoolean("was_inserted")) },
            userId,
            payload.collector.type,
            payload.collector.version,
            payload.session.id,
            payload.source.type,
            payload.source.version,
            domain,
            surface,
            startedAt,
            endedAt,
            durationMs,
            activeMs,
            collectorMetricsJson,
            collectorContextJson,
            rawPayload
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
