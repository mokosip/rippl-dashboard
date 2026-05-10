package app.rippl.ingestion

import app.rippl.profile.TaskMix
import java.util.UUID

enum class ScoringMethod(val dbValue: String) {
    FEEDBACK_ADJUSTED("feedback_adjusted"),
    COLLECTOR_SIGNAL_ADJUSTED("collector_signal_adjusted"),
    PROFILE_DEFAULT("profile_default"),
    GLOBAL_FALLBACK("global_fallback");
}

data class ScoringInput(
    val sessionId: UUID,
    val userId: UUID,
    val collectorType: String,
    val surface: String,
    val activeMs: Long,
    val durationMs: Long,
    val interactionCount: Int,
    val feedbackTaskType: String?
)

data class InferenceResult(
    val taskMix: TaskMix,
    val method: ScoringMethod
)

data class TimeSavedResult(
    val savedMs: Long,
    val effectiveMultiplier: Double
)

data class ConfidenceResult(
    val score: Double,
    val level: String
)
