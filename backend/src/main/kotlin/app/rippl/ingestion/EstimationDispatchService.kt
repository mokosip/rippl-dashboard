package app.rippl.ingestion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EstimationDispatchService(
    private val estimationService: EstimationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun triggerInitialEstimation(sessionId: UUID) {
        try {
            estimationService.estimateInitial(sessionId)
            log.debug("Completed async initial estimation for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async initial estimation for sessionId={}", sessionId, ex)
        }
    }

    @Async
    fun triggerFeedbackReestimation(sessionId: UUID) {
        try {
            estimationService.reestimateFromFeedback(sessionId)
            log.debug("Completed async feedback re-estimation for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async feedback re-estimation for sessionId={}", sessionId, ex)
        }
    }
}
