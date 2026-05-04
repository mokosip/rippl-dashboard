package app.rippl.account

import app.rippl.auth.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/api/account")
class AccountController(private val userRepository: UserRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @DeleteMapping
    @Transactional
    fun deleteAccount(@AuthenticationPrincipal userId: UUID): ResponseEntity<Void> {
        log.debug("Account deletion requested for userId: {}", userId)
        userRepository.deleteById(userId)
        log.debug("Account deleted for userId: {}", userId)
        val cookie = ResponseCookie.from("session", "")
            .httpOnly(true).secure(true).path("/").maxAge(Duration.ZERO).sameSite("Lax").build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
