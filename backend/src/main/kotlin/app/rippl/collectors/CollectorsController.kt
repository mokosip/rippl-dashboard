package app.rippl.collectors

import app.rippl.ingestion.IngestionRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateCollectorRequest(val type: String)

@RestController
@RequestMapping("/api/collectors")
class CollectorsController(
    private val collectorRepository: CollectorRepository,
    private val extensionTokenService: ExtensionTokenService,
    private val ingestionRepository: IngestionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(@AuthenticationPrincipal userId: UUID): List<Map<String, Any?>> {
        log.debug("Listing collectors for userId: {}", userId)
        val lastSync = ingestionRepository.findLastSyncedAt(userId)
        return collectorRepository.findByUserId(userId).map { it.toDto(lastSync) }
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: CreateCollectorRequest
    ): Map<String, Any?> {
        log.debug("Creating collector type: {} for userId: {}", request.type, userId)
        val collector = collectorRepository.save(Collector(userId = userId, type = request.type))
        val token = extensionTokenService.createToken(collectorId = collector.id!!, userId = userId)
        return collector.toDto() + ("token" to token)
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun delete(@AuthenticationPrincipal userId: UUID, @PathVariable id: UUID): ResponseEntity<Void> {
        log.debug("Deleting collector id: {} for userId: {}", id, userId)
        val collector = collectorRepository.findByIdAndUserId(id, userId)
            ?: return ResponseEntity.notFound().build()
        extensionTokenService.revokeByCollector(collector.id!!)
        collectorRepository.delete(collector)
        return ResponseEntity.ok().build()
    }

    private fun Collector.toDto(lastSyncAt: java.time.Instant? = null) = mapOf(
        "id" to id,
        "type" to type,
        "enabled" to enabled,
        "linkedAt" to linkedAt.toString(),
        "lastSyncAt" to lastSyncAt?.toString()
    )
}
