package app.rippl.ingestion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScoringDispatchService(
    private val scoringService: ScoringService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun triggerInitialScoring(sessionId: UUID) {
        try {
            scoringService.scoreInitial(sessionId)
            log.debug("Completed async initial scoring for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async initial scoring for sessionId={}", sessionId, ex)
        }
    }

    @Async
    fun triggerFeedbackRescoring(sessionId: UUID) {
        try {
            scoringService.rescoreFromFeedback(sessionId)
            log.debug("Completed async feedback re-scoring for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async feedback re-scoring for sessionId={}", sessionId, ex)
        }
    }
}
