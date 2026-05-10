package app.rippl.ingestion

import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class ConfidenceCalculator(
    private val config: ScoringConfig
) {

    fun calculate(
        durationMs: Long,
        interactionCount: Int,
        method: ScoringMethod,
        collectorType: String,
        profileOnboarded: Boolean
    ): ConfidenceResult {
        val w = config.confidenceWeights

        val durationQuality = min(durationMs / 1_800_000.0, 1.0)
        val interactionQuality = min(interactionCount / 20.0, 1.0)
        val taskCertainty = taskCertaintyFor(method)
        val collectorSignal = collectorSignalFor(collectorType, interactionCount)
        val profileQuality = if (profileOnboarded) 1.0 else 0.3

        val score = w.duration * durationQuality +
            w.interaction * interactionQuality +
            w.taskCertainty * taskCertainty +
            w.collectorSignal * collectorSignal +
            w.profile * profileQuality

        val level = when {
            score >= config.confidenceThresholds.high -> "high"
            score >= config.confidenceThresholds.medium -> "medium"
            else -> "low"
        }

        return ConfidenceResult(score = score, level = level)
    }

    private fun taskCertaintyFor(method: ScoringMethod): Double = when (method) {
        ScoringMethod.FEEDBACK_ADJUSTED -> 1.0
        ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED -> 0.7
        ScoringMethod.PROFILE_DEFAULT -> 0.4
        ScoringMethod.GLOBAL_FALLBACK -> 0.1
    }

    private fun collectorSignalFor(collectorType: String, interactionCount: Int): Double = when {
        collectorType == "cli_wrapper" -> 0.3
        interactionCount >= 1 -> 0.8
        else -> 0.5
    }
}
