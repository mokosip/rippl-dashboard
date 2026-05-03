package app.rippl.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "token_hash", nullable = false)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
)
