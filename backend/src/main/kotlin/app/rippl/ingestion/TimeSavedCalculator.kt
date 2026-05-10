package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.springframework.stereotype.Service

@Service
class TimeSavedCalculator(
    private val config: ScoringConfig
) {

    fun calculate(
        taskMix: TaskMix,
        activeMs: Long,
        durationMs: Long,
        personalAdjustmentFactor: Double
    ): TimeSavedResult {
        val effectiveActiveMs = if (activeMs > 0) activeMs else (durationMs * 0.7).toLong()

        if (effectiveActiveMs < config.minScorableMs) {
            return TimeSavedResult(savedMs = 0L, effectiveMultiplier = computeMultiplier(taskMix))
        }

        val multiplier = computeMultiplier(taskMix)
        val baseSaved = effectiveActiveMs * (1.0 - 1.0 / multiplier)
        val adjusted = (baseSaved * personalAdjustmentFactor).toLong()

        return TimeSavedResult(savedMs = adjusted, effectiveMultiplier = multiplier)
    }

    private fun computeMultiplier(taskMix: TaskMix): Double {
        val m = config.multipliers
        return taskMix.writing * m.writing +
            taskMix.coding * m.coding +
            taskMix.research * m.research +
            taskMix.planning * m.planning +
            taskMix.communication * m.communication +
            taskMix.other * m.other
    }
}
