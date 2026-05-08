package app.rippl.sync

import app.rippl.ingestion.ActivitySessionRequest
import app.rippl.ingestion.CollectorPayload
import app.rippl.ingestion.IngestionService
import app.rippl.ingestion.PrivacyPayload
import app.rippl.ingestion.SessionPayload
import app.rippl.ingestion.SourcePayload
import app.rippl.sessions.SessionUpsertRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SyncService(
    private val sessionUpsertRepository: SessionUpsertRepository,
    private val ingestionService: IngestionService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sync(userId: UUID, sessions: List<SyncSessionDto>): SyncResponse {
        log.debug("Sync started for userId: {}, sessions: {}", userId, sessions.size)

        var accepted = 0
        var duplicates = 0

        sessions.forEach { legacySession ->
            val request = legacySession.toIngestionRequest()
            val rawPayload = objectMapper.writeValueAsString(request)
            val result = ingestionService.ingest(userId, request, rawPayload)
            if (result.deduped) duplicates++ else accepted++
        }

        // Keep legacy analytics path alive while v1 ingestion rolls out.
        sessionUpsertRepository.upsertSessions(userId, sessions)

        log.debug("Sync complete for userId: {} — accepted: {}, duplicates: {}", userId, accepted, duplicates)
        return SyncResponse(accepted, duplicates, System.currentTimeMillis())
    }

    private fun SyncSessionDto.toIngestionRequest(): ActivitySessionRequest {
        return ActivitySessionRequest(
            collector = CollectorPayload(type = "chrome_extension", version = "legacy"),
            source = SourcePayload(type = "legacy_sync", version = "v1"),
            session = SessionPayload(
                id = id,
                startedAt = startedAt,
                endedAt = endedAt
            ),
            privacy = PrivacyPayload(
                contentCollected = false,
                contentSent = false,
                promptCollected = false,
                responseCollected = false
            ),
            metrics = mapOf(
                "active_seconds" to activeSeconds,
                "estimated_without_minutes" to estimatedWithoutMinutes,
                "time_saved_minutes" to timeSavedMinutes
            ),
            context = mapOf(
                "domain" to domain,
                "date" to date.toString(),
                "activity_type" to (activityType ?: emptyList()),
                "logged" to (logged ?: false)
            )
        )
    }
}
