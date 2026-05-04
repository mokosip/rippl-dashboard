package app.rippl.auth

import app.rippl.TestcontainersConfig
import app.rippl.collectors.Collector
import app.rippl.collectors.CollectorRepository
import app.rippl.collectors.ExtensionTokenService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class AuthControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var collectorRepository: CollectorRepository
    @Autowired lateinit var extensionTokenService: ExtensionTokenService

    @Test
    fun `POST magic-link returns 200 for valid email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "test@example.com"}"""
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST magic-link returns 400 for missing email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET me returns 401 when not authenticated`() {
        mockMvc.get("/api/auth/me")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET me returns user when authenticated`() {
        val user = userRepository.findByEmail("auth-test@example.com")
            ?: userRepository.save(User(email = "auth-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.get("/api/auth/me") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("auth-test@example.com") }
        }
    }

    @Test
    fun `POST logout clears session cookie`() {
        val user = userRepository.findByEmail("logout-test@example.com")
            ?: userRepository.save(User(email = "logout-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.post("/api/auth/logout") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            cookie { maxAge("session", 0) }
        }
    }

    @Test
    fun `POST invalidate-sessions returns 200 and invalidates`() {
        val user = userRepository.findByEmail("invalidate-test@example.com")
            ?: userRepository.save(User(email = "invalidate-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.post("/api/auth/invalidate-sessions") {
            cookie(Cookie("session", token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.invalidated") { value(true) }
        }
    }

    @Test
    fun `session rejected after invalidation`() {
        val user = userRepository.findByEmail("invalidated-session-test@example.com")
            ?: userRepository.save(User(email = "invalidated-session-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        user.sessionsInvalidatedAt = Instant.now()
        userRepository.save(user)

        mockMvc.get("/api/auth/me") {
            cookie(Cookie("session", token))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `new session works after invalidation`() {
        val user = userRepository.findByEmail("new-session-after-invalidation-test@example.com")
            ?: userRepository.save(User(email = "new-session-after-invalidation-test@example.com"))

        user.sessionsInvalidatedAt = Instant.now().minusSeconds(5)
        userRepository.save(user)

        val newToken = jwtService.generateSessionToken(user.id!!)

        mockMvc.get("/api/auth/me") {
            cookie(Cookie("session", newToken))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `bearer token unaffected by session invalidation`() {
        val user = userRepository.findByEmail("bearer-invalidation-test@example.com")
            ?: userRepository.save(User(email = "bearer-invalidation-test@example.com"))

        val collector = collectorRepository.save(Collector(userId = user.id!!, type = "chrome_extension"))
        val rawToken = extensionTokenService.createToken(collector.id!!, user.id!!)

        user.sessionsInvalidatedAt = Instant.now()
        userRepository.save(user)

        mockMvc.get("/api/auth/me") {
            header("Authorization", "Bearer $rawToken")
        }.andExpect {
            status { isOk() }
        }
    }
}
