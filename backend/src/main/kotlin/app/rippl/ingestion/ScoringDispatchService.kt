package app.rippl.ingestion

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScoringDispatchService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun triggerIngestScore(sessionId: UUID) {
        try {
            log.debug("Queued async score for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async ingest score dispatch for sessionId={}", sessionId, ex)
        }
    }

    @Async
    fun triggerFeedbackRescore(sessionId: UUID) {
        try {
            log.debug("Queued async re-score from feedback for sessionId={}", sessionId)
        } catch (ex: Exception) {
            log.warn("Failed async feedback score dispatch for sessionId={}", sessionId, ex)
        }
    }
}
