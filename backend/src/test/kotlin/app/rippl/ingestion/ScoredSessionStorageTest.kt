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
class ScoredSessionStorageTest {

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

    @Test
    fun `ingest creates scored_sessions row eventually`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-est-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        val scoredAt = pollUntil {
            jdbc.queryForObject(
                "SELECT scored_at FROM scored_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            )
        }

        assertNotNull(scoredAt) {
            "Expected a scored_sessions row for session $sessionId but none appeared within timeout"
        }

        val rowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM scored_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(1, rowCount) { "Expected exactly 1 scored_sessions row" }
    }

    @Test
    fun `feedback triggers re-scoring - scored_at moves forward and row count stays 1`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-reest-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        val firstScoredAt: java.sql.Timestamp = pollUntil {
            jdbc.queryForObject(
                "SELECT scored_at FROM scored_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            )
        } ?: error("Initial scored_sessions row never appeared for session $sessionId")

        Thread.sleep(50)

        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"coding"}"""
        }.andExpect { status { isOk() } }

        val updatedScoredAt = pollUntil {
            val ts = jdbc.queryForObject(
                "SELECT scored_at FROM scored_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            ) ?: return@pollUntil null
            if (ts.after(firstScoredAt)) ts else null
        }

        assertNotNull(updatedScoredAt) {
            "expected scored_at to advance after feedback re-scoring for session $sessionId " +
                "(first=$firstScoredAt)"
        }

        val rowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM scored_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(1, rowCount) { "Upsert must keep exactly 1 scored_sessions row, not insert a second" }
    }

    @Test
    fun `cascade delete from activity_sessions removes scored_sessions row`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(sessionExternalId = "sess-cascade-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        pollUntil {
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM scored_sessions WHERE activity_session_id = ?",
                Int::class.java,
                sessionId
            )?.takeIf { it > 0 }
        } ?: error("scored_sessions row never appeared before cascade test for session $sessionId")

        jdbc.update("DELETE FROM activity_sessions WHERE id = ?", sessionId)

        val remaining = jdbc.queryForObject(
            "SELECT COUNT(*) FROM scored_sessions WHERE activity_session_id = ?",
            Int::class.java,
            sessionId
        )
        assertEquals(0, remaining) {
            "ON DELETE CASCADE must remove scored_sessions row when activity_sessions row is deleted"
        }
    }

    @Test
    fun `scoring produces real values not placeholders`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(
                sessionExternalId = "sess-real-${UUID.randomUUID()}",
                startedAt = Instant.now().minusSeconds(1200).toEpochMilli(),
                endedAt = Instant.now().toEpochMilli()
            )
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        val scored = pollUntil {
            jdbc.query(
                """
                SELECT effective_multiplier, estimated_time_saved_ms, confidence, scoring_method, inferred_task_mix
                FROM scored_sessions WHERE activity_session_id = ?
                """,
                { rs, _ ->
                    mapOf(
                        "multiplier" to rs.getDouble("effective_multiplier"),
                        "saved" to rs.getLong("estimated_time_saved_ms"),
                        "confidence" to rs.getString("confidence"),
                        "method" to rs.getString("scoring_method"),
                        "task_mix" to rs.getString("inferred_task_mix")
                    )
                },
                sessionId
            ).firstOrNull()
        } ?: error("scored_sessions row never appeared for session $sessionId")

        val multiplier = scored["multiplier"] as Double
        val saved = scored["saved"] as Long
        val method = scored["method"] as String
        val taskMix = scored["task_mix"] as String

        assert(multiplier > 1.0) { "effective_multiplier should be > 1.0, was $multiplier" }
        assert(saved > 0) { "estimated_time_saved_ms should be > 0 for a 20min session, was $saved" }
        assertEquals("global_fallback", method) { "no profile or feedback, should be global_fallback" }
        assert(!taskMix.contains("unknown")) { "task_mix should not contain 'unknown' placeholder, was $taskMix" }
    }

    @Test
    fun `feedback rescoring updates method to feedback_adjusted`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = sessionPayload(
                sessionExternalId = "sess-fb-${UUID.randomUUID()}",
                startedAt = Instant.now().minusSeconds(600).toEpochMilli(),
                endedAt = Instant.now().toEpochMilli()
            )
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = UUID.fromString(
            objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        )

        pollUntil {
            jdbc.queryForObject(
                "SELECT scored_at FROM scored_sessions WHERE activity_session_id = ?",
                java.sql.Timestamp::class.java,
                sessionId
            )
        } ?: error("Initial scoring never completed for session $sessionId")

        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"coding"}"""
        }.andExpect { status { isOk() } }

        val method = pollUntil {
            val m = jdbc.queryForObject(
                "SELECT scoring_method FROM scored_sessions WHERE activity_session_id = ?",
                String::class.java,
                sessionId
            )
            if (m == "feedback_adjusted") m else null
        }

        assertEquals("feedback_adjusted", method) {
            "After task_type feedback, scoring_method should be feedback_adjusted"
        }

        val multiplier = jdbc.queryForObject(
            "SELECT effective_multiplier FROM scored_sessions WHERE activity_session_id = ?",
            Double::class.java,
            sessionId
        )
        assertEquals(1.7, multiplier!!, 0.001) { "coding feedback should produce coding multiplier 1.7" }
    }

    private fun sessionPayload(
        sessionExternalId: String = "sess-${UUID.randomUUID()}",
        startedAt: Long = Instant.now().minusSeconds(120).toEpochMilli(),
        endedAt: Long = Instant.now().toEpochMilli()
    ): String {
        val durationMs = endedAt - startedAt
        return """
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
              "metrics": {
                "duration_ms": $durationMs,
                "active_ms": $durationMs,
                "sample_metric": 1
              },
              "context": {
                "domain": "claude.ai",
                "surface": "web",
                "mode": "legacy-sync"
              }
            }
        """.trimIndent()
    }
}
