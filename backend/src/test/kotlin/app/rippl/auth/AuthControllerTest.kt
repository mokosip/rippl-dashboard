package app.rippl.auth

import app.rippl.TestcontainersConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class AuthControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var userRepository: UserRepository

    @Test
    fun `POST magic-link returns 200 for valid email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "test@example.com"}"""
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST magic-link returns 400 for missing email`() {
        mockMvc.post("/api/auth/magic-link") {
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET me returns 401 when not authenticated`() {
        mockMvc.get("/api/auth/me")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET me returns user when authenticated`() {
        val user = userRepository.findByEmail("auth-test@example.com")
            ?: userRepository.save(User(email = "auth-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.get("/api/auth/me") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("auth-test@example.com") }
        }
    }

    @Test
    fun `POST logout clears session cookie`() {
        val user = userRepository.findByEmail("logout-test@example.com")
            ?: userRepository.save(User(email = "logout-test@example.com"))
        val token = jwtService.generateSessionToken(user.id!!)

        mockMvc.post("/api/auth/logout") {
            cookie(jakarta.servlet.http.Cookie("session", token))
        }.andExpect {
            status { isOk() }
            cookie { maxAge("session", 0) }
        }
    }
}
