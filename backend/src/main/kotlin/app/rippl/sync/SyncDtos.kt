package app.rippl.sync

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class SyncRequest(val sessions: List<SyncSessionDto>)

data class SyncSessionDto(
    val id: String,
    val domain: String,
    val startedAt: Long,
    val endedAt: Long,
    val activeSeconds: Int,
    val date: LocalDate,
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val activityType: List<String>? = null,
    val estimatedWithoutMinutes: Int? = null,
    val timeSavedMinutes: Int? = null,
    val logged: Boolean? = false
)

data class SyncResponse(val accepted: Int, val duplicates: Int, val syncedAt: Long)
