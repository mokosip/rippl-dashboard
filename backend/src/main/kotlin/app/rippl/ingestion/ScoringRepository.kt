package app.rippl.ingestion

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ScoringRepository(
    private val jdbc: JdbcTemplate
) {

    /**
     * Upsert a scored_sessions row by activity_session_id.
     * user_id is derived from activity_sessions to enforce ownership consistency.
     * On conflict, all mutable fields plus scored_at are updated.
     */
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
}
