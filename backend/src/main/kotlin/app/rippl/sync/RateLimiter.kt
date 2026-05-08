package app.rippl.sync

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class RateLimitDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long? = null
)

@Service
class RateLimiter(
    @Value("\${app.rate-limit.ingestion-per-minute:10}") private val refillPerMinute: Int,
    @Value("\${app.rate-limit.ingestion-burst:30}") private val burstCapacity: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val buckets = ConcurrentHashMap<UUID, TokenBucket>()
    private val refillPerMillisecond = refillPerMinute.toDouble() / 60_000.0

    fun check(userId: UUID): RateLimitDecision {
        val nowMillis = Instant.now().toEpochMilli()
        val bucket = buckets.computeIfAbsent(userId) { TokenBucket(tokens = burstCapacity.toDouble(), lastRefillAt = nowMillis) }

        synchronized(bucket) {
            val elapsed = max(0L, nowMillis - bucket.lastRefillAt)
            if (elapsed > 0) {
                bucket.tokens = min(burstCapacity.toDouble(), bucket.tokens + elapsed * refillPerMillisecond)
                bucket.lastRefillAt = nowMillis
            }

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0
                log.debug("Rate-limit allowed for userId: {} — tokensRemaining={}", userId, "%.2f".format(bucket.tokens))
                return RateLimitDecision(allowed = true)
            }

            val millisUntilNextToken = ceil((1.0 - bucket.tokens) / refillPerMillisecond).toLong()
            val retryAfter = max(1L, ceil(millisUntilNextToken / 1000.0).toLong())
            log.debug("Rate-limit denied for userId: {} — retryAfter={}s", userId, retryAfter)
            return RateLimitDecision(allowed = false, retryAfterSeconds = retryAfter)
        }
    }

    fun tryAcquire(userId: UUID): Boolean = check(userId).allowed

    private data class TokenBucket(
        var tokens: Double,
        var lastRefillAt: Long
    )
}
