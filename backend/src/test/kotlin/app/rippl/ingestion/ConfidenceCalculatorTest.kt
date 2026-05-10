package app.rippl.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfidenceCalculatorTest {

    private val config = ScoringConfig()
    private val calculator = ConfidenceCalculator(config)

    @Test
    fun `high confidence - long session with feedback and interactions`() {
        val result = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 25,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        assertEquals(0.97, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `low confidence - short cli session no feedback no profile`() {
        val result = calculator.calculate(
            durationMs = 60_000,
            interactionCount = 0,
            method = ScoringMethod.GLOBAL_FALLBACK,
            collectorType = "cli_wrapper",
            profileOnboarded = false
        )
        assertEquals(0.11, result.score, 0.02)
        assertEquals("low", result.level)
    }

    @Test
    fun `medium confidence - moderate signals`() {
        val result = calculator.calculate(
            durationMs = 900_000,
            interactionCount = 10,
            method = ScoringMethod.PROFILE_DEFAULT,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        assertEquals(0.565, result.score, 0.01)
        assertEquals("medium", result.level)
    }

    @Test
    fun `browser without interactions gets lower collector signal`() {
        val result = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 0,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        assertEquals(0.725, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `collector signal adjusted method gets 0_7 task certainty`() {
        val result = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 20,
            method = ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED,
            collectorType = "cli_wrapper",
            profileOnboarded = true
        )
        assertEquals(0.805, result.score, 0.01)
        assertEquals("high", result.level)
    }

    @Test
    fun `duration caps at 30 minutes`() {
        val shortResult = calculator.calculate(
            durationMs = 3_600_000,
            interactionCount = 20,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        val capResult = calculator.calculate(
            durationMs = 1_800_000,
            interactionCount = 20,
            method = ScoringMethod.FEEDBACK_ADJUSTED,
            collectorType = "chrome_extension",
            profileOnboarded = true
        )
        assertEquals(capResult.score, shortResult.score, 0.001)
    }
}
