package app.rippl.sync

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimiter(@Value("\${app.rate-limit.sync-per-minute}") private val maxPerMinute: Int) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val requests = ConcurrentHashMap<UUID, MutableList<Instant>>()

    fun tryAcquire(userId: UUID): Boolean {
        val now = Instant.now()
        val userRequests = requests.computeIfAbsent(userId) { mutableListOf() }
        synchronized(userRequests) {
            userRequests.removeIf { it.isBefore(now.minusSeconds(60)) }
            val count = userRequests.size
            log.debug("Rate-limit check for userId: {} — current count: {}/{}", userId, count, maxPerMinute)
            if (count >= maxPerMinute) {
                log.debug("Rate-limit denied for userId: {}", userId)
                return false
            }
            userRequests.add(now)
            log.debug("Rate-limit allowed for userId: {}", userId)
            return true
        }
    }
}
