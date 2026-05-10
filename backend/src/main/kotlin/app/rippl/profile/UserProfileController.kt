package app.rippl.profile

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/profile")
class UserProfileController(
    private val service: UserProfileService
) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userId: UUID): ResponseEntity<ProfileResponse> {
        val profile = service.getProfile(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile)
    }

    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: ProfileUpdateRequest
    ): ResponseEntity<ProfileResponse> {
        val profile = service.updateProfile(userId, request)
        return ResponseEntity.ok(profile)
    }

    @GetMapping("/templates")
    fun getTemplates(): ResponseEntity<List<ProfileTemplate>> {
        return ResponseEntity.ok(service.getTemplates())
    }
}
