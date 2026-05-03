package app.rippl.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthTokenRepository : JpaRepository<AuthToken, UUID> {
    fun findByTokenHash(tokenHash: String): AuthToken?
}
