package app.rippl.collectors

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
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class CollectorsControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var collectorRepository: CollectorRepository

    private lateinit var sessionCookie: Cookie
    private lateinit var userId: java.util.UUID

    @BeforeEach
    fun setup() {
        val user = userRepository.findByEmail("collector-test@example.com")
            ?: userRepository.save(User(email = "collector-test@example.com"))
        userId = user.id!!
        sessionCookie = Cookie("session", jwtService.generateSessionToken(userId))
        collectorRepository.deleteAll(collectorRepository.findByUserId(userId))
    }

    @Test
    fun `GET collectors returns empty list initially`() {
        mockMvc.get("/api/collectors") {
            cookie(sessionCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `POST collectors creates new collector`() {
        mockMvc.post("/api/collectors") {
            contentType = MediaType.APPLICATION_JSON
            cookie(sessionCookie)
            content = """{"type": "chrome_extension"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.type") { value("chrome_extension") }
            jsonPath("$.enabled") { value(true) }
        }
    }

    @Test
    fun `DELETE collectors removes collector`() {
        val collector = collectorRepository.save(Collector(userId = userId, type = "chrome_extension"))

        mockMvc.delete("/api/collectors/${collector.id}") {
            cookie(sessionCookie)
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/collectors") {
            cookie(sessionCookie)
        }.andExpect {
            jsonPath("$.length()") { value(0) }
        }
    }
}
