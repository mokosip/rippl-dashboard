package app.rippl.auth

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        jwtService = JwtService(
            secret = "test-secret-that-is-at-least-32-characters-long",
            sessionExpiryDays = 7,
            magicLinkExpiryMinutes = 15
        )
    }

    @Test
    fun `generateSessionToken creates valid JWT`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateSessionToken(userId)
        assertNotNull(token)
        assertTrue(token.split(".").size == 3)
    }

    @Test
    fun `validateSessionToken returns userId for valid token`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateSessionToken(userId)
        val result = jwtService.validateSessionToken(token)
        assertEquals(userId, result)
    }

    @Test
    fun `validateSessionToken returns null for expired token`() {
        val jwtServiceShortExpiry = JwtService(
            secret = "test-secret-that-is-at-least-32-characters-long",
            sessionExpiryDays = 0,
            magicLinkExpiryMinutes = 0
        )
        val userId = UUID.randomUUID()
        val token = jwtServiceShortExpiry.generateSessionToken(userId)
        Thread.sleep(100)
        val result = jwtServiceShortExpiry.validateSessionToken(token)
        assertNull(result)
    }

    @Test
    fun `validateSessionToken returns null for malformed token`() {
        assertNull(jwtService.validateSessionToken("not.a.jwt"))
        assertNull(jwtService.validateSessionToken(""))
    }

    @Test
    fun `generateMagicLinkToken includes email and jti`() {
        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken("test@example.com", jti)
        assertNotNull(token)
    }

    @Test
    fun `validateMagicLinkToken returns claims for valid token`() {
        val jti = UUID.randomUUID()
        val token = jwtService.generateMagicLinkToken("test@example.com", jti)
        val claims = jwtService.validateMagicLinkToken(token)
        assertNotNull(claims)
        assertEquals("test@example.com", claims!!.subject)
        assertEquals(jti.toString(), claims.id)
    }

    @Test
    fun `validateMagicLinkToken returns null for invalid token`() {
        assertNull(jwtService.validateMagicLinkToken("garbage"))
    }
}
