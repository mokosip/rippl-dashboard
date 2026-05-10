package app.rippl.ingestion

import app.rippl.sync.RateLimiter
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/v1/activity-sessions")
class IngestionController(
    private val ingestionService: IngestionService,
    private val rateLimiter: RateLimiter,
    objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val strictMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    companion object {
        private const val MAX_PAYLOAD_BYTES = 128 * 1024
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun ingest(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody rawBody: String,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val startedAt = System.currentTimeMillis()
        val requestId = resolveRequestId(request)
        var collectorType = "unknown"
        var statusCode = 500
        var errorCode: String? = null

        try {
            val rateLimit = rateLimiter.check(userId)
            if (!rateLimit.allowed) {
                statusCode = 429
                errorCode = "rate_limited"
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", (rateLimit.retryAfterSeconds ?: 1).toString())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problemBody(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Rate limit exceeded", "rate_limited"))
            }

            if (rawBody.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
                statusCode = 413
                errorCode = "payload_too_large"
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problemBody(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", "Request payload exceeds 128KB", "payload_too_large"))
            }

            val payload = try {
                parseSessionBody(rawBody)
            } catch (ex: JsonProcessingException) {
                statusCode = 400
                errorCode = "invalid_schema"
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(
                        problemBody(
                            HttpStatus.BAD_REQUEST,
                            "Bad Request",
                            "Invalid request body",
                            "invalid_schema",
                            extractFieldErrors(ex)
                        )
                    )
            }

            collectorType = payload.collector.type
            val result = ingestionService.ingest(userId, payload, rawBody)

            val askFeedback = ingestionService.shouldRequestFeedback(userId, result.sessionId)
            val responseBody = linkedMapOf<String, Any>(
                "accepted" to true,
                "session_id" to result.sessionId.toString(),
                "feedback_request" to mapOf(
                    "ask" to askFeedback,
                    "session_id" to result.sessionId.toString(),
                    "type" to "task_type"
                )
            )

            if (result.deduped) {
                responseBody["deduped"] = true
                statusCode = 200
                return ResponseEntity.ok(responseBody)
            }

            statusCode = 201
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
        } catch (ex: IngestionProblemException) {
            statusCode = ex.httpStatus.value()
            errorCode = ex.errorCode
            return ResponseEntity.status(ex.httpStatus)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemBody(ex.httpStatus, ex.httpStatus.reasonPhrase, ex.message, ex.errorCode, ex.fieldErrors))
        } finally {
            val latency = System.currentTimeMillis() - startedAt
            log.info(
                "request_id={} user_id_hash={} collector_type={} status_code={} error_code={} latency_ms={}",
                requestId,
                hashUserId(userId),
                collectorType,
                statusCode,
                errorCode ?: "",
                latency
            )
        }
    }

    @PostMapping("/{id}/feedback", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun submitFeedback(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: UUID,
        @RequestBody rawBody: String,
        request: HttpServletRequest
    ): ResponseEntity<Any> {
        val startedAt = System.currentTimeMillis()
        val requestId = resolveRequestId(request)
        var statusCode = 500
        var errorCode: String? = null

        try {
            val rateLimit = rateLimiter.check(userId)
            if (!rateLimit.allowed) {
                statusCode = 429
                errorCode = "rate_limited"
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", (rateLimit.retryAfterSeconds ?: 1).toString())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problemBody(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Rate limit exceeded", "rate_limited"))
            }

            if (rawBody.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
                statusCode = 413
                errorCode = "payload_too_large"
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problemBody(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", "Request payload exceeds 128KB", "payload_too_large"))
            }

            val feedback = try {
                parseFeedbackBody(rawBody)
            } catch (ex: JsonProcessingException) {
                statusCode = 400
                errorCode = "invalid_schema"
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(
                        problemBody(
                            HttpStatus.BAD_REQUEST,
                            "Bad Request",
                            "Invalid request body",
                            "invalid_schema",
                            extractFieldErrors(ex)
                        )
                    )
            }

            ingestionService.saveFeedback(userId, id, feedback)

            statusCode = 200
            return ResponseEntity.ok(
                mapOf(
                    "accepted" to true,
                    "session_id" to id.toString(),
                    "feedback_type" to feedback.type
                )
            )
        } catch (ex: IngestionProblemException) {
            statusCode = ex.httpStatus.value()
            errorCode = ex.errorCode
            return ResponseEntity.status(ex.httpStatus)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemBody(ex.httpStatus, ex.httpStatus.reasonPhrase, ex.message, ex.errorCode, ex.fieldErrors))
        } finally {
            val latency = System.currentTimeMillis() - startedAt
            log.info(
                "request_id={} user_id_hash={} collector_type={} status_code={} error_code={} latency_ms={}",
                requestId,
                hashUserId(userId),
                "unknown",
                statusCode,
                errorCode ?: "",
                latency
            )
        }
    }

    private fun parseSessionBody(rawBody: String): ActivitySessionRequest = strictMapper.readValue(rawBody)

    private fun parseFeedbackBody(rawBody: String): SessionFeedbackRequest = strictMapper.readValue(rawBody)

    private fun extractFieldErrors(ex: JsonProcessingException): List<FieldError>? {
        if (ex !is JsonMappingException) {
            return null
        }

        val field = ex.path.joinToString(".") { it.fieldName ?: "" }.trim('.')
        if (field.isBlank()) {
            return null
        }

        val message = if (ex.originalMessage.contains("Missing required creator property")) {
            "is required"
        } else {
            ex.originalMessage
        }

        return listOf(FieldError(field, message))
    }

    private fun problemBody(
        status: HttpStatus,
        title: String,
        detail: String,
        errorCode: String,
        fieldErrors: List<FieldError>? = null
    ): ProblemResponse {
        return ProblemResponse(
            title = title,
            status = status.value(),
            detail = detail,
            errorCode = errorCode,
            fieldErrors = fieldErrors
        )
    }

    private fun resolveRequestId(request: HttpServletRequest): String {
        return request.getHeader("X-Request-Id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    }

    private fun hashUserId(userId: UUID): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toString().toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).take(16)
    }
}
