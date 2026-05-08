package app.rippl.collectors

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

data class ExtensionTokenPrincipal(
    val userId: UUID,
    val scope: String
)

@Service
class ExtensionTokenService(private val extensionTokenRepository: ExtensionTokenRepository) {
    private val random = SecureRandom()

    fun createToken(collectorId: UUID, userId: UUID, scope: String = "ingest"): String {
        val rawToken = generateRawToken()
        val hash = sha256(rawToken)
        extensionTokenRepository.save(
            ExtensionToken(collectorId = collectorId, userId = userId, tokenHash = hash, scope = scope)
        )
        return rawToken
    }

    fun validateToken(rawToken: String): ExtensionTokenPrincipal? {
        val hash = sha256(rawToken)
        val token = extensionTokenRepository.findByTokenHash(hash) ?: return null
        val scope = token.scope.ifBlank { "ingest" }
        return ExtensionTokenPrincipal(userId = token.userId, scope = scope)
    }

    fun revokeByCollector(collectorId: UUID) {
        extensionTokenRepository.deleteByCollectorId(collectorId)
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return "rpl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(input.toByteArray()))
    }
}
