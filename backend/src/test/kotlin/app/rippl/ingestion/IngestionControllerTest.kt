package app.rippl.ingestion

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.collectors.Collector
import app.rippl.collectors.CollectorRepository
import app.rippl.collectors.ExtensionTokenService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class IngestionControllerTest {

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

        currentUser = userRepository.save(User(email = "ingestion-test-${UUID.randomUUID()}@example.com"))
        val collector = collectorRepository.save(Collector(userId = currentUser.id!!, type = "chrome_extension"))
        bearerToken = extensionTokenService.createToken(collector.id!!, currentUser.id!!)
    }

    @Test
    fun `POST v1 activity sessions accepts valid payload`() {
        val response = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = "sess-${UUID.randomUUID()}")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accepted") { value(true) }
            jsonPath("$.feedback_request.ask") { value(false) }
        }.andReturn()

        val sessionId = objectMapper.readTree(response.response.contentAsString).get("session_id").asText()
        assertTrue(sessionId.isNotBlank())
    }

    @Test
    fun `POST v1 activity sessions dedupes and keeps immutable raw payload`() {
        val externalId = "sess-dedup-${UUID.randomUUID()}"

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = externalId, metricValue = 1)
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = externalId, metricValue = 999)
        }.andExpect {
            status { isOk() }
            jsonPath("$.deduped") { value(true) }
        }

        val rowCount = jdbc.queryForObject("SELECT COUNT(*) FROM activity_sessions", Int::class.java) ?: 0
        assertEquals(1, rowCount)

        val storedMetric = jdbc.queryForObject(
            "SELECT raw_payload -> 'metrics' ->> 'sample_metric' FROM activity_sessions WHERE collector_session_id = ?",
            String::class.java,
            externalId
        )
        assertEquals("1", storedMetric)
    }

    @Test
    fun `POST v1 activity sessions dedupe advances synced_at and keeps raw payload immutable`() {
        val externalId = "sess-syncedat-${UUID.randomUUID()}"

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = externalId, metricValue = 42)
        }.andExpect { status { isCreated() } }

        val syncedAtBefore = jdbc.queryForObject(
            "SELECT synced_at FROM activity_sessions WHERE collector_session_id = ?",
            java.sql.Timestamp::class.java,
            externalId
        )!!

        Thread.sleep(20) // ensure DB clock advances between the two writes

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = externalId, metricValue = 999)
        }.andExpect {
            status { isOk() }
            jsonPath("$.deduped") { value(true) }
        }

        val syncedAtAfter = jdbc.queryForObject(
            "SELECT synced_at FROM activity_sessions WHERE collector_session_id = ?",
            java.sql.Timestamp::class.java,
            externalId
        )!!

        assertTrue(syncedAtAfter.after(syncedAtBefore), "synced_at must advance on dedupe retry")

        val storedMetric = jdbc.queryForObject(
            "SELECT raw_payload -> 'metrics' ->> 'sample_metric' FROM activity_sessions WHERE collector_session_id = ?",
            String::class.java,
            externalId
        )
        assertEquals("42", storedMetric, "raw_payload must remain immutable on dedupe")
    }

    @Test
    fun `POST v1 activity sessions rejects unknown core fields`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(extraCollectorField = true)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("invalid_schema") }
        }
    }

    @Test
    fun `POST v1 activity sessions accepts unknown metrics and context fields`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayloadWithFlexibleMetricsAndContext()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accepted") { value(true) }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects missing session id`() {
        val payloadMissingSessionId = """
            {
              "collector": {"type": "chrome_extension", "version": "1.0.0"},
              "source": {"type": "browser", "version": "126"},
              "session": {
                "started_at": ${Instant.now().minusSeconds(20).toEpochMilli()},
                "ended_at": ${Instant.now().toEpochMilli()}
              },
              "privacy": {
                "content_collected": false,
                "content_sent": false,
                "prompt_collected": false,
                "response_collected": false
              },
              "metrics": {},
              "context": {}
            }
        """.trimIndent()

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = payloadMissingSessionId
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("invalid_schema") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects missing context domain`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload().replace(""""domain": "claude.ai",""", "")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("validation_error") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects missing context surface`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload().replace(""""surface": "web",""", "")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("validation_error") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects missing metrics duration_ms`() {
        val payload = """
            {
              "collector": {"type": "chrome_extension", "version": "1.0.0"},
              "source": {"type": "browser", "version": "126"},
              "session": {
                "id": "sess-${UUID.randomUUID()}",
                "started_at": ${Instant.now().minusSeconds(120).toEpochMilli()},
                "ended_at": ${Instant.now().toEpochMilli()}
              },
              "privacy": {
                "content_collected": false,
                "content_sent": false,
                "prompt_collected": false,
                "response_collected": false
              },
              "metrics": {"active_ms": 5000},
              "context": {"domain": "claude.ai", "surface": "web"}
            }
        """.trimIndent()

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("validation_error") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects missing metrics active_ms`() {
        val payload = """
            {
              "collector": {"type": "chrome_extension", "version": "1.0.0"},
              "source": {"type": "browser", "version": "126"},
              "session": {
                "id": "sess-${UUID.randomUUID()}",
                "started_at": ${Instant.now().minusSeconds(120).toEpochMilli()},
                "ended_at": ${Instant.now().toEpochMilli()}
              },
              "privacy": {
                "content_collected": false,
                "content_sent": false,
                "prompt_collected": false,
                "response_collected": false
              },
              "metrics": {"duration_ms": 5000},
              "context": {"domain": "claude.ai", "surface": "web"}
            }
        """.trimIndent()

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("validation_error") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects policy violations`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(contentCollected = true)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("policy_violation") }
        }
    }

    @Test
    fun `POST v1 activity sessions rejects invalid temporal invariants`() {
        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(startedAt = 2000, endedAt = 1000)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error_code") { value("validation_error") }
        }
    }

    @Test
    fun `POST v1 activity sessions requires ingest scope`() {
        val scopedCollector = collectorRepository.save(Collector(userId = currentUser.id!!, type = "cli"))
        val nonIngestToken = extensionTokenService.createToken(scopedCollector.id!!, currentUser.id!!, scope = "dashboard")

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $nonIngestToken")
            content = validPayload(sessionExternalId = "sess-${UUID.randomUUID()}")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST v1 activity sessions enforces payload cap`() {
        val oversizedContext = "x".repeat(140_000)
        val payload = validPayload().replace("\"legacy-sync\"", "\"$oversizedContext\"")

        mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = payload
        }.andExpect {
            status { isPayloadTooLarge() }
            jsonPath("$.error_code") { value("payload_too_large") }
        }
    }

    @Test
    fun `POST v1 activity sessions feedback enforces payload cap`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = "sess-feedback-cap-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()
        val oversizedValue = "x".repeat(140_000)

        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"$oversizedValue"}"""
        }.andExpect {
            status { isPayloadTooLarge() }
            jsonPath("$.error_code") { value("payload_too_large") }
        }
    }

    @Test
    fun `POST v1 activity sessions feedback upserts by type`() {
        val ingestResponse = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = "sess-feedback-${UUID.randomUUID()}")
        }.andExpect { status { isCreated() } }.andReturn()

        val sessionId = objectMapper.readTree(ingestResponse.response.contentAsString).get("session_id").asText()

        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"coding"}"""
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/v1/activity-sessions/$sessionId/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"task_type","value":"research"}"""
        }.andExpect {
            status { isOk() }
        }

        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM activity_feedback WHERE session_id = ? AND feedback_type = 'task_type'",
            Int::class.java,
            UUID.fromString(sessionId)
        ) ?: 0
        assertEquals(1, count)
    }

    @Test
    fun `POST v1 activity sessions feedback returns 404 for unknown session`() {
        mockMvc.post("/v1/activity-sessions/${UUID.randomUUID()}/feedback") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = """{"type":"accuracy","value":0.9}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error_code") { value("session_not_found") }
        }
    }

    @Test
    fun `POST v1 activity sessions rate limit applies burst and returns retry-after`() {
        val statuses = mutableListOf<Int>()

        repeat(35) { idx ->
            val response = mockMvc.post("/v1/activity-sessions") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $bearerToken")
                content = validPayload(sessionExternalId = "sess-rate-$idx-${UUID.randomUUID()}")
            }.andReturn()

            statuses.add(response.response.status)
        }

        assertTrue(statuses.contains(429))

        val throttled = mockMvc.post("/v1/activity-sessions") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bearerToken")
            content = validPayload(sessionExternalId = "sess-rate-last-${UUID.randomUUID()}")
        }.andReturn()

        if (throttled.response.status == 429) {
            assertTrue((throttled.response.getHeader("Retry-After") ?: "").isNotBlank())
        }
    }

    private fun validPayload(
        sessionExternalId: String = "sess-${UUID.randomUUID()}",
        startedAt: Long = Instant.now().minusSeconds(120).toEpochMilli(),
        endedAt: Long = Instant.now().toEpochMilli(),
        contentCollected: Boolean = false,
        metricValue: Int = 1,
        extraCollectorField: Boolean = false
    ): String {
        val extra = if (extraCollectorField) ",\"unexpected\":\"nope\"" else ""
        return """
            {
              "collector": {"type": "chrome_extension", "version": "1.0.0"$extra},
              "source": {"type": "browser", "version": "126"},
              "session": {
                "id": "$sessionExternalId",
                "started_at": $startedAt,
                "ended_at": $endedAt
              },
              "privacy": {
                "content_collected": $contentCollected,
                "content_sent": false,
                "prompt_collected": false,
                "response_collected": false
              },
              "metrics": {
                "sample_metric": $metricValue
              },
              "context": {
                "domain": "claude.ai",
                "mode": "legacy-sync"
              }
            }
        """.trimIndent()
    }

    private fun validPayloadWithFlexibleMetricsAndContext(): String {
        return """
            {
              "collector": {"type": "chrome_extension", "version": "1.0.0"},
              "source": {"type": "browser", "version": "126"},
              "session": {
                "id": "sess-flex-${UUID.randomUUID()}",
                "started_at": ${Instant.now().minusSeconds(30).toEpochMilli()},
                "ended_at": ${Instant.now().toEpochMilli()}
              },
              "privacy": {
                "content_collected": false,
                "content_sent": false,
                "prompt_collected": false,
                "response_collected": false
              },
              "metrics": {
                "totally_new_metric": {
                  "deep": {"number": 7}
                }
              },
              "context": {
                "extra": {
                  "nested": ["a", "b"]
                }
              }
            }
        """.trimIndent()
    }
}
