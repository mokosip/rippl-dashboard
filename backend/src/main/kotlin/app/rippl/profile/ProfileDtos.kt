package app.rippl.profile

import com.fasterxml.jackson.annotation.JsonProperty

data class ProfileResponse(
    @JsonProperty("task_mix") val taskMix: TaskMix,
    @JsonProperty("personal_adjustment_factor") val personalAdjustmentFactor: Double
)

data class ProfileUpdateRequest(
    @JsonProperty("task_mix") val taskMix: TaskMix? = null,
    @JsonProperty("personal_adjustment_factor") val personalAdjustmentFactor: Double? = null
)

data class ProfileTemplate(
    val name: String,
    @JsonProperty("task_mix") val taskMix: TaskMix
)
