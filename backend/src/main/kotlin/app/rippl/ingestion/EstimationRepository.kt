package app.rippl.ingestion

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class EstimationRepository(
    private val jdbc: JdbcTemplate
) {

    /**
     * Upsert an estimated_sessions row by activity_session_id.
     * user_id is derived from activity_sessions to enforce ownership consistency.
     * On conflict, all mutable fields plus estimated_at are updated.
     */
    fun upsertEstimation(
        sessionId: UUID,
        inferredTaskMixJson: String,
        effectiveMultiplier: Double,
        estimatedTimeSavedMs: Long,
        confidence: String,
        estimationMethod: String
    ) {
        jdbc.update(
            """
            INSERT INTO estimated_sessions (
                activity_session_id,
                user_id,
                inferred_task_mix,
                effective_multiplier,
                estimated_time_saved_ms,
                confidence,
                estimation_method,
                estimated_at
            )
            SELECT
                a.id,
                a.user_id,
                ?::jsonb,
                ?,
                ?,
                ?::estimation_confidence,
                ?::estimation_method,
                now()
            FROM activity_sessions a
            WHERE a.id = ?
            ON CONFLICT (activity_session_id) DO UPDATE SET
                inferred_task_mix       = EXCLUDED.inferred_task_mix,
                effective_multiplier    = EXCLUDED.effective_multiplier,
                estimated_time_saved_ms = EXCLUDED.estimated_time_saved_ms,
                confidence              = EXCLUDED.confidence,
                estimation_method       = EXCLUDED.estimation_method,
                estimated_at            = now()
            """.trimIndent(),
            inferredTaskMixJson,
            effectiveMultiplier,
            estimatedTimeSavedMs,
            confidence,
            estimationMethod,
            sessionId
        )
    }
}
