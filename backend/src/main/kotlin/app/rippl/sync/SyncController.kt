package app.rippl.sync

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

    @PostMapping("/sessions")
    fun syncSessions(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: SyncRequest
    ): ResponseEntity<Any> {
        if (!rateLimiter.tryAcquire(userId)) {
            return ResponseEntity.status(429).body(mapOf("error" to "rate_limit_exceeded"))
        }
        val result = syncService.sync(userId, request.sessions)
        return ResponseEntity.ok(result)
    }
}
