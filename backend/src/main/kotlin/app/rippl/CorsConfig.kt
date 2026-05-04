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
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()

        val syncConfig = CorsConfiguration().apply {
            allowedOrigins = listOf(frontendUrl)
            allowedOriginPatterns = listOf("chrome-extension://*")
            allowedMethods = listOf("POST")
            allowedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = false
        }
        source.registerCorsConfiguration("/api/sync/**", syncConfig)

        val dashboardConfig = CorsConfiguration().apply {
            allowedOrigins = listOf(frontendUrl)
            allowedMethods = listOf("GET", "POST", "DELETE")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        source.registerCorsConfiguration("/api/**", dashboardConfig)

        return source
    }
}
