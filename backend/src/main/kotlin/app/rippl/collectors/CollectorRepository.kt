package app.rippl.collectors

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CollectorRepository : JpaRepository<Collector, UUID> {
    fun findByUserId(userId: UUID): List<Collector>
    fun findByIdAndUserId(id: UUID, userId: UUID): Collector?
    fun deleteByUserId(userId: UUID)
}
