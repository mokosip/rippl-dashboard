package app.rippl.sync

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimiter(@Value("\${app.rate-limit.sync-per-minute}") private val maxPerMinute: Int) {
    private val requests = ConcurrentHashMap<UUID, MutableList<Instant>>()

    fun tryAcquire(userId: UUID): Boolean {
        val now = Instant.now()
        val userRequests = requests.computeIfAbsent(userId) { mutableListOf() }
        synchronized(userRequests) {
            userRequests.removeIf { it.isBefore(now.minusSeconds(60)) }
            if (userRequests.size >= maxPerMinute) return false
            userRequests.add(now)
            return true
        }
    }
}
