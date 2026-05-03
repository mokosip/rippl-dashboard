package app.rippl.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val jwtService: JwtService,
    @Value("\${app.resend.api-key}") private val resendApiKey: String,
    @Value("\${app.resend.from}") private val fromEmail: String,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${app.jwt.magic-link-expiry-minutes}") private val magicLinkExpiryMinutes: Int
) {
    private val restClient = RestClient.create()

    fun sendMagicLink(email: String) {
        val user = userRepository.findByEmail(email)
            ?: userRepository.save(User(email = email))

        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken(email, jti)

        authTokenRepository.save(
            AuthToken(
                userId = user.id!!,
                tokenHash = sha256(jti.toString()),
                expiresAt = Instant.now().plus(magicLinkExpiryMinutes.toLong(), ChronoUnit.MINUTES)
            )
        )

        val link = "$baseUrl/api/auth/verify?token=$token"

        // In test/dev mode with api key "re_test", skip actual email sending
        if (!resendApiKey.startsWith("re_test")) {
            restClient.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer $resendApiKey")
                .header("Content-Type", "application/json")
                .body(mapOf(
                    "from" to fromEmail,
                    "to" to listOf(email),
                    "subject" to "Sign in to rippl",
                    "html" to """<p>Click <a href="$link">here</a> to sign in to rippl. This link expires in $magicLinkExpiryMinutes minutes.</p>"""
                ))
                .retrieve()
                .toBodilessEntity()
        }
    }

    fun verifyMagicLink(token: String): User? {
        val claims = jwtService.validateMagicLinkToken(token) ?: return null
        val jti = claims.id ?: return null

        val authToken = authTokenRepository.findByTokenHash(sha256(jti)) ?: return null
        if (authToken.usedAt != null) return null
        if (authToken.expiresAt.isBefore(Instant.now())) return null

        authToken.usedAt = Instant.now()
        authTokenRepository.save(authToken)

        return userRepository.findById(authToken.userId).orElse(null)
    }

    fun findById(userId: UUID): User? = userRepository.findById(userId).orElse(null)

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
