package app.rippl.auth

import app.rippl.collectors.ExtensionTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val extensionTokenService: ExtensionTokenService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val isSyncEndpoint = request.requestURI.startsWith("/api/sync/")
        val userId = authenticateBearer(request) ?: if (!isSyncEndpoint) authenticateCookie(request) else null

        if (userId != null) {
            val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
            SecurityContextHolder.getContext().authentication = auth
        }

        chain.doFilter(request, response)
    }

    private fun authenticateBearer(request: HttpServletRequest): java.util.UUID? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        val token = header.removePrefix("Bearer ")
        val userId = extensionTokenService.validateToken(token)
        if (userId != null) {
            log.debug("Bearer token valid, userId: {}", userId)
        } else {
            log.debug("Bearer token invalid for {} {}", request.method, request.requestURI)
        }
        return userId
    }

    private fun authenticateCookie(request: HttpServletRequest): java.util.UUID? {
        val token = request.cookies?.find { it.name == "session" }?.value
        if (token == null) {
            log.debug("No session cookie found for {} {}", request.method, request.requestURI)
            return null
        }
        val claims = jwtService.validateSessionToken(token)
        if (claims == null) {
            log.debug("Session token invalid for {} {}", request.method, request.requestURI)
            return null
        }
        val user = userRepository.findById(claims.userId).orElse(null)
        if (user == null) {
            log.debug("Session token references unknown userId: {}", claims.userId)
            return null
        }
        if (user.sessionsInvalidatedAt != null && claims.issuedAt.isBefore(user.sessionsInvalidatedAt)) {
            log.debug("Session token invalidated for userId: {}", claims.userId)
            return null
        }
        log.debug("Session token valid, userId: {}", claims.userId)
        return claims.userId
    }
}
