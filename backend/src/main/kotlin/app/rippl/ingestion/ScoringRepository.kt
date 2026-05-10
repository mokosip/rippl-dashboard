package app.rippl.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ScoringRepository(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    fun upsertScoring(
        sessionId: UUID,
        inferredTaskMixJson: String,
        effectiveMultiplier: Double,
        estimatedTimeSavedMs: Long,
        confidence: String,
        scoringMethod: String
    ) {
        jdbc.update(
            """
            INSERT INTO scored_sessions (
                activity_session_id,
                user_id,
                inferred_task_mix,
                effective_multiplier,
                estimated_time_saved_ms,
                confidence,
                scoring_method,
                scored_at
            )
            SELECT
                a.id,
                a.user_id,
                ?::jsonb,
                ?,
                ?,
                ?::scoring_confidence,
                ?::scoring_method,
                now()
            FROM activity_sessions a
            WHERE a.id = ?
            ON CONFLICT (activity_session_id) DO UPDATE SET
                inferred_task_mix       = EXCLUDED.inferred_task_mix,
                effective_multiplier    = EXCLUDED.effective_multiplier,
                estimated_time_saved_ms = EXCLUDED.estimated_time_saved_ms,
                confidence              = EXCLUDED.confidence,
                scoring_method          = EXCLUDED.scoring_method,
                scored_at               = now()
            """.trimIndent(),
            inferredTaskMixJson,
            effectiveMultiplier,
            estimatedTimeSavedMs,
            confidence,
            scoringMethod,
            sessionId
        )
    }

    fun loadScoringInput(sessionId: UUID): ScoringInput? {
        return jdbc.query(
            """
            SELECT
                a.id AS session_id,
                a.user_id,
                a.collector_type,
                a.surface,
                a.active_ms,
                a.duration_ms,
                a.collector_metrics,
                f.feedback_value AS task_type_feedback
            FROM activity_sessions a
            LEFT JOIN activity_feedback f
                ON f.session_id = a.id AND f.feedback_type = 'task_type'
            WHERE a.id = ?
            """.trimIndent(),
            { rs, _ ->
                val metricsJson = rs.getString("collector_metrics")
                val metrics = try {
                    @Suppress("UNCHECKED_CAST")
                    objectMapper.readValue(metricsJson, Map::class.java) as Map<String, Any?>
                } catch (_: Exception) {
                    emptyMap<String, Any?>()
                }

                val interactionCount = listOf("interaction_count", "copy_events", "paste_events")
                    .sumOf { key -> (metrics[key] as? Number)?.toInt() ?: 0 }

                val feedbackRaw = rs.getString("task_type_feedback")
                val feedbackTaskType = if (feedbackRaw != null) {
                    try {
                        val node = objectMapper.readTree(feedbackRaw)
                        if (node.isTextual) node.asText() else null
                    } catch (_: Exception) {
                        null
                    }
                } else null

                ScoringInput(
                    sessionId = UUID.fromString(rs.getString("session_id")),
                    userId = UUID.fromString(rs.getString("user_id")),
                    collectorType = rs.getString("collector_type"),
                    surface = rs.getString("surface"),
                    activeMs = rs.getLong("active_ms"),
                    durationMs = rs.getLong("duration_ms"),
                    interactionCount = interactionCount,
                    feedbackTaskType = feedbackTaskType
                )
            },
            sessionId
        ).firstOrNull()
    }
}
