package app.rippl.collectors

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ExtensionTokenRepository : JpaRepository<ExtensionToken, UUID> {
    fun findByTokenHash(tokenHash: String): ExtensionToken?

    @Modifying
    @Transactional
    fun deleteByCollectorId(collectorId: UUID)
}
