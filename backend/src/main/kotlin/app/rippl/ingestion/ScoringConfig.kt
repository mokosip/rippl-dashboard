package app.rippl.ingestion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rippl.scoring")
data class ScoringConfig(
    val multipliers: Multipliers = Multipliers(),
    val confidenceWeights: ConfidenceWeights = ConfidenceWeights(),
    val confidenceThresholds: ConfidenceThresholds = ConfidenceThresholds(),
    val minScorableMs: Long = 10_000
) {
    data class Multipliers(
        val writing: Double = 1.4,
        val coding: Double = 1.7,
        val research: Double = 1.3,
        val planning: Double = 1.5,
        val communication: Double = 1.35,
        val other: Double = 1.2
    )

    data class ConfidenceWeights(
        val duration: Double = 0.25,
        val interaction: Double = 0.20,
        val taskCertainty: Double = 0.30,
        val collectorSignal: Double = 0.15,
        val profile: Double = 0.10
    )

    data class ConfidenceThresholds(
        val medium: Double = 0.40,
        val high: Double = 0.70
    )
}
