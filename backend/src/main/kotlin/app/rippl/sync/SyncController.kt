package app.rippl.sync

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncService: SyncService,
    private val rateLimiter: RateLimiter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/sessions")
    fun syncSessions(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: SyncRequest
    ): ResponseEntity<Any> {
        log.debug("Sync request from userId: {}, session count: {}", userId, request.sessions.size)
        if (request.sessions.isEmpty()) {
            return ResponseEntity.ok(SyncResponse(0, 0, System.currentTimeMillis()))
        }
        val rateLimit = rateLimiter.check(userId)
        if (!rateLimit.allowed) {
            log.debug("Rate limit hit for userId: {}", userId)
            return ResponseEntity.status(429)
                .header("Retry-After", (rateLimit.retryAfterSeconds ?: 1).toString())
                .body(mapOf("error" to "rate_limit_exceeded"))
        }
        val result = syncService.sync(userId, request.sessions)
        return ResponseEntity.ok(result)
    }
}
