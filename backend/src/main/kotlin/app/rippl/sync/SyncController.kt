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
        if (!rateLimiter.tryAcquire(userId)) {
            log.debug("Rate limit hit for userId: {}", userId)
            return ResponseEntity.status(429).body(mapOf("error" to "rate_limit_exceeded"))
        }
        val result = syncService.sync(userId, request.sessions)
        return ResponseEntity.ok(result)
    }
}
