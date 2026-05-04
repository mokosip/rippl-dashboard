package app.rippl.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

data class SessionClaims(val userId: UUID, val issuedAt: Instant)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.session-expiry-days}") private val sessionExpiryDays: Int,
    @Value("\${app.jwt.magic-link-expiry-minutes}") private val magicLinkExpiryMinutes: Int
) {
    @PostConstruct
    fun validateSecret() {
        require(secret.toByteArray().size >= 32) { "JWT_SECRET must be at least 32 bytes" }
    }

    private val key get() = SecretKeySpec(secret.toByteArray(), "HmacSHA256")

    fun generateSessionToken(userId: UUID): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + sessionExpiryDays * 86_400_000L))
            .signWith(key)
            .compact()

    fun validateSessionToken(token: String): SessionClaims? =
        try {
            val claims = parse(token)
            SessionClaims(
                userId = UUID.fromString(claims.subject),
                issuedAt = claims.issuedAt.toInstant()
            )
        } catch (_: Exception) {
            null
        }

    fun generateMagicLinkToken(email: String, jti: UUID): String =
        Jwts.builder()
            .subject(email)
            .id(jti.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + magicLinkExpiryMinutes * 60_000L))
            .signWith(key)
            .compact()

    fun validateMagicLinkToken(token: String): Claims? =
        try {
            parse(token)
        } catch (_: Exception) {
            null
        }

    private fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
