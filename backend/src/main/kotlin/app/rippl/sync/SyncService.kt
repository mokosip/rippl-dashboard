package app.rippl.sync

import app.rippl.sessions.SessionUpsertRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SyncService(private val sessionUpsertRepository: SessionUpsertRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sync(userId: UUID, sessions: List<SyncSessionDto>): SyncResponse {
        log.debug("Sync started for userId: {}, sessions: {}", userId, sessions.size)
        val result = sessionUpsertRepository.upsertSessions(userId, sessions)
        log.debug("Sync complete for userId: {} — accepted: {}, duplicates: {}", userId, result.accepted, result.duplicates)
        return SyncResponse(result.accepted, result.duplicates, result.syncedAt)
    }
}
