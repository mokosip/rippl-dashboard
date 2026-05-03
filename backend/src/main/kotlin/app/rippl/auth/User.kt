package app.rippl.auth

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    var email: String,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
)
