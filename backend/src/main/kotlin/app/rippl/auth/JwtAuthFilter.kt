package app.rippl.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = request.cookies?.find { it.name == "session" }?.value
        if (token == null) {
            log.debug("No session cookie found for {} {}", request.method, request.requestURI)
        } else {
            val userId = jwtService.validateSessionToken(token)
            if (userId != null) {
                log.debug("Session token valid, userId: {}", userId)
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            } else {
                log.debug("Session token invalid for {} {}", request.method, request.requestURI)
            }
        }
        chain.doFilter(request, response)
    }
}
