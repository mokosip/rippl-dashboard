package app.rippl.ingestion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = false)
data class ActivitySessionRequest(
    val collector: CollectorPayload,
    val source: SourcePayload,
    val session: SessionPayload,
    val privacy: PrivacyPayload,
    val metrics: Map<String, Any?> = emptyMap(),
    val context: Map<String, Any?> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = false)
data class CollectorPayload(
    val type: String,
    val version: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = false)
data class SourcePayload(
    val type: String,
    val version: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = false)
data class SessionPayload(
    val id: String,
    @JsonProperty("started_at")
    val startedAt: Long,
    @JsonProperty("ended_at")
    val endedAt: Long
)

@JsonIgnoreProperties(ignoreUnknown = false)
data class PrivacyPayload(
    @JsonProperty("content_collected")
    val contentCollected: Boolean,
    @JsonProperty("content_sent")
    val contentSent: Boolean,
    @JsonProperty("prompt_collected")
    val promptCollected: Boolean,
    @JsonProperty("response_collected")
    val responseCollected: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = false)
data class SessionFeedbackRequest(
    val type: String,
    val value: JsonNode
)

data class IngestWriteResult(
    val sessionId: UUID,
    val deduped: Boolean
)
