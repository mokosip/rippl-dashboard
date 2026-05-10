package app.rippl.trends

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class TrendsServiceTest {

    @Autowired lateinit var trendsService: TrendsService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var userId: UUID

    private fun insertActivitySession(
        id: UUID, userId: UUID, domain: String, surface: String,
        startedAt: Instant, endedAt: Instant, durationMs: Long, activeMs: Long
    ) {
        jdbc.update(
            """
            INSERT INTO activity_sessions (id, user_id, collector_type, collector_session_id,
                source_type, domain, surface, raw_payload, started_at, ended_at, duration_ms, active_ms)
            VALUES (?, ?, 'browser', ?, 'extension', ?, ?, ?::jsonb, ?, ?, ?, ?)
            """,
            id, userId, UUID.randomUUID().toString(), domain, surface, "{}",
            java.sql.Timestamp.from(startedAt), java.sql.Timestamp.from(endedAt),
            durationMs, activeMs
        )
    }

    private fun insertScoredSession(
        activitySessionId: UUID, userId: UUID,
        taskMixJson: String, multiplier: Double, savedMs: Long,
        confidence: String, method: String
    ) {
        jdbc.update(
            """
            INSERT INTO scored_sessions (activity_session_id, user_id, inferred_task_mix,
                effective_multiplier, estimated_time_saved_ms, confidence, scoring_method, scored_at)
            VALUES (?, ?, ?::jsonb, ?, ?, ?::scoring_confidence, ?::scoring_method, now())
            """,
            activitySessionId, userId, taskMixJson, multiplier, savedMs, confidence, method
        )
    }

    @BeforeEach
    fun setup() {
        jdbc.update("DELETE FROM scored_sessions")
        jdbc.update("DELETE FROM activity_sessions")
        val user = userRepository.findByEmail("trends-test@example.com")
            ?: userRepository.save(User(email = "trends-test@example.com"))
        userId = user.id!!

        // Session 1: claude.ai, 2026-04-28, 10 min active, scored 19 min saved (high)
        val s1 = UUID.randomUUID()
        val april28 = Instant.parse("2026-04-28T10:00:00Z")
        insertActivitySession(s1, userId, "claude.ai", "web", april28, april28.plusSeconds(1200), 1200000, 600000)
        insertScoredSession(s1, userId, """{"coding":0.7,"research":0.3}""", 1.9, 19 * 60000L, "high", "profile_default")

        // Session 2: chatgpt.com, 2026-04-28, 5 min active, scored 8 min saved (medium)
        val s2 = UUID.randomUUID()
        insertActivitySession(s2, userId, "chatgpt.com", "web", april28.plusSeconds(3600), april28.plusSeconds(4200), 600000, 300000)
        insertScoredSession(s2, userId, """{"writing":0.8,"other":0.2}""", 1.6, 8 * 60000L, "medium", "profile_default")

        // Session 3: claude.ai, 2026-05-01, 15 min active, scored 25 min saved (high)
        val s3 = UUID.randomUUID()
        val may1 = Instant.parse("2026-05-01T14:00:00Z")
        insertActivitySession(s3, userId, "claude.ai", "web", may1, may1.plusSeconds(1800), 1800000, 900000)
        insertScoredSession(s3, userId, """{"coding":0.5,"planning":0.5}""", 1.7, 25 * 60000L, "high", "feedback_adjusted")
    }

    @Test
    fun `weekly returns data grouped by week and domain`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
        val claudeEntries = result.filter { it.domain == "claude.ai" }
        assertTrue(claudeEntries.isNotEmpty())
        assertTrue(claudeEntries.all { it.totalSaved > 0 })
    }

    @Test
    fun `weekly includes confidence from scored sessions`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.all { it.confidence in listOf("low", "medium", "high") })
    }

    @Test
    fun `monthly returns data grouped by month and domain`() {
        val result = trendsService.monthly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `timeSaved returns correct totals`() {
        val result = trendsService.timeSaved(userId)
        assertEquals(52, result.total) // 19 + 8 + 25
        assertEquals(44, result.byDomain["claude.ai"]) // 19 + 25
        assertEquals(8, result.byDomain["chatgpt.com"])
    }

    @Test
    fun `timeSaved returns byTaskMix with weighted minutes`() {
        val result = trendsService.timeSaved(userId)
        // coding: 0.7*19 + 0.5*25 = 13.3 + 12.5 = 25.8 → 25 min
        // research: 0.3*19 = 5.7 → 5 min
        // writing: 0.8*8 = 6.4 → 6 min
        // planning: 0.5*25 = 12.5 → 12 min
        // other: 0.2*8 = 1.6 → 1 min
        assertTrue(result.byTaskMix.containsKey("coding"))
        assertTrue(result.byTaskMix.containsKey("writing"))
        assertTrue(result.byTaskMix["coding"]!! > result.byTaskMix["writing"]!!)
    }

    @Test
    fun `timeSaved returns aggregate confidence`() {
        val result = trendsService.timeSaved(userId)
        assertEquals("high", result.confidence) // 2 high (19+25=44min) vs 1 medium (8min) → weighted avg > 2.5
    }

    @Test
    fun `timeSaved returns zeros for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-trends@example.com"))
        val result = trendsService.timeSaved(emptyUser.id!!)
        assertEquals(0, result.total)
        assertTrue(result.byDomain.isEmpty())
        assertTrue(result.byTaskMix.isEmpty())
    }

    @Test
    fun `activityHeatmap returns 7x24 grid`() {
        val result = trendsService.activityHeatmap(userId)
        assertEquals(7, result.size)
        assertTrue(result.all { it.size == 24 })
    }
}
