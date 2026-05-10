package app.rippl.profile

import app.rippl.TestcontainersConfig
import app.rippl.auth.JwtService
import app.rippl.auth.User
import app.rippl.auth.UserRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
class UserProfileControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var profileRepository: UserProfileRepository

    private lateinit var user: User
    private lateinit var cookie: Cookie

    @BeforeEach
    fun setup() {
        user = userRepository.save(User(email = "profile-${UUID.randomUUID()}@example.com"))
        cookie = Cookie("session", jwtService.generateSessionToken(user.id!!))
    }

    @Test
    fun `GET profile returns 404 when no profile exists`() {
        mockMvc.get("/api/profile") {
            cookie(cookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PUT profile creates new profile and returns it`() {
        mockMvc.put("/api/profile") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "task_mix": {
                        "writing": 0.0,
                        "coding": 0.7,
                        "research": 0.2,
                        "planning": 0.1,
                        "communication": 0.0,
                        "other": 0.0
                    }
                }
            """
        }.andExpect {
            status { isOk() }
            jsonPath("$.task_mix.coding") { value(0.7) }
            jsonPath("$.personal_adjustment_factor") { value(1.0) }
        }
    }

    @Test
    fun `PUT profile returns 400 when weights do not sum to 1`() {
        mockMvc.put("/api/profile") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "task_mix": {
                        "writing": 0.5,
                        "coding": 0.5,
                        "research": 0.5,
                        "planning": 0.0,
                        "communication": 0.0,
                        "other": 0.0
                    }
                }
            """
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT profile returns 400 when adjustment factor out of range`() {
        mockMvc.put("/api/profile") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"personal_adjustment_factor": 5.0}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET profile returns profile after PUT`() {
        mockMvc.put("/api/profile") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "task_mix": {
                        "writing": 0.6,
                        "coding": 0.0,
                        "research": 0.3,
                        "planning": 0.1,
                        "communication": 0.0,
                        "other": 0.0
                    }
                }
            """
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/profile") {
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.task_mix.writing") { value(0.6) }
        }
    }

    @Test
    fun `GET templates returns persona list`() {
        mockMvc.get("/api/profile/templates") {
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].name") { value("developer") }
        }
    }

    @Test
    fun `profile is deleted when user is deleted via CASCADE`() {
        mockMvc.put("/api/profile") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "task_mix": {
                        "writing": 0.0,
                        "coding": 1.0,
                        "research": 0.0,
                        "planning": 0.0,
                        "communication": 0.0,
                        "other": 0.0
                    }
                }
            """
        }.andExpect { status { isOk() } }

        userRepository.deleteById(user.id!!)
        assertNull(profileRepository.findByUserId(user.id!!))
    }
}
