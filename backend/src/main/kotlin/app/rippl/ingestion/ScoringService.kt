package app.rippl.ingestion

import app.rippl.profile.TaskMix
import app.rippl.profile.UserProfileRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScoringService(
    private val scoringRepository: ScoringRepository,
    private val userProfileRepository: UserProfileRepository,
    private val inferenceService: TaskMixInferenceService,
    private val timeSavedCalculator: TimeSavedCalculator,
    private val confidenceCalculator: ConfidenceCalculator,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun scoreInitial(sessionId: UUID) {
        score(sessionId)
    }

    fun rescoreFromFeedback(sessionId: UUID) {
        score(sessionId)
    }

    private fun score(sessionId: UUID) {
        val input = scoringRepository.loadScoringInput(sessionId)
        if (input == null) {
            log.warn("Cannot score session={}: not found", sessionId)
            return
        }

        val profile = userProfileRepository.findByUserId(input.userId)

        val inference = inferenceService.infer(
            feedbackTaskType = input.feedbackTaskType,
            collectorType = input.collectorType,
            profile = profile?.taskMix ?: TaskMix.GLOBAL_DEFAULT,
            profileOnboarded = profile?.onboarded ?: false
        )

        val timeSaved = timeSavedCalculator.calculate(
            taskMix = inference.taskMix,
            activeMs = input.activeMs,
            durationMs = input.durationMs,
            personalAdjustmentFactor = profile?.personalAdjustmentFactor ?: 1.0
        )

        val confidence = confidenceCalculator.calculate(
            durationMs = input.durationMs,
            interactionCount = input.interactionCount,
            method = inference.method,
            collectorType = input.collectorType,
            profileOnboarded = profile?.onboarded ?: false
        )

        val taskMixJson = objectMapper.writeValueAsString(inference.taskMix)

        scoringRepository.upsertScoring(
            sessionId = sessionId,
            inferredTaskMixJson = taskMixJson,
            effectiveMultiplier = timeSaved.effectiveMultiplier,
            estimatedTimeSavedMs = timeSaved.savedMs,
            confidence = confidence.level,
            scoringMethod = inference.method.dbValue
        )
    }
}
