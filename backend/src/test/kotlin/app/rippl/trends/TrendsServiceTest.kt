package app.rippl.trends

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
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfig::class)
class TrendsServiceTest {

    @Autowired lateinit var trendsService: TrendsService
    @Autowired lateinit var sessionRepository: SessionRepository
    @Autowired lateinit var userRepository: UserRepository

    private lateinit var userId: UUID

    @BeforeEach
    fun setup() {
        sessionRepository.deleteAll()
        val user = userRepository.findByEmail("trends-test@example.com")
            ?: userRepository.save(User(email = "trends-test@example.com"))
        userId = user.id!!

        sessionRepository.saveAll(listOf(
            Session("ts-1", userId, "claude.ai", 1000, 2000, 600, LocalDate.of(2026, 4, 28),
                "coding", 30, 19),
            Session("ts-2", userId, "chatgpt.com", 2000, 3000, 300, LocalDate.of(2026, 4, 28),
                "writing", 15, 8),
            Session("ts-3", userId, "claude.ai", 3000, 4000, 900, LocalDate.of(2026, 5, 1),
                "coding", 45, 25)
        ))
    }

    @Test
    fun `weekly returns data grouped by week and domain`() {
        val result = trendsService.weekly(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        assertTrue(result.isNotEmpty())
        val claudeEntries = result.filter { it.domain == "claude.ai" }
        assertTrue(claudeEntries.isNotEmpty())
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
        assertEquals(44, result.byActivity["coding"]) // 19 + 25
        assertEquals(8, result.byActivity["writing"])
    }

    @Test
    fun `timeSaved unnests multi-activity sessions`() {
        sessionRepository.deleteAll()
        val user = userRepository.findByEmail("trends-test@example.com")!!
        sessionRepository.saveAll(listOf(
            Session("ts-multi-1", user.id!!, "claude.ai", 1000, 2000, 600, LocalDate.of(2026, 5, 1),
                "coding, review", 60, 30),
            Session("ts-multi-2", user.id!!, "claude.ai", 3000, 4000, 300, LocalDate.of(2026, 5, 1),
                "review", 30, 10)
        ))
        val result = trendsService.timeSaved(user.id!!)
        assertEquals(30, result.byActivity["coding"])
        assertEquals(40, result.byActivity["review"]) // 30 + 10
    }

    @Test
    fun `timeSaved returns zeros for user with no sessions`() {
        val emptyUser = userRepository.save(User(email = "empty-trends@example.com"))
        val result = trendsService.timeSaved(emptyUser.id!!)
        assertEquals(0, result.total)
        assertTrue(result.byDomain.isEmpty())
    }
}
