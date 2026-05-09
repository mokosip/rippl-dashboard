package app.rippl.ingestion

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.collectors.Collector
import app.rippl.collectors.CollectorRepository
import app.rippl.collectors.ExtensionTokenService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class EstimatedSessionStorageTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var collectorRepository: CollectorRepository
    @Autowired lateinit var extensionTokenService: ExtensionTokenService
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var objectMapper: ObjectMapper

    private lateinit var bearerToken: String
    private lateinit var currentUser: User

    @BeforeEach
    fun setup() {
        jdbc.update("DELETE FROM activity_feedback")
        jdbc.update("DELETE FROM activity_sessions")

        currentUser = userRepository.save(User(email = "est-test-${UUID.randomUUID()}@example.com"))
        val collector = collectorRepository.save(Collector(userId = currentUser.id!!, type = "chrome_extension"))
        bearerToken = extensionTokenService.createToken(collector.id!!, currentUser.id!!)
    }

    // -------------------------------------------------------------------------
    // Polling helper
    // -------------------------------------------------------------------------

    /**
     * Polls [predicate] every [intervalMs] until it returns a non-null value or
     * [timeoutMs] elapses. Returns null on timeout.
     */
    private fun <T : Any> pollUntil(
        timeoutMs: Long = 5_000,
        intervalMs: Long = 100,
        predicate: () -> T?
    ): T? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = try {
                predicate()
            } catch (_: Exception) {
                null
            }
            if (result != null) return result
            Thread.sleep(intervalMs)
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `ingest creates estimated_sessions row eventually`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-est-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        val estimatedAt = pollUntil {
            jdbc.queryForObject(
                "SELECT estimated_at FROM estimated_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            )
        }

        assertNotNull(estimatedAt) {
            "Expected an estimated_sessions row for session $sessionId but none appeared within timeout"
        }

        val rowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM estimated_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(1, rowCount) { "Expected exactly 1 estimated_sessions row" }
    }

    @Test
    fun `feedback triggers re-estimation - estimated_at moves forward and row count stays 1`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-reest-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        // Wait for initial estimation row
        val firstEstimatedAt: java.sql.Timestamp = pollUntil {
            jdbc.queryForObject(
                "SELECT estimated_at FROM estimated_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            )
        } ?: error("Initial estimated_sessions row never appeared for session $sessionId")

        // Small pause so clock advances before the feedback re-estimation
        Thread.sleep(50)

        // Submit feedback to trigger re-estimation
        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"coding"}"""
        }.andExpect { status { isOk() } }

        // Poll until estimated_at is strictly greater than the first value
        val updatedEstimatedAt = pollUntil {
            val ts = jdbc.queryForObject(
                "SELECT estimated_at FROM estimated_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            ) ?: return@pollUntil null
            if (ts.after(firstEstimatedAt)) ts else null
        }

        assertNotNull(updatedEstimatedAt) {
            "expected estimated_at to advance after feedback re-estimation for session $sessionId " +
                "(first=$firstEstimatedAt)"
        }

        val rowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM estimated_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(1, rowCount) { "Upsert must keep exactly 1 estimated_sessions row, not insert a second" }
    }

    @Test
    fun `cascade delete from activity_sessions removes estimated_sessions row`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-cascade-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        // Wait for estimation row to exist before we delete the parent
        pollUntil {
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM estimated_sessions WHERE activity_session_id = ?",
                Int::class.java,
                sessionId
            )?.takeIf { it > 0 }
        } ?: error("estimated_sessions row never appeared before cascade test for session $sessionId")

        // Delete parent row — cascade should remove estimated_sessions row
        jdbc.update("DELETE FROM activity_sessions WHERE id = ?", sessionId)

        val remaining = jdbc.queryForObject(
            "SELECT COUNT(*) FROM estimated_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(0, remaining) {
            "ON DELETE CASCADE must remove estimated_sessions row when activity_sessions row is deleted"
        }
    }

    // -------------------------------------------------------------------------
    // Payload helper
    // -------------------------------------------------------------------------

    private fun sessionPayload(
        sessionExternalId: String = "sess-${UUID.randomUUID()}",
        startedAt: Long = Instant.now().minusSeconds(120).toEpochMilli(),
        endedAt: Long = Instant.now().toEpochMilli()
    ): String = """
        {
          "collector": {"type": "chrome_extension", "version": "1.0.0"},
          "source": {"type": "browser", "version": "126"},
          "session": {
            "id": "$sessionExternalId",
            "started_at": $startedAt,
            "ended_at": $endedAt
          },
          "privacy": {
            "content_collected": false,
            "content_sent": false,
            "prompt_collected": false,
            "response_collected": false
          },
          "metrics": {"sample_metric": 1},
          "context": {"domain": "claude.ai", "mode": "legacy-sync"}
        }
    """.trimIndent()
}
