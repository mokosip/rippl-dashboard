package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class TimeSavedCalculatorTest {

    private val config = ScoringConfig()
    private val calculator = TimeSavedCalculator(config)

    @Test
    fun `pure coding session uses coding multiplier`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 600_000, // 10 min
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // effective_multiplier = 1.7
        // base_saved = 600_000 * (1 - 1/1.7) = 600_000 * 0.41176 = 247_058
        assertEquals(1.7, result.effectiveMultiplier, 0.001)
        assertWithinTolerance(expected = 247_058L, actual = result.savedMs, tolerance = 1_000L)
    }

    @Test
    fun `mixed task mix blends multipliers`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 0.5, writing = 0.5),
            activeMs = 600_000,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // effective_multiplier = 0.5*1.7 + 0.5*1.4 = 1.55
        assertEquals(1.55, result.effectiveMultiplier, 0.001)
        // base_saved = 600_000 * (1 - 1/1.55) = 600_000 * 0.35484 = 212_903
        assertWithinTolerance(expected = 212_903L, actual = result.savedMs, tolerance = 1_000L)
    }

    @Test
    fun `personal adjustment factor scales result`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 600_000,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.5
        )
        // base_saved = 247_058, adjusted = 247_058 * 1.5 = 370_588
        assertWithinTolerance(expected = 370_588L, actual = result.savedMs, tolerance = 1_500L)
    }

    @Test
    fun `session under min threshold returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 5_000, // 5s, under 10s threshold
            durationMs = 5_000,
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }

    @Test
    fun `zero active_ms falls back to duration`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 600_000,
            personalAdjustmentFactor = 1.0
        )
        // fallback activeMs = 600_000 * 0.7 = 420_000
        // base_saved = 420_000 * (1 - 1/1.7) = 420_000 * 0.41176 = 172_941
        assertWithinTolerance(expected = 172_941L, actual = result.savedMs, tolerance = 1_000L)
    }

    @Test
    fun `zero duration returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 0,
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }

    @Test
    fun `duration fallback under threshold returns 0`() {
        val result = calculator.calculate(
            taskMix = TaskMix(coding = 1.0),
            activeMs = 0,
            durationMs = 12_000, // fallback = 8_400ms, under 10s threshold
            personalAdjustmentFactor = 1.0
        )
        assertEquals(0L, result.savedMs)
    }

    private fun assertWithinTolerance(expected: Long, actual: Long, tolerance: Long) {
        assertTrue(abs(expected - actual) <= tolerance)
    }
}
