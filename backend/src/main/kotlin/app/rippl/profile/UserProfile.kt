package app.rippl.profile

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnTransformer
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Column(name = "user_id", unique = true, nullable = false)
    var userId: UUID,

    @Convert(converter = TaskMixConverter::class)
    @Column(name = "task_mix", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    var taskMix: TaskMix = TaskMix.GLOBAL_DEFAULT,

    @Column(name = "personal_adjustment_factor", nullable = false)
    var personalAdjustmentFactor: Double = 1.0,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
