package app.rippl.collectors

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "collectors")
class Collector(
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var type: String,

    var enabled: Boolean = true,

    @Column(name = "linked_at")
    var linkedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null
)
