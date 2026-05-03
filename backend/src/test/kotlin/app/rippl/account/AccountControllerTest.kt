package app.rippl.account

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import app.rippl.sessions.Session
import app.rippl.sessions.SessionRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class AccountControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var sessionRepository: SessionRepository

    @Test
    fun `DELETE account removes user and all data`() {
        val user = userRepository.save(User(email = "delete-me-${System.nanoTime()}@example.com"))
        sessionRepository.save(
            Session("del-${System.nanoTime()}", user.id!!, "claude.ai", 1000, 2000, 600, LocalDate.now())
        )
        val cookie = Cookie("session", jwtService.generateSessionToken(user.id!!))

        mockMvc.delete("/api/account") {
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }

        assertFalse(userRepository.findById(user.id!!).isPresent)
        assertEquals(0, sessionRepository.countByUserId(user.id!!))
    }
}
