package app.rippl.collectors

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "extension_tokens")
class ExtensionToken(
    @Column(name = "collector_id", nullable = false, unique = true)
    var collectorId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "scope", nullable = false)
    var scope: String = "ingest",

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
)
