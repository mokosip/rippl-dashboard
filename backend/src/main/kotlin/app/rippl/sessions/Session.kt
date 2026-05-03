package app.rippl.sessions

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "sessions")
class Session(
    @Id
    var id: String,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var domain: String,

    @Column(name = "started_at", nullable = false)
    var startedAt: Long,

    @Column(name = "ended_at", nullable = false)
    var endedAt: Long,

    @Column(name = "active_seconds", nullable = false)
    var activeSeconds: Int,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(name = "activity_type")
    var activityType: String? = null,

    @Column(name = "estimated_without_minutes")
    var estimatedWithoutMinutes: Int? = null,

    @Column(name = "time_saved_minutes")
    var timeSavedMinutes: Int? = null,

    var logged: Boolean = false,

    @Column(name = "synced_at")
    var syncedAt: Instant = Instant.now()
)
