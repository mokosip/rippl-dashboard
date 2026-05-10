package app.rippl.ingestion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
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
    @TransactionalEventListener
    fun onFeedbackSaved(event: FeedbackSavedEvent) {
        try {
            scoringService.rescoreFromFeedback(event.sessionId)
            log.debug("Completed feedback re-scoring for sessionId={}", event.sessionId)
        } catch (ex: Exception) {
            log.warn("Failed feedback re-scoring for sessionId={}", event.sessionId, ex)
        }
    }
}
