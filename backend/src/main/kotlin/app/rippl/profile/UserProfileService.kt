package app.rippl.profile

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

@Service
class UserProfileService(
    private val repository: UserProfileRepository
) {

    companion object {
        val TEMPLATES = listOf(
            ProfileTemplate("developer", TaskMix(coding = 0.7, research = 0.2, planning = 0.1)),
            ProfileTemplate("marketer", TaskMix(writing = 0.6, research = 0.3, planning = 0.1)),
            ProfileTemplate("researcher", TaskMix(research = 0.5, writing = 0.3, planning = 0.2))
        )

        private const val SUM_TOLERANCE = 0.01
    }

    fun getProfile(userId: UUID): ProfileResponse? {
        val profile = repository.findByUserId(userId) ?: return null
        return ProfileResponse(profile.taskMix, profile.personalAdjustmentFactor, profile.onboarded)
    }

    fun updateProfile(userId: UUID, request: ProfileUpdateRequest): ProfileResponse {
        if (request.taskMix != null) validateTaskMix(request.taskMix)
        if (request.personalAdjustmentFactor != null) validateAdjustmentFactor(request.personalAdjustmentFactor)

        val profile = repository.findByUserId(userId) ?: UserProfile(userId = userId)

        if (request.taskMix != null) profile.taskMix = request.taskMix
        if (request.personalAdjustmentFactor != null) profile.personalAdjustmentFactor = request.personalAdjustmentFactor
        profile.onboarded = true
        profile.updatedAt = Instant.now()

        repository.save(profile)
        return ProfileResponse(profile.taskMix, profile.personalAdjustmentFactor, profile.onboarded)
    }

    fun ensureProfileExists(userId: UUID) {
        if (repository.findByUserId(userId) != null) return
        repository.save(UserProfile(userId = userId))
    }

    fun getTemplates(): List<ProfileTemplate> = TEMPLATES

    private fun validateTaskMix(mix: TaskMix) {
        val values = listOf(mix.writing, mix.coding, mix.research, mix.planning, mix.communication, mix.other)
        if (values.any { it < 0.0 || it > 1.0 }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "All task_mix values must be between 0 and 1")
        }
        if (abs(mix.sum() - 1.0) > SUM_TOLERANCE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "task_mix weights must sum to 1.0 (within 0.01 tolerance)")
        }
    }

    private fun validateAdjustmentFactor(factor: Double) {
        if (factor < 0.1 || factor > 3.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "personal_adjustment_factor must be between 0.1 and 3.0")
        }
    }
}
