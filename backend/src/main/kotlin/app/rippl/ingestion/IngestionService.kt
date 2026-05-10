package app.rippl.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IngestionService(
    private val repository: IngestionRepository,
    private val scoringDispatchService: ScoringDispatchService,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val MAX_SESSION_DURATION_MILLIS = 24 * 60 * 60 * 1000L
    }

    fun ingest(userId: UUID, payload: ActivitySessionRequest, rawPayload: String): IngestWriteResult {
        validateSessionPayload(payload)

        val result = repository.ingest(userId, payload, rawPayload)
        if (!result.deduped) {
            scoringDispatchService.triggerInitialScoring(result.sessionId)
        }
        return result
    }

    fun saveFeedback(userId: UUID, sessionId: UUID, request: SessionFeedbackRequest) {
        validateFeedback(request)

        val ownedSession = repository.findOwnedSession(sessionId, userId)
            ?: throw IngestionProblemException(
                httpStatus = HttpStatus.NOT_FOUND,
                errorCode = "session_not_found",
                message = "Activity session not found"
            )

        val valueJson = objectMapper.writeValueAsString(request.value)
        repository.upsertFeedback(ownedSession, request.type, valueJson)
        scoringDispatchService.triggerFeedbackRescoring(ownedSession)
    }

    private fun validateSessionPayload(payload: ActivitySessionRequest) {
        val errors = mutableListOf<FieldError>()

        if (payload.collector.type.isBlank()) {
            errors.add(FieldError("collector.type", "must not be blank"))
        }

        if (payload.source.type.isBlank()) {
            errors.add(FieldError("source.type", "must not be blank"))
        }

        if (payload.session.id.isBlank()) {
            errors.add(FieldError("session.id", "must not be blank"))
        }

        if (payload.session.endedAt < payload.session.startedAt) {
            errors.add(FieldError("session.ended_at", "must be greater than or equal to session.started_at"))
        }

        if (payload.session.startedAt <= 0) {
            errors.add(FieldError("session.started_at", "must be a positive unix timestamp in milliseconds"))
        }

        if (payload.session.endedAt <= 0) {
            errors.add(FieldError("session.ended_at", "must be a positive unix timestamp in milliseconds"))
        }

        val duration = payload.session.endedAt - payload.session.startedAt
        if (duration > MAX_SESSION_DURATION_MILLIS) {
            errors.add(FieldError("session.ended_at", "duration exceeds max bound of 24h"))
        }

        val domain = payload.context["domain"]
        if (domain !is String || domain.isBlank()) {
            errors.add(FieldError("context.domain", "must be a non-blank string"))
        }

        val surface = payload.context["surface"]
        if (surface !is String || surface.isBlank()) {
            errors.add(FieldError("context.surface", "must be a non-blank string"))
        }

        val durationMs = payload.metrics["duration_ms"]
        if (durationMs !is Number || durationMs.toLong() < 0) {
            errors.add(FieldError("metrics.duration_ms", "must be a non-negative number"))
        }

        val activeMs = payload.metrics["active_ms"]
        if (activeMs !is Number || activeMs.toLong() < 0) {
            errors.add(FieldError("metrics.active_ms", "must be a non-negative number"))
        }

        if (errors.isNotEmpty()) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "validation_error",
                message = "Invalid activity session payload",
                fieldErrors = errors
            )
        }

        if (
            payload.privacy.contentCollected ||
            payload.privacy.contentSent ||
            payload.privacy.promptCollected ||
            payload.privacy.responseCollected
        ) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "policy_violation",
                message = "Forbidden privacy flags must be false",
                fieldErrors = listOf(
                    FieldError("privacy", "content_collected, content_sent, prompt_collected, response_collected must remain false")
                )
            )
        }
    }

    private fun validateFeedback(request: SessionFeedbackRequest) {
        val errors = mutableListOf<FieldError>()

        if (request.type != "task_type" && request.type != "accuracy") {
            errors.add(FieldError("type", "must be one of: task_type, accuracy"))
        }

        if (request.type == "task_type" && (!request.value.isTextual || request.value.asText().isBlank())) {
            errors.add(FieldError("value", "must be non-empty string when type=task_type"))
        }

        if (request.type == "accuracy" && !request.value.isNumber) {
            errors.add(FieldError("value", "must be numeric when type=accuracy"))
        }

        if (errors.isNotEmpty()) {
            throw IngestionProblemException(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "validation_error",
                message = "Invalid feedback payload",
                fieldErrors = errors
            )
        }
    }
}
