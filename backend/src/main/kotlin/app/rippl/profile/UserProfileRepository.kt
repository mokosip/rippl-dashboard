package app.rippl.profile

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserProfileRepository : JpaRepository<UserProfile, UUID> {
    fun findByUserId(userId: UUID): UserProfile?
    fun deleteByUserId(userId: UUID)
}
