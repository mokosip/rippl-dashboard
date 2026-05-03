package app.rippl.auth

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

    @PostMapping("/magic-link")
    fun sendMagicLink(@RequestBody request: MagicLinkRequest): ResponseEntity<Any> {
        val email = request.email?.takeIf { it.contains("@") }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "invalid_email"))
        authService.sendMagicLink(email)
        return ResponseEntity.ok(mapOf("sent" to true))
    }

    @GetMapping("/verify")
    fun verify(@RequestParam token: String): ResponseEntity<Void> {
        val user = authService.verifyMagicLink(token)
            ?: return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, "$frontendUrl/login?error=invalid_token")
                .build()

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
        val user = authService.findById(userId)
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(mapOf("id" to user.id!!, "email" to user.email))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
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
