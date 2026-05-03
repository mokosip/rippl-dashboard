package app.rippl.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.session-expiry-days}") private val sessionExpiryDays: Int,
    @Value("\${app.jwt.magic-link-expiry-minutes}") private val magicLinkExpiryMinutes: Int
) {
    private val key get() = SecretKeySpec(secret.toByteArray(), "HmacSHA256")

    fun generateSessionToken(userId: UUID): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + sessionExpiryDays * 86_400_000L))
            .signWith(key)
            .compact()

    fun validateSessionToken(token: String): UUID? =
        try {
            val claims = parse(token)
            UUID.fromString(claims.subject)
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
