package org.example.kalkulationsprogramm.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class ZeiterfassungSecurityFilter : Filter {

    @Value("\${zeiterfassung.security.enabled:true}")
    private var securityEnabled: Boolean = true

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (!securityEnabled) {
            chain.doFilter(request, response)
            return
        }

        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val path = httpRequest.requestURI
        val clientIp = getClientIp(httpRequest)

        if (isLocalIp(clientIp) || isAllowedPath(path)) {
            chain.doFilter(request, response)
            return
        }

        httpResponse.status = HttpServletResponse.SC_FORBIDDEN
        httpResponse.contentType = "application/json"
        httpResponse.writer.write("{\"error\":\"Zugriff verweigert\"}")
    }

    private fun getClientIp(request: HttpServletRequest): String? {
        val cfIp = request.getHeader("CF-Connecting-IP")
        if (!cfIp.isNullOrBlank()) {
            return cfIp
        }

        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",")[0].trim()
        }

        return request.remoteAddr
    }

    private fun isLocalIp(ip: String?): Boolean {
        return ip != null && LOCAL_IP_PREFIXES.any { ip.startsWith(it) }
    }

    private fun isAllowedPath(path: String?): Boolean {
        return path != null && ALLOWED_PATHS.any { path.startsWith(it) }
    }

    companion object {
        private val ALLOWED_PATHS = listOf(
            "/zeiterfassung",
            "/api/mitarbeiter/by-token",
            "/api/projekte",
            "/api/produktkategorien",
            "/api/arbeitsgaenge",
            "/api/kunden",
            "/api/lieferanten",
            "/api/zeiterfassung",
            "/api/urlaub",
            "/api/anfragen",
            "/api/dokumente",
            "/api/images",
            "/api/kalender/mobile",
            "/api/push",
            "/api/abwesenheit",
        )

        private val LOCAL_IP_PREFIXES = listOf(
            "127.0.0.1",
            "192.168.",
            "10.",
            "100.",
            "172.16.",
            "172.17.",
            "172.18.",
            "172.19.",
            "172.20.",
            "172.21.",
            "172.22.",
            "172.23.",
            "172.24.",
            "172.25.",
            "172.26.",
            "172.27.",
            "172.28.",
            "172.29.",
            "172.30.",
            "172.31.",
            "0:0:0:0:0:0:0:1",
        )
    }
}
