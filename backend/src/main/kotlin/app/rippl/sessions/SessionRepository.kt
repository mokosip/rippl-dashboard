package app.rippl.sessions

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface SessionRepository : JpaRepository<Session, String> {
    fun findByUserIdAndDateBetween(userId: UUID, start: LocalDate, end: LocalDate): List<Session>
    fun findByUserId(userId: UUID): List<Session>
    fun deleteByUserId(userId: UUID)
    fun countByUserId(userId: UUID): Long
}
