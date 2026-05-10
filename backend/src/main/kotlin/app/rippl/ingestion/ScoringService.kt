package app.rippl.ingestion

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScoringService(
    private val scoringRepository: ScoringRepository
) {

    companion object {
        private const val PLACEHOLDER_TASK_MIX = """{"unknown":1.0}"""
        private const val PLACEHOLDER_MULTIPLIER = 1.0
        private const val PLACEHOLDER_TIME_SAVED_MS = 0L
        private const val PLACEHOLDER_CONFIDENCE = "low"
        private const val PLACEHOLDER_METHOD = "global_fallback"
    }

    fun scoreInitial(sessionId: UUID) {
        scoringRepository.upsertScoring(
            sessionId = sessionId,
            inferredTaskMixJson = PLACEHOLDER_TASK_MIX,
            effectiveMultiplier = PLACEHOLDER_MULTIPLIER,
            estimatedTimeSavedMs = PLACEHOLDER_TIME_SAVED_MS,
            confidence = PLACEHOLDER_CONFIDENCE,
            scoringMethod = PLACEHOLDER_METHOD
        )
    }

    fun rescoreFromFeedback(sessionId: UUID) {
        scoringRepository.upsertScoring(
            sessionId = sessionId,
            inferredTaskMixJson = PLACEHOLDER_TASK_MIX,
            effectiveMultiplier = PLACEHOLDER_MULTIPLIER,
            estimatedTimeSavedMs = PLACEHOLDER_TIME_SAVED_MS,
            confidence = PLACEHOLDER_CONFIDENCE,
            scoringMethod = PLACEHOLDER_METHOD
        )
    }
}
