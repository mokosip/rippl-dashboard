package app.rippl.auth

import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    fun sendMagicLink(email: String) {
        val existingUser = userRepository.findByEmail(email)
        val user = if (existingUser != null) {
            log.debug("Existing user found for email: {}", email)
            existingUser
        } else {
            log.debug("Creating new user for email: {}", email)
            userRepository.save(User(email = email))
        }

        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken(email, jti)
        log.debug("Magic-link token generated for user: {}", user.id)

        authTokenRepository.save(
            AuthToken(
                userId = user.id!!,
                tokenHash = sha256(jti.toString()),
                expiresAt = Instant.now().plus(magicLinkExpiryMinutes.toLong(), ChronoUnit.MINUTES)
            )
        )

        val link = "$baseUrl/api/auth/verify?token=$token"

        if (resendApiKey.startsWith("re_test")) {
            log.info("DEV magic link: $link")
        } else {
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
        val claims = jwtService.validateMagicLinkToken(token)
        if (claims == null) {
            log.debug("Magic-link verification failed: JWT invalid")
            return null
        }
        val jti = claims.id
        if (jti == null) {
            log.debug("Magic-link verification failed: missing jti claim")
            return null
        }

        val authToken = authTokenRepository.findByTokenHash(sha256(jti))
        if (authToken == null) {
            log.debug("Magic-link verification failed: token not found in store")
            return null
        }
        if (authToken.usedAt != null) {
            log.debug("Magic-link verification failed: token already used")
            return null
        }
        if (authToken.expiresAt.isBefore(Instant.now())) {
            log.debug("Magic-link verification failed: token expired")
            return null
        }

        log.debug("Magic-link valid, marking as used for user: {}", authToken.userId)
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
