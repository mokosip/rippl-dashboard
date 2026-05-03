package app.rippl.auth

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.util.UUID

data class MagicLinkRequest(val email: String?)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
    @Value("\${app.frontend-url}") private val frontendUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/magic-link")
    fun sendMagicLink(@RequestBody request: MagicLinkRequest): ResponseEntity<Any> {
        val email = request.email?.takeIf { it.contains("@") }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "invalid_email"))
        log.debug("Magic-link requested for email: {}", email)
        authService.sendMagicLink(email)
        return ResponseEntity.ok(mapOf("sent" to true))
    }

    @GetMapping("/verify")
    fun verify(@RequestParam token: String): ResponseEntity<Void> {
        log.debug("Token verification attempt: {}...", token.take(20))
        val user = authService.verifyMagicLink(token)
            ?: run {
                log.debug("Token verification failed: invalid or expired token")
                return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "$frontendUrl/login?error=invalid_token")
                    .build()
            }

        log.debug("Token verification succeeded for user: {}", user.id)
        val sessionToken = jwtService.generateSessionToken(user.id!!)
        val cookie = ResponseCookie.from("session", sessionToken)
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ofDays(7))
            .sameSite("Lax")
            .build()

        return ResponseEntity.status(302)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.LOCATION, "$frontendUrl/")
            .build()
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: UUID): ResponseEntity<Map<String, Any>> {
        log.debug("Looking up user: {}", userId)
        val user = authService.findById(userId)
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(mapOf("id" to user.id!!, "email" to user.email))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
        log.debug("User logout")
        val cookie = ResponseCookie.from("session", "")
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ZERO)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
