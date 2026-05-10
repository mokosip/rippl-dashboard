package app.rippl.ingestion

import app.rippl.profile.TaskMix
import org.springframework.stereotype.Service

@Service
class TaskMixInferenceService {

    companion object {
        private val KNOWN_TASK_TYPES = setOf("writing", "coding", "research", "planning", "communication", "other")
    }

    fun infer(
        feedbackTaskType: String?,
        collectorType: String,
        profile: TaskMix,
        profileOnboarded: Boolean
    ): InferenceResult {
        if (feedbackTaskType != null && feedbackTaskType in KNOWN_TASK_TYPES) {
            return InferenceResult(oneHot(feedbackTaskType), ScoringMethod.FEEDBACK_ADJUSTED)
        }

        if (collectorType == "cli_wrapper") {
            val codingHint = TaskMix(coding = 1.0)
            return InferenceResult(blend(profile, codingHint), ScoringMethod.COLLECTOR_SIGNAL_ADJUSTED)
        }

        if (profileOnboarded) {
            return InferenceResult(profile, ScoringMethod.PROFILE_DEFAULT)
        }

        return InferenceResult(TaskMix.GLOBAL_DEFAULT, ScoringMethod.GLOBAL_FALLBACK)
    }

    private fun oneHot(taskType: String): TaskMix = when (taskType) {
        "writing" -> TaskMix(writing = 1.0)
        "coding" -> TaskMix(coding = 1.0)
        "research" -> TaskMix(research = 1.0)
        "planning" -> TaskMix(planning = 1.0)
        "communication" -> TaskMix(communication = 1.0)
        "other" -> TaskMix(other = 1.0)
        else -> TaskMix.GLOBAL_DEFAULT
    }

    private fun blend(a: TaskMix, b: TaskMix): TaskMix = TaskMix(
        writing = a.writing * 0.5 + b.writing * 0.5,
        coding = a.coding * 0.5 + b.coding * 0.5,
        research = a.research * 0.5 + b.research * 0.5,
        planning = a.planning * 0.5 + b.planning * 0.5,
        communication = a.communication * 0.5 + b.communication * 0.5,
        other = a.other * 0.5 + b.other * 0.5
    )
}
