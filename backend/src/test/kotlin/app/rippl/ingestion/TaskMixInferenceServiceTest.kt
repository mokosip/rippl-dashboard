package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskMixInferenceServiceTest {

    private val service = TaskMixInferenceService()

    @Test
    fun `feedback produces one-hot task mix`() {
        val result = service.infer(
            feedbackTaskType = "coding",
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = true
        )
        assertEquals(TaskMix(coding = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }

    @Test
    fun `unknown feedback type falls through to profile`() {
        val profile = TaskMix(writing = 0.6, research = 0.3, planning = 0.1)
        val result = service.infer(
            feedbackTaskType = "juggling",
            collectorType = "chrome_extension",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(profile, result.taskMix)
        assertEquals(ScoringMethod.PROFILE_DEFAULT, result.method)
    }

    @Test
    fun `cli_wrapper blends profile 50-50 with coding`() {
        val profile = TaskMix(coding = 0.2, writing = 0.8)
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "cli_wrapper",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(0.6, result.taskMix.coding, 0.001)
        assertEquals(0.4, result.taskMix.writing, 0.001)
        assertEquals(ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED, result.method)
    }

    @Test
    fun `onboarded profile used when no feedback or collector hint`() {
        val profile = TaskMix(research = 0.5, writing = 0.3, planning = 0.2)
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "chrome_extension",
            profile = profile,
            profileOnboarded = true
        )
        assertEquals(profile, result.taskMix)
        assertEquals(ScoringMethod.PROFILE_DEFAULT, result.method)
    }

    @Test
    fun `non-onboarded profile falls through to global fallback`() {
        val result = service.infer(
            feedbackTaskType = null,
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = false
        )
        assertEquals(TaskMix.GLOBAL_DEFAULT, result.taskMix)
        assertEquals(ScoringMethod.GLOBAL_FALLBACK, result.method)
    }

    @Test
    fun `feedback for writing produces one-hot`() {
        val result = service.infer(
            feedbackTaskType = "writing",
            collectorType = "chrome_extension",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = false
        )
        assertEquals(TaskMix(writing = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }

    @Test
    fun `cli_wrapper with feedback uses feedback not blend`() {
        val result = service.infer(
            feedbackTaskType = "research",
            collectorType = "cli_wrapper",
            profile = TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = true
        )
        assertEquals(TaskMix(research = 1.0), result.taskMix)
        assertEquals(ScoringMethod.FEEDBACK_ADJUSTED, result.method)
    }
}
