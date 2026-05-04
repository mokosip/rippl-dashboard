package app.rippl.sync

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.collectors.Collector
import app.rippl.collectors.CollectorRepository
import app.rippl.collectors.ExtensionTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var collectorRepository: CollectorRepository
    @Autowired lateinit var extensionTokenService: ExtensionTokenService

    private lateinit var bearerToken: String

    @BeforeEach
    fun setup() {
        val user = userRepository.findByEmail("sync-test@example.com")
            ?: userRepository.save(User(email = "sync-test@example.com"))
        val collector = collectorRepository.findByUserId(user.id!!).firstOrNull()
            ?: collectorRepository.save(Collector(userId = user.id!!, type = "chrome_extension"))
        extensionTokenService.revokeByCollector(collector.id!!)
        bearerToken = extensionTokenService.createToken(collector.id!!, user.id!!)
    }

    @Test
    fun `POST sync sessions returns accepted count`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
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
            header("Authorization", "Bearer $bearerToken")
            content = body
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(0) }
            jsonPath("$.duplicates") { value(1) }
        }
    }

    @Test
    fun `POST sync sessions accepts activityType as array`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """
            {
              "sessions": [{
                "id": "sess-array-activity",
                "domain": "claude.ai",
                "startedAt": 1714700000000,
                "endedAt": 1714700720000,
                "activeSeconds": 680,
                "date": "2026-05-03",
                "activityType": ["coding", "review"],
                "timeSavedMinutes": 15
              }]
            }
            """
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(1) }
        }
    }

    @Test
    fun `POST sync sessions returns 200 for empty sessions`() {
        mockMvc.post("/api/sync/sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"sessions": []}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accepted") { value(0) }
            jsonPath("$.duplicates") { value(0) }
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
