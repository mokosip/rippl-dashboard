package app.rippl.sync

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class SyncControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var sessionCookie: Cookie

    @BeforeEach
    fun setup() {
        val user = userRepository.findByEmail("sync-test@example.com")
            ?: userRepository.save(User(email = "sync-test@example.com"))
        sessionCookie = Cookie("session", jwtService.generateSessionToken(user.id!!))
    }

    @Test
    fun `POST sync sessions returns accepted count`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = """
            {
              "sessions": [{
                "id": "sess-1714700000000-a3f8k2",
                "domain": "claude.ai",
                "startedAt": 1714700000000,
                "endedAt": 1714700720000,
                "activeSeconds": 680,
                "date": "2026-05-03",
                "activityType": "coding",
                "estimatedWithoutMinutes": 30,
                "timeSavedMinutes": 19,
                "logged": true
              }]
            }
            """
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(1) }
            jsonPath("$.duplicates") { value(0) }
        }
    }

    @Test
    fun `POST sync sessions deduplicates existing sessions`() {
        val body = """
        {
          "sessions": [{
            "id": "sess-dedup-test",
            "domain": "claude.ai",
            "startedAt": 1714700000000,
            "endedAt": 1714700720000,
            "activeSeconds": 680,
            "date": "2026-05-03"
          }]
        }
        """

        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = body
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(0) }
            jsonPath("$.duplicates") { value(1) }
        }
    }

    @Test
    fun `POST sync sessions returns 401 without auth`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sessions": []}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
