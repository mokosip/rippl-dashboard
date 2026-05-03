package app.rippl.collectors

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateCollectorRequest(val type: String)

@RestController
@RequestMapping("/api/collectors")
class CollectorsController(private val collectorRepository: CollectorRepository) {

    @GetMapping
    fun list(@AuthenticationPrincipal userId: UUID): List<Map<String, Any?>> =
        collectorRepository.findByUserId(userId).map { it.toDto() }

    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: CreateCollectorRequest
    ): Map<String, Any?> {
        val collector = collectorRepository.save(Collector(userId = userId, type = request.type))
        return collector.toDto()
    }

    @DeleteMapping("/{id}")
    fun delete(@AuthenticationPrincipal userId: UUID, @PathVariable id: UUID): ResponseEntity<Void> {
        val collector = collectorRepository.findByIdAndUserId(id, userId)
            ?: return ResponseEntity.notFound().build()
        collectorRepository.delete(collector)
        return ResponseEntity.ok().build()
    }

    private fun Collector.toDto() = mapOf(
        "id" to id,
        "type" to type,
        "enabled" to enabled,
        "linkedAt" to linkedAt.toString()
    )
}
