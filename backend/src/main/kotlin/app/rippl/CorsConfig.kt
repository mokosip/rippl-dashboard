package app.rippl

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${app.frontend-url}") private val frontendUrl: String,
    @Value("\${app.extension-id:}") private val extensionId: String,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()

        val origins = mutableListOf(frontendUrl)
        if (extensionId.isNotBlank()) {
            origins.add("chrome-extension://$extensionId")
        }

        val config = CorsConfiguration().apply {
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "DELETE")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        source.registerCorsConfiguration("/api/**", config)

        return source
    }
}
