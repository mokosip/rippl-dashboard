package app.rippl.account

import app.rippl.auth.UserRepository
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

    @DeleteMapping
    @Transactional
    fun deleteAccount(@AuthenticationPrincipal userId: UUID): ResponseEntity<Void> {
        userRepository.deleteById(userId)
        val cookie = ResponseCookie.from("session", "")
            .httpOnly(true).path("/").maxAge(Duration.ZERO).build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }
}
