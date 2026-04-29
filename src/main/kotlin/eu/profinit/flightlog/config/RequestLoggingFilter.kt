package eu.profinit.flightlog.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Populates MDC for each request so every log line / Logstash event is
 * automatically tagged with `requestId`, `user`, `role`, `ip`, `method`, `path`.
 *
 * Also emits one INFO access-log line per finished request (logger
 * `eu.profinit.flightlog.access`). Static assets / actuator are skipped.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val accessLog = LoggerFactory.getLogger("eu.profinit.flightlog.access")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val p = request.requestURI
        return p.startsWith("/actuator")
            || p.endsWith(".js") || p.endsWith(".css") || p.endsWith(".ico")
            || p.endsWith(".png") || p.endsWith(".webmanifest")
    }

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val requestId = req.getHeader("X-Request-Id")?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().substring(0, 8)
        MDC.put("requestId", requestId)
        MDC.put("ip", clientIp(req))
        MDC.put("method", req.method)
        MDC.put("path", req.requestURI)
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.name != "anonymousUser") {
            MDC.put("user", auth.name)
            val role = auth.authorities?.firstOrNull()?.authority?.removePrefix("ROLE_")
            if (role != null) MDC.put("role", role)
        }
        val start = System.currentTimeMillis()
        try {
            chain.doFilter(req, res)
        } finally {
            // Re-read auth in case login happened during this request.
            val authAfter = SecurityContextHolder.getContext().authentication
            if (authAfter != null && authAfter.isAuthenticated && authAfter.name != "anonymousUser") {
                MDC.put("user", authAfter.name)
            }
            val dur = System.currentTimeMillis() - start
            MDC.put("durationMs", dur.toString())
            MDC.put("status", res.status.toString())
            res.setHeader("X-Request-Id", requestId)
            accessLog.info("{} {} -> {} in {}ms", req.method, req.requestURI, res.status, dur)
            MDC.clear()
        }
    }

    private fun clientIp(req: HttpServletRequest): String {
        val xff = req.getHeader("X-Forwarded-For")
        if (!xff.isNullOrBlank()) return xff.substringBefore(',').trim()
        return req.remoteAddr ?: "?"
    }
}
