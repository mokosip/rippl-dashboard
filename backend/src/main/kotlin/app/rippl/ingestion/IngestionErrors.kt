package app.rippl.ingestion

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProblemResponse(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String,
    @JsonProperty("error_code")
    val errorCode: String,
    @JsonProperty("field_errors")
    val fieldErrors: List<FieldError>? = null
)

data class FieldError(
    val field: String,
    val message: String
)

class IngestionProblemException(
    val httpStatus: HttpStatus,
    val errorCode: String,
    override val message: String,
    val fieldErrors: List<FieldError>? = null
) : RuntimeException(message)
