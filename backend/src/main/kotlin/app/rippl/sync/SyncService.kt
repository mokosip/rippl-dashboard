package app.rippl.sync

import app.rippl.sessions.SessionUpsertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SyncService(private val sessionUpsertRepository: SessionUpsertRepository) {

    @Transactional
    fun sync(userId: UUID, sessions: List<SyncSessionDto>): SyncResponse {
        val result = sessionUpsertRepository.upsertSessions(userId, sessions)
        return SyncResponse(result.accepted, result.duplicates, result.syncedAt)
    }
}
