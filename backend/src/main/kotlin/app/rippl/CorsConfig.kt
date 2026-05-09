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
    @Value("\${app.extension-id}") private val extensionId: String,
    @Value("\${app.extension-dev-id:}") private val extensionDevId: String,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val extensionOrigins = listOfNotNull(
            extensionId.takeIf { it.isNotBlank() }?.let { "chrome-extension://$it" },
            extensionDevId.takeIf { it.isNotBlank() }?.let { "chrome-extension://$it" }
        )

        val extensionConfig = CorsConfiguration().apply {
            allowedOrigins = extensionOrigins
            allowedMethods = listOf("POST", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "X-Request-Id")
            allowCredentials = false
        }
        source.registerCorsConfiguration("/api/sync/**", extensionConfig)
        source.registerCorsConfiguration("/v1/**", extensionConfig)

        val dashboardConfig = CorsConfiguration().apply {
            allowedOrigins = listOf(frontendUrl)
            allowedMethods = listOf("GET", "POST", "DELETE")
            allowedHeaders = listOf("Authorization", "Content-Type", "X-Request-Id")
            allowCredentials = true
        }
        source.registerCorsConfiguration("/api/**", dashboardConfig)

        return source
    }
}
