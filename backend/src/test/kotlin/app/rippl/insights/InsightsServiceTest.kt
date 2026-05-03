package app.rippl.insights

import app.rippl.TestcontainersConfig
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.sessions.Session
import app.rippl.sessions.SessionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class InsightsServiceTest {

    @Autowired lateinit var insightsService: InsightsService
    @Autowired lateinit var sessionRepository: SessionRepository
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var userId: UUID

    @BeforeEach
    fun setup() {
        sessionRepository.deleteAll()
        val user = userRepository.findByEmail("insights-test@example.com")
            ?: userRepository.save(User(email = "insights-test@example.com"))
        userId = user.id!!

        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastMonday = thisMonday.minusWeeks(1)

        sessionRepository.saveAll(listOf(
            Session("ins-1", userId, "claude.ai", 1000, 2000, 3600, thisMonday,
                "coding", 60, 40),
            Session("ins-2", userId, "claude.ai", 2000, 3000, 1800, lastMonday,
                "coding", 30, 20),
            Session("ins-3", userId, "chatgpt.com", 3000, 4000, 600, thisMonday,
                "writing", 10, 5),
            Session("ins-4", userId, "claude.ai", 4000, 5000, 900, thisMonday.with(DayOfWeek.FRIDAY),
                "coding", 20, 12)
        ))
    }

    @Test
    fun `generates weekly usage moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val weeklyMoment = moments.find { it.type == "weekly_usage" }
        assertNotNull(weeklyMoment)
        assertTrue(weeklyMoment!!.message.contains("hours this week"))
    }

    @Test
    fun `generates top tool moment when multiple tools used`() {
        val moments = insightsService.mirrorMoments(userId)
        val toolMoment = moments.find { it.type == "top_tool" }
        assertNotNull(toolMoment)
        assertTrue(toolMoment!!.message.contains("claude.ai"))
    }

    @Test
    fun `generates time saving activity moment`() {
        val moments = insightsService.mirrorMoments(userId)
        val activityMoment = moments.find { it.type == "time_saving_activity" }
        assertNotNull(activityMoment)
        assertTrue(activityMoment!!.message.contains("coding"))
    }

    @Test
    fun `returns empty list for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-insights@example.com"))
        val moments = insightsService.mirrorMoments(emptyUser.id!!)
        assertTrue(moments.isEmpty())
    }
}
