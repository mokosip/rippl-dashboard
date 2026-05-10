package app.rippl.profile

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class TaskMixConverter : AttributeConverter<TaskMix, String> {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    override fun convertToDatabaseColumn(attribute: TaskMix?): String {
        return objectMapper.writeValueAsString(attribute ?: TaskMix.GLOBAL_DEFAULT)
    }

    override fun convertToEntityAttribute(dbData: String?): TaskMix {
        if (dbData.isNullOrBlank()) return TaskMix.GLOBAL_DEFAULT
        return objectMapper.readValue(dbData)
    }
}
