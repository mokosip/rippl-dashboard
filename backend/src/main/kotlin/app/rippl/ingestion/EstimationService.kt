package app.rippl.ingestion

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EstimationService(
    private val estimationRepository: EstimationRepository
) {

    companion object {
        private const val PLACEHOLDER_TASK_MIX = """{"unknown":1.0}"""
        private const val PLACEHOLDER_MULTIPLIER = 1.0
        private const val PLACEHOLDER_TIME_SAVED_MS = 0L
        private const val PLACEHOLDER_CONFIDENCE = "low"
        private const val PLACEHOLDER_METHOD = "global_fallback"
    }

    fun estimateInitial(sessionId: UUID) {
        estimationRepository.upsertEstimation(
            sessionId = sessionId,
            inferredTaskMixJson = PLACEHOLDER_TASK_MIX,
            effectiveMultiplier = PLACEHOLDER_MULTIPLIER,
            estimatedTimeSavedMs = PLACEHOLDER_TIME_SAVED_MS,
            confidence = PLACEHOLDER_CONFIDENCE,
            estimationMethod = PLACEHOLDER_METHOD
        )
    }

    fun reestimateFromFeedback(sessionId: UUID) {
        estimationRepository.upsertEstimation(
            sessionId = sessionId,
            inferredTaskMixJson = PLACEHOLDER_TASK_MIX,
            effectiveMultiplier = PLACEHOLDER_MULTIPLIER,
            estimatedTimeSavedMs = PLACEHOLDER_TIME_SAVED_MS,
            confidence = PLACEHOLDER_CONFIDENCE,
            estimationMethod = PLACEHOLDER_METHOD
        )
    }
}
