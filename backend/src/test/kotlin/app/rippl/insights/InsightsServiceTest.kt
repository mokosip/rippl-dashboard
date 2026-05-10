package app.rippl.insights

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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class InsightsServiceTest {

    @Autowired lateinit var insightsService: InsightsService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var userId: UUID

    private fun insertActivitySession(
        id: UUID, userId: UUID, domain: String,
        startedAt: Instant, activeMs: Long
    ) {
        jdbc.update(
            """
            INSERT INTO activity_sessions (id, user_id, collector_type, collector_session_id,
                source_type, domain, surface, raw_payload, started_at, ended_at, duration_ms, active_ms)
            VALUES (?, ?, 'browser', ?, 'extension', ?, 'web', ?::jsonb, ?, ?, ?, ?)
            """,
            id, userId, UUID.randomUUID().toString(), domain, "{}",
            java.sql.Timestamp.from(startedAt),
            java.sql.Timestamp.from(startedAt.plusMillis(activeMs)),
            activeMs, activeMs
        )
    }

    @BeforeEach
    fun setup() {
        jdbc.update("DELETE FROM scored_sessions")
        jdbc.update("DELETE FROM activity_sessions")
        val user = userRepository.findByEmail("insights-test@example.com")
            ?: userRepository.save(User(email = "insights-test@example.com"))
        userId = user.id!!

        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = thisMonday.minusWeeks(1)
        val thisFriday = thisMonday.with(DayOfWeek.FRIDAY)

        // This week Monday: claude.ai, 1 hour active
        val s1 = UUID.randomUUID()
        insertActivitySession(s1, userId, "claude.ai",
            thisMonday.atStartOfDay().toInstant(ZoneOffset.UTC), 3600000)

        // Last week Monday: claude.ai, 30 min active
        val s2 = UUID.randomUUID()
        insertActivitySession(s2, userId, "claude.ai",
            lastMonday.atStartOfDay().toInstant(ZoneOffset.UTC), 1800000)

        // This week Monday: chatgpt.com, 10 min active
        val s3 = UUID.randomUUID()
        insertActivitySession(s3, userId, "chatgpt.com",
            thisMonday.atStartOfDay().plusSeconds(7200).toInstant(ZoneOffset.UTC), 600000)

        // This week Friday: claude.ai, 15 min active
        val s4 = UUID.randomUUID()
        insertActivitySession(s4, userId, "claude.ai",
            thisFriday.atStartOfDay().toInstant(ZoneOffset.UTC), 900000)
    }

    @Test
    fun `generates weekly usage moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val weeklyMoment = moments.find { it.type == "weekly_usage" }
        assertNotNull(weeklyMoment)
        assertTrue(weeklyMoment!!.message.contains("this week"))
    }

    @Test
    fun `generates top tool moment when multiple tools used`() {
        val moments = insightsService.mirrorMoments(userId)
        val toolMoment = moments.find { it.type == "top_tool" }
        assertNotNull(toolMoment)
        assertTrue(toolMoment!!.message.contains("claude.ai"))
    }

    @Test
    fun `does not generate time_saving_activity moment`() {
        val moments = insightsService.mirrorMoments(userId)
        assertNull(moments.find { it.type == "time_saving_activity" })
    }

    @Test
    fun `returns empty list for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-insights@example.com"))
        val moments = insightsService.mirrorMoments(emptyUser.id!!)
        assertTrue(moments.isEmpty())
    }
}
